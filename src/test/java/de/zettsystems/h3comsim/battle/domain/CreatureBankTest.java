package de.zettsystems.h3comsim.battle.domain;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Katalog und Aufstellung der Kreaturenbanken (VCMI). */
class CreatureBankTest {

    @Test
    void every_bank_has_four_levels_that_together_cover_the_map() {
        assertThat(CreatureBank.values()).hasSize(11);
        for (CreatureBank bank : CreatureBank.values()) {
            assertThat(bank.levels()).as(bank.displayName()).hasSize(4);
            assertThat(bank.levels().stream().mapToInt(CreatureBank.Level::chance).sum()).isEqualTo(100);
            assertThat(bank.levels()).allSatisfy(l -> assertThat(l.guards()).hasSizeBetween(4, 5));
        }
        assertThat(CreatureBank.CRYPT.level(3).guards()).extracting(CreatureBank.Guard::unit)
                .containsExactly(UnitCatalog.SKELETON, UnitCatalog.WALKING_DEAD, UnitCatalog.WIGHT, UnitCatalog.VAMPIRE);
        assertThatThrownBy(() -> CreatureBank.CRYPT.level(5)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void levels_are_rolled_by_their_share() {
        int[] counts = new int[5];
        Random random = new Random(1);
        for (int i = 0; i < 10_000; i++) {
            counts[CreatureBank.NAGA_BANK.rollLevel(random)]++;
        }
        assertThat(counts[4]).isBetween(800, 1200);
        assertThat(counts[1]).isBetween(2700, 3300);
    }

    @Test
    void the_army_stands_in_the_middle_and_the_guards_around_it() {
        List<Hex> army = IntStream.range(0, 7).mapToObj(CreatureBank::attackerHex).toList();
        assertThat(new HashSet<>(army)).hasSize(7);
        assertThat(army).allMatch(Battlefield.STANDARD::contains).allMatch(h -> h.r() >= 3 && h.r() <= 7);
        for (CreatureBank.Layout layout : CreatureBank.Layout.values()) {
            CreatureBank bank = Arrays.stream(CreatureBank.values()).filter(b -> b.layout() == layout).findFirst()
                    .orElseThrow();
            Set<Hex> guards = new HashSet<>();
            IntStream.range(0, 7).forEach(i -> guards.add(bank.guardHex(i)));
            assertThat(guards).hasSize(7).allMatch(Battlefield.STANDARD::contains).doesNotContainAnyElementsOf(army);
        }
        assertThat(CreatureBank.DRAGON_UTOPIA.guardHex(2).q()).isLessThan(CreatureBank.CRYPT.guardHex(2).q());
    }

    @Test
    void one_guard_per_bank_may_be_upgraded() {
        CreatureBank.Guard guard = CreatureBank.CYCLOPS_STOCKPILE.level(1).guards().get(2);
        Random random = new Random(2);
        Set<Unit> rolled = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            rolled.add(CreatureBank.roll(guard, random));
        }
        assertThat(rolled).containsExactlyInAnyOrder(UnitCatalog.CYCLOPS, UnitCatalog.CYCLOPS_KING);
        assertThat(CreatureBank.roll(CreatureBank.CRYPT.level(1).guards().getFirst(), random))
                .isEqualTo(UnitCatalog.SKELETON);
    }
}
