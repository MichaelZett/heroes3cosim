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
    void catalog_holds_eight_heroes_per_class_and_two_classes_per_faction() {
        // 16 RoE-Klassen plus die zwei Conflux-Klassen aus Armageddon's Blade, je acht Helden.
        assertThat(HeroCatalog.all()).hasSize(HeroClass.values().length * 8);
        for (HeroClass heroClass : HeroClass.values()) {
            assertThat(HeroCatalog.all())
                    .as("Helden der Klasse %s", heroClass)
                    .filteredOn(hero -> hero.heroClass() == heroClass)
                    .hasSize(8);
        }
        for (Faction faction : Faction.values()) {
            assertThat(HeroCatalog.all())
                    .as("Helden für %s", faction)
                    .filteredOn(hero -> hero.faction() == faction)
                    .hasSize(faction == Faction.NEUTRAL ? 0 : 16);
        }
        assertThat(HeroCatalog.all()).extracting(Hero::name).doesNotHaveDuplicates();
    }

    @Test
    void primary_skills_come_from_the_hero_class() {
        assertThat(HeroCatalog.all()).allSatisfy(hero -> {
            HeroClass heroClass = hero.heroClass();
            assertThat(List.of(hero.attack(), hero.defense(), hero.power(), hero.knowledge()))
                    .as(hero.name())
                    .containsExactly(heroClass.attack(), heroClass.defense(), heroClass.power(),
                            heroClass.knowledge());
        });
    }

    @Test
    void every_town_faction_has_a_default_hero_of_its_own() {
        for (Faction faction : Faction.values()) {
            if (faction == Faction.NEUTRAL) {
                assertThat(HeroCatalog.defaultFor(faction)).isEmpty();
            } else {
                assertThat(HeroCatalog.defaultFor(faction))
                        .as("Standardheld für %s", faction)
                        .get()
                        .satisfies(hero -> assertThat(hero.faction()).isEqualTo(faction))
                        .satisfies(hero -> assertThat(HeroCatalog.byName(hero.name())).contains(hero));
            }
        }
        assertThat(HeroCatalog.defaultFor(Faction.STRONGHOLD)).contains(HeroCatalog.CRAG_HACK);
    }

    @Test
    void every_town_faction_has_a_mage_whose_only_combat_edge_is_the_guild_book() {
        assertThat(HeroCatalog.mageFor(Faction.NEUTRAL)).isEmpty();
        for (Faction faction : Faction.values()) {
            if (faction != Faction.NEUTRAL) {
                Hero mage = HeroCatalog.mageFor(faction).orElseThrow();
                assertThat(mage.faction()).as("Magier für %s", faction).isEqualTo(faction);
                assertThat(mage.heroClass().magic()).as(mage.name()).isTrue();
                assertThat(mage.specialty().startingSpells()).as(mage.name()).isEmpty();
                assertThat(mage.spells()).as(mage.name()).isEqualTo(MageGuild.firstLevel(faction));
            }
        }
    }

    @Test
    void catalog_carries_the_manual_start_skills() {
        // Stichproben über beide Handbücher, auch mit Fertigkeiten, die nicht wirken.
        assertThat(HeroCatalog.byName("Melodia").orElseThrow().levelOf(SecondarySkill.LUCK))
                .isEqualTo(SkillLevel.BASIC);
        assertThat(HeroCatalog.byName("Fiona").orElseThrow().levelOf(SecondarySkill.SCOUTING))
                .isEqualTo(SkillLevel.ADVANCED);
        assertThat(HeroCatalog.byName("Luna").orElseThrow().levelOf(SecondarySkill.FIRE_MAGIC))
                .isEqualTo(SkillLevel.BASIC);
        assertThat(HeroCatalog.byName("Brissa").orElseThrow().attack()).isZero();
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

    @Test
    void a_build_adds_primary_points_and_raises_or_adds_skills_but_never_lowers_them() {
        Hero built = HeroCatalog.SORSHA.withBuild(3, 1,
                java.util.Map.of(SecondarySkill.OFFENSE, SkillLevel.EXPERT,
                        SecondarySkill.LEADERSHIP, SkillLevel.NONE,
                        SecondarySkill.LUCK, SkillLevel.ADVANCED));

        assertThat(built.attack()).isEqualTo(5);
        assertThat(built.defense()).isEqualTo(3);
        assertThat(built.levelOf(SecondarySkill.OFFENSE)).isEqualTo(SkillLevel.EXPERT);
        assertThat(built.levelOf(SecondarySkill.LEADERSHIP)).isEqualTo(SkillLevel.BASIC);
        assertThat(built.levelOf(SecondarySkill.LUCK)).isEqualTo(SkillLevel.ADVANCED);
        assertThat(built.specialty()).isEqualTo(HeroCatalog.SORSHA.specialty());
    }
}
