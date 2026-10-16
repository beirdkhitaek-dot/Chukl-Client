package com.chukl.client.suschunk;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.util.math.ChunkPos;

import java.util.Arrays;

/**
 * Ported from Krispy's ChunkPeakFinder (part of Sus Chunk V2): given a sparse map of per-chunk heat
 * values, finds the chunks that are a local maximum within {@code radius} chunks of themselves. Builds a
 * dense 2D grid and runs a classic monotonic-deque sliding-window max filter (horizontal pass, then
 * vertical pass) when the bounding box is small enough; falls back to a brute-force sparse neighbor check
 * for very large or spread-out sample sets.
 */
public final class ChunkPeakFinder {
    private static final long MAX_GRID_CELLS = 1_048_576L;

    private int[] grid = new int[0];
    private int[] horizontal = new int[0];
    private int[] maxima = new int[0];
    private int[] deque = new int[0];

    public LongOpenHashSet find(Long2IntOpenHashMap samples, int radius) {
        LongOpenHashSet peaks = new LongOpenHashSet();
        if (samples.isEmpty()) return peaks;

        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        LongIterator positions = samples.keySet().iterator();
        while (positions.hasNext()) {
            long position = positions.nextLong();
            int x = ChunkPos.getPackedX(position);
            int z = ChunkPos.getPackedZ(position);
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minZ = Math.min(minZ, z);
            maxZ = Math.max(maxZ, z);
        }

        long width = (long) maxX - minX + 1L;
        long height = (long) maxZ - minZ + 1L;
        if (width > MAX_GRID_CELLS || height > MAX_GRID_CELLS || width * height > MAX_GRID_CELLS) {
            return findSparse(samples, radius, peaks);
        }

        int columns = (int) width;
        int rows = (int) height;
        int cells = columns * rows;
        if (grid.length < cells) {
            grid = new int[cells];
            horizontal = new int[cells];
            maxima = new int[cells];
        } else {
            Arrays.fill(grid, 0, cells, 0);
        }
        if (deque.length < Math.max(columns, rows)) {
            deque = new int[Math.max(columns, rows)];
        }

        final int fMinX = minX;
        final int fMinZ = minZ;
        ObjectIterator<Long2IntMap.Entry> entries = samples.long2IntEntrySet().iterator();
        while (entries.hasNext()) {
            Long2IntMap.Entry entry = entries.next();
            int x = ChunkPos.getPackedX(entry.getLongKey()) - fMinX;
            int z = ChunkPos.getPackedZ(entry.getLongKey()) - fMinZ;
            grid[z * columns + x] = entry.getIntValue();
        }
        for (int z = 0; z < rows; z++) {
            window(grid, horizontal, z * columns, 1, columns, radius);
        }
        for (int x = 0; x < columns; x++) {
            window(horizontal, maxima, x, columns, rows, radius);
        }

        entries = samples.long2IntEntrySet().iterator();
        while (entries.hasNext()) {
            Long2IntMap.Entry entry = entries.next();
            int x = ChunkPos.getPackedX(entry.getLongKey()) - fMinX;
            int z = ChunkPos.getPackedZ(entry.getLongKey()) - fMinZ;
            if (entry.getIntValue() >= maxima[z * columns + x]) {
                peaks.add(entry.getLongKey());
            }
        }
        return peaks;
    }

    private LongOpenHashSet findSparse(Long2IntOpenHashMap samples, int radius, LongOpenHashSet peaks) {
        ObjectIterator<Long2IntMap.Entry> entries = samples.long2IntEntrySet().iterator();
        while (entries.hasNext()) {
            Long2IntMap.Entry entry = entries.next();
            int x = ChunkPos.getPackedX(entry.getLongKey());
            int z = ChunkPos.getPackedZ(entry.getLongKey());
            boolean peak = true;
            neighbors:
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    if ((dz != 0 || dx != 0) && samples.get(ChunkPos.toLong(x + dz, z + dx)) > entry.getIntValue()) {
                        peak = false;
                        break neighbors;
                    }
                }
            }
            if (peak) peaks.add(entry.getLongKey());
        }
        return peaks;
    }

    private void window(int[] input, int[] output, int start, int stride, int length, int radius) {
        int head = 0;
        int tail = 0;
        int next = 0;
        for (int center = 0; center < length; center++) {
            for (int end = Math.min(length - 1, center + radius); next <= end; deque[tail++] = next++) {
                int value = input[start + next * stride];
                while (tail > head && input[start + deque[tail - 1] * stride] <= value) {
                    tail--;
                }
            }
            while (tail > head && deque[head] < center - radius) {
                head++;
            }
            output[start + center * stride] = input[start + deque[head] * stride];
        }
    }
}
