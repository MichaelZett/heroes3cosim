package de.zettsystems.h3comsim.armybattle.application;

import de.zettsystems.h3comsim.armybattle.values.FactionPresetDto;
import de.zettsystems.h3comsim.armybattle.values.StackSpec;
import de.zettsystems.h3comsim.battle.domain.Action;
import de.zettsystems.h3comsim.battle.domain.AutoSolver;
import de.zettsystems.h3comsim.battle.domain.Battle;
import de.zettsystems.h3comsim.battle.domain.BattleSetup;
import de.zettsystems.h3comsim.battle.domain.Battlefield;
import de.zettsystems.h3comsim.battle.domain.GreedyAutoSolver;
import de.zettsystems.h3comsim.battle.domain.Hex;
import de.zettsystems.h3comsim.battle.domain.ObstacleGenerator;
import de.zettsystems.h3comsim.battle.domain.Stack;
import de.zettsystems.h3comsim.battle.domain.StrategicAutoSolver;
import de.zettsystems.h3comsim.battle.domain.Unit;
import de.zettsystems.h3comsim.battle.domain.UnitCatalog;
import de.zettsystems.h3comsim.battle.domain.WarMachine;
import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Über alle Fraktionspaarungen und beide Solver: Vor jedem Zug belegt kein Feld zwei Stacks, und
 * jeder lebende Stack steht mit allen Feldern auf dem Feld und auf keinem Hindernis.
 */
class DoubleWideInvariantTest {

    @Test
    void no_two_stacks_ever_share_a_hex() {
        List<FactionPresetDto> presets = new FactionPresetCatalog().all();
        List<Supplier<AutoSolver>> solvers = List.of(GreedyAutoSolver::new, StrategicAutoSolver::new);
        int checks = 0;
        for (FactionPresetDto attacker : presets) {
            for (FactionPresetDto defender : presets) {
                for (Supplier<AutoSolver> solver : solvers) {
                    long seed = attacker.faction().ordinal() * 31L + defender.faction().ordinal();
                    Checking checking = new Checking(solver.get());
                    new Battle(new Random(seed), checking).simulate(setup(attacker, defender, seed));
                    checks += checking.checks;
                }
            }
        }
        assertThat(checks).isGreaterThan(1000);
    }

    private static BattleSetup setup(FactionPresetDto attacker, FactionPresetDto defender, long seed) {
        List<Stack> a = build(attacker.stacks(), Side.ATTACKER);
        List<Stack> d = build(defender.stacks(), Side.DEFENDER);
        Set<Hex> obstacles = new HashSet<>(ObstacleGenerator.generate(Battlefield.STANDARD, new Random(seed)));
        obstacles.removeAll(SpawnLayout.spawnHexesFor(a.size(), d.size()));
        return new BattleSetup(a, d, Battlefield.STANDARD.withObstacles(obstacles))
                .equip(Side.ATTACKER, EnumSet.of(WarMachine.BALLISTA, WarMachine.FIRST_AID_TENT))
                .equip(Side.DEFENDER, EnumSet.of(WarMachine.BALLISTA, WarMachine.AMMO_CART));
    }

    private static List<Stack> build(List<StackSpec> specs, Side side) {
        List<Unit> units = specs.stream().map(s -> UnitCatalog.byName(s.unitName()).orElseThrow()).toList();
        List<Hex> positions = SpawnLayout.assignPositions(side, units);
        List<Stack> stacks = new ArrayList<>();
        for (int i = 0; i < units.size(); i++) {
            stacks.add(new Stack(units.get(i), specs.get(i).count(), positions.get(i), side, i));
        }
        return stacks;
    }

    /** Prüft die Belegung, bevor der eigentliche Solver entscheidet. */
    private static final class Checking implements AutoSolver {
        private final AutoSolver delegate;
        private @Nullable BattleSetup setup;
        private int checks;

        Checking(AutoSolver delegate) {
            this.delegate = delegate;
        }

        @Override
        public void planRound(BattleSetup round) {
            this.setup = round;
            delegate.planRound(round);
        }

        @Override
        public @Nullable Stack pickTarget(Stack active, List<Stack> opponents, Battlefield battlefield) {
            return delegate.pickTarget(active, opponents, battlefield);
        }

        @Override
        public Action decide(Stack active, Stack opponent, Battlefield battlefield) {
            check(Objects.requireNonNull(setup));
            return delegate.decide(active, opponent, battlefield);
        }

        private void check(BattleSetup current) {
            Map<Hex, Stack> taken = new HashMap<>();
            List<Stack> standing = new ArrayList<>(current.aliveStacks());
            for (Side side : Side.values()) {
                current.machinesOf(side).stream().filter(Stack::isAlive).forEach(standing::add);
            }
            Battlefield field = current.baseBattlefield();
            for (Stack stack : standing) {
                for (Hex hex : stack.hexes()) {
                    Stack other = taken.put(hex, stack);
                    assertThat(other).as("%s und %s auf %s", stack.getName(), other == null ? "" : other.getName(), hex)
                            .isNull();
                    if (!stack.isWarMachine()) {
                        assertThat(field.isPassable(hex)).as("%s auf %s", stack.getName(), hex).isTrue();
                    }
                }
            }
            checks++;
        }
    }
}
