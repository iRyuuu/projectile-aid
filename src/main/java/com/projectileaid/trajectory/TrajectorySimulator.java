package com.projectileaid.trajectory;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Simulates projectile physics step-by-step and detects the first collision.
 *
 * Physics model (matches Minecraft internals):
 *   velocity *= drag
 *   velocity.y -= gravity
 *   position += velocity
 */
public class TrajectorySimulator {

    /** Maximum ticks to simulate (~15 seconds). */
    private static final int MAX_TICKS = 300;

    /**
     * Result of a trajectory simulation.
     *
     * @param points       world-space positions along the path (including start)
     * @param hitSomething true if the trajectory ends at a block or entity hit;
     *                     false if it falls into the void or reaches MAX_TICKS
     */
    public record SimResult(List<Vec3> points, boolean hitSomething) {}

    /**
     * Run the simulation.
     *
     * @param level         client world for raycasting
     * @param excludePlayer player entity to ignore in entity checks
     * @param startPos      eye position of the player
     * @param startVel      initial velocity vector
     * @param gravity       downward acceleration per tick
     * @param drag          velocity multiplier per tick
     */
    public static SimResult simulate(
            Level level,
            Player excludePlayer,
            Vec3 startPos,
            Vec3 startVel,
            float gravity,
            float drag
    ) {
        List<Vec3> points = new ArrayList<>(MAX_TICKS / 2 + 2);
        points.add(startPos);

        Vec3 pos = startPos;
        Vec3 vel = startVel;

        for (int tick = 0; tick < MAX_TICKS; tick++) {
            // Apply drag then gravity (order matches Minecraft's AbstractArrow / ThrowableProjectile)
            vel = new Vec3(vel.x * drag, vel.y * drag - gravity, vel.z * drag);
            Vec3 nextPos = pos.add(vel);

            // ── Block collision (raycast) ────────────────────────────────────────
            ClipContext clipCtx = new ClipContext(
                    pos, nextPos,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    excludePlayer
            );
            HitResult blockHit = level.clip(clipCtx);
            if (blockHit.getType() == HitResult.Type.BLOCK) {
                points.add(blockHit.getLocation());
                return new SimResult(points, true);
            }

            // ── Entity collision ─────────────────────────────────────────────────
            AABB scanBox = new AABB(nextPos).inflate(0.3);
            List<Entity> nearby = level.getEntities(
                    excludePlayer, scanBox,
                    e -> e.isAlive() && !(e instanceof ExperienceOrb)
            );
            if (!nearby.isEmpty()) {
                points.add(nextPos);
                return new SimResult(points, true);
            }

            // ── Void ─────────────────────────────────────────────────────────────
            if (nextPos.y < level.getMinBuildHeight() - 16.0) {
                return new SimResult(points, false);
            }

            pos = nextPos;
            points.add(pos);
        }

        // Reached max ticks without hitting anything
        return new SimResult(points, false);
    }
}
