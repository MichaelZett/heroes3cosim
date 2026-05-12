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
  Parallel über Work-Stealing-Pool (50 %-Auslastung der CPU-Kerne, per
  `h3.experiment.parallelism-percent` justierbar), asynchron mit
  Polling-Endpoint und Fortschrittsanzeige. Vier Stack-Sizing-Modi:
  gleiche Stack-Größe, gleiches Gold (LCM-Snap), Wochenproduktion und
  gold-normalisierte Wochenproduktion. Tier- und Faction-Filter im
  Config; Report mit Per-Unit- und Per-Faction-Tabelle plus
  „Anomalien" (Truppen, die mehrheitlich gegen die nächstniedrigere
  Tier-Klasse verlieren).
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

## Erste Erkenntnisse aus der Matrix-Auswertung

Vier vergleichbare Läufe gegen denselben Pool (63 Upgrade-Einheiten,
9 Fraktionen × 7 Tiers, 20 Seeds, je Match-up beide Rollen → ≈ 78 k
Sims pro Modus). Faction-Win-Rate gemittelt über alle Unit-vs-Unit-
Match-ups:

| Rang   | EQUAL_COUNT       | EQUAL_GOLD         | WEEKLY_PRODUCTION | EQUAL_GOLD_WEEKLY  |
|--------|-------------------|--------------------|-------------------|--------------------|
| 1      | Castle 52.6 %     | **Rampart 59.3 %** | **Castle 54.7 %** | **Rampart 59.2 %** |
| 2      | Dungeon 50.8 %    | Tower 58.6 %       | Rampart 51.4 %    | Tower 58.1 %       |
| 3      | Tower 50.2 %      | Castle 53.9 %      | Dungeon 51.1 %    | Castle 53.4 %      |
| 4      | Inferno 50.1 %    | Stronghold 53.0 %  | Conflux 50.6 %    | Stronghold 53.3 %  |
| 5      | Stronghold 49.7 % | Inferno 50.5 %     | Tower 50.0 %      | Inferno 50.7 %     |
| 6      | Necropolis 49.5 % | Conflux 48.1 %     | Stronghold 49.7 % | Dungeon 48.3 %     |
| 7      | Fortress 49.2 %   | Dungeon 47.4 %     | Fortress 49.3 %   | Conflux 48.0 %     |
| 8      | Rampart 49.2 %    | Fortress 43.0 %    | Necropolis 47.4 % | Fortress 43.1 %    |
| 9      | Conflux 48.7 %    | Necropolis 36.2 %  | Inferno 45.7 %    | Necropolis 35.8 %  |
| Spread | 3.9 pp            | **23.1 pp**        | 9.0 pp            | **23.4 pp**        |

**Was die Modi inhaltlich erzählen**:

- **EQUAL_COUNT** ist für Faction-Ranking unaussagekräftig — alle neun
  Factions liegen in einem 4-pp-Band um die 50 %. Jede Faction hat eine
  starke T7 (Arch Angel 99 %, Arch Devil 97 %, Black Dragon 95 %, …),
  und bei gleicher Stack-Größe mittelt sich das raus.
- **EQUAL_GOLD** spreizt drastisch (23 pp). Rampart und Tower
  führen, weil ihre Mid-Tier-Einheiten gold-effizient sind (Iron Golem
  200 g/35 HP, Naga Queen, War Unicorn, Dendroid Soldier). Necropolis
  bricht auf 36 % ein — Lich, Black Knight, Ghost Dragon sind teuer
  gemessen an ihren Stats, und ihr eigentlicher Hebel (Necromancer-
  Skill, Animate Dead) fehlt in der Engine.
- **WEEKLY_PRODUCTION** bleibt nahe an der Tier-Hierarchie (9 pp Spread,
  nur 2 Anomalien gegenüber 23 bei EQUAL_GOLD), weil die Produktionsraten
  den Tier-Gap einpreisen (14 Pikemen pro Woche, 1 Angel pro Woche).
- **EQUAL_GOLD_WEEKLY** liefert nahezu dieselbe Rangfolge wie EQUAL_GOLD
  (Top 5 identisch, max ±0.8 pp Differenz). Mathematisch erwartbar — die
  Wochengewichtung hebt sich im Budget weitgehend wieder auf. Der Modus
  ist primär eine Plausibilitätskontrolle des Gold-Vergleichs.

**Stabile Befunde**:

- **Castle** ist die robusteste Faction (Top 3 in drei von vier Modi).
- **Rampart + Tower** sind die gold-effizientesten Factions.
- **Necropolis** verliert in jeder gold-normierten Auswertung — ein
  Modell-Artefakt, weil Necromancy/Animate-Dead/Skeleton-Transformer
  nicht modelliert sind.
- **Magma Elemental** ist die einzige Anomalie über alle vier Modi
  (T5 verliert mehrheitlich gegen T4) — bekanntermaßen die schwächste
  Tier-5-Einheit, deren Fire-Immunity-Vorteil ohne Spells nicht zieht.

**Modell-Grenzen, die diese Zahlen verzerren**:

- Keine Sprüche → Necropolis (Animate Dead), Tower (Magi-Casts),
  Conflux (Spell-Imm./Fire-Imm.), Black Dragon (Spell-Imm.) verlieren
  ihren H3-Hauptvorteil.
- Keine Hero-Skills → Necromancy, Tactics, Logistics, Sorcery wirken nicht.
- Single-Stack-Armeen → Pit Lord Raise Demons, Lich Death Cloud,
  Cerberus 3-Hex-Attack, Magog Splash-Shot fehlen alle.

Die Werte sind also als „unter spell-/hero-freien 1-vs-1-Bedingungen"
zu lesen, nicht als universelle H3-Tier-Liste.

## Was als nächstes kommt

- **Mixed Armies** — mehrere Stacks pro Seite mit gemeinsamer
  Initiative, Voraussetzung für AoE-Effekte (Lich Death Cloud) und
  echte Truppenkombinationen. Erst damit lassen sich Stadt-vs-Stadt-
  Battles sinnvoll auswerten.
- **Helden** mit Primärwerten, Sekundärfertigkeiten und Zauberbuch.
- **Belagerung**: Mauern, Catapult, Wall-Penalty für Schützen.

## Quellen

- Stat-Werte aus dem RoE-Manual (`files/heroes3_manual.pdf`,
  Text-Layer-Extrakt unter `files/h3_manual.txt`).
- Spezialfähigkeiten: https://heroes.thelazy.net/index.php/Special_ability
