package de.zettsystems.h3comsim.battle.values;

import de.zettsystems.h3comsim.battle.domain.CreatureBank;
import de.zettsystems.h3comsim.battle.domain.Unit;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "Kreaturenbank mit ihren Ausbaustufen und Wächtern.")
public record CreatureBankDto(
        @Schema(description = "Identifier für `creatureBank.bank`", example = "CYCLOPS_STOCKPILE")
        CreatureBank id,

        @Schema(description = "Englischer Name wie im Spiel", example = "Cyclops Stockpile")
        String name,

        @Schema(description = "Ausbaustufen, Stufe 1 zuerst")
        List<Level> levels) {

    @Schema(name = "CreatureBankLevel", description = "Eine Ausbaustufe einer Kreaturenbank.")
    public record Level(
            @Schema(description = "Stufe 1..4", example = "1") int level,
            @Schema(description = "Anteil auf der Karte in Prozent", example = "30") int chance,
            @Schema(description = "Wächter-Stacks in Reihenfolge") List<Guard> guards) {
    }

    @Schema(name = "CreatureBankGuard", description = "Ein Wächter-Stack.")
    public record Guard(
            @Schema(description = "Kreatur (Name aus /api/units)", example = "Cyclops") String unitName,
            @Schema(description = "Anzahl", example = "4") int count,
            @Schema(description = "Ausgebaute Kreatur, die der Stack zu 50 % ist, sonst null", nullable = true)
            @Nullable String upgradeName) {
    }

    public static CreatureBankDto of(CreatureBank bank) {
        List<Level> levels = new ArrayList<>();
        for (int i = 0; i < bank.levels().size(); i++) {
            CreatureBank.Level level = bank.levels().get(i);
            levels.add(new Level(i + 1, level.chance(), level.guards().stream()
                    .map(CreatureBankDto::guard)
                    .toList()));
        }
        return new CreatureBankDto(bank, bank.displayName(), levels);
    }

    private static Guard guard(CreatureBank.Guard guard) {
        Unit upgrade = guard.upgrade();
        return new Guard(guard.unit().name(), guard.count(), upgrade == null ? null : upgrade.name());
    }
}
