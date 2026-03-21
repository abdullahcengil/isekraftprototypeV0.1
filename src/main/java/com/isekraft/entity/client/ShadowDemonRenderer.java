package com.isekraft.entity.client;

import com.isekraft.IseKraftMod;
import com.isekraft.entity.ShadowDemonEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.util.Identifier;

public class ShadowDemonRenderer extends MobEntityRenderer<ShadowDemonEntity, BipedEntityModel<ShadowDemonEntity>> {
    // Uses Goblin King texture (dark warlord look)
    private static final Identifier TEX = new Identifier(IseKraftMod.MOD_ID, "textures/entity/goblin_king.png");

    public ShadowDemonRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new BipedEntityModel<>(ctx.getPart(EntityModelLayers.ZOMBIE)), 1.5f);
    }

    @Override public Identifier getTexture(ShadowDemonEntity e) { return TEX; }
}
