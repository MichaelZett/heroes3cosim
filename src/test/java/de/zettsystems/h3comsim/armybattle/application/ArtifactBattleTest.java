package de.zettsystems.h3comsim.armybattle.application;

import de.zettsystems.h3comsim.armybattle.values.ArmyBattleRequest;
import de.zettsystems.h3comsim.armybattle.values.ArmyBattleSimulation;
import de.zettsystems.h3comsim.armybattle.values.ArmySpec;
import de.zettsystems.h3comsim.armybattle.values.StackSpec;
import de.zettsystems.h3comsim.battle.domain.Artifact;
import de.zettsystems.h3comsim.battle.domain.ArtifactSlot;
import de.zettsystems.h3comsim.battle.domain.Spell;
import de.zettsystems.h3comsim.battle.domain.events.BattleEvent;
import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Artefakte im Request: Plätze, Eröffnungszauber und ihre Wirkung im Kampf. */
class ArtifactBattleTest {

    private static final long SEED = 4711L;
    private static final List<StackSpec> PIKEMEN = List.of(new StackSpec("Pikeman", 30));

    private final DefaultArmyBattleService service = new DefaultArmyBattleService();

    private static ArmySpec withArtifacts(List<StackSpec> stacks, String hero, Map<ArtifactSlot, Artifact> artifacts) {
        return new ArmySpec(stacks, hero, 0, 0, 1, 0, 0, 0, 0, Map.of(), Set.of(), Set.of(), false, false, artifacts);
    }

    @Test
    void the_armor_of_the_damned_curses_the_enemy_at_the_first_own_turn() {
        ArmySpec damned = withArtifacts(PIKEMEN, "Sorsha", Map.of(ArtifactSlot.TORSO, Artifact.ARMOR_OF_THE_DAMNED));
        ArmyBattleSimulation simulation = service.simulate(
                new ArmyBattleRequest(damned, new ArmySpec(PIKEMEN, "Crag Hack"), SEED), SEED);

        List<BattleEvent.ArtifactSpell> casts = simulation.events().stream()
                .filter(BattleEvent.ArtifactSpell.class::isInstance).map(BattleEvent.ArtifactSpell.class::cast).toList();
        assertThat(casts).extracting(BattleEvent.ArtifactSpell::spell)
                .containsExactlyInAnyOrder(Spell.SLOW, Spell.CURSE, Spell.WEAKNESS, Spell.MISFORTUNE);
        assertThat(casts).allSatisfy(c -> assertThat(c.actor()).isEqualTo(Side.ATTACKER));
        assertThat(simulation.events().stream().filter(BattleEvent.SpellHit.class::isInstance)
                .map(BattleEvent.SpellHit.class::cast)
                .filter(hit -> hit.spell() == Spell.SLOW))
                .isNotEmpty()
                .allSatisfy(hit -> assertThat(hit.target()).isEqualTo(Side.DEFENDER));
    }

    @Test
    void artifacts_change_the_outcome() {
        ArmyBattleSimulation plain = service.simulate(
                new ArmyBattleRequest(new ArmySpec(PIKEMEN, "Sorsha"), new ArmySpec(PIKEMEN, "Sorsha"), SEED), SEED);
        ArmySpec armed = withArtifacts(PIKEMEN, "Sorsha", Map.of(ArtifactSlot.RIGHT_HAND, Artifact.SWORD_OF_JUDGEMENT,
                ArtifactSlot.LEFT_HAND, Artifact.SENTINELS_SHIELD, ArtifactSlot.MISC_1, Artifact.VIAL_OF_LIFEBLOOD));
        ArmyBattleSimulation strong = service.simulate(
                new ArmyBattleRequest(armed, new ArmySpec(PIKEMEN, "Sorsha"), SEED), SEED);

        assertThat(strong.result().attackerSurvivors()).isGreaterThan(plain.result().attackerSurvivors());
    }

    @Test
    void an_impossible_equipment_is_a_bad_request() {
        ArmySpec wrong = withArtifacts(PIKEMEN, "Sorsha", Map.of(ArtifactSlot.HEAD, Artifact.CENTAURS_AXE));
        ArmyBattleRequest request = new ArmyBattleRequest(wrong, new ArmySpec(PIKEMEN, null), SEED);

        assertThatThrownBy(() -> service.simulate(request, SEED))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("does not fit");
    }
}
