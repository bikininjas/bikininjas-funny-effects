package com.bikininjas.funnyeffects.item;

import com.bikininjas.corelib.log.LogManager;
import com.bikininjas.corelib.log.ModLogger;
import com.bikininjas.funnyeffects.FunnyEffectsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.Objects;
import java.util.Random;

/**
 * Gadget-related event handlers for Funny Effects items: Magnetic Glove, XP Magnet,
 * Void Pearl, Gravity Anchor, Party Popper, Squeaky Toy, and Infinite Pearl.
 * Registered on the NeoForge event bus.
 */
public final class GadgetHandlers {

    private static final ModLogger LOGGER = LogManager.getLogger(FunnyEffectsMod.MOD_ID, GadgetHandlers.class);
    private static final Random RANDOM = new Random();

    private GadgetHandlers() {
    }

    static {
        NeoForge.EVENT_BUS.register(GadgetHandlers.class);
    }

    /** Force class loading so the static initializer runs. */
    public static void init() {
    }

    /**
     * Magnet glove: pull nearby dropped items toward the player while held in either hand.
     */
    @SubscribeEvent
    public static void onPlayerTick(@NotNull PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        if (!main.is(ModItems.MAGNETIC_GLOVE.get()) && !off.is(ModItems.MAGNETIC_GLOVE.get())) {
            return;
        }

        AABB area = new AABB(player.blockPosition()).inflate(5.0D);
        for (net.minecraft.world.entity.item.ItemEntity item : player.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, area)) {
            Vec3 toPlayer = player.position().subtract(item.position()).normalize();
            double dist = item.position().distanceTo(player.position());
            double strength = Math.max(0.05D, 0.35D - dist * 0.04D);
            item.setDeltaMovement(item.getDeltaMovement().add(toPlayer.scale(strength)));
            item.hasImpulse = true;
        }
    }

    /**
     * Squeaky toy: right-click plays a bat squeak and slows nearby mobs (5 block radius).
     */
    @SubscribeEvent
    public static void onRightClick(@NotNull PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (!stack.is(ModItems.SQUEAKY_TOY.get())) {
            return;
        }
        Level level = player.level();
        if (level.isClientSide()) {
            return;
        }
        BlockPos soundPos = BlockPos.containing(player.position());
        level.playSound(null, soundPos, SoundEvents.BAT_TAKEOFF, SoundSource.PLAYERS, 1.0F, 1.0F);
        AABB area = new AABB(soundPos).inflate(5.0D);
        for (LivingEntity mob : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (mob instanceof net.minecraft.world.entity.monster.Enemy) {
                mob.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 100, 2, false, true));
            }
        }
    }

    /**
     * XP magnet: pull nearby XP orbs toward the holder.
     */
    @SubscribeEvent
    public static void onPlayerTickXpMagnet(@NotNull PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        if (player.getInventory().items.stream().noneMatch(stack -> stack.is(ModItems.XP_MAGNET.get()))
                && player.getInventory().armor.stream().noneMatch(stack -> stack.is(ModItems.XP_MAGNET.get()))
                && player.getInventory().offhand.stream().noneMatch(stack -> stack.is(ModItems.XP_MAGNET.get()))) {
            return;
        }
        if (player.tickCount % 5 != 0) {
            return;
        }
        AABB area = new AABB(player.blockPosition()).inflate(8.0D);
        for (ExperienceOrb orb : player.level().getEntitiesOfClass(ExperienceOrb.class, area)) {
            Vec3 toPlayer = player.position().subtract(orb.position()).normalize();
            orb.setDeltaMovement(toPlayer.scale(0.3D));
        }
    }

    /**
     * Void pearl: right-click to warp 20 blocks in the look direction.
     */
    @SubscribeEvent
    public static void onRightClickVoidPearl(@NotNull PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (!stack.is(ModItems.VOID_PEARL.get())) {
            return;
        }
        Level level = player.level();
        if (level.isClientSide()) {
            return;
        }
        Vec3 look = player.getLookAngle();
        player.teleportTo(player.getX() + look.x * 20.0D,
                player.getY() + look.y * 20.0D,
                player.getZ() + look.z * 20.0D);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 1.0F, 1.0F);
        LOGGER.info("Void warp! {}", player.getName().getString());
    }

    /**
     * Party popper: right-click to fire a confetti burst and consume the item.
     */
    @SubscribeEvent
    public static void partyPopper_onRightClick(@NotNull PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (!stack.is(ModItems.PARTY_POPPER.get())) {
            return;
        }
        Level level = player.level();
        if (level.isClientSide()) {
            return;
        }
        Vec3 pos = player.position();
        for (int i = 0; i < 20; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double radius = RANDOM.nextDouble() * 3.0D;
            double x = pos.x + Math.cos(angle) * radius;
            double z = pos.z + Math.sin(angle) * radius;
            double y = pos.y + RANDOM.nextDouble() * 2.0D;
            int rgb = RANDOM.nextInt(0xFFFFFF);
            float r = ((rgb >> 16) & 0xFF) / 255.0F;
            float g = ((rgb >> 8) & 0xFF) / 255.0F;
            float b = (rgb & 0xFF) / 255.0F;
            level.addParticle(new DustParticleOptions(new Vector3f(r, g, b), 1.0F),
                    x, y, z, 0.0D, 0.05D, 0.0D);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH,
                SoundSource.PLAYERS, 1.0F, 1.0F);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS, 1.0F, 1.0F);
        stack.shrink(1);
        event.setCanceled(true);
        LOGGER.info("Party popper fired by {}", player.getName().getString());
    }

    /**
     * Gravity anchor: right-click floats the player for 5s and launches nearby mobs upward.
     */
    @SubscribeEvent
    public static void gravityAnchor_onRightClick(@NotNull PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (!stack.is(ModItems.GRAVITY_ANCHOR.get())) {
            return;
        }
        Level level = player.level();
        if (level.isClientSide()) {
            return;
        }
        if (player.getCooldowns().isOnCooldown(stack.getItem())) {
            return;
        }
        player.setNoGravity(true);
        player.setDeltaMovement(0.0D, 0.0D, 0.0D);
        player.hurtMarked = true;
        // Launch nearby living entities upward.
        AABB area = new AABB(player.blockPosition()).inflate(3.0D);
        for (LivingEntity mob : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (mob == player) {
                continue;
            }
            mob.setDeltaMovement(mob.getDeltaMovement().add(0.0D, 2.0D, 0.0D));
            mob.hurtMarked = true;
        }
        // Reset gravity after 5 seconds (100 ticks) via a scheduled server task.
        if (level instanceof ServerLevel serverLevel && player.getServer() != null) {
            player.getServer().tell(new net.minecraft.server.TickTask(serverLevel.getServer().getTickCount() + 100,
                    () -> {
                        if (player.isAlive()) {
                            player.setNoGravity(false);
                        }
                    }));
        }
        player.getCooldowns().addCooldown(stack.getItem(), 300); // 15 seconds
        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 1.0F, 1.0F);
        LOGGER.info("Gravity anchor activated for {}", player.getName().getString());
    }

    /**
     * Reset gravity on death to prevent players from floating indefinitely if they die
     * while under the Gravity Anchor effect.
     */
    @SubscribeEvent
    public static void onDeathReset(@NotNull LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.setNoGravity(false);
        }
    }

    /**
     * Infinite pearl: right-click throws an ender pearl without consuming the item.
     * 10% chance to spawn an Endermite at the destination.
     */
    @SubscribeEvent
    public static void infinitePearl_onRightClick(@NotNull PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (!stack.is(ModItems.INFINITE_PEARL.get())) {
            return;
        }
        Level level = player.level();
        if (level.isClientSide()) {
            return;
        }
        if (player.getCooldowns().isOnCooldown(stack.getItem())) {
            return;
        }
        var pearl = new ThrownEnderpearl(level, player);
        pearl.setItem(new ItemStack(Items.ENDER_PEARL));
        Vec3 look = player.getLookAngle();
        pearl.setPos(player.getX(), player.getEyeY() - 0.1D, player.getZ());
        pearl.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
        level.addFreshEntity(pearl);
        player.getCooldowns().addCooldown(stack.getItem(), 20);
        // 10% chance to spawn an Endermite at the destination after the pearl lands.
        if (level instanceof ServerLevel serverLevel && RANDOM.nextDouble() < 0.10D) {
            BlockPos dest = BlockPos.containing(player.getX() + look.x * 8.0D,
                    player.getY() + look.y * 8.0D, player.getZ() + look.z * 8.0D);
            Endermite mite = net.minecraft.world.entity.EntityType.ENDERMITE.create(serverLevel);
            if (mite != null) {
                mite.moveTo(dest.getX() + 0.5D, dest.getY(), dest.getZ() + 0.5D, 0.0F, 0.0F);
                serverLevel.addFreshEntity(mite);
                LOGGER.info("Infinite pearl spawned an Endermite at {}", dest);
            }
        }
        event.setCanceled(true);
    }

    private static boolean isHolding(@NotNull LivingEntity entity, @NotNull Item item) {
        Objects.requireNonNull(entity, "entity must not be null");
        Objects.requireNonNull(item, "item must not be null");
        return entity.getMainHandItem().is(item) || entity.getOffhandItem().is(item);
    }
}
