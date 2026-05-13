package de.zettsystems.h3comsim.battle.domain;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class BattleLogger {
    private BattleLogger() {
    }

    static void logStartOfCombat(String attackerName, int attackerCount, String defenderName, int defenderCount) {
        LOG.debug("Heute ein Kampf zwischen Stack von {} mit {} Einheiten und Stack von {}  mit {}  Einheiten!",
                attackerName, attackerCount, defenderName, defenderCount);
        LOG.debug("---------------------------------------------------------------------------------------------------------------------");
    }

    static void logImmuneToRetaliation(String name) {
        LOG.debug("{} ist immun gegen Rueckschlag.", name);
    }

    static void logDeath(String name) {
        LOG.debug("Stack von {} ist gestorben.", name);
    }

    static void logLastUnitDead(String name) {
        LOG.debug("Letzte Einheit vom Stack von {} wurde getoetet.", name);
    }

    static void logRemainingHealth(String name, int currentHealth) {
        LOG.debug("Oberste Einheit vom Stack von {} hat noch {} Gesundheit.", name, currentHealth);
    }

    static void logAttack(String attackerName, String defenderName) {
        LOG.debug("Stack von {} greift Stack von {} an.", attackerName, defenderName);
    }

    static void logShoot(String attackerName, String defenderName, int distance) {
        LOG.debug("Stack von {} schiesst auf Stack von {} (Distanz {}).", attackerName, defenderName, distance);
    }

    static void logMove(String name, int fromQ, int fromR, int toQ, int toR) {
        LOG.debug("Stack von {} bewegt sich von ({},{}) nach ({},{}).", name, fromQ, fromR, toQ, toR);
    }

    static void logWait(String name) {
        LOG.debug("Stack von {} wartet.", name);
    }

    static void logRetaliation(String name) {
        LOG.debug("Stack von {} schlaegt zurueck.", name);
    }

    static void logDeathStare(String attackerName, String defenderName, int kills) {
        LOG.debug("Stack von {} toetet {} Einheit(en) vom Stack von {} durch Death Stare.",
                attackerName, kills, defenderName);
    }

    static void logCurse(String currentAttacker, String currentDefender) {
        LOG.debug("Stack von {} verflucht Stack von {}.", currentAttacker, currentDefender);
    }

    static void logPetrifying(String attackerName, String defenderName) {
        LOG.debug("Stack von {} versteinert Stack von {}.", attackerName, defenderName);
    }

    static void logThunderbolting(String attackerName, String defenderName, int damage, int currentDefenderCurrentHealth) {
        LOG.debug("Stack von {} fuegt zusaetzlich {} Schaden durch Thunderbolts zu. " +
                        "Oberste Einheit vom Stack von {} hat noch {} Gesundheit.",
                attackerName, damage, defenderName, currentDefenderCurrentHealth);
    }

    static void logPoisoning(String attackerName, String defenderName) {
        LOG.debug("Stack von {} vergiftet Stack von {}.", attackerName, defenderName);
    }

    static void logTwoShots(String name) {
        LOG.debug("Stack von {} schiesst ein zweites Mal.", name);
    }

    static void logShortDelimiter() {
        LOG.debug("---------");
    }

    static void logMiddleDelimiter() {
        LOG.debug("-----------------------------------------------------------------------");
    }

    static void logTwoBlows(String name) {
        LOG.debug("Stack von {} greift mit seinem 2.Schlag erneut an.", name);
    }

    static void logGoodMorale(String name) {
        LOG.debug("Stack von {} hat gute Moral und greift erneut an.", name);
    }

    static void logMoveBack(String name, int q, int r) {
        LOG.debug("Stack von {} fliegt zurueck nach ({},{}).", name, q, r);
    }

    static void logDiseasing(String attackerName, String defenderName) {
        LOG.debug("Stack von {} infiziert Stack von {} mit Krankheit.", attackerName, defenderName);
    }

    static void logAging(String attackerName, String defenderName) {
        LOG.debug("Stack von {} laesst Stack von {} altern.", attackerName, defenderName);
    }

    static void logFireShield(String shieldedName, String attackerName, int reverseDamage) {
        LOG.debug("Stack von {} reflektiert {} Schaden durch Feuerschild auf Stack von {}.",
                shieldedName, reverseDamage, attackerName);
    }

    static void logRebirth(String name, int restoredCount) {
        LOG.debug("Stack von {} wird durch Wiedergeburt mit {} Einheiten zurueckgebracht.",
                name, restoredCount);
    }

    static void logDistancePenalty(String name, int distance) {
        LOG.debug("Stack von {} hat Distanz-Penalty ({} Hex) und halbiert den Schaden.", name, distance);
    }

    static void logObstaclePenalty(String name) {
        LOG.debug("Stack von {} hat ein Hindernis in der Schusslinie und halbiert den Schaden.", name);
    }
}
