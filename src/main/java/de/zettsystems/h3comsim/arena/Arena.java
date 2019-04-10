package de.zettsystems.h3comsim.arena;

import de.zettsystems.h3comsim.unit.Unit;

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

        while (attacker.getHealth() > 0 && defender.getHealth() > 0) {
            Unit currentAttacker = queue.poll();
            Unit currentDefender = queue.poll();
            queue.offer(currentDefender);
            queue.offer(currentAttacker);
            int currentDamage = currentAttacker.calculateCurrentDamage();
            int boniMaliPercentage = currentAttacker.calculateAttackBoniMaliPercentage(currentDefender.getDefense());
            int realDamage = (currentDamage * (100 - boniMaliPercentage)) / 100;
            currentDefender.retrieveDamage(realDamage);
            System.out.println(currentAttacker.getName() + " greift " + currentDefender.getName() + " an.");
            System.out.println("Er fuegt " + realDamage + " Schaden zu. " + currentDefender.getName()
                    + " hat noch " + currentDefender.getHealth() + " Gesundheit.");
        }

        queue.stream().filter(u -> u.getHealth() <0).forEach(u -> System.out.println(u.getName() + " ist gestorben."));
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
