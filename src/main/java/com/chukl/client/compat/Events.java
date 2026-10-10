package com.chukl.client.compat;

import net.minecraft.block.BlockState;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.math.BlockPos;

/** Event shapes used by ported modules. */
public final class Events {
    private Events() { }

    public static final class TickEvent {
        public static final class Post { }
        public static final class Pre { }
    }

    public static final class PacketEvent {
        public static final class Receive {
            public final Packet<?> packet;
            public Receive(Packet<?> packet) { this.packet = packet; }
        }
    }

    public static final class BlockUpdateEvent {
        public final BlockPos pos;
        public final BlockState newState;
        public BlockUpdateEvent(BlockPos pos, BlockState newState) { this.pos = pos; this.newState = newState; }
    }
}
