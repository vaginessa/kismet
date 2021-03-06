package dustine.kismet.tile;

import dustine.kismet.target.Target;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

/**
 * A IItemHandler with one slot that is actually only a view for the current target
 * <p>
 * "Inserting" does nothing, retrieving always returns null
 */
public final class TileDisplaySlotHandler implements IItemHandler {
    private TileDisplay parent;

    public TileDisplaySlotHandler(TileDisplay parent) {
        this.parent = parent;
    }

    /**
     * Number of slot available (one)
     *
     * @return the number of slots
     */
    @Override
    public int getSlots() {
        return 1;
    }

    /**
     * Returns the current target itemstack
     *
     * @param slot the targeted slot
     * @return the parent display's target
     */
    @Override
    public ItemStack getStackInSlot(int slot) {
        validateSlotIndex(slot);
        final Target target = this.parent.getTarget();
        if (target == null) return null;

        final ItemStack itemStack = ItemStack.copyItemStack(target.getStack());
//        itemStack.stackSize = 0;
        return itemStack;
    }

    private void validateSlotIndex(int slot) {
        if (slot < 0 || slot >= 2)
            throw new RuntimeException("Slot " + slot + " not in valid range - [0, 1)");
    }

    /**
     * Tries to insert, and fails, a stack. Because there's nothing stored *ghost noises*
     *
     * @param slot     the targeted slot
     * @param stack    the item to insert (validate against target)
     * @param simulate runs a simulacrum (no action taken)
     * @return the inserted stack (it always bounces off)
     */
    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        validateSlotIndex(slot);
        return stack;
    }

    /**
     * Tries to extract, and fails, the current target. Because there's nothing stored *ghost noises*
     *
     * @param slot     the targeted slot
     * @param amount   the amount to fetch from the slot
     * @param simulate runs a simulacrum (no action taken)
     * @return null
     */
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        validateSlotIndex(slot);
        return null;
    }
}
