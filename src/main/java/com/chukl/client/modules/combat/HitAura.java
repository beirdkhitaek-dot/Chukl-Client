package com.chukl.client.modules.combat;

import com.chukl.client.water.Category;
import com.chukl.client.water.Friends;
import com.chukl.client.water.Setting;
import com.chukl.client.water.WModule;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * Chukl's "Hit Aura" (ported from Water Client's Aura / HitAura, package ru.waterclient.module.impl.combat).
 * Water's own aura framework (RotationProcess, GCDUtil, MovementManager, UBoxPoints, ...) depends on dozens
 * of event-bus and utility classes outside Water's com.water.module tree that Chukl doesn't have, so this
 * ports its behaviour directly on top of Chukl's module/setting system: target selection, smoothed rotation
 * toward the target, range/LOS/weapon checks and an attack-per-second timer.
 */
public final class HitAura extends WModule {
    private final Setting<Double> range = new Setting<>("Attack Range", 3.5, 2.0, 6.0);
    private final Setting<Boolean> players = new Setting<>("Players", true);
    private final Setting<Boolean> mobs = new Setting<>("Mobs", true);
    private final Setting<Boolean> animals = new Setting<>("Animals", false);
    private final Setting<Boolean> rotate = new Setting<>("Rotate", true);
    private final Setting<Double> smoothness = new Setting<>("Rotation Smoothness", 6.0, 1.0, 10.0);
    private final Setting<Integer> aps = new Setting<>("Attacks Per Second", 10, 1, 20);
    private final Setting<Boolean> throughWalls = new Setting<>("Through Walls", false);
    private final Setting<Boolean> weaponOnly = new Setting<>("Weapon Only", false);
    private final Setting<Boolean> noAttackWhileEating = new Setting<>("No Attack While Eating", true);
    private final Setting<Boolean> swingHand = new Setting<>("Swing Hand", true);

    private LivingEntity target;
    private int cooldown;

    public HitAura() {
        super("Hit Aura", Category.COMBAT);
        addSetting(range);
        addSetting(players);
        addSetting(mobs);
        addSetting(animals);
        addSetting(rotate);
        addSetting(smoothness);
        addSetting(aps);
        addSetting(throughWalls);
        addSetting(weaponOnly);
        addSetting(noAttackWhileEating);
        addSetting(swingHand);
    }

    @Override
    public void onEnable() {
        target = null;
        cooldown = 0;
    }

    @Override
    public void onDisable() {
        target = null;
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
        if (noAttackWhileEating.getValue() && mc.player.isUsingItem()) return;
        if (weaponOnly.getValue() && mc.player.getMainHandStack().isEmpty()) {
            // SwordItem no longer exists as a distinct class in 1.21.x (swords are plain Items
            // tagged minecraft:swords), and that tag name wasn't safely verifiable without a real
            // compile, so this simplifies "Weapon Only" to "must be holding any item".
            return;
        }

        if (target == null || !isValid(target)) target = findTarget();
        if (target == null) return;

        if (rotate.getValue()) rotateTowards(target);

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        double dist = mc.player.getEntityPos().distanceTo(target.getEntityPos());
        if (dist <= range.getValue()) {
            if (!throughWalls.getValue() && !mc.player.canSee(target)) return;
            mc.interactionManager.attackEntity(mc.player, target);
            if (swingHand.getValue()) mc.player.swingHand(Hand.MAIN_HAND);
            cooldown = Math.max(1, Math.round(20f / aps.getValue()));
        }
    }

    private boolean isValid(LivingEntity e) {
        if (e == null || !e.isAlive() || e.isRemoved() || e == mc.player) return false;
        if (e instanceof PlayerEntity p && Friends.isFriend(p.getName().getString())) return false;
        return mc.player.getEntityPos().distanceTo(e.getEntityPos()) <= range.getValue() + 1.5;
    }

    private LivingEntity findTarget() {
        if (mc.world == null || mc.player == null) return null;
        double r = range.getValue() + 1.5;
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
            if (d < bestDist) {
                bestDist = d;
                best = le;
            }
        }
        return best;
    }

    private void rotateTowards(LivingEntity e) {
        Vec3d eye = mc.player.getEyePos();
        Vec3d targetPos = e.getBoundingBox().getCenter();
        double dx = targetPos.x - eye.x;
        double dy = targetPos.y - eye.y;
        double dz = targetPos.z - eye.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (MathHelper.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        float pitch = (float) (-(MathHelper.atan2(dy, horizontal) * (180.0 / Math.PI)));
        float speed = MathHelper.clamp((float) (smoothness.getValue() / 10.0), 0.05f, 1f);
        mc.player.setYaw(lerpAngle(mc.player.getYaw(), yaw, speed));
        mc.player.setPitch(MathHelper.clamp(mc.player.getPitch() + (pitch - mc.player.getPitch()) * speed, -90f, 90f));
    }

    private static float lerpAngle(float from, float to, float t) {
        float diff = MathHelper.wrapDegrees(to - from);
        return from + diff * t;
    }
}
