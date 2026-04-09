package com.projectileaid.trajectory;

/**
 * Physics parameters and visual type for a projectile.
 *
 * @param speed          initial launch speed (blocks/tick)
 * @param gravity        downward acceleration per tick (blocks/tick²)
 * @param drag           velocity multiplier per tick (0–1, applied before gravity)
 * @param projectileType which projectile type this is (drives config colour lookup)
 */
public record ProjectileInfo(float speed, float gravity, float drag, ProjectileType projectileType) {

    public enum ProjectileType {
        BOW,
        CROSSBOW_ARROW,
        CROSSBOW_FIREWORK,
        TRIDENT,
        /** Snowball, egg, ender pearl */
        SNOWBALL,
        /** Splash potion, lingering potion */
        POTION,
        XP_BOTTLE,
        WIND_CHARGE
    }
}
