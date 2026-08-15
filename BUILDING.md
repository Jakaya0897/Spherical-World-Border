# Building Spherical World Border

## Requirements

- Java Development Kit 21
- Gradle 9.2.1 recommended

Verify Java:

```bash
java -version
```

Build:

```bash
gradle clean build
```

Output:

```text
build/libs/spherical-world-border-0.2.0-alpha.1-neoforge-1.21.1.jar
```

## IDE

Import the repository as a Gradle project. ModDevGradle will download the Minecraft/NeoForge development dependencies during the first sync.

## Why no compiled JAR in the repository?

Compiled release binaries should normally be attached to GitHub Releases. Keeping the repository source-only makes version history cleaner and avoids committing build output.
