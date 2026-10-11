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

package dev.pecuniamc.danger;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;

/**
 * ARSENIC UI BUG FIX.
 *
 * Arsenic's {@code GuiHooks.containerInitGuiTail} adds a
 * {@code GuiButton(KILLAURA_BUTTON_ID = 7331, "KillAura: ON/OFF")} to every
 * {@link GuiContainer} (including the vanilla player inventory
 * {@code GuiInventory}). This hook forcibly removes that element so the
 * vanilla inventory stays clean.
 *
 * Implemented as a Forge event (no hard Arsenic dependency) plus a Mixin
 * ({@link dev.pecuniamc.mixins.MixinGuiContainerInventoryFix}) for the case
 * where Arsenic re-adds the button after our event runs.
 */
public class ArsenicInventoryFix {

    /** Must match arsenic.runtime.hooks.GuiHooks.KILLAURA_BUTTON_ID. */
    public static final int KILLAURA_BUTTON_ID = 7331;

    @SubscribeEvent
    public void onGuiInit(GuiScreenEvent.InitGuiEvent event) {
        try {
            if (event == null || event.gui == null) {
                return;
            }
            if (!(event.gui instanceof GuiContainer)) {
                return;
            }
            stripKillAuraButton(event.gui);
        } catch (Throwable t) {
            System.out.println("[PecuniaMC] ArsenicInventoryFix failed: " + t.getMessage());
        }
    }

    @SubscribeEvent
    public void onGuiDraw(GuiScreenEvent.DrawScreenEvent.Pre event) {
        try {
            if (event == null || event.gui == null) {
                return;
            }
            if (!(event.gui instanceof GuiContainer)) {
                return;
            }
            // Defensive: Arsenic refreshes the label every frame in
            // containerDrawScreenHead; strip again in case it was re-added.
            stripKillAuraButton(event.gui);
        } catch (Throwable ignored) {
        }
    }

    /**
     * Removes any button with id 7331 or a "killaura" label from the screen.
     * Public so the Mixin can reuse the exact same logic.
     */
    @SuppressWarnings("unchecked")
    public static void stripKillAuraButton(GuiScreen gui) {
        if (gui == null) {
            return;
        }
        try {
            List<GuiButton> buttons = getButtonList(gui);
            if (buttons == null || buttons.isEmpty()) {
                return;
            }
            Iterator<GuiButton> it = buttons.iterator();
            while (it.hasNext()) {
                GuiButton button = it.next();
                if (button == null) {
                    continue;
                }
                boolean idMatch = button.id == KILLAURA_BUTTON_ID;
                boolean labelMatch = false;
                try {
                    String label = button.displayString;
                    labelMatch = label != null && label.toLowerCase().contains("killaura");
                } catch (Throwable ignored) {
                }
                if (idMatch || labelMatch) {
                    button.visible = false;
                    button.enabled = false;
                    it.remove();
                }
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * Version-proof accessor for GuiScreen.buttonList (protected, and named
     * {@code field_146292_n} outside the dev workspace).
     */
    @SuppressWarnings("unchecked")
    private static List<GuiButton> getButtonList(GuiScreen gui) {
        try {
            Field field;
            try {
                field = GuiScreen.class.getDeclaredField("buttonList");
            } catch (NoSuchFieldException e) {
                field = GuiScreen.class.getDeclaredField("field_146292_n");
            }
            field.setAccessible(true);
            return (List<GuiButton>) field.get(gui);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
