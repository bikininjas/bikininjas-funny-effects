package com.bikininjas.funnyeffects.item;

import com.bikininjas.corelib.log.LogManager;
import com.bikininjas.corelib.log.ModLogger;
import com.bikininjas.funnyeffects.FunnyEffectsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Infinite water or lava bucket — places the fluid without being consumed.
 * <p>
 * Water bucket: 5-tick cooldown (0.25s, safe for rapid use).
 * Lava bucket: 100-tick cooldown (5s, prevents grief).
 */
public final class InfiniteBucketItem extends Item {

    private static final ModLogger LOGGER = LogManager.getLogger(FunnyEffectsMod.MOD_ID,
            InfiniteBucketItem.class);

    public enum FluidType {
        WATER(Fluids.WATER, Blocks.WATER, SoundEvents.BUCKET_EMPTY, 5),
        LAVA(Fluids.LAVA, Blocks.LAVA, SoundEvents.BUCKET_EMPTY_LAVA, 100);

        final Fluid fluid;
        final Block block;
        final net.minecraft.sounds.SoundEvent sound;
        final int cooldownTicks;

        FluidType(Fluid fluid, Block block,
                  net.minecraft.sounds.SoundEvent sound, int cooldownTicks) {
            this.fluid = fluid;
            this.block = block;
            this.sound = sound;
            this.cooldownTicks = cooldownTicks;
        }
    }

    private final FluidType fluidType;

    public InfiniteBucketItem(@NotNull Properties properties, @NotNull FluidType fluidType) {
        super(Objects.requireNonNull(properties, "properties must not be null"));
        this.fluidType = Objects.requireNonNull(fluidType, "fluidType must not be null");
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        Direction face = context.getClickedFace();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.FAIL;
        }

        BlockPos placePos = clickedPos.relative(face);
        BlockState existing = level.getBlockState(placePos);

        if (!existing.isAir() && existing.getFluidState().isEmpty()
                && !(existing.getBlock() instanceof LiquidBlockContainer)
                && !(existing.getBlock() instanceof BucketPickup)) {
            return InteractionResult.FAIL;
        }

        if (level.dimension().equals(Level.NETHER) && fluidType == FluidType.WATER) {
            level.playSound(player, placePos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS,
                    1.0F, 1.0F);
            return InteractionResult.FAIL;
        }

        level.setBlock(placePos, fluidType.block.defaultBlockState(), Block.UPDATE_ALL_IMMEDIATE);
        level.playSound(player, placePos, fluidType.sound, SoundSource.BLOCKS, 1.0F, 1.0F);

        if (!player.isCreative()) {
            player.getCooldowns().addCooldown(this, fluidType.cooldownTicks);
        }

        LOGGER.debug("{} placed {} at {} by {}", fluidType.name(),
                fluidType.block.getDescriptionId(), placePos, player.getName().getString());
        return InteractionResult.SUCCESS;
    }
}
