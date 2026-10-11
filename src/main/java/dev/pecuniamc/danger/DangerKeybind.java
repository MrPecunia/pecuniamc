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

import dev.pecuniamc.PecuniaCore;
import dev.pecuniamc.gui.GuiAuthorCredits;
import dev.pecuniamc.gui.GuiConfirmContent;
import dev.pecuniamc.gui.GuiConfirmDanger;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

/**
 * Hidden danger-mode input. No {@code KeyBinding} is registered on purpose so
 * nothing appears in the Controls settings - the keys are polled directly:
 *
 * <ul>
 *   <li>Right Control (rising edge, no GUI open): toggles danger mode.</li>
 *   <li>Numpad 0 (rising edge, no GUI open, danger ON): opens the embedded
 *       Arsenic ClickGUI. RSHIFT always stays on the legit PecuniaMC GUI.</li>
 * </ul>
 *
 * Also runs the periodic OFF-state enforcer and the deferred GUI openers
 * (confirm boxes, credits popup).
 */
public class DangerKeybind {

    private int tickCounter;
    private boolean prevDangerKey;
    private boolean prevClickGuiKey;

    public static void register() {
        // Intentionally no ClientRegistry registration: the toggle must stay
        // invisible in Controls. Polled directly in onClientTick.
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        try {
            if (event == null || event.phase != TickEvent.Phase.END) {
                return;
            }
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null) {
                return;
            }

            boolean noScreen = mc.currentScreen == null;
            boolean dangerDown = noScreen && Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
            if (dangerDown && !prevDangerKey) {
                PecuniaCore.setDangerModeEnabled(!PecuniaCore.isDangerModeEnabled());
            }
            prevDangerKey = dangerDown;

            boolean clickGuiDown = noScreen && PecuniaCore.isDangerModeEnabled()
                    && Keyboard.isKeyDown(Keyboard.KEY_NUMPAD0);
            if (clickGuiDown && !prevClickGuiKey) {
                openArsenicClickGui();
            }
            prevClickGuiKey = clickGuiDown;

            if (GuiConfirmDanger.pollOpenRequest()) {
                try {
                    mc.displayGuiScreen(new GuiConfirmDanger(null));
                } catch (Throwable ignored) {
                }
            }
            if (GuiConfirmContent.pollOpenRequest()) {
                try {
                    mc.displayGuiScreen(new GuiConfirmContent(null));
                } catch (Throwable ignored) {
                }
            }
            if (GuiAuthorCredits.pollOpenRequest()) {
                try {
                    mc.displayGuiScreen(new GuiAuthorCredits());
                } catch (Throwable ignored) {
                }
            }
            if (++tickCounter >= 40) {
                tickCounter = 0;
                if (DangerModeManager.needsEnforce()) {
                    DangerModeManager.applyDangerMode(false);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private void openArsenicClickGui() {
        try {
            arsenic.main.Arsenic instance = arsenic.main.Arsenic.getArsenic();
            if (instance == null || instance.getClickGuiScreen() == null) {
                return;
            }
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.currentScreen != null) {
                return;
            }
            DangerGuiGate.allowClickGuiOnce();
            mc.displayGuiScreen(instance.getClickGuiScreen());
        } catch (Throwable ignored) {
        }
    }
}
