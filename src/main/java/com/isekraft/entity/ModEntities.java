package com.isekraft.entity;

import com.isekraft.entity.OverlordGuardEntity;

import com.isekraft.IseKraftMod;
import com.isekraft.entity.BoomerangEntity;
import com.isekraft.entity.GilgameshEntity;
import com.isekraft.entity.WitchCovenEntity;
import com.isekraft.entity.LightningArrowEntity;
import com.isekraft.entity.ShurikenEntity;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.util.Identifier;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;

public class ModEntities {

    public static final EntityType<SpiritBeastEntity> SPIRIT_BEAST = reg("spirit_beast",
        FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, SpiritBeastEntity::new)
            .dimensions(EntityDimensions.fixed(1.0f, 1.2f)).build());

    public static final EntityType<GoblinKingEntity> GOBLIN_KING = reg("goblin_king",
        FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, GoblinKingEntity::new)
            .dimensions(EntityDimensions.fixed(1.2f, 2.4f)).build());

    public static final EntityType<DarkKnightEntity> DARK_KNIGHT = reg("dark_knight",
        FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, DarkKnightEntity::new)
            .dimensions(EntityDimensions.fixed(0.6f, 1.95f)).build());

    public static final EntityType<ForestWolfEntity> FOREST_WOLF = reg("forest_wolf",
        FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, ForestWolfEntity::new)
            .dimensions(EntityDimensions.fixed(0.8f, 0.85f)).build());

    public static final EntityType<IsekaiNpcEntity> ISEKAI_NPC = reg("isekai_npc",
        FabricEntityTypeBuilder.create(SpawnGroup.MISC, IsekaiNpcEntity::new)
            .dimensions(EntityDimensions.fixed(0.6f, 1.95f)).build());

    public static final EntityType<ShadowDemonEntity> SHADOW_DEMON = reg("shadow_demon",
        FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, ShadowDemonEntity::new)
            .dimensions(EntityDimensions.fixed(1.5f, 3.0f)).build());

    public static final EntityType<WitchCovenEntity> WITCH_COVEN = reg("witch_coven",
        FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, WitchCovenEntity::new)
            .dimensions(EntityDimensions.fixed(0.6f, 1.95f)).build());

    /**
     * Overlord Guard — spawned only by OverlordSealItem, never naturally.
     */
    public static final EntityType<OverlordGuardEntity> OVERLORD_GUARD = reg("overlord_guard",
        FabricEntityTypeBuilder.create(SpawnGroup.MISC, OverlordGuardEntity::new)
            .dimensions(EntityDimensions.fixed(0.6f, 1.95f)).build());

    /**
     * Gilgamesh — King of Heroes. Legendary sky island boss. Never naturally spawned.
     */
    public static final EntityType<GilgameshEntity> GILGAMESH = reg("gilgamesh",
        FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, GilgameshEntity::new)
            .dimensions(EntityDimensions.fixed(1.0f, 2.5f)).build());

    /** Lightning Arrow — projectile fired by Kirin bow. */
    public static final EntityType<LightningArrowEntity> LIGHTNING_ARROW = reg("lightning_arrow",
        FabricEntityTypeBuilder.<LightningArrowEntity>create(SpawnGroup.MISC, LightningArrowEntity::new)
            .dimensions(EntityDimensions.fixed(0.5f, 0.5f)).build());

    /** Shuriken projectile — thrown by ShurikenItem. */
    public static final EntityType<ShurikenEntity> SHURIKEN_PROJECTILE = reg("shuriken_projectile",
        FabricEntityTypeBuilder.<ShurikenEntity>create(SpawnGroup.MISC, ShurikenEntity::new)
            .dimensions(EntityDimensions.fixed(0.25f, 0.25f)).build());

    /** Boomerang projectile — thrown by BoomerangItem, returns to thrower. */
    public static final EntityType<BoomerangEntity> BOOMERANG_PROJECTILE = reg("boomerang_projectile",
        FabricEntityTypeBuilder.<BoomerangEntity>create(SpawnGroup.MISC, BoomerangEntity::new)
            .dimensions(EntityDimensions.fixed(0.3f, 0.3f)).build());

    private static <T extends EntityType<?>> T reg(String name, T type) {
        return Registry.register(Registries.ENTITY_TYPE, new Identifier(IseKraftMod.MOD_ID, name), type);
    }

    // ── SWAMP BIOME KEYS (BiomeTags.IS_SWAMP doesn't exist in 1.20.1 Yarn) ──
    private static final RegistryKey<Biome> SWAMP =
        RegistryKey.of(RegistryKeys.BIOME, new Identifier("minecraft", "swamp"));
    private static final RegistryKey<Biome> MANGROVE_SWAMP =
        RegistryKey.of(RegistryKeys.BIOME, new Identifier("minecraft", "mangrove_swamp"));

    public static void register() {
        FabricDefaultAttributeRegistry.register(SPIRIT_BEAST,  SpiritBeastEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(GOBLIN_KING,   GoblinKingEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(DARK_KNIGHT,   DarkKnightEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(FOREST_WOLF,   ForestWolfEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(ISEKAI_NPC,    IsekaiNpcEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(SHADOW_DEMON,  ShadowDemonEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(WITCH_COVEN,    WitchCovenEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(OVERLORD_GUARD, OverlordGuardEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(GILGAMESH,       GilgameshEntity.createAttributes());
        // LightningArrow and ShurikenEntity are ThrownItemEntity subclasses — no attribute registration needed.

        // Spawn restrictions
        SpawnRestriction.register(DARK_KNIGHT, SpawnRestriction.Location.ON_GROUND,
            Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark);
        SpawnRestriction.register(FOREST_WOLF, SpawnRestriction.Location.ON_GROUND,
            Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, (t, w, r, p, rng) -> true);
        SpawnRestriction.register(SPIRIT_BEAST, SpawnRestriction.Location.ON_GROUND,
            Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, (t, w, r, p, rng) -> true);
        // Witch Coven: must be dark (night only), swamp biome only
        SpawnRestriction.register(WITCH_COVEN, SpawnRestriction.Location.ON_GROUND,
            Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark);

        // Biome spawns
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_OVERWORLD),
            SpawnGroup.MONSTER, DARK_KNIGHT, 50, 1, 2);
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_FOREST),
            SpawnGroup.CREATURE, FOREST_WOLF, 25, 2, 4);
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_OVERWORLD),
            SpawnGroup.CREATURE, SPIRIT_BEAST, 8, 1, 2);

        // Witch Coven: swamp + mangrove_swamp only, weight=2 (extremely rare).
        // Only MORVAINE spawns; she auto-spawns SERAPHEL+HEXARA nearby.
        // Weight 2 vs Dark Knight 50 = 25x rarer. Roughly 1 per large swamp.
        BiomeModifications.addSpawn(
            ctx -> ctx.getBiomeKey().equals(SWAMP) || ctx.getBiomeKey().equals(MANGROVE_SWAMP),
            SpawnGroup.MONSTER, WITCH_COVEN, 2, 1, 1);

        IseKraftMod.LOGGER.info("IseKraft entities registered.");
    }
}
