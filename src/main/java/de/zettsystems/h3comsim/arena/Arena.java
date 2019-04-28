package de.zettsystems.h3comsim.arena;

import de.zettsystems.h3comsim.unit.common.Unit;
import de.zettsystems.h3comsim.unit.common.UnitSpeciality;

import java.util.LinkedList;
import java.util.Queue;

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
        Queue<Unit> queue = determineMoveOrder();

        while (attacker.getCurrentHealth() > 0 && defender.getCurrentHealth() > 0) {
            Unit currentAttacker = queue.poll();
            Unit currentDefender = queue.poll();
            queue.offer(currentDefender);
            queue.offer(currentAttacker);
            attack(currentAttacker, currentDefender, false);
            //check whether counterattck takes place
            if (retaliationPossible(currentAttacker) && bothAlive()) {
                attack(currentDefender, currentAttacker, true);
            } else if (!retaliationPossible(currentAttacker)) {
                System.out.println(currentAttacker.getName()+ " ist immun gegen Rueckschlag.");
            }
        }

        queue.stream().filter(u -> u.getCurrentHealth() < 0).forEach(u -> System.out.println(u.getName() + " ist gestorben."));
    }

    private boolean retaliationPossible(Unit currentAttacker) {
        return !currentAttacker.hasSpeciality(UnitSpeciality.NO_RETALIATION);
    }

    private boolean bothAlive() {
        return attacker.getCurrentHealth() > 0 && defender.getCurrentHealth() > 0;
    }

    private void attack(Unit currentAttacker, Unit currentDefender, boolean counterattack) {
        int currentDamage = currentAttacker.calculateCurrentDamage();
        int boniMaliPercentage = currentAttacker.calculateAttackBoniMaliPercentage(currentDefender.getDefense());
        int realDamage = (currentDamage * (100 - boniMaliPercentage)) / 100;
        currentDefender.retrieveDamage(realDamage);
        if (counterattack) {
            System.out.println(currentAttacker.getName() + " schlaegt zurueck.");
        } else {
            System.out.println(currentAttacker.getName() + " greift " + currentDefender.getName() + " an.");
        }
        System.out.println("Er fuegt " + realDamage + " Schaden zu. " + currentDefender.getName()
                + " hat noch " + currentDefender.getCurrentHealth() + " Gesundheit.");
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
