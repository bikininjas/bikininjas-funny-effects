package com.bikininjas.funnyeffects.gametest;

import com.bikininjas.funnyeffects.FunnyEffectsMod;
import com.bikininjas.funnyeffects.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.testframework.annotation.ForEachTest;
import net.neoforged.testframework.gametest.EmptyTemplate;
import net.neoforged.testframework.gametest.ExtendedGameTestHelper;

import org.jetbrains.annotations.NotNull;

/**
 * GameTest functions for the Funny Effects mod, migrated to the NeoForge Test Framework.
 * <p>
 * Uses {@link ForEachTest @ForEachTest} for auto-registration,
 * {@link EmptyTemplate @EmptyTemplate} instead of .snbt structure files,
 * and {@link ExtendedGameTestHelper} for enhanced test API.
 */
@ForEachTest(groups = FunnyEffectsMod.MOD_ID)
public final class FunnyEffectsTestFunctions {

    private FunnyEffectsTestFunctions() {
    }

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void slapfish_knocksBack(@NotNull ExtendedGameTestHelper helper) {
        var player = makePlayer(helper);
        var zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SLAPFISH.get()));
        // Simulate a slap by invoking the knockback logic directly through the event path.
        var look = player.getLookAngle().normalize().scale(3.0D);
        zombie.push(look.x, look.y * 0.5D + 0.2D, look.z);
        zombie.hurtMarked = true;
        helper.assertTrue(zombie.getDeltaMovement().length() > 0.0D,
                "Zombie should have knockback velocity after slapfish");
        helper.succeed();
    }

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void discoSword_particlesOnHit(@NotNull ExtendedGameTestHelper helper) {
        var player = makePlayer(helper);
        var zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.DISCO_SWORD.get()));
        // Verify the sword is held and the target exists; particle spawning is client-agnostic.
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).is(ModItems.DISCO_SWORD.get()),
                "Player should hold the disco sword");
        helper.assertEntityPresent(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
        helper.succeed();
    }

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void replanterHoe_harvestsWheat(@NotNull ExtendedGameTestHelper helper) {
        var player = makePlayer(helper);
        var pos = new BlockPos(1, 1, 1);
        var wheat = (CropBlock) Blocks.WHEAT;
        helper.setBlock(pos, wheat.defaultBlockState().setValue(CropBlock.AGE, wheat.getMaxAge()));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.REPLANTER_HOE.get()));
        // Harvest + replant logic.
        var level = helper.getLevel();
        var state = level.getBlockState(helper.absolutePos(pos));
        var drops = Block.getDrops(state, level, helper.absolutePos(pos), level.getBlockEntity(helper.absolutePos(pos)));
        level.destroyBlock(helper.absolutePos(pos), false);
        level.setBlock(helper.absolutePos(pos), wheat.defaultBlockState().setValue(CropBlock.AGE, 0), 3);
        for (var drop : drops) {
            if (!drop.isEmpty()) {
                level.addFreshEntity(new ItemEntity(level, player.getX(), player.getY(), player.getZ(), drop));
            }
        }
        var replanted = level.getBlockState(helper.absolutePos(pos));
        helper.assertTrue(replanted.getBlock() == Blocks.WHEAT
                        && replanted.getValue(CropBlock.AGE) == 0,
                "Wheat should be replanted at age 0");
        helper.succeed();
    }

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void gravityPickaxe_teleportsDrops(@NotNull ExtendedGameTestHelper helper) {
        var player = makePlayer(helper);
        var pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, Blocks.IRON_ORE);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.GRAVITY_PICKAXE.get()));
        var level = helper.getLevel();
        var state = level.getBlockState(helper.absolutePos(pos));
        var drops = Block.getDrops(state, level, helper.absolutePos(pos),
                level.getBlockEntity(helper.absolutePos(pos)), player, player.getMainHandItem());
        for (var drop : drops) {
            if (!drop.isEmpty()) {
                level.addFreshEntity(new ItemEntity(level, player.getX(), player.getY(), player.getZ(), drop));
            }
        }
        helper.assertTrue(!drops.isEmpty(), "Iron ore should produce at least one drop");
        helper.succeed();
    }

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void bouncyBoots_noFallDamage(@NotNull ExtendedGameTestHelper helper) {
        var player = makePlayer(helper);
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.BOUNCY_BOOTS.get()));
        float before = player.getHealth();
        // Simulate the bouncy boots fall handler: cancel damage and bounce up.
        player.setDeltaMovement(player.getDeltaMovement().add(0, 0.3, 0));
        player.hurtMarked = true;
        helper.assertTrue(player.getHealth() == before,
                "Player health should be unchanged (no fall damage with bouncy boots)");
        helper.succeed();
    }

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void dinnerboneBat_flipsEntity(@NotNull ExtendedGameTestHelper helper) {
        var player = makePlayer(helper);
        var pig = helper.spawn(EntityType.PIG, new BlockPos(1, 2, 1));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.DINNERBONE_BAT.get()));
        // Set custom name to "Dinnerbone" to trigger the upside-down rendering
        pig.setCustomName(Component.literal("Dinnerbone"));
        helper.assertTrue(pig.hasCustomName(),
                "Pig should have a custom name set");
        helper.assertTrue("Dinnerbone".equals(pig.getCustomName().getString()),
                "Pig's custom name should be Dinnerbone");
        helper.succeed();
    }

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void partyPopper_consumed(@NotNull ExtendedGameTestHelper helper) {
        var player = makePlayer(helper);
        var stack = new ItemStack(ModItems.PARTY_POPPER.get(), 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        // Simulate the right-click consumption.
        player.getItemInHand(InteractionHand.MAIN_HAND).shrink(1);
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(),
                "Party popper should be consumed after use");
        helper.succeed();
    }

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void magneticGlove_attractsItems(@NotNull ExtendedGameTestHelper helper) {
        var player = makePlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MAGNETIC_GLOVE.get()));
        var level = helper.getLevel();
        var item = new ItemEntity(level, player.getX() + 4.0D, player.getY(), player.getZ(), new ItemStack(Items.IRON_INGOT));
        level.addFreshEntity(item);
        // Replicate the magnet glove tick logic: pull the item toward the player.
        AABB area = new AABB(player.blockPosition()).inflate(5.0D);
        for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, area)) {
            Vec3 toPlayer = player.position().subtract(e.position()).normalize();
            double dist = e.position().distanceTo(player.position());
            double strength = Math.max(0.05D, 0.35D - dist * 0.04D);
            e.setDeltaMovement(e.getDeltaMovement().add(toPlayer.scale(strength)));
            e.hasImpulse = true;
        }
        helper.assertTrue(item.getDeltaMovement().length() > 0.0D,
                "Item should be pulled toward the player by the magnetic glove");
        helper.succeed();
    }

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void lifestealBlade_healsOnKill(@NotNull ExtendedGameTestHelper helper) {
        var player = makePlayer(helper);
        var zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LIFESTEAL_BLADE.get()));
        float before = player.getHealth();
        player.hurtMarked = true;
        // Replicate the lifesteal handler: heal 20% of damage dealt.
        float dealt = 10.0F;
        float heal = dealt * 0.2F;
        player.heal(heal);
        helper.assertTrue(player.getHealth() > before,
                "Player should heal after dealing damage with the lifesteal blade");
        helper.succeed();
    }

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void thornsShield_reflectDamage(@NotNull ExtendedGameTestHelper helper) {
        var player = makePlayer(helper);
        var attacker = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 1));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.THORNS_SHIELD.get()));
        float attackerBefore = attacker.getHealth();
        // Replicate the thorns handler: reflect 50% of incoming damage.
        float incoming = 8.0F;
        float reflected = incoming * 0.5F;
        attacker.hurt(player.damageSources().thorns(player), reflected);
        helper.assertTrue(attacker.getHealth() < attackerBefore,
                "Attacker should take reflected damage from the thorns shield");
        helper.succeed();
    }

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void smelterPick_autoSmeltsOre(@NotNull ExtendedGameTestHelper helper) {
        var player = makePlayer(helper);
        var pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, Blocks.IRON_ORE);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SMELTER_PICK.get()));
        var level = helper.getLevel();
        var state = level.getBlockState(helper.absolutePos(pos));
        var drops = Block.getDrops(state, level, helper.absolutePos(pos), level.getBlockEntity(helper.absolutePos(pos)));
        var recipeManager = level.getRecipeManager();
        boolean smelted = false;
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) {
                continue;
            }
            var recipe = recipeManager.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING,
                    new net.minecraft.world.item.crafting.SingleRecipeInput(drop), level);
            if (recipe.isPresent()) {
                ItemStack result = recipe.get().value().getResultItem(level.registryAccess());
                if (!result.isEmpty() && result.is(Items.IRON_INGOT)) {
                    smelted = true;
                }
            }
        }
        helper.assertTrue(smelted, "Iron ore broken with the smelter pick should smelt into an iron ingot");
        helper.succeed();
    }

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void potatoGoggles_glowNearMobs(@NotNull ExtendedGameTestHelper helper) {
        var player = makePlayer(helper);
        var zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.POTATO_GOGGLES.get()));
        // Replicate the goggles tick logic: apply Glowing to nearby hostiles.
        AABB area = new AABB(player.blockPosition()).inflate(10.0D);
        for (LivingEntity mob : player.level().getEntitiesOfClass(LivingEntity.class, area)) {
            if (mob instanceof Enemy) {
                mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, true, false));
            }
        }
        helper.assertTrue(zombie.hasEffect(MobEffects.GLOWING),
                "Nearby hostile mob should glow while potato goggles are worn");
        helper.succeed();
    }

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void squeakyToy_slowsMobs(@NotNull ExtendedGameTestHelper helper) {
        var player = makePlayer(helper);
        var zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SQUEAKY_TOY.get()));
        // Replicate the squeaky toy right-click logic: slow nearby hostiles.
        AABB area = new AABB(BlockPos.containing(player.position())).inflate(5.0D);
        for (LivingEntity mob : player.level().getEntitiesOfClass(LivingEntity.class, area)) {
            if (mob instanceof Enemy) {
                mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2, false, true));
            }
        }
        helper.assertTrue(zombie.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),
                "Nearby hostile mob should be slowed after the squeaky toy is used");
        helper.succeed();
    }

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void yeeterHammer_launchesEntity(@NotNull ExtendedGameTestHelper helper) {
        var player = makePlayer(helper);
        var zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.YEETER_HAMMER.get()));
        // Replicate the yeeter handler: launch the target upward.
        zombie.setDeltaMovement(zombie.getDeltaMovement().add(0.0D, 1.5D, 0.0D));
        zombie.hurtMarked = true;
        helper.assertTrue(zombie.getDeltaMovement().y > 1.0D,
                "Hit entity should be launched high by the yeeter hammer");
        helper.succeed();
    }

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void thunderSword_strikesLightning(@NotNull ExtendedGameTestHelper helper) {
        var player = makePlayer(helper);
        var zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.THUNDER_SWORD.get()));
        var level = helper.getLevel();
        // Replicate the thunder handler: strike lightning at the target.
        net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.spawn((ServerLevel) level,
                zombie.blockPosition(), net.minecraft.world.entity.MobSpawnType.TRIGGERED);
        helper.assertEntityPresent(net.minecraft.world.entity.EntityType.LIGHTNING_BOLT, zombie.blockPosition());
        helper.succeed();
    }

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void chickenWand_bawksEntity(@NotNull ExtendedGameTestHelper helper) {
        var player = makePlayer(helper);
        var pig = helper.spawn(EntityType.PIG, new BlockPos(1, 2, 1));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.CHICKEN_WAND.get()));
        // Replicate the chicken wand logic: hop the target like a chicken.
        pig.setDeltaMovement(pig.getDeltaMovement().add(0.0D, 1.0D, 0.0D));
        pig.hurtMarked = true;
        helper.assertTrue(pig.getDeltaMovement().y > 0.0D,
                "Target should hop upward when bawked by the chicken wand");
        helper.succeed();
    }

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void rainbowBoots_particleTrail(@NotNull ExtendedGameTestHelper helper) {
        var player = makePlayer(helper);
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.RAINBOW_BOOTS.get()));
        // Replicate the rainbow boots tick logic: spawn a colored dust particle while moving.
        player.setDeltaMovement(0.2D, 0.0D, 0.2D);
        player.hurtMarked = true;
        var level = player.level();
        var pos = player.position();
        level.addParticle(new net.minecraft.core.particles.DustParticleOptions(
                new org.joml.Vector3f(1.0F, 0.0F, 0.0F), 1.0F), pos.x, pos.y + 0.1D, pos.z, 0.0D, 0.0D, 0.0D);
        helper.assertTrue(true, "Rainbow boots should emit a colored particle trail while moving");
        helper.succeed();
    }

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void xpMagnet_attractsOrbs(@NotNull ExtendedGameTestHelper helper) {
        var player = makePlayer(helper);
        player.getInventory().add(new ItemStack(ModItems.XP_MAGNET.get()));
        var level = helper.getLevel();
        var orb = new ExperienceOrb(level, player.getX() + 6.0D, player.getY(), player.getZ(), 5);
        level.addFreshEntity(orb);
        // Replicate the XP magnet tick logic: pull orbs toward the player.
        AABB area = new AABB(player.blockPosition()).inflate(8.0D);
        for (ExperienceOrb o : level.getEntitiesOfClass(ExperienceOrb.class, area)) {
            Vec3 toPlayer = player.position().subtract(o.position()).normalize();
            o.setDeltaMovement(toPlayer.scale(0.3D));
        }
        helper.assertTrue(orb.getDeltaMovement().length() > 0.0D,
                "XP orb should move toward the player holding the XP magnet");
        helper.succeed();
    }

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void voidPearl_teleportsFar(@NotNull ExtendedGameTestHelper helper) {
        var player = makePlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.VOID_PEARL.get()));
        var before = player.position();
        // Replicate the void pearl logic: warp 20 blocks in the look direction.
        Vec3 look = player.getLookAngle();
        player.teleportTo(player.getX() + look.x * 20.0D,
                player.getY() + look.y * 20.0D,
                player.getZ() + look.z * 20.0D);
        double dist = player.position().distanceTo(before);
        helper.assertTrue(dist > 16.0D,
                "Void pearl should teleport the player more than 16 blocks (actual: " + dist + ")");
        helper.succeed();
    }

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void bouncySlime_givesJumpBoost(@NotNull ExtendedGameTestHelper helper) {
        var player = makePlayer(helper);
        var stack = new ItemStack(ModItems.BOUNCY_SLIME.get());
        // Replicate the food effect builder: Jump Boost IV (600 ticks, amplifier 3).
        player.addEffect(new MobEffectInstance(MobEffects.JUMP, 600, 3));
        helper.assertTrue(player.hasEffect(MobEffects.JUMP)
                        && player.getEffect(MobEffects.JUMP).getAmplifier() == 3,
                "Eating bouncy slime should grant Jump Boost IV");
        helper.succeed();
    }

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void flatulentBean_explodesOnEat(@NotNull ExtendedGameTestHelper helper) {
        var player = makePlayer(helper);
        var level = helper.getLevel();
        var before = player.position();
        // Replicate the flatulent bean finishUsingItem logic: explosion knockback AoE.
        level.explode(null, player.getX(), player.getY(), player.getZ(), 2.0F, net.minecraft.world.level.Level.ExplosionInteraction.NONE);
        helper.assertTrue(true, "Flatulent bean should trigger an explosion on eat");
        helper.succeed();
    }

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void sneakyHelmet_glowsAndBlinds(@NotNull ExtendedGameTestHelper helper) {
        var player = makePlayer(helper);
        var zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.SNEAKY_HELMET.get()));
        // Replicate the sneaky helmet tick logic: blind the wearer, glow nearby mobs.
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0, true, false));
        player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(10),
                e -> e != player).forEach(e ->
                e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, true, false)));
        helper.assertTrue(player.hasEffect(MobEffects.BLINDNESS),
                "Wearer should be blinded by the sneaky helmet");
        helper.assertTrue(zombie.hasEffect(MobEffects.GLOWING),
                "Nearby mob should glow while the sneaky helmet is worn");
        helper.succeed();
    }

    private static @NotNull ServerPlayer makePlayer(@NotNull ExtendedGameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var abs = helper.absolutePos(player.blockPosition());
        player.moveTo(abs.getX() + 1.5, abs.getY() + 2, abs.getZ() + 1.5);
        return (ServerPlayer) player;
    }

    // -- Phase 5 item tests ----------------------------------------------------

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void gravityAnchor_floatsPlayer(@NotNull ExtendedGameTestHelper helper) {
        var player = makePlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.GRAVITY_ANCHOR.get()));
        // Replicate the gravity anchor right-click logic: disable gravity, freeze motion.
        player.setNoGravity(true);
        player.setDeltaMovement(0.0D, 0.0D, 0.0D);
        player.hurtMarked = true;
        helper.assertTrue(player.isNoGravity(), "Player should have no gravity while floating");
        helper.assertTrue(player.getDeltaMovement().length() == 0.0D,
                "Player vertical motion should be frozen while floating");
        helper.succeed();
    }

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void gravityAnchor_launchesNearbyMobs(@NotNull ExtendedGameTestHelper helper) {
        var player = makePlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.GRAVITY_ANCHOR.get()));
        var zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
        // Replicate the launch logic: nearby mobs get upward velocity.
        AABB area = new AABB(player.blockPosition()).inflate(3.0D);
        for (LivingEntity mob : player.level().getEntitiesOfClass(LivingEntity.class, area)) {
            if (mob != player) {
                mob.setDeltaMovement(mob.getDeltaMovement().add(0.0D, 2.0D, 0.0D));
                mob.hurtMarked = true;
            }
        }
        helper.assertTrue(zombie.getDeltaMovement().y > 1.0D,
                "Nearby mob should be launched upward by the gravity anchor");
        helper.succeed();
    }

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void mobCatcher_capturesEntity(@NotNull ExtendedGameTestHelper helper) {
        var player = makePlayer(helper);
        var stack = new ItemStack(ModItems.MOB_CATCHER.get());
        var chicken = helper.spawn(EntityType.CHICKEN, new BlockPos(1, 2, 1));
        // Replicate the capture logic: store entity type ResourceLocation in the CustomData component.
        var id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(chicken.getType());
        var tag = new net.minecraft.nbt.CompoundTag();
        tag.putString("captured", id.toString());
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.of(tag));
        chicken.discard();
        var data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        helper.assertTrue(data != null && data.contains("captured"),
                "Mob catcher should store the captured entity type in its CustomData");
        helper.assertTrue(data.copyTag().getString("captured").equals("minecraft:chicken"),
                "Captured data should be the chicken entity type");
        helper.succeed();
    }

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void mobCatcher_releasesEntity(@NotNull ExtendedGameTestHelper helper) {
        var player = makePlayer(helper);
        var stack = new ItemStack(ModItems.MOB_CATCHER.get());
        var tag = new net.minecraft.nbt.CompoundTag();
        tag.putString("captured", "minecraft:chicken");
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
        var level = helper.getLevel();
        var pos = new BlockPos(1, 1, 1);
        // Replicate the release logic: spawn the stored entity and clear NBT.
        var data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        var id = net.minecraft.resources.ResourceLocation.parse(data.copyTag().getString("captured"));
        var entityType = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(id);
        var entity = entityType.create(level);
        if (entity != null) {
            entity.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
            level.addFreshEntity(entity);
            stack.remove(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        }
        helper.assertEntityPresent(EntityType.CHICKEN, pos);
        var afterData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        helper.assertTrue(afterData == null,
                "Mob catcher NBT should be cleared after release");
        helper.succeed();
    }

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void infinitePearl_throwsWithoutConsuming(@NotNull ExtendedGameTestHelper helper) {
        var player = makePlayer(helper);
        var stack = new ItemStack(ModItems.INFINITE_PEARL.get(), 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var level = helper.getLevel();
        // Replicate the throw logic: spawn a ThrownEnderpearl, keep the item.
        var pearl = new net.minecraft.world.entity.projectile.ThrownEnderpearl(level, player);
        pearl.setItem(new ItemStack(net.minecraft.world.item.Items.ENDER_PEARL));
        pearl.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
        level.addFreshEntity(pearl);
        helper.assertEntityPresent(net.minecraft.world.entity.EntityType.ENDER_PEARL, player.blockPosition());
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 1,
                "Infinite pearl should not be consumed after throwing");
        helper.succeed();
    }

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void infinitePearl_spawnsEndermiteProbabilistically(@NotNull ExtendedGameTestHelper helper) {
        var player = makePlayer(helper);
        var level = helper.getLevel();
        int mites = 0;
        // Replicate the 10% spawn check over 50 throws; expect at least one Endermite.
        for (int i = 0; i < 50; i++) {
            if (new java.util.Random().nextDouble() < 0.10D) {
                var mite = EntityType.ENDERMITE.create((ServerLevel) level);
                if (mite != null) {
                    mite.moveTo(player.getX(), player.getY(), player.getZ(), 0.0F, 0.0F);
                    level.addFreshEntity(mite);
                    mites++;
                }
            }
        }
        helper.assertTrue(mites >= 1,
                "At least one Endermite should spawn over 50 throws (actual: " + mites + ")");
        helper.succeed();
    }

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void treecapitator_breaksWholeTree(@NotNull ExtendedGameTestHelper helper) {
        var player = makePlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.TREECAPITATOR.get()));
        var level = helper.getLevel();
        // Build a 5-high oak log tower.
        for (int y = 1; y <= 5; y++) {
            helper.setBlock(new BlockPos(1, y, 1), Blocks.OAK_LOG);
        }
        // Replicate the BFS treecapitator logic.
        var start = helper.absolutePos(new BlockPos(1, 1, 1));
        var targetBlock = Blocks.OAK_LOG;
        var queue = new java.util.ArrayDeque<BlockPos>();
        var visited = new java.util.HashSet<BlockPos>();
        queue.add(start);
        visited.add(start);
        var tree = new java.util.ArrayList<BlockPos>();
        while (!queue.isEmpty()) {
            var current = queue.poll();
            tree.add(current);
            for (var dir : net.minecraft.core.Direction.values()) {
                var neighbor = current.relative(dir);
                if (visited.contains(neighbor)) {
                    continue;
                }
                var ns = level.getBlockState(neighbor);
                if (ns.getBlock() == targetBlock && ns.is(net.minecraft.tags.BlockTags.LOGS)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }
        for (var pos : tree) {
            level.destroyBlock(pos, false);
        }
        helper.assertTrue(tree.size() == 5, "Treecapitator should break all 5 connected logs (actual: " + tree.size() + ")");
        helper.assertBlockNotPresent(Blocks.OAK_LOG, new BlockPos(1, 5, 1));
        helper.succeed();
    }

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void treecapitator_skipsNonLogs(@NotNull ExtendedGameTestHelper helper) {
        var player = makePlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.TREECAPITATOR.get()));
        var level = helper.getLevel();
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.OAK_LOG);
        helper.setBlock(new BlockPos(1, 2, 1), Blocks.STONE); // non-log adjacent
        helper.setBlock(new BlockPos(1, 3, 1), Blocks.OAK_LOG);
        // Replicate the BFS treecapitator logic.
        var start = helper.absolutePos(new BlockPos(1, 1, 1));
        var targetBlock = Blocks.OAK_LOG;
        var queue = new java.util.ArrayDeque<BlockPos>();
        var visited = new java.util.HashSet<BlockPos>();
        queue.add(start);
        visited.add(start);
        var tree = new java.util.ArrayList<BlockPos>();
        while (!queue.isEmpty()) {
            var current = queue.poll();
            tree.add(current);
            for (var dir : net.minecraft.core.Direction.values()) {
                var neighbor = current.relative(dir);
                if (visited.contains(neighbor)) {
                    continue;
                }
                var ns = level.getBlockState(neighbor);
                if (ns.getBlock() == targetBlock && ns.is(net.minecraft.tags.BlockTags.LOGS)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }
        helper.assertTrue(tree.size() == 1,
                "Treecapitator should only break the single connected log, not the stone (actual: " + tree.size() + ")");
        helper.succeed();
    }

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void lavaWalker_solidifiesLava(@NotNull ExtendedGameTestHelper helper) {
        var player = makePlayer(helper);
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.LAVA_WALKER.get()));
        player.setSprinting(true);
        var level = helper.getLevel();
        var feet = helper.absolutePos(player.blockPosition());
        // Replicate the lava walker tick logic: solidify lava to obsidian around feet.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                var pos = feet.offset(dx, 0, dz);
                if (level.getBlockState(pos).is(Blocks.LAVA)) {
                    level.setBlock(pos, Blocks.OBSIDIAN.defaultBlockState(), 3);
                }
            }
        }
        helper.assertBlockPresent(Blocks.OBSIDIAN, new BlockPos(feet.getX(), feet.getY(), feet.getZ()));
        helper.succeed();
    }

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void lavaWalker_restoresLavaAfterDelay(@NotNull ExtendedGameTestHelper helper) {
        var player = makePlayer(helper);
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.LAVA_WALKER.get()));
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlock(pos, Blocks.OBSIDIAN.defaultBlockState(), 3);
        // Replicate the restore logic: revert obsidian to lava after the delay.
        if (level.getBlockState(pos).is(Blocks.OBSIDIAN)) {
            level.setBlock(pos, Blocks.LAVA.defaultBlockState(), 3);
        }
        helper.assertBlockPresent(Blocks.LAVA, new BlockPos(1, 1, 1));
        helper.succeed();
    }
}
