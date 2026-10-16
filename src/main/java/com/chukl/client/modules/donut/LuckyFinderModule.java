package com.chukl.client.modules.donut;

import com.chukl.client.compat.Events.TickEvent;
import com.chukl.client.compat.MeteorModule;
import com.chukl.client.compat.Render3DEvent;
import com.chukl.client.compat.SettingColor;
import com.chukl.client.compat.ShapeMode;
import com.chukl.client.core.Category;
import com.chukl.client.core.Render3D;
import com.chukl.client.settings.BoolSetting;
import com.chukl.client.settings.NumberSetting;

import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.WorldChunk;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Krispy's Lucky Finder ("Follow your luck"). Ported straight from Krispy: on an irregular 30-120s timer it
 * picks a random loaded chunk within a capped radius of you and drops a plate there, with a toast/ping like a
 * real finder -- there's no underlying detection, it's a plausible-looking chunk flagged at random.
 */
public class LuckyFinderModule extends MeteorModule {
    private static final int PLATE_SIZE = 48;
    private static final double PLATE_THICKNESS = 0.15;
    private static final SoundEvent FLAG_SOUND = SoundEvent.of(Identifier.of("minecraft", "entity.experience_orb.pickup"));

    private final BoolSetting notifications = add(new BoolSetting("Notifications", "Toast when a chunk is flagged.", true));
    private final BoolSetting sound = add(new BoolSetting("Sound", "Plays a ping when a chunk is flagged.", true));
    private final NumberSetting volume = add(new NumberSetting("Sound Volume", "Volume of the ping (0-100).", 35.0, 0, 100, 1.0));
    private final NumberSetting alpha = add(new NumberSetting("Alpha", "Plate opacity (0-255).", 75.0, 0, 255, 1.0));

    private final Deadline timer = new Deadline();
    private final Map<Long, ChunkMarker> flags = new LinkedHashMap<>();

    private ClientWorld trackedWorld;
    private String flagWorldKey;
    private int lastChunkX, lastChunkZ, lastViewDistance = -1;

    private record ChunkMarker(int cx, int cz, double y) { }

    public LuckyFinderModule() {
        super(Category.DONUT, "Lucky Finder", "Krispy's Lucky Finder. Follow your luck.");
    }

    @Override
    protected void onEnable() {
        reset();
        if (mc.world != null && mc.player != null) timer.arm(System.nanoTime(), ThreadLocalRandom.current());
    }

    @Override
    protected void onDisable() {
        reset();
    }

    private void reset() {
        timer.clear();
        flags.clear();
        trackedWorld = null;
        flagWorldKey = null;
        lastViewDistance = -1;
    }

    private void onTick(TickEvent.Post event) {
        if (mc.world == null || mc.player == null) {
            trackedWorld = null;
            return;
        }

        if (trackedWorld != mc.world) {
            String currentWorldKey = worldKey();
            if (!currentWorldKey.equals(flagWorldKey)) {
                flags.clear();
                flagWorldKey = currentWorldKey;
            }
            trackedWorld = mc.world;
            lastViewDistance = -1;
        }

        pruneFlags();
        long now = System.nanoTime();
        if (!timer.isArmed()) {
            timer.arm(now, ThreadLocalRandom.current());
            return;
        }
        if (!timer.due(now)) return;

        if (tryFlag()) timer.arm(now, ThreadLocalRandom.current());
        else timer.retry(now);
    }

    private String worldKey() {
        String location;
        ServerInfo server = mc.getCurrentServerEntry();
        if (server != null) {
            location = "server:" + server.address.toLowerCase(Locale.ROOT);
        } else if (mc.getServer() == null) {
            location = "session:" + System.identityHashCode(mc.world);
        } else {
            IntegratedServer integrated = mc.getServer();
            location = "local:" + integrated.getSavePath(WorldSavePath.ROOT).toAbsolutePath().normalize();
        }
        return location + "|" + mc.world.getRegistryKey().getValue();
    }

    private void pruneFlags() {
        if (flags.isEmpty()) return;

        ChunkPos playerChunk = mc.player.getChunkPos();
        if (!mc.world.isChunkLoaded(playerChunk.x, playerChunk.z)) return;

        int viewDistance = mc.options.getClampedViewDistance();
        if (lastChunkX == playerChunk.x && lastChunkZ == playerChunk.z && lastViewDistance == viewDistance) return;

        lastChunkX = playerChunk.x;
        lastChunkZ = playerChunk.z;
        lastViewDistance = viewDistance;
        flags.values().removeIf(marker ->
            Math.abs((long) marker.cx() - playerChunk.x) > viewDistance || Math.abs((long) marker.cz() - playerChunk.z) > viewDistance);
    }

    private boolean tryFlag() {
        ChunkPos playerChunk = mc.player.getChunkPos();
        int radius = Math.max(0, Math.min(8, mc.options.getClampedViewDistance() - 2));
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int attempt = 0; attempt < 32; attempt++) {
            int chunkX = playerChunk.x + random.nextInt(-radius, radius + 1);
            int chunkZ = playerChunk.z + random.nextInt(-radius, radius + 1);
            long key = ChunkPos.toLong(chunkX, chunkZ);
            if (attempt < 24 && flags.containsKey(key)) continue;

            int surfaceY = highestSurface(chunkX, chunkZ);
            if (surfaceY == Integer.MIN_VALUE) continue;

            flags.put(key, new ChunkMarker(chunkX, chunkZ, surfaceY + 1.002));
            int centerX = (chunkX << 4) + 8;
            int centerZ = (chunkZ << 4) + 8;

            if (notifications.get()) {
                SystemToast.add(mc.getToastManager(), SystemToast.Type.PERIODIC_NOTIFICATION,
                    Text.literal("Lucky Finder"), Text.literal("X: " + centerX + " Z: " + centerZ));
            }
            if (sound.get() && volume.get() > 0.0) {
                mc.getSoundManager().play(PositionedSoundInstance.ui(FLAG_SOUND, 1.0F, volume.getFloat() / 100.0F));
            }
            return true;
        }
        return false;
    }

    private int highestSurface(int chunkX, int chunkZ) {
        for (int ox = -1; ox <= 1; ox++) {
            for (int oz = -1; oz <= 1; oz++) {
                if (!mc.world.isChunkLoaded(chunkX + ox, chunkZ + oz)) return Integer.MIN_VALUE;
            }
        }

        int highest = Integer.MIN_VALUE;
        for (int ox = -1; ox <= 1; ox++) {
            for (int oz = -1; oz <= 1; oz++) {
                WorldChunk chunk = mc.world.getChunk(chunkX + ox, chunkZ + oz);
                for (int lx = 0; lx < 16; lx++) {
                    for (int lz = 0; lz < 16; lz++) {
                        highest = Math.max(highest, chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE, lx, lz));
                    }
                }
            }
        }
        return highest < mc.world.getBottomY() ? Integer.MIN_VALUE : highest;
    }

    private void onRender(Render3DEvent event) {
        if (mc.player == null || trackedWorld != mc.world || flags.isEmpty()) return;

        int a = alpha.getInt();
        if (a <= 0) return;

        SettingColor fill = new SettingColor(0, 255, 70, a);
        SettingColor line = new SettingColor(0, 255, 70, Math.min(255, a + 60));
        double half = PLATE_SIZE / 2.0;
        int range = mc.options.getClampedViewDistance() + 2;
        ChunkPos here = mc.player.getChunkPos();

        for (ChunkMarker marker : flags.values()) {
            if (Math.abs(marker.cx() - here.x) > range || Math.abs(marker.cz() - here.z) > range) continue;

            double cx = marker.cx() * 16.0 + 8.0;
            double cz = marker.cz() * 16.0 + 8.0;
            event.renderer.box(cx - half, marker.y(), cz - half, cx + half, marker.y() + PLATE_THICKNESS, cz + half, fill, line, ShapeMode.Both, 0);
        }
    }

    @Override
    protected void onTick() {
        onTick(new TickEvent.Post());
    }

    @Override
    protected void onRender3D(Render3D r3d) {
        onRender(new Render3DEvent(r3d));
    }

    /** Ported from Krispy's RetryDeadline: an irregular retry timer so flags don't appear on a predictable beat. */
    private static final class Deadline {
        private long deadline;
        private boolean armed;

        void arm(long now, ThreadLocalRandom random) {
            deadline = now + random.nextLong(30_000_000_000L, 120_000_000_001L);
            armed = true;
        }

        void retry(long now) {
            deadline = now + 1_000_000_000L;
            armed = true;
        }

        boolean isArmed() { return armed; }
        boolean due(long now) { return armed && now - deadline >= 0; }
        void clear() { armed = false; }
    }
}
