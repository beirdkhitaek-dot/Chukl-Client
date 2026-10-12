package com.chukl.client.mixin;

import com.chukl.client.modules.misc.FreecamModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ported from 67Client's KeyboardInputFreecamMixin: re-applies Freecam's latched body movement every tick. */
@Mixin(KeyboardInput.class)
public class MiscKeyboardInputFreecamMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void chukl$freecamReapplyBodyInput(CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && mc.player.input == (Object) this) {
            FreecamModule f = FreecamModule.get();
            if (f != null && f.isActive()) {
                FreecamModule.reapplyBodyInput(mc);
            }
        }
    }
}
