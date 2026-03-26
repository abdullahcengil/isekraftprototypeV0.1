package com.isekraft.entity.client;

import com.isekraft.IseKraftMod;
import com.isekraft.entity.DarkKnightEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.util.Identifier;

public class DarkKnightRenderer extends MobEntityRenderer<DarkKnightEntity, BipedEntityModel<DarkKnightEntity>> {

    private static final Identifier TEXTURE = new Identifier(IseKraftMod.MOD_ID, "textures/entity/dark_knight.png");

    public DarkKnightRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new BipedEntityModel<>(ctx.getPart(EntityModelLayers.ZOMBIE)), 0.5f);
    }

    @Override
    public Identifier getTexture(DarkKnightEntity entity) {
        return TEXTURE;
    }
}