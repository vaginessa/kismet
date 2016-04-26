package dustine.kismet.util;

import dustine.kismet.Kismet;
import dustine.kismet.registry.ModItems;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;

public class SoundHelper {
    public static void onKeyUsage(EntityPlayer player, boolean success) {
        if (success) {
            correctClick(player);
        } else {
            player.renderBrokenItemStack(new ItemStack(ModItems.KEY));
        }
    }

    private static void correctClick(EntityPlayer player) {
        // reusing sounds because it works.
        player.playSound(SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, 0.8F, 0.8F + Kismet.random.nextFloat() * 0.4F);
    }

    public static void onTargetFulfilled(EntityPlayer player) {
        correctClick(player);
    }
}
