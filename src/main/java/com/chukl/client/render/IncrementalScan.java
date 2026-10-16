package com.chukl.client.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Ported from 67Client's dev.sixseven.render.IncrementalScan: spreads a chunk scan over several ticks
 * (closest chunks first) instead of doing it all in one tick, so a wide scan radius doesn't cause a hitch.
 * Re-sweeps from the start whenever {@link #markDirty()} is called or the player moves chunks, and otherwise
 * idles for {@code idleTicks} after a full pass completes.
 */
public final class IncrementalScan<H> {
    private final int chunksPerTick;
    private final int blockBudgetPerTick;
    private final int idleTicks;

    private volatile List<H> published = List.of();
    private List<H> building = new ArrayList<>();
    private int cursor;
    private int[] order = new int[0];
    private int orderRadius = -1;
    private int sweepPcx = Integer.MIN_VALUE;
    private int sweepPcz = Integer.MIN_VALUE;
    private int cooldown;
    private boolean dirty;

    public IncrementalScan(int chunksPerTick, int blockBudgetPerTick, int idleTicks) {
        this.chunksPerTick = chunksPerTick;
        this.blockBudgetPerTick = blockBudgetPerTick;
        this.idleTicks = idleTicks;
    }

    public List<H> get() { return published; }

    public void markDirty() { dirty = true; }

    public void clear() {
        published = List.of();
        building = new ArrayList<>();
        cursor = 0;
        cooldown = 0;
        dirty = false;
        sweepPcx = sweepPcz = Integer.MIN_VALUE;
    }

    public void tick(int radius, ChunkScanner<H> scanner) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientWorld level = mc.world;
        ClientPlayerEntity player = mc.player;
        if (level == null || player == null) return;

        if (orderRadius != radius) {
            ensureOrder(radius);
            dirty = true;
        }

        if (cursor == 0) {
            if (!dirty && cooldown > 0) {
                cooldown--;
                return;
            }
            sweepPcx = player.getChunkPos().x;
            sweepPcz = player.getChunkPos().z;
            building = new ArrayList<>();
            dirty = false;
        } else if (dirty) {
            sweepPcx = player.getChunkPos().x;
            sweepPcz = player.getChunkPos().z;
            cursor = 0;
            building = new ArrayList<>();
            dirty = false;
        }

        int total = order.length;
        int chunks = 0;

        for (int blocks = 0; cursor < total && chunks < chunksPerTick && blocks < blockBudgetPerTick; chunks++) {
            int packed = order[cursor];
            int dx = (short) (packed >> 16);
            int dz = (short) (packed & 0xFFFF);
            blocks += scanner.scan(level.getChunk(sweepPcx + dx, sweepPcz + dz), building);
            cursor++;
        }

        if (cursor >= total) {
            published = building;
            building = new ArrayList<>();
            cursor = 0;
            cooldown = idleTicks;
        }
    }

    private void ensureOrder(int radius) {
        if (orderRadius == radius) return;

        int side = 2 * radius + 1;
        Integer[] offs = new Integer[side * side];
        int i = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                offs[i++] = ((dx & 0xFFFF) << 16) | (dz & 0xFFFF);
            }
        }

        Arrays.sort(offs, (a, b) -> {
            int adx = (short) (a >> 16), adz = (short) (a & 0xFFFF);
            int bdx = (short) (b >> 16), bdz = (short) (b & 0xFFFF);
            return Integer.compare(adx * adx + adz * adz, bdx * bdx + bdz * bdz);
        });

        int[] out = new int[offs.length];
        for (int k = 0; k < offs.length; k++) out[k] = offs[k];
        order = out;
        orderRadius = radius;
    }

    public interface ChunkScanner<H> {
        int scan(WorldChunk chunk, List<H> out);
    }
}
