package com.chukl.client.mixin;

import com.chukl.client.core.ModuleManager;
import com.chukl.client.modules.misc.FakePayModule;
import com.chukl.client.modules.misc.FakeStatsModule;
import com.chukl.client.modules.misc.SpawnerProtectModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ported from 67Client's ClientPacketListenerMixin (FakePay/FakeStats command interception and
 * SpawnerProtect's block-update tracking only — the TPS tracker and BlockEntityEsp hooks aren't
 * part of Chukl's 1.8.0 Misc set).
 */
@Mixin(ClientPlayNetworkHandler.class)
public class MiscPacketCommandMixin {
    @Inject(method = "sendChatCommand", at = @At("HEAD"), cancellable = true)
    private void chukl$fakeCommands(String command, CallbackInfo ci) {
        try {
            FakePayModule fakePay = ModuleManager.get(FakePayModule.class);
            if (fakePay != null && fakePay.tryIntercept(command)) {
                ci.cancel();
                return;
            }
            FakeStatsModule fakeStats = ModuleManager.get(FakeStatsModule.class);
            if (fakeStats != null && fakeStats.tryInterceptBalance(command)) {
                ci.cancel();
            }
        } catch (Exception ignored) { }
    }

    @Inject(method = "onBlockBreakingProgress", at = @At("HEAD"))
    private void chukl$spawnerProtectDestruction(BlockBreakingProgressS2CPacket packet, CallbackInfo ci) {
        SpawnerProtectModule sp = spawnerProtect();
        if (sp != null && sp.isEnabled() && MinecraftClient.getInstance().isOnThread()) {
            try {
                sp.onBlockDestructionPacket(packet.getEntityId(), packet.getPos());
            } catch (Exception ignored) { }
        }
    }

    @Inject(method = "onBlockUpdate", at = @At("HEAD"))
    private void chukl$spawnerProtectBlockUpdate(BlockUpdateS2CPacket packet, CallbackInfo ci) {
        SpawnerProtectModule sp = spawnerProtect();
        if (sp != null && sp.isEnabled() && sp.detectBlockUpdatesEnabled() && MinecraftClient.getInstance().isOnThread()) {
            try {
                sp.onServerBlockUpdate(packet.getPos(), packet.getState(), false);
            } catch (Exception ignored) { }
        }
    }

    @Inject(method = "onChunkDeltaUpdate", at = @At("HEAD"))
    private void chukl$spawnerProtectSectionUpdate(ChunkDeltaUpdateS2CPacket packet, CallbackInfo ci) {
        SpawnerProtectModule sp = spawnerProtect();
        if (sp != null && sp.isEnabled() && sp.detectBlockUpdatesEnabled() && MinecraftClient.getInstance().isOnThread()) {
            try {
                packet.visitUpdates((pos, state) -> sp.onServerBlockUpdate(pos, state, true));
            } catch (Exception ignored) { }
        }
    }

    private static SpawnerProtectModule spawnerProtect() {
        return ModuleManager.get(SpawnerProtectModule.class);
    }
}
