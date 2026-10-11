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
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

import java.util.List;

/**
 * Basic client details screen opened from the Main Menu "Info" button.
 * Strictly shows innocuous client info; never mentions cheat/danger mode.
 */
public class GuiPecuniaInfo extends GuiScreen {

    private static final String DESCRIPTION = "PecuniaMC is the ultimate, 100% free and open-source "
            + "Minecraft PvP client for version 1.8.9. Developed by MrPecunia, it is fully "
            + "optimized to deliver a massive FPS boost and seamless gameplay performance. "
            + "Built with a strict focus on privacy, PecuniaMC features zero telemetry or "
            + "tracking, giving you a clean, secure, and highly competitive edge on your "
            + "favorite PvP servers.";

    private final GuiScreen parent;

    public GuiPecuniaInfo(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        buttonList.add(new GuiButton(0, width / 2 - 100, height - 40, 200, 20, "Back"));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int cx = width / 2;
        int cw = 300;
        List<String> lines;
        try {
            lines = fontRendererObj.listFormattedStringToWidth(DESCRIPTION, cw - 24);
        } catch (Throwable ignored) {
            lines = new java.util.ArrayList<String>();
            lines.add(DESCRIPTION);
        }
        int ch = 58 + lines.size() * 12;
        int cy = height / 2 - ch / 2;
        drawRect(cx - cw / 2, cy, cx + cw / 2, cy + ch, 0xFF1E232B);
        drawRect(cx - cw / 2, cy, cx + cw / 2, cy + 2, 0xFF5EB1FF);

        drawCenteredString(fontRendererObj, PecuniaCore.CLIENT_NAME,
                cx, cy + 12, 0xFFFFFFFF);
        drawCenteredString(fontRendererObj, "Version " + PecuniaCore.CLIENT_VERSION
                + "  (" + PecuniaCore.MC_VERSION + ")", cx, cy + 24, 0xFF8A94A3);
        drawRect(cx - cw / 2 + 12, cy + 38, cx + cw / 2 - 12, cy + 39, 0xFF2D3540);
        int ly = cy + 46;
        for (String line : lines) {
            try {
                fontRendererObj.drawString(line, cx - cw / 2 + 12, ly, 0xFFE8ECF1);
            } catch (Throwable ignored) {
            }
            ly += 12;
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            mc.displayGuiScreen(parent);
        }
    }
}
