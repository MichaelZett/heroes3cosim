package de.zettsystems.h3comsim.application;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class BattleLogger {
    private BattleLogger() {
    }

    static void logStartOfCombat(String attackerName, int attackerCount, String defenderName, int defenderCount) {
        LOG.info("Heute ein Kampf zwischen Stack von {} mit {} Einheiten und Stack von {}  mit {}  Einheiten!",
                attackerName, attackerCount, defenderName, defenderCount);
        LOG.info("---------------------------------------------------------------------------------------------------------------------");
    }

    static void logImmuneToRetaliation(String name) {
        LOG.info("{} ist immun gegen Rueckschlag.", name);
    }

    static void logDeath(String name) {
        LOG.info("Stack von {} ist gestorben.", name);
    }

    static void logLastUnitDead(String name) {
        LOG.info("Letzte Einheit vom Stack von {} wurde getoetet.", name);
    }

    static void logRemainingHealth(String name, int currentHealth) {
        LOG.info("Oberste Einheit vom Stack von {} hat noch {} Gesundheit.", name, currentHealth);
    }

    static void logAttack(String attackerName, String defenderName) {
        LOG.info("Stack von {} greift Stack von {} an.", attackerName, defenderName);
    }

    static void logShoot(String attackerName, String defenderName, int distance) {
        LOG.info("Stack von {} schiesst auf Stack von {} (Distanz {}).", attackerName, defenderName, distance);
    }

    static void logMove(String name, int fromQ, int fromR, int toQ, int toR) {
        LOG.info("Stack von {} bewegt sich von ({},{}) nach ({},{}).", name, fromQ, fromR, toQ, toR);
    }

    static void logWait(String name) {
        LOG.info("Stack von {} wartet.", name);
    }

    static void logRetaliation(String name) {
        LOG.info("Stack von {} schlaegt zurueck.", name);
    }

    static void logDeathStare(String attackerName, String defenderName, int kills) {
        LOG.info("Stack von {} toetet {} Einheit(en) vom Stack von {} durch Death Stare.",
                attackerName, kills, defenderName);
    }

    static void logCurse(String currentAttacker, String currentDefender) {
        LOG.info("Stack von {} verflucht Stack von {}.", currentAttacker, currentDefender);
    }

    static void logPetrifying(String attackerName, String defenderName) {
        LOG.info("Stack von {} versteinert Stack von {}.", attackerName, defenderName);
    }

    static void logThunderbolting(String attackerName, String defenderName, int damage, int currentDefenderCurrentHealth) {
        LOG.info("Stack von {} fuegt zusaetzlich {} Schaden durch Thunderbolts zu. " +
                        "Oberste Einheit vom Stack von {} hat noch {} Gesundheit.",
                attackerName, damage, defenderName, currentDefenderCurrentHealth);
    }

    static void logPoisoning(String attackerName, String defenderName) {
        LOG.info("Stack von {} vergiftet Stack von {}.", attackerName, defenderName);
    }

    static void logTwoShots(String name) {
        LOG.info("Stack von {} schiesst ein zweites Mal.", name);
    }

    static void logShortDelimiter() {
        LOG.info("---------");
    }

    static void logMiddleDelimiter() {
        LOG.info("-----------------------------------------------------------------------");
    }

    static void logTwoBlows(String name) {
        LOG.info("Stack von {} greift mit seinem 2.Schlag erneut an.", name);
    }

    static void logGoodMorale(String name) {
        LOG.info("Stack von {} hat gute Moral und greift erneut an.", name);
    }

    static void logMoveBack(String name, int q, int r) {
        LOG.info("Stack von {} fliegt zurueck nach ({},{}).", name, q, r);
    }
}
