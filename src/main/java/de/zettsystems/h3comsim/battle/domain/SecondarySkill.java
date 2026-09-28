package de.zettsystems.h3comsim.battle.domain;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Sekundärfertigkeiten eines Helden (Manual S. 35-40). Von den 28 des Originals sind hier die
 * acht aufgenommen, die für den Kampf zählen oder die ein Katalog-Held mitbringt.
 *
 * <p>Vier davon wirken, vier liegen als reine Daten im {@link HeroCatalog}:
 * <ul>
 *   <li>{@link #OFFENSE} (+10/20/30 % Nahkampfschaden), {@link #ARCHERY} (+10/25/50 %
 *       Fernkampfschaden) und {@link #ARMORER} (−5/10/15 % erlittener Schaden) greifen in
 *       {@code Battle.dealDamage}.</li>
 *   <li>{@link #LEADERSHIP} (+1/2/3 Moral) geht in das Armee-Rating ein, siehe
 *       {@link ArmyMorale}.</li>
 *   <li>{@link #TACTICS} griffe in die Aufstellung vor dem Kampf ({@code SpawnLayout}) — kein
 *       Katalog-Held hat die Fertigkeit, die Auswertung wäre heute wirkungslos.</li>
 *   <li>{@link #NECROMANCY} wirkt nach der Schlacht, nicht in ihr; {@link #SCHOLAR} und
 *       {@link #MYSTICISM} wirken außerhalb des Kampfes und bleiben hier dauerhaft folgenlos.</li>
 * </ul>
 *
 * <p>Nicht aufgenommen ist <strong>Luck</strong> (+1/2/3, Manual S. 37): in RoE bringen sie nur
 * Melodia und Ufretin mit, beide Rampart — und Rampart ist im Katalog von Jenova besetzt, der
 * einzigen Trägerin von Archery. Die Fertigkeit setzt einen zweiten Helden je Fraktion voraus.
 */
@Schema(description = "Sekundärfertigkeit eines Helden. Offense, Archery, Armorer und Leadership wirken; die übrigen werden nur geführt.",
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
