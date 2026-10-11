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

import dev.pecuniamc.network.NametagSpoofHelper;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Nametag visual override (path C of the spec), base Render side.
 *
 * Covers the {@code Render.renderName(Entity, double, double, double)} shape
 * used by 1.8.9 renderers that do not override it in RendererLivingEntity.
 * Twin of {@link MixinRendererLivingEntityNametag}; both are
 * {@code require = 0} so exactly the existing one applies.
 *
 * Note: the superclass is intentionally {@code Object}, not {@code Render}:
 * a Mixin targeting a class must not extend that same class, otherwise the
 * obfuscated production runtime fails hierarchy validation. Protected members
 * are accessed via {@code @Shadow} (resolved through the refmap) and the
 * public {@code livingPlayer} field.
 */
@Mixin(Render.class)
public abstract class MixinRenderNametag {

    @Shadow protected RenderManager renderManager;

    @Shadow
    protected abstract void renderLivingLabel(Entity entityIn, String str,
                                              double x, double y, double z, int maxDistance);

    @Inject(method = "renderName(Lnet/minecraft/entity/Entity;DDD)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    public void pecuniaRenderNameBase(Entity entity, double x, double y, double z, CallbackInfo ci) {
        try {
            String spoofed = NametagSpoofHelper.getSpoofedName(entity);
            if (spoofed == null) {
                return;
            }
            if (renderManager != null && renderManager.livingPlayer != null) {
                double dist = entity.getDistanceSqToEntity(renderManager.livingPlayer);
                if (dist > 4096.0D) {
                    ci.cancel();
                    return;
                }
            }
            renderLivingLabel(entity, spoofed, x, y, z, 64);
            ci.cancel();
        } catch (Throwable ignored) {
        }
    }
}
