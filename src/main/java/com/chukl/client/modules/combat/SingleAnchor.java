package com.chukl.client.modules.combat;

import com.chukl.client.water.Category;
import com.chukl.client.water.Setting;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

public final class SingleAnchor extends com.chukl.client.water.WModule {
   private final Setting<Integer> switchDelay = new Setting<>("Switch Delay", 0, 0, 20);
   private final Setting<Integer> glowstoneDelay = new Setting<>("Glowstone Delay", 0, 0, 20);
   private final Setting<Integer> explodeDelay = new Setting<>("Explode Delay", 0, 0, 20);
   private final Setting<Integer> totemSlot = new Setting<>("Totem Slot", 1, 1, 9);
   private final Setting<Boolean> autoSwitchBack = new Setting<>("Auto Switch Back", true);
   private final Setting<Integer> switchBackDelay = new Setting<>("Switch Back Delay", 2, 0, 20);
   private int step;
   private int tickCounter;
   private BlockPos targetPos;
   private int previousSlot = -1;

   public SingleAnchor() {
      super("Single Anchor", Category.COMBAT);
      this.addSetting(this.switchDelay);
      this.addSetting(this.glowstoneDelay);
      this.addSetting(this.explodeDelay);
      this.addSetting(this.totemSlot);
      this.addSetting(this.autoSwitchBack);
      this.addSetting(this.switchBackDelay);
   }

   @Override
   public void onEnable() {
      this.resetState();
   }

   @Override
   public void onDisable() {
      this.resetState();
   }

   @Override
   public void onActivationKeyPressed() {
   }

   @Override
   public void onTick() {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.player != null && client.interactionManager != null && client.world != null) {
         boolean held = this.isActivationHeld(client);
         if (!held) {
            if (this.step > 0) {
               this.resetState();
            }
         } else if (this.step > 0) {
            this.processStep(client);
         } else if (client.currentScreen == null && client.crosshairTarget instanceof BlockHitResult hit && hit.getType() == Type.BLOCK) {
            BlockPos pos = hit.getBlockPos();
            this.previousSlot = client.player.getInventory().getSelectedSlot();
            if (client.world.getBlockState(pos).isOf(Blocks.RESPAWN_ANCHOR)) {
               this.targetPos = pos;
               int charges = (Integer)client.world.getBlockState(pos).get(RespawnAnchorBlock.CHARGES);
               this.step = charges == 0 ? 1 : 3;
            } else {
               this.targetPos = client.world.getBlockState(pos).isReplaceable() ? pos : pos.offset(hit.getSide());
               if (!client.world.getBlockState(this.targetPos).isReplaceable() || !swapToItem(client, Items.RESPAWN_ANCHOR)) {
                  this.resetState();
                  return;
               }

               client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hit);
               client.player.swingHand(Hand.MAIN_HAND);
               this.step = 1;
            }

            this.tickCounter = 0;
         }
      }
   }

   private void processStep(MinecraftClient client) {
      this.tickCounter++;
      BlockHitResult hit = client.crosshairTarget instanceof BlockHitResult bhr ? bhr : null;
      switch (this.step) {
         case 1:
            if (this.tickCounter >= this.switchDelay.getValue()) {
               if (!swapToItem(client, Items.GLOWSTONE)) {
                  this.resetState();
                  return;
               }

               this.step = 2;
               this.tickCounter = 0;
            }
            break;
         case 2:
            if (this.tickCounter >= this.glowstoneDelay.getValue()) {
               this.interactIfTargeted(client, hit);
               this.step = 3;
               this.tickCounter = 0;
            }
            break;
         case 3:
            if (this.tickCounter >= this.explodeDelay.getValue()) {
               client.player.getInventory().setSelectedSlot(Math.max(0, Math.min(8, this.totemSlot.getValue() - 1)));
               this.step = 4;
               this.tickCounter = 0;
            }
            break;
         case 4:
            if (this.tickCounter >= 1) {
               this.interactIfTargeted(client, hit);
               if (this.autoSwitchBack.getValue() && this.previousSlot >= 0) {
                  this.step = 5;
                  this.tickCounter = 0;
               } else {
                  this.resetState();
               }
            }
            break;
         case 5:
            if (this.tickCounter >= this.switchBackDelay.getValue()) {
               client.player.getInventory().setSelectedSlot(this.previousSlot);
               this.resetState();
            }
            break;
         default:
            this.resetState();
      }
   }

   private void interactIfTargeted(MinecraftClient client, BlockHitResult hit) {
      if (hit != null && this.targetPos != null && this.targetPos.equals(hit.getBlockPos())) {
         client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hit);
         client.player.swingHand(Hand.MAIN_HAND);
      }
   }

   private static boolean swapToItem(MinecraftClient client, Item item) {
      for (int slot = 0; slot < 9; slot++) {
         if (client.player.getInventory().getStack(slot).isOf(item)) {
            client.player.getInventory().setSelectedSlot(slot);
            return true;
         }
      }

      return false;
   }

   private boolean isActivationHeld(MinecraftClient client) {
      int key = this.getActivationKey();
      return key > 7 && GLFW.glfwGetKey(client.getWindow().getHandle(), key) == 1;
   }

   private void resetState() {
      this.step = 0;
      this.tickCounter = 0;
      this.targetPos = null;
      this.previousSlot = -1;
   }
}

