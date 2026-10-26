package de.zettsystems.h3comsim.armybattle.application;

import de.zettsystems.h3comsim.armybattle.values.ArmyBattleRequest;
import de.zettsystems.h3comsim.armybattle.values.ArmyBattleSimulation;
import de.zettsystems.h3comsim.armybattle.values.ArmySpec;
import de.zettsystems.h3comsim.armybattle.values.StackSpec;
import de.zettsystems.h3comsim.battle.domain.Faction;
import de.zettsystems.h3comsim.battle.domain.Fortification;
import de.zettsystems.h3comsim.battle.domain.SecondarySkill;
import de.zettsystems.h3comsim.battle.domain.SkillLevel;
import de.zettsystems.h3comsim.battle.domain.Spell;
import de.zettsystems.h3comsim.battle.domain.WarMachine;
import de.zettsystems.h3comsim.battle.domain.events.BattleEvent;
import de.zettsystems.h3comsim.battle.domain.events.Side;
import de.zettsystems.h3comsim.battle.domain.events.SiegeSnapshot;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Armeen ohne Helden (neutrale Monster, Garnison) und die Stadt unabhängig vom Helden. */
class HeroAndTownTest {

    private static final long SEED = 4711L;
    private static final List<StackSpec> PIKEMEN = List.of(new StackSpec("Pikeman", 30));
    private static final List<StackSpec> PEASANTS = List.of(new StackSpec("Peasant", 200));

    private final DefaultArmyBattleService service = new DefaultArmyBattleService();

    @Test
    void an_army_without_a_hero_fights_without_luck_spells_and_war_machines() {
        // Luck, Ballista und Zauberbuch stehen im Request, wirken ohne Helden aber nicht.
        ArmySpec heroless = new ArmySpec(PIKEMEN, null, 3, 0, 1, 0, 0, 0, 0,
                Map.of(SecondarySkill.LUCK, SkillLevel.EXPERT), Set.of(WarMachine.BALLISTA),
                Set.of(Spell.MAGIC_ARROW), false, true);

        ArmyBattleSimulation simulation = service.simulate(
                new ArmyBattleRequest(heroless, new ArmySpec(PIKEMEN, null), SEED), SEED);

        BattleEvent.BattleStart start = start(simulation);
        assertThat(start.attackerHero()).isNull();
        assertThat(start.defenderHero()).isEqualTo("Sorsha");
        assertThat(start.attackerLuck()).isZero();
        assertThat(start.warMachineStacks()).noneMatch(m -> m.side() == Side.ATTACKER);
        assertThat(simulation.attackerHero()).isNull();
        assertThat(simulation.defenderHero()).isNotNull();
        assertThat(simulation.events()).noneMatch(e -> e instanceof BattleEvent.HeroSpell h && h.actor() == Side.ATTACKER);
    }

    @Test
    void a_hero_name_and_without_hero_exclude_each_other() {
        ArmySpec both = new ArmySpec(PIKEMEN, "Crag Hack", 0, 0, 1, 0, 0, 0, 0, Map.of(), Set.of(), Set.of(),
                false, true);

        ArmyBattleRequest request = new ArmyBattleRequest(both, new ArmySpec(PIKEMEN), SEED);

        assertThatThrownBy(() -> service.simulate(request, SEED))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("exclude");
    }

    @Test
    void only_a_hero_besieges_a_town() {
        ArmyBattleRequest request = new ArmyBattleRequest(ArmySpec.withoutHero(PIKEMEN), new ArmySpec(PIKEMEN),
                SEED, Fortification.FORT);

        assertThatThrownBy(() -> service.simulate(request, SEED))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("hero");
    }

    @Test
    void the_town_is_chosen_independently_of_the_defending_hero() {
        // Sorsha ist eine Castle-Heldin, die Stadt ist Tower: Landminen statt Graben.
        SiegeSnapshot siege = siege(new ArmySpec(PIKEMEN), new ArmySpec(PIKEMEN, "Sorsha"), Faction.TOWER);

        assertThat(siege.town()).isEqualTo(Faction.TOWER);
        assertThat(siegeOf(new ArmySpec(PIKEMEN, "Sorsha"), null).town()).as("ohne Angabe die Stadt des Helden")
                .isEqualTo(Faction.CASTLE);
    }

    @Test
    void a_garrison_without_a_hero_defends_the_town_of_its_main_faction_with_its_towers() {
        ArmySpec garrison = ArmySpec.withoutHero(List.of(new StackSpec("Imp", 100), new StackSpec("Gnoll", 10)));

        ArmyBattleSimulation simulation = service.simulate(new ArmyBattleRequest(new ArmySpec(PIKEMEN), garrison,
                SEED, Fortification.CASTLE), SEED);

        BattleEvent.BattleStart start = start(simulation);
        assertThat(start.defenderHero()).isNull();
        assertThat(Objects.requireNonNull(start.siege()).town()).isEqualTo(Faction.INFERNO);
        assertThat(simulation.events()).anyMatch(e -> e instanceof BattleEvent.TowerShot);
    }

    @Test
    void a_purely_neutral_garrison_needs_its_town() {
        ArmySpec garrison = ArmySpec.withoutHero(PEASANTS);
        ArmyBattleRequest withoutTown = new ArmyBattleRequest(new ArmySpec(PIKEMEN), garrison, SEED,
                Fortification.FORT);

        assertThatThrownBy(() -> service.simulate(withoutTown, SEED))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Town");
        assertThat(siege(new ArmySpec(PIKEMEN), garrison, Faction.FORTRESS).town()).isEqualTo(Faction.FORTRESS);
        ArmyBattleRequest neutralTown = new ArmyBattleRequest(new ArmySpec(PIKEMEN), garrison, SEED,
                Fortification.FORT, Faction.NEUTRAL, null);
        assertThatThrownBy(() -> service.simulate(neutralTown, SEED))
                .isInstanceOf(ResponseStatusException.class);
    }

    private SiegeSnapshot siegeOf(ArmySpec defender, @Nullable Faction town) {
        return siege(new ArmySpec(PIKEMEN), defender, town);
    }

    private SiegeSnapshot siege(ArmySpec attacker, ArmySpec defender, @Nullable Faction town) {
        ArmyBattleSimulation simulation = service.simulate(
                new ArmyBattleRequest(attacker, defender, SEED, Fortification.CITADEL, town, null), SEED);
        return Objects.requireNonNull(start(simulation).siege());
    }

    private static BattleEvent.BattleStart start(ArmyBattleSimulation simulation) {
        return (BattleEvent.BattleStart) simulation.events().getFirst();
    }
}
