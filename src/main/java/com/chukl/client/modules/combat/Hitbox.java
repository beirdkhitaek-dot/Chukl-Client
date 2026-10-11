package com.chukl.client.modules.combat;

import com.chukl.client.water.Category;
import com.chukl.client.water.Setting;

public final class Hitbox extends com.chukl.client.water.WModule {
   public static Hitbox INSTANCE;
   public final Setting<Float> size = new Setting<>("Expand", 1.0F, 0.5F, 2.0F);

   public Hitbox() {
      super("Hitbox", Category.COMBAT);
      this.addSetting(this.size);
      INSTANCE = this;
   }
}

