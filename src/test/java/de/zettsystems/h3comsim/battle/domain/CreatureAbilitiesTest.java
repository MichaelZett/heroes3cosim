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
 * Fähigkeiten nach heroes.thelazy.net, die der Katalog bisher nur trug oder gar nicht kannte:
 * Angriff auf alle Nachbarn, Anlauf-Immunität, Hass-Paare, Fesselung, Mana-Effekte und die
 * Schutzzauber der Elementare.
 */
class CreatureAbilitiesTest {

    private static final Battlefield FIELD = Battlefield.STANDARD;

    @Test
    void a_hydra_strikes_every_adjacent_enemy_and_nobody_strikes_back() {
        Stack hydra = new Stack(UnitCatalog.CHAOS_HYDRA, 3, new Hex(7, 5), Side.ATTACKER, 0);
        Stack east = new Stack(UnitCatalog.PIKEMAN, 20, new Hex(8, 5), Side.DEFENDER, 0);
        Stack northEast = new Stack(UnitCatalog.PIKEMAN, 20, new Hex(8, 4), Side.DEFENDER, 1);
        Stack south = new Stack(UnitCatalog.PIKEMAN, 20, new Hex(7, 6), Side.DEFENDER, 2);
        Stack far = new Stack(UnitCatalog.PIKEMAN, 20, new Hex(13, 1), Side.DEFENDER, 3);
        BattleSetup setup = new BattleSetup(List.of(hydra), List.of(east, northEast, south, far), FIELD);

        ListEventCollector collector = new ListEventCollector();
        new Battle(new Random(3), new GreedyAutoSolver(), collector).simulate(setup);
        List<BattleEvent> events = collector.events();
        int first = indexOf(events, e -> e instanceof BattleEvent.Melee m && m.actor() == Side.ATTACKER);
        List<Integer> hit = events.subList(first, first + 3).stream()
                .map(e -> ((BattleEvent.Melee) e).targetSlot()).sorted().toList();

        assertThat(hit).containsExactly(0, 1, 2);
        assertThat(events).noneMatch(e -> e instanceof BattleEvent.Retaliation r && r.target() == Side.ATTACKER);
        assertThat(UnitCatalog.MAGIC_ELEMENTAL.specialities())
                .contains(UnitSpeciality.ATTACKS_ALL_ADJACENT, UnitSpeciality.NO_RETALIATION);
        assertThat(UnitCatalog.PSYCHIC_ELEMENTAL.specialities())
                .contains(UnitSpeciality.ATTACKS_ALL_ADJACENT, UnitSpeciality.NO_RETALIATION);
    }

    @Test
    void pikemen_take_no_jousting_bonus() {
        assertThat(firstChargeDamage(UnitCatalog.PIKEMAN, true)).isEqualTo(firstChargeDamage(UnitCatalog.PIKEMAN, false));
        assertThat(firstChargeDamage(UnitCatalog.SWORDSMAN, true))
                .isGreaterThan(firstChargeDamage(UnitCatalog.SWORDSMAN, false));
        assertThat(UnitCatalog.FIREBIRD.specialities()).contains(UnitSpeciality.FIRE_BREATH);
        assertThat(UnitCatalog.PHOENIX.specialities()).contains(UnitSpeciality.FIRE_BREATH);
    }

    @Test
    void hate_pairs_add_fifty_or_a_hundred_percent() {
        assertThat(stack(UnitCatalog.EFREET).hatePercent(UnitCatalog.GENIE.attackerSpecialities())).isEqualTo(50);
        assertThat(stack(UnitCatalog.MASTER_GENIE).hatePercent(UnitCatalog.EFREET_SULTAN.attackerSpecialities()))
                .isEqualTo(50);
        assertThat(stack(UnitCatalog.BLACK_DRAGON).hatePercent(UnitCatalog.TITAN.attackerSpecialities()))
                .isEqualTo(50);
        assertThat(stack(UnitCatalog.TITAN).hatePercent(UnitCatalog.BLACK_DRAGON.attackerSpecialities()))
                .isEqualTo(50);
        assertThat(stack(UnitCatalog.EARTH_ELEMENTAL).hatePercent(UnitCatalog.STORM_ELEMENTAL.attackerSpecialities()))
                .isEqualTo(100);
        assertThat(stack(UnitCatalog.ICE_ELEMENTAL).hatePercent(UnitCatalog.ENERGY_ELEMENTAL.attackerSpecialities()))
                .isEqualTo(100);
        assertThat(stack(UnitCatalog.FIRE_ELEMENTAL).hatePercent(UnitCatalog.AIR_ELEMENTAL.attackerSpecialities()))
                .isZero();

        Stack earth = stack(UnitCatalog.EARTH_ELEMENTAL);
        earth.takeDamage(10, UnitCatalog.AIR_ELEMENTAL.attackerSpecialities());
        assertThat(earth.getCurrentHealth()).as("doppelter Schaden").isEqualTo(20);
    }

    @Test
    void a_bound_stack_stays_put_until_the_dendroids_move_or_fall() {
        Stack dendroids = new Stack(UnitCatalog.DENDROID_SOLDIER, 5, new Hex(6, 5), Side.DEFENDER, 0);
        Stack pikemen = new Stack(UnitCatalog.PIKEMAN, 5, new Hex(5, 5), Side.ATTACKER, 0);
        Stack other = new Stack(UnitCatalog.PEASANT, 1, new Hex(9, 2), Side.DEFENDER, 1);
        pikemen.bindTo(dendroids);

        assertThat(pikemen.isBound()).isTrue();
        assertThat(Battle.rooted(pikemen, new Action.Move(new Hex(4, 5)))).isInstanceOf(Action.Defend.class);
        assertThat(Battle.rooted(pikemen, new Action.MoveAndMelee(new Hex(8, 3), other)))
                .isInstanceOf(Action.Defend.class);
        assertThat(Battle.rooted(pikemen, new Action.MoveAndMelee(new Hex(5, 5), dendroids)))
                .isInstanceOf(Action.Melee.class);
        assertThat(Battle.rooted(pikemen, new Action.Shoot(other))).isInstanceOf(Action.Shoot.class);

        dendroids.moveTo(new Hex(7, 5));
        assertThat(pikemen.isBound()).as("die Dendroiden sind weitergezogen").isFalse();
        pikemen.bindTo(dendroids);
        dendroids.takeDamage(10_000, Set.of());
        assertThat(pikemen.isBound()).as("die Dendroiden sind gefallen").isFalse();
    }

    @Test
    void dendroids_bind_whom_they_strike() {
        Stack dendroids = new Stack(UnitCatalog.DENDROID_SOLDIER, 5, new Hex(6, 5), Side.ATTACKER, 0);
        Stack pikemen = new Stack(UnitCatalog.PIKEMAN, 50, new Hex(7, 5), Side.DEFENDER, 0);
        BattleSetup setup = new BattleSetup(List.of(dendroids), List.of(pikemen), FIELD);

        ListEventCollector collector = new ListEventCollector();
        new Battle(new Random(1), new GreedyAutoSolver(), collector).simulate(setup);

        assertThat(collector.events()).anyMatch(e -> e instanceof BattleEvent.Bind b
                && b.actor() == Side.ATTACKER && b.target() == Side.DEFENDER);
    }

    @Test
    void wraiths_drain_and_familiars_channel_mana() {
        Hero idle = HeroCatalog.SORSHA.withBuild(0, 0, 0, 5, Map.of());
        Stack wraiths = new Stack(UnitCatalog.WRAITH, 5, new Hex(0, 3), Side.ATTACKER, 0);
        Stack wraiths2 = new Stack(UnitCatalog.WRAITH, 5, new Hex(0, 7), Side.ATTACKER, 1);
        Stack pikemen = new Stack(UnitCatalog.PIKEMAN, 200, new Hex(14, 5), Side.DEFENDER, 0);
        BattleSetup drained = new BattleSetup(List.of(wraiths, wraiths2), List.of(pikemen), FIELD, null, idle);
        int before = drained.manaOf(Side.DEFENDER);
        BattleResult result = new Battle(new Random(1), new GreedyAutoSolver()).simulate(drained);
        int drainedMana = before - drained.manaOf(Side.DEFENDER);
        // In der ersten Runde leben beide Wraith-Stacks: mindestens 4; später fällt vielleicht einer.
        assertThat(drainedMana).as("je Runde 2 je lebendem Wraith-Stack").isGreaterThanOrEqualTo(4)
                .isLessThanOrEqualTo(4 * result.turnsTaken());
        assertThat(drainedMana % 2).isZero();

        Hero mage = HeroCatalog.SORSHA.withBuild(0, 0, 0, 5, Map.of(SecondarySkill.WISDOM, SkillLevel.BASIC))
                .withSpellbook(Set.of(Spell.LIGHTNING_BOLT));
        Stack familiars = new Stack(UnitCatalog.FAMILIAR, 30, new Hex(14, 5), Side.DEFENDER, 0);
        BattleSetup channelled = new BattleSetup(
                List.of(new Stack(UnitCatalog.PIKEMAN, 30, new Hex(0, 5), Side.ATTACKER, 0)), List.of(familiars),
                FIELD, mage, idle);
        int defenderBefore = channelled.manaOf(Side.DEFENDER);
        ListEventCollector collector = new ListEventCollector();
        new Battle(new Random(2), new GreedyAutoSolver(), collector).simulate(channelled);
        long bolts = collector.events().stream().filter(e -> e instanceof BattleEvent.HeroSpell h
                && h.actor() == Side.ATTACKER).count();
        assertThat(bolts).isPositive();
        assertThat(channelled.manaOf(Side.DEFENDER)).as("20 %% von 10 Mana je Blitz, solange Familiars leben")
                .isGreaterThan(defenderBefore).isLessThanOrEqualTo(defenderBefore + 2 * (int) bolts);
    }

    @Test
    void elementals_shield_allies_against_their_element_when_they_cannot_strike() {
        Hero pyromancer = HeroCatalog.SORSHA.withBuild(0, 0, 0, 5, Map.of(SecondarySkill.WISDOM, SkillLevel.BASIC))
                .withSpellbook(Set.of(Spell.FIREBALL));
        // Feuerimmun schützt sich der Energy Elemental nicht selbst, wohl aber die Pikeniere.
        Stack energy = new Stack(UnitCatalog.ENERGY_ELEMENTAL, 5, new Hex(0, 5), Side.ATTACKER, 0);
        Stack ally = new Stack(UnitCatalog.PIKEMAN, 20, new Hex(0, 3), Side.ATTACKER, 1);
        Stack enemy = new Stack(UnitCatalog.PIKEMAN, 10, new Hex(14, 5), Side.DEFENDER, 0);
        BattleSetup threatened = new BattleSetup(List.of(energy, ally), List.of(enemy), FIELD, null, pyromancer);
        BattleSetup safe = new BattleSetup(List.of(new Stack(UnitCatalog.ENERGY_ELEMENTAL, 5, new Hex(0, 5),
                Side.ATTACKER, 0)), List.of(new Stack(UnitCatalog.PIKEMAN, 10, new Hex(14, 5), Side.DEFENDER, 0)),
                FIELD);

        assertThat(CreatureSpellcasting.fixedSpell(UnitCatalog.STORM_ELEMENTAL)).isEqualTo(Spell.PROTECTION_FROM_AIR);
        assertThat(CreatureSpellcasting.fixedSpell(UnitCatalog.ICE_ELEMENTAL)).isEqualTo(Spell.PROTECTION_FROM_WATER);
        assertThat(CreatureSpellcasting.fixedSpell(UnitCatalog.MAGMA_ELEMENTAL)).isEqualTo(Spell.PROTECTION_FROM_EARTH);
        assertThat(CreatureSpellcasting.fixedSpell(UnitCatalog.MASTER_GENIE)).isNull();
        assertThat(energy.spellCastsLeft()).isEqualTo(3);
        assertThat(CreatureSpellcasting.decide(energy, enemy, threatened))
                .isEqualTo(new Action.CastSpell(Spell.PROTECTION_FROM_FIRE, ally));
        assertThat(CreatureSpellcasting.decide(safe.attackerStacks().getFirst(), safe.getDefender(), safe))
                .as("ohne Feuerzauber beim Gegner").isNull();

        ListEventCollector collector = new ListEventCollector();
        new Battle(new Random(4), new GreedyAutoSolver(), collector).simulate(threatened);
        assertThat(collector.events()).anyMatch(e -> e instanceof BattleEvent.SpellCast c
                && c.spell() == Spell.PROTECTION_FROM_FIRE && c.actor() == Side.ATTACKER);
    }

    private static int firstChargeDamage(Unit defenderUnit, boolean runUp) {
        Hex target = new Hex(9, 5);
        Hex start = runUp ? new Hex(3, 5) : new Hex(8, 5);
        Stack cavaliers = new Stack(UnitCatalog.CAVALIER, 10, start, Side.ATTACKER, 0);
        Stack defender = new Stack(defenderUnit, 30, target, Side.DEFENDER, 0);
        BattleSetup setup = new BattleSetup(List.of(cavaliers), List.of(defender), FIELD);
        ListEventCollector collector = new ListEventCollector();
        new Battle(new LowestRoll(), new GreedyAutoSolver(), collector).simulate(setup);
        BattleEvent.Melee first = (BattleEvent.Melee) collector.events().get(indexOf(collector.events(),
                e -> e instanceof BattleEvent.Melee m && m.actor() == Side.ATTACKER));
        assertThat(first.hexesMoved() > 0).isEqualTo(runUp);
        return first.damage();
    }

    private static Stack stack(Unit unit) {
        return new Stack(unit, 1, new Hex(5, 5), Side.DEFENDER, 0);
    }

    private static int indexOf(List<BattleEvent> events, java.util.function.Predicate<BattleEvent> which) {
        for (int i = 0; i < events.size(); i++) {
            if (which.test(events.get(i))) {
                return i;
            }
        }
        return -1;
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
