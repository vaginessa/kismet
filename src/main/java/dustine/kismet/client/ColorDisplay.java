package dustine.kismet.client;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.client.renderer.color.IItemColor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public class ColorDisplay implements IBlockColor, IItemColor {
    public ColorDisplay() {
    }

    @Override
    public int colorMultiplier(IBlockState state, IBlockAccess blockAccess, BlockPos pos, int tintIndex) {
//        switch (tintIndex) {
//            case 0:
//                if (state.getBlock() instanceof BlockChillDisplay) {
//                    return 0x00ff00;
//                }
//        }

        return -1;
    }

    @Override
    public int getColorFromItemstack(ItemStack stack, int tintIndex) {
//        switch (tintIndex) {
//            case 0:
//                if (Block.getBlockFromItem(stack.getItem()) instanceof BlockChillDisplay) {
//                    return 0x00ff00;
//                }
//        }

        return -1;
    }
}
