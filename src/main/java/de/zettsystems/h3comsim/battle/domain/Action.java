package de.zettsystems.h3comsim.battle.domain;

public sealed interface Action {

    record Move(Hex destination) implements Action {
    }

    record MoveAndMelee(Hex destination, Stack target) implements Action {
    }

    record Melee(Stack target) implements Action {
    }

    record Shoot(Stack target) implements Action {
    }

    record Wait() implements Action {
    }

    /** Pit Lord: beschwört aus einem ganz gefallenen verbündeten Stack Dämonen, einmal pro Kampf. */
    record RaiseDemons(Stack corpse) implements Action {
    }

    /** Ogre Magi: wirkt einen Zauber auf einen verbündeten Stack ({@link CreatureSpellcasting}). */
    record CastSpell(Spell spell, Stack target) implements Action {
    }

    /** Master Genie: wirkt einen zufälligen nützlichen Zauber; Zauber und Ziel wählt der Kampf. */
    record CastRandomSpell() implements Action {
    }

    /** Arch Angel: Resurrection auf einen verbündeten Stack, einmal pro Kampf. */
    record Resurrect(Stack target) implements Action {
    }

    /**
     * H3-Defend: Stack bewegt sich nicht, bekommt +20 % Defense bis Rundenende (RoE-Manual
     * S. 47). Klassische Schützen-Tank-Strategie, wenn der Tank vor dem Schützen schon
     * richtig steht.
     */
    record Defend() implements Action {
    }
}
