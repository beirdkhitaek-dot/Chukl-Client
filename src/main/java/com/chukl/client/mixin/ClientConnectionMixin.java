package com.chukl.client.mixin;

import com.chukl.client.core.ModuleManager;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lets modules see every packet in and out (Module.onPacketReceive / onPacketSend). */
@Mixin(ClientConnection.class)
public class ClientConnectionMixin {
    @Inject(method = "handlePacket", at = @At("HEAD"))
    private static void chuklclient$receive(Packet<?> packet, PacketListener listener, CallbackInfo ci) {
        try {
            ModuleManager.packetReceive(packet);
        } catch (Throwable ignored) {
        }
    }

    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;Lio/netty/channel/ChannelFutureListener;Z)V", at = @At("HEAD"), cancellable = true)
    private void chuklclient$send(Packet<?> packet, ChannelFutureListener listener, boolean flush, CallbackInfo ci) {
        try {
            if (ModuleManager.packetSend(packet)) ci.cancel();
        } catch (Throwable ignored) {
        }
    }
}
