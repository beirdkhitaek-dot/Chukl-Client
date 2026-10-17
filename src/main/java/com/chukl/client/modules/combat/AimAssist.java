package com.chukl.client.modules.combat;

import com.chukl.client.water.Category;
import com.chukl.client.water.Friends;
import com.chukl.client.water.Setting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * Aim Assist: a soft-aim that smoothly nudges your look direction toward the nearest valid target within a
 * field-of-view cone and range, without attacking for you (that's Hit Aura). Shares Hit Aura's target-pick and
 * smoothed-rotation approach but is purely a rotation helper.
 */
public final class AimAssist extends com.chukl.client.water.WModule {
    private final Setting<Double> range = new Setting<>("Range", 4.5, 2.0, 10.0);
    private final Setting<Double> fov = new Setting<>("FOV", 60.0, 5.0, 180.0);
    private final Setting<Boolean> players = new Setting<>("Players", true);
    private final Setting<Boolean> mobs = new Setting<>("Mobs", true);
    private final Setting<Boolean> animals = new Setting<>("Animals", false);
    private final Setting<Double> smoothness = new Setting<>("Smoothness", 5.0, 1.0, 10.0);
    private final Setting<Boolean> onlyWhileAttacking = new Setting<>("Only While Attacking", true);

    private LivingEntity target;

    public AimAssist() {
        super("Aim Assist", Category.COMBAT);
        addSetting(range);
        addSetting(fov);
        addSetting(players);
        addSetting(mobs);
        addSetting(animals);
        addSetting(smoothness);
        addSetting(onlyWhileAttacking);
    }

    @Override
    public void onEnable() {
        target = null;
    }

    @Override
    public void onDisable() {
        target = null;
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        if (onlyWhileAttacking.getValue() && !mc.options.attackKey.isPressed()) {
            target = null;
            return;
        }

        if (target == null || !isValid(target)) target = findTarget();
        if (target == null) return;

        rotateTowards(target);
    }

    private boolean isValid(LivingEntity e) {
        if (e == null || !e.isAlive() || e.isRemoved() || e == mc.player) return false;
        if (e instanceof PlayerEntity p && Friends.isFriend(p.getName().getString())) return false;
        return mc.player.getEntityPos().distanceTo(e.getEntityPos()) <= range.getValue() + 1.5 && withinFov(e);
    }

    private LivingEntity findTarget() {
        double r = range.getValue();
        Box search = mc.player.getBoundingBox().expand(r);
        List<Entity> nearby = mc.world.getOtherEntities(mc.player, search);
        LivingEntity best = null;
        double bestAngle = Double.MAX_VALUE;

        for (Entity e : nearby) {
            if (!(e instanceof LivingEntity le) || e instanceof ArmorStandEntity || !le.isAlive()) continue;
            if (le instanceof PlayerEntity pe) {
                if (!players.getValue() || Friends.isFriend(pe.getName().getString())) continue;
            } else if (le instanceof AnimalEntity) {
                if (!animals.getValue()) continue;
            } else {
                if (!mobs.getValue()) continue;
            }
            if (mc.player.getEntityPos().distanceTo(le.getEntityPos()) > r) continue;

            double angle = angleTo(le);
            if (angle <= fov.getValue() && angle < bestAngle) {
                bestAngle = angle;
                best = le;
            }
        }
        return best;
    }

    private boolean withinFov(LivingEntity e) {
        return angleTo(e) <= fov.getValue() + 5.0;
    }

    private double angleTo(LivingEntity e) {
        Vec3d eye = mc.player.getEyePos();
        Vec3d look = mc.player.getRotationVector();
        Vec3d toTarget = e.getBoundingBox().getCenter().subtract(eye).normalize();
        double dot = MathHelper.clamp(look.dotProduct(toTarget), -1.0, 1.0);
        return Math.toDegrees(Math.acos(dot));
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
