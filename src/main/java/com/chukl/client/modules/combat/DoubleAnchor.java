package com.chukl.client.modules.combat;

import com.chukl.client.water.Category;
import com.chukl.client.water.Setting;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

public final class DoubleAnchor extends com.chukl.client.water.WModule {
   private final Setting<Float> switchDelay = new Setting<>("Delay", 0.0F, 0.0F, 20.0F);
   private final Setting<Float> totemSlot = new Setting<>("Totem Slot", 1.0F, 1.0F, 9.0F);
   private final Setting<Boolean> switchBack = new Setting<>("Switch Back", false);
   private int delayCounter = 0;
   private int step = 0;
   private boolean isAnchoring = false;
   private BlockPos lastAnchorPos = null;
   private boolean waitingForPop = false;

   public DoubleAnchor() {
      super("Double Anchor", Category.COMBAT);
      this.addSetting(this.switchDelay);
      this.addSetting(this.totemSlot);
      this.addSetting(this.switchBack);
   }

   @Override
   public void onEnable() {
      this.resetState();
      this.isAnchoring = false;
      this.waitingForPop = false;
      this.lastAnchorPos = null;
      super.onEnable();
   }

   @Override
   public void onDisable() {
      this.resetState();
      this.isAnchoring = false;
      this.waitingForPop = false;
      this.lastAnchorPos = null;
      super.onDisable();
   }

   @Override
   public void onBindPressed() {
      super.onBindPressed();
   }

   @Override
   public void onActivationKeyPressed() {
      if (this.isEnabled()) {
         this.resetState();
         this.isAnchoring = true;
         this.waitingForPop = false;
         this.lastAnchorPos = null;
      }
   }

   @Override
   public void onPacketReceive(Packet<?> packet) {
      if (this.switchBack.getValue() && this.waitingForPop && packet instanceof HealthUpdateS2CPacket) {
         this.waitingForPop = false;
         if (mc.player != null) {
            int anchorSlot = this.findItemInHotbar(Items.RESPAWN_ANCHOR);
            if (anchorSlot != -1) {
               mc.player.getInventory().setSelectedSlot(anchorSlot);
            }
         }
      }
   }

   @Override
   public void onTick() {
      if (this.isAnchoring) {
         if (mc.currentScreen == null) {
            if (mc.player != null && mc.world != null) {
               if (!this.hasRequiredItems()) {
                  this.isAnchoring = false;
                  this.resetState();
               } else if (mc.crosshairTarget instanceof BlockHitResult blockHitResult) {
                  if (mc.world.getBlockState(blockHitResult.getBlockPos()).isOf(Blocks.AIR)) {
                     this.isAnchoring = false;
                     this.resetState();
                  } else {
                     int switchDelayTicks = Math.max(0, this.switchDelay.getValue().intValue());
                     if (this.delayCounter < switchDelayTicks) {
                        this.delayCounter++;
                     } else {
                        if (this.step == 0) {
                           this.swapToItem(Items.RESPAWN_ANCHOR);
                        } else if (this.step == 1) {
                           this.interactWithBlock(blockHitResult);
                        } else if (this.step == 2) {
                           this.swapToItem(Items.GLOWSTONE);
                        } else if (this.step == 3) {
                           this.interactWithBlock(blockHitResult);
                        } else if (this.step == 4) {
                           this.swapToItem(Items.RESPAWN_ANCHOR);
                        } else if (this.step == 5) {
                           this.interactWithBlock(blockHitResult);
                           this.interactWithBlock(blockHitResult);
                        } else if (this.step == 6) {
                           this.swapToItem(Items.GLOWSTONE);
                        } else if (this.step == 7) {
                           this.interactWithBlock(blockHitResult);
                        } else if (this.step == 8) {
                           int desiredSlot = this.totemSlot.getValue().intValue() - 1;
                           this.swapToHotbarSlot(desiredSlot);
                           this.lastAnchorPos = blockHitResult.getBlockPos();
                        } else if (this.step == 9) {
                           this.interactWithBlock(blockHitResult);
                           if (this.switchBack.getValue()) {
                              this.waitingForPop = true;
                           }
                        } else if (this.step == 10) {
                           this.isAnchoring = false;
                           this.resetState();
                           return;
                        }

                        this.step++;
                     }
                  }
               } else {
                  this.isAnchoring = false;
                  this.resetState();
               }
            }
         }
      }
   }

   private void resetState() {
      this.delayCounter = 0;
      this.step = 0;
   }

   private boolean hasRequiredItems() {
      boolean hasAnchor = false;
      boolean hasGlowstone = false;

      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.isOf(Items.RESPAWN_ANCHOR)) {
            hasAnchor = true;
         }

         if (stack.isOf(Items.GLOWSTONE)) {
            hasGlowstone = true;
         }
      }

      return hasAnchor && hasGlowstone;
   }

   private int findItemInHotbar(Item item) {
      for (int i = 0; i < 9; i++) {
         if (mc.player.getInventory().getStack(i).isOf(item)) {
            return i;
         }
      }

      return -1;
   }

   private void swapToItem(Item item) {
      int slot = this.findItemInHotbar(item);
      if (slot != -1) {
         mc.player.getInventory().setSelectedSlot(slot);
      }
   }

   private void swapToHotbarSlot(int slot) {
      if (slot >= 0 && slot <= 8) {
         mc.player.getInventory().setSelectedSlot(slot);
      }
   }

   private void interactWithBlock(BlockHitResult hit) {
      mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
      mc.player.swingHand(Hand.MAIN_HAND);
   }
}

