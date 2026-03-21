package com.isekraft.block.entity;
import net.minecraft.entity.EntityType;

import com.isekraft.IseKraftMod;
import com.isekraft.block.ModBlocks;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/** Registers all IseKraft block entity types. */
public class ModBlockEntities {

    public static BlockEntityType<ArcaneTowerBlockEntity> ARCANE_TOWER_ENTITY;

    public static void register() {
        ARCANE_TOWER_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier(IseKraftMod.MOD_ID, "arcane_tower"),
            FabricBlockEntityTypeBuilder
                .create(ArcaneTowerBlockEntity::new, ModBlocks.ARCANE_TOWER)
                .build(null)
        );
        IseKraftMod.LOGGER.info("IseKraft block entities registered.");
    }
}
