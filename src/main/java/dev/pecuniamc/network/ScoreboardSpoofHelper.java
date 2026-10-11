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

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.EnumChatFormatting;

import java.util.Collection;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Scoreboard name rewriting (Hypixel sidebar rank lines included).
 *
 * Lives in a NORMAL class on purpose: Mixin merges every helper method into
 * the target class, and on this exact instanceof+cast shape the merged
 * bytecode failed production verification ({@code VerifyError} on Hypixel
 * join). Called via a 3-line trampoline from
 * {@code MixinScoreboardSpoof}; this file itself is only ever processed by
 * ForgeGradle reobf like the other ~200 classes.
 */
public final class ScoreboardSpoofHelper {


    private static boolean debugLoggedCtx;

    private ScoreboardSpoofHelper() {
    }

    /**
     * @return rewritten full line, or null to keep vanilla behavior.
     */
    public static String rewrite(Team team, String name) {
        try {
            if (name == null || name.isEmpty()) {
                return null;
            }
            // Hypixel sidebar lines are team-split: the entry name is often
            // invisible filler while prefix/suffix carry the text. Match and
            // rewrite on the FULL formatted line, like CT's Score.getName().
            String prefix = "";
            String suffix = "";
            if (team instanceof ScorePlayerTeam) {
                ScorePlayerTeam playerTeam = (ScorePlayerTeam) team;
                try {
                    String p = playerTeam.getColorPrefix();
                    if (p != null) {
                        prefix = p;
                    }
                } catch (Throwable ignored) {
                }
                try {
                    String s = playerTeam.getColorSuffix();
                    if (s != null) {
                        suffix = s;
                    }
                } catch (Throwable ignored) {
                }
            }
            String full = prefix + name + suffix;
            String stripped = EnumChatFormatting.getTextWithoutFormattingCodes(full);
            if (stripped == null || stripped.isEmpty()) {
                return null;
            }
            if (!debugLoggedCtx) {
                debugLoggedCtx = true;
                int profiles = 0;
                try {
                    profiles = ProfileManager.snapshot().size();
                } catch (Throwable ignored) {
                }
                System.out.println("[PecuniaMC] sidebar ctx: hypixel=" + isHypixel()
                        + " localRank=" + RankNickManager.getLocalRank()
                        + " profiles=" + profiles);
            }

            // Case 1: a known player name appears in the line.
            SpoofedProfile byName = findProfileByName(stripped);
            if (byName != null) {
                String realName = realNameFor(byName, stripped);
                if (realName != null) {
                    String out = RankNickManager.applySpoof(byName, realName, full);
                    if (!out.equals(full)) {
                        return out;
                    }
                }
            }

            // NOTE: bracket-token and "Rank:" sidebar lines are intentionally
            // NOT handled here - SidebarRankSync owns them exclusively, so
            // other players' ranks are never replaced with the local one.
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static SpoofedProfile findProfileByName(String strippedLine) {
        try {
            // Local player first (zero network dependency).
            String localName = RankNickManager.localUsername();
            if (localName != null && !localName.isEmpty()
                    && containsWord(strippedLine, localName)) {
                SpoofedProfile local = RankNickManager.localSpoofFor(null, localName);
                if (local != null) {
                    return local;
                }
            }
            // Other PecuniaMC players: resolve uuid through the tab list.
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.getNetHandler() == null) {
                return null;
            }
            Collection<NetworkPlayerInfo> infos;
            try {
                infos = mc.getNetHandler().getPlayerInfoMap();
            } catch (Throwable ignored) {
                return null;
            }
            if (infos == null) {
                return null;
            }
            for (NetworkPlayerInfo info : infos) {
                String entryName = null;
                UUID id = null;
                try {
                    if (info != null && info.getGameProfile() != null) {
                        entryName = info.getGameProfile().getName();
                        id = info.getGameProfile().getId();
                    }
                } catch (Throwable ignored) {
                    continue;
                }
                if (entryName == null || entryName.isEmpty() || id == null) {
                    continue;
                }
                if (entryName.equalsIgnoreCase(localName)) {
                    continue;
                }
                if (containsWord(strippedLine, entryName)) {
                    SpoofedProfile profile = ProfileManager.getProfile(id);
                    if (profile != null && (profile.hasNick() || profile.hasRank())) {
                        return profile;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static String realNameFor(SpoofedProfile profile, String strippedLine) {
        try {
            String localName = RankNickManager.localUsername();
            if (localName != null && containsWord(strippedLine, localName)) {
                SpoofedProfile local = RankNickManager.localSpoofFor(null, localName);
                if (local != null && local.getRank().equals(profile.getRank())
                        && local.getNick().equals(profile.getNick())) {
                    return localName;
                }
            }
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.getNetHandler() == null) {
                return localName;
            }
            Collection<NetworkPlayerInfo> infos = mc.getNetHandler().getPlayerInfoMap();
            if (infos == null) {
                return localName;
            }
            for (NetworkPlayerInfo info : infos) {
                try {
                    if (info != null && info.getGameProfile() != null
                            && profile.getUuid() != null
                            && profile.getUuid().equals(info.getGameProfile().getId())
                            && containsWord(strippedLine, info.getGameProfile().getName())) {
                        return info.getGameProfile().getName();
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static boolean containsWord(String text, String word) {
        try {
            return Pattern.compile("(?<!\\w)" + Pattern.quote(word) + "(?!\\w)").matcher(text).find();
        } catch (Throwable ignored) {
            return text.contains(word);
        }
    }

    private static boolean isHypixel() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.getCurrentServerData() == null
                    || mc.getCurrentServerData().serverIP == null) {
                return false;
            }
            return mc.getCurrentServerData().serverIP.toLowerCase().contains("hypixel");
        } catch (Throwable ignored) {
            return false;
        }
    }

}
