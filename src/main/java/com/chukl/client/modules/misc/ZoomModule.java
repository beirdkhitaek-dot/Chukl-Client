package com.chukl.client.modules.misc;

import com.chukl.client.sixseven.Category;
import com.chukl.client.sixseven.SModule;
import com.chukl.client.sixseven.BooleanSetting;
import com.chukl.client.sixseven.SliderSetting;

public class ZoomModule extends SModule {
   private static final double EASE_SPEED = 14.0;
   public final SliderSetting factor = this.addSetting(new SliderSetting("Factor", "Zoom factor", 4.0, 2.0, 10.0, 0.5, "x"));
   public final BooleanSetting smooth = this.addSetting(new BooleanSetting("Smooth", "Smooth zoom in/out", true));
   private double current = 1.0;
   private long lastNanos = 0L;

   public ZoomModule() {
      super("Zoom", "Optical zoom on a key", Category.MISC);
   }

   public double currentFactor() {
      long now = System.nanoTime();
      double dt = this.lastNanos == 0L ? 0.0 : (now - this.lastNanos) / 1.E9;
      this.lastNanos = now;
      double target = this.isEnabled() ? this.factor.getFloat() : 1.0;
      if (!this.smooth.get()) {
         this.current = target;
         return this.current;
      } else {
         double t = 1.0 - Math.exp(-14.0 * Math.max(0.0, dt));
         this.current = this.current + (target - this.current) * t;
         if (Math.abs(this.current - target) < 0.001) {
            this.current = target;
         }

         return this.current;
      }
   }
}
