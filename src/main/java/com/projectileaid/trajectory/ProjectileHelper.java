package com.projectileaid.trajectory;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.core.component.DataComponents;

/**
 * Maps a held ItemStack to its corresponding ProjectileInfo (physics parameters).
 * Returns null if the item does not fire a projectile.
 */
public class ProjectileHelper {

    /**
     * Returns physics info for the projectile that would be fired from {@code stack},
     * or null if this item is not a projectile launcher.
     */
    public static ProjectileInfo getProjectileInfo(ItemStack stack, LocalPlayer player) {
        if (stack.isEmpty()) return null;
        Item item = stack.getItem();

        // ── Bow ──────────────────────────────────────────────────────────────────
        if (item instanceof BowItem) {
            float power;
            if (player.isUsingItem()) {
                int chargeTime = 72000 - player.getUseItemRemainingTicks();
                power = BowItem.getPowerForTime(chargeTime);
                if (power < 0.05f) power = 0.05f; // small minimum so trail is visible
            } else {
                power = 1.0f; // show full-power preview when not drawing
            }
            return new ProjectileInfo(3.0f * power, 0.05f, 0.99f);
        }

        // ── Crossbow ─────────────────────────────────────────────────────────────
        if (item instanceof CrossbowItem) {
            ChargedProjectiles charged = stack.get(DataComponents.CHARGED_PROJECTILES);
            if (charged == null || charged.isEmpty()) return null;

            boolean hasFirework = charged.getItems().stream()
                    .anyMatch(s -> s.is(Items.FIREWORK_ROCKET));
            if (hasFirework) {
                // Fireworks have thrust and minimal gravity
                return new ProjectileInfo(1.6f, 0.0f, 0.95f);
            }
            return new ProjectileInfo(3.15f, 0.05f, 0.99f);
        }

        // ── Common throwables ─────────────────────────────────────────────────────
        if (stack.is(Items.SNOWBALL) || stack.is(Items.EGG) || stack.is(Items.ENDER_PEARL)) {
            return new ProjectileInfo(1.5f, 0.03f, 0.99f);
        }

        // ── Throwable potions ─────────────────────────────────────────────────────
        if (stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION)) {
            return new ProjectileInfo(0.5f, 0.03f, 0.99f);
        }

        // ── Experience bottle ─────────────────────────────────────────────────────
        if (stack.is(Items.EXPERIENCE_BOTTLE)) {
            return new ProjectileInfo(0.7f, 0.03f, 0.99f);
        }

        // ── Trident ───────────────────────────────────────────────────────────────
        if (stack.is(Items.TRIDENT)) {
            // Trident is thrown at ~2.5 speed (similar to a strong arm throw)
            return new ProjectileInfo(2.5f, 0.05f, 0.99f);
        }

        return null;
    }

    /**
     * Checks both hands and returns the first ProjectileInfo found.
     * Main hand takes priority.
     */
    public static ProjectileInfo getActiveProjectileInfo(LocalPlayer player) {
        ProjectileInfo info = getProjectileInfo(player.getMainHandItem(), player);
        if (info != null) return info;
        return getProjectileInfo(player.getOffhandItem(), player);
    }
}
