# heroes3-combat-simulator

Auto-Solver für taktische Kämpfe aus *Heroes of Might and Magic III*.
Konfiguration eines Setups, deterministische Simulation, Ausgabe sind
Ergebnis und Event-Log. Während des Kampfs gibt es keinen
Spieler-Input.

Endziel: Truppen-, Helden- und Fraktionsvergleiche per Monte-Carlo
über viele Kämpfe entscheiden — welche Einheit ist gold-effizient,
welche Fraktion gewinnt im Schnitt, welche Kombination aus Held und
Spezialisierung zahlt sich aus. Eine HTTP-Schnittstelle und ein
React-Frontend sollen die Engine später ergänzen.

## Stack

- Java 25, Gradle 9.2 (Groovy DSL).
- Aktuell reines Java; Spring Boot 4 kommt dazu, sobald die
  HTTP-Schnittstelle gebraucht wird.
- SLF4J + Logback, Guava, JSpecify.
- Tests: JUnit 5 + AssertJ.
- Statische Analyse: ErrorProne mit NullAway, SpotBugs, JaCoCo.

## Build und Ausführung

```pwsh
# Voraussetzung: JDK 25 auf dem PATH, sonst lädt die Gradle-Toolchain nach.
.\gradlew.bat build              # Übersetzen, Tests, JaCoCo, SpotBugs
.\gradlew.bat run                # Beispielkampf (Grand Elf vs Arch Angel)
.\gradlew.bat test               # nur Tests
.\gradlew.bat dependencyUpdates  # Versionen der Bibliotheken prüfen
```

Berichte landen unter `build/reports/`:
`jacoco/test/html/index.html`, `spotbugs/main.html`,
`tests/test/index.html`.

## Stand der Engine

- 59 Einheiten im `UnitCatalog`. Castle und Rampart sind vollständig,
  Inferno, Dungeon, Tower und Stronghold teilweise.
- Ein Stack gegen einen Stack auf einem 15×11-Hex-Feld.
  Bewegungsweite ergibt sich aus der Geschwindigkeit der Einheit.
- Fernkampf: Schützen feuern aus Distanz ohne Vergeltung. Auf
  Distanz 1 wechselt der Schütze in den Nahkampf mit
  `HAND_TO_HAND`-Malus.
- Umgesetzte Spezialfähigkeiten: `NO_RETALIATION`, `TWO_BLOWS`,
  `GOOD_MORALE`, `DEATH_STARE`, `THUNDERBOLTS`, `PETRYFYING` (sic,
  Heroes-3-Schreibweise), `CURSING`, `DEATH_BLOW`, `ANGEL_HATE`,
  `DEVIL_HATE`, `NO_HAND_TO_HAND_PENALTY`. Die übrigen Einträge im
  Enum sind angelegt, werden von der Engine aber noch nicht
  ausgewertet.
- Deterministische Simulation: Der `RandomGenerator` wird in `Battle`
  injiziert, gleicher Seed liefert identisches `BattleResult`.
  Grundlage für Monte-Carlo-Auswertungen und Wiederholungen.

## Offene Themen

- Mehrere Stacks pro Seite und Initiative über alle Stacks.
- Helden mit Primärwerten, Sekundärfertigkeiten, Zauberbuch.
- Zaubersystem (Mana, Wisdom-Schwelle, Kampfzauber).
- Distanz- und Hindernismalus für Schützen, Belagerungen.
- Spring-Boot-Schnittstelle samt OpenAPI-Generator im Paket
  `adapter`.
- React-Frontend (Armee-Editor, Wiederholungsanzeige,
  Auswertungs-Dashboard).

## Projektstruktur

```
src/main/java/de/zettsystems/h3comsim/
├─ ComSimApp.java     — Beispielkampf als CLI-Einstieg
├─ domain/            — Unit (Record), UnitCatalog, Stack, Hex,
│                       Battlefield, Enums (AttackType, Movement,
│                       UnitSpeciality, UnitSpecialityType)
└─ application/       — Battle, BattleSetup, BattleResult,
                        BattleLogger, Action (sealed), AutoSolver,
                        GreedyAutoSolver
```

Das Paket `adapter` ist für die HTTP-Schicht vorgesehen und aktuell
noch leer.
