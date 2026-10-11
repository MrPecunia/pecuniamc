/*
 * Modifications copyright (C) 2026 PrPecunia, Webget, Diamond Gaming, Eris Group, Pecunia Enterprise (GPL-3.0, see LICENSE).
 * Original code: Arsenic Client (MIT, see licenses/MIT-Arsenic.txt).
 */

package arsenic.injection.mixin;

import net.minecraft.client.gui.GuiChat;
import org.spongepowered.asm.mixin.Mixin;

/**
 * PecuniaMC replacement for Arsenic's chat mixin.
 *
 * The original renders the "." command suggestion list (module commands such
 * as .antiafk) inside the chat screen. PecuniaMC must show no Arsenic traces,
 * so this mixin intentionally hooks nothing: vanilla chat behavior is fully
 * restored. Command routing itself lives in MixinGuiScreen/GuiHooks and is
 * untouched. Kept as a class (rather than deleting the config entry) so
 * {@code mixins.arsenic.json} stays valid.
 */
@Mixin(GuiChat.class)
public abstract class MixinGuiChat {
}
