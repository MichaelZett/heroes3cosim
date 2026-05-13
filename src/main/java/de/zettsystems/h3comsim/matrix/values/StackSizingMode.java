package de.zettsystems.h3comsim.matrix.values;

/**
 * Wie wird die Stack-Größe pro Seite im Matrix-Match bestimmt?
 *
 * <ul>
 *   <li>{@link #EQUAL_COUNT}: beide Seiten bekommen {@code unitCount} Einheiten (Default-Modus).</li>
 *   <li>{@link #EQUAL_GOLD}: Pair-Budget = {@code max(costA, costB) * unitCount}, auf nächstes
 *       LCM-Vielfaches abgerundet → exakt gleicher Gold-Wert je Seite.</li>
 *   <li>{@link #WEEKLY_PRODUCTION}: jede Seite bekommt ihre H3-Wochenproduktion × {@code unitCount}.
 *       Bei {@code unitCount=1} entspricht das exakt einer Stadt-Woche; bei 20 entsprechend 20
 *       Wochen. So spiegeln sich die natürlichen Tier-Verhältnisse innerhalb einer Faktion wider
 *       (z. B. 14 Pikemen vs 4 Swordsmen pro Woche).</li>
 *   <li>{@link #EQUAL_GOLD_WEEKLY}: Gold-normalisierte Wochenproduktion. Pair-Budget =
 *       {@code max(wpA*costA, wpB*costB) * unitCount}, LCM-snap. Spiegelt „bei gleichem
 *       Wochen-Income, wer holt mehr aus diesem Gold-Stack raus".</li>
 * </ul>
 */
public enum StackSizingMode {
    EQUAL_COUNT,
    EQUAL_GOLD,
    WEEKLY_PRODUCTION,
    EQUAL_GOLD_WEEKLY
}
