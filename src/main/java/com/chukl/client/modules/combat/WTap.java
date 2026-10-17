package com.chukl.client.modules.combat;

import com.chukl.client.water.Category;
import com.chukl.client.water.Setting;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult.Type;

/**
 * W-Tap: the manual PvP technique of briefly releasing the forward (and optionally sprint) key the instant
 * you hit someone, since sprinting into a target reduces the knockback you deal. Automates the release so it
 * happens exactly on the attack tick rather than relying on manual timing.
 */
public final class WTap extends com.chukl.client.water.WModule {
    private final Setting<Integer> tapTicks = new Setting<>("Tap Ticks", 2, 1, 10);
    private final Setting<Boolean> resetSprint = new Setting<>("Reset Sprint", true);
    private final Setting<Boolean> onlyOnHit = new Setting<>("Only On Hit", true);

    private boolean attackHeldLastCheck = false;
    private int countdown = 0;

    public WTap() {
        super("W Tap", Category.COMBAT);
        addSetting(tapTicks);
        addSetting(resetSprint);
        addSetting(onlyOnHit);
    }

    @Override
    public void onEnable() {
        countdown = 0;
        attackHeldLastCheck = false;
    }

    @Override
    public void onDisable() {
        countdown = 0;
        attackHeldLastCheck = false;
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.options == null) return;

        if (countdown > 0) {
            mc.options.forwardKey.setPressed(false);
            if (resetSprint.getValue()) mc.options.sprintKey.setPressed(false);
            countdown--;
        }

        boolean pressed = mc.options.attackKey.isPressed();
        boolean justPressed = pressed && !attackHeldLastCheck;
        attackHeldLastCheck = pressed;

        if (justPressed && hasTarget() && mc.options.forwardKey.isPressed()) {
            countdown = Math.max(1, tapTicks.getValue());
        }
    }

    private boolean hasTarget() {
        if (!onlyOnHit.getValue()) return true;
        return mc.crosshairTarget != null
            && mc.crosshairTarget.getType() == Type.ENTITY
            && ((EntityHitResult) mc.crosshairTarget).getEntity() != mc.player;
    }
}
