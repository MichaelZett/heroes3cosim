package de.zettsystems.h3comsim.battle.domain;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/** Zufälliger Aufstieg nach den Regeln (heroes.thelazy.net). */
class HeroGrowthTest {

    private static Hero grow(String name, int level, long seed) {
        return HeroGrowth.grow(HeroCatalog.byName(name).orElseThrow(), level, new Random(seed));
    }

    private static int primarySum(Hero hero) {
        return hero.attack() + hero.defense() + hero.power() + hero.knowledge();
    }

    private static int skillSteps(Hero hero) {
        return hero.skills().values().stream().mapToInt(SkillLevel::ordinal).sum();
    }

    @Test
    void every_level_up_brings_one_primary_point_and_one_skill_step() {
        Hero base = HeroCatalog.byName("Solmyr").orElseThrow();
        Hero grown = grow("Solmyr", 12, 1);

        assertThat(grown.level()).isEqualTo(12);
        assertThat(primarySum(grown)).isEqualTo(primarySum(base) + 11);
        assertThat(skillSteps(grown)).isEqualTo(skillSteps(base) + 11);
        assertThat(grown.skills()).hasSizeLessThanOrEqualTo(Hero.MAX_SKILLS);
        assertThat(grown).as("gleicher Seed, gleicher Held").isEqualTo(grow("Solmyr", 12, 1));
    }

    @Test
    void class_weights_decide_and_banned_skills_never_appear() {
        for (long seed = 0; seed < 50; seed++) {
            assertThat(grow("Sorsha", 30, seed).skills()).doesNotContainKey(SecondarySkill.NECROMANCY);
            assertThat(grow("Tamika", 30, seed).skills()).doesNotContainKey(SecondarySkill.LEADERSHIP)
                    .doesNotContainKey(SecondarySkill.FIRST_AID);
        }
        assertThat(Arrays.stream(HeroClass.values())
                .mapToInt(c -> Arrays.stream(SecondarySkill.values()).mapToInt(s -> HeroGrowth.skillWeight(c, s)).sum()))
                .as("je Klasse 112 Lose").containsOnly(112);
    }

    @Test
    void magic_heroes_are_offered_wisdom_every_three_levels_might_heroes_every_six() {
        for (long seed = 0; seed < 50; seed++) {
            Hero wizard = new Hero("W", HeroClass.WIZARD, Faction.TOWER, 0, 0, 2, 3,
                    Map.of(SecondarySkill.WISDOM, SkillLevel.BASIC, SecondarySkill.ARCHERY, SkillLevel.BASIC));
            assertThat(HeroGrowth.grow(wizard, 3, new Random(seed)).skills().get(SecondarySkill.WISDOM))
                    .isGreaterThanOrEqualTo(SkillLevel.ADVANCED);
            Hero knight = new Hero("K", HeroClass.KNIGHT, Faction.CASTLE, 2, 2, 1, 1,
                    Map.of(SecondarySkill.WISDOM, SkillLevel.BASIC, SecondarySkill.LEADERSHIP, SkillLevel.BASIC));
            assertThat(HeroGrowth.grow(knight, 6, new Random(seed)).skills().get(SecondarySkill.WISDOM))
                    .isGreaterThanOrEqualTo(SkillLevel.ADVANCED);
        }
    }

    @Test
    void primary_chances_follow_the_class() {
        int attack = 0;
        int power = 0;
        for (long seed = 0; seed < 40; seed++) {
            Hero barbarian = grow("Crag Hack", 9, seed);
            Hero warlock = grow("Alamar", 9, seed);
            attack += barbarian.attack() - 4;
            power += warlock.power() - 3;
        }
        // Barbaren +Attack zu 55 %, Warlocks +Power zu 50 % — je 8 Aufstiege, 40 Helden.
        assertThat(attack).isBetween(140, 210);
        assertThat(power).isBetween(125, 195);
    }

    @Test
    void spells_come_from_the_town_guild_as_far_as_wisdom_and_town_allow() {
        assertThat(grow("Crag Hack", 20, 3).spells()).as("Barbar ohne Zauberbuch").isEmpty();

        Hero novice = grow("Solmyr", 1, 3);
        assertThat(novice.spells()).contains(Spell.CHAIN_LIGHTNING)
                .filteredOn(s -> s.level() == 1).as("Tower mit Library").hasSize(6);
        assertThat(novice.spells()).filteredOn(s -> s.level() == 2).isEmpty();

        Hero journeyman = grow("Solmyr", 2, 3);
        assertThat(journeyman.spells()).filteredOn(s -> s.level() == 2).hasSize(5);

        for (long seed = 0; seed < 20; seed++) {
            Hero battleMage = grow("Gird", 40, seed);
            assertThat(battleMage.spells()).as("Stronghold-Gilde nur bis Stufe 3")
                    .allSatisfy(s -> assertThat(s.level()).isLessThanOrEqualTo(3));
            Hero wizard = grow("Solmyr", 30, seed);
            assertThat(wizard.spells()).allSatisfy(s -> {
                assertThat(s.level()).isLessThanOrEqualTo(wizard.maxSpellLevel());
                assertThat(MageGuild.weight(Faction.TOWER, s) > 0 || s == Spell.CHAIN_LIGHTNING).isTrue();
            });
        }
    }

    @Test
    void the_guild_draws_by_town_weights_without_repeats() {
        for (long seed = 0; seed < 30; seed++) {
            assertThat(MageGuild.draw(Faction.NECROPOLIS, 1, new Random(seed))).hasSize(5)
                    .doesNotContain(Spell.BLESS, Spell.CURE);
            assertThat(MageGuild.draw(Faction.CASTLE, 5, new Random(seed))).as("Castle baut Stufe 5 nicht").isEmpty();
            assertThat(MageGuild.draw(Faction.NECROPOLIS, 2, new Random(seed))).hasSize(4);
            assertThat(MageGuild.draw(Faction.TOWER, 2, new Random(seed))).as("Library: einer mehr").hasSize(5);
            assertThat(MageGuild.draw(Faction.TOWER, 5, new Random(seed))).hasSize(2);
        }
    }
}
