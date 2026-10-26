package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Was Zauber auf dem Feld hinterlassen (heroes.thelazy.net; Form und Dauer wie VCMI
 * {@code config/spells/other.json}): Fire Wall (begehbar, verbrennt, wer hindurchgeht oder darin
 * stehen bleibt, zwei Runden), Force Field (unpassierbar, zwei Runden), Quicksand (hält Bodentruppen
 * an, bis Kampfende, für die Gegenseite unsichtbar, bis jemand hineintritt) und Land Mine (trifft
 * Gegner der Zaubernden beim Betreten und verschwindet, für sie unsichtbar).
 */
final class FieldEffects {

    enum Kind { FIRE_WALL, FORCE_FIELD, QUICKSAND, LAND_MINE }

    /** Ein Feld mit Wirkung; {@code lastRound} ist die letzte Runde, in der es noch steht. */
    static final class Patch {
        private final Kind kind;
        private final Hex hex;
        private final Side owner;
        private final int damage;
        private final int lastRound;
        private final @Nullable Hero caster;
        private boolean revealed;

        Patch(Kind kind, Hex hex, Side owner, int damage, int lastRound, @Nullable Hero caster) {
            this.kind = kind;
            this.hex = hex;
            this.owner = owner;
            this.damage = damage;
            this.lastRound = lastRound;
            this.caster = caster;
        }

        Kind kind() {
            return kind;
        }

        Hex hex() {
            return hex;
        }

        Side owner() {
            return owner;
        }

        int damage() {
            return damage;
        }

        int lastRound() {
            return lastRound;
        }

        @Nullable Hero caster() {
            return caster;
        }

        boolean revealed() {
            return revealed;
        }
    }

    private final List<Patch> patches = new ArrayList<>();
    private int version;

    int version() {
        return version;
    }

    boolean isEmpty() {
        return patches.isEmpty();
    }

    void add(Kind kind, Collection<Hex> hexes, Side owner, int damage, int lastRound, @Nullable Hero caster) {
        for (Hex hex : hexes) {
            patches.add(new Patch(kind, hex, owner, damage, lastRound, caster));
        }
        version++;
    }

    List<Patch> patches() {
        return List.copyOf(patches);
    }

    /** Die Felder, die eine Seite nicht betritt: Kraftfelder und der Treibsand, den sie kennt. */
    Set<Hex> blockedFor(Side side, boolean seesHidden) {
        Set<Hex> blocked = new HashSet<>();
        for (Patch patch : patches) {
            if (patch.kind == Kind.FORCE_FIELD
                    || (patch.kind == Kind.QUICKSAND && (patch.owner == side || patch.revealed || seesHidden))) {
                blocked.add(patch.hex);
            }
        }
        return blocked;
    }

    @Nullable Patch at(Hex hex, Kind kind) {
        for (Patch patch : patches) {
            if (patch.kind == kind && patch.hex.equals(hex)) {
                return patch;
            }
        }
        return null;
    }

    boolean occupied(Hex hex) {
        return patches.stream().anyMatch(p -> p.hex.equals(hex));
    }

    /** Meldet eine Änderung, die zwischengespeicherte Felder ungültig macht. */
    void touch() {
        version++;
    }

    void reveal(Patch patch) {
        if (!patch.revealed) {
            patch.revealed = true;
            version++;
        }
    }

    void remove(Patch patch) {
        patches.remove(patch);
        version++;
    }

    /** Entfernt alle Felder einer Art (Remove Obstacle, Dispel). */
    void removeAll(Collection<Patch> removed) {
        if (patches.removeAll(removed)) {
            version++;
        }
    }

    /** Fire Wall und Force Field verschwinden nach ihrer letzten Runde. */
    void expire(int round) {
        if (patches.removeIf(p -> p.lastRound < Integer.MAX_VALUE && round >= p.lastRound)) {
            version++;
        }
    }
}
