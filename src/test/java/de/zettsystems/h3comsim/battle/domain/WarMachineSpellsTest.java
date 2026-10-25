package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.BattleEvent;
import de.zettsystems.h3comsim.battle.domain.events.ListEventCollector;
import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Was Kriegsmaschinen trifft: Splash und Schadenszauber ja, Todeswolke und die meisten Wirkungen nicht. */
class WarMachineSpellsTest {

    private static final Battlefield FIELD = Battlefield.STANDARD;
    private static final Hex ENEMY_BALLISTA = new Hex(FIELD.rightmostQ(3), 3);
    private static final Hex NEXT_TO_BALLISTA = new Hex(ENEMY_BALLISTA.q() - 1, 3);

    @Test
    void machines_resist_all_but_damage_and_the_listed_effects() {
        Unit ballista = WarMachine.BALLISTA.unit();
        assertThat(SpellResistance.isImmune(ballista, Spell.LIGHTNING_BOLT)).isFalse();
        assertThat(SpellResistance.isImmune(ballista, Spell.FIREBALL)).isFalse();
        assertThat(SpellResistance.isImmune(ballista, Spell.ARMAGEDDON)).isFalse();
        assertThat(SpellResistance.isImmune(ballista, Spell.BLESS)).isFalse();
        assertThat(SpellResistance.isImmune(ballista, Spell.IMPLOSION)).isTrue();
        assertThat(SpellResistance.isImmune(ballista, Spell.DEATH_RIPPLE)).isTrue();
        assertThat(SpellResistance.isImmune(ballista, Spell.SLOW)).isTrue();
        assertThat(SpellResistance.isImmune(ballista, Spell.BLIND)).isTrue();
        assertThat(SpellResistance.isImmune(ballista, Spell.BLOODLUST)).isTrue();
    }

    @Test
    void bolts_and_area_spells_reach_enemy_machines_but_implosion_does_not() {
        BattleSetup setup = new BattleSetup(List.of(new Stack(UnitCatalog.PIKEMAN, 10, new Hex(0, 5), Side.ATTACKER, 0)),
                List.of(new Stack(UnitCatalog.PEASANT, 10, NEXT_TO_BALLISTA, Side.DEFENDER, 0)), FIELD)
                .equip(Side.DEFENDER, Set.of(WarMachine.BALLISTA));
        Hero mage = HeroCatalog.SORSHA.withBuild(0, 0, 0, 5, Map.of(SecondarySkill.WISDOM, SkillLevel.EXPERT))
                .withSpellbook(Set.of(Spell.LIGHTNING_BOLT, Spell.IMPLOSION, Spell.FIREBALL, Spell.DEATH_RIPPLE));
        Stack ballista = setup.machinesOf(Side.DEFENDER).getFirst();

        assertThat(HeroSpellcasting.candidates(mage, Side.ATTACKER, Spell.LIGHTNING_BOLT, setup))
                .extracting(HeroSpellcasting.Cast::target).contains(ballista);
        assertThat(HeroSpellcasting.candidates(mage, Side.ATTACKER, Spell.IMPLOSION, setup))
                .extracting(HeroSpellcasting.Cast::target).doesNotContain(ballista);
        assertThat(HeroSpellcasting.inArea(Spell.FIREBALL, ENEMY_BALLISTA, setup)).contains(ballista);
        assertThat(HeroSpellcasting.candidates(mage, Side.ATTACKER, Spell.DEATH_RIPPLE, setup))
                .allSatisfy(cast -> assertThat(cast.affected()).doesNotContain(ballista));
        assertThat(setup.spellTargets()).contains(ballista).hasSize(3);
    }

    @Test
    void a_bolt_that_destroys_the_ammo_cart_ends_unlimited_shots() {
        // Der Wagen ist mehr wert als der eine Bauer daneben: der Blitz geht auf ihn.
        Stack peasant = new Stack(UnitCatalog.PEASANT, 1, new Hex(14, 5), Side.DEFENDER, 0);
        Hero mage = HeroCatalog.SORSHA.withBuild(0, 0, 40, 5, Map.of(SecondarySkill.WISDOM, SkillLevel.BASIC))
                .withSpellbook(Set.of(Spell.LIGHTNING_BOLT));
        BattleSetup setup = new BattleSetup(List.of(new Stack(UnitCatalog.PIKEMAN, 20, new Hex(0, 5), Side.ATTACKER, 0)),
                List.of(peasant), FIELD, mage, null).equip(Side.DEFENDER, Set.of(WarMachine.AMMO_CART));

        ListEventCollector collector = new ListEventCollector();
        new Battle(new Random(1), new GreedyAutoSolver(), collector).simulate(setup);

        assertThat(collector.events()).anyMatch(e -> e instanceof BattleEvent.SpellHit hit
                && hit.target() == Side.DEFENDER && hit.targetSlot() == 21);
        assertThat(setup.hasWorking(Side.DEFENDER, WarMachine.AMMO_CART)).isFalse();
    }

    @Test
    void splash_hits_a_machine_next_to_the_target_but_the_death_cloud_does_not() {
        assertThat(splashHitsOnBallista(UnitCatalog.MAGOG)).isPositive();
        assertThat(splashHitsOnBallista(UnitCatalog.POWER_LICH)).isZero();
    }

    private static long splashHitsOnBallista(Unit shooter) {
        BattleSetup setup = new BattleSetup(List.of(new Stack(shooter, 30, new Hex(0, 3), Side.ATTACKER, 0)),
                List.of(new Stack(UnitCatalog.PEASANT, 500, NEXT_TO_BALLISTA, Side.DEFENDER, 0)), FIELD)
                .equip(Side.DEFENDER, Set.of(WarMachine.BALLISTA));
        ListEventCollector collector = new ListEventCollector();
        new Battle(new Random(3), new GreedyAutoSolver(), collector).simulate(setup);
        return collector.events().stream().filter(e -> e instanceof BattleEvent.Shoot shot
                && shot.actor() == Side.ATTACKER && shot.target() == Side.DEFENDER && shot.targetSlot() == 20).count();
    }
}
