package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.BattleEvent;
import de.zettsystems.h3comsim.battle.domain.events.ListEventCollector;
import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.junit.jupiter.api.Test;

import java.io.Serial;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Zauber-Spezialitäten (SoD-Werte von heroes.thelazy.net), Startzauber, die Mana-Kosten mit
 * Magi und Pegasi und die Zauberwahl mit Mana-Budget und Blick auf den gegnerischen Helden.
 */
class SpellSpecialtyAndChoiceTest {

    private static Hero mage(int power, int knowledge, Map<SecondarySkill, SkillLevel> skills, Spell... spells) {
        return new Hero("Mage", HeroClass.WIZARD, Faction.TOWER, 0, 0, power, knowledge, skills)
                .withSpellbook(Set.of(spells));
    }

    private static Stack stack(Unit unit, int count, int q, int r, Side side, int slot) {
        return new Stack(unit, count, new Hex(q, r), side, slot);
    }

    @Test
    void unique_specialties_add_points_by_target_level() {
        HeroSpecialty haste = new HeroSpecialty.UniqueSpell(Spell.HASTE);
        assertThat(haste.spellEffectBonus(Spell.HASTE, 1, UnitCatalog.PIKEMAN)).isEqualTo(3);
        assertThat(haste.spellEffectBonus(Spell.HASTE, 1, UnitCatalog.MAGI)).isEqualTo(2);
        assertThat(haste.spellEffectBonus(Spell.HASTE, 1, UnitCatalog.CHAMPION)).isEqualTo(1);
        assertThat(haste.spellEffectBonus(Spell.HASTE, 1, UnitCatalog.ANGEL)).isZero();
        assertThat(haste.spellEffectBonus(Spell.SLOW, 1, UnitCatalog.PIKEMAN)).isZero();

        HeroSpecialty slayer = new HeroSpecialty.UniqueSpell(Spell.SLAYER);
        assertThat(slayer.spellEffectBonus(Spell.SLAYER, 1, UnitCatalog.PIKEMAN)).isEqualTo(4);
        assertThat(slayer.spellEffectBonus(Spell.SLAYER, 1, UnitCatalog.MAGI)).isEqualTo(1);
        assertThat(slayer.spellEffectBonus(Spell.SLAYER, 1, UnitCatalog.CHAMPION)).isZero();
        assertThat(new HeroSpecialty.UniqueSpell(Spell.FORTUNE).spellEffectBonus(Spell.FORTUNE, 1, UnitCatalog.ANGEL))
                .isEqualTo(3);
        assertThat(new HeroSpecialty.UniqueSpell(Spell.DISRUPTING_RAY)
                .spellEffectBonus(Spell.DISRUPTING_RAY, 1, UnitCatalog.ANGEL)).isEqualTo(2);
        // Adela auf Level 18: 54 % für Halberdiers (Stufe 1), 9 % für Champions (Stufe 6).
        HeroSpecialty bless = new HeroSpecialty.UniqueSpell(Spell.BLESS);
        assertThat(bless.spellEffectBonus(Spell.BLESS, 18, UnitCatalog.HALBERDIER)).isEqualTo(54);
        assertThat(bless.spellEffectBonus(Spell.BLESS, 18, UnitCatalog.CHAMPION)).isEqualTo(9);
    }

    @Test
    void the_stack_applies_the_specialty_points() {
        Stack hasted = stack(UnitCatalog.PIKEMAN, 10, 0, 5, Side.ATTACKER, 0);
        hasted.applySpell(Spell.HASTE, SkillLevel.BASIC, 3, 3);
        assertThat(hasted.getSpeed()).isEqualTo(4 + 3 + 3);

        Stack lucky = stack(UnitCatalog.PIKEMAN, 10, 0, 3, Side.ATTACKER, 1);
        lucky.applySpell(Spell.FORTUNE, SkillLevel.BASIC, 3, 3);
        assertThat(lucky.getLuck()).isEqualTo(3);

        Stack slaying = stack(UnitCatalog.PIKEMAN, 10, 0, 7, Side.ATTACKER, 2);
        slaying.applySpell(Spell.SLAYER, SkillLevel.BASIC, 3, 4);
        assertThat(slaying.attackAgainst(stack(UnitCatalog.BLACK_DRAGON, 1, 5, 5, Side.DEFENDER, 0),
                AttackType.HAND_TO_HAND)).isEqualTo(4 + 8 + 4);

        Stack blessed = stack(UnitCatalog.CHAMPION, 2, 0, 9, Side.ATTACKER, 3);
        blessed.applySpell(Spell.BLESS, SkillLevel.BASIC, 3, 30);
        assertThat(blessed.calculateCurrentDamage(AttackType.HAND_TO_HAND, 0, new Random(1)))
                .isEqualTo(25 * 130 / 100 * 2);

        Stack rayed = stack(UnitCatalog.CHAMPION, 2, 2, 9, Side.ATTACKER, 4);
        rayed.applySpell(Spell.DISRUPTING_RAY, SkillLevel.BASIC, 3, 2);
        assertThat(rayed.getDefense()).isEqualTo(16 - 3 - 2);
    }

    @Test
    void spell_specialists_start_with_their_spell_even_above_the_wisdom_gate() {
        Hero alamar = HeroCatalog.byName("Alamar").orElseThrow();
        assertThat(alamar.spells()).contains(Spell.RESURRECTION);
        assertThat(alamar.canLearn(Spell.RESURRECTION)).as("Basic Wisdom: nur bis Stufe 3").isFalse();

        Hero withBook = alamar.withSpellbook(Set.of(Spell.LIGHTNING_BOLT, Spell.RESURRECTION));
        assertThat(withBook.spells()).contains(Spell.LIGHTNING_BOLT, Spell.RESURRECTION);

        assertThat(HeroCatalog.byName("Astral").orElseThrow().spells()).as("Hypnotize fehlt im Simulator")
                .isEqualTo(MageGuild.firstLevel(Faction.TOWER));
        assertThat(Arrays.stream(Faction.values()).map(HeroCatalog::defaultFor)
                .flatMap(java.util.Optional::stream))
                .as("Standardhelden ohne Startzauber").allSatisfy(h -> assertThat(h.spells()).isEmpty());
    }

    @Test
    void common_specialty_strengthens_healing_by_target_level() {
        Hero thant = HeroCatalog.byName("Thant").orElseThrow().withLevel(10);
        Stack zombies = stack(UnitCatalog.ZOMBIE, 10, 0, 5, Side.ATTACKER, 0);
        int plain = HeroSpellcasting.healing(Spell.ANIMATE_DEAD, SkillLevel.BASIC, thant.spellPower());

        // Zombies sind Stufe 2: 3 % × 10 / 2 = +15 %.
        assertThat(HeroSpellcasting.healing(thant, Spell.ANIMATE_DEAD, SkillLevel.BASIC, zombies))
                .isEqualTo(plain * 115 / 100);
    }

    @Test
    void magi_lower_and_pegasi_raise_the_spell_cost() {
        Stack magi = stack(UnitCatalog.MAGI, 5, 0, 5, Side.ATTACKER, 0);
        Stack pegasi = stack(UnitCatalog.PEGASUS, 5, 14, 5, Side.DEFENDER, 0);
        Stack pikemen = stack(UnitCatalog.PIKEMAN, 5, 14, 3, Side.DEFENDER, 1);
        BattleSetup setup = new BattleSetup(List.of(magi), List.of(pegasi, pikemen), Battlefield.STANDARD);

        assertThat(HeroSpellcasting.cost(Spell.LIGHTNING_BOLT, Side.ATTACKER, setup)).as("−2 +2").isEqualTo(10);
        assertThat(HeroSpellcasting.cost(Spell.LIGHTNING_BOLT, Side.DEFENDER, setup)).isEqualTo(10);
        pegasi.takeDamage(10_000, Set.of());
        assertThat(HeroSpellcasting.cost(Spell.MAGIC_ARROW, Side.ATTACKER, setup)).isEqualTo(3);
        assertThat(HeroSpellcasting.cost(Spell.MAGIC_ARROW, Side.DEFENDER, setup)).isEqualTo(5);
    }

    @Test
    void magi_stretch_the_mana_in_battle() {
        Stack magi = stack(UnitCatalog.MAGI, 10, 0, 5, Side.ATTACKER, 0);
        Stack enemy = stack(UnitCatalog.PIKEMAN, 200, 14, 5, Side.DEFENDER, 0);
        Hero hero = mage(1, 1, Map.of(), Spell.MAGIC_ARROW);
        BattleSetup setup = new BattleSetup(List.of(magi), List.of(enemy), Battlefield.STANDARD, hero, null);
        ListEventCollector collector = new ListEventCollector();

        new Battle(new Random(2), new GreedyAutoSolver(), collector).simulate(setup);

        assertThat(collector.events().stream().filter(BattleEvent.HeroSpell.class::isInstance)
                .map(e -> ((BattleEvent.HeroSpell) e).manaLeft()).toList()).containsExactly(7, 4, 1);
    }

    @Test
    void scarce_mana_buys_two_cheap_spells_instead_of_one_dear() {
        Stack own = stack(UnitCatalog.PIKEMAN, 10, 0, 5, Side.ATTACKER, 0);
        Stack enemy = stack(UnitCatalog.CHAMPION, 20, 14, 5, Side.DEFENDER, 0);
        BattleSetup setup = new BattleSetup(List.of(own), List.of(enemy), Battlefield.STANDARD);
        Hero hero = mage(1, 1, Map.of(), Spell.MAGIC_ARROW, Spell.LIGHTNING_BOLT);

        // Power 1: Lightning Bolt 35 Schaden einmal, Magic Arrow 20 Schaden zweimal.
        HeroSpellcasting.Cast scarce = HeroSpellcasting.decide(hero, Side.ATTACKER, 10, setup);
        HeroSpellcasting.Cast plenty = HeroSpellcasting.decide(hero, Side.ATTACKER, 100, setup);
        HeroSpellcasting.Cast lastRound = HeroSpellcasting.decide(hero, Side.ATTACKER, 10, setup,
                HeroSpellcasting.EXPECTED_BATTLE_ROUNDS);

        assertThat(scarce).isNotNull();
        assertThat(scarce.spell()).isEqualTo(Spell.MAGIC_ARROW);
        assertThat(plenty).isNotNull();
        assertThat(plenty.spell()).isEqualTo(Spell.LIGHTNING_BOLT);
        assertThat(lastRound).isNotNull();
        assertThat(lastRound.spell()).as("keine Runde mehr zum Sparen").isEqualTo(Spell.LIGHTNING_BOLT);
    }

    @Test
    void protection_and_anti_magic_count_only_against_a_casting_enemy() {
        Stack champions = stack(UnitCatalog.CHAMPION, 10, 0, 5, Side.ATTACKER, 0);
        Stack pikemen = stack(UnitCatalog.PIKEMAN, 10, 0, 3, Side.ATTACKER, 1);
        Stack enemy = stack(UnitCatalog.PIKEMAN, 50, 14, 5, Side.DEFENDER, 0);
        Hero guard = mage(3, 5, Map.of(SecondarySkill.WISDOM, SkillLevel.BASIC),
                Spell.PROTECTION_FROM_AIR, Spell.ANTI_MAGIC);
        Hero bolter = mage(4, 5, Map.of(), Spell.LIGHTNING_BOLT);

        BattleSetup quiet = new BattleSetup(List.of(champions, pikemen), List.of(enemy), Battlefield.STANDARD,
                guard, null);
        assertThat(HeroSpellcasting.decide(guard, Side.ATTACKER, 50, quiet)).isNull();

        BattleSetup threatened = new BattleSetup(List.of(champions, pikemen), List.of(enemy), Battlefield.STANDARD,
                guard, bolter);
        HeroSpellcasting.Cast cast = HeroSpellcasting.decide(guard, Side.ATTACKER, 50, threatened);
        assertThat(cast).isNotNull();
        assertThat(cast.spell()).isIn(Spell.PROTECTION_FROM_AIR, Spell.ANTI_MAGIC);
        assertThat(cast.target()).as("der wertvollste Stack").isSameAs(champions);
        assertThat(HeroSpellcasting.threat(champions, s -> true, threatened)).isPositive();
        champions.applySpell(Spell.ANTI_MAGIC, SkillLevel.BASIC, 3);
        assertThat(HeroSpellcasting.threat(champions, s -> true, threatened)).isZero();
    }

    @Test
    void dispel_reaches_own_stacks_on_basic_any_on_advanced_all_on_expert() {
        Stack slowed = stack(UnitCatalog.CHAMPION, 10, 0, 5, Side.ATTACKER, 0);
        slowed.applySpell(Spell.SLOW, SkillLevel.ADVANCED, 5);
        Stack hasted = stack(UnitCatalog.CHAMPION, 10, 14, 5, Side.DEFENDER, 0);
        hasted.applySpell(Spell.PRAYER, SkillLevel.ADVANCED, 5);
        BattleSetup setup = new BattleSetup(List.of(slowed), List.of(hasted), Battlefield.STANDARD);

        HeroSpellcasting.Cast basic = HeroSpellcasting.decide(mage(1, 5, Map.of(), Spell.DISPEL),
                Side.ATTACKER, 50, setup);
        assertThat(basic).isNotNull();
        assertThat(basic.target()).isSameAs(slowed);

        List<HeroSpellcasting.Cast> advanced = HeroSpellcasting.candidates(
                mage(1, 5, Map.of(SecondarySkill.WATER_MAGIC, SkillLevel.ADVANCED), Spell.DISPEL),
                Side.ATTACKER, Spell.DISPEL, setup);
        assertThat(advanced).extracting(HeroSpellcasting.Cast::target).containsExactly(slowed, hasted);

        List<HeroSpellcasting.Cast> expert = HeroSpellcasting.candidates(
                mage(1, 5, Map.of(SecondarySkill.WATER_MAGIC, SkillLevel.EXPERT), Spell.DISPEL),
                Side.ATTACKER, Spell.DISPEL, setup);
        assertThat(expert).singleElement().satisfies(c -> assertThat(c.affected()).containsExactly(slowed, hasted));
    }

    @Test
    void dispel_on_own_dwarves_ignores_their_resistance() {
        Stack dwarves = stack(UnitCatalog.DWARF, 20, 0, 5, Side.ATTACKER, 0);
        dwarves.applySpell(Spell.SLOW, SkillLevel.ADVANCED, 10);
        Stack enemy = stack(UnitCatalog.PIKEMAN, 30, 14, 5, Side.DEFENDER, 0);
        Hero cleric = mage(1, 1, Map.of(), Spell.DISPEL);
        BattleSetup setup = new BattleSetup(List.of(dwarves), List.of(enemy), Battlefield.STANDARD, cleric, null);
        ListEventCollector collector = new ListEventCollector();

        new Battle(new AlwaysResist(3), new GreedyAutoSolver(), collector).simulate(setup);

        assertThat(collector.events()).anySatisfy(e -> assertThat(e).isInstanceOfSatisfying(BattleEvent.SpellHit.class,
                h -> {
                    assertThat(h.spell()).isEqualTo(Spell.DISPEL);
                    assertThat(h.resisted()).isFalse();
                    assertThat(h.targetAfter().side()).isEqualTo(Side.ATTACKER);
                }));
    }

    @Test
    void specialist_values_his_spell_higher() {
        Stack pikemen = stack(UnitCatalog.PIKEMAN, 10, 0, 5, Side.ATTACKER, 0);
        Stack enemy = stack(UnitCatalog.PIKEMAN, 10, 14, 5, Side.DEFENDER, 0);
        BattleSetup setup = new BattleSetup(List.of(pikemen), List.of(enemy), Battlefield.STANDARD);

        assertThat(HeroSpellcasting.gainPercent(Spell.HASTE, SkillLevel.BASIC, 3, pikemen, setup))
                .isGreaterThan(HeroSpellcasting.gainPercent(Spell.HASTE, SkillLevel.BASIC, pikemen, setup));
        assertThat(HeroSpellcasting.gainPercent(Spell.FORTUNE, SkillLevel.BASIC, 3, pikemen, setup)).isEqualTo(12);
        assertThat(HeroSpellcasting.gainPercent(Spell.MISFORTUNE, SkillLevel.BASIC, pikemen, setup)).isZero();
    }

    /** Jeder Prozentwurf (nextInt(100)) fällt auf 0 — Magieresistenz würde immer abwehren. */
    private static final class AlwaysResist extends Random {
        @Serial
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
