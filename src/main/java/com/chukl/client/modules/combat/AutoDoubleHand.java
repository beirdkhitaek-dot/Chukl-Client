package com.chukl.client.modules.combat;

import com.chukl.client.water.Category;
import com.chukl.client.water.Setting;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;

public final class AutoDoubleHand extends com.chukl.client.water.WModule {
   private final Setting<Boolean> onTotemPop = new Setting<>("On Totem Pop", true);
   private final Setting<Boolean> onHealth = new Setting<>("On Health", true);
   private final Setting<Float> healthThreshold = new Setting<>("Health Threshold", 6.0F, 1.0F, 20.0F);
   private final Setting<Float> cooldown = new Setting<>("Cooldown", 5.0F, 0.0F, 40.0F);
   private boolean hadTotemHeldLastTick = false;
   private int cooldownTicks = 0;
   private int previousSlot = -1;

   public AutoDoubleHand() {
      super("AutoDoubleHand", Category.COMBAT);
      this.addSetting(this.onTotemPop);
      this.addSetting(this.onHealth);
      this.addSetting(this.healthThreshold);
      this.addSetting(this.cooldown);
   }

   @Override
   public void onEnable() {
      this.hadTotemHeldLastTick = false;
      this.cooldownTicks = 0;
      this.previousSlot = -1;
   }

   @Override
   public void onDisable() {
      this.hadTotemHeldLastTick = false;
      this.cooldownTicks = 0;
      this.previousSlot = -1;
   }

   @Override
   public void onTick() {
      if (mc.player != null && mc.interactionManager != null) {
         if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
         }

         PlayerInventory inv = mc.player.getInventory();
         boolean totemMain = mc.player.getMainHandStack().getItem() == Items.TOTEM_OF_UNDYING;
         boolean totemOff = mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING;
         boolean totemHeld = totemMain || totemOff;
         boolean popped = this.hadTotemHeldLastTick && !totemHeld;
         this.hadTotemHeldLastTick = totemHeld;
         if (this.cooldownTicks <= 0) {
            boolean shouldSwitch = false;
            if (this.onTotemPop.getValue() && popped) {
               shouldSwitch = true;
            }

            if (this.onHealth.getValue() && mc.player.getHealth() <= this.healthThreshold.getValue()) {
               shouldSwitch = true;
            }

            if (shouldSwitch) {
               if (!totemMain) {
                  int hotbarSlot = this.findHotbarTotemSlot();
                  if (hotbarSlot >= 0) {
                     if (inv.getSelectedSlot() != hotbarSlot) {
                        this.previousSlot = inv.getSelectedSlot();
                        inv.setSelectedSlot(hotbarSlot);
                        this.cooldownTicks = this.cooldown.getValue().intValue();
                     }
                  }
               }
            }
         }
      }
   }

   private int findHotbarTotemSlot() {
      PlayerInventory inv = mc.player.getInventory();

      for (int i = 0; i < 9; i++) {
         if (inv.getStack(i).isOf(Items.TOTEM_OF_UNDYING)) {
            return i;
         }
      }

      return -1;
   }
}

