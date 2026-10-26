package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.BattleEvent;
import de.zettsystems.h3comsim.battle.domain.events.ListEventCollector;
import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.junit.jupiter.api.Test;

import java.io.Serial;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Zauberwirkungen auf dem Stack (Werte aus der Spell Reference des Manuals), Dauer, Blind,
 * Immunitäten und Magieresistenz.
 */
class SpellEffectTest {

    private static Stack stack(Unit unit) {
        return new Stack(unit, 10, new Hex(0, 5), Side.ATTACKER, 0);
    }

    @Test
    void rating_spells_change_attack_defense_and_speed() {
        Stack pikemen = stack(UnitCatalog.PIKEMAN);
        pikemen.applySpell(Spell.STONE_SKIN, SkillLevel.ADVANCED, 6);
        pikemen.applySpell(Spell.WEAKNESS, SkillLevel.BASIC, 3);
        pikemen.applySpell(Spell.HASTE, SkillLevel.ADVANCED, 6);

        assertThat(pikemen.getDefense()).isEqualTo(5 + 6);
        assertThat(pikemen.getAttack()).isEqualTo(4 - 3);
        assertThat(pikemen.getSpeed()).isEqualTo(4 + 5);

        Stack praying = stack(UnitCatalog.PIKEMAN);
        praying.applySpell(Spell.PRAYER, SkillLevel.ADVANCED, 6);
        assertThat(List.of(praying.getAttack(), praying.getDefense(), praying.getSpeed()))
                .containsExactly(4 + 4, 5 + 4, 4 + 4);
    }

    @Test
    void bloodlust_counts_in_melee_only_precision_at_range_only() {
        Stack archers = stack(UnitCatalog.ARCHER);
        Stack target = stack(UnitCatalog.PIKEMAN);
        archers.applySpell(Spell.BLOODLUST, SkillLevel.ADVANCED, 6);
        archers.applySpell(Spell.PRECISION, SkillLevel.BASIC, 6);

        assertThat(archers.attackAgainst(target, AttackType.HAND_TO_HAND)).isEqualTo(6 + 6);
        assertThat(archers.attackAgainst(target, AttackType.LONG_RANGE)).isEqualTo(6 + 3);
    }

    @Test
    void slayer_hits_big_creatures_and_no_dragon_fly() {
        Stack slayer = stack(UnitCatalog.PIKEMAN);
        slayer.applySpell(Spell.SLAYER, SkillLevel.BASIC, 6);

        assertThat(slayer.attackAgainst(stack(UnitCatalog.BLACK_DRAGON), AttackType.HAND_TO_HAND)).isEqualTo(4 + 8);
        assertThat(slayer.attackAgainst(stack(UnitCatalog.DRAGON_FLY), AttackType.HAND_TO_HAND)).isEqualTo(4);
        // Basic noch nicht gegen Angels, Advanced schon.
        assertThat(Stack.isSlayerTarget(UnitCatalog.ANGEL, SkillLevel.BASIC)).isFalse();
        assertThat(Stack.isSlayerTarget(UnitCatalog.ANGEL, SkillLevel.ADVANCED)).isTrue();
        assertThat(Stack.isSlayerTarget(UnitCatalog.TITAN, SkillLevel.EXPERT)).isTrue();
    }

    @Test
    void frenzy_turns_defense_into_attack() {
        Stack frenzied = stack(UnitCatalog.PIKEMAN);
        frenzied.applySpell(Spell.FRENZY, SkillLevel.ADVANCED, Integer.MAX_VALUE);

        assertThat(frenzied.getDefense()).isZero();
        assertThat(frenzied.getAttack()).isEqualTo(4 + 5 * 150 / 100);
    }

    @Test
    void shields_reduce_incoming_damage_by_attack_type() {
        Stack shielded = stack(UnitCatalog.PIKEMAN);
        shielded.applySpell(Spell.SHIELD, SkillLevel.ADVANCED, 6);
        shielded.applySpell(Spell.AIR_SHIELD, SkillLevel.BASIC, 6);

        assertThat(shielded.incomingDamagePercent(AttackType.HAND_TO_HAND)).isEqualTo(70);
        assertThat(shielded.incomingDamagePercent(AttackType.LONG_RANGE)).isEqualTo(75);
    }

    @Test
    void fortune_mirth_counterstrike_and_fire_shield() {
        Stack pikemen = stack(UnitCatalog.PIKEMAN);
        pikemen.applySpell(Spell.FORTUNE, SkillLevel.ADVANCED, 6);
        pikemen.applySpell(Spell.MIRTH, SkillLevel.BASIC, 6);
        pikemen.applySpell(Spell.COUNTERSTRIKE, SkillLevel.BASIC, 6);
        pikemen.applySpell(Spell.FIRE_SHIELD, SkillLevel.ADVANCED, 6);

        assertThat(pikemen.getLuck()).isEqualTo(2);
        assertThat(pikemen.getMorale()).isEqualTo(1);
        pikemen.recordRetaliation();
        assertThat(pikemen.canRetaliate()).as("zweiter Gegenschlag dank Counterstrike").isTrue();
        assertThat(pikemen.fireShieldDamageFor(100)).isEqualTo(25);

        Stack skeletons = stack(UnitCatalog.SKELETON);
        skeletons.applySpell(Spell.MIRTH, SkillLevel.ADVANCED, 6);
        assertThat(skeletons.getMorale()).as("Untote haben fest Moral 0").isZero();
    }

    @Test
    void bless_deals_maximum_damage_plus_one_from_advanced() {
        Stack blessed = stack(UnitCatalog.PIKEMAN);
        blessed.applySpell(Spell.BLESS, SkillLevel.ADVANCED, 6);

        assertThat(blessed.calculateCurrentDamage(AttackType.HAND_TO_HAND, 0, new Random(1)))
                .isEqualTo((3 + 1) * 10);
    }

    @Test
    void spells_run_out_after_their_rounds() {
        Stack pikemen = stack(UnitCatalog.PIKEMAN);
        pikemen.applySpell(Spell.HASTE, SkillLevel.BASIC, 2);

        pikemen.endTurn();
        assertThat(pikemen.hasSpell(Spell.HASTE)).isTrue();
        pikemen.endTurn();
        assertThat(pikemen.hasSpell(Spell.HASTE)).isFalse();
    }

    @Test
    void blind_freezes_until_attacked_and_weakens_the_waking_retaliation() {
        Stack blinded = stack(UnitCatalog.PIKEMAN);
        blinded.applySpell(Spell.BLIND, SkillLevel.BASIC, 3);
        assertThat(blinded.isAbleToAct()).isFalse();

        assertThat(blinded.wakeFromBlind()).isEqualTo(50);
        assertThat(blinded.isAbleToAct()).isTrue();
        assertThat(blinded.wakeFromBlind()).as("ohne Blind voller Gegenschlag").isEqualTo(100);
    }

    @Test
    void immunities_follow_the_manual() {
        assertThat(SpellResistance.isImmune(UnitCatalog.BLACK_DRAGON, Spell.BLESS)).isTrue();
        assertThat(SpellResistance.isImmune(UnitCatalog.GOLD_DRAGON, Spell.PRAYER)).isTrue();
        assertThat(SpellResistance.isImmune(UnitCatalog.RED_DRAGON, Spell.PRAYER)).isFalse();
        assertThat(SpellResistance.isImmune(UnitCatalog.RED_DRAGON, Spell.WEAKNESS)).isTrue();
        assertThat(SpellResistance.isImmune(UnitCatalog.EFREET_SULTAN, Spell.BLOODLUST)).isTrue();
        assertThat(SpellResistance.isImmune(UnitCatalog.SKELETON, Spell.BLIND)).isTrue();
        assertThat(SpellResistance.isImmune(UnitCatalog.TITAN, Spell.BLIND)).isTrue();
        assertThat(SpellResistance.isImmune(UnitCatalog.TROGLODYTE, Spell.BLIND)).isTrue();
        assertThat(SpellResistance.isImmune(UnitCatalog.PIKEMAN, Spell.BLIND)).isFalse();
    }

    @Test
    void magic_resistance_of_dwarves_and_the_unicorn_aura_combine() {
        Stack dwarves = new Stack(UnitCatalog.BATTLE_DWARF, 10, new Hex(1, 5), Side.ATTACKER, 0);
        // Zweifeldrig: Kopf auf (3,5), das Heck auf (2,5) grenzt an die Zwerge.
        Stack unicorns = new Stack(UnitCatalog.UNICORN, 2, new Hex(3, 5), Side.ATTACKER, 1);
        Stack lonely = new Stack(UnitCatalog.DWARF, 10, new Hex(1, 9), Side.ATTACKER, 2);
        Stack enemy = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(14, 5), Side.DEFENDER, 0);
        BattleSetup setup = new BattleSetup(List.of(dwarves, unicorns, lonely), List.of(enemy), Battlefield.STANDARD);

        assertThat(SpellResistance.resistancePercent(dwarves, HeroSpellcasting.other(dwarves.side()), setup)).isEqualTo(52);
        assertThat(SpellResistance.resistancePercent(lonely, HeroSpellcasting.other(lonely.side()), setup)).isEqualTo(20);
        assertThat(SpellResistance.resistancePercent(unicorns, HeroSpellcasting.other(unicorns.side()), setup)).as("Aura nicht für sich selbst").isZero();
    }

    @Test
    void ogre_magi_cast_bloodlust_on_the_most_valuable_melee_ally_when_they_cannot_attack() {
        Stack ogres = new Stack(UnitCatalog.OGRE_MAGI, 5, new Hex(0, 5), Side.ATTACKER, 0);
        Stack behemoths = new Stack(UnitCatalog.BEHEMOTH, 2, new Hex(0, 3), Side.ATTACKER, 1);
        Stack enemy = new Stack(UnitCatalog.PIKEMAN, 50, new Hex(14, 5), Side.DEFENDER, 0);
        BattleSetup setup = new BattleSetup(List.of(ogres, behemoths), List.of(enemy), Battlefield.STANDARD);
        ListEventCollector collector = new ListEventCollector();

        new Battle(new Random(5), new GreedyAutoSolver(), collector).simulate(setup);

        List<BattleEvent.SpellCast> casts = collector.events().stream()
                .filter(BattleEvent.SpellCast.class::isInstance).map(BattleEvent.SpellCast.class::cast).toList();
        assertThat(casts).isNotEmpty();
        assertThat(casts).allSatisfy(c -> assertThat(c.spell()).isEqualTo(Spell.BLOODLUST));
        assertThat(casts).hasSizeLessThanOrEqualTo(CreatureSpellcasting.CASTS_PER_BATTLE);
        // Wertvollster Nahkämpfer: 2 Behemoths (2 × 1500 Gold) vor 5 Ogre Magi (5 × 400).
        assertThat(casts.getFirst().targetSlot()).isEqualTo(1);
    }

    @Test
    void master_genies_cast_only_useful_spells_their_target_is_not_immune_to() {
        Stack genies = new Stack(UnitCatalog.MASTER_GENIE, 3, new Hex(0, 5), Side.ATTACKER, 0);
        Stack dragon = new Stack(UnitCatalog.BLACK_DRAGON, 1, new Hex(0, 3), Side.ATTACKER, 1);
        Stack enemy = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(14, 5), Side.DEFENDER, 0);
        BattleSetup setup = new BattleSetup(List.of(genies, dragon), List.of(enemy), Battlefield.STANDARD);

        for (int seed = 0; seed < 20; seed++) {
            CreatureSpellcasting.Cast cast = CreatureSpellcasting.randomCast(genies, setup, new Random(seed));
            assertThat(cast).isNotNull();
            assertThat(cast.target()).as("Black Dragon ist immun").isSameAs(genies);
            assertThat(cast.spell()).isNotIn(Spell.PRECISION, Spell.SLAYER, Spell.AIR_SHIELD, Spell.CURE, Spell.FRENZY);
        }
    }

    @Test
    void unicorns_blind_their_target() {
        Stack unicorns = new Stack(UnitCatalog.UNICORN, 5, new Hex(0, 5), Side.ATTACKER, 0);
        Stack target = new Stack(UnitCatalog.PIKEMAN, 40, new Hex(1, 5), Side.DEFENDER, 0);
        BattleSetup setup = new BattleSetup(List.of(unicorns), List.of(target), Battlefield.STANDARD);
        ListEventCollector collector = new ListEventCollector();

        new Battle(new AlwaysTrigger(9), new GreedyAutoSolver(), collector).simulate(setup);

        assertThat(collector.events()).anySatisfy(e -> assertThat(e).isInstanceOfSatisfying(
                BattleEvent.SpellCast.class, c -> {
                    assertThat(c.spell()).isEqualTo(Spell.BLIND);
                    assertThat(c.resisted()).isFalse();
                }));
    }

    @Test
    void dragon_flies_dispel_beneficial_spells_and_cast_weakness() {
        Stack flies = new Stack(UnitCatalog.DRAGON_FLY, 10, new Hex(0, 5), Side.ATTACKER, 0);
        Stack target = new Stack(UnitCatalog.PIKEMAN, 200, new Hex(1, 5), Side.DEFENDER, 0);
        target.applySpell(Spell.HASTE, SkillLevel.BASIC, 6);
        BattleSetup setup = new BattleSetup(List.of(flies), List.of(target), Battlefield.STANDARD);
        ListEventCollector collector = new ListEventCollector();

        new Battle(new AlwaysTrigger(9), new GreedyAutoSolver(), collector).simulate(setup);

        assertThat(collector.events()).anySatisfy(e -> assertThat(e).isInstanceOf(BattleEvent.Dispel.class));
        assertThat(collector.events()).anySatisfy(e -> assertThat(e).isInstanceOfSatisfying(
                BattleEvent.SpellCast.class, c -> assertThat(c.spell()).isEqualTo(Spell.WEAKNESS)));
    }

    /** Jeder Prozentwurf (nextInt(100)) trifft; alles andere wie Random. */
    private static final class AlwaysTrigger extends Random {
        @Serial
        private static final long serialVersionUID = 1L;

        AlwaysTrigger(long seed) {
            super(seed);
        }

        @Override
        public int nextInt(int bound) {
            return bound == 100 ? 0 : super.nextInt(bound);
        }
    }
}
