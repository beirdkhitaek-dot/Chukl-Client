package com.chukl.client.mixin;

import com.chukl.client.core.ModuleManager;
import com.chukl.client.modules.misc.FakeRolesModule;
import com.chukl.client.modules.misc.NameProtectModule;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Ported from 67Client's ChatComponentMixin: censors / decorates a chat line before it's displayed. */
@Mixin(ChatHud.class)
public class MiscChatCensorMixin {
    @ModifyVariable(method = "addMessage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Text chukl$censorChat(Text component) {
        Text result = component;

        FakeRolesModule fakeRoles = ModuleManager.get(FakeRolesModule.class);
        if (fakeRoles != null) {
            result = fakeRoles.decorateChat(component);
        }

        NameProtectModule nameProtect = ModuleManager.get(NameProtectModule.class);
        if (nameProtect != null && nameProtect.isEnabled()) {
            result = nameProtect.censorChat(result);
        }

        return result;
    }
}
