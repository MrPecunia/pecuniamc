/*
 * Modifications copyright (C) 2026 PrPecunia, Webget, Diamond Gaming, Eris Group, Pecunia Enterprise (GPL-3.0, see LICENSE).
 * Original code: Arsenic Client (MIT, see licenses/MIT-Arsenic.txt).
 */

package arsenic.runtime.hooks;

import arsenic.runtime.Access;
import arsenic.main.Arsenic;
import arsenic.module.impl.blatant.KillAura;
import arsenic.module.impl.visual.custommainmenu.MenuTheme;
import arsenic.module.impl.visual.custommainmenu.ScreenTransition;
import dev.pecuniamc.PecuniaCore;
import dev.pecuniamc.gui.GuiConfirmDanger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.achievement.GuiAchievement;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;

import java.util.List;

/** Screen and widget hooks. */
public final class GuiHooks {

    private static final Access.FieldRef INPUT_FIELD = Access.field(GuiChat.class, "inputField");
    private static final Access.FieldRef BUTTON_LIST = Access.field(GuiScreen.class, "buttonList");
    private static final Access.FieldRef HOVERED = Access.field(GuiButton.class, "hovered");
    private static final Access.MethodRef MOUSE_DRAGGED = Access.method(GuiButton.class, "mouseDragged", Minecraft.class, int.class, int.class);
    // protected in Minecraft (Forge makes them public), so read by reflection to work in every game
    private static final Access.FieldRef SLOT_WIDTH = Access.field(GuiSlot.class, "width");
    private static final Access.FieldRef SLOT_TOP = Access.field(GuiSlot.class, "top");
    private static final Access.FieldRef SLOT_BOTTOM = Access.field(GuiSlot.class, "bottom");
    private static final Access.FieldRef SLOT_LEFT = Access.field(GuiSlot.class, "left");
    private static final Access.FieldRef SLOT_RIGHT = Access.field(GuiSlot.class, "right");

    private static final ResourceLocation WIDGETS = new ResourceLocation("textures/gui/widgets.png");

    // chat completion preview, computed on key press and drawn with the screen
    private static String trimmedAutoCompletion;
    private static String lastArg;
    private static Boolean isLastArgValidArg;

    // only one container screen is open at a time
    private static final int KILLAURA_BUTTON_ID = 7331;
    private static GuiButton killAuraButton;

    private GuiHooks() {}

    /**
     * PecuniaMC gate: Arsenic's visual takeover (ocean backgrounds, themed
     * widgets, container button, screen transitions) is active ONLY while
     * danger mode is on. Chat/command hooks below stay always active so the
     * {@code .danger} command works from a clean client too. Defaults to
     * {@code false} (vanilla look) on any error.
     */
    private static boolean danger() {
        try {
            return PecuniaCore.isDangerModeEnabled();
        } catch (Throwable ignored) {
            return false;
        }
    }

    // ---- GuiChat ----

    /**
     * PecuniaMC: Arsenic's "." command suggestions are permanently disabled
     * (no autocomplete computation, no theme tint, no Tab completion, no
     * preview rendering). "." messages behave like vanilla chat; Arsenic
     * commands still execute when danger mode routes them.
     */

    /** GuiChat.keyTyped RETURN. */
    public static void chatKeyTypedReturn(GuiChat self, char typedChar, int keyCode) {
    }

    /** GuiChat.keyTyped HEAD. @return true to cancel */
    public static boolean chatKeyTypedHead(GuiChat self, char typedChar, int keyCode) {
        return false;
    }

    /** GuiChat.drawScreen RETURN. */
    public static void chatDrawScreenReturn(GuiChat self, int mouseX, int mouseY, float partialTicks) {
    }

    // ---- GuiContainer ----

    private static KillAura killAura() {
        return Arsenic.getArsenic().getModuleManager().getModuleByClass(KillAura.class);
    }

    private static String killAuraLabel() {
        KillAura aura = killAura();
        return "KillAura: " + (aura != null && aura.isEnabled() ? "ON" : "OFF");
    }

    /** GuiContainer.initGui TAIL. */
    public static void containerInitGuiTail(GuiContainer self) {
        // PecuniaMC: the KillAura inventory button is never added, danger on or off.
        killAuraButton = null;
    }

    /** GuiContainer.drawScreen HEAD. */
    public static void containerDrawScreenHead(GuiContainer self, int mouseX, int mouseY, float partialTicks) {
        // PecuniaMC: nothing to refresh, the button is never added.
    }

    /** GuiContainer.mouseClicked HEAD (GuiContainer doesn't declare actionPerformed). @return true to cancel */
    public static boolean containerMouseClickedHead(GuiContainer self, int mouseX, int mouseY, int mouseButton) {
        // PecuniaMC: no inventory button exists, never intercept clicks.
        return false;
    }

    // ---- GuiScreen ----

    /** GuiScreen.drawWorldBackground HEAD. PecuniaMC: always vanilla. @return true */
    public static boolean drawWorldBackground(GuiScreen self, int tint) {
        return false;
    }

    /** GuiScreen.drawBackground HEAD. PecuniaMC: always vanilla. @return true */
    public static boolean drawBackground(GuiScreen self, int tint) {
        return false;
    }

    /** GuiScreen.sendChatMessage(String, boolean) HEAD. @return true to cancel */
    public static boolean sendChatMessage(GuiScreen self, String msg, boolean addToChat) {
        if (dev.pecuniamc.chat.ChatCommands.handleOutgoing(msg)) {
            return true;
        }
        if (!danger()) {
            // PecuniaMC: danger mode off behaves like a vanilla client - messages
            // starting with "." are sent to the server as normal chat.
            return false;
        }
        if (!msg.startsWith("."))
            return false;
        Arsenic.getInstance().getCommandManager().executeCommand(msg);
        Minecraft.getMinecraft().ingameGUI.getChatGUI().addToSentMessages(msg);
        return true;
    }

    /**
     * PecuniaMC chat commands (delegated).
     *
     * @deprecated logic moved to {@link dev.pecuniamc.chat.ChatCommands},
     * shared with the packet-level intercept.
     */
    @Deprecated
    private static void handleDangerModeCommand(String msg) {
        dev.pecuniamc.chat.ChatCommands.handleOutgoing(msg);
    }

    private static void tellPlayer(String text) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null && mc.thePlayer != null) {
                mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(text));
            }
        } catch (Throwable ignored) {
        }
    }

    // ---- lists, buttons, sliders ----

    /** GuiSlot.drawContainerBackground HEAD (added by Forge). PecuniaMC: always vanilla. @return true */
    public static boolean slotContainerBackground(GuiSlot self, Tessellator tessellator) {
        return false;
    }

    /** GuiSlot.overlayBackground HEAD. PecuniaMC: always vanilla. @return true */
    public static boolean slotOverlayBackground(GuiSlot self, int startY, int endY, int startAlpha, int endAlpha) {
        return false;
    }

    /** GuiButton.drawButton HEAD. PecuniaMC: always vanilla. @return true */
    public static boolean drawButton(GuiButton self, Minecraft mc, int mouseX, int mouseY) {
        return false;
    }

    /** GuiOptionSlider knob: PecuniaMC keeps the vanilla knob, danger on or off. */
    public static void sliderKnob(GuiOptionSlider self, int x, int y, int u, int v, int w, int h) {
        drawVanillaKnobHalf(x, y, u, v);
    }

    /** GuiOptionSlider knob (second half): PecuniaMC keeps the vanilla knob. */
    public static void sliderKnobSecondHalf(GuiOptionSlider self, int x, int y, int u, int v, int w, int h) {
        drawVanillaKnobHalf(x, y, u, v);
    }

    /**
     * Vanilla slider-knob replica (see {@code GuiOptionSlider.mouseDragged}):
     * one 4x20 half of {@code textures/gui/widgets.png}. Used while danger
     * mode is off so sliders keep their vanilla look.
     */
    private static void drawVanillaKnobHalf(int x, int y, int u, int v) {
        try {
            Minecraft.getMinecraft().getTextureManager().bindTexture(WIDGETS);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            net.minecraft.client.gui.Gui.drawModalRectWithCustomSizedTexture(
                    x, y, (float) u, (float) v, 4, 20, 256.0F, 256.0F);
        } catch (Throwable ignored) {
        }
    }

    /** GuiAchievement.updateAchievementWindow HEAD. PecuniaMC: vanilla toast stays. @return true */
    public static boolean updateAchievementWindow(GuiAchievement self) {
        return false;
    }

    /**
     * EntityRenderer.updateCameraAndRender, the call to GuiScreen.drawScreen (games without Forge, which draw the
     * screen there): the same cross-fade as {@link #forgeDrawScreenHead}.
     */
    public static void drawScreen(GuiScreen screen, int mouseX, int mouseY, float partialTicks) {
        // PecuniaMC: no cross-fade wrapper, plain vanilla draw.
        screen.drawScreen(mouseX, mouseY, partialTicks);
    }

    /** ForgeHooksClient.drawScreen HEAD. PecuniaMC: no wrapper. */
    public static void forgeDrawScreenHead(GuiScreen screen, int mouseX, int mouseY, float partialTicks) {
    }

    /** ForgeHooksClient.drawScreen RETURN. PecuniaMC: no wrapper. */
    public static void forgeDrawScreenReturn(GuiScreen screen, int mouseX, int mouseY, float partialTicks) {
    }
}
