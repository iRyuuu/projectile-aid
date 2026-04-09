package com.projectileaid.trajectory;

import net.minecraft.world.phys.Vec3;

/**
 * A single trajectory to simulate — one per arrow for multishot crossbows,
 * one for everything else.
 *
 * @param startPos  world-space eye position of the player
 * @param startVel  initial velocity vector (direction × speed)
 * @param gravity   downward acceleration per tick (blocks/tick²)
 * @param drag      velocity multiplier per tick (0–1)
 * @param category  visual colour group
 */
public record TrajectorySpec(
        Vec3 startPos,
        Vec3 startVel,
        float gravity,
        float drag,
        ProjectileInfo.ProjectileCategory category
) {}
