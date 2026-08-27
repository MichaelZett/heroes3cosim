package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.BattleEvent;
import de.zettsystems.h3comsim.battle.domain.events.ListEventCollector;
import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Die drei Schadens-Fertigkeiten aus Stufe 2:
 * <ul>
 *   <li>Offense (Manual S. 38): +10/20/30 % Nahkampfschaden.</li>
 *   <li>Archery (Manual S. 35): +10/25/50 % Fernkampfschaden.</li>
 *   <li>Armorer (Manual S. 35): −5/10/15 % erlittener Schaden.</li>
 * </ul>
 *
 * <p>Alle drei formuliert das Manual als Prozent auf den <em>zugefügten</em> Schaden, also auf
 * das Ergebnis der Formel von S. 43 — nicht auf den Würfelwurf davor. Geprüft wird über die
 * Schadenswerte im Event-Strom bei festem Seed: identische Aufstellung, einziger Unterschied
 * ist die Fertigkeit des Helden.
 */
class HeroCombatSkillsTest {

    private static final long SEED = 20260827L;

    private static Hero heroWith(SecondarySkill skill, SkillLevel level) {
        return new Hero("Testheld", HeroClass.KNIGHT, Faction.CASTLE, 0, 0, 1, 1,
                Map.of(skill, level));
    }

    /** Held ohne jede Fertigkeit und ohne Primärwerte — isoliert den Fertigkeits-Effekt. */
    private static Hero blankHero() {
        return new Hero("Statist", HeroClass.KNIGHT, Faction.CASTLE, 0, 0, 1, 1, Map.of());
    }

    private static int firstMeleeDamage(Hero attackerHero) {
        return firstDamage(attackerHero, null, UnitCatalog.PIKEMAN, BattleEvent.Melee.class);
    }

    private static int firstShotDamage(Hero attackerHero, Hero defenderHero) {
        return firstDamage(attackerHero, defenderHero, UnitCatalog.MARKSMAN, BattleEvent.Shoot.class);
    }

    /**
     * Die Stacks stehen bewusst sofort in Reichweite — Nahkampf adjazent, Fernkampf auf
     * fünf Hex (unter der 10-Hex-Grenze für den Distanz-Malus). Sonst laufen sie erst
     * mehrere Runden aufeinander zu, der Verlauf hängt am Schaden, und das erste Treffer-
     * Event stammt je nach Held von einem anderen Angriff.
     */
    private static int firstDamage(Hero attackerHero, Hero defenderHero,
                                   Unit attackerUnit, Class<? extends BattleEvent> eventType) {
        Hex defenderHex = eventType == BattleEvent.Melee.class ? new Hex(1, 5) : new Hex(5, 5);
        Stack attacker = new Stack(attackerUnit, 10, new Hex(0, 5), Side.ATTACKER, 0);
        Stack defender = new Stack(UnitCatalog.PIKEMAN, 10, defenderHex, Side.DEFENDER, 0);
        BattleSetup setup = new BattleSetup(List.of(attacker), List.of(defender),
                Battlefield.STANDARD, attackerHero, defenderHero);
        ListEventCollector collector = new ListEventCollector();
        new Battle(new Random(SEED), new GreedyAutoSolver(), collector).simulate(setup);

        return collector.events().stream()
                .filter(eventType::isInstance)
                .mapToInt(e -> e instanceof BattleEvent.Melee m ? m.damage()
                        : ((BattleEvent.Shoot) e).damage())
                .findFirst()
                .orElseThrow(() -> new AssertionError("kein " + eventType.getSimpleName() + "-Event"));
    }

    @Test
    void offense_raises_melee_damage_by_the_documented_percentage() {
        int plain = firstMeleeDamage(blankHero());

        assertThat(firstMeleeDamage(heroWith(SecondarySkill.OFFENSE, SkillLevel.BASIC)))
                .isEqualTo(plain * 110 / 100);
        assertThat(firstMeleeDamage(heroWith(SecondarySkill.OFFENSE, SkillLevel.ADVANCED)))
                .isEqualTo(plain * 120 / 100);
        assertThat(firstMeleeDamage(heroWith(SecondarySkill.OFFENSE, SkillLevel.EXPERT)))
                .isEqualTo(plain * 130 / 100);
    }

    @Test
    void archery_raises_ranged_damage_by_the_documented_percentage() {
        int plain = firstShotDamage(blankHero(), null);

        assertThat(firstShotDamage(heroWith(SecondarySkill.ARCHERY, SkillLevel.BASIC), null))
                .isEqualTo(plain * 110 / 100);
        assertThat(firstShotDamage(heroWith(SecondarySkill.ARCHERY, SkillLevel.ADVANCED), null))
                .isEqualTo(plain * 125 / 100);
        assertThat(firstShotDamage(heroWith(SecondarySkill.ARCHERY, SkillLevel.EXPERT), null))
                .isEqualTo(plain * 150 / 100);
    }

    @Test
    void offense_does_not_touch_ranged_damage() {
        // Manual S. 38 sagt ausdrücklich „hand-to-hand damage" — ein Schütze profitiert nicht.
        int plain = firstShotDamage(blankHero(), null);

        assertThat(firstShotDamage(heroWith(SecondarySkill.OFFENSE, SkillLevel.EXPERT), null))
                .isEqualTo(plain);
    }

    @Test
    void archery_does_not_touch_melee_damage() {
        // Gegenprobe: Archery gilt nur für „ranged attackers" (Manual S. 35).
        int plain = firstMeleeDamage(blankHero());

        assertThat(firstMeleeDamage(heroWith(SecondarySkill.ARCHERY, SkillLevel.EXPERT)))
                .isEqualTo(plain);
    }

    @Test
    void armorer_reduces_incoming_damage_regardless_of_attack_type() {
        int plain = firstShotDamage(blankHero(), blankHero());

        assertThat(firstShotDamage(blankHero(), heroWith(SecondarySkill.ARMORER, SkillLevel.BASIC)))
                .isEqualTo(plain * 95 / 100);
        assertThat(firstShotDamage(blankHero(), heroWith(SecondarySkill.ARMORER, SkillLevel.ADVANCED)))
                .isEqualTo(plain * 90 / 100);
        assertThat(firstShotDamage(blankHero(), heroWith(SecondarySkill.ARMORER, SkillLevel.EXPERT)))
                .isEqualTo(plain * 85 / 100);
    }

    @Test
    void offense_and_armorer_stack_multiplicatively() {
        // Beide Seiten geführt: erst der Aufschlag des Angreifers, dann die Minderung des
        // Verteidigers — die Prozente verrechnen sich nacheinander, nicht als Summe.
        int plain = firstMeleeDamage(blankHero());
        Hero offensive = heroWith(SecondarySkill.OFFENSE, SkillLevel.EXPERT);
        Hero armored = heroWith(SecondarySkill.ARMORER, SkillLevel.EXPERT);

        Stack attacker = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(0, 5), Side.ATTACKER, 0);
        Stack defender = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(1, 5), Side.DEFENDER, 0);
        BattleSetup setup = new BattleSetup(List.of(attacker), List.of(defender),
                Battlefield.STANDARD, offensive, armored);
        ListEventCollector collector = new ListEventCollector();
        new Battle(new Random(SEED), new GreedyAutoSolver(), collector).simulate(setup);

        int combined = collector.events().stream()
                .filter(BattleEvent.Melee.class::isInstance)
                .mapToInt(e -> ((BattleEvent.Melee) e).damage())
                .findFirst()
                .orElseThrow();

        assertThat(combined).isEqualTo(plain * 130 / 100 * 85 / 100);
    }

    @Test
    void a_hero_without_the_skill_changes_nothing() {
        assertThat(blankHero().offenseBonusPercent()).isZero();
        assertThat(blankHero().archeryBonusPercent()).isZero();
        assertThat(blankHero().armorerReductionPercent()).isZero();
        assertThat(firstMeleeDamage(heroWith(SecondarySkill.SCHOLAR, SkillLevel.EXPERT)))
                .isEqualTo(firstMeleeDamage(blankHero()));
    }

    @Test
    void the_catalog_heroes_carry_the_percentages_from_the_manual() {
        // Stichproben über den Katalog: Advanced Offense = +20 %, Advanced Archery = +25 %,
        // Advanced Armorer = −10 %.
        assertThat(HeroCatalog.CRAG_HACK.offenseBonusPercent()).isEqualTo(20);
        assertThat(HeroCatalog.JENOVA.archeryBonusPercent()).isEqualTo(25);
        assertThat(HeroCatalog.TAZAR.armorerReductionPercent()).isEqualTo(10);
        // Sorsha hat Offense nur auf Basic.
        assertThat(HeroCatalog.SORSHA.offenseBonusPercent()).isEqualTo(10);
        // Und niemand bekommt einen Bonus, den er nicht gelernt hat.
        assertThat(HeroCatalog.CRAG_HACK.armorerReductionPercent()).isZero();
    }
}
