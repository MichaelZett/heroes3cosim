package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** SoD: eine beherrschte Schule senkt die Zauberkosten auf den zweiten Wert (heroes.thelazy.net). */
class SodSpellCostsTest {

    @Test
    void a_mastered_school_lowers_the_cost_only_with_sod_rules() {
        Hero airMage = mage(Map.of(SecondarySkill.AIR_MAGIC, SkillLevel.BASIC));
        Hero plain = mage(Map.of());

        assertThat(cost(airMage, Spell.HASTE, true)).isEqualTo(5);
        assertThat(cost(airMage, Spell.HASTE, false)).isEqualTo(6);
        assertThat(cost(plain, Spell.HASTE, true)).isEqualTo(6);
        assertThat(cost(airMage, Spell.LIGHTNING_BOLT, true)).isEqualTo(8);
        assertThat(cost(airMage, Spell.FIREBALL, true)).as("fremde Schule").isEqualTo(15);
        assertThat(cost(airMage, Spell.MAGIC_ARROW, true)).as("Magic Arrow gehört allen vier Schulen").isEqualTo(4);
        assertThat(SodSpellCosts.reduced(Spell.INFERNO)).isEqualTo(12);
        assertThat(SodSpellCosts.reduced(Spell.TELEPORT)).as("eigene Staffel in Teleport").isEqualTo(15);
    }

    private static int cost(Hero hero, Spell spell, boolean sod) {
        BattleSetup setup = new BattleSetup(List.of(new Stack(UnitCatalog.PIKEMAN, 1, new Hex(0, 5), Side.ATTACKER, 0)),
                List.of(new Stack(UnitCatalog.PIKEMAN, 1, new Hex(14, 5), Side.DEFENDER, 0)), Battlefield.STANDARD,
                hero, null).sodSpellCosts(sod);
        return HeroSpellcasting.cost(spell, Side.ATTACKER, setup);
    }

    private static Hero mage(Map<SecondarySkill, SkillLevel> skills) {
        return HeroCatalog.CRAG_HACK.withBuild(0, 0, skills).withSpellbook(Set.of());
    }
}
