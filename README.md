# PecuniaMC

PecuniaMC is the ultimate, 100% free and open-source Minecraft PvP client for version 1.8.9. Developed by MrPecunia, it is fully optimized to deliver a massive FPS boost and seamless gameplay performance.

Built with a strict focus on privacy: **zero telemetry or tracking**.

## Features

- **1.7 animations** (OptiFine-safe Mixins) and FPS-oriented optimizations
- **Lunar-style menus**: custom splash, main menu, Info and network Status screens
- **Pecunia Network**: synced custom ranks, nicknames and capes between PecuniaMC players (`/setrank`, `/setnick`)
- **Content mode** (`/contentmode`): fake `/ban` with customizable message (`/customban`), fake `/gamemode 1` / `/gmc` creative with fly — everything client-side, nothing reaches the server
- **Danger mode** (hidden): embedded Arsenic modules with a dedicated ModMenu section and ClickGUI
- Custom splash screen, capes support, Hypixel sidebar/tab/nametag rank display

Type `/pmc help` in game for the full command list.

## Build

Requirements: **JDK 8** (Gradle 4.4.1 + ForgeGradle 2.1 do not run on newer Java).

```bash
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
./gradlew setupDecompWorkspace   # once (~30 min: downloads + decompile)
./gradlew build                  # jar in build/libs/pecuniamc-1.0.0.jar
```

Copy the jar into `.minecraft/mods/` (Forge 1.8.9-11.15.1.2318).

## Credits

- MrPecunia, Webget, Diamond Gaming, Eris Group, Pecunia Enterprise
- Based on CloudClient by DupliCAT / FrostClient by QuiteFrosty (LGPL-3.0)
- Embedded Arsenic Client v2.0 (MIT)
- Rank logic inspired by RankSpoof (cxntered)

## License

GPL-3.0 — see [LICENSE](LICENSE) and [THIRD-PARTY.txt](THIRD-PARTY.txt).
Holders: PrPecunia, Webget, Diamond Gaming, Eris Group, Pecunia Enterprise.
