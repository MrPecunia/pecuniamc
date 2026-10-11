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
import dev.pecuniamc.network.PecuniaWebSocket;
import dev.pecuniamc.network.ProfileManager;
import dev.pecuniamc.network.RankNickManager;
import dev.pecuniamc.network.SpoofedProfile;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Pecunia Network status screen opened from the Main Menu "Status" button.
 *
 * Shows whether the client is synced for ranks, nicknames and capes, plus a
 * short note explaining the client connects only for that reason.
 */
public class GuiPecuniaStatus extends GuiScreen {

    private static final String NOTE = "PecuniaMC connects to the Pecunia Network only to sync "
            + "ranks, nicknames and capes between PecuniaMC players. No telemetry, "
            + "no tracking: nothing else ever leaves your client.";

    private final GuiScreen parent;

    public GuiPecuniaStatus(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        buttonList.add(new GuiButton(0, width / 2 - 100, height - 40, 200, 20, "Back"));
    }

    private String connectionLine() {
        try {
            PecuniaWebSocket socket = PecuniaCore.getSocket();
            if (socket == null) {
                return "\u00A7cDisconnected \u00A77(not initialized)";
            }
            if (socket.isOpen()) {
                return "\u00A7aConnected";
            }
            try {
                if (socket.getReadyState() == org.java_websocket.enums.ReadyState.NOT_YET_CONNECTED) {
                    return "\u00A7eConnecting...";
                }
            } catch (Throwable ignored) {
            }
            return "\u00A7cDisconnected";
        } catch (Throwable ignored) {
            return "\u00A77Unknown";
        }
    }

    private int syncedProfiles() {
        try {
            Map<UUID, SpoofedProfile> snapshot = ProfileManager.snapshot();
            return snapshot == null ? 0 : snapshot.size();
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private int syncedCapes() {
        try {
            Map<UUID, SpoofedProfile> snapshot = ProfileManager.snapshot();
            if (snapshot == null) {
                return 0;
            }
            int count = 0;
            for (SpoofedProfile profile : snapshot.values()) {
                try {
                    if (profile != null && profile.getCapeUrl() != null
                            && !profile.getCapeUrl().isEmpty()) {
                        count++;
                    }
                } catch (Throwable ignored) {
                }
            }
            return count;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private String localLine() {
        try {
            String rank = RankNickManager.getLocalRank();
            String nick = RankNickManager.getLocalNick();
            boolean hasRank = rank != null && !rank.isEmpty();
            boolean hasNick = nick != null && !nick.isEmpty();
            if (!hasRank && !hasNick) {
                return "\u00A77none (use /setrank and /setnick in game)";
            }
            String shown = (hasRank ? RankNickManager.translateColorCodes(rank) : "")
                    + (hasRank && hasNick ? " " : "")
                    + (hasNick ? nick : "");
            return shown + "\u00A7r";
        } catch (Throwable ignored) {
            return "\u00A77Unknown";
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int cx = width / 2;
        int cw = 300;
        List<String> noteLines;
        try {
            noteLines = fontRendererObj.listFormattedStringToWidth(NOTE, cw - 24);
        } catch (Throwable ignored) {
            noteLines = new java.util.ArrayList<String>();
            noteLines.add(NOTE);
        }
        int ch = 108 + noteLines.size() * 11;
        int cy = height / 2 - ch / 2;
        drawRect(cx - cw / 2, cy, cx + cw / 2, cy + ch, 0xFF1E232B);
        drawRect(cx - cw / 2, cy, cx + cw / 2, cy + 2, 0xFF5EB1FF);

        drawCenteredString(fontRendererObj, "Network Status", cx, cy + 12, 0xFFFFFFFF);

        int y = cy + 42;
        drawCenteredString(fontRendererObj, "Connection: " + connectionLine(), cx, y, 0xFFE8ECF1);
        y += 12;
        drawCenteredString(fontRendererObj, "Ranks & nicks synced: \u00A7b" + syncedProfiles(),
                cx, y, 0xFFE8ECF1);
        y += 12;
        drawCenteredString(fontRendererObj, "Capes synced: \u00A7b" + syncedCapes(),
                cx, y, 0xFFE8ECF1);
        y += 12;
        fontRendererObj.drawString("Your profile: ", cx - cw / 2 + 12, y, 0xFF8A94A3);
        try {
            fontRendererObj.drawString(localLine(),
                    cx - cw / 2 + 12 + fontRendererObj.getStringWidth("Your profile: "), y, 0xFFE8ECF1);
        } catch (Throwable ignored) {
        }
        y += 16;
        for (String line : noteLines) {
            try {
                fontRendererObj.drawString(line, cx - cw / 2 + 12, y, 0xFF8A94A3);
            } catch (Throwable ignored) {
            }
            y += 11;
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
