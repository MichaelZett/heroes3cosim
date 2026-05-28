package de.zettsystems.h3comsim.battle.domain;

/**
 * Team-Stance pro Seite, vom {@link StrategicAutoSolver} pro Runde aus der Power-Balance
 * (eigene vs. gegnerische Ranged- und Melee-Power) abgeleitet. Steuert das Verhalten der
 * eigenen Stacks: Schützen schützen vs. Sturmangriff vs. Greedy-Fallback.
 */
public enum TeamStance {
    RANGED_DOMINANT,
    MELEE_DOMINANT,
    BALANCED
}
