# 0.2.2-alpha.1
- Fixed NeoForge 1.21.1 runtime linkage for Elytra detection by calling `isFallFlying()` on `LivingEntity` instead of base `Entity`.

- Fixed unsafe destination validation that could accept unsupported air as a valid landing position.
- Normal walking/jumping crossings now require real support.
- Added safe-water landing support.
- Preserved altitude for intentional flight/Elytra/swimming.
- Bedrock is no longer accepted as a safe landing surface.
- Updated internal mod version and manifest to 0.2.2-alpha.1.

## Safe teleport refinement
- Water crossings now preserve water as the destination medium where possible instead of merely preserving Y level.
- Added a nearby-water search around obstructed mirrored destinations such as icebergs.
- Surface fallback now starts slightly above the local heightmap for additional clearance.
- Removed downward cave fallback for ordinary safe teleports; final fallback searches upward only.
- Added nearby-column surface search when the exact mirrored column is obstructed or hazardous.
