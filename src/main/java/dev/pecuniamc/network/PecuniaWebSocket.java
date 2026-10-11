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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.pecuniamc.PecuniaCore;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.UUID;

/**
 * Java-WebSocket client for the Pecunia Network.
 *
 * Protocol:
 *  - outbound: {"type":"update_profile","uuid":"...","rank":"[Sus] ","nick":"Notch"}
 *  - inbound:  broadcasts with the same shape (plus optional cape_url),
 *               applied to {@link ProfileManager}.
 */
public class PecuniaWebSocket extends WebSocketClient {

    private static final JsonParser PARSER = new JsonParser();

    public PecuniaWebSocket(String serverUrl) {
        super(URI.create(serverUrl));
        // Reconnect-friendly defaults; never throw from constructor.
        setConnectionLostTimeout(60);
    }

    /** Fire-and-forget reconnect that never throws. */
    public void connectAsync() {
        try {
            connect();
        } catch (Throwable t) {
            System.out.println("[PecuniaMC] WebSocket connect failed: " + t.getMessage());
        }
    }

    /**
     * Sends {"type":"update_profile","uuid":...,"rank":...,"nick":...}
     * when the local rank/nick changes.
     */
    public void sendProfileUpdate(UUID uuid, String rank, String nick) {
        if (uuid == null) {
            return;
        }
        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("type", "update_profile");
            payload.addProperty("uuid", uuid.toString());
            payload.addProperty("rank", rank == null ? "" : rank);
            payload.addProperty("nick", nick == null ? "" : nick);
            String json = payload.toString();
            if (isOpen()) {
                send(json);
            } else {
                // Queue a reconnect; the next broadcast will resync.
                System.out.println("[PecuniaMC] Socket not open, queuing profile update, reconnecting...");
                connectAsync();
            }
        } catch (Throwable t) {
            System.out.println("[PecuniaMC] Failed to send profile update: " + t.getMessage());
        }
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        System.out.println("[PecuniaMC] Connected to Pecunia Network.");
        // Push local spoof immediately so other clients see us.
        try {
            RankNickManager.pushLocalProfile();
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void onMessage(String message) {
        try {
            JsonObject obj = PARSER.parse(message).getAsJsonObject();
            String type = optString(obj, "type");
            if (type == null) {
                type = "";
            }
            if ("update_profile".equals(type) || "profile".equals(type) || "broadcast".equals(type)) {
                handleProfileMessage(obj);
            } else if ("cape".equals(type) || obj.has("cape_url")) {
                handleCapeMessage(obj);
            }
        } catch (Throwable t) {
            System.out.println("[PecuniaMC] Bad websocket payload: " + t.getMessage());
        }
    }

    private String optString(JsonObject obj, String key) {
        try {
            if (obj == null || !obj.has(key)) {
                return null;
            }
            com.google.gson.JsonElement element = obj.get(key);
            if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
                return null;
            }
            return element.getAsString();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private void handleProfileMessage(JsonObject obj) {
        try {
            String uuidRaw = optString(obj, "uuid");
            if (uuidRaw == null) {
                return;
            }
            UUID uuid = RankNickManager.parseUuid(uuidRaw);
            if (uuid == null) {
                return;
            }
            // Absent keys stay null so stored values are preserved (a cape-only
            // broadcast must not wipe rank/nick).
            ProfileManager.updateProfile(uuid, optString(obj, "rank"), optString(obj, "nick"));
            String capeUrl = optString(obj, "cape_url");
            if (capeUrl != null) {
                ProfileManager.updateCape(uuid, capeUrl);
            }
        } catch (Throwable t) {
            System.out.println("[PecuniaMC] Bad profile payload: " + t.getMessage());
        }
    }

    private void handleCapeMessage(JsonObject obj) {
        try {
            String uuidRaw = optString(obj, "uuid");
            if (uuidRaw == null) {
                return;
            }
            UUID uuid = RankNickManager.parseUuid(uuidRaw);
            if (uuid == null) {
                return;
            }
            String capeUrl = optString(obj, "cape_url");
            if (capeUrl == null) {
                capeUrl = optString(obj, "cape");
            }
            ProfileManager.updateCape(uuid, capeUrl == null ? "" : capeUrl);
        } catch (Throwable t) {
            System.out.println("[PecuniaMC] Bad cape payload: " + t.getMessage());
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("[PecuniaMC] Disconnected from Pecunia Network (" + code + "): " + reason);
    }

    @Override
    public void onError(Exception ex) {
        System.out.println("[PecuniaMC] WebSocket error: " + ex.getMessage());
    }

    /** Helper for mixins/managers without touching PecuniaCore internals. */
    public static PecuniaWebSocket current() {
        return PecuniaCore.getSocket();
    }
}
