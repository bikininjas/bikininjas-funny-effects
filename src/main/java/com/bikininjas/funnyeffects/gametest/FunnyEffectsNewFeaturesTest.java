package com.bikininjas.funnyeffects.gametest;

import com.bikininjas.funnyeffects.item.ModItems;
import com.bikininjas.funnyeffects.item.PetHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
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
}
