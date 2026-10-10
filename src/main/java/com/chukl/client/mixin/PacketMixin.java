package com.chukl.client.mixin;

import com.chukl.client.core.ModuleManager;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.LightUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldEventS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hands incoming packets to modules (Module.onPacket) after the game has processed them. */
@Mixin(ClientPlayNetworkHandler.class)
public class PacketMixin {
    @Inject(method = "onChunkData", at = @At("TAIL"))
    private void chuklclient$chunkData(ChunkDataS2CPacket p, CallbackInfo ci) { ModuleManager.packet(p); }

    @Inject(method = "onLightUpdate", at = @At("TAIL"))
    private void chuklclient$light(LightUpdateS2CPacket p, CallbackInfo ci) { ModuleManager.packet(p); }

    @Inject(method = "onBlockUpdate", at = @At("TAIL"))
    private void chuklclient$blockUpdate(BlockUpdateS2CPacket p, CallbackInfo ci) { ModuleManager.packet(p); }

    @Inject(method = "onChunkDeltaUpdate", at = @At("TAIL"))
    private void chuklclient$delta(ChunkDeltaUpdateS2CPacket p, CallbackInfo ci) { ModuleManager.packet(p); }

    @Inject(method = "onBlockEntityUpdate", at = @At("TAIL"))
    private void chuklclient$blockEntity(BlockEntityUpdateS2CPacket p, CallbackInfo ci) { ModuleManager.packet(p); }

    @Inject(method = "onBlockBreakingProgress", at = @At("TAIL"))
    private void chuklclient$breaking(BlockBreakingProgressS2CPacket p, CallbackInfo ci) { ModuleManager.packet(p); }

    @Inject(method = "onWorldEvent", at = @At("TAIL"))
    private void chuklclient$worldEvent(WorldEventS2CPacket p, CallbackInfo ci) { ModuleManager.packet(p); }

    @Inject(method = "onGameJoin", at = @At("TAIL"))
    private void chuklclient$join(GameJoinS2CPacket p, CallbackInfo ci) { ModuleManager.packet(p); }

    @Inject(method = "onPlayerRespawn", at = @At("TAIL"))
    private void chuklclient$respawn(PlayerRespawnS2CPacket p, CallbackInfo ci) { ModuleManager.packet(p); }
}
