package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.BattleEvent;
import de.zettsystems.h3comsim.battle.domain.events.ListEventCollector;
import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Teleport (heroes.thelazy.net): Basic nicht über Mauern oder Graben, Advanced über den Graben, Expert überall. */
class TeleportTest {

    private static final Battlefield FIELD = Battlefield.STANDARD;

    @Test
    void only_teleport_gets_cheaper_with_its_school() {
        BattleSetup setup = field(new Stack(UnitCatalog.PIKEMAN, 10, new Hex(0, 5), Side.ATTACKER, 0), null);
        assertThat(costWith(null, setup)).isEqualTo(15);
        assertThat(costWith(SkillLevel.BASIC, setup)).isEqualTo(12);
        assertThat(costWith(SkillLevel.ADVANCED, setup)).isEqualTo(6);
        assertThat(costWith(SkillLevel.EXPERT, setup)).isEqualTo(3);
    }

    @Test
    void a_slow_walker_far_from_the_enemy_jumps_next_to_it() {
        Stack pikemen = new Stack(UnitCatalog.PIKEMAN, 20, new Hex(0, 5), Side.ATTACKER, 0);
        BattleSetup setup = field(pikemen, mage(null));

        HeroSpellcasting.Cast cast = HeroSpellcasting.decide(setup.heroOf(Side.ATTACKER), Side.ATTACKER, 50, setup, 0,
                pikemen);

        assertThat(cast).isNotNull();
        assertThat(cast.spell()).isEqualTo(Spell.TELEPORT);
        Hex destination = Objects.requireNonNull(Teleport.destination(pikemen, cast.level(), setup));
        assertThat(setup.stacksOf(Side.DEFENDER).getFirst().distanceTo(destination)).isEqualTo(1);
        assertThat(HeroSpellcasting.decide(setup.heroOf(Side.ATTACKER), Side.ATTACKER, 50, setup, 0, null))
                .as("ohne Stack am Zug kein Teleport").isNull();
    }

    @Test
    void no_jump_for_who_reaches_the_enemy_anyway_flies_or_is_immune() {
        Stack near = new Stack(UnitCatalog.PIKEMAN, 20, new Hex(10, 5), Side.ATTACKER, 0);
        Stack angel = new Stack(UnitCatalog.ANGEL, 1, new Hex(0, 5), Side.ATTACKER, 0);
        Stack dragon = new Stack(UnitCatalog.GREEN_DRAGON, 1, new Hex(0, 5), Side.ATTACKER, 0);
        Stack archers = new Stack(UnitCatalog.ARCHER, 10, new Hex(0, 5), Side.ATTACKER, 0);
        for (Stack s : List.of(near, angel, dragon, archers)) {
            assertThat(Teleport.destination(s, SkillLevel.EXPERT, field(s, null))).as(s.getName()).isNull();
        }
    }

    @Test
    void no_jump_into_the_reach_of_strong_melee() {
        Stack pikemen = new Stack(UnitCatalog.PIKEMAN, 20, new Hex(0, 5), Side.ATTACKER, 0);
        BattleSetup setup = new BattleSetup(List.of(pikemen),
                List.of(new Stack(UnitCatalog.BEHEMOTH, 5, new Hex(14, 5), Side.DEFENDER, 0)), FIELD);

        assertThat(Teleport.destination(pikemen, SkillLevel.EXPERT, setup)).isNull();
    }

    @Test
    void in_a_siege_basic_stays_before_the_moat_advanced_crosses_it_expert_also_the_walls() {
        Stack pikemen = new Stack(UnitCatalog.PIKEMAN, 20, new Hex(0, 5), Side.ATTACKER, 0);
        BattleSetup setup = siege(pikemen);
        Hex beforeMoat = new Hex(5, 3);
        Hex moat = SiegeLayout.MOAT.stream().filter(h -> h.r() == 3).findFirst().orElseThrow();
        Hex inside = new Hex(13, 3);

        assertThat(Teleport.allowed(SkillLevel.BASIC, pikemen, beforeMoat, setup)).isTrue();
        assertThat(Teleport.allowed(SkillLevel.BASIC, pikemen, moat, setup)).isFalse();
        assertThat(Teleport.allowed(SkillLevel.ADVANCED, pikemen, moat, setup)).isTrue();
        assertThat(Teleport.allowed(SkillLevel.ADVANCED, pikemen, inside, setup)).isFalse();
        assertThat(Teleport.allowed(SkillLevel.EXPERT, pikemen, inside, setup)).isTrue();
        assertThat(Teleport.allowed(SkillLevel.BASIC, pikemen, inside, field(pikemen, null))).as("ohne Belagerung").isTrue();

        // Der Verteidiger steht hinter der Mauer: nur Expert bringt den Stack zu ihm.
        assertThat(Teleport.destination(pikemen, SkillLevel.ADVANCED, setup)).isNull();
        Hex expert = Objects.requireNonNull(Teleport.destination(pikemen, SkillLevel.EXPERT, setup));
        assertThat(SiegeLayout.isOutside(expert)).isFalse();
    }

    @Test
    void the_hero_teleports_the_stack_over_the_walls_and_it_strikes_at_once() {
        Stack pikemen = new Stack(UnitCatalog.PIKEMAN, 30, new Hex(0, 5), Side.ATTACKER, 0);
        Hero mage = mage(SkillLevel.EXPERT);
        BattleSetup setup = new BattleSetup(List.of(pikemen),
                List.of(new Stack(UnitCatalog.PEASANT, 40, new Hex(14, 5), Side.DEFENDER, 0)), FIELD, mage,
                CollectedBonus.NONE, null, CollectedBonus.NONE, new Siege(Fortification.FORT, Faction.CASTLE));

        ListEventCollector collector = new ListEventCollector();
        new Battle(new Random(1), new GreedyAutoSolver(), collector).simulate(setup);

        List<BattleEvent> events = collector.events();
        int cast = indexOf(events, e -> e instanceof BattleEvent.HeroSpell h && h.spell() == Spell.TELEPORT);
        assertThat(cast).isNotNegative();
        assertThat(events.get(cast + 1)).isInstanceOfSatisfying(BattleEvent.Move.class,
                m -> assertThat(SiegeLayout.isOutside(new Hex(m.toQ(), m.toR()))).isFalse());
        assertThat(events.get(cast + 2)).isInstanceOfSatisfying(BattleEvent.Melee.class,
                m -> assertThat(m.actor()).isEqualTo(Side.ATTACKER));
    }

    private static int costWith(SkillLevel water, BattleSetup base) {
        BattleSetup setup = new BattleSetup(base.stacksOf(Side.ATTACKER), base.stacksOf(Side.DEFENDER), FIELD,
                mage(water), null);
        return HeroSpellcasting.cost(Spell.TELEPORT, Side.ATTACKER, setup);
    }

    private static Hero mage(SkillLevel water) {
        Map<SecondarySkill, SkillLevel> skills = new HashMap<>(Map.of(SecondarySkill.WISDOM, SkillLevel.BASIC));
        if (water != null) {
            skills.put(SecondarySkill.WATER_MAGIC, water);
        }
        return HeroCatalog.SORSHA.withBuild(0, 0, 0, 10, skills).withSpellbook(Set.of(Spell.TELEPORT));
    }

    private static BattleSetup field(Stack attacker, Hero hero) {
        return new BattleSetup(List.of(attacker),
                List.of(new Stack(UnitCatalog.PEASANT, 10, new Hex(14, 5), Side.DEFENDER, 0)), FIELD, hero, null);
    }

    private static BattleSetup siege(Stack attacker) {
        return new BattleSetup(List.of(attacker),
                List.of(new Stack(UnitCatalog.PEASANT, 10, new Hex(14, 5), Side.DEFENDER, 0)), FIELD, null,
                CollectedBonus.NONE, null, CollectedBonus.NONE, new Siege(Fortification.CITADEL, Faction.CASTLE));
    }

    private static int indexOf(List<BattleEvent> events, java.util.function.Predicate<BattleEvent> p) {
        for (int i = 0; i < events.size(); i++) {
            if (p.test(events.get(i))) {
                return i;
            }
        }
        return -1;
    }
}
