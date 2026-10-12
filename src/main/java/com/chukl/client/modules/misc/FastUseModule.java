package com.chukl.client.modules.misc;

import com.chukl.client.mixin.MinecraftAccessor;
import com.chukl.client.sixseven.Category;
import com.chukl.client.sixseven.SModule;
import com.chukl.client.sixseven.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

public class FastUseModule extends SModule {
   public final ModeSetting items = this.addSetting(new ModeSetting("Items", "What to speed up", "All", "All", "Pearls", "XP Bottles"));

   public FastUseModule() {
      super("FastUse", "Removes item use cooldowns", Category.MISC);
   }

   @Override
   public void onTick() {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player != null) {
         if (this.appliesTo(mc)) {
            ((MinecraftAccessor)mc).chukl$setItemUseCooldown(0);
         }
      }
   }

   private boolean appliesTo(MinecraftClient mc) {
      if (this.items.check("All")) {
         return true;
      } else {
         Item target = this.items.check("Pearls") ? Items.ENDER_PEARL : Items.EXPERIENCE_BOTTLE;
         return mc.player.getMainHandStack().isOf(target) || mc.player.getOffHandStack().isOf(target);
      }
   }
}
