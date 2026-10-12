package com.chukl.client.mixin;

import com.chukl.client.core.ModuleManager;
import com.chukl.client.modules.misc.SkinProtectModule;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Ported from 67Client's AbstractClientPlayerMixin: swaps in SkinProtect's replacement skin, if any. */
@Mixin(AbstractClientPlayerEntity.class)
public class MiscSkinProtectMixin {
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void chukl$replaceSkin(CallbackInfoReturnable<SkinTextures> cir) {
        SkinProtectModule skinProtect = ModuleManager.get(SkinProtectModule.class);
        if (skinProtect != null && skinProtect.isEnabled()) {
            SkinTextures replacement = skinProtect.replacementSkin();
            if (replacement != null) {
                AbstractClientPlayerEntity self = (AbstractClientPlayerEntity) (Object) this;
                if (skinProtect.shouldReplace(self.getUuid())) {
                    cir.setReturnValue(replacement);
                }
            }
        }
    }
}
