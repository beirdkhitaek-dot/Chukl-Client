package com.chukl.client.mixin;

import com.chukl.client.modules.misc.FreecamModule;
import net.minecraft.client.Mouse;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ported from 67Client's MouseHandlerFreecamMixin: routes mouse look into the detached camera instead of the player. */
@Mixin(Mouse.class)
public abstract class MiscMouseFreecamMixin {
    @Shadow
    private double cursorDeltaX;
    @Shadow
    private double cursorDeltaY;

    @Inject(method = "updateMouse", at = @At("HEAD"), cancellable = true)
    private void chukl$freecamMouse(double time, CallbackInfo ci) {
        FreecamModule freecam = FreecamModule.get();
        if (freecam != null && freecam.isActive()) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.currentScreen == null) {
                double sensitivity = mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2;
                double factor = sensitivity * sensitivity * sensitivity * 8.0;
                double dx = this.cursorDeltaX * factor * freecam.getLookSensitivity();
                double dy = this.cursorDeltaY * factor * freecam.getLookSensitivity();
                float newYaw = freecam.getCurrentYaw() + (float) dx * 0.15F;
                float newPitch = freecam.getCurrentPitch() + (float) dy * 0.15F;
                freecam.setRotation(newYaw, newPitch);
                ci.cancel();
            }
        }
    }
}
