package de.zettsystems.h3comsim.battle.domain;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

/**
 * Zaubernde Kreaturen. Ogre Magi wirken Bloodlust, Master Genies einen zufälligen nützlichen
 * Zauber, Storm, Ice, Energy und Magma Elementals den Schutz vor ihrem Element — alle laut
 * heroes.thelazy.net „at advanced level with a spell power of 6", dreimal pro Kampf, und das
 * Zaubern verbraucht ihre Aktion. Die Dauer ist damit 6 Runden („1
 * Round/Power", Manual S. 64 ff.).
 *
 * <p>Die neutralen Zauberer folgen eigenen Regeln (heroes.thelazy.net): Faerie Dragons greifen
 * mit einem zufälligen Angriffszauber an, solange sie Würfe haben und das Ziel nicht immun ist,
 * sonst im Nahkampf; Enchanters wirken in jeder dritten Runde zu Beginn ihres Zugs einen
 * zufälligen Massenzauber, ohne ihre Aktion herzugeben.
 *
 * <p>Wann gezaubert wird, ist eine Solver-Entscheidung: hier nur, wenn der Stack in diesem Zug
 * keinen Gegner angreifen kann — eine Aktion für einen Zauber herzugeben, während ein Angriff
 * möglich wäre, lohnt sich selten.
 */
final class CreatureSpellcasting {

    static final int CASTS_PER_BATTLE = 3;
    static final int SPELL_POWER = 6;
    static final SkillLevel CAST_LEVEL = SkillLevel.ADVANCED;

    /** Was ein Master Genie wirken kann und im Simulator wirkt (heroes.thelazy.net). */
    static final List<Spell> GENIE_SPELLS = List.of(
            Spell.BLESS, Spell.BLOODLUST, Spell.CURE, Spell.HASTE, Spell.SHIELD, Spell.STONE_SKIN,
            Spell.FORTUNE, Spell.PRECISION, Spell.AIR_SHIELD, Spell.MIRTH, Spell.COUNTERSTRIKE,
            Spell.FIRE_SHIELD, Spell.FRENZY, Spell.PRAYER, Spell.SLAYER);

    /** Faerie Dragon (heroes.thelazy.net): fünf Zauber pro Kampf, Advanced, Power 5 je Drache. */
    static final int FAERIE_CASTS = 5;
    static final int FAERIE_POWER_PER_DRAGON = 5;
    static final SkillLevel FAERIE_LEVEL = SkillLevel.ADVANCED;

    /** Die Zauber der Faerie Dragons mit ihren Wahrscheinlichkeiten in Prozent (heroes.thelazy.net). */
    private static final Map<Spell, Integer> FAERIE_WEIGHTS = weights(
            Spell.MAGIC_ARROW, 10, Spell.ICE_BOLT, 22, Spell.LIGHTNING_BOLT, 22, Spell.FIREBALL, 21,
            Spell.FROST_RING, 10, Spell.INFERNO, 5, Spell.METEOR_SHOWER, 5, Spell.CHAIN_LIGHTNING, 5);

    /**
     * Die Massenzauber der Enchanters mit ihren Gewichten aus VCMI ({@code neutral.json}); die
     * Anteile stimmen mit der Tabelle auf heroes.thelazy.net überein (dort Stone Skin verschrieben).
     */
    private static final Map<Spell, Integer> ENCHANTER_WEIGHTS = weights(
            Spell.HASTE, 15, Spell.BLESS, 15, Spell.STONE_SKIN, 15, Spell.CURE, 10, Spell.BLOODLUST, 5,
            Spell.SLOW, 10, Spell.WEAKNESS, 4, Spell.AIR_SHIELD, 10);

    /** Enchanter: Expert, drei Runden, alle drei Runden. Power 3 passt zur Dauer (Cure heilt 45). */
    static final SkillLevel ENCHANTER_LEVEL = SkillLevel.EXPERT;
    static final int ENCHANTER_ROUNDS = 3;
    static final int ENCHANTER_POWER = 3;

    private CreatureSpellcasting() {
    }

    static int castsFor(Unit unit) {
        if (unit.hasSpeciality(UnitSpeciality.FAERIE_SPELLS)) {
            return FAERIE_CASTS;
        }
        return unit.hasSpeciality(UnitSpeciality.CASTS_BLOODLUST)
                || unit.hasSpeciality(UnitSpeciality.CASTS_RANDOM_BENEFICIAL)
                || unit.hasSpeciality(UnitSpeciality.CASTS_PROTECTION) ? CASTS_PER_BATTLE : 0;
    }

    /**
     * Auf Cursed Ground zaubern Kreaturen gar nicht (heroes.thelazy.net, <em>Terrain</em>). Für die
     * Faerie Dragons; die übrigen hält auch Orb of Inhibition ab ({@link #blockedByArtifact}).
     */
    static boolean blocked(BattleSetup setup) {
        return setup.magicTerrain() == MagicTerrain.CURSED_GROUND;
    }

    /** Cursed Ground oder Orb of Inhibition ({@link BattleSetup#creatureMagicBlocked()}). */
    static boolean blockedByArtifact(BattleSetup setup) {
        return blocked(setup) || setup.creatureMagicBlocked();
    }

    /** Die Stufe eines Kreaturzaubers: auf Magic Plains Expert, sonst die eigene. */
    static SkillLevel level(SkillLevel own, BattleSetup setup) {
        return setup.magicTerrain() == MagicTerrain.MAGIC_PLAINS ? SkillLevel.EXPERT : own;
    }

    /** Der Zauber, den ein Faerie-Dragon-Stack in diesem Zug vorbereitet. */
    static Spell faerieSpell(RandomGenerator rng) {
        return draw(FAERIE_WEIGHTS, rng);
    }

    /**
     * Der Massenzauber eines Enchanter-Stacks; Air Shield nur, wenn ein Gegner noch schießen kann
     * (heroes.thelazy.net).
     */
    static Spell enchanterSpell(Stack caster, BattleSetup setup, RandomGenerator rng) {
        boolean enemyShoots = setup.opponentsOf(caster).stream().anyMatch(o -> o.isAlive() && o.canShoot());
        Map<Spell, Integer> weights = new LinkedHashMap<>(ENCHANTER_WEIGHTS);
        if (!enemyShoots) {
            weights.remove(Spell.AIR_SHIELD);
        }
        return draw(weights, rng);
    }

    /** Zaubert der Enchanter in dieser Runde (ab 0 gezählt)? In der ersten und dann jeder dritten. */
    static boolean enchanterCastsIn(int round) {
        return round % ENCHANTER_ROUNDS == 0;
    }

    /**
     * Wohin ein Flächenzauber des Faerie Dragon geht: unter den Feldern des Ziels und ihren
     * Nachbarn das mit dem größten Goldwert getroffener Gegner abzüglich eigener Verluste.
     */
    static Hex areaCenter(Stack caster, Spell spell, Stack target, BattleSetup setup) {
        Hex best = target.position();
        long bestValue = Long.MIN_VALUE;
        for (Hex own : target.hexes()) {
            List<Hex> candidates = new ArrayList<>(own.neighbors());
            candidates.addFirst(own);
            for (Hex center : candidates) {
                if (!setup.battlefield().contains(center)) {
                    continue;
                }
                long value = areaValue(caster, spell, center, setup);
                if (value > bestValue) {
                    best = center;
                    bestValue = value;
                }
            }
        }
        return best;
    }

    private static long areaValue(Stack caster, Spell spell, Hex center, BattleSetup setup) {
        long value = 0;
        for (Stack hit : HeroSpellcasting.inArea(spell, center, setup)) {
            long worth = (long) hit.getCount() * hit.unit().cost();
            value += hit.side() == caster.side() ? -worth : worth;
        }
        return value;
    }

    private static Spell draw(Map<Spell, Integer> weights, RandomGenerator rng) {
        int total = weights.values().stream().mapToInt(Integer::intValue).sum();
        int roll = rng.nextInt(total);
        for (Map.Entry<Spell, Integer> entry : weights.entrySet()) {
            roll -= entry.getValue();
            if (roll < 0) {
                return entry.getKey();
            }
        }
        throw new IllegalStateException("weights exhausted");
    }

    private static Map<Spell, Integer> weights(Object... pairs) {
        Map<Spell, Integer> weights = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            weights.put((Spell) pairs[i], (Integer) pairs[i + 1]);
        }
        return Collections.unmodifiableMap(weights);
    }

    /** Der feste Zauber dieser Kreatur, oder {@code null} für Master Genies und Nicht-Zauberer. */
    static @Nullable Spell fixedSpell(Unit unit) {
        if (unit.hasSpeciality(UnitSpeciality.CASTS_BLOODLUST)) {
            return Spell.BLOODLUST;
        }
        if (!unit.hasSpeciality(UnitSpeciality.CASTS_PROTECTION)) {
            return null;
        }
        return switch (unit.name()) {
            case "Storm Elemental" -> Spell.PROTECTION_FROM_AIR;
            case "Ice Elemental" -> Spell.PROTECTION_FROM_WATER;
            case "Energy Elemental" -> Spell.PROTECTION_FROM_FIRE;
            default -> Spell.PROTECTION_FROM_EARTH;
        };
    }

    /** Ein möglicher Zauber auf ein Ziel. */
    record Cast(Spell spell, Stack target) {
    }

    /** Solver-Entscheidung: zaubern statt handeln? {@code null} heißt nein. */
    static @Nullable Action decide(Stack active, Stack opponent, @Nullable BattleSetup setup) {
        if (setup == null || active.spellCastsLeft() == 0 || blockedByArtifact(setup) || canAttackNow(active, opponent)) {
            return null;
        }
        Spell fixed = fixedSpell(active.unit());
        if (fixed != null) {
            Stack target = best(usefulTargets(active, fixed, setup));
            return target == null ? null : new Action.CastSpell(fixed, target);
        }
        if (active.hasSpeciality(UnitSpeciality.CASTS_RANDOM_BENEFICIAL) && !usefulCasts(active, setup).isEmpty()) {
            return new Action.CastRandomSpell();
        }
        return null;
    }

    private static boolean canAttackNow(Stack active, Stack opponent) {
        if (active.canShoot()) {
            return true;
        }
        return active.distanceTo(opponent) - 1 <= active.getSpeed();
    }

    /**
     * Zufallsauswahl des Master Genies: unter den Zaubern, die mindestens ein Ziel sinnvoll
     * treffen, gleichverteilt einer; Ziel ist dann der wertvollste passende Stack. Die Genies
     * „will not cast spells that have no effect" und keine, gegen die das Ziel immun ist.
     */
    static @Nullable Cast randomCast(Stack caster, BattleSetup setup, RandomGenerator rng) {
        List<Spell> candidates = new ArrayList<>();
        for (Spell spell : GENIE_SPELLS) {
            if (!usefulTargets(caster, spell, setup).isEmpty()) {
                candidates.add(spell);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        Spell spell = candidates.get(rng.nextInt(candidates.size()));
        Stack target = best(usefulTargets(caster, spell, setup));
        return target == null ? null : new Cast(spell, target);
    }

    static List<Cast> usefulCasts(Stack caster, BattleSetup setup) {
        List<Cast> casts = new ArrayList<>();
        for (Spell spell : GENIE_SPELLS) {
            for (Stack target : usefulTargets(caster, spell, setup)) {
                casts.add(new Cast(spell, target));
            }
        }
        return casts;
    }

    // ally != caster: Frenzy nicht auf den Zaubernden selbst (Identitätsvergleich).
    @SuppressWarnings("ReferenceEquality")
    static List<Stack> usefulTargets(Stack caster, Spell spell, BattleSetup setup) {
        List<Stack> targets = new ArrayList<>();
        for (Stack ally : setup.stacksOf(caster.side())) {
            if (ally.isAlive() && !SpellResistance.isImmune(ally, spell)
                    && !(spell == Spell.FRENZY && ally == caster)
                    && isUseful(spell, ally, setup)) {
                targets.add(ally);
            }
        }
        return targets;
    }

    private static boolean isUseful(Spell spell, Stack target, BattleSetup setup) {
        if (spell != Spell.CURE && target.hasSpell(spell)) {
            return false;
        }
        boolean shooter = target.unit().attackType() == AttackType.LONG_RANGE;
        return switch (spell) {
            case BLESS -> target.unit().minDamage() < target.unit().maxDamage();
            case BLOODLUST -> !shooter;
            case CURE -> target.topMissingHealth() > 0 || target.hasSpell(Spell.BLIND) || target.hasSpell(Spell.WEAKNESS);
            case PRECISION -> shooter;
            case FORTUNE -> target.getLuck() < ArmyLuck.MAX;
            case MIRTH -> !target.unit().hasFixedZeroMorale() && target.getMorale() < ArmyMorale.MAX;
            case AIR_SHIELD -> setup.opponentsOf(target).stream()
                    .anyMatch(o -> o.isAlive() && o.unit().attackType() == AttackType.LONG_RANGE);
            case COUNTERSTRIKE -> !target.hasSpeciality(UnitSpeciality.COUNTERSTRIKE_UNLIMITED);
            case FRENZY -> !shooter && target.getDefense() > 0;
            case SLAYER -> setup.opponentsOf(target).stream()
                    .anyMatch(o -> o.isAlive() && Stack.isSlayerTarget(o.unit(), CAST_LEVEL));
            case HASTE, SHIELD, STONE_SKIN, FIRE_SHIELD, PRAYER -> true;
            case PROTECTION_FROM_AIR, PROTECTION_FROM_WATER, PROTECTION_FROM_FIRE, PROTECTION_FROM_EARTH ->
                    HeroSpellcasting.threat(target, e -> e.kind() == Spell.Kind.DAMAGE
                            && e.school() == spell.school(), setup) > 0;
            default -> false;
        };
    }

    /** Der wertvollste Stack (Anzahl × Kosten); bei Gleichstand der erste. */
    private static @Nullable Stack best(List<Stack> targets) {
        return targets.stream()
                .max(Comparator.comparingLong(s -> (long) s.getCount() * s.unit().cost()))
                .orElse(null);
    }
}
