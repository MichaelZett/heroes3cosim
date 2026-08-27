package de.zettsystems.h3comsim.battle.domain;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Sekundärfertigkeiten eines Helden (Manual S. 35-40). Von den 28 des Originals sind hier die
 * acht aufgenommen, die für den Kampf zählen oder die ein Katalog-Held mitbringt.
 *
 * <p><strong>Keine davon wird heute ausgewertet.</strong> Sie liegen als Daten im
 * {@link HeroCatalog}, damit der Katalog beim Aktivieren nicht erneut angefasst werden muss.
 * Welche Fertigkeit wo eingreift:
 * <ul>
 *   <li>{@link #OFFENSE} (+10/20/30 % Nahkampfschaden), {@link #ARCHERY} (+10/25/50 %
 *       Fernkampfschaden) und {@link #ARMORER} (−5/10/15 % erlittener Schaden) greifen in
 *       {@code Battle.dealDamage}.</li>
 *   <li>{@link #TACTICS} greift in die Aufstellung vor dem Kampf ({@code SpawnLayout}).</li>
 *   <li>{@link #LEADERSHIP} (+1/2/3 Moral) braucht erst ein Armee-Moralsystem — heute liefert
 *       {@code Unit.morale()} nur 0 oder 1 aus {@code GOOD_MORALE}.</li>
 *   <li>{@link #NECROMANCY} wirkt nach der Schlacht, nicht in ihr; {@link #SCHOLAR} und
 *       {@link #MYSTICISM} wirken außerhalb des Kampfes und bleiben hier dauerhaft folgenlos.</li>
 * </ul>
 */
@Schema(description = "Sekundärfertigkeit eines Helden. Wird derzeit nur geführt, nicht ausgewertet.",
        enumAsRef = true)
public enum SecondarySkill {
    OFFENSE,
    ARCHERY,
    ARMORER,
    TACTICS,
    LEADERSHIP,
    NECROMANCY,
    SCHOLAR,
    MYSTICISM
}
