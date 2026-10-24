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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Heldenmagie: Zauberbuch, Mana, Schulen, Schaden und Wirkungsbereich der Zauber, die Zauberwahl
 * und das Zaubern im Rundenablauf.
 */
class HeroSpellTest {

    private static Hero mage(int power, int knowledge, Map<SecondarySkill, SkillLevel> skills, Spell... spells) {
        return new Hero("Mage", HeroClass.WIZARD, Faction.TOWER, 0, 0, power, knowledge, skills)
                .withSpellbook(Set.of(spells));
    }

    private static Stack stack(Unit unit, int count, int q, int r, Side side, int slot) {
        return new Stack(unit, count, new Hex(q, r), side, slot);
    }

    @Test
    void wisdom_gates_the_spell_level() {
        Hero novice = mage(1, 1, Map.of());
        assertThat(novice.maxSpellLevel()).isEqualTo(2);
        assertThat(novice.canLearn(Spell.LIGHTNING_BOLT)).isTrue();
        assertThat(novice.canLearn(Spell.FIREBALL)).isFalse();
        assertThat(novice.canLearn(Spell.FRENZY)).as("nur für Kreaturen").isFalse();
        Set<Spell> tooHigh = Set.of(Spell.IMPLOSION);
        assertThatThrownBy(() -> assertThat(novice.withSpellbook(tooHigh)).isNotNull())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot learn IMPLOSION");

        assertThat(mage(1, 1, Map.of(SecondarySkill.WISDOM, SkillLevel.EXPERT)).canLearn(Spell.IMPLOSION)).isTrue();
    }

    @Test
    void school_skill_sets_the_cast_level_magic_arrow_uses_the_best_school() {
        Hero hero = mage(1, 1, Map.of(SecondarySkill.FIRE_MAGIC, SkillLevel.ADVANCED,
                SecondarySkill.WATER_MAGIC, SkillLevel.EXPERT));

        assertThat(hero.castLevel(Spell.BLOODLUST)).isEqualTo(SkillLevel.ADVANCED);
        assertThat(hero.castLevel(Spell.HASTE)).as("ohne Air Magic auf Basic").isEqualTo(SkillLevel.BASIC);
        assertThat(hero.castLevel(Spell.MAGIC_ARROW)).isEqualTo(SkillLevel.EXPERT);
    }

    @Test
    void mana_spell_power_sorcery_and_resistance() {
        assertThat(mage(0, 3, Map.of()).maxMana()).isEqualTo(30);
        assertThat(mage(0, 3, Map.of(SecondarySkill.INTELLIGENCE, SkillLevel.ADVANCED)).maxMana()).isEqualTo(45);
        assertThat(mage(0, 3, Map.of()).spellPower()).as("mindestens 1").isEqualTo(1);
        assertThat(mage(0, 1, Map.of(SecondarySkill.SORCERY, SkillLevel.EXPERT)).sorceryPercent()).isEqualTo(15);
        assertThat(mage(0, 1, Map.of(SecondarySkill.RESISTANCE, SkillLevel.EXPERT)).resistancePercent()).isEqualTo(20);
    }

    @Test
    void damage_formulas_follow_the_spell_reference() {
        assertThat(SpellDamage.base(Spell.MAGIC_ARROW, SkillLevel.BASIC, 3)).isEqualTo(40);
        assertThat(SpellDamage.base(Spell.LIGHTNING_BOLT, SkillLevel.EXPERT, 2)).isEqualTo(100);
        assertThat(SpellDamage.base(Spell.IMPLOSION, SkillLevel.ADVANCED, 4)).isEqualTo(500);
        assertThat(SpellDamage.base(Spell.BLESS, SkillLevel.EXPERT, 4)).isZero();
    }

    @Test
    void damage_modifiers_sorcery_protection_golems_and_elementals() {
        Hero sorcerer = mage(1, 1, Map.of(SecondarySkill.SORCERY, SkillLevel.ADVANCED));
        Stack pikemen = stack(UnitCatalog.PIKEMAN, 10, 5, 5, Side.DEFENDER, 0);
        assertThat(SpellDamage.toTarget(Spell.FIREBALL, 100, sorcerer, pikemen)).isEqualTo(110);

        pikemen.applySpell(Spell.PROTECTION_FROM_FIRE, SkillLevel.BASIC, 3);
        assertThat(SpellDamage.toTarget(Spell.FIREBALL, 100, sorcerer, pikemen)).isEqualTo(77);
        assertThat(SpellDamage.toTarget(Spell.ICE_BOLT, 100, sorcerer, pikemen)).as("andere Schule").isEqualTo(110);

        Hero plain = mage(1, 1, Map.of());
        assertThat(SpellDamage.toTarget(Spell.MAGIC_ARROW, 100, plain, stack(UnitCatalog.STONE_GOLEM, 1, 0, 0, Side.DEFENDER, 0)))
                .isEqualTo(50);
        assertThat(SpellDamage.toTarget(Spell.MAGIC_ARROW, 100, plain, stack(UnitCatalog.IRON_GOLEM, 1, 0, 0, Side.DEFENDER, 0)))
                .isEqualTo(25);
        Stack water = stack(UnitCatalog.WATER_ELEMENTAL, 1, 0, 0, Side.DEFENDER, 0);
        assertThat(SpellDamage.toTarget(Spell.FIREBALL, 100, plain, water)).isEqualTo(200);
        assertThat(SpellDamage.toTarget(Spell.ICE_BOLT, 100, plain, water)).isZero();
    }

    @Test
    void specialist_adds_three_percent_per_creature_level_span() {
        Hero solmyr = new Hero("Solmyr", HeroClass.WIZARD, Faction.TOWER, 0, 0, 1, 1, Map.of(),
                new HeroSpecialty.SpellDamage(Spell.CHAIN_LIGHTNING, 0), 10);
        // Level 10 gegen Stufe 1: 3 % × 10 = +30 %; gegen Stufe 7: 3 % × 1.
        assertThat(SpellDamage.toTarget(Spell.CHAIN_LIGHTNING, 100, solmyr,
                stack(UnitCatalog.PIKEMAN, 1, 0, 0, Side.DEFENDER, 0))).isEqualTo(130);
        assertThat(SpellDamage.toTarget(Spell.CHAIN_LIGHTNING, 100, solmyr,
                stack(UnitCatalog.ANGEL, 1, 0, 0, Side.DEFENDER, 0))).isEqualTo(103);
    }

    @Test
    void area_spells_hit_by_distance_and_spare_nobody() {
        Stack center = stack(UnitCatalog.PIKEMAN, 10, 5, 5, Side.DEFENDER, 0);
        Stack neighbour = stack(UnitCatalog.PIKEMAN, 10, 6, 5, Side.ATTACKER, 0);
        Stack twoAway = stack(UnitCatalog.PIKEMAN, 10, 7, 5, Side.DEFENDER, 1);
        BattleSetup setup = new BattleSetup(List.of(neighbour), List.of(center, twoAway), Battlefield.STANDARD);

        assertThat(HeroSpellcasting.inArea(Spell.FIREBALL, center.position(), setup)).containsExactlyInAnyOrder(center, neighbour);
        assertThat(HeroSpellcasting.inArea(Spell.FROST_RING, center.position(), setup)).containsExactly(neighbour);
        assertThat(HeroSpellcasting.inArea(Spell.INFERNO, center.position(), setup)).hasSize(3);
    }

    @Test
    void chain_lightning_jumps_to_the_nearest_and_halves_each_time() {
        Stack first = stack(UnitCatalog.PIKEMAN, 10, 10, 5, Side.DEFENDER, 0);
        Stack near = stack(UnitCatalog.PIKEMAN, 10, 10, 7, Side.DEFENDER, 1);
        Stack far = stack(UnitCatalog.PIKEMAN, 10, 14, 1, Side.DEFENDER, 2);
        Stack own = stack(UnitCatalog.PIKEMAN, 10, 7, 7, Side.ATTACKER, 0);
        BattleSetup setup = new BattleSetup(List.of(own), List.of(first, near, far), Battlefield.STANDARD);

        List<Stack> chain = HeroSpellcasting.chain(first, 4, setup);
        assertThat(chain).containsExactly(first, near, own, far);

        HeroSpellcasting.Cast cast = new HeroSpellcasting.Cast(Spell.CHAIN_LIGHTNING, SkillLevel.BASIC, first, chain);
        assertThat(List.of(HeroSpellcasting.baseDamage(cast, 1, 0), HeroSpellcasting.baseDamage(cast, 1, 1),
                HeroSpellcasting.baseDamage(cast, 1, 2))).containsExactly(65, 32, 16);
    }

    @Test
    void no_cast_without_mana_or_spellbook() {
        Stack own = stack(UnitCatalog.PIKEMAN, 10, 0, 5, Side.ATTACKER, 0);
        Stack enemy = stack(UnitCatalog.PIKEMAN, 10, 14, 5, Side.DEFENDER, 0);
        BattleSetup setup = new BattleSetup(List.of(own), List.of(enemy), Battlefield.STANDARD);

        assertThat(HeroSpellcasting.decide(mage(1, 1, Map.of(), Spell.MAGIC_ARROW), Side.ATTACKER, 4, setup)).isNull();
        assertThat(HeroSpellcasting.decide(mage(1, 1, Map.of()), Side.ATTACKER, 50, setup)).isNull();
        assertThat(HeroSpellcasting.decide(null, Side.ATTACKER, 50, setup)).isNull();
        assertThat(HeroSpellcasting.decide(mage(1, 1, Map.of(), Spell.MAGIC_ARROW), Side.ATTACKER, 5, setup))
                .isNotNull();
    }

    @Test
    void prayer_goes_on_the_most_valuable_stack_and_is_not_recast() {
        Stack pikemen = stack(UnitCatalog.PIKEMAN, 10, 0, 5, Side.ATTACKER, 0);
        Stack champions = stack(UnitCatalog.CHAMPION, 10, 0, 3, Side.ATTACKER, 1);
        Stack enemy = stack(UnitCatalog.PIKEMAN, 100, 14, 5, Side.DEFENDER, 0);
        BattleSetup setup = new BattleSetup(List.of(pikemen, champions), List.of(enemy), Battlefield.STANDARD);
        Hero cleric = mage(3, 5, Map.of(SecondarySkill.WISDOM, SkillLevel.ADVANCED), Spell.PRAYER);

        HeroSpellcasting.Cast cast = HeroSpellcasting.decide(cleric, Side.ATTACKER, 50, setup);
        assertThat(cast).isNotNull();
        assertThat(cast.target()).isSameAs(champions);

        champions.applySpell(Spell.PRAYER, SkillLevel.BASIC, 3);
        HeroSpellcasting.Cast next = HeroSpellcasting.decide(cleric, Side.ATTACKER, 50, setup);
        assertThat(next).isNotNull();
        assertThat(next.target()).isSameAs(pikemen);
    }

    @Test
    void fireball_is_aimed_away_from_own_troops() {
        Stack own = stack(UnitCatalog.CHAMPION, 10, 6, 5, Side.ATTACKER, 0);
        Stack nextToOwn = stack(UnitCatalog.PIKEMAN, 20, 7, 5, Side.DEFENDER, 0);
        Stack farther = stack(UnitCatalog.PIKEMAN, 20, 10, 5, Side.DEFENDER, 1);
        BattleSetup setup = new BattleSetup(List.of(own), List.of(nextToOwn, farther), Battlefield.STANDARD);
        Hero mage = mage(5, 5, Map.of(SecondarySkill.WISDOM, SkillLevel.BASIC), Spell.FIREBALL);

        HeroSpellcasting.Cast cast = HeroSpellcasting.decide(mage, Side.ATTACKER, 50, setup);

        assertThat(cast).isNotNull();
        assertThat(cast.affected()).doesNotContain(own).isNotEmpty();
    }

    @Test
    void mass_spells_at_expert_hit_the_whole_side() {
        Stack a = stack(UnitCatalog.PIKEMAN, 10, 0, 5, Side.ATTACKER, 0);
        Stack b = stack(UnitCatalog.ARCHER, 10, 0, 3, Side.ATTACKER, 1);
        Stack enemy = stack(UnitCatalog.PIKEMAN, 100, 14, 5, Side.DEFENDER, 0);
        BattleSetup setup = new BattleSetup(List.of(a, b), List.of(enemy), Battlefield.STANDARD);
        Hero hero = mage(2, 5, Map.of(SecondarySkill.AIR_MAGIC, SkillLevel.EXPERT), Spell.HASTE);

        HeroSpellcasting.Cast cast = HeroSpellcasting.decide(hero, Side.ATTACKER, 50, setup);

        assertThat(cast).isNotNull();
        assertThat(cast.target()).isNull();
        assertThat(cast.affected()).containsExactly(a, b);
    }

    @Test
    void anti_magic_blocks_low_level_spells() {
        Stack pikemen = stack(UnitCatalog.PIKEMAN, 10, 0, 5, Side.ATTACKER, 0);
        pikemen.applySpell(Spell.ANTI_MAGIC, SkillLevel.BASIC, 3);

        assertThat(SpellResistance.isImmune(pikemen, Spell.FIREBALL)).isTrue();
        assertThat(SpellResistance.isImmune(pikemen, Spell.ARMAGEDDON)).isFalse();
    }

    @Test
    void resistance_skill_protects_the_whole_army() {
        Hero resistant = mage(0, 1, Map.of(SecondarySkill.RESISTANCE, SkillLevel.EXPERT));
        Stack pikemen = stack(UnitCatalog.PIKEMAN, 10, 0, 5, Side.ATTACKER, 0);
        Stack dwarves = stack(UnitCatalog.DWARF, 10, 0, 3, Side.ATTACKER, 1);
        Stack enemy = stack(UnitCatalog.PIKEMAN, 10, 14, 5, Side.DEFENDER, 0);
        BattleSetup setup = new BattleSetup(List.of(pikemen, dwarves), List.of(enemy), Battlefield.STANDARD,
                resistant, null);

        assertThat(SpellResistance.resistancePercent(pikemen, setup)).isEqualTo(20);
        assertThat(SpellResistance.resistancePercent(dwarves, setup)).isEqualTo(36);
    }

    @Test
    void hero_casts_once_per_round_until_the_mana_runs_out() {
        Stack own = stack(UnitCatalog.PIKEMAN, 20, 0, 5, Side.ATTACKER, 0);
        Stack enemy = stack(UnitCatalog.PIKEMAN, 30, 14, 5, Side.DEFENDER, 0);
        Hero mage = mage(2, 2, Map.of(), Spell.MAGIC_ARROW);
        BattleSetup setup = new BattleSetup(List.of(own), List.of(enemy), Battlefield.STANDARD, mage, null);
        ListEventCollector collector = new ListEventCollector();

        BattleResult result = new Battle(new Random(3), new GreedyAutoSolver(), collector).simulate(setup);

        List<BattleEvent.HeroSpell> casts = collector.events().stream()
                .filter(BattleEvent.HeroSpell.class::isInstance).map(BattleEvent.HeroSpell.class::cast).toList();
        assertThat(casts).hasSize(4);
        assertThat(casts).extracting(BattleEvent.HeroSpell::manaLeft).containsExactly(15, 10, 5, 0);
        assertThat(casts.size()).isLessThanOrEqualTo(result.turnsTaken());
        List<BattleEvent.SpellHit> hits = collector.events().stream()
                .filter(BattleEvent.SpellHit.class::isInstance).map(BattleEvent.SpellHit.class::cast).toList();
        // Magic Arrow mit Power 2 auf Basic: 2 × 10 + 10.
        assertThat(hits).hasSize(4).allSatisfy(h -> {
            assertThat(h.damage()).isEqualTo(30);
            assertThat(h.target()).isEqualTo(Side.DEFENDER);
        });
        assertThat(hits.getFirst().killed()).isEqualTo(3);
        assertThat(setup.manaOf(Side.ATTACKER)).isZero();
    }

    @Test
    void cure_lifts_negative_spells_and_animate_dead_restores_undead() {
        Stack zombies = stack(UnitCatalog.ZOMBIE, 10, 0, 5, Side.ATTACKER, 0);
        zombies.takeDamage(65, Set.of());
        Stack enemy = stack(UnitCatalog.PIKEMAN, 10, 14, 5, Side.DEFENDER, 0);
        BattleSetup setup = new BattleSetup(List.of(zombies), List.of(enemy), Battlefield.STANDARD);
        Hero necro = mage(1, 5, Map.of(SecondarySkill.WISDOM, SkillLevel.BASIC), Spell.ANIMATE_DEAD);

        HeroSpellcasting.Cast cast = HeroSpellcasting.decide(necro, Side.ATTACKER, 50, setup);

        assertThat(cast).isNotNull();
        assertThat(cast.spell()).isEqualTo(Spell.ANIMATE_DEAD);
        assertThat(HeroSpellcasting.healing(Spell.ANIMATE_DEAD, SkillLevel.BASIC, 1)).isEqualTo(80);
        assertThat(HeroSpellcasting.healing(Spell.CURE, SkillLevel.EXPERT, 2)).isEqualTo(40);

        Stack pikemen = stack(UnitCatalog.PIKEMAN, 10, 0, 3, Side.ATTACKER, 1);
        pikemen.applySpell(Spell.SLOW, SkillLevel.ADVANCED, 3);
        BattleSetup slowed = new BattleSetup(List.of(pikemen), List.of(enemy), Battlefield.STANDARD);
        HeroSpellcasting.Cast cure = HeroSpellcasting.decide(mage(1, 5, Map.of(), Spell.CURE), Side.ATTACKER, 50, slowed);
        assertThat(cure).isNotNull();
        assertThat(cure.target()).isSameAs(pikemen);
    }

    @Test
    void every_damage_spell_has_its_formula() {
        // Power 2 auf Basic/Advanced/Expert, Werte aus der Spell Reference.
        Map<Spell, List<Integer>> expected = Map.ofEntries(
                Map.entry(Spell.MAGIC_ARROW, List.of(30, 40, 50)),
                Map.entry(Spell.LIGHTNING_BOLT, List.of(60, 70, 100)),
                Map.entry(Spell.ICE_BOLT, List.of(50, 60, 90)),
                Map.entry(Spell.DEATH_RIPPLE, List.of(20, 30, 40)),
                Map.entry(Spell.DESTROY_UNDEAD, List.of(30, 40, 70)),
                Map.entry(Spell.FIREBALL, List.of(35, 50, 80)),
                Map.entry(Spell.FROST_RING, List.of(35, 50, 80)),
                Map.entry(Spell.CHAIN_LIGHTNING, List.of(105, 130, 180)),
                Map.entry(Spell.METEOR_SHOWER, List.of(75, 100, 150)),
                Map.entry(Spell.INFERNO, List.of(40, 60, 100)),
                Map.entry(Spell.ARMAGEDDON, List.of(130, 160, 220)),
                Map.entry(Spell.IMPLOSION, List.of(250, 350, 450)));
        expected.forEach((spell, values) -> assertThat(List.of(
                SpellDamage.base(spell, SkillLevel.BASIC, 2),
                SpellDamage.base(spell, SkillLevel.ADVANCED, 2),
                SpellDamage.base(spell, SkillLevel.EXPERT, 2))).as(spell.name()).isEqualTo(values));
        assertThat(HeroSpellcasting.healing(Spell.RESURRECTION, SkillLevel.ADVANCED, 2)).isEqualTo(180);
    }

    @Test
    void elementals_take_double_or_no_damage_from_their_opposites() {
        Hero plain = mage(1, 1, Map.of());
        Stack air = stack(UnitCatalog.AIR_ELEMENTAL, 1, 0, 0, Side.DEFENDER, 0);
        Stack earth = stack(UnitCatalog.EARTH_ELEMENTAL, 1, 0, 0, Side.DEFENDER, 0);
        Stack fire = stack(UnitCatalog.FIRE_ELEMENTAL, 1, 0, 0, Side.DEFENDER, 0);

        assertThat(SpellDamage.toTarget(Spell.LIGHTNING_BOLT, 100, plain, air)).isEqualTo(200);
        assertThat(SpellDamage.toTarget(Spell.METEOR_SHOWER, 100, plain, air)).isZero();
        assertThat(SpellDamage.toTarget(Spell.MAGIC_ARROW, 100, plain, air)).isEqualTo(100);
        assertThat(SpellDamage.toTarget(Spell.CHAIN_LIGHTNING, 100, plain, earth)).isZero();
        assertThat(SpellDamage.toTarget(Spell.METEOR_SHOWER, 100, plain, earth)).isEqualTo(200);
        assertThat(SpellDamage.toTarget(Spell.MAGIC_ARROW, 100, plain, earth)).isEqualTo(100);
        assertThat(SpellDamage.toTarget(Spell.FROST_RING, 100, plain, fire)).isEqualTo(200);
        assertThat(SpellDamage.toTarget(Spell.FIREBALL, 100, plain, fire)).as("immun gegen Feuer").isZero();
    }

    @Test
    void ripple_spares_the_undead_destroy_undead_hits_only_them_armageddon_all() {
        Stack zombies = stack(UnitCatalog.ZOMBIE, 10, 0, 5, Side.ATTACKER, 0);
        Stack pikemen = stack(UnitCatalog.PIKEMAN, 10, 14, 5, Side.DEFENDER, 0);
        Stack skeletons = stack(UnitCatalog.SKELETON, 10, 14, 3, Side.DEFENDER, 1);
        BattleSetup setup = new BattleSetup(List.of(zombies), List.of(pikemen, skeletons), Battlefield.STANDARD);
        Hero hero = mage(1, 5, Map.of(SecondarySkill.WISDOM, SkillLevel.ADVANCED));

        assertThat(HeroSpellcasting.candidates(hero, Side.ATTACKER, Spell.DEATH_RIPPLE, setup).getFirst().affected())
                .containsExactly(pikemen);
        assertThat(HeroSpellcasting.candidates(hero, Side.ATTACKER, Spell.DESTROY_UNDEAD, setup).getFirst().affected())
                .containsExactly(zombies, skeletons);
        assertThat(HeroSpellcasting.candidates(hero, Side.ATTACKER, Spell.ARMAGEDDON, setup).getFirst().affected())
                .hasSize(3);
    }

    @Test
    void the_heuristic_values_only_what_a_spell_changes() {
        Stack archers = stack(UnitCatalog.ARCHER, 10, 0, 5, Side.ATTACKER, 0);
        Stack pikemen = stack(UnitCatalog.PIKEMAN, 10, 0, 3, Side.ATTACKER, 1);
        Stack enemyArchers = stack(UnitCatalog.ARCHER, 10, 14, 5, Side.DEFENDER, 0);
        Stack dragons = stack(UnitCatalog.BLACK_DRAGON, 1, 14, 3, Side.DEFENDER, 1);
        BattleSetup setup = new BattleSetup(List.of(archers, pikemen), List.of(enemyArchers, dragons),
                Battlefield.STANDARD);
        SkillLevel basic = SkillLevel.BASIC;

        assertThat(HeroSpellcasting.gainPercent(Spell.BLOODLUST, basic, archers, setup)).isZero();
        assertThat(HeroSpellcasting.gainPercent(Spell.PRECISION, basic, archers, setup)).isEqualTo(15);
        assertThat(HeroSpellcasting.gainPercent(Spell.PRECISION, basic, pikemen, setup)).isZero();
        assertThat(HeroSpellcasting.gainPercent(Spell.FORGETFULNESS, basic, enemyArchers, setup)).isEqualTo(50);
        assertThat(HeroSpellcasting.gainPercent(Spell.FORGETFULNESS, basic, dragons, setup)).isZero();
        assertThat(HeroSpellcasting.gainPercent(Spell.SLOW, SkillLevel.ADVANCED, dragons, setup)).isEqualTo(50);
        assertThat(HeroSpellcasting.gainPercent(Spell.SLOW, basic, enemyArchers, setup)).isEqualTo(5);
        assertThat(HeroSpellcasting.gainPercent(Spell.BLESS, basic, pikemen, setup)).as("1–3 Schaden").isEqualTo(50);
        assertThat(HeroSpellcasting.gainPercent(Spell.STONE_SKIN, basic, pikemen, setup)).isEqualTo(7.5);
        // Die gegnerischen Schützen stellen nur einen kleinen Teil des Gegnerwerts.
        assertThat(HeroSpellcasting.gainPercent(Spell.AIR_SHIELD, basic, pikemen, setup)).isBetween(0.0, 10.0);
        assertThat(HeroSpellcasting.gainPercent(Spell.SLAYER, basic, pikemen, setup)).isGreaterThan(30);
        assertThat(HeroSpellcasting.gainPercent(Spell.FORTUNE, basic, pikemen, setup)).isEqualTo(4);
        assertThat(HeroSpellcasting.gainPercent(Spell.MISFORTUNE, basic, dragons, setup)).as("ohne Glück").isZero();
        assertThat(HeroSpellcasting.gainPercent(Spell.MIRTH, basic, pikemen, setup)).isEqualTo(4);
        assertThat(HeroSpellcasting.gainPercent(Spell.SORROW, basic, enemyArchers, setup)).isEqualTo(4);
        assertThat(HeroSpellcasting.gainPercent(Spell.COUNTERSTRIKE, basic, pikemen, setup)).isEqualTo(10);
        assertThat(HeroSpellcasting.gainPercent(Spell.FIRE_SHIELD, basic, archers, setup)).isZero();
        assertThat(HeroSpellcasting.gainPercent(Spell.BLIND, basic, enemyArchers, setup)).isEqualTo(70);
        assertThat(HeroSpellcasting.gainPercent(Spell.DISRUPTING_RAY, SkillLevel.EXPERT, dragons, setup)).isEqualTo(25);
        assertThat(HeroSpellcasting.gainPercent(Spell.PROTECTION_FROM_FIRE, basic, pikemen, setup)).isZero();
        assertThat(HeroSpellcasting.gainPercent(Spell.PRAYER, basic, pikemen, null)).isEqualTo(20);
        assertThat(HeroSpellcasting.gainPercent(Spell.AIR_SHIELD, basic, pikemen, null)).isZero();
    }

    @Test
    void stronger_curse_forgetfulness_and_stacking_disrupting_ray() {
        Stack pikemen = stack(UnitCatalog.PIKEMAN, 10, 0, 5, Side.ATTACKER, 0);
        pikemen.applySpell(Spell.DISRUPTING_RAY, SkillLevel.BASIC, 2);
        pikemen.applySpell(Spell.DISRUPTING_RAY, SkillLevel.EXPERT, 2);
        assertThat(pikemen.getDefense()).isEqualTo(Math.max(0, 5 - 3 - 5));

        Stack archers = stack(UnitCatalog.ARCHER, 10, 0, 3, Side.ATTACKER, 1);
        archers.applySpell(Spell.FORGETFULNESS, SkillLevel.BASIC, 3);
        assertThat(archers.canShoot()).isTrue();
        int halved = archers.calculateCurrentDamage(AttackType.LONG_RANGE, 0, new Random(1));
        archers.applySpell(Spell.FORGETFULNESS, SkillLevel.ADVANCED, 3);
        assertThat(archers.canShoot()).isFalse();
        assertThat(halved).isLessThanOrEqualTo(10 * 3 / 2);

        Stack cursed = stack(UnitCatalog.PIKEMAN, 10, 0, 7, Side.ATTACKER, 2);
        cursed.applySpell(Spell.CURSE, SkillLevel.ADVANCED, 3);
        // Advanced: 80 % des Minimums minus 1, mindestens 1 je Kreatur.
        assertThat(cursed.calculateCurrentDamage(AttackType.HAND_TO_HAND, 0, new Random(1))).isEqualTo(10);
        assertThat(pikemen.spellDamagePercent(MagicSchool.EARTH)).isEqualTo(100);
    }

    @Test
    void hero_cures_and_animates_in_battle_and_resistance_can_stop_a_bolt() {
        Stack zombies = stack(UnitCatalog.ZOMBIE, 10, 0, 5, Side.ATTACKER, 0);
        zombies.takeDamage(65, Set.of());
        Stack slowed = stack(UnitCatalog.PIKEMAN, 10, 0, 3, Side.ATTACKER, 1);
        slowed.applySpell(Spell.SLOW, SkillLevel.ADVANCED, 5);
        Stack dwarves = stack(UnitCatalog.DWARF, 30, 14, 5, Side.DEFENDER, 0);
        Hero healer = mage(2, 4, Map.of(SecondarySkill.WISDOM, SkillLevel.BASIC,
                SecondarySkill.RESISTANCE, SkillLevel.BASIC), Spell.ANIMATE_DEAD, Spell.CURE);
        Hero bolter = mage(2, 2, Map.of(), Spell.LIGHTNING_BOLT);
        BattleSetup setup = new BattleSetup(List.of(zombies, slowed), List.of(dwarves), Battlefield.STANDARD,
                healer, bolter);
        ListEventCollector collector = new ListEventCollector();

        new Battle(new AlwaysResist(4), new GreedyAutoSolver(), collector).simulate(setup);

        List<BattleEvent.SpellHit> hits = collector.events().stream()
                .filter(BattleEvent.SpellHit.class::isInstance).map(BattleEvent.SpellHit.class::cast).toList();
        assertThat(hits).anySatisfy(h -> {
            assertThat(h.spell()).isEqualTo(Spell.ANIMATE_DEAD);
            assertThat(h.healed()).isPositive();
        });
        assertThat(hits).anySatisfy(h -> assertThat(h.spell()).isEqualTo(Spell.CURE));
        // Basic Resistance des Angreifer-Helden: 5 %, und jeder Wurf fällt hier auf 0.
        assertThat(hits).filteredOn(h -> h.spell() == Spell.LIGHTNING_BOLT).isNotEmpty().allSatisfy(h -> {
            assertThat(h.resisted()).isTrue();
            assertThat(h.damage()).isZero();
        });
    }

    /** Jeder Prozentwurf (nextInt(100)) fällt auf 0 — Magieresistenz wehrt immer ab. */
    private static final class AlwaysResist extends Random {
        @java.io.Serial
        private static final long serialVersionUID = 1L;

        AlwaysResist(long seed) {
            super(seed);
        }

        @Override
        public int nextInt(int bound) {
            return bound == 100 ? 0 : super.nextInt(bound);
        }
    }
}
