package com.chukl.client.modules.donut;

import com.chukl.client.compat.Events.BlockUpdateEvent;
import com.chukl.client.compat.Events.TickEvent;
import com.chukl.client.compat.MeteorModule;
import com.chukl.client.compat.Render3DEvent;
import com.chukl.client.compat.SettingColor;
import com.chukl.client.core.Category;
import com.chukl.client.core.Render3D;
import com.chukl.client.settings.BoolSetting;
import com.chukl.client.settings.ColorSetting;
import com.chukl.client.settings.ModeSetting;
import com.chukl.client.settings.NumberSetting;
import com.chukl.client.suschunk.ChunkPlateRenderer;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Krispy's "Chunk Finder Better". Ported near-verbatim: looks for columns of plain stone, tall enough and
 * flanked on every side by the same "wall" block (deepslate/stone), that sit next to a blob of
 * diorite/andesite/granite/gravel within a shallow height band -- the signature of an ore vein that was mined
 * out and the hole back-filled with stone, which legitimate deposits almost never look like. Calm mode wants a
 * taller, cleaner stack; Aggressive is looser. Flags spread into a radius like Sus Chunk V2 and are drawn as
 * merged plates, with the dense patches (not every flagged chunk) kept visible to avoid visual clutter.
 */
public class ChunkFinderBetterModule extends MeteorModule {
    private record ChunkCandidate(long pos, int height, long[] stacks) {
        boolean sameAs(ChunkCandidate other) {
            return pos == other.pos && height == other.height && Arrays.equals(stacks, other.stacks);
        }
    }

    private record HeightBand(int minY, int maxY, int stackHeight) { }

    private static final int PROBE_CHUNKS = 48;
    private static final byte OTHER = 0;
    private static final byte STONE = 1;
    private static final byte BLOB = 2;
    private static final byte AIR = 3;
    private static final byte MISSING = 4;
    private static final int[] SIDE_X = {1, -1, 0, 0};
    private static final int[] SIDE_Z = {0, 0, 1, -1};
    private static final int[] CORNER_X = {1, 1, -1, -1};
    private static final int[] CORNER_Z = {1, -1, 1, -1};
    private static final int MIN_Y = 0;
    private static final int MAX_Y = 32;
    private static final SoundEvent PING = SoundEvent.of(Identifier.of("minecraft", "entity.experience_orb.pickup"));
    private static final ChunkCandidate UNDECIDED = new ChunkCandidate(0L, 0, new long[0]);

    private final ModeSetting detection = add(new ModeSetting("Detection", "Calm wants a taller, cleaner stack; Aggressive is looser and flags more.", "Calm", "Calm", "Aggressive"));
    private final NumberSetting spreadRadius = add(new NumberSetting("Spread Radius", "How far (in chunks) a flagged chunk's heat spreads before only the densest patches are kept visible.", 1, 0, 16, 1));
    private final NumberSetting alpha = add(new NumberSetting("Alpha", "Plate opacity (0-255).", 96, 0, 255, 1));
    private final ColorSetting chunkColor = add(new ColorSetting("Chunk Color", "Color of the flagged-chunk plates (alpha controlled separately below).", Render3D.argb(255, 255, 255, 96)));
    private final BoolSetting notifications = add(new BoolSetting("Notifications", "Toast when a new shaft is flagged.", true));
    private final BoolSetting sound = add(new BoolSetting("Sound", "Plays a ping with the toast.", true));

    private final Long2ObjectOpenHashMap<ChunkCandidate> flagged = new Long2ObjectOpenHashMap<>();
    private final LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
    private final LongOpenHashSet queued = new LongOpenHashSet();
    private final List<ChunkCandidate> fresh = new ArrayList<>();
    private final Long2IntOpenHashMap heat = new Long2IntOpenHashMap();
    private int appliedRadius;
    private LongOpenHashSet shown = new LongOpenHashSet();
    private boolean shownDirty = true;
    private int shownAge;
    private ClientWorld lastLevel;
    private boolean scanning;
    private HeightBand appliedConfig;
    private WeakReference<ClientPlayNetworkHandler> probeServer = new WeakReference<>(null);
    private boolean probing;
    private final LongOpenHashSet probed = new LongOpenHashSet();

    // ---- scan scratch state ----
    private final WorldChunk[] around = new WorldChunk[9];
    private final int[] groupX = new int[4];
    private final int[] groupZ = new int[4];
    private int groupSize;
    private boolean guessed;
    private Block wall;
    private Block lastBlock;
    private final int[] stackTop = new int[256];
    private final int[] stackShape = new int[256];
    private final int[] stackAreas = new int[256];
    private final boolean[] stackGuessed = new boolean[256];
    private final Block[] stackWall = new Block[256];
    private boolean sawBlob;
    private int scanX;
    private int scanZ;
    private int keepHeight;
    private long[] keptStacks = new long[16];
    private int keptStackCount;
    private int tallest;
    private long bottom;
    private boolean unsure;

    public ChunkFinderBetterModule() {
        super(Category.DONUT, "Chunk Finder Better", "Krispy's Chunk Finder Better: scans suspicious back-filled shafts with Calm or Aggressive detection.");
        sound.visibleWhen(notifications::get);
    }

    @Override
    protected void onEnable() {
        startLevel(mc.world);
    }

    @Override
    protected void onDisable() {
        startLevel(null);
    }

    private void startLevel(ClientWorld world) {
        flagged.clear();
        heat.clear();
        appliedRadius = spreadRadius.getInt();
        queue.clear();
        queued.clear();
        fresh.clear();
        shown = new LongOpenHashSet();
        shownDirty = true;
        shownAge = 0;
        lastLevel = world;
        scanning = world != null && world.getRegistryKey() != World.NETHER && world.getRegistryKey() != World.END;

        if (scanning) {
            ClientPlayNetworkHandler handler = mc.getNetworkHandler();
            if (handler != probeServer.get()) {
                probeServer = new WeakReference<>(handler);
                probing = true;
                probed.clear();
            }

            appliedConfig = currentConfig(world);
            queueLoadedChunks(world);
        }
    }

    private HeightBand currentConfig(ClientWorld world) {
        return new HeightBand(Math.max(0, world.getBottomY()), Math.min(32, world.getTopYInclusive()), detection.is("Aggressive") ? 3 : 2);
    }

    private void queueLoadedChunks(ClientWorld world) {
        if (mc.player == null) return;

        int radius = mc.options.getClampedViewDistance() + 1;
        ChunkPos center = mc.player.getChunkPos();
        for (int x = center.x - radius; x <= center.x + radius; x++) {
            for (int z = center.z - radius; z <= center.z + radius; z++) {
                if (world.isChunkLoaded(x, z)) enqueue(ChunkPos.toLong(x, z));
            }
        }
    }

    private void enqueue(long key) {
        if (scanning && queued.add(key)) queue.enqueue(key);
    }

    // ---------- packets ----------

    private void onChunkData(ChunkDataS2CPacket packet) {
        shownDirty = true;
        int cx = packet.getChunkX();
        int cz = packet.getChunkZ();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                enqueue(ChunkPos.toLong(cx + dx, cz + dz));
            }
        }
    }

    private void onBlockUpdate(BlockUpdateEvent event) {
        if (!scanning || event.pos == null) return;
        int y = event.pos.getY();
        if (y < appliedConfig.minY() - 1 || y > appliedConfig.maxY() + 1) return;
        // Chukl's BlockUpdateEvent only carries the new state (no before/after tracking like Krispy's own
        // event bus), so this is a reasonable approximation of the original's "did this block change matter" check.
        if (!matters(event.newState)) return;

        int x = event.pos.getX();
        int z = event.pos.getZ();
        for (int cx = (x - 2) >> 4; cx <= (x + 1) >> 4; cx++) {
            for (int cz = (z - 2) >> 4; cz <= (z + 2) >> 4; cz++) {
                enqueue(ChunkPos.toLong(cx, cz));
            }
        }
    }

    // ---------- tick ----------

    private void onTick(TickEvent.Post event) {
        ClientWorld world = mc.world;
        if (world != lastLevel) startLevel(world);
        if (!scanning) return;

        HeightBand freshConfig = currentConfig(world);
        if (!freshConfig.equals(appliedConfig)) {
            appliedConfig = freshConfig;
            queueLoadedChunks(world);
        }

        int radius = spreadRadius.getInt();
        if (radius != appliedRadius) {
            appliedRadius = radius;
            shownDirty = true;
            heat.clear();
            ObjectIterator<Long2ObjectMap.Entry<ChunkCandidate>> it = flagged.long2ObjectEntrySet().fastIterator();
            while (it.hasNext()) {
                Long2ObjectMap.Entry<ChunkCandidate> entry = it.next();
                spread(entry.getLongKey(), entry.getValue(), 1);
            }
        }

        long deadline = System.nanoTime() + 3_000_000L;
        while (!queue.isEmpty() && System.nanoTime() < deadline) {
            long key = queue.dequeueLong();
            queued.remove(key);
            int x = ChunkPos.getPackedX(key);
            int z = ChunkPos.getPackedZ(key);
            if (!world.isChunkLoaded(x, z)) continue;

            ChunkCandidate candidate = scan(world, x, z, appliedConfig);
            if (sawBlob) probe(key);

            if (candidate != UNDECIDED) {
                ChunkCandidate previous = flagged.get(key);
                boolean changed = candidate == null ? previous != null : previous == null || !previous.sameAs(candidate);
                if (changed) {
                    refile(key, previous, candidate);
                    if (previous == null && candidate != null) fresh.add(candidate);
                }
            }
        }

        announce(fresh);
        fresh.clear();

        if (shownDirty || ++shownAge >= 20) {
            shown = computeShown(world);
            shownDirty = false;
            shownAge = 0;
        }
    }

    private void refile(long key, ChunkCandidate oldValue, ChunkCandidate newValue) {
        shownDirty = true;
        if (oldValue != null) spread(key, oldValue, -1);

        if (newValue == null) {
            flagged.remove(key);
        } else {
            flagged.put(key, newValue);
            spread(key, newValue, 1);
        }
    }

    private void spread(long key, ChunkCandidate candidate, int sign) {
        int delta = sign * candidate.stacks().length;
        int radius = appliedRadius;
        int cx = ChunkPos.getPackedX(key);
        int cz = ChunkPos.getPackedZ(key);

        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int z = cz - radius; z <= cz + radius; z++) {
                long pos = ChunkPos.toLong(x, z);
                if (heat.addTo(pos, delta) + delta == 0) heat.remove(pos);
            }
        }
    }

    private LongOpenHashSet computeShown(ClientWorld world) {
        Long2IntOpenHashMap loaded = new Long2IntOpenHashMap();
        ObjectIterator<Long2IntMap.Entry> it = heat.long2IntEntrySet().iterator();
        while (it.hasNext()) {
            Long2IntMap.Entry entry = it.next();
            long key = entry.getLongKey();
            if (world.isChunkLoaded(ChunkPos.getPackedX(key), ChunkPos.getPackedZ(key))) {
                loaded.put(key, entry.getIntValue());
            }
        }
        return appliedRadius == 0 ? new LongOpenHashSet(loaded.keySet()) : hottestPerPatch(loaded);
    }

    private static LongOpenHashSet hottestPerPatch(Long2IntOpenHashMap heatMap) {
        LongOpenHashSet result = new LongOpenHashSet();
        LongOpenHashSet visited = new LongOpenHashSet();
        LongArrayList patch = new LongArrayList();
        LongArrayFIFOQueue frontier = new LongArrayFIFOQueue();
        LongIterator it = heatMap.keySet().iterator();

        while (it.hasNext()) {
            long start = it.nextLong();
            if (!visited.add(start)) continue;

            patch.clear();
            frontier.enqueue(start);
            int max = 0;

            while (!frontier.isEmpty()) {
                long current = frontier.dequeueLong();
                patch.add(current);
                max = Math.max(max, heatMap.get(current));
                int x = ChunkPos.getPackedX(current);
                int z = ChunkPos.getPackedZ(current);

                for (int i = 0; i < 4; i++) {
                    long neighbor = ChunkPos.toLong(x + SIDE_X[i], z + SIDE_Z[i]);
                    if (heatMap.containsKey(neighbor) && visited.add(neighbor)) frontier.enqueue(neighbor);
                }
            }

            for (int i = 0; i < patch.size(); i++) {
                if (heatMap.get(patch.getLong(i)) == max) result.add(patch.getLong(i));
            }
        }
        return result;
    }

    // ---------- shaft scan ----------

    private ChunkCandidate scan(ClientWorld world, int chunkX, int chunkZ, HeightBand band) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                around[(dx + 1) * 3 + dz + 1] = world.getChunkManager().getWorldChunk(chunkX + dx, chunkZ + dz);
            }
        }

        sawBlob = false;
        scanX = chunkX << 4;
        scanZ = chunkZ << 4;
        keepHeight = band.stackHeight();
        keptStackCount = 0;
        tallest = 0;
        bottom = 0L;
        unsure = false;

        ChunkCandidate result;
        try {
            WorldChunk center = around[4];
            ChunkSection[] sections = center.getSectionArray();
            int bottomSection = center.getBottomSectionCoord();
            Arrays.fill(stackTop, Integer.MIN_VALUE);
            boolean noBlocksInBand = true;

            for (int i = 0; i < sections.length; i++) {
                ChunkSection section = sections[i];
                if (section == null || section.isEmpty()) continue;

                int sectionY = (bottomSection + i) << 4;
                if (sectionY + 15 < band.minY() || sectionY > band.maxY()) continue;

                int loY = Math.max(band.minY() - sectionY, 0);
                int hiY = Math.min(band.maxY() - sectionY, 15);
                if (noBlocksInBand) {
                    noBlocksInBand = !hasBlocks(section, loY, hiY);
                }

                if (section.hasAny(s -> s.isOf(Blocks.STONE)) && section.hasAny(s -> isBlob(s.getBlock()))) {
                    for (int ly = loY; ly <= hiY; ly++) {
                        for (int lz = 0; lz < 16; lz++) {
                            boolean prevWasStone = false;

                            for (int lx = 0; lx < 16; lx++) {
                                Block block = section.getBlockState(lx, ly, lz).getBlock();
                                boolean isStone = block == Blocks.STONE;
                                if (isStone) {
                                    if (!prevWasStone && isArea(lx, sectionY + ly, lz)) {
                                        stack(lx, sectionY + ly, lz);
                                    }
                                } else if (probing && isBlob(block)) {
                                    sawBlob = true;
                                    if (enclosed(lx, sectionY + ly, lz)) {
                                        probing = false;
                                        probed.clear();
                                    }
                                }
                                prevWasStone = isStone;
                            }
                        }
                    }
                }
            }

            for (int i = 0; i < 256; i++) keep(i);

            if (tallest >= band.stackHeight()) {
                return new ChunkCandidate(bottom, tallest, Arrays.copyOf(keptStacks, keptStackCount));
            }

            result = (!unsure && !noBlocksInBand) ? null : UNDECIDED;
        } finally {
            Arrays.fill(around, null);
        }
        return result;
    }

    private void stack(int x, int y, int z) {
        int idx = (z << 4) | x;
        int shape = shape(x, z);
        boolean continued = stackTop[idx] == y - 1 && stackShape[idx] == shape && stackWall[idx] == wall;
        if (!continued) keep(idx);

        stackTop[idx] = y;
        stackShape[idx] = shape;
        stackWall[idx] = wall;
        stackAreas[idx] = continued ? stackAreas[idx] + 1 : 1;
        stackGuessed[idx] = guessed || (continued && stackGuessed[idx]);
    }

    private void keep(int idx) {
        int area = stackAreas[idx];
        int top = stackTop[idx];
        if (top == Integer.MIN_VALUE || area < keepHeight) return;

        int shape = stackShape[idx];
        if (!goesOn(idx & 15, top + 1, idx >> 4, shape) || !goesOn(idx & 15, top - area, idx >> 4, shape)) return;

        if (stackGuessed[idx]) {
            unsure = true;
            return;
        }

        int x = scanX + (idx & 15);
        int z = scanZ + (idx >> 4);
        long pos = BlockPos.asLong(x, top - area + 1, z);
        if (area > tallest || (area == tallest && before(pos, bottom))) {
            tallest = area;
            bottom = pos;
        }

        if (keptStackCount == keptStacks.length) {
            keptStacks = Arrays.copyOf(keptStacks, keptStacks.length << 1);
        }
        keptStacks[keptStackCount++] = pos;
    }

    private boolean goesOn(int x, int y, int z, int shape) {
        for (int i = 0; i < 6; i++) {
            if (((shape >> i) & 1) != 0) {
                kind(x + i / 3, y, z + i % 3 - 1);
                if (!carriesStone(lastBlock)) return false;
            }
        }
        return true;
    }

    private static boolean before(long a, long b) {
        if (BlockPos.unpackLongY(a) != BlockPos.unpackLongY(b)) return BlockPos.unpackLongY(a) < BlockPos.unpackLongY(b);
        if (BlockPos.unpackLongZ(a) != BlockPos.unpackLongZ(b)) return BlockPos.unpackLongZ(a) < BlockPos.unpackLongZ(b);
        return BlockPos.unpackLongX(a) < BlockPos.unpackLongX(b);
    }

    private int shape(int x, int z) {
        int shape = 0;
        for (int i = 0; i < groupSize; i++) {
            shape |= 1 << ((groupX[i] - x) * 3 + groupZ[i] - z + 1);
        }
        return shape;
    }

    private boolean isArea(int x, int y, int z) {
        guessed = false;
        wall = null;
        if (seen(x - 1, y, z) != 2 || !sameWall() || seen(x, y, z - 1) != 2 || !sameWall()) return false;

        groupX[0] = x;
        groupZ[0] = z;
        int count = 1;
        int maxX = x, minZ = z, maxZ = z;

        for (int i = 0; i < count; i++) {
            for (int s = 0; s < 4; s++) {
                int nx = groupX[i] + SIDE_X[s];
                int nz = groupZ[i] + SIDE_Z[s];
                if (inGroup(nx, nz, count)) continue;

                byte kind = seen(nx, y, nz);
                if (kind == 2) {
                    if (!sameWall()) return false;
                } else {
                    if (kind != 1) return false;
                    if (nx < x || (nx == x && nz < z)) return false;

                    maxX = Math.max(maxX, nx);
                    minZ = Math.min(minZ, nz);
                    maxZ = Math.max(maxZ, nz);
                    if (maxX - x > 1 || maxZ - minZ > 1) return false;

                    groupX[count] = nx;
                    groupZ[count] = nz;
                    count++;
                }
            }
        }

        for (int i = 0; i < count; i++) {
            for (int c = 0; c < 4; c++) {
                int cx = groupX[i] + CORNER_X[c];
                int cz = groupZ[i] + CORNER_Z[c];
                if (!inGroup(cx, cz, count) && (seen(cx, y, cz) != 2 || !sameWall())) return false;
            }
        }

        for (int i = 0; i < count; i++) {
            if (kind(groupX[i], y + 1, groupZ[i]) == AIR || kind(groupX[i], y - 1, groupZ[i]) == AIR) return false;
        }

        groupSize = count;
        return true;
    }

    private boolean inGroup(int x, int z, int count) {
        for (int i = 0; i < count; i++) {
            if (groupX[i] == x && groupZ[i] == z) return true;
        }
        return false;
    }

    private byte seen(int x, int y, int z) {
        byte kind = kind(x, y, z);
        if (kind != MISSING) return kind;
        guessed = true;
        return 2;
    }

    private boolean sameWall() {
        if (lastBlock == null) return true;
        if (wall == null) wall = lastBlock;
        return lastBlock == wall;
    }

    private byte kind(int x, int y, int z) {
        WorldChunk chunk = around[((x >> 4) + 1) * 3 + (z >> 4) + 1];
        lastBlock = null;
        if (chunk == null) return MISSING;

        int index = chunk.getSectionIndex(y);
        if (index < 0 || index >= chunk.countVerticalSections()) return OTHER;

        BlockState state = chunk.getSection(index).getBlockState(x & 15, y & 15, z & 15);
        lastBlock = state.getBlock();
        return kindOf(state);
    }

    private boolean enclosed(int x, int y, int z) {
        return full(x + 1, y, z) && full(x - 1, y, z) && full(x, y, z + 1) && full(x, y, z - 1) && full(x, y + 1, z) && full(x, y - 1, z);
    }

    private boolean full(int x, int y, int z) {
        WorldChunk chunk = around[((x >> 4) + 1) * 3 + (z >> 4) + 1];
        if (chunk == null) return false;

        int index = chunk.getSectionIndex(y);
        return index >= 0 && index < chunk.countVerticalSections() && chunk.getSection(index).getBlockState(x & 15, y & 15, z & 15).isOpaqueFullCube();
    }

    private static boolean hasBlocks(ChunkSection section, int loY, int hiY) {
        if (!section.hasAny(s -> !s.isAir())) return false;

        for (int y = loY; y <= hiY; y++) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    if (!section.getBlockState(x, y, z).isAir()) return true;
                }
            }
        }
        return false;
    }

    private static byte kindOf(BlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.STONE) return STONE;
        if (isBlob(block)) return BLOB;
        return state.isAir() ? AIR : OTHER;
    }

    private static boolean isBlob(Block block) {
        return block == Blocks.DIORITE || block == Blocks.ANDESITE || block == Blocks.GRANITE || block == Blocks.GRAVEL;
    }

    private static boolean carriesStone(Block block) {
        return block == Blocks.STONE || block == Blocks.DEEPSLATE;
    }

    private static boolean matters(BlockState state) {
        return state != null && (kindOf(state) != OTHER || carriesStone(state.getBlock()));
    }

    private void probe(long key) {
        if (!probing || !probed.add(key) || probed.size() < PROBE_CHUNKS) return;

        probing = false;
        probed.clear();
        if (mc.world != null && mc.player != null) {
            mc.player.sendMessage(Text.literal("Krispy: This server hides ores with anti-xray; patched shafts may not be visible."), false);
        }
    }

    private void announce(List<ChunkCandidate> newlyFlagged) {
        if (!notifications.get() || newlyFlagged.isEmpty()) return;

        ChunkCandidate first = newlyFlagged.get(0);
        int x = BlockPos.unpackLongX(first.pos());
        int z = BlockPos.unpackLongZ(first.pos());

        SystemToast.add(mc.getToastManager(), SystemToast.Type.PERIODIC_NOTIFICATION,
            Text.literal("Chunk Finder Better"), Text.literal("X: " + x + " Z: " + z));

        if (sound.get()) {
            mc.getSoundManager().play(PositionedSoundInstance.ui(PING, 1.0F, 1.0F));
        }
    }

    // ---------- render ----------

    private void onRender(Render3DEvent event) {
        if (mc.world == null) return;

        int a = alpha.getInt();
        int r = chunkColor.channel(0), g = chunkColor.channel(1), b = chunkColor.channel(2);
        SettingColor fill = new SettingColor(r, g, b, a);
        SettingColor edge = new SettingColor(r, g, b, Math.min(220, a + 45));
        ChunkPlateRenderer.draw(event, shown, fill, edge, 1.5F, 63.0, 0.025);
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
        } else if (packet instanceof BlockUpdateS2CPacket bu) {
            onBlockUpdate(new BlockUpdateEvent(bu.getPos(), bu.getState()));
        } else if (packet instanceof ChunkDeltaUpdateS2CPacket cd) {
            cd.visitUpdates((pos, state) -> onBlockUpdate(new BlockUpdateEvent(pos.toImmutable(), state)));
        }
    }
}
