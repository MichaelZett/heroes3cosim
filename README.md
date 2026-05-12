# heroes3-combat-simulator

Ein **Auto-Solver für taktische Kämpfe aus *Heroes of Might and Magic III***.
Du wählst zwei Truppen, der Server simuliert den Kampf mit der gleichen
Logik wie das Spiel und liefert Ergebnis plus vollständiges Replay zurück.
Kein Spielerinput während des Kampfs — deterministische Engine über einen
injizierten Zufallsgenerator, gleicher Seed liefert identischen Ablauf.

Endziel sind belastbare Truppen-, Helden- und Fraktionsvergleiche per
Monte-Carlo: welche Einheit ist gold-effizient, welche Fraktion gewinnt
im Schnitt, wann zahlt sich welche Spezial-Synergie aus.

## Was drin ist

- **Vollständiger SoD-Kreaturenkatalog** — alle 126 Einheiten der neun
  Fraktionen (Castle, Rampart, Tower, Inferno, Necropolis, Dungeon,
  Stronghold, Fortress, Conflux) inklusive Upgrade-Varianten, Stats aus
  dem RoE-Manual.
- **Engine-evaluierte Spezialfähigkeiten** unter anderem No Retaliation,
  Two Blows / Two Shots, Good Morale, Death Stare, Thunderbolts,
  Petryfying (Medusa/Scorpicore), Cursing, Poisonous, Diseases, Aging,
  Death Blow, Angel/Devil/Titan Hate, Impact Damage (Cavalier-Jousting),
  Defense Reduction (Behemoth), Move Back (Harpy), Counterstrike
  Twice/Unlimited (Griffin), Regeneration (Wight), Fire Shield (Efreet
  Sultan), Rebirth (Phoenix), No Hand-to-Hand Penalty.
- **Hex-Schlachtfeld 15 × 11** mit zufälligen Hindernissen pro Sim,
  Hex-A*-Pathfinding um Obstacles herum, zugbasierte Move-Sequenz pro
  Hex (Token wandert sichtbar Schritt für Schritt).
- **Fernkampf** mit `shotsRemaining`, Nahkampf-Penalty bei Distanz 1,
  ½-Schaden ab >10 Hex Distanz und nochmal ½ wenn ein Hindernis in der
  Schusslinie liegt — beide negierbar durch entsprechende Marker.
- **HTTP-API** über Spring Boot 4 mit
  [Swagger-UI](http://localhost:8080/swagger-ui.html): `GET /api/units`,
  `GET /api/factions`, `POST /api/battles/simulate` für einzelne Kämpfe,
  `POST /api/experiments/matrix` für Matrix-Auswertungen. Antworten
  enthalten Ergebnis plus strukturierten `BattleEvent`-Stream (sealed
  Interface, Jackson-polymorph serialisiert).
- **Matrix-Auswertung** — jede Einheit gegen jede andere, pro Match-up
  mehrere Seeds und automatisch getauschte Attacker-/Defender-Rollen.
  Parallel über Work-Stealing-Pool; Report listet Win-Rate, mittlere
  Überlebensquote und „Anomalien" (Truppen, die mehrheitlich gegen die
  nächstniedrigere Tier-Klasse verlieren).
- **Replay-UI** — React 19 + Vite + Tailwind. Truppenkonfiguration mit
  Seed-Wahl, Hex-Replay mit Step/Pause/Geschwindigkeitsslider, Rückspiel-
  Button (gleicher Seed, getauschte Seiten), scrollender Combat-Log.
  Dazu eine eigene Matrix-Seite mit Faction-/Unit-Excludes und
  sortierbarer Ergebnis-Tabelle. Deutsch und Englisch umschaltbar.
- **Deterministisch über Seed**: gleicher Seed → identische
  `BattleResult` + identischer Event-Stream + identische Obstacle-
  Verteilung. Voraussetzung für reproduzierbare Auswertungen.

## Schnellstart

Voraussetzung: JDK 25 auf dem PATH (sonst lädt die Gradle-Toolchain
nach), Node ist nicht erforderlich — Gradle ruft den Frontend-Build
selbst auf.

```pwsh
.\gradlew.bat bootRun
```

Spring Boot lauscht auf `http://localhost:8080`, liefert die fertige
React-UI unter `/`, die REST-API unter `/api/**`, Swagger-UI unter
`/swagger-ui.html`. Beim ersten Lauf installiert Gradle die
Node-Dependencies und baut das Frontend.

CLI-Demo ohne UI (Grand Elf vs. Arch Angel mit deutschem Combat-Log auf
der Konsole):

```pwsh
.\gradlew.bat bootRun --args='--spring.profiles.active=cli'
```

## Entwickeln

```pwsh
.\gradlew.bat build              # Java-Build + Tests + Coverage-Gate + SpotBugs
.\gradlew.bat test               # nur Java-Tests
cd frontend; npm run dev         # Vite-Dev-Server auf :5173 mit /api-Proxy auf :8080
cd frontend; npm test            # Vitest + Testing Library + MSW
.\gradlew.bat dependencyUpdates  # Versions-Check
```

Backend-Hot-Reload im Dev-Modus: `ComSimApp` als Spring-Boot-Run-Config
starten, separat im Frontend-Verzeichnis `npm run dev` — Vite reicht
`/api/*` an den Spring-Server durch, keine CORS-Konfiguration nötig.

Reports landen unter `build/reports/`: JaCoCo, SpotBugs, Tests.

## Stack

- **Engine**: Java 25, Spring Boot 4, ErrorProne + NullAway, SpotBugs,
  JaCoCo (Coverage-Gate Line ≥ 80 % / Branch ≥ 60 % erzwungen).
- **API-Schicht**: Spring MVC, Validation per `jakarta.validation`,
  springdoc-openapi für OpenAPI/Swagger.
- **Frontend**: React 19, Vite 8, TypeScript 6, Tailwind 4,
  TanStack Query, Zustand, React Router 7, react-i18next.
- **Tests**: JUnit 5 + AssertJ + Spring-Boot-Test (Backend);
  Vitest + Testing Library + MSW (Frontend).
- **CI**: GitHub Actions auf `push`/`pull_request` gegen `master`,
  JDK 25 Temurin, `gradlew build spotbugsMain spotbugsTest
  jacocoTestReport`.

## Architektur

Hexagonal-light:

- **`domain`** — pure Combat-Domäne ohne Spring-Abhängigkeit. `Unit`,
  `UnitCatalog`, `Stack`, `Hex`, `Battlefield`, `PathFinder`,
  `ObstacleGenerator`, Enums + Event-Records unter `domain.events`.
- **`application`** — `Battle.simulate(...)` als Orchestrator,
  `BattleSetup`, `BattleResult`, `AutoSolver` (Default
  `GreedyAutoSolver`). Unter `application.experiment` der parallele
  `MatrixExperimentService`.
- **`adapter.web`** — `BattleController`, `ExperimentController`,
  DTOs, CORS-Config für den Vite-Dev-Proxy.
- **`frontend/`** — eigenständiges Vite-Projekt, vom Gradle-Build als
  statisches Resource gebündelt. Feature-Buckets: `battle-config`,
  `battle-replay`, `matrix-experiment`.

## Was als nächstes kommt

- **Mixed Armies** — mehrere Stacks pro Seite mit gemeinsamer
  Initiative, Voraussetzung für AoE-Effekte (Lich Death Cloud) und
  echte Truppenkombinationen.
- **Helden** mit Primärwerten, Sekundärfertigkeiten und Zauberbuch.
- **Belagerung**: Mauern, Catapult, Wall-Penalty für Schützen.
- **Gold-Effizienz-Modus** für die Matrix: Stack-Größe aus Gold-Budget
  ableiten, nicht aus festem Count.

## Quellen

- Stat-Werte aus dem RoE-Manual (`files/heroes3_manual.pdf`,
  Text-Layer-Extrakt unter `files/h3_manual.txt`).
- Spezialfähigkeiten: https://heroes.thelazy.net/index.php/Special_ability
