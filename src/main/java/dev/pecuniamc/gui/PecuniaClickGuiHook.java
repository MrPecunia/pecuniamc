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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Injects Arsenic's categories seamlessly into the ClickGUI when danger mode
 * is ON, and hides them completely when OFF.
 *
 * Frost's ClickGUI is {@code dev.frostclient.gui.modmenu.ModMenu} with a
 * category panel. Since Arsenic's ClickGUI model is only available
 * reflectively, this hook exposes category names + module counts so the panel
 * can render an "Arsenic" section without a hard dependency.
 */
public final class PecuniaClickGuiHook {

    private PecuniaClickGuiHook() {
    }

    /** Returns Arsenic category display names, or empty when danger OFF/absent. */
    public static List<String> getInjectedCategories() {
        List<String> out = new ArrayList<String>();
        if (!PecuniaCore.isDangerModeEnabled()) {
            return out;
        }
        try {
            Class<?> catClass = Class.forName("arsenic.module.ModuleCategory");
            Object[] constants = catClass.getEnumConstants();
            if (constants == null) {
                return out;
            }
            for (Object c : constants) {
                if (c == null) {
                    continue;
                }
                try {
                    // SEARCH is a pseudo-category (all modules), not a tab.
                    String constName;
                    try {
                        constName = ((Enum<?>) c).name();
                    } catch (Throwable ignored) {
                        constName = "";
                    }
                    if ("SEARCH".equals(constName)) {
                        continue;
                    }
                    Method getName = c.getClass().getMethod("getName");
                    Object name = getName.invoke(c);
                    if (name != null) {
                        out.add(String.valueOf(name));
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    /** Returns module count per category for panel rendering. */
    @SuppressWarnings("unchecked")
    public static int getModuleCount(String categoryName) {
        if (!PecuniaCore.isDangerModeEnabled() || categoryName == null) {
            return 0;
        }
        try {
            Class<?> main = Class.forName("arsenic.main.Arsenic");
            Object instance = main.getMethod("getArsenic").invoke(null);
            Object moduleManager = instance.getClass().getMethod("getModuleManager").invoke(instance);
            Collection<?> modules = (Collection<?>)
                    moduleManager.getClass().getMethod("getModules").invoke(moduleManager);
            int count = 0;
            for (Object module : modules) {
                try {
                    Object info = module.getClass().getMethod("getCategory").invoke(module);
                    Method getName = info.getClass().getMethod("getName");
                    if (categoryName.equals(String.valueOf(getName.invoke(info)))) {
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
}
