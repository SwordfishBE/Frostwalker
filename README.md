# Frostwalker

## Features

- **Frost Walker while riding:** enchanted boots worn by the rider generate frosted ice beneath horses, camels, pigs, donkeys, mules, skeleton horses, zombie horses, camel husks, llamas and trader llamas. Striders, boats and other vehicles are excluded.
- **Still Ice:** generate and refresh frosted ice even while standing still.
- **Toggle:** use `/frostwalker`, `/frostwalker toggle`, `/frostwalker on`, or `/frostwalker off`. No operator permission is required; commands affect only the executing player. The state defaults to enabled and persists across reconnects, server restarts, dimension changes and death.

Only equipped boots count. The wearer or mount must be on the ground for ice generation.
Ice forms from exposed water source blocks; the radius is 3 blocks for Frost Walker I and 4 for Frost Walker II.
Ice retains vanilla melting behavior after you leave. Toggling off stops new ice; existing ice melts normally.

## Installation

Place `frostwalker-<version>` mod Fabric API in the Fabric dedicated server's `mods/` directory.
Players can connect with vanilla Minecraft; no client mod or resource pack is required.
The mod is marked as server-only and does not load when installed only on a client.

## Build and verification

Run `./gradlew build` (Windows: `.\gradlew.bat build`) with Java 25.
The installable jar is in `build/libs`; do not install the `-sources.jar`.
`build` includes configuration and text-rendering regression verification.

## License

AGPL-3.0; see [LICENSE](LICENSE). Copyright (c) 2026 SwordfishBE.