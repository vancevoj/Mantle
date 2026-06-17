# Mantle NeoForge 1.21.1 port - status

This is the **Mantle** half of the NeoForge 1.21.1 Tinkers' Construct port. Mantle
is TConstruct's hard dependency and must compile first. The primary status doc and
roadmap live in the sibling repo
[`vancevoj/NeoTinkersConstruct`](https://github.com/vancevoj/NeoTinkersConstruct)
(`PORTING_STATUS.md`, `PORTING.md`). Migration patterns:
[`docs/forge-to-neoforge.md`](docs/forge-to-neoforge.md).

Target: **Minecraft 1.21.1 / NeoForge 21.1.233 / Java 21 / Parchment 2024.11.17.**

## Build environment

Run Gradle on JDK 21 (the box default `java` is 25, unsupported by Gradle 8.10.2;
and only `openjdk-21-jdk` provides a `javac`):
```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
./gradlew <task>
```

## Done
- Build system converted to ModDevGradle (NeoForge 21.1.233, Java 21, Parchment
  2024.11.17), `neoforge.mods.toml`, gradle wrapper 8.10.2, upstream sync tooling.
- Toolchain verified: `./gradlew createMinecraftArtifacts` succeeds.
- Mechanical Forge->NeoForge import pass applied (`scripts/migrate-forge-imports.sh`):
  452 `net.minecraftforge` references -> 0.

## Next
`./gradlew compileJava -I /tmp/maxerrs.init.gradle` reports **~1630 errors**, all
genuine reworks. Order: `util`/`registration` -> `network` -> `fluid` ->
capabilities -> `data`/`recipe`/`loot` -> `client` -> `command`/`config`/`plugin`.
See [`docs/forge-to-neoforge.md`](docs/forge-to-neoforge.md) for before/after
snippets of each rework (registration, capabilities, networking, fluids, codecs,
data components).

Milestone: `./gradlew build` green, then a `runServer` smoke test. Once Mantle
publishes, TConstruct's composite build picks it up automatically.
