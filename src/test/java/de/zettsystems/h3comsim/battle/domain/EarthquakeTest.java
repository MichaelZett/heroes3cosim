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
import static org.assertj.core.api.Assertions.within;

/** Earthquake: zwei, drei oder vier Treffer auf zufällige Teile der Befestigung. */
class EarthquakeTest {

    private static final Hero QUAKER = HeroCatalog.SORSHA
            .withBuild(0, 0, 0, 5, Map.of(SecondarySkill.WISDOM, SkillLevel.BASIC,
                    SecondarySkill.EARTH_MAGIC, SkillLevel.EXPERT))
            .withSpellbook(Set.of(Spell.EARTHQUAKE));

    @Test
    void the_chance_of_a_breach_follows_from_the_standing_parts() {
        assertThat(Earthquake.hits(SkillLevel.NONE)).isEqualTo(2);
        assertThat(Earthquake.hits(SkillLevel.EXPERT)).isEqualTo(4);
        // Fort: vier Segmente und das Tor mit je 2 Treffern; zwei Treffer brechen nur, wenn sie
        // dasselbe Teil treffen.
        Siege fort = new Siege(Fortification.FORT, Faction.CASTLE);
        assertThat(Earthquake.breachChance(fort, 2)).isCloseTo(0.2, within(1e-9));
        assertThat(Earthquake.breachChance(fort, 1)).isZero();
        fort.damage(WallPart.UPPER_WALL, 1);
        assertThat(Earthquake.breachChance(fort, 1)).isCloseTo(0.2, within(1e-9));
        // Castle: drei Treffer je Segment, ein Treffer auf einen Turm bringt nichts.
        assertThat(Earthquake.breachChance(new Siege(Fortification.CASTLE, Faction.CASTLE), 2))
                .as("das Tor mit 2 Treffern unter 8 Teilen").isCloseTo(1.0 / 64, within(1e-9));
    }

    @Test
    void a_strike_hits_only_standing_parts() {
        Siege fort = new Siege(Fortification.FORT, Faction.CASTLE);
        Random rng = new Random(3);
        for (int i = 0; i < 10; i++) {
            WallPart hit = Earthquake.strike(fort, rng);
            assertThat(hit).isNotNull().matches(part -> part.isWall() || part == WallPart.GATE);
        }
        assertThat(Earthquake.strike(fort, rng)).as("alles eingestürzt").isNull();
    }

    @Test
    void only_the_attacker_casts_it_and_only_while_the_walls_hold() {
        BattleSetup setup = siege(QUAKER, null);
        assertThat(HeroSpellcasting.candidates(QUAKER, Side.ATTACKER, Spell.EARTHQUAKE, setup)).hasSize(1);
        assertThat(HeroSpellcasting.candidates(QUAKER, Side.DEFENDER, Spell.EARTHQUAKE, setup)).isEmpty();
        assertThat(HeroSpellcasting.earthquakeValue(SkillLevel.EXPERT, Side.ATTACKER, setup)).isPositive();

        BattleSetup field = new BattleSetup(List.of(pikemen()), List.of(peasants()), Battlefield.STANDARD,
                QUAKER, null);
        assertThat(HeroSpellcasting.candidates(QUAKER, Side.ATTACKER, Spell.EARTHQUAKE, field)).isEmpty();
        assertThat(HeroSpellcasting.earthquakeValue(SkillLevel.EXPERT, Side.ATTACKER, field)).isZero();

        java.util.Objects.requireNonNull(setup.siege()).damage(WallPart.GATE, 2);
        assertThat(HeroSpellcasting.candidates(QUAKER, Side.ATTACKER, Spell.EARTHQUAKE, setup)).isEmpty();
    }

    @Test
    void the_hero_shakes_the_walls_in_battle() {
        // Castle-Mauern halten den ersten Katapultschuss aus; dann zaubert der Held.
        BattleSetup castle = new BattleSetup(List.of(pikemen()), List.of(peasants()), Battlefield.STANDARD, QUAKER,
                CollectedBonus.NONE, null, CollectedBonus.NONE, new Siege(Fortification.CASTLE, Faction.CASTLE));
        ListEventCollector collector = new ListEventCollector();
        new Battle(new Random(5), new GreedyAutoSolver(), collector).simulate(castle);

        List<BattleEvent> events = collector.events();
        assertThat(events).anyMatch(e -> e instanceof BattleEvent.HeroSpell h && h.spell() == Spell.EARTHQUAKE);
        List<BattleEvent.QuakeHit> hits = events.stream().filter(BattleEvent.QuakeHit.class::isInstance)
                .map(BattleEvent.QuakeHit.class::cast).toList();
        assertThat(hits).hasSizeGreaterThanOrEqualTo(4)
                .allSatisfy(hit -> assertThat(hit.healthAfter()).isBetween(0, 2));
    }

    private static BattleSetup siege(Hero attacker, Hero defender) {
        return new BattleSetup(List.of(pikemen()), List.of(peasants()), Battlefield.STANDARD, attacker,
                CollectedBonus.NONE, defender, CollectedBonus.NONE, new Siege(Fortification.FORT, Faction.CASTLE));
    }

    private static Stack pikemen() {
        return new Stack(UnitCatalog.PIKEMAN, 40, new Hex(0, 5), Side.ATTACKER, 0);
    }

    private static Stack peasants() {
        return new Stack(UnitCatalog.PEASANT, 20, new Hex(14, 5), Side.DEFENDER, 0);
    }
}
