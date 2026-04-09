package com.projectileaid.screen;

import com.projectileaid.config.ModConfig;
import com.projectileaid.trajectory.ProjectileInfo.ProjectileType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * In-game configuration screen for Projectile Aid.
 * Shows one RGB row per projectile type, opened via Fabric Mod Menu.
 */
public class ConfigScreen extends Screen {

    private final Screen parent;

    private static final int FIELD_W   = 36;
    private static final int FIELD_H   = 16;
    private static final int FIELD_GAP = 2;
    private static final int ROW_H     = 22;
    private static final int PREVIEW_W = 18;
    private static final int LABEL_W   = 150;
    // Total width of one row: label + gap + 3 fields + 2 inner gaps + gap + preview
    private static final int ROW_CONTENT_W =
            LABEL_W + 10 + FIELD_W * 3 + FIELD_GAP * 2 + 8 + PREVIEW_W;

    private static final ProjectileType[] ROW_TYPES = {
            ProjectileType.BOW,
            ProjectileType.CROSSBOW_ARROW,
            ProjectileType.CROSSBOW_FIREWORK,
            ProjectileType.TRIDENT,
            ProjectileType.SNOWBALL,
            ProjectileType.POTION,
            ProjectileType.XP_BOTTLE,
    };

    private static final String[] ROW_LABELS = {
            "Bow",
            "Crossbow \u2014 Arrow",
            "Crossbow \u2014 Firework",
            "Trident",
            "Snowball / Egg / Pearl",
            "Splash / Lingering Potion",
            "XP Bottle",
    };

    /** Working copies — rgb[row][0..2] = R, G, B */
    private final int[][] rgb = new int[ROW_TYPES.length][3];

    /** EditBox widgets — boxes[row][0..2] = R, G, B */
    private final List<EditBox[]> boxes = new ArrayList<>();

    public ConfigScreen(Screen parent) {
        super(Component.literal("Projectile Aid \u2014 Trail Colours"));
        this.parent = parent;
    }

    // ── Screen lifecycle ──────────────────────────────────────────────────────

    @Override
    protected void init() {
        ModConfig cfg = ModConfig.get();

        for (int i = 0; i < ROW_TYPES.length; i++) {
            int[] c = cfg.colorFor(ROW_TYPES[i]);
            rgb[i][0] = c[0];
            rgb[i][1] = c[1];
            rgb[i][2] = c[2];
        }

        boxes.clear();

        int totalH  = ROW_TYPES.length * ROW_H;
        int startY  = height / 2 - totalH / 2;
        int fieldsX = rowStartX() + LABEL_W + 10;

        for (int row = 0; row < ROW_TYPES.length; row++) {
            boxes.add(makeRow(fieldsX, startY + row * ROW_H, rgb[row]));
        }

        int buttonY = startY + totalH + 12;
        addRenderableWidget(Button.builder(
                Component.literal("Save"),
                b -> saveAndClose()
        ).bounds(width / 2 - 82, buttonY, 80, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("Cancel"),
                b -> onClose()
        ).bounds(width / 2 + 2, buttonY, 80, 20).build());
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float delta) {
        renderBackground(gfx, mouseX, mouseY, delta);

        int totalH   = ROW_TYPES.length * ROW_H;
        int startY   = height / 2 - totalH / 2;
        int rowStart = rowStartX();
        int labelX   = rowStart;
        int fieldsX  = rowStart + LABEL_W + 10;
        int previewX = fieldsX + FIELD_W * 3 + FIELD_GAP * 2 + 6;

        // Title
        gfx.drawCenteredString(font, title, width / 2, startY - 24, 0xFFFFFF);

        // R / G / B column headers (above first row)
        String[] chan = {"R", "G", "B"};
        for (int i = 0; i < 3; i++) {
            int cx = fieldsX + i * (FIELD_W + FIELD_GAP) + FIELD_W / 2 - font.width(chan[i]) / 2;
            gfx.drawString(font, chan[i], cx, startY - 12, 0xFF888888, false);
        }

        for (int i = 0; i < ROW_TYPES.length; i++) {
            int y = startY + i * ROW_H;
            int textY = y + (FIELD_H - font.lineHeight) / 2 + 1;

            // Row label
            gfx.drawString(font, ROW_LABELS[i], labelX, textY, 0xFFCCCCCC, false);

            // Colour preview swatch (live — reflects current EditBox values)
            int argb = ModConfig.argb(rgb[i][0], rgb[i][1], rgb[i][2], 255);
            gfx.fill(previewX,     y,         previewX + PREVIEW_W,     y + FIELD_H, 0xFF000000);
            gfx.fill(previewX + 1, y + 1, previewX + PREVIEW_W - 1, y + FIELD_H - 1, argb);
        }

        super.render(gfx, mouseX, mouseY, delta);
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

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Left edge of the whole row block, centred on screen. */
    private int rowStartX() {
        return width / 2 - ROW_CONTENT_W / 2;
    }

    /** Creates three side-by-side EditBoxes (R, G, B) for one colour row. */
    private EditBox[] makeRow(int fieldsX, int y, int[] rowRgb) {
        EditBox[] row = new EditBox[3];
        for (int i = 0; i < 3; i++) {
            int x = fieldsX + i * (FIELD_W + FIELD_GAP);
            final int channel = i;
            EditBox box = new EditBox(font, x, y, FIELD_W, FIELD_H, Component.empty());
            box.setMaxLength(3);
            box.setFilter(s -> s.isEmpty() || (s.matches("\\d+") && Integer.parseInt(s) <= 255));
            box.setValue(String.valueOf(rowRgb[channel]));
            box.setResponder(s -> {
                if (!s.isEmpty() && s.matches("\\d+")) {
                    rowRgb[channel] = ModConfig.clamp(Integer.parseInt(s));
                }
            });
            addRenderableWidget(box);
            row[i] = box;
        }
        return row;
    }

    private void saveAndClose() {
        ModConfig cfg = ModConfig.get();
        for (int i = 0; i < ROW_TYPES.length; i++) {
            setColor(cfg, ROW_TYPES[i], rgb[i]);
        }
        ModConfig.save();
        onClose();
    }

    private void setColor(ModConfig cfg, ProjectileType type, int[] c) {
        switch (type) {
            case BOW               -> { cfg.bowR        = c[0]; cfg.bowG        = c[1]; cfg.bowB        = c[2]; }
            case CROSSBOW_ARROW    -> { cfg.cbArrowR    = c[0]; cfg.cbArrowG    = c[1]; cfg.cbArrowB    = c[2]; }
            case CROSSBOW_FIREWORK -> { cfg.cbFireworkR = c[0]; cfg.cbFireworkG = c[1]; cfg.cbFireworkB = c[2]; }
            case TRIDENT           -> { cfg.tridentR    = c[0]; cfg.tridentG    = c[1]; cfg.tridentB    = c[2]; }
            case SNOWBALL          -> { cfg.snowballR   = c[0]; cfg.snowballG   = c[1]; cfg.snowballB   = c[2]; }
            case POTION            -> { cfg.potionR     = c[0]; cfg.potionG     = c[1]; cfg.potionB     = c[2]; }
            case XP_BOTTLE         -> { cfg.xpBottleR   = c[0]; cfg.xpBottleG   = c[1]; cfg.xpBottleB   = c[2]; }
        }
    }
}
