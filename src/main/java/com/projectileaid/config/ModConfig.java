package com.projectileaid.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.projectileaid.trajectory.ProjectileInfo.ProjectileType;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Persistent configuration for Projectile Aid.
 * Saved as JSON in the Fabric config directory.
 * Each projectile type has its own RGB trail colour.
 */
public class ModConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("projectile-aid.json");

    private static ModConfig INSTANCE = new ModConfig();

    // ── Per-projectile trail colours (RGB 0–255) ──────────────────────────────
    public int bowR = 0,   bowG = 220,  bowB = 80;       // green
    public int cbArrowR = 0,   cbArrowG = 200,  cbArrowB = 255;  // cyan
    public int cbFireworkR = 255, cbFireworkG = 140, cbFireworkB = 0;  // orange
    public int tridentR = 30,  tridentG = 100,  tridentB = 255;  // blue
    public int snowballR = 180, snowballG = 220, snowballB = 255; // light blue
    public int potionR = 200,  potionG = 0,    potionB = 255;   // purple
    public int xpBottleR = 255, xpBottleG = 230, xpBottleB = 0;  // yellow

    // ── Shared ────────────────────────────────────────────────────────────────
    public int trailAlpha   = 200;
    public int outlineAlpha = 255;

    // ─────────────────────────────────────────────────────────────────────────

    public static ModConfig get() {
        return INSTANCE;
    }

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
                if (loaded != null) INSTANCE = loaded;
            } catch (IOException ignored) {
            }
        }
        save(); // write defaults if file didn't exist
    }

    public static void save() {
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(INSTANCE, writer);
        } catch (IOException ignored) {
        }
    }

    /** Returns [R, G, B] for the given projectile type. */
    public int[] colorFor(ProjectileType type) {
        return switch (type) {
            case BOW              -> new int[]{bowR,        bowG,        bowB};
            case CROSSBOW_ARROW   -> new int[]{cbArrowR,    cbArrowG,    cbArrowB};
            case CROSSBOW_FIREWORK-> new int[]{cbFireworkR, cbFireworkG, cbFireworkB};
            case TRIDENT          -> new int[]{tridentR,    tridentG,    tridentB};
            case SNOWBALL         -> new int[]{snowballR,   snowballG,   snowballB};
            case POTION           -> new int[]{potionR,     potionG,     potionB};
            case XP_BOTTLE        -> new int[]{xpBottleR,   xpBottleG,   xpBottleB};
        };
    }

    /** Packs R, G, B, A into a single ARGB int for use with GuiGraphics.fill(). */
    public static int argb(int r, int g, int b, int a) {
        return (a & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF);
    }

    /** Clamps a value to [0, 255]. */
    public static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
