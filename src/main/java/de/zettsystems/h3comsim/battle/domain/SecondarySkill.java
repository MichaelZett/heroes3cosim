package de.zettsystems.h3comsim.battle.domain;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Die 28 Sekundärfertigkeiten (Manual S. 35-40, alphabetisch wie dort).
 *
 * <p>Vier davon wirken in der Simulation:
 * <ul>
 *   <li>{@link #OFFENSE} (+10/20/30 % Nahkampfschaden), {@link #ARCHERY} (+10/25/50 %
 *       Fernkampfschaden) und {@link #ARMORER} (−5/10/15 % erlittener Schaden) greifen in
 *       {@code Battle.dealDamage}.</li>
 *   <li>{@link #LEADERSHIP} (+1/2/3 Moral) geht in das Armee-Rating ein, siehe
 *       {@link ArmyMorale}.</li>
 * </ul>
 *
 * <p>Die übrigen werden nur geführt, damit jeder Held aus dem {@link HeroCatalog} mit seinen
 * echten Startfertigkeiten dasteht. Die meisten wirken außerhalb des Kampfes (Abenteuerkarte,
 * Necromancy nach der Schlacht) oder setzen ein Zaubersystem voraus (Wisdom, die vier
 * Magie-Schulen, Sorcery, Intelligence, Mysticism, Eagle Eye, Resistance). Artillery, Ballistics
 * und First Aid brauchen Kriegsmaschinen, {@link #TACTICS} eine freie Aufstellung vor dem Kampf;
 * beides gibt es hier nicht. Offen ist {@link #LUCK}.
 */
@Schema(description = "Sekundärfertigkeit eines Helden. Offense, Archery, Armorer und Leadership wirken; die übrigen werden nur geführt.",
        enumAsRef = true)
public enum SecondarySkill {
    AIR_MAGIC,
    ARCHERY,
    ARMORER,
    ARTILLERY,
    BALLISTICS,
    DIPLOMACY,
    EAGLE_EYE,
    EARTH_MAGIC,
    ESTATES,
    FIRE_MAGIC,
    FIRST_AID,
    INTELLIGENCE,
    LEADERSHIP,
    LEARNING,
    LOGISTICS,
    LUCK,
    MYSTICISM,
    NAVIGATION,
    NECROMANCY,
    OFFENSE,
    PATHFINDING,
    RESISTANCE,
    SCHOLAR,
    SCOUTING,
    SORCERY,
    TACTICS,
    WATER_MAGIC,
    WISDOM
}
