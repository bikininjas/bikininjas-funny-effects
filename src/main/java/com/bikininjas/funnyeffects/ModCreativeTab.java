package com.bikininjas.funnyeffects;

import com.bikininjas.funnyeffects.item.ModItems;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Creative mode tab for the Funny Effects mod, containing all 8 custom items.
 * The tab icon uses {@link ModItems#BOUNCY_SLIME}.
 */
public final class ModCreativeTab {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB, FunnyEffectsMod.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FUNNY_EFFECTS_TAB =
            CREATIVE_TABS.register("funny_effects_tab", ModCreativeTab::build);

    private ModCreativeTab() {
    }

    private static @NotNull CreativeModeTab build() {
        CreativeModeTab.Builder builder = CreativeModeTab.builder()
                .title(net.minecraft.network.chat.Component.translatable("itemGroup.funnyeffects"))
                .icon(() -> new ItemStack(ModItems.BOUNCY_SLIME.get()))
                .displayItems((parameters, output) -> {
                    output.accept(ModItems.BOUNCY_SLIME.get());
                    output.accept(ModItems.MAGNETIC_GLOVE.get());
                    output.accept(ModItems.FLATULENT_BEAN.get());
                    output.accept(ModItems.SQUEAKY_TOY.get());
                    output.accept(ModItems.LIFESTEAL_BLADE.get());
                    output.accept(ModItems.SMELTER_PICK.get());
                    output.accept(ModItems.POTATO_GOGGLES.get());
                    output.accept(ModItems.THORNS_SHIELD.get());
                    output.accept(ModItems.YEETER_HAMMER.get());
                    output.accept(ModItems.THUNDER_SWORD.get());
                    output.accept(ModItems.CHICKEN_WAND.get());
                    output.accept(ModItems.RAINBOW_BOOTS.get());
                    output.accept(ModItems.XP_MAGNET.get());
                    output.accept(ModItems.VOID_PEARL.get());
                    output.accept(ModItems.DISCO_SWORD.get());
                    output.accept(ModItems.GRAVITY_PICKAXE.get());
                    output.accept(ModItems.BOUNCY_BOOTS.get());
                    output.accept(ModItems.SNEAKY_HELMET.get());
                    output.accept(ModItems.SLAPFISH.get());
                    output.accept(ModItems.REPLANTER_HOE.get());
                    output.accept(ModItems.DINNERBONE_BAT.get());
                    output.accept(ModItems.PARTY_POPPER.get());
                    output.accept(ModItems.GRAVITY_ANCHOR.get());
                    output.accept(ModItems.MOB_CATCHER.get());
                    output.accept(ModItems.INFINITE_PEARL.get());
                    output.accept(ModItems.TREECAPITATOR.get());
                    output.accept(ModItems.LAVA_WALKER.get());
                    output.accept(ModItems.MYSTERY_BOX.get());
                    output.accept(ModItems.VOID_PEARL_TRAP.get());
                    output.accept(ModItems.INFINITE_WATER_BUCKET.get());
                    output.accept(ModItems.INFINITE_LAVA_BUCKET.get());
                    output.accept(ModItems.EXPLOSIVE_ARROW.get());
                    output.accept(ModItems.TELEPORT_ARROW.get());
                    output.accept(ModItems.CHICKEN_ARROW.get());
                    output.accept(ModItems.CONFUSION_ARROW.get());
                    output.accept(ModItems.LIGHTNING_ARROW.get());
                });
        return builder.build();
    }
}
