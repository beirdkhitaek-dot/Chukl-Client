package com.chukl.client.modules.render;

import com.chukl.client.core.Category;
import com.chukl.client.core.Module;
import com.chukl.client.core.Render3D;
import com.chukl.client.render.EspBoxRenderer;
import com.chukl.client.render.IncrementalScan;
import com.chukl.client.settings.BoolSetting;
import com.chukl.client.settings.ColorSetting;
import com.chukl.client.settings.ModeSetting;
import com.chukl.client.settings.NumberSetting;

import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BrewingStandBlock;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.SpawnerBlock;
import net.minecraft.block.TrappedChestBlock;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.joml.Vector3fc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 67Client's Storage ESP, replacing the earlier Krispy-based port and moved from Donut to Render (matching
 * 67Client's own category for it). Highlights chests, trapped chests, ender chests, shulkers, barrels,
 * spawners, hoppers and furnaces (brewing stands and dispensers share the furnace/hopper colors, the same
 * quirk 67Client's own classifier has) through walls, with an inset box for chest-family blocks that merges
 * across a double chest's other half, outline or filled render mode, tracers, and an opened-container filter.
 * Chukl has no equivalent of 67Client's IconListSetting picker GUI, so each type is a fixed Bool+Color pair
 * (same flattening used for the Donut Storage ESP and Block ESP ports).
 */
public class StorageEspModule extends Module {
    private enum StorageType {
        CHEST("Chest", 255, 170, 0, true),
        TRAPPED("Trapped Chest", 255, 0, 0, true),
        ENDER("Ender Chest", 120, 0, 255, true),
        SHULKER("Shulker Box", 255, 68, 255, true),
        BARREL("Barrel", 136, 85, 0, true),
        SPAWNER("Spawner", 0, 255, 0, true),
        HOPPER("Hopper", 136, 136, 136, false),
        FURNACE("Furnace", 140, 140, 140, false);

        final String label;
        final int r, g, b;
        final boolean defaultOn;

        StorageType(String label, int r, int g, int b, boolean defaultOn) {
            this.label = label;
            this.r = r;
            this.g = g;
            this.b = b;
            this.defaultOn = defaultOn;
        }
    }

    private record Hit(int x, int y, int z, StorageType type) { }

    private static final int MAX_CHUNK_RADIUS = 16;
    private static final int MAX_RESULTS = 6000;
    private static final float BOX_INFLATE = 0.002F;
    private static final double CHEST_INSET = 0.0625;
    private static final int INTERACTED_RGB = 0x645A64; // 6579300, matches 67Client's "opened" tint

    private final ModeSetting mode = add(new ModeSetting("Mode", "Box style.", "Outline", "Outline", "Full"));
    private final NumberSetting range = add(new NumberSetting("Range", "Max distance a container is highlighted.", 128, 16, 256, 8));
    private final NumberSetting highlightAlpha = add(new NumberSetting("Highlight Alpha", "Box opacity (0-255).", 200, 0, 255, 1));
    private final BoolSetting tracers = add(new BoolSetting("Tracers", "Draw lines from the camera to each container.", false));
    private final BoolSetting hideOpened = add(new BoolSetting("Hide Opened", "Don't highlight containers you've already opened.", false));

    private final BoolSetting[] enabled = new BoolSetting[StorageType.values().length];
    private final ColorSetting[] color = new ColorSetting[StorageType.values().length];

    private final IncrementalScan<Hit> scan = new IncrementalScan<>(48, 80_000, 20);
    private final Set<BlockPos> interactedBlocks = new HashSet<>();

    public StorageEspModule() {
        super(Category.RENDER, "StorageESP", "67Client's Storage ESP. Highlights chests, barrels, shulkers and more through walls.");
        for (StorageType t : StorageType.values()) {
            enabled[t.ordinal()] = add(new BoolSetting(t.label, "Highlight " + t.label + ".", t.defaultOn));
            color[t.ordinal()] = add(new ColorSetting(t.label + " Color", "", (t.r << 16) | (t.g << 8) | t.b | 0xFF000000));
            color[t.ordinal()].visibleWhen(enabled[t.ordinal()]::get);
        }
    }

    /** Marks a container (and its double-chest partner, if any) as already opened, for "Hide Opened". Nothing
     *  calls this automatically yet -- 67Client's own source only defines it without wiring a screen-open
     *  hook either, so this stays available for a future interaction hook rather than invented behaviour. */
    public void trackInteraction(BlockPos pos) {
        if (pos == null || mc.world == null) return;
        interactedBlocks.add(pos.toImmutable());

        BlockState state = mc.world.getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock && state.get(ChestBlock.CHEST_TYPE) != ChestType.SINGLE) {
            Direction facing = state.get(ChestBlock.FACING);
            ChestType type = state.get(ChestBlock.CHEST_TYPE);
            Direction toOther = type == ChestType.LEFT ? facing.rotateYClockwise() : facing.rotateYCounterclockwise();
            interactedBlocks.add(pos.offset(toOther));
        }
    }

    @Override
    protected void onEnable() {
        interactedBlocks.clear();
    }

    @Override
    protected void onDisable() {
        scan.clear();
    }

    @Override
    protected void onTick() {
        if (mc.world == null || mc.player == null) return;

        double rangeSq = range.get() * range.get();
        int wanted = (int) Math.ceil(range.get() / 16.0) + 1;
        int chunkRadius = Math.min(Math.min(MAX_CHUNK_RADIUS, wanted), mc.options.getClampedViewDistance());
        scan.tick(chunkRadius, (chunk, out) -> scanChunk(chunk, rangeSq, out));
    }

    private static int scanChunk(net.minecraft.world.chunk.WorldChunk chunk, double rangeSq, List<Hit> out) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return 0;

        net.minecraft.world.chunk.ChunkSection[] sections = chunk.getSectionArray();
        int minSectionY = chunk.getBottomSectionCoord();
        int baseX = chunk.getPos().getStartX();
        int baseZ = chunk.getPos().getStartZ();
        int blocks = 0;
        if (out.size() >= MAX_RESULTS) return 0;

        for (int s = 0; s < sections.length; s++) {
            net.minecraft.world.chunk.ChunkSection section = sections[s];
            if (section.isEmpty() || !section.hasAny(st -> classify(st.getBlock()) != null)) continue;

            int baseY = (minSectionY + s) << 4;
            blocks += 4096;

            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        StorageType type = classify(section.getBlockState(x, y, z).getBlock());
                        if (type == null) continue;

                        int wx = baseX + x, wy = baseY + y, wz = baseZ + z;
                        if (mc.player.squaredDistanceTo(wx + 0.5, wy + 0.5, wz + 0.5) > rangeSq) continue;

                        out.add(new Hit(wx, wy, wz, type));
                        if (out.size() >= MAX_RESULTS) return blocks;
                    }
                }
            }
        }
        return blocks;
    }

    private static StorageType classify(Block b) {
        if (b instanceof TrappedChestBlock) return StorageType.TRAPPED;
        if (b instanceof ChestBlock) return StorageType.CHEST;
        if (b instanceof EnderChestBlock) return StorageType.ENDER;
        if (b instanceof ShulkerBoxBlock) return StorageType.SHULKER;
        if (b instanceof BarrelBlock) return StorageType.BARREL;
        if (b instanceof SpawnerBlock) return StorageType.SPAWNER;
        if (b instanceof HopperBlock) return StorageType.HOPPER;
        if (b instanceof AbstractFurnaceBlock) return StorageType.FURNACE;
        if (b instanceof BrewingStandBlock) return StorageType.FURNACE;
        if (b instanceof DispenserBlock) return StorageType.HOPPER;
        return null;
    }

    @Override
    protected void onRender3D(Render3D r) {
        List<Hit> snapshot = scan.get();
        if (snapshot.isEmpty() || mc.world == null) return;

        boolean fill = mode.is("Full");
        int alphaBits = Math.max(0, Math.min(255, highlightAlpha.getInt())) << 24;
        boolean drawTracers = tracers.get();
        boolean hide = hideOpened.get();

        var bufferSource = r.buffers();
        var poseStack = r.matrices();
        var cam = r.camera;
        Vector3fc forward = mc.gameRenderer.getCamera().getHorizontalPlane();

        Map<StorageType, Integer> colorByType = new HashMap<>();
        for (StorageType t : StorageType.values()) colorByType.put(t, color[t.ordinal()].get());

        for (Hit hit : snapshot) {
            if (!enabled[hit.type().ordinal()].get()) continue;

            boolean interacted = !interactedBlocks.isEmpty() && interactedBlocks.contains(new BlockPos(hit.x(), hit.y(), hit.z()));
            if (interacted && hide) continue;

            int rgb = (interacted ? INTERACTED_RGB : colorByType.get(hit.type())) & 0xFFFFFF;
            int argb = alphaBits | rgb;

            double x1 = hit.x(), y1 = hit.y(), z1 = hit.z();
            double x2 = x1 + 1.0, y2 = y1 + 1.0, z2 = z1 + 1.0;

            if (hit.type() == StorageType.CHEST || hit.type() == StorageType.TRAPPED || hit.type() == StorageType.ENDER) {
                x1 += CHEST_INSET;
                z1 += CHEST_INSET;
                x2 -= CHEST_INSET;
                y2 -= CHEST_INSET * 2;
                z2 -= CHEST_INSET;

                if (hit.type() == StorageType.CHEST || hit.type() == StorageType.TRAPPED) {
                    BlockState st = mc.world.getBlockState(new BlockPos(hit.x(), hit.y(), hit.z()));
                    if (st.getBlock() instanceof ChestBlock && st.get(ChestBlock.CHEST_TYPE) != ChestType.SINGLE) {
                        Direction facing = st.get(ChestBlock.FACING);
                        ChestType ct = st.get(ChestBlock.CHEST_TYPE);
                        Direction nb = ct == ChestType.LEFT ? facing.rotateYClockwise() : facing.rotateYCounterclockwise();
                        if (nb == Direction.WEST) x1 = hit.x();
                        else if (nb == Direction.EAST) x2 = hit.x() + 1;
                        else if (nb == Direction.NORTH) z1 = hit.z();
                        else if (nb == Direction.SOUTH) z2 = hit.z() + 1;
                    }
                }
            }

            if (fill) {
                EspBoxRenderer.fill(bufferSource, poseStack, cam, x1 - BOX_INFLATE, y1 - BOX_INFLATE, z1 - BOX_INFLATE,
                    x2 + BOX_INFLATE, y2 + BOX_INFLATE, z2 + BOX_INFLATE, argb);
            } else {
                EspBoxRenderer.outline(bufferSource, poseStack, cam, x1, y1, z1, x2, y2, z2, argb, 1.0F);
            }

            if (drawTracers) {
                int tracerColor = (180 << 24) | rgb;
                EspBoxRenderer.tracer(bufferSource, poseStack, cam, forward, (x1 + x2) / 2.0, (y1 + y2) / 2.0, (z1 + z2) / 2.0, tracerColor, 1.15F);
            }
        }

        EspBoxRenderer.flush(bufferSource);
    }
}
