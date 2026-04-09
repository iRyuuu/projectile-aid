package com.projectileaid.screen;

import com.projectileaid.config.ModConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * In-game configuration screen for Projectile Aid, opened via Fabric Mod Menu.
 *
 * Layout (two sections):
 *   ▶ TRAJECTORY PREVIEW VISUALISATION
 *       Enabled   [ON / OFF]
 *       Color     [◄] ████ ColorName [►]
 *       Opacity   [−] 200 [+]
 *       Style     [Solid / Dashed]
 *
 *   ▶ TARGET OUTLINING
 *       Enabled   [ON / OFF]
 *       Color     [◄] ████ ColorName [►]
 *
 *                       [Done]
 */
public class ConfigScreen extends Screen {

    private final Screen parent;

    // Working copies of config values — only written to ModConfig on Done
    private boolean trajEnabled;
    private int     trajColorIdx;
    private int     trajOpacity;
    private boolean trajDashed;
    private boolean outlineEnabled;
    private int     outlineColorIdx;

    // Button references so we can call setMessage() when values change
    private Button btnTrajEnabled;
    private Button btnTrajColorPrev;
    private Button btnTrajColorNext;
    private Button btnTrajOpacityDown;
    private Button btnTrajOpacityUp;
    private Button btnTrajStyle;
    private Button btnOutlineEnabled;
    private Button btnOutlineColorPrev;
    private Button btnOutlineColorNext;

    // Swatch positions set during init() and used during render()
    private int trajSwatchX, trajSwatchY;
    private int outlineSwatchX, outlineSwatchY;

    // Layout constants
    private static final int ROW_H       = 24;
    private static final int BTN_H       = 20;
    private static final int SWATCH_SIZE = 18;
    private static final int SWATCH_GAP  = 4;
    private static final int COLOR_BTN_W = 16;
    private static final int COLOR_LABEL_W = 60; // space for colour name text

    public ConfigScreen(Screen parent) {
        super(Component.literal("Projectile Aid \u2014 Configuration"));
        this.parent = parent;
    }

    // ── Screen lifecycle ──────────────────────────────────────────────────────

    @Override
    protected void init() {
        // Load working copies
        ModConfig cfg  = ModConfig.get();
        trajEnabled    = cfg.trajectoryEnabled;
        trajColorIdx   = cfg.trajectoryColorIdx;
        trajOpacity    = cfg.trajectoryOpacity;
        trajDashed     = cfg.trajectoryDashed;
        outlineEnabled = cfg.outlineEnabled;
        outlineColorIdx= cfg.outlineColorIdx;

        // Layout anchors
        int cx        = width  / 2;
        int labelX    = cx - 130;  // left edge of row labels
        int ctrlX     = cx - 10;   // left edge of control widgets

        // Section 1 starts here; section 2 is below
        int sec1Y = height / 2 - 105;
        int sec2Y = sec1Y + 5 * ROW_H + 18; // 4 rows + gap

        // ── Trajectory section ─────────────────────────────────────────────

        int y = sec1Y + ROW_H; // first row below section header

        // Enabled
        btnTrajEnabled = Button.builder(
                enabledLabel(trajEnabled),
                b -> { trajEnabled = !trajEnabled; b.setMessage(enabledLabel(trajEnabled)); }
        ).bounds(ctrlX, y, 80, BTN_H).build();
        addRenderableWidget(btnTrajEnabled);
        y += ROW_H;

        // Color ◄ ▶ — swatch drawn in render()
        trajSwatchX = ctrlX;
        trajSwatchY = y + 1;
        btnTrajColorPrev = Button.builder(Component.literal("\u25C4"), b -> {
            trajColorIdx = (trajColorIdx - 1 + ModConfig.PRESET_COLORS.length) % ModConfig.PRESET_COLORS.length;
        }).bounds(ctrlX, y, COLOR_BTN_W, BTN_H).build();
        btnTrajColorNext = Button.builder(Component.literal("\u25BA"), b -> {
            trajColorIdx = (trajColorIdx + 1) % ModConfig.PRESET_COLORS.length;
        }).bounds(ctrlX + COLOR_BTN_W + SWATCH_SIZE + SWATCH_GAP * 2 + COLOR_LABEL_W, y, COLOR_BTN_W, BTN_H).build();
        addRenderableWidget(btnTrajColorPrev);
        addRenderableWidget(btnTrajColorNext);
        y += ROW_H;

        // Opacity − +
        btnTrajOpacityDown = Button.builder(Component.literal("\u2212"), b -> {
            trajOpacity = ModConfig.clamp(trajOpacity - 10, 10, 255);
        }).bounds(ctrlX, y, COLOR_BTN_W, BTN_H).build();
        btnTrajOpacityUp = Button.builder(Component.literal("+"), b -> {
            trajOpacity = ModConfig.clamp(trajOpacity + 10, 10, 255);
        }).bounds(ctrlX + COLOR_BTN_W + 36, y, COLOR_BTN_W, BTN_H).build();
        addRenderableWidget(btnTrajOpacityDown);
        addRenderableWidget(btnTrajOpacityUp);
        y += ROW_H;

        // Style
        btnTrajStyle = Button.builder(
                styleLabel(trajDashed),
                b -> { trajDashed = !trajDashed; b.setMessage(styleLabel(trajDashed)); }
        ).bounds(ctrlX, y, 80, BTN_H).build();
        addRenderableWidget(btnTrajStyle);

        // ── Target outline section ─────────────────────────────────────────

        y = sec2Y + ROW_H;

        btnOutlineEnabled = Button.builder(
                enabledLabel(outlineEnabled),
                b -> { outlineEnabled = !outlineEnabled; b.setMessage(enabledLabel(outlineEnabled)); }
        ).bounds(ctrlX, y, 80, BTN_H).build();
        addRenderableWidget(btnOutlineEnabled);
        y += ROW_H;

        outlineSwatchX = ctrlX;
        outlineSwatchY = y + 1;
        btnOutlineColorPrev = Button.builder(Component.literal("\u25C4"), b -> {
            outlineColorIdx = (outlineColorIdx - 1 + ModConfig.PRESET_COLORS.length) % ModConfig.PRESET_COLORS.length;
        }).bounds(ctrlX, y, COLOR_BTN_W, BTN_H).build();
        btnOutlineColorNext = Button.builder(Component.literal("\u25BA"), b -> {
            outlineColorIdx = (outlineColorIdx + 1) % ModConfig.PRESET_COLORS.length;
        }).bounds(ctrlX + COLOR_BTN_W + SWATCH_SIZE + SWATCH_GAP * 2 + COLOR_LABEL_W, y, COLOR_BTN_W, BTN_H).build();
        addRenderableWidget(btnOutlineColorPrev);
        addRenderableWidget(btnOutlineColorNext);

        // ── Done button ────────────────────────────────────────────────────

        addRenderableWidget(Button.builder(
                Component.literal("Done"),
                b -> saveAndClose()
        ).bounds(cx - 40, sec2Y + 3 * ROW_H, 80, BTN_H).build());
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float delta) {
        renderBackground(gfx, mouseX, mouseY, delta);

        int cx     = width  / 2;
        int labelX = cx - 130;
        int sec1Y  = height / 2 - 105;
        int sec2Y  = sec1Y + 5 * ROW_H + 18;

        // ── Section headers ────────────────────────────────────────────────
        int headerColor = 0xFFFFCC00; // gold
        gfx.drawString(font, "\u25BA TRAJECTORY PREVIEW VISUALISATION", labelX, sec1Y + 4, headerColor, false);
        gfx.drawString(font, "\u25BA TARGET OUTLINING",                  labelX, sec2Y + 4, headerColor, false);

        // Title
        gfx.drawCenteredString(font, title, cx, sec1Y - 18, 0xFFFFFFFF);

        // ── Row labels ─────────────────────────────────────────────────────
        int rowLabelColor = 0xFFCCCCCC;
        // Section 1
        int y = sec1Y + ROW_H;
        gfx.drawString(font, "Enabled:",  labelX, y + 6, rowLabelColor, false); y += ROW_H;
        gfx.drawString(font, "Color:",    labelX, y + 6, rowLabelColor, false); y += ROW_H;
        gfx.drawString(font, "Opacity:",  labelX, y + 6, rowLabelColor, false); y += ROW_H;
        gfx.drawString(font, "Style:",    labelX, y + 6, rowLabelColor, false);
        // Section 2
        y = sec2Y + ROW_H;
        gfx.drawString(font, "Enabled:",  labelX, y + 6, rowLabelColor, false); y += ROW_H;
        gfx.drawString(font, "Color:",    labelX, y + 6, rowLabelColor, false);

        // ── Trajectory colour swatch + name ────────────────────────────────
        renderColorControl(gfx, trajSwatchX, trajSwatchY, trajColorIdx);

        // ── Trajectory opacity value ────────────────────────────────────────
        int opY = sec1Y + ROW_H * 3;
        int opCtrlX = cx - 10;
        // Value text sits between − and + (each COLOR_BTN_W=16 wide, gap of 4px each side)
        String opText = String.valueOf(trajOpacity);
        int textW = font.width(opText);
        gfx.drawString(font, opText, opCtrlX + COLOR_BTN_W + (36 - textW) / 2, opY + 6, 0xFFFFFFFF, false);

        // ── Outline colour swatch + name ───────────────────────────────────
        renderColorControl(gfx, outlineSwatchX, outlineSwatchY, outlineColorIdx);

        super.render(gfx, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        assert minecraft != null;
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Draws the colour swatch (between ◄ and ► buttons) and the colour name.
     * swatchX is the left edge of the ◄ button; the swatch follows immediately after.
     */
    private void renderColorControl(GuiGraphics gfx, int swatchX, int swatchY, int colorIdx) {
        int[] rgb = ModConfig.PRESET_COLORS[colorIdx];
        int swX = swatchX + COLOR_BTN_W + SWATCH_GAP;

        // Black border then coloured fill
        gfx.fill(swX,     swatchY,     swX + SWATCH_SIZE,     swatchY + SWATCH_SIZE,     0xFF000000);
        gfx.fill(swX + 1, swatchY + 1, swX + SWATCH_SIZE - 1, swatchY + SWATCH_SIZE - 1,
                ModConfig.argb(rgb[0], rgb[1], rgb[2], 255));

        // Colour name
        int nameX = swX + SWATCH_SIZE + SWATCH_GAP;
        int nameY = swatchY + (SWATCH_SIZE - font.lineHeight) / 2;
        gfx.drawString(font, ModConfig.COLOR_NAMES[colorIdx], nameX, nameY, 0xFFFFFFFF, false);
    }

    private static Component enabledLabel(boolean enabled) {
        return Component.literal(enabled ? "ON" : "OFF");
    }

    private static Component styleLabel(boolean dashed) {
        return Component.literal(dashed ? "Dashed" : "Solid");
    }

    private void saveAndClose() {
        ModConfig cfg          = ModConfig.get();
        cfg.trajectoryEnabled  = trajEnabled;
        cfg.trajectoryColorIdx = trajColorIdx;
        cfg.trajectoryOpacity  = trajOpacity;
        cfg.trajectoryDashed   = trajDashed;
        cfg.outlineEnabled     = outlineEnabled;
        cfg.outlineColorIdx    = outlineColorIdx;
        ModConfig.save();
        onClose();
    }
}
