package com.projectileaid.trajectory;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Maps a held ItemStack to one or more TrajectorySpecs to simulate.
 * Returns an empty list if the held item does not launch a projectile.
 *
 * Multishot crossbows return three specs (spread ±10° in the horizontal plane).
 */
public class ProjectileHelper {

    public static List<TrajectorySpec> getTrajectories(ItemStack stack, LocalPlayer player) {
        if (stack.isEmpty()) return Collections.emptyList();
        Item item = stack.getItem();
        Vec3 startPos = player.getEyePosition();
        Vec3 look = player.getLookAngle();

        // ── Bow ───────────────────────────────────────────────────────────────
        if (item instanceof BowItem) {
            float power;
            if (player.isUsingItem()) {
                int chargeTime = 72000 - player.getUseItemRemainingTicks();
                power = BowItem.getPowerForTime(chargeTime);
                if (power < 0.05f) power = 0.05f;
            } else {
                power = 1.0f; // preview at full power when idle
            }
            return single(startPos, look.scale(3.0f * power), 0.05f, 0.99f,
                    ProjectileInfo.ProjectileCategory.COMBAT);
        }

        // ── Crossbow ──────────────────────────────────────────────────────────
        if (item instanceof CrossbowItem) {
            ChargedProjectiles charged = stack.get(DataComponents.CHARGED_PROJECTILES);
            if (charged == null || charged.isEmpty()) return Collections.emptyList();

            // Firework rocket
            if (charged.getItems().stream().anyMatch(s -> s.is(Items.FIREWORK_ROCKET))) {
                return single(startPos, look.scale(1.6f), 0.0f, 0.95f,
                        ProjectileInfo.ProjectileCategory.COMBAT);
            }

            // Check for Multishot enchantment
            boolean multishot = hasEnchantment(stack, "multishot");
            if (multishot) {
                List<TrajectorySpec> specs = new ArrayList<>(3);
                for (int deg : new int[]{-10, 0, 10}) {
                    specs.add(new TrajectorySpec(
                            startPos, rotateAroundY(look, deg).scale(3.15f),
                            0.05f, 0.99f, ProjectileInfo.ProjectileCategory.COMBAT));
                }
                return specs;
            }
            return single(startPos, look.scale(3.15f), 0.05f, 0.99f,
                    ProjectileInfo.ProjectileCategory.COMBAT);
        }

        // ── Trident ───────────────────────────────────────────────────────────
        if (stack.is(Items.TRIDENT)) {
            return single(startPos, look.scale(2.5f), 0.05f, 0.99f,
                    ProjectileInfo.ProjectileCategory.COMBAT);
        }

        // ── Common throwables ─────────────────────────────────────────────────
        if (stack.is(Items.SNOWBALL) || stack.is(Items.EGG) || stack.is(Items.ENDER_PEARL)) {
            return single(startPos, look.scale(1.5f), 0.03f, 0.99f,
                    ProjectileInfo.ProjectileCategory.UTILITY);
        }

        // ── Throwable potions ─────────────────────────────────────────────────
        if (stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION)) {
            return single(startPos, look.scale(0.5f), 0.03f, 0.99f,
                    ProjectileInfo.ProjectileCategory.UTILITY);
        }

        // ── Experience bottle ─────────────────────────────────────────────────
        if (stack.is(Items.EXPERIENCE_BOTTLE)) {
            return single(startPos, look.scale(0.7f), 0.03f, 0.99f,
                    ProjectileInfo.ProjectileCategory.UTILITY);
        }

        return Collections.emptyList();
    }

    /** Checks both hands; main hand takes priority. */
    public static List<TrajectorySpec> getActiveTrajectories(LocalPlayer player) {
        List<TrajectorySpec> specs = getTrajectories(player.getMainHandItem(), player);
        if (!specs.isEmpty()) return specs;
        return getTrajectories(player.getOffhandItem(), player);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static List<TrajectorySpec> single(
            Vec3 pos, Vec3 vel, float gravity, float drag,
            ProjectileInfo.ProjectileCategory cat
    ) {
        return Collections.singletonList(new TrajectorySpec(pos, vel, gravity, drag, cat));
    }

    private static Vec3 rotateAroundY(Vec3 v, double degrees) {
        double rad = Math.toRadians(degrees);
        double cos = Math.cos(rad), sin = Math.sin(rad);
        return new Vec3(v.x * cos + v.z * sin, v.y, -v.x * sin + v.z * cos);
    }

    private static boolean hasEnchantment(ItemStack stack, String pathName) {
        ItemEnchantments enchants = stack.get(DataComponents.ENCHANTMENTS);
        if (enchants == null) return false;
        for (var entry : enchants.entrySet()) {
            if (entry.getKey().unwrapKey()
                    .filter(k -> k.getValue().getPath().equals(pathName))
                    .isPresent()) {
                return true;
            }
        }
        return false;
    }
}
