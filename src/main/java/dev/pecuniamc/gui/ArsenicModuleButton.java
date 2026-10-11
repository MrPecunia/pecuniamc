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
import arsenic.module.property.Property;
import arsenic.module.property.impl.BooleanProperty;
import arsenic.module.property.impl.ButtonProperty;
import arsenic.module.property.impl.ColourProperty;
import arsenic.module.property.impl.EnumProperty;
import arsenic.module.property.impl.FolderProperty;
import arsenic.module.property.impl.TextProperty;
import arsenic.module.property.impl.doubleproperty.DoubleProperty;
import arsenic.module.property.impl.doubleproperty.DoubleValue;
import arsenic.module.property.impl.rangeproperty.RangeProperty;
import arsenic.module.property.impl.rangeproperty.RangeValue;
import dev.frostclient.Frost;
import dev.frostclient.gui.Style;
import dev.frostclient.helpers.MathHelper;
import dev.frostclient.helpers.hud.ScrollHelper;
import dev.frostclient.helpers.render.GLHelper;
import dev.frostclient.helpers.render.Helper2D;
import net.minecraft.util.ChatAllowedCharacters;
import org.lwjgl.input.Keyboard;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Grid cell + expandable settings overlay for one embedded Arsenic module,
 * rendered inside the PecuniaMC ModMenu "Danger" section.
 *
 * Left-click toggles the module, right-click opens the parameter overlay:
 * keybind, booleans, enums, number/range sliders, RGBA color channels, text
 * fields, action buttons and collapsible folders. Every change is persisted
 * through Arsenic's own config manager.
 */
public class ArsenicModuleButton {

    private final Module module;
    private final ArsenicSection section;
    private final ScrollHelper scrollHelper = new ScrollHelper(0, 210, 35, 300);
    private final ArrayList<Row> rows = new ArrayList<Row>();
    private final Set<String> expandedFolders = new HashSet<String>();

    private int x;
    private int y;
    private final int w = 120;
    private final int h = 80;
    private boolean open;

    private boolean keyListening;
    private boolean dragging;
    private Row dragRow;
    private int dragChannel;
    private boolean textFocused;
    private Row textRow;
    private String textBuffer = "";

    public ArsenicModuleButton(Module module, ArsenicSection section, int x, int y) {
        this.module = module;
        this.section = section;
        this.x = x;
        this.y = y;
    }

    public boolean isOpen() {
        return open;
    }

    public int getW() {
        return w;
    }

    public int getH() {
        return h;
    }

    public void setY(int y) {
        this.y = y;
    }

    private static void saveConfig() {
        try {
            Arsenic arsenic = Arsenic.getArsenic();
            if (arsenic != null && arsenic.getConfigManager() != null) {
                arsenic.getConfigManager().saveConfig();
            }
        } catch (Throwable ignored) {
        }
    }

    private static String trimTo(String text, int maxWidth) {
        try {
            if (Frost.INSTANCE.fontHelper.size20.getStringWidth(text) <= maxWidth) {
                return text;
            }
            while (text.length() > 1
                    && Frost.INSTANCE.fontHelper.size20.getStringWidth(text + "..") > maxWidth) {
                text = text.substring(0, text.length() - 1);
            }
            return text + "..";
        } catch (Throwable ignored) {
            return text;
        }
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

    private static int accentColor() {
        try {
            dev.frostclient.feature.option.Option option =
                    Frost.INSTANCE.optionManager.getOptionByName("Color");
            if (option != null && option.getColor() != null) {
                return option.getColor().getRGB();
            }
        } catch (Throwable ignored) {
        }
        return 0xFF5EB1FF;
    }

    // ---------- grid cell ----------

    public void renderButton(int mouseX, int mouseY) {
        boolean rounded = roundedCorners();
        int accent = accentColor();
        int px = section.getPanel().getX();
        int py = section.getPanel().getY();
        int ph = section.getPanel().getH();

        if (open) {
            return;
        }
        {
            boolean enabled;
            try {
                enabled = module.isEnabled();
            } catch (Throwable ignored) {
                enabled = false;
            }
            int cx = px + 5 + x;
            int cy = py + ph + 35 + y;
            Helper2D.drawRoundedRectangle(cx, cy, w, h, 2, Style.getColor(40).getRGB(), rounded ? 0 : -1);
            Helper2D.drawRoundedRectangle(cx, cy + h - 25, w, 25, 2, Style.getColor(50).getRGB(), rounded ? 2 : -1);
            String name;
            try {
                name = module.getName();
            } catch (Throwable ignored) {
                name = "?";
            }
            if (name == null) {
                name = "?";
            }
            Frost.INSTANCE.fontHelper.size20.drawString(trimTo(name, w - 12), cx + 6, cy + h - 18, accent);
            int pillColor = enabled ? 0xFF3FA34D : Style.getColor(50).getRGB();
            Helper2D.drawRoundedRectangle(cx + w - 44, cy + h - 21, 38, 16, 2, pillColor, rounded ? 0 : -1);
            String state = enabled ? "ON" : "OFF";
            Frost.INSTANCE.fontHelper.size20.drawString(state,
                    cx + w - 44 + 19 - Frost.INSTANCE.fontHelper.size20.getStringWidth(state) / 2,
                    cy + h - 17, -1);
        }
    }

    /** Overlay frame; called after the grid scissor so header/X stay visible. */
    public void renderOverlayIfOpen(int mouseX, int mouseY) {
        if (!open) {
            return;
        }
        boolean rounded = roundedCorners();
        int accent = accentColor();
        int px = section.getPanel().getX();
        int py = section.getPanel().getY();
        int ph = section.getPanel().getH();
        renderOverlay(mouseX, mouseY, rounded, accent, px, py, ph);
    }

    /** Closes the overlay and clears transient input state. */
    public void closeOverlay() {
        open = false;
        dragging = false;
        dragRow = null;
        keyListening = false;
        textFocused = false;
        textRow = null;
    }

    // ---------- overlay ----------

    private void renderOverlay(int mouseX, int mouseY, boolean rounded, int accent, int px, int py, int ph) {
        int pw = section.getPanel().getW();
        int oy = py + ph + 5;
        scrollHelper.update();

        Helper2D.drawRoundedRectangle(px + 5, oy, pw - 10, 300, 2, Style.getColor(80).getRGB(), rounded ? 1 : -1);
        Helper2D.drawRectangle(px + 5, oy + 30, pw - 10, 270, Style.getColor(40).getRGB());

        boolean hoveredX = MathHelper.withinBox(px + pw - 30, oy + 5, 20, 20, mouseX, mouseY);
        Helper2D.drawRoundedRectangle(px + pw - 30, oy + 5, 20, 20, 2,
                Style.getColor(hoveredX ? 70 : 50).getRGB(), rounded ? 0 : -1);
        Helper2D.drawPicture(px + pw - 30, oy + 5, 20, 20, accent, "icon/cross.png");

        String name;
        String desc;
        try {
            name = module.getName();
        } catch (Throwable ignored) {
            name = "?";
        }
        if (name == null) {
            name = "?";
        }
        try {
            desc = module.getDescription();
        } catch (Throwable ignored) {
            desc = "";
        }
        if (desc == null) {
            desc = "";
        }
        Frost.INSTANCE.fontHelper.size30.drawString(name, px + 12, oy + 8, accent);
        if (desc != null && !desc.isEmpty()) {
            Frost.INSTANCE.fontHelper.size20.drawString(trimTo(desc, pw - 60),
                    px + 18 + Frost.INSTANCE.fontHelper.size30.getStringWidth(name), oy + 14, -1);
        }

        GLHelper.startScissor(px + 5, py + ph + 65, pw - 10, 235);
        for (Row row : rows) {
            try {
                row.render(mouseX, mouseY);
            } catch (Throwable ignored) {
            }
        }
        GLHelper.endScissor();

        if (rows.size() != 0 && MathHelper.withinBox(px + 5, py + ph + 35, pw - 10, 260, mouseX, mouseY)) {
            int height = 0;
            for (Row row : rows) {
                height += row.h;
            }
            scrollHelper.setHeight(height);
            scrollHelper.updateScroll();

            height = 0;
            for (Row row : rows) {
                float position = height;
                position += scrollHelper.getCalculatedScroll() + rowsTopOffset();
                row.y = (int) position;
                height += row.h;
            }
        }
    }

    private int rowsTopOffset() {
        int py = section.getPanel().getY();
        int ph = section.getPanel().getH();
        return py + ph + 70;
    }

    private void buildRows() {
        rows.clear();
        dragging = false;
        dragRow = null;
        keyListening = false;
        textFocused = false;
        textRow = null;
        rows.add(new KeybindRow());
        try {
            for (Property<?> property : module.getContents()) {
                if (property == null) {
                    continue;
                }
                try {
                    if (!property.isVisible()) {
                        continue;
                    }
                } catch (Throwable ignored) {
                }
                addPropertyRows(property, 0);
            }
        } catch (Throwable ignored) {
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void addPropertyRows(Property<?> property, int indent) {
        if (property instanceof FolderProperty) {
            List<Property<?>> children = null;
            try {
                children = ((FolderProperty) property).getValue();
            } catch (Throwable ignored) {
            }
            rows.add(new FolderRow((FolderProperty) property, indent));
            if (children != null && expandedFolders.contains(folderKey((FolderProperty) property))) {
                for (Property<?> child : children) {
                    if (child == null) {
                        continue;
                    }
                    try {
                        if (!child.isVisible()) {
                            continue;
                        }
                    } catch (Throwable ignored) {
                    }
                    addPropertyRows(child, indent + 1);
                }
            }
            return;
        }
        if (property instanceof BooleanProperty) {
            rows.add(new BooleanRow((BooleanProperty) property, indent));
        } else if (property instanceof EnumProperty) {
            rows.add(new EnumRow((EnumProperty<?>) property, indent));
        } else if (property instanceof DoubleProperty) {
            rows.add(new DoubleRow((DoubleProperty) property, indent));
        } else if (property instanceof RangeProperty) {
            rows.add(new RangeRow((RangeProperty) property, indent));
        } else if (property instanceof ColourProperty) {
            rows.add(new ColourRow((ColourProperty) property, indent));
        } else if (property instanceof TextProperty) {
            rows.add(new TextRow((TextProperty) property, indent));
        } else if (property instanceof ButtonProperty) {
            rows.add(new ButtonRow((ButtonProperty) property, indent));
        } else {
            try {
                Class<?> stringClass = Class.forName("arsenic.module.property.impl.StringProperty");
                if (stringClass.isInstance(property)) {
                    rows.add(new StringRow(property, indent));
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private static String folderKey(FolderProperty folder) {
        try {
            return folder.getName();
        } catch (Throwable ignored) {
            return String.valueOf(System.identityHashCode(folder));
        }
    }

    // ---------- input ----------

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        int px = section.getPanel().getX();
        int py = section.getPanel().getY();
        int ph = section.getPanel().getH();
        if (!open) {
            if (MathHelper.withinBox(px + 5 + x, py + ph + 35 + y, w, h, mouseX, mouseY)) {
                if (!section.isAnyButtonOpen()) {
                    if (mouseButton == 0) {
                        try {
                            module.toggle();
                        } catch (Throwable ignored) {
                        }
                        saveConfig();
                    } else if (mouseButton == 1) {
                        buildRows();
                        open = true;
                    }
                }
            }
            return;
        }
        int pw = section.getPanel().getW();
        int oy = py + ph + 5;
        if (mouseButton == 0 && MathHelper.withinBox(px + pw - 30, oy + 5, 20, 20, mouseX, mouseY)) {
            closeOverlay();
            return;
        }
        if (open && mouseButton == 0
                && MathHelper.withinBox(px + 5, py + ph + 35, pw - 10, 260, mouseX, mouseY)) {
            // Copy: row clicks may rebuild the row list (visibility refresh).
            for (Row row : new java.util.ArrayList<Row>(rows)) {
                try {
                    row.click(mouseX, mouseY);
                } catch (Throwable ignored) {
                }
            }
        }
    }

    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (dragging) {
            dragging = false;
            dragRow = null;
            saveConfig();
        }
    }

    public void keyTyped(char typedChar, int keyCode) {
        if (!open) {
            return;
        }
        if (keyListening) {
            try {
                if (keyCode == Keyboard.KEY_ESCAPE) {
                    module.setKeybind(0);
                } else {
                    module.setKeybind(keyCode);
                }
            } catch (Throwable ignored) {
            }
            keyListening = false;
            saveConfig();
            return;
        }
        if (textFocused && textRow instanceof TextRow) {
            ((TextRow) textRow).type(typedChar, keyCode);
        }
    }

    // ---------- rows ----------

    private abstract class Row {
        int y;
        int h = 24;
        final int indent;

        Row(int indent) {
            this.indent = indent;
        }

        Row(int indent, int h) {
            this.indent = indent;
            this.h = h;
        }

        int rowX() {
            return section.getPanel().getX() + 12 + indent * 12;
        }

        int rowW() {
            return section.getPanel().getW() - 24 - indent * 12;
        }

        boolean visible() {
            return open;
        }

        abstract void render(int mouseX, int mouseY);

        void click(int mouseX, int mouseY) {
        }
    }

    private String propName(Property<?> property) {
        try {
            String name = property.getName();
            return name == null ? "?" : name;
        } catch (Throwable ignored) {
            return "?";
        }
    }

    private void drawLabel(String text, int x, int y, int color) {
        Frost.INSTANCE.fontHelper.size20.drawString(trimTo(text, rowContentWidth()), x, y, color);
    }

    private int rowContentWidth() {
        return section.getPanel().getW() - 60;
    }

    private boolean rowHover(Row row, int mouseX, int mouseY) {
        return MathHelper.withinBox(row.rowX(), row.y, row.rowW(), row.h, mouseX, mouseY);
    }

    private class KeybindRow extends Row {
        KeybindRow() {
            super(0);
        }

        @Override
        void render(int mouseX, int mouseY) {
            String keyName;
            try {
                int key = module.getKeybind();
                keyName = key == 0 ? "NONE" : Keyboard.getKeyName(key);
            } catch (Throwable ignored) {
                keyName = "?";
            }
            if (keyListening) {
                keyName = "...";
            }
            boolean rounded = roundedCorners();
            drawLabel("Bind", rowX(), y + 8, -1);
            int bw = 90;
            int bx = rowX() + rowW() - bw;
            boolean hovered = MathHelper.withinBox(bx, y + 3, bw, 18, mouseX, mouseY);
            Helper2D.drawRoundedRectangle(bx, y + 3, bw, 18, 2,
                    Style.getColor(keyListening ? 120 : (hovered ? 70 : 50)).getRGB(), rounded ? 0 : -1);
            Frost.INSTANCE.fontHelper.size20.drawString(keyName,
                    bx + bw / 2 - Frost.INSTANCE.fontHelper.size20.getStringWidth(keyName) / 2, y + 8, -1);
        }

        @Override
        void click(int mouseX, int mouseY) {
            int bw = 90;
            int bx = rowX() + rowW() - bw;
            if (MathHelper.withinBox(bx, y + 3, bw, 18, mouseX, mouseY)) {
                keyListening = true;
            }
        }
    }

    private class BooleanRow extends Row {
        private final BooleanProperty property;

        BooleanRow(BooleanProperty property, int indent) {
            super(indent);
            this.property = property;
        }

        @Override
        void render(int mouseX, int mouseY) {
            boolean rounded = roundedCorners();
            boolean value = false;
            try {
                value = property.getValue();
            } catch (Throwable ignored) {
            }
            drawLabel(propName(property), rowX(), y + 8, -1);
            int bw = 60;
            int bx = rowX() + rowW() - bw;
            boolean hovered = MathHelper.withinBox(bx, y + 3, bw, 18, mouseX, mouseY);
            Helper2D.drawRoundedRectangle(bx, y + 3, bw, 18, 2,
                    (value ? 0xFF3FA34D : Style.getColor(hovered ? 70 : 50).getRGB()), rounded ? 0 : -1);
            String text = value ? "ON" : "OFF";
            Frost.INSTANCE.fontHelper.size20.drawString(text,
                    bx + bw / 2 - Frost.INSTANCE.fontHelper.size20.getStringWidth(text) / 2, y + 8, -1);
        }

        @Override
        void click(int mouseX, int mouseY) {
            int bw = 60;
            int bx = rowX() + rowW() - bw;
            if (MathHelper.withinBox(bx, y + 3, bw, 18, mouseX, mouseY)) {
                try {
                    property.setValue(!property.getValue());
                } catch (Throwable ignored) {
                }
                saveConfig();
                buildRows();
            }
        }
    }

    private class EnumRow extends Row {
        private final EnumProperty<?> property;

        EnumRow(EnumProperty<?> property, int indent) {
            super(indent);
            this.property = property;
        }

        @Override
        void render(int mouseX, int mouseY) {
            boolean rounded = roundedCorners();
            String value = "?";
            try {
                Object v = property.getValue();
                value = v == null ? "?" : v.toString();
            } catch (Throwable ignored) {
            }
            drawLabel(propName(property), rowX(), y + 8, -1);
            int bw = 110;
            int bx = rowX() + rowW() - bw;
            boolean hovered = MathHelper.withinBox(bx, y + 3, bw, 18, mouseX, mouseY);
            Helper2D.drawRoundedRectangle(bx, y + 3, bw, 18, 2,
                    Style.getColor(hovered ? 70 : 50).getRGB(), rounded ? 0 : -1);
            Frost.INSTANCE.fontHelper.size20.drawString(trimTo(value, bw - 8),
                    bx + bw / 2 - Frost.INSTANCE.fontHelper.size20.getStringWidth(trimTo(value, bw - 8)) / 2,
                    y + 8, -1);
        }

        @Override
        void click(int mouseX, int mouseY) {
            int bw = 110;
            int bx = rowX() + rowW() - bw;
            if (MathHelper.withinBox(bx, y + 3, bw, 18, mouseX, mouseY)) {
                try {
                    property.nextMode();
                } catch (Throwable ignored) {
                }
                saveConfig();
                buildRows();
            }
        }
    }

    private void drawSliderBar(int bx, int by, int bw, double min, double max, double value,
                               int mouseX, boolean rounded) {
        double t = max <= min ? 0.0D : (value - min) / (max - min);
        if (t < 0.0D) {
            t = 0.0D;
        }
        if (t > 1.0D) {
            t = 1.0D;
        }
        Helper2D.drawRoundedRectangle(bx, by, bw, 12, 2, Style.getColor(50).getRGB(), rounded ? 0 : -1);
        Helper2D.drawRoundedRectangle(bx, by, Math.max(4, (int) (bw * t)), 12, 2,
                accentColor(), rounded ? 0 : -1);
    }

    private double sliderValueFromMouse(int mouseX, int bx, int bw, double min, double max) {
        double t = (double) (mouseX - bx) / (double) Math.max(1, bw);
        if (t < 0.0D) {
            t = 0.0D;
        }
        if (t > 1.0D) {
            t = 1.0D;
        }
        return min + t * (max - min);
    }

    private String formatNumber(double value) {
        try {
            if (value == Math.rint(value)) {
                return String.valueOf((long) value);
            }
            String text = String.valueOf(Math.round(value * 100.0D) / 100.0D);
            return text;
        } catch (Throwable ignored) {
            return "?";
        }
    }

    private class DoubleRow extends Row {
        private final DoubleProperty property;

        DoubleRow(DoubleProperty property, int indent) {
            super(indent);
            this.property = property;
        }

        private DoubleValue current() {
            try {
                return property.getValue();
            } catch (Throwable ignored) {
                return null;
            }
        }

        @Override
        void render(int mouseX, int mouseY) {
            boolean rounded = roundedCorners();
            DoubleValue value = current();
            double min = 0.0D;
            double max = 1.0D;
            double current = 0.0D;
            try {
                if (value != null) {
                    min = value.getMinBound();
                    max = value.getMaxBound();
                    current = value.getInput();
                }
            } catch (Throwable ignored) {
            }
            if (dragging && dragRow == this && value != null) {
                try {
                    int bw = rowW() - 130;
                    int bx = rowX() + 125;
                    value.setInput(sliderValueFromMouse(mouseX, bx, bw, min, max));
                    current = value.getInput();
                } catch (Throwable ignored) {
                }
            }
            drawLabel(propName(property) + ": " + formatNumber(current), rowX(), y + 8, -1);
            int bw = rowW() - 130;
            int bx = rowX() + 125;
            drawSliderBar(bx, y + 6, bw, min, max, current, mouseX, rounded);
        }

        @Override
        void click(int mouseX, int mouseY) {
            DoubleValue value = current();
            if (value == null) {
                return;
            }
            int bw = rowW() - 130;
            int bx = rowX() + 125;
            if (MathHelper.withinBox(bx, y + 2, bw, 20, mouseX, mouseY)) {
                double min = 0.0D;
                double max = 1.0D;
                try {
                    min = value.getMinBound();
                    max = value.getMaxBound();
                    value.setInput(sliderValueFromMouse(mouseX, bx, bw, min, max));
                } catch (Throwable ignored) {
                }
                dragging = true;
                dragRow = this;
            }
        }
    }

    private class RangeRow extends Row {
        private final RangeProperty property;

        RangeRow(RangeProperty property, int indent) {
            super(indent, 56);
            this.property = property;
        }

        private RangeValue current() {
            try {
                return property.getValue();
            } catch (Throwable ignored) {
                return null;
            }
        }

        @Override
        void render(int mouseX, int mouseY) {
            boolean rounded = roundedCorners();
            RangeValue value = current();
            double minBound = 0.0D;
            double maxBound = 1.0D;
            double min = 0.0D;
            double max = 1.0D;
            try {
                if (value != null) {
                    minBound = value.getMinBound();
                    maxBound = value.getMaxBound();
                    min = value.getMin();
                    max = value.getMax();
                }
            } catch (Throwable ignored) {
            }
            if (dragging && dragRow == this && value != null) {
                try {
                    int bw = rowW() - 130;
                    int bx = rowX() + 125;
                    double v = sliderValueFromMouse(mouseX, bx, bw, minBound, maxBound);
                    if (dragChannel == 0) {
                        if (v > max) {
                            v = max;
                        }
                        value.setMin(v);
                        min = value.getMin();
                    } else {
                        if (v < min) {
                            v = min;
                        }
                        value.setMax(v);
                        max = value.getMax();
                    }
                } catch (Throwable ignored) {
                }
            }
            drawLabel(propName(property), rowX(), y + 4, -1);
            int bw = rowW() - 130;
            int bx = rowX() + 125;
            Frost.INSTANCE.fontHelper.size20.drawString("Min " + formatNumber(min), rowX(), y + 18, -1);
            drawSliderBar(bx, y + 16, bw, minBound, maxBound, min, mouseX, rounded);
            Frost.INSTANCE.fontHelper.size20.drawString("Max " + formatNumber(max), rowX(), y + 32, -1);
            drawSliderBar(bx, y + 30, bw, minBound, maxBound, max, mouseX, rounded);
        }

        @Override
        void click(int mouseX, int mouseY) {
            RangeValue value = current();
            if (value == null) {
                return;
            }
            int bw = rowW() - 130;
            int bx = rowX() + 125;
            double minBound = 0.0D;
            double maxBound = 1.0D;
            double min = 0.0D;
            double max = 1.0D;
            try {
                minBound = value.getMinBound();
                maxBound = value.getMaxBound();
                min = value.getMin();
                max = value.getMax();
            } catch (Throwable ignored) {
                return;
            }
            if (MathHelper.withinBox(bx, y + 10, bw, 18, mouseX, mouseY)) {
                double v = sliderValueFromMouse(mouseX, bx, bw, minBound, maxBound);
                if (v > max) {
                    v = max;
                }
                try {
                    value.setMin(v);
                } catch (Throwable ignored) {
                }
                dragging = true;
                dragRow = this;
                dragChannel = 0;
            } else if (MathHelper.withinBox(bx, y + 28, bw, 18, mouseX, mouseY)) {
                double v = sliderValueFromMouse(mouseX, bx, bw, minBound, maxBound);
                if (v < min) {
                    v = min;
                }
                try {
                    value.setMax(v);
                } catch (Throwable ignored) {
                }
                dragging = true;
                dragRow = this;
                dragChannel = 1;
            }
        }
    }

    @Override
    public String toString() {
        return "ArsenicModuleButton";
    }

    private class ColourRow extends Row {
        private final ColourProperty property;

        ColourRow(ColourProperty property, int indent) {
            super(indent, 88);
            this.property = property;
        }

        private int channel(int i) {
            try {
                return Math.max(0, Math.min(255, property.getColor(i)));
            } catch (Throwable ignored) {
                return 0;
            }
        }

        @Override
        void render(int mouseX, int mouseY) {
            boolean rounded = roundedCorners();
            if (dragging && dragRow == this) {
                try {
                    int bw = rowW() - 130;
                    int bx = rowX() + 125;
                    int v = (int) Math.round(sliderValueFromMouse(mouseX, bx, bw, 0.0D, 255.0D));
                    property.setColor(dragChannel, v);
                } catch (Throwable ignored) {
                }
            }
            int r = channel(1);
            int g = channel(2);
            int b = channel(3);
            drawLabel(propName(property), rowX(), y + 4, -1);
            Helper2D.drawRoundedRectangle(rowX() + rowW() - 46, y + 2, 40, 16, 2,
                    new Color(r, g, b).getRGB(), rounded ? 0 : -1);
            String[] names = {"R", "G", "B"};
            int[] values = {r, g, b};
            for (int i = 0; i < 3; i++) {
                int ry = y + 22 + i * 22;
                Frost.INSTANCE.fontHelper.size20.drawString(names[i] + " " + values[i], rowX(), ry + 4, -1);
                int bw = rowW() - 130;
                int bx = rowX() + 125;
                drawSliderBar(bx, ry + 2, bw, 0.0D, 255.0D, values[i], mouseX, rounded);
            }
        }

        @Override
        void click(int mouseX, int mouseY) {
            for (int i = 0; i < 3; i++) {
                int ry = y + 22 + i * 22;
                int bw = rowW() - 130;
                int bx = rowX() + 125;
                if (MathHelper.withinBox(bx, ry - 2, bw, 20, mouseX, mouseY)) {
                    int v = (int) Math.round(sliderValueFromMouse(mouseX, bx, bw, 0.0D, 255.0D));
                    try {
                        property.setColor(i + 1, v);
                    } catch (Throwable ignored) {
                    }
                    dragging = true;
                    dragRow = this;
                    dragChannel = i + 1;
                    return;
                }
            }
        }
    }

    private class TextRow extends Row {
        private final TextProperty property;
        private boolean focused;

        TextRow(TextProperty property, int indent) {
            super(indent);
            this.property = property;
        }

        @Override
        void render(int mouseX, int mouseY) {
            boolean rounded = roundedCorners();
            String shown;
            try {
                Object current = property.getValue();
                shown = current == null ? "" : String.valueOf(current);
            } catch (Throwable ignored) {
                shown = "";
            }
            if (textFocused) {
                shown = textBuffer;
            }
            if (shown == null) {
                shown = "";
            }
            drawLabel(propName(property), rowX(), y + 8, -1);
            int bw = rowW() - 130;
            int bx = rowX() + 125;
            boolean hovered = MathHelper.withinBox(bx, y + 3, bw, 18, mouseX, mouseY);
            Helper2D.drawRoundedRectangle(bx, y + 3, bw, 18, 2,
                    Style.getColor(textFocused ? 120 : (hovered ? 70 : 50)).getRGB(), rounded ? 0 : -1);
            String text = trimTo(shown + (textFocused ? "_" : ""), bw - 8);
            Frost.INSTANCE.fontHelper.size20.drawString(text, bx + 4, y + 8, -1);
            focused = textFocused;
        }

        @Override
        void click(int mouseX, int mouseY) {
            int bw = rowW() - 130;
            int bx = rowX() + 125;
            if (MathHelper.withinBox(bx, y + 3, bw, 18, mouseX, mouseY)) {
                textFocused = true;
                textRow = this;
                try {
                    textBuffer = String.valueOf(property.getValue());
                } catch (Throwable ignored) {
                    textBuffer = "";
                }
                if (textBuffer == null) {
                    textBuffer = "";
                }
            } else if (textFocused) {
                textFocused = false;
                if (textRow == this) {
                    textRow = null;
                }
            }
        }

        void type(char typedChar, int keyCode) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                textFocused = false;
                textRow = null;
                return;
            }
            if (keyCode == Keyboard.KEY_RETURN) {
                try {
                    property.setValue(textBuffer);
                } catch (Throwable ignored) {
                }
                saveConfig();
                textFocused = false;
                textRow = null;
                return;
            }
            if (keyCode == Keyboard.KEY_BACK) {
                if (!textBuffer.isEmpty()) {
                    textBuffer = textBuffer.substring(0, textBuffer.length() - 1);
                }
                return;
            }
            try {
                if (textBuffer.length() < 48 && ChatAllowedCharacters.isAllowedCharacter(typedChar)) {
                    textBuffer += typedChar;
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private class StringRow extends Row {
        private final Property<?> property;

        StringRow(Property<?> property, int indent) {
            super(indent);
            this.property = property;
        }

        @Override
        void render(int mouseX, int mouseY) {
            String value = "?";
            try {
                Object v = property.getValue();
                value = v == null ? "" : v.toString();
            } catch (Throwable ignored) {
            }
            drawLabel(propName(property) + ": " + value, rowX(), y + 8, -1);
        }
    }

    private class ButtonRow extends Row {
        private final ButtonProperty property;

        ButtonRow(ButtonProperty property, int indent) {
            super(indent);
            this.property = property;
        }

        @Override
        void render(int mouseX, int mouseY) {
            boolean rounded = roundedCorners();
            drawLabel(propName(property), rowX(), y + 8, -1);
            int bw = 90;
            int bx = rowX() + rowW() - bw;
            boolean hovered = MathHelper.withinBox(bx, y + 3, bw, 18, mouseX, mouseY);
            Helper2D.drawRoundedRectangle(bx, y + 3, bw, 18, 2,
                    Style.getColor(hovered ? 70 : 50).getRGB(), rounded ? 0 : -1);
            Frost.INSTANCE.fontHelper.size20.drawString("Run",
                    bx + bw / 2 - Frost.INSTANCE.fontHelper.size20.getStringWidth("Run") / 2, y + 8, -1);
        }

        @Override
        void click(int mouseX, int mouseY) {
            int bw = 90;
            int bx = rowX() + rowW() - bw;
            if (MathHelper.withinBox(bx, y + 3, bw, 18, mouseX, mouseY)) {
                try {
                    property.fire();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private class FolderRow extends Row {
        private final FolderProperty folder;

        FolderRow(FolderProperty folder, int indent) {
            super(indent);
            this.folder = folder;
        }

        @Override
        void render(int mouseX, int mouseY) {
            boolean rounded = roundedCorners();
            boolean expanded = expandedFolders.contains(folderKey(folder));
            drawLabel((expanded ? "- " : "+ ") + propName(folder), rowX(), y + 8, -1);
            int bw = 60;
            int bx = rowX() + rowW() - bw;
            boolean hovered = MathHelper.withinBox(bx, y + 3, bw, 18, mouseX, mouseY);
            Helper2D.drawRoundedRectangle(bx, y + 3, bw, 18, 2,
                    Style.getColor(hovered ? 70 : 50).getRGB(), rounded ? 0 : -1);
            String text = expanded ? "Hide" : "Show";
            Frost.INSTANCE.fontHelper.size20.drawString(text,
                    bx + bw / 2 - Frost.INSTANCE.fontHelper.size20.getStringWidth(text) / 2, y + 8, -1);
        }

        @Override
        void click(int mouseX, int mouseY) {
            int bw = rowW();
            if (MathHelper.withinBox(rowX(), y, bw, h, mouseX, mouseY)) {
                String key = folderKey(folder);
                if (expandedFolders.contains(key)) {
                    expandedFolders.remove(key);
                } else {
                    expandedFolders.add(key);
                }
                buildRows();
            }
        }
    }
}
