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

import dev.pecuniamc.danger.ArsenicInventoryFix;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ARSENIC UI BUG FIX (mixin side).
 *
 * Arsenic injects {@code GuiHooks.containerInitGuiTail} into every
 * {@link GuiContainer}, adding {@code GuiButton(7331, "KillAura: ON/OFF")}
 * to the vanilla player inventory. This mixin runs after
 * {@code initGui} and forcibly strips that element. The Forge-event twin is
 * {@link ArsenicInventoryFix}; both use the same strip logic so whichever
 * runs last wins.
 */
@Mixin(GuiContainer.class)
public abstract class MixinGuiContainerInventoryFix extends GuiScreen {

    @Inject(method = {"initGui", "func_73866_w_"}, at = @At("RETURN"))
    public void pecuniaStripKillAuraButton(CallbackInfo ci) {
        try {
            ArsenicInventoryFix.stripKillAuraButton((GuiScreen) (Object) this);
        } catch (Throwable ignored) {
        }
    }
}
