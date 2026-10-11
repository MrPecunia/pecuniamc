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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Local cache of {@link SpoofedProfile} entries received from the
 * Pecunia Network websocket broadcasts.
 */
public final class ProfileManager {

    private static final Map<UUID, SpoofedProfile> PROFILES = new ConcurrentHashMap<UUID, SpoofedProfile>();

    private ProfileManager() {
    }

    public static void init() {
        PROFILES.clear();
    }

    public static void updateProfile(UUID uuid, String rank, String nick) {
        if (uuid == null) {
            return;
        }
        SpoofedProfile existing = PROFILES.get(uuid);
        if (existing == null) {
            PROFILES.put(uuid, new SpoofedProfile(uuid, rank, nick));
        } else {
            if (rank != null) {
                existing.setRank(rank);
            }
            if (nick != null) {
                existing.setNick(nick);
            }
        }
    }

    public static void updateCape(UUID uuid, String capeUrl) {
        if (uuid == null) {
            return;
        }
        SpoofedProfile existing = PROFILES.get(uuid);
        if (existing == null) {
            existing = new SpoofedProfile(uuid, "", "");
            PROFILES.put(uuid, existing);
        }
        existing.setCapeUrl(capeUrl);
    }

    public static void removeProfile(UUID uuid) {
        if (uuid != null) {
            PROFILES.remove(uuid);
        }
    }

    public static SpoofedProfile getProfile(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        return PROFILES.get(uuid);
    }

    public static Map<UUID, SpoofedProfile> snapshot() {
        return new ConcurrentHashMap<UUID, SpoofedProfile>(PROFILES);
    }
}
