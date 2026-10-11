package dev.frostclient.mixins;

import dev.pecuniamc.chat.ChatCommands;
import net.minecraft.client.entity.EntityPlayerSP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Authoritative outgoing-chat intercept: every chat message funnels through
 * {@code EntityPlayerSP.sendChatMessage} right before packet dispatch, so the
 * {@code /dangermode} command is caught here no matter which GUI path sent it.
 */
@Mixin(EntityPlayerSP.class)
public abstract class DangerChatMixin {

    @Inject(method = {"sendChatMessage", "func_71165_d"}, at = @At("HEAD"), cancellable = true)
    public void pecuniaDangerChat(String message, CallbackInfo ci) {
        try {
            if (ChatCommands.handleOutgoing(message)) {
                ci.cancel();
            }
        } catch (Throwable ignored) {
        }
    }
}
