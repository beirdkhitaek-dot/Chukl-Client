package com.chukl.client.modules.combat;

import com.chukl.client.water.Category;
import com.chukl.client.water.Friends;
import com.chukl.client.water.Setting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult.Type;

public final class Triggerbot extends com.chukl.client.water.WModule {
   private static final int FIXED_DELAY_TICKS = 9;
   private final Setting<Boolean> onlyCrit = new Setting<>("Only Crit", false);
   private final Setting<Boolean> checkShield = new Setting<>("Check Shield", false);
   private int delayCounter = 0;

   public Triggerbot() {
      super("Triggerbot", Category.COMBAT);
      this.addSetting(this.onlyCrit);
      this.addSetting(this.checkShield);
   }

   @Override
   public void onEnable() {
      this.delayCounter = 0;
   }

   @Override
   public void onDisable() {
      this.delayCounter = 0;
   }

   @Override
   public void onTick() {
      if (mc.player != null && mc.world != null) {
         if (mc.currentScreen == null) {
            if (this.delayCounter > 0) {
               this.delayCounter--;
            } else if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == Type.ENTITY) {
               if (mc.crosshairTarget instanceof EntityHitResult entityHitResult) {
                  Entity entity = entityHitResult.getEntity();
                  if (entity instanceof LivingEntity) {
                     if (entity != mc.player) {
                        if (!(entity instanceof PlayerEntity p && Friends.isAntiTriggerbot() && Friends.isFriend(p.getName().getString()))) {
                           if (!mc.options.attackKey.isPressed()) {
                              if (this.passesFilters((LivingEntity)entity)) {
                                 this.performAttack(entity);
                                 this.delayCounter = 9;
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

   private boolean passesFilters(LivingEntity entity) {
      return this.onlyCrit.getValue() && !this.isValidCrit(mc.player) ? false : !this.checkShield.getValue() || !this.isHoldingShield(entity);
   }

   private boolean isHoldingShield(LivingEntity entity) {
      ItemStack main = entity.getMainHandStack();
      ItemStack off = entity.getOffHandStack();
      return main.getItem() == Items.SHIELD || off.getItem() == Items.SHIELD;
   }

   private boolean isValidCrit(PlayerEntity p) {
      if (p.fallDistance <= 0.05F) {
         return false;
      } else if (p.isOnGround()) {
         return false;
      } else {
         return !p.isTouchingWater() && !p.isInLava() && !p.isClimbing() && !p.hasVehicle() ? !p.isSprinting() : false;
      }
   }

   private void performAttack(Entity target) {
      mc.interactionManager.attackEntity(mc.player, target);
      mc.player.swingHand(Hand.MAIN_HAND);
   }
}

