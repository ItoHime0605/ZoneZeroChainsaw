# Zone Zero Chainsaw — 0.1.1-alpha

Minecraft 1.21.1 / NeoForge 21.1.x / Java 21.

Early development source scaffold, **not yet a playable mod**.

## Implemented in source
- NeoForge entrypoint and registry declarations: chainsaw, fermented crude, biofuel bottle, fermentation barrel, fuel refinery.
- Traditional Chinese and English item/block labels.
- Per-item fuel helpers (0–1,800 capacity, 600 refill) and independent pure-Java state logic.
- 4-tick active-cut fuel consumption, no idle consumption, 40-tick inertial coast calculation.
- Pure-Java processing rules: fermentation 600 ticks, refining 200 ticks consuming 6 crude.
- Standalone logic smoke test.

## Not implemented
- Actual processing block entities, inventory GUIs, and recipes.
- R-key input/networking, actual attack-hold detection, dedicated damage/knockback logic, integration with other damage systems.
- GeckoLib model, chain animation, sound effects, first/third person visuals.
- Final fueling interaction and synchronization.

## Locked gameplay rules
- Organic vegetables/fruits/rotten flesh -> one fermented crude each 600 ticks.
- Six fermented crude -> one biofuel bottle (planned 200-tick refinement).
- Each bottle adds up to 600 cutting units; chainsaw maximum 1,800.
- Engine idle consumes no fuel.
- Holding primary attack while running consumes one unit every four ticks, including misses.
- Actual contacts should deal 2 damage every four ticks, ignoring vanilla hurt invulnerability without changing global invulnerability for other sources, plus extremely light knockback.
- Turning off should coast chain rotation down to zero over 40 ticks (2 seconds); no damage or fuel use in coast phase.

## Build
GitHub Actions uses Java 21 and Gradle 8.10.2 to attempt a NeoForge build. Read the workflow result before treating any JAR as usable.

Local: `gradle build` with Java 21 and Gradle 8.10.2.

Pure-Java smoke test:
```sh
javac -d build/logic-test src/main/java/tw/zonezero/chainsaw/logic/*.java src/test/java/tw/zonezero/chainsaw/logic/*.java
java -ea -cp build/logic-test tw.zonezero.chainsaw.logic.LogicSmokeTest
```
