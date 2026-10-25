package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.Side;

import java.util.Arrays;

/**
 * Zyklopen schießen in einer Belagerung auf die Mauern ({@link UnitSpeciality#ATTACKS_WALLS}):
 * der Cyclops wie ein Katapult mit Basic Ballistics, der Cyclops King mit Advanced (RoE-Manual;
 * heroes.thelazy.net, <em>Cyclops</em>). Der König schießt damit zweimal, wie in VCMI. Der Schuss
 * ersetzt den Fernangriff auf eine Kreatur und verbraucht dessen Munition; er ist nur dem
 * Angreifer möglich und nur, wenn der Zyklop überhaupt schießen dürfte.
 */
final class WallShots {

    private WallShots() {
    }

    static SkillLevel ballistics(Stack shooter) {
        return shooter.unit().equals(UnitCatalog.CYCLOPS_KING) ? SkillLevel.ADVANCED : SkillLevel.BASIC;
    }

    /** Kann dieser Stack jetzt auf die Befestigung schießen? */
    static boolean possible(Stack active, BattleSetup setup) {
        Siege siege = setup.siege();
        return siege != null && active.side() == Side.ATTACKER
                && active.hasSpeciality(UnitSpeciality.ATTACKS_WALLS) && active.canShoot()
                && Arrays.stream(WallPart.values()).anyMatch(siege::stands);
    }

    /**
     * Lohnt der Schuss auf die Mauern? Solange das Tor zu ist, alle Mauersegmente stehen und ein
     * eigener Nahkämpfer am Boden auf den Weg hinein wartet.
     */
    static boolean worthwhile(Stack active, BattleSetup setup) {
        Siege siege = setup.siege();
        if (siege == null || siege.hasBreach()) {
            return false;
        }
        return setup.stacksOf(active.side()).stream().anyMatch(s -> !s.equals(active) && waitsOutside(s));
    }

    /** Ein lebender Nahkämpfer am Boden: Er braucht einen Weg durch die Mauern. */
    static boolean waitsOutside(Stack s) {
        return s.isAlive() && !s.canShoot() && s.unit().movement() == Movement.GROUND;
    }
}
