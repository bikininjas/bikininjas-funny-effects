package com.bikininjas.funnyeffects.item;

import com.bikininjas.corelib.log.LogManager;
import com.bikininjas.corelib.log.ModLogger;
import com.bikininjas.funnyeffects.FunnyEffectsMod;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.Objects;
import java.util.Random;

/**
 * Combat-related event handlers for Funny Effects items: Lifesteal Blade, Thorns Shield,
 * Thunder Sword, Yeeter Hammer, Disco Sword, and Slapfish.
 * Registered on the NeoForge event bus.
 */
public final class CombatHandlers {

    private static final ModLogger LOGGER = LogManager.getLogger(FunnyEffectsMod.MOD_ID, CombatHandlers.class);
    private static final Random RANDOM = new Random();

    private CombatHandlers() {
    }

    static {
        NeoForge.EVENT_BUS.register(CombatHandlers.class);
    }

    /** Force class loading so the static initializer runs. */
    public static void init() {
    }

    /**
     * Lifesteal blade: heal the attacker for 20% of damage dealt when wielding the blade.
     * Combo bonus with Thorns Shield in offhand: 30% lifesteal.
     */
    @SubscribeEvent
    public static void onLivingDamageLifesteal(@NotNull LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide()) {
            return;
        }
        Entity source = event.getSource().getEntity();
        if (!(source instanceof Player player)) {
            return;
        }
        if (!isHolding(player, ModItems.LIFESTEAL_BLADE.get())) {
            return;
        }
        float dealt = event.getNewDamage();
        boolean combo = player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.THORNS_SHIELD.get());
        float heal = dealt * (combo ? 0.3F : 0.2F);
        if (heal > 0.0F) {
            player.heal(heal);
        }
    }

    /**
     * Thorns shield: reflect 50% of pre-mitigation damage back to a LivingEntity attacker.
     */
    @SubscribeEvent
    public static void onIncomingDamage(@NotNull LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) {
            return;
        }
        // Prevent recursion: skip if damage is our own thorns reflection
        if (event.getSource().getMsgId().equals("thorns")) {
            return;
        }
        ItemStack chest = victim.getItemBySlot(EquipmentSlot.CHEST);
        if (!chest.is(ModItems.THORNS_SHIELD.get())) {
            return;
        }
        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof LivingEntity livingAttacker) || livingAttacker == victim) {
            return;
        }
        float reflected = event.getOriginalAmount() * 0.5F;
        if (reflected > 0.0F) {
            livingAttacker.hurt(victim.damageSources().thorns(victim), reflected);
        }
    }

    /**
     * Yeeter hammer: launch the hit entity high into the air.
     */
    @SubscribeEvent
    public static void onLivingDamageYeeter(@NotNull LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide()) {
            return;
        }
        Entity source = event.getSource().getEntity();
        if (!(source instanceof LivingEntity attacker)) {
            return;
        }
        if (!isHolding(attacker, ModItems.YEETER_HAMMER.get())) {
            return;
        }
        target.setDeltaMovement(target.getDeltaMovement().add(0.0D, 1.5D, 0.0D));
        target.hurtMarked = true;
        LOGGER.info("Yeeeeet! Launched {}", target.getName().getString());
    }

    /**
     * Thunder sword: strike the hit entity with lightning.
     */
    @SubscribeEvent
    public static void onLivingDamageThunder(@NotNull LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide()) {
            return;
        }
        Entity source = event.getSource().getEntity();
        if (!(source instanceof LivingEntity attacker)) {
            return;
        }
        if (!isHolding(attacker, ModItems.THUNDER_SWORD.get())) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel) target.level();
        EntityType.LIGHTNING_BOLT.spawn(serverLevel, target.blockPosition(),
                net.minecraft.world.entity.MobSpawnType.TRIGGERED);
        LOGGER.info("Thunder struck {}", target.getName().getString());
    }

    /**
     * Slapfish: negate incoming damage and knock the target away from the attacker while held.
     */
    @SubscribeEvent
    public static void slapfish_onDamage(@NotNull LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        Level level = victim.level();
        if (level.isClientSide()) {
            return;
        }
        Entity source = event.getSource().getEntity();
        if (!(source instanceof LivingEntity attacker)) {
            return;
        }
        if (!isHolding(attacker, ModItems.SLAPFISH.get())) {
            return;
        }
        if (attacker instanceof Player p && p.getCooldowns().isOnCooldown(ModItems.SLAPFISH.get())) return;
        // Cancel the default damage entirely.
        event.getContainer().setNewDamage(0.0F);
        // Knock the victim away along the attacker's look vector.
        Vec3 dir = attacker.getLookAngle().normalize().scale(3.0D);
        victim.push(dir.x, dir.y * 0.5D + 0.2D, dir.z);
        victim.hurtMarked = true;
        level.playSound(null, victim.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 1.0F, 1.0F);
        if (attacker instanceof Player slapPlayer) {
            slapPlayer.getCooldowns().addCooldown(ModItems.SLAPFISH.get(), 30);
        }
        LOGGER.info("Slapfish! {} slapped {}", attacker.getName().getString(), victim.getName().getString());
    }

    /**
     * Disco sword: on hit, spray colored dust particles and play a random note block sound.
     */
    @SubscribeEvent
    public static void discoSword_onDamage(@NotNull LivingDamageEvent.Post event) {
        LivingEntity victim = event.getEntity();
        Level level = victim.level();
        if (level.isClientSide()) {
            return;
        }
        Entity source = event.getSource().getEntity();
        if (!(source instanceof LivingEntity attacker)) {
            return;
        }
        if (!isHolding(attacker, ModItems.DISCO_SWORD.get())) {
            return;
        }
        if (attacker instanceof Player p && p.getCooldowns().isOnCooldown(ModItems.DISCO_SWORD.get())) return;
        Vec3 pos = victim.position();
        for (int i = 0; i < 5; i++) {
            int rgb = RANDOM.nextInt(0xFFFFFF);
            float r = ((rgb >> 16) & 0xFF) / 255.0F;
            float g = ((rgb >> 8) & 0xFF) / 255.0F;
            float b = (rgb & 0xFF) / 255.0F;
            level.addParticle(new DustParticleOptions(new Vector3f(r, g, b), 1.0F),
                    pos.x, pos.y + 1.0D, pos.z, 0.0D, 0.1D, 0.0D);
        }
        @SuppressWarnings("unchecked") // SoundEvents note fields are Holder.Reference<SoundEvent>
        Holder<SoundEvent>[] notes = new Holder[]{
                SoundEvents.NOTE_BLOCK_HAT, SoundEvents.NOTE_BLOCK_BASEDRUM};
        level.playSound(null, victim.blockPosition(), notes[RANDOM.nextInt(notes.length)].value(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        if (attacker instanceof Player discoPlayer) {
            discoPlayer.getCooldowns().addCooldown(ModItems.DISCO_SWORD.get(), 30);
        }
        LOGGER.info("Disco! {} hit {}", attacker.getName().getString(), victim.getName().getString());
    }

    private static boolean isHolding(@NotNull LivingEntity entity, @NotNull Item item) {
        Objects.requireNonNull(entity, "entity must not be null");
        Objects.requireNonNull(item, "item must not be null");
        return entity.getMainHandItem().is(item) || entity.getOffhandItem().is(item);
    }
}
