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
import net.minecraft.item.AxeItem;
import net.minecraft.item.SwordItem;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * Chukl's "Legit Aura" (ported from Water Client's ru.waterclient.module.impl.combat.LegitAura). Unlike
 * Hit Aura, it never snaps the player's rotation: it only attacks a target that already sits within a
 * configurable aim cone of the player's real crosshair, which keeps the module looking like normal play.
 * As with Hit Aura, Water's own rotation framework isn't ported line-by-line (see Hit Aura's javadoc);
 * this rebuilds the same behaviour on Chukl's module system.
 */
public final class LegitAura extends WModule {
    private final Setting<Double> attackRange = new Setting<>("Attack Range", 3.2, 2.0, 6.0);
    private final Setting<Double> fov = new Setting<>("Aim FOV", 25.0, 2.0, 90.0);
    private final Setting<Boolean> players = new Setting<>("Players", true);
    private final Setting<Boolean> mobs = new Setting<>("Mobs", true);
    private final Setting<Boolean> animals = new Setting<>("Animals", false);
    private final Setting<Boolean> onlyWhenCharged = new Setting<>("Only Full Charge", true);
    private final Setting<Boolean> throughWalls = new Setting<>("Through Walls", false);
    private final Setting<Boolean> weaponOnly = new Setting<>("Weapon Only", false);
    private final Setting<Boolean> noAttackInGui = new Setting<>("No Attack In GUI", true);
    private final Setting<Boolean> resetSprint = new Setting<>("Reset Sprint", true);
    private final Setting<Boolean> swingHand = new Setting<>("Swing Hand", true);

    @Override
    public void onDisable() { }

    public LegitAura() {
        super("Legit Aura", Category.COMBAT);
        addSetting(attackRange);
        addSetting(fov);
        addSetting(players);
        addSetting(mobs);
        addSetting(animals);
        addSetting(onlyWhenCharged);
        addSetting(throughWalls);
        addSetting(weaponOnly);
        addSetting(noAttackInGui);
        addSetting(resetSprint);
        addSetting(swingHand);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
        if (noAttackInGui.getValue() && mc.currentScreen != null) return;
        if (weaponOnly.getValue()) {
            var held = mc.player.getMainHandStack().getItem();
            if (!(held instanceof SwordItem) && !(held instanceof AxeItem)) return;
        }
        if (onlyWhenCharged.getValue() && mc.player.getAttackCooldownProgress(0.0F) < 1.0F) return;

        LivingEntity target = findAimedTarget();
        if (target == null) return;

        double dist = mc.player.getPos().distanceTo(target.getPos());
        if (dist > attackRange.getValue()) return;
        if (!throughWalls.getValue() && !mc.player.canSee(target)) return;

        if (resetSprint.getValue() && mc.player.isSprinting()) {
            mc.player.setSprinting(false);
        }

        mc.interactionManager.attackEntity(mc.player, target);
        if (swingHand.getValue()) mc.player.swingHand(Hand.MAIN_HAND);
    }

    /** Finds the closest valid entity sitting inside the player's real aim cone — no rotation is touched. */
    private LivingEntity findAimedTarget() {
        double r = attackRange.getValue() + 2.0;
        Box search = mc.player.getBoundingBox().expand(r);
        List<Entity> nearby = mc.world.getOtherEntities(mc.player, search);
        Vec3d eye = mc.player.getEyePos();
        Vec3d look = mc.player.getRotationVector();

        LivingEntity best = null;
        double bestAngle = fov.getValue();
        for (Entity e : nearby) {
            if (!(e instanceof LivingEntity le) || e instanceof ArmorStandEntity || !le.isAlive()) continue;
            if (le instanceof PlayerEntity pe) {
                if (!players.getValue() || Friends.isFriend(pe.getName().getString())) continue;
            } else if (le instanceof AnimalEntity) {
                if (!animals.getValue()) continue;
            } else {
                if (!mobs.getValue()) continue;
            }

            Vec3d toTarget = le.getBoundingBox().getCenter().subtract(eye).normalize();
            double angle = Math.toDegrees(Math.acos(MathHelper.clamp(look.dotProduct(toTarget), -1.0, 1.0)));
            if (angle < bestAngle) {
                bestAngle = angle;
                best = le;
            }
        }
        return best;
    }
}
