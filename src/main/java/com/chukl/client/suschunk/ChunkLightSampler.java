package com.chukl.client.suschunk;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.ChunkLightingView;

import java.util.function.LongConsumer;

/**
 * Ported from Krispy's ChunkLightSampler (part of Sus Chunk V2). Caches a 3x3-chunk by 8-section block-light
 * grid around a chunk, then scans every "open" block (air or an amethyst cluster) in Y range [-58, 30]: if
 * that block itself has zero light but sits next to a block with light exactly 4 that is also open, it's
 * counted as an anomaly (the signature of a geode-style sealed light pocket). Used as the per-chunk "heat"
 * signal for Sus Chunk V2's peak detection.
 */
public final class ChunkLightSampler {
    public static final int MIN_Y = -58;
    public static final int MAX_Y = 30;
    private static final int EDGE_LIGHT = 4;
    private static final int LOW_SECTION = -4;
    private static final int HIGH_SECTION = 1;
    private static final int FIRST_CACHED = -5;
    private static final int CACHED = 8;
    private static final Direction[] SIDES = Direction.values();

    private final ChunkNibbleArray[] layers = new ChunkNibbleArray[72];
    private final BlockPos.Mutable cursor = new BlockPos.Mutable();
    private int chunkX;
    private int chunkZ;

    public int scan(ClientWorld world, WorldChunk chunk, LongConsumer out) {
        this.chunkX = chunk.getPos().x;
        this.chunkZ = chunk.getPos().z;
        if (!cacheLight(world)) return 0;

        ChunkSection[] sections = chunk.getSectionArray();
        int baseX = this.chunkX << 4;
        int baseZ = this.chunkZ << 4;
        int count = 0;

        for (int sy = LOW_SECTION; sy <= HIGH_SECTION; sy++) {
            int index = chunk.sectionCoordToIndex(sy);
            if (index < 0 || index >= sections.length || sections[index] == null || !nearLight(sy)) continue;

            ChunkSection section = sections[index];
            if (!section.hasAny(ChunkLightSampler::open)) continue;

            int minY = Math.max(MIN_Y, sy << 4);
            int maxY = Math.min(MAX_Y, (sy << 4) + 15);
            for (int y = minY; y <= maxY; y++) {
                for (int lx = 0; lx < 16; lx++) {
                    for (int lz = 0; lz < 16; lz++) {
                        if (!open(section.getBlockState(lz, y & 15, lx))) continue;
                        int x = baseX + lz;
                        int z = baseZ + lx;
                        if (lightAt(x, y, z) == 0 && edge(world, x, y, z)) {
                            count++;
                            if (out != null) out.accept(BlockPos.asLong(x, y, z));
                        }
                    }
                }
            }
        }
        return count;
    }

    private boolean cacheLight(ClientWorld world) {
        ChunkLightingView view = world.getLightingProvider().get(LightType.BLOCK);
        boolean any = false;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int sy = 0; sy < CACHED; sy++) {
                    ChunkNibbleArray section = view.getLightSection(
                        ChunkSectionPos.from(this.chunkX + dx, FIRST_CACHED + sy, this.chunkZ + dz));
                    if (section != null && section.isUninitialized()) section = null;
                    layers[slot(dx, dz, sy)] = section;
                    any |= section != null;
                }
            }
        }
        return any;
    }

    private boolean nearLight(int sy) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int s = sy - 1; s <= sy + 1; s++) {
                    if (layers[slot(dx, dz, s - FIRST_CACHED)] != null) return true;
                }
            }
        }
        return false;
    }

    private boolean edge(ClientWorld world, int x, int y, int z) {
        boolean sawOpenLit = false;
        int maxLight = 0;

        for (Direction side : SIDES) {
            int nx = x + side.getOffsetX();
            int ny = y + side.getOffsetY();
            int nz = z + side.getOffsetZ();
            int light = lightAt(nx, ny, nz);
            if (light > EDGE_LIGHT) return false;

            maxLight = Math.max(maxLight, light);
            if (light == EDGE_LIGHT && !sawOpenLit && open(world.getBlockState(cursor.set(nx, ny, nz)))) {
                sawOpenLit = true;
            }
        }
        return maxLight == EDGE_LIGHT && sawOpenLit;
    }

    private int lightAt(int x, int y, int z) {
        int sy = (y >> 4) - FIRST_CACHED;
        if (sy < 0 || sy >= CACHED) return 0;
        ChunkNibbleArray section = layers[slot((x >> 4) - this.chunkX, (z >> 4) - this.chunkZ, sy)];
        return section == null ? 0 : section.get(x & 15, y & 15, z & 15);
    }

    private static int slot(int dx, int dz, int sy) {
        return ((dx + 1) * 3 + dz + 1) * CACHED + sy;
    }

    private static boolean open(BlockState state) {
        return state.isAir() || state.isOf(Blocks.AMETHYST_CLUSTER);
    }
}
