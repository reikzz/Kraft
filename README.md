# Kraft

Kraft is a lightweight 3D voxel sandbox built with **Kotlin** and **LibGDX**. 

---

## Features

*   **Procedural World**: Generates terrain heights dynamically using 2D Perlin Noise.
*   **Multiplayer Support**: Experimental local/network multiplayer using KryoNet.
*   **Free Look Camera**: Look around naturally using mouse movement.
*   **Block Interactions**: Left-click to destroy blocks, right-click to place blocks.
*   **HUD & Pause Menu**: Features a crosshair, selection outline, and a pause menu.

---

## Controls

*   `W`, `A`, `S`, `D` — Move around
*   `Space` — Jump
*   `Mouse Move` — Rotate camera
*   `Left Click` — Destroy block
*   `Right Click` — Place block (Stone)
*   `Escape` — Pause menu (Resume, Quit)

---

## Running the Project

Ensure you have **Java 21** installed, then execute:

```bash
# Run the client (Local/Singleplayer mode)
./gradlew run
```

*Note: Dedicated server launch instructions will be added as multiplayer matures.*
