package com.isekraft.entity.client;
import com.isekraft.IseKraftMod;
import com.isekraft.entity.IsekaiNpcEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Identifier;
public class IsekaiNpcRenderer extends MobEntityRenderer<IsekaiNpcEntity, PlayerEntityModel<IsekaiNpcEntity>> {
    private static final Identifier TEX = new Identifier(IseKraftMod.MOD_ID, "textures/entity/isekai_npc.png");
    public IsekaiNpcRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new PlayerEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER_SLIM), true), 0.5f);
    }
    @Override public Identifier getTexture(IsekaiNpcEntity e) { return TEX; }
}
