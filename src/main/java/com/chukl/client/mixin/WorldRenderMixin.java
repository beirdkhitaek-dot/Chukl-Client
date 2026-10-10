package com.chukl.client.mixin;

import com.chukl.client.core.ModuleManager;
import com.chukl.client.core.Render3D;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.state.WorldRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Gives modules a place to draw in the 3D world. Runs once per frame, in the translucent pass. */
@Mixin(WorldRenderer.class)
public class WorldRenderMixin {
    @Inject(method = "renderTargetBlockOutline", at = @At("HEAD"))
    private void chuklclient$render3d(VertexConsumerProvider.Immediate buffers, MatrixStack matrices, boolean translucentPass,
                                      WorldRenderState state, CallbackInfo ci) {
        if (!translucentPass) return;
        ModuleManager.render3D(new Render3D(buffers, matrices, state.cameraRenderState.pos));
    }
}
