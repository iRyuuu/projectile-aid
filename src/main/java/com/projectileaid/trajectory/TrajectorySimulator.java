package com.projectileaid.trajectory;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
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

    private static final int MAX_TICKS = 300;

    public enum HitType { NONE, BLOCK, ENTITY }

    /**
     * Result of a trajectory simulation.
     *
     * @param points          world-space positions along the path (including start)
     * @param hitType         what the trajectory ended on
     * @param hitEntityBounds AABB of the hit entity in world space, or null
     * @param hitBlockPos     block position of the hit block, or null
     */
    public record SimResult(
            List<Vec3> points,
            HitType hitType,
            AABB hitEntityBounds,
            BlockPos hitBlockPos
    ) {
        public boolean hitSomething() { return hitType != HitType.NONE; }
        public boolean hitEntity()    { return hitType == HitType.ENTITY; }
        public boolean hitBlock()     { return hitType == HitType.BLOCK; }
    }

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
            vel = new Vec3(vel.x * drag, vel.y * drag - gravity, vel.z * drag);
            Vec3 nextPos = pos.add(vel);

            // ── Block collision ──────────────────────────────────────────────────
            ClipContext clipCtx = new ClipContext(
                    pos, nextPos,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    excludePlayer
            );
            HitResult blockHit = level.clip(clipCtx);
            if (blockHit.getType() == HitResult.Type.BLOCK) {
                BlockHitResult bhr = (BlockHitResult) blockHit;
                points.add(bhr.getLocation());
                return new SimResult(points, HitType.BLOCK, null, bhr.getBlockPos());
            }

            // ── Entity collision ─────────────────────────────────────────────────
            AABB scanBox = new AABB(nextPos, nextPos).inflate(0.3);
            List<Entity> nearby = level.getEntities(
                    excludePlayer, scanBox,
                    e -> e.isAlive() && !(e instanceof ExperienceOrb)
            );
            if (!nearby.isEmpty()) {
                AABB entityBounds = nearby.get(0).getBoundingBox();
                points.add(nextPos);
                return new SimResult(points, HitType.ENTITY, entityBounds, null);
            }

            // ── Void ─────────────────────────────────────────────────────────────
            if (nextPos.y < level.getMinY() - 16.0) {
                return new SimResult(points, HitType.NONE, null, null);
            }

            pos = nextPos;
            points.add(pos);
        }

        return new SimResult(points, HitType.NONE, null, null);
    }
}
