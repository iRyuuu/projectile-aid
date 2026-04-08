package com.projectileaid.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.projectileaid.trajectory.ProjectileHelper;
import com.projectileaid.trajectory.ProjectileInfo;
import com.projectileaid.trajectory.TrajectorySimulator;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Registers a WorldRenderEvents.LAST callback and draws the trajectory trail
 * as a series of coloured line segments in world space.
 *
 * Colour convention:
 *   GREEN  – the projectile will hit a block or entity
 *   RED    – the projectile falls into the void (or MAX_TICKS exceeded with no hit)
 *
 * Alpha fades from 85 % at the start to 20 % at the end of the trail.
 */
public class TrajectoryRenderer {

    public static void register() {
        WorldRenderEvents.LAST.register(TrajectoryRenderer::onRenderLast);
    }

    private static void onRenderLast(WorldRenderContext context) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;

        if (player == null || level == null) return;

        // Determine which hand (if any) holds a projectile item
        ProjectileInfo info = ProjectileHelper.getActiveProjectileInfo(player);
        if (info == null) return;

        // ── Physics simulation ────────────────────────────────────────────────────
        Vec3 startPos = player.getEyePosition();
        Vec3 startVel = player.getLookAngle().scale(info.speed());

        TrajectorySimulator.SimResult result = TrajectorySimulator.simulate(
                level, player, startPos, startVel, info.gravity(), info.drag()
        );

        List<Vec3> points = result.points();
        if (points.size() < 2) return;

        // ── Rendering ─────────────────────────────────────────────────────────────
        Vec3 camPos = context.camera().getPosition();
        PoseStack poseStack = context.matrixStack();
        Matrix4f matrix = poseStack.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();   // visible through walls
        RenderSystem.lineWidth(2.0f);
        RenderSystem.setShader(CoreShaders.POSITION_COLOR);

        BufferBuilder buf = Tesselator.getInstance()
                .begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

        boolean hit = result.hitSomething();
        int totalSegments = points.size() - 1;

        for (int i = 0; i < totalSegments; i++) {
            Vec3 p1 = points.get(i);
            Vec3 p2 = points.get(i + 1);

            // Fade alpha 217 (85%) → 51 (20%) along the trail
            float progress = (float) i / totalSegments;
            int alpha = (int) (217 - 166 * progress);

            int r = hit ? 0   : 255;
            int g = hit ? 255 : 0;

            float x1 = (float) (p1.x - camPos.x);
            float y1 = (float) (p1.y - camPos.y);
            float z1 = (float) (p1.z - camPos.z);
            float x2 = (float) (p2.x - camPos.x);
            float y2 = (float) (p2.y - camPos.y);
            float z2 = (float) (p2.z - camPos.z);

            buf.addVertex(matrix, x1, y1, z1).setColor(r, g, 0, alpha);
            buf.addVertex(matrix, x2, y2, z2).setColor(r, g, 0, alpha);
        }

        MeshData mesh = buf.buildOrThrow();
        BufferUploader.drawWithShader(mesh);

        // Restore default GL state
        RenderSystem.enableDepthTest();
        RenderSystem.lineWidth(1.0f);
        RenderSystem.disableBlend();
    }
}
