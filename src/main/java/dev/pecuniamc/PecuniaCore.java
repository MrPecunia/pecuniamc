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

package dev.pecuniamc;

import dev.pecuniamc.capes.CapeIntegration;
import dev.pecuniamc.content.ContentMode;
import dev.pecuniamc.danger.ArsenicInventoryFix;
import dev.pecuniamc.danger.DangerGuiGate;
import dev.pecuniamc.danger.DangerKeybind;
import dev.pecuniamc.danger.DangerModeManager;
import dev.pecuniamc.integration.ChatTriggersIntegration;
import dev.pecuniamc.network.ChatHook;
import dev.pecuniamc.network.PecuniaWebSocket;
import dev.pecuniamc.network.ProfileManager;
import dev.pecuniamc.network.RankNickManager;
import dev.pecuniamc.network.SidebarRankSync;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;

import java.io.File;

/**
 * PecuniaMC client core.
 *
 * Holds branding constants, the STRICTLY HIDDEN danger-mode toggle and
 * lifecycle hooks for the Pecunia Network (websocket), capes, rank/nick
 * spoofing and optional third-party integrations (ChatTriggers, MinecraftCapes).
 *
 * The cheat/danger functionality is never referenced from any visible GUI:
 * see {@link dev.pecuniamc.gui.GuiPecuniaMainMenu} which only shows an
 * innocuous "Info" button.
 */
public final class PecuniaCore {

    public static final String CLIENT_NAME = "PecuniaMC";
    public static final String CLIENT_VERSION = "1.0.0";
    public static final String MC_VERSION = "1.8.9";
    /** Pecunia Network websocket endpoint. */
    public static final String SERVER_URL = "wss://pecuniaserver.onrender.com";

    /**
     * STRICTLY HIDDEN danger-mode toggle. Defaults to OFF.
     * Danger mode only controls Arsenic <i>visibility</i> (module lists,
     * toggle notifications, ClickGUI access): modules keep working silently
     * either way. No GUI element may expose this field.
     */
    private static volatile boolean isDangerModeEnabled = false;

    private static volatile PecuniaWebSocket socket;
    private static boolean initialized = false;

    private PecuniaCore() {
    }

    /** Hidden accessor - do not bind to any visible UI. */
    public static boolean isDangerModeEnabled() {
        return isDangerModeEnabled;
    }

    /** Hidden mutator - invoked only from internal keybind/config, never from menus. */
    public static void setDangerModeEnabled(boolean enabled) {
        if (isDangerModeEnabled == enabled) {
            return;
        }
        isDangerModeEnabled = enabled;
        DangerModeManager.applyDangerMode(enabled);
        try {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
            if (mc != null && mc.thePlayer != null && mc.theWorld != null) {
                mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(
                        "\u00A7b[PecuniaMC] \u00A7fDanger Mode: "
                                + (enabled ? "\u00A7aEnabled" : "\u00A7cDisabled")));
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * Called from the Forge mod init path (after every mod's preInit, so the
     * embedded Arsenic client has constructed). Re-applies the current danger
     * state to cover init-ordering races.
     */
    public static synchronized void onPostInit() {
        try {
            DangerModeManager.applyDangerMode(isDangerModeEnabled);
        } catch (Throwable t) {
            System.out.println("[PecuniaMC] PostInit danger sweep failed: " + t.getMessage());
        }
    }

    /** Returns true when OptiFine is on the classpath (render pipeline differs). */
    public static boolean isOptiFineLoaded() {
        try {
            Class.forName("optifine.OptiFineTweaker");
            return true;
        } catch (Throwable ignored) {
        }
        try {
            Class.forName("net.optifine.Config");
            return true;
        } catch (Throwable ignored) {
        }
        return Loader.isModLoaded("optifine");
    }

    public static PecuniaWebSocket getSocket() {
        return socket;
    }

    /**
     * Reconnects to the Pecunia Network (async, never blocks). Used by the
     * main-menu Sync button. If already connected this is a no-op.
     */
    public static synchronized void reconnectNetwork() {
        try {
            PecuniaWebSocket current = socket;
            if (current != null) {
                try {
                    if (current.isOpen()) {
                        return;
                    }
                } catch (Throwable ignored) {
                }
                try {
                    current.close();
                } catch (Throwable ignored) {
                }
            }
            socket = new PecuniaWebSocket(SERVER_URL);
            socket.connectAsync();
            System.out.println("[PecuniaMC] Reconnecting to Pecunia Network...");
        } catch (Throwable t) {
            System.out.println("[PecuniaMC] Reconnect failed: " + t.getMessage());
        }
    }

    /**
     * Called once from the Forge mod init path. Safe to call twice.
     */
    public static synchronized void init(File configDir) {
        if (initialized) {
            return;
        }
        initialized = true;

        ProfileManager.init();
        RankNickManager.loadLocal(configDir);
        ContentMode.load(configDir);

        // Danger mode starts OFF: Arsenic stays hidden and silent.
        DangerModeManager.applyDangerMode(false);

        MinecraftForge.EVENT_BUS.register(new ChatHook());
        MinecraftForge.EVENT_BUS.register(new SidebarRankSync());
        MinecraftForge.EVENT_BUS.register(new ArsenicInventoryFix());
        MinecraftForge.EVENT_BUS.register(new DangerGuiGate());
        DangerKeybind.register();
        MinecraftForge.EVENT_BUS.register(new DangerKeybind());

        // MinecraftCapes works out of the box via its own API; init reflectively
        // so the client still compiles/runs when the mod jar is absent.
        CapeIntegration.init();

        // ChatTriggers is an external mod; only hook when present.
        ChatTriggersIntegration.init();

        // Connect to the Pecunia Network in the background. Never crash startup
        // if the server is unreachable.
        try {
            socket = new PecuniaWebSocket(SERVER_URL);
            socket.connectAsync();
        } catch (Throwable t) {
            System.out.println("[PecuniaMC] WebSocket init failed: " + t.getMessage());
        }

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    RankNickManager.saveLocal(configDir);
                } catch (Throwable ignored) {
                }
                try {
                    ContentMode.save(configDir);
                } catch (Throwable ignored) {
                }
                try {
                    if (socket != null) {
                        // Async close: closeBlocking() can hang shutdown forever
                        // when the server never answers the close handshake.
                        socket.close();
                    }
                } catch (Throwable ignored) {
                }
            }
        }));
    }
}
