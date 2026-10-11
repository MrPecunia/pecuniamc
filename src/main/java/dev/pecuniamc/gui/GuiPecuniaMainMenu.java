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
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Project;

/**
 * Lunar-Client style main menu replacement: modern, flat, no panorama noise.
 *
 * Flat dark background + subtle vignette, centered client title, wide flat
 * buttons (Singleplayer / Multiplayer / Settings) and a strictly innocuous
 * "Info" button showing basic client details. Never mentions cheat/danger mode.
 */
public class GuiPecuniaMainMenu extends GuiScreen {

    private static final int BG_TOP = 0xFF14171C;
    private static final int BG_BOTTOM = 0xFF1E232B;
    private static final int ACCENT = 0xFF5EB1FF;
    private static final int BTN_IDLE = 0xFF232A33;
    private static final int BTN_HOVER = 0xFF2D3540;
    private static final int BTN_TEXT = 0xFFE8ECF1;
    private static final int MUTED = 0xFF8A94A3;

    private static int panoramaTimer = 0;
    private final DynamicTexture viewportTexture = new DynamicTexture(256, 256);

    private static final ResourceLocation[] PANORAMA = new ResourceLocation[]{
            new ResourceLocation("pecuniamc", "panorama/panorama_0.png"),
            new ResourceLocation("pecuniamc", "panorama/panorama_1.png"),
            new ResourceLocation("pecuniamc", "panorama/panorama_2.png"),
            new ResourceLocation("pecuniamc", "panorama/panorama_3.png"),
            new ResourceLocation("pecuniamc", "panorama/panorama_4.png"),
            new ResourceLocation("pecuniamc", "panorama/panorama_5.png")
    };

    private FlatButton btnSingle;
    private FlatButton btnMulti;
    private FlatButton btnSettings;
    private FlatButton btnInfo;
    private FlatButton btnStatus;
    private FlatButton btnSync;

    private static final int CARD_W = 240;
    private static final int CARD_H = 228;
    private static final int BTN_W = 208;
    private static final int BTN_H = 22;
    private static final int BTN_PITCH = 26;

    private int cardX() {
        return width / 2 - CARD_W / 2;
    }

    private int cardY() {
        return height / 2 - CARD_H / 2 - 4;
    }

    private boolean isNetworkUp() {
        try {
            return PecuniaCore.getSocket() != null && PecuniaCore.getSocket().isOpen();
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public void initGui() {
        int cx = width / 2;
        int by = cardY() + 84;
        buttonList.clear();
        btnSingle = new FlatButton(1, cx - BTN_W / 2, by, BTN_W, BTN_H, "Singleplayer");
        btnMulti = new FlatButton(2, cx - BTN_W / 2, by + BTN_PITCH, BTN_W, BTN_H, "Multiplayer");
        btnSettings = new FlatButton(3, cx - BTN_W / 2, by + BTN_PITCH * 2, BTN_W, BTN_H, "Settings");
        btnInfo = new FlatButton(4, cx - BTN_W / 2, by + BTN_PITCH * 3, 102, BTN_H, "Info");
        btnStatus = new FlatButton(5, cx + 2, by + BTN_PITCH * 3, 102, BTN_H, "Status");
        btnSync = new FlatButton(6, cx - 60, by + BTN_PITCH * 4 + 12, 120, 20, "Sync");
        buttonList.add(btnSingle);
        buttonList.add(btnMulti);
        buttonList.add(btnSettings);
        buttonList.add(btnInfo);
        buttonList.add(btnStatus);
        buttonList.add(btnSync);
    }

    @Override
    public void updateScreen() {
        panoramaTimer++;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawFlatBackground();
        // Dim panorama underneath for depth, Lunar-style: mostly flat.
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 0.12F);
        try {
            renderSkybox(partialTicks);
        } catch (Throwable ignored) {
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
        drawFlatBackgroundOverlay();

        int cx = width / 2;
        int cardX = cardX();
        int cardY = cardY();
        boolean up = isNetworkUp();
        if (btnSync != null) {
            btnSync.visible = !up;
        }

        // Card, same language as the Info/Status screens.
        drawRect(cardX, cardY, cardX + CARD_W, cardY + CARD_H, 0xFF1E232B);
        drawRect(cardX, cardY, cardX + CARD_W, cardY + 2, ACCENT);

        // Title x2, accent bar, subtitle.
        GlStateManager.pushMatrix();
        GlStateManager.scale(2.0F, 2.0F, 2.0F);
        String title = PecuniaCore.CLIENT_NAME;
        fontRendererObj.drawString(title,
                cx / 2 - fontRendererObj.getStringWidth(title) / 2,
                (cardY + 14) / 2, 0xFFFFFFFF);
        GlStateManager.popMatrix();
        int titleW = fontRendererObj.getStringWidth(title) * 2;
        drawRect(cx - titleW / 2, cardY + 40, cx + titleW / 2, cardY + 42, ACCENT);
        String sub = "PvP Client  \u2022  " + PecuniaCore.MC_VERSION;
        fontRendererObj.drawString(sub, cx - fontRendererObj.getStringWidth(sub) / 2,
                cardY + 48, MUTED);

        super.drawScreen(mouseX, mouseY, partialTicks);

        // Network status row inside the card.
        int statusY = cardY() + 84 + BTN_PITCH * 4;
        String status = "Network: " + (up ? "\u00A7aOnline" : "\u00A7cOffline");
        fontRendererObj.drawString(status,
                cx - fontRendererObj.getStringWidth("Network: Offline") / 2, statusY, BTN_TEXT);

        String version = PecuniaCore.CLIENT_NAME + " " + PecuniaCore.CLIENT_VERSION;
        fontRendererObj.drawString(version, 6, height - 12, MUTED);
        String copy = "Copyright Mojang Studios. Do not distribute!";
        fontRendererObj.drawString(copy, width - fontRendererObj.getStringWidth(copy) - 6, height - 12, MUTED);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 1) {
            mc.displayGuiScreen(new GuiSelectWorld(this));
        } else if (button.id == 2) {
            mc.displayGuiScreen(new GuiMultiplayer(this));
        } else if (button.id == 3) {
            mc.displayGuiScreen(new GuiOptions(this, mc.gameSettings));
        } else if (button.id == 4) {
            mc.displayGuiScreen(new GuiPecuniaInfo(this));
        } else if (button.id == 5) {
            mc.displayGuiScreen(new GuiPecuniaStatus(this));
        } else if (button.id == 6) {
            PecuniaCore.reconnectNetwork();
        }
    }

    private void drawFlatBackground() {
        drawGradientRect(0, 0, width, height, BG_TOP, BG_BOTTOM);
    }

    private void drawFlatBackgroundOverlay() {
        // Flat dark veil so text stays readable over the dimmed panorama.
        drawRect(0, 0, width, height, 0xD214171C);
        // Thin top accent line.
        drawRect(0, 0, width, 2, ACCENT);
    }

    // ---- dimmed panorama (reused vanilla maths, low alpha) ----

    private void renderSkybox(float partialTicks) {
        mc.getFramebuffer().unbindFramebuffer();
        try {
            GlStateManager.viewport(0, 0, 256, 256);
            drawPanorama(partialTicks);
            rotateAndBlurSkybox();
            rotateAndBlurSkybox();
            rotateAndBlurSkybox();
            mc.getFramebuffer().bindFramebuffer(true);
            GlStateManager.viewport(0, 0, mc.displayWidth, mc.displayHeight);
            drawSkyboxQuad();
        } finally {
            try {
                mc.getFramebuffer().bindFramebuffer(true);
            } catch (Throwable ignored) {
            }
            try {
                GlStateManager.viewport(0, 0, mc.displayWidth, mc.displayHeight);
            } catch (Throwable ignored) {
            }
        }
    }

    private void drawSkyboxQuad() {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        float f = width > height ? 120.0F / width : 120.0F / height;
        float f1 = height * f / 256.0F;
        float f2 = width * f / 256.0F;
        wr.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        wr.pos(0.0, height, zLevel).tex(0.5F - f1, 0.5F + f2).color(1F, 1F, 1F, 1F).endVertex();
        wr.pos(width, height, zLevel).tex(0.5F - f1, 0.5F - f2).color(1F, 1F, 1F, 1F).endVertex();
        wr.pos(width, 0.0, zLevel).tex(0.5F + f1, 0.5F - f2).color(1F, 1F, 1F, 1F).endVertex();
        wr.pos(0.0, 0.0, zLevel).tex(0.5F + f1, 0.5F + f2).color(1F, 1F, 1F, 1F).endVertex();
        tessellator.draw();
    }

    private void drawPanorama(float partial) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        GlStateManager.matrixMode(5889);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        Project.gluPerspective(120.0F, 1.0F, 0.05F, 10.0F);
        GlStateManager.matrixMode(5888);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.rotate(180.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        for (int j = 0; j < 64; ++j) {
            GlStateManager.pushMatrix();
            float f = ((j % 8) / 8.0F - 0.5F) / 64.0F;
            float f1 = ((j / 8) / 8.0F - 0.5F) / 64.0F;
            GlStateManager.translate(f, f1, 0.0F);
            GlStateManager.rotate(MathHelper.sin((panoramaTimer + partial) / 400.0F) * 25.0F + 20.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(-(panoramaTimer + partial) * 0.1F, 0.0F, 1.0F, 0.0F);
            for (int k = 0; k < 6; ++k) {
                GlStateManager.pushMatrix();
                if (k == 1) GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
                if (k == 2) GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
                if (k == 3) GlStateManager.rotate(-90.0F, 0.0F, 1.0F, 0.0F);
                if (k == 4) GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
                if (k == 5) GlStateManager.rotate(-90.0F, 1.0F, 0.0F, 0.0F);
                try {
                    mc.getTextureManager().bindTexture(PANORAMA[k]);
                } catch (Throwable ignored) {
                    GlStateManager.popMatrix();
                    continue;
                }
                wr.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
                int l = 255 / (j + 1);
                wr.pos(-1.0, -1.0, 1.0).tex(0.0, 0.0).color(255, 255, 255, l).endVertex();
                wr.pos(1.0, -1.0, 1.0).tex(1.0, 0.0).color(255, 255, 255, l).endVertex();
                wr.pos(1.0, 1.0, 1.0).tex(1.0, 1.0).color(255, 255, 255, l).endVertex();
                wr.pos(-1.0, 1.0, 1.0).tex(0.0, 1.0).color(255, 255, 255, l).endVertex();
                tessellator.draw();
                GlStateManager.popMatrix();
            }
            GlStateManager.popMatrix();
            GlStateManager.colorMask(true, true, true, false);
        }
        wr.setTranslation(0.0, 0.0, 0.0);
        GlStateManager.colorMask(true, true, true, true);
        GlStateManager.matrixMode(5889);
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(5888);
        GlStateManager.popMatrix();
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.enableDepth();
    }

    private void rotateAndBlurSkybox() {
        ResourceLocation bg = mc.getTextureManager().getDynamicTextureLocation("background", viewportTexture);
        mc.getTextureManager().bindTexture(bg);
        GL11.glTexParameteri(3553, 10241, 9729);
        GL11.glTexParameteri(3553, 10240, 9729);
        GL11.glCopyTexSubImage2D(3553, 0, 0, 0, 0, 0, 256, 256);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.colorMask(true, true, true, false);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        GlStateManager.disableAlpha();
        for (int j = 0; j < 3; ++j) {
            float f = 1.0F / (j + 1);
            float f1 = (j - 1) / 256.0F;
            wr.pos(width, height, zLevel).tex(0.0F + f1, 1.0).color(1F, 1F, 1F, f).endVertex();
            wr.pos(width, 0.0, zLevel).tex(1.0F + f1, 1.0).color(1F, 1F, 1F, f).endVertex();
            wr.pos(0.0, 0.0, zLevel).tex(1.0F + f1, 0.0).color(1F, 1F, 1F, f).endVertex();
            wr.pos(0.0, height, zLevel).tex(0.0F + f1, 0.0).color(1F, 1F, 1F, f).endVertex();
        }
        tessellator.draw();
        GlStateManager.enableAlpha();
        GlStateManager.colorMask(true, true, true, true);
    }

    /** Minimal flat button renderer (no vanilla textures). */
    private static class FlatButton extends GuiButton {
        FlatButton(int id, int x, int y, int w, int h, String text) {
            super(id, x, y, w, h, text);
        }

        @Override
        public void drawButton(net.minecraft.client.Minecraft mc, int mouseX, int mouseY) {
            if (!visible) {
                return;
            }
            hovered = mouseX >= xPosition && mouseY >= yPosition
                    && mouseX < xPosition + width && mouseY < yPosition + height;
            int bg = hovered ? BTN_HOVER : BTN_IDLE;
            drawRect(xPosition, yPosition, xPosition + width, yPosition + height, bg);
            if (hovered) {
                drawRect(xPosition, yPosition, xPosition + width, yPosition + 1, ACCENT);
                drawRect(xPosition, yPosition + height - 1, xPosition + width, yPosition + height, ACCENT);
            } else {
                drawRect(xPosition, yPosition + height - 1, xPosition + width, yPosition + height, 0xFF161B21);
            }
            int color = enabled ? BTN_TEXT : MUTED;
            drawCenteredString(mc.fontRendererObj, displayString,
                    xPosition + width / 2, yPosition + (height - 8) / 2, color);
        }
    }
}
