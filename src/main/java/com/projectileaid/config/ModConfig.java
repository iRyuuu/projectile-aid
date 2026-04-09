package com.projectileaid.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Persistent configuration for Projectile Aid.
 * Saved as JSON in the Fabric config directory.
 */
public class ModConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("projectile-aid.json");

    private static ModConfig INSTANCE = new ModConfig();

    // ── Combat weapon colours (bow / crossbow arrow / trident) ────────────────
    /** Trail + outline colour when a combat projectile will hit a mob. */
    public int combatHitR = 255, combatHitG = 0,   combatHitB = 0;   // red
    /** Trail + outline colour when a combat projectile will NOT hit a mob. */
    public int combatMissR = 255, combatMissG = 255, combatMissB = 255; // white

    // ── Utility projectile colour (pearl / snowball / potions / xp bottle) ───
    public int utilityR = 0, utilityG = 220, utilityB = 80;            // green

    // ── Shared ────────────────────────────────────────────────────────────────
    /** Alpha for the trail line (0–255). */
    public int trailAlpha = 200;
    /** Alpha for the landing outline (0–255). */
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

    /** Packs R, G, B, A into a single ARGB int for use with GuiGraphics.fill(). */
    public static int argb(int r, int g, int b, int a) {
        return (a & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF);
    }

    /** Clamps a value to [0, 255]. */
    public static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
