package com.chukl.client.mixin;

import com.chukl.client.core.ModuleManager;
import com.chukl.client.modules.misc.CustomFovModule;
import com.chukl.client.modules.misc.ZoomModule;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Applies Zoom and CustomFOV on top of the vanilla FOV. 67Client's own GameRendererMixin reaches into
 * getFov's local sprint-multiplier variable via MixinExtras (@ModifyExpressionValue), which Chukl doesn't
 * depend on; this applies both modules' effect to the method's final return value instead, which is a
 * simpler (but equally functional) integration point.
 */
@Mixin(GameRenderer.class)
public abstract class MiscFovMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void chukl$applyFov(Camera camera, float tickProgress, boolean changingFov, CallbackInfoReturnable<Float> cir) {
        float fov = cir.getReturnValue();

        ZoomModule zoom = ModuleManager.get(ZoomModule.class);
        if (zoom != null) {
            double factor = zoom.currentFactor();
            if (factor > 1.0001) fov = (float) (fov / factor);
        }

        CustomFovModule customFov = ModuleManager.get(CustomFovModule.class);
        if (customFov != null) {
            fov *= customFov.fovMultiplier(1.0F);
        }

        cir.setReturnValue(fov);
    }
}
