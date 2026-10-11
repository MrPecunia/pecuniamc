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

package dev.pecuniamc.network;

import dev.pecuniamc.PecuniaCore;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumChatFormatting;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Local rank/nick state, ported from RankSpoof/CustomRankUpdated (legacy/ornithe)
 * to Forge 1.8.9 string-based formatting.
 *
 * RankSpoof's modern editions manipulate Components; on 1.8.9 chat/tab/nametag
 * are legacy {@code EnumChatFormatting} strings, so spoofing is applied as a
 * rank prefix prepended to the (possibly replaced) username.
 */
public final class RankNickManager {

    private static volatile String localRank = "";
    private static volatile String localNick = "";
    private static File saveFile;

    private RankNickManager() {
    }

    public static void loadLocal(File configDir) {
        try {
            if (configDir != null && !configDir.exists()) {
                configDir.mkdirs();
            }
            saveFile = configDir == null ? null : new File(configDir, "pecuniamc_profile.properties");
            if (saveFile != null && saveFile.exists()) {
                Properties props = new Properties();
                FileReader reader = new FileReader(saveFile);
                try {
                    props.load(reader);
                } finally {
                    reader.close();
                }
                localRank = props.getProperty("rank", "");
                localNick = props.getProperty("nick", "");
            }
        } catch (Throwable t) {
            System.out.println("[PecuniaMC] Failed to load local profile: " + t.getMessage());
        }
    }

    public static void saveLocal(File configDir) {
        try {
            if (saveFile == null && configDir != null) {
                saveFile = new File(configDir, "pecuniamc_profile.properties");
            }
            if (saveFile == null) {
                return;
            }
            Properties props = new Properties();
            props.setProperty("rank", localRank == null ? "" : localRank);
            props.setProperty("nick", localNick == null ? "" : localNick);
            FileWriter writer = new FileWriter(saveFile);
            try {
                props.store(writer, "PecuniaMC local rank/nick");
            } finally {
                writer.close();
            }
        } catch (Throwable t) {
            System.out.println("[PecuniaMC] Failed to save local profile: " + t.getMessage());
        }
    }

    public static String getLocalRank() {
        return localRank == null ? "" : localRank;
    }

    public static String getLocalNick() {
        return localNick == null ? "" : localNick;
    }

    /**
     * Updates the local rank/nick, refreshes the local {@link ProfileManager}
     * entry and broadcasts
     * {@code {"type":"update_profile","uuid":"...","rank":"...","nick":"..."}}
     * to {@code wss://pecuniaserver.onrender.com}.
     */
    public static void setLocalRankNick(String rank, String nick) {
        localRank = rank == null ? "" : rank;
        localNick = nick == null ? "" : nick;
        pushLocalProfile();
    }

    /** Pushes the current local profile to the websocket + local cache. */
    public static void pushLocalProfile() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.getSession() == null) {
                return;
            }
            String uuidStr = mc.getSession().getPlayerID();
            if (uuidStr == null || uuidStr.isEmpty()) {
                return;
            }
            UUID uuid = parseUuid(uuidStr);
            if (uuid == null) {
                System.out.println("[PecuniaMC] Failed to push local profile: Invalid UUID string: " + uuidStr);
                return;
            }
            ProfileManager.updateProfile(uuid, localRank, localNick);
            PecuniaWebSocket socket = PecuniaCore.getSocket();
            if (socket != null) {
                socket.sendProfileUpdate(uuid, localRank, localNick);
            }
        } catch (Throwable t) {
            System.out.println("[PecuniaMC] Failed to push local profile: " + t.getMessage());
        }
    }

    /** Hypixel-style rank prefix, e.g. "[MVP++] " (possibly with legacy color codes inside). */
    private static final Pattern RANK_PREFIX_PATTERN =
            Pattern.compile("^(?:\u00A7[0-9a-fk-or])*\\[[^\\[\\]]+\\](?:\u00A7[0-9a-fk-or])*\\s?");

    /**
     * Applies spoofing to a display string: replaces the real username with the
     * spoofed nick (whole-word only, so substrings inside other words are left
     * alone) and <i>replaces</i> any existing Hypixel-style rank prefix with
     * the spoofed rank instead of stacking them (CustomRankUpdated/RankSpoof
     * semantics, ported to 1.8.9 strings).
     */
    public static String applySpoof(SpoofedProfile profile, String realName, String currentDisplay) {
        if (profile == null || realName == null) {
            return currentDisplay;
        }
        if (!profile.hasNick() && !profile.hasRank()) {
            return currentDisplay;
        }
        String base = currentDisplay == null ? realName : currentDisplay;
        String shown = base;
        if (profile.hasNick()) {
            String nick = profile.getNick();
            String replaced = replaceWholeWord(shown, realName, nick);
            if (!replaced.equals(shown)) {
                shown = replaced;
            }
            // else: name embedded in a longer word (or absent) - leave the
            // line untouched rather than destroying its context.
        }
        if (profile.hasRank()) {
            String rank = translateColorCodes(profile.getRank());
            // Replace (not stack): drop one existing Hypixel-style rank prefix first.
            String withoutRank = RANK_PREFIX_PATTERN.matcher(shown).replaceFirst("");
            if (!withoutRank.equals(shown)) {
                shown = withoutRank;
            }
            // Drop a rank block attached mid-line to the username (party /
            // sidebar lines like "Party: [VIP] Name").
            String effective = effectiveNameInText(shown, profile, realName);
            shown = stripRankBeforeName(shown, effective);
            // Insert the rank right before the username (not always at 0, so
            // mid-line names keep their position). Skip if already placed.
            int at = indexOfWord(shown, effective);
            if (at >= 0) {
                if (!shown.substring(0, at).endsWith(rank)) {
                    shown = shown.substring(0, at) + rank + shown.substring(at);
                }
            } else if (!shown.startsWith(rank)) {
                shown = rank + shown;
            }
        }
        return shown;
    }

    private static String effectiveNameInText(String shown, SpoofedProfile profile, String realName) {
        try {
            if (profile.hasNick() && indexOfWord(shown, profile.getNick()) >= 0) {
                return profile.getNick();
            }
        } catch (Throwable ignored) {
        }
        return realName;
    }

    private static int indexOfWord(String text, String word) {
        try {
            java.util.regex.Matcher m = Pattern.compile("(?<!\\w)" + Pattern.quote(word) + "(?!\\w)")
                    .matcher(text);
            if (m.find()) {
                return m.start();
            }
        } catch (Throwable ignored) {
        }
        return -1;
    }

    /**
     * Hypixel-style rank tokens. Only these bracket blocks may be stripped in
     * front of a username (a generic block like {@code [Party]} is left alone).
     */
    private static final java.util.Set<String> RANK_TOKENS = new java.util.HashSet<String>(
            java.util.Arrays.asList(
                    "VIP", "VIP+", "MVP", "MVP+", "MVP++",
                    "YOUTUBE", "YT", "APPLE",
                    "HELPER", "MOD", "MODERATOR", "ADMIN", "OWNER", "GM",
                    "PIG", "PIG+", "PIG++", "PIG+++",
                    "MOJANG", "EVENTS", "STAFF", "BUILDER", "SLOTH", "MIXER", "BEAM",
                    "PARTNER", "BETA", "GOD", "NONE"));

    private static boolean isRankToken(String inner) {
        try {
            if (inner == null) {
                return false;
            }
            String cleaned = EnumChatFormatting.getTextWithoutFormattingCodes(inner);
            if (cleaned == null) {
                return false;
            }
            return RANK_TOKENS.contains(cleaned.trim().toUpperCase());
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Removes a <i>rank-like</i> {@code [...]} block that directly precedes the
     * username (optional spaces/legacy codes in between), e.g. the
     * {@code [VIP]} in {@code "Party: [VIP] Name"}. Anything else in brackets
     * is preserved.
     */
    private static String stripRankBeforeName(String text, String name) {
        try {
            Matcher blocks = Pattern.compile("\\[[^\\[\\]]+\\]").matcher(text);
            StringBuilder out = new StringBuilder();
            int last = 0;
            boolean changed = false;
            while (blocks.find()) {
                String inner = blocks.group();
                inner = inner.substring(1, Math.max(1, inner.length() - 1));
                if (!isRankToken(inner)) {
                    continue;
                }
                int p = blocks.end();
                while (p < text.length()) {
                    char c = text.charAt(p);
                    if (c == ' ') {
                        p++;
                    } else if (c == '\u00A7' && p + 1 < text.length()) {
                        p += 2;
                    } else {
                        break;
                    }
                }
                String tail = text.substring(p);
                if (tail.regionMatches(true, 0, name, 0, name.length())
                        && (tail.length() == name.length()
                        || !Character.isLetterOrDigit(tail.charAt(name.length())))) {
                    out.append(text, last, blocks.start());
                    last = p;
                    changed = true;
                }
            }
            if (changed) {
                out.append(text, last, text.length());
                return out.toString();
            }
        } catch (Throwable ignored) {
        }
        return text;
    }

    /** Map lookup with local-player fallback (see {@link #localSpoofFor}). */
    public static String applySpoof(UUID uuid, String realName, String currentDisplay) {
        if (uuid == null || realName == null) {
            return currentDisplay;
        }
        SpoofedProfile profile = ProfileManager.getProfile(uuid);
        if (profile == null) {
            profile = localSpoofFor(uuid, realName);
        }
        return applySpoof(profile, realName, currentDisplay);
    }

    /**
     * Local-player fallback: when the network map has no entry (offline session,
     * singleplayer, socket down) but the viewed identity is the local player,
     * spoof from the locally configured rank/nick. This is what makes the tab
     * entry, chat lines and nametag work exactly like CustomRankUpdated's
     * client-side display, with zero network dependency.
     */
    public static SpoofedProfile localSpoofFor(UUID uuid, String realName) {
        try {
            if (!isLocalIdentity(uuid, realName)) {
                return null;
            }
            String rank = getLocalRank();
            String nick = getLocalNick();
            if ((rank == null || rank.isEmpty()) && (nick == null || nick.isEmpty())) {
                return null;
            }
            UUID id = uuid != null ? uuid : new UUID(0L, 0L);
            return new SpoofedProfile(id, rank == null ? "" : rank, nick == null ? "" : nick);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** True when the uuid or name belongs to the local session player. */    public static boolean isLocalIdentity(UUID uuid, String realName) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.getSession() == null) {
                return false;
            }
            if (realName != null && realName.equalsIgnoreCase(mc.getSession().getUsername())) {
                return true;
            }
            if (uuid != null) {
                UUID sessionId = parseUuid(mc.getSession().getPlayerID());
                if (sessionId != null && uuid.equals(sessionId)) {
                    return true;
                }
            }
            try {
                if (mc.thePlayer != null && uuid != null && uuid.equals(mc.thePlayer.getUniqueID())) {
                    return true;
                }
                if (mc.thePlayer != null && realName != null
                        && realName.equalsIgnoreCase(mc.thePlayer.getName())) {
                    return true;
                }
            } catch (Throwable ignored) {
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    /**
     * Parses Mojang UUID strings with or without dashes (the session service
     * hands out undashed ids like {@code d6acc2e7...}, which
     * {@code UUID.fromString} rejects).
     */
    public static UUID parseUuid(String value) {
        if (value == null) {
            return null;
        }
        String s = value.trim();
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException first) {
            if (s.matches("[0-9a-fA-F]{32}")) {
                String dashed = s.substring(0, 8) + "-" + s.substring(8, 12) + "-"
                        + s.substring(12, 16) + "-" + s.substring(16, 20) + "-"
                        + s.substring(20);
                try {
                    return UUID.fromString(dashed);
                } catch (IllegalArgumentException ignored) {
                }
            }
            return null;
        }
    }

    /** Local session username, or null outside a session. */
    public static String localUsername() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null && mc.getSession() != null) {
                return mc.getSession().getUsername();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /** Replaces whole-word occurrences only (formatting codes count as boundaries). */
    private static String replaceWholeWord(String text, String word, String replacement) {
        try {
            return Pattern.compile("(?<!\\w)" + Pattern.quote(word) + "(?!\\w)")
                    .matcher(text)
                    .replaceAll(Matcher.quoteReplacement(replacement));
        } catch (Throwable ignored) {
            return text.replace(word, replacement);
        }
    }

    /** Legacy '&' color codes -> section-sign codes used by 1.8.9. */
    public static String translateColorCodes(String text) {
        if (text == null) {
            return "";
        }
        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length - 1; i++) {
            if (chars[i] == '&' && "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(chars[i + 1]) > -1) {
                chars[i] = '\u00A7';
                chars[i + 1] = Character.toLowerCase(chars[i + 1]);
            }
        }
        return new String(chars);
    }

    /** Strips legacy formatting for safe comparisons. */
    public static String stripFormatting(String text) {
        if (text == null) {
            return "";
        }
        return EnumChatFormatting.getTextWithoutFormattingCodes(text);
    }
}
