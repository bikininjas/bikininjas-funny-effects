package com.bikininjas.funnyeffects.item;

import com.bikininjas.funnyeffects.FunnyEffectsMod;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Central registry of all Funny Effects items.
 * <p>
 * Items are registered via NeoForge's {@link DeferredRegister} system. Gameplay behaviour
 * (effects on eat, tick-based abilities, damage reflection, etc.) is wired through
 * {@code ToolHandlers}, {@code CombatHandlers}, {@code ArmorHandlers}, {@code GadgetHandlers},
 * and {@code EntityInteractHandlers} on the NeoForge event bus —
 * never via the deprecated {@code @EventBusSubscriber}.
 */
public final class ModItems {

    public static final DeferredRegister.Items MOD_ITEMS =
            DeferredRegister.createItems(FunnyEffectsMod.MOD_ID);

    private ModItems() {
    }

    // -- Items -----------------------------------------------------------------

    /** Food item: eat grants Jump Boost IV (30s) + Slow Falling (5s). Always edible. */
    public static final DeferredItem<Item> BOUNCY_SLIME = MOD_ITEMS.registerItem("bouncy_slime",
            props -> new Item(props
                    .food(new FoodProperties.Builder()
                            .nutrition(4)
                            .saturationModifier(0.3F)
                            .alwaysEdible()
                            .effect(() -> new MobEffectInstance(MobEffects.JUMP, 600, 3), 1.0F)
                            .effect(() -> new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0), 1.0F)
                            .build())
                    .stacksTo(16)
                    .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));

    /** Glove that magnetically attracts nearby dropped items while held. */
    public static final DeferredItem<Item> MAGNETIC_GLOVE = MOD_ITEMS.registerItem("magnetic_glove",
            props -> new Item(props.stacksTo(1).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));

    /** Food item: eat triggers an explosion knockback AoE + Nausea II (5s). Always edible. */
    public static final DeferredItem<Item> FLATULENT_BEAN = MOD_ITEMS.registerItem("flatulent_bean",
            props -> new FlatulentBeanItem(props.component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));

    /** Toy: right-click plays a bat squeak and slows nearby mobs (5 block radius). */
    public static final DeferredItem<Item> SQUEAKY_TOY = MOD_ITEMS.registerItem("squeaky_toy",
            props -> new Item(props.stacksTo(1).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));

    /** Netherite sword: heals the wielder for 20% of damage dealt. */
    public static final DeferredItem<Item> LIFESTEAL_BLADE = MOD_ITEMS.registerItem("lifesteal_blade",
            props -> new SwordItem(Tiers.NETHERITE,
                    props.attributes(SwordItem.createAttributes(Tiers.NETHERITE, 8.0F, 3.5F))
                            .stacksTo(1)
                            .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));

    /** Netherite pickaxe: auto-smelts broken block drops. */
    public static final DeferredItem<Item> SMELTER_PICK = MOD_ITEMS.registerItem("smelter_pick",
            props -> new PickaxeItem(Tiers.NETHERITE,
                    props.attributes(PickaxeItem.createAttributes(Tiers.NETHERITE, 6.0F, 1.2F))
                            .stacksTo(1)
                            .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));

    /** Iron helmet: reveals nearby hostile mobs (Glowing) while worn. */
    public static final DeferredItem<ArmorItem> POTATO_GOGGLES = MOD_ITEMS.registerItem("potato_goggles",
            props -> new ArmorItem(FunnyArmorMaterials.POTATO, ArmorItem.Type.HELMET,
                    props.stacksTo(1).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));

    /** Netherite chestplate: reflects 50% of incoming damage back to the attacker. */
    public static final DeferredItem<ArmorItem> THORNS_SHIELD = MOD_ITEMS.registerItem("thorns_shield",
            props -> new ArmorItem(FunnyArmorMaterials.THORNS, ArmorItem.Type.CHESTPLATE,
                    props.stacksTo(1).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));

    /** Netherite sword: launches the hit entity high into the air. */
    public static final DeferredItem<SwordItem> YEETER_HAMMER = MOD_ITEMS.registerItem("yeeter_hammer",
            props -> new SwordItem(Tiers.NETHERITE,
                    props.attributes(SwordItem.createAttributes(Tiers.NETHERITE, 7.0F, 3.0F))
                            .fireResistant().stacksTo(1)
                            .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));

    /** Netherite sword: strikes the hit entity with lightning. */
    public static final DeferredItem<SwordItem> THUNDER_SWORD = MOD_ITEMS.registerItem("thunder_sword",
            props -> new SwordItem(Tiers.NETHERITE,
                    props.attributes(SwordItem.createAttributes(Tiers.NETHERITE, 6.0F, 2.5F))
                            .fireResistant().stacksTo(1)
                            .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));

    /** Wand: right-click an entity to make it bawk and hop like a chicken. */
    public static final DeferredItem<Item> CHICKEN_WAND = MOD_ITEMS.registerItem("chicken_wand",
            props -> new Item(props.stacksTo(1).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));

    /** Potato-tier boots: leave a rainbow particle trail while moving. */
    public static final DeferredItem<ArmorItem> RAINBOW_BOOTS = MOD_ITEMS.registerItem("rainbow_boots",
            props -> new ArmorItem(FunnyArmorMaterials.POTATO, ArmorItem.Type.BOOTS,
                    props.stacksTo(1).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));

    /** Plain item: pulls nearby XP orbs toward the holder. */
    public static final DeferredItem<Item> XP_MAGNET = MOD_ITEMS.registerItem("xp_magnet",
            props -> new Item(props.stacksTo(1).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));

    /** Plain item: right-click to warp 20 blocks in the look direction. */
    public static final DeferredItem<Item> VOID_PEARL = MOD_ITEMS.registerItem("void_pearl",
            props -> new Item(props.stacksTo(16).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));

    // -- New items (Phase 2) ---------------------------------------------------

    /** Netherite sword: on hit, sprays disco particles and plays random notes. */
    public static final DeferredItem<SwordItem> DISCO_SWORD = MOD_ITEMS.registerItem("disco_sword",
            props -> new SwordItem(Tiers.NETHERITE,
                    props.attributes(SwordItem.createAttributes(Tiers.NETHERITE, 5, -2.4f))
                            .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));

    /** Diamond pickaxe: teleports broken block drops to the player instead of the block. */
    public static final DeferredItem<PickaxeItem> GRAVITY_PICKAXE = MOD_ITEMS.registerItem("gravity_pickaxe",
            props -> new PickaxeItem(Tiers.DIAMOND,
                    props.attributes(PickaxeItem.createAttributes(Tiers.DIAMOND, -1, -2.8f))
                            .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));

    /** Boots: negate fall damage and bounce the wearer upward. */
    public static final DeferredItem<ArmorItem> BOUNCY_BOOTS = MOD_ITEMS.registerItem("bouncy_boots",
            props -> new ArmorItem(FunnyArmorMaterials.BOUNCY, ArmorItem.Type.BOOTS,
                    props.stacksTo(1).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));

    /** Helmet: blinds the wearer and reveals nearby mobs with Glowing. */
    public static final DeferredItem<ArmorItem> SNEAKY_HELMET = MOD_ITEMS.registerItem("sneaky_helmet",
            props -> new ArmorItem(FunnyArmorMaterials.SNEAKY, ArmorItem.Type.HELMET,
                    props.stacksTo(1).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));

    /** Utility item: right-click an entity to slap it away with knockback. */
    public static final DeferredItem<Item> SLAPFISH = MOD_ITEMS.registerItem("slapfish",
            props -> new Item(props.component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));

    /** Utility item: offhand right-click on a mature crop to auto-replant it. */
    public static final DeferredItem<Item> REPLANTER_HOE = MOD_ITEMS.registerItem("replanter_hoe",
            props -> new Item(props.component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));

    /** Utility item: right-click an entity to flip its "Dinnerbone" name tag. */
    public static final DeferredItem<Item> DINNERBONE_BAT = MOD_ITEMS.registerItem("dinnerbone_bat",
            props -> new Item(props.component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));

    /** Utility item: right-click to fire a confetti burst and consume the item. */
    public static final DeferredItem<Item> PARTY_POPPER = MOD_ITEMS.registerItem("party_popper",
            props -> new Item(props.component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));

    // -- New items (Phase 5) ---------------------------------------------------

    /** Plain item: right-click to float for 5s and launch nearby mobs upward. */
    public static final DeferredItem<Item> GRAVITY_ANCHOR = MOD_ITEMS.registerItem("gravity_anchor",
            props -> new Item(props.stacksTo(1).fireResistant()
                    .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));

    /** Utility item: capture a mob into NBT and release it later. */
    public static final DeferredItem<Item> MOB_CATCHER = MOD_ITEMS.registerItem("mob_catcher",
            props -> new MobCatcherItem(props.stacksTo(1).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));

    /** Ender-pearl-like item: throws a pearl without consuming, 10% Endermite spawn. */
    public static final DeferredItem<Item> INFINITE_PEARL = MOD_ITEMS.registerItem("infinite_pearl",
            props -> new Item(props.stacksTo(1).fireResistant()
                    .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));

    /** Diamond axe: breaks the whole connected log tree at once. */
    public static final DeferredItem<net.minecraft.world.item.AxeItem> TREECAPITATOR =
            MOD_ITEMS.registerItem("treecapitator",
                    props -> new net.minecraft.world.item.AxeItem(Tiers.DIAMOND,
                            props.attributes(net.minecraft.world.item.AxeItem.createAttributes(Tiers.DIAMOND, 5.0F, 1.0F))
                                    .stacksTo(1)
                                    .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));

    /** Netherite-tier boots: turn lava to obsidian while sprinting. */
    public static final DeferredItem<ArmorItem> LAVA_WALKER = MOD_ITEMS.registerItem("lava_walker",
            props -> new ArmorItem(FunnyArmorMaterials.LAVA_WALKER, ArmorItem.Type.BOOTS,
                    props.stacksTo(1).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));

    // -- New items (Phase 6) ---------------------------------------------------

    /** Craftable item: right-click to get a random item from the mystery_box_loot tag. */
    public static final DeferredItem<Item> MYSTERY_BOX = MOD_ITEMS.registerItem("mystery_box",
            props -> new MysteryBoxItem(props.stacksTo(1).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));

    /** Ender-pearl variant: right-click on a player to teleport them to your cursor. */
    public static final DeferredItem<Item> VOID_PEARL_TRAP = MOD_ITEMS.registerItem("void_pearl_trap",
            props -> new VoidPearlTrapItem(props.stacksTo(8).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));
}
