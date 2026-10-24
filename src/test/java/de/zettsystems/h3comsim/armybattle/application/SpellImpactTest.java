package de.zettsystems.h3comsim.armybattle.application;

import de.zettsystems.h3comsim.armybattle.values.FactionPresetDto;
import de.zettsystems.h3comsim.armybattle.values.StackSpec;
import de.zettsystems.h3comsim.battle.domain.Battle;
import de.zettsystems.h3comsim.battle.domain.BattleResult;
import de.zettsystems.h3comsim.battle.domain.BattleSetup;
import de.zettsystems.h3comsim.battle.domain.Battlefield;
import de.zettsystems.h3comsim.battle.domain.Faction;
import de.zettsystems.h3comsim.battle.domain.Hero;
import de.zettsystems.h3comsim.battle.domain.HeroCatalog;
import de.zettsystems.h3comsim.battle.domain.Hex;
import de.zettsystems.h3comsim.battle.domain.ObstacleGenerator;
import de.zettsystems.h3comsim.battle.domain.SecondarySkill;
import de.zettsystems.h3comsim.battle.domain.SkillLevel;
import de.zettsystems.h3comsim.battle.domain.Spell;
import de.zettsystems.h3comsim.battle.domain.Stack;
import de.zettsystems.h3comsim.battle.domain.StrategicAutoSolver;
import de.zettsystems.h3comsim.battle.domain.Unit;
import de.zettsystems.h3comsim.battle.domain.UnitCatalog;
import de.zettsystems.h3comsim.battle.domain.events.BattleEvent;
import de.zettsystems.h3comsim.battle.domain.events.Side;
import de.zettsystems.h3comsim.battle.domain.events.Winner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Was bringen Heldenzauber?
 *
 * <p>Spiegel-Duell: dieselbe Armee (eine Wochenproduktion) auf beiden Seiten, beide geführt vom
 * Standardhelden ihrer Fraktion; eine Seite hat zusätzlich ein Zauberbuch, je nach Stufe auch
 * Power, Knowledge und Magie-Fertigkeiten. Gemessen wird die Win-Rate der zaubernden Seite und
 * welche Zauber die Heuristik wählt. In der letzten Stufe zaubern beide Seiten mit demselben
 * Erstbuch; nur eine kennt dazu Schutz vor Luft, Dispel und Anti-Magic.
 *
 * <p>Aktivieren: {@code .\gradlew.bat test --tests "*SpellImpactTest" "-Ph3.harness=spell-impact"}.
 * Report unter {@code build/reports/spell-impact.md}.
 */
@EnabledIfSystemProperty(named = "h3.harness", matches = "spell-impact")
class SpellImpactTest {

    private static final int SEEDS_PER_PAIR = 40;

    private static final Set<Spell> FIRST_BOOK = Set.of(Spell.MAGIC_ARROW, Spell.BLESS, Spell.BLOODLUST,
            Spell.HASTE, Spell.SHIELD, Spell.STONE_SKIN, Spell.CURSE, Spell.SLOW, Spell.PRECISION,
            Spell.WEAKNESS, Spell.LIGHTNING_BOLT, Spell.CURE);

    private static final Set<Spell> GUARDED_BOOK = guarded();

    private static Set<Spell> guarded() {
        Set<Spell> book = EnumSet.copyOf(FIRST_BOOK);
        book.addAll(List.of(Spell.PROTECTION_FROM_AIR, Spell.DISPEL, Spell.ANTI_MAGIC));
        return book;
    }

    /**
     * Zauberbuch und Aufbau der zaubernden Seite, bei {@link #GUARD_VS_BOOK} auch die Zauber des
     * Gegners: beide zaubern, nur eine Seite kennt Schutz vor Luft, Dispel und Anti-Magic.
     */
    private enum Kit {
        MAGIC_ARROW(Set.of(Spell.MAGIC_ARROW), 0, 0, Map.of(), Set.of()),
        FIRST_BOOK(SpellImpactTest.FIRST_BOOK, 0, 0, Map.of(), Set.of()),
        FIRST_BOOK_PLUS_5(SpellImpactTest.FIRST_BOOK, 5, 5, Map.of(), Set.of()),
        MASTER(Set.of(Spell.SLOW, Spell.HASTE, Spell.BLESS, Spell.STONE_SKIN, Spell.PRAYER,
                Spell.CHAIN_LIGHTNING, Spell.METEOR_SHOWER, Spell.FIREBALL, Spell.RESURRECTION,
                Spell.ANIMATE_DEAD), 5, 5,
                Map.of(SecondarySkill.WISDOM, SkillLevel.ADVANCED, SecondarySkill.AIR_MAGIC, SkillLevel.EXPERT,
                        SecondarySkill.EARTH_MAGIC, SkillLevel.EXPERT), Set.of()),
        GUARD_VS_BOOK(GUARDED_BOOK, 5, 5, Map.of(SecondarySkill.WISDOM, SkillLevel.BASIC),
                SpellImpactTest.FIRST_BOOK);

        private final Set<Spell> spells;
        private final int power;
        private final int knowledge;
        private final Map<SecondarySkill, SkillLevel> skills;
        private final Set<Spell> opponentSpells;

        Kit(Set<Spell> spells, int power, int knowledge, Map<SecondarySkill, SkillLevel> skills,
            Set<Spell> opponentSpells) {
            this.spells = spells;
            this.power = power;
            this.knowledge = knowledge;
            this.skills = skills;
            this.opponentSpells = opponentSpells;
        }
    }

    private static final List<Faction> FACTIONS_IN_ORDER = List.of(
            Faction.CASTLE, Faction.RAMPART, Faction.TOWER, Faction.INFERNO,
            Faction.NECROPOLIS, Faction.DUNGEON, Faction.STRONGHOLD, Faction.FORTRESS,
            Faction.CONFLUX);

    @Test
    void spells_versus_none() throws Exception {
        FactionPresetCatalog presets = new FactionPresetCatalog();
        Map<Faction, FactionPresetDto> presetByFaction = new EnumMap<>(Faction.class);
        for (FactionPresetDto p : presets.all()) {
            presetByFaction.put(p.faction(), p);
        }

        Map<Kit, Map<Faction, Tally>> byKit = new EnumMap<>(Kit.class);
        Map<Kit, Tally> overallByKit = new EnumMap<>(Kit.class);
        for (Kit kit : Kit.values()) {
            Map<Faction, Tally> perFaction = new EnumMap<>(Faction.class);
            Tally overall = new Tally();
            for (Faction f : FACTIONS_IN_ORDER) {
                Tally tally = new Tally();
                perFaction.put(f, tally);
                for (int s = 0; s < SEEDS_PER_PAIR; s++) {
                    long seed = (long) f.ordinal() * 7919L + kit.ordinal() * 131L + s;
                    duel(presetByFaction, f, kit, seed, true, tally, overall);
                    duel(presetByFaction, f, kit, seed, false, tally, overall);
                }
            }
            byKit.put(kit, perFaction);
            overallByKit.put(kit, overall);
        }

        String report = render(byKit, overallByKit);
        Path reportsDir = Path.of("build", "reports");
        Files.createDirectories(reportsDir);
        Path out = reportsDir.resolve("spell-impact.md");
        Files.writeString(out, report);
        IO.println("Spell-Impact-Report: " + out.toAbsolutePath());
        IO.println(report);
    }

    private static void duel(Map<Faction, FactionPresetDto> presets, Faction faction, Kit kit,
                             long seed, boolean casterIsAttacker, Tally perFaction, Tally overall) {
        List<Stack> attackerStacks = buildStacks(presets.get(faction).stacks(), Side.ATTACKER);
        List<Stack> defenderStacks = buildStacks(presets.get(faction).stacks(), Side.DEFENDER);
        Hero hero = HeroCatalog.defaultFor(faction).orElseThrow();
        Hero caster = hero.withBuild(0, 0, kit.power, kit.knowledge, kit.skills).withSpellbook(kit.spells);
        Hero opponent = kit.opponentSpells.isEmpty() ? hero
                : hero.withBuild(0, 0, kit.power, kit.knowledge, Map.of()).withSpellbook(kit.opponentSpells);
        BattleSetup setup = new BattleSetup(attackerStacks, defenderStacks,
                buildBattlefield(attackerStacks, defenderStacks, seed),
                casterIsAttacker ? caster : opponent,
                casterIsAttacker ? opponent : caster);

        BattleResult result = new Battle(new Random(seed), new StrategicAutoSolver(), event -> {
            if (event instanceof BattleEvent.HeroSpell cast && (cast.actor() == Side.ATTACKER) == casterIsAttacker) {
                perFaction.casts.merge(cast.spell(), 1, Integer::sum);
                overall.casts.merge(cast.spell(), 1, Integer::sum);
            }
        }).simulate(setup);
        Side casterSide = casterIsAttacker ? Side.ATTACKER : Side.DEFENDER;
        tally(perFaction, result, casterSide);
        tally(overall, result, casterSide);
    }

    private static void tally(Tally tally, BattleResult result, Side casterSide) {
        tally.battles++;
        Winner winner = result.winner();
        if (winner == Winner.DRAW) {
            tally.draws++;
        } else if ((winner == Winner.ATTACKER) == (casterSide == Side.ATTACKER)) {
            tally.wins++;
        } else {
            tally.losses++;
        }
    }

    private static List<Stack> buildStacks(List<StackSpec> specs, Side side) {
        List<Unit> units = new ArrayList<>(specs.size());
        for (StackSpec spec : specs) {
            units.add(UnitCatalog.byName(spec.unitName()).orElseThrow());
        }
        List<Hex> positions = SpawnLayout.assignPositions(side, units);
        List<Stack> stacks = new ArrayList<>(specs.size());
        for (int slot = 0; slot < specs.size(); slot++) {
            stacks.add(new Stack(units.get(slot), specs.get(slot).count(), positions.get(slot), side, slot));
        }
        return stacks;
    }

    private static Battlefield buildBattlefield(List<Stack> a, List<Stack> d, long seed) {
        Set<Hex> obstacles = new HashSet<>(
                ObstacleGenerator.generate(Battlefield.STANDARD, new Random(seed)));
        obstacles.removeAll(SpawnLayout.spawnHexesFor(a.size(), d.size()));
        return Battlefield.STANDARD.withObstacles(obstacles);
    }

    private static String render(Map<Kit, Map<Faction, Tally>> byKit, Map<Kit, Tally> overallByKit) {
        StringBuilder sb = new StringBuilder(4096);
        sb.append("# Wirkung von Heldenzaubern\n\n");
        sb.append("Spiegel-Duell: dieselbe Armee auf beiden Seiten, beide geführt vom ")
                .append("Standardhelden ihrer Fraktion; eine Seite hat zusätzlich das Zauberbuch der Stufe.\n\n");
        sb.append(SEEDS_PER_PAIR).append(" Seeds je Faktion und Stufe, jede Paarung zusätzlich ")
                .append("mit getauschten Rollen. Solver: StrategicAutoSolver.\n\n");
        sb.append("**Lesart**: Win-Rate der zaubernden Seite. 0.500 = kein Unterschied.\n\n");

        sb.append("## Gesamt je Stufe\n\n");
        sb.append("| Stufe | Battles | Siege | Niederlagen | Draws | Win-Rate | Sigma | Zauber je Kampf |\n");
        sb.append("|--|--|--|--|--|--|--|--|\n");
        for (Kit kit : Kit.values()) {
            Tally t = overallByKit.get(kit);
            sb.append("| ").append(kit.name());
            row(sb, t);
        }

        sb.append("\n## Gewählte Zauber\n\n");
        for (Kit kit : Kit.values()) {
            sb.append("- ").append(kit.name()).append(": ").append(overallByKit.get(kit).casts).append('\n');
        }

        for (Kit kit : Kit.values()) {
            sb.append("\n## ").append(kit.name()).append(" je Faktion\n\n");
            sb.append("| Faktion | Battles | Siege | Niederlagen | Draws | Win-Rate | Sigma | Zauber je Kampf |\n");
            sb.append("|--|--|--|--|--|--|--|--|\n");
            for (Faction f : FACTIONS_IN_ORDER) {
                sb.append("| ").append(f.name());
                row(sb, byKit.get(kit).get(f));
            }
        }
        return sb.toString();
    }

    private static void row(StringBuilder sb, Tally t) {
        int casts = t.casts.values().stream().mapToInt(Integer::intValue).sum();
        sb.append(" | ").append(t.battles)
                .append(" | ").append(t.wins)
                .append(" | ").append(t.losses)
                .append(" | ").append(t.draws)
                .append(" | ").append(rate(t))
                .append(" | ").append(sigma(t))
                .append(" | ").append(String.format(Locale.ROOT, "%.1f", (double) casts / t.battles))
                .append(" |\n");
    }

    private static String rate(Tally t) {
        int decided = t.wins + t.losses;
        return decided == 0 ? "-" : String.format(Locale.ROOT, "%.3f", (double) t.wins / decided);
    }

    /** Abweichung von „Zauber wirken nicht" in Standardabweichungen; Binomial mit p = 0.5. */
    private static String sigma(Tally t) {
        int decided = t.wins + t.losses;
        if (decided == 0) {
            return "-";
        }
        double expected = decided / 2.0;
        double sd = Math.sqrt(decided / 4.0);
        return String.format(Locale.ROOT, "%+.2f", (t.wins - expected) / sd);
    }

    private static final class Tally {
        private int battles;
        private int wins;
        private int losses;
        private int draws;
        private final Map<Spell, Integer> casts = new EnumMap<>(Spell.class);
    }
}
