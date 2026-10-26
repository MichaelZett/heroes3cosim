package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.BattleEvent;
import de.zettsystems.h3comsim.battle.domain.events.EventCollector;
import de.zettsystems.h3comsim.battle.domain.events.HexCoord;
import de.zettsystems.h3comsim.battle.domain.events.NoopEventCollector;
import de.zettsystems.h3comsim.battle.domain.events.Side;
import de.zettsystems.h3comsim.battle.domain.events.SiegeSnapshot;
import de.zettsystems.h3comsim.battle.domain.events.StackSnapshot;
import de.zettsystems.h3comsim.battle.domain.events.Winner;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.random.RandomGenerator;

public final class Battle {

    /**
     * Harte Obergrenze gegen pathologische Endlos-Schleifen.
     */
    private static final int TURN_CAP = 200;
    /**
     * Sicherheitsabbruch, wenn so viele aufeinanderfolgende Runden ohne Verluste auf beiden Seiten
     * vergehen. Trifft typische Kite-Patterns, in denen ein Schütze ohne Schüsse aus dem Stand
     * läuft, ohne dass jemand stirbt.
     */
    private static final int NO_PROGRESS_LIMIT = 20;

    private final RandomGenerator rng;
    /** Das laufende Setup, für Effekte tief in der Schadensrechnung (Magieresistenz der Nachbarn). */
    private @Nullable BattleSetup currentSetup;
    /** Laufende Runde ab 0, für die Mana-Einteilung der Helden. */
    private int round;
    private final List<Stack> joinedThisRound = new ArrayList<>();
    /** Dauer der Zauber, die Unicorns und Dragon Flies beim Angriff auslösen (heroes.thelazy.net). */
    private static final int PROC_ROUNDS = 3;
    private final AutoSolver autoSolver;
    private final EventCollector events;

    public Battle(RandomGenerator rng) {
        this(rng, new GreedyAutoSolver(), NoopEventCollector.INSTANCE);
    }

    public Battle(RandomGenerator rng, AutoSolver autoSolver) {
        this(rng, autoSolver, NoopEventCollector.INSTANCE);
    }

    public Battle(RandomGenerator rng, AutoSolver autoSolver, EventCollector events) {
        this.rng = rng;
        this.autoSolver = autoSolver;
        this.events = events;
    }

    public BattleResult simulate(BattleSetup setup) {
        this.currentSetup = setup;
        BattleLogger.logStartOfCombat(setup.getAttackerName(), setup.getAttackerCount(),
                setup.getDefenderName(), setup.getDefenderCount());
        Battlefield bf = setup.baseBattlefield();
        List<HexCoord> obstacleCoords = bf.obstacles().stream()
                .map(h -> new HexCoord(h.q(), h.r()))
                .toList();
        StackSnapshot attackerSnap = snapshot(setup.getAttacker());
        StackSnapshot defenderSnap = snapshot(setup.getDefender());
        List<StackSnapshot> initialStacks = setup.aliveStacks().stream().map(Battle::snapshot).toList();
        events.emit(new BattleEvent.BattleStart(bf.width(), bf.height(), obstacleCoords,
                attackerSnap, defenderSnap, initialStacks,
                setup.moraleOf(Side.ATTACKER), setup.moraleOf(Side.DEFENDER),
                heroName(setup.attackerHero()), heroName(setup.defenderHero()),
                setup.luckOf(Side.ATTACKER), setup.luckOf(Side.DEFENDER),
                List.copyOf(setup.warMachinesOf(Side.ATTACKER)), List.copyOf(setup.warMachinesOf(Side.DEFENDER)),
                machineSnapshots(setup), siegeSnapshot(setup.siege()), setup.terrain(), setup.magicTerrain()));
        for (Side side : Side.values()) {
            if (setup.tacticsOf(side) > 0) {
                events.emit(new BattleEvent.TacticsPhase(side, setup.tacticsOf(side)));
            }
        }

        int attackerStart = setup.getAttackerCount();
        int defenderStart = setup.getDefenderCount();
        int turn = 0;

        int lastAttackerCount = attackerStart;
        int lastDefenderCount = defenderStart;
        int stalledRounds = 0;
        TurnOrder order = new TurnOrder();
        while (setup.bothAlive() && turn < TURN_CAP && stalledRounds < NO_PROGRESS_LIMIT) {
            round = turn;
            doTurn(setup, order);
            turn++;
            int aCount = setup.getAttackerCount();
            int dCount = setup.getDefenderCount();
            if (aCount == lastAttackerCount && dCount == lastDefenderCount) {
                stalledRounds++;
            } else {
                stalledRounds = 0;
                lastAttackerCount = aCount;
                lastDefenderCount = dCount;
            }
        }
        BattleLogger.logMiddleDelimiter();

        Winner winner = determineWinner(setup);

        List<StackSnapshot> finalStacks = new ArrayList<>();
        setup.attackerStacks().forEach(s -> finalStacks.add(snapshot(s)));
        setup.defenderStacks().forEach(s -> finalStacks.add(snapshot(s)));
        events.emit(new BattleEvent.BattleEnd(winner,
                setup.getAttackerCount(), setup.getDefenderCount(), turn, finalStacks));
        return new BattleResult(winner,
                attackerStart, setup.survivorsOf(Side.ATTACKER),
                defenderStart, setup.survivorsOf(Side.DEFENDER),
                turn);
    }

    /**
     * Eine Runde in zwei Phasen, Manual S. 43: „Play will pass on to the next creature and return
     * to the waiting creature at the end of the first phase, after all other creatures have had a
     * chance to move." Wer in Phase 1 wartet, landet in der Late-Queue und handelt erst, wenn alle
     * anderen gezogen sind — mit dem Brett, wie es dann aussieht.
     *
     * <p>Die Reihenfolge entsteht Schritt für Schritt statt als vorab sortierte Queue: Bei
     * Speed-Gleichstand hängt sie davon ab, welche Seite zuletzt gehandelt hat
     * ({@link TurnOrder}). Tote Stacks fallen über {@link Stack#isAbleToAct()} heraus.
     */
    private void doTurn(BattleSetup setup, TurnOrder order) {
        drainMana(setup);
        siegeEnginesAct(setup);
        updateGate(setup);
        autoSolver.planRound(setup);
        List<Stack> roundStacks = new ArrayList<>(setup.aliveStacks());
        List<Stack> pending = new ArrayList<>(roundStacks);
        List<Stack> waiting = new ArrayList<>();
        Set<Side> heroesCast = EnumSet.noneOf(Side.class);
        while (!pending.isEmpty()) {
            Stack activeStack = order.next(pending, true);
            if (activeStack.isAbleToAct() && setup.bothAlive()) {
                order.acted(activeStack);
                heroCasts(setup, activeStack, heroesCast);
                joinRound(pending, roundStacks);
                if (activeStack.isAbleToAct() && setup.bothAlive()) {
                    actWithMorale(activeStack, setup);
                }
                updateGate(setup);
                if (activeStack.hasWaitedThisTurn()) {
                    waiting.add(activeStack);
                }
            }
            BattleLogger.logShortDelimiter();
        }
        runLatePhase(waiting, setup, order, heroesCast, roundStacks);
        warMachinesAct(setup);
        updateGate(setup);
        roundStacks.forEach(Stack::endTurn);
        // Zauber auf Maschinen (Bless auf der Ballista) laufen ab wie auf Kreaturen.
        for (Side side : Side.values()) {
            setup.machinesOf(side).stream().filter(Stack::isAlive).forEach(Stack::endTurn);
        }
    }

    /** Zu Rundenbeginn: jeder lebende Wraith-Stack zieht dem gegnerischen Helden 2 Mana ab. */
    private static void drainMana(BattleSetup setup) {
        for (Side side : Side.values()) {
            long wraiths = setup.stacksOf(side).stream()
                    .filter(s -> s.isAlive() && s.hasSpeciality(UnitSpeciality.MANA_DRAIN)).count();
            if (wraiths > 0) {
                setup.spendMana(HeroSpellcasting.other(side), (int) (2 * wraiths));
            }
        }
    }

    /** Ein lebender Familiar beim Gegner leitet 20 % des ausgegebenen Manas, abgerundet, um. */
    private static void channelMana(BattleSetup setup, Side caster, int spent) {
        Side other = HeroSpellcasting.other(caster);
        if (setup.stacksOf(other).stream().anyMatch(s -> s.isAlive() && s.hasSpeciality(UnitSpeciality.MANA_CHANNEL))) {
            setup.gainMana(other, spent * 20 / 100);
        }
    }

    /** Nach jedem Zug: Bewegung oder ein Gefallener auf dem Tor kann die Zugbrücke umstellen. */
    private void updateGate(BattleSetup setup) {
        Siege.@Nullable Gate changed = setup.updateGate();
        if (changed != null) {
            events.emit(new BattleEvent.Drawbridge(changed));
        }
    }

    /**
     * Der Held zaubert, wenn einer seiner Stacks am Zug ist — einmal pro Runde (Manual S. 45), ohne
     * dem Stack die Aktion zu nehmen. Findet die Heuristik nichts Lohnendes, versucht er es beim
     * nächsten eigenen Stack erneut. Ohne Zauberbuch zieht das keinen Zufallswert.
     */
    private void heroCasts(BattleSetup setup, Stack active, Set<Side> heroesCast) {
        Side side = active.side();
        Hero hero = setup.heroOf(side);
        if (hero == null || heroesCast.contains(side)) {
            return;
        }
        HeroSpellcasting.Cast cast = HeroSpellcasting.decide(hero, side, setup.manaOf(side), setup, round, active);
        if (cast == null) {
            return;
        }
        heroesCast.add(side);
        Spell spell = cast.spell();
        int cost = HeroSpellcasting.cost(spell, side, setup);
        setup.spendMana(side, cost);
        channelMana(setup, side, cost);
        events.emit(new BattleEvent.HeroSpell(side, spell, cast.level(), setup.manaOf(side)));
        if (spell.kind() == Spell.Kind.SUMMON) {
            summon(setup, side, hero, cast);
            return;
        }
        if (spell == Spell.EARTHQUAKE) {
            earthquake(Objects.requireNonNull(setup.siege()), cast.level());
            return;
        }
        if (spell == Spell.TELEPORT) {
            teleport(setup, hero, active, cast.level());
            return;
        }
        for (int i = 0; i < cast.affected().size(); i++) {
            Stack target = cast.affected().get(i);
            // Resurrection und Animate Dead holen auch ganz Gefallene zurück (Ziel geprüft in der Wahl).
            if (target.isAlive() || HeroSpellcasting.revives(spell)) {
                Stack reflectedTo = mirrored(setup, side, cast, target);
                Stack hit = reflectedTo == null ? target : reflectedTo;
                BattleLogger.logSpell(hero.name(), spell, hit.getName());
                applyHeroSpell(setup, side, hero, cast, i, hit, reflectedTo != null);
            }
        }
    }

    /**
     * Magic Mirror (Manual S. 66): ein feindlicher Zauber auf ein einzelnes Ziel geht mit 20/30/40 %
     * an einen zufälligen Stack des Zaubernden. Ohne Spiegel zieht das keinen Zufallswert.
     *
     * @return der neue Empfänger, oder {@code null}, wenn der Zauber sein Ziel trifft
     */
    private @Nullable Stack mirrored(BattleSetup setup, Side side, HeroSpellcasting.Cast cast, Stack target) {
        return cast.target() == null ? null : mirrored(setup, side, cast.spell(), target);
    }

    /**
     * Wie oben für einen Zauber auf genau dieses Ziel. Faerie Dragons tragen den Spiegel auf Basic
     * (20 %) dauerhaft; ein stärker gewirkter Spiegel geht vor.
     */
    private @Nullable Stack mirrored(BattleSetup setup, Side side, Spell spell, Stack target) {
        int percent = mirrorPercent(target);
        if (percent == 0 || target.side() == side || !HeroSpellcasting.singleTarget(spell)
                || rng.nextInt(100) >= percent) {
            return null;
        }
        List<Stack> casters = setup.stacksOf(side).stream().filter(Stack::isAlive).toList();
        return casters.isEmpty() ? null : casters.get(rng.nextInt(casters.size()));
    }

    private static int mirrorPercent(Stack target) {
        ActiveSpell mirror = target.activeSpell(Spell.MAGIC_MIRROR);
        if (mirror != null) {
            return HeroSpellcasting.mirrorPercent(mirror);
        }
        return target.hasSpeciality(UnitSpeciality.NATURAL_MAGIC_MIRROR) ? 20 : 0;
    }

    /** Frisch beschworene Stacks reihen sich in die laufende Runde ein. */
    private void joinRound(List<Stack> pending, List<Stack> roundStacks) {
        for (Stack joined : joinedThisRound) {
            if (!pending.contains(joined)) {
                pending.add(joined);
            }
            if (!roundStacks.contains(joined)) {
                roundStacks.add(joined);
            }
        }
        joinedThisRound.clear();
    }

    /**
     * Summon Elementals (Manual S. 66 ff.): Power × 2/3/4 Elementare als neuer Stack am eigenen
     * Rand. Dieselbe Art erneut verstärkt den beschworenen Stack bzw. ruft ihn zurück, wenn er
     * gefallen ist. Ein neuer oder zurückgerufener Stack handelt noch in dieser Runde, wenn die
     * Zugreihenfolge ihn erreicht — wie in VCMI, wo er mit dem Status „bereit“ ins Feld kommt.
     */
    private void summon(BattleSetup setup, Side side, Hero hero, HeroSpellcasting.Cast cast) {
        Unit unit = HeroSpellcasting.summonedUnit(cast.spell());
        int count = HeroSpellcasting.summonCount(cast.level(), hero.spellPower());
        Stack summoned = setup.stacksOf(side).stream()
                .filter(st -> st.isSummoned() && st.unit().equals(unit))
                .findFirst().orElse(null);
        if (summoned != null && summoned.isAlive()) {
            summoned.reinforce(count);
        } else {
            Hex hex = setup.freeHexFor(side, unit.doubleWide());
            if (hex == null) {
                return;
            }
            if (summoned == null) {
                summoned = new Stack(unit, count, hex, side, setup.nextSlot(side));
                setup.addSummoned(summoned);
            } else {
                summoned.moveTo(hex);
                summoned.riseAs(unit, count);
            }
            joinedThisRound.add(summoned);
        }
        BattleLogger.logSpell(hero.name(), cast.spell(), summoned.getName());
        events.emit(new BattleEvent.Summon(side, cast.spell(), count, snapshot(summoned)));
    }

    /** Teleport ({@link Teleport}): der Stack springt; im Replay ein Move mit Start und Ziel. */
    private void teleport(BattleSetup setup, Hero hero, Stack mover, SkillLevel level) {
        Hex destination = Objects.requireNonNull(Teleport.destination(mover, level, setup));
        Hex from = mover.position();
        mover.moveTo(destination);
        BattleLogger.logSpell(hero.name(), Spell.TELEPORT, mover.getName());
        events.emit(new BattleEvent.Move(mover.side(), mover.slot(), from.q(), from.r(),
                destination.q(), destination.r(), List.of(coord(from), coord(destination))));
    }

    private void applyHeroSpell(BattleSetup setup, Side side, Hero hero, HeroSpellcasting.Cast cast, int index,
                                Stack target, boolean reflected) {
        Spell spell = cast.spell();
        // Dispel auf eigene Stacks ist kein feindlicher Zauber und würfelt nicht gegen Resistenz.
        boolean friendlyDispel = spell == Spell.DISPEL && target.side() == side;
        boolean works = friendlyDispel
                ? !SpellResistance.isImmune(target, spell)
                : SpellResistance.takesEffect(spell, target, setup, rng);
        int damage = 0;
        int killed = 0;
        int healed = 0;
        if (works) {
            int power = hero.spellPower();
            switch (spell.kind()) {
                case DAMAGE -> {
                    damage = SpellDamage.toTarget(spell, HeroSpellcasting.baseDamage(cast, power, index), hero, target);
                    int countBefore = target.getCount();
                    boolean rebirthUsedBefore = target.isRebirthUsed();
                    target.takeDamage(damage, Set.of());
                    killed = Math.max(0, countBefore - target.getCount());
                    emitRebirthIfFired(target, rebirthUsedBefore);
                    setup.machineHit(target);
                }
                case INSTANT -> healed = applyInstantSpell(spell,
                        HeroSpellcasting.healing(hero, spell, cast.level(), target), target);
                case EFFECT -> target.applySpell(spell, cast.level(), power,
                        hero.specialty().spellEffectBonus(spell, hero.level(), target.unit()));
                case SUMMON, OTHER -> throw new IllegalStateException(spell + " is not cast on a stack");
            }
        }
        events.emit(new BattleEvent.SpellHit(side, spell, target.side(), target.slot(), damage, killed, healed,
                !works, reflected, snapshot(target)));
    }

    /** Cure, Animate Dead, Resurrection und Dispel; liefert die geheilten Lebenspunkte. */
    private static int applyInstantSpell(Spell spell, int amount, Stack target) {
        return switch (spell) {
            case CURE -> {
                target.removeSpells(s -> !s.beneficial());
                yield target.healTop(amount);
            }
            case ANIMATE_DEAD, RESURRECTION -> target.heal(amount);
            case DISPEL -> {
                target.removeSpells(s -> true);
                yield 0;
            }
            default -> 0;
        };
    }

    /**
     * Kriegsmaschinen am Rundenende: erst die Ballisten (Angreifer, dann Verteidiger), zuletzt die
     * Sanitätszelte — „First Aid Tents act last at the combat round" (heroes.thelazy.net). Ohne
     * Maschinen zieht diese Methode keinen Zufallswert.
     */
    private void warMachinesAct(BattleSetup setup) {
        for (Side side : Side.values()) {
            if (setup.hasWorking(side, WarMachine.BALLISTA)) {
                fireBallista(setup, side);
            }
        }
        for (Side side : Side.values()) {
            if (setup.bothAlive() && setup.hasWorking(side, WarMachine.FIRST_AID_TENT)) {
                tendWounded(setup, side);
            }
        }
    }

    /**
     * Zu Rundenbeginn einer Belagerung handeln zuerst die Türme, dann das Katapult (heroes.thelazy.net,
     * <em>Combat</em>). Ohne Belagerung zieht das keinen Zufallswert.
     */
    private void siegeEnginesAct(BattleSetup setup) {
        Siege siege = setup.siege();
        if (siege == null) {
            return;
        }
        for (WallPart tower : ArrowTowers.standing(siege)) {
            Stack target = setup.bothAlive() ? ArrowTowers.target(tower, siege, setup) : null;
            if (target != null) {
                towerShot(tower, siege, target);
            }
        }
        if (setup.bothAlive() && setup.hasWorking(Side.ATTACKER, WarMachine.CATAPULT)) {
            catapultShots(setup, siege);
        }
    }

    private void towerShot(WallPart tower, Siege siege, Stack target) {
        int damage = ArrowTowers.damage(tower, siege, rng);
        int countBefore = target.getCount();
        boolean rebirthUsedBefore = target.isRebirthUsed();
        target.takeDamage(damage, Set.of());
        emitRebirthIfFired(target, rebirthUsedBefore);
        events.emit(new BattleEvent.TowerShot(tower, target.slot(), damage,
                Math.max(0, countBefore - target.getCount()), snapshot(target)));
    }

    private void catapultShots(BattleSetup setup, Siege siege) {
        Hero hero = setup.heroOf(Side.ATTACKER);
        SkillLevel ballistics = hero == null ? SkillLevel.NONE : hero.levelOf(SecondarySkill.BALLISTICS);
        for (int i = 0; i < Catapult.shots(ballistics); i++) {
            Catapult.Shot shot = Catapult.fire(siege, ballistics, rng);
            if (shot == null) {
                return;
            }
            siege.damage(shot.hit(), shot.damage());
            events.emit(new BattleEvent.CatapultShot(shot.aimed(), shot.hit(), shot.damage(), siege.health(shot.hit())));
        }
    }

    private void earthquake(Siege siege, SkillLevel level) {
        for (int i = 0; i < Earthquake.hits(level); i++) {
            WallPart hit = Earthquake.strike(siege, rng);
            if (hit == null) {
                return;
            }
            events.emit(new BattleEvent.QuakeHit(hit, siege.health(hit)));
        }
    }

    /** Ein Zyklop schießt auf die Befestigung, mit dem Ballistics-Grad seiner Art. */
    private void wallShots(Stack shooter, Siege siege) {
        SkillLevel ballistics = WallShots.ballistics(shooter);
        shooter.useShot();
        for (int i = 0; i < Catapult.shots(ballistics); i++) {
            Catapult.Shot shot = Catapult.fire(siege, ballistics, rng);
            if (shot == null) {
                return;
            }
            siege.damage(shot.hit(), shot.damage());
            events.emit(new BattleEvent.CatapultShot(shot.aimed(), shot.hit(), shot.damage(), siege.health(shot.hit()),
                    shooter.slot()));
        }
    }

    private static @Nullable SiegeSnapshot siegeSnapshot(@Nullable Siege siege) {
        if (siege == null) {
            return null;
        }
        List<SiegeSnapshot.Part> parts = new ArrayList<>();
        for (WallPart part : WallPart.values()) {
            Hex hex = part.hex();
            parts.add(new SiegeSnapshot.Part(part, hex == null ? null : coord(hex), siege.health(part)));
        }
        return new SiegeSnapshot(siege.fortification(), siege.town(),
                SiegeLayout.PERMANENT_WALL.stream().map(Battle::coord).toList(), parts,
                siege.moat().stream().map(Battle::coord).toList());
    }

    private static HexCoord coord(Hex hex) {
        return new HexCoord(hex.q(), hex.r());
    }

    private static List<StackSnapshot> machineSnapshots(BattleSetup setup) {
        List<StackSnapshot> machines = new ArrayList<>();
        for (Side side : Side.values()) {
            setup.machinesOf(side).forEach(machine -> machines.add(snapshot(machine)));
        }
        return machines;
    }

    private void fireBallista(BattleSetup setup, Side side) {
        Hero hero = setup.heroOf(side);
        int shots = hero == null ? 1 : hero.ballistaShots();
        for (int i = 0; i < shots && setup.bothAlive(); i++) {
            Stack target = ballistaTarget(setup, side, hero);
            if (target != null) {
                ballistaShot(setup, side, hero, target);
            }
        }
    }

    /**
     * Ziel der Ballista: der gegnerische Stack, bei dem der erwartete Schaden den meisten Goldwert
     * vernichtet (Schaden, höchstens die Lebenspunkte des Stacks, mal Kosten je Lebenspunkt). So
     * zielt sie wie ein Spieler mit Artillery auf das wertvollste erreichbare Fleisch — ohne die
     * Fertigkeit wählt im Spiel der Computer, und der zielt ebenso auf den größten Schaden.
     */
    private static @Nullable Stack ballistaTarget(BattleSetup setup, Side side, @Nullable Hero hero) {
        int heroAttack = hero == null ? 0 : hero.attack();
        int attack = hero == null ? WarMachine.BALLISTA.attack() : hero.ballistaAttack();
        Stack best = null;
        double bestValue = -1;
        for (Stack enemy : setup.stacksOf(side == Side.ATTACKER ? Side.DEFENDER : Side.ATTACKER)) {
            if (!enemy.isAlive()) {
                continue;
            }
            double damage = 2.5 * (heroAttack + 1)
                    * (100 + Stack.boniMaliPercentage(attack, enemy.getDefense())) / 100.0;
            int totalHealth = (enemy.getCount() - 1) * enemy.unit().health() + enemy.getCurrentHealth();
            double value = Math.min(damage, totalHealth) * enemy.unit().cost() / enemy.unit().health();
            if (value > bestValue) {
                best = enemy;
                bestValue = value;
            }
        }
        return best;
    }

    /**
     * Ein Ballista-Schuss (Manual S. 117): Grundschaden (2–3) × (Attack des Helden + 1), dann die
     * normale Formel mit Attack 10 (plus Ballista-Spezialität) gegen die Defense des Ziels. Glück
     * wirkt (heroes.thelazy.net), Artillery gibt mit 50/75/100 % Chance weitere +100 % auf den
     * Grundschaden; Archery des Helden und Armorer des Gegners greifen wie bei Schützen.
     */
    private void ballistaShot(BattleSetup setup, Side side, @Nullable Hero hero, Stack target) {
        int heroAttack = hero == null ? 0 : hero.attack();
        Stack ballista = setup.working(side, WarMachine.BALLISTA);
        int base = BallistaSpells.roll(ballista, rng) * (heroAttack + 1);
        int attack = (hero == null ? WarMachine.BALLISTA.attack() : hero.ballistaAttack())
                + BallistaSpells.attackBonus(ballista, target);
        int luck = BallistaSpells.luck(ballista, setup.luckOf(side));
        boolean lucky = luck > 0 && rng.nextInt(1000) < ArmyMorale.triggerChancePerMille(luck);
        int chance = hero == null ? 0 : hero.artilleryDoubleDamagePercent();
        boolean doubled = chance >= 100 || (chance > 0 && rng.nextInt(100) < chance);
        int percent = bonusDamagePercent(Stack.boniMaliPercentage(attack, target.getDefense()),
                (lucky ? 1 : 0) + (doubled ? 1 : 0));
        int damage = base * percent / 100;
        if (hero != null) {
            damage = damage * (1000 + hero.archeryBonusPerMille()) / 1000;
        }
        Hero targetHero = target.commander();
        if (targetHero != null) {
            damage = damage * (1000 - targetHero.armorerReductionPerMille()) / 1000;
        }
        int countBefore = target.getCount();
        boolean rebirthUsedBefore = target.isRebirthUsed();
        target.takeDamage(damage, Set.of());
        emitRebirthIfFired(target, rebirthUsedBefore);
        events.emit(new BattleEvent.BallistaShot(side, target.side(), target.slot(), damage,
                Math.max(0, countBefore - target.getCount()), lucky, doubled, snapshot(target)));
    }

    /**
     * Sanitätszelt: heilt die oberste Kreatur des eigenen Stacks, dem dort am meisten fehlt, um
     * 1 bis zur Obergrenze ({@link Hero#firstAidMaxHeal()}, ohne Helden 25). Ist niemand
     * verwundet, bleibt das Zelt untätig und zieht keinen Zufallswert.
     */
    private void tendWounded(BattleSetup setup, Side side) {
        Stack patient = null;
        for (Stack stack : setup.stacksOf(side)) {
            if (stack.isAlive() && stack.topMissingHealth() > 0
                    && (patient == null || stack.topMissingHealth() > patient.topMissingHealth())) {
                patient = stack;
            }
        }
        if (patient == null) {
            return;
        }
        Hero hero = setup.heroOf(side);
        int max = hero == null ? 25 : hero.firstAidMaxHeal();
        int healed = patient.healTop(rng.nextInt(1, max + 1));
        events.emit(new BattleEvent.FirstAid(side, patient.slot(), healed, snapshot(patient)));
    }

    /**
     * Wait-Phase: langsamste Wartende zuerst, also umgekehrt zur Hauptphase. Im RoE-Manual steht
     * dazu nichts — belegt ist dort nur die Phasen-Trennung selbst (S. 43); die Reihenfolge ist
     * H3-Spielverhalten. Die Gleichstandsregel gilt hier genauso.
     */
    private void runLatePhase(List<Stack> waiting, BattleSetup setup, TurnOrder order, Set<Side> heroesCast,
                              List<Stack> roundStacks) {
        List<Stack> pending = new ArrayList<>(waiting);
        while (!pending.isEmpty()) {
            Stack activeStack = order.next(pending, false);
            if (activeStack.isAbleToAct() && setup.bothAlive()) {
                order.acted(activeStack);
                heroCasts(setup, activeStack, heroesCast);
                joinRound(pending, roundStacks);
                if (activeStack.isAbleToAct() && setup.bothAlive()) {
                    actWithMorale(activeStack, setup);
                }
                updateGate(setup);
            }
            BattleLogger.logShortDelimiter();
        }
    }

    /**
     * Was zu Beginn des Zugs passiert, bevor der Stack wählt: Enchanters zaubern, dann Furcht vor
     * Azure Dragons und der Wurf auf schlechte Moral.
     *
     * @return {@code true}, wenn der Stack seine Aktion verliert oder der Kampf vorbei ist
     */
    private boolean losesTurnAtStart(Stack activeStack, BattleSetup setup) {
        if (activeStack.hasSpeciality(UnitSpeciality.ENCHANTER_SPELLS) && CreatureSpellcasting.enchanterCastsIn(round)
                && !CreatureSpellcasting.blocked(setup)) {
            enchant(activeStack, setup);
            if (!setup.bothAlive()) {
                return true;
            }
        }
        if (fearsAzureDragons(activeStack, setup) && rng.nextInt(100) < 10) {
            BattleLogger.logFear(activeStack.getName());
            events.emit(new BattleEvent.Fear(activeStack.side(), activeStack.slot()));
            return true;
        }
        if (activeStack.freezes(rng)) {
            BattleLogger.logBadMorale(activeStack.getName());
            events.emit(new BattleEvent.BadMorale(activeStack.side(), activeStack.slot()));
            return true;
        }
        return false;
    }

    /** Steht auf der Gegenseite ein Azure Dragon, dem dieser Stack nicht widersteht? */
    private static boolean fearsAzureDragons(Stack active, BattleSetup setup) {
        if (active.isWarMachine() || active.unit().isUndead() || active.hasSpeciality(UnitSpeciality.FEAR)) {
            return false;
        }
        return setup.opponentsOf(active).stream()
                .anyMatch(s -> s.isAlive() && s.hasSpeciality(UnitSpeciality.FEAR));
    }

    /**
     * Reguläre Aktion eines Stacks, eingerahmt von den beiden Moral-Würfen (Manual S. 43):
     * negative Moral kann die Aktion davor kosten, positive kann eine zweite dahinter setzen.
     *
     * <p>Beide Würfe passieren bewusst erst nach ihren Vorbedingungen — sie ziehen nur dann
     * einen Zufallswert, wenn die Moral überhaupt in die jeweilige Richtung zeigt. So hält
     * eine Armee mit Moral 0 den RNG-Strom bei gleichem Seed identisch.
     */
    private void actWithMorale(Stack activeStack, BattleSetup setup) {
        Battlefield battlefield = setup.battlefieldFor(activeStack.side());
        // Der Freeze-Wurf gehört vor die Entscheidung, nicht dahinter: in H3 friert die Kreatur
        // ein, bevor der Spieler überhaupt zwischen Angriff, Wait und Defend wählen darf.
        // Wer in Phase 1 gewartet hat, hat dort bereits gewürfelt — in der Late-Phase würde ein
        // zweiter Wurf dem Warten eine doppelte Einfrier-Chance aufbürden.
        if (!activeStack.hasWaitedThisTurn() && losesTurnAtStart(activeStack, setup)) {
            return;
        }
        Siege siege = setup.siege();
        if (siege != null && !activeStack.hasWaitedThisTurn() && siege.isMoat(activeStack.position())) {
            // Wer seinen Zug im Graben beginnt, nimmt erneut Schaden; wer aussetzt, nicht.
            hazard(activeStack, siege.moatDamage(), false);
            if (!activeStack.isAlive()) {
                return;
            }
        }
        Stack opponent = autoSolver.pickTarget(activeStack, setup.opponentsOf(activeStack), battlefield);
        if (opponent == null) {
            return;
        }
        if (!takeAction(activeStack, opponent, setup)) {
            // Wait ist keine Aktion, sondern deren Verschiebung. Der Moral-Wurf gehört in die
            // Late-Phase, in der der Stack tatsächlich handelt — und er darf hier auch keinen
            // RNG ziehen, sonst verschiebt schon das bloße Warten den Zufallsstrom.
            return;
        }
        if (!activeStack.isAbleToAct() || !opponent.isAlive() || !activeStack.hasGoodMorale(rng)) {
            return;
        }
        BattleLogger.logGoodMorale(activeStack.getName());
        events.emit(new BattleEvent.GoodMorale(activeStack.side(), activeStack.slot()));
        Stack moralOpponent = opponent.isAlive() ? opponent
                : autoSolver.pickTarget(activeStack, setup.opponentsOf(activeStack), battlefield);
        if (moralOpponent != null) {
            takeAction(activeStack, moralOpponent, setup);
        }
    }

    /**
     * @return {@code false}, wenn der Stack seine Aktion nur verschoben hat (Wait) und deshalb in
     *         der Late-Phase erneut drankommt; {@code true}, wenn er sie verbraucht hat.
     */
    private boolean takeAction(Stack active, Stack opponent, BattleSetup setup) {
        if (faerieCasts(active, opponent, setup)) {
            active.removeSpells(s -> s == Spell.FRENZY);
            return true;
        }
        Battlefield battlefield = setup.battlefieldFor(active.side());
        Action action = autoSolver.decide(active, opponent, battlefield);
        if ((action instanceof Action.Move || action instanceof Action.Defend) && autoSolver.attacksWarMachines(active)) {
            Action machineAttack = WarMachineTargeting.attack(active, setup);
            if (machineAttack != null) {
                action = machineAttack;
            }
        }
        if (active.isBound()) {
            action = rooted(active, action);
        }
        switch (action) {
            case Action.Wait() -> {
                if (active.hasWaitedThisTurn()) {
                    // Zweites Wait in derselben Runde: die Verzögerung ist verbraucht, sonst
                    // könnte sich ein Stack endlos nach hinten schieben. Defend statt die
                    // Aktion ersatzlos zu verlieren — er bekommt wenigstens +20 % Defense.
                    emitDefend(active);
                } else {
                    emitWait(active);
                    return false;
                }
            }
            case Action.Defend() -> emitDefend(active);
            case Action.Move(Hex destination) -> {
                if (isHexBlocked(destination, active, setup)) {
                    // Solver hat einen besetzten Hex gewählt (z.B. Flieger straight-line gegen
                    // Tank-Wall). Defensiv: keine Doppelbelegung, fallback zu Defend — H3-
                    // konformer als Wait, weil der Stack damit wenigstens +20 % Defense bekommt
                    // statt seine Aktion ersatzlos zu verlieren.
                    emitDefend(active);
                } else {
                    moveTo(active, destination, battlefield);
                }
            }
            case Action.MoveAndMelee(Hex destination, Stack target) ->
                    moveAndMelee(active, destination, target, setup);
            case Action.Melee(Stack target) -> meleeAttack(active, target, 0, setup);
            case Action.Shoot(Stack target) -> shootCreatureOrWall(active, target, setup);
            case Action.Resurrect(Stack target) -> resurrect(active, target, setup);
            case Action.RaiseDemons(Stack corpse) -> raiseDemons(active, corpse, setup);
            case Action.CastSpell(Spell spell, Stack target) -> castSpell(active, spell, target, setup);
            case Action.CastRandomSpell() -> castRandomSpell(active, setup);
        }
        // Frenzy hält „until target troop's next action" (Manual S. 75).
        active.removeSpells(spell -> spell == Spell.FRENZY);
        return true;
    }

    /**
     * Ogre Magi und Elementare: ihr fester Zauber auf einen verbündeten Stack (Advanced, 6 Runden). Ein ungültiger
     * Auftrag fällt auf Defend zurück, wie bei {@link #resurrect}.
     */
    private void castSpell(Stack caster, Spell spell, Stack target, BattleSetup setup) {
        boolean allowed = caster.spellCastsLeft() > 0
                && spell == CreatureSpellcasting.fixedSpell(caster.unit())
                && CreatureSpellcasting.usefulTargets(caster, spell, setup).contains(target);
        if (!allowed) {
            emitDefend(caster);
            return;
        }
        applyCreatureSpell(caster, new CreatureSpellcasting.Cast(spell, target), setup);
    }

    /** Master Genie: Zauber und Ziel per Zufall unter den sinnvollen (CreatureSpellcasting). */
    private void castRandomSpell(Stack caster, BattleSetup setup) {
        CreatureSpellcasting.Cast cast = caster.spellCastsLeft() > 0
                && caster.hasSpeciality(UnitSpeciality.CASTS_RANDOM_BENEFICIAL)
                ? CreatureSpellcasting.randomCast(caster, setup, rng) : null;
        if (cast == null) {
            emitDefend(caster);
            return;
        }
        applyCreatureSpell(caster, cast, setup);
    }

    /**
     * Faerie Dragons greifen mit Zaubern an, solange sie Würfe haben; ist das Ziel gegen den
     * vorbereiteten immun, handeln sie normal, also im Nahkampf (heroes.thelazy.net).
     */
    private boolean faerieCasts(Stack active, Stack opponent, BattleSetup setup) {
        if (!active.hasSpeciality(UnitSpeciality.FAERIE_SPELLS) || active.spellCastsLeft() == 0
                || CreatureSpellcasting.blocked(setup)) {
            return false;
        }
        Spell spell = CreatureSpellcasting.faerieSpell(rng);
        if (SpellResistance.isImmune(opponent, spell)) {
            return false;
        }
        faerieCast(active, spell, opponent, setup);
        return true;
    }

    /**
     * Angriffszauber eines Faerie-Dragon-Stacks: Advanced mit Power 5 je Drache, ohne Sorcery und
     * Spezialität. Einzelziele kann Magic Mirror umlenken, Flächen gehen auf den besten Mittelpunkt
     * um das Ziel, Chain Lightning springt wie beim Helden.
     */
    private void faerieCast(Stack caster, Spell spell, Stack target, BattleSetup setup) {
        caster.useSpellCast();
        int power = CreatureSpellcasting.FAERIE_POWER_PER_DRAGON * caster.getCount();
        int base = SpellDamage.base(spell, CreatureSpellcasting.level(CreatureSpellcasting.FAERIE_LEVEL, setup), power);
        List<Stack> affected = switch (spell) {
            case FIREBALL, FROST_RING, INFERNO, METEOR_SHOWER -> HeroSpellcasting.inArea(spell,
                    CreatureSpellcasting.areaCenter(caster, spell, target, setup), setup);
            case CHAIN_LIGHTNING -> HeroSpellcasting.chain(target, 5, setup);
            default -> List.of(target);
        };
        BattleLogger.logSpell(caster.getName(), spell, target.getName());
        events.emit(new BattleEvent.SpellCast(caster.side(), caster.slot(), spell,
                target.side(), target.slot(), false, snapshot(target)));
        for (int i = 0; i < affected.size(); i++) {
            Stack aimed = affected.get(i);
            if (!aimed.isAlive()) {
                continue;
            }
            Stack reflectedTo = mirrored(setup, caster.side(), spell, aimed);
            Stack hit = reflectedTo == null ? aimed : reflectedTo;
            boolean works = SpellResistance.takesEffect(spell, hit, setup, rng);
            int damage = 0;
            int killed = 0;
            if (works) {
                damage = SpellDamage.toTarget(spell, spell == Spell.CHAIN_LIGHTNING ? base >> i : base, null, hit);
                int countBefore = hit.getCount();
                boolean rebirthUsedBefore = hit.isRebirthUsed();
                hit.takeDamage(damage, Set.of());
                killed = Math.max(0, countBefore - hit.getCount());
                emitRebirthIfFired(hit, rebirthUsedBefore);
                setup.machineHit(hit);
            }
            events.emit(new BattleEvent.SpellHit(caster.side(), spell, hit.side(), hit.slot(), damage, killed, 0,
                    !works, reflectedTo != null, snapshot(hit)));
        }
    }

    /**
     * Enchanters zu Beginn ihres Zugs in jeder dritten Runde: ein zufälliger Massenzauber auf
     * Expert, drei Runden lang — nützliche auf alle eigenen, Slow und Weakness auf alle gegnerischen
     * Stacks. Verbraucht die Aktion nicht.
     */
    private void enchant(Stack caster, BattleSetup setup) {
        Spell spell = CreatureSpellcasting.enchanterSpell(caster, setup, rng);
        List<Stack> targets = (spell.beneficial() ? setup.stacksOf(caster.side()) : setup.opponentsOf(caster))
                .stream().filter(Stack::isAlive).toList();
        BattleLogger.logSpell(caster.getName(), spell, "alle");
        for (Stack target : targets) {
            boolean works = SpellResistance.takesEffect(spell, target, setup, rng);
            if (works && spell == Spell.CURE) {
                target.removeSpells(s -> !s.beneficial());
                target.healTop(CreatureSpellcasting.ENCHANTER_POWER * 5 + 30);
            } else if (works) {
                target.applySpell(spell, CreatureSpellcasting.ENCHANTER_LEVEL, CreatureSpellcasting.ENCHANTER_ROUNDS);
            }
            events.emit(new BattleEvent.SpellCast(caster.side(), caster.slot(), spell,
                    target.side(), target.slot(), !works, snapshot(target)));
        }
    }

    private void applyCreatureSpell(Stack caster, CreatureSpellcasting.Cast cast, BattleSetup setup) {
        Stack target = cast.target();
        Spell spell = cast.spell();
        caster.useSpellCast();
        if (spell == Spell.CURE) {
            // Cure (Manual S. 76): hebt negative Zauber auf und heilt (Power × 5) + 20 auf Advanced.
            target.removeSpells(s -> !s.beneficial());
            target.healTop(CreatureSpellcasting.SPELL_POWER * 5 + 20);
        } else {
            int rounds = spell == Spell.FRENZY ? Integer.MAX_VALUE : CreatureSpellcasting.SPELL_POWER;
            target.applySpell(spell, CreatureSpellcasting.level(CreatureSpellcasting.CAST_LEVEL, setup), rounds);
        }
        BattleLogger.logSpell(caster.getName(), spell, target.getName());
        events.emit(new BattleEvent.SpellCast(caster.side(), caster.slot(), spell,
                target.side(), target.slot(), false, snapshot(target)));
    }

    /**
     * Pit Lord: beschwört aus einem ganz gefallenen verbündeten Stack Dämonen (Manual S. 98).
     * Verbraucht die Aktion. Ein ungültiger Leichnam oder ein zweiter Versuch fällt auf Defend
     * zurück, wie bei {@link #resurrect}.
     */
    private void raiseDemons(Stack caster, Stack corpse, BattleSetup setup) {
        if (!caster.canRaiseDemons() || !DemonRaising.isValidCorpse(caster, corpse, setup)) {
            emitDefend(caster);
            return;
        }
        int demons = DemonRaising.demonsFrom(caster, corpse);
        String fallen = corpse.getName();
        corpse.riseAs(UnitCatalog.DEMON, demons);
        caster.markDemonsRaised();
        BattleLogger.logRaiseDemons(caster.getName(), fallen, demons);
        events.emit(new BattleEvent.RaiseDemons(caster.side(), caster.slot(),
                corpse.side(), corpse.slot(), demons, snapshot(corpse)));
    }

    /**
     * Arch Angel: Resurrection auf einen verbündeten Stack (Manual S. 87). Verbraucht die Aktion
     * des Arch Angels. Ein Ziel, das die Regeln nicht erlauben, oder ein zweiter Versuch im
     * selben Kampf fällt auf Defend zurück — Solver-Bug-Sicherheitsnetz wie bei {@code shoot}.
     */
    private void resurrect(Stack caster, Stack target, BattleSetup setup) {
        if (!caster.canResurrect() || !Resurrection.isValidTarget(caster, target, setup)) {
            emitDefend(caster);
            return;
        }
        int before = target.getCount();
        target.heal(Resurrection.healthFor(caster));
        caster.markResurrectionUsed();
        int restored = target.getCount() - before;
        BattleLogger.logResurrection(caster.getName(), target.getName(), restored);
        events.emit(new BattleEvent.Resurrection(caster.side(), caster.slot(),
                target.side(), target.slot(), restored, snapshot(target)));
    }

    private void moveAndMelee(Stack active, Hex destination, Stack target, BattleSetup setup) {
        if (isHexBlocked(destination, active, setup)) {
            emitDefend(active);
            return;
        }
        Battlefield battlefield = setup.battlefieldFor(active.side());
        Hex startPos = active.position();
        moveTo(active, destination, battlefield);
        if (!active.isAlive() || active.distanceTo(target) != 1) {
            // Im Graben stecken geblieben oder auf dem Weg gefallen: kein Angriff mehr.
            return;
        }
        int hexesMoved = startPos.distanceTo(active.position());
        meleeAttack(active, target, hexesMoved, setup);
        if (active.hasSpeciality(UnitSpeciality.MOVE_BACK) && active.isAlive() && !active.isBound()) {
            BattleLogger.logMoveBack(active.getName(), startPos.q(), startPos.r());
            List<HexCoord> backPath = battlefield.pathFor(active, startPos).stream()
                    .map(h -> new HexCoord(h.q(), h.r())).toList();
            active.moveTo(startPos);
            events.emit(new BattleEvent.MoveBack(active.side(), active.slot(),
                    startPos.q(), startPos.r(), backPath));
        }
    }

    /**
     * Ein gefesselter Stack bewegt sich nicht: statt eines Zugs verteidigt er, einen Angriff führt
     * er nur auf einen Gegner aus, der schon neben ihm steht.
     */
    static Action rooted(Stack active, Action action) {
        return switch (action) {
            case Action.Move _ -> new Action.Defend();
            case Action.MoveAndMelee charge ->
                    active.distanceTo(charge.target()) == 1 ? new Action.Melee(charge.target()) : new Action.Defend();
            default -> action;
        };
    }

    /** Ein Zyklop des Angreifers darf statt der Kreatur die Befestigung beschießen ({@link WallShots}). */
    private void shootCreatureOrWall(Stack active, Stack target, BattleSetup setup) {
        if (WallShots.possible(active, setup) && !hasAdjacentEnemy(active, setup)
                && autoSolver.shootsWalls(active, setup)) {
            wallShots(active, Objects.requireNonNull(setup.siege()));
        } else {
            shoot(active, target, setup);
        }
    }

    private void shoot(Stack active, Stack target, BattleSetup setup) {
        if (!active.canShoot()) {
            // Keine Shots übrig (oder gar keine ranged Attacke) — Solver-Bug-
            // Sicherheitsnetz: kein Phantom-Schuss, Engine fällt zu Defend.
            emitDefend(active);
        } else if (hasAdjacentEnemy(active, setup)) {
            // Manual S. 42: „Creatures with ranged attacks ... can fire only when
            // there are no adjacent enemies." Engaged Schütze kann nicht schießen
            // → defensiv Defend (Engine-Sicherheitsnetz; Solver wählt den Pfad
            // eigentlich nicht, weil pickTarget Adjacent-Engagement priorisiert).
            emitDefend(active);
        } else {
            rangedAttack(active, target, setup);
        }
    }

    private static boolean hasAdjacentEnemy(Stack active, BattleSetup setup) {
        for (Stack o : setup.opponentsOf(active)) {
            if (o.isAlive() && active.distanceTo(o) == 1) {
                return true;
            }
        }
        return false;
    }

    private void emitWait(Stack active) {
        active.markWaited();
        BattleLogger.logWait(active.getName());
        events.emit(new BattleEvent.Wait(active.side(), active.slot()));
    }

    private void emitDefend(Stack active) {
        active.defend();
        BattleLogger.logDefend(active.getName());
        events.emit(new BattleEvent.Defend(active.side(), active.slot()));
    }

    /** Kann {@code mover} mit dem Kopf nicht auf {@code hex} stehen — bei zweifeldrigen auch das Heck? */
    private static boolean isHexBlocked(Hex hex, Stack mover, BattleSetup setup) {
        return !setup.fits(mover, hex);
    }

    /**
     * Bewegt den Stack. Bei einer Belagerung hält der Graben Bodentruppen auf dem ersten Grabenfeld
     * des Wegs an (heroes.thelazy.net, <em>Moat</em>); danach folgen Graben- und Minenschaden.
     */
    private void moveTo(Stack active, Hex destination, Battlefield battlefield) {
        Hex from = active.position();
        Siege siege = currentSetup == null ? null : currentSetup.siege();
        boolean ground = active.unit().movement() == Movement.GROUND;
        List<Hex> walked = battlefield.pathFor(active, destination);
        if (siege != null && ground) {
            walked = aroundMoat(active, destination, battlefield, walked, siege);
            walked = untilMoat(walked, siege);
            if (currentSetup != null && stuckWithoutRoom(currentSetup, active, walked, destination)) {
                return;
            }
            walked = currentSetup == null ? walked : whereItFits(currentSetup, active, walked, destination);
        }
        Hex end = walked.isEmpty() ? destination : walked.getLast();
        List<HexCoord> path = walked.stream().map(h -> new HexCoord(h.q(), h.r())).toList();
        BattleLogger.logMove(active.getName(), from.q(), from.r(), end.q(), end.r());
        active.moveTo(end);
        events.emit(new BattleEvent.Move(active.side(), active.slot(),
                from.q(), from.r(), end.q(), end.r(), path));
        if (siege != null) {
            siegeHazards(active, ground ? walked : List.of(end), siege);
        }
    }

    /**
     * Bleibt ein Stack vor dem Ziel im Graben stehen, braucht auch sein Heck dort Platz: der Weg
     * endet am letzten Feld davor, auf dem er passt.
     */
    static List<Hex> whereItFits(BattleSetup setup, Stack active, List<Hex> walked, Hex destination) {
        List<Hex> path = walked;
        while (!path.isEmpty() && !path.getLast().equals(destination) && !setup.fits(active, path.getLast())) {
            path = path.subList(0, path.size() - 1);
        }
        return path;
    }

    /** Passt der Stack auf keinem Feld des gekürzten Wegs, bleibt er stehen. */
    static boolean stuckWithoutRoom(BattleSetup setup, Stack active, List<Hex> walked, Hex destination) {
        return !walked.isEmpty() && !walked.getLast().equals(destination)
                && whereItFits(setup, active, walked, destination).isEmpty();
    }

    /**
     * Ein Grabenfeld hält an (VCMI: „Stopper“) — gibt es zum Ziel einen Weg ohne Graben, den der
     * Stack in diesem Zug schafft, nimmt er ihn, etwa durch das offene Tor.
     */
    static List<Hex> aroundMoat(Stack active, Hex destination, Battlefield battlefield, List<Hex> path,
                                        Siege siege) {
        if (path.stream().noneMatch(siege::isMoat) || siege.isMoat(destination)) {
            return path;
        }
        Set<Hex> blocked = new HashSet<>(battlefield.obstacles());
        siege.moat().stream().filter(siege::isMoat).forEach(blocked::add);
        List<Hex> dry = battlefield.withObstacles(blocked).pathFor(active, destination);
        return !dry.isEmpty() && dry.size() <= active.getSpeed() ? dry : path;
    }

    /** Der Weg bis einschließlich des ersten Grabenfelds. */
    private static List<Hex> untilMoat(List<Hex> path, Siege siege) {
        for (int i = 0; i < path.size(); i++) {
            if (siege.isMoat(path.get(i))) {
                return path.subList(0, i + 1);
            }
        }
        return path;
    }

    /** Minen auf den betretenen Feldern (nur Angreifer), dann der Graben am Ziel. */
    private void siegeHazards(Stack active, List<Hex> entered, Siege siege) {
        for (Hex hex : entered) {
            if (active.side() == Side.ATTACKER && active.isAlive() && siege.triggerMine(hex)) {
                hazard(active, mineDamage(active), true);
            }
        }
        if (active.isAlive() && siege.isMoat(active.position())) {
            hazard(active, siege.moatDamage(), false);
        }
    }

    /**
     * Landmine von Tower (SoD): 10 × Power des verteidigenden Helden plus 25/50/100 nach Fire
     * Magic, mindestens 150. Gegen Feuer- und Zauberimmune wirkungslos.
     */
    private int mineDamage(Stack target) {
        if (SpellResistance.isImmune(target, Spell.LAND_MINE)) {
            return 0;
        }
        Hero defender = currentSetup == null ? null : currentSetup.heroOf(Side.DEFENDER);
        if (defender == null) {
            return 150;
        }
        int fire = SpellDamage.byLevel(defender.levelOf(SecondarySkill.FIRE_MAGIC), 25, 50, 100);
        int bonus = defender.levelOf(SecondarySkill.FIRE_MAGIC) == SkillLevel.NONE ? 0 : fire;
        return Math.max(150, 10 * defender.spellPower() + bonus);
    }

    /** Im Graben verliert ein Stack 3 Defense (heroes.thelazy.net, <em>Moat</em>). */
    private int inMoat(Stack stack, int defense) {
        Siege siege = currentSetup == null ? null : currentSetup.siege();
        return siege != null && siege.isMoat(stack.position()) ? Math.max(0, defense - 3) : defense;
    }

    private void hazard(Stack target, int damage, boolean mine) {
        if (damage <= 0) {
            return;
        }
        int countBefore = target.getCount();
        boolean rebirthUsedBefore = target.isRebirthUsed();
        target.takeDamage(damage, Set.of());
        emitRebirthIfFired(target, rebirthUsedBefore);
        events.emit(new BattleEvent.MoatDamage(target.side(), target.slot(), damage,
                Math.max(0, countBefore - target.getCount()), mine, snapshot(target)));
    }

    private void meleeAttack(Stack active, Stack passive, int hexesMoved, BattleSetup setup) {
        int countBeforeFirst = passive.getCount();
        int retaliationPercent = passive.wakeFromBlind();
        int dealt = dealDamage(active, passive, AttackType.HAND_TO_HAND, hexesMoved, 0, false);
        events.emit(new BattleEvent.Melee(active.side(), active.slot(),
                passive.side(), passive.slot(), hexesMoved,
                dealt, countBeforeFirst - passive.getCount(), snapshot(passive)));
        applyFireShield(active, passive, dealt);
        applyMeleeSplash(active, passive, setup);
        applyLifeDrain(active, passive, dealt);
        triggerRetaliation(active, passive, retaliationPercent);
        if (active.hasSpeciality(UnitSpeciality.TWO_BLOWS) && passive.isAlive() && active.isAlive()) {
            BattleLogger.logTwoBlows(active.getName());
            events.emit(new BattleEvent.TwoBlows(active.side(), active.slot()));
            int countBeforeSecond = passive.getCount();
            // Second blow does not gain Jousting-Bonus — kein erneutes Anfahren.
            int dealtSecond = dealDamage(active, passive, AttackType.HAND_TO_HAND, 0, 0, false);
            events.emit(new BattleEvent.Melee(active.side(), active.slot(),
                    passive.side(), passive.slot(), 0,
                    dealtSecond, countBeforeSecond - passive.getCount(), snapshot(passive)));
            applyFireShield(active, passive, dealtSecond);
            applyLifeDrain(active, passive, dealtSecond);
            triggerRetaliation(active, passive, 100);
        }
        if (passive.isWarMachine()) {
            setup.machineHit(passive);
        }
    }

    /** Maschinen sind gegen Life Drain immun (heroes.thelazy.net, <em>War Machine</em>). */
    private void applyLifeDrain(Stack active, Stack passive, int dealt) {
        if (dealt <= 0 || passive.isWarMachine() || !active.isAlive()
                || !active.hasSpeciality(UnitSpeciality.LIFE_DRAIN)) {
            return;
        }
        int healed = active.heal(dealt);
        if (healed > 0) {
            BattleLogger.logLifeDrain(active.getName(), healed);
        }
    }

    /**
     * Multi-Stack-Splash für Nahkampf: Three-Headed-Attack (Cerberus) trifft bis zu zwei
     * weitere adjazente Stacks; Fire-Breath (Green/Gold/Red/Black-Dragon) trifft den
     * Stack auf dem inline-Hex direkt hinter dem Hauptziel. Splash-Hits triggern keine
     * Retaliation und keine After-Attack-Procs — sie sind reine Collateral-Hits.
     *
     * <p>Three-Headed Attack trifft nur Gegner, und zwar auf den beiden Feldern links und rechts
     * des Ziels aus der Angriffsrichtung — den gemeinsamen Nachbarn von Cerberus und Ziel.
     * <strong>Fire Breath</strong> trifft dagegen auch eigene Stacks auf dem Feld dahinter.
     */
    private void applyMeleeSplash(Stack active, Stack primary, BattleSetup setup) {
        Hex[] contact = closestPair(active, primary);
        if (active.hasSpeciality(UnitSpeciality.THREE_HEADED_ATTACK)) {
            threeHeaded(active, contact, setup);
        }
        if (active.hasSpeciality(UnitSpeciality.ATTACKS_ALL_ADJACENT)) {
            for (Stack enemy : adjacentEnemies(active, primary, setup)) {
                applySplashHit(active, enemy, AttackType.HAND_TO_HAND);
            }
        }
        if (active.hasSpeciality(UnitSpeciality.FIRE_BREATH)) {
            fireBreath(active, primary, contact, setup);
        }
    }

    /** Die beiden Flanken zwischen Cerberus und Ziel, nur Gegner. */
    private void threeHeaded(Stack active, Hex[] contact, BattleSetup setup) {
        for (Hex flank : contact[0].commonNeighbors(contact[1])) {
            Stack candidate = findStackAt(active, flank, setup);
            if (candidate != null && candidate.isAlive() && candidate.side() != active.side()) {
                applySplashHit(active, candidate, AttackType.HAND_TO_HAND);
            }
        }
    }

    /** Der Stack auf dem Feld hinter dem Ziel, eigene eingeschlossen. */
    // collateral != primary: Identitätsvergleich auf der mutable Stack-Entity (kein equals).
    @SuppressWarnings("ReferenceEquality")
    private void fireBreath(Stack active, Stack primary, Hex[] contact, BattleSetup setup) {
        Hex behind = behindHex(contact[0], contact[1]);
        if (primary.occupies(behind)) {
            // Hinter dem getroffenen Feld liegt das Heck des Ziels: der Atem geht ein Feld weiter.
            behind = behindHex(contact[1], behind);
        }
        Stack collateral = findStackAt(active, behind, setup);
        if (collateral != null && collateral != primary && collateral.isAlive()) {
            applySplashHit(active, collateral, AttackType.HAND_TO_HAND);
        }
    }

    /**
     * Alle anderen Gegner neben dem Angreifer, auch Kriegsmaschinen — für Hydra und die
     * Psychic/Magic Elementals, die alle benachbarten Gegner treffen.
     */
    // s != primary: Identitätsvergleich auf der mutable Stack-Entity.
    @SuppressWarnings("ReferenceEquality")
    private static List<Stack> adjacentEnemies(Stack active, Stack primary, BattleSetup setup) {
        List<Stack> enemies = new ArrayList<>(setup.opponentsOf(active));
        enemies.addAll(setup.enemyMachinesOf(active));
        return enemies.stream()
                .filter(s -> s != primary && s.isAlive() && active.distanceTo(s) == 1)
                .toList();
    }

    /**
     * Die beiden Felder, an denen sich zwei Stacks am nächsten kommen, Angreifer zuerst; bei
     * Gleichstand die Köpfe.
     */
    private static Hex[] closestPair(Stack a, Stack b) {
        Hex[] best = {a.position(), b.position()};
        for (Hex ha : a.hexes()) {
            for (Hex hb : b.hexes()) {
                if (ha.distanceTo(hb) < best[0].distanceTo(best[1])) {
                    best = new Hex[] {ha, hb};
                }
            }
        }
        return best;
    }

    private static Hex behindHex(Hex from, Hex through) {
        return new Hex(through.q() + (through.q() - from.q()),
                through.r() + (through.r() - from.r()));
    }

    /**
     * Liefert irgendeinen lebenden Stack auf {@code hex} (egal welche Seite, außer
     * {@code active} selbst), auch eine stehende Kriegsmaschine. Für H3-Friendly-Fire-Splash-
     * Mechaniken; Splash trifft Maschinen (heroes.thelazy.net, <em>War Machine</em>).
     */
    // s != active: Identitätsvergleich auf der mutable Stack-Entity (kein equals).
    @SuppressWarnings("ReferenceEquality")
    private @Nullable Stack findStackAt(Stack active, Hex hex, BattleSetup setup) {
        for (Stack s : setup.attackerStacks()) {
            if (s != active && s.isAlive() && s.occupies(hex)) {
                return s;
            }
        }
        for (Stack s : setup.defenderStacks()) {
            if (s != active && s.isAlive() && s.occupies(hex)) {
                return s;
            }
        }
        return setup.machineAt(hex);
    }

    private void applySplashHit(Stack active, Stack secondary, AttackType attackType) {
        secondary.wakeFromBlind();
        int currentDamage = active.calculateCurrentDamage(attackType, 0, rng);
        int effectiveDefense = inMoat(secondary, secondary.effectiveDefenseAgainst(active.getAttackerSpecialities()));
        int boniMaliPercentage = active.calculateAttackBoniMaliPercentage(secondary, attackType, effectiveDefense);
        // Splash-Treffer sind normale Angriffe und bekommen dieselben Helden-Modifikatoren
        // wie das Hauptziel — sonst würde ein Cerberus mit Offense-Held nur beim Primary
        // profitieren und bei den Kollateralen nicht.
        int realDamage = applyHeroCombatSkills(active, secondary, attackType,
                (currentDamage * (100 + boniMaliPercentage)) / 100)
                * secondary.incomingDamagePercent(attackType) / 100;
        int countBefore = secondary.getCount();
        BattleLogger.logAttack(active.getName(), secondary.getName());
        secondary.takeDamage(realDamage, active.getAttackerSpecialities());
        if (currentSetup != null) {
            currentSetup.machineHit(secondary);
        }
        int killed = countBefore - secondary.getCount();
        if (attackType == AttackType.HAND_TO_HAND) {
            events.emit(new BattleEvent.Melee(active.side(), active.slot(),
                    secondary.side(), secondary.slot(), 0, realDamage, killed, snapshot(secondary)));
        } else {
            int distance = active.distanceTo(secondary);
            events.emit(new BattleEvent.Shoot(active.side(), active.slot(),
                    secondary.side(), secondary.slot(), distance, realDamage, killed, snapshot(secondary)));
        }
    }

    /**
     * Schadens-Modifikatoren der beteiligten Helden, angewandt auf den bereits fertig
     * berechneten Schaden. Das Manual formuliert alle drei als Prozent auf den
     * <em>zugefügten</em> Schaden — also auf das Ergebnis der Formel von S. 43, nicht auf
     * den Würfelwurf davor. Offense (Nahkampf) und Archery (Fernkampf) schließen einander
     * aus, Armorer des Verteidigers greift danach.
     *
     * <p>Bewusst nicht erfasst: Fire Shield und Thunderbolts. Beides sind keine Angriffe
     * des Stacks, sondern feste Reflexions- bzw. Proc-Schäden — Offense und Archery haben
     * dort keinen Anknüpfungspunkt.
     */
    private static int applyHeroCombatSkills(Stack active, Stack passive, AttackType attackType,
                                             int damage) {
        int result = damage;
        Hero attackerHero = active.commander();
        if (attackerHero != null) {
            int bonus = attackType == AttackType.HAND_TO_HAND
                    ? attackerHero.offenseBonusPerMille()
                    : attackerHero.archeryBonusPerMille();
            result = (result * (1000 + bonus)) / 1000;
        }
        Hero defenderHero = passive.commander();
        if (defenderHero != null) {
            result = (result * (1000 - defenderHero.armorerReductionPerMille())) / 1000;
        }
        return result;
    }

    private void applyFireShield(Stack active, Stack passive, int damageDealt) {
        if (damageDealt <= 0 || !active.isAlive()) {
            return;
        }
        int reverse = passive.fireShieldDamageFor(damageDealt);
        if (reverse <= 0) {
            return;
        }
        BattleLogger.logFireShield(passive.getName(), active.getName(), reverse);
        active.takeDamage(reverse, Set.of());
        events.emit(new BattleEvent.FireShield(passive.side(), passive.slot(),
                active.side(), active.slot(), reverse, snapshot(active)));
    }

    /**
     * @param attackPercent Attack des Zurückschlagenden in Prozent — unter 100 nur, wenn der Angriff
     *                      ihn eben aus Blind geweckt hat (0 = gar kein Gegenschlag)
     */
    private void triggerRetaliation(Stack active, Stack passive, int attackPercent) {
        if (attackPercent == 0) {
            return;
        }
        if (active.hasSpeciality(UnitSpeciality.NO_RETALIATION)) {
            BattleLogger.logImmuneToRetaliation(active.getName());
            return;
        }
        if (!passive.isAbleToAct() || passive.distanceTo(active) != 1) {
            return;
        }
        if (!passive.canRetaliate()) {
            return;
        }
        BattleLogger.logRetaliation(passive.getName());
        passive.recordRetaliation();
        int countBefore = active.getCount();
        // Retaliation kehrt aktiv/passiv bewusst um — passive schlaegt zurueck.
        passive.setAttackPercent(attackPercent);
        int dealt = dealDamage(/* active= */ passive, /* passive= */ active, AttackType.HAND_TO_HAND, 0, 0, false);
        passive.setAttackPercent(100);
        events.emit(new BattleEvent.Retaliation(passive.side(), passive.slot(),
                active.side(), active.slot(),
                dealt, countBefore - active.getCount(), snapshot(active)));
    }

    private void rangedAttack(Stack active, Stack passive, BattleSetup setup) {
        int distance = active.distanceTo(passive);
        boolean behindWall = setup.shotCrossesWall(active, passive);
        BattleLogger.logShoot(active.getName(), passive.getName(), distance);
        int countBefore = passive.getCount();
        passive.wakeFromBlind();
        int dealt = dealDamage(active, passive, AttackType.LONG_RANGE, 0, distance, behindWall);
        events.emit(new BattleEvent.Shoot(active.side(), active.slot(),
                passive.side(), passive.slot(), distance,
                dealt, countBefore - passive.getCount(), snapshot(passive)));
        applyRangedSplash(active, passive, setup);
        active.useShot();
        if (active.hasSpeciality(UnitSpeciality.TWO_SHOTS) && active.canShoot() && passive.isAlive()) {
            BattleLogger.logTwoShots(active.getName());
            events.emit(new BattleEvent.TwoShots(active.side(), active.slot()));
            int distance2 = active.distanceTo(passive);
            boolean behindWall2 = setup.shotCrossesWall(active, passive);
            BattleLogger.logShoot(active.getName(), passive.getName(), distance2);
            int countBefore2 = passive.getCount();
            int dealt2 = dealDamage(active, passive, AttackType.LONG_RANGE, 0, distance2, behindWall2);
            events.emit(new BattleEvent.Shoot(active.side(), active.slot(),
                    passive.side(), passive.slot(), distance2,
                    dealt2, countBefore2 - passive.getCount(), snapshot(passive)));
            applyRangedSplash(active, passive, setup);
            active.useShot();
        }
        if (passive.isWarMachine()) {
            setup.machineHit(passive);
        }
    }

    /**
     * Multi-Stack-Splash für Fernkampf: SPLASH_SHOT (Magog, Manual S. 97) trifft alle Stacks
     * im 1-Hex-Radius um das Hauptziel; DEATH_CLOUD (Lich/Power Lich) trifft alle
     * <strong>non-undead</strong> Stacks im 1-Hex-Radius (Manual S. 101).
     *
     * <p><strong>Friendly Fire</strong>: H3-Splash trifft auch eigene Stacks im Radius
     * (außer Undead bei DEATH_CLOUD). Engine iteriert über {@link #findStackAt}, nicht
     * nur über Gegner.
     */
    private void applyRangedSplash(Stack active, Stack primary, BattleSetup setup) {
        if (active.hasSpeciality(UnitSpeciality.SPLASH_SHOT)) {
            applySplashShot(active, primary, setup);
        }
        if (active.hasSpeciality(UnitSpeciality.DEATH_CLOUD)) {
            applyDeathCloud(active, primary, setup);
        }
    }

    /**
     * SPLASH_SHOT: alle Stacks rund um das Hauptziel, wie ein Fireball — Undead inklusive.
     */
    private void applySplashShot(Stack active, Stack primary, BattleSetup setup) {
        for (Stack collateral : around(active, primary, setup)) {
            applySplashHit(active, collateral, AttackType.LONG_RANGE);
        }
    }

    /**
     * DEATH_CLOUD: alle non-undead Stacks im 1-Hex-Radius, ohne Stück-Limit, keine Kriegsmaschinen.
     */
    private void applyDeathCloud(Stack active, Stack primary, BattleSetup setup) {
        for (Stack collateral : around(active, primary, setup)) {
            // Kriegsmaschinen sind immun gegen die Todeswolke (heroes.thelazy.net, War Machine).
            if (!collateral.unit().isUndead() && !collateral.isWarMachine()) {
                applySplashHit(active, collateral, AttackType.LONG_RANGE);
            }
        }
    }

    /**
     * Die lebenden Stacks rund um das Hauptziel, jeder einmal — auch wenn ein zweifeldriger an
     * zwei Felder grenzt oder das Ziel selbst zwei Felder hat.
     */
    private List<Stack> around(Stack active, Stack primary, BattleSetup setup) {
        boolean leftward = Hex.isLeftward(active.position(), primary.position());
        Set<Stack> seen = new LinkedHashSet<>();
        for (Hex own : primary.hexes()) {
            for (Hex neighbor : own.neighbors(leftward)) {
                Stack collateral = collateralAt(active, neighbor, primary, setup);
                if (collateral != null) {
                    seen.add(collateral);
                }
            }
        }
        return List.copyOf(seen);
    }

    /**
     * Lebender Stack auf {@code hex}, sofern es nicht das Hauptziel selbst ist.
     */
    // collateral != primary: Identitätsvergleich auf der mutable Stack-Entity (kein equals).
    @SuppressWarnings("ReferenceEquality")
    private @Nullable Stack collateralAt(Stack active, Hex hex, Stack primary, BattleSetup setup) {
        Stack collateral = findStackAt(active, hex, setup);
        return collateral != null && collateral != primary && collateral.isAlive() ? collateral : null;
    }

    private int dealDamage(Stack active, Stack passive, AttackType attackType,
                           int hexesMoved, int distance, boolean behindWall) {
        // Pikeniere und Hellebardiere heben den Anlauf-Bonus auf.
        int runUp = passive.hasSpeciality(UnitSpeciality.JOUSTING_IMMUNE) ? 0 : hexesMoved;
        int currentDamage = active.calculateCurrentDamage(attackType, runUp, rng);
        if (attackType == AttackType.LONG_RANGE) {
            Set<UnitSpeciality> attackerSpecs = active.getAttackerSpecialities();
            if (distance > 10 && !attackerSpecs.contains(UnitSpeciality.NO_DISTANCE_PENALTY)) {
                currentDamage = currentDamage / 2;
                BattleLogger.logDistancePenalty(active.getName(), distance);
            }
            // Hindernis-Strafe nur über Stadtmauern (heroes.thelazy.net, Obstacle penalty).
            // NO_OBSTACLE_PENALTY ist keine Angriffs-, sondern eine Sonderfähigkeit — also am Stack prüfen.
            if (behindWall && !active.hasSpeciality(UnitSpeciality.NO_OBSTACLE_PENALTY)) {
                currentDamage = currentDamage / 2;
                BattleLogger.logObstaclePenalty(active.getName());
            }
        }
        int effectiveDefense = inMoat(passive, passive.effectiveDefenseAgainst(active.getAttackerSpecialities()));
        int boniMaliPercentage = active.calculateAttackBoniMaliPercentage(passive, attackType, effectiveDefense);
        int damagePercent = 100 + boniMaliPercentage;
        if (active.isLucky(rng)) {
            damagePercent = luckyDamagePercent(boniMaliPercentage);
            BattleLogger.logLuckyStrike(active.getName());
            events.emit(new BattleEvent.LuckyStrike(active.side(), active.slot()));
        }
        int realDamage = applyHeroCombatSkills(active, passive, attackType,
                (currentDamage * damagePercent) / 100)
                * passive.incomingDamagePercent(attackType) / 100;

        BattleLogger.logAttack(active.getName(), passive.getName());
        boolean rebirthUsedBefore = passive.isRebirthUsed();
        passive.takeDamage(realDamage, active.getAttackerSpecialities());
        emitRebirthIfFired(passive, rebirthUsedBefore);

        // Maschinen sind gegen die Fähigkeiten nach dem Treffer immun (Death Stare, Petrify, Poison …).
        if (passive.isAlive() && !passive.isWarMachine()) {
            doDeathStare(active, passive, attackType);
            doThunderbolts(active, passive, attackType);
            doPetrifying(active, passive);
            doCursing(active, passive);
            doAcidBreath(active, passive);
            doPoisoning(active, passive);
            doDiseasing(active, passive);
            doAging(active, passive);
            doCreatureSpellProcs(active, passive);
            doBinding(active, passive, attackType);
            if (passive.isAlive()) {
                BattleLogger.logRemainingHealth(passive.getName(), passive.getCurrentHealth());
            } else {
                BattleLogger.logLastUnitDead(passive.getName());
            }
        } else if (!passive.isAlive()) {
            BattleLogger.logLastUnitDead(passive.getName());
        }
        return realDamage;
    }

    /**
     * Schaden in Prozent bei einem Glückstreffer. Das Manual sagt „double damage" (S. 44); in der
     * SoD-Formel ist Glück ein Aufschlag von +100 % auf den Grundschaden, der sich mit dem
     * Attack-Bonus addiert, während ein Defense-Überhang danach als Abschlag wirkt
     * (heroes.thelazy.net, Damage). Mit Attack-Überhang wird aus +15 % also +115 %, mit
     * Defense-Überhang aus 80 % genau das Doppelte, 160 %.
     *
     * <p>Gilt nur für das Hauptziel: Splash-Treffer würfeln ihren Schaden selbst und kennen
     * kein Glück — in SoD trifft der Glückstreffer nur das angegriffene Ziel.
     */
    static int luckyDamagePercent(int boniMaliPercentage) {
        return bonusDamagePercent(boniMaliPercentage, 1);
    }

    /** Wie {@link #luckyDamagePercent}, mit {@code shares} Aufschlägen von je +100 % Grundschaden. */
    static int bonusDamagePercent(int boniMaliPercentage, int shares) {
        return boniMaliPercentage >= 0
                ? 100 + boniMaliPercentage + 100 * shares
                : (100 + boniMaliPercentage) * (1 + shares);
    }

    private void emitRebirthIfFired(Stack passive, boolean rebirthUsedBefore) {
        if (!rebirthUsedBefore && passive.isRebirthUsed()) {
            BattleLogger.logRebirth(passive.getName(), passive.getCount());
            events.emit(new BattleEvent.Rebirth(passive.side(), passive.slot(),
                    passive.getCount(), snapshot(passive)));
        }
    }

    private void doDeathStare(Stack active, Stack target, AttackType attackType) {
        // H3 Manual S. 95: „10% chance per attack of killing the top creature of a troop
        // outright per 10 Mighty Gorgons." → Trigger-Chance skaliert linear mit Stack-Größe
        // (1 % pro Gorgon, gedeckelt bei 100 %); Kill ist immer genau 1 Top-Creature.
        // Death Stare triggert nur bei Nahkampf.
        if (attackType != AttackType.HAND_TO_HAND) {
            return;
        }
        if (!active.hasSpeciality(UnitSpeciality.DEATH_STARE)) {
            return;
        }
        int chancePercent = Math.min(100, active.getCount());
        if (rng.nextInt(100) >= chancePercent) {
            return;
        }
        target.loseTopCreatures(1);
        BattleLogger.logDeathStare(active.getName(), target.getName(), 1);
        events.emit(new BattleEvent.DeathStare(active.side(), active.slot(),
                target.side(), target.slot(), 1, snapshot(target)));
    }

    private void doThunderbolts(Stack active, Stack target, AttackType attackType) {
        // H3: Thunderbolts triggern nur bei Nahkampf.
        if (attackType != AttackType.HAND_TO_HAND) {
            return;
        }
        if (!active.hasSpeciality(UnitSpeciality.THUNDERBOLTS)) {
            return;
        }
        int hits = 0;
        int count = active.getCount();
        for (int i = 0; i < count; i++) {
            if (rng.nextInt(100) < 20) {
                hits++;
            }
        }
        if (hits > 0) {
            int damage = hits * 10;
            target.takeDamage(damage, active.getAttackerSpecialities());
            BattleLogger.logThunderbolting(active.getName(), target.getName(), damage, target.getCurrentHealth());
            events.emit(new BattleEvent.Thunderbolts(active.side(), active.slot(),
                    target.side(), target.slot(), damage, snapshot(target)));
        }
    }

    /**
     * Zauber, die ein Angriff auslöst: Unicorns blenden mit 20 % Chance (Basic, 3 Runden), Serpent
     * und Dragon Flies heben die nützlichen Zauber des Ziels auf (Advanced, nicht abzuwehren),
     * Dragon Flies wirken danach Weakness (Advanced, 3 Runden) — heroes.thelazy.net. Gegen ein
     * immunes Ziel wird gar nicht erst gewürfelt.
     */
    private void doCreatureSpellProcs(Stack active, Stack target) {
        BattleSetup setup = currentSetup;
        if (setup == null) {
            return;
        }
        if (active.hasSpeciality(UnitSpeciality.BLINDING)
                && !SpellResistance.isImmune(target, Spell.BLIND) && rng.nextInt(100) < 20) {
            boolean takes = SpellResistance.takesEffect(Spell.BLIND, target, setup, rng);
            if (takes) {
                target.applySpell(Spell.BLIND, SkillLevel.BASIC, PROC_ROUNDS);
            }
            emitProc(active, Spell.BLIND, target, !takes);
        }
        if (active.hasSpeciality(UnitSpeciality.DISPELS_BENEFICIAL)) {
            int removed = target.removeSpells(Spell::beneficial);
            if (removed > 0) {
                events.emit(new BattleEvent.Dispel(active.side(), active.slot(),
                        target.side(), target.slot(), removed, snapshot(target)));
            }
        }
        if (active.hasSpeciality(UnitSpeciality.CASTS_WEAKNESS)
                && !SpellResistance.isImmune(target, Spell.WEAKNESS)) {
            boolean takes = SpellResistance.takesEffect(Spell.WEAKNESS, target, setup, rng);
            if (takes) {
                target.applySpell(Spell.WEAKNESS, SkillLevel.ADVANCED, PROC_ROUNDS);
            }
            emitProc(active, Spell.WEAKNESS, target, !takes);
        }
    }

    private void emitProc(Stack active, Spell spell, Stack target, boolean resisted) {
        BattleLogger.logSpell(active.getName(), spell, target.getName());
        events.emit(new BattleEvent.SpellCast(active.side(), active.slot(), spell,
                target.side(), target.slot(), resisted, snapshot(target)));
    }

    private void doPetrifying(Stack active, Stack target) {
        if (active.hasSpeciality(UnitSpeciality.PETRYFYING) && rng.nextInt(100) < 20) {
            target.petrify();
            BattleLogger.logPetrifying(active.getName(), target.getName());
            events.emit(new BattleEvent.Petrifying(active.side(), active.slot(),
                    target.side(), target.slot()));
        }
    }

    private void doCursing(Stack active, Stack target) {
        int chance = curseChance(active.unit());
        if (chance > 0 && rng.nextInt(100) < chance) {
            target.curse();
            BattleLogger.logCurse(active.getName(), target.getName());
            events.emit(new BattleEvent.Cursing(active.side(), active.slot(),
                    target.side(), target.slot()));
        }
    }

    /** Black/Dread Knight 20 %, Mummy 25 %. */
    private static int curseChance(Unit unit) {
        if (unit.hasSpeciality(UnitSpeciality.CURSING)) {
            return 20;
        }
        return unit.hasSpeciality(UnitSpeciality.CURSING_25) ? 25 : 0;
    }

    private void doAcidBreath(Stack active, Stack target) {
        if (!active.hasSpeciality(UnitSpeciality.ACID_BREATH)) {
            return;
        }
        target.corrode();
        int damage = rng.nextInt(100) < 20 ? 25 * active.getCount() : 0;
        int countBefore = target.getCount();
        if (damage > 0) {
            boolean rebirthUsedBefore = target.isRebirthUsed();
            target.takeDamage(damage, Set.of());
            emitRebirthIfFired(target, rebirthUsedBefore);
        }
        BattleLogger.logAcidBreath(active.getName(), target.getName(), damage);
        events.emit(new BattleEvent.AcidBreath(active.side(), active.slot(), target.side(), target.slot(),
                damage, countBefore - target.getCount(), snapshot(target)));
    }

    private void doPoisoning(Stack active, Stack target) {
        if (active.hasSpeciality(UnitSpeciality.POISONOUS) && rng.nextInt(100) < 25) {
            target.poison();
            BattleLogger.logPoisoning(active.getName(), target.getName());
            events.emit(new BattleEvent.Poisoning(active.side(), active.slot(),
                    target.side(), target.slot()));
        }
    }

    private void doDiseasing(Stack active, Stack target) {
        if (active.hasSpeciality(UnitSpeciality.DISEASES) && rng.nextInt(100) < 20) {
            target.disease();
            BattleLogger.logDiseasing(active.getName(), target.getName());
            events.emit(new BattleEvent.Diseasing(active.side(), active.slot(),
                    target.side(), target.slot()));
        }
    }

    /** Dendroiden fesseln, wen sie im Nahkampf treffen — auch bei ihrer Vergeltung. */
    private void doBinding(Stack active, Stack target, AttackType attackType) {
        if (attackType == AttackType.HAND_TO_HAND && active.hasSpeciality(UnitSpeciality.BINDS)) {
            target.bindTo(active);
            events.emit(new BattleEvent.Bind(active.side(), active.slot(), target.side(), target.slot()));
        }
    }

    private void doAging(Stack active, Stack target) {
        if (active.hasSpeciality(UnitSpeciality.AGING) && rng.nextInt(100) < 20) {
            target.age();
            BattleLogger.logAging(active.getName(), target.getName());
            events.emit(new BattleEvent.Aging(active.side(), active.slot(),
                    target.side(), target.slot()));
        }
    }

    private static @Nullable String heroName(@Nullable Hero hero) {
        return hero == null ? null : hero.name();
    }

    private static StackSnapshot snapshot(Stack stack) {
        return new StackSnapshot(stack.side(), stack.slot(), stack.getName(), stack.getCount(),
                stack.getCurrentHealth(), stack.position().q(), stack.position().r(), stack.isDoubleWide());
    }

    private static Winner determineWinner(BattleSetup setup) {
        // Wer nur noch Beschworene hat, verliert: sie verschwinden nach dem Kampf.
        boolean attackerAlive = setup.hasOwnTroops(Side.ATTACKER);
        boolean defenderAlive = setup.hasOwnTroops(Side.DEFENDER);
        if (attackerAlive && !defenderAlive) {
            BattleLogger.logDeath(setup.getDefenderName());
            return Winner.ATTACKER;
        }
        if (!attackerAlive && defenderAlive) {
            BattleLogger.logDeath(setup.getAttackerName());
            return Winner.DEFENDER;
        }
        if (!attackerAlive) {
            BattleLogger.logDeath(setup.getAttackerName());
            BattleLogger.logDeath(setup.getDefenderName());
            return Winner.DRAW;
        }
        // Beide noch lebendig — Turn-Cap oder Stall. Sieger über Gesamt-HP.
        int aHp = totalHp(setup.getAttacker());
        int dHp = totalHp(setup.getDefender());
        if (aHp > dHp) {
            return Winner.ATTACKER;
        }
        if (dHp > aHp) {
            return Winner.DEFENDER;
        }
        return Winner.DRAW;
    }

    private static int totalHp(Stack stack) {
        int max = stack.unit().health();
        return Math.max(0, stack.getCount() - 1) * max + stack.getCurrentHealth();
    }

}
