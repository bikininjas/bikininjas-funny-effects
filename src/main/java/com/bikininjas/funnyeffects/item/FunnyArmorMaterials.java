package com.bikininjas.funnyeffects.item;

import com.bikininjas.funnyeffects.FunnyEffectsMod;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Armor materials for Funny Effects custom armor. In NeoForge 1.21.1 {@link ArmorMaterial}
 * is an immutable record, so materials are built as {@link Holder<ArmorMaterial>} via the
 * registry (no deprecated vanilla enum usage).
 */
public final class FunnyArmorMaterials {

    private FunnyArmorMaterials() {
    }

    /** Iron-tier helmet material (2 armor, 2.5 toughness). */
    public static final Holder<ArmorMaterial> POTATO = register("potato",
            defense(2, 0, 0, 0), 9, SoundEvents.ARMOR_EQUIP_IRON, 0.0F, 2.5F,
            () -> Ingredient.of(Items.POTATO));

    /** Netherite-tier chestplate material (8 armor, 4 toughness). */
    public static final Holder<ArmorMaterial> THORNS = register("thorns",
            defense(0, 8, 0, 0), 15, SoundEvents.ARMOR_EQUIP_NETHERITE, 3.0F, 4.0F,
            () -> Ingredient.of(Items.NETHERITE_INGOT));

    /** Iron-tier boots material (2 armor boots only). Bouncy effect. */
    public static final Holder<ArmorMaterial> BOUNCY = register("bouncy",
            defense(0, 0, 0, 2), 9, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 2.5F,
            () -> Ingredient.of(Items.SLIME_BALL));

    /** Gold-tier helmet material (1 armor helmet only). Sneaky effect. */
    public static final Holder<ArmorMaterial> SNEAKY = register("sneaky",
            defense(1, 0, 0, 0), 25, SoundEvents.ARMOR_EQUIP_GOLD, 0.0F, 0.0F,
            () -> Ingredient.of(Items.PHANTOM_MEMBRANE));

    private static @NotNull EnumMap<ArmorItem.Type, Integer> defense(int helmet, int chestplate,
                                                                      int leggings, int boots) {
        EnumMap<ArmorItem.Type, Integer> map = new EnumMap<>(ArmorItem.Type.class);
        map.put(ArmorItem.Type.HELMET, helmet);
        map.put(ArmorItem.Type.CHESTPLATE, chestplate);
        map.put(ArmorItem.Type.LEGGINGS, leggings);
        map.put(ArmorItem.Type.BOOTS, boots);
        return map;
    }

    private static @NotNull Holder<ArmorMaterial> register(@NotNull String name,
                                                            @NotNull EnumMap<ArmorItem.Type, Integer> defense,
                                                            int enchantmentValue,
                                                            @NotNull Holder<SoundEvent> equipSound,
                                                            float toughness, float knockbackResistance,
                                                            @NotNull Supplier<Ingredient> repairIngredient) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(defense, "defense must not be null");
        Objects.requireNonNull(equipSound, "equipSound must not be null");
        Objects.requireNonNull(repairIngredient, "repairIngredient must not be null");
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(FunnyEffectsMod.MOD_ID, name);
        ArmorMaterial material = new ArmorMaterial(defense, enchantmentValue, equipSound,
                repairIngredient, List.of(new ArmorMaterial.Layer(location)), toughness, knockbackResistance);
        return Registry.registerForHolder(BuiltInRegistries.ARMOR_MATERIAL, location, material);
    }
}
