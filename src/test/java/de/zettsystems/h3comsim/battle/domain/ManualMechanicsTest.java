package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.BattleEvent;
import de.zettsystems.h3comsim.battle.domain.events.ListEventCollector;
import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Engine-Mechanik-Tests gegen das RoE-Manual ({@code files/heroes3_manual.pdf}).
 * Bündelt die Verifikationen für Items aus dem Manual-Audit, damit Regressionen
 * an einer Stelle auffallen.
 */
class ManualMechanicsTest {

    @Test
    void death_stare_kills_at_most_one_top_creature_per_trigger() {
        // Manual S. 95: „10% chance per attack of killing the top creature outright per
        // 10 Mighty Gorgons." Trigger-Chance skaliert linear mit Stack-Größe, Kill ist
        // immer genau 1 — niemals count/10 Kills.
        // Pikeman-Stack groß genug, dass mehrere Runden Combat anfallen → DEATH_STARE
        // sollte mindestens einmal triggern (100 Gorgons → 100 % Chance pro Schlag).
        Stack gorgons = new Stack(UnitCatalog.MIGHTY_GORGON, 100, new Hex(7, 5),
                Side.ATTACKER, 0);
        Stack target = new Stack(UnitCatalog.PIKEMAN, 5000, new Hex(8, 5), Side.DEFENDER, 0);
        BattleSetup setup = new BattleSetup(List.of(gorgons), List.of(target), Battlefield.STANDARD);

        ListEventCollector collector = new ListEventCollector();
        new Battle(new Random(1L), new GreedyAutoSolver(), collector).simulate(setup);

        List<BattleEvent.DeathStare> stares = collector.events().stream()
                .filter(e -> e instanceof BattleEvent.DeathStare)
                .map(e -> (BattleEvent.DeathStare) e)
                .toList();
        assertThat(stares).isNotEmpty();
        assertThat(stares).allMatch(s -> s.kills() == 1);
    }

    @Test
    void engaged_shooter_cannot_fire_via_engine() {
        // Manual S. 42: „Creatures with ranged attacks ... can fire only when there are no
        // adjacent enemies." Falls ein Solver trotzdem Shoot wählt während ein Gegner
        // adjacent ist, lehnt die Engine ab und stellt den Stack auf Defend.
        Stack marksman = new Stack(UnitCatalog.MARKSMAN, 9, new Hex(5, 5), Side.ATTACKER, 0);
        Stack engager = new Stack(UnitCatalog.HALBERDIER, 14, new Hex(6, 5), Side.DEFENDER, 0);
        BattleSetup setup = new BattleSetup(List.of(marksman), List.of(engager),
                Battlefield.STANDARD);

        // Forciere Shoot-Action, obwohl marksman engaged ist.
        AutoSolver forceShoot = (active, opp, bf) -> active == marksman
                ? new Action.Shoot(engager)
                : new Action.Wait();

        ListEventCollector collector = new ListEventCollector();
        new Battle(new Random(1L), forceShoot, collector).simulate(setup);

        boolean anyShoot = collector.events().stream()
                .anyMatch(e -> e instanceof BattleEvent.Shoot s
                        && s.actor() == Side.ATTACKER && s.actorSlot() == 0);
        boolean anyDefend = collector.events().stream()
                .anyMatch(e -> e instanceof BattleEvent.Defend d
                        && d.actor() == Side.ATTACKER && d.actorSlot() == 0);
        assertThat(anyShoot).as("engaged shooter must not fire").isFalse();
        assertThat(anyDefend).as("engine falls back to Defend").isTrue();
    }

    @Test
    void splash_shot_hits_friendly_stack_adjacent_to_target() {
        // H3-Splash trifft auch eigene Stacks. Setup: Magog schießt auf gegnerischen
        // Stack, der adjacent zu einem eigenen Magog-Verbündeten steht. Der eigene
        // Verbündete wird durch Splash mitgetroffen.
        Stack magog = new Stack(UnitCatalog.MAGOG, 10, new Hex(0, 5), Side.ATTACKER, 0);
        Stack friendlyAdjacent = new Stack(UnitCatalog.HORNED_DEMON, 4, new Hex(7, 5),
                Side.ATTACKER, 1);
        Stack enemyPrimary = new Stack(UnitCatalog.PIKEMAN, 30, new Hex(8, 5),
                Side.DEFENDER, 0);
        BattleSetup setup = new BattleSetup(List.of(magog, friendlyAdjacent),
                List.of(enemyPrimary), Battlefield.STANDARD);
        int allyCountBefore = friendlyAdjacent.getCount();

        // Force Magog to shoot the enemy that's adjacent to the friendly stack.
        AutoSolver forceShoot = (active, opp, bf) -> {
            if (active == magog) {
                return new Action.Shoot(enemyPrimary);
            }
            return new Action.Wait();
        };

        ListEventCollector collector = new ListEventCollector();
        new Battle(new Random(1L), forceShoot, collector).simulate(setup);

        // Splash-Hits werden als Shoot-Events emittiert mit Magog als actor und dem
        // Verbündeten als target.
        boolean friendlyHit = collector.events().stream()
                .anyMatch(e -> e instanceof BattleEvent.Shoot s
                        && s.actorSlot() == magog.slot()
                        && s.target() == Side.ATTACKER
                        && s.targetSlot() == friendlyAdjacent.slot());
        assertThat(friendlyHit).as("Splash trifft eigene Stacks im Radius").isTrue();
        assertThat(friendlyAdjacent.getCount()).isLessThan(allyCountBefore);
    }

    @Test
    void death_cloud_spares_undead_stacks() {
        // Manual S. 101: Death Cloud trifft alle non-undead Stacks im 1-Hex-Radius.
        // Setup: Power Lich schießt auf einen gegnerischen Pikeman, der adjacent zu
        // einem gegnerischen Skeleton steht. Skeleton ist Undead → kein Splash-Schaden.
        Stack lich = new Stack(UnitCatalog.POWER_LICH, 5, new Hex(0, 5), Side.ATTACKER, 0);
        Stack pikeman = new Stack(UnitCatalog.PIKEMAN, 5, new Hex(8, 5), Side.DEFENDER, 0);
        Stack skeleton = new Stack(UnitCatalog.SKELETON, 30, new Hex(9, 5),
                Side.DEFENDER, 1);
        BattleSetup setup = new BattleSetup(List.of(lich), List.of(pikeman, skeleton),
                Battlefield.STANDARD);
        int skeletonCountBefore = skeleton.getCount();

        AutoSolver forceLichShot = (active, opp, bf) -> {
            if (active == lich) {
                return new Action.Shoot(pikeman);
            }
            return new Action.Wait();
        };

        ListEventCollector collector = new ListEventCollector();
        new Battle(new Random(1L), forceLichShot, collector).simulate(setup);

        boolean skeletonHit = collector.events().stream()
                .anyMatch(e -> e instanceof BattleEvent.Shoot s
                        && s.actorSlot() == lich.slot()
                        && s.target() == Side.DEFENDER
                        && s.targetSlot() == skeleton.slot());
        assertThat(skeletonHit).as("Death Cloud darf Undead nicht treffen").isFalse();
        assertThat(skeleton.getCount()).isEqualTo(skeletonCountBefore);
    }

    @Test
    void vampire_lord_heal_method_resurrects_fallen_creatures() {
        // Stack.heal direkt: Auffüllen + ganze Resurrects bis startCount.
        Stack vampLord = new Stack(UnitCatalog.VAMPIRE_LORD, 4, new Hex(5, 5),
                Side.ATTACKER, 0);
        vampLord.takeDamage(80, Set.of());  // tötet 2 Vampire Lords (40 HP each)
        assertThat(vampLord.getCount()).isEqualTo(2);

        int healed = vampLord.heal(40);

        assertThat(healed).isEqualTo(40);
        assertThat(vampLord.getCount()).isEqualTo(3);  // 1 resurrected
    }

    @Test
    void vampire_lord_drain_triggers_resurrect_via_engine() {
        // Manual S. 101: Vampire Lord heilt sich am verursachten Nahkampf-Schaden,
        // kann tote eigene Vampire Lords resurrecten. Test via Battle: ein einzelner
        // Vampire-Lord-Hit gegen einen großen Stack erzeugt genug Schaden für 1 Resurrect.
        Stack vampLord = new Stack(UnitCatalog.VAMPIRE_LORD, 10, new Hex(5, 5),
                Side.ATTACKER, 0);
        vampLord.takeDamage(200, Set.of());  // tötet 5 Vampire Lords → aliveCount=5
        assertThat(vampLord.getCount()).isEqualTo(5);

        // Großer Pikeman-Stack: viel Damage-Empfänger; Vampire Lord macht ~80 Damage
        // (5 alive × 6.5 avg × 1.25 attack-bonus), heilt 80 → 1 voller Resurrect + 40 Rest.
        Stack pikemen = new Stack(UnitCatalog.PIKEMAN, 200, new Hex(6, 5),
                Side.DEFENDER, 0);
        BattleSetup setup = new BattleSetup(List.of(vampLord), List.of(pikemen),
                Battlefield.STANDARD);

        AutoSolver forceMelee = (active, opp, bf) -> {
            if (active == vampLord && opp.position().distanceTo(active.position()) == 1) {
                return new Action.Melee(opp);
            }
            return new Action.Wait();
        };

        // Nur eine Runde simulieren — Vampire schlägt zuerst (Speed 9 > Pikeman Speed 4),
        // Drain heilt vor der Pikeman-Retaliation. Test prüft, dass mind. ein Resurrect
        // direkt nach diesem Schlag passiert.
        new Battle(new Random(1L), forceMelee).simulate(setup);

        assertThat(vampLord.getCount()).isGreaterThan(5);
    }

    @Test
    void shooter_without_shots_cannot_fire_via_engine() {
        // Engine-Sicherheitsnetz: ein Solver der Shoot wählt während der Stack keine
        // Shots mehr hat (oder gar keine ranged attack), darf nicht zum Phantom-Schuss
        // führen. Engine fällt defensiv zu Defend zurück.
        Stack marksman = new Stack(UnitCatalog.MARKSMAN, 5, new Hex(0, 5), Side.ATTACKER, 0);
        Stack target = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(14, 5), Side.DEFENDER, 0);
        // Verbrauche alle Shots vorab.
        while (marksman.shotsRemaining() > 0) {
            marksman.useShot();
        }
        assertThat(marksman.canShoot()).isFalse();

        BattleSetup setup = new BattleSetup(List.of(marksman), List.of(target),
                Battlefield.STANDARD);
        AutoSolver forceShoot = (active, opp, bf) -> active == marksman
                ? new Action.Shoot(target)
                : new Action.Wait();

        ListEventCollector collector = new ListEventCollector();
        new Battle(new Random(1L), forceShoot, collector).simulate(setup);

        boolean anyShoot = collector.events().stream()
                .anyMatch(e -> e instanceof BattleEvent.Shoot s
                        && s.actor() == Side.ATTACKER && s.actorSlot() == 0);
        boolean anyDefend = collector.events().stream()
                .anyMatch(e -> e instanceof BattleEvent.Defend d
                        && d.actor() == Side.ATTACKER && d.actorSlot() == 0);
        assertThat(anyShoot).as("shooter without shots must not fire").isFalse();
        assertThat(anyDefend).as("engine falls back to Defend").isTrue();
    }

    @Test
    void fire_breath_flyer_lands_inline_to_hit_shooter_behind_tank() {
        // Black Dragon (FIRE_BREATH) gegen Eck-Schütze (14,0) mit Tank davor (13,0).
        // Adjacents von (14,0): (13,0)[Tank], (13,1)[frei], (14,1)[frei].
        // (13,0) ist Tank — blockt für Lande-Hex. (14,1) und (13,1) sind frei.
        // FIRE_BREATH-aware: Dragon soll auf den Hex landen, von dem der Tank inline-
        // angegriffen wird und der Schütze als "behind" getroffen wird.
        // (12,0) liegt 1 Hex VOR dem Tank — Dragon engaged Tank, Splash trifft Schütze.
        Stack dragon = new Stack(UnitCatalog.BLACK_DRAGON, 1, new Hex(0, 5), Side.ATTACKER, 0);
        Stack tank = new Stack(UnitCatalog.HALBERDIER, 14, new Hex(13, 0), Side.DEFENDER, 0);
        Stack shooter = new Stack(UnitCatalog.MARKSMAN, 9, new Hex(14, 0), Side.DEFENDER, 1);
        BattleSetup setup = new BattleSetup(List.of(dragon), List.of(tank, shooter),
                Battlefield.STANDARD);

        StrategicAutoSolver solver = new StrategicAutoSolver();
        solver.planRound(setup);
        Action action = solver.decide(dragon, tank, Battlefield.STANDARD);

        // Dragon engaged Tank — Lande-Hex sollte (12,0) sein, weil von dort behindHex
        // zum Schützen führt. behindHex((12,0), (13,0)) = (14,0) = Schütze.
        assertThat(action).isInstanceOf(Action.MoveAndMelee.class);
        Action.MoveAndMelee mm = (Action.MoveAndMelee) action;
        assertThat(mm.target()).isSameAs(tank);
        assertThat(mm.destination()).isEqualTo(new Hex(12, 0));
    }

    @Test
    void tank_pattern_skips_high_tier_attackers() {
        // Tank-Duty greift heute für jeden Melee auf RANGED_DOMINANT-Seite.
        // Mit isTankCandidate-Filter sollen T7+ und Stacks mit aggressiven Specials
        // (FIRE_BREATH) NICHT als Tank abgezogen werden, sondern angreifen.
        Stack beholder = new Stack(UnitCatalog.BEHOLDER, 5, new Hex(0, 0), Side.ATTACKER, 0);
        Stack troglodyte = new Stack(UnitCatalog.TROGLODYTE, 14, new Hex(0, 2),
                Side.ATTACKER, 1);
        Stack blackDragon = new Stack(UnitCatalog.BLACK_DRAGON, 1, new Hex(0, 5),
                Side.ATTACKER, 2);
        Stack threat = new Stack(UnitCatalog.PIKEMAN, 14, new Hex(10, 5),
                Side.DEFENDER, 0);
        BattleSetup setup = new BattleSetup(
                List.of(beholder, troglodyte, blackDragon), List.of(threat),
                Battlefield.STANDARD);

        StrategicAutoSolver solver = new StrategicAutoSolver();
        solver.planRound(setup);
        // Troglodyte (T1, tank-candidate): bekommt Tank-Position adjacent zum Beholder.
        Action troglodyteAction = solver.decide(troglodyte, threat, Battlefield.STANDARD);
        assertThat(troglodyteAction).isInstanceOf(Action.Move.class);
        Hex troglodyteDest = ((Action.Move) troglodyteAction).destination();
        assertThat(troglodyteDest.distanceTo(beholder.position())).isEqualTo(1);

        // Black Dragon (T7, FIRE_BREATH): kein Tank, soll chargen → MoveAndMelee/Move
        // Richtung threat, nicht Richtung Beholder.
        Action dragonAction = solver.decide(blackDragon, threat, Battlefield.STANDARD);
        Hex dragonDest = switch (dragonAction) {
            case Action.Move m -> m.destination();
            case Action.MoveAndMelee mm -> mm.destination();
            default -> throw new AssertionError("expected Move/MoveAndMelee, got " + dragonAction);
        };
        // Distance zu threat von Dragon-Position aus geringer als von Start aus.
        assertThat(dragonDest.distanceTo(threat.position()))
                .isLessThan(blackDragon.position().distanceTo(threat.position()));
    }

    @Test
    void devil_teleports_beyond_speed_to_reach_distant_target() {
        // Manual S. 99: Devils können zu jedem Hex auf dem Battlefield teleportieren —
        // unabhängig von Speed. Test: Devil auf (0, 5), Speed 11. Ziel-Pikeman auf
        // (14, 0) — Distanz 14, weiter als Speed. Ohne TELEPORT_NO_COST würde
        // findFlyerLanding einen Adjacent außerhalb der Reichweite überspringen, mit
        // Marker akzeptiert er ihn.
        Stack devil = new Stack(UnitCatalog.DEVIL, 1, new Hex(0, 5), Side.ATTACKER, 0);
        Stack pikeman = new Stack(UnitCatalog.PIKEMAN, 5, new Hex(14, 0), Side.DEFENDER, 0);
        BattleSetup setup = new BattleSetup(List.of(devil), List.of(pikeman),
                Battlefield.STANDARD);

        GreedyAutoSolver solver = new GreedyAutoSolver();
        solver.planRound(setup);
        Action action = solver.decide(devil, pikeman, Battlefield.STANDARD);

        // Devil teleportiert zu einem Adjacent des Pikeman (Distanz von Devil >11).
        assertThat(action).isInstanceOf(Action.MoveAndMelee.class);
        Action.MoveAndMelee mm = (Action.MoveAndMelee) action;
        assertThat(mm.destination().distanceTo(pikeman.position())).isEqualTo(1);
        // Lande-Hex war weiter als Devil-Speed weg.
        assertThat(devil.position().distanceTo(mm.destination())).isGreaterThan(11);
    }

    @Test
    void shooter_does_not_kite_in_multi_stack_context() {
        // Multi-Stack: Kite-Heuristik ist abgeschaltet. Setup: ein einzelner Marksman
        // gegen einen Cavalier (Speed 7, kann engagement nächste Runde forcieren) PLUS
        // einen zweiten Gegner-Stack. Im 1v1 hätte der Marksman kiten können, im
        // Multi-Stack soll er stattdessen schießen.
        Stack marksman = new Stack(UnitCatalog.MARKSMAN, 9, new Hex(0, 5), Side.ATTACKER, 0);
        // Cavalier auf adjacent-Drohung-Distanz (Distanz 7, Speed 7 → next round adjacent).
        Stack cavalier = new Stack(UnitCatalog.CAVALIER, 10, new Hex(7, 5),
                Side.DEFENDER, 0);
        Stack secondEnemy = new Stack(UnitCatalog.PIKEMAN, 30, new Hex(14, 5),
                Side.DEFENDER, 1);
        BattleSetup setup = new BattleSetup(List.of(marksman),
                List.of(cavalier, secondEnemy), Battlefield.STANDARD);

        GreedyAutoSolver solver = new GreedyAutoSolver();
        solver.planRound(setup);
        Action action = solver.decide(marksman, cavalier, Battlefield.STANDARD);

        // Im Multi-Stack-Kontext: Shoot statt Move (Kite).
        assertThat(action).isInstanceOf(Action.Shoot.class);
    }
}
