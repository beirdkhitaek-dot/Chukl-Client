package com.chukl.client.modules.combat;

import com.chukl.client.water.Category;
import com.chukl.client.water.Friends;
import com.chukl.client.water.Setting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;

import java.util.List;

/**
 * Auto Mace: automates the Mace smash-attack loop end to end. When a valid target is within range, it equips
 * the Mace (if not already held), hops to generate a small fall if you're grounded (so the next landing
 * counts as a smash), and swings the instant you have both a falling player and a target below your swing
 * range -- instead of needing Breach Swap plus manual jump timing on every attack.
 */
public final class AutoMace extends com.chukl.client.water.WModule {
    private final Setting<Double> range = new Setting<>("Range", 4.0, 2.0, 8.0);
    private final Setting<Boolean> players = new Setting<>("Players", true);
    private final Setting<Boolean> mobs = new Setting<>("Mobs", true);
    private final Setting<Boolean> animals = new Setting<>("Animals", false);
    private final Setting<Boolean> jumpAssist = new Setting<>("Jump Assist", true);
    private final Setting<Double> minFallDistance = new Setting<>("Min Fall Distance", 1.5, 0.5, 10.0);
    private final Setting<Boolean> switchToMace = new Setting<>("Switch To Mace", true);
    private final Setting<Boolean> switchBack = new Setting<>("Switch Back", true);
    private final Setting<Integer> cooldownTicks = new Setting<>("Cooldown", 10, 1, 40);
    private final Setting<Boolean> swingHand = new Setting<>("Swing Hand", true);

    private int previousSlot = -1;
    private int cooldown = 0;
    private int jumpCooldown = 0;

    public AutoMace() {
        super("Auto Mace", Category.COMBAT);
        addSetting(range);
        addSetting(players);
        addSetting(mobs);
        addSetting(animals);
        addSetting(jumpAssist);
        addSetting(minFallDistance);
        addSetting(switchToMace);
        addSetting(switchBack);
        addSetting(cooldownTicks);
        addSetting(swingHand);
    }

    @Override
    public void onEnable() {
        reset();
    }

    @Override
    public void onDisable() {
        if (switchBack.getValue() && previousSlot >= 0 && previousSlot < 9 && mc.player != null) {
            mc.player.getInventory().setSelectedSlot(previousSlot);
        }
        reset();
    }

    private void reset() {
        previousSlot = -1;
        cooldown = 0;
        jumpCooldown = 0;
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        if (cooldown > 0) {
            cooldown--;
            return;
        }
        if (jumpCooldown > 0) jumpCooldown--;

        LivingEntity target = findTarget();
        if (target == null) return;

        boolean falling = mc.player.fallDistance >= minFallDistance.getValue();

        if (!falling) {
            if (jumpAssist.getValue() && mc.player.isOnGround() && jumpCooldown <= 0) {
                mc.options.jumpKey.setPressed(true);
                jumpCooldown = 10;
            } else if (jumpAssist.getValue()) {
                mc.options.jumpKey.setPressed(false);
            }
            return;
        }
        mc.options.jumpKey.setPressed(false);

        PlayerInventory inv = mc.player.getInventory();
        int maceSlot = findMaceSlot(inv);
        if (maceSlot < 0) return;

        int currentSlot = inv.getSelectedSlot();
        boolean swapped = false;
        if (switchToMace.getValue() && currentSlot != maceSlot) {
            previousSlot = currentSlot;
            inv.setSelectedSlot(maceSlot);
            swapped = true;
        } else if (currentSlot != maceSlot) {
            return; // not holding a mace and not allowed to swap
        }

        mc.interactionManager.attackEntity(mc.player, target);
        if (swingHand.getValue()) mc.player.swingHand(Hand.MAIN_HAND);
        cooldown = Math.max(1, cooldownTicks.getValue());

        if (swapped && switchBack.getValue() && previousSlot >= 0 && previousSlot < 9) {
            inv.setSelectedSlot(previousSlot);
            previousSlot = -1;
        }
    }

    private int findMaceSlot(PlayerInventory inv) {
        for (int i = 0; i < 9; i++) {
            ItemStack s = inv.getStack(i);
            if (s.isOf(Items.MACE)) return i;
        }
        return -1;
    }

    private LivingEntity findTarget() {
        double r = range.getValue();
        Box search = mc.player.getBoundingBox().expand(r);
        List<Entity> nearby = mc.world.getOtherEntities(mc.player, search);
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;

        for (Entity e : nearby) {
            if (!(e instanceof LivingEntity le) || e instanceof ArmorStandEntity || !le.isAlive()) continue;
            if (le instanceof PlayerEntity pe) {
                if (!players.getValue() || Friends.isFriend(pe.getName().getString())) continue;
            } else if (le instanceof AnimalEntity) {
                if (!animals.getValue()) continue;
            } else {
                if (!mobs.getValue()) continue;
            }
            double d = mc.player.getEntityPos().distanceTo(le.getEntityPos());
            if (d <= r && d < bestDist) {
                bestDist = d;
                best = le;
            }
        }
        return best;
    }
}
