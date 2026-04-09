package com.projectileaid.screen;

import com.projectileaid.config.ModConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * In-game configuration screen for Projectile Aid.
 * Opened via the Fabric Mod Menu or a keybind.
 */
public class ConfigScreen extends Screen {

    private final Screen parent;

    // ── EditBox groups — each group is [R, G, B] ──────────────────────────────
    private final List<EditBox[]> colorBoxes = new ArrayList<>();

    // Working copies (written on Save)
    private int[] combatHit;
    private int[] combatMiss;
    private int[] utility;

    private static final int FIELD_W  = 38;
    private static final int FIELD_H  = 18;
    private static final int ROW_H    = 28;
    private static final int PREVIEW_W = 20;

    public ConfigScreen(Screen parent) {
        super(Component.literal("Projectile Aid Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ModConfig cfg = ModConfig.get();
        combatHit  = new int[]{cfg.combatHitR,  cfg.combatHitG,  cfg.combatHitB};
        combatMiss = new int[]{cfg.combatMissR, cfg.combatMissG, cfg.combatMissB};
        utility    = new int[]{cfg.utilityR,    cfg.utilityG,    cfg.utilityB};

        colorBoxes.clear();

        int startY = height / 2 - 50;
        colorBoxes.add(makeRow(startY,             combatHit));
        colorBoxes.add(makeRow(startY + ROW_H,     combatMiss));
        colorBoxes.add(makeRow(startY + ROW_H * 2, utility));

        // Save button
        addRenderableWidget(Button.builder(
                Component.literal("Save"),
                b -> saveAndClose()
        ).bounds(width / 2 - 82, height / 2 + 55, 80, 20).build());

        // Cancel button
        addRenderableWidget(Button.builder(
                Component.literal("Cancel"),
                b -> onClose()
        ).bounds(width / 2 + 2, height / 2 + 55, 80, 20).build());
    }

    /** Creates three side-by-side EditBox fields (R, G, B) for one colour row. */
    private EditBox[] makeRow(int y, int[] rgb) {
        int startX = width / 2 - (FIELD_W * 3 + 4 + PREVIEW_W + 2) / 2;
        EditBox[] boxes = new EditBox[3];
        for (int i = 0; i < 3; i++) {
            int x = startX + i * (FIELD_W + 2);
            final int idx = i;
            EditBox box = new EditBox(font, x, y, FIELD_W, FIELD_H, Component.empty());
            box.setMaxLength(3);
            box.setFilter(s -> s.isEmpty() || (s.matches("\\d+") && Integer.parseInt(s) <= 255));
            box.setValue(String.valueOf(rgb[idx]));
            box.setResponder(s -> {
                if (!s.isEmpty() && s.matches("\\d+")) {
                    rgb[idx] = ModConfig.clamp(Integer.parseInt(s));
                }
            });
            addRenderableWidget(box);
            boxes[i] = box;
        }
        return boxes;
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float delta) {
        renderBackground(gfx, mouseX, mouseY, delta);

        // Title
        gfx.drawCenteredString(font, title, width / 2, height / 2 - 80, 0xFFFFFFFF);

        // Row labels
        int startY = height / 2 - 50;
        String[] labels = {"Combat — hit mob (R G B):", "Combat — no target  (R G B):", "Utility / throwables (R G B):"};
        int[][] colors  = {combatHit, combatMiss, utility};

        int labelX  = width / 2 - 130;
        int previewX = width / 2 + FIELD_W + 14;

        for (int i = 0; i < 3; i++) {
            int y = startY + i * ROW_H;
            gfx.drawString(font, labels[i], labelX, y + 5, 0xFFCCCCCC);

            // Colour preview swatch
            int argb = ModConfig.argb(colors[i][0], colors[i][1], colors[i][2], 255);
            gfx.fill(previewX, y, previewX + PREVIEW_W, y + FIELD_H, 0xFF000000);
            gfx.fill(previewX + 1, y + 1, previewX + PREVIEW_W - 1, y + FIELD_H - 1, argb);
        }

        // R G B header above first row
        int startX = width / 2 - (FIELD_W * 3 + 4 + PREVIEW_W + 2) / 2;
        String[] chan = {"R", "G", "B"};
        for (int i = 0; i < 3; i++) {
            int cx = startX + i * (FIELD_W + 2) + FIELD_W / 2 - font.width(chan[i]) / 2;
            gfx.drawString(font, chan[i], cx, startY - 10, 0xFF888888);
        }

        super.render(gfx, mouseX, mouseY, delta);
    }

    private void saveAndClose() {
        ModConfig cfg = ModConfig.get();
        cfg.combatHitR  = combatHit[0];  cfg.combatHitG  = combatHit[1];  cfg.combatHitB  = combatHit[2];
        cfg.combatMissR = combatMiss[0]; cfg.combatMissG = combatMiss[1]; cfg.combatMissB = combatMiss[2];
        cfg.utilityR    = utility[0];    cfg.utilityG    = utility[1];    cfg.utilityB    = utility[2];
        ModConfig.save();
        onClose();
    }

    @Override
    public void onClose() {
        assert minecraft != null;
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
