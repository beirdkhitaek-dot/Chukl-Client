package com.chukl.client.modules.donut;

import com.chukl.client.compat.Events.TickEvent;
import com.chukl.client.compat.MeteorModule;
import com.chukl.client.compat.Render3DEvent;
import com.chukl.client.compat.SettingColor;
import com.chukl.client.core.Category;
import com.chukl.client.core.Render3D;
import com.chukl.client.settings.BoolSetting;
import com.chukl.client.settings.NumberSetting;
import com.chukl.client.suschunk.ChunkLightSampler;
import com.chukl.client.suschunk.ChunkPeakFinder;
import com.chukl.client.suschunk.ChunkPlateRenderer;

import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.LightUpdateS2CPacket;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import java.util.function.LongConsumer;

/**
 * Krispy's Sus Chunk V2. Ported near-verbatim from Krispy: scores every chunk by its count of "sealed light
 * pocket" anomalies ({@link ChunkLightSampler}), spreads that heat additively into a radius around it, and
 * flags only the chunks that come out as local peaks ({@link ChunkPeakFinder}) of the spread heat map, above
 * a sensitivity threshold and with enough already-scanned neighbours to be confident. Flagged chunks are
 * drawn as merged plates via {@link ChunkPlateRenderer}.
 */
public class SusChunkV2Module extends MeteorModule {
    private record ChunkEvidence(int selfHeat, boolean hasUngrown) { }

    private static final long SCAN_BUDGET_NANOS = 3_000_000L;
    private static final int MIN_SCANNED_NEIGHBOURS = 3;
    private static final int VISIBLE_REFRESH_TICKS = 20;
    private static final int PACKET_MIN_Y = -16;
    private static final int PACKET_MAX_Y = 30;
    private static final int GLOW_CELLS_PER_POINT = 12;

    private final NumberSetting spreadRadius = add(new NumberSetting("Simulation Distance", "How far (in chunks) a chunk's heat spreads, and the peak-detection radius.", 5, 2, 16, 1));
    private final NumberSetting sensitivity = add(new NumberSetting("Sensitivity", "Minimum spread heat for a peak chunk to be flagged.", 6, 1, 20, 1));
    private final BoolSetting outline = add(new BoolSetting("Outline", "Draws an outline around the merged plates.", true));
    private final NumberSetting alpha = add(new NumberSetting("Alpha", "Plate opacity (0-255).", 100, 0, 255, 1));

    private final ChunkLightSampler glow = new ChunkLightSampler();
    private final ChunkPeakFinder peakFinder = new ChunkPeakFinder();

    private final Long2IntOpenHashMap heat = new Long2IntOpenHashMap();
    private final Long2IntOpenHashMap vetoes = new Long2IntOpenHashMap();
    private final Long2IntOpenHashMap chunkScore = new Long2IntOpenHashMap();
    private final Long2BooleanOpenHashMap chunkUngrown = new Long2BooleanOpenHashMap();
    private final Long2BooleanOpenHashMap chunkHasPacket = new Long2BooleanOpenHashMap();
    private final LongOpenHashSet scannedChunks = new LongOpenHashSet();
    private final LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
    private final LongOpenHashSet queued = new LongOpenHashSet();
    private final Long2IntOpenHashMap settling = new Long2IntOpenHashMap();

    private Long2IntOpenHashMap visible = new Long2IntOpenHashMap();
    private boolean visibleDirty = true;
    private int visibleAge;
    private int lastSensitivity;

    private ClientWorld trackedWorld;
    private int appliedRadius;
    private int ticks;

    public SusChunkV2Module() {
        super(Category.DONUT, "Sus Chunk V2", "Krispy's Sus Chunk V2: detects suspicious underground activity from geode light patterns.");
        lastSensitivity = sensitivity.getInt();
    }

    @Override
    protected void onEnable() {
        clearAll();
        trackedWorld = mc.world;
        queueLoadedChunks();
    }

    @Override
    protected void onDisable() {
        clearAll();
        trackedWorld = null;
    }

    private void clearAll() {
        heat.clear();
        vetoes.clear();
        chunkScore.clear();
        chunkUngrown.clear();
        chunkHasPacket.clear();
        scannedChunks.clear();
        queue.clear();
        queued.clear();
        settling.clear();
        ticks = 0;
        visible = new Long2IntOpenHashMap();
        visibleDirty = true;
        visibleAge = 0;
        appliedRadius = spreadRadius.getInt();
    }

    private void syncState(ClientWorld world) {
        if (world != trackedWorld) {
            clearAll();
            trackedWorld = world;
            queueLoadedChunks();
        }
    }

    private void queueLoadedChunks() {
        if (mc.world == null || mc.player == null) return;

        int radius = mc.options.getClampedViewDistance() + 1;
        ChunkPos center = mc.player.getChunkPos();
        for (int x = center.x - radius; x <= center.x + radius; x++) {
            for (int z = center.z - radius; z <= center.z + radius; z++) {
                if (mc.world.isChunkLoaded(x, z)) enqueue(ChunkPos.toLong(x, z));
            }
        }
    }

    private void enqueue(long key) {
        if (queued.add(key)) queue.enqueue(key);
    }

    // ---------- packets ----------

    private void onChunkData(ChunkDataS2CPacket packet) {
        syncState(mc.world);
        enqueue(ChunkPos.toLong(packet.getChunkX(), packet.getChunkZ()));
        settleAround(packet.getChunkX(), packet.getChunkZ());
    }

    private void onLightUpdate(LightUpdateS2CPacket packet) {
        syncState(mc.world);
        settleAround(packet.getChunkX(), packet.getChunkZ());
    }

    private void settleAround(int chunkX, int chunkZ) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                long key = ChunkPos.toLong(chunkX + dx, chunkZ + dz);
                settling.put(key, Math.min(settling.getOrDefault(key, ticks + 2), ticks + 2));
            }
        }
    }

    private void drainSettling() {
        ObjectIterator<Long2IntMap.Entry> it = settling.long2IntEntrySet().fastIterator();
        while (it.hasNext()) {
            Long2IntMap.Entry entry = it.next();
            if (entry.getIntValue() <= ticks) {
                enqueue(entry.getLongKey());
                it.remove();
            }
        }
    }

    // ---------- tick ----------

    private void onTick(TickEvent.Post event) {
        ClientWorld world = mc.world;
        syncState(world);
        if (world == null) return;

        ticks++;
        drainSettling();

        if (spreadRadius.getInt() != appliedRadius) {
            clearAll();
            queueLoadedChunks();
        }
        if (sensitivity.getInt() != lastSensitivity) {
            lastSensitivity = sensitivity.getInt();
            visibleDirty = true;
        }

        long deadline = System.nanoTime() + SCAN_BUDGET_NANOS;
        while (!queue.isEmpty() && System.nanoTime() < deadline) {
            long key = queue.dequeueLong();
            queued.remove(key);
            int x = ChunkPos.getPackedX(key);
            int z = ChunkPos.getPackedZ(key);
            if (world.isChunkLoaded(x, z)) {
                updateChunk(key, world.getChunk(x, z), world);
                if (scannedChunks.add(key)) visibleDirty = true;
            }
        }

        if (visibleDirty || ++visibleAge >= VISIBLE_REFRESH_TICKS) {
            visible = computeVisible(world);
            visibleDirty = false;
            visibleAge = 0;
        }
    }

    private void updateChunk(long key, WorldChunk chunk, ClientWorld world) {
        int radius = spreadRadius.getInt();
        int cx = ChunkPos.getPackedX(key);
        int cz = ChunkPos.getPackedZ(key);
        int oldSelfHeat = chunkScore.getOrDefault(key, 0);
        boolean oldUngrown = chunkUngrown.getOrDefault(key, false);
        boolean hasPacket = hasBlocksInRange(chunk, PACKET_MIN_Y, PACKET_MAX_Y);

        ChunkEvidence evidence = scoreChunk(chunk, world);

        if (!chunkHasPacket.containsKey(key) || chunkHasPacket.get(key) != hasPacket) {
            visibleDirty = true;
        }
        chunkHasPacket.put(key, hasPacket);

        if (oldSelfHeat != evidence.selfHeat() || oldUngrown != evidence.hasUngrown() || !chunkScore.containsKey(key)) {
            if (oldSelfHeat > 0) {
                forArea(cx, cz, radius, pos -> subtract(heat, pos, oldSelfHeat));
            }
            if (oldUngrown) {
                forArea(cx, cz, radius, pos -> subtract(vetoes, pos, 1));
            }

            chunkScore.put(key, evidence.selfHeat());
            chunkUngrown.put(key, evidence.hasUngrown());

            if (evidence.hasUngrown()) {
                forArea(cx, cz, radius, pos -> vetoes.addTo(pos, 1));
            }
            if (evidence.selfHeat() > 0) {
                forArea(cx, cz, radius, pos -> heat.addTo(pos, evidence.selfHeat()));
            }

            visibleDirty = true;
        }
    }

    private static void forArea(int cx, int cz, int radius, LongConsumer consumer) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                consumer.accept(ChunkPos.toLong(cx + dx, cz + dz));
            }
        }
    }

    private static void subtract(Long2IntOpenHashMap map, long key, int amount) {
        int next = map.get(key) - amount;
        if (next > 0) map.put(key, next);
        else map.remove(key);
    }

    private static boolean hasBlocksInRange(WorldChunk chunk, int minY, int maxY) {
        ChunkSection[] sections = chunk.getSectionArray();
        int bottom = chunk.getBottomSectionCoord();

        for (int i = 0; i < sections.length; i++) {
            ChunkSection section = sections[i];
            if (section == null || section.isEmpty()) continue;

            int sectionY = (bottom + i) << 4;
            if (sectionY >= minY && sectionY + 15 <= maxY) return true;

            if (sectionY + 15 >= minY && sectionY <= maxY) {
                for (int ly = 0; ly < 16; ly++) {
                    int y = sectionY + ly;
                    if (y < minY || y > maxY) continue;
                    for (int lx = 0; lx < 16; lx++) {
                        for (int lz = 0; lz < 16; lz++) {
                            if (!section.getBlockState(lx, ly, lz).isAir()) return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private ChunkEvidence scoreChunk(WorldChunk chunk, ClientWorld world) {
        return new ChunkEvidence(glow.scan(world, chunk, null) / GLOW_CELLS_PER_POINT, false);
    }

    private Long2IntOpenHashMap computeVisible(ClientWorld world) {
        Long2IntOpenHashMap scanned = new Long2IntOpenHashMap();
        ObjectIterator<Long2IntMap.Entry> it = heat.long2IntEntrySet().iterator();
        while (it.hasNext()) {
            Long2IntMap.Entry entry = it.next();
            long key = entry.getLongKey();
            if (vetoes.containsKey(key)) continue;

            int x = ChunkPos.getPackedX(key);
            int z = ChunkPos.getPackedZ(key);
            if (world.isChunkLoaded(x, z) && scannedNeighbours(x, z) >= MIN_SCANNED_NEIGHBOURS) {
                scanned.put(key, entry.getIntValue());
            }
        }

        int threshold = sensitivity.getInt();
        int radius = appliedRadius;
        Long2IntOpenHashMap result = new Long2IntOpenHashMap();
        LongOpenHashSet peaks = peakFinder.find(scanned, radius);

        ObjectIterator<Long2IntMap.Entry> it2 = scanned.long2IntEntrySet().iterator();
        while (it2.hasNext()) {
            Long2IntMap.Entry entry = it2.next();
            long key = entry.getLongKey();
            int value = entry.getIntValue();
            int localThreshold = chunkHasPacket.getOrDefault(key, true) ? threshold : 1;
            if (value >= localThreshold && peaks.contains(key)) {
                result.put(key, value);
            }
        }
        return result;
    }

    private int scannedNeighbours(int cx, int cz) {
        int count = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if ((dx != 0 || dz != 0) && scannedChunks.contains(ChunkPos.toLong(cx + dx, cz + dz))) count++;
            }
        }
        return count;
    }

    // ---------- render ----------

    private void onRender(Render3DEvent event) {
        ClientWorld world = mc.world;
        syncState(world);
        if (world == null) return;

        int a = alpha.getInt();
        SettingColor fill = new SettingColor(255, 235, 0, a);
        SettingColor edge = outline.get() ? new SettingColor(255, 242, 75, Math.min(220, a + 45)) : null;
        ChunkPlateRenderer.draw(event, visible.keySet(), fill, edge, 1.0F, 63.0, 0.025);
    }

    @Override
    protected void onTick() {
        onTick(new TickEvent.Post());
    }

    @Override
    protected void onRender3D(Render3D r3d) {
        onRender(new Render3DEvent(r3d));
    }

    @Override
    protected void onPacket(Packet<?> packet) {
        if (packet instanceof ChunkDataS2CPacket chunkData) {
            onChunkData(chunkData);
        } else if (packet instanceof LightUpdateS2CPacket lightUpdate) {
            onLightUpdate(lightUpdate);
        }
    }
}
