package com.isekraft.block;
import net.minecraft.entity.EntityType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

import com.isekraft.block.entity.ArcaneTowerBlockEntity;
import com.isekraft.block.entity.ModBlockEntities;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Arcane Tower Block.
 *
 * When placed:
 *   - Automatically detects hostile mobs within 16 blocks
 *   - Fires a magic arrow at the nearest hostile mob every 40 ticks (2 sec)
 *   - Player can right-click to see tower status
 *
 * The logic lives in ArcaneTowerBlockEntity (the block entity).
 */
public class ArcaneTowerBlock extends BlockWithEntity {

    public ArcaneTowerBlock(Settings settings) {
        super(settings);
    }

    // ── BLOCK ENTITY ──────────────────────────────────────────────────────────

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ArcaneTowerBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        World world, BlockState state, BlockEntityType<T> type) {
        // Attach the server-side tick method
        return world.isClient ? null :
            checkType(type, ModBlockEntities.ARCANE_TOWER_ENTITY, ArcaneTowerBlockEntity::tick);
    }

    // ── INTERACTION ───────────────────────────────────────────────────────────

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                              PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient) {
            if (world.getBlockEntity(pos) instanceof ArcaneTowerBlockEntity tower) {
                player.sendMessage(Text.literal(
                    "⚔ Arcane Tower — Targets in range: " + tower.getTargetCount() +
                    " | Shots fired: " + tower.getTotalShots()), false);
            }
        }
        return ActionResult.SUCCESS;
    }
}
