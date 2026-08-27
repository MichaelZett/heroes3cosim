package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Konfiguration einer Schlacht — bis zu 7 Stacks pro Seite. Single-Battle nutzt die
 * Convenience-Konstruktoren mit einem Stack pro Seite (Slot 0); Multi-Stack-Battles
 * konstruieren die Listen vorgängig.
 */
public class BattleSetup {

    private static final Hex DEFAULT_ATTACKER_POSITION = new Hex(0, 5);
    private static final Hex DEFAULT_DEFENDER_POSITION = new Hex(14, 5);

    private final List<Stack> attackerStacks;
    private final List<Stack> defenderStacks;
    private final Battlefield battlefield;
    private final @Nullable Hero attackerHero;
    private final @Nullable Hero defenderHero;

    public BattleSetup(Unit attackerUnit, int attackerCount,
                       Unit defenderUnit, int defenderCount) {
        this(attackerUnit, attackerCount, defenderUnit, defenderCount,
                Battlefield.STANDARD, DEFAULT_ATTACKER_POSITION, DEFAULT_DEFENDER_POSITION);
    }

    public BattleSetup(Unit attackerUnit, int attackerCount,
                       Unit defenderUnit, int defenderCount,
                       Battlefield battlefield, Hex attackerPosition, Hex defenderPosition) {
        this(List.of(new Stack(attackerUnit, attackerCount, attackerPosition, Side.ATTACKER, 0)),
                List.of(new Stack(defenderUnit, defenderCount, defenderPosition, Side.DEFENDER, 0)),
                battlefield);
    }

    public BattleSetup(List<Stack> attackerStacks, List<Stack> defenderStacks, Battlefield battlefield) {
        this(attackerStacks, defenderStacks, battlefield, null, null);
    }

    /**
     * Variante mit Helden. Ein Held führt die ganze Armee, nicht einen einzelnen Stack —
     * deshalb wird er hier auf alle Stacks seiner Seite verteilt, statt beim Stack-Bau
     * mitgegeben zu werden. So kann kein Stack ohne oder mit dem falschen Anführer entstehen.
     *
     * @param attackerHero Held des Angreifers, {@code null} für eine führerlose Armee
     * @param defenderHero Held des Verteidigers, {@code null} für eine führerlose Armee
     */
    public BattleSetup(List<Stack> attackerStacks, List<Stack> defenderStacks,
                       Battlefield battlefield,
                       @Nullable Hero attackerHero, @Nullable Hero defenderHero) {
        Objects.requireNonNull(attackerStacks, "attackerStacks");
        Objects.requireNonNull(defenderStacks, "defenderStacks");
        if (attackerStacks.isEmpty() || defenderStacks.isEmpty()) {
            throw new IllegalArgumentException("Each side needs at least one stack");
        }
        this.attackerStacks = List.copyOf(attackerStacks);
        this.defenderStacks = List.copyOf(defenderStacks);
        this.battlefield = Objects.requireNonNull(battlefield, "battlefield");
        this.attackerHero = attackerHero;
        this.defenderHero = defenderHero;
        this.attackerStacks.forEach(stack -> stack.assignCommander(attackerHero));
        this.defenderStacks.forEach(stack -> stack.assignCommander(defenderHero));
    }

    public @Nullable Hero attackerHero() {
        return attackerHero;
    }

    public @Nullable Hero defenderHero() {
        return defenderHero;
    }

    public @Nullable Hero heroOf(Side side) {
        return side == Side.ATTACKER ? attackerHero : defenderHero;
    }

    public List<Stack> attackerStacks() {
        return attackerStacks;
    }

    public List<Stack> defenderStacks() {
        return defenderStacks;
    }

    public List<Stack> stacksOf(Side side) {
        return side == Side.ATTACKER ? attackerStacks : defenderStacks;
    }

    public List<Stack> aliveStacks() {
        List<Stack> alive = new ArrayList<>(attackerStacks.size() + defenderStacks.size());
        for (Stack s : attackerStacks) {
            if (s.isAlive()) {
                alive.add(s);
            }
        }
        for (Stack s : defenderStacks) {
            if (s.isAlive()) {
                alive.add(s);
            }
        }
        return alive;
    }

    public List<Stack> opponentsOf(Stack stack) {
        List<Stack> opponents = stack.side() == Side.ATTACKER ? defenderStacks : attackerStacks;
        List<Stack> alive = new ArrayList<>(opponents.size());
        for (Stack s : opponents) {
            if (s.isAlive()) {
                alive.add(s);
            }
        }
        return alive;
    }

    /**
     * Single-Battle-Convenience: liefert den ersten Attacker-Stack. Im Multi-Stack-Pfad
     * sollte stattdessen {@link #attackerStacks()} verwendet werden.
     */
    public Stack getAttacker() {
        return attackerStacks.get(0);
    }

    /**
     * Single-Battle-Convenience: liefert den ersten Defender-Stack.
     */
    public Stack getDefender() {
        return defenderStacks.get(0);
    }

    public Battlefield battlefield() {
        return battlefield;
    }

    public String getAttackerName() {
        return getAttacker().getName();
    }

    public String getDefenderName() {
        return getDefender().getName();
    }

    public boolean isAttackerAlive() {
        return attackerStacks.stream().anyMatch(Stack::isAlive);
    }

    public boolean isDefenderAlive() {
        return defenderStacks.stream().anyMatch(Stack::isAlive);
    }

    public boolean bothAlive() {
        return isAttackerAlive() && isDefenderAlive();
    }

    public int getAttackerCount() {
        return attackerStacks.stream().mapToInt(Stack::getCount).sum();
    }

    public int getDefenderCount() {
        return defenderStacks.stream().mapToInt(Stack::getCount).sum();
    }

    /**
     * Single-Battle-Convenience: liefert den einzigen lebenden Gegner. Wirft, wenn die Seite
     * mehr als einen lebenden Stack hat (Multi-Stack-Pfad soll {@link #opponentsOf(Stack)} nutzen).
     */
    public Stack getTarget(Stack activeStack) {
        List<Stack> opponents = opponentsOf(activeStack);
        if (opponents.size() != 1) {
            throw new IllegalStateException(
                    "getTarget(...) ist nur für 1-vs-1-Pfade definiert; gefunden: " + opponents.size()
                            + " lebende Gegner. Im Multi-Stack-Pfad opponentsOf(...) nutzen.");
        }
        return opponents.get(0);
    }
}
