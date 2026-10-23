package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.BattleEvent;
import de.zettsystems.h3comsim.battle.domain.events.ListEventCollector;
import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Dämonen-Beschwörung der Pit Lords. Manual S. 98: „Once per combat, pit lords are able to
 * resurrect dead allied troops as demons in place of another action. The number raised cannot be
 * higher than the number of pit lords performing the resurrection." Die Rechnung — 50 HP je Pit
 * Lord, höchstens die Lebenspunkte und die Zahl der Toten — stammt aus heroes.thelazy.net.
 */
class DemonRaisingTest {

    private static Stack ally(Unit unit, int count, int slot) {
        return new Stack(unit, count, new Hex(0, 1 + 2 * slot), Side.ATTACKER, slot);
    }

    private static Stack enemy() {
        return new Stack(UnitCatalog.PIKEMAN, 10, new Hex(14, 5), Side.DEFENDER, 0);
    }

    private static Stack fallen(Unit unit, int count, int slot) {
        Stack stack = ally(unit, count, slot);
        stack.loseTopCreatures(count);
        return stack;
    }

    private static BattleSetup setup(Stack... attackers) {
        return new BattleSetup(List.of(attackers), List.of(enemy()), Battlefield.STANDARD);
    }

    @Test
    void the_pit_lord_matches_the_manual() {
        Unit lord = UnitCatalog.PIT_LORD;
        assertThat(lord.attack()).isEqualTo(13);
        assertThat(lord.defense()).isEqualTo(13);
        assertThat(lord.health()).isEqualTo(45);
        assertThat(lord.speed()).isEqualTo(7);
        assertThat(lord.minDamage()).isEqualTo(13);
        assertThat(lord.maxDamage()).isEqualTo(17);
        assertThat(lord.hasSpeciality(UnitSpeciality.RAISE_DEMONS)).isTrue();
        assertThat(UnitCatalog.PIT_FIEND.hasSpeciality(UnitSpeciality.RAISE_DEMONS)).isFalse();
    }

    @Test
    void each_pit_lord_brings_back_50_health_worth_of_demons() {
        Stack lords = ally(UnitCatalog.PIT_LORD, 7, 0);
        // 7 × 50 = 350 HP = 10 Dämonen à 35 HP, wenn der Leichnam genug hergibt.
        assertThat(DemonRaising.demonsFrom(lords, fallen(UnitCatalog.HORNED_DEMON, 20, 1))).isEqualTo(10);
    }

    @Test
    void the_corpse_limits_both_health_and_head_count() {
        Stack lords = ally(UnitCatalog.PIT_LORD, 10, 0);
        // 4 Pikemen = 40 HP → ein Dämon; ein einzelner Pikeman (10 HP) reicht für keinen.
        assertThat(DemonRaising.demonsFrom(lords, fallen(UnitCatalog.PIKEMAN, 4, 1))).isOne();
        assertThat(DemonRaising.demonsFrom(lords, fallen(UnitCatalog.PIKEMAN, 1, 1))).isZero();
        // 2 Angels = 400 HP, aber nie mehr Dämonen als Tote.
        assertThat(DemonRaising.demonsFrom(lords, fallen(UnitCatalog.ANGEL, 2, 1))).isEqualTo(2);
    }

    @Test
    void the_fallen_stack_returns_as_demons_in_its_own_slot() {
        Stack lords = ally(UnitCatalog.PIT_LORD, 3, 0);
        Stack corpse = fallen(UnitCatalog.HORNED_DEMON, 4, 1);
        BattleSetup setup = setup(lords, corpse);

        ListEventCollector collector = new ListEventCollector();
        // Stack ist eine mutable Entity ohne equals — der ==-Vergleich meint bewusst Identität.
        @SuppressWarnings("ReferenceEquality")
        AutoSolver raise = (active, opponent, bf) ->
                active == lords ? new Action.RaiseDemons(corpse) : new Action.Defend();
        new Battle(new Random(1L), raise, collector).simulate(setup);

        // 3 × 50 = 150 HP → 4 Dämonen; der Leichnam (4 × 40 HP) gibt das her.
        assertThat(corpse.unit()).isSameAs(UnitCatalog.DEMON);
        assertThat(corpse.getCount()).isEqualTo(4);
        assertThat(corpse.slot()).isEqualTo(1);
        List<BattleEvent.RaiseDemons> raised = collector.events().stream()
                .filter(BattleEvent.RaiseDemons.class::isInstance)
                .map(BattleEvent.RaiseDemons.class::cast)
                .toList();
        assertThat(raised).singleElement().satisfies(r -> {
            assertThat(r.raisedCount()).isEqualTo(4);
            assertThat(r.targetAfter().unitName()).isEqualTo("Demon");
        });
        assertThat(lords.canRaiseDemons()).as("nur einmal pro Kampf").isFalse();
    }

    @Test
    void only_fully_fallen_living_allies_on_a_free_hex_qualify() {
        Stack lords = ally(UnitCatalog.PIT_LORD, 3, 0);
        Stack wounded = ally(UnitCatalog.HORNED_DEMON, 4, 1);
        wounded.loseTopCreatures(3);
        Stack skeletons = fallen(UnitCatalog.SKELETON, 20, 2);
        Stack golems = fallen(UnitCatalog.STONE_GOLEM, 10, 3);
        Stack covered = fallen(UnitCatalog.HORNED_DEMON, 4, 4);
        Stack standingOnCorpse = new Stack(UnitCatalog.PIKEMAN, 5, covered.position(), Side.ATTACKER, 5);
        BattleSetup setup = setup(lords, wounded, skeletons, golems, covered, standingOnCorpse);

        assertThat(DemonRaising.isValidCorpse(lords, wounded, setup)).as("lebt noch").isFalse();
        assertThat(DemonRaising.isValidCorpse(lords, skeletons, setup)).as("untot").isFalse();
        assertThat(DemonRaising.isValidCorpse(lords, golems, setup)).as("kein Lebewesen").isFalse();
        assertThat(DemonRaising.isValidCorpse(lords, covered, setup)).as("Feld besetzt").isFalse();
    }

    @Test
    void the_solvers_raise_once_a_corpse_yields_at_least_half_the_possible_demons() {
        Stack lords = ally(UnitCatalog.PIT_LORD, 3, 0);
        Stack familiars = fallen(UnitCatalog.FAMILIAR, 15, 1);
        Stack foe = enemy();
        BattleSetup setup = new BattleSetup(List.of(lords, familiars), List.of(foe), Battlefield.STANDARD);

        // 3 Pit Lords: höchstens 4 Dämonen. 15 Familiars (60 HP) geben nur einen her.
        assertThat(DemonRaising.worthwhileCorpse(lords, setup)).isNull();

        Stack magogs = fallen(UnitCatalog.MAGOG, 8, 2);
        BattleSetup withMagogs = new BattleSetup(List.of(lords, familiars, magogs), List.of(foe), Battlefield.STANDARD);
        // 8 Magogs (104 HP) geben zwei her — die Hälfte, das reicht.
        for (AutoSolver solver : List.of(new GreedyAutoSolver(), new StrategicAutoSolver())) {
            solver.planRound(withMagogs);
            assertThat(solver.decide(lords, foe, Battlefield.STANDARD))
                    .as(solver.getClass().getSimpleName())
                    .isEqualTo(new Action.RaiseDemons(magogs));
        }
    }
}
