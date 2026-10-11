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

import dev.frostclient.helpers.render.Helper2D;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Credits popup opened by {@code /author}.
 *
 * Shows the contributors with their skin-face avatar, role and GitHub link.
 * Click a row to select it; "Open GitHub" opens the selected profile in the
 * browser (falls back to printing the link in chat). A chest-open sound plays
 * when the popup appears. To add a contributor, append one line to
 * {@link #contributors} and drop a 64x64 helm PNG under
 * {@code assets/frostclient/pictures/}.
 */
public class GuiAuthorCredits extends GuiScreen {

    private static final class Contributor {
        final String name;
        final String role;
        final String url;
        final String avatar;

        Contributor(String name, String role, String url, String avatar) {
            this.name = name;
            this.role = role;
            this.url = url;
            this.avatar = avatar;
        }
    }

    private static final List<Contributor> contributors = new ArrayList<Contributor>();

    static {
        contributors.add(new Contributor("MrPecunia", "dev",
                "https://github.com/MrPecunia", "pictures/mrpecunia.png"));
    }

    private static volatile boolean openRequested;
    private int selected;

    /** Deferred open: called from chat input, consumed on the next client tick. */
    public static void requestOpen() {
        openRequested = true;
    }

    /** Returns true once per request. Called from the client tick handler. */
    public static boolean pollOpenRequest() {
        if (!openRequested) {
            return false;
        }
        openRequested = false;
        return true;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        int cx = width / 2;
        int cy = height / 2 - cardHeight() / 2;
        buttonList.add(new GuiButton(1, cx - 155, cy + cardHeight() - 28, 150, 20, "Open GitHub"));
        buttonList.add(new GuiButton(0, cx + 5, cy + cardHeight() - 28, 150, 20, "Close"));
        playChestSound();
    }

    private void playChestSound() {
        try {
            if (mc != null && mc.thePlayer != null) {
                mc.thePlayer.playSound("random.chestopen", 1.0F, 1.0F);
            }
        } catch (Throwable ignored) {
        }
    }

    private int cardHeight() {
        return 84 + contributors.size() * 42 + 40;
    }

    private int rowTop(int index, int cy) {
        return cy + 78 + index * 42;
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 1) {
            openSelectedGithub();
        } else if (button.id == 0) {
            mc.displayGuiScreen(null);
        }
    }

    private Contributor selectedContributor() {
        try {
            if (selected >= 0 && selected < contributors.size()) {
                return contributors.get(selected);
            }
        } catch (Throwable ignored) {
        }
        return contributors.isEmpty() ? null : contributors.get(0);
    }

    private void openSelectedGithub() {
        Contributor contributor = selectedContributor();
        String url = contributor == null ? null : contributor.url;
        if (url == null || url.isEmpty()) {
            return;
        }
        try {
            Object desktop = Class.forName("java.awt.Desktop").getMethod("getDesktop").invoke(null);
            desktop.getClass().getMethod("browse", URI.class).invoke(desktop, new URI(url));
        } catch (Throwable t) {
            try {
                if (mc != null && mc.thePlayer != null) {
                    mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(
                            "\u00A77Open in your browser: \u00A7b" + url));
                }
            } catch (Throwable ignored) {
            }
            System.out.println("[PecuniaMC] Could not open browser: " + t.getMessage());
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton != 0) {
            return;
        }
        try {
            int cx = width / 2;
            int cw = 340;
            int cy = height / 2 - cardHeight() / 2;
            for (int i = 0; i < contributors.size(); i++) {
                int ry = rowTop(i, cy);
                if (mouseX >= cx - cw / 2 + 10 && mouseX <= cx + cw / 2 - 10
                        && mouseY >= ry && mouseY <= ry + 38) {
                    selected = i;
                    try {
                        if (mc != null && mc.thePlayer != null) {
                            mc.thePlayer.playSound("random.click", 1.0F, 1.0F);
                        }
                    } catch (Throwable ignored) {
                    }
                    return;
                }
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int cx = width / 2;
        int cw = 340;
        int ch = cardHeight();
        int cy = height / 2 - ch / 2;
        drawRect(cx - cw / 2, cy, cx + cw / 2, cy + ch, 0xFF1E232B);
        drawRect(cx - cw / 2, cy, cx + cw / 2, cy + 2, 0xFF5EB1FF);

        drawCenteredString(fontRendererObj, "PecuniaMC", cx, cy + 12, 0xFFFFFFFF);
        drawCenteredString(fontRendererObj, "Special thanks to our contributors", cx, cy + 26, 0xFF8A94A3);
        drawCenteredString(fontRendererObj, "for building this client.", cx, cy + 38, 0xFF8A94A3);

        for (int i = 0; i < contributors.size(); i++) {
            Contributor contributor = contributors.get(i);
            int ry = rowTop(i, cy);
            if (i == selected) {
                drawRect(cx - cw / 2 + 10, ry, cx + cw / 2 - 10, ry + 38, 0xFF2D3540);
                drawRect(cx - cw / 2 + 10, ry, cx - cw / 2 + 12, ry + 38, 0xFF5EB1FF);
            }
            try {
                Helper2D.drawPicture(cx - cw / 2 + 16, ry + 3, 32, 32, 0, contributor.avatar);
            } catch (Throwable ignored) {
            }
            int tx = cx - cw / 2 + 56;
            String title = contributor.name;
            String url = contributor.url;
            String role = contributor.role;
            if (title == null) {
                title = "?";
            }
            if (url == null) {
                url = "";
            }
            if (role == null) {
                role = "";
            }
            fontRendererObj.drawString(title + "  \u00A77" + role, tx, ry + 5, 0xFFFFFFFF);
            fontRendererObj.drawString(url, tx, ry + 20, 0xFF5EB1FF);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) {
            mc.displayGuiScreen(null);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }
}
