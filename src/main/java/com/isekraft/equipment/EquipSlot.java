package com.isekraft.equipment;

/**
 * The three RPG accessory slots added by IseKraft.
 * These are separate from vanilla armor slots.
 *
 * Each slot stores its equipped ItemStack as a sub-compound in the player's
 * IseKraftRPG NBT under the key defined here.
 */
public enum EquipSlot {

    GLOVE   ("EquipGlove",    "✦ Glove",    "glove"),
    NECKLACE("EquipNecklace", "✦ Necklace", "necklace"),
    RING    ("EquipRing",     "✦ Ring",     "ring");

    /** NBT sub-key inside the IseKraftRPG compound. */
    public final String nbtKey;
    /** Display label shown in the Character Screen. */
    public final String label;
    /** Used to identify which items belong to this slot via item's registered name. */
    public final String typeTag;

    EquipSlot(String nbtKey, String label, String typeTag) {
        this.nbtKey  = nbtKey;
        this.label   = label;
        this.typeTag = typeTag;
    }
}
