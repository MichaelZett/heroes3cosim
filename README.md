# heroes3-combat-simulator

Auto-Solver für taktische Kämpfe aus *Heroes of Might and Magic III*.
Konfiguration eines Setups → deterministische Simulation → Ergebnis +
Event-Log. Kein menschlicher Spieler-Input während des Kampfs.

Das Endziel: Truppen-, Helden- und Fraktions-Vergleiche per Monte-Carlo
über viele Kämpfe entscheiden — *welche Einheit ist gold-effizient?
welche Fraktion gewinnt im Schnitt? welche Held-Spec-Kombi rocked?* Eine
HTTP-API plus React-Frontend ergänzen die Engine später.

## Tech-Stack

- **Java 25** (LTS), Gradle 9.2 (Groovy DSL).
- Plain Java aktuell — Spring Boot 4 wird ergänzt, sobald die HTTP-API
  beginnt.
- SLF4J + Logback, Guava, JSpecify.
- **Tests**: JUnit 5 + AssertJ.
- **Code-Qualität**: ErrorProne + NullAway (severity `WARN` während der
  Aufbauphase), SpotBugs (`ignoreFailures = true` initial), JaCoCo.

## Schnellstart

```pwsh
# Voraussetzung: JDK 25 auf dem PATH (oder Gradle Toolchain lädt nach).
.\gradlew.bat build       # Compile + Tests + JaCoCo + SpotBugs
.\gradlew.bat run         # Demo-Battle (Grand Elf vs Arch Angel)
.\gradlew.bat test        # nur Tests
.\gradlew.bat dependencyUpdates  # Library-/Plugin-Updates checken
```

Reports landen in `build/reports/`:
`jacoco/test/html/index.html`, `spotbugs/main.html`,
`tests/test/index.html`.

## Was schon funktioniert

- 59 H3-Einheiten im `UnitCatalog` (Castle, Rampart, Inferno teilweise,
  Dungeon teilweise, Tower teilweise, Stronghold teilweise — siehe
  BACKLOG für Vollständigkeit).
- 1-Stack-vs-1-Stack auf einem 15×11-Hex-Battlefield mit
  Bewegungsreichweite per Speed.
- Long-Range-Combat: Schützen schießen aus Distanz ohne Retaliation,
  schalten bei Distanz 1 in den Nahkampf-Modus mit
  `HAND_TO_HAND`-Penalty.
- Speciality-Subset: `NO_RETALIATION`, `TWO_BLOWS`, `GOOD_MORALE`,
  `DEATH_STARE`, `THUNDERBOLTS`, `PETRYFYING` (sic — H3-Schreibweise),
  `CURSING`, `DEATH_BLOW`, `ANGEL_HATE` / `DEVIL_HATE`,
  `NO_HAND_TO_HAND_PENALTY`. Restliche Specialities sind im Enum
  vorhanden, aber von der Engine noch nicht ausgewertet.
- Deterministische Simulation per `RandomGenerator`-Injection — gleicher
  Seed liefert identisches `BattleResult`. Voraussetzung für
  Monte-Carlo-Experimente und Replay.

## Was fehlt (Auswahl, vollständig in `BACKLOG.md`)

- Mixed Armies (mehrere Stacks pro Seite) plus Initiative über alle
  Stacks.
- Helden mit Primary-Stats, Secondary Skills, Spell Book.
- Spruchsystem (Mana, Wisdom-Gate, Combat-Spells).
- Distanz-/Obstacle-Penalty für Schützen, Belagerungen.
- Spring-Boot-API + OpenAPI-Generator → REST-Adapter im
  `adapter`-Paket.
- React-Frontend (Army-Builder, Replay-Viewer, Experiment-Dashboard).

## Projektstruktur

```
src/main/java/de/zettsystems/h3comsim/
├─ ComSimApp.java       — Demo-CLI-Entry
├─ domain/              — Unit (Record), UnitCatalog, Stack, Hex,
│                         Battlefield, Enums (AttackType, Movement,
│                         UnitSpeciality(Type))
├─ application/         — Battle, BattleSetup, BattleResult,
│                         BattleLogger, Action (sealed), AutoSolver,
│                         GreedyAutoSolver
└─ adapter/             — (geplant) OpenAPI-Spring-RestController
```

`AGENTS.md` beschreibt die Konventionen für Coding-Agenten,
`BACKLOG.md` führt forward-looking Items.
