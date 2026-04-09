package com.projectileaid.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.projectileaid.config.ModConfig;
import com.projectileaid.trajectory.*;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Renders trajectory trails and target outlines each frame.
 * Reads all settings (enabled, colour, opacity, style) from ModConfig.
 */
public class TrajectoryRenderer {

    public static void register() {
        WorldRenderEvents.END_MAIN.register(TrajectoryRenderer::onEndMain);
    }

    private static void onEndMain(WorldRenderContext context) {
        ModConfig cfg = ModConfig.get();
        if (!cfg.trajectoryEnabled && !cfg.outlineEnabled) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level  = mc.level;
        if (player == null || level == null) return;

        List<TrajectorySpec> specs = ProjectileHelper.getActiveTrajectories(player);
        if (specs.isEmpty()) return;

        Vec3 camPos = mc.gameRenderer.getMainCamera().position();

        for (TrajectorySpec spec : specs) {
            TrajectorySimulator.SimResult result = TrajectorySimulator.simulate(
                    level, player,
                    spec.startPos(), spec.startVel(),
                    spec.gravity(), spec.drag()
            );
            renderResult(context, camPos, result, cfg);
        }
    }

    // ── Routing ───────────────────────────────────────────────────────────────

    private static void renderResult(
            WorldRenderContext context,
            Vec3 camPos,
            TrajectorySimulator.SimResult result,
            ModConfig cfg
    ) {
        List<Vec3> points = result.points();
        if (points.size() < 2) return;

        // ── Trail ─────────────────────────────────────────────────────────────
        if (cfg.trajectoryEnabled) {
            int[] rgb = cfg.trajectoryRGB();
            int r = rgb[0], g = rgb[1], b = rgb[2];
            final int totalSegments = points.size() - 1;
            final boolean dashed = cfg.trajectoryDashed;
            final int opacity = cfg.trajectoryOpacity;

            context.commandQueue().submitCustomGeometry(
                    context.matrices(),
                    RenderTypes.LINES,
                    (pose, consumer) -> {
                        for (int i = 0; i < totalSegments; i++) {
                            // Dashed: draw pairs 0-1, skip 2-3, draw 4-5 …
                            if (dashed && (i % 4) >= 2) continue;

                            Vec3 p1 = points.get(i);
                            Vec3 p2 = points.get(i + 1);

                            // Alpha fades from full → ~25% toward end of trail
                            float progress = (float) i / totalSegments;
                            int alpha = (int) (opacity * (1.0f - 0.75f * progress));

                            Vec3 dir = p2.subtract(p1).normalize();
                            float nx = (float) dir.x, ny = (float) dir.y, nz = (float) dir.z;

                            addVertex(consumer, pose, camPos, p1, r, g, b, alpha, nx, ny, nz);
                            addVertex(consumer, pose, camPos, p2, r, g, b, alpha, nx, ny, nz);
                        }
                    }
            );
        }

        // ── Target outline ────────────────────────────────────────────────────
        if (cfg.outlineEnabled) {
            int[] oc = cfg.outlineRGB();
            int or_ = oc[0], og = oc[1], ob = oc[2];
            int oa  = 255;

            if (result.hitBlock()) {
                BlockPos bp = result.hitBlockPos();
                double ex = 0.002;
                drawBoxOutline(context, camPos,
                        bp.getX() - ex,     bp.getY() - ex,     bp.getZ() - ex,
                        bp.getX() + 1 + ex, bp.getY() + 1 + ex, bp.getZ() + 1 + ex,
                        or_, og, ob, oa);
            } else if (result.hitEntity()) {
                AABB bb = result.hitEntityBounds();
                double ex = 0.05;
                drawBoxOutline(context, camPos,
                        bb.minX - ex, bb.minY - ex, bb.minZ - ex,
                        bb.maxX + ex, bb.maxY + ex, bb.maxZ + ex,
                        or_, og, ob, oa);
            }
        }
    }

    // ── Box outline ───────────────────────────────────────────────────────────

    private static void drawBoxOutline(
            WorldRenderContext context, Vec3 camPos,
            double x1, double y1, double z1,
            double x2, double y2, double z2,
            int r, int g, int b, int alpha
    ) {
        context.commandQueue().submitCustomGeometry(
                context.matrices(),
                RenderTypes.LINES,
                (pose, consumer) -> {
                    // Bottom face
                    addEdge(consumer, pose, camPos, x1,y1,z1, x2,y1,z1, r,g,b,alpha);
                    addEdge(consumer, pose, camPos, x2,y1,z1, x2,y1,z2, r,g,b,alpha);
                    addEdge(consumer, pose, camPos, x2,y1,z2, x1,y1,z2, r,g,b,alpha);
                    addEdge(consumer, pose, camPos, x1,y1,z2, x1,y1,z1, r,g,b,alpha);
                    // Top face
                    addEdge(consumer, pose, camPos, x1,y2,z1, x2,y2,z1, r,g,b,alpha);
                    addEdge(consumer, pose, camPos, x2,y2,z1, x2,y2,z2, r,g,b,alpha);
                    addEdge(consumer, pose, camPos, x2,y2,z2, x1,y2,z2, r,g,b,alpha);
                    addEdge(consumer, pose, camPos, x1,y2,z2, x1,y2,z1, r,g,b,alpha);
                    // Vertical edges
                    addEdge(consumer, pose, camPos, x1,y1,z1, x1,y2,z1, r,g,b,alpha);
                    addEdge(consumer, pose, camPos, x2,y1,z1, x2,y2,z1, r,g,b,alpha);
                    addEdge(consumer, pose, camPos, x2,y1,z2, x2,y2,z2, r,g,b,alpha);
                    addEdge(consumer, pose, camPos, x1,y1,z2, x1,y2,z2, r,g,b,alpha);
                }
        );
    }

    // ── Vertex helpers ────────────────────────────────────────────────────────

    private static void addEdge(
            VertexConsumer consumer, PoseStack.Pose pose, Vec3 cam,
            double ax, double ay, double az,
            double bx, double by, double bz,
            int r, int g, int b, int alpha
    ) {
        float nx = (float)(bx-ax), ny = (float)(by-ay), nz = (float)(bz-az);
        float len = (float) Math.sqrt(nx*nx + ny*ny + nz*nz);
        if (len > 0) { nx /= len; ny /= len; nz /= len; }
        consumer.addVertex(pose, (float)(ax-cam.x), (float)(ay-cam.y), (float)(az-cam.z))
                .setColor(r,g,b,alpha).setNormal(pose,nx,ny,nz).setLineWidth(2.0f);
        consumer.addVertex(pose, (float)(bx-cam.x), (float)(by-cam.y), (float)(bz-cam.z))
                .setColor(r,g,b,alpha).setNormal(pose,nx,ny,nz).setLineWidth(2.0f);
    }

    private static void addVertex(
            VertexConsumer consumer, PoseStack.Pose pose, Vec3 cam,
            Vec3 pos, int r, int g, int b, int alpha,
            float nx, float ny, float nz
    ) {
        consumer.addVertex(pose,
                        (float)(pos.x-cam.x),
                        (float)(pos.y-cam.y),
                        (float)(pos.z-cam.z))
                .setColor(r,g,b,alpha)
                .setNormal(pose,nx,ny,nz)
                .setLineWidth(2.0f);
    }
}
