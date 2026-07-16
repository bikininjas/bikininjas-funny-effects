package com.bikininjas.funnyeffects;

import com.bikininjas.corelib.log.LogManager;
import com.bikininjas.corelib.log.ModLogger;
import com.bikininjas.funnyeffects.item.ModItems;
import com.bikininjas.funnyeffects.item.ModItems.ItemEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Funny Effects mod entry point.
 * <p>
 * Registers the item {@link net.neoforged.neoforge.registries.DeferredRegister} to the mod bus
 * and the gameplay event handlers to the NeoForge event bus.
 */
@Mod(FunnyEffectsMod.MOD_ID)
public final class FunnyEffectsMod {

    public static final String MOD_ID = "funnyeffects";

    private static final ModLogger LOGGER = LogManager.getLogger(MOD_ID, FunnyEffectsMod.class);

    public FunnyEffectsMod(@NotNull IEventBus modBus) {
        Objects.requireNonNull(modBus, "modBus must not be null");

        LOGGER.info("Initializing Funny Effects mod");

        // Register all items + creative tab to the mod event bus
        ModItems.MOD_ITEMS.register(modBus);
        ModCreativeTab.CREATIVE_TABS.register(modBus);

        // Register gameplay event handlers on the NeoForge (game) event bus
        NeoForge.EVENT_BUS.register(ItemEvents.class);

        LOGGER.info("Funny Effects mod initialized");
    }
}
