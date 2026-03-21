package com.isekraft.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.List;

/** A material item with a descriptive tooltip explaining its use. */
public class IseKraftMaterialItem extends Item {

    private final String description;
    private final String use;
    private final Formatting color;

    public IseKraftMaterialItem(Settings settings, String description, String use, Formatting color) {
        super(settings);
        this.description = description;
        this.use = use;
        this.color = color;
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext ctx) {
        tooltip.add(Text.literal(description).formatted(Formatting.GRAY));
        tooltip.add(Text.literal("✦ Use: " + use).formatted(color));
    }
}
