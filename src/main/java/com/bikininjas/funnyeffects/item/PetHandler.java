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
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
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
        var server = owner.getServer();
        long now = (server != null) ? server.getTickCount() : 0L;
        activePets.put(pet.getUUID(), new PetData(owner.getUUID(), now));
        LOGGER.info("Pet registered: {} for player {}", pet.getName().getString(), owner.getName().getString());
    }

    public static @NotNull Map<UUID, PetData> getActivePets(@NotNull UUID ownerId) {
        java.util.Objects.requireNonNull(ownerId, "ownerId must not be null");
        Map<UUID, PetData> result = new java.util.HashMap<>();
        for (var entry : activePets.entrySet()) {
            if (entry.getValue().ownerId.equals(ownerId)) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    public record PetData(UUID ownerId, long spawnedAt) {}

    private static final class EventHandler {
        private EventHandler() {
        }

        @SubscribeEvent
        static void onPlayerLogout(@NotNull PlayerEvent.PlayerLoggedOutEvent event) {
            var playerId = event.getEntity().getUUID();
            var iterator = activePets.entrySet().iterator();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                if (entry.getValue().ownerId.equals(playerId)) {
                    for (var level : event.getEntity().getServer().getAllLevels()) {
                        Entity pet = level.getEntity(entry.getKey());
                        if (pet != null) {
                            pet.discard();
                            break;
                        }
                    }
                    iterator.remove();
                }
            }
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
            long now = player.getServer() != null ? player.getServer().getTickCount() : 0L;
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
                long age = now - data.spawnedAt;
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

        @SubscribeEvent
        static void onServerTick(@NotNull ServerTickEvent.Post event) {
            if (event.getServer().getTickCount() % TELEPORT_INTERVAL_TICKS != 0) {
                return;
            }
            long now = event.getServer().getTickCount();
            var iterator = activePets.entrySet().iterator();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                PetData data = entry.getValue();
                if (now - data.spawnedAt >= PET_LIFETIME_TICKS) {
                    for (var level : event.getServer().getAllLevels()) {
                        Entity pet = level.getEntity(entry.getKey());
                        if (pet != null) {
                            pet.discard();
                            break;
                        }
                    }
                    iterator.remove();
                    LOGGER.debug("Orphan pet cleaned up (lifetime expired)");
                }
            }
        }
    }
}
