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

import arsenic.main.Arsenic;
import arsenic.module.Module;
import arsenic.module.ModuleCategory;
import dev.frostclient.Frost;
import dev.frostclient.gui.modmenu.impl.Panel;
import dev.frostclient.gui.modmenu.impl.sidebar.TextBox;
import dev.frostclient.helpers.MathHelper;
import dev.frostclient.helpers.hud.ScrollHelper;
import dev.frostclient.helpers.render.GLHelper;
import dev.frostclient.helpers.render.Helper2D;
import dev.pecuniamc.PecuniaCore;

import java.util.ArrayList;
import java.util.Collection;

/**
 * "Danger" section of the PecuniaMC ModMenu (Frost {@link Panel}).
 *
 * Visible only while danger mode is on. Shows the embedded Arsenic modules in
 * a separate tab with per-category pills, search, a toggle grid and an
 * expandable overlay per module to adjust every parameter
 * (booleans, enums, sliders, ranges, colors, text, keybinds).
 */
public class ArsenicSection {

    private final Panel panel;
    private final ArrayList<ArsenicModuleButton> buttonList = new ArrayList<ArsenicModuleButton>();
    private final ScrollHelper scrollHelper = new ScrollHelper(0, 270, 35, 300);
    private final TextBox searchBox = new TextBox("Search", 0, 0, 150, 20);

    private String selectedCategory = "All";
    private boolean dirty = true;
    private boolean lastDanger;

    public ArsenicSection(Panel panel) {
        this.panel = panel;
        boolean danger = false;
        try {
            danger = PecuniaCore.isDangerModeEnabled();
        } catch (Throwable ignored) {
        }
        this.lastDanger = danger;
    }

    private static boolean roundedCorners() {
        try {
            dev.frostclient.feature.option.Option option =
                    Frost.INSTANCE.optionManager.getOptionByName("Rounded Corners");
            return option != null && option.isCheckToggled();
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static ArrayList<String> getCategories() {
        ArrayList<String> categories = new ArrayList<String>();
        categories.add("All");
        try {
            for (ModuleCategory category : ModuleCategory.values()) {
                if (category == ModuleCategory.SEARCH) {
                    continue;
                }
                String name = category.getName();
                if (name != null && !categories.contains(name)) {
                    categories.add(name);
                }
            }
        } catch (Throwable ignored) {
        }
        return categories;
    }

    public Panel getPanel() {
        return panel;
    }

    public void markDirty() {
        dirty = true;
    }

    public void refresh() {
        buttonList.clear();
        scrollHelper.setScrollStep(0);
        if (!PecuniaCore.isDangerModeEnabled()) {
            dirty = false;
            return;
        }
        try {
            Arsenic arsenic = Arsenic.getArsenic();
            if (arsenic == null || arsenic.getModuleManager() == null) {
                dirty = false;
                return;
            }
            Collection<Module> modules = arsenic.getModuleManager().getModules();
            if (modules == null) {
                dirty = false;
                return;
            }
            String query = "";
            try {
                query = searchBox.getText().toLowerCase();
            } catch (Throwable ignored) {
            }
            int addButtonX = 0;
            int addButtonY = 0;
            int buttonCounter = 0;
            for (Module module : modules) {
                if (module == null) {
                    continue;
                }
                try {
                    if (module.isHidden()) {
                        continue;
                    }
                    String categoryName = module.getCategory() == null ? "" : module.getCategory().getName();
                    if (!selectedCategory.equals("All") && !selectedCategory.equals(categoryName)) {
                        continue;
                    }
                    String name = module.getName() == null ? "" : module.getName().toLowerCase();
                    if (!query.isEmpty() && !name.contains(query)) {
                        continue;
                    }
                    ArsenicModuleButton button = new ArsenicModuleButton(module, this, addButtonX, addButtonY);
                    buttonList.add(button);
                    buttonCounter++;
                    addButtonX += button.getW() + 3;
                    if (buttonCounter % 4 == 0) {
                        addButtonX = 0;
                        addButtonY += button.getH() + 3;
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
        dirty = false;
    }

    public boolean isAnyButtonOpen() {
        for (ArsenicModuleButton button : buttonList) {
            if (button.isOpen()) {
                return true;
            }
        }
        return false;
    }

    /** Closes every overlay and clears transient input state (tab switches). */
    public void closeOverlays() {
        for (ArsenicModuleButton button : buttonList) {
            try {
                button.closeOverlay();
            } catch (Throwable ignored) {
            }
        }
    }

    public void renderSection(int mouseX, int mouseY) {
        boolean danger = PecuniaCore.isDangerModeEnabled();
        if (danger != lastDanger) {
            lastDanger = danger;
            dirty = true;
        }
        if (dirty) {
            refresh();
        }
        if (!danger) {
            return;
        }
        int x = panel.getX();
        int y = panel.getY();
        int w = panel.getW();
        int h = panel.getH();

        scrollHelper.update();

        int offset = 0;
        for (String category : getCategories()) {
            int length = Frost.INSTANCE.fontHelper.size20.getStringWidth(category);
            boolean selected = selectedCategory.equals(category);
            Helper2D.drawRoundedRectangle(
                    x + offset + 5,
                    y + h + 5,
                    length + 25,
                    20, 2,
                    dev.frostclient.gui.Style.getColor(selected ? 120 : 50).getRGB(),
                    roundedCorners() ? 0 : -1
            );
            Frost.INSTANCE.fontHelper.size20.drawString(category, x + offset + 26, y + h + 11, -1);
            offset += length + 30;
        }

        searchBox.renderTextBox(x + w - searchBox.getW() - 5, y + h + 5, mouseX, mouseY);

        GLHelper.startScissor(x, y + 60, w, h + 240);
        for (ArsenicModuleButton button : buttonList) {
            try {
                button.renderButton(mouseX, mouseY);
            } catch (Throwable ignored) {
            }
        }
        GLHelper.endScissor();

        // Open overlays render above the grid scissor so header and close
        // button are never clipped.
        for (ArsenicModuleButton button : buttonList) {
            try {
                button.renderOverlayIfOpen(mouseX, mouseY);
            } catch (Throwable ignored) {
            }
        }

        if (MathHelper.withinBox(x, y + 30, w, h + 270, mouseX, mouseY)) {
            int height = 0;
            int index = 0;
            for (ArsenicModuleButton button : buttonList) {
                if (index % 4 == 0) {
                    height += button.getH() + 3;
                }
                index++;
            }
            scrollHelper.updateScroll();
            scrollHelper.setHeight(height);

            index = 0;
            int count = 0;
            for (ArsenicModuleButton button : buttonList) {
                float position = scrollHelper.getCalculatedScroll();
                position += count * (button.getH() + 3);
                button.setY((int) position);
                index++;
                if (index % 4 == 0) {
                    count++;
                }
            }
        }
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (!PecuniaCore.isDangerModeEnabled()) {
            return;
        }
        try {
            searchBox.setFocused(searchBox.isHovered(mouseX, mouseY));
        } catch (Throwable ignored) {
        }
        int x = panel.getX();
        int y = panel.getY();
        int h = panel.getH();
        if (mouseButton == 0) {
            int offset = 0;
            for (String category : getCategories()) {
                int length = Frost.INSTANCE.fontHelper.size20.getStringWidth(category);
                if (MathHelper.withinBox(x + offset + 5, y + h + 5, length + 25, 20, mouseX, mouseY)) {
                    selectedCategory = category;
                    scrollHelper.setScrollStep(0);
                    refresh();
                }
                offset += length + 30;
            }
        }
        for (ArsenicModuleButton button : buttonList) {
            try {
                button.mouseClicked(mouseX, mouseY, mouseButton);
            } catch (Throwable ignored) {
            }
        }
    }

    public void mouseReleased(int mouseX, int mouseY, int state) {
        for (ArsenicModuleButton button : buttonList) {
            try {
                button.mouseReleased(mouseX, mouseY, state);
            } catch (Throwable ignored) {
            }
        }
    }

    public void keyTyped(char typedChar, int keyCode) {
        if (isAnyButtonOpen()) {
            for (ArsenicModuleButton button : buttonList) {
                try {
                    button.keyTyped(typedChar, keyCode);
                } catch (Throwable ignored) {
                }
            }
        } else {
            try {
                searchBox.keyTyped(typedChar, keyCode);
            } catch (Throwable ignored) {
            }
            refresh();
        }
    }
}
