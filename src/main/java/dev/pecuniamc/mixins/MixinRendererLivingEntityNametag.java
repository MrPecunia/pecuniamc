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
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Nametag visual override (path C of the spec), RendererLivingEntity side.
 *
 * Covers the {@code renderName(EntityLivingBase, double, double, double)}
 * shape. A twin mixin ({@link MixinRenderNametag}) covers the
 * {@code Render.renderName(Entity, ...)} shape; both are {@code require = 0}
 * so exactly the existing one applies on any 1.8.9 mapping.
 */
@Mixin(RendererLivingEntity.class)
public abstract class MixinRendererLivingEntityNametag<T extends EntityLivingBase> extends Render<T> {

    protected MixinRendererLivingEntityNametag(RenderManager renderManager) {
        super(renderManager);
    }

    @Inject(method = "renderName(Lnet/minecraft/entity/EntityLivingBase;DDD)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    public void pecuniaRenderNameLiving(T entity, double x, double y, double z, CallbackInfo ci) {
        try {
            String spoofed = NametagSpoofHelper.getSpoofedName(entity);
            if (spoofed == null) {
                return;
            }
            double dist = entity.getDistanceSqToEntity(renderManager.livingPlayer);
            if (dist > 4096.0D) {
                ci.cancel();
                return;
            }
            renderLivingLabel(entity, spoofed, x, y, z, 64);
            ci.cancel();
        } catch (Throwable ignored) {
        }
    }
}
