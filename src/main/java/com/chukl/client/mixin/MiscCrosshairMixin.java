package com.chukl.client.mixin;

import com.chukl.client.core.ModuleManager;
import com.chukl.client.modules.misc.CustomCrosshairModule;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ported from 67Client's GuiCrosshairMixin (vanilla-hide half only; drawing itself happens in the module's HUD hook). */
@Mixin(InGameHud.class)
public class MiscCrosshairMixin {
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void chukl$hideVanillaCrosshair(DrawContext ctx, RenderTickCounter tickCounter, CallbackInfo ci) {
        CustomCrosshairModule module = ModuleManager.get(CustomCrosshairModule.class);
        if (module != null && module.shouldHideVanilla()) {
            ci.cancel();
        }
    }
}
