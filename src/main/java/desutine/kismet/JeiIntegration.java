package desutine.kismet;

import mezz.jei.api.*;
import mezz.jei.gui.ItemListOverlay;

import javax.annotation.Nonnull;

@JEIPlugin
public class JeiIntegration implements IModPlugin {
    public static IItemListOverlay itemListOverlay;

    @Override
    public void register(@Nonnull IModRegistry registry) {

    }

    @Override
    public void onRuntimeAvailable(@Nonnull IJeiRuntime jeiRuntime) {
        itemListOverlay = jeiRuntime.getItemListOverlay();
    }
}
