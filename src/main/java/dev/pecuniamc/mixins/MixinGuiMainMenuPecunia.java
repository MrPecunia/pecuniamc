/*
 * Copyright (C) 2026 PrPecunia, Webget, Diamond Gaming, Eris Group, Pecunia Enterprise
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 */

package dev.pecuniamc.mixins;

import dev.pecuniamc.gui.GuiPecuniaMainMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces the vanilla {@link GuiMainMenu} with the Lunar-style
 * {@link GuiPecuniaMainMenu}. Runs after Frost's own GuiMainMenuMixin; the
 * Pecunia menu wins because it re-displays itself on top.
 */
@Mixin(GuiMainMenu.class)
public abstract class MixinGuiMainMenuPecunia extends GuiScreen {

    @Inject(method = {"initGui", "func_73866_w_"}, at = @At("RETURN"))
    public void pecuniaInitGui(CallbackInfo ci) {
        try {
            if (!(mc.currentScreen instanceof GuiPecuniaMainMenu)) {
                mc.displayGuiScreen(new GuiPecuniaMainMenu());
            }
        } catch (Throwable ignored) {
        }
    }
}
