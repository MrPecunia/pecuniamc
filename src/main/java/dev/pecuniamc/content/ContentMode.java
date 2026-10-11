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

package dev.pecuniamc.content;

import dev.pecuniamc.network.RankNickManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.WorldSettings;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Properties;

/**
 * "Content mode" for content creators (YT videos etc.).
 *
 * When ON, {@code /ban}, {@code /gamemode 1} and {@code /gmc} (plus the
 * survival counterparts) are intercepted client-side and faked: a
 * customizable ban message, and a purely client-side creative mode (no
 * ability packet is ever sent, the server still sees a survival player).
 * Flying on real servers will still get you banned - this is for content.
 * When OFF everything passes through to the server untouched.
 *
 * Always defaults to OFF on launch for safety. Only the ban template is
 * persisted.
 */
public final class ContentMode {

    public static final String DEFAULT_BAN_TEMPLATE =
            "\u00A7c{player} has been banned from the server.";

    private static volatile boolean enabled;
    private static volatile String banTemplate = DEFAULT_BAN_TEMPLATE;
    private static File saveFile;

    private static boolean savedAllowFlying;
    private static boolean savedCreative;
    private static boolean savedDisableDamage;
    private static WorldSettings.GameType savedGameType;
    private static boolean hasSnapshot;
    private static java.lang.reflect.Field gameTypeField;

    private static java.lang.reflect.Field gameTypeField() {
        if (gameTypeField == null) {
            String[] names = {"currentGameType", "field_78779_k"};
            for (String name : names) {
                try {
                    java.lang.reflect.Field field =
                            net.minecraft.client.multiplayer.PlayerControllerMP.class
                                    .getDeclaredField(name);
                    field.setAccessible(true);
                    gameTypeField = field;
                    break;
                } catch (Throwable ignored) {
                }
            }
        }
        return gameTypeField;
    }

    private static WorldSettings.GameType readGameType(net.minecraft.client.multiplayer.PlayerControllerMP controller) {
        try {
            java.lang.reflect.Field field = gameTypeField();
            if (field != null) {
                Object value = field.get(controller);
                if (value instanceof WorldSettings.GameType) {
                    return (WorldSettings.GameType) value;
                }
            }
        } catch (Throwable ignored) {
        }
        return WorldSettings.GameType.SURVIVAL;
    }

    private static void writeGameType(net.minecraft.client.multiplayer.PlayerControllerMP controller,
                                      WorldSettings.GameType type) {
        try {
            java.lang.reflect.Field field = gameTypeField();
            if (field != null && type != null) {
                field.set(controller, type);
            }
        } catch (Throwable ignored) {
        }
    }

    private ContentMode() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        if (enabled == value) {
            return;
        }
        enabled = value;
        // NOTE: enabling content mode alone never changes the game mode -
        // fake creative starts only via /gamemode 1, /gmc, /gamemode c or
        // /gamemode creative.
        if (!enabled && hasSnapshot) {
            restoreSurvival();
        }
    }

    public static String getBanTemplate() {
        return banTemplate == null || banTemplate.isEmpty() ? DEFAULT_BAN_TEMPLATE : banTemplate;
    }

    public static void setBanTemplate(String template) {
        if (template == null || template.trim().isEmpty()) {
            banTemplate = DEFAULT_BAN_TEMPLATE;
        } else {
            banTemplate = template;
        }
    }

    public static void resetBanTemplate() {
        banTemplate = DEFAULT_BAN_TEMPLATE;
    }

    public static void load(File configDir) {
        try {
            if (configDir != null && !configDir.exists()) {
                configDir.mkdirs();
            }
            saveFile = configDir == null ? null : new File(configDir, "pecuniamc_content.properties");
            if (saveFile != null && saveFile.exists()) {
                Properties props = new Properties();
                FileReader reader = new FileReader(saveFile);
                try {
                    props.load(reader);
                } finally {
                    reader.close();
                }
                String template = props.getProperty("banformat");
                if (template != null && !template.isEmpty()) {
                    banTemplate = template;
                }
            }
        } catch (Throwable t) {
            System.out.println("[PecuniaMC] Failed to load content config: " + t.getMessage());
        }
    }

    public static void save(File configDir) {
        try {
            if (saveFile == null && configDir != null) {
                saveFile = new File(configDir, "pecuniamc_content.properties");
            }
            if (saveFile == null) {
                return;
            }
            Properties props = new Properties();
            props.setProperty("banformat", banTemplate == null ? "" : banTemplate);
            FileWriter writer = new FileWriter(saveFile);
            try {
                props.store(writer, "PecuniaMC content mode");
            } finally {
                writer.close();
            }
        } catch (Throwable t) {
            System.out.println("[PecuniaMC] Failed to save content config: " + t.getMessage());
        }
    }

    /** Fake ban message. Supports {player} {reason} and &-codes. */
    public static void fakeBan(String target, String reason) {
        try {
            String text = RankNickManager.translateColorCodes(getBanTemplate());
            text = text.replace("{player}", target == null ? "" : target);
            String cleanReason = reason == null ? "" : RankNickManager.translateColorCodes(reason);
            if (!cleanReason.isEmpty()) {
                if (text.contains("{reason}")) {
                    text = text.replace("{reason}", cleanReason);
                } else {
                    text = text + " \u00A77Reason: " + cleanReason;
                }
            } else {
                text = text.replace("{reason}", "").replaceAll("  +", " ");
            }
            tellPlayer(text);
        } catch (Throwable t) {
            System.out.println("[PecuniaMC] fakeBan failed: " + t.getMessage());
        }
    }

    /** Purely client-side creative: no ability packet is sent to the server. */
    public static void applyCreative() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.thePlayer == null || mc.playerController == null) {
                return;
            }
            EntityPlayerSP player = mc.thePlayer;
            if (!hasSnapshot) {
                savedAllowFlying = player.capabilities.allowFlying;
                savedCreative = player.capabilities.isCreativeMode;
                savedDisableDamage = player.capabilities.disableDamage;
                savedGameType = readGameType(mc.playerController);
                hasSnapshot = true;
            }
            player.capabilities.allowFlying = true;
            player.capabilities.isFlying = true;
            player.capabilities.isCreativeMode = true;
            player.capabilities.disableDamage = true;
            writeGameType(mc.playerController, WorldSettings.GameType.CREATIVE);
            tellPlayer("Set own game mode to Creative Mode");
        } catch (Throwable t) {
            System.out.println("[PecuniaMC] applyCreative failed: " + t.getMessage());
        }
    }

    /** Restores the pre-fake values (no-op when fake creative was never applied). */
    public static void restoreSurvival() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.thePlayer == null || mc.playerController == null) {
                hasSnapshot = false;
                return;
            }
            EntityPlayerSP player = mc.thePlayer;
            if (hasSnapshot) {
                player.capabilities.allowFlying = savedAllowFlying;
                player.capabilities.isCreativeMode = savedCreative;
                player.capabilities.disableDamage = savedDisableDamage;
                writeGameType(mc.playerController, savedGameType);
                player.capabilities.isFlying = false;
                hasSnapshot = false;
            }
            tellPlayer("Set own game mode to Survival Mode");
        } catch (Throwable t) {
            System.out.println("[PecuniaMC] restoreSurvival failed: " + t.getMessage());
        }
    }

    static void tellPlayer(String text) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null && mc.thePlayer != null) {
                mc.thePlayer.addChatMessage(new ChatComponentText(text));
            }
        } catch (Throwable ignored) {
        }
    }
}
