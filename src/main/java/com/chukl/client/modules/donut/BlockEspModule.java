package com.chukl.client.modules.donut;

import com.chukl.client.core.Category;
import com.chukl.client.core.Module;
import com.chukl.client.core.Render3D;
import com.chukl.client.render.EspBoxRenderer;
import com.chukl.client.render.IncrementalScan;
import com.chukl.client.settings.BoolSetting;
import com.chukl.client.settings.ColorSetting;
import com.chukl.client.settings.ModeSetting;
import com.chukl.client.settings.NumberSetting;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.joml.Vector3fc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 67Client's Block ESP: highlights chosen blocks through walls. 67Client's version lets you search the whole
 * block registry and add/remove any block through its own GUI (BlockListSetting); Chukl doesn't have that
 * picker, so the default target list it ships with (seedDefaults(): ores, ancient debris, spawner, end
 * portal frame, chest) is exposed as fixed Bool + Color setting pairs instead, same as Storage ESP.
 */
public class BlockEspModule extends Module {
    private record Target(String label, Block block, boolean defaultOn, int defaultColor) { }

    private static final Target[] TARGETS = {
        new Target("Diamond Ore", Blocks.DIAMOND_ORE, true, -16711736),
        new Target("Deepslate Diamond Ore", Blocks.DEEPSLATE_DIAMOND_ORE, true, -16711736),
        new Target("Emerald Ore", Blocks.EMERALD_ORE, false, -16711868),
        new Target("Deepslate Emerald Ore", Blocks.DEEPSLATE_EMERALD_ORE, false, -16711868),
        new Target("Ancient Debris", Blocks.ANCIENT_DEBRIS, true, -39356),
        new Target("Nether Gold Ore", Blocks.NETHER_GOLD_ORE, false, -10496),
        new Target("Gold Ore", Blocks.GOLD_ORE, false, -10496),
        new Target("Deepslate Gold Ore", Blocks.DEEPSLATE_GOLD_ORE, false, -10496),
        new Target("Iron Ore", Blocks.IRON_ORE, false, -3618616),
        new Target("Deepslate Iron Ore", Blocks.DEEPSLATE_IRON_ORE, false, -3618616),
        new Target("Coal Ore", Blocks.COAL_ORE, false, -12303292),
        new Target("Deepslate Coal Ore", Blocks.DEEPSLATE_COAL_ORE, false, -12303292),
        new Target("Copper Ore", Blocks.COPPER_ORE, false, -4689101),
        new Target("Deepslate Copper Ore", Blocks.DEEPSLATE_COPPER_ORE, false, -4689101),
        new Target("Lapis Ore", Blocks.LAPIS_ORE, false, -12490271),
        new Target("Deepslate Lapis Ore", Blocks.DEEPSLATE_LAPIS_ORE, false, -12490271),
        new Target("Redstone Ore", Blocks.REDSTONE_ORE, false, -65536),
        new Target("Deepslate Redstone Ore", Blocks.DEEPSLATE_REDSTONE_ORE, false, -65536),
        new Target("Spawner", Blocks.SPAWNER, false, -7846657),
        new Target("End Portal Frame", Blocks.END_PORTAL_FRAME, false, -12255250),
        new Target("Chest", Blocks.CHEST, false, -22016),
    };

    private static final int MAX_CHUNK_RADIUS = 12;
    private static final int MAX_RESULTS = 8000;
    private static final double INSET = 0.002;

    private final ModeSetting shapeMode = add(new ModeSetting("Shape Mode", "How highlights are drawn", "Both", "Both", "Lines", "Sides"));
    private final BoolSetting tracers = add(new BoolSetting("Tracers", "Draw lines from the camera to each block", false));
    private final NumberSetting highlightAlpha = add(new NumberSetting("Highlight Alpha", "Box opacity", 255, 0, 255, 5));
    private final NumberSetting rangeExtraChunks = add(new NumberSetting("Range Extra Chunks", "Extra scan radius beyond render distance", 1, 0, 4, 1));

    private final BoolSetting[] enabled = new BoolSetting[TARGETS.length];
    private final ColorSetting[] color = new ColorSetting[TARGETS.length];

    private final IncrementalScan<Hit> scan = new IncrementalScan<>(48, 80_000, 20);
    private Set<Block> lastWanted = Set.of();

    private record Hit(int x, int y, int z, Block block) { }

    public BlockEspModule() {
        super(Category.DONUT, "BlockESP", "67Client's Block ESP. Highlights chosen blocks through walls.");
        for (Target t : TARGETS) {
            enabled[indexOf(t)] = add(new BoolSetting(t.label(), "Highlight " + t.label() + ".", t.defaultOn()));
            color[indexOf(t)] = add(new ColorSetting(t.label() + " Color", "", t.defaultColor() | 0xFF000000));
            color[indexOf(t)].visibleWhen(enabled[indexOf(t)]::get);
        }
    }

    private static int indexOf(Target t) {
        for (int i = 0; i < TARGETS.length; i++) if (TARGETS[i] == t) return i;
        return -1;
    }

    @Override
    protected void onDisable() {
        scan.clear();
        lastWanted = Set.of();
    }

    @Override
    protected void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        Set<Block> wanted = new HashSet<>();
        for (int i = 0; i < TARGETS.length; i++) if (enabled[i].get()) wanted.add(TARGETS[i].block());

        if (wanted.isEmpty()) {
            scan.clear();
            lastWanted = Set.of();
            return;
        }

        if (!wanted.equals(lastWanted)) {
            lastWanted = wanted;
            scan.markDirty();
        }

        int extra = rangeExtraChunks.getInt();
        int chunkRadius = Math.min(MAX_CHUNK_RADIUS, mc.options.getClampedViewDistance() + extra);
        scan.tick(chunkRadius, (chunk, out) -> scanChunk(chunk, wanted, out));
    }

    private static int scanChunk(WorldChunk chunk, Set<Block> wanted, List<Hit> out) {
        ChunkSection[] sections = chunk.getSectionArray();
        int minSectionY = chunk.getBottomSectionCoord();
        int baseX = chunk.getPos().getStartX();
        int baseZ = chunk.getPos().getStartZ();
        int blocks = 0;
        if (out.size() >= MAX_RESULTS) return 0;

        for (int s = 0; s < sections.length; s++) {
            ChunkSection section = sections[s];
            if (section.isEmpty() || !section.hasAny(st -> wanted.contains(st.getBlock()))) continue;

            int baseY = (minSectionY + s) << 4;
            blocks += 4096;

            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        Block block = section.getBlockState(x, y, z).getBlock();
                        if (wanted.contains(block)) {
                            out.add(new Hit(baseX + x, baseY + y, baseZ + z, block));
                            if (out.size() >= MAX_RESULTS) return blocks;
                        }
                    }
                }
            }
        }
        return blocks;
    }

    @Override
    protected void onRender3D(Render3D r) {
        List<Hit> snapshot = scan.get();
        if (snapshot.isEmpty()) return;

        Map<Block, Integer> colorByBlock = new HashMap<>();
        for (int i = 0; i < TARGETS.length; i++) colorByBlock.put(TARGETS[i].block(), color[i].get());

        int alpha = Math.max(0, Math.min(255, highlightAlpha.getInt()));
        boolean drawLines = shapeMode.is("Both") || shapeMode.is("Lines");
        boolean drawFill = shapeMode.is("Both") || shapeMode.is("Sides");
        boolean drawTracers = tracers.get();

        var bufferSource = r.buffers();
        var poseStack = r.matrices();
        var cam = r.camera;

        for (Hit hit : snapshot) {
            int rgb = colorByBlock.getOrDefault(hit.block(), -16711736) & 0xFFFFFF;
            int argb = rgb | (alpha << 24);
            if (drawFill) {
                EspBoxRenderer.fill(bufferSource, poseStack, cam, hit.x() + INSET, hit.y() + INSET, hit.z() + INSET,
                    hit.x() + 1 - INSET, hit.y() + 1 - INSET, hit.z() + 1 - INSET, argb);
            }
            if (drawLines) {
                EspBoxRenderer.outline(bufferSource, poseStack, cam, hit.x() + INSET, hit.y() + INSET, hit.z() + INSET,
                    hit.x() + 1 - INSET, hit.y() + 1 - INSET, hit.z() + 1 - INSET, argb, 1.6F);
            }
        }

        if (drawTracers) {
            Vector3fc forward = mc.gameRenderer.getCamera().getHorizontalPlane();
            for (Hit hit : snapshot) {
                int rgb = colorByBlock.getOrDefault(hit.block(), -16711736) & 0xFFFFFF;
                int col = rgb | (200 << 24);
                EspBoxRenderer.tracer(bufferSource, poseStack, cam, forward, hit.x() + 0.5, hit.y() + 0.5, hit.z() + 0.5, col, 1.2F);
            }
        }

        EspBoxRenderer.flush(bufferSource);
    }
}
