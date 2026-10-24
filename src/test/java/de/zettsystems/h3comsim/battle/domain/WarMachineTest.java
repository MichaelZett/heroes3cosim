package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.BattleEvent;
import de.zettsystems.h3comsim.battle.domain.events.ListEventCollector;
import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.junit.jupiter.api.Test;

import java.io.Serial;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kriegsmaschinen (Manual S. 117): Ballista mit (2–3) × (Attack des Helden + 1), Artillery mit
 * zweitem Schuss und Doppelschaden-Chance, Munitionswagen, Sanitätszelt.
 */
class WarMachineTest {

    private static Hero hero(int attack, Map<SecondarySkill, SkillLevel> skills) {
        return new Hero("Kanonier", HeroClass.KNIGHT, Faction.CASTLE, attack, 0, 1, 1, skills);
    }

    private static List<BattleEvent.BallistaShot> firstRoundShots(Hero hero) {
        // Die Stacks stehen weit auseinander: in Runde 1 trifft nur die Ballista.
        Stack attacker = new Stack(UnitCatalog.PEASANT, 1, new Hex(0, 5), Side.ATTACKER, 0);
        Stack defender = new Stack(UnitCatalog.PIKEMAN, 50, new Hex(14, 5), Side.DEFENDER, 0);
        BattleSetup setup = new BattleSetup(List.of(attacker), List.of(defender), Battlefield.STANDARD,
                hero, null).equip(Side.ATTACKER, Set.of(WarMachine.BALLISTA));
        ListEventCollector collector = new ListEventCollector();
        new Battle(new LowestRoll(), new GreedyAutoSolver(), collector).simulate(setup);
        return collector.events().stream()
                .filter(BattleEvent.BallistaShot.class::isInstance)
                .map(BattleEvent.BallistaShot.class::cast)
                .limit(hero.ballistaShots())
                .toList();
    }

    @Test
    void the_ballista_scales_with_the_hero_attack_and_uses_its_own_attack_of_ten() {
        // Kleinster Wurf 2 × (4 + 1) = 10; Attack 10 gegen Pikeman-Defense 5 → +25 % → 12.
        List<BattleEvent.BallistaShot> shots = firstRoundShots(hero(4, Map.of()));

        assertThat(shots).hasSize(1);
        assertThat(shots.getFirst().damage()).isEqualTo(12);
        assertThat(shots.getFirst().doubled()).isFalse();
    }

    @Test
    void expert_artillery_fires_twice_and_always_doubles_the_base_damage() {
        // 10 Grundschaden, +25 % Attack-Bonus und +100 % Artillery → 225 % → 22.
        List<BattleEvent.BallistaShot> shots =
                firstRoundShots(hero(4, Map.of(SecondarySkill.ARTILLERY, SkillLevel.EXPERT)));

        assertThat(shots).hasSize(2);
        assertThat(shots).allSatisfy(shot -> {
            assertThat(shot.damage()).isEqualTo(22);
            assertThat(shot.doubled()).isTrue();
        });
    }

    @Test
    void artillery_levels_set_shots_and_double_damage_chance() {
        Hero basic = hero(0, Map.of(SecondarySkill.ARTILLERY, SkillLevel.BASIC));
        Hero advanced = hero(0, Map.of(SecondarySkill.ARTILLERY, SkillLevel.ADVANCED));
        assertThat(basic.ballistaShots()).isEqualTo(1);
        assertThat(basic.artilleryDoubleDamagePercent()).isEqualTo(50);
        assertThat(advanced.ballistaShots()).isEqualTo(2);
        assertThat(advanced.artilleryDoubleDamagePercent()).isEqualTo(75);
    }

    @Test
    void a_ballista_specialist_raises_the_ballista_attack_every_five_levels() {
        // Torosar, Level 25: fünf Spannen → +25 % von 10 → 2.5, aufgerundet 3.
        Hero torosar = HeroCatalog.byName("Torosar").orElseThrow().withLevel(25);
        assertThat(torosar.ballistaAttack()).isEqualTo(13);
        assertThat(torosar.specialty().effectiveInCombat()).isTrue();
    }

    @Test
    void the_ammo_cart_keeps_the_shooters_supplied() {
        Stack archers = new Stack(UnitCatalog.ARCHER, 10, new Hex(0, 5), Side.ATTACKER, 0);
        Stack enemy = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(14, 5), Side.DEFENDER, 0);
        new BattleSetup(List.of(archers), List.of(enemy), Battlefield.STANDARD)
                .equip(Side.ATTACKER, Set.of(WarMachine.AMMO_CART));
        int before = archers.shotsRemaining();

        archers.useShot();

        assertThat(archers.shotsRemaining()).isEqualTo(before);
    }

    @Test
    void the_first_aid_tent_heals_the_top_creature_up_to_its_limit() {
        assertThat(hero(0, Map.of()).firstAidMaxHeal()).isEqualTo(25);
        assertThat(hero(0, Map.of(SecondarySkill.FIRST_AID, SkillLevel.EXPERT)).firstAidMaxHeal()).isEqualTo(100);
        // Rion, First-Aid-Spezialist mit Basic First Aid, Level 10: 50 × 1.5.
        assertThat(HeroCatalog.byName("Rion").orElseThrow().withLevel(10).firstAidMaxHeal()).isEqualTo(75);

        Stack angels = new Stack(UnitCatalog.ANGEL, 2, new Hex(0, 5), Side.ATTACKER, 0);
        angels.takeDamage(30, Set.of());
        assertThat(angels.healTop(100)).isEqualTo(30);
        assertThat(angels.topMissingHealth()).isZero();
    }

    @Test
    void the_tent_tends_the_most_wounded_stack_at_the_end_of_the_round() {
        Stack angels = new Stack(UnitCatalog.ANGEL, 2, new Hex(0, 5), Side.ATTACKER, 0);
        angels.takeDamage(30, Set.of());
        // Genug Pikemen, dass die Schlacht die erste Runde überdauert und das Zelt drankommt.
        Stack enemy = new Stack(UnitCatalog.PIKEMAN, 300, new Hex(14, 5), Side.DEFENDER, 0);
        BattleSetup setup = new BattleSetup(List.of(angels), List.of(enemy), Battlefield.STANDARD,
                hero(0, Map.of(SecondarySkill.FIRST_AID, SkillLevel.EXPERT)), null)
                .equip(Side.ATTACKER, Set.of(WarMachine.FIRST_AID_TENT));
        ListEventCollector collector = new ListEventCollector();

        new Battle(new Random(3), new GreedyAutoSolver(), collector).simulate(setup);

        assertThat(collector.events()).anySatisfy(e -> assertThat(e).isInstanceOfSatisfying(
                BattleEvent.FirstAid.class, aid -> assertThat(aid.healed()).isBetween(1, 100)));
    }

    /** Liefert bei jedem Bereichs-Wurf die Untergrenze; Promille- und Prozentwürfe verfehlen. */
    private static final class LowestRoll extends Random {
        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public int nextInt(int origin, int bound) {
            return origin;
        }

        @Override
        public int nextInt(int bound) {
            return bound - 1;
        }
    }
}
