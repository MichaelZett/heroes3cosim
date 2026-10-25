package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Heuristik mit Schützen-Intelligenz:
 * <ul>
 *   <li>Bei Distanz 1 → Nahkampf-Angriff.</li>
 *   <li>Schütze ohne Engagement-Drohung → schießen.</li>
 *   <li>Schütze gegen schnelleren Melee-Gegner, der dieses Mal nicht in Distanz 1 wäre → schießen.</li>
 *   <li>Schütze, der schneller als der heranrückende Melee-Gegner ist und nächste Runde im
 *       Nahkampf wäre → ausweichen in beliebiger Richtung, damit Distanz &gt; Gegner-Speed bleibt.
 *   <li>Schütze vs. Schütze: bleiben stehen und schießen. Deckung hinter Hindernissen gibt es
 *       nicht — die Hindernis-Strafe gilt nur für Schüsse über Stadtmauern.</li>
 *   <li>Sonst → Bewegung Richtung Gegner, ggf. Move-and-Melee.</li>
 * </ul>
 *
 * <p><strong>Stack-Belegung</strong>: alle Kandidaten-Suchen (Charge, Kite, Flieger-
 * Landung) filtern Hexen heraus, die von einem anderen lebenden Stack besetzt sind. Dafür
 * speichert {@link #planRound(BattleSetup)} das aktuelle Setup. Ohne planRound-Call
 * (z.B. in einigen Unit-Tests) bleibt {@code currentSetup} null und der Filter ist inaktiv —
 * im Single-Battle-Pfad gibt's eh keine Stack-Konflikte.
 */
public final class GreedyAutoSolver implements AutoSolver {

    /**
     * Gewicht eines Gegner-Treffers gegenüber einem Eigentreffer bei der Lande-Hex-Wahl.
     * Größer als die höchste erreichbare Zahl vermiedener Eigentreffer, damit ein Gegner-Treffer
     * sie überwiegt — die Wertung wird dadurch lexikografisch.
     */
    private static final int ENEMY_HIT_WEIGHT = 10;

    private @Nullable BattleSetup currentSetup;

    private final boolean tacticalWait;
    private final boolean avoidExposedCharges;

    public GreedyAutoSolver() {
        this(true);
    }

    /**
     * @param tacticalWait ob ein Nahkämpfer, der sein Ziel diese Runde nicht erreicht, per
     *                     {@link Action.Wait} ans Rundenende ausweicht. Abschaltbar, damit
     *                     Harnesses die Heuristik gegen sich selbst antreten lassen können —
     *                     in der Faction-Matrix spielen beide Seiten denselben Solver, dort
     *                     ist ein Solver-Vorteil per Konstruktion unsichtbar.
     */
    public GreedyAutoSolver(boolean tacticalWait) {
        this(tacticalWait, true);
    }

    /**
     * @param avoidExposedCharges ob ein Anlauf, der allein in die gegnerische Armee führen würde,
     *                            per Wait verschoben wird ({@link #chargeOrWait}). Der
     *                            {@link StrategicAutoSolver} schaltet das ab: Im Duell gewann
     *                            Greedy damit 0.585 gegen sich selbst ohne (+16.9 Sigma) und
     *                            mit dem Ziel in der Schätzung noch einmal 0.562 (+12.3 Sigma);
     *                            Strategic verlor in beiden Fassungen leicht (0.490 / 0.493) —
     *                            das Warten stört dort die geplanten Fokus-Angriffe.
     */
    GreedyAutoSolver(boolean tacticalWait, boolean avoidExposedCharges) {
        this.tacticalWait = tacticalWait;
        this.avoidExposedCharges = avoidExposedCharges;
    }

    @Override
    public void planRound(BattleSetup setup) {
        this.currentSetup = setup;
    }

    /**
     * Multi-Target-Auswahl für Multi-Stack-Battles. Heuristik:
     * <ul>
     *   <li>Lebender Gegner auf Distanz 1 → diesen (Engagement zwingt zum Nahkampf).</li>
     *   <li>Schütze (kann noch schießen) → gefährlichster Gegner = {@code avgDmg × count};
     *       Tiebreak: kürzere Distanz, dann niedrigerer Slot.</li>
     *   <li>Sonst (Melee-Mover) → kürzeste Distanz, Tiebreak niedrigerer Slot.</li>
     * </ul>
     * Sichert dieselbe API wie die 1-vs-1-Variante: {@link #decide(Stack, Stack, Battlefield)}
     * läuft anschließend gegen das hier gewählte Single-Target weiter — bestehende
     * Kite/Charge-Heuristiken bleiben unverändert.
     */
    @Override
    public @Nullable Stack pickTarget(Stack active, List<Stack> opponents, Battlefield battlefield) {
        List<Stack> alive = opponents.stream().filter(Stack::isAlive).toList();
        if (alive.isEmpty()) {
            return null;
        }
        Stack adjacent = alive.stream()
                .filter(o -> active.distanceTo(o) == 1)
                .min(Comparator.comparingInt(Stack::slot))
                .orElse(null);
        if (adjacent != null) {
            return adjacent;
        }
        if (active.canShoot()) {
            return alive.stream()
                    .max(Comparator
                            .comparingInt(GreedyAutoSolver::dangerScore)
                            .thenComparingInt(o -> -active.distanceTo(o))
                            .thenComparingInt(o -> -o.slot()))
                    .orElseThrow();
        }
        return alive.stream()
                .min(Comparator
                        .comparingInt((Stack o) -> active.distanceTo(o))
                        .thenComparingInt(Stack::slot))
                .orElseThrow();
    }

    private static int dangerScore(Stack opponent) {
        int avgDmg = (opponent.unit().minDamage() + opponent.unit().maxDamage()) / 2;
        return avgDmg * opponent.getCount();
    }

    /**
     * Sonderaktionen vor jeder Bewegung, für beide Solver gleich: Resurrection (Arch Angel),
     * Dämonen beschwören (Pit Lord), Kreatur-Zauber (Ogre Magi, Master Genie). {@code null}, wenn
     * keine davon jetzt lohnt.
     */
    static @Nullable Action specialAction(Stack active, Stack opponent, @Nullable BattleSetup setup) {
        Stack resurrectionTarget = Resurrection.worthwhileTarget(active, setup);
        if (resurrectionTarget != null) {
            return new Action.Resurrect(resurrectionTarget);
        }
        Stack corpse = DemonRaising.worthwhileCorpse(active, setup);
        if (corpse != null) {
            return new Action.RaiseDemons(corpse);
        }
        return CreatureSpellcasting.decide(active, opponent, setup);
    }

    @Override
    public Action decide(Stack active, Stack opponent, Battlefield battlefield) {
        Action special = specialAction(active, opponent, currentSetup);
        if (special != null) {
            return special;
        }
        Hex from = active.position();
        // Bei einem zweifeldrigen Ziel das nähere seiner Felder.
        Hex to = opponent.nearestHexTo(from);
        int distance = active.distanceTo(opponent);
        int speed = active.getSpeed();
        Movement movement = active.unit().movement();

        if (distance == 1) {
            return new Action.Melee(opponent);
        }
        if (active.canShoot()) {
            return decideShooter(active, opponent, battlefield, from, to, distance);
        }
        // Flieger umfliegen Tank-Walls: statt straight-line auf einen vermutlich belegten
        // Hex zu zielen, suche den nächsten freien Adjacent des Ziels. Findet sich keiner
        // (alle Adjacents belegt oder außer Reichweite), defendiere — semantisch korrekter
        // als ein blinder Move.
        if (movement == Movement.FLYING) {
            Hex landing = findFlyerLanding(active, opponent, battlefield);
            if (landing != null) {
                return chargeOrWait(active, opponent, landing);
            }
            return waitOr(active, new Action.Defend());
        }
        // IMPACT_DAMAGE-Units (Cavalier/Champion): maximalen Run-up wählen statt direkter Linie.
        // Engine zählt {@code hexesMoved = startPos.distanceTo(destination)} → straight-line.
        if (active.hasSpeciality(UnitSpeciality.IMPACT_DAMAGE)) {
            Hex charge = findMaxRunupCharge(active, opponent, battlefield, speed);
            if (charge != null) {
                return chargeOrWait(active, opponent, charge);
            }
        }
        return approach(active, opponent, battlefield, to);
    }

    /** Bodentruppen ohne Anlauf-Bonus: auf geradem Weg heran, angreifen, wenn es reicht. */
    private Action approach(Stack active, Stack opponent, Battlefield battlefield, Hex to) {
        Hex from = active.position();
        int speed = active.getSpeed();
        Hex moveTarget = battlefield.moveToward(active, to, speed);
        boolean fits = fitsAt(active, moveTarget, battlefield);
        if (fits && !moveTarget.equals(from) && BattleSetup.touches(active, moveTarget, opponent)) {
            return chargeOrWait(active, opponent, moveTarget);
        }
        if (active.isDoubleWide()) {
            // Der gerade Weg führt den Kopf ans Ziel; passt das Heck dort nicht, vielleicht ein
            // anderes Landefeld, an dem das Heck anliegt.
            Hex landing = findGroundLanding(active, opponent, battlefield, speed);
            if (landing != null) {
                return chargeOrWait(active, opponent, landing);
            }
        }
        if (moveTarget.equals(from)) {
            return waitOr(active, new Action.Defend());
        }
        if (!fits) {
            // moveToward berücksichtigt keine Stacks → könnte auf den Tank vor dem Ziel zielen.
            // Statt blind hinzulaufen und vom Engine-Sicherheitsnetz zu Defend degradiert zu
            // werden, defendieren wir direkt.
            return waitOr(active, new Action.Defend());
        }
        // Reiner Annäherungsschritt: der Stack erreicht sein Ziel diese Runde nicht und würde
        // sich nur exponiert ins Niemandsland stellen. H3-Standardzug ist hier Wait (Manual
        // S. 43) — die Aktion geht nicht verloren, sie wandert ans Rundenende. Dort ist der
        // Gegner schon gezogen: entweder er ist herangerückt und wir greifen an, statt ihm den
        // ersten Schlag zu schenken, oder wir laufen wenigstens auf seine aktuelle Position zu
        // statt auf die von vorhin. Der Guard ist zwingend: in der Late-Phase muss gehandelt
        // werden, sonst degradiert die Engine das zweite Wait zu Defend und der Stack bewegt
        // sich nie.
        if (tacticalWait && !active.hasWaitedThisTurn()) {
            return new Action.Wait();
        }
        return new Action.Move(moveTarget);
    }

    /**
     * Findet für einen Flieger den am besten erreichbaren freien Adjacent-Hex des Ziels.
     * Cube-Distanz reicht (Flieger ignorieren Obstacles). Bei mehreren Kandidaten gewinnt
     * der mit der geringsten Distanz vom Startfeld. Liefert {@code null}, wenn alle
     * Adjacents blockiert oder außer Reichweite sind.
     */
    private @Nullable Hex findFlyerLanding(Stack active, Stack opponent, Battlefield bf) {
        Hex from = active.position();
        int speed = active.getSpeed();
        // Devil/Arch Devil (TELEPORT_NO_COST): Speed-Schranke entfällt, jeder freie
        // passierbare Adjacent-Hex am Ziel ist erreichbar (Manual S. 99).
        boolean teleports = active.hasSpeciality(UnitSpeciality.TELEPORT_NO_COST);
        boolean fireBreath = active.hasSpeciality(UnitSpeciality.FIRE_BREATH);
        boolean threeHeaded = active.hasSpeciality(UnitSpeciality.THREE_HEADED_ATTACK);
        Hex best = null;
        int bestScore = Integer.MIN_VALUE;
        int bestDist = Integer.MAX_VALUE;
        for (Hex candidate : landingCandidates(active, opponent)) {
            int d = from.distanceTo(candidate);
            if (candidate.equals(from)
                    || (!teleports && d > speed)
                    || !fitsAt(active, candidate, bf)) {
                continue;
            }
            int splashScore = computeSplashScore(active, candidate, opponent, fireBreath, threeHeaded);
            // Primärkriterium: möglichst viele Splash-Bonus-Hits (FIRE_BREATH/THREE_HEADED).
            // Sekundär: kürzester Anflug.
            if (splashScore > bestScore || (splashScore == bestScore && d < bestDist)) {
                bestScore = splashScore;
                bestDist = d;
                best = candidate;
            }
        }
        return best;
    }

    /**
     * Bewertet einen potenziellen Lande-Hex anhand des <strong>Netto</strong>-Splash:
     * getroffene Gegner (+1) minus getroffene eigene Stacks (−1).
     * <ul>
     *   <li>FIRE_BREATH (Dragons): der Inline-Hex hinter dem Primärziel.</li>
     *   <li>THREE_HEADED_ATTACK (Cerberus): die beiden Felder links und rechts des Ziels aus
     *       Sicht des Lande-Hex, nur Gegner — so wie {@code Battle.applyMeleeSplash}.</li>
     * </ul>
     *
     * <p>Der Abzug ist der Punkt: der Fire-Breath-Splash trifft beide Seiten. Wer nur Gegner
     * zählt, sucht sich den Lande-Hex mit dem größten Eigenschaden aus, sobald die eigene
     * Linie dicht steht. Und die Wahl ist klebrig — einmal adjacent, schlägt der Stack aus
     * derselben Position weiter zu, ohne sich neu zu positionieren.
     *
     * <p><strong>Gewichtung ist lexikografisch</strong>, nicht symmetrisch: Gegner-Treffer
     * zählen {@value #ENEMY_HIT_WEIGHT}, eigene 1. Ein Gegner-Treffer schlägt damit jede
     * Zahl vermiedener Eigentreffer — Eigenbeschuss zu vermeiden lohnt nur, solange es
     * keinen Gegner-Treffer kostet.
     *
     * <p>Greift nur für Flieger über {@link #findFlyerLanding}, die ohnehin einen Lande-Hex
     * wählen. Bodeneinheiten bekommen bewusst <em>keine</em> eigene Lande-Hex-Suche: gemessen
     * kostete das Inferno 14 Prozentpunkte gegen Dungeon, während der eingesparte
     * Eigenbeschuss fast ausschließlich Tier-1/2-Einheiten betraf und den Ausgang nicht
     * bewegte.
     */
    private int computeSplashScore(Stack active, Hex landingHex, Stack primary,
                                   boolean fireBreath, boolean threeHeaded) {
        // Die beiden Felder, an denen sich Angreifer und Ziel nach der Landung berühren.
        Hex own = landingHex;
        for (Hex h : active.hexesAt(landingHex)) {
            if (primary.distanceTo(h) < primary.distanceTo(own)) {
                own = h;
            }
        }
        Hex hit = primary.nearestHexTo(own);
        int score = 0;
        if (fireBreath) {
            Hex behind = behindHex(own, hit);
            score += collateralValue(active, primary.occupies(behind) ? behindHex(hit, behind) : behind);
        }
        if (threeHeaded) {
            for (Hex flank : own.commonNeighbors(hit)) {
                // Three-Headed Attack trifft nur Gegner — eigene Stacks auf den Flanken zählen nicht.
                score += Math.max(0, collateralValue(active, flank));
            }
        }
        return score;
    }

    /**
     * {@code +}{@value #ENEMY_HIT_WEIGHT} für einen gegnerischen, {@code -1} für einen eigenen
     * Stack auf {@code hex}, {@code 0} wenn der Hex leer ist.
     */
    private int collateralValue(Stack active, Hex hex) {
        Stack occupant = findStackAt(hex, active);
        if (occupant == null) {
            return 0;
        }
        return occupant.side() == active.side() ? -1 : ENEMY_HIT_WEIGHT;
    }

    private static Hex behindHex(Hex from, Hex through) {
        return new Hex(through.q() + (through.q() - from.q()),
                through.r() + (through.r() - from.r()));
    }

    /**
     * Sucht unter den sechs adjazenten Hexen des Gegners das mit der größten geraden Distanz vom
     * Startfeld — bei IMPACT_DAMAGE-Einheiten gibt jeder Hex Anlauf +5 % Schaden (gekappt bei +50 %
     * = 10 Hex). Kandidat muss passable, frei und in einer Runde erreichbar sein (sowohl
     * straight-line als auch A*-Pfad ≤ Speed). Liefert {@code null}, wenn kein adjazenter Hex
     * erreichbar ist — dann fällt der Solver auf den normalen Move-Pfad zurück.
     */
    private @Nullable Hex findMaxRunupCharge(Stack active, Stack opponent, Battlefield bf, int speed) {
        Hex from = active.position();
        return landingCandidates(active, opponent).stream()
                .filter(c -> isReachableInOneTurn(active, c, from, bf, speed))
                .filter(c -> from.distanceTo(c) > 0)
                .max(Comparator.comparingInt(c -> Math.min(from.distanceTo(c), 10)))
                .orElse(null);
    }

    /**
     * Das nächste Landefeld am Ziel, das ein zweifeldriger Bodenstack in diesem Zug erreicht —
     * auch eines, an dem nur sein Heck anliegt; {@code null}, wenn keines erreichbar ist.
     */
    private @Nullable Hex findGroundLanding(Stack active, Stack opponent, Battlefield bf, int speed) {
        Hex from = active.position();
        return landingCandidates(active, opponent).stream()
                .filter(c -> !c.equals(from))
                .filter(c -> isReachableInOneTurn(active, c, from, bf, speed))
                .min(Comparator.comparingInt(from::distanceTo))
                .orElse(null);
    }

    /**
     * Kopffelder, von denen aus der Stack das Ziel im Nahkampf erreicht ({@link BattleSetup#attackHeads});
     * ohne Setup die Nachbarn des Zielfelds.
     */
    private List<Hex> landingCandidates(Stack active, Stack opponent) {
        BattleSetup setup = currentSetup;
        if (setup == null) {
            return opponent.position().neighbors(Hex.isLeftward(active.position(), opponent.position()));
        }
        return setup.attackHeads(active, opponent);
    }

    private boolean isReachableInOneTurn(Stack active, Hex candidate, Hex from, Battlefield bf,
                                         int speed) {
        if (from.distanceTo(candidate) > speed) {
            return false;
        }
        if (!fitsAt(active, candidate, bf)) {
            return false;
        }
        List<Hex> path = bf.pathFor(active, candidate);
        return !path.isEmpty() && path.size() <= speed;
    }

    /**
     * Hat {@code mover} mit dem Kopf auf {@code head} Platz — alle seine Felder begehbar, keines
     * von einem anderen Stack oder einer Maschine belegt? Ohne Setup nur das Feld selbst.
     */
    private boolean fitsAt(Stack mover, Hex head, Battlefield bf) {
        BattleSetup setup = currentSetup;
        if (setup == null) {
            return bf.isPassable(head);
        }
        return mover.hexesAt(head).stream().allMatch(bf::isPassable) && setup.fits(mover, head);
    }

    /**
     * Lebender Stack auf {@code hex} — beide Seiten, nur {@code mover} selbst zählt nicht.
     * Spiegelt {@code Battle.findStackAt}, damit Solver und Engine dieselbe Belegung sehen.
     */
    // s != mover: Identitätsvergleich auf der mutable Stack-Entity (kein equals).
    @SuppressWarnings("ReferenceEquality")
    private @Nullable Stack findStackAt(Hex hex, Stack mover) {
        BattleSetup setup = currentSetup;
        if (setup == null) {
            return null;
        }
        for (Stack s : setup.attackerStacks()) {
            if (s != mover && s.isAlive() && s.occupies(hex)) {
                return s;
            }
        }
        for (Stack s : setup.defenderStacks()) {
            if (s != mover && s.isAlive() && s.occupies(hex)) {
                return s;
            }
        }
        return null;
    }

    private Action decideShooter(Stack active, Stack opponent, Battlefield bf,
                                 Hex from, Hex to, int distance) {
        int opponentSpeed = opponent.getSpeed();
        int mySpeed = active.getSpeed();

        if (opponent.canShoot()) {
            return new Action.Shoot(opponent);
        }

        // Multi-Stack: Kite-Heuristik ist nicht sinnvoll, weil sie nur gegen den
        // pickTarget-Threat bewertet und andere lebende Gegner ignoriert (Kite-Position
        // könnte direkt in deren Reichweite landen). Im Multi-Stack-Kontext setzen wir
        // statt Kiten auf Tank-Wall + Defend (siehe Strategic-Solver). Hier: direkt
        // schießen — der gegnerische Stack-Cluster macht 1-Target-Kite ohnehin sinnlos.
        if (hasMultipleLivingEnemies(active)) {
            return new Action.Shoot(opponent);
        }

        // Keine unmittelbare Engagement-Drohung → schießen.
        if (distance - opponentSpeed > 1) {
            return new Action.Shoot(opponent);
        }
        // Wir sind nicht schneller — Kiten würde nichts bringen, lieber schießen.
        if (mySpeed <= opponentSpeed) {
            return new Action.Shoot(opponent);
        }
        // DPS-Race: Wenn wir den Gegner schätzungsweise erledigen, bevor er uns erreicht,
        // hat Kiten keinen Sinn — der verlorene Schuss wäre vermutlich der entscheidende.
        if (willOutpaceOpponent(active, opponent, distance)) {
            return new Action.Shoot(opponent);
        }
        // Kite nur bei wirklicher Lebensgefahr — wenn ein eingehender Treffer den Top-Creature
        // (oder mehr) one-shotten würde. Sonst Treffer kassieren und feuern; sonst landen Sims
        // in 50+ Rounds wegen Kite/Shoot-Wechsel.
        if (!threatIsDeadly(opponent, active)) {
            return new Action.Shoot(opponent);
        }
        Hex kite = findKitePosition(active, from, to, bf, mySpeed, opponentSpeed);
        if (kite == null) {
            return new Action.Shoot(opponent);
        }
        return new Action.Move(kite);
    }

    /**
     * Anlauf in den Nahkampf — es sei denn, der Stack liefe allein in die gegnerische Armee.
     *
     * <p>Im Spiegel-Duell verlor fast immer, wer zuerst zog: Der schnellste Stack stürmte in
     * Runde 1 los und wurde von allen langsameren Gegnern zusammengeschlagen, bevor die eigene
     * Armee nachkam. Ein H3-Spieler wartet hier. Deshalb: Können gegnerische Nahkämpfer, die
     * noch dran sind, das Landefeld erreichen und zusammen mindestens die Hälfte der eigenen
     * Lebenspunkte ausrichten, wird gewartet. In der Wait-Phase wird gehandelt, wie bei
     * {@link #waitOr}.
     */
    private Action chargeOrWait(Stack active, Stack target, Hex landing) {
        if (avoidExposedCharges && tacticalWait && !active.hasWaitedThisTurn() && active.getSpeed() > 0
                && isExposedCharge(active, landing)) {
            return new Action.Wait();
        }
        return new Action.MoveAndMelee(landing, target);
    }

    /**
     * Schätzt den Nahkampfschaden, der das Landefeld in dieser Runde noch treffen kann: Gegner,
     * die nicht schneller sind als der Angreifer (sie haben vermutlich noch nicht gezogen) und das
     * Feld erreichen. Schützen zählen nicht, sie treffen ihn ohnehin überall. Das Ziel zählt mit:
     * Nach seinem Gegenschlag hat es, wenn es nicht schneller ist, noch seinen eigenen Zug — im
     * Phoenix- und Black-Dragon-Spiegel war genau das der Schlag, der den Erstzieher erledigte.
     */
    private boolean isExposedCharge(Stack active, Hex landing) {
        BattleSetup setup = currentSetup;
        if (setup == null) {
            return false;
        }
        int incoming = 0;
        for (Stack enemy : setup.opponentsOf(active)) {
            if (!enemy.isAlive() || enemy.canShoot() || enemy.getSpeed() > active.getSpeed()) {
                continue;
            }
            if (enemy.distanceTo(landing) <= enemy.getSpeed() + 1) {
                incoming += dangerScore(enemy);
            }
        }
        int ownHealth = (active.getCount() - 1) * active.unit().health() + active.getCurrentHealth();
        return 2 * incoming >= ownHealth;
    }

    /**
     * Verschiebt eine Verlegenheits-Aktion ans Rundenende, statt sie sofort zu verbrauchen.
     * Alle drei Aufrufer sind Sackgassen (kein freier Lande-Hex, Stack kommt nicht vom Fleck,
     * Zielhex belegt) — in der Late-Phase haben sich die blockierenden Stacks vielleicht
     * wegbewegt.
     *
     * <p>Zwei Ausnahmen, in denen der Fallback sofort greift: der Stack hat diese Runde schon
     * gewartet, oder er ist unbeweglich. Bei Speed 0 kann sich bis zur Late-Phase nichts
     * ändern, was ihm hilft — dann ist Defend (+20 % Defense) sofort der bessere Zug.
     */
    private Action waitOr(Stack active, Action fallback) {
        if (!tacticalWait || active.hasWaitedThisTurn() || active.getSpeed() == 0) {
            return fallback;
        }
        return new Action.Wait();
    }

    private boolean hasMultipleLivingEnemies(Stack active) {
        BattleSetup setup = currentSetup;
        if (setup == null) {
            return false;
        }
        int count = 0;
        for (Stack o : setup.opponentsOf(active)) {
            if (o.isAlive()) {
                count++;
                if (count > 1) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Grobe Schätzung: würde ein durchschnittlicher Treffer des Gegners die Top-HP des Shooters
     * zerlegen? Att/Def-Boni werden ignoriert — die Heuristik ist bewusst pessimistisch zugunsten
     * von „lieber schießen", weil der gewählte Aktionsraum (Schuss verlieren) teuer ist.
     */
    private static boolean threatIsDeadly(Stack opponent, Stack active) {
        int avgBase = (opponent.unit().minDamage() + opponent.unit().maxDamage()) / 2;
        int avgIncoming = opponent.getCount() * avgBase;
        return avgIncoming >= active.getCurrentHealth();
    }

    /**
     * Erwartet der Schütze, den Gegner mit den verbleibenden Schüssen während dessen Anlauf zu
     * töten? Konservative Schätzung: avg-Schadens-Range × Stack-Count × Schüsse-pro-Runde ×
     * Runden-bis-Engagement gegen die Gesamt-HP des Gegners. Att/Def und Penalties bleiben außen
     * vor — die Heuristik soll Kite/Shoot-Loops abbrechen, sobald sich der Race klar abzeichnet.
     */
    private static boolean willOutpaceOpponent(Stack active, Stack opponent, int distance) {
        int opponentSpeed = Math.max(1, opponent.getSpeed());
        // Aufgerundete Runden bis Engagement; Engagement = Distanz 1.
        int turnsUntilMelee = Math.max(1, (distance - 1 + opponentSpeed - 1) / opponentSpeed);
        double avgPerShot = (active.unit().minDamage() + active.unit().maxDamage()) / 2.0;
        int shotsPerTurn = active.hasSpeciality(UnitSpeciality.TWO_SHOTS) ? 2 : 1;
        int shotsAvailable = Math.min(active.shotsRemaining(), turnsUntilMelee * shotsPerTurn);
        double projectedDamage = active.getCount() * avgPerShot * shotsAvailable;

        int opponentMaxHp = opponent.unit().health();
        int opponentTotalHp = Math.max(0, opponent.getCount() - 1) * opponentMaxHp
                + opponent.getCurrentHealth();
        return projectedDamage >= opponentTotalHp;
    }

    /**
     * Sucht ein Feld, das mindestens {@code opponentSpeed + 2} Hex vom Gegner entfernt ist
     * (damit der Gegner nächste Runde noch nicht adjacent ist) und vom Start in einem Zug
     * erreichbar ist. Bei mehreren Optionen gewinnt die größere Distanz; bei Gleichstand
     * die obstacle-freie eigene Schusslinie.
     */
    private @Nullable Hex findKitePosition(Stack active, Hex from, Hex to, Battlefield bf,
                                           int mySpeed, int opponentSpeed) {
        return reachableCandidates(active, from, to, bf, mySpeed).stream()
                .filter(c -> c.distanceTo(to) - opponentSpeed > 1)
                .max(Comparator.comparingInt(c -> c.distanceTo(to)))
                .orElse(null);
    }

    private List<Hex> reachableCandidates(Stack active, Hex from, Hex to, Battlefield bf,
                                          int mySpeed) {
        List<Hex> candidates = new ArrayList<>();
        for (Hex c : bf.cells()) {
            if (isKiteCandidate(active, c, from, to, bf, mySpeed)) {
                candidates.add(c);
            }
        }
        // Die Aufrufer nehmen bei Gleichstand das erste Element (findFirst, max). Eine feste
        // Reihenfolge nach q bevorzugte Zellen der linken Seite — also die des Angreifers. Die
        // Sortierung hier ist spiegelsymmetrisch: kürzester Weg, dann Reihe, dann q von der
        // eigenen Seite aus gezählt.
        int lateral = active.side() == Side.ATTACKER ? 1 : -1;
        candidates.sort(Comparator.<Hex>comparingInt(c -> c.distanceTo(from))
                .thenComparingInt(Hex::r)
                .thenComparingInt(c -> lateral * c.q()));
        return candidates;
    }

    private boolean isKiteCandidate(Stack active, Hex c, Hex from, Hex to, Battlefield bf,
                                    int mySpeed) {
        if (c.equals(from) || c.equals(to)) {
            return false;
        }
        return isReachableInOneTurn(active, c, from, bf, mySpeed);
    }
}
