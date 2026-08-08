# 🌌 Glowberry
> **The Ultimate Utility Addon for Cactus Mod**

Glowberry is a comprehensive expansion toolkit built exclusively for the [Cactus Mod](https://modrinth.com/mod/cactus) ecosystem. It injects high-performance utility modules, modern HUD enhancements, and direct quality-of-life upgrades into your game.

[![Platform: Fabric](https://img.shields.io/badge/Platform-Fabric-black?style=for-the-badge&logo=fabric&logoColor=white)](https://fabricmc.net/)
[![Requires: Fabric API](https://img.shields.io/badge/Requires-Fabric_API-blue?style=for-the-badge&logo=fabric&logoColor=white)](https://modrinth.com/mod/fabric-api)

---

## 📅 Version Matrix
### Minecraft `26.2`
* **Glowberry Version:** `1.6.0`+
* **Required Cactus:** `1.14`+
* **Download:** [Get Latest Release (.jar)](https://cdn.modrinth.com/data/Lnf97vJG/versions/ofDXConU/glowberry-1.6.0.jar?mr_download_reason=standalone)

### Minecraft `26.1.x`
* **Glowberry Version:** `1.5.2`+
* **Required Cactus:** `0.13`+
* **Download:** [Get Latest Release (.jar)](https://cdn.modrinth.com/data/Lnf97vJG/versions/4F8rdsAd/glowberry-1.5.2.jar?mr_download_reason=standalone)

### Minecraft `1.21.11`
* **Glowberry Version:** `1.4.1`+
* **Required Cactus:** `0.12.2`+
* **Download:** [Get Legacy Release (.jar)](https://cdn.modrinth.com/data/Lnf97vJG/versions/fSkHiLZU/glowberry-1.4.1.jar?mr_download_reason=standalone)

> [!IMPORTANT]
> **Overlay Lib Dependency:** Required *only* if you are running legacy version `1.0`. Version `1.1` and above is entirely standalone.

---

## 🛠️ Feature Modules

### 📺 Interface & HUD
* **`Pickup Log`** — A sleek, real-time visual feed showing exactly what items enter or leave your inventory.
* **`Timer / Stopwatch`** — Pins precise time-tracking and countdown utilities directly onto your primary screen overlay.
* **`ArmorHud+`** - ArmorHud 
* **`HorseStats`** — Reads hidden entity data to show raw jump heights, speed scales, and exact health values.
* **`Waila / Target Info`** — Displays real-time block and entity info directly on your screen.

### ⚔️ Tactical Combat (PvP)
* **`AutoClicker`** — Simulates raw mouse hardware input to deliver flawless, customizable click rates.
* **`NoHurtCam`** — Strips away aggressive screen-shake and blood-red tints to keep your vision clear under fire.
* **`ShieldStatus`** — A live overlay tracking enemy shield durability, active blocks, and cooldown windows.
* **`TotemCounter`** — Tracks your internal storage and prints exact enemy Totem pops straight into the player list.
* **`Trajectory`** `⚠️ Server Unsafe` `✨Up to 1.6.0` — Draws real-time physics vectors for arrows, snowballs, and eggs.

### 🌲 Survival Mechanics
* **`AppleSkin`** `✨Up to 1.6.0` — Integrates saturation, hunger depletion, and exhaustion statistics directly into your hotbar graphics.
* **`AutoTool`** — Evaluates blocks instantly and hot-swaps your slot to the mathematically optimal tool.
* **`AutoFish`** `⚠️ Server Unsafe` — Automates the full cast-and-reel cycle using precise audio/packet cues.
* **`FastPlace`** `⚠️ Server Unsafe` — Completely removes the internal tick delay for placing blocks.
* **`LightLevel`** — Projects a visual grid across blocks to map out potential hostile mob spawning spots.
* **`Shuffle`** — Dynamically scrambles blocks across your active hotbar to build realistic, random texture patterns.
* **`TabList`** — Upgrades the classic server list by displaying raw, exact numerical ping updates.

### 💡 Utility & Sandbox
* **`BookEdit`** — Unlocks full rich-text formatting, allowing standard color codes (`&` / `§`) right inside writable books.
* **`FastBreak`** — Fully eliminates the vanilla block reset timer for instantaneous breaking while in Creative Mode.

### 💻 Client Commands
* **`Calc / Calculator`** `✨ 1.5.1+` — Runs an advanced internal math compiler directly through your chat box. Supports variables like `pi`, `e`, and `ans` (previous answer), item-to-stack math breakdowns (`stack`, `shulker`), coordinate conversion using a `Current` location shortcut, and full operation history tracking.
* **`PrivateShare / Share`** `✨ 1.5.1+` — Allows you to safely broadcast your exact location data to other targeted players on the server.

---

## ⚡ Core Engine Upgrades

### 🔄 Reworked Macro Matrix
We scrapped the original macro framework and rebuilt it with a versatile input handler.
* **Multi-Format Input:** Bind text strings, vanilla `/` commands, or native Cactus `#` instructions seamlessly to a single key.
* **String Event Triggers:** Automate your workflow. Program specific macros to fire instantly the split-second a matching word or phrase is detected in game chat.

### 🔍 Built-in Placeholder Browser
No more documentation digging. Access an integrated, searchable glossary containing every active macro key.
* **Smart Categorization:** Instantly sort through Player, World, System, and Server variables.
* **Copy on Click:** Selecting any variable copies its precise `{key}` identifier straight to your clipboard for immediate use.

---

## 💬 Connect & Report

Found an edge-case bug or want to suggest a new utility module? Let us know:

🌌 [**GitHub Issue Tracker**](https://github.com/M9MX/Glowberry/issues)  
💬 Connect with me on Discord: [**Open Profile Link**](https://discord.com/users/821825926944784477) or add me to friends (**Username:** `m9mx`)

---
<div align="center">
<small>Glowberry is an independent addon. By installation, you remain bound to the overarching Cactus Terms of Service.</small>
</div>
