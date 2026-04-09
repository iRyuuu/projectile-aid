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

    // ── Preset colour palette ─────────────────────────────────────────────────

    public static final int[][] PRESET_COLORS = {
            {  0, 220,  80},  // 0 Green
            {255,  60,  60},  // 1 Red
            { 60, 120, 255},  // 2 Blue
            {  0, 220, 255},  // 3 Cyan
            {255, 140,   0},  // 4 Orange
            {200,   0, 255},  // 5 Purple
            {255, 230,   0},  // 6 Yellow
            {255, 255, 255},  // 7 White
    };

    public static final String[] COLOR_NAMES = {
            "Green", "Red", "Blue", "Cyan", "Orange", "Purple", "Yellow", "White"
    };

    // ── Trajectory section ────────────────────────────────────────────────────

    public boolean trajectoryEnabled   = true;
    public int     trajectoryColorIdx  = 0;      // index into PRESET_COLORS
    public int     trajectoryOpacity   = 200;    // 0–255
    public boolean trajectoryDashed    = false;  // false = Solid, true = Dashed

    // ── Target outline section ────────────────────────────────────────────────

    public boolean outlineEnabled      = true;
    public int     outlineColorIdx     = 7;      // White by default

    // ─────────────────────────────────────────────────────────────────────────

    public static ModConfig get() { return INSTANCE; }

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
                if (loaded != null) {
                    // Clamp loaded indices in case palette size ever changes
                    loaded.trajectoryColorIdx = clampIdx(loaded.trajectoryColorIdx);
                    loaded.outlineColorIdx    = clampIdx(loaded.outlineColorIdx);
                    loaded.trajectoryOpacity  = clamp(loaded.trajectoryOpacity, 0, 255);
                    INSTANCE = loaded;
                }
            } catch (IOException ignored) {}
        }
        save(); // write defaults if file didn't exist
    }

    public static void save() {
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(INSTANCE, writer);
        } catch (IOException ignored) {}
    }

    /** Returns [R, G, B] for the trajectory trail colour. */
    public int[] trajectoryRGB() {
        return PRESET_COLORS[trajectoryColorIdx];
    }

    /** Returns [R, G, B] for the target outline colour. */
    public int[] outlineRGB() {
        return PRESET_COLORS[outlineColorIdx];
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    /** Packs R, G, B, A into a single ARGB int. */
    public static int argb(int r, int g, int b, int a) {
        return (a & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF);
    }

    public static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static int clampIdx(int idx) {
        return clamp(idx, 0, PRESET_COLORS.length - 1);
    }
}
