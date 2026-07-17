package com.bikininjas.funnyeffects.item;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Custom arrow with one of five effects triggered on hit.
 */
public final class FunnyArrowEntity extends Arrow {

    public enum ArrowEffect {
        EXPLOSIVE,
        TELEPORT,
        CHICKEN,
        CONFUSION,
        LIGHTNING
    }

    private ArrowEffect effect = ArrowEffect.EXPLOSIVE;

    public FunnyArrowEntity(@NotNull EntityType<? extends Arrow> type, @NotNull Level level) {
        super(type, level);
    }

    public FunnyArrowEntity(@NotNull Level level, @NotNull LivingEntity shooter,
                            @NotNull ItemStack pickup, @Nullable ItemStack weapon) {
        super(level, shooter, pickup, weapon);
    }

    public void setEffect(@NotNull ArrowEffect effect) {
        this.effect = Objects.requireNonNull(effect, "effect must not be null");
    }

    public @NotNull ArrowEffect getEffect() {
        return effect;
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("funnyEffect", effect.name());
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("funnyEffect")) {
            try {
                effect = ArrowEffect.valueOf(tag.getString("funnyEffect"));
            } catch (IllegalArgumentException e) {
                effect = ArrowEffect.EXPLOSIVE;
            }
        }
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult result) {
        super.onHitEntity(result);
        if (level().isClientSide()) return;
        if (inGround) return;

        Entity hit = result.getEntity();
        applyEffect((ServerLevel) level(), hit);
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult result) {
        super.onHitBlock(result);
        if (level().isClientSide()) return;

        applyEffect((ServerLevel) level(), null);
    }

    private void applyEffect(@NotNull ServerLevel level, @Nullable Entity hit) {
        switch (effect) {
            case EXPLOSIVE -> {
                level.explode(this, getX(), getY(), getZ(), 2.0F,
                        Level.ExplosionInteraction.TNT);
            }
            case TELEPORT -> {
                if (hit instanceof LivingEntity && getOwner() instanceof LivingEntity owner
                        && hit != owner) {
                    double hx = hit.getX();
                    double hy = hit.getY();
                    double hz = hit.getZ();
                    double ox = owner.getX();
                    double oy = owner.getY();
                    double oz = owner.getZ();

                    // Safety: avoid teleporting into void or solid blocks
                    if (oy > level.getMinBuildHeight()
                            && level.getBlockState(owner.blockPosition()).isAir()) {
                        hit.teleportTo(ox, oy, oz);
                    }
                    if (hy > level.getMinBuildHeight()
                            && level.getBlockState(hit.blockPosition()).isAir()) {
                        owner.teleportTo(hx, hy, hz);
                    }

                    level.playSound(null, hx, hy, hz,
                            SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
                    level.playSound(null, ox, oy, oz,
                            SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
                }
            }
            case CHICKEN -> {
                for (int i = 0; i < 3; i++) {
                    Chicken chicken = EntityType.CHICKEN.create(level);
                    if (chicken != null) {
                        double spawnY = Math.max(getY() + 0.5, level.getMinBuildHeight() + 1);
                        chicken.moveTo(getX(), spawnY, getZ(),
                                random.nextFloat() * 360.0F, 0.0F);
                        level.addFreshEntity(chicken);
                    }
                }
                level.playSound(null, getX(), getY(), getZ(),
                        SoundEvents.CHICKEN_AMBIENT, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            case CONFUSION -> {
                if (hit instanceof LivingEntity living) {
                    living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 1,
                            true, false));
                    living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2,
                            true, false));
                }
                level.playSound(null, getX(), getY(), getZ(),
                        SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            case LIGHTNING -> {
                BlockPos pos = hit != null ? hit.blockPosition() : this.blockPosition();
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt != null) {
                    bolt.moveTo(pos.getX(), pos.getY(), pos.getZ());
                    level.addFreshEntity(bolt);
                }
            }
        }
        discard();
    }

    @Override
    protected @NotNull ItemStack getDefaultPickupItem() {
        return ItemStack.EMPTY;
    }
}
