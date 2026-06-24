# Kraft

Kraft is a lightweight 3D voxel game engine built from scratch using **Kotlin** and **LibGDX**. 

---

## Features

*   **Procedural World**: Generates terrain heights dynamically using 2D Perlin Noise.
*   **Free Look Camera**: Look around naturally using mouse movement (no button holding required).
*   **Block Interactions**: Left-click to destroy blocks, right-click to place stone blocks.
*   **HUD Elements**: Includes a selection outline around the targeted block and a central crosshair.
*   **Decoupled Physics**: Inputs are handled by a dedicated controller, keeping physics and rendering logic independent.

---

## Controls

*   `W`, `A`, `S`, `D` — Move around
*   `Space` — Jump
*   `Mouse Move` — Rotate camera
*   `Left Click` — Destroy block
*   `Right Click` — Place block (Stone)
*   `Escape` — Toggle cursor capture

---

## Running the Project

Ensure you have **Java 21** installed, then execute:

```bash
./gradlew run
```
