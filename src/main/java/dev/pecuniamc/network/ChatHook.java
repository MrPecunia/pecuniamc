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
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.UUID;

/**
 * Chat visual override (path A of the spec).
 *
 * Rewrites incoming chat lines so spoofed rank/nick from the Pecunia Network
 * replace the player's rank and name. Operates on legacy 1.8.9 formatting
 * strings, mirroring RankSpoof's intent without the modern Component API.
 */
public class ChatHook {

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        try {
            if (event == null || event.message == null) {
                return;
            }
            IChatComponent original = event.message;
            String unformatted = EnumChatFormatting.getTextWithoutFormattingCodes(original.getUnformattedText());
            if (unformatted == null || unformatted.isEmpty()) {
                return;
            }

            boolean changed = false;
            String formatted = original.getFormattedText();
            String strippedFormatted;
            try {
                strippedFormatted = RankNickManager.stripFormatting(formatted);
            } catch (Throwable ignored) {
                strippedFormatted = "";
            }
            if (strippedFormatted == null) {
                strippedFormatted = "";
            }

            try {
                String localName = RankNickManager.localUsername();
                if (localName != null && !localName.isEmpty()
                        && strippedFormatted.contains(localName)) {
                    SpoofedProfile local = RankNickManager.localSpoofFor(null, localName);
                    if (local != null) {
                        String spoofed = RankNickManager.applySpoof(local, localName, formatted);
                        if (!spoofed.equals(formatted)) {
                            formatted = spoofed;
                            changed = true;
                            strippedFormatted = RankNickManager.stripFormatting(formatted);
                            if (strippedFormatted == null) {
                                strippedFormatted = "";
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {
            }

            java.util.Map<UUID, String> tabNames = buildTabNameMap();
            for (SpoofedProfile profile : ProfileManager.snapshot().values()) {
                String realName = profile == null ? null : resolveRealName(profile.getUuid(), tabNames);
                if (realName == null || realName.isEmpty()) {
                    continue;
                }
                if (!strippedFormatted.contains(realName) && !unformatted.contains(realName)) {
                    continue;
                }
                String spoofed = RankNickManager.applySpoof(profile, realName, formatted);
                if (!spoofed.equals(formatted)) {
                    formatted = spoofed;
                    changed = true;
                    try {
                        strippedFormatted = RankNickManager.stripFormatting(formatted);
                    } catch (Throwable ignored) {
                    }
                    if (strippedFormatted == null) {
                        strippedFormatted = "";
                    }
                }
            }

            if (changed) {
                ChatComponentText replacement = new ChatComponentText(formatted);
                try {
                    if (original.getChatStyle() != null) {
                        replacement.setChatStyle(original.getChatStyle());
                    }
                } catch (Throwable ignored) {
                }
                event.message = replacement;
            }
        } catch (Throwable t) {
            System.out.println("[PecuniaMC] ChatHook failed: " + t.getMessage());
        }
    }

    private String resolveRealName(UUID uuid, java.util.Map<UUID, String> tabNames) {
        try {
            if (uuid != null && tabNames != null) {
                String cached = tabNames.get(uuid);
                if (cached != null && !cached.isEmpty()) {
                    return cached;
                }
            }
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.getNetHandler() == null) {
                return null;
            }
            for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
                try {
                    if (info != null && info.getGameProfile() != null
                            && uuid != null && uuid.equals(info.getGameProfile().getId())) {
                        return info.getGameProfile().getName();
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private java.util.Map<UUID, String> buildTabNameMap() {
        java.util.Map<UUID, String> map = new java.util.HashMap<UUID, String>();
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.getNetHandler() == null) {
                return map;
            }
            for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
                try {
                    if (info != null && info.getGameProfile() != null
                            && info.getGameProfile().getId() != null
                            && info.getGameProfile().getName() != null) {
                        map.put(info.getGameProfile().getId(), info.getGameProfile().getName());
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
        return map;
    }
}
