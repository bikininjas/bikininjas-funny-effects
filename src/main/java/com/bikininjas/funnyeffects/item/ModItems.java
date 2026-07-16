package com.bikininjas.funnyeffects.item;

import com.bikininjas.corelib.log.LogManager;
import com.bikininjas.corelib.log.ModLogger;
import com.bikininjas.funnyeffects.FunnyEffectsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Central registry of all Funny Effects items and their gameplay event handlers.
 * <p>
 * Items are registered via NeoForge's {@link DeferredRegister} system. Gameplay behaviour
 * (effects on eat, tick-based abilities, damage reflection, etc.) is wired through
 * {@link SubscribeEvent} handlers on the NeoForge event bus — never via the deprecated
 * {@code @EventBusSubscriber}.
 */
public final class ModItems {

    public static final DeferredRegister.Items MOD_ITEMS =
            DeferredRegister.createItems(FunnyEffectsMod.MOD_ID);

    private static final ModLogger LOGGER = LogManager.getLogger(FunnyEffectsMod.MOD_ID, ModItems.class);

    // -- Items -----------------------------------------------------------------

    /** Food item: eat grants Jump Boost IV (30s) + Slow Falling (5s). Always edible. */
    public static final DeferredItem<Item> BOUNCY_SLIME = MOD_ITEMS.registerItem("bouncy_slime",
            props -> new Item(props
                    .food(new FoodProperties.Builder()
                            .nutrition(4)
                            .saturationModifier(0.3F)
                            .alwaysEdible()
                            .effect(() -> new MobEffectInstance(MobEffects.JUMP, 600, 3), 1.0F)
                            .effect(() -> new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0), 1.0F)
                            .build())
                    .stacksTo(16)));

    /** Glove that magnetically attracts nearby dropped items while held. */
    public static final DeferredItem<Item> MAGNETIC_GLOVE = MOD_ITEMS.registerItem("magnetic_glove",
            props -> new Item(props.stacksTo(1)));

    /** Food item: eat triggers an explosion knockback AoE + Nausea II (5s). Always edible. */
    public static final DeferredItem<Item> FLATULENT_BEAN = MOD_ITEMS.registerItem("flatulent_bean",
            props -> new FlatulentBeanItem(props));

    /** Toy: right-click plays a bat squeak and slows nearby mobs (5 block radius). */
    public static final DeferredItem<Item> SQUEAKY_TOY = MOD_ITEMS.registerItem("squeaky_toy",
            props -> new Item(props.stacksTo(1)));

    /** Netherite sword: heals the wielder for 20% of damage dealt. */
    public static final DeferredItem<Item> LIFESTEAL_BLADE = MOD_ITEMS.registerItem("lifesteal_blade",
            props -> new SwordItem(Tiers.NETHERITE,
                    props.attributes(SwordItem.createAttributes(Tiers.NETHERITE, 8.0F, 3.5F))
                            .stacksTo(1)));

    /** Netherite pickaxe: auto-smelts broken block drops. */
    public static final DeferredItem<Item> SMELTER_PICK = MOD_ITEMS.registerItem("smelter_pick",
            props -> new PickaxeItem(Tiers.NETHERITE,
                    props.attributes(PickaxeItem.createAttributes(Tiers.NETHERITE, 6.0F, 1.2F))
                            .stacksTo(1)));

    /** Iron helmet: reveals nearby hostile mobs (Glowing) while worn. */
    public static final DeferredItem<Item> POTATO_GOGGLES = MOD_ITEMS.registerItem("potato_goggles",
            props -> new ArmorItem(FunnyArmorMaterials.POTATO, ArmorItem.Type.HELMET, props.stacksTo(1)));

    /** Netherite chestplate: reflects 50% of incoming damage back to the attacker. */
    public static final DeferredItem<Item> THORNS_SHIELD = MOD_ITEMS.registerItem("thorns_shield",
            props -> new ArmorItem(FunnyArmorMaterials.THORNS, ArmorItem.Type.CHESTPLATE, props.stacksTo(1)));

    private ModItems() {
    }

    // -- Event handlers --------------------------------------------------------

    /**
     * All gameplay event subscriptions for Funny Effects items. Registered on the NeoForge
     * event bus from {@link FunnyEffectsMod}'s constructor.
     */
    public static final class ItemEvents {

        private ItemEvents() {
        }

        /**
         * Magnet glove: pull nearby dropped items toward the player while held in either hand.
         */
        @SubscribeEvent
        static void onPlayerTick(@NotNull PlayerTickEvent.Post event) {
            Player player = event.getEntity();
            if (player.level().isClientSide()) {
                return;
            }
            ItemStack main = player.getMainHandItem();
            ItemStack off = player.getOffhandItem();
            if (!main.is(MAGNETIC_GLOVE.get()) && !off.is(MAGNETIC_GLOVE.get())) {
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
        static void onLivingDamageLifesteal(@NotNull LivingDamageEvent.Post event) {
            LivingEntity target = event.getEntity();
            if (target.level().isClientSide()) {
                return;
            }
            Entity direct = event.getSource().getDirectEntity();
            if (!(direct instanceof Player player)) {
                return;
            }
            if (!isHolding(player, LIFESTEAL_BLADE.get())) {
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
        static void onIncomingDamage(@NotNull LivingIncomingDamageEvent event) {
            LivingEntity victim = event.getEntity();
            if (victim.level().isClientSide()) {
                return;
            }
            ItemStack chest = victim.getItemBySlot(EquipmentSlot.CHEST);
            if (!chest.is(THORNS_SHIELD.get())) {
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
        static void onBlockBreak(@NotNull BlockEvent.BreakEvent event) {
            Player player = event.getPlayer();
            if (player.level().isClientSide()) {
                return;
            }
            if (!isHolding(player, SMELTER_PICK.get())) {
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
            RecipeManager recipeManager = serverLevel.getRecipeManager();
            for (ItemStack drop : drops) {
                if (drop.isEmpty()) {
                    continue;
                }
                Optional<RecipeHolder<SmeltingRecipe>> recipe = recipeManager.getRecipeFor(RecipeType.SMELTING,
                        new SingleRecipeInput(drop), serverLevel);
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
        static void onPlayerTickGoggles(@NotNull PlayerTickEvent.Post event) {
            Player player = event.getEntity();
            if (player.level().isClientSide()) {
                return;
            }
            ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
            if (!head.is(POTATO_GOGGLES.get())) {
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
        static void onRightClick(@NotNull PlayerInteractEvent.RightClickItem event) {
            Player player = event.getEntity();
            ItemStack stack = event.getItemStack();
            if (!stack.is(SQUEAKY_TOY.get())) {
                return;
            }
            Level level = player.level();
            if (level.isClientSide()) {
                return;
            }
            Vec3 pos = player.position();
            BlockPos soundPos = BlockPos.containing(pos);
            level.playSound(null, soundPos, SoundEvents.BAT_TAKEOFF, SoundSource.PLAYERS, 1.0F, 1.0F);
            AABB area = new AABB(soundPos).inflate(5.0D);
            for (LivingEntity mob : level.getEntitiesOfClass(LivingEntity.class, area)) {
                if (mob instanceof Enemy) {
                    mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2, false, true));
                }
            }
        }

        private static boolean isHolding(@NotNull Player player, @NotNull Item item) {
            return player.getMainHandItem().is(item) || player.getOffhandItem().is(item);
        }
    }

    // -- Flatulent bean custom item -------------------------------------------

    /**
     * Food item that, on consumption, emits an explosion knockback AoE around the eater.
     * Nausea II is applied via the food effect builder.
     */
    public static final class FlatulentBeanItem extends Item {
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
            if (!level.isClientSide() && livingEntity instanceof Player) {
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

    // -- Custom armor materials (record-based ArmorMaterial + Holder) ---------

    /**
     * Armor materials for Funny Effects custom armor. In NeoForge 1.21.1 {@link ArmorMaterial}
     * is an immutable record, so materials are built as {@link Holder<ArmorMaterial>} via the
     * registry (no deprecated vanilla enum usage).
     */
    public static final class FunnyArmorMaterials {

        private FunnyArmorMaterials() {
        }

        /** Iron-tier helmet material (2 armor, 2.5 toughness). */
        public static final Holder<ArmorMaterial> POTATO = register("potato",
                defense(2, 0, 0, 0), 9, SoundEvents.ARMOR_EQUIP_IRON, 0.0F, 2.5F,
                () -> Ingredient.of(Items.POTATO));

        /** Netherite-tier chestplate material (8 armor, 4 toughness). */
        public static final Holder<ArmorMaterial> THORNS = register("thorns",
                defense(0, 8, 0, 0), 15, SoundEvents.ARMOR_EQUIP_NETHERITE, 3.0F, 4.0F,
                () -> Ingredient.of(Items.NETHERITE_INGOT));

        private static @NotNull EnumMap<ArmorItem.Type, Integer> defense(int helmet, int chestplate,
                                                                          int leggings, int boots) {
            EnumMap<ArmorItem.Type, Integer> map = new EnumMap<>(ArmorItem.Type.class);
            map.put(ArmorItem.Type.HELMET, helmet);
            map.put(ArmorItem.Type.CHESTPLATE, chestplate);
            map.put(ArmorItem.Type.LEGGINGS, leggings);
            map.put(ArmorItem.Type.BOOTS, boots);
            return map;
        }

        private static @NotNull Holder<ArmorMaterial> register(@NotNull String name,
                                                                @NotNull EnumMap<ArmorItem.Type, Integer> defense,
                                                                int enchantmentValue,
                                                                @NotNull Holder<SoundEvent> equipSound,
                                                                float toughness, float knockbackResistance,
                                                                @NotNull Supplier<Ingredient> repairIngredient) {
            Objects.requireNonNull(name, "name must not be null");
            Objects.requireNonNull(defense, "defense must not be null");
            Objects.requireNonNull(equipSound, "equipSound must not be null");
            Objects.requireNonNull(repairIngredient, "repairIngredient must not be null");
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(FunnyEffectsMod.MOD_ID, name);
            ArmorMaterial material = new ArmorMaterial(defense, enchantmentValue, equipSound,
                    repairIngredient, List.of(new ArmorMaterial.Layer(location)), toughness, knockbackResistance);
            return Registry.registerForHolder(BuiltInRegistries.ARMOR_MATERIAL, location, material);
        }
    }
}
