package com.chukl.client.mixin;

import com.mojang.authlib.GameProfile;
import com.chukl.client.core.ModuleManager;
import com.chukl.client.modules.misc.FakeRolesModule;
import com.chukl.client.modules.misc.NameProtectModule;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Ported from 67Client's PlayerTabOverlayMixin, using a plain @Inject(RETURN) in place of MixinExtras' @ModifyReturnValue. */
@Mixin(PlayerListHud.class)
public class MiscTabNameMixin {
    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
    private void chukl$protectTabName(PlayerListEntry playerInfo, CallbackInfoReturnable<Text> cir) {
        GameProfile profile = playerInfo.getProfile();
        String name = profile == null ? null : profile.name();
        Text result = cir.getReturnValue();
        boolean changed = false;

        NameProtectModule nameProtect = ModuleManager.get(NameProtectModule.class);
        if (nameProtect != null && nameProtect.isEnabled() && name != null && !name.isEmpty()) {
            String replacement = nameProtect.replacementForDisplay(name);
            if (replacement != null) {
                result = Text.literal(replacement);
                changed = true;
            }
        }

        FakeRolesModule fakeRoles = ModuleManager.get(FakeRolesModule.class);
        if (fakeRoles != null && name != null) {
            result = fakeRoles.decorateTab(result, name);
            changed = true;
        }

        if (changed) cir.setReturnValue(result);
    }
}
