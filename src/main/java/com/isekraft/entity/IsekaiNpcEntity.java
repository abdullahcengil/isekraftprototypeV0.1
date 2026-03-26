package com.isekraft.entity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.Formatting;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

import com.isekraft.network.ModPackets;
import com.isekraft.rpg.PlayerRpgManager;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.world.World;

public class IsekaiNpcEntity extends PathAwareEntity {

    private static final String[] NAMES = {
        "Aria","Kazuto","Emilia","Rimuru","Aqua",
        "Ainz","Albedo","Kirito","Asuna","Subaru",
        "Tanya","Naofumi","Raphtalia","Darkness","Megumin"
    };
    private static final long COOLDOWN = 12000L;

    private String npcName = "Aria";
    private long lastInteract = -1L;

    public IsekaiNpcEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
        npcName = NAMES[world.random.nextInt(NAMES.length)];
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return PathAwareEntity.createMobAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(1, new WanderAroundFarGoal(this, 0.6));
        goalSelector.add(2, new LookAtEntityGoal(this, PlayerEntity.class, 6.0f));
        goalSelector.add(3, new LookAroundGoal(this));
    }

    @Override public boolean isInvulnerable() { return true; }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
        if (!getWorld().isClient && player instanceof ServerPlayerEntity sp) {
            int level = PlayerRpgManager.getLevel(player);
            String title = PlayerRpgManager.getLevelTitle(level);

            // XP blessing on cooldown
            long now = getWorld().getTime();
            if (lastInteract < 0 || (now - lastInteract) > COOLDOWN) {
                int xp = 25 + (level * 5);
                PlayerRpgManager.addXp(player, xp);
                sp.sendMessage(Text.literal("[" + npcName + "]: Ah, " + title + "! I have quests for you. +" + xp + " XP for visiting.")
                    .formatted(Formatting.AQUA), false);
                lastInteract = now;
                getWorld().playSound(null, getBlockPos(),
                    SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.NEUTRAL, 1f, 1f);
            } else {
                sp.sendMessage(Text.literal("[" + npcName + "]: Check the board, " + title + ".")
                    .formatted(Formatting.AQUA), false);
            }
            // Open interactive quest board on the client
            com.isekraft.network.ModPackets.sendQuestBoardPacket(sp, npcName);
        }
        return ActionResult.SUCCESS;
    }

    private String getDialogue(int level, PlayerEntity player) {
        int kills = PlayerRpgManager.getTotalKills(player);
        if (level < 5)  return "You are new here. Seek the northern ruins when ready.";
        if (level < 15) return "Your power grows. Dark Knights patrol at night.";
        if (level < 30) return "The Goblin King trembles at your name.";
        if (level < 50) return "You've slain " + kills + " enemies. The Demon Realm stirs...";
        return "You've transcended mortal limits!";
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putString("NpcName", npcName);
        nbt.putLong("LastInteract", lastInteract);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("NpcName")) npcName = nbt.getString("NpcName");
        if (nbt.contains("LastInteract")) lastInteract = nbt.getLong("LastInteract");
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.ENTITY_VILLAGER_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.ENTITY_VILLAGER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ENTITY_VILLAGER_DEATH; }
}
