package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.BattleEvent;
import de.zettsystems.h3comsim.battle.domain.events.ListEventCollector;
import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.junit.jupiter.api.Test;

import java.io.Serial;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Glück (Manual S. 44): ein Armee-Rating 0..3 aus Luck-Fertigkeit, gesammeltem Glück und der
 * Devil-Aura der Gegenseite; bei positivem Glück 4.2 / 8.3 / 12.5 % Chance auf doppelten
 * Grundschaden.
 */
class LuckTest {

    private static final long SEED = 20261023L;

    private static Hero heroWithLuck(SkillLevel level) {
        return new Hero("Glückspilz", HeroClass.DRUID, Faction.RAMPART, 0, 0, 1, 1,
                Map.of(SecondarySkill.LUCK, level));
    }

    private static Hero blankHero() {
        return new Hero("Statist", HeroClass.KNIGHT, Faction.CASTLE, 0, 0, 1, 1, Map.of());
    }

    private static List<Stack> army(Unit unit) {
        return List.of(new Stack(unit, 1, new Hex(0, 5), Side.DEFENDER, 0));
    }

    @Test
    void luck_skill_and_collected_luck_add_up() {
        List<Stack> enemy = army(UnitCatalog.PIKEMAN);

        assertThat(ArmyLuck.ratingOf(heroWithLuck(SkillLevel.BASIC), CollectedBonus.NONE, enemy))
                .isEqualTo(1);
        assertThat(ArmyLuck.ratingOf(heroWithLuck(SkillLevel.ADVANCED), new CollectedBonus(1, 0), enemy))
                .isEqualTo(3);
        assertThat(HeroCatalog.byName("Melodia").orElseThrow().luckBonus()).isEqualTo(1);
    }

    @Test
    void luck_is_capped_at_three_and_never_negative() {
        assertThat(ArmyLuck.ratingOf(heroWithLuck(SkillLevel.EXPERT), new CollectedBonus(3, 0),
                army(UnitCatalog.PIKEMAN))).isEqualTo(3);
        // SoD kennt kein negatives Glück: die Devil-Aura drückt 0 nicht unter 0.
        assertThat(ArmyLuck.ratingOf(blankHero(), CollectedBonus.NONE, army(UnitCatalog.DEVIL)))
                .isZero();
    }

    @Test
    void devils_lower_enemy_luck_by_one_once_per_army() {
        List<Stack> twoDevilStacks = List.of(
                new Stack(UnitCatalog.DEVIL, 1, new Hex(0, 5), Side.DEFENDER, 0),
                new Stack(UnitCatalog.ARCH_DEVIL, 1, new Hex(0, 6), Side.DEFENDER, 1));

        assertThat(ArmyLuck.ratingOf(heroWithLuck(SkillLevel.EXPERT), CollectedBonus.NONE, twoDevilStacks))
                .isEqualTo(2);
    }

    @Test
    void an_army_without_a_hero_has_no_luck_even_with_a_bonus() {
        assertThat(ArmyLuck.ratingOf(null, new CollectedBonus(3, 0), army(UnitCatalog.PIKEMAN))).isZero();
    }

    @Test
    void collected_bonus_is_range_checked() {
        assertThatThrownBy(() -> new CollectedBonus(4, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CollectedBonus(-1, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CollectedBonus(0, 4)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void collected_morale_adds_to_the_army_rating_only_with_a_hero() {
        List<Stack> stacks = List.of(new Stack(UnitCatalog.PIKEMAN, 1, new Hex(0, 5), Side.ATTACKER, 0));
        List<Stack> enemy = army(UnitCatalog.GOBLIN);
        int base = ArmyMorale.ratingOf(stacks, blankHero(), enemy);

        assertThat(ArmyMorale.ratingOf(stacks, blankHero(), new CollectedBonus(0, 2), enemy))
                .isEqualTo(base + 2);
        assertThat(ArmyMorale.ratingOf(stacks, null, new CollectedBonus(0, 2), enemy))
                .isEqualTo(ArmyMorale.ratingOf(stacks, null, enemy));
    }

    @Test
    void lucky_damage_adds_one_hundred_percent_to_an_attack_bonus_and_doubles_a_defense_malus() {
        assertThat(Battle.luckyDamagePercent(0)).isEqualTo(200);
        assertThat(Battle.luckyDamagePercent(15)).isEqualTo(215);
        assertThat(Battle.luckyDamagePercent(-20)).isEqualTo(160);
    }

    @Test
    void a_lucky_strike_doubles_the_damage_and_is_announced() {
        ListEventCollector plain = new ListEventCollector();
        ListEventCollector lucky = new ListEventCollector();
        simulate(CollectedBonus.NONE, plain);
        simulate(new CollectedBonus(3, 0), lucky);

        // Pikeman (Attack 4) gegen Pikeman (Defense 5): −2 %. Ein Glückstreffer verdoppelt diese
        // 98 % auf 196 % — bis auf die Rundung genau das Doppelte des gleichen Würfelwurfs.
        assertThat(firstMeleeDamage(lucky)).isCloseTo(2 * firstMeleeDamage(plain), within(1));
        assertThat(lucky.events()).anySatisfy(e -> assertThat(e).isInstanceOf(BattleEvent.LuckyStrike.class));
        assertThat(plain.events()).noneSatisfy(e -> assertThat(e).isInstanceOf(BattleEvent.LuckyStrike.class));
        BattleEvent.BattleStart start = (BattleEvent.BattleStart) lucky.events().getFirst();
        assertThat(start.attackerLuck()).isEqualTo(3);
        assertThat(start.defenderLuck()).isZero();
    }

    @Test
    void without_luck_the_random_stream_is_untouched() {
        // Ein Bonus ohne Helden bleibt wirkungslos — der Kampf verläuft Wurf für Wurf wie ohne.
        BattleResult without = simulateWithoutHero(CollectedBonus.NONE);
        BattleResult withIneffectiveBonus = simulateWithoutHero(new CollectedBonus(3, 0));

        assertThat(withIneffectiveBonus).isEqualTo(without);
    }

    private static void simulate(CollectedBonus attackerBonus, ListEventCollector collector) {
        Stack attacker = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(0, 5), Side.ATTACKER, 0);
        Stack defender = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(1, 5), Side.DEFENDER, 0);
        BattleSetup setup = new BattleSetup(List.of(attacker), List.of(defender), Battlefield.STANDARD,
                blankHero(), attackerBonus, null, CollectedBonus.NONE);
        new Battle(new AlwaysLucky(SEED), new GreedyAutoSolver(), collector).simulate(setup);
    }

    private static BattleResult simulateWithoutHero(CollectedBonus bonus) {
        Stack attacker = new Stack(UnitCatalog.PIKEMAN, 20, new Hex(0, 5), Side.ATTACKER, 0);
        Stack defender = new Stack(UnitCatalog.PIKEMAN, 20, new Hex(14, 5), Side.DEFENDER, 0);
        BattleSetup setup = new BattleSetup(List.of(attacker), List.of(defender), Battlefield.STANDARD,
                null, bonus, null, CollectedBonus.NONE);
        return new Battle(new Random(SEED), new GreedyAutoSolver(), new ListEventCollector()).simulate(setup);
    }

    private static int firstMeleeDamage(ListEventCollector collector) {
        return collector.events().stream()
                .filter(BattleEvent.Melee.class::isInstance)
                .mapToInt(e -> ((BattleEvent.Melee) e).damage())
                .findFirst()
                .orElseThrow(() -> new AssertionError("kein Melee-Event"));
    }

    /**
     * Gewinnt jeden Promille-Wurf (Glück und Moral ziehen {@code nextInt(1000)}), alle anderen
     * Würfe wie {@link Random}. Damit liegt der erste Glückstreffer sicher auf dem ersten Schlag,
     * und der Schadenswurf davor ist in beiden Läufen derselbe.
     */
    private static final class AlwaysLucky extends Random {
        @Serial
        private static final long serialVersionUID = 1L;

        AlwaysLucky(long seed) {
            super(seed);
        }

        @Override
        public int nextInt(int bound) {
            return bound == 1000 ? 0 : super.nextInt(bound);
        }
    }
}
