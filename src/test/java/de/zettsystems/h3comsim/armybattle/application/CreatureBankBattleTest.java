package de.zettsystems.h3comsim.armybattle.application;

import de.zettsystems.h3comsim.armybattle.values.ArmyBattleRequest;
import de.zettsystems.h3comsim.armybattle.values.ArmyBattleSimulation;
import de.zettsystems.h3comsim.armybattle.values.ArmySpec;
import de.zettsystems.h3comsim.armybattle.values.CreatureBankSpec;
import de.zettsystems.h3comsim.armybattle.values.StackSpec;
import de.zettsystems.h3comsim.battle.domain.CreatureBank;
import de.zettsystems.h3comsim.battle.domain.Fortification;
import de.zettsystems.h3comsim.battle.domain.SecondarySkill;
import de.zettsystems.h3comsim.battle.domain.SkillLevel;
import de.zettsystems.h3comsim.battle.domain.WarMachine;
import de.zettsystems.h3comsim.battle.domain.events.BattleEvent;
import de.zettsystems.h3comsim.battle.domain.events.Side;
import de.zettsystems.h3comsim.battle.domain.events.StackSnapshot;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Kampf um eine Kreaturenbank: Wächter, Aufstellung und Regeln der Bank. */
class CreatureBankBattleTest {

    private static final long SEED = 4711L;
    private static final List<StackSpec> ARMY = List.of(new StackSpec("Champion", 10), new StackSpec("Marksman", 30),
            new StackSpec("Royal Griffin", 15), new StackSpec("Zealot", 10), new StackSpec("Halberdier", 40),
            new StackSpec("Crusader", 15), new StackSpec("Arch Angel", 2));

    private final DefaultArmyBattleService service = new DefaultArmyBattleService();

    private ArmyBattleSimulation simulate(CreatureBank bank, @Nullable Integer level, long seed) {
        ArmySpec tactician = new ArmySpec(ARMY, "Sorsha", 0, 0, 1, 0, 0, 0, 0,
                Map.of(SecondarySkill.TACTICS, SkillLevel.EXPERT), Set.of(WarMachine.BALLISTA), Set.of(), false, false);
        return service.simulate(new ArmyBattleRequest(tactician, null, seed, null, null, null, null, null,
                new CreatureBankSpec(bank, level)), seed);
    }

    private static BattleEvent.BattleStart start(ArmyBattleSimulation simulation) {
        return (BattleEvent.BattleStart) simulation.events().getFirst();
    }

    @Test
    void the_guards_defend_in_the_layout_of_the_bank_without_tactics_obstacles_or_war_machines() {
        ArmyBattleSimulation simulation = simulate(CreatureBank.CYCLOPS_STOCKPILE, 2, SEED);
        BattleEvent.BattleStart start = start(simulation);

        List<StackSnapshot> guards = start.stacks().stream().filter(s -> s.side() == Side.DEFENDER).toList();
        assertThat(guards).hasSize(5).allSatisfy(g -> {
            assertThat(g.unitName()).isIn("Cyclops", "Cyclops King");
            assertThat(g.count()).isEqualTo(6);
        });
        assertThat(simulation.creatureBankLevel()).isEqualTo(2);
        assertThat(start.obstacles()).isEmpty();
        assertThat(start.attackerWarMachines()).isEmpty();
        assertThat(simulation.events()).noneMatch(BattleEvent.TacticsPhase.class::isInstance);
        assertThat(start.stacks().stream().filter(s -> s.side() == Side.ATTACKER).map(s -> s.q() + "/" + s.r()))
                .as("die Armee steht auf den Feldern der Bank")
                .containsExactlyElementsOf(List.of(0, 1, 2, 3, 4, 5, 6).stream()
                        .map(CreatureBank::attackerHex).map(h -> h.q() + "/" + h.r()).toList());
        assertThat(simulation.defenderHero()).isNull();
    }

    @Test
    void the_level_is_rolled_reproducibly_and_one_stack_may_be_upgraded() {
        assertThat(simulate(CreatureBank.IMP_CACHE, null, 9L).creatureBankLevel())
                .isEqualTo(simulate(CreatureBank.IMP_CACHE, null, 9L).creatureBankLevel());
        Set<Integer> levels = new HashSet<>();
        Set<String> thirdGuard = new HashSet<>();
        for (long seed = 1; seed <= 30; seed++) {
            ArmyBattleSimulation simulation = simulate(CreatureBank.IMP_CACHE, null, seed);
            levels.add(simulation.creatureBankLevel());
            thirdGuard.add(start(simulation).stacks().stream()
                    .filter(s -> s.side() == Side.DEFENDER && s.slot() == 2).findFirst().orElseThrow().unitName());
        }
        assertThat(levels).hasSizeGreaterThan(1).allMatch(l -> l >= 1 && l <= 4);
        assertThat(thirdGuard).containsExactlyInAnyOrder("Imp", "Familiar");
    }

    @Test
    void wide_guards_fit_into_the_bank() {
        ArmyBattleSimulation simulation = simulate(CreatureBank.DRAGON_UTOPIA, 1, SEED);
        assertThat(start(simulation).stacks().stream().filter(s -> s.side() == Side.DEFENDER).map(StackSnapshot::unitName))
                .containsExactly("Green Dragon", "Red Dragon", "Gold Dragon", "Black Dragon");
    }

    @Test
    void only_a_hero_visits_a_bank_and_a_bank_is_no_town() {
        ArmySpec heroless = ArmySpec.withoutHero(ARMY);
        CreatureBankSpec crypt = new CreatureBankSpec(CreatureBank.CRYPT, 1);
        assertThatThrownBy(() -> service.simulate(new ArmyBattleRequest(heroless, null, SEED, null, null, null, null,
                null, crypt), SEED)).isInstanceOf(ResponseStatusException.class);
        ArmySpec hero = new ArmySpec(ARMY, "Sorsha");
        assertThatThrownBy(() -> service.simulate(new ArmyBattleRequest(hero, null, SEED, Fortification.FORT,
                null, null, null, null, crypt), SEED)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.simulate(new ArmyBattleRequest(hero, null, SEED), SEED))
                .as("ohne Bank braucht es einen Verteidiger").isInstanceOf(ResponseStatusException.class);
    }
}
