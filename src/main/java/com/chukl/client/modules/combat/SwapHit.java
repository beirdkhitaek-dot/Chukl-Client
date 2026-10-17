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
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult.Type;

/**
 * Swap Hit: the instant you attack a living entity, swaps to your strongest melee weapon (scored by
 * sword/axe, material tier and Sharpness/Smite/Bane of Arthropods level) for that single hit, then swaps back.
 * General-purpose version of the same weapon-swap idea Spear Swap applies to lunge-enchanted weapons only.
 */
public final class SwapHit extends com.chukl.client.water.WModule {
    private final Setting<Boolean> switchBack = new Setting<>("Switch Back", true);
    private final Setting<Integer> switchDelay = new Setting<>("Switch Delay", 1, 1, 20);

    private int previousSlot = -1;
    private int countdown = 0;
    private boolean attackHeldLastCheck = false;

    public SwapHit() {
        super("Swap Hit", Category.COMBAT);
        addSetting(switchBack);
        addSetting(switchDelay);
    }

    @Override
    public void onEnable() {
        previousSlot = -1;
        countdown = 0;
        attackHeldLastCheck = false;
    }

    @Override
    public void onDisable() {
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

        if (justPressed && hasEntityTarget() && countdown <= 0) {
            int slot = findBestWeaponSlot(inv);
            if (slot >= 0 && inv.getSelectedSlot() != slot) {
                previousSlot = inv.getSelectedSlot();
                inv.setSelectedSlot(slot);
                countdown = Math.max(1, switchDelay.getValue());
            }
        }
    }

    private boolean hasEntityTarget() {
        return mc.crosshairTarget != null
            && mc.crosshairTarget.getType() == Type.ENTITY
            && ((EntityHitResult) mc.crosshairTarget).getEntity() != mc.player;
    }

    private int findBestWeaponSlot(PlayerInventory inv) {
        int bestSlot = -1;
        int bestScore = Integer.MIN_VALUE;

        for (int i = 0; i < 9; i++) {
            ItemStack s = inv.getStack(i);
            if (s.isEmpty()) continue;

            String path = net.minecraft.registry.Registries.ITEM.getId(s.getItem()).getPath();
            boolean isSword = path.endsWith("_sword");
            boolean isAxe = s.getItem() instanceof AxeItem;
            if (!isSword && !isAxe) continue;

            int score = isSword ? 200 : 100;
            score += tierScore(s) * 10;
            score += sharpLikeLevel(s) * 60;

            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    private int tierScore(ItemStack s) {
        String id = net.minecraft.registry.Registries.ITEM.getId(s.getItem()).getPath();
        if (id.startsWith("netherite_")) return 5;
        if (id.startsWith("diamond_")) return 4;
        if (id.startsWith("iron_")) return 3;
        if (id.startsWith("golden_") || id.startsWith("gold_")) return 2;
        if (id.startsWith("stone_")) return 1;
        return 0;
    }

    private int sharpLikeLevel(ItemStack s) {
        try {
            ItemEnchantmentsComponent comp = s.get(DataComponentTypes.ENCHANTMENTS);
            if (comp == null) return 0;

            int best = 0;
            for (RegistryEntry<Enchantment> entry : comp.getEnchantments()) {
                RegistryKey<Enchantment> key = entry.getKey().orElse(null);
                if (key == null) continue;
                if (key.equals(Enchantments.SHARPNESS) || key.equals(Enchantments.SMITE) || key.equals(Enchantments.BANE_OF_ARTHROPODS)) {
                    best = Math.max(best, comp.getLevel(entry));
                }
            }
            return best;
        } catch (Throwable t) {
            return 0;
        }
    }
}
