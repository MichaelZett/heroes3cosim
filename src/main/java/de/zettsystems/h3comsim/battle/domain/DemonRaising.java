package de.zettsystems.h3comsim.battle.domain;

import org.jspecify.annotations.Nullable;

import java.util.Comparator;

/**
 * Dämonen-Beschwörung der Pit Lords (Manual S. 98): „Once per combat, pit lords are able to
 * resurrect dead allied troops as demons in place of another action. The number raised cannot be
 * higher than the number of pit lords performing the resurrection."
 *
 * <p>Die Rechnung stammt aus heroes.thelazy.net: Jeder Pit Lord bringt 50 Lebenspunkte an
 * Dämonen auf, höchstens aber so viele Lebenspunkte, wie der gefallene Stack hatte, und nie mehr
 * Dämonen als dort Kreaturen lagen —
 * {@code min(⌊min(Tote × HP, 50 × Pit Lords) / 35⌋, Tote)}.
 *
 * <p>Ziel ist ein <em>ganz</em> gefallener verbündeter Stack aus Lebewesen ({@link Unit#isLiving()}),
 * dessen Feld frei ist. Er kehrt an seinem Platz und in seinem Slot als Dämonen-Stack zurück.
 */
final class DemonRaising {

    static final int HEALTH_PER_CASTER = 50;

    private DemonRaising() {
    }

    static int demonsFrom(Stack caster, Stack corpse) {
        int corpseHealth = corpse.startCount() * corpse.unit().health();
        int capacity = HEALTH_PER_CASTER * caster.getCount();
        return Math.min(Math.min(corpseHealth, capacity) / UnitCatalog.DEMON.health(), corpse.startCount());
    }

    // corpse != caster, s != corpse: Identitätsvergleich auf der mutable Stack-Entity (kein equals).
    @SuppressWarnings("ReferenceEquality")
    static boolean isValidCorpse(Stack caster, Stack corpse, BattleSetup setup) {
        return corpse != caster
                && corpse.side() == caster.side()
                && !corpse.isAlive()
                && corpse.leavesCorpse()
                && corpse.unit().isLiving()
                && setup.aliveStacks().stream().noneMatch(s -> s.occupies(corpse.position()))
                && demonsFrom(caster, corpse) > 0;
    }

    /**
     * Solver-Heuristik: lohnt die Beschwörung jetzt? Ja, sobald ein Leichnam mindestens die
     * Hälfte der Dämonen hergibt, die die Pit Lords höchstens beschwören können — sonst kämpfen
     * sie besser selbst. Unter mehreren Leichnamen der ergiebigste.
     *
     * @return der Leichnam, oder {@code null}, wenn die Pit Lords besser kämpfen
     */
    static @Nullable Stack worthwhileCorpse(Stack caster, @Nullable BattleSetup setup) {
        if (setup == null || !caster.canRaiseDemons()) {
            return null;
        }
        int maxDemons = HEALTH_PER_CASTER * caster.getCount() / UnitCatalog.DEMON.health();
        return setup.stacksOf(caster.side()).stream()
                .filter(corpse -> isValidCorpse(caster, corpse, setup))
                .filter(corpse -> 2 * demonsFrom(caster, corpse) >= maxDemons)
                .max(Comparator.comparingInt((Stack corpse) -> demonsFrom(caster, corpse))
                        .thenComparingInt(corpse -> -corpse.slot()))
                .orElse(null);
    }
}
