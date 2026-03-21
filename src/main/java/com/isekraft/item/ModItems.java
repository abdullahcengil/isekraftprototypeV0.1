package com.isekraft.item;
import net.minecraft.item.ItemStack;

import com.isekraft.IseKraftMod;
import com.isekraft.item.KirinBowItem;
import com.isekraft.item.ShurikenItem;
import com.isekraft.item.BoomerangItem;
import com.isekraft.item.WarHammerItem;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class ModItems {

    // ── WEAPONS ──────────────────────────────────────────────────────────────
    public static final RuneSwordItem      RUNE_SWORD      = register("rune_sword",
        new RuneSwordItem(ToolMaterials.NETHERITE, 5, -2.4f, new FabricItemSettings().maxDamage(2500)));
    public static final BerserkerSwordItem BERSERKER_SWORD = register("berserker_sword",
        new BerserkerSwordItem(new FabricItemSettings().maxDamage(1800)));
    public static final MagicStaffItem     ARCANE_STAFF    = register("arcane_staff",
        new MagicStaffItem(new FabricItemSettings().maxDamage(500)));
    public static final SoulBowItem        SOUL_BOW        = register("soul_bow",
        new SoulBowItem(new FabricItemSettings().maxDamage(800)));
    public static final GodSwordItem       GOD_SWORD       = register("god_sword",
        new GodSwordItem(new FabricItemSettings().maxCount(1)));

    // ── NEW WEAPONS ──────────────────────────────────────────────────────────
    public static final WarHammerItem WAR_HAMMER_WOOD    = register("war_hammer_wood",
        new WarHammerItem(ToolMaterials.WOOD,    2, new FabricItemSettings().maxDamage(60)));
    public static final WarHammerItem WAR_HAMMER_STONE   = register("war_hammer_stone",
        new WarHammerItem(ToolMaterials.STONE,   3, new FabricItemSettings().maxDamage(132)));
    public static final WarHammerItem WAR_HAMMER_IRON    = register("war_hammer_iron",
        new WarHammerItem(ToolMaterials.IRON,    4, new FabricItemSettings().maxDamage(251)));
    public static final WarHammerItem WAR_HAMMER_GOLD    = register("war_hammer_gold",
        new WarHammerItem(ToolMaterials.GOLD,    3, new FabricItemSettings().maxDamage(33)));
    public static final WarHammerItem WAR_HAMMER_DIAMOND = register("war_hammer_diamond",
        new WarHammerItem(ToolMaterials.DIAMOND, 5, new FabricItemSettings().maxDamage(1562)));
    public static final KirinBowItem  KIRIN              = register("kirin",
        new KirinBowItem(new FabricItemSettings().maxCount(1)));
    public static final Item          STEEL_INGOT        = register("steel_ingot",
        new IseKraftMaterialItem(new FabricItemSettings().maxCount(64),
            "Hardened steel ingot.", "Craft Shurikens", Formatting.WHITE));
    public static final ShurikenItem  SHURIKEN           = register("shuriken",
        new ShurikenItem(new FabricItemSettings().maxCount(16)));
    public static final BoomerangItem BOOMERANG          = register("boomerang",
        new BoomerangItem(new FabricItemSettings().maxCount(1)));

    // ── ARMOR / BOSS REWARDS ─────────────────────────────────────────────────
    public static final DemonLordCrownItem DEMON_LORD_CROWN = register("demon_lord_crown",
        new DemonLordCrownItem(new FabricItemSettings().fireproof()));

    public static final OverlordSealItem OVERLORD_SEAL = register("overlord_seal",
        new OverlordSealItem(new FabricItemSettings().maxCount(1).fireproof()));

    // ── UTILITY ───────────────────────────────────────────────────────────────
    public static final TeleportStoneItem  TELEPORT_STONE  = register("teleport_stone",
        new TeleportStoneItem(new FabricItemSettings().maxCount(1)));
    public static final RecipeGuideItem    RECIPE_GUIDE    = register("recipe_guide",
        new RecipeGuideItem(new FabricItemSettings().maxCount(1)));
    public static final HealthPotionItem   HEALTH_POTION   = register("health_potion",
        new HealthPotionItem(new FabricItemSettings().maxCount(16)));
    public static final MountCommandItem   MOUNT_WHISTLE   = register("mount_whistle",
        new MountCommandItem(new FabricItemSettings().maxCount(1)));

    // ── MATERIALS ─────────────────────────────────────────────────────────────
    public static final Item SOUL_CRYSTAL = register("soul_crystal",
        new IseKraftMaterialItem(new FabricItemSettings().maxCount(64),
            "A crystal pulsing with soul energy.",
            "Craft Soul Crystal armor + Arcane Tower", Formatting.AQUA));
    public static final Item SPIRIT_ESSENCE = register("spirit_essence",
        new IseKraftMaterialItem(new FabricItemSettings().maxCount(16),
            "A wisp of spirit energy.",
            "Right-click Spirit Beast to tame it (33% chance)", Formatting.LIGHT_PURPLE));
    public static final Item DEMON_CORE = register("demon_core",
        new IseKraftMaterialItem(new FabricItemSettings().maxCount(16).fireproof(),
            "The hardened core of a demon.",
            "Craft Demon King armor (endgame)", Formatting.DARK_RED));
    public static final Item RUNE_FRAGMENT = register("rune_fragment",
        new IseKraftMaterialItem(new FabricItemSettings().maxCount(64),
            "A shard of runic power. Dropped by mobs.",
            "Craft Rune Sword + Arcane Staff + Soul Bow", Formatting.YELLOW));
    public static final Item IRON_RUNE_SHARD = register("iron_rune_shard",
        new IseKraftMaterialItem(new FabricItemSettings().maxCount(64),
            "Iron infused with faint rune energy.",
            "Basic crafting — combine 4 for 1 Rune Fragment", Formatting.GRAY));
    public static final Item MANA_CRYSTAL = register("mana_crystal",
        new IseKraftMaterialItem(new FabricItemSettings().maxCount(64),
            "A crystal resonating with arcane mana.",
            "Craft Arcane Staff upgrades + Health Potion", Formatting.BLUE));
    public static final Item ANCIENT_COIN = register("ancient_coin",
        new IseKraftMaterialItem(new FabricItemSettings().maxCount(64),
            "Currency of the old world. Dropped by NPCs.",
            "Trade with Isekai NPCs for rare items", Formatting.GOLD));
    public static final Item DEMON_DUST = register("demon_dust",
        new IseKraftMaterialItem(new FabricItemSettings().maxCount(64).fireproof(),
            "Crushed Demon Core dust.",
            "Craft Demon King armor polish + potions", Formatting.DARK_RED));

    // ── CREATIVE TAB ──────────────────────────────────────────────────────────
    public static final RegistryKey<ItemGroup> ISEKRAFT_GROUP_KEY =
        RegistryKey.of(Registries.ITEM_GROUP.getKey(), new Identifier(IseKraftMod.MOD_ID, "isekraft_tab"));

    public static void register() {
        Registry.register(Registries.ITEM_GROUP, ISEKRAFT_GROUP_KEY,
            FabricItemGroup.builder()
                .displayName(Text.literal("✦ IseKraft RPG"))
                .icon(() -> new ItemStack(RUNE_SWORD))
                .build());

        ItemGroupEvents.modifyEntriesEvent(ISEKRAFT_GROUP_KEY).register(content -> {
            // Weapons
            content.add(RUNE_SWORD); content.add(BERSERKER_SWORD);
            content.add(ARCANE_STAFF); content.add(SOUL_BOW);
            content.add(WAR_HAMMER_WOOD); content.add(WAR_HAMMER_STONE); content.add(WAR_HAMMER_IRON);
            content.add(WAR_HAMMER_GOLD); content.add(WAR_HAMMER_DIAMOND);
            content.add(KIRIN); content.add(STEEL_INGOT); content.add(SHURIKEN); content.add(BOOMERANG);
            // Utility
            content.add(HEALTH_POTION); content.add(MOUNT_WHISTLE);
            content.add(TELEPORT_STONE); content.add(RECIPE_GUIDE);
            // Boss / Endgame
            content.add(DEMON_LORD_CROWN); content.add(OVERLORD_SEAL); content.add(GOD_SWORD);
            // Materials
            content.add(IRON_RUNE_SHARD); content.add(RUNE_FRAGMENT);
            content.add(SOUL_CRYSTAL); content.add(SPIRIT_ESSENCE);
            content.add(MANA_CRYSTAL); content.add(DEMON_CORE);
            content.add(ANCIENT_COIN); content.add(DEMON_DUST);
        });
        IseKraftMod.LOGGER.info("IseKraft items registered.");
    }

    private static <T extends Item> T register(String name, T item) {
        return Registry.register(Registries.ITEM, new Identifier(IseKraftMod.MOD_ID, name), item);
    }
}
