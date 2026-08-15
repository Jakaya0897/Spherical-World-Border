# 🌍 Spherical World Border

**Spherical World Border** is a lightweight NeoForge mod that makes a normal Minecraft world behave more like the surface of a planet.

Instead of changing Minecraft's chunk system, world generation, or coordinate system, the mod simply controls what happens when a player or entity reaches the edge of the configured planet.

The result is **globe-like travel using normal Minecraft coordinates**.

> [!WARNING]
> This mod is currently **alpha software**. Back up important worlds before testing new releases.

---

## 🎯 What is the purpose of this mod?

Minecraft worlds are normally flat and effectively endless.

Spherical World Border lets modpack creators turn part of that world into a **finite planet-sized play area** while still allowing players to travel around it in a way that feels geographically logical.

It is designed for worlds where you may want:

- a fixed planetary circumference;
- east/west travel to wrap around the planet;
- proper North and South Pole crossings;
- normal Minecraft chunks and coordinates;
- compatibility with ordinary world generation;
- compatibility with LOD/rendering mods that expect normal coordinates;
- optional warnings and visible crossing boundaries;
- safer teleport destinations when crossing a planetary boundary.

The mod does **not** curve Minecraft terrain or render a literal sphere.

It simulates the **travel topology** of a globe while leaving the Minecraft world itself completely normal.

---

# 🧭 How does the wrapping work?

The simplest way to think about it is this:

## East / West

If you keep travelling east until you reach the eastern edge of the planet, you appear at the western edge at the **same latitude**.

```text
WEST                                                  EAST
  │                                                     │
  │ <────────────────── PLANET ───────────────────────> │
  │                                                     │
  └──── crossing west                      crossing east ┘
        sends you east                     sends you west
```

Example with the default `1,000,000` block circumference:

```text
X +500,000  →  X -500,000
X -500,000  →  X +500,000
```

Your north/south position does not change.

---

## North / South Poles

The poles work differently.

On a real globe, travelling north does **not** eventually take you to the South Pole. You reach the North Pole, cross over it, and then begin travelling south on the opposite side of the planet.

Spherical World Border recreates that behaviour.

```text
                    NORTH POLE
                        ●
                       / \
                      /   \
                     /     \
        approach    /       \    leave pole
        northward  /         \   southward
                  ●           ●
              longitude   opposite longitude
```

When you cross a pole, the mod:

1. keeps you at that same pole;
2. reflects you back inside the playable world;
3. moves you **180° around the planet** in longitude;
4. reverses your north/south direction and Z momentum.

So a North Pole crossing might look like:

```text
Before crossing:
X = +100,000
Z = -250,001

After crossing:
X = -400,000
Z = -249,999
```

You have crossed the North Pole and emerged on the **opposite longitude**, travelling south.

The South Pole works the same way in reverse.

---

# 📐 Planet geometry

The whole planet is controlled by one setting:

```toml
circumference = 1000000
```

Call the circumference `C`.

The mod automatically derives the important boundaries:

| Planet feature | Coordinate |
|---|---:|
| West longitude seam | `X = -C / 2` |
| East longitude seam | `X = +C / 2` |
| North Pole | `Z = -C / 4` |
| South Pole | `Z = +C / 4` |
| Longitude shift when crossing a pole | `C / 2` |

With the default:

```text
C = 1,000,000
```

the world becomes:

```text
                    NORTH POLE
                     Z -250,000
                         │
                         │
WEST                     │                     EAST
X -500,000 ──────────────┼────────────── X +500,000
                         │
                         │
                     Z +250,000
                    SOUTH POLE
```

This creates a **1,000,000 × 500,000 block planetary surface**.

For clean geometry, using a circumference divisible by `8` is recommended.

---

# 🌡️ Natural Temperature compatibility

Spherical World Border does **not** require Natural Temperature.

However, the two mods can be configured to line up neatly for an Earth-like climate layout and the original reason i made the mod.

For the layout used during development:

```text
Natural Temperature equatorial_distance = circumference / 8
```

With the default circumference:

```text
1,000,000 / 8 = 125,000
```

Example:

```toml
# Spherical World Border
circumference = 1000000
```

```toml
# Natural Temperature
equatorial_distance = 125000.0
looping_world = false
```

That places:

```text
North Pole     Z -250,000
Equator        Z 0
South Pole     Z +250,000
```

---

# ⚙️ Configuration


Below is every current option.

---

## Master settings

### `enabled`

```toml
enabled = true
```

Master switch for the mod.

- `true` — spherical boundary behaviour is active.
- `false` — the mod does nothing.

**Default:** `true`

---

### `overworldOnly`

```toml
overworldOnly = true
```

Controls whether the spherical planet system only operates in the Overworld.

- `true` — Nether, End, and other dimensions behave normally.
- `false` — eligible dimensions can also use the boundary logic.

For most modpacks, leaving this enabled is recommended.

**Default:** `true`

---

# 🌍 Planet size

### `circumference`

```toml
circumference = 1000000
```

The full circumference of the planet in blocks.

From this single number the mod calculates the longitude seams, poles, and 180° pole-crossing shift.

For example:

| Circumference | East / West | North / South |
|---:|---:|---:|
| `400000` | `X ±200000` | `Z ±100000` |
| `500000` | `X ±250000` | `Z ±125000` |
| `600000` | `X ±300000` | `Z ±150000` |
| `1000000` | `X ±500000` | `Z ±250000` |

**Default:** `1,000,000`  
**Allowed range:** `64` to `30,000,000`

A value divisible by `8` is recommended.

---

# ↔️ Longitude wrapping

### `wrapLongitude`

```toml
wrapLongitude = true
```

Controls normal east/west wrapping.

When enabled:

```text
east edge → west edge
west edge → east edge
```

Latitude, facing direction, and movement are preserved as naturally as possible.

**Default:** `true`

---

# 🧊 Pole crossing

### `crossPoles`

```toml
crossPoles = true
```

Enables spherical North and South Pole behaviour.

When crossing a pole, the mod:

- reflects the entity back inside the same polar boundary;
- moves it halfway around the planet in longitude;
- reverses north/south movement;
- adjusts facing so travel continues away from the pole.

This is what prevents the world from behaving like a simple north-to-south looping rectangle.

**Default:** `true`

---

# ⚠️ Border approach warnings

### `showBorderWarnings`

```toml
showBorderWarnings = true
```

Shows an on-screen warning when the player approaches a planetary boundary.

The warning identifies the boundary:

- **East Longitude Seam**
- **West Longitude Seam**
- **North Pole**
- **South Pole**

It also shows the remaining distance to the crossing.

**Default:** `true`

---

### `warningDistance`

```toml
warningDistance = 250
```

How close the player must be before the on-screen warning appears.

Measured in blocks.

**Default:** `250`  
**Allowed range:** `8` to `10,000`

---

# ✨ Visible border shimmer

### `showVisibleBorders`

```toml
showVisibleBorders = true
```

Displays a subtle particle curtain along the nearby crossing boundary.

This is only a visual effect.

It does **not**:

- create collision;
- change terrain;
- alter world generation;
- modify chunks;
- create fake coordinates.

**Default:** `true`

---

### `visibleBorderDistance`

```toml
visibleBorderDistance = 96
```

Controls how close the player must be before the visual border shimmer appears.

Lower values reduce particle activity.

**Default:** `96`  
**Allowed range:** `16` to `512`  
**Suggested range:** `48` to `128`

---

# 🛡️ Safe teleporting

### `safeTeleport`

```toml
safeTeleport = true
```

Enables destination safety checks when crossing a boundary.

Without a safety check, a mathematically correct destination could happen to be:

- inside a mountain;
- inside an iceberg;
- underground;
- in lava;
- on another dangerous surface.

When this option is enabled, the mod checks the destination before moving the entity.

It attempts to find enough collision-free space and avoids obvious vanilla hazards.

If no safe destination can be found, the crossing **fails safely** and the entity is kept just inside the original boundary rather than being teleported into danger.

**Default:** `true`

> [!NOTE]
> Safety system cannot automatically recognise every dangerous block added by every other mod.

---

### `safeTeleportSearchHeight`

```toml
safeTeleportSearchHeight = 128
```

Maximum number of blocks above the destination surface that the mod may search while trying to find a safe free position.

Higher values can help with extremely tall or irregular terrain but increase the amount of work performed during a difficult crossing.

**Default:** `128`  
**Allowed range:** `16` to `512`

---

# 🐎 Entity handling

### `teleportNonPlayers`

```toml
teleportNonPlayers = true
```

Controls whether the spherical boundary system also applies to non-player root entities.

When enabled, ordinary entities such as mobs and many vehicles can use the same planetary boundaries.

Passengers are handled through their root vehicle rather than being independently teleported.

- `true` — players and eligible root entities are handled.
- `false` — only players are handled.

**Default:** `true`

---

### `excludeCreateEntities`

```toml
excludeCreateEntities = true
```

Compatibility safety option for the **Create** mod.

Create trains and contraptions can contain additional track/contraption state that may not survive a generic entity teleport correctly.

When enabled, recognised Create train/contraption entity classes are ignored by Spherical World Border.

Keep this enabled unless dedicated compatibility has been tested.

**Default:** `true`

---

# ✅ Recommended default configuration

```toml
enabled = true
overworldOnly = true

circumference = 1000000

wrapLongitude = true
crossPoles = true

showBorderWarnings = true
warningDistance = 250

showVisibleBorders = true
visibleBorderDistance = 96

safeTeleport = true
safeTeleportSearchHeight = 128

teleportNonPlayers = true
excludeCreateEntities = true
```

---

# 🧩 Compatibility philosophy

Spherical World Border deliberately avoids replacing Minecraft's coordinate or chunk systems.

A crossing is effectively:

```text
normal Minecraft coordinate
        ↓
calculate destination
        ↓
safely move entity
        ↓
normal Minecraft coordinate
```

There are no endlessly repeated client coordinates, fake dimensions, duplicated chunks, or portal-based world copies.

This design is intended to make the mod easier to combine with:

- custom terrain generation;
- biome mods;
- climate mods;
- performance mods;
- LOD renderers;
- large modpacks.

Compatibility with every mod cannot be guaranteed, especially for mods that maintain complex state attached to moving entities.

---

# 🚧 Current alpha limitations

- Create trains and contraptions are excluded from generic teleporting by default and untested.
- The mod simulates spherical **travel**, not curved terrain.
- Modded hazards may not always be recognised by the safe teleport system.
- Large modpacks should still test entity and vehicle behaviour before relying on it in an important world.

---

# 📦 Installation

1. Install **Minecraft 1.21.1**.
2. Install a compatible **NeoForge 21.1.x** release.
3. Place the Spherical World Border JAR in the instance's `mods` folder.
4. Launch Minecraft.
5. The configuration file will be created automatically.

```text
config/sphericalworldborder-common.toml
```
---

# 🤝 Contributions

Bug reports, compatibility findings, and improvements are welcome.

If you modify the project with an improvement you would like included, please submit the change back to the official project for review.

---


---

## 🌎 In one sentence

**Spherical World Border turns a normal finite Minecraft map into a globe-like world where east meets west and crossing a pole sends you over the top of the planet instead of simply looping north into south.**
