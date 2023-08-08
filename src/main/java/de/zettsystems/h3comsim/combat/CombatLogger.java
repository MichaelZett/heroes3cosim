package de.zettsystems.h3comsim.combat;

import de.zettsystems.h3comsim.unit.common.Stack;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class CombatLogger {
    private CombatLogger() {
        //not intended
    }

    static void logStartOfCombat(String attackerName, int attackerCount, String defenderName, int defenderCount) {
        LOG.info("Heute ein Kampf zwischen Stack von {} mit {} Einheiten und Stack von {}  mit {}  Einheiten!"
                , attackerName, attackerCount, defenderName, defenderCount);

        LOG.info("---------------------------------------------------------------------------------------------------------------------");
    }

    static void logImmuneToRetaliation(String name) {
        LOG.info("{} ist immun gegen Rueckschlag.", name);
    }

    static void logDoNotRetaliateAgainstPetrified(Stack currentDefender) {
        LOG.info("{} greift versteinerten Gegner nicht an.", currentDefender);
    }
    static void logDeath(String name) {
        LOG.info("Stack von {} ist gestorben.", name);
    }

    static void logLastUnitDead(String name) {
        LOG.info("Letzte Einheit vom Stack von " + name + " wurde getoetet.");
    }

    static void logRemainingHealth(String name, int currentHealth) {
        LOG.info("Oberste Einheit vom Stack von " + name + " hat noch " + currentHealth + " Gesundheit.");
    }

    static void logAttack(String attackerName, String defenderName) {
        LOG.info("Stack von {} greift Stack von {} an.", attackerName, defenderName);
    }

    static void logCounterAttack(String name) {
        LOG.info("Stack von {} schlaegt zurueck.", name);
    }

    static void logPetrified(String name) {
        LOG.info("Stack von {} ist versteinert und macht nichts.", name);
    }
    static void logDeathStare(String attackerName, String defenderName) {
        LOG.info("Stack von {} toetet 1 Einheit vom Stack von {} durch Death Stare.", attackerName, defenderName);
    }

    static void logCurse(String currentAttacker, String currentDefender) {
        LOG.info("Stack von {} verflucht Stack von {}.", currentAttacker, currentDefender);
    }
    static void logPetrifying(String attackerName, String defenderName) {
        LOG.info("Stack von {} versteinert Stack von {}.", attackerName, defenderName);
    }
    static void logThunderbolting(String attackerName, String defenderName, int currentDefenderCurrentHealth) {
        LOG.info("Stack von {} fuegt zusaetzlich 10 Schaden durch Thunderbolts zu. " +
                "Oberste Einheit vom Stack von {} hat noch {} Gesundheit.", attackerName, defenderName, currentDefenderCurrentHealth);
    }

    static void logShortDelimiter() {
        LOG.info("---------");
    }

    static void logMiddleDelimiter() {
        LOG.info("-----------------------------------------------------------------------");
    }

}
