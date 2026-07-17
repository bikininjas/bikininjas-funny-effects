package com.bikininjas.funnyeffects.item;

import com.bikininjas.corelib.log.LogManager;
import com.bikininjas.corelib.log.ModLogger;
import com.bikininjas.funnyeffects.FunnyEffectsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

/**
 * Tool-related event handlers for Funny Effects items: Smelter Pick, Gravity Pickaxe,
 * Replanter Hoe, and Treecapitator. Registered on the NeoForge event bus.
 */
public final class ToolHandlers {

    private static final ModLogger LOGGER = LogManager.getLogger(FunnyEffectsMod.MOD_ID, ToolHandlers.class);
    private static final Random RANDOM = new Random();

    private ToolHandlers() {
    }

    static {
        NeoForge.EVENT_BUS.register(ToolHandlers.class);
    }

    /** Force class loading so the static initializer runs. */
    public static void init() {
    }

    /**
     * Smelter pick: auto-smelt drops when a block is broken by a player holding the pick.
     */
    @SubscribeEvent
    public static void onBlockBreak(@NotNull BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player.level().isClientSide()) {
            return;
        }
        if (!isHolding(player, ModItems.SMELTER_PICK.get())) {
            return;
        }
        Level level = player.level();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockPos pos = event.getPos();
        BlockState state = event.getState();
        // Compute the block's normal drops, smelt each one, and spawn the result.
        List<ItemStack> drops = Block.getDrops(state, serverLevel, pos, level.getBlockEntity(pos));
        var recipeManager = serverLevel.getRecipeManager();
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) {
                continue;
            }
            var recipe = recipeManager.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING,
                    new net.minecraft.world.item.crafting.SingleRecipeInput(drop), serverLevel);
            if (recipe.isPresent()) {
                ItemStack smelted = recipe.get().value().getResultItem(serverLevel.registryAccess());
                if (!smelted.isEmpty()) {
                    int count = smelted.getCount() * drop.getCount();
                    serverLevel.addFreshEntity(new ItemEntity(serverLevel, pos.getX() + 0.5D,
                            pos.getY() + 0.5D, pos.getZ() + 0.5D,
                            new ItemStack(smelted.getItem(), count)));
                    continue;
                }
            }
            serverLevel.addFreshEntity(new ItemEntity(serverLevel, pos.getX() + 0.5D,
                    pos.getY() + 0.5D, pos.getZ() + 0.5D, drop));
        }
        // Cancel vanilla drops to avoid duplicates
        event.setCanceled(true);
    }

    /**
     * Replanter hoe: offhand right-click on a mature crop harvests it and replants at age 0.
     */
    @SubscribeEvent
    public static void replanterHoe_onRightClick(@NotNull PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level level = player.level();
        if (level.isClientSide()) {
            return;
        }
        if (event.getHand() != InteractionHand.OFF_HAND) {
            return;
        }
        if (!isHolding(player, ModItems.REPLANTER_HOE.get())) {
            return;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof CropBlock cropBlock)) {
            return;
        }
        int maxAge = cropBlock.getMaxAge();
        if (state.getValue(CropBlock.AGE) != maxAge) {
            return;
        }
        // Collect the crop's drops.
        List<ItemStack> drops = Block.getDrops(state, serverLevel, pos, level.getBlockEntity(pos));
        // Break the block without dropping items (we spawn them manually).
        level.destroyBlock(pos, false);
        // Replant at age 0.
        level.setBlock(pos, cropBlock.defaultBlockState().setValue(CropBlock.AGE, 0), 3);
        // Spawn the harvested drops at the player.
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) {
                continue;
            }
            ItemEntity itemEntity = new ItemEntity(level, player.getX(), player.getY(), player.getZ(), drop);
            level.addFreshEntity(itemEntity);
        }
        event.setCanceled(true);
        LOGGER.info("Replanter hoe harvested crop at {}", pos);
    }

    /**
     * Gravity pickaxe: broken block drops are teleported to the player instead of the block.
     */
    @SubscribeEvent
    public static void gravityPickaxe_onBreak(@NotNull BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        Level level = player.level();
        if (level.isClientSide()) {
            return;
        }
        if (!isHolding(player, ModItems.GRAVITY_PICKAXE.get())) {
            return;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockPos pos = event.getPos();
        BlockState state = event.getState();
        // Cancel vanilla drops to avoid duplicates at the block position.
        List<ItemStack> drops = Block.getDrops(state, serverLevel, pos,
                level.getBlockEntity(pos), player, player.getMainHandItem());
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) {
                continue;
            }
            ItemEntity itemEntity = new ItemEntity(level, player.getX(), player.getY(), player.getZ(), drop);
            level.addFreshEntity(itemEntity);
        }
        event.setCanceled(true);
        LOGGER.info("Gravity pickaxe teleported drops to {}", player.getName().getString());
    }

    /**
     * Treecapitator: breaking a log while holding the axe breaks the whole connected tree.
     */
    @SubscribeEvent
    public static void treecapitator_onBreak(@NotNull BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        Level level = player.level();
        if (level.isClientSide()) {
            return;
        }
        if (!isHolding(player, ModItems.TREECAPITATOR.get())) {
            return;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockPos start = event.getPos();
        BlockState startState = event.getState();
        if (!startState.is(BlockTags.LOGS)) {
            return;
        }
        var targetBlock = startState.getBlock();
        Deque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(start);
        visited.add(start);
        List<BlockPos> tree = new ArrayList<>();
        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            tree.add(current);
            for (var dir : Direction.values()) {
                if (dir.getAxis() == Direction.Axis.Y
                        || dir.getAxis() == Direction.Axis.X
                        || dir.getAxis() == Direction.Axis.Z) {
                    BlockPos neighbor = current.relative(dir);
                    if (visited.contains(neighbor)) {
                        continue;
                    }
                    BlockState neighborState = level.getBlockState(neighbor);
                    if (neighborState.getBlock() == targetBlock && neighborState.is(BlockTags.LOGS)) {
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
        }
        for (BlockPos pos : tree) {
            BlockState state = level.getBlockState(pos);
            List<ItemStack> drops = Block.getDrops(state, serverLevel, pos,
                    level.getBlockEntity(pos), player, player.getMainHandItem());
            level.destroyBlock(pos, false);
            for (ItemStack drop : drops) {
                if (!drop.isEmpty()) {
                    serverLevel.addFreshEntity(new ItemEntity(serverLevel,
                            pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, drop));
                }
            }
        }
        event.setCanceled(true);
        LOGGER.info("Treecapitator felled {} logs", tree.size());
    }

    private static boolean isHolding(@NotNull LivingEntity entity, @NotNull Item item) {
        Objects.requireNonNull(entity, "entity must not be null");
        Objects.requireNonNull(item, "item must not be null");
        return entity.getMainHandItem().is(item) || entity.getOffhandItem().is(item);
    }
}
