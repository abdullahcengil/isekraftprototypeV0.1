package com.isekraft.block;

import com.isekraft.IseKraftMod;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import static com.isekraft.item.ModItems.ISEKRAFT_GROUP_KEY;

public class ModBlocks {

    public static final ArcaneTowerBlock ARCANE_TOWER = registerBlock("arcane_tower",
        new ArcaneTowerBlock(FabricBlockSettings.copyOf(Blocks.STONE_BRICKS)
            .hardness(4f).resistance(6f).requiresTool()));

    public static final Block RIFT_CRYSTAL_BLOCK = registerBlock("rift_crystal_block",
        new Block(FabricBlockSettings.copyOf(Blocks.GLASS)
            .hardness(2f).luminance(7).nonOpaque()));

    public static final Block SOUL_ALTAR = registerBlock("soul_altar",
        new Block(FabricBlockSettings.copyOf(Blocks.STONE_BRICKS)
            .hardness(5f).resistance(10f).requiresTool()));

    public static void register() {
        ItemGroupEvents.modifyEntriesEvent(ISEKRAFT_GROUP_KEY).register(content -> {
            content.add(ARCANE_TOWER.asItem());
            content.add(RIFT_CRYSTAL_BLOCK.asItem());
            content.add(SOUL_ALTAR.asItem());
        });
        IseKraftMod.LOGGER.info("IseKraft blocks registered.");
    }

    private static <T extends Block> T registerBlock(String name, T block) {
        registerBlockItem(name, block);
        return Registry.register(Registries.BLOCK, new Identifier(IseKraftMod.MOD_ID, name), block);
    }

    private static void registerBlockItem(String name, Block block) {
        Registry.register(Registries.ITEM, new Identifier(IseKraftMod.MOD_ID, name),
            new BlockItem(block, new FabricItemSettings()));
    }
}
