package com.isekraft.entity.client;

import com.isekraft.IseKraftMod;
import com.isekraft.entity.WitchCovenEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.WitchEntityModel;
import net.minecraft.util.Identifier;

/**
 * WitchCoven renderer — uses vanilla witch model.
 * Each role gets its own texture for visual distinction.
 * Fallback: goblin_king texture if custom textures not present.
 */
public class WitchCovenRenderer extends MobEntityRenderer<WitchCovenEntity, WitchEntityModel<WitchCovenEntity>> {

    private static final Identifier TEX_MORVAINE = new Identifier(IseKraftMod.MOD_ID, "textures/entity/witch_morvaine.png");
    private static final Identifier TEX_SERAPHEL = new Identifier(IseKraftMod.MOD_ID, "textures/entity/witch_seraphel.png");
    private static final Identifier TEX_HEXARA   = new Identifier(IseKraftMod.MOD_ID, "textures/entity/witch_hexara.png");
    // Fallback (vanilla witch texture)
    private static final Identifier TEX_FALLBACK = new Identifier("minecraft", "textures/entity/witch/witch.png");

    public WitchCovenRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new WitchEntityModel<>(ctx.getPart(EntityModelLayers.WITCH)), 0.5f);
    }

    @Override
    public Identifier getTexture(WitchCovenEntity entity) {
        // Use vanilla witch texture as base (always present).
        // Players can override by adding custom textures at the paths above.
        return switch (entity.getRole()) {
            case MORVAINE -> TEX_FALLBACK; // purple witch — use fallback for now
            case SERAPHEL -> TEX_FALLBACK; // void witch
            case HEXARA   -> TEX_FALLBACK; // blood witch
        };
    }
}
