package com.isekraft.armor;

import com.isekraft.IseKraftMod;
import com.isekraft.item.ModItems;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

import java.util.function.Supplier;

/**
 * Custom armor materials defining durability, protection, and enchantability.
 * Soul Crystal: mid-tier (diamond+), teal aesthetic
 * Demon King:   endgame (netherite+), dark red aesthetic, knockback resistant
 */
public enum ModArmorMaterials implements ArmorMaterial {

    SOUL_CRYSTAL("soul_crystal",
        new int[]{3, 7, 9, 4}, 18,
        SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND,
        () -> Ingredient.ofItems(ModItems.SOUL_CRYSTAL),
        2.0f, 0.0f),

    DEMON_KING("demon_king",
        new int[]{4, 9, 12, 5}, 15,
        SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE,
        () -> Ingredient.ofItems(ModItems.DEMON_CORE),
        4.0f, 0.2f);

    private static final int[] BASE_DUR = {13, 15, 16, 11};

    private final String name;
    private final int[] protection;
    private final int enchantability;
    private final SoundEvent equipSound;
    private final Supplier<Ingredient> repair;
    private final float toughness;
    private final float knockbackResist;

    ModArmorMaterials(String name, int[] protection, int enchantability,
                      SoundEvent equipSound, Supplier<Ingredient> repair,
                      float toughness, float knockbackResist) {
        this.name = name;
        this.protection = protection;
        this.enchantability = enchantability;
        this.equipSound = equipSound;
        this.repair = repair;
        this.toughness = toughness;
        this.knockbackResist = knockbackResist;
    }

    @Override public int getDurability(ArmorItem.Type t)  { return BASE_DUR[t.getEquipmentSlot().getEntitySlotId()] * 25; }
    @Override public int getProtection(ArmorItem.Type t)  { return protection[t.getEquipmentSlot().getEntitySlotId()]; }
    @Override public int getEnchantability()              { return enchantability; }
    @Override public SoundEvent getEquipSound()           { return equipSound; }
    @Override public Ingredient getRepairIngredient()     { return repair.get(); }
    @Override public String getName()                     { return IseKraftMod.MOD_ID + ":" + name; }
    @Override public float getToughness()                 { return toughness; }
    @Override public float getKnockbackResistance()       { return knockbackResist; }
}
