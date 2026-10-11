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

import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.client.SplashProgress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Custom splash: overrides Forge's loading screen to display
 * "PecuniaMC is loading..." and hides the default Forge mod-loading text.
 *
 * Targets {@link SplashProgress} (present in the Forge 1.8.9 runtime).
 * {@code remap = false} everywhere: Forge classes are not obfuscated, so no
 * refmap translation must be attempted. Only {@code drawVanillaScreen} is
 * hooked because it is the only splash entry point that exists in Forge
 * 1.8.9 (verified via javap: there is no {@code drawScreen} method on this
 * class, and a dead inject target crashes production even with
 * {@code require = 0} on Mixin 0.7.11).
 */
@Mixin(value = SplashProgress.class, remap = false)
public abstract class MixinSplashProgress {

    @Inject(method = "drawVanillaScreen", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void pecuniaDrawVanillaScreen(CallbackInfo ci) {
        try {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
            if (mc == null) {
                return;
            }
            drawPecuniaSplash(mc);
            // Cancel vanilla text rendering; we draw our own below.
            ci.cancel();
        } catch (Throwable ignored) {
        }
    }

    private static void drawPecuniaSplash(net.minecraft.client.Minecraft mc) {
        try {
            int w = mc.displayWidth;
            int h = mc.displayHeight;
            // Flat dark background.
            GlStateManager.pushMatrix();
            GlStateManager.disableTexture2D();
            net.minecraft.client.gui.Gui.drawRect(0, 0, w, h, 0xFF14171C);
            GlStateManager.enableTexture2D();
            GlStateManager.popMatrix();
            String title = "PecuniaMC is loading...";
            mc.fontRendererObj.drawString(title,
                    w / 2 - mc.fontRendererObj.getStringWidth(title) / 2,
                    h / 2 - 20, 0xFFFFFFFF);
        } catch (Throwable ignored) {
        }
    }
}
