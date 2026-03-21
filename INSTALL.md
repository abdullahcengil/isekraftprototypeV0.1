# IseKraft Installation Guide

## What you need

- **Minecraft 1.20.1** (Java Edition)
- **Java 17** — [Download here](https://adoptium.net) (click "Latest LTS Release")
- **Fabric Loader** — [Download here](https://fabricmc.net/use/installer/)
- **Fabric API** — [Download here](https://modrinth.com/mod/fabric-api) (pick the 1.20.1 version)

---

## Option A — Install the pre-built jar (easiest)

1. Download `isekraft-1.1.0.jar` from the [Releases page](../../releases)
2. Download `fabric-api-xxxx+1.20.1.jar` from [Fabric API](https://modrinth.com/mod/fabric-api)
3. Put both jars into your mods folder:
   - Press `Win + R`, type `%appdata%\.minecraft\mods`, press Enter
   - That opens the mods folder directly
4. Launch Minecraft with the **Fabric 1.20.1** profile
5. Done

---

## Option B — Build from source (Windows)

1. Install **Java 17** from [adoptium.net](https://adoptium.net)
2. Download or clone this repository
3. Open the `isekraft` folder
4. Double-click **`SETUP_WINDOWS.bat`**
   - First run downloads ~200MB of build tools and Minecraft libraries
   - Takes 5–10 minutes on first run
   - The jar gets copied to your mods folder automatically
5. Download Fabric API and put it in your mods folder
6. Launch Minecraft with the Fabric 1.20.1 profile

---

## Verifying it works

When you launch Minecraft and create a new world, you should see:
- A welcome message in chat explaining the mod
- A Recipe Guide book in your inventory
- The `✦ IseKraft RPG` tab in the creative menu

If the game crashes on launch, make sure **Fabric API** is in your mods folder — the mod won't run without it.

---

## Recommended settings

- Render Distance: **8–12** chunks (higher values can cause lag with castle generation)
- RAM: **3–4 GB** (set in your launcher's JVM arguments: `-Xmx4G`)

---

## Optional mods that work well with IseKraft

- [JEI (Just Enough Items)](https://modrinth.com/mod/jei) — browse all IseKraft recipes in-game
- [Sodium](https://modrinth.com/mod/sodium) — better FPS

---

## Uninstalling

Remove `isekraft-1.1.0.jar` from your mods folder. Your worlds will be fine, but any IseKraft items in them will disappear.
