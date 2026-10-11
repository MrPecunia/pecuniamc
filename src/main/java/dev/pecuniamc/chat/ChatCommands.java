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

package dev.pecuniamc.chat;

import dev.pecuniamc.PecuniaCore;
import dev.pecuniamc.gui.GuiConfirmDanger;
import dev.pecuniamc.network.RankNickManager;
import net.minecraft.client.Minecraft;

/**
 * Outgoing-chat commands for PecuniaMC (always active, danger on or off).
 *
 * Called from both the chat-screen hook and the player packet-level hook, so
 * commands are intercepted no matter which vanilla path the message takes.
 * Matching is case-insensitive and whitespace-tolerant. Returns true when the
 * message was consumed (never sent to the server).
 *
 * <ul>
 *   <li>{@code /dangermode <on|off>} - danger toggle ("on" opens the safety box).</li>
 *   <li>{@code /setrank <rank|clear>} - sets the Pecunia Network rank prefix
 *       (supports {@code &} color codes), synced to other PecuniaMC players.</li>
 *   <li>{@code /setnick <nick|clear>} - sets the Pecunia Network nickname,
 *       synced to other PecuniaMC players.</li>
 *   <li>{@code /contentmode <on|off>} - content-mode toggle (always active).</li>
 *   <li>{@code /ban}, {@code /banformat}, {@code /gamemode}, {@code /gmc} -
 *       intercepted only while content mode is ON, passed to the server
 *       untouched while OFF.</li>
 * </ul>
 */
public final class ChatCommands {

    private ChatCommands() {
    }

    public static boolean handleOutgoing(String rawMessage) {
        try {
            if (rawMessage == null) {
                return false;
            }
            String msg = rawMessage.trim();
            if (matches(msg, "/dangermode")) {
                return handleDangerMode(msg.substring("/dangermode".length()).trim());
            }
            if (matches(msg, "/setrank")) {
                handleSetRank(msg.substring("/setrank".length()).trim());
                return true;
            }
            if (matches(msg, "/setnick")) {
                handleSetNick(msg.substring("/setnick".length()).trim());
                return true;
            }
            if (matches(msg, "/contentmode")) {
                handleContentMode(msg.substring("/contentmode".length()).trim());
                return true;
            }
            if (matches(msg, "/pmc") || matches(msg, "/pecunia")) {
                String keyword = matches(msg, "/pmc") ? "/pmc" : "/pecunia";
                handleHelp(msg.length() > keyword.length()
                        ? msg.substring(keyword.length()).trim() : "");
                return true;
            }
            if (matches(msg, "/author")) {
                addToSentMessages(msg);
                dev.pecuniamc.gui.GuiAuthorCredits.requestOpen();
                return true;
            }
            if (isContentCommand(msg)) {
                // Handled (or deliberately passed through) below.
                return handleContentCommand(msg);
            }
            return false;
        } catch (Throwable t) {
            System.out.println("[PecuniaMC] chat command failed: " + t.getMessage());
            return false;
        }
    }

    private static boolean matches(String msg, String prefix) {
        return msg.length() >= prefix.length()
                && msg.regionMatches(true, 0, prefix, 0, prefix.length())
                && (msg.length() == prefix.length() || msg.charAt(prefix.length()) == ' ');
    }

    private static boolean handleDangerMode(String rest) {
        String arg = rest.isEmpty() ? "" : rest.split("\\s+")[0].toLowerCase();
        if (arg.equals("on")) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null) {
                addToSentMessages("/dangermode on");
                // Deferred by one tick: opening a screen synchronously from
                // chat input gets wiped instantly by the chat screen's own
                // post-send close. The client tick handler opens it instead.
                GuiConfirmDanger.requestOpen();
            }
            return true;
        }
        if (arg.equals("off")) {
            addToSentMessages("/dangermode off");
            PecuniaCore.setDangerModeEnabled(false);
            return true;
        }
        tellPlayer("\u00A7b[PecuniaMC]\u00A7f Usage: /dangermode <on|off>");
        return true;
    }

    private static void handleSetRank(String rest) {
        addToSentMessages("/setrank " + rest);
        if (isClear(rest)) {
            RankNickManager.setLocalRankNick("", RankNickManager.getLocalNick());
            tellPlayer("\u00A7b[PecuniaMC]\u00A7f Rank cleared.");
            return;
        }
        String rank = cap(rest, 24);
        RankNickManager.setLocalRankNick(rank, RankNickManager.getLocalNick());
        tellPlayer("\u00A7b[PecuniaMC]\u00A7f Rank set to "
                + RankNickManager.translateColorCodes(rank) + "\u00A7r\u00A7f (synced).");
    }

    private static void handleSetNick(String rest) {        addToSentMessages("/setnick " + rest);
        if (isClear(rest)) {
            RankNickManager.setLocalRankNick(RankNickManager.getLocalRank(), "");
            tellPlayer("\u00A7b[PecuniaMC]\u00A7f Nick cleared.");
            return;
        }
        String nick = cap(stripColorCodes(rest).replace(" ", ""), 16);
        if (nick.isEmpty()) {
            tellPlayer("\u00A7b[PecuniaMC]\u00A7f Usage: /setnick <nick|clear>");
            return;
        }
        RankNickManager.setLocalRankNick(RankNickManager.getLocalRank(), nick);
        tellPlayer("\u00A7b[PecuniaMC]\u00A7f Nick set to " + nick + " (synced).");
    }

    private static void handleContentMode(String rest) {
        addToSentMessages("/contentmode " + rest);
        String arg = rest.isEmpty() ? "" : rest.split("\\s+")[0].toLowerCase();
        if (arg.equals("on")) {
            dev.pecuniamc.gui.GuiConfirmContent.requestOpen();
            return;
        }
        if (arg.equals("off")) {
            dev.pecuniamc.content.ContentMode.setEnabled(false);
            tellPlayer("\u00A7b[PecuniaMC]\u00A7f Content Mode: \u00A7cDisabled");
            return;
        }
        tellPlayer("\u00A7b[PecuniaMC]\u00A7f Content Mode is "
                + (dev.pecuniamc.content.ContentMode.isEnabled() ? "\u00A7aON" : "\u00A7cOFF")
                + "\u00A7f. Usage: /contentmode <on|off>");
    }

    private static boolean isContentCommand(String msg) {
        return matches(msg, "/ban") || matches(msg, "/banformat") || matches(msg, "/customban")
                || matches(msg, "/gamemode") || matches(msg, "/gmc")
                || matches(msg, "/gms");
    }

    /**
     * Fake content commands. While content mode is ON they are handled locally
     * and never reach the server; while OFF they pass through untouched so the
     * server (permissions) decides.
     *
     * @return true when consumed.
     */
    private static boolean handleContentCommand(String msg) {
        if (!dev.pecuniamc.content.ContentMode.isEnabled()) {
            return false;
        }
        if (matches(msg, "/banformat") || matches(msg, "/customban")) {
            String keyword = matches(msg, "/banformat") ? "/banformat" : "/customban";
            String rest = msg.length() > keyword.length()
                    ? msg.substring(keyword.length()).trim() : "";
            addToSentMessages(msg);
            if (rest.isEmpty()) {
                tellPlayer("\u00A7b[PecuniaMC]\u00A7f Ban format: "
                        + dev.pecuniamc.content.ContentMode.getBanTemplate());
            } else if (isClear(rest)) {
                dev.pecuniamc.content.ContentMode.resetBanTemplate();
                tellPlayer("\u00A7b[PecuniaMC]\u00A7f Ban format reset.");
            } else {
                dev.pecuniamc.content.ContentMode.setBanTemplate(rest);
                tellPlayer("\u00A7b[PecuniaMC]\u00A7f Ban format saved. /ban will use it.");
            }
            return true;
        }
        if (matches(msg, "/ban")) {
            String rest = msg.length() > "/ban".length() ? msg.substring("/ban".length()).trim() : "";
            addToSentMessages(msg);
            if (rest.isEmpty()) {
                tellPlayer("\u00A7cUsage: /ban <player> [reason]");
                return true;
            }
            String[] parts = rest.split("\\s+", 2);
            String target = parts[0];
            String reason = parts.length > 1 ? parts[1] : "";
            dev.pecuniamc.content.ContentMode.fakeBan(target, reason);
            return true;
        }
        if (matches(msg, "/gmc") || isGamemode(msg, "1", "creative", "c")) {
            addToSentMessages(msg);
            dev.pecuniamc.content.ContentMode.applyCreative();
            return true;
        }
        if (matches(msg, "/gms") || isGamemode(msg, "0", "survival", "s")) {
            addToSentMessages(msg);
            dev.pecuniamc.content.ContentMode.restoreSurvival();
            return true;
        }
        return false;
    }

    private static boolean isGamemode(String msg, String... options) {
        if (!matches(msg, "/gamemode")) {
            return false;
        }
        String rest = msg.length() > "/gamemode".length() ? msg.substring("/gamemode".length()).trim() : "";
        if (rest.isEmpty()) {
            return false;
        }
        String first = rest.split("\\s+")[0].toLowerCase();
        for (String option : options) {
            if (first.equals(option)) {
                return true;
            }
        }
        return false;
    }

    /**
     * PecuniaMC help. Lists every client command except /dangermode, which
     * stays hidden for now.
     */
    private static void handleHelp(String rest) {
        addToSentMessages("/pmc " + rest);
        tellPlayer("\u00A72\u2503 \u00A7a\u00A7lPecuniaMC\u00A7r \u00A72\u2503");
        tellPlayer("\u00A7a[Profile] \u00A77(rank & nick, synced to other PecuniaMC players)");
        tellPlayer("\u00A72/setrank <rank|clear> \u00A7f- set your rank prefix in chat, tab, nametags and sidebar. Supports & color codes.");
        tellPlayer("\u00A72/setnick <nick|clear> \u00A7f- set the nickname shown instead of your username everywhere.");
        tellPlayer("\u00A7a[Content] \u00A77(fake commands for videos, need /contentmode on)");
        tellPlayer("\u00A72/contentmode <on|off> \u00A7f- enable or disable the fake content commands below. Off by default.");
        tellPlayer("\u00A72/ban <player> [reason] \u00A7f- show a fake ban message, visible only to you. Nothing is sent to the server.");
        tellPlayer("\u00A72/customban <text> \u00A7f- rewrite the ban message freely, with {player}, {reason} and & colors. Empty shows it, clear resets it.");
        tellPlayer("\u00A72/gamemode 1|creative \u00A7f- client-side fake creative mode with fly. The server still sees survival.");
        tellPlayer("\u00A72/gamemode 0|survival \u00A7f- leave fake creative, restore your previous abilities.");
        tellPlayer("\u00A72/gmc \u00A7f- shortcut for fake creative  \u00A78|  \u00A72/gms \u00A7f- shortcut back to survival.");
        tellPlayer("\u00A72/author \u00A7f- credits and thanks to MrPecunia.");
        tellPlayer("\u00A78\u00BB \u00A77Type \u00A7a/pmc help \u00A77anytime to see this list again.");
    }

    private static boolean isClear(String rest) {
        return rest.equalsIgnoreCase("clear") || rest.equalsIgnoreCase("off")
                || rest.equalsIgnoreCase("reset");
    }

    private static String cap(String text, int max) {
        if (text != null && text.length() > max) {
            return text.substring(0, max);
        }
        return text == null ? "" : text;
    }

    private static String stripColorCodes(String text) {
        if (text == null) {
            return "";
        }
        String out = text.replaceAll("(?i)&[0-9a-fk-or]", "");
        return net.minecraft.util.EnumChatFormatting.getTextWithoutFormattingCodes(out);
    }

    private static void addToSentMessages(String msg) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null && mc.ingameGUI != null && mc.ingameGUI.getChatGUI() != null) {
                mc.ingameGUI.getChatGUI().addToSentMessages(msg);
            }
        } catch (Throwable ignored) {
        }
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
}
