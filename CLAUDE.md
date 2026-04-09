# Projectile Aid – Developer Reference

## Project Overview
A **client-side** Fabric mod for Minecraft 1.21.11 that renders a real-time 3D trajectory trail for any held projectile item. The trail is **green** when the projectile will hit a block or entity, and **red** when it falls into the void.

---

## Exact Versions (do not change without good reason)

| Component         | Version                  |
|-------------------|--------------------------|
| Minecraft         | 1.21.11                  |
| Fabric Loader     | 0.18.1                   |
| Fabric API        | 0.141.3+1.21.11          |
| Fabric Loom       | 1.14-SNAPSHOT            |
| Mappings          | **Mojang official** (NOT Yarn — Yarn is unavailable for 1.21.11) |
| Java              | 21 (source/target)        |
| Gradle            | 8.11.1 (via wrapper)     |

> **Important:** Minecraft 1.21.11 is a real Java Edition release from late 2025. It is the last obfuscated version before the new versioning system (26.1). Use `loom.officialMojangMappings()` in build.gradle.

---

## Author

The author of this mod is **Ryuu**. Never use "TDumitrescu" or "tdumitrescu" in any
file, comment, string, or commit message — always use "Ryuu" instead.

---

## GitHub Repository

**Repo:** `https://github.com/iRyuuu/projectile-aid`
**Owner:** Ryuu

### Commit workflow
After **every meaningful change**, commit and push:
```bash
git add -A
git commit -m "descriptive message"
git push origin main
```
Use the `gh` CLI (`gh repo view`, `gh repo create`, etc.) for GitHub operations.

---

## Source Structure

```
src/main/java/com/projectileaid/
├── ProjectileAidClient.java          ← ClientModInitializer entrypoint
├── trajectory/
│   ├── ProjectileInfo.java           ← record(speed, gravity, drag)
│   ├── ProjectileHelper.java         ← ItemStack → ProjectileInfo mapping
│   └── TrajectorySimulator.java      ← physics loop + collision detection
└── render/
    └── TrajectoryRenderer.java       ← WorldRenderEvents.LAST → line drawing
```

---

## Key Mojang-Mapped Classes (1.21.x)

| Purpose                  | Class                                                           |
|--------------------------|-----------------------------------------------------------------|
| Minecraft singleton      | `net.minecraft.client.Minecraft`                               |
| Client player            | `net.minecraft.client.player.LocalPlayer`                      |
| Client world             | `net.minecraft.client.multiplayer.ClientLevel`                 |
| 3D vector                | `net.minecraft.world.phys.Vec3`                                |
| Ray cast context         | `net.minecraft.world.level.ClipContext`                        |
| Hit result               | `net.minecraft.world.phys.HitResult`                           |
| Data components          | `net.minecraft.core.component.DataComponents`                  |
| Charged projectiles      | `net.minecraft.world.item.component.ChargedProjectiles`        |
| Tesselator               | `com.mojang.blaze3d.vertex.Tesselator`                         |
| Buffer builder           | `com.mojang.blaze3d.vertex.BufferBuilder`                      |
| Mesh upload              | `com.mojang.blaze3d.vertex.BufferUploader`                     |
| Vertex formats           | `com.mojang.blaze3d.vertex.DefaultVertexFormat`                |
| Render system            | `com.mojang.blaze3d.systems.RenderSystem`                      |
| Built-in shaders (1.21+) | `net.minecraft.client.renderer.CoreShaders`                    |
| Fabric render events     | `net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents`|

---

## Physics Parameters

| Item                    | Speed (b/t) | Gravity | Drag  |
|-------------------------|-------------|---------|-------|
| Bow (full charge)       | 3.0         | 0.05    | 0.99  |
| Crossbow (arrow)        | 3.15        | 0.05    | 0.99  |
| Crossbow (firework)     | 1.6         | 0.0     | 0.95  |
| Snowball / Egg / Pearl  | 1.5         | 0.03    | 0.99  |
| Splash / Lingering pot. | 0.5         | 0.03    | 0.99  |
| Experience bottle       | 0.7         | 0.03    | 0.99  |
| Trident                 | 2.5         | 0.05    | 0.99  |

Physics order per tick: `vel *= drag`, `vel.y -= gravity`, `pos += vel`

---

## Rendering Notes

- Hook: `WorldRenderEvents.LAST`
- Shader: `CoreShaders.POSITION_COLOR` (post-1.20.5 API; replaces `GameRenderer::getPositionColorShader`)
- Vertex format: `DefaultVertexFormat.POSITION_COLOR` with `VertexFormat.Mode.DEBUG_LINES`
- Vertices submitted as pairs (each segment = 2 vertices)
- Camera-relative coordinates: subtract `context.camera().getPosition()` from world coords
- `RenderSystem.disableDepthTest()` makes the trail visible through walls
- Alpha fades 85 % → 20 % along the trail length

---

## Building & Running

```bash
# Build the mod JAR
./gradlew build

# Launch the game with the mod loaded
./gradlew runClient

# Output JAR location
build/libs/projectile-aid-1.0.0.jar
```
