package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Tactics (Manual S. 42): der Held stellt seine Truppen vor Kampfbeginn auf, wenn der Gegner die
 * Fertigkeit nicht oder auf niedrigerer Stufe hat. Erlaubt sind die ersten 3 / 5 / 7 Hex-Spalten
 * bei einem, zwei oder drei Graden Vorsprung — bei gleicher Stufe stellt keiner auf.
 *
 * <p>Die Aufstellung selbst ist eine Entscheidung, die das Spiel dem Spieler überlässt. Hier
 * rücken alle Nahkämpfer in ihrer Reihe an die vorderste freie Zelle der Zone vor, höchstens
 * {@value #MAX_USEFUL_COLUMNS} Spalten tief. Die Schützen rücken nur mit, wenn die Zone diese
 * fünf Spalten erreicht: von dort liegt der gegnerische Rand höchstens zehn Felder entfernt, der
 * erste Schuss trifft ohne Distanzstrafe. Gemessen mit dem umgebauten Strategic-Solver
 * (Win-Rate der Tactics-Seite Basic/Advanced/Expert): Schützen am Rand 0.555/0.528/0.530,
 * Schützen bei fünf Spalten mit vorn 0.555/0.588/0.555, Schützen immer mit vorn
 * 0.482/0.588/0.555 — drei Spalten heben die Strafe nicht auf und stellen sie nur bloß.
 * Verworfene Varianten: nur Bodentruppen vorrücken 0.520/0.629/0.619 und nur außer Reichweite
 * gleich schneller Gegner 0.473/0.471/0.464 (beides mit dem alten Solver); nur Stacks mit Speed
 * unter 7 vorrücken 0.461/0.474/0.478, Tiefe nach Speed (9 − Speed) 0.477/0.499/0.495.
 */
public final class Tactics {

    /**
     * Tiefer als fünf Spalten rückt die Aufstellung nicht vor, auch wenn Expert Tactics sieben
     * erlaubt. Gemessen mit {@code TacticsImpactTest} (Expert gegen keinen): volle sieben
     * Spalten 0.602, auf fünf gekappt 0.672 — bei sieben stehen die Nahkämpfer in Runde 1 vor
     * fast jeder gegnerischen Einheit.
     */
    static final int MAX_USEFUL_COLUMNS = 5;

    private Tactics() {
    }

    /**
     * Breite der Aufstellungszone in Hex-Spalten, gezählt vom eigenen Rand; 0 = keine
     * Aufstellungsphase.
     */
    public static int columns(@Nullable Hero own, @Nullable Hero enemy) {
        int advantage = rank(own) - rank(enemy);
        return advantage <= 0 ? 0 : 2 * advantage + 1;
    }

    private static int rank(@Nullable Hero hero) {
        if (hero == null) {
            return 0;
        }
        return switch (hero.levelOf(SecondarySkill.TACTICS)) {
            case NONE -> 0;
            case BASIC -> 1;
            case ADVANCED -> 2;
            case EXPERT -> 3;
        };
    }

    /**
     * Rückt die Nahkämpfer von {@code stacks} innerhalb der Zone vor. Ziel ist je Stack die
     * vorderste passierbare, unbesetzte Zelle seiner Reihe, sofern sie weiter vorn liegt als der
     * Stack schon steht.
     *
     * @param all alle Stacks beider Seiten, für die Belegung
     */
    static void arrange(List<Stack> stacks, Side side, int columns, Battlefield field, List<Stack> all) {
        if (columns <= 0) {
            return;
        }
        int depth = Math.min(columns, MAX_USEFUL_COLUMNS);
        for (Stack stack : stacks) {
            if (stack.unit().attackType() != AttackType.LONG_RANGE || depth == MAX_USEFUL_COLUMNS) {
                Hex target = targetFor(stack, side, depth, field, all);
                if (target != null) {
                    stack.moveTo(target);
                }
            }
        }
    }

    /** Vorderste freie Zelle der Reihe innerhalb von {@code depth} Spalten vor dem Stack, sonst null. */
    private static @Nullable Hex targetFor(Stack stack, Side side, int depth, Battlefield field,
                                           List<Stack> all) {
        int r = stack.position().r();
        for (int d = depth - 1; d >= 0; d--) {
            Hex target = new Hex(side == Side.ATTACKER ? field.leftmostQ(r) + d : field.rightmostQ(r) - d, r);
            if (!isAhead(target, stack.position(), side)) {
                return null;
            }
            if (stack.hexesAt(target).stream().allMatch(h -> field.isPassable(h) && isFree(h, stack, all))) {
                return target;
            }
        }
        return null;
    }

    private static boolean isAhead(Hex target, Hex current, Side side) {
        return side == Side.ATTACKER ? target.q() > current.q() : target.q() < current.q();
    }

    // s != mover: Identitätsvergleich auf der mutable Stack-Entity.
    @SuppressWarnings("ReferenceEquality")
    private static boolean isFree(Hex hex, Stack mover, List<Stack> all) {
        return all.stream().noneMatch(s -> s != mover && s.isAlive() && s.occupies(hex));
    }
}
