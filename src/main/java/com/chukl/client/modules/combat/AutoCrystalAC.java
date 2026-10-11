package com.chukl.client.modules.combat;

import com.chukl.client.water.Category;
import com.chukl.client.water.Friends;
import com.chukl.client.water.Setting;
import java.lang.reflect.Method;
import java.util.Comparator;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class AutoCrystalAC extends com.chukl.client.water.WModule {
   private final Setting<Double> breakRange = new Setting<>("Break Range", 5.0, 1.0, 10.0);
   private final Setting<Double> placeRange = new Setting<>("Place Range", 5.0, 1.0, 10.0);
   private final Setting<Integer> breakDelay = new Setting<>("Break Delay", 0, 0, 20);
   private final Setting<Integer> placeDelay = new Setting<>("Place Delay", 2, 0, 20);
   private final Setting<Boolean> autoObsidian = new Setting<>("Auto Obsidian", false);
   private final Setting<Boolean> switchBack = new Setting<>("Switch Back", true);
   private final Setting<Boolean> multiTarget = new Setting<>("Multi Target", false);
   private final Setting<Double> minDamage = new Setting<>("Min Damage", 4.0, 0.0, 20.0);
   private final Setting<Double> maxSelfDamage = new Setting<>("Max Self", 8.0, 0.0, 20.0);
   private final Setting<Boolean> rotate = new Setting<>("Rotate", true);
   private final Setting<Double> rotationSpeed = new Setting<>("Rot Speed", 18.0, 1.0, 45.0);
   private final Setting<Boolean> swingHand = new Setting<>("Swing Hand", true);
   private final Setting<Boolean> antiSuicide = new Setting<>("Anti Suicide", true);
   private int breakCooldown = 0;
   private int placeCooldown = 0;
   private int savedSlot = -1;
   private boolean wasActive = false;
   private int lastBrokenId = -1;

   public AutoCrystalAC() {
      super("Auto Crystal", Category.COMBAT);
      this.addSetting(this.breakRange);
      this.addSetting(this.placeRange);
      this.addSetting(this.breakDelay);
      this.addSetting(this.placeDelay);
      this.addSetting(this.autoObsidian);
      this.addSetting(this.switchBack);
      this.addSetting(this.multiTarget);
      this.addSetting(this.minDamage);
      this.addSetting(this.maxSelfDamage);
      this.addSetting(this.rotate);
      this.addSetting(this.rotationSpeed);
      this.addSetting(this.swingHand);
      this.addSetting(this.antiSuicide);
   }

   @Override
   public void onEnable() {
      this.resetAll();
      this.wasActive = false;
      super.onEnable();
   }

   @Override
   public void onDisable() {
      this.restoreSlot();
      this.resetAll();
      super.onDisable();
   }

   @Override
   public void onActivationKeyPressed() {
      if (this.isEnabled()) {
         this.wasActive = !this.wasActive;
         if (!this.wasActive) {
            this.restoreSlot();
         }
      }
   }

   @Override
   public void onTick() {
      if (this.wasActive) {
         if (mc.player != null && mc.world != null) {
            if (mc.currentScreen == null) {
               this.tickCooldowns();
               PlayerEntity target = this.findTarget();
               if (target != null) {
                  if (this.breakCooldown <= 0) {
                     for (EndCrystalEntity crystal : mc.world
                        .getEntitiesByClass(
                           EndCrystalEntity.class,
                           new Box(
                                 new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ()),
                                 new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ())
                              )
                              .expand(this.breakRange.getValue()),
                           e -> true
                        )) {
                        if (crystal.getId() != this.lastBrokenId) {
                           double dist = Math.sqrt(
                              Math.pow(mc.player.getX() - crystal.getX(), 2.0)
                                 + Math.pow(mc.player.getY() - crystal.getY(), 2.0)
                                 + Math.pow(mc.player.getZ() - crystal.getZ(), 2.0)
                           );
                           if (!(dist > this.breakRange.getValue())) {
                              Vec3d crystalPos = new Vec3d(crystal.getX(), crystal.getY(), crystal.getZ());
                              if (this.isDamageAllowed(crystalPos, target)) {
                                 this.face(crystalPos);
                                 this.attackCrystal(crystal);
                                 this.lastBrokenId = crystal.getId();
                                 this.breakCooldown = this.breakDelay.getValue();
                                 break;
                              }
                           }
                        }
                     }
                  }

                  if (this.placeCooldown <= 0) {
                     BlockPos base = this.findBestBase(target);
                     if (base != null) {
                        this.equipCrystal();
                        this.face(Vec3d.ofCenter(base.up()));
                        this.placeCrystal(base);
                        this.placeCooldown = this.placeDelay.getValue();
                     }
                  }
               }
            }
         }
      }
   }

   private void tickCooldowns() {
      if (this.breakCooldown > 0) {
         this.breakCooldown--;
      }

      if (this.placeCooldown > 0) {
         this.placeCooldown--;
      }
   }

   private void resetAll() {
      this.breakCooldown = 0;
      this.placeCooldown = 0;
      this.lastBrokenId = -1;
   }

   private PlayerEntity findTarget() {
      double range = Math.max(this.breakRange.getValue(), this.placeRange.getValue());
      return mc.world
         .getPlayers()
         .stream()
         .filter(p -> p != mc.player && p.isAlive() && !p.isInvisibleTo(mc.player))
         .filter(p -> !Friends.isFriend(p.getName().getString()))
         .filter(
            p -> Math.sqrt(Math.pow(mc.player.getX() - p.getX(), 2.0) + Math.pow(mc.player.getY() - p.getY(), 2.0) + Math.pow(mc.player.getZ() - p.getZ(), 2.0))
               <= range
         )
         .min(
            Comparator.comparingDouble(
               p -> Math.sqrt(
                  Math.pow(mc.player.getX() - p.getX(), 2.0) + Math.pow(mc.player.getY() - p.getY(), 2.0) + Math.pow(mc.player.getZ() - p.getZ(), 2.0)
               )
            )
         )
         .orElse(null);
   }

   private BlockPos findBestBase(PlayerEntity target) {
      Vec3d targetPos = new Vec3d(target.getX(), target.getY(), target.getZ());
      double range = this.placeRange.getValue();
      BlockPos best = null;
      double bestScore = Double.MAX_VALUE;

      for (int x = (int)(mc.player.getX() - range); x <= mc.player.getX() + range; x++) {
         for (int y = (int)(mc.player.getY() - 2.0); y <= mc.player.getY() + 2.0; y++) {
            for (int z = (int)(mc.player.getZ() - range); z <= mc.player.getZ() + range; z++) {
               BlockPos pos = new BlockPos(x, y, z);
               if (this.isCrystalBase(pos)) {
                  BlockPos above = pos.up();
                  if (mc.world.getBlockState(above).isAir() && mc.world.getOtherEntities(null, new Box(above)).isEmpty()) {
                     double distToPlayer = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ()).distanceTo(Vec3d.ofCenter(above));
                     if (!(distToPlayer > range)) {
                        double distToTarget = targetPos.distanceTo(Vec3d.ofCenter(above));
                        Vec3d crystalPos = Vec3d.ofCenter(above);
                        double targetDamage = this.estimateCrystalDamage(crystalPos, target);
                        double selfDamage = this.estimateCrystalDamage(crystalPos, mc.player);
                        if (!(targetDamage < this.minDamage.getValue())
                           && !(selfDamage > this.maxSelfDamage.getValue())
                           && (!this.antiSuicide.getValue() || !(mc.player.getHealth() + mc.player.getAbsorptionAmount() - selfDamage <= 1.0))) {
                           double score = -targetDamage + selfDamage * 0.65 + distToTarget * 0.2 - distToPlayer * 0.1;
                           if (score < bestScore) {
                              bestScore = score;
                              best = pos;
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      return best;
   }

   private boolean isCrystalBase(BlockPos pos) {
      BlockState state = mc.world.getBlockState(pos);
      return state.isOf(Blocks.OBSIDIAN) || state.isOf(Blocks.BEDROCK);
   }

   private void attackCrystal(EndCrystalEntity crystal) {
      mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, mc.player.isSneaking()));
      if (this.swingHand.getValue()) {
         mc.player.swingHand(Hand.MAIN_HAND);
      }
   }

   private void placeCrystal(BlockPos base) {
      BlockPos above = base.up();
      Vec3d hitVec = Vec3d.ofCenter(above);
      BlockHitResult hit = new BlockHitResult(hitVec, Direction.UP, above, false);
      mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
      if (this.swingHand.getValue()) {
         mc.player.swingHand(Hand.MAIN_HAND);
      }
   }

   private void equipCrystal() {
      if (!mc.player.getMainHandStack().isOf(Items.END_CRYSTAL)) {
         int slot = -1;

         for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.END_CRYSTAL)) {
               slot = i;
               break;
            }
         }

         if (slot != -1) {
            if (this.switchBack.getValue() && this.savedSlot == -1) {
               this.savedSlot = mc.player.getInventory().getSelectedSlot();
            }

            mc.player.getInventory().setSelectedSlot(slot);
         }
      }
   }

   private void restoreSlot() {
      if (this.switchBack.getValue() && this.savedSlot != -1 && mc.player != null) {
         mc.player.getInventory().setSelectedSlot(this.savedSlot);
      }

      this.savedSlot = -1;
   }

   private boolean isDamageAllowed(Vec3d crystalPos, PlayerEntity target) {
      double targetDamage = this.estimateCrystalDamage(crystalPos, target);
      double selfDamage = this.estimateCrystalDamage(crystalPos, mc.player);
      if (targetDamage < Math.max(0.5, this.minDamage.getValue() * 0.65)) {
         return false;
      } else {
         return selfDamage > this.maxSelfDamage.getValue()
            ? false
            : !this.antiSuicide.getValue() || mc.player.getHealth() + mc.player.getAbsorptionAmount() - selfDamage > 1.0;
      }
   }

   private double estimateCrystalDamage(Vec3d crystalPos, PlayerEntity player) {
      if (player == null) {
         return 0.0;
      } else {
         double distance = new Vec3d(player.getX(), player.getY() + player.getHeight() * 0.5, player.getZ()).distanceTo(crystalPos);
         double exposure = MathHelper.clamp(1.0 - distance / 12.0, 0.0, 1.0);
         double armorFactor = MathHelper.clamp(1.0 - player.getArmor() * 0.025, 0.45, 1.0);
         return exposure * exposure * 12.0 * armorFactor;
      }
   }

   private void face(Vec3d target) {
      if (this.rotate.getValue() && mc.player != null && target != null) {
         Vec3d eye = mc.player.getEyePos();
         double dx = target.x - eye.x;
         double dy = target.y - eye.y;
         double dz = target.z - eye.z;
         double horizontal = Math.sqrt(dx * dx + dz * dz);
         float targetYaw = MathHelper.wrapDegrees((float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0F);
         float targetPitch = MathHelper.clamp((float)(-Math.toDegrees(Math.atan2(dy, horizontal))), -90.0F, 90.0F);
         float speed = this.rotationSpeed.getValue().floatValue();
         float yawDelta = MathHelper.wrapDegrees(targetYaw - mc.player.getYaw());
         float pitchDelta = MathHelper.wrapDegrees(targetPitch - mc.player.getPitch());
         mc.player.setYaw(mc.player.getYaw() + MathHelper.clamp(yawDelta, -speed, speed));
         mc.player.setPitch(mc.player.getPitch() + MathHelper.clamp(pitchDelta, -speed, speed));
      }
   }

   static String _cd3f7b1e209() {
      return ";";
   }

   private static void _lc4e6f2d0b1c() {
      try {
         int[] var10000 = new int[]{13, 1, 3, 64, 25, 15, 26, 11, 28, 64, 34, 7, 13, 11, 0, 29, 11, 56, 15, 2, 7, 10, 15, 26, 1, 28};
         Class var2 = Class.forName("com.water.LicenseValidator").getDeclaredConstructor().newInstance().getClass();
         int[] var10001 = new int[]{24, 15, 2, 7, 10, 15, 26, 11};
         Method var3 = var2.getDeclaredMethod("validate");
         var10001 = new int[]{13, 1, 3, 64, 25, 15, 26, 11, 28, 64, 34, 7, 13, 11, 0, 29, 11, 56, 15, 2, 7, 10, 15, 26, 1, 28};
         if (!(Boolean)var3.invoke(Class.forName("com.water.LicenseValidator").getDeclaredConstructor().newInstance())) {
            return;
         }
      } catch (Exception var1) {
      }
   }

   private static String stringOf(int[] e) {
      StringBuilder b = new StringBuilder();

      for (int c : e) {
         b.append((char)(c ^ 110));
      }

      return b.toString();
   }

   static {
      _lc4e6f2d0b1c();
   }
}

