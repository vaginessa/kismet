package desutine.kismet.common.tile;

import desutine.kismet.Kismet;
import desutine.kismet.ModLogger;
import desutine.kismet.common.ConfigKismet;
import desutine.kismet.common.block.BlockDisplay;
import desutine.kismet.common.block.BlockTimedDisplay;
import desutine.kismet.common.registry.ModBlocks;
import desutine.kismet.util.StackHelper;
import desutine.kismet.util.TargetGenerationResult;
import desutine.kismet.util.TargetHelper;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import org.apache.commons.lang3.time.DurationFormatUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TileDisplay extends TileEntity implements ITickable {
    private final TextFormatting[] colors = new TextFormatting[] {
            TextFormatting.WHITE,
            TextFormatting.GREEN,
            TextFormatting.BLUE,
            TextFormatting.DARK_PURPLE,
            TextFormatting.GOLD
    };
    private int score;
    private long deadline;
    private ItemStack target;
    private List<ItemStack> lastTargets;
    private HashMap<String, Integer> modWeights;
    private boolean stateChanged;
    private long newTargetTimeout;

    public TileDisplay() {
        super();
        modWeights = new HashMap<>();
        lastTargets = new ArrayList<>();
    }

    public String getStylizedDeadline() {
        // format the time remaining as hh:mm:ss
        // less error-prone way to get the seconds already rounded up
        final long l = getDeadline() - worldObj.getTotalWorldTime();
        long remainingTime = l / 20 + (l % 20 == 0 ? 0 : 1);

        // yellow -> red -> bold red
        String styleCode;
        if (remainingTime <= 15 * 60) {
            if (remainingTime > 10 * 60) {
                styleCode = TextFormatting.YELLOW.toString();
            } else if (remainingTime > 5 * 60) {
                styleCode = TextFormatting.RED.toString();
            } else {
                // bold after colour
                styleCode = TextFormatting.RED.toString() + TextFormatting.BOLD.toString();
            }
        } else styleCode = "";

        String remainingTimeString = DurationFormatUtils.formatDurationHMS(remainingTime * 1000);
        remainingTimeString = remainingTimeString.substring(0, remainingTimeString.indexOf("."));

        String resetStyleCode = TextFormatting.RESET.toString();
        return styleCode + remainingTimeString + resetStyleCode;
    }

    public long getDeadline() {
        return deadline;
    }

    public void setDeadline(long deadline) {
        this.deadline = deadline;
    }

    public String getStylizedScore() {
        int deliminator = getScore() / 10;
        String styleCode;
        if (deliminator >= colors.length) {
            // use the last colour
            styleCode = colors[colors.length - 1].toString();
        } else {
            styleCode = colors[deliminator].toString();
        }

        String resetStyleCode = TextFormatting.RESET.toString();
        return styleCode + score + resetStyleCode;
    }

    public int getScore() {
        return score;
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    public void setScore(int score) {
        if (this.score != score) {
            this.score = score;
        }
    }

    @Override
    public void update() {
        // isDirty is set to true whenever the internal state of the tile is changed
        // name coming from the parent's method markDirty()
        boolean isDirty = false;
        if (getBlockType() instanceof BlockTimedDisplay) {
            isDirty = checkForDeadline();
        }
        isDirty |= checkForNullTarget();

        if (isDirty) {
            markDirty();
        }
        if (this.stateChanged) {
            stateChanged = false;
            worldObj.setBlockState(pos, ModBlocks.CHILL_DISPLAY.getActualState(worldObj.getBlockState(pos), worldObj, pos));
        }
    }

    private boolean checkForNullTarget() {
        if (getTarget() != null) return false;
        return getNewTarget();
    }

    private boolean checkForDeadline() {
        if (getDeadline() < worldObj.getTotalWorldTime()) {
            setDeadline(worldObj.getTotalWorldTime() + ConfigKismet.getTimedLimit() * 20 * 60);

            if (!isFulfilled()) {
                lastTargets.clear();
                setScore(0);
            }
            setFulfilled(false);

            return getNewTarget();
        }
        return false;
    }

    /**
     * Tries to get a new target from config
     *
     * @return true if a new target has been assigned (even if the new target is invalid/null)
     */
    public boolean getNewTarget() {
        // a timeout for server issues
        if (newTargetTimeout > worldObj.getTotalWorldTime()) return false;

        // remove the fulfillment
        final IBlockState state = worldObj.getBlockState(pos);
        worldObj.setBlockState(pos, state.withProperty(BlockDisplay.FULFILLED, false));

        // only server pls from now on
        if (worldObj.isRemote) {
            return false;
        }

        final ItemStack oldTarget = this.target;

        TargetGenerationResult targetResult = TargetHelper.generateTarget(modWeights, lastTargets);
        if (targetResult.hasFlag()) {
            this.newTargetTimeout = worldObj.getTotalWorldTime() + 5 * 20;
            ModLogger.warning("Failed to get target, " + targetResult.getFlag());
        }
        setTarget(targetResult.getValue());

        // sync client with server as target picking only happens server-wise (for safety)
        if (oldTarget != target) {

            Kismet.network.syncDisplayTargetToClient(worldObj.provider.getDimension(), this);
            return true;
        }

        return false;
    }

    public List<ItemStack> getLastTargets() {
        return lastTargets;
    }

    public void setLastTargets(List<ItemStack> lastTargets) {
        this.lastTargets = lastTargets;
    }

    public HashMap<String, Integer> getModWeights() {
        return modWeights;
    }

    public void setModWeights(HashMap<String, Integer> modWeights) {
        this.modWeights = modWeights;
    }

    private boolean isFulfilled() {
        return worldObj.getBlockState(pos).getValue(BlockDisplay.FULFILLED);
    }

    private void setFulfilled(boolean fulfilled) {
        IBlockState state = worldObj.getBlockState(pos);
        boolean oldFulfilled = state.getValue(BlockDisplay.FULFILLED);
        // optimization trick, less state packets
        if (oldFulfilled == fulfilled) return;
        state = state.withProperty(BlockDisplay.FULFILLED, fulfilled);
        worldObj.setBlockState(pos, state);
    }

    public boolean isReady() {
        return this.target != null && this.target.getItem() != null;
    }


    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);

        setDeadline(compound.getLong("deadline"));
        setScore(compound.getInteger("score"));

        if (compound.hasKey("target")) {
            target = ItemStack.loadItemStackFromNBT(compound.getCompoundTag("target"));
        } else {
            target = null;
        }

        // 10 for COMPOUND, check NBTBase
        NBTTagList modWeightsNbt = compound.getTagList("modWeights", 10);
        for (int i = 0; i < modWeightsNbt.tagCount(); i++) {
            NBTTagCompound nbtTagCompound = modWeightsNbt.getCompoundTagAt(i);
            String modId = nbtTagCompound.getString("id");
            int modWeight = nbtTagCompound.getInteger("weight");
            this.modWeights.put(modId, modWeight);
        }

        lastTargets.clear();
        NBTTagList lastTargetsNbt = compound.getTagList("lastTargets", 10);
        for (int i = 0; i < lastTargetsNbt.tagCount(); i++) {
            NBTTagCompound nbtTagCompound = lastTargetsNbt.getCompoundTagAt(i);
            lastTargets.add(ItemStack.loadItemStackFromNBT(nbtTagCompound));
        }

//        worldObj.setBlockState(pos, ModBlocks.CHILL_DISPLAY.getActualState(worldObj.getBlockState(pos), worldObj, pos));
    }

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);

        compound.setLong("deadline", getDeadline());
        compound.setInteger("score", getScore());

        // target can be null :/
        if (getTarget() != null) {
            NBTTagCompound targetTag = new NBTTagCompound();
            getTarget().writeToNBT(targetTag);
            compound.setTag("target", targetTag);
        }

        NBTTagList modWeightsNbt = new NBTTagList();
        for (String key : modWeights.keySet()) {
            NBTTagCompound nbtTagCompound = new NBTTagCompound();
            nbtTagCompound.setString("id", key);
            nbtTagCompound.setInteger("weight", modWeights.get(key));
            modWeightsNbt.appendTag(nbtTagCompound);
        }
        compound.setTag("modWeights", modWeightsNbt);

        NBTTagList lastTargetsNbt = new NBTTagList();
        for (ItemStack lastTarget : lastTargets) {
            NBTTagCompound nbtTagCompound = new NBTTagCompound();
            lastTarget.writeToNBT(nbtTagCompound);
            lastTargetsNbt.appendTag(nbtTagCompound);
        }
        compound.setTag("lastTargets", lastTargetsNbt);

    }

    @Override
    public Packet<INetHandlerPlayClient> getDescriptionPacket() {
        NBTTagCompound nbtTagCompound = new NBTTagCompound();
        writeToNBT(nbtTagCompound);
        return new SPacketUpdateTileEntity(this.pos, getBlockMetadata(), nbtTagCompound);
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        readFromNBT(pkt.getNbtCompound());
    }

    public ItemStack getTarget() {
        return target;
    }

    public void setTarget(ItemStack target) {
        boolean oldReady = isReady();

        if (!StackHelper.isEquivalent(this.target, target)) {
            this.target = target;
        }

        // check if we need to force a block update regarding the ready
        if (oldReady != isReady())
            this.stateChanged = true;
    }
}
