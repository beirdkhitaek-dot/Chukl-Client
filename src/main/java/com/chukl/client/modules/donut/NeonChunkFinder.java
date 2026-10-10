package com.chukl.client.modules.donut;

import com.chukl.client.water.Category;
import com.chukl.client.water.WModule;
import com.chukl.client.water.Setting;
import com.chukl.client.water.RenderUtils;
import java.awt.Color;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.entity.TrappedChestBlockEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldEventS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

public final class NeonChunkFinder extends WModule {
   private final Setting<Boolean> cacheMode = new Setting<>("Cache Mode", true);
   private final Setting<Boolean> detectSpawner = new Setting<>("Spawner", true);
   private final Setting<Boolean> detectChest = new Setting<>("Chest", true);
   private final Setting<Boolean> detectDungeon = new Setting<>("Dungeon Pattern", true);
   private final Setting<Boolean> packetLeaks = new Setting<>("Packet Leaks", true);
   private final Setting<Boolean> showTracers = new Setting<>("Tracers", true);
   private final Setting<Double> tracerWidth = new Setting<>("Tracer Width", 1.0, 0.1, 3.0);
   private final Setting<Boolean> notify = new Setting<>("Notify", true);
   private final Setting<Boolean> showBeam = new Setting<>("Vertical Beam", true);
   private final Map<BlockPos, NeonChunkFinder.CachedSpot> cache = new ConcurrentHashMap<>();
   private final Set<Long> scannedChunks = ConcurrentHashMap.newKeySet();
   private final Set<String> notifiedKeys = ConcurrentHashMap.newKeySet();
   private int scanTick = 0;
   private int totalFound = 0;

   public NeonChunkFinder() {
      super("Neon Chunk Finder", Category.DONUT);
      this.addSetting(this.cacheMode);
      this.addSetting(this.detectSpawner);
      this.addSetting(this.detectChest);
      this.addSetting(this.detectDungeon);
      this.addSetting(this.packetLeaks);
      this.addSetting(this.showTracers);
      this.addSetting(this.tracerWidth);
      this.addSetting(this.showBeam);
      this.addSetting(this.notify);
   }

   @Override
   public void onEnable() {
      this.cache.clear();
      this.scannedChunks.clear();
      this.notifiedKeys.clear();
      this.scanTick = 0;
      this.totalFound = 0;
      if (mc.player != null) {
         mc.player.sendMessage(Text.literal("§d[Neon] §fCache cleared. Dive underground to scan."), false);
      }
   }

   @Override
   public void onDisable() {
      this.cache.clear();
      this.scannedChunks.clear();
      this.notifiedKeys.clear();
   }

   @Override
   public void onTick() {
      if (mc.world != null && mc.player != null) {
         if (++this.scanTick >= 5) {
            this.scanTick = 0;
            int pcx = mc.player.getChunkPos().x;
            int pcz = mc.player.getChunkPos().z;

            for (int cx = pcx - 6; cx <= pcx + 6; cx++) {
               for (int cz = pcz - 6; cz <= pcz + 6; cz++) {
                  long key = ChunkPos.toLong(cx, cz);
                  if (!this.scannedChunks.contains(key)) {
                     WorldChunk chunk = mc.world.getChunkManager().getWorldChunk(cx, cz, false);
                     if (chunk != null) {
                        this.scannedChunks.add(key);
                        this.deepScanChunk(chunk, cx, cz);
                     }
                  }
               }
            }

            this.scannedChunks.removeIf(k -> Math.abs(ChunkPos.getPackedX(k) - pcx) > 8 || Math.abs(ChunkPos.getPackedZ(k) - pcz) > 8);
         }
      }
   }

   @Override
   public void onPacketReceive(Packet<?> packet) {
      if (mc.world != null && mc.player != null) {
         if (packet instanceof ChunkDataS2CPacket cd) {
            long key = ChunkPos.toLong(cd.getChunkX(), cd.getChunkZ());
            this.scannedChunks.remove(key);
         }

         if (this.packetLeaks.getValue()) {
            if (packet instanceof BlockBreakingProgressS2CPacket mining) {
               BlockPos pos = mining.getPos();
               if (pos.getY() < 50 && mining.getEntityId() != mc.player.getId()) {
                  this.addToCache(pos, "Mining", new Color(255, 50, 50));
               }
            }

            if (packet instanceof WorldEventS2CPacket event) {
               BlockPos pos = event.getPos();
               if (pos.getY() < 50 && event.getEventId() == 2001) {
                  this.addToCache(pos, "Break", new Color(255, 150, 50));
               }
            }
         }
      }
   }

   private void deepScanChunk(WorldChunk chunk, int cx, int cz) {
      for (Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
         BlockEntity be = entry.getValue();
         BlockPos pos = entry.getKey();
         if (this.detectSpawner.getValue() && be instanceof MobSpawnerBlockEntity) {
            this.addToCache(pos, "Spawner", new Color(255, 30, 30));
         }

         if (this.detectChest.getValue()) {
            if (be instanceof ChestBlockEntity || be instanceof TrappedChestBlockEntity) {
               this.addToCache(pos, "Chest", new Color(255, 200, 30));
            }

            if (be instanceof BarrelBlockEntity) {
               this.addToCache(pos, "Barrel", new Color(200, 130, 50));
            }

            if (be instanceof ShulkerBoxBlockEntity) {
               this.addToCache(pos, "Shulker", new Color(200, 50, 220));
            }

            if (be instanceof EnderChestBlockEntity) {
               this.addToCache(pos, "EnderChest", new Color(140, 80, 220));
            }
         }
      }

      if (this.detectDungeon.getValue()) {
         ChunkSection[] sections = chunk.getSectionArray();
         int minY = chunk.getBottomY();

         for (int si = 0; si < sections.length; si++) {
            ChunkSection sec = sections[si];
            if (sec != null && !sec.isEmpty()) {
               int baseY = minY + si * 16;
               int mossy = 0;
               int cobble = 0;
               int web = 0;

               for (int lx = 0; lx < 16; lx++) {
                  for (int lz = 0; lz < 16; lz++) {
                     for (int ly = 0; ly < 16; ly++) {
                        Block b = sec.getBlockState(lx, ly, lz).getBlock();
                        if (b == Blocks.MOSSY_COBBLESTONE) {
                           mossy++;
                        } else if (b == Blocks.COBBLESTONE) {
                           cobble++;
                        } else if (b == Blocks.COBWEB) {
                           web++;
                        }
                     }
                  }
               }

               if (mossy >= 3 && cobble >= 5) {
                  this.addToCache(new BlockPos(cx * 16 + 8, baseY + 8, cz * 16 + 8), "Dungeon", new Color(255, 100, 30));
               }

               if (web >= 3 && mossy >= 2) {
                  this.addToCache(new BlockPos(cx * 16 + 8, baseY + 8, cz * 16 + 8), "SpiderDungeon", new Color(150, 50, 50));
               }
            }
         }
      }
   }

   private void addToCache(BlockPos rawPos, String type, Color color) {
      BlockPos pos = rawPos.toImmutable();
      if (!this.cache.containsKey(pos)) {
         this.cache.put(pos, new CachedSpot(type, color));
         this.totalFound++;
         String nKey = pos.getX() + "," + pos.getY() + "," + pos.getZ();
         if (this.notify.getValue() && !this.notifiedKeys.contains(nKey) && mc.player != null) {
            this.notifiedKeys.add(nKey);
            mc.player
               .sendMessage(
                  Text.literal("§d[Neon] §f" + type + " §eat §f" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + " §7(cached: " + this.totalFound + ")"),
                  false
               );
         }
      }
   }

   @Override
   public void onRender(MatrixStack matrices, float tickDelta) {
      if (mc.world != null && mc.player != null && !this.cache.isEmpty()) {
         Camera cam = RenderUtils.getCamera();
         if (cam != null) {
            Vec3d camPos = RenderUtils.getCameraPos(cam);
            Vec3d camFwd = RenderUtils.getCameraForward(cam);
            RenderUtils.WorldBatch batch = RenderUtils.beginWorldBatch(matrices);

            for (Entry<BlockPos, NeonChunkFinder.CachedSpot> entry : this.cache.entrySet()) {
               BlockPos pos = entry.getKey();
               NeonChunkFinder.CachedSpot spot = entry.getValue();
               Color color = spot.color;
               double rx = pos.getX() - camPos.x;
               double ry = pos.getY() - camPos.y;
               double rz = pos.getZ() - camPos.z;
               if (!(rx * rx + rz * rz > 250000.0)) {
                  batch.renderOutlineBox(rx, ry, rz, rx + 1.0, ry + 1.0, rz + 1.0, new Color(color.getRed(), color.getGreen(), color.getBlue(), 200));
                  if (this.showBeam.getValue()) {
                     batch.renderLine(
                        new Color(color.getRed(), color.getGreen(), color.getBlue(), 60),
                        new Vec3d(rx + 0.5, ry - 64.0, rz + 0.5),
                        new Vec3d(rx + 0.5, ry + 128.0, rz + 0.5),
                        0.3F
                     );
                  }

                  if (this.showTracers.getValue()) {
                     Vec3d target = new Vec3d(rx + 0.5, ry + 0.5, rz + 0.5);
                     Vec3d start = camFwd.multiply(0.1);
                     float w = this.tracerWidth.getValue().floatValue();
                     batch.renderLine(new Color(color.getRed(), color.getGreen(), color.getBlue(), 40), start, target, w + 2.0F);
                     batch.renderLine(new Color(color.getRed(), color.getGreen(), color.getBlue(), 150), start, target, w + 0.8F);
                     batch.renderLine(color, start, target, w);
                  }
               }
            }

            batch.flush();
         }
      }
   }

   final class CachedSpot {
      private String type;
      private Color color;

      private CachedSpot(String type, Color color) {
         this.type = type;
         this.color = color;
      }

      public String type() {
         return this.type;
      }

      public Color color() {
         return this.color;
      }
   }
}

