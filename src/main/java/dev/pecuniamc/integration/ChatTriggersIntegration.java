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

package dev.pecuniamc.integration;

/**
 * ChatTriggers integration shim.
 *
 * ChatTriggers (https://github.com/ChatTriggers/ChatTriggers) is a separate
 * Forge mod; on 1.8.9 it is loaded as a normal coremod jar. PecuniaMC does not
 * embed it - this hook only detects its presence and logs, so scripts keep
 * working out of the box without any coupling.
 */
public final class ChatTriggersIntegration {

    private static boolean present = false;

    private ChatTriggersIntegration() {
    }

    public static void init() {
        try {
            Class.forName("com.chattriggers.ctjs.CTJS");
            present = true;
            System.out.println("[PecuniaMC] ChatTriggers detected: scripts enabled.");
            return;
        } catch (Throwable ignored) {
        }
        try {
            Class.forName("com.chattriggers.ctjs.Reference");
            present = true;
            System.out.println("[PecuniaMC] ChatTriggers detected: scripts enabled.");
            return;
        } catch (Throwable ignored) {
        }
        try {
            net.minecraftforge.fml.common.Loader.isModLoaded("chattriggers");
            // Loader check only; reflectively confirm via ModContainer list.
            for (net.minecraftforge.fml.common.ModContainer mod
                    : net.minecraftforge.fml.common.Loader.instance().getModList()) {
                if (mod != null && mod.getModId() != null
                        && mod.getModId().toLowerCase().contains("chattrigger")) {
                    present = true;
                    System.out.println("[PecuniaMC] ChatTriggers detected: scripts enabled.");
                    return;
                }
            }
        } catch (Throwable ignored) {
        }
        System.out.println("[PecuniaMC] ChatTriggers not present: skipping hooks.");
    }

    public static boolean isPresent() {
        return present;
    }
}
