package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.BattleEvent;
import de.zettsystems.h3comsim.battle.domain.events.ListEventCollector;
import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Resurrection und Animate Dead auf ganz gefallene Stacks, wenn ihr Feld frei ist. */
class CorpseSpellTest {

    private static final Map<SecondarySkill, SkillLevel> ADVANCED_WISDOM =
            Map.of(SecondarySkill.WISDOM, SkillLevel.ADVANCED);

    private static Hero mage(int power, int knowledge, Spell... spells) {
        return new Hero("Mage", HeroClass.WIZARD, Faction.TOWER, 0, 0, power, knowledge, ADVANCED_WISDOM)
                .withSpellbook(Set.of(spells));
    }

    private static Stack dead(Unit unit, int count, Hex hex, int slot) {
        Stack stack = new Stack(unit, count, hex, Side.ATTACKER, slot);
        stack.takeDamage(100_000, Set.of());
        return stack;
    }

    @Test
    void resurrection_raises_a_fallen_stack_in_battle() {
        Stack archers = new Stack(UnitCatalog.ARCHER, 10, new Hex(0, 5), Side.ATTACKER, 0);
        Stack fallen = dead(UnitCatalog.SWORDSMAN, 10, new Hex(1, 3), 1);
        Stack enemy = new Stack(UnitCatalog.PIKEMAN, 30, new Hex(14, 5), Side.DEFENDER, 0);
        Hero priest = mage(3, 5, Spell.RESURRECTION);
        BattleSetup setup = new BattleSetup(List.of(archers, fallen), List.of(enemy), Battlefield.STANDARD,
                priest, null);
        ListEventCollector collector = new ListEventCollector();

        new Battle(new Random(3), new GreedyAutoSolver(), collector).simulate(setup);

        BattleEvent.SpellHit hit = collector.events().stream()
                .filter(BattleEvent.SpellHit.class::isInstance).map(BattleEvent.SpellHit.class::cast)
                .findFirst().orElseThrow();
        assertThat(hit.spell()).isEqualTo(Spell.RESURRECTION);
        assertThat(hit.targetSlot()).isEqualTo(1);
        // Power 3 auf Basic: 3 × 50 + 40 = 190 HP, Swordsmen haben 35 HP — fünf kommen zurück.
        assertThat(hit.healed()).isEqualTo(175);
        assertThat(hit.targetAfter().count()).isEqualTo(5);
    }

    @Test
    void animate_dead_only_for_the_undead_and_only_if_one_creature_is_affordable() {
        Stack fallenZombies = dead(UnitCatalog.ZOMBIE, 10, new Hex(1, 3), 1);
        Stack fallenPikemen = dead(UnitCatalog.PIKEMAN, 10, new Hex(1, 7), 2);
        Stack living = new Stack(UnitCatalog.SKELETON, 10, new Hex(0, 5), Side.ATTACKER, 0);
        Stack enemy = new Stack(UnitCatalog.PIKEMAN, 30, new Hex(14, 5), Side.DEFENDER, 0);
        BattleSetup setup = new BattleSetup(List.of(living, fallenZombies, fallenPikemen), List.of(enemy),
                Battlefield.STANDARD);

        HeroSpellcasting.Cast cast = HeroSpellcasting.decide(mage(1, 5, Spell.ANIMATE_DEAD), Side.ATTACKER, 50, setup);
        assertThat(cast).isNotNull();
        assertThat(cast.target()).isSameAs(fallenZombies);

        // Ein Ziel, von dem keine ganze Kreatur bezahlbar ist, bringt nichts.
        Stack fallenGiants = dead(UnitCatalog.GIANT, 2, new Hex(1, 3), 1);
        BattleSetup giants = new BattleSetup(List.of(living, fallenGiants), List.of(enemy), Battlefield.STANDARD);
        HeroSpellcasting.Cast none = HeroSpellcasting.decide(mage(1, 5, Spell.RESURRECTION), Side.ATTACKER, 50, giants);
        assertThat(none).isNull();
    }

    @Test
    void an_occupied_corpse_cannot_be_raised() {
        Stack fallen = dead(UnitCatalog.SWORDSMAN, 10, new Hex(1, 3), 1);
        Stack standing = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(1, 3), Side.ATTACKER, 0);
        Stack enemy = new Stack(UnitCatalog.PIKEMAN, 30, new Hex(14, 5), Side.DEFENDER, 0);
        BattleSetup setup = new BattleSetup(List.of(standing, fallen), List.of(enemy), Battlefield.STANDARD);

        assertThat(setup.hasFreeCorpse(fallen)).isFalse();
        assertThat(HeroSpellcasting.candidates(mage(3, 5, Spell.RESURRECTION), Side.ATTACKER, Spell.RESURRECTION, setup))
                .extracting(HeroSpellcasting.Cast::target).containsExactly(standing);
    }
}
