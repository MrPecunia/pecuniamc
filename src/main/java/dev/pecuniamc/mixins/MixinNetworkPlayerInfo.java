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

import com.mojang.authlib.GameProfile;
import dev.pecuniamc.network.ProfileManager;
import dev.pecuniamc.network.RankNickManager;
import dev.pecuniamc.network.SpoofedProfile;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

/**
 * Tablist visual override (path B of the spec): replaces rank + name based on
 * {@link SpoofedProfile} by overriding {@code getDisplayName()}.
 */
@Mixin(NetworkPlayerInfo.class)
public abstract class MixinNetworkPlayerInfo {

    @Shadow public abstract GameProfile getGameProfile();

    @Inject(method = {"getDisplayName", "func_178854_k"}, at = @At("HEAD"), cancellable = true)
    public void pecuniaGetDisplayName(CallbackInfoReturnable<IChatComponent> cir) {
        try {
            GameProfile profile = getGameProfile();
            if (profile == null) {
                return;
            }
            UUID uuid = profile.getId();
            String realName = profile.getName();
            SpoofedProfile spoofed = uuid == null ? null : ProfileManager.getProfile(uuid);
            if (spoofed == null) {
                spoofed = RankNickManager.localSpoofFor(uuid, realName);
            }
            if (spoofed == null || (!spoofed.hasNick() && !spoofed.hasRank())) {
                return;
            }
            String display = RankNickManager.applySpoof(spoofed, realName, realName);
            cir.setReturnValue(new ChatComponentText(display));
        } catch (Throwable ignored) {
        }
    }
}
