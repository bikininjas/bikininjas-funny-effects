package com.bikininjas.funnyeffects.item;

import com.bikininjas.corelib.log.LogManager;
import com.bikininjas.corelib.log.ModLogger;
import com.bikininjas.funnyeffects.FunnyEffectsMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Right-click on another player to teleport them to the block you are looking at.
 * Consumes the pearl on use. Max stack 8.
 */
public final class VoidPearlTrapItem extends Item {

    private static final ModLogger LOGGER = LogManager.getLogger(FunnyEffectsMod.MOD_ID, VoidPearlTrapItem.class);

    public VoidPearlTrapItem(@NotNull Properties properties) {
        super(Objects.requireNonNull(properties, "properties must not be null"));
    }

    @Override
    public @NotNull InteractionResult interactLivingEntity(@NotNull ItemStack stack,
                                                            @NotNull Player player,
                                                            @NotNull LivingEntity target,
                                                            @NotNull InteractionHand hand) {
        if (player.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(target instanceof ServerPlayer targetPlayer)) {
            return InteractionResult.PASS;
        }
        Vec3 look = player.getLookAngle().scale(20.0D);
        Vec3 dest = player.position().add(look.x, look.y, look.z);
        targetPlayer.teleportTo((ServerLevel) player.level(), dest.x, dest.y, dest.z,
                targetPlayer.getYRot(), targetPlayer.getXRot());
        targetPlayer.level().playSound(null, targetPlayer.blockPosition(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.5F);
        stack.shrink(1);
        LOGGER.info("Player {} void-trapped {}", player.getName().getString(),
                targetPlayer.getName().getString());
        return InteractionResult.SUCCESS;
    }
}
