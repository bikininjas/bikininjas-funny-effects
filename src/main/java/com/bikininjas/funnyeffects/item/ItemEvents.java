package com.bikininjas.funnyeffects.item;

import com.bikininjas.corelib.log.LogManager;
import com.bikininjas.corelib.log.ModLogger;
import com.bikininjas.funnyeffects.FunnyEffectsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * All gameplay event subscriptions for Funny Effects items. Registered on the NeoForge
 * event bus from {@link com.bikininjas.funnyeffects.FunnyEffectsMod}'s constructor.
 */
public final class ItemEvents {

    private ItemEvents() {
    }

    private static final ModLogger LOGGER = LogManager.getLogger(FunnyEffectsMod.MOD_ID, ItemEvents.class);

    /**
     * Magnet glove: pull nearby dropped items toward the player while held in either hand.
     */
    @SubscribeEvent
    public static void onPlayerTick(@NotNull PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        if (!main.is(ModItems.MAGNETIC_GLOVE.get()) && !off.is(ModItems.MAGNETIC_GLOVE.get())) {
            return;
        }

        AABB area = new AABB(player.blockPosition()).inflate(5.0D);
        for (ItemEntity item : player.level().getEntitiesOfClass(ItemEntity.class, area)) {
            Vec3 toPlayer = player.position().subtract(item.position()).normalize();
            double dist = item.position().distanceTo(player.position());
            double strength = Math.max(0.05D, 0.35D - dist * 0.04D);
            item.setDeltaMovement(item.getDeltaMovement().add(toPlayer.scale(strength)));
            item.hasImpulse = true;
        }
    }

    /**
     * Lifesteal blade: heal the attacker for 20% of damage dealt when wielding the blade.
     */
    @SubscribeEvent
    public static void onLivingDamageLifesteal(@NotNull LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide()) {
            return;
        }
        Entity direct = event.getSource().getDirectEntity();
        if (!(direct instanceof Player player)) {
            return;
        }
        if (!isHolding(player, ModItems.LIFESTEAL_BLADE.get())) {
            return;
        }
        float dealt = event.getNewDamage();
        float heal = dealt * 0.2F;
        if (heal > 0.0F) {
            player.heal(heal);
        }
    }

    /**
     * Thorns shield: reflect 50% of pre-mitigation damage back to a LivingEntity attacker.
     */
    @SubscribeEvent
    public static void onIncomingDamage(@NotNull LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) {
            return;
        }
        ItemStack chest = victim.getItemBySlot(EquipmentSlot.CHEST);
        if (!chest.is(ModItems.THORNS_SHIELD.get())) {
            return;
        }
        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof LivingEntity livingAttacker) || livingAttacker == victim) {
            return;
        }
        float reflected = event.getOriginalAmount() * 0.5F;
        if (reflected > 0.0F) {
            livingAttacker.hurt(victim.damageSources().thorns(victim), reflected);
        }
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
     * Potato goggles: reveal nearby hostile mobs with Glowing while worn on the head.
     */
    @SubscribeEvent
    public static void onPlayerTickGoggles(@NotNull PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!head.is(ModItems.POTATO_GOGGLES.get())) {
            return;
        }
        AABB area = new AABB(player.blockPosition()).inflate(10.0D);
        for (LivingEntity mob : player.level().getEntitiesOfClass(LivingEntity.class, area)) {
            if (mob instanceof Enemy) {
                mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, true, false));
            }
        }
    }

    /**
     * Squeaky toy: right-click plays a bat squeak and slows nearby mobs (5 block radius).
     */
    @SubscribeEvent
    public static void onRightClick(@NotNull PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (!stack.is(ModItems.SQUEAKY_TOY.get())) {
            return;
        }
        Level level = player.level();
        if (level.isClientSide()) {
            return;
        }
        BlockPos soundPos = BlockPos.containing(player.position());
        level.playSound(null, soundPos, SoundEvents.BAT_TAKEOFF, SoundSource.PLAYERS, 1.0F, 1.0F);
        AABB area = new AABB(soundPos).inflate(5.0D);
        for (LivingEntity mob : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (mob instanceof Enemy) {
                mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2, false, true));
            }
        }
    }

    /**
     * Yeeter hammer: launch the hit entity high into the air.
     */
    @SubscribeEvent
    public static void onLivingDamageYeeter(@NotNull LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide()) {
            return;
        }
        Entity direct = event.getSource().getDirectEntity();
        if (!(direct instanceof LivingEntity attacker)) {
            return;
        }
        if (!isHolding(attacker, ModItems.YEETER_HAMMER.get())) {
            return;
        }
        target.setDeltaMovement(target.getDeltaMovement().add(0.0D, 1.5D, 0.0D));
        target.hurtMarked = true;
        LOGGER.info("Yeeeeet! Launched {}", target.getName().getString());
    }

    /**
     * Thunder sword: strike the hit entity with lightning.
     */
    @SubscribeEvent
    public static void onLivingDamageThunder(@NotNull LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide()) {
            return;
        }
        Entity direct = event.getSource().getDirectEntity();
        if (!(direct instanceof LivingEntity attacker)) {
            return;
        }
        if (!isHolding(attacker, ModItems.THUNDER_SWORD.get())) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel) target.level();
        net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.spawn(serverLevel, target.blockPosition(),
                net.minecraft.world.entity.MobSpawnType.TRIGGERED);
        LOGGER.info("Thunder struck {}", target.getName().getString());
    }

    /**
     * Chicken wand: right-click an entity to make it bawk and hop like a chicken.
     */
    @SubscribeEvent
    public static void onEntityInteractChicken(@NotNull PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (!stack.is(ModItems.CHICKEN_WAND.get())) {
            return;
        }
        Level level = player.level();
        if (level.isClientSide()) {
            return;
        }
        Entity target = event.getTarget();
        if (!(target instanceof LivingEntity living) || target instanceof Player) {
            return;
        }
        living.setDeltaMovement(living.getDeltaMovement().add(0.0D, 1.0D, 0.0D));
        living.hurtMarked = true;
        level.playSound(null, living.blockPosition(), SoundEvents.CHICKEN_AMBIENT,
                SoundSource.PLAYERS, 1.0F, 1.0F);
        LOGGER.info("Bawk! {} is now a chicken", living.getName().getString());
    }

    /**
     * Rainbow boots: leave a rainbow particle trail while moving.
     */
    @SubscribeEvent
    public static void onPlayerTickRainbow(@NotNull PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        ItemStack feet = player.getItemBySlot(EquipmentSlot.FEET);
        if (!feet.is(ModItems.RAINBOW_BOOTS.get())) {
            return;
        }
        Vec3 movement = player.getDeltaMovement();
        if (movement.length() <= 0.1D) {
            return;
        }
        if (player.tickCount % 3 != 0) {
            return;
        }
        Random random = new Random();
        int randomRGB = random.nextInt(0xFFFFFF);
        float r = ((randomRGB >> 16) & 0xFF) / 255.0F;
        float g = ((randomRGB >> 8) & 0xFF) / 255.0F;
        float b = (randomRGB & 0xFF) / 255.0F;
        Vec3 pos = player.position();
        player.level().addParticle(new DustParticleOptions(new Vector3f(r, g, b), 1.0F),
                pos.x, pos.y + 0.1D, pos.z, 0.0D, 0.0D, 0.0D);
        LOGGER.debug("Rainbow trail at {}", player.position());
    }

    /**
     * XP magnet: pull nearby XP orbs toward the holder.
     */
    @SubscribeEvent
    public static void onPlayerTickXpMagnet(@NotNull PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        if (player.getInventory().items.stream().noneMatch(stack -> stack.is(ModItems.XP_MAGNET.get()))
                && player.getInventory().armor.stream().noneMatch(stack -> stack.is(ModItems.XP_MAGNET.get()))
                && player.getInventory().offhand.stream().noneMatch(stack -> stack.is(ModItems.XP_MAGNET.get()))) {
            return;
        }
        if (player.tickCount % 5 != 0) {
            return;
        }
        AABB area = new AABB(player.blockPosition()).inflate(8.0D);
        for (ExperienceOrb orb : player.level().getEntitiesOfClass(ExperienceOrb.class, area)) {
            Vec3 toPlayer = player.position().subtract(orb.position()).normalize();
            orb.setDeltaMovement(toPlayer.scale(0.3D));
        }
    }

    /**
     * Void pearl: right-click to warp 20 blocks in the look direction.
     */
    @SubscribeEvent
    public static void onRightClickVoidPearl(@NotNull PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (!stack.is(ModItems.VOID_PEARL.get())) {
            return;
        }
        Level level = player.level();
        if (level.isClientSide()) {
            return;
        }
        Vec3 look = player.getLookAngle();
        player.teleportTo(player.getX() + look.x * 20.0D,
                player.getY() + look.y * 20.0D,
                player.getZ() + look.z * 20.0D);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 1.0F, 1.0F);
        LOGGER.info("Void warp! {}", player.getName().getString());
    }

    // -- New item handlers (Phase 4) -------------------------------------------

    /**
     * Slapfish: negate incoming damage and knock the target away from the attacker while held.
     */
    @SubscribeEvent
    public static void slapfish_onDamage(@NotNull LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        Level level = victim.level();
        if (level.isClientSide()) {
            return;
        }
        Entity source = event.getSource().getEntity();
        if (!(source instanceof LivingEntity attacker)) {
            return;
        }
        if (!isHolding(attacker, ModItems.SLAPFISH.get())) {
            return;
        }
        try {
            // Cancel the default damage entirely.
            event.getContainer().setNewDamage(0.0F);
            // Knock the victim away along the attacker's look vector.
            Vec3 dir = attacker.getLookAngle().normalize().scale(3.0D);
            victim.push(dir.x, dir.y * 0.5D + 0.2D, dir.z);
            victim.hurtMarked = true;
            level.playSound(null, victim.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
            LOGGER.info("Slapfish! {} slapped {}", attacker.getName().getString(), victim.getName().getString());
        } catch (Exception e) {
            LOGGER.error("Failed to apply slapfish knockback")
                    .ctx("victim", victim.getName().getString())
                    .cause(e)
                    .report();
        }
    }

    /**
     * Disco sword: on hit, spray colored dust particles and play a random note block sound.
     */
    @SubscribeEvent
    public static void discoSword_onDamage(@NotNull LivingDamageEvent.Post event) {
        LivingEntity victim = event.getEntity();
        Level level = victim.level();
        if (level.isClientSide()) {
            return;
        }
        Entity source = event.getSource().getDirectEntity();
        if (!(source instanceof LivingEntity attacker)) {
            return;
        }
        if (!isHolding(attacker, ModItems.DISCO_SWORD.get())) {
            return;
        }
        try {
            Random random = new Random();
            Vec3 pos = victim.position();
            for (int i = 0; i < 5; i++) {
                int rgb = random.nextInt(0xFFFFFF);
                float r = ((rgb >> 16) & 0xFF) / 255.0F;
                float g = ((rgb >> 8) & 0xFF) / 255.0F;
                float b = (rgb & 0xFF) / 255.0F;
                level.addParticle(new DustParticleOptions(new Vector3f(r, g, b), 1.0F),
                        pos.x, pos.y + 1.0D, pos.z, 0.0D, 0.1D, 0.0D);
            }
            @SuppressWarnings("unchecked") // SoundEvents note fields are Holder.Reference<SoundEvent>
            Holder<SoundEvent>[] notes = new Holder[]{
                    SoundEvents.NOTE_BLOCK_HAT, SoundEvents.NOTE_BLOCK_BASEDRUM};
            level.playSound(null, victim.blockPosition(), notes[random.nextInt(notes.length)].value(),
                    SoundSource.PLAYERS, 1.0F, 1.0F);
            LOGGER.info("Disco! {} hit {}", attacker.getName().getString(), victim.getName().getString());
        } catch (Exception e) {
            LOGGER.error("Failed to apply disco sword effect")
                    .ctx("victim", victim.getName().getString())
                    .cause(e)
                    .report();
        }
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
        if (event.getHand() != net.minecraft.world.InteractionHand.OFF_HAND) {
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
        try {
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
        } catch (Exception e) {
            LOGGER.error("Failed to replant crop")
                    .ctx("pos", pos.toString())
                    .cause(e)
                    .report();
        }
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
        try {
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
        } catch (Exception e) {
            LOGGER.error("Failed to teleport gravity pickaxe drops")
                    .ctx("player", player.getName().getString())
                    .cause(e)
                    .report();
        }
    }

    /**
     * Bouncy boots: negate fall damage and launch the wearer slightly upward.
     */
    @SubscribeEvent
    public static void bouncyBoots_onFall(@NotNull LivingFallEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.BOUNCY_BOOTS.get())) {
            return;
        }
        try {
            event.setDamageMultiplier(0);
            event.setDistance(0);
            Level level = player.level();
            level.playSound(null, player.blockPosition(), SoundEvents.SLIME_BLOCK_FALL,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
            player.setDeltaMovement(player.getDeltaMovement().add(0, 0.3, 0));
            player.hurtMarked = true;
            LOGGER.info("Bouncy boots absorbed fall for {}", player.getName().getString());
        } catch (Exception e) {
            LOGGER.error("Failed to apply bouncy boots")
                    .ctx("player", player.getName().getString())
                    .cause(e)
                    .report();
        }
    }

    /**
     * Sneaky helmet: blind the wearer and reveal nearby mobs with Glowing.
     */
    @SubscribeEvent
    public static void sneakyHelmet_onTick(@NotNull PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        if (!player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.SNEAKY_HELMET.get())) {
            return;
        }
        try {
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0, true, false));
            Level level = player.level();
            level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(10),
                    e -> e != player).forEach(e ->
                    e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, true, false)));
        } catch (Exception e) {
            LOGGER.error("Failed to apply sneaky helmet")
                    .ctx("player", player.getName().getString())
                    .cause(e)
                    .report();
        }
    }

    /**
     * Dinnerbone bat: right-click an entity to flip its "Dinnerbone" name tag.
     */
    @SubscribeEvent
    public static void dinnerboneBat_onEntityInteract(@NotNull PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        Level level = player.level();
        if (level.isClientSide()) {
            return;
        }
        if (!isHolding(player, ModItems.DINNERBONE_BAT.get())) {
            return;
        }
        Entity target = event.getTarget();
        if (target == null) {
            return;
        }
        try {
            if (target.getTags().contains("Dinnerbone")) {
                target.removeTag("Dinnerbone");
                level.playSound(null, target.blockPosition(), SoundEvents.VILLAGER_NO,
                        SoundSource.PLAYERS, 1.0F, 1.0F);
            } else {
                target.addTag("Dinnerbone");
                level.playSound(null, target.blockPosition(), SoundEvents.BAT_TAKEOFF,
                        SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            event.setCanceled(true);
            LOGGER.info("Dinnerbone bat toggled tag on {}", target.getName().getString());
        } catch (Exception e) {
            LOGGER.error("Failed to toggle Dinnerbone tag")
                    .ctx("target", target.getName().getString())
                    .cause(e)
                    .report();
        }
    }

    /**
     * Party popper: right-click to fire a confetti burst and consume the item.
     */
    @SubscribeEvent
    public static void partyPopper_onRightClick(@NotNull PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (!stack.is(ModItems.PARTY_POPPER.get())) {
            return;
        }
        Level level = player.level();
        if (level.isClientSide()) {
            return;
        }
        try {
            Random random = new Random();
            Vec3 pos = player.position();
            for (int i = 0; i < 20; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double radius = random.nextDouble() * 3.0D;
                double x = pos.x + Math.cos(angle) * radius;
                double z = pos.z + Math.sin(angle) * radius;
                double y = pos.y + random.nextDouble() * 2.0D;
                int rgb = random.nextInt(0xFFFFFF);
                float r = ((rgb >> 16) & 0xFF) / 255.0F;
                float g = ((rgb >> 8) & 0xFF) / 255.0F;
                float b = (rgb & 0xFF) / 255.0F;
                level.addParticle(new DustParticleOptions(new Vector3f(r, g, b), 1.0F),
                        x, y, z, 0.0D, 0.05D, 0.0D);
            }
            level.playSound(null, player.blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
            stack.shrink(1);
            event.setCanceled(true);
            LOGGER.info("Party popper fired by {}", player.getName().getString());
        } catch (Exception e) {
            LOGGER.error("Failed to fire party popper")
                    .ctx("player", player.getName().getString())
                    .cause(e)
                    .report();
        }
    }

    private static boolean isHolding(@NotNull LivingEntity entity, @NotNull Item item) {
        Objects.requireNonNull(entity, "entity must not be null");
        Objects.requireNonNull(item, "item must not be null");
        return entity.getMainHandItem().is(item) || entity.getOffhandItem().is(item);
    }

    // -- Phase 5 item handlers -------------------------------------------------

    /** Lava walker: obsidian positions and the server tick time they were solidified. */
    private static final java.util.Map<BlockPos, Long> LAVA_WALKER_SOLIDIFIED = new java.util.concurrent.ConcurrentHashMap<>();

    private static final long LAVA_RESTORE_DELAY_TICKS = 60L; // 3 seconds at 20 TPS

    /**
     * Gravity anchor: right-click floats the player for 5s and launches nearby mobs upward.
     */
    @SubscribeEvent
    public static void gravityAnchor_onRightClick(@NotNull PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (!stack.is(ModItems.GRAVITY_ANCHOR.get())) {
            return;
        }
        Level level = player.level();
        if (level.isClientSide()) {
            return;
        }
        if (player.getCooldowns().isOnCooldown(stack.getItem())) {
            return;
        }
        try {
            player.setNoGravity(true);
            player.setDeltaMovement(0.0D, 0.0D, 0.0D);
            player.hurtMarked = true;
            // Launch nearby living entities upward.
            AABB area = new AABB(player.blockPosition()).inflate(3.0D);
            for (LivingEntity mob : level.getEntitiesOfClass(LivingEntity.class, area)) {
                if (mob == player) {
                    continue;
                }
                mob.setDeltaMovement(mob.getDeltaMovement().add(0.0D, 2.0D, 0.0D));
                mob.hurtMarked = true;
            }
            // Reset gravity after 5 seconds (100 ticks) via a scheduled server task.
            if (level instanceof ServerLevel serverLevel && player.getServer() != null) {
                player.getServer().tell(new net.minecraft.server.TickTask(serverLevel.getServer().getTickCount() + 100,
                        () -> {
                            if (player.isAlive()) {
                                player.setNoGravity(false);
                            }
                        }));
            }
            player.getCooldowns().addCooldown(stack.getItem(), 300); // 15 seconds
            level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
            LOGGER.info("Gravity anchor activated for {}", player.getName().getString());
        } catch (Exception e) {
            LOGGER.error("Failed to activate gravity anchor")
                    .ctx("player", player.getName().getString())
                    .cause(e)
                    .report();
        }
    }

    /**
     * Infinite pearl: right-click throws an ender pearl without consuming the item.
     * 10% chance to spawn an Endermite at the destination.
     */
    @SubscribeEvent
    public static void infinitePearl_onRightClick(@NotNull PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (!stack.is(ModItems.INFINITE_PEARL.get())) {
            return;
        }
        Level level = player.level();
        if (level.isClientSide()) {
            return;
        }
        if (player.getCooldowns().isOnCooldown(stack.getItem())) {
            return;
        }
        try {
            var pearl = new net.minecraft.world.entity.projectile.ThrownEnderpearl(level, player);
            pearl.setItem(new ItemStack(net.minecraft.world.item.Items.ENDER_PEARL));
            Vec3 look = player.getLookAngle();
            pearl.setPos(player.getX(), player.getEyeY() - 0.1D, player.getZ());
            pearl.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
            level.addFreshEntity(pearl);
            player.getCooldowns().addCooldown(stack.getItem(), 20);
            // 10% chance to spawn an Endermite at the destination after the pearl lands.
            if (level instanceof ServerLevel serverLevel && new Random().nextDouble() < 0.10D) {
                BlockPos dest = BlockPos.containing(player.getX() + look.x * 8.0D,
                        player.getY() + look.y * 8.0D, player.getZ() + look.z * 8.0D);
                Endermite mite = EntityType.ENDERMITE.create(serverLevel);
                if (mite != null) {
                    mite.moveTo(dest.getX() + 0.5D, dest.getY(), dest.getZ() + 0.5D, 0.0F, 0.0F);
                    serverLevel.addFreshEntity(mite);
                    LOGGER.info("Infinite pearl spawned an Endermite at {}", dest);
                }
            }
            event.setCanceled(true);
        } catch (Exception e) {
            LOGGER.error("Failed to throw infinite pearl")
                    .ctx("player", player.getName().getString())
                    .cause(e)
                    .report();
        }
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
        try {
            BlockPos start = event.getPos();
            BlockState startState = event.getState();
            if (!startState.is(net.minecraft.tags.BlockTags.LOGS)) {
                return;
            }
            var targetBlock = startState.getBlock();
            Deque<BlockPos> queue = new ArrayDeque<>();
            java.util.Set<BlockPos> visited = new java.util.HashSet<>();
            queue.add(start);
            visited.add(start);
            List<BlockPos> tree = new java.util.ArrayList<>();
            while (!queue.isEmpty()) {
                BlockPos current = queue.poll();
                tree.add(current);
                for (var dir : net.minecraft.core.Direction.values()) {
                    if (dir.getAxis() == net.minecraft.core.Direction.Axis.Y
                            || dir.getAxis() == net.minecraft.core.Direction.Axis.X
                            || dir.getAxis() == net.minecraft.core.Direction.Axis.Z) {
                        BlockPos neighbor = current.relative(dir);
                        if (visited.contains(neighbor)) {
                            continue;
                        }
                        BlockState neighborState = level.getBlockState(neighbor);
                        if (neighborState.getBlock() == targetBlock && neighborState.is(net.minecraft.tags.BlockTags.LOGS)) {
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
        } catch (Exception e) {
            LOGGER.error("Failed to treecapitate")
                    .ctx("player", player.getName().getString())
                    .cause(e)
                    .report();
        }
    }

    /**
     * Lava walker boots: while sprinting, solidify lava to obsidian around the player's feet.
     */
    @SubscribeEvent
    public static void lavaWalker_onTick(@NotNull PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        if (!player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.LAVA_WALKER.get())) {
            return;
        }
        if (!player.isSprinting()) {
            return;
        }
        try {
            Level level = player.level();
            BlockPos feet = player.blockPosition();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos pos = feet.offset(dx, 0, dz);
                    if (level.getBlockState(pos).is(Blocks.LAVA)) {
                        level.setBlock(pos, Blocks.OBSIDIAN.defaultBlockState(), 3);
                        LAVA_WALKER_SOLIDIFIED.put(pos.immutable(), level.getGameTime());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to apply lava walker")
                    .ctx("player", player.getName().getString())
                    .cause(e)
                    .report();
        }
    }

    /**
     * Lava walker restore: revert obsidian back to lava 3 seconds after the player leaves.
     */
    @SubscribeEvent
    public static void lavaWalker_onServerTick(@NotNull ServerTickEvent.Post event) {
        if (LAVA_WALKER_SOLIDIFIED.isEmpty()) {
            return;
        }
        try {
            var server = event.getServer();
            long now = server.getTickCount();
            var iterator = LAVA_WALKER_SOLIDIFIED.entrySet().iterator();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                if (now - entry.getValue() >= LAVA_RESTORE_DELAY_TICKS) {
                    BlockPos pos = entry.getKey();
                    for (var level : server.getAllLevels()) {
                        if (level.getBlockState(pos).is(Blocks.OBSIDIAN)) {
                            level.setBlock(pos, Blocks.LAVA.defaultBlockState(), 3);
                        }
                    }
                    iterator.remove();
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to restore lava walker obsidian").cause(e).report();
        }
    }
}
