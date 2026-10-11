package com.chukl.client.modules.combat;

import com.chukl.client.water.Category;
import com.chukl.client.water.Setting;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

public class AutoTotem extends com.chukl.client.water.WModule {
   private final Setting<Float> delay = new Setting<>("Delay", 4.0F, 1.0F, 40.0F);
   private final Setting<Boolean> forceTotem = new Setting<>("Force Totem", false);
   private final Setting<Boolean> hotbarTotem = new Setting<>("Hotbar Totem", false);
   private final Setting<Float> hotbarSlot = new Setting<>("Hotbar Slot", 1.0F, 1.0F, 9.0F);
   private final Setting<Float> hotbarDelay = new Setting<>("Hotbar Delay", 4.0F, 1.0F, 40.0F);
   private int tickCounter;
   private int pendingHotbarSlot = -1;

   public AutoTotem() {
      this("Auto Totem");
   }

   protected AutoTotem(String name) {
      super(name, Category.COMBAT);
      this.addSetting(this.delay);
      this.addSetting(this.forceTotem);
      this.addSetting(this.hotbarTotem);
      this.addSetting(this.hotbarSlot);
      this.addSetting(this.hotbarDelay);
   }

   @Override
   public void onEnable() {
      this.tickCounter = 0;
      this.pendingHotbarSlot = -1;
   }

   @Override
   public void onDisable() {
      this.tickCounter = 0;
      this.pendingHotbarSlot = -1;
   }

   @Override
   public void onTick() {
      if (mc.player != null && mc.interactionManager != null && this.canRun()) {
         this.tickCounter++;
         int wait = this.hotbarTotem.getValue() && this.needsHotbarWork() && !this.needsOffhandWork()
            ? this.hotbarDelay.getValue().intValue()
            : this.delay.getValue().intValue();
         if (this.tickCounter >= Math.max(1, wait)) {
            this.tickCounter = 0;
            int configuredHotbar = Math.max(0, Math.min(8, this.hotbarSlot.getValue().intValue() - 1));
            if (this.hotbarTotem.getValue() && this.needsHotbarWork()) {
               if (mc.player.getInventory().getStack(configuredHotbar).isOf(Items.TOTEM_OF_UNDYING)) {
                  mc.player.getInventory().setSelectedSlot(configuredHotbar);
               } else {
                  int source = this.findTotemInMainInventory();
                  if (source != -1 && this.canContainerClick()) {
                     this.swapInventoryToHotbar(source, configuredHotbar);
                  }
               }
            } else if (!this.needsOffhandWork()) {
               this.pendingHotbarSlot = -1;
            } else {
               int hotbar = this.findTotemInHotbar();
               if (hotbar != -1) {
                  this.pendingHotbarSlot = hotbar;
                  mc.player.getInventory().setSelectedSlot(hotbar);
                  this.swapSlotToOffhand(hotbar);
                  this.pendingHotbarSlot = -1;
               } else if (this.pendingHotbarSlot >= 0 && mc.player.getInventory().getStack(this.pendingHotbarSlot).isOf(Items.TOTEM_OF_UNDYING)) {
                  mc.player.getInventory().setSelectedSlot(this.pendingHotbarSlot);
                  this.swapSlotToOffhand(this.pendingHotbarSlot);
                  this.pendingHotbarSlot = -1;
               } else {
                  int mainSlot = this.findTotemInMainInventory();
                  if (mainSlot != -1 && this.canContainerClick()) {
                     int target = this.hotbarTotem.getValue() ? configuredHotbar : mc.player.getInventory().getSelectedSlot();
                     this.swapInventoryToHotbar(mainSlot, target);
                     this.pendingHotbarSlot = target;
                  }
               }
            }
         }
      }
   }

   private boolean canRun() {
      return mc.currentScreen == null || mc.currentScreen instanceof InventoryScreen;
   }

   private boolean canContainerClick() {
      return mc.currentScreen != null || !this.isMoving();
   }

   private boolean isMoving() {
      if (!mc.options.forwardKey.isPressed()
         && !mc.options.backKey.isPressed()
         && !mc.options.leftKey.isPressed()
         && !mc.options.rightKey.isPressed()
         && !mc.options.jumpKey.isPressed()
         && !mc.player.isSprinting()
         && !mc.player.isSneaking()) {
         double x = mc.player.getVelocity().x;
         double z = mc.player.getVelocity().z;
         return x * x + z * z > 0.0025;
      } else {
         return true;
      }
   }

   private boolean needsOffhandWork() {
      return this.forceTotem.getValue() || !mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING);
   }

   private boolean needsHotbarWork() {
      if (!this.hotbarTotem.getValue()) {
         return false;
      } else {
         int slot = Math.max(0, Math.min(8, this.hotbarSlot.getValue().intValue() - 1));
         return !mc.player.getInventory().getStack(slot).isOf(Items.TOTEM_OF_UNDYING);
      }
   }

   private int findTotemInMainInventory() {
      for (int i = 9; i < 36; i++) {
         if (mc.player.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) {
            return i;
         }
      }

      return -1;
   }

   private int findTotemInHotbar() {
      for (int i = 0; i < 9; i++) {
         if (mc.player.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) {
            return i;
         }
      }

      return -1;
   }

   private void swapInventoryToHotbar(int source, int hotbar) {
      mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, source, hotbar, SlotActionType.SWAP, mc.player);
   }

   private void swapSlotToOffhand(int inventorySlot) {
      int handlerSlot = inventorySlot < 9 ? 36 + inventorySlot : inventorySlot;
      mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, handlerSlot, 40, SlotActionType.SWAP, mc.player);
   }
}

