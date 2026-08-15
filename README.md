# Spherical World Border

**Spherical World Border** is a NeoForge mod for Minecraft 1.21.1 that simulates globe-like travel while keeping ordinary Minecraft world and chunk coordinates.

Instead of replacing Minecraft's coordinate system, the mod performs controlled boundary transformations:

- Crossing the east or west longitude seam wraps you to the opposite longitude edge.
- Crossing the North or South Pole reflects your latitude back into the same polar region and shifts longitude by 180°.
- North/south facing and Z momentum are reflected at a pole.
- Optional HUD warnings identify the boundary being approached.
- Optional particle shimmer makes the crossing line visible without altering terrain or chunk rendering.


> Alpha software. Back up important worlds before testing.

## Planet geometry

The mod is driven by one value: `circumference`.

For circumference `C`:

| Feature | Coordinate |
|---|---:|
| West longitude seam | `X = -C / 2` |
| East longitude seam | `X = +C / 2` |
| North Pole | `Z = -C / 4` |
| South Pole | `Z = +C / 4` |
| 180° longitude shift at a pole | `C / 2` blocks |

The default circumference is `1,000,000` blocks, giving:

- West/East seams at `X ±500,000`
- North/South poles at `Z ±250,000`

### Natural Temperature compatibility

For the default linear Earth-style layout used during development, the intended relationship is:

```text
Natural Temperature equatorial_distance = circumference / 8
```

For the default `1,000,000` block circumference this is `125,000` blocks.

The mod does **not** depend on Natural Temperature; this is simply a fantastic relationship when the two mods are used together.

## Configuration

The config is generated at:

Main options:

| Setting | Default | Purpose |
|---|---:|---|
| `enabled` | `true` | Master switch |
| `overworldOnly` | `true` | Restrict spherical behaviour to the Overworld |
| `circumference` | `1000000` | Full planet circumference in blocks |
| `wrapLongitude` | `true` | Enable east/west wrapping |
| `crossPoles` | `true` | Enable spherical pole crossing |
| `showBorderWarnings` | `true` | Show boundary approach HUD text |
| `warningDistance` | `250` | HUD warning range in blocks |
| `showVisibleBorders` | `true` | Show the particle boundary shimmer |
| `visibleBorderDistance` | `96` | Distance at which shimmer becomes visible |
| `teleportNonPlayers` | `true` | Apply transformations to root entities as well as players |
| `excludeCreateEntities` | `true` | Safety exclusion for Create train/contraption classes |

## Known alpha limitations

- Create trains and contraptions are excluded from generic boundary teleporting by default. Their railway/contraption state needs dedicated compatibility testing rather than a blind entity teleport.
- The visible boundary is cosmetic and intentionally uses ordinary client-side particles rather than a custom world renderer.
- This mod simulates spherical travel topology; it does not curve terrain or render an actual spherical planet.

## Building

Requirements:

- JDK 21
- Gradle 9.2.1 recommended
- Internet access for the first dependency download

Build with:

```bash
gradle clean build
```

The release JAR will be written to `build/libs/`.

This project uses NeoForge's ModDevGradle development plugin and targets NeoForge `21.1.248`.

## Installing

Put the built JAR in the Minecraft instance's `mods` folder and launch NeoForge 1.21.1.

## Releases

For public distribution, attach compiled JARs to GitHub **Releases** rather than committing them into the source tree.
