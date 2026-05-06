package de.zettsystems.h3comsim.domain;

import java.util.Objects;

public record Combat(int minDamage, int maxDamage, int shots, AttackType attackType) {
    public Combat {
        Objects.requireNonNull(attackType, "attackType");
    }
}
