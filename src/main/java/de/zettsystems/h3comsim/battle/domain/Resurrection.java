package de.zettsystems.h3comsim.battle.domain;

import org.jspecify.annotations.Nullable;

import java.util.Comparator;

/**
 * Resurrection des Arch Angels (Manual S. 87): „the archangel is able to resurrect dead allied
 * troops once per combat".
 *
 * <p>Wie viel zurückkommt, sagt das Manual nicht. Übernommen ist der SoD-Wert von
 * heroes.thelazy.net: 100 HP je Arch Angel im Stack, unabhängig von der Zauberformel und
 * dauerhaft. Die HP füllen zuerst die angeschlagene oberste Kreatur auf und holen dann ganze
 * Kreaturen zurück, nie über die Start-Stärke hinaus ({@link Stack#heal(int)}).
 *
 * <p>Bewusst eingeschränkt: Ziel ist nur ein <em>lebender</em> verbündeter Stack mit Verlusten.
 * In H3 kann die Resurrection auch einen ganz gefallenen Stack zurückholen, sofern sein Feld
 * frei ist — dafür fehlt der Engine ein Leichen-Konzept. Der Arch Angel belebt außerdem nicht
 * sich selbst.
 */
final class Resurrection {

    static final int HEALTH_PER_CASTER = 100;

    private Resurrection() {
    }

    static int healthFor(Stack caster) {
        return HEALTH_PER_CASTER * caster.getCount();
    }

    // target != caster: Identitätsvergleich auf der mutable Stack-Entity (kein equals).
    @SuppressWarnings("ReferenceEquality")
    static boolean isValidTarget(Stack caster, Stack target) {
        return target != caster
                && target.side() == caster.side()
                && target.isAlive()
                && target.unit().isResurrectable()
                && target.missingHealth() > 0;
    }

    /**
     * Solver-Heuristik: lohnt die Resurrection jetzt? Ja, sobald ein verbündeter Stack mindestens
     * so viele Lebenspunkte verloren hat, wie der Zauber zurückbringt — dann verfällt nichts.
     * Unter mehreren Kandidaten der mit dem größten Verlust.
     *
     * @return das Ziel, oder {@code null}, wenn der Arch Angel besser kämpft
     */
    static @Nullable Stack worthwhileTarget(Stack caster, @Nullable BattleSetup setup) {
        if (setup == null || !caster.canResurrect()) {
            return null;
        }
        int amount = healthFor(caster);
        return setup.stacksOf(caster.side()).stream()
                .filter(target -> isValidTarget(caster, target))
                .filter(target -> target.missingHealth() >= amount)
                .max(Comparator.comparingInt(Stack::missingHealth).thenComparingInt(s -> -s.slot()))
                .orElse(null);
    }
}
