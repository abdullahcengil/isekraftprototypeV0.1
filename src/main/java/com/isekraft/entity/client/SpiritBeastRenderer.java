package com.isekraft.entity.client;
import com.isekraft.IseKraftMod;
import com.isekraft.entity.SpiritBeastEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.OcelotEntityModel;
import net.minecraft.util.Identifier;
public class SpiritBeastRenderer extends MobEntityRenderer<SpiritBeastEntity, OcelotEntityModel<SpiritBeastEntity>> {
    private static final Identifier TEX = new Identifier(IseKraftMod.MOD_ID, "textures/entity/spirit_beast.png");
    public SpiritBeastRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new OcelotEntityModel<>(ctx.getPart(EntityModelLayers.OCELOT)), 0.4f);
    }
    @Override public Identifier getTexture(SpiritBeastEntity e) { return TEX; }
}
