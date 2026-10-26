package de.zettsystems.h3comsim.battle.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Arrays;
import java.util.List;

/**
 * Die 14 Plätze am Helden, wie im Heldenfenster des Spiels: Kopf, Hals, Schultern, beide Hände,
 * Torso, zwei Ringe, Füße und fünf Taschen (heroes.thelazy.net, <em>Artifact</em>). Jeder Platz
 * nimmt nur Artefakte seiner Art ({@link Kind}).
 */
@Schema(description = "Platz für ein Artefakt am Helden.", enumAsRef = true)
public enum ArtifactSlot {
    HEAD(Kind.HELMET),
    NECK(Kind.NECKLACE),
    SHOULDERS(Kind.CAPE),
    RIGHT_HAND(Kind.WEAPON),
    LEFT_HAND(Kind.SHIELD),
    TORSO(Kind.TORSO),
    RING_1(Kind.RING),
    RING_2(Kind.RING),
    FEET(Kind.FEET),
    MISC_1(Kind.MISC),
    MISC_2(Kind.MISC),
    MISC_3(Kind.MISC),
    MISC_4(Kind.MISC),
    MISC_5(Kind.MISC);

    /** Art eines Platzes und der Artefakte, die dort liegen dürfen. */
    @Schema(description = "Art eines Artefakts: welcher Platz es aufnimmt.", enumAsRef = true)
    public enum Kind {
        HELMET, NECKLACE, CAPE, WEAPON, SHIELD, TORSO, RING, FEET, MISC
    }

    private final Kind kind;

    ArtifactSlot(Kind kind) {
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    /** Alle Plätze dieser Art, in fester Reihenfolge. */
    public static List<ArtifactSlot> of(Kind kind) {
        return Arrays.stream(values()).filter(slot -> slot.kind == kind).toList();
    }
}
