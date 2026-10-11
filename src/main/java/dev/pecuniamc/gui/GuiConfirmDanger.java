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
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

/**
 * Safety confirm box shown by {@code /dangermode on}.
 *
 * Warns that enabling danger mode on servers is not safe and requires typing
 * {@code CONFIRM} (exact, uppercase) to proceed. The enable button stays
 * disabled until the text matches.
 */
public class GuiConfirmDanger extends GuiScreen {

    private final GuiScreen parent;
    private GuiTextField confirmField;
    private static volatile boolean openRequested;

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

    public GuiConfirmDanger(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        String preserved = null;
        try {
            if (confirmField != null) {
                preserved = confirmField.getText();
            }
        } catch (Throwable ignored) {
        }
        buttonList.clear();
        int cx = width / 2;
        buttonList.add(new GuiButton(0, cx - 155, height / 2 + 52, 150, 20, "Enable Danger Mode"));
        buttonList.add(new GuiButton(1, cx + 5, height / 2 + 52, 150, 20, "Cancel"));
        confirmField = new GuiTextField(2, fontRendererObj, cx - 155, height / 2 + 24, 310, 20);
        confirmField.setMaxStringLength(16);
        confirmField.setFocused(true);
        if (preserved != null) {
            try {
                confirmField.setText(preserved);
            } catch (Throwable ignored) {
            }
        }
        updateEnableButton();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    private boolean isConfirmed() {
        try {
            return confirmField != null && "CONFIRM".equals(confirmField.getText().trim());
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void updateEnableButton() {
        try {
            for (Object o : buttonList) {
                if (o instanceof GuiButton) {
                    GuiButton button = (GuiButton) o;
                    if (button.id == 0) {
                        button.enabled = isConfirmed();
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0 && isConfirmed()) {
            PecuniaCore.setDangerModeEnabled(true);
            mc.displayGuiScreen(null);
        } else if (button.id == 1) {
            mc.displayGuiScreen(parent);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        try {
            if (confirmField != null && confirmField.textboxKeyTyped(typedChar, keyCode)) {
                updateEnableButton();
                return;
            }
        } catch (Throwable ignored) {
        }
        if (keyCode == Keyboard.KEY_RETURN && isConfirmed()) {
            PecuniaCore.setDangerModeEnabled(true);
            mc.displayGuiScreen(null);
            return;
        }
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(parent);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        try {
            if (confirmField != null) {
                confirmField.mouseClicked(mouseX, mouseY, mouseButton);
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        try {
            if (confirmField != null) {
                confirmField.updateCursorCounter();
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int cx = width / 2;
        int cy = height / 2;
        drawRect(cx - 170, cy - 80, cx + 170, cy + 80, 0xFF14171C);
        drawRect(cx - 170, cy - 80, cx + 170, cy - 78, 0xFF5EB1FF);

        drawCenteredString(fontRendererObj, "Danger Mode", cx, cy - 68, 0xFFFFFFFF);
        drawCenteredString(fontRendererObj, "Enabling Danger Mode loads cheat modules.", cx, cy - 48, 0xFFE8ECF1);
        drawCenteredString(fontRendererObj, "This is NOT SAFE on servers.", cx, cy - 34, 0xFFFF5555);
        drawCenteredString(fontRendererObj, "Anticheats may detect and ban you.", cx, cy - 22, 0xFFFF5555);
        drawCenteredString(fontRendererObj, "Type CONFIRM below to enable it.", cx, cy - 8, 0xFF8A94A3);

        try {
            if (confirmField != null) {
                confirmField.drawTextBox();
            }
        } catch (Throwable ignored) {
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
