package com.chukl.client.modules.combat;

import com.chukl.client.water.Category;
import com.chukl.client.water.Setting;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public final class AutoHitCrystal extends com.chukl.client.water.WModule {
   private final Setting<Integer> delay = new Setting<>("Delay (ticks)", 1, 0, 10);
   private int cooldown = 0;

   public AutoHitCrystal() {
      super("AutoHitCrystal", Category.COMBAT);
      this.addSetting(this.delay);
   }

   @Override
   public void onEnable() {
      this.cooldown = 0;
   }

   @Override
   public void onActivationKeyPressed() {
   }

   private boolean isActivationHeld() {
      int key = this.getActivationKey();
      if (key == 0) {
         return true;
      } else if (mc.getWindow() == null) {
         return false;
      } else {
         try {
            return GLFW.glfwGetKey(mc.getWindow().getHandle(), key) == 1;
         } catch (Exception var3) {
            return false;
         }
      }
   }

   @Override
   public void onTick() {
      if (this.cooldown > 0) {
         this.cooldown--;
      } else if (mc.player != null && mc.world != null && mc.interactionManager != null) {
         if (mc.currentScreen == null) {
            if (this.isActivationHeld()) {
               if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == Type.BLOCK) {
                  BlockHitResult lookHit = (BlockHitResult)mc.crosshairTarget;
                  BlockPos surface = lookHit.getBlockPos();
                  BlockPos obsidianPos = surface.up();
                  BlockPos crystalPos = obsidianPos.up();
                  EndCrystalEntity existing = this.findCrystalAt(crystalPos);
                  if (existing != null) {
                     mc.interactionManager.attackEntity(mc.player, existing);
                     mc.player.swingHand(Hand.MAIN_HAND);
                     this.cooldown = this.delay.getValue();
                  } else if (!this.isObsidianLike(obsidianPos)) {
                     int obsSlot = this.findHotbarSlot(Items.OBSIDIAN);
                     if (obsSlot >= 0) {
                        if (mc.player.getInventory().getSelectedSlot() != obsSlot) {
                           mc.player.getInventory().setSelectedSlot(obsSlot);
                        }

                        BlockHitResult placeObs = new BlockHitResult(Vec3d.ofCenter(surface).add(0.0, 0.5, 0.0), Direction.UP, surface, false);
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, placeObs);
                        mc.player.swingHand(Hand.MAIN_HAND);
                        this.cooldown = this.delay.getValue();
                     }
                  } else if (this.canPlaceCrystal(crystalPos)) {
                     int crystalSlot = this.findHotbarSlot(Items.END_CRYSTAL);
                     if (crystalSlot >= 0) {
                        if (mc.player.getInventory().getSelectedSlot() != crystalSlot) {
                           mc.player.getInventory().setSelectedSlot(crystalSlot);
                        }

                        BlockHitResult placeCrystal = new BlockHitResult(Vec3d.ofCenter(obsidianPos).add(0.0, 0.5, 0.0), Direction.UP, obsidianPos, false);
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, placeCrystal);
                        mc.player.swingHand(Hand.MAIN_HAND);
                        this.cooldown = this.delay.getValue();
                     }
                  }
               }
            }
         }
      }
   }

   private boolean isObsidianLike(BlockPos pos) {
      BlockState s = mc.world.getBlockState(pos);
      return s.isOf(Blocks.OBSIDIAN) || s.isOf(Blocks.BEDROCK);
   }

   private boolean canPlaceCrystal(BlockPos crystalPos) {
      if (!mc.world.isAir(crystalPos)) {
         return false;
      } else {
         Box box = new Box(crystalPos.getX(), crystalPos.getY(), crystalPos.getZ(), crystalPos.getX() + 1.0, crystalPos.getY() + 2.0, crystalPos.getZ() + 1.0);
         return mc.world.getOtherEntities(null, box).isEmpty();
      }
   }

   private EndCrystalEntity findCrystalAt(BlockPos pos) {
      Box box = new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0, pos.getY() + 2.0, pos.getZ() + 1.0);

      for (Entity e : mc.world.getOtherEntities(null, box)) {
         if (e instanceof EndCrystalEntity crystal && crystal.isAlive()) {
            return crystal;
         }
      }

      return null;
   }

   private int findHotbarSlot(Item item) {
      for (int i = 0; i < 9; i++) {
         if (mc.player.getInventory().getStack(i).isOf(item)) {
            return i;
         }
      }

      return -1;
   }
}

