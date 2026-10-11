/*
 * Modifications copyright (C) 2026 PrPecunia, Webget, Diamond Gaming, Eris Group, Pecunia Enterprise (GPL-3.0, see LICENSE).
 * Original code: Arsenic Client (MIT, see licenses/MIT-Arsenic.txt).
 */

package arsenic.injection.mixin;

import net.minecraftforge.fml.client.SplashProgress;
import org.spongepowered.asm.mixin.Mixin;

/**
 * PecuniaMC replacement for Arsenic's splash hijack.
 *
 * The original unconditionally took over Forge's loading splash with the
 * Arsenic-branded thread ({@code ArsenicSplash}). PecuniaMC must boot showing
 * only Minecraft + PecuniaMC, so this mixin intentionally hooks nothing: the
 * vanilla Forge splash runs, with PecuniaMC's own overlay on top.
 * Kept as a class (rather than deleting the config entry) so
 * {@code mixins.arsenic.json} stays valid.
 */
@Mixin(value = SplashProgress.class, remap = false)
public abstract class MixinSplashProgress {
}
