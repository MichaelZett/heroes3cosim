package de.zettsystems.h3comsim.combat;

import de.zettsystems.h3comsim.arena.Arena;
import de.zettsystems.h3comsim.unit.common.Unit;
import de.zettsystems.h3comsim.unit.common.UnitSpeciality;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ThreadLocalRandom;

public class CombatEngine {
    public static void solveCombat(Arena arena) {
        System.out.println("Heute ein Kampf zwischen " + arena.getAttacker().getName()
                + " und " + arena.getDefender().getName() + "!");
        System.out.println("------------------------------------------------------------------------");
        Queue<Unit> queue = determineMoveOrder(arena.getAttacker(), arena.getDefender());

        while (arena.getAttacker().getCurrentHealth() > 0 && arena.getDefender().getCurrentHealth() > 0) {
            Unit currentAttacker = queue.poll();
            Unit currentDefender = queue.poll();
            queue.offer(currentDefender);
            queue.offer(currentAttacker);
            attack(currentAttacker, currentDefender, false);
            //check whether counterattack takes place
            if (retaliationPossible(currentAttacker) && bothAlive(arena.getAttacker(), arena.getDefender()) && !currentAttacker.isPetrified()) {
                attack(currentDefender, currentAttacker, true);
            } else if (!retaliationPossible(currentAttacker)) {
                System.out.println(currentAttacker.getName() + " ist immun gegen Rueckschlag.");
            }
            System.out.println("---------");
            queue.stream().forEach(u -> u.endTurn());
        }
        System.out.println("-----------------------------------------------------------------------");

        queue.stream().filter(u -> u.getCurrentHealth() <= 0).forEach(u -> System.out.println(u.getName() + " ist gestorben."));
    }

    private static boolean retaliationPossible(Unit currentAttacker) {
        return !currentAttacker.hasSpeciality(UnitSpeciality.NO_RETALIATION);
    }

    private static boolean bothAlive(Unit attacker, Unit defender) {
        return attacker.getCurrentHealth() > 0 && defender.getCurrentHealth() > 0;
    }

    private static void attack(Unit currentAttacker, Unit currentDefender, boolean counterattack) {
        if (currentAttacker.isPetrified()) {
            System.out.println(currentAttacker.getName() + " ist versteinert und macht nichts.");
        } else {
            int currentDamage = currentAttacker.calculateCurrentDamage();
            int boniMaliPercentage = currentAttacker.calculateAttackBoniMaliPercentage(currentDefender.getDefense());
            int realDamage = (currentDamage * (100 + boniMaliPercentage)) / 100;
            if (counterattack) {
                System.out.println(currentAttacker.getName() + " schlaegt zurueck.");
            } else {
                System.out.println(currentAttacker.getName() + " greift " + currentDefender.getName() + " an.");
            }
            currentDefender.retrieveDamage(realDamage);
            System.out.println(currentDefender.getName() + " hat noch " + currentDefender.getCurrentHealth() + " Gesundheit.");

            if (!counterattack) {
                doDeathStare(currentAttacker, currentDefender);
                doThunderbolts(currentAttacker, currentDefender);
                doPetryfing(currentAttacker, currentDefender);
            }
            doCursing(currentAttacker, currentDefender);
        }
    }

    private static void doDeathStare(Unit currentAttacker, Unit currentDefender) {
        if (currentAttacker.hasSpeciality(UnitSpeciality.DEATH_STARE)) {
            int value = ThreadLocalRandom.current().nextInt(1, 101);
            if (value <= 10) {
                currentDefender.retrieveDamageToDeath();
                System.out.println(currentAttacker.getName() + " toetet 1 " + currentDefender.getName() + " durch Death Stare.");
            }
        }
    }

    private static void doThunderbolts(Unit currentAttacker, Unit currentDefender) {
        if (currentAttacker.hasSpeciality(UnitSpeciality.THUNDERBOLTS)) {
            int value = ThreadLocalRandom.current().nextInt(1, 101);
            if (value <= 20) {
                currentDefender.retrieveDamage(10);
                System.out.println(currentAttacker.getName() + " fuegt zusaetzlich 10 Schaden durch Thunderbolts zu. "
                        + currentDefender.getName() + " hat noch " + currentDefender.getCurrentHealth() + " Gesundheit.");
            }
        }
    }

    private static void doPetryfing(Unit currentAttacker, Unit currentDefender) {
        if (currentAttacker.hasSpeciality(UnitSpeciality.PETRYFYING)) {
            int value = ThreadLocalRandom.current().nextInt(1, 101);
            if (value <= 20) {
                currentDefender.petrify();
                System.out.println(currentAttacker.getName() + " versteinert " + currentDefender.getName() + ".");
            }
        }
    }

    private static void doCursing(Unit currentAttacker, Unit currentDefender) {
        if (currentAttacker.hasSpeciality(UnitSpeciality.CURSING)) {
            int value = ThreadLocalRandom.current().nextInt(1, 101);
            if (value <= 20) {
                currentDefender.curse();
                System.out.println(currentAttacker.getName() + " verflucht " + currentDefender.getName() + ".");
            }
        }
    }

    private static Queue<Unit> determineMoveOrder(Unit attacker, Unit defender) {
        Queue<Unit> units = new LinkedList<>();
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
