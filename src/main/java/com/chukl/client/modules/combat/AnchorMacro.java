package com.chukl.client.modules.combat;

import com.chukl.client.water.Category;
import com.chukl.client.water.Setting;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.ShieldItem;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

public final class AnchorMacro extends com.chukl.client.water.WModule {
   private final Setting<Float> switchDelay = new Setting<>("Switch Delay", 0.0F, 0.0F, 20.0F);
   private final Setting<Float> glowstoneDelay = new Setting<>("Glowstone Delay", 0.0F, 0.0F, 20.0F);
   private final Setting<Float> explodeDelay = new Setting<>("Explode Delay", 0.0F, 0.0F, 20.0F);
   private final Setting<Float> totemSlot = new Setting<>("Totem Slot", 1.0F, 1.0F, 9.0F);
   private final Setting<Boolean> switchBack = new Setting<>("Switch Back", false);
   private int switchCounter;
   private int glowstoneDelayCounter;
   private int explodeDelayCounter;
   private boolean waitingForPop = false;

   public AnchorMacro() {
      super("Anchor Macro", Category.COMBAT);
      this.addSetting(this.switchDelay);
      this.addSetting(this.glowstoneDelay);
      this.addSetting(this.explodeDelay);
      this.addSetting(this.totemSlot);
      this.addSetting(this.switchBack);
   }

   @Override
   public void onEnable() {
      this.resetCounters();
      this.waitingForPop = false;
      super.onEnable();
   }

   @Override
   public void onDisable() {
      this.resetCounters();
      this.waitingForPop = false;
      super.onDisable();
   }

   @Override
   public void onPacketReceive(Packet<?> packet) {
      if (this.switchBack.getValue() && this.waitingForPop && packet instanceof HealthUpdateS2CPacket) {
         this.waitingForPop = false;
         if (mc.player != null) {
            int anchorSlot = this.findItemSlot(Items.RESPAWN_ANCHOR);
            if (anchorSlot != -1) {
               mc.player.getInventory().setSelectedSlot(anchorSlot);
            }
         }
      }
   }

   @Override
   public void onTick() {
      if (mc.player != null && mc.world != null && mc.interactionManager != null) {
         if (mc.currentScreen == null) {
            if (!this.isShieldOrFoodActive()) {
               if (!this.isRightClickHeld()) {
                  this.resetCounters();
               } else {
                  this.handleAnchorInteraction();
               }
            }
         }
      }
   }

   private boolean isShieldOrFoodActive() {
      boolean isFood = mc.player.getMainHandStack().getItem().getComponents().contains(DataComponentTypes.FOOD)
         || mc.player.getOffHandStack().getItem().getComponents().contains(DataComponentTypes.FOOD);
      boolean isShield = mc.player.getMainHandStack().getItem() instanceof ShieldItem || mc.player.getOffHandStack().getItem() instanceof ShieldItem;
      boolean rightClickPressed = this.isRightClickHeld();
      return (isFood || isShield) && rightClickPressed;
   }

   private boolean isRightClickHeld() {
      return mc.getWindow() != null && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), 1) == 1;
   }

   private void handleAnchorInteraction() {
      if (mc.crosshairTarget instanceof BlockHitResult blockHitResult) {
         if (blockHitResult.getType() == Type.BLOCK) {
            BlockPos pos = blockHitResult.getBlockPos();
            BlockState state = mc.world.getBlockState(pos);
            if (state.isOf(Blocks.RESPAWN_ANCHOR)) {
               mc.options.useKey.setPressed(false);
               int charges = (Integer)state.get(RespawnAnchorBlock.CHARGES);
               if (charges == 0) {
                  this.placeGlowstone(blockHitResult);
               } else {
                  this.explodeAnchor(blockHitResult);
               }
            }
         }
      }
   }

   private void placeGlowstone(BlockHitResult blockHitResult) {
      if (!mc.player.getMainHandStack().isOf(Items.GLOWSTONE)) {
         if (this.switchCounter < this.switchDelay.getValue().intValue()) {
            this.switchCounter++;
            return;
         }

         this.switchCounter = 0;
         if (!this.swapToItem(Items.GLOWSTONE)) {
            return;
         }
      }

      if (mc.player.getMainHandStack().isOf(Items.GLOWSTONE)) {
         if (this.glowstoneDelayCounter < this.glowstoneDelay.getValue().intValue()) {
            this.glowstoneDelayCounter++;
            return;
         }

         this.glowstoneDelayCounter = 0;
         this.interactWith(blockHitResult);
      }
   }

   private void explodeAnchor(BlockHitResult blockHitResult) {
      int selectedSlot = Math.max(0, Math.min(8, this.totemSlot.getValue().intValue() - 1));
      if (mc.player.getInventory().getSelectedSlot() != selectedSlot) {
         if (this.switchCounter < this.switchDelay.getValue().intValue()) {
            this.switchCounter++;
            return;
         }

         this.switchCounter = 0;
         mc.player.getInventory().setSelectedSlot(selectedSlot);
      }

      if (mc.player.getInventory().getSelectedSlot() == selectedSlot) {
         if (this.explodeDelayCounter < this.explodeDelay.getValue().intValue()) {
            this.explodeDelayCounter++;
            return;
         }

         this.explodeDelayCounter = 0;
         this.interactWith(blockHitResult);
         if (this.switchBack.getValue()) {
            this.waitingForPop = true;
         }
      }
   }

   private int findItemSlot(Item item) {
      for (int i = 0; i < 9; i++) {
         if (mc.player.getInventory().getStack(i).isOf(item)) {
            return i;
         }
      }

      return -1;
   }

   private boolean swapToItem(Item item) {
      int slot = this.findItemSlot(item);
      if (slot != -1) {
         mc.player.getInventory().setSelectedSlot(slot);
         return true;
      } else {
         return false;
      }
   }

   private void interactWith(BlockHitResult hit) {
      mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
      mc.player.swingHand(Hand.MAIN_HAND);
   }

   private void resetCounters() {
      this.switchCounter = 0;
      this.glowstoneDelayCounter = 0;
      this.explodeDelayCounter = 0;
   }
}

