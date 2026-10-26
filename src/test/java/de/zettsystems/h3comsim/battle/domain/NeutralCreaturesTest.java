package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.BattleEvent;
import de.zettsystems.h3comsim.battle.domain.events.ListEventCollector;
import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

/** Die neutralen Kreaturen aus Armageddon's Blade (heroes.thelazy.net, VCMI {@code neutral.json}). */
class NeutralCreaturesTest {

    private static final Battlefield FIELD = Battlefield.STANDARD;

    @Test
    void the_catalog_holds_all_fifteen_neutrals_with_their_values() {
        assertThat(UnitCatalog.byFaction(Faction.NEUTRAL)).extracting(Unit::name).containsExactlyInAnyOrder(
                "Azure Dragon", "Boar", "Crystal Dragon", "Diamond Golem", "Enchanter", "Faerie Dragon",
                "Gold Golem", "Halfling", "Mummy", "Nomad", "Peasant", "Rogue", "Rust Dragon", "Sharpshooter",
                "Troll");
        Unit azure = UnitCatalog.AZURE_DRAGON;
        assertThat(List.of(azure.attack(), azure.defense(), azure.health(), azure.speed(), azure.minDamage(),
                azure.maxDamage(), azure.cost())).containsExactly(50, 50, 1000, 19, 70, 80, 30000);
        assertThat(UnitCatalog.SHARPSHOOTER.shots()).isEqualTo(32);
        assertThat(List.of(UnitCatalog.BOAR, UnitCatalog.NOMAD, UnitCatalog.FAERIE_DRAGON, UnitCatalog.RUST_DRAGON,
                UnitCatalog.CRYSTAL_DRAGON, UnitCatalog.AZURE_DRAGON)).allMatch(Unit::doubleWide);
        assertThat(UnitCatalog.CRYSTAL_DRAGON.movement()).isEqualTo(Movement.GROUND);
        assertThat(Stack.isSlayerTarget(UnitCatalog.FAERIE_DRAGON, SkillLevel.BASIC)).isTrue();
    }

    @Test
    void the_mummy_is_undead_without_belonging_to_necropolis() {
        Unit mummy = UnitCatalog.MUMMY;
        assertThat(mummy.isUndead()).isTrue();
        assertThat(mummy.hasFixedZeroMorale()).isTrue();
        assertThat(mummy.isResurrectable()).isFalse();
    }

    @Test
    void a_creature_curse_works_like_advanced_curse() {
        Stack knights = new Stack(UnitCatalog.BLACK_KNIGHT, 1, new Hex(0, 5));
        knights.curse();
        // Black Knight 15–30: 80 % von 15 minus 1.
        assertThat(knights.calculateCurrentDamage(AttackType.HAND_TO_HAND, 0, new Random(1))).isEqualTo(11);
    }

    @Test
    void mummies_curse_a_quarter_of_their_targets() {
        int attacks = 0;
        int curses = 0;
        for (int seed = 0; seed < 200; seed++) {
            List<BattleEvent> events = fight(seed, new Stack(UnitCatalog.MUMMY, 30, new Hex(10, 5), Side.ATTACKER, 0),
                    new Stack(UnitCatalog.TROLL, 5, new Hex(11, 5), Side.DEFENDER, 0), null);
            // Nur Treffer, die das Ziel überlebt: auf einen gefallenen Stack wird nicht gewürfelt.
            attacks += count(events, e -> e instanceof BattleEvent.Melee m && m.actor() == Side.ATTACKER
                    && m.targetAfter().count() > 0
                    || e instanceof BattleEvent.Retaliation r && r.retaliator() == Side.ATTACKER
                    && r.targetAfter().count() > 0);
            curses += count(events, e -> e instanceof BattleEvent.Cursing c && c.actor() == Side.ATTACKER);
        }
        assertThat((double) curses / attacks).isBetween(0.18, 0.32);
    }

    @Test
    void golems_shrug_off_most_spell_damage() {
        Stack gold = new Stack(UnitCatalog.GOLD_GOLEM, 10, new Hex(0, 5));
        Stack diamond = new Stack(UnitCatalog.DIAMOND_GOLEM, 10, new Hex(0, 5));
        assertThat(SpellDamage.toTarget(Spell.LIGHTNING_BOLT, 1000, null, gold)).isEqualTo(150);
        assertThat(SpellDamage.toTarget(Spell.LIGHTNING_BOLT, 1000, null, diamond)).isEqualTo(50);
    }

    @Test
    void halflings_have_at_least_good_luck_but_only_under_a_hero() {
        Stack led = new Stack(UnitCatalog.HALFLING, 10, new Hex(0, 5));
        led.assignCommander(HeroCatalog.SORSHA);
        Stack leaderless = new Stack(UnitCatalog.HALFLING, 10, new Hex(0, 5));

        assertThat(led.getLuck()).isEqualTo(1);
        assertThat(leaderless.getLuck()).isZero();
    }

    @Test
    void the_troll_regenerates_its_top_creature() {
        Stack troll = new Stack(UnitCatalog.TROLL, 3, new Hex(0, 5));
        troll.takeDamage(30, Set.of());
        assertThat(troll.getCurrentHealth()).isEqualTo(10);
        troll.endTurn();
        assertThat(troll.getCount()).isEqualTo(3);
        assertThat(troll.getCurrentHealth()).isEqualTo(40);
    }

    @Test
    void rust_dragons_corrode_the_defense_of_what_they_bite() {
        Stack dragons = new Stack(UnitCatalog.RUST_DRAGON, 1, new Hex(10, 5), Side.ATTACKER, 0);
        Stack golems = new Stack(UnitCatalog.GOLD_GOLEM, 100, new Hex(12, 5), Side.DEFENDER, 0);
        int defense = golems.getDefense();

        List<BattleEvent> events = fight(3, dragons, golems, null);

        int bites = count(events, e -> e instanceof BattleEvent.AcidBreath a && a.actor() == Side.ATTACKER);
        assertThat(bites).isPositive();
        assertThat(golems.getDefense()).isEqualTo(Math.max(0, defense - 3 * bites));
        assertThat(events).filteredOn(e -> e instanceof BattleEvent.AcidBreath a && a.damage() > 0)
                .allMatch(e -> ((BattleEvent.AcidBreath) e).damage() == 25);
    }

    @Test
    void azure_dragons_frighten_the_living_but_not_the_undead() {
        int livingFears = 0;
        int undeadFears = 0;
        for (int seed = 0; seed < 40; seed++) {
            livingFears += count(fight(seed, new Stack(UnitCatalog.PIKEMAN, 400, new Hex(0, 5), Side.ATTACKER, 0),
                    new Stack(UnitCatalog.AZURE_DRAGON, 1, new Hex(13, 5), Side.DEFENDER, 0), null),
                    e -> e instanceof BattleEvent.Fear f && f.actor() == Side.ATTACKER);
            undeadFears += count(fight(seed, new Stack(UnitCatalog.MUMMY, 100, new Hex(0, 5), Side.ATTACKER, 0),
                    new Stack(UnitCatalog.AZURE_DRAGON, 1, new Hex(13, 5), Side.DEFENDER, 0), null),
                    e -> e instanceof BattleEvent.Fear);
        }
        assertThat(livingFears).isPositive();
        assertThat(undeadFears).isZero();
    }

    @Test
    void faerie_dragons_attack_with_up_to_five_spells_from_their_table() {
        Set<Spell> table = Set.of(Spell.MAGIC_ARROW, Spell.ICE_BOLT, Spell.LIGHTNING_BOLT, Spell.FIREBALL,
                Spell.FROST_RING, Spell.INFERNO, Spell.METEOR_SHOWER, Spell.CHAIN_LIGHTNING);
        List<BattleEvent> events = fight(5, new Stack(UnitCatalog.FAERIE_DRAGON, 3, new Hex(0, 5), Side.ATTACKER, 0),
                new Stack(UnitCatalog.TROLL, 60, new Hex(13, 5), Side.DEFENDER, 0), null);

        List<BattleEvent.SpellCast> casts = events.stream()
                .filter(e -> e instanceof BattleEvent.SpellCast c && c.actor() == Side.ATTACKER)
                .map(BattleEvent.SpellCast.class::cast).toList();
        assertThat(casts).hasSizeBetween(1, 5).allMatch(c -> table.contains(c.spell()));
        assertThat(events).anyMatch(e -> e instanceof BattleEvent.SpellHit h && h.actor() == Side.ATTACKER
                && h.damage() > 0);
    }

    @Test
    void faerie_dragons_mirror_hostile_spells_now_and_then() {
        int reflected = 0;
        for (int seed = 0; seed < 30; seed++) {
            Hero mage = HeroCatalog.SORSHA.withBuild(0, 0, 10, 20, Map.of(SecondarySkill.WISDOM, SkillLevel.BASIC))
                    .withSpellbook(Set.of(Spell.MAGIC_ARROW));
            reflected += count(fight(seed, new Stack(UnitCatalog.PIKEMAN, 50, new Hex(0, 5), Side.ATTACKER, 0),
                    new Stack(UnitCatalog.FAERIE_DRAGON, 2, new Hex(13, 5), Side.DEFENDER, 0), mage),
                    e -> e instanceof BattleEvent.SpellHit h && h.reflected());
        }
        assertThat(reflected).isPositive();
    }

    @Test
    void enchanters_cast_a_mass_spell_in_the_first_and_every_third_round() {
        Set<Spell> table = Set.of(Spell.HASTE, Spell.BLESS, Spell.STONE_SKIN, Spell.CURE, Spell.BLOODLUST,
                Spell.SLOW, Spell.WEAKNESS, Spell.AIR_SHIELD);
        BattleSetup setup = new BattleSetup(
                List.of(new Stack(UnitCatalog.ENCHANTER, 5, new Hex(0, 1), Side.ATTACKER, 0),
                        new Stack(UnitCatalog.PIKEMAN, 30, new Hex(0, 5), Side.ATTACKER, 1),
                        new Stack(UnitCatalog.SWORDSMAN, 20, new Hex(0, 9), Side.ATTACKER, 2)),
                List.of(new Stack(UnitCatalog.TROLL, 30, new Hex(14, 5), Side.DEFENDER, 0)), FIELD);
        ListEventCollector collector = new ListEventCollector();
        BattleResult result = new Battle(new Random(2), new GreedyAutoSolver(), collector).simulate(setup);

        List<BattleEvent> events = collector.events();
        Predicate<BattleEvent> enchanting = e -> e instanceof BattleEvent.SpellCast c && c.actor() == Side.ATTACKER;
        int batches = 0;
        for (int i = 0; i < events.size(); i++) {
            if (enchanting.test(events.get(i)) && (i == 0 || !enchanting.test(events.get(i - 1)))) {
                batches++;
            }
        }
        assertThat(batches).isPositive().isLessThanOrEqualTo((result.turnsTaken() + 2) / 3);
        assertThat(events).filteredOn(enchanting).allMatch(e -> table.contains(((BattleEvent.SpellCast) e).spell()));
        // Ein Massenzauber trifft alle Stacks einer Seite auf einmal.
        BattleEvent.SpellCast first = (BattleEvent.SpellCast) events.stream().filter(enchanting).findFirst().orElseThrow();
        assertThat(events).filteredOn(enchanting).filteredOn(e -> ((BattleEvent.SpellCast) e).spell() == first.spell())
                .hasSizeGreaterThanOrEqualTo(first.spell().beneficial() ? 3 : 1);
    }

    private static List<BattleEvent> fight(long seed, Stack attacker, Stack defender, Hero attackerHero) {
        BattleSetup setup = new BattleSetup(List.of(attacker), List.of(defender), FIELD, attackerHero, null);
        ListEventCollector collector = new ListEventCollector();
        new Battle(new Random(seed), new GreedyAutoSolver(), collector).simulate(setup);
        return collector.events();
    }

    private static int count(List<BattleEvent> events, Predicate<BattleEvent> p) {
        return (int) events.stream().filter(p).count();
    }
}
