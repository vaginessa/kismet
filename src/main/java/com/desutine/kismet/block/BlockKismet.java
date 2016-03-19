package com.desutine.kismet.block;

import com.desutine.kismet.reference.Reference;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

public class BlockKismet extends Block {
    public BlockKismet(Material material) {
        super(material);
    }

    public BlockKismet(){
        this(Material.rock);
    }
}
