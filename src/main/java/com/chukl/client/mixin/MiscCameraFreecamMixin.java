package com.chukl.client.mixin;

import com.chukl.client.modules.misc.FreecamModule;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ported from 67Client's CameraFreecamMixin: after the vanilla camera update, snap to the detached camera. */
@Mixin(Camera.class)
public abstract class MiscCameraFreecamMixin {
    @Shadow
    protected abstract void setPos(Vec3d pos);

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "update", at = @At("TAIL"))
    private void chukl$freecam(World area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickProgress, CallbackInfo ci) {
        FreecamModule freecam = FreecamModule.get();
        if (freecam != null && freecam.isActive()) {
            this.setRotation(freecam.getInterpolatedYaw(tickProgress), freecam.getInterpolatedPitch(tickProgress));
            this.setPos(freecam.getInterpolatedPos(tickProgress));
        }
    }
}
