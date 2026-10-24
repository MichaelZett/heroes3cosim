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
 * <p>Ziel ist ein verbündeter Stack mit Verlusten — auch ein ganz gefallener, sofern sein Feld frei
 * ist ({@link BattleSetup#hasFreeCorpse}). Der Arch Angel belebt nicht sich selbst.
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
    static boolean isValidTarget(Stack caster, Stack target, BattleSetup setup) {
        return target != caster
                && target.side() == caster.side()
                && target.unit().isResurrectable()
                && (target.isAlive() ? target.missingHealth() > 0 : setup.hasFreeCorpse(target));
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
                .filter(target -> isValidTarget(caster, target, setup))
                .filter(target -> target.missingHealth() >= amount)
                .max(Comparator.comparingInt(Stack::missingHealth).thenComparingInt(s -> -s.slot()))
                .orElse(null);
    }
}
