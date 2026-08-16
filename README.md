# Spherical World Border 0.2.3-alpha.1

NeoForge 1.21.1 source package reconstructed from the supplied 0.2.1-alpha.1 build and updated with the safe-teleport repair.

## Safe teleport repair

- Ordinary walking, stepping, jumping and falling now require a real supported landing instead of relying only on `onGround` from the source tick.
- Water immediately below the destination is accepted as a safe non-solid landing medium.
- Elytra/player flight may preserve altitude; water crossings preferentially preserve nearby safe water at a similar depth.
- Bedrock is explicitly rejected as a safe landing surface.
- Successful pole and longitude crossings are pushed 2 blocks into the destination side to prevent shallow-angle re-trigger jitter.
- Existing collision and lava checks remain in place.
- If no safe transformed destination can be found, the existing fallback search is used and the crossing fails closed if that also fails.

## Version

`0.2.3-alpha.1`

## Target

- Minecraft 1.21.1
- NeoForge 21.1.248
- Java 21

Build with Gradle using `gradle clean build`.
