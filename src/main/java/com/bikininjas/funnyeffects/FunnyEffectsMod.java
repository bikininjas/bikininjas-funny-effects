package com.bikininjas.funnyeffects;

import com.bikininjas.corelib.log.LogManager;
import com.bikininjas.corelib.log.ModLogger;
import com.bikininjas.corelib.color.ColorAPI;
import com.bikininjas.corelib.randomevent.RandomEventManager;
import com.bikininjas.corelib.randomevent.RandomEvents;
import com.bikininjas.funnyeffects.item.ArmorHandlers;
import com.bikininjas.funnyeffects.item.CombatHandlers;
import com.bikininjas.funnyeffects.item.EntityInteractHandlers;
import com.bikininjas.funnyeffects.item.GadgetHandlers;
import com.bikininjas.funnyeffects.item.ModItems;
import com.bikininjas.funnyeffects.item.ToolHandlers;
import com.bikininjas.funnyeffects.item.TooltipHandler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
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
        ToolHandlers.init();
        CombatHandlers.init();
        ArmorHandlers.init();
        GadgetHandlers.init();
        EntityInteractHandlers.init();

        // Configure random events — fires every 5-15 minutes
        var rem = RandomEventManager.getInstance();
        rem.setInterval(5 * 60 * 20, 15 * 60 * 20); // 5-15 min in ticks
        rem.setEnabled(true);

        // Register fun random events
        rem.register(RandomEvents.announceEvent("A mysterious giggle echoes from nowhere..."), "mysterious_giggle");
        rem.register(RandomEvents.announceEvent("Did something just move?! You feel watched..."), "something_moved");
        rem.register(RandomEvents.spawnEntityEvent(net.minecraft.world.entity.EntityType.CHICKEN, 10), "chicken_invasion");
        rem.register(RandomEvents.randomWeatherEvent(), "random_weather");

        TooltipHandler.init();

        ColorAPI.tintItem(modBus, ModItems.BOUNCY_SLIME, 0xFF88FF88);
        ColorAPI.tintItem(modBus, ModItems.MAGNETIC_GLOVE, 0xFF4444FF);
        ColorAPI.tintItem(modBus, ModItems.FLATULENT_BEAN, 0xFF886644);
        ColorAPI.tintItem(modBus, ModItems.SQUEAKY_TOY, 0xFFFF88FF);
        ColorAPI.tintItem(modBus, ModItems.LIFESTEAL_BLADE, 0xFFFF4444);
        ColorAPI.tintItem(modBus, ModItems.SMELTER_PICK, 0xFFFFAA44);
        ColorAPI.tintItem(modBus, ModItems.POTATO_GOGGLES, 0xFFFFDD88);
        ColorAPI.tintItem(modBus, ModItems.THORNS_SHIELD, 0xFF666666);
        ColorAPI.tintItem(modBus, ModItems.YEETER_HAMMER, 0xFFFF6644);
        ColorAPI.tintItem(modBus, ModItems.THUNDER_SWORD, 0xFFFFDD44);
        ColorAPI.tintItem(modBus, ModItems.CHICKEN_WAND, 0xFFFFFFFF);
        ColorAPI.tintItem(modBus, ModItems.RAINBOW_BOOTS, 0xFF44FFFF);
        ColorAPI.tintItem(modBus, ModItems.XP_MAGNET, 0xFF44FF44);
        ColorAPI.tintItem(modBus, ModItems.VOID_PEARL, 0xFF222244);
        ColorAPI.tintItem(modBus, ModItems.DISCO_SWORD, 0xFFFF88FF);
        ColorAPI.tintItem(modBus, ModItems.GRAVITY_PICKAXE, 0xFF4488FF);
        ColorAPI.tintItem(modBus, ModItems.BOUNCY_BOOTS, 0xFFFF6644);
        ColorAPI.tintItem(modBus, ModItems.SNEAKY_HELMET, 0xFF222222);
        ColorAPI.tintItem(modBus, ModItems.SLAPFISH, 0xFFFF8888);
        ColorAPI.tintItem(modBus, ModItems.REPLANTER_HOE, 0xFF448844);
        ColorAPI.tintItem(modBus, ModItems.DINNERBONE_BAT, 0xFF8844AA);
        ColorAPI.tintItem(modBus, ModItems.PARTY_POPPER, 0xFFFF44FF);
        ColorAPI.tintItem(modBus, ModItems.GRAVITY_ANCHOR, 0xFF884488);
        ColorAPI.tintItem(modBus, ModItems.MOB_CATCHER, 0xFF88AAFF);
        ColorAPI.tintItem(modBus, ModItems.INFINITE_PEARL, 0xFF44FF88);
        ColorAPI.tintItem(modBus, ModItems.TREECAPITATOR, 0xFF448844);
        ColorAPI.tintItem(modBus, ModItems.LAVA_WALKER, 0xFFFF8844);

        LOGGER.info("Funny Effects mod initialized");
    }
}
