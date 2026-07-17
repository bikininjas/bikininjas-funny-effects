package com.bikininjas.funnyeffects.item;

import com.bikininjas.corelib.log.LogManager;
import com.bikininjas.corelib.log.ModLogger;
import com.bikininjas.funnyeffects.FunnyEffectsMod;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Craftable item that gives a random item from {@code #funnyeffects:mystery_box_loot} on right-click.
 * Consumed on use. Stacks to 1.
 */
public final class MysteryBoxItem extends Item {

    private static final ModLogger LOGGER = LogManager.getLogger(FunnyEffectsMod.MOD_ID, MysteryBoxItem.class);
    private static final Random RANDOM = new Random();
    private static final TagKey<Item> LOOT_TAG = TagKey.create(BuiltInRegistries.ITEM.key(),
            ResourceLocation.fromNamespaceAndPath(FunnyEffectsMod.MOD_ID, "mystery_box_loot"));

    public MysteryBoxItem(@NotNull Properties properties) {
        super(Objects.requireNonNull(properties, "properties must not be null"));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player,
                                                            @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }
        var items = getLootItems();
        if (items.isEmpty()) {
            LOGGER.warn("Mystery box loot tag is empty — no items to give");
            return InteractionResultHolder.pass(stack);
        }
        ItemStack loot = new ItemStack(items.get(RANDOM.nextInt(items.size())));
        player.getInventory().placeItemBackInInventory(loot);
        stack.shrink(1);
        LOGGER.info("Player {} opened mystery box — got {}", player.getName().getString(),
                BuiltInRegistries.ITEM.getKey(loot.getItem()));
        return InteractionResultHolder.consume(stack);
    }

    private static List<Item> getLootItems() {
        var items = new ArrayList<Item>();
        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(LOOT_TAG)) {
            items.add(holder.value());
        }
        return items;
    }
}
