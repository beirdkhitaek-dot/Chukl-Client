package com.chukl.client.modules.donut;

import com.chukl.client.water.Category;
import com.chukl.client.water.WModule;
import com.chukl.client.water.Freecam;
import com.chukl.client.water.Setting;
import com.chukl.client.water.RenderUtils;
import com.chukl.client.water.StorageTracerRenderer;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RepeaterBlock;
import net.minecraft.block.entity.BeehiveBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.client.toast.Toast.Visibility;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.PolarBearEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

public final class OGChunkFinder extends WModule {
   private static final double CHUNK_THICKNESS = 0.035;
   private static final double MARKER_SIZE = 18.0;
   private static final double RENDER_Y = 63.0;
   private static final long COOLDOWN_MS = 50000L;
   private static final long SCAN_INTERVAL_MS = 2000L;
   private static final long REPEATER_SCAN_INTERVAL_MS = 200L;
   private static final float SCAN_DURATION_MS = 800.0F;
   private static final float SCAN_MAX_RADIUS = 140.0F;
   private static final int SEAGRASS_THRESHOLD = 70;
   private static final int TALL_SEAGRASS_THRESHOLD = 75;
   private static final int MIN_MARKER_DISTANCE_CHUNKS = 24;
   private static final long MARKER_ANIMATION_MS = 700L;
   private static final int GROWN_FLOWER_THRESHOLD = 6;
   private static final int SCAN_RADIUS_CHUNKS = 3;
   private static final int TRACER_ALPHA = 255;
   private static final int TRACER_GLOW_ALPHA = 70;
   private static final int TRACER_MID_ALPHA = 145;
   private static final int COBBLED_DEEPSLATE_THRESHOLD = 3;
   private static final int COBBLED_DEEPSLATE_Y_MIN = 0;
   private static final int COBBLED_DEEPSLATE_Y_MAX = 50;
   private static final int ROTATED_DEEPSLATE_THRESHOLD = 3;
   private static final int ROTATED_DEEPSLATE_Y_MIN = 0;
   private static final int ROTATED_DEEPSLATE_Y_MAX = 63;
   private static final int LIGHT_Y_MIN = -1;
   private static final int LIGHT_Y_MAX = 1;
   private static final int POLAR_BEAR_MIN_COUNT = 2;
   private final ConcurrentHashMap<ChunkPos, String> markedChunks = new ConcurrentHashMap<>();
   private final ConcurrentHashMap<ChunkPos, Long> markerCreatedAt = new ConcurrentHashMap<>();
   private final Set<Long> processedDetections = ConcurrentHashMap.newKeySet();
   private final Set<Long> acceptedMarkerChunks = ConcurrentHashMap.newKeySet();
   private final AtomicBoolean scanning = new AtomicBoolean(false);
   private final AtomicBoolean repeaterScanning = new AtomicBoolean(false);
   private ExecutorService executor;
   private long lastMarkTime = 0L;
   private long lastScanTime = 0L;
   private long lastRepScanTime = 0L;
   private long scanStartTime = 0L;
   private boolean scanActive = false;
   private final Setting<Boolean> tracers = new Setting<>("Tracers", false);
   private final Setting<Float> tracerWidth = new Setting<>("Tracer Width", 1.0F, 1.0F, 4.0F);
   private StorageTracerRenderer tracerRenderer;
   private final Setting<Color> markerColor = new Setting<>("Marker Color", new Color(80, 255, 0));
   private final Setting<Float> glassIntensity = new Setting<>("Glass Intensity", 1.5F, 0.0F, 2.0F);

   public OGChunkFinder() {
      super("OG Chunk Finder", Category.DONUT);
      this.addSetting(this.glassIntensity);
      this.addSetting(this.markerColor);
      this.addSetting(this.tracers);
      this.addSetting(this.tracerWidth);
   }

   @Override
   public void onEnable() {
      if (mc.player != null && mc.world != null) {
         this.markedChunks.clear();
         this.markerCreatedAt.clear();
         this.processedDetections.clear();
         this.acceptedMarkerChunks.clear();
         this.lastMarkTime = 0L;
         this.lastScanTime = 0L;
         this.scanActive = false;
      }
   }

   @Override
   public void onDisable() {
      if (this.tracerRenderer != null) {
         this.tracerRenderer.close();
         this.tracerRenderer = null;
      }

      this.markedChunks.clear();
      this.markerCreatedAt.clear();
      this.processedDetections.clear();
      this.acceptedMarkerChunks.clear();
      if (this.executor != null) {
         this.executor.shutdownNow();
      }

      this.scanning.set(false);
      this.repeaterScanning.set(false);
      this.scanActive = false;
   }

   @Override
   public void onTick() {
      if (mc.world != null && mc.player != null) {
         int simDist = (Integer)mc.options.getSimulationDistance().getValue();
         ChunkPos playerChunk = mc.player.getChunkPos();
         this.markedChunks.keySet().removeIf(cpx -> {
            if (Math.abs(cpx.x - playerChunk.x) <= simDist && Math.abs(cpx.z - playerChunk.z) <= simDist) {
               WorldChunk wc = mc.world.getChunkManager().getWorldChunk(cpx.x, cpx.z, false);
               return wc == null || wc.isEmpty();
            } else {
               return true;
            }
         });
         this.markerCreatedAt.keySet().removeIf(cpx -> !this.markedChunks.containsKey(cpx));
         this.processedDetections.removeIf(key -> {
            int cx = ChunkPos.getPackedX(key);
            int cz = ChunkPos.getPackedZ(key);
            return Math.abs(cx - playerChunk.x) > simDist || Math.abs(cz - playerChunk.z) > simDist;
         });
         if (this.scanActive && (float)(System.currentTimeMillis() - this.scanStartTime) > 800.0F) {
            this.scanActive = false;
         }

         long now = System.currentTimeMillis();
         if (now - this.lastRepScanTime >= 200L && !this.scanning.get() && this.repeaterScanning.compareAndSet(false, true)) {
            this.lastRepScanTime = now;

            try {
               ChunkPos center2 = mc.player.getChunkPos();
               List<ChunkPos> repPos = new ArrayList<>();
               List<WorldChunk> repChunks = new ArrayList<>();

               for (int cx = -3; cx <= 3; cx++) {
                  for (int cz = -3; cz <= 3; cz++) {
                     ChunkPos cp2 = new ChunkPos(center2.x + cx, center2.z + cz);
                     WorldChunk wc = mc.world.getChunkManager().getWorldChunk(cp2.x, cp2.z, false);
                     if (wc != null && !wc.isEmpty()) {
                        repPos.add(cp2);
                        repChunks.add(wc);
                     }
                  }
               }

               this.ensureExecutor();
               this.executor.submit(() -> {
                  try {
                     for (int i = 0; i < repPos.size(); i++) {
                        WorldChunk chunk = repChunks.get(i);
                        int count = 0;

                        for (ChunkSection section : chunk.getSectionArray()) {
                           if (section != null && !section.isEmpty() && section.hasAny(bsx -> bsx.isOf(Blocks.REPEATER))) {
                              for (int lx = 0; lx < 16; lx++) {
                                 for (int lz = 0; lz < 16; lz++) {
                                    for (int ly = 0; ly < 16; ly++) {
                                       BlockState bs = section.getBlockState(lx, ly, lz);
                                       if (bs.isOf(Blocks.REPEATER) && (Boolean)bs.get(RepeaterBlock.POWERED)) {
                                          if (++count >= 3) {
                                             this.markDetection(repPos.get(i), "repeater");
                                             return;
                                          }
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  } finally {
                     this.repeaterScanning.set(false);
                  }
               });
            } catch (Throwable var23) {
               this.repeaterScanning.set(false);
            }
         }

         if (now - this.lastScanTime >= 2000L) {
            if (this.scanning.compareAndSet(false, true)) {
               this.ensureExecutor();
               this.lastScanTime = now;
               this.scanActive = true;
               this.scanStartTime = now;
               ChunkPos center = mc.player.getChunkPos();
               List<ChunkPos> unsortedPos = new ArrayList<>();
               List<WorldChunk> unsortedChunks = new ArrayList<>();

               for (int cx = -3; cx <= 3; cx++) {
                  for (int czx = -3; czx <= 3; czx++) {
                     ChunkPos cp = new ChunkPos(center.x + cx, center.z + czx);
                     WorldChunk chunk = mc.world.getChunkManager().getWorldChunk(cp.x, cp.z, false);
                     if (chunk != null && !chunk.isEmpty()) {
                        unsortedPos.add(cp);
                        unsortedChunks.add(chunk);
                     }
                  }
               }

               Vec3d vel = mc.player.getVelocity();
               double speed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
               double moveZ;
               double moveX;
               if (speed > 0.05) {
                  moveX = vel.x / speed;
                  moveZ = vel.z / speed;
               } else {
                  float yaw = mc.player.getYaw();
                  moveX = -Math.sin(Math.toRadians(yaw));
                  moveZ = Math.cos(Math.toRadians(yaw));
               }

               Integer[] indices = new Integer[unsortedPos.size()];

               for (int i = 0; i < indices.length; i++) {
                  indices[i] = i;
               }

               Arrays.sort(indices, (a, b) -> {
                  double dotA = (unsortedPos.get(a).x - center.x) * moveX + (unsortedPos.get(a).z - center.z) * moveZ;
                  double dotB = (unsortedPos.get(b).x - center.x) * moveX + (unsortedPos.get(b).z - center.z) * moveZ;
                  return Double.compare(dotB, dotA);
               });
               List<ChunkPos> finalPositions = new ArrayList<>(unsortedPos.size());
               List<WorldChunk> finalChunks = new ArrayList<>(unsortedChunks.size());
               Integer[] ignored = indices;
               int var19 = indices.length;

               for (int var20 = 0; var20 < var19; var20++) {
                  int idx = ignored[var20];
                  finalPositions.add(unsortedPos.get(idx));
                  finalChunks.add(unsortedChunks.get(idx));
               }

               try {
                  this.executor
                     .submit(
                        () -> {
                           try {
                              for (int i = 0; i < finalPositions.size(); i++) {
                                 if (countTallSeagrass(finalChunks.get(i), 75) >= 75) {
                                    this.markDetection(finalPositions.get(i), "tall_seagrass");
                                    return;
                                 }
                              }

                              ChunkPos repeaterChunk = null;

                              label1074:
                              for (int ix = 0; ix < finalPositions.size(); ix++) {
                                 WorldChunk chunk = finalChunks.get(ix);
                                 int count = 0;

                                 for (ChunkSection section : chunk.getSectionArray()) {
                                    if (section != null && !section.isEmpty() && section.hasAny(bsx -> bsx.isOf(Blocks.REPEATER))) {
                                       for (int lx = 0; lx < 16; lx++) {
                                          for (int lz = 0; lz < 16; lz++) {
                                             for (int ly = 0; ly < 16; ly++) {
                                                BlockState bs = section.getBlockState(lx, ly, lz);
                                                if (bs.isOf(Blocks.REPEATER) && (Boolean)bs.get(RepeaterBlock.POWERED)) {
                                                   if (++count >= 3) {
                                                      repeaterChunk = finalPositions.get(ix);
                                                      break label1074;
                                                   }
                                                }
                                             }
                                          }
                                       }
                                    }
                                 }
                              }

                              if (repeaterChunk != null) {
                                 this.markDetection(repeaterChunk, "repeater");
                              } else {
                                 ChunkPos lightChunk = null;

                                 label1037:
                                 for (int ix = 0; ix < finalPositions.size(); ix++) {
                                    WorldChunk chunk = finalChunks.get(ix);
                                    ChunkSection[] sections = chunk.getSectionArray();
                                    int minY = chunk.getBottomY();

                                    for (int si = 0; si < sections.length; si++) {
                                       int sectionBottomY = minY + si * 16;
                                       if (sectionBottomY <= 1) {
                                          if (sectionBottomY + 16 >= -1) {
                                             ChunkSection sectionx = sections[si];
                                             if (sectionx != null && !sectionx.isEmpty() && sectionx.hasAny(bsx -> isLightSource(bsx.getBlock()))) {
                                                for (int lx = 0; lx < 16; lx++) {
                                                   for (int lz = 0; lz < 16; lz++) {
                                                      for (int lyx = 0; lyx < 16; lyx++) {
                                                         int worldY = sectionBottomY + lyx;
                                                         if (worldY >= -1 && worldY <= 1 && isLightSource(sectionx.getBlockState(lx, lyx, lz).getBlock())) {
                                                            lightChunk = finalPositions.get(ix);
                                                            break label1037;
                                                         }
                                                      }
                                                   }
                                                }
                                             }
                                          }
                                          continue;
                                       }
                                    }
                                 }

                                 if (lightChunk != null) {
                                    this.markDetection(lightChunk, "light");
                                 } else {
                                    ChunkPos glowLichenChunk = null;

                                    label1000:
                                    for (int ix = 0; ix < finalPositions.size(); ix++) {
                                       ChunkPos glcp = finalPositions.get(ix);
                                       if (Math.abs(glcp.x - center.x) <= 7 && Math.abs(glcp.z - center.z) <= 7) {
                                          WorldChunk chunk = finalChunks.get(ix);
                                          ChunkSection[] sections = chunk.getSectionArray();
                                          int minY = chunk.getBottomY();

                                          for (int six = 0; six < sections.length; six++) {
                                             ChunkSection sectionx = sections[six];
                                             if (sectionx != null && !sectionx.isEmpty() && sectionx.hasAny(bsx -> bsx.isOf(Blocks.GLOW_LICHEN))) {
                                                int sectionBottomY = minY + six * 16;

                                                for (int lx = 0; lx < 16; lx++) {
                                                   for (int lz = 0; lz < 16; lz++) {
                                                      for (int lyxx = 0; lyxx < 16; lyxx++) {
                                                         if (sectionx.getBlockState(lx, lyxx, lz).isOf(Blocks.GLOW_LICHEN)) {
                                                            double lichenX = glcp.getStartX() + lx + 0.5;
                                                            double lichenZ = glcp.getStartZ() + lz + 0.5;
                                                            double glowRange = 112.0;

                                                            for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
                                                               if (player != mc.player) {
                                                                  double pdx = player.getX() - lichenX;
                                                                  double pdz = player.getZ() - lichenZ;
                                                                  if (pdx * pdx + pdz * pdz <= glowRange * glowRange) {
                                                                     glowLichenChunk = glcp;
                                                                     break label1000;
                                                                  }
                                                               }
                                                            }
                                                         }
                                                      }
                                                   }
                                                }
                                             }
                                          }
                                       }
                                    }

                                    if (glowLichenChunk != null) {
                                       this.markDetection(glowLichenChunk, "glow_lichen");
                                    } else {
                                       ChunkPos polarBearChunk = this.checkPolarBears();
                                       if (polarBearChunk != null) {
                                          this.markDetection(polarBearChunk, "polar_bear");
                                       } else {
                                          ChunkPos matched = null;
                                          String matchedType = "normal";

                                          for (int ixx = 0; ixx < finalPositions.size(); ixx++) {
                                             ChunkPos cpx = finalPositions.get(ixx);
                                             WorldChunk chunk = finalChunks.get(ixx);
                                             boolean hasHive = false;

                                             for (BlockEntity be : chunk.getBlockEntities().values()) {
                                                BlockState state = chunk.getBlockState(be.getPos());
                                                if ((state.isOf(Blocks.BEEHIVE) || state.isOf(Blocks.BEE_NEST))
                                                   && be instanceof BeehiveBlockEntity hive
                                                   && hive.getBeeCount() > 0) {
                                                   hasHive = true;
                                                   break;
                                                }
                                             }

                                             boolean hasCobbledDeepslate = false;
                                             if (!hasHive) {
                                                int count = 0;
                                                ChunkSection[] sections = chunk.getSectionArray();
                                                int minY = chunk.getBottomY();

                                                label935:
                                                for (int sixx = 0; sixx < sections.length; sixx++) {
                                                   int sby = minY + sixx * 16;
                                                   if (sby > 50) {
                                                      break;
                                                   }

                                                   if (sby + 16 >= 0) {
                                                      ChunkSection sectionx = sections[sixx];
                                                      if (sectionx != null && !sectionx.isEmpty() && sectionx.hasAny(bsx -> bsx.isOf(Blocks.COBBLED_DEEPSLATE))
                                                         )
                                                       {
                                                         for (int lx = 0; lx < 16; lx++) {
                                                            for (int lz = 0; lz < 16; lz++) {
                                                               for (int lyxxx = 0; lyxxx < 16; lyxxx++) {
                                                                  int wy = sby + lyxxx;
                                                                  if (wy >= 0
                                                                     && wy <= 50
                                                                     && sectionx.getBlockState(lx, lyxxx, lz).isOf(Blocks.COBBLED_DEEPSLATE)) {
                                                                     if (++count >= 3) {
                                                                        hasCobbledDeepslate = true;
                                                                        break label935;
                                                                     }
                                                                  }
                                                               }
                                                            }
                                                         }
                                                      }
                                                   }
                                                }
                                             }

                                             boolean hasRotatedDeepslate = false;
                                             if (!hasHive && !hasCobbledDeepslate) {
                                                int count = 0;
                                                ChunkSection[] sections = chunk.getSectionArray();
                                                int minY = chunk.getBottomY();

                                                label894:
                                                for (int sixx = 0; sixx < sections.length; sixx++) {
                                                   int sbyx = minY + sixx * 16;
                                                   if (sbyx > 63) {
                                                      break;
                                                   }

                                                   if (sbyx + 16 >= 0) {
                                                      ChunkSection sectionx = sections[sixx];
                                                      if (sectionx != null && !sectionx.isEmpty() && sectionx.hasAny(bsx -> bsx.isOf(Blocks.DEEPSLATE))) {
                                                         for (int lx = 0; lx < 16; lx++) {
                                                            for (int lz = 0; lz < 16; lz++) {
                                                               for (int lyxxxx = 0; lyxxxx < 16; lyxxxx++) {
                                                                  int wy = sbyx + lyxxxx;
                                                                  if (wy >= 0 && wy <= 63) {
                                                                     BlockState bs = sectionx.getBlockState(lx, lyxxxx, lz);
                                                                     if (bs.isOf(Blocks.DEEPSLATE)) {
                                                                        Axis axis = (Axis)bs.get(Properties.AXIS);
                                                                        if (axis != Axis.Y) {
                                                                           if (++count >= 3) {
                                                                              hasRotatedDeepslate = true;
                                                                              break label894;
                                                                           }
                                                                        }
                                                                     }
                                                                  }
                                                               }
                                                            }
                                                         }
                                                      }
                                                   }
                                                }
                                             }

                                             boolean hasVines = false;
                                             if (!hasHive) {
                                                int count = 0;

                                                for (ChunkSection sectionx : chunk.getSectionArray()) {
                                                   if (sectionx != null && !sectionx.isEmpty() && sectionx.hasAny(bsx -> bsx.isOf(Blocks.VINE))) {
                                                      for (int lx = 0; lx < 16; lx++) {
                                                         for (int lz = 0; lz < 16; lz++) {
                                                            for (int lyxxxxx = 0; lyxxxxx < 16; lyxxxxx++) {
                                                               if (sectionx.getBlockState(lx, lyxxxxx, lz).isOf(Blocks.VINE)) {
                                                                  if (++count >= 150) {
                                                                     hasVines = true;
                                                                     break;
                                                                  }
                                                               }
                                                            }
                                                         }
                                                      }
                                                   }
                                                }
                                             }

                                             boolean hasSea = false;
                                             boolean hasGrownFlowers = false;
                                             if (!hasHive && !hasVines) {
                                                int seaCount = 0;
                                                int flowerCount = 0;
                                                ChunkSection[] sections = chunk.getSectionArray();
                                                int minY = chunk.getBottomY();

                                                label818:
                                                for (int sixx = 0; sixx < sections.length && minY + sixx * 16 <= 16; sixx++) {
                                                   ChunkSection sectionxx = sections[sixx];
                                                   if (sectionxx != null
                                                      && !sectionxx.isEmpty()
                                                      && sectionxx.hasAny(
                                                         bsx -> bsx.isOf(Blocks.SEAGRASS) || bsx.isOf(Blocks.TALL_SEAGRASS) || bsx.isIn(BlockTags.FLOWERS)
                                                      )) {
                                                      for (int lx = 0; lx < 16; lx++) {
                                                         for (int lz = 0; lz < 16; lz++) {
                                                            for (int lyxxxxxx = 0; lyxxxxxx < 16; lyxxxxxx++) {
                                                               BlockState bs = sectionxx.getBlockState(lx, lyxxxxxx, lz);
                                                               if (bs.isOf(Blocks.SEAGRASS) || bs.isOf(Blocks.TALL_SEAGRASS)) {
                                                                  if (++seaCount >= 70) {
                                                                     hasSea = true;
                                                                  }
                                                               } else if (bs.isIn(BlockTags.FLOWERS)) {
                                                                  if (++flowerCount >= 6) {
                                                                     hasGrownFlowers = true;
                                                                  }
                                                               }

                                                               if (hasSea || hasGrownFlowers) {
                                                                  break label818;
                                                               }
                                                            }
                                                         }
                                                      }
                                                   }
                                                }
                                             }

                                             boolean hasRepeater = false;
                                             if (!hasHive && !hasVines && !hasSea && !hasCobbledDeepslate && !hasRotatedDeepslate) {
                                                int count = 0;

                                                label771:
                                                for (ChunkSection sectionxx : chunk.getSectionArray()) {
                                                   if (sectionxx != null && !sectionxx.isEmpty() && sectionxx.hasAny(bsx -> bsx.isOf(Blocks.REPEATER))) {
                                                      for (int lx = 0; lx < 16; lx++) {
                                                         for (int lz = 0; lz < 16; lz++) {
                                                            for (int lyxxxxxx = 0; lyxxxxxx < 16; lyxxxxxx++) {
                                                               if (sectionxx.getBlockState(lx, lyxxxxxx, lz).isOf(Blocks.REPEATER)) {
                                                                  if (++count >= 3) {
                                                                     hasRepeater = true;
                                                                     break label771;
                                                                  }
                                                               }
                                                            }
                                                         }
                                                      }
                                                   }
                                                }
                                             }

                                             if (hasHive || hasVines || hasSea || hasGrownFlowers || hasCobbledDeepslate || hasRotatedDeepslate || hasRepeater) {
                                                matched = cpx;
                                                matchedType = hasRepeater ? "repeater" : "normal";
                                                break;
                                             }
                                          }

                                          if (matched != null) {
                                             boolean canFlag = "repeater".equals(matchedType) || System.currentTimeMillis() - this.lastMarkTime >= 50000L;
                                             if (canFlag) {
                                                this.markDetection(matched, matchedType);
                                                return;
                                             }
                                          }
                                       }
                                    }
                                 }
                              }
                           } finally {
                              this.scanning.set(false);
                           }
                        }
                     );
               } catch (Throwable var22) {
                  this.scanning.set(false);
               }
            }
         }
      }
   }

   private void markDetection(ChunkPos actual, String type) {
      long actualKey = actual.toLong();
      if (!this.processedDetections.contains(actualKey)) {
         if (this.hasMinimumMarkerDistance(actual)) {
            this.processedDetections.add(actualKey);
            this.acceptedMarkerChunks.add(actualKey);
            this.lastMarkTime = System.currentTimeMillis();
            this.markedChunks.put(actual, type);
            this.markerCreatedAt.put(actual, System.currentTimeMillis());
            MinecraftClient.getInstance().execute(() -> {
               MinecraftClient mc2 = MinecraftClient.getInstance();
               if (mc2.getToastManager() != null) {
                  mc2.getToastManager().add(new ChunkFinderToast(actual.getStartX(), actual.getStartZ()));
               }

               if (mc2.player != null) {
                  mc2.player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
               }
            });
         }
      }
   }

   private boolean hasMinimumMarkerDistance(ChunkPos candidate) {
      for (long packed : this.acceptedMarkerChunks) {
         long dx = (long)candidate.x - ChunkPos.getPackedX(packed);
         long dz = (long)candidate.z - ChunkPos.getPackedZ(packed);
         if (dx * dx + dz * dz < 576L) {
            return false;
         }
      }

      return true;
   }

   private ChunkPos checkPolarBears() {
      if (mc.world != null && mc.player != null) {
         Box searchBox = mc.player.getBoundingBox().expand(48.0, 128.0, 48.0);
         List<PolarBearEntity> bears = mc.world.getEntitiesByClass(PolarBearEntity.class, searchBox, LivingEntity::isAlive);
         if (bears.size() < 2) {
            return null;
         } else {
            for (PolarBearEntity bear : bears) {
               BlockPos pos = bear.getBlockPos();
               RegistryEntry<Biome> biomeEntry = mc.world.getBiome(pos);
               if (biomeEntry != null) {
                  if (biomeEntry.isIn(BiomeTags.IS_OCEAN)
                     || biomeEntry.isIn(BiomeTags.SPAWNS_SNOW_FOXES)
                     || biomeEntry.isIn(BiomeTags.POLAR_BEARS_SPAWN_ON_ALTERNATE_BLOCKS)) {
                     return new ChunkPos(pos);
                  }

                  int iceCount = 0;

                  for (int dx = -3; dx <= 3; dx++) {
                     for (int dz = -3; dz <= 3; dz++) {
                        Block block = mc.world.getBlockState(pos.add(dx, 0, dz)).getBlock();
                        if (block == Blocks.ICE
                           || block == Blocks.PACKED_ICE
                           || block == Blocks.BLUE_ICE
                           || block == Blocks.SNOW_BLOCK
                           || block == Blocks.POWDER_SNOW) {
                           iceCount++;
                        }
                     }
                  }

                  if (iceCount >= 5) {
                     return new ChunkPos(pos);
                  }
               }
            }

            return null;
         }
      } else {
         return null;
      }
   }

   private static int countTallSeagrass(WorldChunk chunk, int stopAt) {
      if (chunk != null && !chunk.isEmpty()) {
         int count = 0;

         for (ChunkSection section : chunk.getSectionArray()) {
            if (section != null && !section.isEmpty() && section.hasAny(state -> state.isOf(Blocks.TALL_SEAGRASS))) {
               for (int lx = 0; lx < 16; lx++) {
                  for (int lz = 0; lz < 16; lz++) {
                     for (int ly = 0; ly < 16; ly++) {
                        if (section.getBlockState(lx, ly, lz).isOf(Blocks.TALL_SEAGRASS)) {
                           if (++count >= stopAt) {
                              return count;
                           }
                        }
                     }
                  }
               }
            }
         }

         return count;
      } else {
         return 0;
      }
   }

   private void ensureExecutor() {
      if (this.executor == null || this.executor.isShutdown()) {
         this.executor = Executors.newSingleThreadExecutor(t -> {
            Thread th = new Thread(t, "ogchunk-scan");
            th.setDaemon(true);
            return th;
         });
      }
   }

   private static boolean isLightSource(Block block) {
      return block == Blocks.TORCH
         || block == Blocks.WALL_TORCH
         || block == Blocks.SOUL_TORCH
         || block == Blocks.SOUL_WALL_TORCH
         || block == Blocks.LANTERN
         || block == Blocks.SOUL_LANTERN
         || block == Blocks.GLOWSTONE
         || block == Blocks.SHROOMLIGHT
         || block == Blocks.SEA_LANTERN
         || block == Blocks.JACK_O_LANTERN
         || block == Blocks.CAMPFIRE
         || block == Blocks.SOUL_CAMPFIRE
         || block == Blocks.REDSTONE_LAMP
         || block == Blocks.END_ROD
         || block == Blocks.CRYING_OBSIDIAN
         || block == Blocks.OCHRE_FROGLIGHT
         || block == Blocks.VERDANT_FROGLIGHT
         || block == Blocks.PEARLESCENT_FROGLIGHT;
   }

   @Override
   public void onRender(MatrixStack matrices, float tickDelta) {
      if (mc.world != null && mc.player != null) {
         Camera cam = RenderUtils.getCamera();
         if (cam != null) {
            Vec3d camPos = RenderUtils.getCameraPos(cam);
            if (this.tracers.getValue() && !this.markedChunks.isEmpty()) {
               if (this.tracerRenderer == null) {
                  this.tracerRenderer = new StorageTracerRenderer();
               }

               this.tracerRenderer.begin(matrices);
               Vec3d origin = Freecam.resolveTracerOrigin(camPos, tickDelta);
               Vec3d start = origin.equals(camPos) ? RenderUtils.getCameraForward(cam).multiply(0.1) : origin.subtract(camPos);

               for (ChunkPos cp : this.markedChunks.keySet()) {
                  this.tracerRenderer
                     .addLine(
                        start,
                        new Vec3d(cp.getStartX() + 8.0 - camPos.x, 63.0 - camPos.y, cp.getStartZ() + 8.0 - camPos.z),
                        this.markerColor.getValue(),
                        this.tracerWidth.getValue()
                     );
               }

               this.tracerRenderer.flush();
            }

            double yBot = 63.0 - camPos.y;
            double yTop = yBot + 0.035;
            matrices.push();

            try {
               RenderUtils.WorldBatch batch = RenderUtils.beginWorldBatch(matrices);

               for (Entry<ChunkPos, String> entry : this.markedChunks.entrySet()) {
                  ChunkPos cp = entry.getKey();
                  long createdAt = this.markerCreatedAt.getOrDefault(cp, 0L);
                  float progress = createdAt == 0L ? 1.0F : Math.min(1.0F, (float)(System.currentTimeMillis() - createdAt) / 700.0F);
                  float eased = 1.0F - (float)Math.pow(1.0F - progress, 3.0);
                  float animatedSize = 18.0F * (0.12F + 0.88F * eased);
                  int fillAlpha = Math.max(0, Math.min(120, Math.round(120.0F * eased)));
                  double centerX = cp.getStartX() + 8.0 - camPos.x;
                  double centerZ = cp.getStartZ() + 8.0 - camPos.z;
                  double half = animatedSize * 0.5;
                  double minX = centerX - half;
                  double minZ = centerZ - half;
                  double maxX = centerX + half;
                  double maxZ = centerZ + half;
                  batch.renderGlassPane(
                     minX,
                     yTop,
                     minZ,
                     maxX,
                     maxZ,
                     new Color(
                        this.markerColor.getValue().getRed(),
                        this.markerColor.getValue().getGreen(),
                        this.markerColor.getValue().getBlue(),
                        Math.round(fillAlpha * this.markerColor.getValue().getAlpha() / 255.0F)
                     ),
                     this.glassIntensity.getValue(),
                     System.nanoTime() / 1.E9
                  );
               }

               batch.flush();
            } finally {
               matrices.pop();
            }
         }
      }
   }

   final class ChunkFinderToast implements Toast {
      private static final Identifier BACKGROUND = Identifier.ofVanilla("toast/advancement");
      private int intVal;
      private int intVal2;
      private Visibility visibility = Visibility.HIDE;

      private ChunkFinderToast(int x, int z) {
         this.intVal = x;
         this.intVal2 = z;
      }

      public Visibility getVisibility() {
         return this.visibility;
      }

      public void update(ToastManager manager, long time) {
         this.visibility = time >= 5000.0 * manager.getNotificationDisplayTimeMultiplier() ? Visibility.HIDE : Visibility.SHOW;
      }

      public void draw(DrawContext context, TextRenderer textRenderer, long startTime) {
         context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, BACKGROUND, 0, 0, this.getWidth(), this.getHeight());
         context.drawItemWithoutEntity(new ItemStack(Items.CHEST), 8, 8);
         context.drawText(textRenderer, Text.literal("OG Chunk Finder"), 30, 7, -5085441, false);
         context.drawText(textRenderer, Text.literal("X: " + this.intVal + " Z: " + this.intVal2), 30, 18, -1, false);
      }
   }
}

