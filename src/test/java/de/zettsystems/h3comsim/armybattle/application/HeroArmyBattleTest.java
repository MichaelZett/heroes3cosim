package de.zettsystems.h3comsim.armybattle.application;

import de.zettsystems.h3comsim.armybattle.values.ArmyBattleRequest;
import de.zettsystems.h3comsim.armybattle.values.ArmyBattleSimulation;
import de.zettsystems.h3comsim.armybattle.values.ArmySpec;
import de.zettsystems.h3comsim.armybattle.values.StackSpec;
import de.zettsystems.h3comsim.battle.domain.events.BattleEvent;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Der Held muss den ganzen Weg vom Request bis in die Schadensrechnung gehen, und jede Armee hat
 * einen — ohne Auswahl den Standardhelden ihrer Hauptfraktion.
 */
class HeroArmyBattleTest {

    private static final long SEED = 4711L;

    private final DefaultArmyBattleService service = new DefaultArmyBattleService();

    private static ArmyBattleRequest duel(@Nullable String attackerHero) {
        List<StackSpec> army = List.of(new StackSpec("Pikeman", 30));
        return new ArmyBattleRequest(
                new ArmySpec(army, attackerHero),
                new ArmySpec(army, null),
                SEED);
    }

    private static BattleEvent.BattleStart start(ArmyBattleSimulation simulation) {
        return (BattleEvent.BattleStart) simulation.events().getFirst();
    }

    @Test
    void the_chosen_hero_changes_the_outcome_of_an_otherwise_symmetric_battle() {
        // Ohne Auswahl führt Sorsha (2/2) beide Pikeman-Armeen. Crag Hacks Attack 4 und
        // Defense 0 statt Sorshas 2/2 verschieben die Rechnung — das muss im Ergebnis ankommen.
        ArmyBattleSimulation standard = service.simulate(duel(null), SEED);
        ArmyBattleSimulation chosen = service.simulate(duel("Crag Hack"), SEED);

        assertThat(start(chosen).attackerHero()).isEqualTo("Crag Hack");
        assertThat(chosen.result()).isNotEqualTo(standard.result());
    }

    @Test
    void an_army_without_a_chosen_hero_is_led_by_the_default_hero_of_its_faction() {
        ArmyBattleSimulation simulation = service.simulate(duel(null), SEED);

        assertThat(start(simulation).attackerHero()).isEqualTo("Sorsha");
        assertThat(start(simulation).defenderHero()).isEqualTo("Sorsha");
    }

    @Test
    void the_faction_with_the_largest_gold_value_picks_the_default_hero() {
        // 30 Pikemen (60 Gold) stellen 1800 Gold, ein Black Dragon (4000 Gold) mehr als das
        // Doppelte — die Armee gilt als Dungeon-Armee und bekommt Damacon.
        List<StackSpec> mixed = List.of(new StackSpec("Pikeman", 30), new StackSpec("Black Dragon", 1));
        ArmyBattleRequest request = new ArmyBattleRequest(
                new ArmySpec(mixed, null),
                new ArmySpec(List.of(new StackSpec("Peasant", 10)), null),
                SEED);

        ArmyBattleSimulation simulation = service.simulate(request, SEED);

        assertThat(start(simulation).attackerHero()).isEqualTo("Damacon");
        // Rein neutrale Armee: fester Ersatz, damit auch sie einen Helden hat.
        assertThat(start(simulation).defenderHero()).isEqualTo("Sorsha");
    }

    @Test
    void an_unknown_hero_is_rejected_instead_of_silently_ignored() {
        // Request außerhalb des Lambdas bauen: sonst stünden dort zwei Aufrufe, die werfen
        // könnten, und der Test würde auch bestehen, wenn schon duel(...) fehlschlägt.
        ArmyBattleRequest request = duel("Gandalf");

        assertThatThrownBy(() -> service.simulate(request, SEED))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Unknown hero: Gandalf");
    }

    @Test
    void a_blank_hero_name_means_the_default_hero() {
        // Ein leerer String darf nicht als unbekannter Held durchfallen, sondern muss dasselbe
        // bedeuten wie null.
        ArmyBattleSimulation blank = service.simulate(duel("  "), SEED);
        ArmyBattleSimulation none = service.simulate(duel(null), SEED);

        assertThat(start(blank).attackerHero()).isEqualTo("Sorsha");
        assertThat(blank.result()).isEqualTo(none.result());
    }

    @Test
    void the_same_seed_stays_deterministic_with_a_hero() {
        ArmyBattleSimulation first = service.simulate(duel("Crag Hack"), SEED);
        ArmyBattleSimulation second = service.simulate(duel("Crag Hack"), SEED);

        assertThat(first.result()).isEqualTo(second.result());
    }
}
