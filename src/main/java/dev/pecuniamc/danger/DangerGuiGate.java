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

import arsenic.gui.click.ClickGuiScreen;
import dev.pecuniamc.PecuniaCore;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Gates the embedded Arsenic ClickGUI.
 *
 * The ClickGUI opens ONLY through PecuniaMC's own Numpad-0 handler while
 * danger mode is ON. Every other open attempt (notably Arsenic's own RSHIFT
 * hook, so RSHIFT always stays on the legit PecuniaMC GUI) is cancelled, as
 * is any attempt while danger mode is OFF.
 */
public class DangerGuiGate {

    private static boolean allowOnce;

    /** Permits exactly one upcoming ClickGUI open (our Numpad-0 handler). */
    public static void allowClickGuiOnce() {
        allowOnce = true;
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        try {
            if (event == null || event.gui == null) {
                return;
            }
            if (!(event.gui instanceof ClickGuiScreen)) {
                return;
            }
            if (!PecuniaCore.isDangerModeEnabled()) {
                event.setCanceled(true);
                return;
            }
            if (allowOnce) {
                allowOnce = false;
            } else {
                event.setCanceled(true);
            }
        } catch (Throwable ignored) {
        }
    }
}
