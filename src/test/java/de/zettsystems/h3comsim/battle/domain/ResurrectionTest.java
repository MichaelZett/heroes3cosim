package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.BattleEvent;
import de.zettsystems.h3comsim.battle.domain.events.ListEventCollector;
import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Resurrection des Arch Angels. Manual S. 87: „the archangel is able to resurrect dead allied
 * troops once per combat". Die Menge — 100 HP je Arch Angel — stammt aus heroes.thelazy.net,
 * das Manual nennt keine.
 *
 * <p>Die Engine-Tests stellen die Armeen außer Reichweite und lassen einen Skript-Solver nur
 * Resurrect und Defend liefern. So passiert kein Schaden, und die Schlacht läuft ins
 * {@code NO_PROGRESS_LIMIT}.
 */
class ResurrectionTest {

    private static Stack ally(Unit unit, int count, int slot) {
        return new Stack(unit, count, new Hex(0, 1 + 2 * slot), Side.ATTACKER, slot);
    }

    private static Stack enemy() {
        return new Stack(UnitCatalog.PIKEMAN, 10, new Hex(14, 5), Side.DEFENDER, 0);
    }

    /** Arch Angel versucht in jeder Aktion die Resurrection auf {@code target}, alle anderen verteidigen. */
    // Stack ist eine mutable Entity ohne equals — der ==-Vergleich meint bewusst Identität.
    @SuppressWarnings("ReferenceEquality")
    private static ListEventCollector run(BattleSetup setup, Stack caster, Stack target) {
        ListEventCollector collector = new ListEventCollector();
        AutoSolver solver = (active, opponent, bf) ->
                active == caster ? new Action.Resurrect(target) : new Action.Defend();
        new Battle(new Random(1L), solver, collector).simulate(setup);
        return collector;
    }

    private static List<BattleEvent.Resurrection> resurrections(ListEventCollector collector) {
        return collector.events().stream()
                .filter(BattleEvent.Resurrection.class::isInstance)
                .map(BattleEvent.Resurrection.class::cast)
                .toList();
    }

    @Test
    void one_arch_angel_brings_back_100_health() {
        Stack angel = ally(UnitCatalog.ARCH_ANGEL, 1, 0);
        Stack pikemen = ally(UnitCatalog.PIKEMAN, 20, 1);
        pikemen.loseTopCreatures(15);
        BattleSetup setup = new BattleSetup(List.of(angel, pikemen), List.of(enemy()), Battlefield.STANDARD);

        ListEventCollector collector = run(setup, angel, pikemen);

        // Pikeman hat 10 HP: 100 HP sind zehn Kreaturen, von 5 zurück auf 15.
        assertThat(pikemen.getCount()).isEqualTo(15);
        assertThat(resurrections(collector)).singleElement()
                .satisfies(r -> {
                    assertThat(r.restoredCount()).isEqualTo(10);
                    assertThat(r.targetSlot()).isEqualTo(pikemen.slot());
                    assertThat(r.targetAfter().count()).isEqualTo(15);
                });
    }

    @Test
    void the_amount_scales_with_the_angels_but_stops_at_the_starting_strength() {
        Stack angels = ally(UnitCatalog.ARCH_ANGEL, 3, 0);
        Stack pikemen = ally(UnitCatalog.PIKEMAN, 20, 1);
        pikemen.loseTopCreatures(15);
        BattleSetup setup = new BattleSetup(List.of(angels, pikemen), List.of(enemy()), Battlefield.STANDARD);

        run(setup, angels, pikemen);

        // 300 HP würden 30 Pikemen zurückholen, es fehlen aber nur 15.
        assertThat(pikemen.getCount()).isEqualTo(20);
    }

    @Test
    void the_angel_resurrects_only_once_per_combat() {
        Stack angel = ally(UnitCatalog.ARCH_ANGEL, 1, 0);
        Stack pikemen = ally(UnitCatalog.PIKEMAN, 40, 1);
        pikemen.loseTopCreatures(30);
        BattleSetup setup = new BattleSetup(List.of(angel, pikemen), List.of(enemy()), Battlefield.STANDARD);

        ListEventCollector collector = run(setup, angel, pikemen);

        assertThat(resurrections(collector)).hasSize(1);
        assertThat(pikemen.getCount()).isEqualTo(20);
        // Jeder weitere Versuch fällt auf Defend zurück, statt die Aktion zu verschenken.
        assertThat(collector.events().stream()
                .filter(BattleEvent.Defend.class::isInstance)
                .map(BattleEvent.Defend.class::cast)
                .anyMatch(d -> d.actor() == Side.ATTACKER && d.actorSlot() == angel.slot()))
                .isTrue();
    }

    @Test
    void undead_allies_cannot_be_resurrected() {
        Stack angel = ally(UnitCatalog.ARCH_ANGEL, 1, 0);
        Stack skeletons = ally(UnitCatalog.SKELETON, 20, 1);
        skeletons.loseTopCreatures(10);
        BattleSetup setup = new BattleSetup(List.of(angel, skeletons), List.of(enemy()), Battlefield.STANDARD);

        ListEventCollector collector = run(setup, angel, skeletons);

        assertThat(resurrections(collector)).isEmpty();
        assertThat(skeletons.getCount()).isEqualTo(10);
        assertThat(angel.canResurrect()).as("ein abgewiesener Versuch verbraucht den Wurf nicht").isTrue();
    }

    @Test
    void enemies_and_the_angel_itself_are_no_valid_targets() {
        Stack angels = ally(UnitCatalog.ARCH_ANGEL, 3, 0);
        angels.loseTopCreatures(1);
        Stack foe = enemy();
        foe.loseTopCreatures(5);

        BattleSetup setup = new BattleSetup(List.of(angels), List.of(foe), Battlefield.STANDARD);

        assertThat(Resurrection.isValidTarget(angels, angels, setup)).isFalse();
        assertThat(Resurrection.isValidTarget(angels, foe, setup)).isFalse();
    }

    @Test
    void a_fallen_stack_rises_fresh_when_its_field_is_free() {
        Stack angel = ally(UnitCatalog.ARCH_ANGEL, 2, 0);
        Stack pikemen = new Stack(UnitCatalog.PIKEMAN, 30, new Hex(1, 3), Side.ATTACKER, 1);
        pikemen.applySpell(Spell.SLOW, SkillLevel.ADVANCED, 10);
        pikemen.takeDamage(10_000, java.util.Set.of());
        BattleSetup setup = new BattleSetup(List.of(angel, pikemen), List.of(enemy()), Battlefield.STANDARD);
        assertThat(Resurrection.isValidTarget(angel, pikemen, setup)).isTrue();

        ListEventCollector collector = run(setup, angel, pikemen);

        assertThat(resurrections(collector)).singleElement()
                .satisfies(r -> assertThat(r.restoredCount()).isEqualTo(20));
        assertThat(pikemen.getCount()).as("200 HP für Pikemen zu 10 HP").isEqualTo(20);
        assertThat(pikemen.hasSpell(Spell.SLOW)).as("zurück ohne Zauber").isFalse();
    }

    @Test
    void a_fallen_stack_under_another_stack_stays_dead() {
        Stack angel = ally(UnitCatalog.ARCH_ANGEL, 2, 0);
        Stack pikemen = new Stack(UnitCatalog.PIKEMAN, 30, new Hex(1, 3), Side.ATTACKER, 1);
        pikemen.takeDamage(10_000, java.util.Set.of());
        Stack standing = new Stack(UnitCatalog.SWORDSMAN, 5, pikemen.position(), Side.ATTACKER, 2);
        BattleSetup setup = new BattleSetup(List.of(angel, pikemen, standing), List.of(enemy()),
                Battlefield.STANDARD);

        assertThat(Resurrection.isValidTarget(angel, pikemen, setup)).isFalse();
    }

    @Test
    void lifeless_and_spell_immune_creatures_stay_dead() {
        for (Unit unit : List.of(UnitCatalog.SKELETON, UnitCatalog.FIRE_ELEMENTAL,
                UnitCatalog.STONE_GOLEM, UnitCatalog.IRON_GOLEM, UnitCatalog.STONE_GARGOYLE,
                UnitCatalog.OBSIDIAN_GARGOYLE, UnitCatalog.BLACK_DRAGON, UnitCatalog.GOLD_DRAGON)) {
            assertThat(unit.isResurrectable()).as(unit.name()).isFalse();
        }
        for (Unit unit : List.of(UnitCatalog.PIKEMAN, UnitCatalog.ANGEL, UnitCatalog.RED_DRAGON,
                UnitCatalog.PHOENIX, UnitCatalog.GREMLIN)) {
            assertThat(unit.isResurrectable()).as(unit.name()).isTrue();
        }
    }

    @Test
    void the_solvers_wait_until_nothing_of_the_spell_would_go_to_waste() {
        Stack angel = ally(UnitCatalog.ARCH_ANGEL, 1, 0);
        Stack pikemen = ally(UnitCatalog.PIKEMAN, 20, 1);
        Stack foe = enemy();
        BattleSetup setup = new BattleSetup(List.of(angel, pikemen), List.of(foe), Battlefield.STANDARD);

        for (AutoSolver solver : List.of(new GreedyAutoSolver(), new StrategicAutoSolver())) {
            solver.planRound(setup);
            assertThat(solver.decide(angel, foe, Battlefield.STANDARD))
                    .as("%s ohne Verluste", solver.getClass().getSimpleName())
                    .isNotInstanceOf(Action.Resurrect.class);
        }

        // 90 HP Verlust: der Zauber brächte 100 — noch nicht.
        pikemen.loseTopCreatures(9);
        assertThat(Resurrection.worthwhileTarget(angel, setup)).isNull();

        pikemen.loseTopCreatures(1);
        for (AutoSolver solver : List.of(new GreedyAutoSolver(), new StrategicAutoSolver())) {
            solver.planRound(setup);
            assertThat(solver.decide(angel, foe, Battlefield.STANDARD))
                    .as("%s bei 100 HP Verlust", solver.getClass().getSimpleName())
                    .isEqualTo(new Action.Resurrect(pikemen));
        }
    }

    @Test
    void the_heuristic_picks_the_ally_with_the_largest_loss() {
        Stack angel = ally(UnitCatalog.ARCH_ANGEL, 1, 0);
        Stack pikemen = ally(UnitCatalog.PIKEMAN, 20, 1);
        Stack swordsmen = ally(UnitCatalog.SWORDSMAN, 10, 2);
        pikemen.loseTopCreatures(12);
        swordsmen.loseTopCreatures(5);
        BattleSetup setup = new BattleSetup(List.of(angel, pikemen, swordsmen), List.of(enemy()),
                Battlefield.STANDARD);

        // Pikemen: 120 HP verloren, Swordsmen (35 HP): 175.
        assertThat(Resurrection.worthwhileTarget(angel, setup)).isSameAs(swordsmen);
    }

    @Test
    void only_arch_angels_carry_the_ability() {
        Stack angel = ally(UnitCatalog.ANGEL, 1, 0);
        Stack pikemen = ally(UnitCatalog.PIKEMAN, 20, 1);
        pikemen.loseTopCreatures(15);
        BattleSetup setup = new BattleSetup(List.of(angel, pikemen), List.of(enemy()), Battlefield.STANDARD);

        assertThat(angel.canResurrect()).isFalse();
        assertThat(Resurrection.worthwhileTarget(angel, setup)).isNull();
    }
}
