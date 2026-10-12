package com.chukl.client.mixin;

import com.chukl.client.modules.misc.FreecamModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Ported from 67Client's EntityFreecamMixin: lets the local player see its own body while detached. */
@Mixin(Entity.class)
public class MiscEntityFreecamMixin {
    @Inject(method = "isInvisibleTo", at = @At("HEAD"), cancellable = true)
    private void chukl$freecamSeeOwnBody(PlayerEntity viewer, CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && self == mc.player && viewer == mc.player) {
            FreecamModule freecam = FreecamModule.get();
            if (freecam != null && freecam.isActive() && freecam.isShowPlayerModel()) {
                cir.setReturnValue(false);
            }
        }
    }
}
