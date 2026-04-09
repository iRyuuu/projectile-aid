package com.projectileaid.trajectory;

/**
 * Physics parameters and visual category for a projectile type.
 *
 * @param speed    initial launch speed (blocks/tick)
 * @param gravity  downward acceleration per tick (blocks/tick²)
 * @param drag     velocity multiplier per tick (0–1, applied before gravity)
 * @param category COMBAT (bow/crossbow/trident) or UTILITY (throwables/pearls)
 */
public record ProjectileInfo(float speed, float gravity, float drag, ProjectileCategory category) {

    public enum ProjectileCategory {
        /** Bow, crossbow (arrow), trident — red on mob hit, white otherwise. */
        COMBAT,
        /** Snowball, egg, ender pearl, potions — shows configurable colour + landing block. */
        UTILITY
    }
}
