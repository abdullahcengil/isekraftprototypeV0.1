package com.isekraft.entity;

import com.isekraft.item.ModItems;
import com.isekraft.rpg.PlayerRpgManager;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;

/**
 * Witch Coven — legendary 3-member boss group.
 *
 * MORVAINE the Cursed     — melee curse (Slowness+Weakness), curses attacker
 * SERAPHEL the Voidweaver — ranged void bolts (fireballs)
 * HEXARA the Bloodmage    — Wither+Poison, summons enchanted skeleton soldiers
 *
 * Spawn: swamp biomes only, weight=2 (Dark Knight=50 → ~25x rarer).
 * Only MORVAINE spawns naturally; she spawns SERAPHEL+HEXARA 7 blocks away.
 *
 * Achievement: kill all 3 in any order → "Witch Hunter" unlocks per-player,
 * broadcasts server-wide, gives Mana/Soul Crystals + Luck I.
 */
public class WitchCovenEntity extends HostileEntity {

    public enum WitchRole { MORVAINE, SERAPHEL, HEXARA }

    private WitchRole role = WitchRole.MORVAINE;
    private final ServerBossBar bossBar;
    private int abilityTimer = 80;
    private int summonTimer  = 300;
    private boolean covenSpawned = false;

    public WitchCovenEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        bossBar = new ServerBossBar(
            Text.literal("???").formatted(Formatting.DARK_PURPLE, Formatting.BOLD),
            BossBar.Color.PURPLE, BossBar.Style.NOTCHED_6);
        setPersistent();
    }

    // ── ROLE ASSIGNMENT ───────────────────────────────────────────────────────

    /** Must be called before spawning to assign role and update display. */
    public void setRole(WitchRole r) {
        this.role = r;
        refreshDisplay();
    }

    public WitchRole getRole() { return role; }

    private void refreshDisplay() {
        Text name = switch (role) {
            case MORVAINE -> Text.literal("✦ Morvaine the Cursed ✦")
                .formatted(Formatting.DARK_PURPLE, Formatting.BOLD);
            case SERAPHEL -> Text.literal("✦ Seraphel the Voidweaver ✦")
                .formatted(Formatting.DARK_PURPLE, Formatting.BOLD);
            case HEXARA   -> Text.literal("✦ Hexara the Bloodmage ✦")
                .formatted(Formatting.DARK_RED, Formatting.BOLD);
        };
        setCustomName(name);
        setCustomNameVisible(true);
        bossBar.setName(name);
        bossBar.setColor(role == WitchRole.HEXARA ? BossBar.Color.RED : BossBar.Color.PURPLE);
    }

    // ── ATTRIBUTES ────────────────────────────────────────────────────────────

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH, 200.0)
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 14.0)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.26)
            .add(EntityAttributes.GENERIC_ARMOR, 12.0)
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0)
            .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.5);
    }

    // ── GOALS ─────────────────────────────────────────────────────────────────

    @Override
    protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(1, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.add(2, new WanderAroundFarGoal(this, 0.7));
        goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 16f));
        targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
        targetSelector.add(2, new RevengeGoal(this));
    }

    // ── INITIALIZATION (coven auto-spawn) ────────────────────────────────────

    /**
     * Called when entity spawns. MORVAINE spawns SERAPHEL + HEXARA nearby.
     * BUG FIX: role is always set before initialize() via setRole(), so the
     * check is safe. covenSpawned flag prevents double-spawn on NBT reload.
     */
    @Override
    public EntityData initialize(ServerWorldAccess worldAccess, LocalDifficulty diff,
                                 SpawnReason reason, EntityData data, NbtCompound nbt) {
        EntityData result = super.initialize(worldAccess, diff, reason, data, nbt);

        if (role == WitchRole.MORVAINE && !covenSpawned
                && worldAccess instanceof ServerWorld sw) {
            covenSpawned = true;
            spawnSister(sw, WitchRole.SERAPHEL,  7, 0);
            spawnSister(sw, WitchRole.HEXARA,   -7, 0);
        }
        return result;
    }

    private void spawnSister(ServerWorld sw, WitchRole sisterRole, int dx, int dz) {
        WitchCovenEntity sister = ModEntities.WITCH_COVEN.create(sw);
        if (sister == null) return;
        sister.setRole(sisterRole);
        sister.covenSpawned = true;           // prevent her from triggering again
        sister.setPos(getX() + dx, getY(), getZ() + dz);
        sister.initialize(sw, sw.getLocalDifficulty(getBlockPos()),
            SpawnReason.MOB_SUMMONED, null, null);
        sw.spawnEntity(sister);
        sw.spawnParticles(ParticleTypes.WITCH,
            sister.getX(), sister.getY() + 1, sister.getZ(), 20, 0.5, 0.5, 0.5, 0.05);
    }

    // ── TICK ─────────────────────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient) return;

        bossBar.setPercent(Math.max(0f, getHealth() / getMaxHealth()));
        ServerWorld sw = (ServerWorld) getWorld();

        // Passive particle aura — visually distinct per role
        if (getWorld().getTime() % 8 == 0) {
            switch (role) {
                case MORVAINE -> sw.spawnParticles(ParticleTypes.WITCH,
                    getX(), getY() + 1, getZ(), 3, 0.4, 0.5, 0.4, 0.02);
                case SERAPHEL -> sw.spawnParticles(ParticleTypes.PORTAL,
                    getX(), getY() + 1, getZ(), 4, 0.4, 0.5, 0.4, 0.05);
                case HEXARA   -> sw.spawnParticles(ParticleTypes.CRIMSON_SPORE,
                    getX(), getY() + 1, getZ(), 4, 0.4, 0.5, 0.4, 0.02);
            }
        }

        // Role ability (every 4 seconds)
        if (--abilityTimer <= 0) {
            abilityTimer = 80;
            useAbility(sw);
        }

        // Hexara skeleton summon (every 12 seconds)
        if (role == WitchRole.HEXARA && --summonTimer <= 0) {
            summonTimer = 240;
            summonSkeletons(sw, 2);
        }
    }

    // ── ABILITIES ─────────────────────────────────────────────────────────────

    private void useAbility(ServerWorld sw) {
        PlayerEntity target = sw.getClosestPlayer(this, 24);
        if (target == null) return;

        switch (role) {
            case MORVAINE -> {
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 120, 2));
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 120, 1));
                sw.spawnParticles(ParticleTypes.WITCH,
                    target.getX(), target.getY() + 1, target.getZ(), 15, 0.5, 0.8, 0.5, 0.05);
                sw.playSound(null, target.getBlockPos(),
                    SoundEvents.ENTITY_WITCH_CELEBRATE, SoundCategory.HOSTILE, 1.2f, 0.8f);
            }
            case SERAPHEL -> {
                Vec3d origin = getPos().add(0, 1.5, 0);
                Vec3d dir = target.getPos().add(0, 1, 0).subtract(origin).normalize();
                SmallFireballEntity bolt = new SmallFireballEntity(sw, this,
                    dir.x * 1.2, dir.y * 1.2, dir.z * 1.2);
                bolt.setPos(origin.x + dir.x * 1.5, origin.y, origin.z + dir.z * 1.5);
                sw.spawnEntity(bolt);
                sw.spawnParticles(ParticleTypes.REVERSE_PORTAL,
                    origin.x, origin.y, origin.z, 10, 0.3, 0.3, 0.3, 0.08);
                sw.playSound(null, getBlockPos(),
                    SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 1f, 0.6f);
            }
            case HEXARA -> {
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 100, 1));
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON,  80, 0));
                sw.spawnParticles(ParticleTypes.SOUL,
                    target.getX(), target.getY() + 1, target.getZ(), 12, 0.5, 0.8, 0.5, 0.05);
            }
        }
    }

    /** Hexara summons enchanted iron skeleton soldiers. */
    private void summonSkeletons(ServerWorld sw, int count) {
        broadcastNearby(sw, Text.literal("☠ Hexara calls her skeleton guard!").formatted(Formatting.DARK_RED));
        for (int i = 0; i < count; i++) {
            SkeletonEntity skeleton = EntityType.SKELETON.create(sw);
            if (skeleton == null) continue;
            double ox = getX() + (random.nextDouble() - 0.5) * 6;
            double oz = getZ() + (random.nextDouble() - 0.5) * 6;
            skeleton.setPos(ox, getY(), oz);
            skeleton.initialize(sw, sw.getLocalDifficulty(getBlockPos()),
                SpawnReason.MOB_SUMMONED, null, null);

            // Enchanted iron armor — using ItemStack.addEnchantment (correct 1.20.1 API)
            ItemStack helm  = new ItemStack(Items.IRON_HELMET);
            ItemStack chest = new ItemStack(Items.IRON_CHESTPLATE);
            ItemStack legs  = new ItemStack(Items.IRON_LEGGINGS);
            ItemStack boots = new ItemStack(Items.IRON_BOOTS);
            helm.addEnchantment(Enchantments.PROTECTION, 2);
            chest.addEnchantment(Enchantments.PROTECTION, 2);
            legs.addEnchantment(Enchantments.PROTECTION, 1);
            boots.addEnchantment(Enchantments.PROTECTION, 1);

            skeleton.equipStack(EquipmentSlot.HEAD,  helm);
            skeleton.equipStack(EquipmentSlot.CHEST, chest);
            skeleton.equipStack(EquipmentSlot.LEGS,  legs);
            skeleton.equipStack(EquipmentSlot.FEET,  boots);

            // Zero drop chance — no armor loot
            skeleton.setEquipmentDropChance(EquipmentSlot.HEAD,  0f);
            skeleton.setEquipmentDropChance(EquipmentSlot.CHEST, 0f);
            skeleton.setEquipmentDropChance(EquipmentSlot.LEGS,  0f);
            skeleton.setEquipmentDropChance(EquipmentSlot.FEET,  0f);

            sw.spawnEntity(skeleton);
            sw.spawnParticles(ParticleTypes.SOUL, ox, getY() + 1, oz, 8, 0.3, 0.5, 0.3, 0.05);
        }
        sw.playSound(null, getBlockPos(),
            SoundEvents.ENTITY_WITHER_AMBIENT, SoundCategory.HOSTILE, 0.8f, 1.4f);
    }

    // ── HIT REACTION ─────────────────────────────────────────────────────────

    @Override
    public boolean damage(DamageSource source, float amount) {
        // MORVAINE curses the attacker when struck
        if (role == WitchRole.MORVAINE && source.getAttacker() instanceof PlayerEntity p)
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 1));
        return super.damage(source, amount);
    }

    // ── DEATH ─────────────────────────────────────────────────────────────────

    @Override
    public void onDeath(DamageSource src) {
        super.onDeath(src);
        bossBar.clearPlayers();
        if (!(getWorld() instanceof ServerWorld sw)) return;

        // Death particles
        sw.spawnParticles(ParticleTypes.WITCH,
            getX(), getY() + 1, getZ(), 40, 1.5, 1.0, 1.5, 0.1);
        sw.spawnParticles(ParticleTypes.EXPLOSION,
            getX(), getY(), getZ(), 5, 1.0, 0.5, 1.0, 0);
        sw.playSound(null, getBlockPos(),
            SoundEvents.ENTITY_WITCH_DEATH, SoundCategory.HOSTILE, 2f, 0.7f);

        // Drops
        dropStack(new ItemStack(ModItems.SOUL_CRYSTAL, 2 + random.nextInt(3)));
        dropStack(new ItemStack(ModItems.MANA_CRYSTAL, 3 + random.nextInt(4)));
        if (role == WitchRole.HEXARA)
            dropStack(new ItemStack(ModItems.DEMON_CORE, 1 + random.nextInt(2)));

        // XP + achievement check for nearby players
        for (PlayerEntity p : sw.getPlayers()) {
            if (p.distanceTo(this) > 64f) continue;
            int xp = (role == WitchRole.HEXARA) ? 400 : 300;
            PlayerRpgManager.addXp(p, xp);
            p.sendMessage(Text.literal(getCustomName().getString()
                + " defeated! +" + xp + " XP!").formatted(Formatting.LIGHT_PURPLE), false);
            if (p instanceof ServerPlayerEntity sp)
                checkCovenAchievement(sp, role);
        }
    }

    /**
     * Per-player achievement: kill all 3 members → "Witch Hunter".
     *
     * BUG FIX vs previous: reset is done AFTER awarding, not before checking,
     * so in multiplayer all players who were nearby get credit before reset.
     * The per-player NBT means each player tracks independently.
     */
    private static void checkCovenAchievement(ServerPlayerEntity p, WitchRole killed) {
        NbtCompound d = PlayerRpgManager.getData(p);
        d.putBoolean("WitchKill_" + killed.name(), true);
        PlayerRpgManager.setData(p, d);

        boolean m = d.getBoolean("WitchKill_MORVAINE");
        boolean s = d.getBoolean("WitchKill_SERAPHEL");
        boolean h = d.getBoolean("WitchKill_HEXARA");
        boolean alreadyDone = d.getBoolean("WitchHunterEarned");

        if (m && s && h && !alreadyDone) {
            // Mark EARNED first (persistent, not reset)
            d.putBoolean("WitchHunterEarned", true);
            // Reset kill tracking so next coven can be hunted
            d.remove("WitchKill_MORVAINE");
            d.remove("WitchKill_SERAPHEL");
            d.remove("WitchKill_HEXARA");
            PlayerRpgManager.setData(p, d);

            // Server-wide broadcast
            if (p.getServer() != null)
                p.getServer().getPlayerManager().broadcast(
                    Text.literal("★ [ACHIEVEMENT] " + p.getName().getString()
                        + " has slain the entire Witch Coven! ★")
                        .formatted(Formatting.GOLD, Formatting.BOLD), false);

            // Title on player screen
            p.networkHandler.sendPacket(new TitleS2CPacket(
                Text.literal("☽  WITCH HUNTER  ☽").formatted(Formatting.DARK_PURPLE, Formatting.BOLD)));
            p.networkHandler.sendPacket(new SubtitleS2CPacket(
                Text.literal("Morvaine · Seraphel · Hexara — all slain")
                    .formatted(Formatting.LIGHT_PURPLE)));

            // Reward
            p.getInventory().insertStack(new ItemStack(ModItems.MANA_CRYSTAL, 12));
            p.getInventory().insertStack(new ItemStack(ModItems.SOUL_CRYSTAL, 8));
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 6000, 0));
            p.sendMessage(Text.literal("☽ Witch Hunter! Luck I for 5 minutes + 12 Mana Crystals.")
                .formatted(Formatting.DARK_PURPLE), false);
        }
    }

    // ── BOSS BAR TRACKING ────────────────────────────────────────────────────

    @Override
    public void onStartedTrackingBy(ServerPlayerEntity p) {
        super.onStartedTrackingBy(p);
        bossBar.addPlayer(p);
    }
    @Override
    public void onStoppedTrackingBy(ServerPlayerEntity p) {
        super.onStoppedTrackingBy(p);
        bossBar.removePlayer(p);
    }

    // ── NBT ───────────────────────────────────────────────────────────────────

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putString("WitchRole", role.name());
        nbt.putBoolean("CovenSpawned", covenSpawned);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("WitchRole")) {
            try { role = WitchRole.valueOf(nbt.getString("WitchRole")); }
            catch (IllegalArgumentException ignored) { role = WitchRole.MORVAINE; }
        }
        covenSpawned = nbt.getBoolean("CovenSpawned");
        refreshDisplay();
    }

    // ── SOUNDS ───────────────────────────────────────────────────────────────

    private void broadcastNearby(ServerWorld sw, Text msg) {
        sw.getPlayers().stream()
            .filter(p -> p.distanceTo(this) <= 80f)
            .forEach(p -> p.sendMessage(msg, false));
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.ENTITY_WITCH_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.ENTITY_WITCH_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ENTITY_WITCH_DEATH; }
}
