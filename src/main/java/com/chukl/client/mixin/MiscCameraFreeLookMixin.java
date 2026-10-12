package com.chukl.client.mixin;

import com.chukl.client.modules.misc.FreeLookModule;
import com.chukl.client.modules.misc.FreecamModule;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/** Ported from 67Client's CameraFreeLookMixin: substitutes Free Look's own yaw/pitch into the camera update. */
@Mixin(Camera.class)
public abstract class MiscCameraFreeLookMixin {
    @ModifyArgs(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V"))
    private void chukl$freeLookRotation(Args args) {
        FreeLookModule freeLook = FreeLookModule.get();
        if (freeLook != null && freeLook.isActive()) {
            FreecamModule freecam = FreecamModule.get();
            if (freecam == null || !freecam.isActive()) {
                args.set(0, freeLook.getCameraYaw());
                args.set(1, freeLook.getCameraPitch());
            }
        }
    }

    @Inject(method = "clipToSpace", at = @At("HEAD"), cancellable = true)
    private void chukl$freeLookThroughWalls(float distance, CallbackInfoReturnable<Float> cir) {
        FreeLookModule freeLook = FreeLookModule.get();
        if (freeLook != null && freeLook.seeThroughWalls()) {
            FreecamModule freecam = FreecamModule.get();
            if (freecam == null || !freecam.isActive()) {
                cir.setReturnValue(distance);
            }
        }
    }
}
