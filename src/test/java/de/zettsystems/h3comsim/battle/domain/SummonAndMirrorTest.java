package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.BattleEvent;
import de.zettsystems.h3comsim.battle.domain.events.ListEventCollector;
import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.junit.jupiter.api.Test;

import java.io.Serial;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Summon Elementals und Magic Mirror (Manual S. 66 ff.). */
class SummonAndMirrorTest {

    private static final Map<SecondarySkill, SkillLevel> EXPERT_WISDOM = Map.of(SecondarySkill.WISDOM, SkillLevel.EXPERT);

    private static Hero mage(int power, int knowledge, Map<SecondarySkill, SkillLevel> skills, Spell... spells) {
        return new Hero("Mage", HeroClass.WIZARD, Faction.TOWER, 0, 0, power, knowledge, skills)
                .withSpellbook(Set.of(spells));
    }

    private static Stack stack(Unit unit, int count, int q, int r, Side side, int slot) {
        return new Stack(unit, count, new Hex(q, r), side, slot);
    }

    private static <T extends BattleEvent> List<T> events(ListEventCollector collector, Class<T> type) {
        return collector.events().stream().filter(type::isInstance).map(type::cast).toList();
    }

    @Test
    void summon_formulas_and_single_target_spells() {
        assertThat(HeroSpellcasting.summonCount(SkillLevel.BASIC, 3)).isEqualTo(6);
        assertThat(HeroSpellcasting.summonCount(SkillLevel.EXPERT, 3)).isEqualTo(12);
        assertThat(HeroSpellcasting.summonedUnit(Spell.SUMMON_WATER_ELEMENTAL)).isEqualTo(UnitCatalog.WATER_ELEMENTAL);
        assertThat(HeroSpellcasting.singleTarget(Spell.LIGHTNING_BOLT)).isTrue();
        assertThat(HeroSpellcasting.singleTarget(Spell.SLOW)).isTrue();
        assertThat(HeroSpellcasting.singleTarget(Spell.FIREBALL)).isFalse();
        assertThat(HeroSpellcasting.singleTarget(Spell.BLESS)).isFalse();
    }

    @Test
    void summoned_elementals_join_from_the_next_round_and_do_not_survive() {
        Stack pikemen = stack(UnitCatalog.PIKEMAN, 20, 0, 5, Side.ATTACKER, 0);
        Stack enemy = stack(UnitCatalog.GOBLIN, 30, 14, 5, Side.DEFENDER, 0);
        Hero summoner = mage(2, 10, EXPERT_WISDOM, Spell.SUMMON_EARTH_ELEMENTAL);
        BattleSetup setup = new BattleSetup(List.of(pikemen), List.of(enemy), Battlefield.STANDARD, summoner, null);
        ListEventCollector collector = new ListEventCollector();

        BattleResult result = new Battle(new Random(4), new GreedyAutoSolver(), collector).simulate(setup);

        List<BattleEvent.Summon> summons = events(collector, BattleEvent.Summon.class);
        assertThat(summons).isNotEmpty();
        assertThat(summons.getFirst().count()).isEqualTo(4);
        assertThat(summons.getFirst().stack().slot()).isEqualTo(1);
        assertThat(summons.getFirst().stack().unitName()).isEqualTo("Earth Elemental");
        assertThat(setup.attackerStacks()).hasSize(2);
        Stack elementals = setup.attackerStacks().get(1);
        assertThat(elementals.isSummoned()).isTrue();
        assertThat(result.attackerSurvivors()).isEqualTo(pikemen.getCount());
        if (summons.size() > 1) {
            assertThat(summons.get(1).stack().slot()).as("dieselbe Art verstärkt denselben Stack").isEqualTo(1);
        }
    }

    @Test
    void only_one_kind_of_elemental_per_side() {
        Stack pikemen = stack(UnitCatalog.PIKEMAN, 20, 0, 5, Side.ATTACKER, 0);
        Stack enemy = stack(UnitCatalog.GOBLIN, 30, 14, 5, Side.DEFENDER, 0);
        Hero summoner = mage(2, 10, EXPERT_WISDOM, Spell.SUMMON_EARTH_ELEMENTAL, Spell.SUMMON_FIRE_ELEMENTAL);
        BattleSetup setup = new BattleSetup(List.of(pikemen), List.of(enemy), Battlefield.STANDARD, summoner, null);
        assertThat(HeroSpellcasting.candidates(summoner, Side.ATTACKER, Spell.SUMMON_FIRE_ELEMENTAL, setup)).hasSize(1);

        Hex hex = setup.freeHexFor(Side.ATTACKER);
        assertThat(hex).isNotNull().isNotEqualTo(pikemen.position());
        setup.addSummoned(new Stack(UnitCatalog.EARTH_ELEMENTAL, 4, hex, Side.ATTACKER, setup.nextSlot(Side.ATTACKER)));

        assertThat(setup.summonedKindOf(Side.ATTACKER)).isEqualTo(UnitCatalog.EARTH_ELEMENTAL);
        assertThat(HeroSpellcasting.candidates(summoner, Side.ATTACKER, Spell.SUMMON_FIRE_ELEMENTAL, setup)).isEmpty();
        assertThat(HeroSpellcasting.candidates(summoner, Side.ATTACKER, Spell.SUMMON_EARTH_ELEMENTAL, setup)).hasSize(1);
        assertThat(setup.survivorsOf(Side.ATTACKER)).isEqualTo(20);
        assertThat(setup.getAttackerCount()).isEqualTo(24);
    }

    @Test
    void a_fallen_summoned_stack_returns_and_a_living_one_is_reinforced() {
        Stack pikemen = stack(UnitCatalog.PIKEMAN, 150, 0, 5, Side.ATTACKER, 0);
        Stack enemy = stack(UnitCatalog.ANGEL, 4, 14, 5, Side.DEFENDER, 0);
        Hero summoner = mage(1, 20, EXPERT_WISDOM, Spell.SUMMON_AIR_ELEMENTAL);
        BattleSetup setup = new BattleSetup(List.of(pikemen), List.of(enemy), Battlefield.STANDARD, summoner, null);
        ListEventCollector collector = new ListEventCollector();

        new Battle(new Random(8), new GreedyAutoSolver(), collector).simulate(setup);

        List<BattleEvent.Summon> summons = events(collector, BattleEvent.Summon.class);
        assertThat(summons).hasSizeGreaterThan(1)
                .allSatisfy(s -> assertThat(s.stack().slot()).isEqualTo(1));
        assertThat(setup.attackerStacks()).hasSize(2);
    }

    @Test
    void magic_mirror_turns_a_bolt_back_on_the_caster() {
        Stack champions = stack(UnitCatalog.CHAMPION, 10, 0, 5, Side.ATTACKER, 0);
        champions.applySpell(Spell.MAGIC_MIRROR, SkillLevel.EXPERT, 20);
        Stack enemy = stack(UnitCatalog.PIKEMAN, 40, 14, 5, Side.DEFENDER, 0);
        Hero bolter = mage(3, 5, Map.of(), Spell.LIGHTNING_BOLT);
        BattleSetup setup = new BattleSetup(List.of(champions), List.of(enemy), Battlefield.STANDARD, null, bolter);
        ListEventCollector collector = new ListEventCollector();

        new Battle(new AlwaysTrigger(5), new GreedyAutoSolver(), collector).simulate(setup);

        List<BattleEvent.SpellHit> hits = events(collector, BattleEvent.SpellHit.class);
        assertThat(hits).isNotEmpty().allSatisfy(h -> {
            assertThat(h.reflected()).isTrue();
            assertThat(h.target()).isEqualTo(Side.DEFENDER);
        });
    }

    @Test
    void magic_mirror_is_valued_only_against_a_casting_enemy() {
        Stack champions = stack(UnitCatalog.CHAMPION, 10, 0, 5, Side.ATTACKER, 0);
        Stack pikemen = stack(UnitCatalog.PIKEMAN, 10, 0, 3, Side.ATTACKER, 1);
        Stack enemy = stack(UnitCatalog.PIKEMAN, 40, 14, 5, Side.DEFENDER, 0);
        Hero guard = mage(3, 5, EXPERT_WISDOM, Spell.MAGIC_MIRROR);

        BattleSetup quiet = new BattleSetup(List.of(champions, pikemen), List.of(enemy), Battlefield.STANDARD,
                guard, null);
        assertThat(HeroSpellcasting.decide(guard, Side.ATTACKER, 50, quiet)).isNull();

        BattleSetup threatened = new BattleSetup(List.of(champions, pikemen), List.of(enemy), Battlefield.STANDARD,
                guard, mage(4, 5, Map.of(), Spell.LIGHTNING_BOLT));
        HeroSpellcasting.Cast cast = HeroSpellcasting.decide(guard, Side.ATTACKER, 50, threatened);
        assertThat(cast).isNotNull();
        assertThat(cast.spell()).isEqualTo(Spell.MAGIC_MIRROR);
        assertThat(cast.target()).isSameAs(champions);
    }

    @Test
    void a_fallen_summoned_stack_rises_again_on_a_free_hex() {
        Stack pikemen = stack(UnitCatalog.PIKEMAN, 20, 0, 5, Side.ATTACKER, 0);
        Stack enemy = stack(UnitCatalog.GOBLIN, 30, 14, 5, Side.DEFENDER, 0);
        Hero summoner = mage(2, 3, EXPERT_WISDOM, Spell.SUMMON_FIRE_ELEMENTAL);
        BattleSetup setup = new BattleSetup(List.of(pikemen), List.of(enemy), Battlefield.STANDARD, summoner, null);
        Stack fallen = new Stack(UnitCatalog.FIRE_ELEMENTAL, 3, new Hex(1, 5), Side.ATTACKER, 1);
        setup.addSummoned(fallen);
        fallen.takeDamage(10_000, Set.of());
        ListEventCollector collector = new ListEventCollector();

        new Battle(new Random(6), new GreedyAutoSolver(), collector).simulate(setup);

        BattleEvent.Summon summon = events(collector, BattleEvent.Summon.class).getFirst();
        assertThat(summon.stack().slot()).isEqualTo(1);
        assertThat(summon.count()).isEqualTo(4);
        assertThat(setup.attackerStacks()).hasSize(2);
    }

    @Test
    void a_hero_slows_the_enemy_in_battle() {
        Stack pikemen = stack(UnitCatalog.PIKEMAN, 20, 0, 5, Side.ATTACKER, 0);
        Stack enemy = stack(UnitCatalog.CHAMPION, 5, 14, 5, Side.DEFENDER, 0);
        Hero slower = mage(3, 1, Map.of(SecondarySkill.EARTH_MAGIC, SkillLevel.ADVANCED), Spell.SLOW);
        BattleSetup setup = new BattleSetup(List.of(pikemen), List.of(enemy), Battlefield.STANDARD, slower, null);
        ListEventCollector collector = new ListEventCollector();

        new Battle(new Random(1), new GreedyAutoSolver(), collector).simulate(setup);

        assertThat(events(collector, BattleEvent.SpellHit.class)).singleElement().satisfies(h -> {
            assertThat(h.spell()).isEqualTo(Spell.SLOW);
            assertThat(h.target()).isEqualTo(Side.DEFENDER);
            assertThat(h.resisted()).isFalse();
        });
    }

    @Test
    void the_elemental_kind_follows_the_situation() {
        Hero summoner = mage(3, 10, EXPERT_WISDOM, Spell.SUMMON_AIR_ELEMENTAL, Spell.SUMMON_EARTH_ELEMENTAL,
                Spell.SUMMON_FIRE_ELEMENTAL, Spell.SUMMON_WATER_ELEMENTAL);
        Stack pikemen = stack(UnitCatalog.PIKEMAN, 20, 0, 0, Side.ATTACKER, 0);

        // Ohne gegnerische Zauber: Earth, das wertvollste — auch gegen ferne Schützen.
        Stack archers = stack(UnitCatalog.ARCHER, 30, 14, 5, Side.DEFENDER, 0);
        BattleSetup shooters = new BattleSetup(List.of(pikemen), List.of(archers), Battlefield.STANDARD, summoner, null);
        assertThat(chosen(summoner, shooters)).isEqualTo(Spell.SUMMON_EARTH_ELEMENTAL);

        // Ein starker Meteor Shower des Gegners trifft Earth doppelt, Air gar nicht.
        Hero meteor = mage(10, 5, Map.of(SecondarySkill.WISDOM, SkillLevel.ADVANCED), Spell.METEOR_SHOWER);
        Stack far = stack(UnitCatalog.GOBLIN, 30, 14, 5, Side.DEFENDER, 0);
        BattleSetup stormy = new BattleSetup(List.of(pikemen), List.of(far), Battlefield.STANDARD, summoner, meteor);
        assertThat(chosen(summoner, stormy)).isNotEqualTo(Spell.SUMMON_EARTH_ELEMENTAL);

        // Ein starker Ice Bolt des Gegners: Fire nimmt doppelten Schaden, Water keinen.
        Hero frost = mage(10, 5, Map.of(), Spell.ICE_BOLT);
        BattleSetup icy = new BattleSetup(List.of(pikemen), List.of(far), Battlefield.STANDARD, summoner, frost);
        assertThat(chosen(summoner, icy)).isEqualTo(Spell.SUMMON_WATER_ELEMENTAL);
    }

    private static Spell chosen(Hero summoner, BattleSetup setup) {
        HeroSpellcasting.Cast cast = HeroSpellcasting.decide(summoner, Side.ATTACKER, 100, setup);
        assertThat(cast).isNotNull();
        return cast.spell();
    }

    @Test
    void only_summoned_elementals_left_means_both_sides_lose() {
        Stack pikemen = stack(UnitCatalog.PIKEMAN, 5, 0, 5, Side.ATTACKER, 0);
        Stack goblins = stack(UnitCatalog.GOBLIN, 3, 14, 5, Side.DEFENDER, 0);
        BattleSetup setup = new BattleSetup(List.of(pikemen), List.of(goblins), Battlefield.STANDARD);
        setup.addSummoned(new Stack(UnitCatalog.EARTH_ELEMENTAL, 50, new Hex(0, 3), Side.ATTACKER, 1));
        pikemen.takeDamage(10_000, Set.of());

        BattleResult result = new Battle(new Random(2)).simulate(setup);

        assertThat(goblins.isAlive()).isFalse();
        assertThat(result.winner()).isEqualTo(de.zettsystems.h3comsim.battle.domain.events.Winner.DRAW);
        assertThat(result.attackerSurvivors()).isZero();
    }

    @Test
    void reinforcing_adds_fresh_creatures_and_rejects_zero() {
        Stack elementals = stack(UnitCatalog.EARTH_ELEMENTAL, 4, 0, 5, Side.ATTACKER, 7);
        elementals.reinforce(3);
        assertThat(elementals.getCount()).isEqualTo(7);
        assertThat(elementals.startCount()).isEqualTo(7);
        assertThatThrownBy(() -> elementals.reinforce(0)).isInstanceOf(IllegalArgumentException.class);
    }

    /** Jeder Prozentwurf (nextInt(100)) trifft; alles andere wie Random. */
    private static final class AlwaysTrigger extends Random {
        @Serial
        private static final long serialVersionUID = 1L;

        AlwaysTrigger(long seed) {
            super(seed);
        }

        @Override
        public int nextInt(int bound) {
            return bound == 100 ? 0 : super.nextInt(bound);
        }
    }
}
