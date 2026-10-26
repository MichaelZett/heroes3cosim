package de.zettsystems.h3comsim.battle.domain;

import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.random.RandomGenerator;

/**
 * Zauber auf der Ballista (heroes.thelazy.net, <em>War Machine</em>): Bless und Curse setzen ihren
 * Grundschaden (2–3) auf das Maximum bzw. Minimum, Precision und Prayer heben ihre Attack wie bei
 * einem Schützen, Slayer gegen seine Ziele um 8; Fortune und Misfortune ändern das Glück ihrer
 * Schüsse.
 *
 * <p>In der Zauberwahl zählt ein solcher Zauber, was er am Schaden der Ballista ändert, nicht ihren
 * Goldwert: erwarteter Schaden je Runde mal Goldwert je Lebenspunkt der Gegner, über die
 * erwarteten Runden.
 */
final class BallistaSpells {

    /**
     * Die Zauber, die die Wahl auf die eigene (Bless, Precision, Prayer, Slayer, Fortune) bzw.
     * gegnerische Ballista (Curse, Misfortune) legt.
     */
    static final Set<Spell> SPELLS = Set.of(Spell.BLESS, Spell.CURSE, Spell.PRECISION, Spell.PRAYER,
            Spell.SLAYER, Spell.FORTUNE, Spell.MISFORTUNE);

    private static final int SLAYER_ATTACK = 8;

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

    /** Zusätzliche Attack durch Precision (+3/6), Prayer (+2/4) und Slayer (+8 gegen seine Ziele). */
    static int attackBonus(@Nullable Stack ballista, Stack target) {
        if (ballista == null) {
            return 0;
        }
        int bonus = points(ballista.activeSpell(Spell.PRECISION), 3, 6) + points(ballista.activeSpell(Spell.PRAYER), 2, 4);
        ActiveSpell slayer = ballista.activeSpell(Spell.SLAYER);
        if (slayer != null && Stack.isSlayerTarget(target.unit(), slayer.level())) {
            bonus += SLAYER_ATTACK + slayer.amount();
        }
        return bonus;
    }

    /** Glück der Schüsse: das der Armee, mit Fortune und Misfortune auf der Ballista. */
    static int luck(@Nullable Stack ballista, int armyLuck) {
        return ballista == null ? armyLuck : ballista.getLuck();
    }

    private static int points(@Nullable ActiveSpell active, int basic, int higher) {
        return active == null ? 0 : active.byLevel(basic, higher, higher) + active.amount();
    }

    /** Um wie viel Prozent der Zauber den Schaden der Ballista ändert (Curse als Verlust positiv). */
    static double gainPercent(Spell spell, SkillLevel level, int bonus, Stack ballista, BattleSetup setup) {
        Unit unit = WarMachine.BALLISTA.unit();
        return switch (spell) {
            case BLESS -> 100.0 * ((unit.maxDamage() + SpellDamage.byLevel(level, 0, 1, 1)) * (100 + bonus) / 100.0
                    - AVERAGE_ROLL) / AVERAGE_ROLL;
            case CURSE -> 100.0 * (AVERAGE_ROLL - (level == SkillLevel.BASIC ? unit.minDamage()
                    : Math.max(1, unit.minDamage() * 80 / 100 - 1))) / AVERAGE_ROLL;
            case PRECISION -> 5.0 * (SpellDamage.byLevel(level, 3, 6, 6) + bonus);
            case PRAYER -> 5.0 * (SpellDamage.byLevel(level, 2, 4, 4) + bonus);
            case SLAYER -> 5.0 * (SLAYER_ATTACK + bonus) * slayerShare(level, ballista, setup);
            // Wie bei Kreaturen: je Glückspunkt rund 4 % mehr Schaden (Chance 1/24 auf doppelten Grundschaden).
            case FORTUNE -> 4.0 * Math.min(ArmyLuck.MAX - ballista.getLuck(),
                    Math.max(SpellDamage.byLevel(level, 1, 2, 2), bonus));
            case MISFORTUNE -> 4.0 * Math.min(ballista.getLuck() - ArmyLuck.MIN, SpellDamage.byLevel(level, 1, 2, 2));
            default -> 0;
        };
    }

    /** Anteil der Lebenspunkte der Gegner, auf die Slayer auf dieser Stufe wirkt. */
    private static double slayerShare(SkillLevel level, Stack ballista, BattleSetup setup) {
        double all = 0;
        double hit = 0;
        for (Stack enemy : setup.stacksOf(HeroSpellcasting.other(ballista.side()))) {
            if (enemy.isAlive()) {
                int hp = HeroSpellcasting.totalHealth(enemy);
                all += hp;
                if (Stack.isSlayerTarget(enemy.unit(), level)) {
                    hit += hp;
                }
            }
        }
        return all == 0 ? 0 : hit / all;
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
