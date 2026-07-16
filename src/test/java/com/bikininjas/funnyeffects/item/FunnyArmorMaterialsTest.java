package com.bikininjas.funnyeffects.item;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for {@link FunnyArmorMaterials} defense values.
 * <p>
 * The {@code defense()} helper is private, so we verify the resulting {@link ArmorMaterial}
 * via the public {@link Holder} fields and the record's {@link ArmorMaterial#defense()} accessor.
 * The materials are registered into {@code BuiltInRegistries.ARMOR_MATERIAL} at class-load time,
 * so {@code Holder#value()} is resolvable in a plain unit test.
 */
class FunnyArmorMaterialsTest {

    @Test
    void potato_defense_mapsHelmetOnly() {
        assertDefense(FunnyArmorMaterials.POTATO.value(), 2, 0, 0, 0);
    }

    @Test
    void thorns_defense_mapsChestplateOnly() {
        assertDefense(FunnyArmorMaterials.THORNS.value(), 0, 8, 0, 0);
    }

    @Test
    void bouncy_defense_mapsBootsOnly() {
        assertDefense(FunnyArmorMaterials.BOUNCY.value(), 0, 0, 0, 2);
    }

    @Test
    void sneaky_defense_mapsHelmetOnly() {
        assertDefense(FunnyArmorMaterials.SNEAKY.value(), 1, 0, 0, 0);
    }

    @Test
    void materials_exposeNonNullDefense() {
        for (var material : new ArmorMaterial[]{
                FunnyArmorMaterials.POTATO.value(),
                FunnyArmorMaterials.THORNS.value(),
                FunnyArmorMaterials.BOUNCY.value(),
                FunnyArmorMaterials.SNEAKY.value()}) {
            Map<ArmorItem.Type, Integer> map = material.defense();
            assertNotNull(map, "defense map must not be null");
        }
    }

    private static void assertDefense(@NotNull ArmorMaterial material,
                                      int helmet, int chestplate, int leggings, int boots) {
        Objects.requireNonNull(material, "material must not be null");
        Map<ArmorItem.Type, Integer> map = material.defense();
        assertEquals(helmet, defenseOf(map, ArmorItem.Type.HELMET), "helmet defense mismatch");
        assertEquals(chestplate, defenseOf(map, ArmorItem.Type.CHESTPLATE), "chestplate defense mismatch");
        assertEquals(leggings, defenseOf(map, ArmorItem.Type.LEGGINGS), "leggings defense mismatch");
        assertEquals(boots, defenseOf(map, ArmorItem.Type.BOOTS), "boots defense mismatch");
    }

    private static int defenseOf(@NotNull Map<ArmorItem.Type, Integer> map, @NotNull ArmorItem.Type type) {
        Integer value = map.get(type);
        return value == null ? 0 : value;
    }
}
