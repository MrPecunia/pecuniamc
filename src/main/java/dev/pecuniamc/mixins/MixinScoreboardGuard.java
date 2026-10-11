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

package dev.pecuniamc.mixins;

import net.minecraft.client.Minecraft;
import net.minecraft.network.play.server.S3BPacketScoreboardObjective;
import net.minecraft.scoreboard.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Guards vanilla scoreboard-objective packets.
 *
 * On BungeeCord server switches Hypixel sends objective remove/update packets
 * for objectives the client never created; vanilla 1.8.9 then NPEs in
 * {@code Scoreboard.removeObjective} and the game crashes. Stale packets for
 * unknown objectives are safely ignored.
 */
@Mixin(net.minecraft.client.network.NetHandlerPlayClient.class)
public abstract class MixinScoreboardGuard {

    @Inject(method = {"handleScoreboardObjective", "func_147291_a"},
            at = @At("HEAD"), cancellable = true)
    public void pecuniaGuardScoreboardObjective(S3BPacketScoreboardObjective packetIn,
                                                CallbackInfo ci) {
        try {
            if (packetIn == null) {
                ci.cancel();
                return;
            }
            int action;
            try {
                action = packetIn.func_149338_e();
            } catch (Throwable ignored) {
                return;
            }
            if (action == 0) {
                return;
            }
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.theWorld == null) {
                return;
            }
            Scoreboard board;
            try {
                board = mc.theWorld.getScoreboard();
            } catch (Throwable ignored) {
                return;
            }
            if (board == null) {
                return;
            }
            String name;
            try {
                name = packetIn.func_149339_c();
            } catch (Throwable ignored) {
                return;
            }
            boolean missing;
            try {
                missing = name == null || board.getObjective(name) == null;
            } catch (Throwable ignored) {
                return;
            }
            if (missing) {
                ci.cancel();
            }
        } catch (Throwable ignored) {
        }
    }
}
