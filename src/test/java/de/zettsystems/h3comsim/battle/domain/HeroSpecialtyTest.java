package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.junit.jupiter.api.Test;

import java.io.Serial;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spezialfähigkeiten der Helden in der SoD-Fassung: Kreatur-Spezialisten (+1 Speed, +5 % Attack
 * und Defense je angefangene Spanne von Kreaturstufe Leveln), Fertigkeits-Spezialisten (+5 % je
 * Level auf den Effekt) und die festen Boni der Planeswalker.
 */
class HeroSpecialtyTest {

    private static Stack ledBy(Hero hero, Unit unit) {
        Stack stack = new Stack(unit, 10, new Hex(0, 5), Side.ATTACKER, 0);
        Stack enemy = new Stack(UnitCatalog.PEASANT, 10, new Hex(14, 5), Side.DEFENDER, 0);
        new BattleSetup(List.of(stack), List.of(enemy), Battlefield.STANDARD, hero, null);
        return stack;
    }

    @Test
    void a_creature_specialist_raises_speed_attack_and_defense_of_both_upgrade_levels() {
        // Sorsha, Swordsmen (Stufe 4), Level 1: eine angefangene Spanne → +5 %, aufgerundet.
        Stack swordsmen = ledBy(HeroCatalog.SORSHA, UnitCatalog.SWORDSMAN);
        assertThat(swordsmen.getSpeed()).isEqualTo(UnitCatalog.SWORDSMAN.speed() + 1);
        assertThat(swordsmen.getAttack()).isEqualTo(10 + 2 + 1);
        assertThat(swordsmen.getDefense()).isEqualTo(12 + 2 + 1);

        Stack crusaders = ledBy(HeroCatalog.SORSHA, UnitCatalog.CRUSADER);
        assertThat(crusaders.getSpeed()).isEqualTo(UnitCatalog.CRUSADER.speed() + 1);

        Stack pikemen = ledBy(HeroCatalog.SORSHA, UnitCatalog.PIKEMAN);
        assertThat(pikemen.getSpeed()).isEqualTo(UnitCatalog.PIKEMAN.speed());
        assertThat(pikemen.getAttack()).isEqualTo(UnitCatalog.PIKEMAN.attack() + 2);
    }

    @Test
    void the_creature_bonus_grows_with_every_started_span_of_creature_level_levels() {
        // Level 9 bei Stufe 4: drei angefangene Spannen → +15 %: Attack 10 → +2, Defense 12 → +2.
        Stack swordsmen = ledBy(HeroCatalog.SORSHA.withLevel(9), UnitCatalog.SWORDSMAN);
        assertThat(swordsmen.getAttack()).isEqualTo(10 + 2 + 2);
        assertThat(swordsmen.getDefense()).isEqualTo(12 + 2 + 2);

        // Galthran, Skeletons (Stufe 1), Level 20: +100 % — der Grundwert verdoppelt sich.
        Hero galthran = HeroCatalog.byName("Galthran").orElseThrow().withLevel(20);
        Stack skeletons = ledBy(galthran, UnitCatalog.SKELETON);
        assertThat(skeletons.getAttack()).isEqualTo(5 + galthran.attack() + 5);
    }

    @Test
    void a_skill_specialist_scales_the_skill_effect_by_five_percent_per_level() {
        // Crag Hack: Advanced Offense 20 % × 1.05 auf Level 1, × 1.5 auf Level 10.
        assertThat(HeroCatalog.CRAG_HACK.offenseBonusPerMille()).isEqualTo(210);
        assertThat(HeroCatalog.CRAG_HACK.withLevel(10).offenseBonusPerMille()).isEqualTo(300);
        // Tazar: Advanced Armorer 10 % → 10.5 %.
        assertThat(HeroCatalog.TAZAR.armorerReductionPerMille()).isEqualTo(105);
        // Ohne Spezialität bleibt der Tabellenwert.
        assertThat(HeroCatalog.SORSHA.offenseBonusPerMille()).isEqualTo(100);
    }

    @Test
    void a_planeswalker_adds_fixed_attack_defense_and_damage_to_his_elementals() {
        // Fiur: Fire/Energy Elementals +1 Attack, +2 Defense, +2 Schaden — unabhängig vom Level.
        Stack energy = ledBy(HeroCatalog.FIUR.withLevel(30), UnitCatalog.ENERGY_ELEMENTAL);
        assertThat(energy.getAttack()).isEqualTo(12 + 3 + 1);
        assertThat(energy.getDefense()).isEqualTo(8 + 1 + 2);
        assertThat(energy.getSpeed()).isEqualTo(UnitCatalog.ENERGY_ELEMENTAL.speed());
        // Schaden 4–6 wird zu 6–8 je Kreatur: der kleinste Wurf liefert 6 × 10.
        assertThat(energy.calculateCurrentDamage(AttackType.HAND_TO_HAND, 0, new LowestRoll()))
                .isEqualTo(60);
    }

    @Test
    void every_creature_specialty_covers_a_base_and_an_upgrade() {
        for (Hero hero : HeroCatalog.all()) {
            if (hero.specialty() instanceof HeroSpecialty.Creature(Unit creature)) {
                assertThat(UnitCatalog.all())
                        .as("Kreaturen für %s", hero.name())
                        .filteredOn(unit -> HeroSpecialty.sameCreature(creature, unit))
                        .hasSize(2);
            }
        }
    }

    @Test
    void only_combat_specialties_count_as_effective() {
        assertThat(HeroCatalog.byName("Oris").orElseThrow().specialty().effectiveInCombat())
                .as("Eagle Eye braucht Zauber").isFalse();
        assertThat(HeroCatalog.JENOVA.specialty().effectiveInCombat()).as("Gold").isFalse();
        assertThat(HeroCatalog.NEELA.specialty().effectiveInCombat()).isTrue();
        assertThat(HeroCatalog.TAMIKA.specialty()).isEqualTo(new HeroSpecialty.Creature(UnitCatalog.BLACK_KNIGHT));
    }

    /** Liefert bei jedem Bereichs-Wurf die Untergrenze. */
    private static final class LowestRoll extends Random {
        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public int nextInt(int origin, int bound) {
            return origin;
        }
    }
}
