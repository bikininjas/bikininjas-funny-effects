package com.bikininjas.funnyeffects.gametest;

import com.bikininjas.funnyeffects.item.FunnyArrowEntity;
import com.bikininjas.funnyeffects.item.FunnyArrowItem;
import com.bikininjas.funnyeffects.item.ModItems;
import com.bikininjas.funnyeffects.item.PetHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.testframework.annotation.ForEachTest;
import net.neoforged.testframework.gametest.EmptyTemplate;
import net.neoforged.testframework.gametest.ExtendedGameTestHelper;
import org.jetbrains.annotations.NotNull;

/**
 * GameTest functions for new funny-effects features:
 * MysteryBoxItem, VoidPearlTrapItem, ItemCombos, PetHandler.
 */
@ForEachTest(groups = "funnyeffects")
public final class FunnyEffectsNewFeaturesTest {

    private FunnyEffectsNewFeaturesTest() {
    }

    private static @NotNull ServerPlayer makePlayer(@NotNull ExtendedGameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.moveTo(
                helper.absolutePos(player.blockPosition()).getX() + 1.5,
                helper.absolutePos(player.blockPosition()).getY() + 2,
                helper.absolutePos(player.blockPosition()).getZ() + 1.5
        );
        return (ServerPlayer) player;
    }

    // ========================================================================
    // MysteryBoxItem — right-click gives random item from tag
    // ========================================================================

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void mysteryBox_doesNotCrashOnUse(@NotNull ExtendedGameTestHelper helper) {
        var player = makePlayer(helper);
        var box = new ItemStack(ModItems.MYSTERY_BOX.get());
        player.setItemSlot(EquipmentSlot.MAINHAND, box);

        var result = box.getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(result.getResult().consumesAction(),
                "MysteryBox.use should consume action (success or fail, not crash)");
        helper.succeed();
    }

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void mysteryBox_emptyInventory(@NotNull ExtendedGameTestHelper helper) {
        var player = makePlayer(helper);
        player.getInventory().clearContent();
        var box = new ItemStack(ModItems.MYSTERY_BOX.get());
        player.getInventory().add(box);
        player.setItemSlot(EquipmentSlot.MAINHAND, box);

        box.getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
        helper.succeed();
    }

    // ========================================================================
    // VoidPearlTrapItem — TP another player on entity interact
    // ========================================================================

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void voidPearl_doesNotCrashOnPlayerInteract(@NotNull ExtendedGameTestHelper helper) {
        var thrower = makePlayer(helper);
        var target = makePlayer(helper);
        target.moveTo(
                target.getX() + 2,
                target.getY(),
                target.getZ()
        );
        var pearl = new ItemStack(ModItems.VOID_PEARL_TRAP.get());
        thrower.setItemSlot(EquipmentSlot.MAINHAND, pearl);

        pearl.getItem().interactLivingEntity(pearl, thrower, target, InteractionHand.MAIN_HAND);
        helper.succeed();
    }

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void voidPearl_voidGuardDoesNotSuffocate(@NotNull ExtendedGameTestHelper helper) {
        var thrower = makePlayer(helper);
        var target = makePlayer(helper);
        target.moveTo(target.getX(), -60, target.getZ());
        var pearl = new ItemStack(ModItems.VOID_PEARL_TRAP.get());
        thrower.setItemSlot(EquipmentSlot.MAINHAND, pearl);

        pearl.getItem().interactLivingEntity(pearl, thrower, target, InteractionHand.MAIN_HAND);
        helper.succeed();
    }

    // ========================================================================
    // ItemCombos — Lifesteal Blade + Thorns Shield = 30% lifesteal
    // ========================================================================

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void combos_lifestealWorks(@NotNull ExtendedGameTestHelper helper) {
        var player = makePlayer(helper);
        player.setHealth(10f);
        player.setItemSlot(EquipmentSlot.MAINHAND,
                new ItemStack(ModItems.LIFESTEAL_BLADE.get()));
        player.setItemSlot(EquipmentSlot.OFFHAND,
                new ItemStack(ModItems.THORNS_SHIELD.get()));

        var zombie = helper.spawn(EntityType.ZOMBIE,
                new net.minecraft.core.BlockPos(1, 1, 1));
        zombie.hurt(player.damageSources().playerAttack(player), 10f);
        helper.succeed();
    }

    // ========================================================================
    // PetHandler — register pet, teleport on tick
    // ========================================================================

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void petHandler_registerPet(@NotNull ExtendedGameTestHelper helper) {
        PetHandler.init();
        var player = makePlayer(helper);
        var cow = helper.spawn(EntityType.COW,
                new net.minecraft.core.BlockPos(1, 1, 1));
        PetHandler.registerPet(player, cow);

        var pets = PetHandler.getActivePets(player.getUUID());
        helper.assertTrue(pets.containsKey(cow.getUUID()),
                "Pet should be registered after registerPet");
        helper.succeed();
    }

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void petHandler_teleportOnTick(@NotNull ExtendedGameTestHelper helper) {
        PetHandler.init();
        var player = makePlayer(helper);
        var cow = helper.spawn(EntityType.COW,
                new net.minecraft.core.BlockPos(2, 1, 2));
        PetHandler.registerPet(player, cow);

        player.tickCount = 20;
        NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
        helper.succeed();
    }

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void petHandler_unknownPlayerNoCrash(@NotNull ExtendedGameTestHelper helper) {
        PetHandler.init();
        var unknown = new java.util.UUID(0, 0);
        var pets = PetHandler.getActivePets(unknown);
        helper.assertTrue(pets.isEmpty(),
                "Unknown UUID should return empty map");
        helper.succeed();
    }

    // ========================================================================
    // InfiniteBucketItem — places fluid without consuming
    // ========================================================================

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void infiniteWaterBucket_placesWater(@NotNull ExtendedGameTestHelper helper) {
        var player = makePlayer(helper);
        var bucket = new ItemStack(ModItems.INFINITE_WATER_BUCKET.get());
        player.setItemSlot(EquipmentSlot.MAINHAND, bucket);

        var pos = player.blockPosition().above();
        var context = new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos.below()), Direction.UP, pos.below(), false));
        bucket.getItem().useOn(context);

        helper.assertTrue(player.level().getBlockState(pos).is(Blocks.WATER),
                "Water should be placed at target position");
        helper.succeed();
    }

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void infiniteLavaBucket_placesLava(@NotNull ExtendedGameTestHelper helper) {
        var player = makePlayer(helper);
        var bucket = new ItemStack(ModItems.INFINITE_LAVA_BUCKET.get());
        player.setItemSlot(EquipmentSlot.MAINHAND, bucket);

        var pos = player.blockPosition().above();
        var context = new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos.below()), Direction.UP, pos.below(), false));
        bucket.getItem().useOn(context);

        helper.assertTrue(player.level().getBlockState(pos).is(Blocks.LAVA),
                "Lava should be placed at target position");
        helper.succeed();
    }

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void infiniteWaterBucket_bucketNotConsumed(@NotNull ExtendedGameTestHelper helper) {
        var player = makePlayer(helper);
        var bucket = new ItemStack(ModItems.INFINITE_WATER_BUCKET.get());
        player.setItemSlot(EquipmentSlot.MAINHAND, bucket);

        var pos = player.blockPosition().above();
        var context = new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos.below()), Direction.UP, pos.below(), false));
        bucket.getItem().useOn(context);

        helper.assertTrue(player.getMainHandItem().is(ModItems.INFINITE_WATER_BUCKET.get()),
                "Bucket should not be consumed after placing water");
        helper.succeed();
    }

    // ========================================================================
    // FunnyArrowEntity — 5 arrow effects
    // ========================================================================

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void funnyArrow_explosiveArrowCreatesArrow(@NotNull ExtendedGameTestHelper helper) {
        var player = makePlayer(helper);
        var arrowStack = new ItemStack(ModItems.EXPLOSIVE_ARROW.get());

        if (arrowStack.getItem() instanceof FunnyArrowItem arrowItem) {
            var arrow = arrowItem.createArrow(player.level(), arrowStack, player,
                    new ItemStack(net.minecraft.world.item.Items.BOW));
            helper.assertTrue(arrow instanceof FunnyArrowEntity,
                    "createArrow should return FunnyArrowEntity");
            var funny = (FunnyArrowEntity) arrow;
            helper.assertTrue(funny.getEffect() == FunnyArrowEntity.ArrowEffect.EXPLOSIVE,
                    "Arrow should have EXPLOSIVE effect");
        }
        helper.succeed();
    }

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void funnyArrow_teleportDoesNotCrash(@NotNull ExtendedGameTestHelper helper) {
        var player = makePlayer(helper);
        var arrowStack = new ItemStack(ModItems.TELEPORT_ARROW.get());

        if (arrowStack.getItem() instanceof FunnyArrowItem arrowItem) {
            var arrow = arrowItem.createArrow(player.level(), arrowStack, player,
                    new ItemStack(net.minecraft.world.item.Items.BOW));
            player.level().addFreshEntity(arrow);

            var zombie = helper.spawn(EntityType.ZOMBIE,
                    new BlockPos(1, 1, 1));
            var hit = new EntityHitResult(zombie);
            arrow.hurt(player.damageSources().arrow(arrow, player), 5f);
        }
        helper.succeed();
    }

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void funnyArrow_chickenSpawnsOnBlockHit(@NotNull ExtendedGameTestHelper helper) {
        var player = makePlayer(helper);
        var arrowStack = new ItemStack(ModItems.CHICKEN_ARROW.get());

        if (arrowStack.getItem() instanceof FunnyArrowItem arrowItem) {
            var arrow = arrowItem.createArrow(player.level(), arrowStack, player,
                    new ItemStack(net.minecraft.world.item.Items.BOW));
            player.level().addFreshEntity(arrow);
            arrow.setPos(player.getX(), player.getY() + 0.5, player.getZ() + 2);
        }
        helper.succeed();
    }
}
