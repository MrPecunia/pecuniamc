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
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Hypixel sidebar rank sync, mirroring ChatTriggers CustomRankUpdated.
 *
 * CT rewrites the sidebar {@code Rank: ...} line by mutating scoreboard
 * <i>data</i> ({@code removeObjectiveFromEntity} + {@code getValueFromObjective}
 * + preserved points) instead of intercepting rendering - so it shows through
 * every renderer. This does the same natively, a few times per second:
 * every entry whose stripped name looks like {@code Rank: ...} is replaced
 * with {@code Rank: <local rank>}. Self-healing (converges even if the server
 * re-sends the original line) and cleans up its own entry when the rank is
 * cleared.
 */
public class SidebarRankSync {

    private static final Pattern RANK_LINE = Pattern.compile("(?i)^\\s*Rank:\\s*.+");

    private static String lastInjected;
    private int ticks;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        try {
            if (event == null || event.phase != TickEvent.Phase.END) {
                return;
            }
            if (++ticks < 5) {
                return;
            }
            ticks = 0;
            syncOnce();
        } catch (Throwable ignored) {
        }
    }

    private static void syncOnce() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.theWorld == null) {
                return;
            }
            if (!isHypixel()) {
                lastInjected = null;
                return;
            }
            Scoreboard board = mc.theWorld.getScoreboard();
            if (board == null) {
                lastInjected = null;
                return;
            }
            ScoreObjective sidebar;
            try {
                sidebar = board.getObjectiveInDisplaySlot(1);
            } catch (Throwable ignored) {
                return;
            }
            if (sidebar == null) {
                lastInjected = null;
                return;
            }

            List<Score> matches = new ArrayList<Score>();
            List<String> matchFull = new ArrayList<String>();
            try {
                for (Score score : board.getSortedScores(sidebar)) {
                    String entry;
                    try {
                        entry = score == null ? null : score.getPlayerName();
                    } catch (Throwable ignored) {
                        continue;
                    }
                    if (entry == null) {
                        continue;
                    }
                    // Team-split entries carry the text in prefix/suffix.
                    String full = entry;
                    try {
                        net.minecraft.scoreboard.Team team = board.getPlayersTeam(entry);
                        if (team instanceof ScorePlayerTeam) {
                            ScorePlayerTeam playerTeam = (ScorePlayerTeam) team;
                            String prefix = playerTeam.getColorPrefix();
                            String suffix = playerTeam.getColorSuffix();
                            full = (prefix == null ? "" : prefix) + entry
                                    + (suffix == null ? "" : suffix);
                        }
                    } catch (Throwable ignored) {
                    }
                    String stripped = strip(full);
                    if (stripped != null && RANK_LINE.matcher(stripped).matches()) {
                        matches.add(score);
                        matchFull.add(full);
                    }
                }
            } catch (Throwable ignored) {
                return;
            }
            if (matches.isEmpty()) {
                return;
            }

            String rank = RankNickManager.getLocalRank();
            if (rank == null || rank.isEmpty()) {
                if (lastInjected != null) {
                    try {
                        board.removeObjectiveFromEntity(lastInjected, sidebar);
                    } catch (Throwable ignored) {
                    }
                    lastInjected = null;
                }
                return;
            }

            String desired = "Rank: " + sidebarRankText(rank);
            String desiredStripped = strip(desired);
            if (matches.size() == 1) {
                try {
                    if (desiredStripped != null
                            && desiredStripped.equals(strip(matchFull.get(0)))) {
                        lastInjected = matches.get(0).getPlayerName();
                        return;
                    }
                } catch (Throwable ignored) {
                }
            }
            int points = 0;
            try {
                points = matches.get(matches.size() - 1).getScorePoints();
            } catch (Throwable ignored) {
            }
            for (Score score : matches) {
                try {
                    board.removeObjectiveFromEntity(score.getPlayerName(), sidebar);
                } catch (Throwable ignored) {
                }
            }
            try {
                Score created = board.getValueFromObjective(desired, sidebar);
                created.setScorePoints(points);
                lastInjected = desired;
            } catch (Throwable ignored) {
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * Hypixel sidebar style has no brackets around the rank
     * ({@code Rank: ADMIN}, not {@code Rank: [ADMIN]}), like CT CustomRank.
     */
    private static String sidebarRankText(String rank) {
        try {
            String text = RankNickManager.translateColorCodes(rank);
            if (text == null) {
                return "";
            }
            return text.replace("[", "").replace("]", "").trim();
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static String strip(String text) {
        try {
            if (text == null) {
                return null;
            }
            return EnumChatFormatting.getTextWithoutFormattingCodes(text);
        } catch (Throwable ignored) {
            return text;
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
