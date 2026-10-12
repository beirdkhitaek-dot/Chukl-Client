package com.chukl.client.mixin;

import com.mojang.authlib.GameProfile;
import com.chukl.client.core.ModuleManager;
import com.chukl.client.modules.misc.FakeRolesModule;
import com.chukl.client.modules.misc.NameProtectModule;
import com.chukl.client.modules.misc.NameTagsModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ported from 67Client's EntityNameTagMixin: one hook for NameTags (hide vanilla tags), NameProtect
 * (censor a display name) and FakeRoles (decorate the local player's own tag).
 */
@Mixin(EntityRenderer.class)
public class MiscNameTagMixin {
    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
    private void chukl$nameTag(EntityRenderState state, MatrixStack poseStack, OrderedRenderCommandQueue collector,
                                CameraRenderState cameraRenderState, CallbackInfo ci) {
        if (state.displayName == null) return;

        NameTagsModule nameTags = ModuleManager.get(NameTagsModule.class);
        boolean nameTagsOn = nameTags != null && nameTags.isEnabled();
        if (nameTagsOn) {
            String display = state.displayName.getString();
            if (isLocalPlayer(display)) {
                if (nameTags.hideOwnTag.get() || (nameTags.players.get() && nameTags.self.get())) {
                    ci.cancel();
                    return;
                }
            } else if (isOnlinePlayer(display)) {
                if (nameTags.players.get() || nameTags.hidePlayerTags.get()) {
                    ci.cancel();
                    return;
                }
            } else if (nameTags.hideOtherTags.get()) {
                ci.cancel();
                return;
            }
        }

        NameProtectModule nameProtect = ModuleManager.get(NameProtectModule.class);
        if (nameProtect != null && nameProtect.isEnabled()) {
            String replacement = nameProtect.replacementForDisplay(state.displayName.getString());
            if (replacement != null) {
                state.displayName = Text.literal(replacement);
            }
        }

        FakeRolesModule fakeRoles = ModuleManager.get(FakeRolesModule.class);
        if (fakeRoles != null && isLocalPlayer(state.displayName.getString())) {
            state.displayName = fakeRoles.decorateNametag(state.displayName);
        }
    }

    private static boolean isLocalPlayer(String display) {
        if (display == null || display.isEmpty()) return false;
        ClientPlayerEntity self = MinecraftClient.getInstance().player;
        if (self == null) return false;
        String name = self.getGameProfile().name();
        return name != null && !name.isEmpty() && display.contains(name);
    }

    private static boolean isOnlinePlayer(String display) {
        if (display == null || display.isEmpty()) return false;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getNetworkHandler() == null) return false;
        for (PlayerListEntry info : mc.getNetworkHandler().getPlayerList()) {
            GameProfile profile = info.getProfile();
            String name = profile == null ? null : profile.name();
            if (name != null && !name.isEmpty() && display.contains(name)) return true;
        }
        return false;
    }
}
