package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.BattleEvent;
import de.zettsystems.h3comsim.battle.domain.events.ListEventCollector;
import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Clone, Sacrifice, Berserk und Hypnotize (heroes.thelazy.net, VCMI). */
class ControlSpellsTest {

    private static final Battlefield FIELD = Battlefield.STANDARD;

    @Test
    void a_clone_vanishes_without_a_corpse_at_the_first_damage() {
        Stack origin = new Stack(UnitCatalog.ANGEL, 5, new Hex(3, 5), Side.ATTACKER, 0);
        Stack clone = new Stack(UnitCatalog.ANGEL, 5, new Hex(3, 7), Side.ATTACKER, 1);
        clone.markClone(origin, 3);

        clone.takeDamage(1, Set.of());

        assertThat(clone.isAlive()).isFalse();
        assertThat(clone.leavesCorpse()).isFalse();
        assertThat(origin.isAlive()).isTrue();
    }

    @Test
    void a_clone_ends_with_its_origin_or_after_its_last_round() {
        Stack origin = new Stack(UnitCatalog.PIKEMAN, 5, new Hex(3, 5), Side.ATTACKER, 0);
        Stack clone = new Stack(UnitCatalog.PIKEMAN, 5, new Hex(3, 7), Side.ATTACKER, 1);
        clone.markClone(origin, 2);

        assertThat(clone.cloneEnds(1, true)).isFalse();
        assertThat(clone.cloneEnds(2, false)).isFalse();
        assertThat(clone.cloneEnds(2, true)).isTrue();
        origin.takeDamage(1000, Set.of());
        assertThat(clone.cloneEnds(0, false)).isTrue();
    }

    @Test
    void clone_takes_only_creatures_up_to_its_tier_limit() {
        Stack pikemen = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(3, 5), Side.ATTACKER, 0);
        Stack angels = new Stack(UnitCatalog.ANGEL, 2, new Hex(3, 3), Side.ATTACKER, 1);
        BattleSetup setup = setup(List.of(pikemen, angels));

        assertThat(targets(Spell.CLONE, SkillLevel.BASIC, setup)).containsExactly(pikemen);
        assertThat(targets(Spell.CLONE, SkillLevel.EXPERT, setup)).containsExactly(pikemen, angels);
    }

    @Test
    void the_hero_clones_a_stack_that_then_fights_for_its_side() {
        Stack angels = new Stack(UnitCatalog.ANGEL, 10, new Hex(3, 5), Side.ATTACKER, 0);
        Hero mage = mage(Spell.CLONE, SecondarySkill.WATER_MAGIC);
        BattleSetup setup = new BattleSetup(List.of(angels),
                List.of(new Stack(UnitCatalog.PEASANT, 300, new Hex(14, 5), Side.DEFENDER, 0)), FIELD, mage, null);

        List<BattleEvent> events = fight(setup);

        assertThat(events).anyMatch(e -> e instanceof BattleEvent.Summon s && s.spell() == Spell.CLONE
                && s.count() == 10);
        assertThat(setup.stacksOf(Side.ATTACKER)).anyMatch(s -> s.cloneOf() == angels);
    }

    @Test
    void sacrifice_turns_one_stack_into_the_life_of_another() {
        Stack peasants = new Stack(UnitCatalog.PEASANT, 100, new Hex(3, 1), Side.ATTACKER, 0);
        Stack angels = new Stack(UnitCatalog.ANGEL, 10, new Hex(3, 5), Side.ATTACKER, 1);
        angels.takeDamage(1000, Set.of());
        BattleSetup setup = setup(List.of(peasants, angels));

        // (Power 2 + 1 Lebenspunkt + 10 auf Expert) × 100 Bauern.
        assertThat(ControlSpells.sacrificeAmount(SkillLevel.EXPERT, 2, peasants)).isEqualTo(1300);
        List<HeroSpellcasting.Cast> casts = ControlSpells.candidates(HeroCatalog.SORSHA, Side.ATTACKER,
                Spell.SACRIFICE, SkillLevel.EXPERT, setup);
        assertThat(casts).anyMatch(c -> c.affected().equals(List.of(peasants, angels)));
        assertThat(casts).noneMatch(c -> c.affected().getFirst() == angels && c.affected().get(1) == angels);
    }

    @Test
    void a_sacrificed_stack_leaves_no_corpse() {
        Stack peasants = new Stack(UnitCatalog.PEASANT, 100, new Hex(3, 1), Side.ATTACKER, 0);
        BattleSetup setup = setup(List.of(peasants));
        peasants.vanish();

        assertThat(setup.hasFreeCorpse(peasants)).isFalse();
    }

    @Test
    void a_berserk_stack_attacks_the_nearest_stack_even_its_own() {
        Stack pikemen = new Stack(UnitCatalog.PIKEMAN, 20, new Hex(5, 5), Side.ATTACKER, 0);
        Stack swordsmen = new Stack(UnitCatalog.GOLD_GOLEM, 10, new Hex(6, 5), Side.ATTACKER, 1);
        pikemen.applySpell(Spell.BERSERK, SkillLevel.BASIC, Integer.MAX_VALUE);
        BattleSetup setup = new BattleSetup(List.of(pikemen, swordsmen),
                List.of(new Stack(UnitCatalog.PEASANT, 10, new Hex(14, 9), Side.DEFENDER, 0)), FIELD);

        List<BattleEvent> events = fight(setup);

        BattleEvent.Melee first = events.stream()
                .filter(e -> e instanceof BattleEvent.Melee m && m.actor() == Side.ATTACKER && m.actorSlot() == 0)
                .map(BattleEvent.Melee.class::cast).findFirst().orElseThrow();
        assertThat(first.target()).isEqualTo(Side.ATTACKER);
        assertThat(pikemen.hasSpell(Spell.BERSERK)).as("endet nach dem Angriff").isFalse();
    }

    @Test
    void advanced_berserk_hits_everyone_around_the_centre() {
        Stack a = new Stack(UnitCatalog.PIKEMAN, 20, new Hex(8, 5), Side.DEFENDER, 0);
        Stack b = new Stack(UnitCatalog.ARCHER, 20, new Hex(9, 5), Side.DEFENDER, 1);
        BattleSetup setup = new BattleSetup(List.of(new Stack(UnitCatalog.PEASANT, 10, new Hex(0, 5), Side.ATTACKER, 0)),
                List.of(a, b), FIELD);

        List<HeroSpellcasting.Cast> casts = ControlSpells.candidates(HeroCatalog.SORSHA, Side.ATTACKER,
                Spell.BERSERK, SkillLevel.ADVANCED, setup);

        assertThat(casts).anyMatch(c -> c.affected().containsAll(List.of(a, b)));
        HeroSpellcasting.Cast both = casts.stream().filter(c -> c.affected().containsAll(List.of(a, b)))
                .findFirst().orElseThrow();
        assertThat(ControlSpells.value(HeroCatalog.SORSHA, Side.ATTACKER, both, setup)).isPositive();
    }

    @Test
    void basic_berserk_is_worth_most_where_the_enemy_would_hit_its_own() {
        Stack orcs = new Stack(UnitCatalog.ORC, 30, new Hex(10, 5), Side.DEFENDER, 0);
        Stack goblins = new Stack(UnitCatalog.GOBLIN, 60, new Hex(11, 5), Side.DEFENDER, 1);
        BattleSetup setup = new BattleSetup(List.of(new Stack(UnitCatalog.ARCHER, 20, new Hex(2, 5), Side.ATTACKER, 0)),
                List.of(orcs, goblins), FIELD);

        List<HeroSpellcasting.Cast> casts = ControlSpells.candidates(HeroCatalog.SORSHA, Side.ATTACKER,
                Spell.BERSERK, SkillLevel.BASIC, setup);

        assertThat(casts).extracting(HeroSpellcasting.Cast::target).containsExactly(orcs, goblins);
        assertThat(ControlSpells.value(HeroCatalog.SORSHA, Side.ATTACKER, casts.getFirst(), setup)).isPositive();
    }

    @Test
    void a_hypnotized_stack_fights_for_the_caster_until_the_spell_ends() {
        Stack pikemen = new Stack(UnitCatalog.PIKEMAN, 20, new Hex(5, 5), Side.ATTACKER, 0);
        Stack imps = new Stack(UnitCatalog.IMP, 5, new Hex(10, 5), Side.DEFENDER, 0);
        Stack demons = new Stack(UnitCatalog.DEMON, 5, new Hex(12, 5), Side.DEFENDER, 1);
        BattleSetup setup = new BattleSetup(List.of(pikemen), List.of(imps, demons), FIELD);

        imps.applySpell(Spell.HYPNOTIZE, SkillLevel.BASIC, 2);

        assertThat(imps.controller()).isEqualTo(Side.ATTACKER);
        assertThat(setup.opponentsOf(imps)).containsExactly(demons);
        assertThat(setup.opponentsOf(demons)).containsExactly(pikemen, imps);
        assertThat(setup.opponentsOf(pikemen)).containsExactly(demons);
        imps.endTurn();
        imps.endTurn();
        assertThat(imps.controller()).isEqualTo(Side.DEFENDER);
    }

    @Test
    void hypnotize_only_takes_stacks_below_its_health_limit() {
        Stack imps = new Stack(UnitCatalog.IMP, 5, new Hex(10, 5), Side.DEFENDER, 0);
        Stack devils = new Stack(UnitCatalog.DEVIL, 5, new Hex(12, 5), Side.DEFENDER, 1);
        BattleSetup setup = new BattleSetup(List.of(new Stack(UnitCatalog.PIKEMAN, 10, new Hex(0, 5), Side.ATTACKER, 0)),
                List.of(imps, devils), FIELD);
        Hero mage = HeroCatalog.SORSHA.withBuild(0, 0, 4, 0, Map.of());

        // Basic, Power 5 (Sorsha 1 + 4): 10 + 125 = 135 > 5 × 4 Imps, < 5 × 160 Devils.
        assertThat(ControlSpells.hypnotizeLimit(SkillLevel.BASIC, mage.spellPower())).isEqualTo(135);
        assertThat(ControlSpells.candidates(mage, Side.ATTACKER, Spell.HYPNOTIZE, SkillLevel.BASIC, setup))
                .extracting(HeroSpellcasting.Cast::target).containsExactly(imps);
    }

    @Test
    void the_hero_sacrifices_cheap_creatures_for_fallen_angels() {
        Stack angels = new Stack(UnitCatalog.ANGEL, 10, new Hex(3, 5), Side.ATTACKER, 0);
        angels.takeDamage(1000, Set.of());
        Stack peasants = new Stack(UnitCatalog.PEASANT, 200, new Hex(3, 1), Side.ATTACKER, 1);
        BattleSetup setup = new BattleSetup(List.of(angels, peasants),
                List.of(new Stack(UnitCatalog.PIKEMAN, 20, new Hex(14, 5), Side.DEFENDER, 0)), FIELD,
                mage(Spell.SACRIFICE, SecondarySkill.FIRE_MAGIC), null);

        List<BattleEvent> events = fight(setup);

        assertThat(events).anyMatch(e -> e instanceof BattleEvent.SpellHit h && h.spell() == Spell.SACRIFICE
                && h.killed() == 200);
        assertThat(events).anyMatch(e -> e instanceof BattleEvent.SpellHit h && h.spell() == Spell.SACRIFICE
                && h.healed() > 0);
        assertThat(setup.hasFreeCorpse(peasants)).isFalse();
    }

    @Test
    void the_hero_hypnotizes_a_small_enemy_stack() {
        BattleSetup setup = new BattleSetup(List.of(new Stack(UnitCatalog.SWORDSMAN, 30, new Hex(3, 5), Side.ATTACKER, 0)),
                List.of(new Stack(UnitCatalog.WOLF_RIDER, 3, new Hex(13, 3), Side.DEFENDER, 0),
                        new Stack(UnitCatalog.ORC, 40, new Hex(14, 7), Side.DEFENDER, 1)),
                FIELD, mage(Spell.HYPNOTIZE, SecondarySkill.AIR_MAGIC), null);

        List<BattleEvent> events = fight(setup);

        assertThat(events).anyMatch(e -> e instanceof BattleEvent.SpellHit h && h.spell() == Spell.HYPNOTIZE
                && h.target() == Side.DEFENDER);
    }

    @Test
    void the_hero_drives_a_cluster_of_enemies_berserk() {
        BattleSetup setup = new BattleSetup(List.of(new Stack(UnitCatalog.ARCHER, 20, new Hex(3, 5), Side.ATTACKER, 0)),
                List.of(new Stack(UnitCatalog.ORC, 30, new Hex(12, 4), Side.DEFENDER, 0),
                        new Stack(UnitCatalog.GOBLIN, 60, new Hex(12, 5), Side.DEFENDER, 1),
                        new Stack(UnitCatalog.OGRE, 10, new Hex(12, 6), Side.DEFENDER, 2)),
                FIELD, mage(Spell.BERSERK, SecondarySkill.FIRE_MAGIC), null);

        List<BattleEvent> events = fight(setup);

        assertThat(events).anyMatch(e -> e instanceof BattleEvent.SpellHit h && h.spell() == Spell.BERSERK
                && !h.resisted());
        assertThat(events).anyMatch(e -> e instanceof BattleEvent.Melee m && m.actor() == Side.DEFENDER
                && m.target() == Side.DEFENDER);
    }

    @Test
    void a_berserk_shooter_shoots_the_nearest_stack() {
        Stack archers = new Stack(UnitCatalog.ARCHER, 20, new Hex(3, 5), Side.ATTACKER, 0);
        Stack pikemen = new Stack(UnitCatalog.PIKEMAN, 20, new Hex(3, 7), Side.ATTACKER, 1);
        archers.applySpell(Spell.BERSERK, SkillLevel.BASIC, Integer.MAX_VALUE);
        BattleSetup setup = new BattleSetup(List.of(archers, pikemen),
                List.of(new Stack(UnitCatalog.PEASANT, 10, new Hex(14, 5), Side.DEFENDER, 0)), FIELD);

        List<BattleEvent> events = fight(setup);

        assertThat(events).anyMatch(e -> e instanceof BattleEvent.Shoot sh && sh.actor() == Side.ATTACKER
                && sh.target() == Side.ATTACKER);
    }

    @Test
    void a_berserk_walker_approaches_a_target_out_of_reach() {
        Stack pikemen = new Stack(UnitCatalog.PIKEMAN, 20, new Hex(3, 5), Side.ATTACKER, 0);
        pikemen.applySpell(Spell.BERSERK, SkillLevel.BASIC, Integer.MAX_VALUE);
        BattleSetup setup = new BattleSetup(List.of(pikemen),
                List.of(new Stack(UnitCatalog.PEASANT, 10, new Hex(14, 5), Side.DEFENDER, 0)), FIELD);

        List<BattleEvent> events = fight(setup);

        BattleEvent.Move first = events.stream().filter(e -> e instanceof BattleEvent.Move m && m.actor() == Side.ATTACKER)
                .map(BattleEvent.Move.class::cast).findFirst().orElseThrow();
        assertThat(first.toQ()).isGreaterThan(3);
    }

    private static List<Stack> targets(Spell spell, SkillLevel level, BattleSetup setup) {
        return ControlSpells.candidates(HeroCatalog.SORSHA, Side.ATTACKER, spell, level, setup).stream()
                .map(c -> Objects.requireNonNull(c.target())).toList();
    }

    private static Hero mage(Spell spell, SecondarySkill school) {
        Map<SecondarySkill, SkillLevel> skills = new HashMap<>(Map.of(SecondarySkill.WISDOM, SkillLevel.EXPERT));
        skills.put(school, SkillLevel.EXPERT);
        return HeroCatalog.SORSHA.withBuild(0, 0, 4, 20, skills).withSpellbook(Set.of(spell));
    }

    private static BattleSetup setup(List<Stack> attackers) {
        return new BattleSetup(attackers, List.of(new Stack(UnitCatalog.PEASANT, 10, new Hex(14, 5), Side.DEFENDER, 0)),
                FIELD);
    }

    private static List<BattleEvent> fight(BattleSetup setup) {
        ListEventCollector collector = new ListEventCollector();
        new Battle(new Random(3), new GreedyAutoSolver(), collector).simulate(setup);
        return collector.events();
    }
}
