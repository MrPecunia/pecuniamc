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

import dev.pecuniamc.network.ProfileManager;
import dev.pecuniamc.network.RankNickManager;
import dev.pecuniamc.network.SpoofedProfile;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

import java.util.UUID;

/**
 * Shared nametag-spoof logic used by both nametag mixins.
 * Returns the spoofed display string, or null when no spoof applies.
 */
public final class NametagSpoofHelper {

    private NametagSpoofHelper() {
    }

    public static String getSpoofedName(Entity entity) {
        try {
            if (!(entity instanceof EntityPlayer)) {
                return null;
            }
            EntityPlayer player = (EntityPlayer) entity;
            UUID uuid;
            try {
                uuid = player.getUniqueID();
            } catch (Throwable ignored) {
                return null;
            }
            SpoofedProfile spoofed = ProfileManager.getProfile(uuid);
            if (spoofed == null) {
                spoofed = RankNickManager.localSpoofFor(uuid, player.getName());
            }
            if (spoofed == null || (!spoofed.hasNick() && !spoofed.hasRank())) {
                return null;
            }
            String realName = player.getName();
            String out = RankNickManager.applySpoof(spoofed, realName, realName);
            if (out == null || out.equals(realName)) {
                return null;
            }
            return out;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
