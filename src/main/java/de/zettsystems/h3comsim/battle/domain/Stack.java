package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.Side;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

// Kampfmeldungen gehören ins Combat-Log (logback.xml: battle.log, DEBUG), nicht ins App-Log.
@Slf4j(topic = "de.zettsystems.h3comsim.battle.domain.BattleLogger")
public class Stack {

    /** Nicht final: Ein gefallener Stack kann als Dämonen zurückkehren ({@link #riseAs}). */
    private Unit unit;
    private final Side side;
    private final int slot;
    private int startCount;
    private int aliveCount;
    private int topUnitCurrentHealth;
    private int shotsRemaining;
    private Hex position;
    /** Die Dendroiden, die diesen Stack gefesselt haben, und ihr Feld zu diesem Zeitpunkt. */
    private @Nullable Stack boundBy;
    private @Nullable Hex boundAnchor;
    private boolean petrified;
    private int petrifiedCounter;
    private boolean cursed;
    /** Wie oft ein Rust Dragon das Ziel mit Säure getroffen hat; jeder Treffer −3 Defense. */
    private int acidHits;
    private int cursedCounter;
    private boolean poisoned;
    private int poisonedCounter;
    private int retaliationsThisTurn;
    private boolean diseased;
    private int diseasedCounter;
    private boolean aged;
    private boolean rebirthUsed;
    private boolean summoned;
    /** Clone: das Original; der Klon vergeht beim ersten Schaden, nach seiner letzten Runde oder mit dem Original. */
    private @Nullable Stack cloneOf;
    private int cloneLastRound;
    /** Geopfert (Sacrifice) oder vergangener Klon: keine Leiche für Resurrection oder Pit Lords. */
    private boolean corpseless;
    private boolean resurrectionUsed;
    private boolean demonsRaised;
    private boolean defending;
    private boolean waitedThisTurn;
    private @Nullable Hero commander;
    /** Artefakte des Helden: Lebenspunkte je Kreatur, Speed, Attack/Defense für Drachen. */
    private int artifactHealth;
    private int artifactSpeed;
    private int artifactDragonBonus;
    private int armyMorale;
    private int armyLuck;
    /** +1 Attack, Defense und Speed auf dem Heimatgelände (siehe {@link Terrain}). */
    private int nativeBonus;
    /** Moral und Glück vom magischen Gelände (Holy Ground, Evil Fog, Clover Field). */
    private int terrainMorale;
    private int terrainLuck;
    /** Cursed Ground: positive Moral und positives Glück fallen weg. */
    private boolean cursedGround;
    /** Hourglass of the Evil Hour bzw. Spirit of Oppression bei einem der Helden. */
    private boolean noLuck;
    private boolean noPositiveMorale;
    private boolean unlimitedAmmo;
    private final Map<Spell, ActiveSpell> spells = new EnumMap<>(Spell.class);
    private int spellCastsLeft;
    /** Höchstens so viele Lebenspunkte bringt Regeneration je Zug zurück. */
    private static final int REGENERATION_HEALTH = 50;
    private static final Map<Spell, Spell> OPPOSITES = Map.of(
            Spell.BLESS, Spell.CURSE, Spell.CURSE, Spell.BLESS,
            Spell.HASTE, Spell.SLOW, Spell.SLOW, Spell.HASTE,
            Spell.FORTUNE, Spell.MISFORTUNE, Spell.MISFORTUNE, Spell.FORTUNE,
            Spell.MIRTH, Spell.SORROW, Spell.SORROW, Spell.MIRTH);
    /** Attack in Prozent — unter 100 nur beim Gegenschlag eines eben geweckten Blinden. */
    private int attackPercent = 100;

    /** Convenience constructor — defaults the side to {@link Side#ATTACKER}. */
    public Stack(Unit unit, int count, Hex position) {
        this(unit, count, position, Side.ATTACKER, 0);
    }

    public Stack(Unit unit, int count, Hex position, Side side) {
        this(unit, count, position, side, 0);
    }

    public Stack(Unit unit, int count, Hex position, Side side, int slot) {
        if (count < 0) {
            throw new IllegalArgumentException("count must be >= 0, was " + count);
        }
        if (slot < 0) {
            throw new IllegalArgumentException("slot must be >= 0, was " + slot);
        }
        this.unit = Objects.requireNonNull(unit, "unit");
        this.position = Objects.requireNonNull(position, "position");
        this.side = Objects.requireNonNull(side, "side");
        this.slot = slot;
        this.startCount = count;
        this.aliveCount = count;
        this.topUnitCurrentHealth = unit.health();
        this.shotsRemaining = unit.shots();
        this.spellCastsLeft = CreatureSpellcasting.castsFor(unit);
    }

    public Unit unit() {
        return unit;
    }

    public Side side() {
        return side;
    }

    public int slot() {
        return slot;
    }

    /** Das Feld des Kopfs; bei zweifeldrigen Kreaturen liegt das zweite dahinter ({@link #tailOf}). */
    public Hex position() {
        return position;
    }

    public void moveTo(Hex position) {
        this.position = Objects.requireNonNull(position, "position");
    }

    /** Dendroiden fesseln den Stack an seinen Platz, solange sie leben und sich nicht bewegen. */
    void bindTo(Stack dendroids) {
        this.boundBy = dendroids;
        this.boundAnchor = dendroids.position();
    }

    /** Ist der Stack gefesselt? Haben sich die Dendroiden bewegt oder sind sie gefallen, nicht mehr. */
    public boolean isBound() {
        Stack by = boundBy;
        if (by != null && (!by.isAlive() || !by.position().equals(boundAnchor))) {
            boundBy = null;
            boundAnchor = null;
        }
        return boundBy != null;
    }

    public boolean isDoubleWide() {
        return unit.doubleWide();
    }

    /**
     * Das zweite Feld einer zweifeldrigen Kreatur mit dem Kopf auf {@code head}: zum eigenen Rand
     * hin, beim Angreifer links, beim Verteidiger rechts. Wie in VCMI dreht sie sich nicht um.
     */
    public static Hex tailOf(Hex head, Side side) {
        return new Hex(head.q() + (side == Side.ATTACKER ? -1 : 1), head.r());
    }

    /** Spaltenabstand des Hecks vom Kopf: −1 beim Angreifer, +1 beim Verteidiger, 0 einfeldig. */
    public int tailDq() {
        if (!isDoubleWide()) {
            return 0;
        }
        return side == Side.ATTACKER ? -1 : 1;
    }

    /** Die Felder, die der Stack belegt: der Kopf, bei zweifeldrigen dazu das Heck. */
    public List<Hex> hexes() {
        return hexesAt(position);
    }

    /** Die Felder, die der Stack mit dem Kopf auf {@code head} belegen würde. */
    public List<Hex> hexesAt(Hex head) {
        return isDoubleWide() ? List.of(head, tailOf(head, side)) : List.of(head);
    }

    public boolean occupies(Hex hex) {
        return position.equals(hex) || (isDoubleWide() && tailOf(position, side).equals(hex));
    }

    /** Kürzeste Distanz von einem der eigenen Felder zu {@code hex}. */
    public int distanceTo(Hex hex) {
        return hexes().stream().mapToInt(h -> h.distanceTo(hex)).min().orElseThrow();
    }

    /** Kürzeste Distanz zwischen einem Feld dieses und einem Feld des anderen Stacks; 1 = benachbart. */
    public int distanceTo(Stack other) {
        return other.hexes().stream().mapToInt(this::distanceTo).min().orElseThrow();
    }

    /** Das eigene Feld, das {@code from} am nächsten liegt; bei Gleichstand der Kopf. */
    public Hex nearestHexTo(Hex from) {
        Hex best = position;
        for (Hex h : hexes()) {
            if (h.distanceTo(from) < best.distanceTo(from)) {
                best = h;
            }
        }
        return best;
    }

    public int shotsRemaining() {
        return shotsRemaining;
    }

    public void useShot() {
        if (shotsRemaining > 0 && !unlimitedAmmo) {
            shotsRemaining--;
        }
    }

    /** Der Munitionswagen ist zerstört: ab jetzt zählen die Schüsse wieder. */
    void revokeUnlimitedAmmo() {
        this.unlimitedAmmo = false;
    }

    /** Munitionswagen der eigenen Armee: Schüsse gehen nicht mehr aus. */
    void grantUnlimitedAmmo() {
        this.unlimitedAmmo = true;
    }

    /**
     * Golden Bow und Bow of the Sharpshooter: die Schützen des Helden schießen ohne Entfernungs-
     * und Hindernis-Strafe (heroes.thelazy.net).
     */
    public boolean ignoresShootingPenalties() {
        return commander != null && !isWarMachine()
                && commander.artifacts().has(ArtifactEffect.NoShootingPenalty.class);
    }

    /** Bow of the Sharpshooter: schießt auch mit einem Gegner daneben (heroes.thelazy.net). */
    public boolean shootsWhenAdjacent() {
        return canShoot() && commander != null && !isWarMachine()
                && commander.artifacts().has(ArtifactEffect.FreeShooting.class);
    }

    /** Ab Advanced Forgetfulness kann der Stack gar nicht mehr schießen (Manual S. 78). */
    public boolean canShoot() {
        ActiveSpell forgetful = spells.get(Spell.FORGETFULNESS);
        return unit.attackType() == AttackType.LONG_RANGE && shotsRemaining > 0
                && (forgetful == null || forgetful.level() == SkillLevel.BASIC);
    }

    public String getName() {
        return unit.name();
    }

    /**
     * Speed der Kreatur plus +1 auf dem Heimatgelände, +1 durch einen Kreatur-Spezialisten
     * ({@link HeroSpecialty}) und Haste (+3/5) bzw. Prayer (+2/4).
     */
    public int getSpeed() {
        int speed = unit.speed() + nativeBonus + artifactSpeed
                + (commander == null ? 0 : commander.specialty().speedBonus(unit))
                + spellPoints(Spell.HASTE, 3, 5, 5) + spellPoints(Spell.PRAYER, 2, 4, 4);
        ActiveSpell slow = spells.get(Spell.SLOW);
        // Slow (Manual S. 68): auf 25 % (Basic) bzw. 50 % der normalen Geschwindigkeit.
        return slow == null ? speed : Math.max(1, speed * slow.byLevel(25, 50, 50) / 100);
    }

    /**
     * Attack-Rating ohne die Zauber, die nur je Angriffsart oder Ziel greifen: Grundwert,
     * Heimatgelände, Krankheit, Held und Spezialität, Weakness (−3/6), Prayer (+2/4) und Frenzy (+100/150/200 %
     * der Defense, Manual S. 75).
     */
    public int getAttack() {
        int attack = unit.attack() + nativeBonus + artifactDragonBonus - (diseased ? 2 : 0)
                + (commander == null ? 0 : commander.attack() + commander.specialtyAttackBonus(unit))
                - spellPoints(Spell.WEAKNESS, 3, 6, 6) + spellPoints(Spell.PRAYER, 2, 4, 4);
        ActiveSpell frenzy = spells.get(Spell.FRENZY);
        if (frenzy != null) {
            attack += baseDefense() * frenzy.byLevel(100, 150, 200) / 100;
        }
        return attack * attackPercent / 100;
    }

    /**
     * Attack-Rating für einen konkreten Angriff: dazu Bloodlust (+3/6 im Nahkampf), Precision
     * (+3/6 im Fernkampf) und Slayer (+8 gegen die genannten Kreaturen).
     */
    public int attackAgainst(Stack target, AttackType attackType) {
        int attack = getAttack();
        if (attackType == AttackType.HAND_TO_HAND) {
            attack += spellPoints(Spell.BLOODLUST, 3, 6, 6);
        } else {
            attack += spellPoints(Spell.PRECISION, 3, 6, 6);
        }
        ActiveSpell slayer = spells.get(Spell.SLAYER);
        if (slayer != null && isSlayerTarget(target.unit(), slayer.level())) {
            attack += 8 + slayer.amount();
        }
        return attack;
    }

    /**
     * Slayer (Manual S. 75): Basic gegen Behemoths, Dragons und Hydras, Advanced zusätzlich
     * gegen Devils und Angels, Expert auch gegen Titans. Erkannt an Stufe 7 und Namen bzw. an den
     * Rassen-Markern — der Dragon Fly ist kein Drache.
     */
    static boolean isSlayerTarget(Unit unit, SkillLevel level) {
        String name = unit.name();
        boolean big = unit.tier() == 7
                && (name.contains("Dragon") || name.contains("Behemoth") || name.contains("Hydra"));
        boolean advanced = level.compareTo(SkillLevel.ADVANCED) >= 0
                && (unit.hasSpeciality(UnitSpeciality.DEVIL_RACE) || unit.hasSpeciality(UnitSpeciality.ANGEL_RACE));
        boolean expert = level == SkillLevel.EXPERT && unit.hasSpeciality(UnitSpeciality.TITAN_RACE);
        return big || advanced || expert;
    }

    private int baseDefense() {
        return unit.defense() + nativeBonus + artifactDragonBonus - (diseased ? 2 : 0)
                + (commander == null ? 0 : commander.defense() + commander.specialtyDefenseBonus(unit));
    }

    /**
     * Defense-Rating mit Stone Skin (+3/6), Prayer (+2/4), Disrupting Ray (−3/4/5 je Wurf,
     * stapelbar, Manual S. 65) und Säureatem (−3 je Treffer); Frenzy setzt sie auf 0.
     */
    public int getDefense() {
        if (spells.containsKey(Spell.FRENZY)) {
            return 0;
        }
        ActiveSpell ray = spells.get(Spell.DISRUPTING_RAY);
        int base = baseDefense() + spellPoints(Spell.STONE_SKIN, 3, 6, 6) + spellPoints(Spell.PRAYER, 2, 4, 4)
                - (ray == null ? 0 : ray.amount()) - 3 * acidHits;
        base = Math.max(0, base);
        if (defending) {
            // H3-Defend laut RoE-Manual S. 47: +20 % auf die Defense-Rating bis Rundenende.
            // Stackt nicht mit anderen Defense-Boni — gehört hierhin, damit alle Damage-
            // Berechnungen profitieren. Der Heldenbonus ist zu diesem Zeitpunkt bereits
            // eingerechnet: Manual S. 33 addiert ihn auf das Defense-Rating der Kreatur,
            // die +20 % gelten also auf die Summe.
            base = (int) Math.round(base * 1.2);
        }
        return base;
    }

    public void defend() {
        defending = true;
    }

    public boolean isDefending() {
        return defending;
    }

    /**
     * Der Held, der diese Armee führt, oder {@code null}. Wird von {@link BattleSetup} auf alle
     * Stacks der jeweiligen Seite gesetzt — ein Stack wählt seinen Anführer nicht selbst.
     */
    public @Nullable Hero commander() {
        return commander;
    }

    void assignCommander(@Nullable Hero hero) {
        boolean unhurt = topUnitCurrentHealth == maxHealth();
        this.commander = hero;
        Equipment artifacts = hero == null || isWarMachine() ? Equipment.NONE : hero.artifacts();
        artifactHealth = artifactHealthFor(hero);
        artifactSpeed = artifacts.speed();
        artifactDragonBonus = unit.isDragon() ? artifacts.dragonBonus() : 0;
        if (unhurt && aliveCount > 0) {
            topUnitCurrentHealth = maxHealth();
        }
    }

    /**
     * Lebenspunkte je Kreatur aus den Artefakten des Helden: Ringe und Vial für alle, das Elixir of
     * Life dazu 25 % der Grund-Lebenspunkte für Lebewesen, abgerundet (heroes.thelazy.net: ein Imp
     * bekommt 4 + 1 + 4, nicht 4 + 4 + 2).
     */
    private int artifactHealthFor(@Nullable Hero hero) {
        if (hero == null || isWarMachine()) {
            return 0;
        }
        Equipment artifacts = hero.artifacts();
        int living = unit.isLiving() ? unit.health() * artifacts.livingHealthPercent() / 100 : 0;
        return artifacts.health() + living;
    }

    /** Lebenspunkte einer Kreatur dieses Stacks: Grundwert plus Artefakte des Helden. */
    public int maxHealth() {
        return unit.health() + artifactHealth;
    }

    /**
     * Regeneration von Wights, Wraiths und Trolls, mit dem Elixir of Life auch aller Lebewesen:
     * die oberste Kreatur gewinnt zu Beginn ihres Zugs bis zu 50 Lebenspunkte zurück
     * (heroes.thelazy.net, <em>Regeneration</em>).
     */
    private boolean regenerates() {
        return unit.hasSpeciality(UnitSpeciality.REGENERATION)
                || (commander != null && !isWarMachine() && unit.isLiving()
                && commander.artifacts().livingHealthPercent() > 0);
    }

    /** Moral-Rating der Armee, zu der dieser Stack gehoert — gesetzt von {@link BattleSetup}. */
    void assignArmyMorale(int rating) {
        this.armyMorale = rating;
    }

    /** Glücks-Rating der Armee, bereits gekappt — gesetzt von {@link BattleSetup}. */
    void assignArmyLuck(int rating) {
        this.armyLuck = rating;
    }

    /** Was Grund- und magisches Gelände diesem Stack geben — gesetzt von {@link BattleSetup}. */
    void assignGround(@Nullable Terrain terrain, @Nullable MagicTerrain magic) {
        boolean onCursedGround = magic == MagicTerrain.CURSED_GROUND;
        nativeBonus = !onCursedGround && !isWarMachine() && terrain != null && terrain == Terrain.nativeOf(unit.faction()) ? 1 : 0;
        terrainMorale = magic == null || isWarMachine() ? 0 : magic.moraleFor(unit.faction());
        terrainLuck = magic == null || isWarMachine() ? 0 : magic.luckFor(unit.faction());
        cursedGround = onCursedGround;
    }

    /**
     * Hourglass of the Evil Hour nimmt beiden Seiten das positive Glück, Spirit of Oppression die
     * positive Moral, auch die von Zaubern wie Mirth (heroes.thelazy.net) — gesetzt von {@link BattleSetup}.
     */
    void assignNegations(boolean luck, boolean morale) {
        noLuck = luck;
        noPositiveMorale = morale;
    }

    /** Kämpft der Stack auf seinem Heimatgelände (und nicht auf Cursed Ground)? */
    public boolean onNativeTerrain() {
        return nativeBonus > 0;
    }

    /**
     * Glück dieses Stacks, 0..3, mit Clover Field. Halflinge haben unter einem Helden mindestens
     * +1 (ohne Helden wirkt Glück in SoD gar nicht, auch nicht ihres); Cursed Ground nimmt jedes
     * Glück.
     */
    public int getLuck() {
        ActiveSpell fortune = spells.get(Spell.FORTUNE);
        // Ein Fortune-Spezialist gibt immer +3 (amount), sonst +1/2.
        int fortuneLuck = fortune == null ? 0 : Math.max(fortune.byLevel(1, 2, 2), fortune.amount());
        int clover = commander == null ? 0 : terrainLuck;
        int luck = Math.clamp(armyLuck + clover + fortuneLuck - spellValue(Spell.MISFORTUNE, 1, 2, 2),
                ArmyLuck.MIN, ArmyLuck.MAX);
        if (cursedGround || noLuck) {
            return 0;
        }
        return commander != null && unit.hasSpeciality(UnitSpeciality.MINIMUM_LUCK_1) ? Math.max(1, luck) : luck;
    }

    /**
     * Glückstreffer, Manual S. 44: Chance auf doppelten Schaden — 4.2 / 8.3 / 12.5 %. Zieht nur
     * bei positivem Glück einen Zufallswert; ohne Glück bleibt der Zufallsstrom unangetastet.
     */
    public boolean isLucky(RandomGenerator rng) {
        int luck = getLuck();
        return luck > 0 && rng.nextInt(1000) < ArmyMorale.triggerChancePerMille(luck);
    }

    /**
     * Manual S. 43: Wait verschiebt die Aktion ans Ende der ersten Phase. Die Verzögerung ist
     * damit pro Runde verbraucht — ein zweites Wait gibt es nicht, sonst könnte sich ein Stack
     * beliebig oft nach hinten schieben.
     */
    public boolean hasWaitedThisTurn() {
        return waitedThisTurn;
    }

    public void markWaited() {
        waitedThisTurn = true;
    }

    public int getCount() {
        return aliveCount;
    }

    public boolean isAlive() {
        return aliveCount > 0;
    }

    public boolean isPetrified() {
        return petrified;
    }

    public int getCurrentHealth() {
        return aliveCount > 0 ? topUnitCurrentHealth : 0;
    }

    public boolean hasSpeciality(UnitSpeciality speciality) {
        return unit.hasSpeciality(speciality);
    }

    public Set<UnitSpeciality> getAttackerSpecialities() {
        return unit.attackerSpecialities();
    }

    public int calculateCurrentDamage(AttackType usedAttackType, int hexesMoved, RandomGenerator rng) {
        // Planeswalker-Spezialität: fester Aufschlag auf Minimal- und Maximalschaden je Kreatur.
        int bonus = commander == null ? 0 : commander.specialty().damageBonus(unit);
        ActiveSpell bless = spells.get(Spell.BLESS);
        ActiveSpell curseSpell = spells.get(Spell.CURSE);
        int baseValue;
        if (cursed || (curseSpell != null && curseSpell.level() != SkillLevel.BASIC)) {
            // Curse ab Advanced (Manual S. 72): 80 % des Mindestschadens minus 1. Der Fluch von
            // Black/Dread Knight und Mummy wirkt auf Advanced (heroes.thelazy.net).
            baseValue = Math.max(1, (unit.minDamage() + bonus) * 80 / 100 - 1);
        } else if (curseSpell != null) {
            baseValue = unit.minDamage() + bonus;
        } else if (bless != null) {
            // Bless (Manual S. 76): maximaler Schaden, ab Advanced +1; ein Bless-Spezialist legt
            // amount Prozent darauf.
            baseValue = unit.maxDamage() + bonus + bless.byLevel(0, 1, 1);
            baseValue = baseValue * (100 + bless.amount()) / 100;
        } else {
            baseValue = rng.nextInt(unit.minDamage() + bonus, unit.maxDamage() + bonus + 1);
        }
        if (usedAttackType == AttackType.LONG_RANGE && spells.containsKey(Spell.FORGETFULNESS)) {
            // Forgetfulness auf Basic (Manual S. 78): die Hälfte der Kreaturen vergisst zu schießen.
            baseValue = baseValue / 2;
        }
        if (unit.hasPenality(usedAttackType)) {
            baseValue = (int) Math.round(0.5 * baseValue);
            LOG.debug("Stack von {} hat Nachteil, halbiert also den Schaden.", getName());
        }
        if (unit.hasSpeciality(UnitSpeciality.DEATH_BLOW) && rng.nextInt(1, 101) <= 20) {
            baseValue = baseValue * 2;
            LOG.debug("Stack von {} nutzt Death Blow, verdoppelt also den Schaden.", getName());
        }
        if (unit.hasSpeciality(UnitSpeciality.IMPACT_DAMAGE) && hexesMoved > 0) {
            // H3 Jousting: +5 % pro Hex Bewegung, gedeckelt bei +50 %.
            int bonusPercent = Math.min(50, hexesMoved * 5);
            baseValue = (int) Math.round(baseValue * (1.0 + bonusPercent / 100.0));
            LOG.debug("Stack von {} bekommt {}% Jousting-Bonus durch {} Hex Bewegung.",
                    getName(), bonusPercent, hexesMoved);
        }
        return baseValue * aliveCount;
    }

    public int calculateAttackBoniMaliPercentage(int defense) {
        return boniMaliPercentage(getAttack(), defense);
    }

    /** Wie {@link #calculateAttackBoniMaliPercentage(int)}, mit den Zaubern für diesen Angriff. */
    public int calculateAttackBoniMaliPercentage(Stack target, AttackType attackType, int defense) {
        return boniMaliPercentage(attackAgainst(target, attackType), defense);
    }

    /**
     * Anteil des Schadens, den der Stack nach Shield (−15/30 % im Nahkampf) und Air Shield
     * (−25/50 % im Fernkampf) tatsächlich nimmt, in Prozent.
     */
    public int incomingDamagePercent(AttackType attackType) {
        return attackType == AttackType.HAND_TO_HAND
                ? 100 - spellValue(Spell.SHIELD, 15, 30, 30)
                : 100 - spellValue(Spell.AIR_SHIELD, 25, 50, 50);
    }

    static int boniMaliPercentage(int attack, int defense) {
        // RoE-Manual S. 43: +5 % pro Attack-Punkt Differenz, gedeckelt bei +400 %
        // (80 Punkte); −2 % pro Defense-Punkt Differenz, min. 30 % Damage (= −70 %, also
        // 35 Punkte). Ohne Cap explodiert Damage bei extremen Tier-Lücken.
        int diff = attack - defense;
        return diff >= 0 ? Math.min(diff * 5, 400) : Math.max(diff * 2, -70);
    }

    /** Lebenspunkte, die der obersten Kreatur fehlen. */
    public int topMissingHealth() {
        return aliveCount > 0 ? maxHealth() - topUnitCurrentHealth : 0;
    }

    /**
     * Heilt die oberste Kreatur, höchstens bis zu ihren vollen Lebenspunkten (First Aid Tent,
     * Manual S. 117). Tote kommen nicht zurück.
     *
     * @return tatsächlich geheilte Lebenspunkte
     */
    public int healTop(int amount) {
        int healed = Math.min(amount, topMissingHealth());
        topUnitCurrentHealth += healed;
        return healed;
    }

    public int effectiveDefenseAgainst(Set<UnitSpeciality> attackerSpecialities) {
        int reductionPercent = 0;
        if (attackerSpecialities.contains(UnitSpeciality.DEFENSE_REDUCTION_80)) {
            reductionPercent = 80;
        } else if (attackerSpecialities.contains(UnitSpeciality.DEFENSE_REDUCTION_40)) {
            reductionPercent = 40;
        }
        return (getDefense() * (100 - reductionPercent)) / 100;
    }

    public boolean canRetaliate() {
        return !isWarMachine() && retaliationsThisTurn < maxRetaliationsPerTurn();
    }

    /** Eine Kriegsmaschine ({@link WarMachine}): steht fest, schlägt nicht zurück. */
    public boolean isWarMachine() {
        return WarMachine.isMachine(unit);
    }

    public void recordRetaliation() {
        retaliationsThisTurn++;
    }

    private int maxRetaliationsPerTurn() {
        if (unit.hasSpeciality(UnitSpeciality.COUNTERSTRIKE_UNLIMITED)) {
            return Integer.MAX_VALUE;
        }
        int base = unit.hasSpeciality(UnitSpeciality.COUNTERSTRIKE_TWICE) ? 2 : 1;
        return base + spellValue(Spell.COUNTERSTRIKE, 1, 2, 2);
    }

    /**
     * Moral dieses Stacks: Armee-Rating plus individueller Bonus der Kreatur und Moral vom
     * Gelände, gekappt auf [-3, +3] (Manual S. 43); Cursed Ground nimmt positive Moral.
     *
     * <p>Untote und Elementare stehen außerhalb: ihre Moral ist fest 0, „independent of their
     * army's morale rating". Ein Necropolis-Held mit Leadership hebt seine Skelette also nicht —
     * weshalb das Manual Necromancern die Fertigkeit gleich ganz verwehrt.
     */
    public int getMorale() {
        if (unit.hasFixedZeroMorale()) {
            return 0;
        }
        int morale = ArmyMorale.clamp(armyMorale + unit.morale() + terrainMorale + spellValue(Spell.MIRTH, 1, 2, 2)
                - spellValue(Spell.SORROW, 1, 2, 2));
        return cursedGround || noPositiveMorale ? Math.min(0, morale) : morale;
    }

    /**
     * Positive Moral, Manual S. 43: Chance auf eine zweite Aktion — 4.2 / 8.3 / 12.5 %.
     * Zieht nur bei positiver Moral einen Zufallswert; bei 0 oder negativer Moral bleibt der
     * Zufallsstrom unangetastet.
     */
    public boolean hasGoodMorale(RandomGenerator rng) {
        int morale = getMorale();
        return morale > 0 && rng.nextInt(1000) < ArmyMorale.triggerChancePerMille(morale);
    }

    /**
     * Negative Moral, Manual S. 43: Chance, die Aktion dieser Runde zu verlieren — mit denselben
     * 4.2 / 8.3 / 12.5 %. Die Kreatur „freezes", sie handelt gar nicht; sie verteidigt auch
     * nicht ersatzweise.
     */
    public boolean freezes(RandomGenerator rng) {
        int morale = getMorale();
        return morale < 0 && rng.nextInt(1000) < ArmyMorale.triggerChancePerMille(-morale);
    }

    public void takeDamage(int baseDamage, Set<UnitSpeciality> attackerSpecialities) {
        if (cloneOf != null && baseDamage > 0) {
            // Clone (heroes.thelazy.net): der Klon vergeht beim ersten Schaden, ohne Vergeltung.
            vanish();
            return;
        }
        int realDamage = baseDamage;
        int hatePercent = hatePercent(attackerSpecialities);
        if (hatePercent > 0) {
            realDamage = (int) Math.round(realDamage * (100 + hatePercent) / 100.0);
            LOG.debug("Stack von {} wird vom Gegner gehasst, bekommt {} % mehr Schaden.", getName(), hatePercent);
        }
        if (petrified) {
            applyDamage((int) Math.round(0.5 * realDamage));
            unpetrify();
        } else {
            applyDamage(realDamage);
        }
        tryRebirth();
    }

    /** Ein Hass-Paar: Marker am Angreifer, Volks-Marker am Ziel, Aufschlag in Prozent. */
    private record Hatred(UnitSpeciality hate, UnitSpeciality race, int percent) {
    }

    /**
     * Hass (heroes.thelazy.net): Angel und Devil, Titan und Black Dragon, Genie und Efreet je
     * +50 %, Elementare gegen ihr Gegenelement +100 % (Air gegen Earth, Water gegen Fire, je mit
     * Ausbau). Hier als Faktor auf den fertigen Schaden.
     */
    private static final List<Hatred> HATREDS = List.of(
            new Hatred(UnitSpeciality.ANGEL_HATE, UnitSpeciality.ANGEL_RACE, 50),
            new Hatred(UnitSpeciality.DEVIL_HATE, UnitSpeciality.DEVIL_RACE, 50),
            new Hatred(UnitSpeciality.TITAN_HATE, UnitSpeciality.TITAN_RACE, 50),
            new Hatred(UnitSpeciality.BLACK_DRAGON_HATE, UnitSpeciality.BLACK_DRAGON_RACE, 50),
            new Hatred(UnitSpeciality.GENIE_HATE, UnitSpeciality.GENIE_RACE, 50),
            new Hatred(UnitSpeciality.EFREET_HATE, UnitSpeciality.EFREET_RACE, 50),
            new Hatred(UnitSpeciality.AIR_HATE, UnitSpeciality.AIR_RACE, 100),
            new Hatred(UnitSpeciality.EARTH_HATE, UnitSpeciality.EARTH_RACE, 100),
            new Hatred(UnitSpeciality.FIRE_HATE, UnitSpeciality.FIRE_RACE, 100),
            new Hatred(UnitSpeciality.WATER_HATE, UnitSpeciality.WATER_RACE, 100));

    /** Aufschlag in Prozent, den ein Angreifer mit diesen Fähigkeiten gegen diesen Stack bekommt. */
    int hatePercent(Set<UnitSpeciality> attackerSpecialities) {
        return HATREDS.stream()
                .filter(h -> unit.hasSpeciality(h.race()) && attackerSpecialities.contains(h.hate()))
                .mapToInt(Hatred::percent).max().orElse(0);
    }

    public void loseTopCreatures(int killCount) {
        int actualKills = Math.min(killCount, aliveCount);
        aliveCount -= actualKills;
        topUnitCurrentHealth = aliveCount > 0 ? maxHealth() : 0;
        tryRebirth();
    }

    /**
     * Heilt den Stack um {@code hp} Lebenspunkte: füllt zuerst die aktuelle Top-Creature
     * auf, dann resurrected ganze tote Creatures (max bis zur Start-Stack-Größe).
     * Partielle Resurrects gibt es nicht — ein Rest unterhalb {@link #maxHealth()}
     * verfällt. Liefert die tatsächlich verwendete Heilmenge (für Diagnose-Tests).
     */
    public int heal(int hp) {
        if (hp <= 0) {
            return 0;
        }
        int max = maxHealth();
        int remaining = hp;
        int used = 0;
        // Spezialfall: Stack komplett tot → braucht ganze HP-Wert für erste Resurrect. Die
        // Zurückgeholten kommen frisch: ohne Zauber und Zustände.
        if (aliveCount == 0) {
            if (remaining < max || startCount == 0) {
                return 0;
            }
            resetConditions();
            aliveCount = 1;
            topUnitCurrentHealth = max;
            remaining -= max;
            used += max;
        }
        // Top-Creature auffüllen
        int gap = max - topUnitCurrentHealth;
        if (gap > 0 && remaining > 0) {
            int fill = Math.min(gap, remaining);
            topUnitCurrentHealth += fill;
            remaining -= fill;
            used += fill;
        }
        // Weitere ganze Creatures resurrecten — kein Über-startCount.
        while (remaining >= max && aliveCount < startCount) {
            aliveCount++;
            remaining -= max;
            used += max;
        }
        return used;
    }

    /**
     * Legt einen Zauber auf den Stack; ein schon liegender desselben Namens wird ersetzt, sein
     * Gegenstück aufgehoben (Bless/Curse, Haste/Slow, Fortune/Misfortune, Mirth/Sorrow).
     * Disrupting Ray stapelt sich und hält den ganzen Kampf.
     */
    public void applySpell(Spell spell, SkillLevel level, int rounds) {
        applySpell(spell, level, rounds, 0);
    }

    /**
     * Wie {@link #applySpell(Spell, SkillLevel, int)}, mit Zusatzpunkten aus der Spezialität des
     * Zaubernden ({@link HeroSpecialty#spellEffectBonus}).
     */
    public void applySpell(Spell spell, SkillLevel level, int rounds, int bonus) {
        Spell opposite = OPPOSITES.get(spell);
        if (opposite != null) {
            spells.remove(opposite);
        }
        if (spell == Spell.DISRUPTING_RAY) {
            ActiveSpell previous = spells.get(spell);
            int amount = (previous == null ? 0 : previous.amount())
                    + new ActiveSpell(spell, level, rounds).byLevel(3, 4, 5) + bonus;
            spells.put(spell, new ActiveSpell(spell, level, Integer.MAX_VALUE, amount));
            return;
        }
        spells.put(spell, new ActiveSpell(spell, level, rounds, bonus));
    }

    /** Anteil des Zauberschadens einer Schule nach Protection from … (−30/50 %), in Prozent. */
    public int spellDamagePercent(MagicSchool school) {
        Spell protection = switch (school) {
            case AIR -> Spell.PROTECTION_FROM_AIR;
            case EARTH -> Spell.PROTECTION_FROM_EARTH;
            case FIRE -> Spell.PROTECTION_FROM_FIRE;
            case WATER -> Spell.PROTECTION_FROM_WATER;
        };
        return 100 - spellValue(protection, 30, 50, 50);
    }

    public boolean hasSpell(Spell spell) {
        return spells.containsKey(spell);
    }

    public @Nullable ActiveSpell activeSpell(Spell spell) {
        return spells.get(spell);
    }

    public Collection<ActiveSpell> activeSpells() {
        return Collections.unmodifiableCollection(spells.values());
    }

    /** Entfernt die passenden Zauber und liefert, wie viele es waren. */
    public int removeSpells(Predicate<Spell> which) {
        int before = spells.size();
        spells.keySet().removeIf(which);
        return before - spells.size();
    }

    /**
     * Blind endet, sobald der Stack angegriffen wird (Manual S. 73). Der Angriff, der ihn weckt,
     * wird mit 50 % (Basic), 25 % (Advanced) oder gar nicht (Expert) des Attack-Ratings
     * beantwortet.
     *
     * @return Attack in Prozent für den Gegenschlag auf diesen Angriff; 100 ohne Blind
     */
    public int wakeFromBlind() {
        ActiveSpell blind = spells.remove(Spell.BLIND);
        return blind == null ? 100 : blind.byLevel(50, 25, 0);
    }

    void setAttackPercent(int percent) {
        this.attackPercent = percent;
    }

    public int spellCastsLeft() {
        return spellCastsLeft;
    }

    void useSpellCast() {
        if (spellCastsLeft > 0) {
            spellCastsLeft--;
        }
    }

    /** Wie {@link #spellValue}, dazu die Zusatzpunkte der Spezialität des Zaubernden. */
    private int spellPoints(Spell spell, int basic, int advanced, int expert) {
        ActiveSpell active = spells.get(spell);
        return active == null ? 0 : active.byLevel(basic, advanced, expert) + active.amount();
    }

    /** Wert eines liegenden Zaubers je Stufe, 0 ohne den Zauber. */
    private int spellValue(Spell spell, int basic, int advanced, int expert) {
        ActiveSpell active = spells.get(spell);
        return active == null ? 0 : active.byLevel(basic, advanced, expert);
    }

    /**
     * Rückschaden eines Feuerschilds: 20 % beim Efreet Sultan, der Zauber Fire Shield gibt
     * 20/25/30 % (Manual S. 74). Beides zusammen zählt nicht doppelt, es gilt der höhere Wert.
     */
    public int fireShieldDamageFor(int incomingDamage) {
        int percent = Math.max(unit.hasSpeciality(UnitSpeciality.FIRE_SHIELD) ? 20 : 0,
                spellValue(Spell.FIRE_SHIELD, 20, 25, 30));
        return percent == 0 ? 0 : (int) Math.round(incomingDamage * percent / 100.0);
    }

    public boolean isRebirthUsed() {
        return rebirthUsed;
    }

    /** Ob der Stack seine Resurrection noch wirken kann — einmal pro Kampf (Manual S. 87). */
    public boolean canResurrect() {
        return aliveCount > 0 && !resurrectionUsed && unit.hasSpeciality(UnitSpeciality.RESURRECTION);
    }

    public void markResurrectionUsed() {
        resurrectionUsed = true;
    }

    /** Ob der Stack seine Dämonen-Beschwörung noch wirken kann — einmal pro Kampf (Manual S. 98). */
    public boolean canRaiseDemons() {
        return aliveCount > 0 && !demonsRaised && unit.hasSpeciality(UnitSpeciality.RAISE_DEMONS);
    }

    public void markDemonsRaised() {
        demonsRaised = true;
    }

    /**
     * Lässt einen ganz gefallenen Stack als andere Kreatur auf demselben Feld zurückkehren —
     * die Pit Lords machen aus den Toten Dämonen. Der Stack beginnt frisch: volle Lebenspunkte,
     * keine Zustände, {@code count} ist seine neue Start-Stärke.
     */
    public void riseAs(Unit newUnit, int count) {
        if (aliveCount > 0) {
            throw new IllegalStateException(getName() + " lebt noch und kann nicht auferstehen");
        }
        if (count <= 0) {
            throw new IllegalArgumentException("count must be > 0, was " + count);
        }
        unit = Objects.requireNonNull(newUnit, "newUnit");
        startCount = count;
        aliveCount = count;
        artifactHealth = artifactHealthFor(commander);
        topUnitCurrentHealth = maxHealth();
        shotsRemaining = newUnit.shots();
        resetConditions();
        spellCastsLeft = CreatureSpellcasting.castsFor(newUnit);
    }

    /** Ein Stack, der aus dem Tod zurückkehrt, trägt keine Zauber und Zustände mehr. */
    private void resetConditions() {
        petrified = false;
        petrifiedCounter = 0;
        cursed = false;
        cursedCounter = 0;
        poisoned = false;
        poisonedCounter = 0;
        diseased = false;
        diseasedCounter = 0;
        aged = false;
        defending = false;
        retaliationsThisTurn = 0;
        spells.clear();
    }

    /** Beschworen (Summon Elementals): verschwindet nach dem Kampf und zählt nicht als Überlebender. */
    public boolean isSummoned() {
        return summoned;
    }

    /** Macht den Stack zum Klon von {@code origin}, der nach Runde {@code lastRound} vergeht. */
    void markClone(Stack origin, int lastRound) {
        cloneOf = origin;
        cloneLastRound = lastRound;
        markSummoned();
    }

    public @Nullable Stack cloneOf() {
        return cloneOf;
    }

    /** Vergeht der lebende Klon jetzt: Original gefallen oder (am Rundenende) seine Zeit um? */
    boolean cloneEnds(int round, boolean roundEnd) {
        Stack origin = cloneOf;
        return origin != null && isAlive() && (!origin.isAlive() || (roundEnd && round >= cloneLastRound));
    }

    /** Der Stack verschwindet ganz, ohne Leiche (geopfert oder vergangener Klon). */
    void vanish() {
        aliveCount = 0;
        topUnitCurrentHealth = 0;
        corpseless = true;
    }

    /** Hinterlässt der gefallene Stack eine Leiche? Beschworene, Klone und Geopferte nicht. */
    public boolean leavesCorpse() {
        return !summoned && !corpseless;
    }

    /** Wer den Stack steuert: unter Hypnotize die Gegenseite, sonst die eigene. */
    public Side controller() {
        if (!spells.containsKey(Spell.HYPNOTIZE)) {
            return side;
        }
        return side == Side.ATTACKER ? Side.DEFENDER : Side.ATTACKER;
    }

    void markSummoned() {
        summoned = true;
    }

    /** Verstärkt einen lebenden Stack um {@code count} frische Kreaturen (erneute Beschwörung). */
    void reinforce(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("count must be > 0, was " + count);
        }
        aliveCount += count;
        startCount += count;
    }

    /** Kreaturen, mit denen der Stack in den Kampf ging (nach einer Auferstehung: die neue Zahl). */
    public int startCount() {
        return startCount;
    }

    /** Lebenspunkte, die dem Stack zur vollen Start-Stärke fehlen. */
    public int missingHealth() {
        int full = startCount * maxHealth();
        int current = aliveCount > 0 ? (aliveCount - 1) * maxHealth() + topUnitCurrentHealth : 0;
        return full - current;
    }

    private void tryRebirth() {
        if (aliveCount > 0 || rebirthUsed || !unit.hasSpeciality(UnitSpeciality.REBIRTH)) {
            return;
        }
        int restored = Math.max(1, startCount / 5);
        aliveCount = restored;
        topUnitCurrentHealth = maxHealth();
        rebirthUsed = true;
        LOG.debug("{} wird durch Wiedergeburt mit {} Einheiten zurueckgebracht.", getName(), restored);
    }

    public void petrify() {
        petrified = true;
        petrifiedCounter = 3;
    }

    /** Säureatem des Rust Dragon: −3 Defense bis zum Ende des Kampfs, stapelbar. */
    void corrode() {
        acidHits++;
    }

    public void curse() {
        cursed = true;
        cursedCounter = 3;
    }

    public void poison() {
        poisoned = true;
        poisonedCounter = 3;
    }

    public boolean isPoisoned() {
        return poisoned;
    }

    public void disease() {
        diseased = true;
        diseasedCounter = 3;
    }

    public boolean isDiseased() {
        return diseased;
    }

    public void age() {
        aged = true;
        topUnitCurrentHealth = Math.max(1, topUnitCurrentHealth / 2);
    }

    public boolean isAged() {
        return aged;
    }

    public void endTurn() {
        // Defend gilt nur für die laufende Runde — nächste Runde gibt's wieder Base-Defense.
        defending = false;
        waitedThisTurn = false;
        spells.replaceAll((spell, active) -> active.tick());
        spells.values().removeIf(active -> active.roundsLeft() <= 0);
        if (petrifiedCounter > 0 && --petrifiedCounter == 0) {
            unpetrify();
        }
        if (cursedCounter > 0 && --cursedCounter == 0) {
            uncurse();
        }
        if (poisonedCounter > 0) {
            applyPoisonTick();
            if (--poisonedCounter == 0) {
                unpoison();
            }
        }
        if (diseasedCounter > 0 && --diseasedCounter == 0) {
            undisease();
        }
        if (regenerates() && isAlive()) {
            topUnitCurrentHealth = Math.min(maxHealth(), topUnitCurrentHealth + REGENERATION_HEALTH);
        }
        retaliationsThisTurn = 0;
    }

    public boolean isAbleToAct() {
        if (!isAlive()) {
            LOG.debug("Stack von {} ist bereits tot und macht nichts.", getName());
            return false;
        }
        if (petrified) {
            LOG.debug("Stack von {} ist versteinert und macht nichts.", getName());
            return false;
        }
        if (spells.containsKey(Spell.BLIND)) {
            LOG.debug("Stack von {} ist geblendet und macht nichts.", getName());
            return false;
        }
        return true;
    }

    private void applyDamage(int damage) {
        int rest = damage;
        int countBefore = aliveCount;
        while (rest > 0 && aliveCount > 0) {
            int absorbed = Math.min(topUnitCurrentHealth, rest);
            topUnitCurrentHealth -= absorbed;
            rest -= absorbed;
            if (topUnitCurrentHealth == 0) {
                aliveCount--;
                if (aliveCount > 0) {
                    topUnitCurrentHealth = maxHealth();
                }
            }
        }
        LOG.debug("Stack von {} erhaelt {} Schaden. {} wurden getoetet.",
                getName(), damage, countBefore - aliveCount);
    }

    private void uncurse() {
        cursed = false;
        cursedCounter = 0;
        LOG.debug("{} wurde entflucht.", getName());
    }

    private void unpetrify() {
        petrified = false;
        petrifiedCounter = 0;
        LOG.debug("{} wurde entsteinert.", getName());
    }

    private void unpoison() {
        poisoned = false;
        poisonedCounter = 0;
        LOG.debug("{} ist nicht mehr vergiftet.", getName());
    }

    private void undisease() {
        diseased = false;
        diseasedCounter = 0;
        LOG.debug("{} ist nicht mehr krank.", getName());
    }

    private void applyPoisonTick() {
        if (!isAlive()) {
            return;
        }
        int loss = maxHealth() / 2;
        LOG.debug("{} verliert {} Gesundheit durch Gift.", getName(), loss);
        applyDamage(loss);
    }
}
