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
                });
        return builder.build();
    }
}
