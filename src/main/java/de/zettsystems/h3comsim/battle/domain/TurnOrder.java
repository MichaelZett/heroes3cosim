package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

/**
 * Wer als Nächstes handelt. Grundregel ist der Speed (Manual S. 40: „faster units going before
 * slower ones"), in der Hauptphase absteigend, in der Wait-Phase aufsteigend.
 *
 * <p>Gleichstand zwischen den Seiten: Das Manual sagt nur „the attacker will go first". Das gilt
 * in SoD aber nur zu Kampfbeginn. Danach handelt bei gleichem Speed die Seite zuerst, die
 * <em>nicht</em> als letzte am Zug war — über Phasen- und Rundengrenzen hinweg
 * (heroes.thelazy.net, Combat, „Ties"). Ein Spiegel-Duell verliert damit seinen eingebauten
 * Angreifer-Vorteil: Zwei gleich schnelle Stacks wechseln sich ab, statt dass der Angreifer jede
 * Runde zuerst zuschlägt.
 *
 * <p>Gleichstand auf derselben Seite: der niedrigere Slot zuerst.
 *
 * <p>Zustandsbehaftet, ein Exemplar pro Kampf: Es merkt sich, welche Seite zuletzt gehandelt hat.
 * Als „am Zug gewesen" zählt jede Aktivierung, auch Wait und das Einfrieren durch schlechte
 * Moral; ein Stack, der vor seinem Zug fällt, zählt nicht.
 */
final class TurnOrder {

    private @Nullable Side lastActed;

    /**
     * Wählt den nächsten Stack aus {@code pending} und entfernt ihn dort.
     *
     * @param fastestFirst {@code true} in der Hauptphase, {@code false} in der Wait-Phase
     */
    Stack next(List<Stack> pending, boolean fastestFirst) {
        Comparator<Stack> bySpeed = Comparator.comparingInt(Stack::getSpeed);
        int speed = (fastestFirst ? pending.stream().max(bySpeed) : pending.stream().min(bySpeed))
                .orElseThrow()
                .getSpeed();
        List<Stack> tied = pending.stream().filter(s -> s.getSpeed() == speed).toList();
        Side side = tied.stream().anyMatch(s -> s.side() == preferredSide())
                ? preferredSide()
                : tied.getFirst().side();
        Stack chosen = tied.stream()
                .filter(s -> s.side() == side)
                .min(Comparator.comparingInt(Stack::slot))
                .orElseThrow();
        pending.remove(chosen);
        return chosen;
    }

    void acted(Stack stack) {
        lastActed = stack.side();
    }

    private Side preferredSide() {
        if (lastActed == null) {
            return Side.ATTACKER;
        }
        return lastActed == Side.ATTACKER ? Side.DEFENDER : Side.ATTACKER;
    }
}
