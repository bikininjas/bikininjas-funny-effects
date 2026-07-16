package com.bikininjas.funnyeffects.item;

import com.bikininjas.corelib.log.LogManager;
import com.bikininjas.corelib.log.ModLogger;
import com.bikininjas.funnyeffects.FunnyEffectsMod;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Food item that, on consumption, emits an explosion knockback AoE around the eater.
 * Nausea II is applied via the food effect builder.
 */
public final class FlatulentBeanItem extends Item {

    private static final ModLogger LOGGER = LogManager.getLogger(FunnyEffectsMod.MOD_ID, FlatulentBeanItem.class);

    public FlatulentBeanItem(@NotNull Properties properties) {
        super(Objects.requireNonNull(properties, "properties must not be null")
                .food(new FoodProperties.Builder()
                        .nutrition(2)
                        .saturationModifier(0.2F)
                        .alwaysEdible()
                        .effect(() -> new MobEffectInstance(MobEffects.CONFUSION, 100, 1), 1.0F)
                        .build())
                .stacksTo(16));
    }

    @Override
    public @NotNull ItemStack finishUsingItem(@NotNull ItemStack stack, @NotNull Level level,
                                              @NotNull LivingEntity livingEntity) {
        ItemStack result = super.finishUsingItem(stack, level, livingEntity);
        if (!level.isClientSide() && livingEntity instanceof net.minecraft.world.entity.player.Player) {
            // Explosion knockback AoE (no block damage, mild power)
            level.explode(null, livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(),
                    2.0F, Level.ExplosionInteraction.NONE);
            LOGGER.info("Flatulent bean released by {}", livingEntity.getName().getString());
        }
        return result;
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) {
        return UseAnim.EAT;
    }
}
