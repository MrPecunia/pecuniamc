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

import java.util.UUID;

/**
 * A spoofed rank/nick/cape profile broadcast by the Pecunia Network.
 * Ported from RankSpoof / CustomRankUpdated concepts to Forge 1.8.9 strings.
 */
public class SpoofedProfile {

    private final UUID uuid;
    private volatile String rank;
    private volatile String nick;
    private volatile String capeUrl;

    public SpoofedProfile(UUID uuid, String rank, String nick) {
        this.uuid = uuid;
        this.rank = rank == null ? "" : rank;
        this.nick = nick == null ? "" : nick;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getRank() {
        return rank == null ? "" : rank;
    }

    public void setRank(String rank) {
        this.rank = rank == null ? "" : rank;
    }

    public String getNick() {
        return nick == null ? "" : nick;
    }

    public void setNick(String nick) {
        this.nick = nick == null ? "" : nick;
    }

    public String getCapeUrl() {
        return capeUrl == null ? "" : capeUrl;
    }

    public void setCapeUrl(String capeUrl) {
        this.capeUrl = capeUrl == null ? "" : capeUrl;
    }

    public boolean hasCape() {
        return capeUrl != null && !capeUrl.isEmpty();
    }

    public boolean hasNick() {
        return nick != null && !nick.isEmpty();
    }

    public boolean hasRank() {
        return rank != null && !rank.isEmpty();
    }
}
