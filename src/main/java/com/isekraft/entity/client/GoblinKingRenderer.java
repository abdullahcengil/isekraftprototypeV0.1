package com.isekraft.entity.client;

import com.isekraft.IseKraftMod;
import com.isekraft.entity.GoblinKingEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.util.Identifier;

public class GoblinKingRenderer extends MobEntityRenderer<GoblinKingEntity, BipedEntityModel<GoblinKingEntity>> {

    private static final Identifier TEXTURE = new Identifier(IseKraftMod.MOD_ID, "textures/entity/goblin_king.png");

    public GoblinKingRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new BipedEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER)), 0.7f);   
    }

    @Override
    public Identifier getTexture(GoblinKingEntity entity) {
        return TEXTURE;
    }
}