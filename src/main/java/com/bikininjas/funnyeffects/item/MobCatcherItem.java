package com.bikininjas.funnyeffects.item;

import com.bikininjas.corelib.log.LogManager;
import com.bikininjas.corelib.log.ModLogger;
import com.bikininjas.funnyeffects.FunnyEffectsMod;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Item that captures a living entity into NBT and releases it later.
 * <p>
 * Right-clicking a living entity stores its {@code EntityType} ResourceLocation under the
 * {@code captured} key inside the item's {@link CustomData} component. Right-clicking a block
 * spawns the stored entity at that position and clears the data.
 */
public final class MobCatcherItem extends Item {

    private static final ModLogger LOGGER = LogManager.getLogger(FunnyEffectsMod.MOD_ID, MobCatcherItem.class);

    private static final String CAPTURED_KEY = "captured";

    public MobCatcherItem(@NotNull Properties properties) {
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
        if (hasCaptured(stack)) {
            return InteractionResult.PASS;
        }
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        CompoundTag tag = new CompoundTag();
        tag.putString(CAPTURED_KEY, id.toString());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        target.discard();
        LOGGER.info("Captured entity {} into mob catcher", id);
        return InteractionResult.SUCCESS;
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (player == null || player.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || !data.contains(CAPTURED_KEY)) {
            return InteractionResult.PASS;
        }
        ResourceLocation id = ResourceLocation.parse(data.copyTag().getString(CAPTURED_KEY));
        var entityType = BuiltInRegistries.ENTITY_TYPE.get(id);
        if (entityType == null) {
            LOGGER.error("Unknown captured entity type").ctx("id", id.toString()).report();
            return InteractionResult.FAIL;
        }
        Level level = context.getLevel();
        var spawnPos = context.getClickedPos().relative(context.getClickedFace());
        var entity = entityType.create(level);
        if (entity == null) {
            LOGGER.error("Failed to create captured entity").ctx("id", id.toString()).report();
            return InteractionResult.FAIL;
        }
        entity.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D,
                player.getYRot(), 0.0F);
        level.addFreshEntity(entity);
        stack.remove(DataComponents.CUSTOM_DATA);
        LOGGER.info("Released entity {} from mob catcher", id);
        return InteractionResult.SUCCESS;
    }

    /** Returns the captured entity type ResourceLocation, or {@code null} if empty. */
    public static ResourceLocation getCaptured(@NotNull ItemStack stack) {
        Objects.requireNonNull(stack, "stack must not be null");
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || !data.contains(CAPTURED_KEY)) {
            return null;
        }
        return ResourceLocation.parse(data.copyTag().getString(CAPTURED_KEY));
    }

    private static boolean hasCaptured(@NotNull ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.contains(CAPTURED_KEY);
    }
}
