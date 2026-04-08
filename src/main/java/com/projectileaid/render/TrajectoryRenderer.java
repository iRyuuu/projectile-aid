package com.projectileaid.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.projectileaid.trajectory.ProjectileHelper;
import com.projectileaid.trajectory.ProjectileInfo;
import com.projectileaid.trajectory.TrajectorySimulator;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Registers a WorldRenderEvents.END_MAIN callback and draws the trajectory
 * trail using the 1.21.11 submitCustomGeometry API with RenderTypes.LINES.
 *
 * Colour: GREEN = will hit block/entity, RED = falls into void.
 * Alpha fades 85 % → 20 % along the trail length.
 */
public class TrajectoryRenderer {

    public static void register() {
        WorldRenderEvents.END_MAIN.register(TrajectoryRenderer::onEndMain);
    }

    private static void onEndMain(WorldRenderContext context) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null) return;

        // ── Determine held projectile ─────────────────────────────────────────
        ProjectileInfo info = ProjectileHelper.getActiveProjectileInfo(player);
        if (info == null) return;

        // ── Simulate trajectory ───────────────────────────────────────────────
        Vec3 startPos = player.getEyePosition();
        Vec3 startVel = player.getLookAngle().scale(info.speed());
        TrajectorySimulator.SimResult result = TrajectorySimulator.simulate(
                level, player, startPos, startVel, info.gravity(), info.drag()
        );

        List<Vec3> points = result.points();
        if (points.size() < 2) return;

        // Capture values for the lambda (must be effectively final)
        final boolean hit = result.hitSomething();
        final int totalSegments = points.size() - 1;
        final Vec3 camPos = mc.gameRenderer.getMainCamera().position();

        // ── Submit custom geometry via the render command queue ───────────────
        context.commandQueue().submitCustomGeometry(
                context.matrices(),
                RenderTypes.LINES,
                (pose, consumer) -> {
                    for (int i = 0; i < totalSegments; i++) {
                        Vec3 p1 = points.get(i);
                        Vec3 p2 = points.get(i + 1);

                        // Fade alpha 217 (85 %) → 51 (20 %) along the trail
                        float progress = (float) i / totalSegments;
                        int alpha = (int) (217 - 166 * progress);

                        int r = hit ? 0   : 255;
                        int g = hit ? 255 : 0;

                        // Camera-relative coordinates
                        float x1 = (float) (p1.x - camPos.x);
                        float y1 = (float) (p1.y - camPos.y);
                        float z1 = (float) (p1.z - camPos.z);
                        float x2 = (float) (p2.x - camPos.x);
                        float y2 = (float) (p2.y - camPos.y);
                        float z2 = (float) (p2.z - camPos.z);

                        // Line segment direction (used as normal)
                        Vec3 dir = p2.subtract(p1).normalize();
                        float nx = (float) dir.x;
                        float ny = (float) dir.y;
                        float nz = (float) dir.z;

                        addLineVertex(consumer, pose, x1, y1, z1, r, g, alpha, nx, ny, nz);
                        addLineVertex(consumer, pose, x2, y2, z2, r, g, alpha, nx, ny, nz);
                    }
                }
        );
    }

    private static void addLineVertex(
            VertexConsumer consumer,
            com.mojang.blaze3d.vertex.PoseStack.Pose pose,
            float x, float y, float z,
            int r, int g, int alpha,
            float nx, float ny, float nz
    ) {
        consumer.addVertex(pose, x, y, z)
                .setColor(r, g, 0, alpha)
                .setNormal(pose, nx, ny, nz)
                .setLineWidth(2.0f);
    }
}
