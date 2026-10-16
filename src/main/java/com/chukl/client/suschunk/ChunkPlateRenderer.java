package com.chukl.client.suschunk;

import com.chukl.client.compat.Color;
import com.chukl.client.compat.Render3DEvent;
import com.chukl.client.compat.ShapeMode;
import net.minecraft.util.math.ChunkPos;

import java.util.HashSet;
import java.util.Set;

/**
 * Ported from Krispy's ChunkPlateRenderer: merges a set of flagged chunks into the fewest rectangular
 * plates (greedy row/column expansion) instead of one box per chunk, and draws a single outline around the
 * merged shape's outer boundary rather than between every pair of touching chunks. Simplified to draw through
 * Chukl's {@link com.chukl.client.core.Render3D}, which already offsets by the camera itself, so unlike the
 * original there's no manual matrix-stack transform to push/pop here.
 */
public final class ChunkPlateRenderer {
    private ChunkPlateRenderer() { }

    public static void draw(Render3DEvent event, Set<Long> chunks, Color fill, Color edge, float edgeWidth, double y, double thickness) {
        if (chunks.isEmpty() || fill.a == 0) return;

        Set<Long> remaining = new HashSet<>(chunks);
        while (!remaining.isEmpty()) {
            long start = remaining.iterator().next();
            int cx = ChunkPos.getPackedX(start);
            int cz = ChunkPos.getPackedZ(start);
            int left = cx, right = cx, top = cz, bottom = cz;

            while (remaining.contains(ChunkPos.toLong(left - 1, cz))) left--;
            while (remaining.contains(ChunkPos.toLong(right + 1, cz))) right++;
            while (hasRow(remaining, left, right, top - 1)) top--;
            while (hasRow(remaining, left, right, bottom + 1)) bottom++;

            for (int x = left; x <= right; x++) {
                for (int z = top; z <= bottom; z++) remaining.remove(ChunkPos.toLong(x, z));
            }

            double x1 = left * 16.0, z1 = top * 16.0;
            double x2 = (right + 1) * 16.0, z2 = (bottom + 1) * 16.0;
            event.renderer.box(x1, y, z1, x2, y + thickness, z2, fill, fill, ShapeMode.Sides, 0);
        }

        if (edge == null || edge.a == 0) return;

        // Outer boundary only: a north/south edge exists where exactly one of the two chunks straddling it
        // is flagged, and likewise for east/west; merge each run of those edges into one long line.
        Set<Long> northSouth = new HashSet<>();
        Set<Long> eastWest = new HashSet<>();
        for (long key : chunks) {
            int cx = ChunkPos.getPackedX(key);
            int cz = ChunkPos.getPackedZ(key);
            if (!chunks.contains(ChunkPos.toLong(cx, cz - 1))) northSouth.add(key);
            if (!chunks.contains(ChunkPos.toLong(cx, cz + 1))) northSouth.add(ChunkPos.toLong(cx, cz + 1));
            if (!chunks.contains(ChunkPos.toLong(cx - 1, cz))) eastWest.add(ChunkPos.toLong(cz, cx));
            if (!chunks.contains(ChunkPos.toLong(cx + 1, cz))) eastWest.add(ChunkPos.toLong(cz, cx + 1));
        }

        double lineY = y + thickness + 0.003;
        mergeLines(event, northSouth, false, lineY, edge, edgeWidth);
        mergeLines(event, eastWest, true, lineY, edge, edgeWidth);
    }

    private static boolean hasRow(Set<Long> set, int left, int right, int z) {
        for (int x = left; x <= right; x++) if (!set.contains(ChunkPos.toLong(x, z))) return false;
        return true;
    }

    private static void mergeLines(Render3DEvent event, Set<Long> set, boolean swapped, double y, Color edge, float width) {
        Set<Long> remaining = new HashSet<>(set);
        while (!remaining.isEmpty()) {
            long start = remaining.iterator().next();
            int a = ChunkPos.getPackedX(start);
            int b = ChunkPos.getPackedZ(start);
            int lo = a, hi = a;

            while (remaining.contains(ChunkPos.toLong(lo - 1, b))) lo--;
            while (remaining.contains(ChunkPos.toLong(hi + 1, b))) hi++;
            for (int x = lo; x <= hi; x++) remaining.remove(ChunkPos.toLong(x, b));

            double x1 = swapped ? b * 16.0 : lo * 16.0;
            double z1 = swapped ? lo * 16.0 : b * 16.0;
            double x2 = swapped ? b * 16.0 : (hi + 1) * 16.0;
            double z2 = swapped ? (hi + 1) * 16.0 : b * 16.0;
            event.renderer.line(x1, y, z1, x2, y, z2, edge);
        }
    }
}
