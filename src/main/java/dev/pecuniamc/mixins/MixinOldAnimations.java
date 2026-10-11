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

import dev.pecuniamc.PecuniaCore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * "1.7 Animations" module, OptiFine-safe.
 *
 * Unlike legacy clients that {@code @Overwrite} ItemRenderer (which crashes
 * when OptiFine rewrites the same rendering pipeline), every hook here is an
 * {@code @Inject} with defensive try/catch + runtime OptiFine detection, so
 * the game falls back to vanilla rendering instead of crashing.
 *
 * Targets {@link ItemRenderer} and works together with
 * {@link MixinEntityPlayerSPAnimations} (swing-while-blocking).
 */
@Mixin(ItemRenderer.class)
public abstract class MixinOldAnimations {

    @Shadow @Final private Minecraft mc;

    private boolean pecuniaBlockAnimEnabled() {
        // Pecunia fills in only where Frost's own pose is off: Frost's
        // ItemRendererMixin applies its 1.7 pose whenever its "Block Animation"
        // setting is on, so applying ours too would stack both rotations.
        try {
            if (dev.frostclient.Frost.INSTANCE != null
                    && dev.frostclient.Frost.INSTANCE.settingManager != null
                    && dev.frostclient.Frost.INSTANCE.settingManager
                            .getSettingByModAndName("Animation", "Block Animation").isCheckToggled()) {
                return false;
            }
        } catch (Throwable ignored) {
        }
        return true;
    }

    private boolean pecuniaEatAnimEnabled() {
        try {
            if (dev.frostclient.Frost.INSTANCE != null
                    && dev.frostclient.Frost.INSTANCE.settingManager != null
                    && dev.frostclient.Frost.INSTANCE.settingManager
                            .getSettingByModAndName("Animation", "Eat/Drink Animation").isCheckToggled()) {
                return false;
            }
        } catch (Throwable ignored) {
        }
        return true;
    }

    /**
     * 1.7 third-person sword block pose. Runs after vanilla pushes the sword
     * matrix; extra transforms only, so OptiFine's pipeline is preserved.
     */
    @Inject(method = {"renderItem", "func_178099_a"},
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/RenderItem;renderItemModelForEntity(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/client/renderer/block/model/ItemCameraTransforms$TransformType;)V"))
    public void pecuniaThirdPersonBlock(net.minecraft.entity.EntityLivingBase entity,
                                        ItemStack item,
                                        net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType transformType,
                                        CallbackInfo ci) {
        try {
            if (item == null || !(item.getItem() instanceof ItemSword)) {
                return;
            }
            if (!(entity instanceof net.minecraft.entity.player.EntityPlayer)) {
                return;
            }
            if (((net.minecraft.entity.player.EntityPlayer) entity).getItemInUseCount() <= 0) {
                return;
            }
            if (item.getItemUseAction() != EnumAction.BLOCK) {
                return;
            }
            if (transformType != net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType.THIRD_PERSON) {
                return;
            }
            if (!pecuniaBlockAnimEnabled()) {
                return;
            }
            GlStateManager.rotate(-45.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(-20.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(-60.0F, 0.0F, 0.0F, 1.0F);
            GlStateManager.translate(-0.04F, -0.04F, 0.0F);
        } catch (Throwable ignored) {
            // Never break rendering because of an animation tweak.
        }
    }

    /**
     * 1.7 first-person block-hitting pose. Injected at the HEAD of
     * {@code doBlockTransformations} and cancelled only when our 1.7 pose
     * applies; when OptiFine is present we apply a reduced transform that does
     * not fight its shaders/arm pipeline.
     */
    @Inject(method = {"doBlockTransformations", "func_178103_d"}, at = @At("HEAD"), cancellable = true)
    public void pecuniaBlockTransformations(CallbackInfo ci) {
        try {
            if (!pecuniaBlockAnimEnabled()) {
                return;
            }
            if (PecuniaCore.isOptiFineLoaded()) {
                // Reduced 1.7-style pose: keeps OptiFine's lighting intact.
                GlStateManager.translate(-0.20F, 0.15F, 0.0F);
                GlStateManager.rotate(28.0F, 0.0F, 1.0F, 0.0F);
                GlStateManager.rotate(-70.0F, 1.0F, 0.0F, 0.0F);
                GlStateManager.rotate(50.0F, 0.0F, 1.0F, 0.0F);
                ci.cancel();
                return;
            }
            GlStateManager.translate(-0.24F, 0.17F, 0.0F);
            GlStateManager.rotate(30.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(-80.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(60.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.translate(0.0F, 0.18F, 0.0F);
            ci.cancel();
        } catch (Throwable ignored) {
        }
    }

    /**
     * 1.7 eating/drinking: vanilla 1.8 holds the item still while eating.
     * Apply a small swing-based bob via an extra transform after vanilla runs.
     * Purely additive - no overwrite, no OptiFine conflict.
     */
    @Inject(method = {"performDrinking", "func_178104_a"}, at = @At("RETURN"))
    public void pecuniaDrinkSwing(AbstractClientPlayer clientPlayer, float partialTicks, CallbackInfo ci) {
        try {
            if (mc == null || mc.thePlayer == null) {
                return;
            }
            EntityPlayerSP player = mc.thePlayer;
            if (player.getItemInUseCount() <= 0) {
                return;
            }
            ItemStack held = player.getHeldItem();
            if (held == null) {
                return;
            }
            EnumAction action = held.getItemUseAction();
            if (action != EnumAction.EAT && action != EnumAction.DRINK) {
                return;
            }
            if (!pecuniaEatAnimEnabled()) {
                return;
            }
            float swing = player.getSwingProgress(partialTicks);
            GlStateManager.translate(0.0F, swing * 0.08F, 0.0F);
            GlStateManager.rotate(swing * 12.0F, 0.0F, 1.0F, 0.0F);
        } catch (Throwable ignored) {
        }
    }
}
