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

package dev.pecuniamc.gui;

import dev.pecuniamc.PecuniaCore;
import net.minecraft.client.gui.GuiScreen;

/**
 * Custom splash screen: flat dark background with "PecuniaMC is loading...".
 * Used by {@link dev.pecuniamc.mixins.MixinSplashProgress} to replace the
 * default Forge mod-loading text.
 */
public class GuiPecuniaSplash extends GuiScreen {

    private String status = "Loading...";
    private int progress = 0;

    public void setStatus(String status, int progress) {
        this.status = status == null ? "Loading..." : status;
        this.progress = Math.max(0, Math.min(100, progress));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        try {
            if (fontRendererObj == null) {
                return;
            }
            drawGradientRect(0, 0, width, height, 0xFF14171C, 0xFF1E232B);
        int cx = width / 2;
        int cy = height / 2;
        fontRendererObj.drawString(PecuniaCore.CLIENT_NAME + " is loading...",
                cx - fontRendererObj.getStringWidth(PecuniaCore.CLIENT_NAME + " is loading...") / 2,
                cy - 20, 0xFFFFFFFF);
        String sub = status + "  " + progress + "%";
        fontRendererObj.drawString(sub, cx - fontRendererObj.getStringWidth(sub) / 2,
                cy - 4, 0xFF8A94A3);
        // Flat progress bar.
        int bw = 200;
        drawRect(cx - bw / 2, cy + 12, cx + bw / 2, cy + 18, 0xFF232A33);
        int fill = (int) (bw * (progress / 100.0F));
        drawRect(cx - bw / 2, cy + 12, cx - bw / 2 + fill, cy + 18, 0xFF5EB1FF);
        } catch (Throwable ignored) {
        }
    }
}
