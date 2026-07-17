package com.bikininjas.funnyeffects.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Arrow item that creates a {@link FunnyArrowEntity} with the specified
 * {@link FunnyArrowEntity.ArrowEffect} when fired from a bow or crossbow.
 */
public final class FunnyArrowItem extends ArrowItem {

    private final FunnyArrowEntity.ArrowEffect effect;

    public FunnyArrowItem(@NotNull Properties properties,
                          @NotNull FunnyArrowEntity.ArrowEffect effect) {
        super(properties);
        this.effect = Objects.requireNonNull(effect, "effect must not be null");
    }

    @Override
    public @NotNull AbstractArrow createArrow(@NotNull Level level,
                                              @NotNull ItemStack ammo,
                                              @NotNull LivingEntity shooter,
                                              @Nullable ItemStack weapon) {
        var arrow = new FunnyArrowEntity(level, shooter, ammo.copy(),
                weapon != null ? weapon : ItemStack.EMPTY);
        arrow.setEffect(effect);
        return arrow;
    }
}
