package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Manual S. 33: „A hero's Attack skill number is added to each of their creature's attack
 * rating" und „A hero's Defense skill is added to each of their army creature's defense rating".
 *
 * <p>Die Primärwerte greifen damit an genau zwei Stellen — {@link Stack#getAttack()} und
 * {@link Stack#getDefense()} — und wirken von dort automatisch durch die gesamte
 * Schadensformel, weil {@code calculateAttackBoniMaliPercentage} und
 * {@code effectiveDefenseAgainst} beide darauf aufsetzen.
 */
class HeroPrimarySkillsTest {

    private final Battlefield battlefield = Battlefield.STANDARD;

    @Test
    void hero_attack_is_added_to_every_creature_of_his_army() {
        Stack withoutHero = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(0, 5), Side.ATTACKER, 0);
        int baseAttack = withoutHero.getAttack();

        Stack led = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(0, 5), Side.ATTACKER, 0);
        Stack second = new Stack(UnitCatalog.ARCHER, 10, new Hex(0, 6), Side.ATTACKER, 1);
        Stack enemy = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(14, 5), Side.DEFENDER, 0);
        new BattleSetup(List.of(led, second), List.of(enemy), battlefield,
                HeroCatalog.CRAG_HACK, null);

        // Crag Hack: Attack 4 — auf jeden Stack seiner Armee, nicht nur auf den ersten.
        assertThat(led.getAttack()).isEqualTo(baseAttack + 4);
        assertThat(second.getAttack()).isEqualTo(UnitCatalog.ARCHER.attack() + 4);
        // Der Gegner bleibt unberührt.
        assertThat(enemy.getAttack()).isEqualTo(UnitCatalog.PIKEMAN.attack());
    }

    @Test
    void hero_defense_is_added_before_the_defend_bonus_applies() {
        // Manual S. 33 addiert den Heldenbonus auf das Defense-Rating der Kreatur; die +20 %
        // aus Defend (S. 47) gelten damit auf die Summe, nicht auf den Kreaturwert allein.
        Stack led = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(0, 5), Side.ATTACKER, 0);
        Stack enemy = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(14, 5), Side.DEFENDER, 0);
        new BattleSetup(List.of(led), List.of(enemy), battlefield, HeroCatalog.TAZAR, null);

        int expectedBase = UnitCatalog.PIKEMAN.defense() + 4;
        assertThat(led.getDefense()).isEqualTo(expectedBase);

        led.defend();
        assertThat(led.getDefense()).isEqualTo(Math.round(expectedBase * 1.2f));
    }

    @Test
    void hero_attack_raises_the_damage_bonus_through_the_existing_formula() {
        // Pikeman gegen Pikeman: Attack 4 gegen Defense 5. Ohne Held ist die Differenz −1,
        // die Formel liegt also im Malus-Ast (−2 % je Punkt) → −2 %. Crag Hacks Attack 4
        // dreht die Differenz auf +3 und damit in den Bonus-Ast (+5 % je Punkt) → +15 %.
        //
        // Die Absolutwerte stehen hier bewusst statt einer Differenz: die Formel knickt bei
        // Differenz 0 von −2 % auf +5 % pro Punkt, ein linearer Aufschlag wäre falsch.
        Stack plain = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(0, 5), Side.ATTACKER, 0);
        Stack led = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(0, 5), Side.ATTACKER, 0);
        Stack enemy = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(14, 5), Side.DEFENDER, 0);
        new BattleSetup(List.of(led), List.of(enemy), battlefield, HeroCatalog.CRAG_HACK, null);

        int defense = enemy.getDefense();
        assertThat(plain.calculateAttackBoniMaliPercentage(defense)).isEqualTo(-2);
        assertThat(led.calculateAttackBoniMaliPercentage(defense)).isEqualTo(15);
    }

    @Test
    void an_army_without_a_hero_keeps_its_plain_creature_values() {
        // Gegenprobe: der Held ist optional, die alten Konstruktoren bleiben heldenfrei.
        Stack stack = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(0, 5), Side.ATTACKER, 0);
        Stack enemy = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(14, 5), Side.DEFENDER, 0);
        new BattleSetup(List.of(stack), List.of(enemy), battlefield);

        assertThat(stack.commander()).isNull();
        assertThat(stack.getAttack()).isEqualTo(UnitCatalog.PIKEMAN.attack());
        assertThat(stack.getDefense()).isEqualTo(UnitCatalog.PIKEMAN.defense());
    }

    @Test
    void every_faction_has_exactly_one_hero() {
        // Stufe 1 führt genau einen Helden je Fraktion — NEUTRAL hat keinen.
        for (Faction faction : Faction.values()) {
            if (faction == Faction.NEUTRAL) {
                assertThat(HeroCatalog.byFaction(faction)).isEmpty();
            } else {
                assertThat(HeroCatalog.byFaction(faction))
                        .as("Held für %s", faction)
                        .isPresent();
            }
        }
        assertThat(HeroCatalog.all()).hasSize(Faction.values().length - 1);
    }

    @Test
    void hero_class_matches_the_faction_of_its_hero() {
        assertThat(HeroCatalog.all())
                .allSatisfy(hero -> assertThat(hero.heroClass().faction()).isEqualTo(hero.faction()));
    }

    @Test
    void secondary_skills_are_carried_but_not_yet_evaluated() {
        // Die Fertigkeiten liegen im Katalog, damit Stufe 2 ihn nicht erneut anfassen muss.
        // Dass sie noch nichts tun, ist hier festgehalten, damit es niemand für einen Bug hält.
        assertThat(HeroCatalog.CRAG_HACK.levelOf(SecondarySkill.OFFENSE))
                .isEqualTo(SkillLevel.ADVANCED);
        assertThat(HeroCatalog.CRAG_HACK.levelOf(SecondarySkill.ARMORER))
                .isEqualTo(SkillLevel.NONE);

        Stack led = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(0, 5), Side.ATTACKER, 0);
        Stack enemy = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(14, 5), Side.DEFENDER, 0);
        new BattleSetup(List.of(led), List.of(enemy), battlefield, HeroCatalog.CRAG_HACK, null);
        // Advanced Offense wäre +20 % Nahkampfschaden — der Attack-Wert allein erklärt den
        // gesamten Unterschied, es kommt noch kein Fertigkeits-Aufschlag dazu.
        assertThat(led.getAttack()).isEqualTo(UnitCatalog.PIKEMAN.attack() + 4);
    }
}
