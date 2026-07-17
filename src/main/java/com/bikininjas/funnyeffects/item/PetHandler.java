package com.bikininjas.funnyeffects.item;

import com.bikininjas.corelib.log.LogManager;
import com.bikininjas.corelib.log.ModLogger;
import com.bikininjas.funnyeffects.FunnyEffectsMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PetHandler {

    private static final ModLogger LOGGER = LogManager.getLogger(FunnyEffectsMod.MOD_ID, PetHandler.class);
    private static final long PET_LIFETIME_TICKS = 6000L; // 5 minutes
    private static final int TELEPORT_INTERVAL_TICKS = 20;

    private static final Map<UUID, PetData> activePets = new ConcurrentHashMap<>();

    static {
        NeoForge.EVENT_BUS.register(EventHandler.class);
    }

    private PetHandler() {
    }

    public static void init() {
    }

    public static void registerPet(@NotNull Player owner, @NotNull LivingEntity pet) {
        activePets.put(pet.getUUID(), new PetData(owner.getUUID(), owner.level().getGameTime()));
        LOGGER.info("Pet registered: {} for player {}", pet.getName().getString(), owner.getName().getString());
    }

    private static final class PetData {
        final UUID ownerId;
        final long spawnedAt;

        PetData(UUID ownerId, long spawnedAt) {
            this.ownerId = ownerId;
            this.spawnedAt = spawnedAt;
        }
    }

    private static final class EventHandler {
        private EventHandler() {
        }

        @SubscribeEvent
        static void onPlayerTick(@NotNull PlayerTickEvent.Post event) {
            Player player = event.getEntity();
            if (player.level().isClientSide()) {
                return;
            }
            if (player.tickCount % TELEPORT_INTERVAL_TICKS != 0) {
                return;
            }
            var iterator = activePets.entrySet().iterator();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                PetData data = entry.getValue();
                if (!data.ownerId.equals(player.getUUID())) {
                    continue;
                }
                Entity pet = ((ServerLevel) player.level()).getEntity(entry.getKey());
                if (pet == null || !pet.isAlive()) {
                    iterator.remove();
                    continue;
                }
                long age = player.level().getGameTime() - data.spawnedAt;
                if (age >= PET_LIFETIME_TICKS) {
                    pet.discard();
                    iterator.remove();
                    LOGGER.info("Pet {} despawned (lifetime expired)", pet.getName().getString());
                    continue;
                }
                double offsetX = player.getRandom().nextDouble() * 3.0D - 1.5D;
                double offsetZ = player.getRandom().nextDouble() * 3.0D - 1.5D;
                pet.moveTo(player.getX() + offsetX, player.getY(), player.getZ() + offsetZ,
                        pet.getYRot(), pet.getXRot());
            }
        }
    }
}
