package com.bikininjas.funnyeffects.item;

import com.bikininjas.corelib.log.LogManager;
import com.bikininjas.corelib.log.ModLogger;
import com.bikininjas.funnyeffects.FunnyEffectsMod;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Random;

/**
 * Entity interaction event handlers for Funny Effects items: Chicken Wand and Dinnerbone Bat.
 * Registered on the NeoForge event bus.
 */
public final class EntityInteractHandlers {

    private static final ModLogger LOGGER = LogManager.getLogger(FunnyEffectsMod.MOD_ID, EntityInteractHandlers.class);
    private static final Random RANDOM = new Random();

    private EntityInteractHandlers() {
    }

    static {
        NeoForge.EVENT_BUS.register(EntityInteractHandlers.class);
    }

    /** Force class loading so the static initializer runs. */
    public static void init() {
    }

    /**
     * Chicken wand: right-click an entity to make it bawk and hop like a chicken.
     */
    @SubscribeEvent
    public static void onEntityInteractChicken(@NotNull PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (!stack.is(ModItems.CHICKEN_WAND.get())) {
            return;
        }
        Level level = player.level();
        if (level.isClientSide()) {
            return;
        }
        Entity target = event.getTarget();
        if (!(target instanceof LivingEntity living) || target instanceof Player) {
            return;
        }
        living.setDeltaMovement(living.getDeltaMovement().add(0.0D, 1.0D, 0.0D));
        living.hurtMarked = true;
        level.playSound(null, living.blockPosition(), SoundEvents.CHICKEN_AMBIENT,
                SoundSource.PLAYERS, 1.0F, 1.0F);
        LOGGER.info("Bawk! {} is now a chicken", living.getName().getString());
    }

    /**
     * Dinnerbone bat: right-click an entity to flip it upside down by setting its custom name
     * to "Dinnerbone". This triggers the vanilla entity renderer's flipped-name check.
     */
    @SubscribeEvent
    public static void dinnerboneBat_onEntityInteract(@NotNull PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        Level level = player.level();
        if (level.isClientSide()) {
            return;
        }
        if (!isHolding(player, ModItems.DINNERBONE_BAT.get())) {
            return;
        }
        Entity target = event.getTarget();
        if (target == null) {
            return;
        }
        if (target.getCustomName() != null && "Dinnerbone".equals(target.getCustomName().getString())) {
            target.setCustomName(null);
            level.playSound(null, target.blockPosition(), SoundEvents.VILLAGER_NO,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
        } else {
            target.setCustomName(Component.literal("Dinnerbone"));
            level.playSound(null, target.blockPosition(), SoundEvents.BAT_TAKEOFF,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        event.setCanceled(true);
        LOGGER.info("Dinnerbone bat toggled name on {}", target.getName().getString());
    }

    private static boolean isHolding(@NotNull LivingEntity entity, @NotNull Item item) {
        Objects.requireNonNull(entity, "entity must not be null");
        Objects.requireNonNull(item, "item must not be null");
        return entity.getMainHandItem().is(item) || entity.getOffhandItem().is(item);
    }
}
