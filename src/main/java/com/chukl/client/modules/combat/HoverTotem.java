package com.chukl.client.modules.combat;

import com.chukl.client.mixin.HandledScreenAccessor;
import com.chukl.client.water.Category;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

public final class HoverTotem extends com.chukl.client.water.WModule {
   private int lastSwapAttemptHandlerSlotId = -1;

   public HoverTotem() {
      super("Hover Totem", Category.COMBAT);
   }

   @Override
   public void onDisable() {
      this.lastSwapAttemptHandlerSlotId = -1;
      super.onDisable();
   }

   @Override
   public void onTick() {
      if (mc.player != null && mc.interactionManager != null) {
         if (mc.currentScreen instanceof HandledScreen<?> handled) {
            Slot focused = ((HandledScreenAccessor)handled).chukl$getFocusedSlot();
            if (focused != null && !focused.getStack().isEmpty()) {
               if (!focused.getStack().isOf(Items.TOTEM_OF_UNDYING)) {
                  this.lastSwapAttemptHandlerSlotId = -1;
               } else if (!mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
                  if (focused.id != this.lastSwapAttemptHandlerSlotId) {
                     int syncId = mc.player.currentScreenHandler.syncId;
                     mc.interactionManager.clickSlot(syncId, focused.id, 40, SlotActionType.SWAP, mc.player);
                     this.lastSwapAttemptHandlerSlotId = focused.id;
                  }
               }
            } else {
               this.lastSwapAttemptHandlerSlotId = -1;
            }
         } else {
            this.lastSwapAttemptHandlerSlotId = -1;
         }
      }
   }
}

