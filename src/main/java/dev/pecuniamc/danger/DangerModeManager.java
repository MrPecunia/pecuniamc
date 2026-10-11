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

import arsenic.main.Arsenic;
import arsenic.module.Module;
import arsenic.module.ModuleManager;
import dev.pecuniamc.PecuniaCore;

import java.util.Collection;
import java.lang.reflect.Field;
/**
 * Gates the embedded Arsenic cheat modules behind the hidden danger-mode toggle.
 *
 * Modules always keep working (their keybinds stay live, event subscriptions
 * untouched) - danger mode only controls their <i>visibility</i>:
 *
 * <ul>
 *   <li>OFF: every module is hidden. Hidden modules show no activation /
 *       deactivation notifications and stay out of the ClickGUI lists, so the
 *       client runs silently with zero Arsenic surface.</li>
 *   <li>ON: modules are unhidden (ClickGUI lists, toggle notifications) and
 *       the ClickGUI itself may be opened via Numpad 0. Nothing is
 *       force-enabled; the user enables what they want.</li>
 * </ul>
 *
 * All calls are exception-guarded: PecuniaMC must boot and run identically
 * with or without the embedded Arsenic classes.
 */
public final class DangerModeManager {

    private DangerModeManager() {
    }

    public static boolean isDangerModeEnabled() {
        return PecuniaCore.isDangerModeEnabled();
    }

    public static void applyDangerMode(boolean enabled) {
        try {
            Arsenic arsenic;
            try {
                arsenic = Arsenic.getArsenic();
            } catch (Throwable ignored) {
                return;
            }
            if (arsenic == null) {
                return;
            }
            ModuleManager manager;
            try {
                manager = arsenic.getModuleManager();
            } catch (Throwable ignored) {
                return;
            }
            if (manager == null) {
                return;
            }
            Collection<Module> modules;
            try {
                modules = manager.getModules();
            } catch (Throwable ignored) {
                return;
            }
            if (modules == null) {
                return;
            }
            for (Module module : modules) {
                if (module == null) {
                    continue;
                }
                try {
                    module.setHidden(!enabled);
                } catch (Throwable ignored) {
                }
            }
            silenceNotifications(arsenic);
        } catch (Throwable t) {
            System.out.println("[PecuniaMC] DangerMode apply failed: " + t.getMessage());
        }
    }

    /**
     * Unsubscribes Arsenic's notification renderer permanently (danger on or
     * off): module toggle toasts must never appear. Queueing is harmless -
     * without the render listener nothing is ever drawn.
     */
    private static void silenceNotifications(Arsenic arsenic) {
        try {
            if (arsenic == null || arsenic.getEventManager() == null) {
                return;
            }
            Field field = Arsenic.class.getDeclaredField("notificationManager");
            field.setAccessible(true);
            Object manager = field.get(arsenic);
            if (manager != null) {
                arsenic.getEventManager().unsubscribe(manager);
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * Returns true when danger mode is OFF but an Arsenic module is currently
     * visible (startup race or runtime drift). Used by the periodic enforcer
     * to re-hide everything.
     */
    public static boolean needsEnforce() {
        try {
            if (PecuniaCore.isDangerModeEnabled()) {
                return false;
            }
            Arsenic arsenic = Arsenic.getArsenic();
            if (arsenic == null || arsenic.getModuleManager() == null
                    || arsenic.getModuleManager().getModules() == null) {
                return false;
            }
            for (Module module : arsenic.getModuleManager().getModules()) {
                if (module == null) {
                    continue;
                }
                try {
                    if (!module.isHidden()) {
                        return true;
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }
}
