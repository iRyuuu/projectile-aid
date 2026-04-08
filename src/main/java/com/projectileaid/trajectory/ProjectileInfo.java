package com.projectileaid.trajectory;

/**
 * Physics parameters for a projectile type.
 *
 * @param speed   initial launch speed (blocks/tick)
 * @param gravity downward acceleration per tick (blocks/tick²)
 * @param drag    velocity multiplier per tick (0–1, applied before gravity)
 */
public record ProjectileInfo(float speed, float gravity, float drag) {}
