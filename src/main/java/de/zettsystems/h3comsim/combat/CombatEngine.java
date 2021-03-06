package de.zettsystems.h3comsim.combat;

import de.zettsystems.h3comsim.arena.Arena;
import de.zettsystems.h3comsim.unit.common.Stack;
import de.zettsystems.h3comsim.unit.common.UnitSpeciality;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ThreadLocalRandom;

public class CombatEngine {
    public static void solveCombat(Arena arena) {
        System.out.println("Heute ein Kampf zwischen Stack von " + arena.getAttackerName()
                + " mit " + arena.getAttackerCount() + " Einheiten und Stack von " + arena.getDefenderName() + " mit " + arena.getDefenderCount() + " Einheiten!");
        System.out.println("---------------------------------------------------------------------------------------------------------------------");
        Queue<Stack> queue = determineMoveOrder(arena.getAttacker(), arena.getDefender());

        while (arena.isAttackerAlive() && arena.isDefenderAlive()) {
            Stack currentAttacker = queue.poll();
            Stack currentDefender = queue.poll();
            queue.offer(currentDefender);
            queue.offer(currentAttacker);
            attack(currentAttacker, currentDefender, false);
            //check whether counterattack takes place
            if (retaliationPossible(currentAttacker) && arena.bothAlive() && !currentAttacker.isPetrified()) {
                attack(currentDefender, currentAttacker, true);
            } else if (!retaliationPossible(currentAttacker)) {
                System.out.println(currentAttacker.getName() + " ist immun gegen Rueckschlag.");
            }
            System.out.println("---------");
            queue.stream().forEach(u -> u.endTurn());
        }
        System.out.println("-----------------------------------------------------------------------");

        queue.stream().filter(u -> !u.isAlive()).forEach(u -> System.out.println("Stack von " + u.getName() + " ist gestorben."));
    }

    private static boolean retaliationPossible(Stack currentAttacker) {
        return !currentAttacker.hasSpeciality(UnitSpeciality.NO_RETALIATION);
    }

    private static void attack(Stack currentAttacker, Stack currentDefender, boolean counterattack) {
        if (currentAttacker.isPetrified()) {
            System.out.println("Stack von " + currentAttacker.getName() + " ist versteinert und macht nichts.");
        } else {
            int currentDamage = currentAttacker.calculateCurrentDamage();
            int boniMaliPercentage = currentAttacker.calculateAttackBoniMaliPercentage(currentDefender.getDefense());
            int realDamage = (currentDamage * (100 + boniMaliPercentage)) / 100;
            if (counterattack) {
                System.out.println("Stack von " + currentAttacker.getName() + " schlaegt zurueck.");
            } else {
                System.out.println("Stack von " + currentAttacker.getName() + " greift Stack von " + currentDefender.getName() + " an.");
            }
            currentDefender.retrieveDamage(realDamage);
            if (currentDefender.isAlive()) {
                System.out.println("Oberste Einheit vom Stack von " + currentDefender.getName() + " hat noch " + currentDefender.getCurrentHealth() + " Gesundheit.");
            } else {
                System.out.println("Letzte Einheit vom Stack von " + currentDefender.getName() + " wurde getoetet.");
            }

            if (!counterattack) {
                doDeathStare(currentAttacker, currentDefender);
                doThunderbolts(currentAttacker, currentDefender);
                doPetryfing(currentAttacker, currentDefender);
            }
            doCursing(currentAttacker, currentDefender);
        }
    }

    private static void doDeathStare(Stack currentAttacker, Stack currentDefender) {
        if (currentAttacker.hasSpeciality(UnitSpeciality.DEATH_STARE)) {
            int value = ThreadLocalRandom.current().nextInt(1, 101);
            if (value <= 10) {
                currentDefender.retrieveDamageToDeath();
                System.out.println("Stack von " + currentAttacker.getName() + " toetet 1 Einheit vom Stack von " + currentDefender.getName() + " durch Death Stare.");
            }
        }
    }

    private static void doThunderbolts(Stack currentAttacker, Stack currentDefender) {
        if (currentAttacker.hasSpeciality(UnitSpeciality.THUNDERBOLTS)) {
            int value = ThreadLocalRandom.current().nextInt(1, 101);
            if (value <= 20) {
                currentDefender.retrieveDamage(10);
                System.out.println("Stack von " + currentAttacker.getName() + " fuegt zusaetzlich 10 Schaden durch Thunderbolts zu. Oberste Einheit vom Stack von "
                        + currentDefender.getName() + " hat noch " + currentDefender.getCurrentHealth() + " Gesundheit.");
            }
        }
    }

    private static void doPetryfing(Stack currentAttacker, Stack currentDefender) {
        if (currentAttacker.hasSpeciality(UnitSpeciality.PETRYFYING)) {
            int value = ThreadLocalRandom.current().nextInt(1, 101);
            if (value <= 20) {
                currentDefender.petrify();
                System.out.println("Stack von " + currentAttacker.getName() + " versteinert Stack von " + currentDefender.getName() + ".");
            }
        }
    }

    private static void doCursing(Stack currentAttacker, Stack currentDefender) {
        if (currentAttacker.hasSpeciality(UnitSpeciality.CURSING)) {
            int value = ThreadLocalRandom.current().nextInt(1, 101);
            if (value <= 20) {
                currentDefender.curse();
                System.out.println("Stack von " + currentAttacker.getName() + " verflucht Stack von " + currentDefender.getName() + ".");
            }
        }
    }

    private static Queue<Stack> determineMoveOrder(Stack attacker, Stack defender) {
        Queue<Stack> units = new LinkedList<>();
        if (attacker.getSpeed() >= defender.getSpeed()) {
            units.offer(attacker);
            units.offer(defender);
        } else {
            units.offer(defender);
            units.offer(attacker);
        }
        return units;
    }
}
