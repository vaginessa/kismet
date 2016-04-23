package desutine.kismet.network;

import desutine.kismet.Kismet;
import desutine.kismet.ModLogger;
import desutine.kismet.Reference;
import desutine.kismet.addon.AddonJei;
import desutine.kismet.client.util.ClientTargetHelper;
import desutine.kismet.network.packet.*;
import desutine.kismet.server.WSDTargetDatabase;
import desutine.kismet.target.InformedStack;
import desutine.kismet.tile.TileDisplay;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

import java.util.List;

public class NetworkHandlerKismet {
    private static int discriminator;
    private final SimpleNetworkWrapper channel;

    public NetworkHandlerKismet() {
        // registering channel
        channel = NetworkRegistry.INSTANCE.newSimpleChannel(Reference.MOD_ID);
        // registering messages
        channel.registerMessage(SyncClientTileDisplay.class, SCTDMessage.class, getDiscriminator(), Side.CLIENT);
        channel.registerMessage(GenerateSkippedTarget.class, GSTMessage.class, getDiscriminator(), Side.SERVER);

        channel.registerMessage(SendConfigToClient.class, SCTCMessage.class, getDiscriminator(), Side.CLIENT);

        channel.registerMessage(EnrichStackList.class, ESLMessage.class, getDiscriminator(), Side.CLIENT);
        channel.registerMessage(ReceiveEnrichedStacks.class, RESMessage.class, getDiscriminator(), Side.SERVER);
        channel.registerMessage(FinishedEnrichingStacks.class, FESMessage.class, getDiscriminator(), Side.SERVER);
    }

    private static int getDiscriminator() {
        return discriminator++;
    }

    public void syncDisplayTargetToClient(int dimension, TileEntity tileEntity) {
        channel.sendToDimension(new SCTDMessage(tileEntity), dimension);
    }

    public void sendConfigToClient(EntityPlayerMP player) {
        channel.sendTo(new SCTCMessage(), player);
    }

    public void enrichStacks(List<InformedStack> targets, EntityPlayerMP player) {
        channel.sendTo(new ESLMessage(targets), player);
    }

    public void newSkippedTarget(TileDisplay te) {
        channel.sendToServer(new GSTMessage(te.getPos(), te.getSkipped()));
    }

    @SuppressWarnings("WeakerAccess")
    public static class SyncClientTileDisplay implements IMessageHandler<SCTDMessage, IMessage> {
        @Override
        public IMessage onMessage(SCTDMessage message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                int x, y, z;
                x = message.nbtTagCompound.getInteger("x");
                y = message.nbtTagCompound.getInteger("y");
                z = message.nbtTagCompound.getInteger("z");
                BlockPos pos = new BlockPos(x, y, z);

                TileEntity tileEntity = Minecraft.getMinecraft().theWorld.getTileEntity(pos);
                if (tileEntity == null) return;
                if (tileEntity instanceof TileDisplay) {
                    TileDisplay tileDisplay = (TileDisplay) tileEntity;
                    try {
                        tileDisplay.readFromNBT(message.nbtTagCompound);
                    } catch (Throwable te) {
                        ModLogger.error(te);
                    }
                }
            });
            return null;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class SendConfigToClient implements IMessageHandler<SCTCMessage, IMessage> {
        @Override
        public IMessage onMessage(SCTCMessage message, MessageContext ctx) {
//            Minecraft.getMinecraft().addScheduledTask(() -> ConfigKismet.clientSync(message.syncValues));
            return null;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class EnrichStackList implements IMessageHandler<ESLMessage, IMessage> {
        @Override
        public ESLMessage onMessage(ESLMessage message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                for (InformedStack stack : message.stacks) {
                    List<InformedStack> targets;
                    if (Kismet.instance.isJeiLoaded()) {
                        targets = AddonJei.enrich(stack);
                    } else {
                        targets = ClientTargetHelper.vanillaEnrich(stack);
                    }
                    targets.forEach(InformedStack::seal);
                    Kismet.network.channel.sendToServer(new RESMessage(targets));
                }
                Kismet.network.channel.sendToServer(new FESMessage());
            });
            return null;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class ReceiveEnrichedStacks implements IMessageHandler<RESMessage, IMessage> {
        @Override
        public IMessage onMessage(final RESMessage message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            final WorldServer world = (WorldServer) (player.worldObj);
            world.addScheduledTask(() -> {
                if (Kismet.libraryFactory != null) {
                    final WSDTargetDatabase wsdStacks = WSDTargetDatabase.get(world);
                    wsdStacks.enrichStacks(message.stacks);
                }
            });
            return null;
        }

    }

    @SuppressWarnings("WeakerAccess")
    public static class FinishedEnrichingStacks implements IMessageHandler<FESMessage, IMessage> {
        @Override
        public IMessage onMessage(FESMessage message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            final WorldServer world = (WorldServer) (player.worldObj);
            world.addScheduledTask(() -> {
                if (Kismet.libraryFactory != null) {
                    final boolean sent = Kismet.libraryFactory.sendNextPacket(player);
                    if (!sent) {
                        Kismet.libraryFactory.recreateLibrary();
                        player.addChatMessage(
                                new TextComponentString("[Kismet] Finished resetting library!"));
                    }
                }
            });
            return null;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class GenerateSkippedTarget implements IMessageHandler<GSTMessage, IMessage> {
        @Override
        public IMessage onMessage(GSTMessage message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            final WorldServer world = (WorldServer) (player.worldObj);
            world.addScheduledTask(() -> {
                final TileDisplay tileDisplay = (TileDisplay) world.getTileEntity(message.pos);
                tileDisplay.setSkipped(message.skipped);
                tileDisplay.getNewTarget();
            });
            return null;
        }
    }
}

