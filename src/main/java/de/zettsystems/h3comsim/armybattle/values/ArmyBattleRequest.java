package de.zettsystems.h3comsim.armybattle.values;

import de.zettsystems.h3comsim.battle.domain.Faction;
import de.zettsystems.h3comsim.battle.domain.Fortification;
import de.zettsystems.h3comsim.battle.domain.MagicTerrain;
import de.zettsystems.h3comsim.battle.domain.Terrain;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

@Schema(description = "Konfiguration einer Army-vs-Army-Simulation. Bis zu 7 Stacks pro Seite. Unit-Namen müssen exakt einem Eintrag aus /api/units entsprechen.")
public record ArmyBattleRequest(
        @Schema(description = "Armee der Attacker-Seite", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Valid ArmySpec attacker,

        @Schema(description = "Armee der Defender-Seite; entfällt im Kampf um eine Kreaturenbank (`creatureBank`).",
                nullable = true)
        @Valid @Nullable ArmySpec defender,

        @Schema(description = "Optionaler PRNG-Seed für deterministische Reproduktion. Bei null wird ein Zufalls-Seed gezogen.",
                example = "42", nullable = true)
        @Nullable Long seed,

        @Schema(description = "Belagerung: Befestigung der Stadt des Verteidigers (FORT, CITADEL, CASTLE). Bei null eine Feldschlacht. Der Angreifer braucht einen Helden.",
                example = "CASTLE", nullable = true)
        @Nullable Fortification fortification,

        @Schema(description = "Stadt des Verteidigers bei einer Belagerung; bestimmt Graben und Turmschaden. Bei null die Fraktion seines Helden, ohne Helden die Hauptfraktion der Garnison (nach Goldwert). Eine rein neutrale Garnison braucht die Angabe.",
                example = "RAMPART", nullable = true)
        @Nullable Faction town,

        @Schema(description = "Grundgelände: Kreaturen auf ihrem Heimatgelände bekommen +1 Attack, Defense und Speed, die Hindernisse kommen aus dem Satz des Geländes. Bei null ein Feld ohne Gelände: keine Heimatboni, zufällig verteilte Felsen. Eine Belagerung findet immer auf dem Heimatgelände der Stadt statt.",
                example = "GRASS", nullable = true)
        @Nullable Terrain terrain,

        @Schema(description = "Magisches Gelände über dem Grundgelände (Magic Plains, Cursed Ground, Holy Ground, …), sonst null. Nicht in einer Belagerung.",
                example = "MAGIC_PLAINS", nullable = true)
        @Nullable MagicTerrain magicTerrain,

        @Schema(description = "Zauberkosten nach Shadow of Death: beherrscht ein Held die Schule eines Zaubers (gleich auf welcher Stufe), kostet der Zauber weniger, etwa Haste 5 statt 6. Bei null oder false gelten die Kosten des RoE-Manuals.",
                example = "false", nullable = true)
        @Nullable Boolean sodSpellCosts,

        @Schema(description = """
                Kampf um eine Kreaturenbank: ihre Wächter verteidigen in der Aufstellung der Bank (Armee
                des Angreifers in der Mitte, Wächter an den Rändern), ohne Tactics, Hindernisse und
                Kriegsmaschinen; `defender` wird ignoriert. Nur ein Held greift eine Bank an; keine Belagerung.
                """, nullable = true)
        @Valid @Nullable CreatureBankSpec creatureBank) {

    /** Feldschlacht ohne Belagerung. */
    public ArmyBattleRequest(ArmySpec attacker, ArmySpec defender, @Nullable Long seed) {
        this(attacker, defender, seed, null);
    }

    /** Mit oder ohne Belagerung, Zauberkosten nach RoE. */
    public ArmyBattleRequest(ArmySpec attacker, ArmySpec defender, @Nullable Long seed,
                             @Nullable Fortification fortification) {
        this(attacker, defender, seed, fortification, null, null, null, null);
    }

    /** Ohne Kreaturenbank. */
    // Je Feld ein Parameter, wie beim kanonischen Konstruktor.
    @SuppressWarnings("java:S107")
    public ArmyBattleRequest(ArmySpec attacker, ArmySpec defender, @Nullable Long seed,
                             @Nullable Fortification fortification, @Nullable Faction town, @Nullable Terrain terrain,
                             @Nullable MagicTerrain magicTerrain, @Nullable Boolean sodSpellCosts) {
        this(attacker, defender, seed, fortification, town, terrain, magicTerrain, sodSpellCosts, null);
    }
}
