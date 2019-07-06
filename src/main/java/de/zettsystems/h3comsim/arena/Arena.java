package de.zettsystems.h3comsim.arena;

import de.zettsystems.h3comsim.unit.common.Unit;
import de.zettsystems.h3comsim.unit.common.UnitSpeciality;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ThreadLocalRandom;

public class Arena {
    private Unit attacker;
    private Unit defender;

    public Arena(Unit attacker, Unit defender) {
        this.attacker = attacker;
        this.defender = defender;
    }

    public void startCombat() {
        System.out.println("Heute ein Kampf zwischen " + attacker.getName()
                + " und " + defender.getName() + "!");
        System.out.println("------------------------------------------------------------------------");
        Queue<Unit> queue = determineMoveOrder();

        while (attacker.getCurrentHealth() > 0 && defender.getCurrentHealth() > 0) {
            Unit currentAttacker = queue.poll();
            Unit currentDefender = queue.poll();
            queue.offer(currentDefender);
            queue.offer(currentAttacker);
            attack(currentAttacker, currentDefender, false);
            //check whether counterattck takes place
            if (retaliationPossible(currentAttacker) && bothAlive() && !currentAttacker.isPetrified()) {
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

    private boolean retaliationPossible(Unit currentAttacker) {
        return !currentAttacker.hasSpeciality(UnitSpeciality.NO_RETALIATION);
    }

    private boolean bothAlive() {
        return attacker.getCurrentHealth() > 0 && defender.getCurrentHealth() > 0;
    }

    private void attack(Unit currentAttacker, Unit currentDefender, boolean counterattack) {
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
                if (currentAttacker.hasSpeciality(UnitSpeciality.DEATH_STARE)) {
                    int value = ThreadLocalRandom.current().nextInt(1, 101);
                    if (value <= 10) {
                        currentDefender.retrieveDamageToDeath();
                        System.out.println(currentAttacker.getName() + " toetet 1 " + currentDefender.getName() + " durch Death Stare.");
                    }
                }
                if (currentAttacker.hasSpeciality(UnitSpeciality.THUNDERBOLTS)) {
                    int value = ThreadLocalRandom.current().nextInt(1, 101);
                    if (value <= 20) {
                        currentDefender.retrieveDamage(10);
                        System.out.println(currentAttacker.getName() + " fuegt zusaetzlich 10 Schaden durch Thunderbolts zu. "
                                + currentDefender.getName() + " hat noch " + currentDefender.getCurrentHealth() + " Gesundheit.");
                    }
                }
                if (currentAttacker.hasSpeciality(UnitSpeciality.PETRYFYING)) {
                    int value = ThreadLocalRandom.current().nextInt(1, 101);
                    if (value <= 20) {
                        currentDefender.petrify();
                        System.out.println(currentAttacker.getName() + " versteinert " + currentDefender.getName() + ".");
                    }
                }
            }
            if (currentAttacker.hasSpeciality(UnitSpeciality.CURSING)) {
                int value = ThreadLocalRandom.current().nextInt(1, 101);
                if (value <= 20) {
                    currentDefender.curse();
                    System.out.println(currentAttacker.getName() + " verflucht " + currentDefender.getName() + ".");
                }
            }
        }
    }

    private Queue<Unit> determineMoveOrder() {
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
