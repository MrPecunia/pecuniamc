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

package dev.pecuniamc.capes;

import dev.pecuniamc.network.ProfileManager;
import dev.pecuniamc.network.SpoofedProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MinecraftCapes integration.
 *
 * The MinecraftCapes mod (forge-1.8.9 branch) is initialized natively through
 * its own {@code @Mod} entrypoint, so capes work out of the box via their API
 * when the jar is present. This class only:
 *
 * <ol>
 *   <li>detects the mod reflectively (no hard dependency),</li>
 *   <li>provides a lightweight Pecunia Network cape fallback cache
 *       (cape_url broadcasts -> downloadable ResourceLocation),</li>
 *   <li>exposes {@link #getPecuniaCape(UUID)} for a future LayerCape hook.</li>
 * </ol>
 */
public final class CapeIntegration {

    private static final Map<UUID, ResourceLocation> CAPE_CACHE = new ConcurrentHashMap<UUID, ResourceLocation>();
    private static final Map<UUID, String> CAPE_URLS = new ConcurrentHashMap<UUID, String>();
    private static final Map<UUID, Long> LAST_ATTEMPT = new ConcurrentHashMap<UUID, Long>();
    private static final java.util.Set<UUID> IN_FLIGHT =
            java.util.Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());
    private static final int MAX_BYTES = 1024 * 1024;
    private static final int MAX_DIMENSION = 512;
    private static boolean minecraftCapesPresent = false;

    private CapeIntegration() {
    }

    public static void init() {
        try {
            Class.forName("net.minecraftcapes.MinecraftCapes");
            minecraftCapesPresent = true;
            System.out.println("[PecuniaMC] MinecraftCapes detected: native capes enabled.");
        } catch (Throwable ignored) {
            System.out.println("[PecuniaMC] MinecraftCapes not present: using Pecunia cape fallback only.");
        }
    }

    public static boolean isMinecraftCapesPresent() {
        return minecraftCapesPresent;
    }

    /**
     * Returns (and lazily downloads) a Pecunia Network cape for the uuid,
     * or null when none is assigned.
     */
    public static ResourceLocation getPecuniaCape(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        try {
            SpoofedProfile profile = ProfileManager.getProfile(uuid);
            String url = profile == null ? null : profile.getCapeUrl();
            if (url == null || url.isEmpty()) {
                evict(uuid);
                return null;
            }
            ResourceLocation cached = CAPE_CACHE.get(uuid);
            if (cached != null && url.equals(CAPE_URLS.get(uuid))) {
                return cached;
            }
            if (url.equals(CAPE_URLS.get(uuid)) && IN_FLIGHT.contains(uuid)) {
                return cached;
            }
            evict(uuid);
            CAPE_URLS.put(uuid, url);
            if (!IN_FLIGHT.add(uuid)) {
                return null;
            }
            try {
                Long last = LAST_ATTEMPT.get(uuid);
                long now = System.currentTimeMillis();
                if (last != null && now - last.longValue() < 30000L) {
                    IN_FLIGHT.remove(uuid);
                    return null;
                }
                LAST_ATTEMPT.put(uuid, now);
            } catch (Throwable ignored) {
            }
            final ResourceLocation location = new ResourceLocation("pecuniamc/capes/" + uuid.toString());
            final String downloadUrl = url;
            // Async download to avoid freezing the render thread. The in-flight
            // guard above guarantees at most one download thread per player.
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        downloadCape(downloadUrl, location, uuid);
                    } catch (Throwable t) {
                        System.out.println("[PecuniaMC] Cape download failed: " + t.getMessage());
                    } finally {
                        IN_FLIGHT.remove(uuid);
                    }
                }
            }, "PecuniaMC-Cape-DL");
            thread.setDaemon(true);
            thread.start();
            return CAPE_CACHE.get(uuid);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void evict(UUID uuid) {
        try {
            CAPE_CACHE.remove(uuid);
            CAPE_URLS.remove(uuid);
        } catch (Throwable ignored) {
        }
    }

    private static void downloadCape(String url, ResourceLocation location, final UUID uuid) throws Exception {
        if (url == null) {
            return;
        }
        String lower = url.toLowerCase();
        int schemeEnd = lower.indexOf("://");
        String scheme = schemeEnd > 0 ? lower.substring(0, schemeEnd) : "";
        if (!scheme.equals("http") && !scheme.equals("https")) {
            System.out.println("[PecuniaMC] Refusing non-http cape URL.");
            return;
        }
        java.net.HttpURLConnection connection =
                (java.net.HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(10000);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", "PecuniaMC/1.0");
        int length = connection.getContentLength();
        if (length > MAX_BYTES) {
            System.out.println("[PecuniaMC] Cape too large, skipping.");
            try {
                connection.disconnect();
            } catch (Throwable ignored) {
            }
            return;
        }
        InputStream raw = connection.getInputStream();
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        try {
            byte[] chunk = new byte[8192];
            int total = 0;
            int read;
            while ((read = raw.read(chunk)) != -1) {
                total += read;
                if (total > MAX_BYTES) {
                    System.out.println("[PecuniaMC] Cape too large, skipping.");
                    return;
                }
                buffer.write(chunk, 0, read);
            }
        } finally {
            try {
                raw.close();
            } catch (Throwable ignored) {
            }
            try {
                connection.disconnect();
            } catch (Throwable ignored) {
            }
        }
        final BufferedImage image = ImageIO.read(new java.io.ByteArrayInputStream(buffer.toByteArray()));
        if (image == null) {
            return;
        }
        if (image.getWidth() <= 0 || image.getHeight() <= 0
                || image.getWidth() > MAX_DIMENSION || image.getHeight() > MAX_DIMENSION) {
            System.out.println("[PecuniaMC] Cape has invalid dimensions, skipping.");
            return;
        }
        final String boundUrl = url;
        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                try {
                    // Drop stale results: the profile may point elsewhere by now.
                    if (!boundUrl.equals(CAPE_URLS.get(uuid))) {
                        return;
                    }
                    // Version-safe bind: DynamicTexture(BufferedImage) exists on 1.8.9.
                    DynamicTexture texture = new DynamicTexture(image);
                    Minecraft.getMinecraft().getTextureManager().loadTexture(location, texture);
                    CAPE_CACHE.put(uuid, location);
                } catch (Throwable t) {
                    System.out.println("[PecuniaMC] Cape bind failed: " + t.getMessage());
                }
            }
        });
    }

    /** Convenience: does this player have any cape (native or Pecunia)? Pure cache lookup. */
    public static boolean hasCape(AbstractClientPlayer player) {
        if (player == null) {
            return false;
        }
        try {
            if (player.getLocationCape() != null) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        try {
            return CAPE_CACHE.containsKey(player.getUniqueID());
        } catch (Throwable ignored) {
            return false;
        }
    }
}
