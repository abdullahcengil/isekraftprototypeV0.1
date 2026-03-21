package com.isekraft.armor;
import java.util.Set;

import com.isekraft.IseKraftMod;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import static com.isekraft.item.ModItems.ISEKRAFT_GROUP_KEY;

/**
 * Registers all IseKraft armor items.
 * Soul Crystal Set — mid-tier, better than diamond
 * Demon King Set   — endgame, fireproof, boss-drop crafted
 */
public class ModArmors {

    // ── SOUL CRYSTAL SET ─────────────────────────────────────────────────────
    public static final ArmorItem SOUL_HELMET     = register("soul_crystal_helmet",
        new ArmorItem(ModArmorMaterials.SOUL_CRYSTAL, ArmorItem.Type.HELMET, new FabricItemSettings()));
    public static final ArmorItem SOUL_CHESTPLATE = register("soul_crystal_chestplate",
        new ArmorItem(ModArmorMaterials.SOUL_CRYSTAL, ArmorItem.Type.CHESTPLATE, new FabricItemSettings()));
    public static final ArmorItem SOUL_LEGGINGS   = register("soul_crystal_leggings",
        new ArmorItem(ModArmorMaterials.SOUL_CRYSTAL, ArmorItem.Type.LEGGINGS, new FabricItemSettings()));
    public static final ArmorItem SOUL_BOOTS      = register("soul_crystal_boots",
        new ArmorItem(ModArmorMaterials.SOUL_CRYSTAL, ArmorItem.Type.BOOTS, new FabricItemSettings()));

    // ── DEMON KING SET ────────────────────────────────────────────────────────
    public static final ArmorItem DEMON_HELMET     = register("demon_king_helmet",
        new ArmorItem(ModArmorMaterials.DEMON_KING, ArmorItem.Type.HELMET, new FabricItemSettings().fireproof()));
    public static final ArmorItem DEMON_CHESTPLATE = register("demon_king_chestplate",
        new ArmorItem(ModArmorMaterials.DEMON_KING, ArmorItem.Type.CHESTPLATE, new FabricItemSettings().fireproof()));
    public static final ArmorItem DEMON_LEGGINGS   = register("demon_king_leggings",
        new ArmorItem(ModArmorMaterials.DEMON_KING, ArmorItem.Type.LEGGINGS, new FabricItemSettings().fireproof()));
    public static final ArmorItem DEMON_BOOTS      = register("demon_king_boots",
        new ArmorItem(ModArmorMaterials.DEMON_KING, ArmorItem.Type.BOOTS, new FabricItemSettings().fireproof()));

    public static void register() {
        ItemGroupEvents.modifyEntriesEvent(ISEKRAFT_GROUP_KEY).register(content -> {
            content.add(SOUL_HELMET); content.add(SOUL_CHESTPLATE);
            content.add(SOUL_LEGGINGS); content.add(SOUL_BOOTS);
            content.add(DEMON_HELMET); content.add(DEMON_CHESTPLATE);
            content.add(DEMON_LEGGINGS); content.add(DEMON_BOOTS);
        });
        IseKraftMod.LOGGER.info("IseKraft armor registered.");
    }

    private static <T extends Item> T register(String name, T item) {
        return Registry.register(Registries.ITEM, new Identifier(IseKraftMod.MOD_ID, name), item);
    }
}
