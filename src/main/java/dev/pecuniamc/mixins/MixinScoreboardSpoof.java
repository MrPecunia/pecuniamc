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

import dev.pecuniamc.network.ScoreboardSpoofHelper;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Team;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sidebar/tablist team-name override (Hypixel sidebar rank lines included).
 *
 * Trampoline only: all logic lives in {@link ScoreboardSpoofHelper} so the
 * merged mixin body stays trivially verifiable in production.
 */
@Mixin(ScorePlayerTeam.class)
public abstract class MixinScoreboardSpoof {

    @Inject(method = {"formatPlayerName", "func_96667_a"}, at = @At("HEAD"), cancellable = true)
    private static void pecuniaFormatPlayerName(Team team, String name,
                                                CallbackInfoReturnable<String> cir) {
        try {
            String out = ScoreboardSpoofHelper.rewrite(team, name);
            if (out != null) {
                cir.setReturnValue(out);
            }
        } catch (Throwable ignored) {
        }
    }
}
