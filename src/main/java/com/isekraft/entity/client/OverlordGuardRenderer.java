package com.isekraft.entity.client;

import com.isekraft.IseKraftMod;
import com.isekraft.entity.OverlordGuardEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.util.Identifier;

/**
 * Overlord Guard renderer — uses Dark Knight texture (dark armored warrior look).
 * Can be customized with a dedicated texture later.
 */
public class OverlordGuardRenderer extends MobEntityRenderer<OverlordGuardEntity, BipedEntityModel<OverlordGuardEntity>> {

    private static final Identifier TEX =
        new Identifier(IseKraftMod.MOD_ID, "textures/entity/dark_knight.png");

    public OverlordGuardRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new BipedEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public Identifier getTexture(OverlordGuardEntity e) { return TEX; }
}
