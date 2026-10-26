package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.Side;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * Die Zauber, die Angelic Alliance (Prayer auf alle eigenen Stacks, 10 Runden) und Armor of the
 * Damned (Slow, Curse, Weakness, Misfortune auf alle feindlichen, 50 Runden) zu Beginn der
 * Schlacht wirken, auf Expert und ohne Mana.
 *
 * <p>Laut heroes.thelazy.net (<em>Armor of the Damned</em>) erst, wenn die erste eigene Kreatur
 * ihren Zug antritt; setzt sie wegen schlechter Moral aus, wartet der Zauber auf die nächste.
 * Immunität und Magieresistenz wirken wie bei einem Heldenzauber, Cursed Ground lässt nur Zauber
 * der Stufe 1 auf Basic zu; die Sperre durch Orb of Inhibition und Recanter's Cloak gilt nicht
 * (<em>Orb of Inhibition</em>: „Does not prevent: Artifact spell casting"). Eine
 * Zauber-Spezialität des Helden wirkt mit.
 */
final class ArtifactSpells {

    /** Wirkung auf einen Stack; {@code works} ist falsch, wenn er immun war oder widerstand. */
    record Hit(Artifact artifact, Spell spell, SkillLevel level, Stack target, boolean works) {
    }

    private ArtifactSpells() {
    }

    /** Wirkt alle Eröffnungszauber des Helden dieser Seite und liefert die Treffer in Reihenfolge. */
    static List<Hit> cast(Side side, BattleSetup setup, RandomGenerator rng) {
        Hero hero = setup.heroOf(side);
        List<Hit> hits = new ArrayList<>();
        if (hero == null) {
            return hits;
        }
        boolean cursed = setup.magicTerrain() == MagicTerrain.CURSED_GROUND;
        for (Artifact artifact : hero.artifacts().artifacts()) {
            for (ArtifactEffect effect : artifact.effects()) {
                if (!(effect instanceof ArtifactEffect.OpeningSpell opening)
                        || (cursed && opening.spell().level() > 1)) {
                    continue;
                }
                Spell spell = opening.spell();
                SkillLevel level = cursed ? SkillLevel.BASIC : SkillLevel.EXPERT;
                Side targets = spell.beneficial() ? side : HeroSpellcasting.other(side);
                for (Stack target : setup.stacksOf(targets)) {
                    if (!target.isAlive()) {
                        continue;
                    }
                    boolean works = SpellResistance.takesEffect(spell, side, target, setup, rng);
                    if (works) {
                        target.applySpell(spell, level, opening.rounds(),
                                hero.specialty().spellEffectBonus(spell, hero.level(), target.unit()));
                    }
                    hits.add(new Hit(artifact, spell, level, target, works));
                }
            }
        }
        return hits;
    }
}
