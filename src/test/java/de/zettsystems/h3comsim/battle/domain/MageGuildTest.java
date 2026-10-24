package de.zettsystems.h3comsim.battle.domain;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/** Stufe 1 der Magiergilde je Fraktion und das Startbuch der Helden magischer Klassen. */
class MageGuildTest {

    @Test
    void every_town_offers_five_first_level_combat_spells() {
        for (Faction faction : Faction.values()) {
            if (faction == Faction.NEUTRAL) {
                assertThat(MageGuild.firstLevel(faction)).isEmpty();
                continue;
            }
            assertThat(MageGuild.firstLevel(faction)).as(faction.name()).hasSize(5)
                    .allSatisfy(spell -> {
                        assertThat(spell.level()).isEqualTo(1);
                        assertThat(spell.heroCastable()).isTrue();
                    })
                    .contains(Spell.MAGIC_ARROW, Spell.DISPEL);
        }
    }

    @Test
    void spells_that_never_appear_in_a_town_stay_out() {
        assertThat(MageGuild.firstLevel(Faction.NECROPOLIS)).doesNotContain(Spell.BLESS, Spell.CURE);
        assertThat(MageGuild.firstLevel(Faction.INFERNO)).doesNotContain(Spell.BLESS);
        assertThat(MageGuild.firstLevel(Faction.CASTLE)).doesNotContain(Spell.BLOODLUST, Spell.CURSE);
        assertThat(MageGuild.firstLevel(Faction.NECROPOLIS)).contains(Spell.CURSE);
    }

    @Test
    void magic_heroes_start_with_their_guild_martial_heroes_without() {
        assertThat(HeroCatalog.all()).allSatisfy(hero -> {
            if (hero.heroClass().magic()) {
                assertThat(hero.spells()).as(hero.name()).containsAll(MageGuild.firstLevel(hero.faction()));
            } else {
                assertThat(hero.spells()).as(hero.name()).isEqualTo(hero.specialty().startingSpells());
            }
        });
        assertThat(Arrays.stream(HeroClass.values()).filter(HeroClass::magic)).hasSize(9);
        // Solmyr: Chain Lightning aus der Spezialität, dazu die Gilde von Tower.
        assertThat(HeroCatalog.byName("Solmyr").orElseThrow().spells()).hasSize(6).contains(Spell.CHAIN_LIGHTNING);
    }
}
