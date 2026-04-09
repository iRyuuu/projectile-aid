package com.projectileaid.trajectory;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Maps a held ItemStack to one or more TrajectorySpecs to simulate.
 * Returns an empty list if the held item does not launch a projectile.
 *
 * Trail origins use a yaw-only right vector so the start point is stable
 * when panning the camera up/down — no jitter.
 *
 * Multishot crossbows return three specs (spread ±10° in the horizontal plane).
 */
public class ProjectileHelper {

    public static List<TrajectorySpec> getTrajectories(ItemStack stack, LocalPlayer player) {
        if (stack.isEmpty()) return Collections.emptyList();
        Item item = stack.getItem();
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
            return single(bowOrigin(player), look.scale(3.0f * power), 0.05f, 0.99f,
                    ProjectileInfo.ProjectileType.BOW);
        }

        // ── Crossbow ──────────────────────────────────────────────────────────
        if (item instanceof CrossbowItem) {
            ChargedProjectiles charged = stack.get(DataComponents.CHARGED_PROJECTILES);
            if (charged == null || charged.isEmpty()) return Collections.emptyList();

            if (charged.getItems().stream().anyMatch(s -> s.is(Items.FIREWORK_ROCKET))) {
                return single(crossbowOrigin(player), look.scale(1.6f), 0.0f, 0.95f,
                        ProjectileInfo.ProjectileType.CROSSBOW_FIREWORK);
            }

            boolean multishot = hasEnchantment(stack, "multishot");
            if (multishot) {
                Vec3 origin = crossbowOrigin(player);
                List<TrajectorySpec> specs = new ArrayList<>(3);
                for (int deg : new int[]{-10, 0, 10}) {
                    specs.add(new TrajectorySpec(
                            origin, rotateAroundY(look, deg).scale(3.15f),
                            0.05f, 0.99f, ProjectileInfo.ProjectileType.CROSSBOW_ARROW));
                }
                return specs;
            }
            return single(crossbowOrigin(player), look.scale(3.15f), 0.05f, 0.99f,
                    ProjectileInfo.ProjectileType.CROSSBOW_ARROW);
        }

        // ── Trident ───────────────────────────────────────────────────────────
        if (stack.is(Items.TRIDENT)) {
            return single(tridentOrigin(player), look.scale(2.5f), 0.05f, 0.99f,
                    ProjectileInfo.ProjectileType.TRIDENT);
        }

        // ── Wind charge ───────────────────────────────────────────────────────
        if (stack.is(Items.WIND_CHARGE)) {
            return single(throwableOrigin(player), look.scale(1.5f), 0.0f, 0.99f,
                    ProjectileInfo.ProjectileType.WIND_CHARGE);
        }

        // ── Common throwables ─────────────────────────────────────────────────
        if (stack.is(Items.SNOWBALL) || stack.is(Items.EGG) || stack.is(Items.ENDER_PEARL)) {
            return single(throwableOrigin(player), look.scale(1.5f), 0.03f, 0.99f,
                    ProjectileInfo.ProjectileType.SNOWBALL);
        }

        // ── Throwable potions ─────────────────────────────────────────────────
        if (stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION)) {
            return single(throwableOrigin(player), look.scale(0.5f), 0.03f, 0.99f,
                    ProjectileInfo.ProjectileType.POTION);
        }

        // ── Experience bottle ─────────────────────────────────────────────────
        if (stack.is(Items.EXPERIENCE_BOTTLE)) {
            return single(throwableOrigin(player), look.scale(0.7f), 0.03f, 0.99f,
                    ProjectileInfo.ProjectileType.XP_BOTTLE);
        }

        return Collections.emptyList();
    }

    /** Checks both hands; main hand takes priority. */
    public static List<TrajectorySpec> getActiveTrajectories(LocalPlayer player) {
        List<TrajectorySpec> specs = getTrajectories(player.getMainHandItem(), player);
        if (!specs.isEmpty()) return specs;
        return getTrajectories(player.getOffhandItem(), player);
    }

    // ── Hand-position origins ─────────────────────────────────────────────────
    //
    // All origins use a yaw-only right vector (no pitch component) so the start
    // point stays stable when the player looks up or down.
    // The forward component uses the full look direction so the origin correctly
    // follows aim direction.
    //
    // right = (cos(yaw), 0, sin(yaw)) in world space — always horizontal.

    /**
     * Throwable items (snowball, egg, pearl, wind charge, potions, XP bottle).
     * The item is held low-right, arm bent: right*0.50, forward*0.10, down*0.30.
     */
    private static Vec3 throwableOrigin(LocalPlayer player) {
        Vec3 eye  = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double[] r = yawRight(player);
        return new Vec3(
                eye.x + r[0] * 0.50 + look.x * 0.10,
                eye.y - 0.30        + look.y * 0.10,
                eye.z + r[1] * 0.50 + look.z * 0.10
        );
    }

    /**
     * Bow — arrow nocked on the bowstring, arm extended slightly forward.
     * right*0.30, forward*0.20, down*0.15.
     */
    private static Vec3 bowOrigin(LocalPlayer player) {
        Vec3 eye  = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double[] r = yawRight(player);
        return new Vec3(
                eye.x + r[0] * 0.30 + look.x * 0.20,
                eye.y - 0.15        + look.y * 0.20,
                eye.z + r[1] * 0.30 + look.z * 0.20
        );
    }

    /**
     * Crossbow — loaded arrow/firework tip, held further forward than bow.
     * right*0.25, forward*0.30, down*0.15.
     */
    private static Vec3 crossbowOrigin(LocalPlayer player) {
        Vec3 eye  = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double[] r = yawRight(player);
        return new Vec3(
                eye.x + r[0] * 0.25 + look.x * 0.30,
                eye.y - 0.15        + look.y * 0.30,
                eye.z + r[1] * 0.25 + look.z * 0.30
        );
    }

    /**
     * Trident — tip of trident held out in front.
     * right*0.40, forward*0.50, down*0.05.
     */
    private static Vec3 tridentOrigin(LocalPlayer player) {
        Vec3 eye  = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double[] r = yawRight(player);
        return new Vec3(
                eye.x + r[0] * 0.40 + look.x * 0.50,
                eye.y - 0.05        + look.y * 0.50,
                eye.z + r[1] * 0.40 + look.z * 0.50
        );
    }

    /**
     * Returns the player's rightward unit vector in the horizontal plane,
     * derived from yaw only (not pitch-dependent, so no jitter on camera pan).
     * result[0] = X component, result[1] = Z component.
     *
     * MC yaw: 0° = south (+Z), 90° = west (-X), 180° = north (-Z), -90° = east (+X).
     * Horizontal right = rotate look 90° CW around Y:
     *   rightX = cos(yaw), rightZ = sin(yaw).
     */
    private static double[] yawRight(LocalPlayer player) {
        double yawRad = Math.toRadians(player.getYRot());
        return new double[]{ Math.cos(yawRad), Math.sin(yawRad) };
    }

    // ── Misc helpers ──────────────────────────────────────────────────────────

    private static List<TrajectorySpec> single(
            Vec3 pos, Vec3 vel, float gravity, float drag,
            ProjectileInfo.ProjectileType type
    ) {
        return Collections.singletonList(new TrajectorySpec(pos, vel, gravity, drag, type));
    }

    private static Vec3 rotateAroundY(Vec3 v, double degrees) {
        double rad = Math.toRadians(degrees);
        double cos = Math.cos(rad), sin = Math.sin(rad);
        return new Vec3(v.x * cos + v.z * sin, v.y, -v.x * sin + v.z * cos);
    }

    private static boolean hasEnchantment(ItemStack stack, String pathName) {
        ItemEnchantments enchants = stack.get(DataComponents.ENCHANTMENTS);
        if (enchants == null) return false;
        Identifier target = Identifier.withDefaultNamespace(pathName);
        for (var entry : enchants.entrySet()) {
            if (entry.getKey().is(target)) return true;
        }
        return false;
    }
}
