package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Die Zauber, die Kreaturen lenken oder vervielfältigen (heroes.thelazy.net; Reichweiten und
 * Dauer wie VCMI {@code config/spells/timed.json} und {@code other.json}):
 *
 * <ul>
 *   <li>Clone: Kopie eines eigenen Stacks der Stufe 1–5/6/7, eine Runde je Spell Power; vergeht
 *       beim ersten Schaden, nach ihrer Zeit oder mit dem Original. Ein Original hat höchstens
 *       einen Klon.</li>
 *   <li>Sacrifice: ein eigener lebender Stack wird geopfert (ohne Leiche); ein anderer eigener
 *       Stack bekommt (Power + Lebenspunkte des Geopferten + 3/6/10) × Anzahl Lebenspunkte
 *       dauerhaft zurück, wie bei Resurrection.</li>
 *   <li>Berserk: Basic ein Stack, Advanced alle im Radius 1, Expert im Radius 2 — auch eigene.
 *       Wer betroffen ist, greift bei seinem nächsten Zug den nächsten Stack an, gleich welcher
 *       Seite; danach endet der Zauber. Hebt Hypnotize auf.</li>
 *   <li>Hypnotize: ein gegnerischer Stack, dessen volle Lebenspunkte (Anzahl × Lebenspunkte) unter
 *       10/20/50 + 25 × Power liegen, kämpft eine Runde je Spell Power für den Zaubernden.</li>
 * </ul>
 */
final class ControlSpells {

    private ControlSpells() {
    }

    static boolean handles(Spell spell) {
        return spell == Spell.CLONE || spell == Spell.SACRIFICE || spell == Spell.BERSERK || spell == Spell.HYPNOTIZE;
    }

    // receiver != victim: Identitätsvergleich auf der mutable Stack-Entity.
    @SuppressWarnings("ReferenceEquality")
    static List<HeroSpellcasting.Cast> candidates(Hero hero, Side side, Spell spell, SkillLevel level,
                                                  BattleSetup setup) {
        List<HeroSpellcasting.Cast> casts = new ArrayList<>();
        switch (spell) {
            case CLONE -> {
                int maxTier = SpellDamage.byLevel(level, 5, 6, 7);
                for (Stack own : alive(setup.stacksOf(side))) {
                    if (own.unit().tier() <= maxTier && !own.isSummoned() && !hasClone(own, setup)
                            && !SpellResistance.isImmune(own, spell)
                            && setup.freeHexFor(side, own.unit().doubleWide()) != null) {
                        casts.add(new HeroSpellcasting.Cast(spell, level, own, List.of(own)));
                    }
                }
            }
            case SACRIFICE -> {
                for (Stack victim : alive(setup.stacksOf(side))) {
                    if (!sacrificeable(victim)) {
                        continue;
                    }
                    for (Stack receiver : setup.stacksOf(side)) {
                        if (receiver != victim && receivesSacrifice(receiver, setup)) {
                            casts.add(new HeroSpellcasting.Cast(spell, level, receiver, List.of(victim, receiver)));
                        }
                    }
                }
            }
            case BERSERK -> {
                if (level == SkillLevel.BASIC || level == SkillLevel.NONE) {
                    for (Stack enemy : alive(setup.stacksOf(HeroSpellcasting.other(side)))) {
                        if (!SpellResistance.isImmune(enemy, spell)) {
                            casts.add(new HeroSpellcasting.Cast(spell, level, enemy, List.of(enemy)));
                        }
                    }
                } else {
                    int radius = level == SkillLevel.ADVANCED ? 1 : 2;
                    for (Hex center : centers(setup)) {
                        List<Stack> hit = alive(setup.aliveStacks()).stream()
                                .filter(s -> s.distanceTo(center) <= radius)
                                .filter(s -> !SpellResistance.isImmune(s, spell)).toList();
                        if (!hit.isEmpty()) {
                            casts.add(new HeroSpellcasting.Cast(spell, level, null, hit));
                        }
                    }
                }
            }
            case HYPNOTIZE -> {
                int limit = hypnotizeLimit(level, hero.spellPower());
                for (Stack enemy : alive(setup.stacksOf(HeroSpellcasting.other(side)))) {
                    if (!SpellResistance.isImmune(enemy, spell) && !enemy.hasSpell(Spell.MAGIC_MIRROR)
                            && enemy.controller() != side
                            && (long) enemy.getCount() * enemy.unit().health() < limit) {
                        casts.add(new HeroSpellcasting.Cast(spell, level, enemy, List.of(enemy)));
                    }
                }
            }
            default -> throw new IllegalArgumentException(spell + " is no control spell");
        }
        return casts;
    }

    /**
     * Was der Zauber bringt, in Gold: Clone gut eine Aktion des Stacks, Sacrifice die
     * zurückgeholten Lebenspunkte abzüglich des Geopferten, Berserk den Schaden, den sich der
     * Gegner selbst zufügt, plus die Aktion, die er uns nicht schadet (eigene Betroffene, die sich
     * gegenseitig treffen, zählen negativ), Hypnotize die Aktionen des Stacks für uns statt gegen uns.
     */
    static double value(Hero hero, Side side, HeroSpellcasting.Cast cast, BattleSetup setup) {
        return switch (cast.spell()) {
            case CLONE -> {
                Stack origin = cast.affected().getFirst();
                yield HeroSpellcasting.stackValue(origin) * HeroSpellcasting.ROUND_SHARE
                        * Math.min(CLONE_ACTIONS, hero.spellPower());
            }
            case SACRIFICE -> {
                Stack victim = cast.affected().getFirst();
                Stack receiver = cast.affected().get(1);
                int amount = sacrificeAmount(cast.level(), hero.spellPower(), victim);
                yield HeroSpellcasting.restorable(receiver, amount) * HeroSpellcasting.costPerHealth(receiver)
                        - HeroSpellcasting.stackValue(victim);
            }
            case BERSERK -> {
                double total = 0;
                for (Stack s : cast.affected()) {
                    Stack nearest = nearestOther(s, setup);
                    if (nearest == null || nearest.side() != s.side()) {
                        continue;
                    }
                    double action = HeroSpellcasting.stackValue(s) * HeroSpellcasting.ROUND_SHARE;
                    double chance = HeroSpellcasting.successChance(Spell.BERSERK, s, side, setup);
                    total += (s.side() == side ? -2 : 2) * action * chance;
                }
                yield total;
            }
            case HYPNOTIZE -> {
                Stack target = cast.affected().getFirst();
                yield 2 * HeroSpellcasting.stackValue(target) * HeroSpellcasting.ROUND_SHARE
                        * Math.min(HeroSpellcasting.EXPECTED_ROUNDS, hero.spellPower())
                        * HeroSpellcasting.successChance(Spell.HYPNOTIZE, target, side, setup);
            }
            default -> throw new IllegalArgumentException(cast.spell() + " is no control spell");
        };
    }

    /** Ein Klon lebt selten länger als einen Gegenzug: gut eine Aktion. */
    private static final double CLONE_ACTIONS = 1.5;

    static int hypnotizeLimit(SkillLevel level, int power) {
        return SpellDamage.byLevel(level, 10, 20, 50) + 25 * power;
    }

    static int sacrificeAmount(SkillLevel level, int power, Stack victim) {
        return (power + victim.unit().health() + SpellDamage.byLevel(level, 3, 6, 10)) * victim.getCount();
    }

    /** Geopfert werden kann ein lebender, nicht immuner eigener Stack, keine Maschine, kein Klon. */
    static boolean sacrificeable(Stack victim) {
        return victim.isAlive() && victim.unit().isLiving() && !victim.isWarMachine() && !victim.isSummoned()
                && !SpellResistance.isImmune(victim, Spell.SACRIFICE);
    }

    /** Wiederbelebt wird wie mit Resurrection: wer Gefallene hat, ein ganz gefallener mit freier Leiche. */
    static boolean receivesSacrifice(Stack receiver, BattleSetup setup) {
        return receiver.unit().isResurrectable() && !receiver.isWarMachine() && !receiver.isSummoned()
                && (receiver.isAlive() ? receiver.missingHealth() > 0 : setup.hasFreeCorpse(receiver));
    }

    @SuppressWarnings("ReferenceEquality")
    private static boolean hasClone(Stack origin, BattleSetup setup) {
        return setup.stacksOf(origin.side()).stream().anyMatch(s -> s.isAlive() && s.cloneOf() == origin);
    }

    /** Der nächste andere Stack, Kriegsmaschinen eingeschlossen; bei Gleichstand der erste. */
    @SuppressWarnings("ReferenceEquality")
    static @Nullable Stack nearestOther(Stack stack, BattleSetup setup) {
        return setup.spellTargets().stream()
                .filter(s -> s != stack && s.isAlive())
                .min(Comparator.comparingInt(stack::distanceTo))
                .orElse(null);
    }

    private static Set<Hex> centers(BattleSetup setup) {
        Set<Hex> centers = new LinkedHashSet<>();
        for (Stack stack : setup.aliveStacks()) {
            centers.addAll(stack.hexes());
        }
        return centers;
    }

    private static List<Stack> alive(List<Stack> stacks) {
        return stacks.stream().filter(Stack::isAlive).toList();
    }
}
