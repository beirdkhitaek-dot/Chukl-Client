package com.chukl.client.mixin;

import com.chukl.client.modules.misc.FreecamModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ported from 67Client's LocalPlayerFreecamMixin: re-applies latched movement right before it's used or sent. */
@Mixin(ClientPlayerEntity.class)
public abstract class MiscLocalPlayerFreecamMixin {
    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void chukl$freecamReapplyBeforeAiStep(CallbackInfo ci) {
        chukl$reapply();
    }

    @Inject(method = "sendMovementPackets", at = @At("HEAD"))
    private void chukl$freecamReapplyBeforeSendPosition(CallbackInfo ci) {
        chukl$reapply();
    }

    private void chukl$reapply() {
        ClientPlayerEntity self = (ClientPlayerEntity) (Object) this;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == self) {
            FreecamModule f = FreecamModule.get();
            if (f != null && f.isActive()) {
                FreecamModule.reapplyBodyInput(mc);
            }
        }
    }
}
