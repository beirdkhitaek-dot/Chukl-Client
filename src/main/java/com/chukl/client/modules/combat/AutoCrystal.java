package com.chukl.client.modules.combat;

import com.chukl.client.water.Category;
import com.chukl.client.water.Setting;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public final class AutoCrystal extends com.chukl.client.water.WModule {
   private final Setting<Integer> breakDelay = new Setting<>("Break Delay", 1, 0, 20);
   private final Setting<Integer> placeDelay = new Setting<>("Place Delay", 1, 0, 20);
   private final Setting<Boolean> autoObsidian = new Setting<>("Auto Obsidian", true);
   private final Setting<Integer> obsidianDelay = new Setting<>("Obsidian Delay", 2, 0, 20);
   private final Setting<Double> range = new Setting<>("Break Range", 3.0, 1.0, 6.0);
   private final Setting<Boolean> switchBack = new Setting<>("Switch Back", true);
   private int breakCooldown;
   private int placeCooldown;
   private int obsidianCooldown;
   private int savedSlot = -1;
   private int lastBrokenId = -1;
   private boolean wasActive;
   private volatile boolean blockedUntilRelease;

   public AutoCrystal() {
      super("Auto Crystal", Category.COMBAT);
      this.addSetting(this.breakDelay);
      this.addSetting(this.placeDelay);
      this.addSetting(this.autoObsidian);
      this.addSetting(this.obsidianDelay);
      this.addSetting(this.range);
      this.addSetting(this.switchBack);
   }

   @Override
   public void onEnable() {
      this.resetAll();
   }

   @Override
   public void onDisable() {
      this.restoreSlot();
      this.resetAll();
   }

   @Override
   public void onActivationKeyPressed() {
   }

   @Override
   public void onPacketReceive(Packet<?> packet) {
      if (this.wasActive && mc.world != null && packet instanceof EntityStatusS2CPacket status && status.getStatus() == 3) {
         Entity dead = status.getEntity(mc.world);
         if (dead instanceof PlayerEntity && dead != mc.player) {
            this.blockedUntilRelease = true;
            this.wasActive = false;
            this.breakCooldown = this.placeCooldown = this.obsidianCooldown = 0;
            this.lastBrokenId = -1;
            mc.execute(this::restoreSlot);
         }
      }
   }

   @Override
   public void onTick() {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.player != null && client.interactionManager != null && client.world != null && client.currentScreen == null && this.isActivationHeld(client)) {
         this.wasActive = true;
         this.tickCooldowns();
         HitResult target = client.crosshairTarget;
         if (target instanceof EntityHitResult ehr && ehr.getEntity() instanceof EndCrystalEntity crystal) {
            if (this.tryBreak(client, crystal)) {
               this.lastBrokenId = crystal.getId();
            }

            return;
         }

         if (target instanceof BlockHitResult hit && hit.getType() == Type.BLOCK) {
            BlockPos clicked = hit.getBlockPos();
            if (isCrystalBase(client.world, clicked)) {
               this.serviceBase(client, hit, clicked);
            } else if (this.autoObsidian.getValue()) {
               this.layObsidian(client, hit, clicked);
            }
         }
      } else {
         this.endActiveHold();
      }
   }

   private void serviceBase(MinecraftClient client, BlockHitResult hit, BlockPos base) {
      EndCrystalEntity crystal = crystalOn(client.world, base);
      if (crystal != null) {
         if (this.tryBreak(client, crystal)) {
            this.lastBrokenId = crystal.getId();
         }
      } else {
         this.lastBrokenId = -1;
         if (this.placeCooldown == 0 && canFitCrystal(client.world, base)) {
            Hand hand = this.equip(client, Items.END_CRYSTAL);
            if (hand != null) {
               client.interactionManager.interactBlock(client.player, hand, hit);
               client.player.swingHand(hand);
               this.placeCooldown = this.placeDelay.getValue();
            }
         }
      }
   }

   private void layObsidian(MinecraftClient client, BlockHitResult hit, BlockPos clicked) {
      if (this.obsidianCooldown == 0) {
         BlockPos target = client.world.getBlockState(clicked).isReplaceable() ? clicked : clicked.offset(hit.getSide());
         if (client.world.getBlockState(target).isReplaceable() && canFitCrystal(client.world, target)) {
            Hand hand = this.equip(client, Items.OBSIDIAN);
            if (hand != null) {
               client.interactionManager.interactBlock(client.player, hand, hit);
               client.player.swingHand(hand);
               this.obsidianCooldown = Math.max(1, this.obsidianDelay.getValue());
            }
         }
      }
   }

   private boolean tryBreak(MinecraftClient client, EndCrystalEntity crystal) {
      if (this.breakCooldown == 0
         && crystal.getId() != this.lastBrokenId
         && !(client.player.getEyePos().distanceTo(crystal.getEntityPos()) > this.range.getValue())) {
         client.interactionManager.attackEntity(client.player, crystal);
         client.player.swingHand(Hand.MAIN_HAND);
         this.breakCooldown = this.breakDelay.getValue();
         return true;
      } else {
         return false;
      }
   }

   @Nullable
   private Hand equip(MinecraftClient client, Item item) {
      ClientPlayerEntity player = client.player;
      if (player.getMainHandStack().isOf(item)) {
         return Hand.MAIN_HAND;
      } else if (player.getOffHandStack().isOf(item)) {
         return Hand.OFF_HAND;
      } else {
         int slot = findHotbarSlot(player, item);
         if (slot < 0) {
            return null;
         } else {
            if (this.savedSlot < 0) {
               this.savedSlot = player.getInventory().getSelectedSlot();
            }

            player.getInventory().setSelectedSlot(slot);
            return Hand.MAIN_HAND;
         }
      }
   }

   private static int findHotbarSlot(ClientPlayerEntity player, Item item) {
      for (int slot = 0; slot < 9; slot++) {
         if (player.getInventory().getStack(slot).isOf(item)) {
            return slot;
         }
      }

      return -1;
   }

   private static boolean isCrystalBase(World world, BlockPos pos) {
      BlockState state = world.getBlockState(pos);
      return state.isOf(Blocks.OBSIDIAN) || state.isOf(Blocks.BEDROCK);
   }

   @Nullable
   private static EndCrystalEntity crystalOn(World world, BlockPos base) {
      BlockPos up = base.up();
      Box region = new Box(up.getX() + 0.125, up.getY() - 0.1, up.getZ() + 0.125, up.getX() + 0.875, up.getY() + 2.5, up.getZ() + 0.875);
      List<EndCrystalEntity> found = world.getEntitiesByClass(EndCrystalEntity.class, region, entity -> true);
      return found.isEmpty() ? null : found.getFirst();
   }

   private static boolean canFitCrystal(World world, BlockPos base) {
      BlockPos up = base.up();
      if (!world.getBlockState(up).isAir()) {
         return false;
      } else {
         Box box = new Box(up.getX(), up.getY(), up.getZ(), up.getX() + 1.0, up.getY() + 2.0, up.getZ() + 1.0);
         return world.getNonSpectatingEntities(Entity.class, box).isEmpty();
      }
   }

   private void tickCooldowns() {
      if (this.breakCooldown > 0) {
         this.breakCooldown--;
      }

      if (this.placeCooldown > 0) {
         this.placeCooldown--;
      }

      if (this.obsidianCooldown > 0) {
         this.obsidianCooldown--;
      }
   }

   private boolean isActivationHeld(MinecraftClient client) {
      int key = this.getActivationKey();
      if (key <= 7) {
         return false;
      } else {
         boolean physicallyHeld = InputUtil.isKeyPressed(client.getWindow(), key);
         if (!physicallyHeld) {
            this.blockedUntilRelease = false;
         }

         return physicallyHeld && !this.blockedUntilRelease;
      }
   }

   private void endActiveHold() {
      if (this.wasActive) {
         this.restoreSlot();
         this.breakCooldown = this.placeCooldown = this.obsidianCooldown = 0;
         this.lastBrokenId = -1;
         this.wasActive = false;
      }
   }

   private void restoreSlot() {
      if (this.switchBack.getValue() && this.savedSlot >= 0 && mc.player != null) {
         mc.player.getInventory().setSelectedSlot(this.savedSlot);
      }

      this.savedSlot = -1;
   }

   private void resetAll() {
      this.breakCooldown = this.placeCooldown = this.obsidianCooldown = 0;
      this.savedSlot = -1;
      this.lastBrokenId = -1;
      this.wasActive = false;
      this.blockedUntilRelease = false;
   }
}

