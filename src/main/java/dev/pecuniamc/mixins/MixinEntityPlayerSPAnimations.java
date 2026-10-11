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

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * EntityPlayerSP side of 1.7 animations: restores block-hitting (swinging the
 * sword while right-click blocking) without touching OptiFine's player
 * renderer. Pure {@code @Inject}, no overwrites.
 */
@Mixin(EntityPlayerSP.class)
public abstract class MixinEntityPlayerSPAnimations {

    /**
     * Each tick, if the player is blocking with a sword while also attacking
     * (1.7 block-hit), force an arm swing so the ItemRenderer hook has motion
     * to work with.
     */
    @Inject(method = {"onLivingUpdate", "func_70636_d"}, at = @At("RETURN"))
    public void pecuniaBlockHitSwing(CallbackInfo ci) {
        try {
            EntityPlayerSP self = (EntityPlayerSP) (Object) this;
            if (self.worldObj == null) {
                return;
            }
            ItemStack held = self.getHeldItem();
            if (held == null || held.getItemUseAction() != EnumAction.BLOCK) {
                return;
            }
            if (self.getItemInUseCount() <= 0) {
                return;
            }
            boolean animEnabled = true;
            try {
                animEnabled = dev.frostclient.Frost.INSTANCE.settingManager
                        .getSettingByModAndName("Animation", "Block Animation").isCheckToggled();
            } catch (Throwable ignored) {
            }
            if (!animEnabled) {
                return;
            }
            if (self.isSwingInProgress) {
                return;
            }
            // Only swing when the attack key is held (real block-hitting).
            if (net.minecraft.client.Minecraft.getMinecraft().gameSettings.keyBindAttack.isKeyDown()) {
                self.swingItem();
            }
        } catch (Throwable ignored) {
        }
    }
}
