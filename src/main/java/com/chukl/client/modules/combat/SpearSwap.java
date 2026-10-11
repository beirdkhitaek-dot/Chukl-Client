package com.chukl.client.modules.combat;

import com.chukl.client.water.Category;
import com.chukl.client.water.Setting;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;

public final class SpearSwap extends com.chukl.client.water.WModule {
   public static SpearSwap INSTANCE;
   private final Setting<Boolean> lunge = new Setting<>("Lunge", true);
   private final Setting<Boolean> sharpness = new Setting<>("Sharpness", false);
   private final Setting<Boolean> onlySword = new Setting<>("Only Sword", false);
   private final Setting<Boolean> onlyAxe = new Setting<>("Only Axe", false);
   private final Setting<Boolean> switchBack = new Setting<>("Switch Back", true);
   private final Setting<Float> switchDelay = new Setting<>("Switch Delay", 1.0F, 1.0F, 20.0F);
   private int previousSlot = -1;
   private int countdown = 0;
   private boolean attackHeldLastCheck = false;

   public SpearSwap() {
      super("SpearSwap", Category.COMBAT);
      this.addSetting(this.lunge);
      this.addSetting(this.sharpness);
      this.addSetting(this.onlySword);
      this.addSetting(this.onlyAxe);
      this.addSetting(this.switchBack);
      this.addSetting(this.switchDelay);
      INSTANCE = this;
   }

   public void preAttack() {
      if (mc.player != null) {
         boolean wasHeld = this.attackHeldLastCheck;
         this.attackHeldLastCheck = true;
         if (!wasHeld) {
            if (this.countdown <= 0) {
               PlayerInventory inv = mc.player.getInventory();
               int slot = this.findBestWeaponSlot();
               if (slot >= 0) {
                  if (inv.getSelectedSlot() != slot) {
                     this.previousSlot = inv.getSelectedSlot();
                     inv.setSelectedSlot(slot);
                     this.countdown = Math.max(1, this.switchDelay.getValue().intValue());
                  }
               }
            }
         }
      }
   }

   public void noAttack() {
      this.attackHeldLastCheck = false;
   }

   @Override
   public void onEnable() {
      this.previousSlot = -1;
      this.countdown = 0;
   }

   @Override
   public void onDisable() {
      this.previousSlot = -1;
      this.countdown = 0;
   }

   @Override
   public void onTick() {
      if (mc.player != null && mc.options != null) {
         PlayerInventory inv = mc.player.getInventory();
         if (this.countdown > 0) {
            this.countdown--;
            if (this.countdown == 0) {
               if (this.switchBack.getValue() && this.previousSlot >= 0 && this.previousSlot < 9 && inv.getSelectedSlot() != this.previousSlot) {
                  inv.setSelectedSlot(this.previousSlot);
               }

               this.previousSlot = -1;
            }
         }

         if (mc.options.attackKey.isPressed()) {
            this.preAttack();
         } else {
            this.noAttack();
         }
      }
   }

   private int findBestWeaponSlot() {
      PlayerInventory inv = mc.player.getInventory();
      boolean swordOnly = this.onlySword.getValue();
      boolean axeOnly = this.onlyAxe.getValue();
      boolean wantLunge = this.lunge.getValue();
      boolean wantSharp = this.sharpness.getValue();
      int bestSlot = -1;
      int bestScore = Integer.MIN_VALUE;

      for (int i = 0; i < 9; i++) {
         ItemStack s = inv.getStack(i);
         if (!s.isEmpty()) {
            String path = Registries.ITEM.getId(s.getItem()).getPath();
            String displayName = "";

            try {
               displayName = s.getName().getString().toLowerCase();
            } catch (Throwable var18) {
            }

            boolean isSword = path.endsWith("_sword");
            boolean isAxe = s.getItem() instanceof AxeItem;
            boolean isSpear = s.getItem() == Items.TRIDENT || s.getItem() == Items.MACE || path.contains("spear") || displayName.contains("spear");
            boolean hasLunge = this.hasLungeEnchant(s);
            boolean lungeItem = wantLunge && hasLunge;
            if ((!swordOnly || isSword) && (!axeOnly || isAxe) && (swordOnly || axeOnly || isSword || isAxe || isSpear || lungeItem)) {
               int score = 0;
               if (lungeItem) {
                  score += 500;
               }

               if (isSpear) {
                  score += 300;
               } else if (isSword) {
                  score += 200;
               } else if (isAxe) {
                  score += 100;
               }

               if (wantSharp) {
                  score += this.sharpnessLevel(s) * 60;
               }

               if (score > bestScore) {
                  bestScore = score;
                  bestSlot = i;
               }
            }
         }
      }

      return bestSlot;
   }

   private boolean hasLungeEnchant(ItemStack s) {
      try {
         ItemEnchantmentsComponent comp = (ItemEnchantmentsComponent)s.get(DataComponentTypes.ENCHANTMENTS);
         if (comp == null) {
            return false;
         }

         for (RegistryEntry<Enchantment> entry : comp.getEnchantments()) {
            RegistryKey<Enchantment> key = (RegistryKey<Enchantment>)entry.getKey().orElse(null);
            if (key != null) {
               if (key.equals(Enchantments.WIND_BURST) || key.equals(Enchantments.DENSITY) || key.equals(Enchantments.RIPTIDE)) {
                  return true;
               }

               String idPath = key.getValue().toString().toLowerCase();
               if (idPath.contains("lunge")) {
                  return true;
               }
            }
         }
      } catch (Throwable var7) {
      }

      return false;
   }

   private int sharpnessLevel(ItemStack s) {
      try {
         ItemEnchantmentsComponent comp = (ItemEnchantmentsComponent)s.get(DataComponentTypes.ENCHANTMENTS);
         if (comp == null) {
            return 0;
         }

         for (RegistryEntry<Enchantment> entry : comp.getEnchantments()) {
            RegistryKey<Enchantment> key = (RegistryKey<Enchantment>)entry.getKey().orElse(null);
            if (key != null && key.equals(Enchantments.SHARPNESS)) {
               return comp.getLevel(entry);
            }
         }
      } catch (Throwable var6) {
      }

      return 0;
   }
}

