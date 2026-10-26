package de.zettsystems.h3comsim.battle.domain;

import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.random.RandomGenerator;

/**
 * Zauber auf der Ballista (heroes.thelazy.net, <em>War Machine</em>): Bless und Curse setzen ihren
 * Grundschaden (2–3) auf das Maximum bzw. Minimum, Precision und Prayer heben ihre Attack wie bei
 * einem Schützen. Glückszauber und Slayer wirken laut Liste auch, sind hier nicht abgebildet.
 *
 * <p>In der Zauberwahl zählt ein solcher Zauber, was er am Schaden der Ballista ändert, nicht ihren
 * Goldwert: erwarteter Schaden je Runde mal Goldwert je Lebenspunkt der Gegner, über die
 * erwarteten Runden.
 */
final class BallistaSpells {

    /** Die Zauber, die die Wahl auf die eigene (Bless, Precision, Prayer) bzw. gegnerische Ballista (Curse) legt. */
    static final Set<Spell> SPELLS = Set.of(Spell.BLESS, Spell.CURSE, Spell.PRECISION, Spell.PRAYER);

    private static final double AVERAGE_ROLL = 2.5;

    private BallistaSpells() {
    }

    /** Der Grundschaden eines Schusses (2–3), mit Bless oder Curse auf der Ballista. */
    static int roll(@Nullable Stack ballista, RandomGenerator rng) {
        Unit unit = WarMachine.BALLISTA.unit();
        ActiveSpell curse = ballista == null ? null : ballista.activeSpell(Spell.CURSE);
        ActiveSpell bless = ballista == null ? null : ballista.activeSpell(Spell.BLESS);
        if (curse != null) {
            // Wie bei Kreaturen (Manual S. 72): Mindestschaden, ab Advanced 80 % davon minus 1.
            return curse.level() == SkillLevel.BASIC ? unit.minDamage() : Math.max(1, unit.minDamage() * 80 / 100 - 1);
        }
        if (bless != null) {
            return (unit.maxDamage() + bless.byLevel(0, 1, 1)) * (100 + bless.amount()) / 100;
        }
        return rng.nextInt(unit.minDamage(), unit.maxDamage() + 1);
    }

    /** Zusätzliche Attack durch Precision (+3/6) und Prayer (+2/4). */
    static int attackBonus(@Nullable Stack ballista) {
        if (ballista == null) {
            return 0;
        }
        return points(ballista.activeSpell(Spell.PRECISION), 3, 6) + points(ballista.activeSpell(Spell.PRAYER), 2, 4);
    }

    private static int points(@Nullable ActiveSpell active, int basic, int higher) {
        return active == null ? 0 : active.byLevel(basic, higher, higher) + active.amount();
    }

    /** Um wie viel Prozent der Zauber den Schaden der Ballista ändert (Curse als Verlust positiv). */
    static double gainPercent(Spell spell, SkillLevel level, int bonus) {
        Unit unit = WarMachine.BALLISTA.unit();
        return switch (spell) {
            case BLESS -> 100.0 * ((unit.maxDamage() + SpellDamage.byLevel(level, 0, 1, 1)) * (100 + bonus) / 100.0
                    - AVERAGE_ROLL) / AVERAGE_ROLL;
            case CURSE -> 100.0 * (AVERAGE_ROLL - (level == SkillLevel.BASIC ? unit.minDamage()
                    : Math.max(1, unit.minDamage() * 80 / 100 - 1))) / AVERAGE_ROLL;
            case PRECISION -> 5.0 * (SpellDamage.byLevel(level, 3, 6, 6) + bonus);
            case PRAYER -> 5.0 * (SpellDamage.byLevel(level, 2, 4, 4) + bonus);
            default -> 0;
        };
    }

    /**
     * Was die Ballista einer Seite je Runde an Goldwert vernichtet: erwarteter Schaden je Schuss
     * (Grundschaden × (Attack des Helden + 1), Attack gegen die mittlere Defense der Gegner) mal
     * Schüsse mal mittlerer Goldwert je Lebenspunkt der Gegner.
     */
    static double roundValue(Stack ballista, BattleSetup setup) {
        Hero hero = setup.heroOf(ballista.side());
        int heroAttack = hero == null ? 0 : hero.attack();
        int attack = hero == null ? WarMachine.BALLISTA.attack() : hero.ballistaAttack();
        int shots = hero == null ? 1 : hero.ballistaShots();
        double health = 0;
        double worth = 0;
        double defense = 0;
        for (Stack enemy : setup.stacksOf(HeroSpellcasting.other(ballista.side()))) {
            if (enemy.isAlive()) {
                int hp = HeroSpellcasting.totalHealth(enemy);
                health += hp;
                worth += HeroSpellcasting.stackValue(enemy);
                defense += (double) enemy.getDefense() * hp;
            }
        }
        if (health == 0) {
            return 0;
        }
        int meanDefense = (int) Math.round(defense / health);
        double damage = AVERAGE_ROLL * (heroAttack + 1) * (100 + Stack.boniMaliPercentage(attack, meanDefense)) / 100.0;
        return damage * shots * worth / health;
    }
}
