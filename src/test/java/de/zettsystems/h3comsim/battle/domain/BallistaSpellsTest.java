package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Bless, Curse, Precision und Prayer auf der Ballista (heroes.thelazy.net, War Machine). */
class BallistaSpellsTest {

    private static final Battlefield FIELD = Battlefield.STANDARD;

    @Test
    void bless_and_curse_fix_the_roll_precision_and_prayer_add_attack() {
        BattleSetup setup = setup(null);
        Stack ballista = Objects.requireNonNull(setup.working(Side.ATTACKER, WarMachine.BALLISTA));
        assertThat(BallistaSpells.roll(ballista, new Random(1))).isBetween(2, 3);

        ballista.applySpell(Spell.BLESS, SkillLevel.ADVANCED, 5, 0);
        assertThat(BallistaSpells.roll(ballista, new Random(1))).isEqualTo(4);
        ballista.applySpell(Spell.PRECISION, SkillLevel.BASIC, 5, 0);
        ballista.applySpell(Spell.PRAYER, SkillLevel.EXPERT, 5, 0);
        assertThat(BallistaSpells.attackBonus(ballista)).isEqualTo(3 + 4);

        Stack enemyBallista = Objects.requireNonNull(setup.working(Side.DEFENDER, WarMachine.BALLISTA));
        enemyBallista.applySpell(Spell.CURSE, SkillLevel.ADVANCED, 5, 0);
        assertThat(BallistaSpells.roll(enemyBallista, new Random(1))).isEqualTo(1);
        assertThat(BallistaSpells.attackBonus(null)).isZero();
    }

    @Test
    void the_gain_follows_the_roll_and_the_attack_points() {
        assertThat(BallistaSpells.gainPercent(Spell.BLESS, SkillLevel.BASIC, 0)).isEqualTo(20.0);
        assertThat(BallistaSpells.gainPercent(Spell.BLESS, SkillLevel.EXPERT, 0)).isEqualTo(60.0);
        assertThat(BallistaSpells.gainPercent(Spell.CURSE, SkillLevel.BASIC, 0)).isEqualTo(20.0);
        assertThat(BallistaSpells.gainPercent(Spell.CURSE, SkillLevel.ADVANCED, 0)).isEqualTo(60.0);
        assertThat(BallistaSpells.gainPercent(Spell.PRECISION, SkillLevel.ADVANCED, 0)).isEqualTo(30.0);
        assertThat(BallistaSpells.gainPercent(Spell.PRAYER, SkillLevel.BASIC, 1)).isEqualTo(15.0);
        assertThat(BallistaSpells.gainPercent(Spell.HASTE, SkillLevel.EXPERT, 0)).isZero();
    }

    @Test
    void basic_curse_and_no_ballista_roll_like_the_unit() {
        BattleSetup setup = setup(null);
        Stack ballista = Objects.requireNonNull(setup.working(Side.ATTACKER, WarMachine.BALLISTA));
        ballista.applySpell(Spell.CURSE, SkillLevel.BASIC, 5, 0);
        assertThat(BallistaSpells.roll(ballista, new Random(1))).isEqualTo(2);
        assertThat(BallistaSpells.roll(null, new Random(1))).isBetween(2, 3);
    }

    @Test
    void the_round_value_needs_living_enemies_and_grows_with_the_hero() {
        BattleSetup without = setup(null);
        Stack ballista = Objects.requireNonNull(without.working(Side.ATTACKER, WarMachine.BALLISTA));
        double plain = BallistaSpells.roundValue(ballista, without);
        assertThat(plain).isPositive();

        Hero gunner = HeroCatalog.SORSHA.withBuild(5, 0, Map.of(SecondarySkill.ARTILLERY, SkillLevel.EXPERT));
        BattleSetup with = setup(gunner);
        assertThat(BallistaSpells.roundValue(Objects.requireNonNull(with.working(Side.ATTACKER, WarMachine.BALLISTA)), with))
                .isGreaterThan(plain);

        without.stacksOf(Side.DEFENDER).getFirst().loseTopCreatures(30);
        assertThat(BallistaSpells.roundValue(ballista, without)).isZero();
    }

    @Test
    void the_hero_may_bless_his_ballista_and_curse_the_enemy_one() {
        Hero mage = HeroCatalog.SORSHA.withBuild(0, 0, 0, 10,
                Map.of(SecondarySkill.WISDOM, SkillLevel.BASIC, SecondarySkill.ARTILLERY, SkillLevel.EXPERT))
                .withSpellbook(Set.of(Spell.BLESS, Spell.CURSE));
        BattleSetup setup = setup(mage);
        Stack own = setup.working(Side.ATTACKER, WarMachine.BALLISTA);
        Stack enemy = setup.working(Side.DEFENDER, WarMachine.BALLISTA);

        assertThat(HeroSpellcasting.candidates(mage, Side.ATTACKER, Spell.BLESS, setup))
                .extracting(HeroSpellcasting.Cast::target).contains(own).doesNotContain(enemy);
        assertThat(HeroSpellcasting.candidates(mage, Side.ATTACKER, Spell.CURSE, setup))
                .extracting(HeroSpellcasting.Cast::target).contains(enemy).doesNotContain(own);
        HeroSpellcasting.Cast bless = HeroSpellcasting.candidates(mage, Side.ATTACKER, Spell.BLESS, setup).stream()
                .filter(c -> c.target() == own).findFirst().orElseThrow();
        assertThat(HeroSpellcasting.value(mage, Side.ATTACKER, bless, setup)).isPositive();
    }

    private static BattleSetup setup(Hero hero) {
        return new BattleSetup(List.of(new Stack(UnitCatalog.PIKEMAN, 10, new Hex(0, 5), Side.ATTACKER, 0)),
                List.of(new Stack(UnitCatalog.PIKEMAN, 30, new Hex(14, 5), Side.DEFENDER, 0)), FIELD, hero, null)
                .equip(Side.ATTACKER, Set.of(WarMachine.BALLISTA))
                .equip(Side.DEFENDER, Set.of(WarMachine.BALLISTA));
    }
}
