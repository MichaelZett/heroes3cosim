package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class StrategicAutoSolverTest {

    private final StrategicAutoSolver solver = new StrategicAutoSolver();
    private final Battlefield battlefield = Battlefield.STANDARD;

    @Test
    void ranged_dominant_side_protects_shooters_via_stance() {
        // Attacker: 1 Halberdier (Tank) + 1 Marksman + 1 Zealot (zwei Schützen → Ranged-Power).
        // Defender: 1 Pikeman (Melee only).
        Stack hal = new Stack(UnitCatalog.HALBERDIER, 14, new Hex(0, 4), Side.ATTACKER, 0);
        Stack mark = new Stack(UnitCatalog.MARKSMAN, 9, new Hex(0, 5), Side.ATTACKER, 1);
        Stack zealot = new Stack(UnitCatalog.ZEALOT, 3, new Hex(0, 6), Side.ATTACKER, 2);
        Stack pikeman = new Stack(UnitCatalog.PIKEMAN, 14, new Hex(14, 5), Side.DEFENDER, 0);
        BattleSetup setup = new BattleSetup(List.of(hal, mark, zealot), List.of(pikeman), battlefield);

        solver.planRound(setup);

        assertThat(solver.currentPlan().stanceOf(Side.ATTACKER)).isEqualTo(TeamStance.RANGED_DOMINANT);
        assertThat(solver.currentPlan().protectedShooters()).containsExactlyInAnyOrder(mark, zealot);
    }

    @Test
    void melee_dominant_side_charges_via_stance() {
        // Attacker: 3 Melee-Stacks ohne Schützen. Defender: 1 schwacher Schütze.
        Stack hal = new Stack(UnitCatalog.HALBERDIER, 14, new Hex(0, 4), Side.ATTACKER, 0);
        Stack crusader = new Stack(UnitCatalog.CRUSADER, 4, new Hex(0, 5), Side.ATTACKER, 1);
        Stack archAngel = new Stack(UnitCatalog.ARCH_ANGEL, 1, new Hex(0, 6), Side.ATTACKER, 2);
        Stack marksman = new Stack(UnitCatalog.MARKSMAN, 1, new Hex(14, 5), Side.DEFENDER, 0);
        BattleSetup setup = new BattleSetup(List.of(hal, crusader, archAngel), List.of(marksman), battlefield);

        solver.planRound(setup);

        assertThat(solver.currentPlan().stanceOf(Side.ATTACKER)).isEqualTo(TeamStance.MELEE_DOMINANT);
        // Defender mit einsamem Marksman ist seinerseits RANGED_DOMINANT — der Set enthält ihn.
        // Wichtig ist nur: Attacker hat keine Schützen drin (er chargt geschlossen).
        assertThat(solver.currentPlan().protectedShooters())
                .noneMatch(s -> s.side() == Side.ATTACKER);
    }

    @Test
    void focus_target_picks_highest_threat_enemy() {
        // Defender hat zwei Stacks: schwache Pikemen + harte Arch Angels. Focus muss Arch Angels sein.
        Stack hal = new Stack(UnitCatalog.HALBERDIER, 14, new Hex(0, 5), Side.ATTACKER, 0);
        Stack pikemen = new Stack(UnitCatalog.PIKEMAN, 14, new Hex(14, 3), Side.DEFENDER, 0);
        Stack angels = new Stack(UnitCatalog.ARCH_ANGEL, 1, new Hex(14, 7), Side.DEFENDER, 1);
        BattleSetup setup = new BattleSetup(List.of(hal), List.of(pikemen, angels), battlefield);

        solver.planRound(setup);

        assertThat(solver.currentPlan().focusOf(Side.ATTACKER)).isSameAs(angels);
    }

    @Test
    void aoe_shooter_picks_target_with_most_adjacent_enemies() {
        // Magog auf Distanz; zwei Pikemen-Stacks rechts isoliert, eine Dreier-Gruppe links eng.
        Stack magog = new Stack(UnitCatalog.MAGOG, 10, new Hex(0, 5), Side.ATTACKER, 0);
        Stack isolated = new Stack(UnitCatalog.PIKEMAN, 50, new Hex(10, 0), Side.DEFENDER, 0);
        Stack center = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(10, 5), Side.DEFENDER, 1);
        Stack near1 = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(10, 4), Side.DEFENDER, 2);
        Stack near2 = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(10, 6), Side.DEFENDER, 3);
        BattleSetup setup = new BattleSetup(
                List.of(magog),
                List.of(isolated, center, near1, near2),
                battlefield);
        solver.planRound(setup);

        Stack target = solver.pickTarget(magog, setup.opponentsOf(magog), battlefield);

        // center hat 2 adjazente Verbündete → soll von Splash-Shot bevorzugt werden.
        assertThat(target).isSameAs(center);
    }

    @Test
    void fire_breath_picks_inline_pair() {
        // Black Dragon auf (5,5). Pikemen direkt davor auf (6,5) + Inferno-Goblin auf (7,5) inline dahinter.
        Stack dragon = new Stack(UnitCatalog.BLACK_DRAGON, 1, new Hex(5, 5), Side.ATTACKER, 0);
        Stack inlinePair = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(6, 5), Side.DEFENDER, 0);
        Stack inlineBehind = new Stack(UnitCatalog.GOBLIN, 50, new Hex(7, 5), Side.DEFENDER, 1);
        Stack isolated = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(4, 3), Side.DEFENDER, 2);
        BattleSetup setup = new BattleSetup(
                List.of(dragon),
                List.of(inlinePair, inlineBehind, isolated),
                battlefield);
        solver.planRound(setup);

        Stack target = solver.pickTarget(dragon, setup.opponentsOf(dragon), battlefield);

        assertThat(target).isSameAs(inlinePair);
    }

    @Test
    void tank_defends_when_already_adjacent_to_protected_shooter() {
        // Marksman auf (0, 0), Halberdier (Tank) steht bereits adjacent auf (1, 0).
        // Stance RANGED_DOMINANT → hasTankDuty=true. Erwartung: Defend (+30 % Defense)
        // statt redundantem Move zu einer anderen Adjacent-Position des Schützen.
        Stack mark = new Stack(UnitCatalog.MARKSMAN, 9, new Hex(0, 0), Side.ATTACKER, 0);
        Stack hal = new Stack(UnitCatalog.HALBERDIER, 14, new Hex(1, 0), Side.ATTACKER, 1);
        Stack threat = new Stack(UnitCatalog.PIKEMAN, 14, new Hex(10, 5), Side.DEFENDER, 0);
        BattleSetup setup = new BattleSetup(List.of(mark, hal), List.of(threat), battlefield);
        solver.planRound(setup);
        assertThat(solver.currentPlan().stanceOf(Side.ATTACKER)).isEqualTo(TeamStance.RANGED_DOMINANT);

        Action action = solver.decide(hal, threat, battlefield);

        assertThat(action).isInstanceOf(Action.Defend.class);
    }

    @Test
    void ranged_dominant_melee_tank_moves_towards_shooter_lane() {
        // Attacker: Marksman (Schütze) hinten + Halberdier (Tank, kann sich bewegen).
        // Defender: schneller Pikeman, der den Marksman bedroht.
        Stack hal = new Stack(UnitCatalog.HALBERDIER, 14, new Hex(3, 5), Side.ATTACKER, 0);
        Stack mark = new Stack(UnitCatalog.MARKSMAN, 9, new Hex(0, 5), Side.ATTACKER, 1);
        Stack zealot = new Stack(UnitCatalog.ZEALOT, 3, new Hex(0, 6), Side.ATTACKER, 2);
        Stack threat = new Stack(UnitCatalog.PIKEMAN, 14, new Hex(8, 5), Side.DEFENDER, 0);
        BattleSetup setup = new BattleSetup(List.of(hal, mark, zealot), List.of(threat), battlefield);
        solver.planRound(setup);

        Action action = solver.decide(hal, threat, battlefield);

        // Tank soll sich bewegen statt direkt anzugreifen — Move-Aktion, kein MoveAndMelee.
        assertThat(action).isInstanceOf(Action.Move.class);
    }

    @Test
    void balanced_stance_when_neither_side_dominates() {
        // Spiegel-Armeen: identische Schützen+Tank-Mischung beider Seiten.
        // Keiner überschreitet DOMINANCE_THRESHOLD (1.2) → beide BALANCED.
        Stack halA = new Stack(UnitCatalog.HALBERDIER, 14, new Hex(0, 4), Side.ATTACKER, 0);
        Stack markA = new Stack(UnitCatalog.MARKSMAN, 9, new Hex(0, 5), Side.ATTACKER, 1);
        Stack halD = new Stack(UnitCatalog.HALBERDIER, 14, new Hex(14, 4), Side.DEFENDER, 0);
        Stack markD = new Stack(UnitCatalog.MARKSMAN, 9, new Hex(14, 5), Side.DEFENDER, 1);
        BattleSetup setup = new BattleSetup(List.of(halA, markA), List.of(halD, markD), battlefield);

        solver.planRound(setup);

        assertThat(solver.currentPlan().stanceOf(Side.ATTACKER)).isEqualTo(TeamStance.BALANCED);
        assertThat(solver.currentPlan().stanceOf(Side.DEFENDER)).isEqualTo(TeamStance.BALANCED);
        // Tank-Pattern greift nur bei RANGED_DOMINANT → bei BALANCED kein Schutzauftrag.
        assertThat(solver.currentPlan().protectedShooters()).isEmpty();
    }

    @Test
    void stale_focus_target_falls_back_to_greedy_when_dead() {
        // Defender: schwache Pikemen + harte Arch Angels → Focus = Arch Angels.
        // Wir töten die Arch Angels zwischen planRound() und pickTarget(): der Solver darf
        // keinen toten Stack zurückgeben, sondern muss auf den lebenden Pikeman fallen.
        Stack hal = new Stack(UnitCatalog.HALBERDIER, 14, new Hex(0, 5), Side.ATTACKER, 0);
        Stack pikemen = new Stack(UnitCatalog.PIKEMAN, 14, new Hex(14, 3), Side.DEFENDER, 0);
        Stack angels = new Stack(UnitCatalog.ARCH_ANGEL, 1, new Hex(14, 7), Side.DEFENDER, 1);
        BattleSetup setup = new BattleSetup(List.of(hal), List.of(pikemen, angels), battlefield);
        solver.planRound(setup);
        assertThat(solver.currentPlan().focusOf(Side.ATTACKER)).isSameAs(angels);

        angels.loseTopCreatures(1);

        Stack target = solver.pickTarget(hal, setup.opponentsOf(hal), battlefield);
        assertThat(target).isSameAs(pikemen);
    }

    @Test
    void aoe_shooter_without_cluster_falls_back_to_danger_pick() {
        // Magog mit drei isolierten Gegnern (alle adjazenten Allies = 0) → AoE-Pick liefert null.
        // Greedy-Fallback: gefährlichster Stack = avgDmg × count, hier der große Pikeman-Stack.
        Stack magog = new Stack(UnitCatalog.MAGOG, 10, new Hex(0, 5), Side.ATTACKER, 0);
        Stack weak = new Stack(UnitCatalog.PIKEMAN, 5, new Hex(10, 0), Side.DEFENDER, 0);
        Stack heavy = new Stack(UnitCatalog.PIKEMAN, 60, new Hex(10, 5), Side.DEFENDER, 1);
        Stack medium = new Stack(UnitCatalog.PIKEMAN, 20, new Hex(10, 10), Side.DEFENDER, 2);
        BattleSetup setup = new BattleSetup(List.of(magog), List.of(weak, heavy, medium), battlefield);
        solver.planRound(setup);

        Stack target = solver.pickTarget(magog, setup.opponentsOf(magog), battlefield);

        assertThat(target).isSameAs(heavy);
    }

    @Test
    void tank_pattern_skips_blocked_preferred_spot() {
        // Marksman (einziger Schütze → unstrittig "most threatened") auf (3,5),
        // Threat auf (10,5) → preferred Tank-Spot = (4,5).
        // Swordsman als Melee-Blocker auf (4,5) — Tank-Halberdier muss auf einen anderen
        // Marksman-Nachbarn ausweichen. (Blocker MUSS Melee sein, sonst tritt er selbst
        // als Schütze in die Threat-Auswahl und verfälscht die Tank-Position.)
        Stack hal = new Stack(UnitCatalog.HALBERDIER, 14, new Hex(0, 5), Side.ATTACKER, 0);
        Stack mark = new Stack(UnitCatalog.MARKSMAN, 9, new Hex(3, 5), Side.ATTACKER, 1);
        Stack blocker = new Stack(UnitCatalog.SWORDSMAN, 10, new Hex(4, 5), Side.ATTACKER, 2);
        Stack threat = new Stack(UnitCatalog.PIKEMAN, 14, new Hex(10, 5), Side.DEFENDER, 0);
        BattleSetup setup = new BattleSetup(List.of(hal, mark, blocker), List.of(threat), battlefield);
        solver.planRound(setup);

        Action action = solver.decide(hal, threat, battlefield);

        assertThat(action).isInstanceOf(Action.Move.class);
        Hex destination = ((Action.Move) action).destination();
        assertThat(destination).isNotEqualTo(new Hex(4, 5));
        assertThat(destination).isNotEqualTo(blocker.position());
        // Ziel muss adjazent zum geschützten Marksman sein (Tank-Position).
        assertThat(destination.distanceTo(mark.position())).isEqualTo(1);
    }

    @Test
    void tank_pattern_without_alive_shooters_delegates_to_greedy_charge() {
        // RANGED_DOMINANT Plan; danach stirbt der einzige Schütze → myShooters leer →
        // findTankPosition liefert null → Greedy übernimmt und zieht Richtung Gegner.
        Stack hal = new Stack(UnitCatalog.HALBERDIER, 14, new Hex(3, 5), Side.ATTACKER, 0);
        Stack mark = new Stack(UnitCatalog.MARKSMAN, 9, new Hex(0, 5), Side.ATTACKER, 1);
        Stack threat = new Stack(UnitCatalog.PIKEMAN, 14, new Hex(10, 5), Side.DEFENDER, 0);
        BattleSetup setup = new BattleSetup(List.of(hal, mark), List.of(threat), battlefield);
        solver.planRound(setup);
        assertThat(solver.currentPlan().stanceOf(Side.ATTACKER)).isEqualTo(TeamStance.RANGED_DOMINANT);

        mark.loseTopCreatures(mark.getCount());

        Action action = solver.decide(hal, threat, battlefield);

        // Greedy bewegt sich Richtung Pikeman (östlich), nicht Richtung Marksman-Lane (westlich).
        Hex destination = switch (action) {
            case Action.Move m -> m.destination();
            case Action.MoveAndMelee mm -> mm.destination();
            default -> throw new AssertionError("expected Move/MoveAndMelee, got " + action);
        };
        assertThat(destination.distanceTo(threat.position()))
                .isLessThan(hal.position().distanceTo(threat.position()));
    }

    @Test
    void plan_round_refreshes_setup_and_focus() {
        // Erst Setup A → Focus = Pikemen (einziger Feind, Attacker BALANCED).
        Stack halA = new Stack(UnitCatalog.HALBERDIER, 14, new Hex(0, 5), Side.ATTACKER, 0);
        Stack pikemenA = new Stack(UnitCatalog.PIKEMAN, 14, new Hex(14, 5), Side.DEFENDER, 0);
        BattleSetup setupA = new BattleSetup(List.of(halA), List.of(pikemenA), battlefield);
        solver.planRound(setupA);
        assertThat(solver.currentPlan().focusOf(Side.ATTACKER)).isSameAs(pikemenA);

        // Setup B → andere Stacks, andere Stance (Marksman → RANGED_DOMINANT für Attacker).
        Stack markB = new Stack(UnitCatalog.MARKSMAN, 9, new Hex(0, 5), Side.ATTACKER, 0);
        Stack pikemenB = new Stack(UnitCatalog.PIKEMAN, 14, new Hex(14, 5), Side.DEFENDER, 0);
        BattleSetup setupB = new BattleSetup(List.of(markB), List.of(pikemenB), battlefield);
        solver.planRound(setupB);

        assertThat(solver.currentPlan().stanceOf(Side.ATTACKER)).isEqualTo(TeamStance.RANGED_DOMINANT);
        // Focus zeigt auf den NEUEN Pikeman, nicht den aus Setup A.
        assertThat(solver.currentPlan().focusOf(Side.ATTACKER)).isSameAs(pikemenB);
        assertThat(solver.currentPlan().focusOf(Side.ATTACKER)).isNotSameAs(pikemenA);
    }

    @Test
    void focus_score_ability_bonus_outweighs_higher_raw_damage() {
        // Pikeman 28 → roher Threat 56 (avgDmg 2 × 28), kein Special-Bonus.
        // Vampire 5  → roher Threat 32.5 (avgDmg 6.5 × 5) + 30 (NO_RETALIATION) = 62.5.
        // Trotz ~40 % weniger rohem Damage gewinnt der Vampire wegen Ability-Gewichtung.
        Stack hal = new Stack(UnitCatalog.HALBERDIER, 14, new Hex(0, 5), Side.ATTACKER, 0);
        Stack pikemen = new Stack(UnitCatalog.PIKEMAN, 28, new Hex(14, 3), Side.DEFENDER, 0);
        Stack vampires = new Stack(UnitCatalog.VAMPIRE, 5, new Hex(14, 7), Side.DEFENDER, 1);
        BattleSetup setup = new BattleSetup(List.of(hal), List.of(pikemen, vampires), battlefield);

        solver.planRound(setup);

        assertThat(solver.currentPlan().focusOf(Side.ATTACKER)).isSameAs(vampires);
    }

    @Test
    void two_tanks_form_complete_wall_in_front_of_corner_shooter() {
        // Marksman im Eck (0,0) — strukturell nur 2 in-board Nachbarn: (1,0) und (0,1).
        // 2 Tanks sollten beide adjacenten Hexen besetzen → komplette Wall.
        // (Stance ist RANGED_DOMINANT, weil der einzige Schütze auf Attacker-Seite steht;
        // hier verifizieren wir nur, dass die bestehende Tank-Heuristik tatsächlich beide
        // Tanks koordiniert und nicht beide auf denselben preferred Spot schickt.)
        Stack mark = new Stack(UnitCatalog.MARKSMAN, 9, new Hex(0, 0), Side.ATTACKER, 0);
        Stack hal = new Stack(UnitCatalog.HALBERDIER, 14, new Hex(0, 2), Side.ATTACKER, 1);
        Stack sword = new Stack(UnitCatalog.SWORDSMAN, 10, new Hex(0, 4), Side.ATTACKER, 2);
        Stack threat = new Stack(UnitCatalog.PIKEMAN, 14, new Hex(8, 5), Side.DEFENDER, 0);
        BattleSetup setup = new BattleSetup(List.of(mark, hal, sword), List.of(threat), battlefield);
        solver.planRound(setup);
        assertThat(solver.currentPlan().stanceOf(Side.ATTACKER)).isEqualTo(TeamStance.RANGED_DOMINANT);

        // Tank A entscheidet, bewegt sich → blockiert seinen Hex für Tank B.
        Action actA = solver.decide(hal, threat, battlefield);
        Hex destA = ((Action.Move) actA).destination();
        hal.moveTo(destA);

        Action actB = solver.decide(sword, threat, battlefield);
        Hex destB = ((Action.Move) actB).destination();

        assertThat(destA.distanceTo(mark.position())).isEqualTo(1);
        assertThat(destB.distanceTo(mark.position())).isEqualTo(1);
        assertThat(destA).isNotEqualTo(destB);
        // Vollständige Wall: kein in-board-Nachbar des Schützen ist mehr frei.
        Set<Hex> stillFreeAdjacents = mark.position().neighbors().stream()
                .filter(battlefield::isPassable)
                .filter(h -> !h.equals(destA) && !h.equals(destB))
                .collect(Collectors.toSet());
        assertThat(stillFreeAdjacents).isEmpty();
    }

    @Test
    void balanced_stance_still_protects_corner_shooter_via_tank_duty() {
        // Spiegel-Setup: beide Seiten haben Marksman + 2 Tanks → BALANCED-Stance.
        // Trotz BALANCED soll der Tank den Eck-Schützen (r=0) decken, weil dieser strukturell
        // verwundbar ist (nur 2 in-board Nachbarn). Trigger: RoundPlan.hasTankDuty.
        Stack mark = new Stack(UnitCatalog.MARKSMAN, 9, new Hex(0, 0), Side.ATTACKER, 0);
        Stack hal = new Stack(UnitCatalog.HALBERDIER, 14, new Hex(0, 2), Side.ATTACKER, 1);
        Stack sword = new Stack(UnitCatalog.SWORDSMAN, 10, new Hex(0, 4), Side.ATTACKER, 2);
        Stack enemyMark = new Stack(UnitCatalog.MARKSMAN, 9, new Hex(14, 0), Side.DEFENDER, 0);
        Stack enemyHal = new Stack(UnitCatalog.HALBERDIER, 14, new Hex(14, 2), Side.DEFENDER, 1);
        Stack enemySword = new Stack(UnitCatalog.SWORDSMAN, 10, new Hex(14, 4), Side.DEFENDER, 2);
        BattleSetup setup = new BattleSetup(
                List.of(mark, hal, sword),
                List.of(enemyMark, enemyHal, enemySword),
                battlefield);
        solver.planRound(setup);
        assertThat(solver.currentPlan().stanceOf(Side.ATTACKER)).isEqualTo(TeamStance.BALANCED);
        assertThat(solver.currentPlan().hasTankDuty(Side.ATTACKER)).isTrue();
        assertThat(solver.currentPlan().protectedShooters()).contains(mark);

        Action actA = solver.decide(hal, enemyHal, battlefield);
        Hex destA = ((Action.Move) actA).destination();
        hal.moveTo(destA);

        Action actB = solver.decide(sword, enemyHal, battlefield);
        Hex destB = ((Action.Move) actB).destination();

        // Beide Tanks adjacent zum Eck-Marksman → komplette Wall (Eck hat nur 2 Adjacents).
        assertThat(destA.distanceTo(mark.position())).isEqualTo(1);
        assertThat(destB.distanceTo(mark.position())).isEqualTo(1);
        assertThat(destA).isNotEqualTo(destB);
    }

    @Test
    void melee_dominant_side_still_charges_even_with_edge_shooter() {
        // Sicherheitstest: bei MELEE_DOMINANT bleibt Charge-Modus, auch wenn ein Schütze
        // zufällig am Rand sitzt. Sonst würden Tanks aus dem Sturm abgezogen.
        // Defender hat starke Schützen-Power, damit Attacker trotz seines Mini-Archers nicht
        // selbst RANGED_DOMINANT kippt — Attacker bleibt klar MELEE_DOMINANT.
        Stack hal = new Stack(UnitCatalog.HALBERDIER, 14, new Hex(0, 4), Side.ATTACKER, 0);
        Stack crusader = new Stack(UnitCatalog.CRUSADER, 4, new Hex(0, 5), Side.ATTACKER, 1);
        Stack archAngel = new Stack(UnitCatalog.ARCH_ANGEL, 1, new Hex(0, 6), Side.ATTACKER, 2);
        Stack weakArcher = new Stack(UnitCatalog.ARCHER, 1, new Hex(0, 0), Side.ATTACKER, 3);
        Stack threat = new Stack(UnitCatalog.PIKEMAN, 14, new Hex(14, 5), Side.DEFENDER, 0);
        Stack enemyShooters = new Stack(UnitCatalog.MARKSMAN, 14, new Hex(14, 4), Side.DEFENDER, 1);
        BattleSetup setup = new BattleSetup(
                List.of(hal, crusader, archAngel, weakArcher),
                List.of(threat, enemyShooters), battlefield);
        solver.planRound(setup);
        assertThat(solver.currentPlan().stanceOf(Side.ATTACKER)).isEqualTo(TeamStance.MELEE_DOMINANT);
        assertThat(solver.currentPlan().hasTankDuty(Side.ATTACKER)).isFalse();

        Action act = solver.decide(hal, threat, battlefield);

        Hex dest = switch (act) {
            case Action.Move m -> m.destination();
            case Action.MoveAndMelee mm -> mm.destination();
            default -> throw new AssertionError("expected Move/MoveAndMelee, got " + act);
        };
        // Charge: Tank bewegt sich Richtung Gegner, nicht zum schwachen Eck-Archer.
        assertThat(dest.distanceTo(threat.position()))
                .isLessThan(hal.position().distanceTo(threat.position()));
    }

    @Test
    void flyer_pickTarget_prioritizes_unguarded_enemy_shooter() {
        // Black Dragon (Flieger, speed 15) gegen Defender mit Marksman (T2 ranged) + Swordsman
        // (T4 melee). Standard-Focus-Score gibt Swordsman höhere Priorität (mehr Rohschaden).
        // Mit Flieger-Heuristik: Dragon priorisiert Marksman, weil dieser ungeschützt ist
        // (Adjacents frei + in Reichweite). Sonst chargt der Dragon den Tank und der Schütze
        // schießt ungestört weiter.
        Stack dragon = new Stack(UnitCatalog.BLACK_DRAGON, 1, new Hex(0, 5), Side.ATTACKER, 0);
        Stack marksman = new Stack(UnitCatalog.MARKSMAN, 5, new Hex(14, 0), Side.DEFENDER, 0);
        Stack swordsman = new Stack(UnitCatalog.SWORDSMAN, 10, new Hex(14, 5), Side.DEFENDER, 1);
        BattleSetup setup = new BattleSetup(List.of(dragon), List.of(marksman, swordsman),
                battlefield);
        solver.planRound(setup);

        Stack target = solver.pickTarget(dragon, setup.opponentsOf(dragon), battlefield);

        assertThat(target).isSameAs(marksman);
    }

    @Test
    void flyer_pickTarget_falls_back_when_shooter_is_fully_guarded() {
        // Marksman im Eck, Tank-Wall vor ihm (alle in-board Adjacents besetzt von Defendern).
        // Flieger-Heuristik findet keinen freien Lande-Hex → fallback zum default Focus
        // (hier: Swordsman wegen höherem Rohschaden).
        Stack dragon = new Stack(UnitCatalog.BLACK_DRAGON, 1, new Hex(0, 5), Side.ATTACKER, 0);
        Stack marksman = new Stack(UnitCatalog.MARKSMAN, 5, new Hex(14, 0), Side.DEFENDER, 0);
        Stack tank1 = new Stack(UnitCatalog.HALBERDIER, 14, new Hex(13, 0), Side.DEFENDER, 1);
        Stack tank2 = new Stack(UnitCatalog.HALBERDIER, 14, new Hex(13, 1), Side.DEFENDER, 2);
        Stack tank3 = new Stack(UnitCatalog.HALBERDIER, 14, new Hex(14, 1), Side.DEFENDER, 3);
        Stack swordsman = new Stack(UnitCatalog.SWORDSMAN, 10, new Hex(8, 5), Side.DEFENDER, 4);
        BattleSetup setup = new BattleSetup(List.of(dragon),
                List.of(marksman, tank1, tank2, tank3, swordsman), battlefield);
        solver.planRound(setup);

        Stack target = solver.pickTarget(dragon, setup.opponentsOf(dragon), battlefield);

        // Marksman komplett umstellt → Heuristik überspringt ihn → Default-Focus
        // (Swordsman, weil höchster threat-Score auf der Defender-Seite ohne Marksman).
        assertThat(target).isNotSameAs(marksman);
    }

    @Test
    void flyer_picks_free_adjacent_when_straight_line_to_shooter_is_blocked() {
        // Black Dragon (ATTACKER, FLYING, speed 15) gegen einen Eck-Schützen (Marksman auf
        // (14,0)). Defender baut Tank-Wall vor dem Schützen — Halberdier auf (13,0),
        // Swordsman auf (13,1). Nur (14,1) bleibt als freier Adjacent.
        //
        // Heute ohne Flieger-Heuristik: Greedy.moveToward straight-line landet auf (13,0),
        // Engine erkennt Belegung → Wait. Mit Heuristik: findFlyerLanding wählt (14,1).
        Stack dragon = new Stack(UnitCatalog.BLACK_DRAGON, 1, new Hex(0, 5), Side.ATTACKER, 0);
        Stack tank1 = new Stack(UnitCatalog.HALBERDIER, 14, new Hex(13, 0), Side.DEFENDER, 0);
        Stack tank2 = new Stack(UnitCatalog.SWORDSMAN, 10, new Hex(13, 1), Side.DEFENDER, 1);
        Stack marksman = new Stack(UnitCatalog.MARKSMAN, 9, new Hex(14, 0), Side.DEFENDER, 2);
        BattleSetup setup = new BattleSetup(List.of(dragon),
                List.of(tank1, tank2, marksman), battlefield);
        solver.planRound(setup);

        Action act = solver.decide(dragon, marksman, battlefield);

        assertThat(act).isInstanceOf(Action.MoveAndMelee.class);
        Action.MoveAndMelee mm = (Action.MoveAndMelee) act;
        assertThat(mm.destination()).isEqualTo(new Hex(14, 1));
        assertThat(mm.target()).isSameAs(marksman);
    }

    @Test
    void symmetric_shooter_armies_stay_balanced_with_no_protect_orders() {
        // Beide Seiten haben identische Ranged-Power. Mathematisch unmöglich, dass beide
        // gleichzeitig RANGED_DOMINANT sind (würde A > A × 1.44 erfordern) → BALANCED beide
        // → keine Tank-Aufträge, weil niemand klar dominiert.
        Stack markA = new Stack(UnitCatalog.MARKSMAN, 9, new Hex(0, 4), Side.ATTACKER, 0);
        Stack zealotA = new Stack(UnitCatalog.ZEALOT, 3, new Hex(0, 6), Side.ATTACKER, 1);
        Stack markD = new Stack(UnitCatalog.MARKSMAN, 9, new Hex(14, 4), Side.DEFENDER, 0);
        Stack zealotD = new Stack(UnitCatalog.ZEALOT, 3, new Hex(14, 6), Side.DEFENDER, 1);
        BattleSetup setup = new BattleSetup(List.of(markA, zealotA), List.of(markD, zealotD), battlefield);

        solver.planRound(setup);

        assertThat(solver.currentPlan().stanceOf(Side.ATTACKER)).isEqualTo(TeamStance.BALANCED);
        assertThat(solver.currentPlan().stanceOf(Side.DEFENDER)).isEqualTo(TeamStance.BALANCED);
        assertThat(solver.currentPlan().protectedShooters()).isEmpty();
    }
}
