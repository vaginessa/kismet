package desutine.kismet.common.config;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import desutine.kismet.ModLogger;
import desutine.kismet.common.config.tests.OriginTest;
import desutine.kismet.common.config.tests.ProcessTest;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

public class InformedItemStack {
    public ItemStack item;
    // fixme swapped for debug reasons
    public boolean crafted = true;
    public boolean generated = false;
    public boolean looted = false;
    public boolean silkTouched = false;
    public boolean harvested = false;
    public boolean cooked = false;
    public boolean brewed = false;
    public Set<OriginTest> originTests = new HashSet<>();
    public Set<ProcessTest> processTests = new HashSet<>();

    public String getMod() {
        // gets the mod name from before the :
        return new ResourceLocation(item.getItem().getRegistryName()).getResourceDomain();
    }

    public void addOriginTest(OriginTest test){
        originTests.add(test);
    }

    public void addProcessTest(ProcessTest test){
        processTests.add(test);
    }


    public interface ITest {
        String getName();
        boolean hasPassed();
        void test(ItemStack test);
    }

//        ModBlocks.kismetDisplayBlock.getHarvestLevel();
//        ModBlocks.kismetDisplayBlock.getBlockState().getValidStates()

    public boolean isObtainable() {
        return originTests.stream().anyMatch(input -> input != null && input.hasPassed()) || processTests.stream().anyMatch(input -> input != null && input.hasPassed());

//            BiomeGenBase.biomeRegistry.getObject()
    }

    public InformedItemStack(ItemStack item) {
        this.item = item;
    }
}
