package com.chukl.client.modules.donut;

import com.chukl.client.compat.Events.TickEvent;
import com.chukl.client.compat.MeteorModule;
import com.chukl.client.compat.Render3DEvent;
import com.chukl.client.compat.SettingColor;
import com.chukl.client.compat.ShapeMode;
import com.chukl.client.core.Category;
import com.chukl.client.core.Render3D;
import com.chukl.client.settings.BoolSetting;
import com.chukl.client.settings.ColorSetting;
import com.chukl.client.settings.NumberSetting;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BrewingStandBlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.ChiseledBookshelfBlockEntity;
import net.minecraft.block.entity.CrafterBlockEntity;
import net.minecraft.block.entity.DecoratedPotBlockEntity;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.block.entity.DropperBlockEntity;
import net.minecraft.block.entity.EnchantingTableBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.entity.PistonBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

/**
 * Krispy's Storage ESP: highlights selected storage blocks through walls, with an optional tracer line.
 * Krispy's version lets you pick a color per storage type through its own inventory-style picker screen
 * (StorageColorPickerScreen); Chukl doesn't have that screen, so each storage type gets its own Bool + Color
 * setting pair instead -- same per-type on/off and color control, just through Chukl's normal setting rows.
 */
public class StorageEspModule extends MeteorModule {
    private enum StorageType {
        CHEST("Chest", 156, 91, 0),
        ENDER_CHEST("Ender Chest", 127, 91, 255),
        SHULKER("Shulker Box", 236, 85, 204),
        SPAWNER("Spawner", 255, 76, 98),
        FURNACE("Furnace", 102, 168, 255),
        BARREL("Barrel", 232, 151, 70),
        ENCHANTING_TABLE("Enchanting Table", 62, 224, 207),
        PISTON("Moving Piston", 151, 224, 88),
        HOPPER("Hopper", 0, 0, 255),
        DISPENSER("Dispenser", 95, 188, 255),
        DROPPER("Dropper", 118, 134, 162),
        BREWING_STAND("Brewing Stand", 249, 115, 140),
        CRAFTER("Crafter", 255, 195, 72),
        CHISELED_BOOKSHELF("Chiseled Bookshelf", 189, 126, 255),
        DECORATED_POT("Decorated Pot", 223, 119, 92);

        final String label;
        final int r, g, b;
        StorageType(String label, int r, int g, int b) { this.label = label; this.r = r; this.g = g; this.b = b; }
        /** Krispy ships Dispenser/Dropper/Brewing Stand/Crafter/Chiseled Bookshelf/Decorated Pot disabled by default. */
        boolean defaultOn() {
            return this != DISPENSER && this != DROPPER && this != BREWING_STAND
                && this != CRAFTER && this != CHISELED_BOOKSHELF && this != DECORATED_POT;
        }
    }

    private record StoragePair(BlockPos first, BlockPos second, StorageType type) {
        Vec3d center() {
            return new Vec3d((first.getX() + second.getX() + 1) * 0.5, (first.getY() + second.getY() + 1) * 0.5, (first.getZ() + second.getZ() + 1) * 0.5);
        }
    }

    private final NumberSetting alpha = add(new NumberSetting("Alpha", "Highlight opacity (0-255).", 53.0, 0, 255, 1.0));
    private final BoolSetting tracers = add(new BoolSetting("Tracers", "Draws a line from the camera to each target.", false));

    private final BoolSetting[] enabled = new BoolSetting[StorageType.values().length];
    private final ColorSetting[] color = new ColorSetting[StorageType.values().length];

    private List<StoragePair> targets = List.of();
    private Object scannedWorld;
    private int scanCooldown;

    public StorageEspModule() {
        super(Category.DONUT, "Storage ESP", "Krispy's Storage ESP. Highlights selected loaded storage blocks through walls.");
        for (StorageType t : StorageType.values()) {
            enabled[t.ordinal()] = add(new BoolSetting(t.label, "Highlight " + t.label + " blocks.", t.defaultOn()));
            color[t.ordinal()] = add(new ColorSetting(t.label + " Color", "", Render3D.argb(t.r, t.g, t.b, 255)));
            color[t.ordinal()].visibleWhen(() -> enabled[t.ordinal()].get());
        }
    }

    @Override
    protected void onEnable() {
        targets = List.of();
        scannedWorld = null;
        scanCooldown = 0;
    }

    @Override
    protected void onDisable() {
        targets = List.of();
        scannedWorld = null;
    }

    private void onTick(TickEvent.Post event) {
        if (mc.world != null && mc.player != null) {
            if (scannedWorld != mc.world) {
                targets = List.of();
                scannedWorld = mc.world;
                scanCooldown = 0;
            }
            if (scanCooldown-- <= 0) {
                refreshTargets();
                scanCooldown = 9;
            }
        } else {
            targets = List.of();
            scannedWorld = null;
            scanCooldown = 0;
        }
    }

    private void refreshTargets() {
        int range = mc.options.getClampedViewDistance();
        ChunkPos center = mc.player.getChunkPos();
        List<StoragePair> found = new ArrayList<>();
        HashSet<BlockPos> claimed = new HashSet<>();

        for (int cx = center.x - range; cx <= center.x + range; cx++) {
            for (int cz = center.z - range; cz <= center.z + range; cz++) {
                if (!mc.world.isChunkLoaded(cx, cz)) continue;
                WorldChunk chunk = mc.world.getChunk(cx, cz);

                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    StorageType type = typeOf(be);
                    if (type == null) continue;

                    BlockPos pos = be.getPos().toImmutable();
                    if (type == StorageType.CHEST) {
                        if (claimed.contains(pos)) continue;
                        BlockPos partner = chestPartner(pos);
                        if (partner != null) {
                            claimed.add(pos);
                            claimed.add(partner);
                            found.add(new StoragePair(pos, partner, type));
                            continue;
                        }
                    }
                    found.add(new StoragePair(pos, pos, type));
                }
            }
        }

        found.sort(Comparator.comparingDouble(this::distanceSquared));
        targets = List.copyOf(found);
    }

    private BlockPos chestPartner(BlockPos pos) {
        if (!(mc.world.getBlockEntity(pos) instanceof ChestBlockEntity)) return null;
        BlockState state = mc.world.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock) || state.get(ChestBlock.CHEST_TYPE) == ChestType.SINGLE) return null;

        for (Direction d : Direction.Type.HORIZONTAL) {
            BlockPos n = pos.offset(d);
            BlockState ns = mc.world.getBlockState(n);
            if (ns.getBlock() == state.getBlock() && mc.world.getBlockEntity(n) instanceof ChestBlockEntity
                && ns.get(ChestBlock.CHEST_TYPE) != ChestType.SINGLE) return n.toImmutable();
        }
        return null;
    }

    private boolean targetStillLive(StoragePair pair) {
        if (!mc.world.isChunkLoaded(pair.first().getX() >> 4, pair.first().getZ() >> 4)) return false;
        BlockEntity be = mc.world.getBlockEntity(pair.first());
        if (be == null || be.isRemoved() || typeOf(be) != pair.type()) return false;

        if (!pair.first().equals(pair.second())) {
            if (!mc.world.isChunkLoaded(pair.second().getX() >> 4, pair.second().getZ() >> 4)) return false;
            BlockEntity be2 = mc.world.getBlockEntity(pair.second());
            if (be2 == null || be2.isRemoved() || typeOf(be2) != pair.type()) return false;
        }
        return true;
    }

    private void onRender(Render3DEvent event) {
        if (mc.world == null || mc.player == null || targets.isEmpty()) return;

        Vec3d tracerOrigin = event.r3d.camera;
        int a = alpha.getInt();

        for (StoragePair pair : targets) {
            int idx = pair.type().ordinal();
            if (!enabled[idx].get()) continue;
            if (!targetStillLive(pair)) continue;

            int argb = (a << 24) | (color[idx].get() & 0xFFFFFF);
            SettingColor c = new SettingColor(com.chukl.client.compat.Color.ofArgb(argb));

            int minX = Math.min(pair.first().getX(), pair.second().getX());
            int minY = Math.min(pair.first().getY(), pair.second().getY());
            int minZ = Math.min(pair.first().getZ(), pair.second().getZ());
            int maxX = Math.max(pair.first().getX(), pair.second().getX()) + 1;
            int maxY = Math.max(pair.first().getY(), pair.second().getY()) + 1;
            int maxZ = Math.max(pair.first().getZ(), pair.second().getZ()) + 1;

            if (a > 0) {
                event.renderer.box(minX + 0.08, minY + 0.08, minZ + 0.08, maxX - 0.08, maxY - 0.08, maxZ - 0.08, c, c, ShapeMode.Sides, 0);
            }
            if (tracers.get()) {
                Vec3d center = pair.center();
                event.renderer.line(tracerOrigin.x, tracerOrigin.y, tracerOrigin.z, center.x, center.y, center.z, c);
            }
        }
    }

    private double distanceSquared(StoragePair pair) {
        Vec3d c = pair.center();
        return mc.player.squaredDistanceTo(c.x, c.y, c.z);
    }

    private static StorageType typeOf(BlockEntity be) {
        if (be instanceof ChestBlockEntity) return StorageType.CHEST;
        if (be instanceof EnderChestBlockEntity) return StorageType.ENDER_CHEST;
        if (be instanceof ShulkerBoxBlockEntity) return StorageType.SHULKER;
        if (be instanceof MobSpawnerBlockEntity) return StorageType.SPAWNER;
        if (be instanceof AbstractFurnaceBlockEntity) return StorageType.FURNACE;
        if (be instanceof BarrelBlockEntity) return StorageType.BARREL;
        if (be instanceof EnchantingTableBlockEntity) return StorageType.ENCHANTING_TABLE;
        if (be instanceof PistonBlockEntity) return StorageType.PISTON;
        if (be instanceof HopperBlockEntity) return StorageType.HOPPER;
        if (be instanceof DropperBlockEntity) return StorageType.DROPPER;
        if (be instanceof DispenserBlockEntity) return StorageType.DISPENSER;
        if (be instanceof BrewingStandBlockEntity) return StorageType.BREWING_STAND;
        if (be instanceof CrafterBlockEntity) return StorageType.CRAFTER;
        if (be instanceof ChiseledBookshelfBlockEntity) return StorageType.CHISELED_BOOKSHELF;
        if (be instanceof DecoratedPotBlockEntity) return StorageType.DECORATED_POT;
        return null;
    }

    @Override
    protected void onTick() {
        onTick(new TickEvent.Post());
    }

    @Override
    protected void onRender3D(Render3D r3d) {
        onRender(new Render3DEvent(r3d));
    }
}
