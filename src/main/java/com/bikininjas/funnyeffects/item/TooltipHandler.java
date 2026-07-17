package com.bikininjas.funnyeffects.item;

import com.bikininjas.corelib.log.LogManager;
import com.bikininjas.corelib.log.ModLogger;
import com.bikininjas.funnyeffects.FunnyEffectsMod;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;
import java.util.Map;

/**
 * Adds descriptive tooltips to all Funny Effects items so players can
 * understand what each item does before using it.
 */
public final class TooltipHandler {

    private static final ModLogger LOGGER = LogManager.getLogger(FunnyEffectsMod.MOD_ID, TooltipHandler.class);

    private static final Map<String, String[]> TOOLTIPS = Map.<String, String[]>ofEntries(
            Map.entry("bouncy_slime", new String[]{"§a◆ Food", "§7Jump Boost IV (30s) + Slow Falling"}),
            Map.entry("flatulent_bean", new String[]{"§a◆ Food", "§7Explosive knockback + Nausea II"}),
            Map.entry("magnetic_glove", new String[]{"§9◆ Gadget", "§7Pulls nearby items toward you"}),
            Map.entry("squeaky_toy", new String[]{"§9◆ Gadget", "§7Slows nearby mobs in 5 block radius", "§7Cooldown: 1.5s"}),
            Map.entry("xp_magnet", new String[]{"§9◆ Gadget", "§7Pulls XP orbs toward holder"}),
            Map.entry("void_pearl", new String[]{"§9◆ Gadget", "§7Warps 20 blocks in look direction", "§7Cooldown: 1.5s"}),
            Map.entry("gravity_anchor", new String[]{"§9◆ Gadget", "§7Float for 5s + launch nearby mobs", "§7Cooldown: 15s"}),
            Map.entry("party_popper", new String[]{"§9◆ Gadget", "§7Confetti burst — consumed on use", "§7Cooldown: 1.5s"}),
            Map.entry("infinite_pearl", new String[]{"§9◆ Gadget", "§7Infinite ender pearl, 10% Endermite spawn", "§7Cooldown: 1s"}),
            Map.entry("lifesteal_blade", new String[]{"§c◆ Combat", "§7Heals 20% of damage dealt"}),
            Map.entry("thorns_shield", new String[]{"§c◆ Combat — Chestplate", "§7Reflects 50% damage to attacker"}),
            Map.entry("thunder_sword", new String[]{"§c◆ Combat", "§7Strikes target with lightning"}),
            Map.entry("yeeter_hammer", new String[]{"§c◆ Combat", "§7Launches hit entity high into the air"}),
            Map.entry("disco_sword", new String[]{"§c◆ Combat", "§7Disco particles + note sounds on hit", "§7Cooldown: 1.5s"}),
            Map.entry("slapfish", new String[]{"§c◆ Combat", "§7Knocks enemies away — no damage", "§7Cooldown: 1.5s"}),
            Map.entry("smelter_pick", new String[]{"§6◆ Tool", "§7Auto-smelts broken block drops"}),
            Map.entry("gravity_pickaxe", new String[]{"§6◆ Tool", "§7Teleports drops directly to you"}),
            Map.entry("replanter_hoe", new String[]{"§6◆ Tool", "§7Harvests + replants crops (offhand)"}),
            Map.entry("treecapitator", new String[]{"§6◆ Tool", "§7Breaks entire connected tree at once"}),
            Map.entry("potato_goggles", new String[]{"§5◆ Armor — Helmet", "§7Defense: +2", "§7Reveals nearby hostiles with Glowing"}),
            Map.entry("rainbow_boots", new String[]{"§5◆ Armor — Boots", "§7Defense: +2", "§7Rainbow particle trail while moving"}),
            Map.entry("bouncy_boots", new String[]{"§5◆ Armor — Boots", "§7Defense: +2", "§7Negates all fall damage + bounce"}),
            Map.entry("sneaky_helmet", new String[]{"§5◆ Armor — Helmet", "§7Defense: +1", "§7Blinds you + reveals mobs around"}),
            Map.entry("lava_walker", new String[]{"§5◆ Armor — Boots", "§7Defense: +2", "§7Turns lava to obsidian while sprinting"}),
            Map.entry("chicken_wand", new String[]{"§d◆ Interaction", "§7Makes mobs bawk like chickens", "§7Cooldown: 1.5s"}),
            Map.entry("dinnerbone_bat", new String[]{"§d◆ Interaction", "§7Flips entity upside-down (Dinnerbone)", "§7Cooldown: 1.5s"}),
            Map.entry("mob_catcher", new String[]{"§d◆ Interaction", "§7Captures + releases any living mob"})
    );

    static {
        NeoForge.EVENT_BUS.register(TooltipHandler.class);
    }

    private TooltipHandler() {
    }

    public static void init() {
    }

    @SubscribeEvent
    static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;

        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        String[] lines = TOOLTIPS.get(path);
        if (lines == null) return;

        List<Component> tooltip = event.getToolTip();
        for (String line : lines) {
            tooltip.add(Component.literal(line.substring(2)).withStyle(
                    line.startsWith("§a") ? ChatFormatting.GREEN :
                    line.startsWith("§c") ? ChatFormatting.RED :
                    line.startsWith("§9") ? ChatFormatting.BLUE :
                    line.startsWith("§6") ? ChatFormatting.GOLD :
                    line.startsWith("§5") ? ChatFormatting.DARK_PURPLE :
                    line.startsWith("§d") ? ChatFormatting.LIGHT_PURPLE :
                    line.startsWith("§7") ? ChatFormatting.GRAY :
                    ChatFormatting.WHITE
            ));
        }
    }
}
