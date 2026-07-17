package com.bikininjas.funnyeffects.item;

import com.bikininjas.corelib.log.LogManager;
import com.bikininjas.corelib.log.ModLogger;
import com.bikininjas.funnyeffects.FunnyEffectsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Armor-related event handlers for Funny Effects items: Rainbow Boots, Bouncy Boots,
 * Sneaky Helmet, Lava Walker, and Potato Goggles. Registered on the NeoForge event bus.
 */
public final class ArmorHandlers {

    private static final ModLogger LOGGER = LogManager.getLogger(FunnyEffectsMod.MOD_ID, ArmorHandlers.class);
    private static final Random RANDOM = new Random();

    private ArmorHandlers() {
    }

    static {
        NeoForge.EVENT_BUS.register(ArmorHandlers.class);
    }

    /** Force class loading so the static initializer runs. */
    public static void init() {
    }

    /**
     * Potato goggles: reveal nearby hostile mobs with Glowing while worn on the head.
     */
    @SubscribeEvent
    public static void onPlayerTickGoggles(@NotNull PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!head.is(ModItems.POTATO_GOGGLES.get())) {
            return;
        }
        net.minecraft.world.phys.AABB area = new net.minecraft.world.phys.AABB(player.blockPosition()).inflate(10.0D);
        for (LivingEntity mob : player.level().getEntitiesOfClass(LivingEntity.class, area)) {
            if (mob instanceof Enemy) {
                mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, true, false));
            }
        }
    }

    /**
     * Rainbow boots: leave a rainbow particle trail while moving.
     */
    @SubscribeEvent
    public static void onPlayerTickRainbow(@NotNull PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        ItemStack feet = player.getItemBySlot(EquipmentSlot.FEET);
        if (!feet.is(ModItems.RAINBOW_BOOTS.get())) {
            return;
        }
        Vec3 movement = player.getDeltaMovement();
        if (movement.length() <= 0.1D) {
            return;
        }
        if (player.tickCount % 3 != 0) {
            return;
        }
        int randomRGB = RANDOM.nextInt(0xFFFFFF);
        float r = ((randomRGB >> 16) & 0xFF) / 255.0F;
        float g = ((randomRGB >> 8) & 0xFF) / 255.0F;
        float b = (randomRGB & 0xFF) / 255.0F;
        Vec3 pos = player.position();
        player.level().addParticle(new DustParticleOptions(new Vector3f(r, g, b), 1.0F),
                pos.x, pos.y + 0.1D, pos.z, 0.0D, 0.0D, 0.0D);
        LOGGER.debug("Rainbow trail at {}", player.position());
    }

    /**
     * Bouncy boots: negate fall damage and launch the wearer slightly upward.
     */
    @SubscribeEvent
    public static void bouncyBoots_onFall(@NotNull LivingFallEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.BOUNCY_BOOTS.get())) {
            return;
        }
        event.setDamageMultiplier(0);
        event.setDistance(0);
        Level level = player.level();
        level.playSound(null, player.blockPosition(), SoundEvents.SLIME_BLOCK_FALL,
                SoundSource.PLAYERS, 1.0F, 1.0F);
        player.setDeltaMovement(player.getDeltaMovement().add(0, 0.3, 0));
        player.hurtMarked = true;
        LOGGER.info("Bouncy boots absorbed fall for {}", player.getName().getString());
    }

    /**
     * Sneaky helmet: blind the wearer and reveal nearby mobs with Glowing.
     */
    @SubscribeEvent
    public static void sneakyHelmet_onTick(@NotNull PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        if (!player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.SNEAKY_HELMET.get())) {
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0, true, false));
        Level level = player.level();
        level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(10),
                e -> e != player && e instanceof Enemy).forEach(e ->
                e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, true, false)));
    }

    /** Lava walker: obsidian positions and the server tick time they were solidified. */
    private static final Map<BlockPos, Long> LAVA_WALKER_SOLIDIFIED = new ConcurrentHashMap<>();

    private static final long LAVA_RESTORE_DELAY_TICKS = 60L; // 3 seconds at 20 TPS

    /**
     * Lava walker boots: while sprinting, solidify lava to obsidian around the player's feet.
     */
    @SubscribeEvent
    public static void lavaWalker_onTick(@NotNull PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        if (!player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.LAVA_WALKER.get())) {
            return;
        }
        if (!player.isSprinting()) {
            return;
        }
        Level level = player.level();
        BlockPos feet = player.blockPosition();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos pos = feet.offset(dx, 0, dz);
                if (level.getBlockState(pos).is(Blocks.LAVA)) {
                    level.setBlock(pos, Blocks.OBSIDIAN.defaultBlockState(), 3);
                    LAVA_WALKER_SOLIDIFIED.put(pos.immutable(), (long) ((ServerLevel) level).getServer().getTickCount());
                }
            }
        }
    }

    /**
     * Lava walker restore: revert obsidian back to lava 3 seconds after the player leaves.
     */
    @SubscribeEvent
    public static void lavaWalker_onServerTick(@NotNull ServerTickEvent.Post event) {
        if (LAVA_WALKER_SOLIDIFIED.isEmpty()) {
            return;
        }
        var server = event.getServer();
        long now = server.getTickCount();
        var overworld = server.overworld();
        var iterator = LAVA_WALKER_SOLIDIFIED.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (now - entry.getValue() >= LAVA_RESTORE_DELAY_TICKS) {
                BlockPos pos = entry.getKey();
                if (overworld.getBlockState(pos).is(Blocks.OBSIDIAN)) {
                    overworld.setBlock(pos, Blocks.LAVA.defaultBlockState(), 3);
                }
                iterator.remove();
            }
        }
    }

    private static boolean isHolding(@NotNull LivingEntity entity, @NotNull Item item) {
        Objects.requireNonNull(entity, "entity must not be null");
        Objects.requireNonNull(item, "item must not be null");
        return entity.getMainHandItem().is(item) || entity.getOffhandItem().is(item);
    }
}
