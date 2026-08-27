package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.Side;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Set;
import java.util.random.RandomGenerator;

@Slf4j
public class Stack {

    private final Unit unit;
    private final Side side;
    private final int slot;
    private final int startCount;
    private int aliveCount;
    private int topUnitCurrentHealth;
    private int shotsRemaining;
    private Hex position;
    private boolean petrified;
    private int petrifiedCounter;
    private boolean cursed;
    private int cursedCounter;
    private boolean poisoned;
    private int poisonedCounter;
    private int retaliationsThisTurn;
    private boolean diseased;
    private int diseasedCounter;
    private boolean aged;
    private boolean rebirthUsed;
    private boolean defending;
    private boolean waitedThisTurn;
    private @Nullable Hero commander;

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

    public Hex position() {
        return position;
    }

    public void moveTo(Hex position) {
        this.position = Objects.requireNonNull(position, "position");
    }

    public int shotsRemaining() {
        return shotsRemaining;
    }

    public void useShot() {
        if (shotsRemaining > 0) {
            shotsRemaining--;
        }
    }

    public boolean canShoot() {
        return unit.attackType() == AttackType.LONG_RANGE && shotsRemaining > 0;
    }

    public String getName() {
        return unit.name();
    }

    public int getSpeed() {
        return unit.speed();
    }

    public int getAttack() {
        return unit.attack() - (diseased ? 2 : 0) + (commander == null ? 0 : commander.attack());
    }

    public int getDefense() {
        int base = unit.defense() - (diseased ? 2 : 0)
                + (commander == null ? 0 : commander.defense());
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
        this.commander = hero;
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
        int baseValue = cursed ? unit.minDamage() : rng.nextInt(unit.minDamage(), unit.maxDamage() + 1);
        if (unit.hasPenality(usedAttackType)) {
            baseValue = (int) Math.round(0.5 * baseValue);
            LOG.info("Stack von {} hat Nachteil, halbiert also den Schaden.", getName());
        }
        if (unit.hasSpeciality(UnitSpeciality.DEATH_BLOW) && rng.nextInt(1, 101) <= 20) {
            baseValue = baseValue * 2;
            LOG.info("Stack von {} nutzt Death Blow, verdoppelt also den Schaden.", getName());
        }
        if (unit.hasSpeciality(UnitSpeciality.IMPACT_DAMAGE) && hexesMoved > 0) {
            // H3 Jousting: +5 % pro Hex Bewegung, gedeckelt bei +50 %.
            int bonusPercent = Math.min(50, hexesMoved * 5);
            baseValue = (int) Math.round(baseValue * (1.0 + bonusPercent / 100.0));
            LOG.info("Stack von {} bekommt {}% Jousting-Bonus durch {} Hex Bewegung.",
                    getName(), bonusPercent, hexesMoved);
        }
        return baseValue * aliveCount;
    }

    public int calculateAttackBoniMaliPercentage(int defense) {
        // RoE-Manual S. 43: +5 % pro Attack-Punkt Differenz, gedeckelt bei +400 %
        // (80 Punkte); −2 % pro Defense-Punkt Differenz, min. 30 % Damage (= −70 %, also
        // 35 Punkte). Ohne Cap explodiert Damage bei extremen Tier-Lücken.
        int diff = getAttack() - defense;
        return diff >= 0 ? Math.min(diff * 5, 400) : Math.max(diff * 2, -70);
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
        return retaliationsThisTurn < maxRetaliationsPerTurn();
    }

    public void recordRetaliation() {
        retaliationsThisTurn++;
    }

    private int maxRetaliationsPerTurn() {
        if (unit.hasSpeciality(UnitSpeciality.COUNTERSTRIKE_UNLIMITED)) {
            return Integer.MAX_VALUE;
        }
        if (unit.hasSpeciality(UnitSpeciality.COUNTERSTRIKE_TWICE)) {
            return 2;
        }
        return 1;
    }

    public boolean hasGoodMorale(RandomGenerator rng) {
        int morale = unit.morale();
        if (morale > 0) {
            int random = rng.nextInt(1000);
            return (morale == 3 && random <= 125)
                    || (morale == 2 && random <= 83)
                    || (morale == 1 && random <= 42);
        }
        return false;
    }

    public void takeDamage(int baseDamage, Set<UnitSpeciality> attackerSpecialities) {
        int realDamage = baseDamage;
        boolean angelHate = unit.hasSpeciality(UnitSpeciality.ANGEL_RACE)
                && attackerSpecialities.contains(UnitSpeciality.ANGEL_HATE);
        boolean devilHate = unit.hasSpeciality(UnitSpeciality.DEVIL_RACE)
                && attackerSpecialities.contains(UnitSpeciality.DEVIL_HATE);
        boolean titanHate = unit.hasSpeciality(UnitSpeciality.TITAN_RACE)
                && attackerSpecialities.contains(UnitSpeciality.TITAN_HATE);
        if (angelHate || devilHate || titanHate) {
            realDamage = (int) Math.round(1.5 * realDamage);
            LOG.info("Stack von {} wird vom Gegner gehasst, bekommt 1,5x Schaden.", getName());
        }
        if (petrified) {
            applyDamage((int) Math.round(0.5 * realDamage));
            unpetrify();
        } else {
            applyDamage(realDamage);
        }
        tryRebirth();
    }

    public void loseTopCreatures(int killCount) {
        int actualKills = Math.min(killCount, aliveCount);
        aliveCount -= actualKills;
        topUnitCurrentHealth = aliveCount > 0 ? unit.health() : 0;
        tryRebirth();
    }

    /**
     * Heilt den Stack um {@code hp} Lebenspunkte: füllt zuerst die aktuelle Top-Creature
     * auf, dann resurrected ganze tote Creatures (max bis zur Start-Stack-Größe).
     * Partielle Resurrects gibt es nicht — ein Rest unterhalb {@code unit.health()}
     * verfällt. Liefert die tatsächlich verwendete Heilmenge (für Diagnose-Tests).
     */
    public int heal(int hp) {
        if (hp <= 0) {
            return 0;
        }
        int max = unit.health();
        int remaining = hp;
        int used = 0;
        // Spezialfall: Stack komplett tot → braucht ganze HP-Wert für erste Resurrect.
        if (aliveCount == 0) {
            if (remaining < max || startCount == 0) {
                return 0;
            }
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

    public int fireShieldDamageFor(int incomingDamage) {
        if (!unit.hasSpeciality(UnitSpeciality.FIRE_SHIELD)) {
            return 0;
        }
        return (int) Math.round(incomingDamage * 0.2);
    }

    public boolean isRebirthUsed() {
        return rebirthUsed;
    }

    private void tryRebirth() {
        if (aliveCount > 0 || rebirthUsed || !unit.hasSpeciality(UnitSpeciality.REBIRTH)) {
            return;
        }
        int restored = Math.max(1, startCount / 5);
        aliveCount = restored;
        topUnitCurrentHealth = unit.health();
        rebirthUsed = true;
        LOG.info("{} wird durch Wiedergeburt mit {} Einheiten zurueckgebracht.", getName(), restored);
    }

    public void petrify() {
        petrified = true;
        petrifiedCounter = 3;
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
        if (unit.hasSpeciality(UnitSpeciality.REGENERATION) && isAlive()) {
            topUnitCurrentHealth = unit.health();
        }
        retaliationsThisTurn = 0;
    }

    public boolean isAbleToAct() {
        if (!isAlive()) {
            LOG.info("Stack von {} ist bereits tot und macht nichts.", getName());
            return false;
        }
        if (petrified) {
            LOG.info("Stack von {} ist versteinert und macht nichts.", getName());
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
                    topUnitCurrentHealth = unit.health();
                }
            }
        }
        LOG.info("Stack von {} erhaelt {} Schaden. {} wurden getoetet.",
                getName(), damage, countBefore - aliveCount);
    }

    private void uncurse() {
        cursed = false;
        cursedCounter = 0;
        LOG.info("{} wurde entflucht.", getName());
    }

    private void unpetrify() {
        petrified = false;
        petrifiedCounter = 0;
        LOG.info("{} wurde entsteinert.", getName());
    }

    private void unpoison() {
        poisoned = false;
        poisonedCounter = 0;
        LOG.info("{} ist nicht mehr vergiftet.", getName());
    }

    private void undisease() {
        diseased = false;
        diseasedCounter = 0;
        LOG.info("{} ist nicht mehr krank.", getName());
    }

    private void applyPoisonTick() {
        if (!isAlive()) {
            return;
        }
        int loss = unit.health() / 2;
        LOG.info("{} verliert {} Gesundheit durch Gift.", getName(), loss);
        applyDamage(loss);
    }
}
