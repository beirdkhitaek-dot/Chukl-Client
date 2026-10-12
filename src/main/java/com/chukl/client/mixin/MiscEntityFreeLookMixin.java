package com.chukl.client.mixin;

import com.chukl.client.modules.misc.FreeLookModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ported from 67Client's EntityFreeLookMixin: in Camera mode, mouse look drives the camera, not the player body. */
@Mixin(Entity.class)
public class MiscEntityFreeLookMixin {
    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void chukl$freeLookTurn(double dx, double dy, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if ((Object) this == mc.player) {
            FreeLookModule freeLook = FreeLookModule.get();
            if (freeLook != null && freeLook.cameraMode()) {
                freeLook.addCameraLook(dx, dy);
                ci.cancel();
            }
        }
    }
}
