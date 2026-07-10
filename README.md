# Windy: Configurable - NeoForge 1.21.1 fork source

This is an unofficial NeoForge 1.21.1 configurable fork/remake source tree of **Windy** by BonfireMC.

- Original project: https://modrinth.com/mod/windy
- Original source: https://github.com/BonfireMC/Windy
- Original license: LGPL-3.0-only

## Notes

- Client-side wind particle spawning remains lightweight and runs from the existing client animate-tick logic.
- JSON config file: `config/windy-config.json`.
- Config v2 adds constant or Y-level scaling wind density.
- Config v2 adds optional biome wind rules with exact biome IDs, biome tags using `#namespace:tag`, blacklist support, and cached biome multiplier resolution.
- Old v1 top-level configs are migrated into the new `constantWind` section on load.
- Intended for client-side use.

## Build

Use Java 21, then run:

```powershell
./gradlew build
```

The built jar should appear in `build/libs`.
