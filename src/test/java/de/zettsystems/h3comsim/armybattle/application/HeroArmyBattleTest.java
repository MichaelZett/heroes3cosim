package de.zettsystems.h3comsim.armybattle.application;

import de.zettsystems.h3comsim.armybattle.values.ArmyBattleRequest;
import de.zettsystems.h3comsim.armybattle.values.ArmyBattleSimulation;
import de.zettsystems.h3comsim.armybattle.values.ArmySpec;
import de.zettsystems.h3comsim.armybattle.values.StackSpec;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Der Held muss den ganzen Weg vom Request bis in die Schadensrechnung gehen. Geprüft wird
 * über das Ergebnis: dieselbe Armee, derselbe Seed, einmal mit und einmal ohne Anführer.
 */
class HeroArmyBattleTest {

    private static final long SEED = 4711L;

    private final DefaultArmyBattleService service = new DefaultArmyBattleService();

    private static ArmyBattleRequest duel(String attackerHero) {
        List<StackSpec> army = List.of(new StackSpec("Pikeman", 30));
        return new ArmyBattleRequest(
                new ArmySpec(army, attackerHero),
                new ArmySpec(army, null),
                SEED);
    }

    @Test
    void a_hero_changes_the_outcome_of_an_otherwise_symmetric_battle() {
        // Pikeman gegen Pikeman ist bis auf den Attacker-Vorzug symmetrisch. Crag Hacks
        // Attack 4 dreht die Attack/Defense-Differenz von -1 auf +3 und damit den
        // Schadensmodifikator von -2 % auf +15 % — das muss im Ergebnis ankommen.
        ArmyBattleSimulation without = service.simulate(duel(null), SEED);
        ArmyBattleSimulation with = service.simulate(duel("Crag Hack"), SEED);

        assertThat(with.result().attackerSurvivors())
                .as("geführte Armee verliert weniger Einheiten")
                .isGreaterThan(without.result().attackerSurvivors());
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
    void a_blank_hero_name_means_no_hero() {
        // Das UI schickt für "ohne Held" null; ein leerer String darf nicht als unbekannter
        // Held durchfallen, sondern muss dasselbe bedeuten.
        ArmyBattleSimulation blank = service.simulate(duel("  "), SEED);
        ArmyBattleSimulation none = service.simulate(duel(null), SEED);

        assertThat(blank.result().attackerSurvivors()).isEqualTo(none.result().attackerSurvivors());
        assertThat(blank.result().winner()).isEqualTo(none.result().winner());
    }

    @Test
    void the_same_seed_stays_deterministic_with_a_hero() {
        ArmyBattleSimulation first = service.simulate(duel("Crag Hack"), SEED);
        ArmyBattleSimulation second = service.simulate(duel("Crag Hack"), SEED);

        assertThat(first.result()).isEqualTo(second.result());
    }
}
