package com.chukl.client.modules.combat;

import com.chukl.client.water.Category;
import com.chukl.client.water.Setting;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult.Type;

/**
 * Breach Swap: the Mace's smash attack (triggered by falling onto a target) ignores a fraction of armor per
 * enchant level of Breach. This swaps to your best Breach-enchanted Mace only for the instant of a genuine
 * falling smash attack (enough fall distance, about to hit a living entity), then swaps back -- so you don't
 * have to keep the Mace selected (and lose your normal weapon) between smashes.
 */
public final class BreachSwap extends com.chukl.client.water.WModule {
    private final Setting<Double> minFallDistance = new Setting<>("Min Fall Distance", 1.5, 0.5, 10.0);
    private final Setting<Boolean> requireBreach = new Setting<>("Require Breach", true);
    private final Setting<Boolean> switchBack = new Setting<>("Switch Back", true);
    private final Setting<Integer> switchDelay = new Setting<>("Switch Delay", 2, 1, 20);

    private int previousSlot = -1;
    private int countdown = 0;
    private boolean attackHeldLastCheck = false;

    public BreachSwap() {
        super("Breach Swap", Category.COMBAT);
        addSetting(minFallDistance);
        addSetting(requireBreach);
        addSetting(switchBack);
        addSetting(switchDelay);
    }

    @Override
    public void onEnable() {
        reset();
    }

    @Override
    public void onDisable() {
        reset();
    }

    private void reset() {
        previousSlot = -1;
        countdown = 0;
        attackHeldLastCheck = false;
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.options == null) return;

        PlayerInventory inv = mc.player.getInventory();
        if (countdown > 0) {
            countdown--;
            if (countdown == 0 && switchBack.getValue() && previousSlot >= 0 && previousSlot < 9 && inv.getSelectedSlot() != previousSlot) {
                inv.setSelectedSlot(previousSlot);
            }
            if (countdown == 0) previousSlot = -1;
        }

        boolean pressed = mc.options.attackKey.isPressed();
        boolean justPressed = pressed && !attackHeldLastCheck;
        attackHeldLastCheck = pressed;

        if (!justPressed || countdown > 0) return;
        if (mc.player.fallDistance < minFallDistance.getValue()) return;
        if (!hasEntityTarget()) return;

        int slot = findMaceSlot(inv);
        if (slot >= 0 && inv.getSelectedSlot() != slot) {
            previousSlot = inv.getSelectedSlot();
            inv.setSelectedSlot(slot);
            countdown = Math.max(1, switchDelay.getValue());
        }
    }

    private boolean hasEntityTarget() {
        return mc.crosshairTarget != null
            && mc.crosshairTarget.getType() == Type.ENTITY
            && ((EntityHitResult) mc.crosshairTarget).getEntity() != mc.player;
    }

    private int findMaceSlot(PlayerInventory inv) {
        int bestSlot = -1;
        int bestScore = -1;

        for (int i = 0; i < 9; i++) {
            ItemStack s = inv.getStack(i);
            if (!s.isOf(Items.MACE)) continue;

            int breach = breachLevel(s);
            if (requireBreach.getValue() && breach <= 0) continue;

            int score = breach;
            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    private int breachLevel(ItemStack s) {
        try {
            ItemEnchantmentsComponent comp = s.get(DataComponentTypes.ENCHANTMENTS);
            if (comp == null) return 0;

            for (RegistryEntry<Enchantment> entry : comp.getEnchantments()) {
                RegistryKey<Enchantment> key = entry.getKey().orElse(null);
                if (key != null && key.equals(Enchantments.BREACH)) {
                    return comp.getLevel(entry);
                }
            }
        } catch (Throwable t) {
            // ignored, treated as no Breach
        }
        return 0;
    }
}
