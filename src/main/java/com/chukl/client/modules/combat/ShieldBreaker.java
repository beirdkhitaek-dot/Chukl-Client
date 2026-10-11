package com.chukl.client.modules.combat;

import com.chukl.client.water.Category;
import com.chukl.client.water.Setting;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult.Type;

public final class ShieldBreaker extends com.chukl.client.water.WModule {
   private final Setting<Boolean> switchBack = new Setting<>("Switch Back", true);
   private final Setting<Float> switchDelayMs = new Setting<>("Switch Delay", 0.0F, 0.0F, 500.0F);
   private boolean isBlockingState = false;
   private long firstDetectedTime = -1L;
   private boolean hasAttacked = false;
   private boolean needsSwitchBack = false;
   private int previousSlot = -1;

   public ShieldBreaker() {
      super("Shield Breaker", Category.COMBAT);
      this.addSetting(this.switchBack);
      this.addSetting(this.switchDelayMs);
   }

   @Override
   public void onEnable() {
      this.resetState();
      super.onEnable();
   }

   @Override
   public void onDisable() {
      if (this.hasAttacked && this.switchBack.getValue() && this.needsSwitchBack && mc.player != null) {
         this.swapToSlot(this.previousSlot);
      }

      this.resetState();
      super.onDisable();
   }

   private void resetState() {
      this.isBlockingState = false;
      this.firstDetectedTime = -1L;
      this.hasAttacked = false;
      this.needsSwitchBack = false;
      this.previousSlot = -1;
   }

   @Override
   public void onTick() {
      if (mc.player != null && mc.world != null) {
         PlayerEntity target = this.getTargetPlayer();
         if (target != null && target.isBlocking()) {
            if (!this.isBlockingState) {
               this.isBlockingState = true;
               this.firstDetectedTime = System.currentTimeMillis();
            }

            long delay = this.switchDelayMs.getValue().longValue();
            if (!this.hasAttacked && this.firstDetectedTime >= 0L && System.currentTimeMillis() - this.firstDetectedTime >= delay) {
               int bestAxeSlot = this.findAxeSlot();
               if (bestAxeSlot != -1) {
                  this.previousSlot = mc.player.getInventory().getSelectedSlot();
                  if (this.previousSlot != bestAxeSlot) {
                     this.swapToSlot(bestAxeSlot);
                     this.needsSwitchBack = true;
                  }

                  mc.interactionManager.attackEntity(mc.player, target);
                  mc.player.swingHand(Hand.MAIN_HAND);
                  this.hasAttacked = true;
               }
            }
         } else {
            if (this.isBlockingState) {
               this.isBlockingState = false;
               this.firstDetectedTime = -1L;
            }

            if (this.hasAttacked) {
               if (this.switchBack.getValue() && this.needsSwitchBack && this.previousSlot != -1) {
                  this.swapToSlot(this.previousSlot);
               }

               this.hasAttacked = false;
               this.needsSwitchBack = false;
               this.previousSlot = -1;
            }
         }
      }
   }

   private PlayerEntity getTargetPlayer() {
      return mc.crosshairTarget != null
            && mc.crosshairTarget.getType() == Type.ENTITY
            && ((EntityHitResult)mc.crosshairTarget).getEntity() instanceof PlayerEntity player
            && player != mc.player
         ? player
         : null;
   }

   private int findAxeSlot() {
      int bestScore = -1;
      int bestSlot = -1;

      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem() instanceof AxeItem) {
            int score = this.getAxeStrengthScore(stack);
            if (score > bestScore) {
               bestScore = score;
               bestSlot = i;
            }
         }
      }

      return bestSlot;
   }

   private int getAxeStrengthScore(ItemStack stack) {
      if (stack.isOf(Items.NETHERITE_AXE)) {
         return 6;
      } else if (stack.isOf(Items.DIAMOND_AXE)) {
         return 5;
      } else if (stack.isOf(Items.IRON_AXE)) {
         return 4;
      } else if (stack.isOf(Items.GOLDEN_AXE)) {
         return 3;
      } else if (stack.isOf(Items.STONE_AXE)) {
         return 2;
      } else {
         return stack.isOf(Items.WOODEN_AXE) ? 1 : 0;
      }
   }

   private void swapToSlot(int slot) {
      if (mc.player != null) {
         if (slot >= 0 && slot <= 8) {
            mc.player.getInventory().setSelectedSlot(slot);
         }
      }
   }
}

