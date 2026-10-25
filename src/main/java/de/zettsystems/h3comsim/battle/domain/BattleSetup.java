package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Konfiguration einer Schlacht — bis zu 7 Stacks pro Seite. Single-Battle nutzt die
 * Convenience-Konstruktoren mit einem Stack pro Seite (Slot 0); Multi-Stack-Battles
 * konstruieren die Listen vorgängig.
 */
public class BattleSetup {

    private static final Hex DEFAULT_ATTACKER_POSITION = new Hex(0, 5);
    private static final Hex DEFAULT_DEFENDER_POSITION = new Hex(14, 5);

    private final List<Stack> attackerStacks;
    private final List<Stack> defenderStacks;
    private final Battlefield battlefield;
    private final @Nullable Siege siege;
    /** Das Feld je Seite mit den Mauern, zwischengespeichert bis zur nächsten Änderung der Belagerung. */
    private final Map<Side, Battlefield> siegeFields = new EnumMap<>(Side.class);
    private int siegeFieldsVersion = -1;
    private final @Nullable Hero attackerHero;
    private final @Nullable Hero defenderHero;
    private final int attackerMorale;
    private final int defenderMorale;
    private final int attackerLuck;
    private final int defenderLuck;
    private final int attackerTactics;
    private final int defenderTactics;
    private final Map<Side, Set<WarMachine>> warMachines = new EnumMap<>(Side.class);
    /** Die Maschinen als Stacks auf dem Feld; nicht Teil der Armeelisten. */
    private final Map<Side, Map<WarMachine, Stack>> machineStacks = new EnumMap<>(Side.class);
    private final Map<Side, Integer> mana = new EnumMap<>(Side.class);
    /** Je Seite die eine Elementar-Art, die beschworen werden darf, sobald die erste gerufen ist. */
    private final Map<Side, Unit> summonedKind = new EnumMap<>(Side.class);

    public BattleSetup(Unit attackerUnit, int attackerCount,
                       Unit defenderUnit, int defenderCount) {
        this(attackerUnit, attackerCount, defenderUnit, defenderCount,
                Battlefield.STANDARD, DEFAULT_ATTACKER_POSITION, DEFAULT_DEFENDER_POSITION);
    }

    public BattleSetup(Unit attackerUnit, int attackerCount,
                       Unit defenderUnit, int defenderCount,
                       Battlefield battlefield, Hex attackerPosition, Hex defenderPosition) {
        this(List.of(new Stack(attackerUnit, attackerCount, attackerPosition, Side.ATTACKER, 0)),
                List.of(new Stack(defenderUnit, defenderCount, defenderPosition, Side.DEFENDER, 0)),
                battlefield);
    }

    public BattleSetup(List<Stack> attackerStacks, List<Stack> defenderStacks, Battlefield battlefield) {
        this(attackerStacks, defenderStacks, battlefield, null, null);
    }

    /**
     * Variante mit Helden. Ein Held führt die ganze Armee, nicht einen einzelnen Stack —
     * deshalb wird er hier auf alle Stacks seiner Seite verteilt, statt beim Stack-Bau
     * mitgegeben zu werden. So kann kein Stack ohne oder mit dem falschen Anführer entstehen.
     *
     * @param attackerHero Held des Angreifers, {@code null} für eine führerlose Armee
     * @param defenderHero Held des Verteidigers, {@code null} für eine führerlose Armee
     */
    public BattleSetup(List<Stack> attackerStacks, List<Stack> defenderStacks,
                       Battlefield battlefield,
                       @Nullable Hero attackerHero, @Nullable Hero defenderHero) {
        this(attackerStacks, defenderStacks, battlefield, attackerHero, CollectedBonus.NONE,
                defenderHero, CollectedBonus.NONE);
    }

    /**
     * Variante mit Helden und deren gesammelten Boni (Glück und Moral aus Artefakten, Gebäuden,
     * Abenteuer-Orten). Ein Bonus ohne Helden bleibt wirkungslos. Hat ein Held Tactics im
     * Vorsprung, stellt er hier seine Nahkämpfer auf ({@link Tactics}).
     */
    public BattleSetup(List<Stack> attackerStacks, List<Stack> defenderStacks,
                       Battlefield battlefield,
                       @Nullable Hero attackerHero, CollectedBonus attackerBonus,
                       @Nullable Hero defenderHero, CollectedBonus defenderBonus) {
        this(attackerStacks, defenderStacks, battlefield, attackerHero, attackerBonus, defenderHero, defenderBonus,
                null);
    }

    /**
     * Wie oben, als Belagerung, wenn {@code siege} gesetzt ist: der Verteidiger steht hinter den
     * Mauern ({@link Siege}). Tactics stellt dann nur der Angreifer auf — die Aufstellung des
     * Verteidigers würde seine Nahkämpfer über die Mauer schieben.
     */
    // Je Seite Stacks, Held und Boni, dazu Feld und Belagerung: ein Parameterobjekt würde nur die
    // Aufrufer verlängern, die Konstruktoren darüber decken die üblichen Fälle ab.
    @SuppressWarnings("java:S107")
    public BattleSetup(List<Stack> attackerStacks, List<Stack> defenderStacks,
                       Battlefield battlefield,
                       @Nullable Hero attackerHero, CollectedBonus attackerBonus,
                       @Nullable Hero defenderHero, CollectedBonus defenderBonus,
                       @Nullable Siege siege) {
        Objects.requireNonNull(attackerStacks, "attackerStacks");
        Objects.requireNonNull(defenderStacks, "defenderStacks");
        if (attackerStacks.isEmpty() || defenderStacks.isEmpty()) {
            throw new IllegalArgumentException("Each side needs at least one stack");
        }
        // Veränderlich nur für Beschwörungen ({@link #addSummoned}); nach außen unveränderliche Sichten.
        this.attackerStacks = new ArrayList<>(attackerStacks);
        this.defenderStacks = new ArrayList<>(defenderStacks);
        this.battlefield = Objects.requireNonNull(battlefield, "battlefield");
        this.siege = siege;
        fitOnField(this.attackerStacks, battlefield);
        fitOnField(this.defenderStacks, battlefield);
        Objects.requireNonNull(attackerBonus, "attackerBonus");
        Objects.requireNonNull(defenderBonus, "defenderBonus");
        this.attackerHero = attackerHero;
        this.defenderHero = defenderHero;
        this.attackerStacks.forEach(stack -> stack.assignCommander(attackerHero));
        this.defenderStacks.forEach(stack -> stack.assignCommander(defenderHero));
        this.attackerMorale = ArmyMorale.ratingOf(this.attackerStacks, attackerHero, attackerBonus,
                this.defenderStacks);
        this.defenderMorale = ArmyMorale.ratingOf(this.defenderStacks, defenderHero, defenderBonus,
                this.attackerStacks);
        this.attackerStacks.forEach(stack -> stack.assignArmyMorale(attackerMorale));
        this.defenderStacks.forEach(stack -> stack.assignArmyMorale(defenderMorale));
        this.attackerLuck = ArmyLuck.ratingOf(attackerHero, attackerBonus, this.defenderStacks);
        this.defenderLuck = ArmyLuck.ratingOf(defenderHero, defenderBonus, this.attackerStacks);
        this.attackerStacks.forEach(stack -> stack.assignArmyLuck(attackerLuck));
        this.defenderStacks.forEach(stack -> stack.assignArmyLuck(defenderLuck));
        mana.put(Side.ATTACKER, attackerHero == null ? 0 : attackerHero.maxMana());
        mana.put(Side.DEFENDER, defenderHero == null ? 0 : defenderHero.maxMana());
        this.attackerTactics = Tactics.columns(attackerHero, defenderHero);
        this.defenderTactics = siege == null ? Tactics.columns(defenderHero, attackerHero) : 0;
        List<Stack> all = new ArrayList<>(this.attackerStacks);
        all.addAll(this.defenderStacks);
        Tactics.arrange(this.attackerStacks, Side.ATTACKER, attackerTactics, fieldFor(Side.ATTACKER), all);
        Tactics.arrange(this.defenderStacks, Side.DEFENDER, defenderTactics, fieldFor(Side.ATTACKER), all);
        if (siege != null && attackerHero != null) {
            placeMachines(Side.ATTACKER, Set.of());
        }
    }

    /**
     * Eine zweifeldrige Kreatur auf der Randzelle ragte mit dem Heck über den Rand; sie rückt um
     * ein Feld vor, wie im Spiel der Kopf in der zweiten Spalte steht.
     */
    private static void fitOnField(List<Stack> stacks, Battlefield field) {
        for (Stack stack : stacks) {
            Hex head = stack.position();
            if (stack.isDoubleWide() && !field.contains(Stack.tailOf(head, stack.side()))) {
                stack.moveTo(new Hex(head.q() + (stack.side() == Side.ATTACKER ? 1 : -1), head.r()));
            }
        }
    }

    /**
     * Rüstet eine Seite mit Kriegsmaschinen aus ({@link WarMachine}) und stellt sie auf ihre
     * Felder. Vor {@code simulate} aufzurufen; ein Munitionswagen gibt den Stacks der Seite sofort
     * unbegrenzte Schüsse.
     *
     * @return dieses Setup, zum Verketten
     */
    public BattleSetup equip(Side side, Set<WarMachine> machines) {
        placeMachines(side, machines);
        return this;
    }

    private void placeMachines(Side side, Set<WarMachine> machines) {
        Set<WarMachine> copy = machines.isEmpty() ? EnumSet.noneOf(WarMachine.class) : EnumSet.copyOf(machines);
        if (siege != null && side == Side.DEFENDER) {
            // Der Verteidiger einer Stadt hat keine Ballista auf dem Feld; seine Türme ersetzen sie.
            copy.remove(WarMachine.BALLISTA);
        }
        // Das Katapult hat jeder angreifende Held, aber nur bei einer Belagerung.
        copy.remove(WarMachine.CATAPULT);
        if (siege != null && side == Side.ATTACKER && attackerHero != null) {
            copy.add(WarMachine.CATAPULT);
        }
        warMachines.put(side, copy);
        Map<WarMachine, Stack> placed = new EnumMap<>(WarMachine.class);
        for (WarMachine machine : copy) {
            Stack stack = new Stack(machine.unit(), 1, machine.hex(side, battlefield), side, machine.slot());
            // Die Primärwerte des Helden gelten auch für seine Maschinen (siehe Hero#ballistaAttack).
            stack.assignCommander(side == Side.ATTACKER ? attackerHero : defenderHero);
            placed.put(machine, stack);
        }
        machineStacks.put(side, placed);
        if (copy.contains(WarMachine.AMMO_CART)) {
            side(side).forEach(Stack::grantUnlimitedAmmo);
        }
    }

    /** Kriegsmaschinen dieser Seite, leer ohne {@link #equip} — auch die schon zerstörten. */
    public Set<WarMachine> warMachinesOf(Side side) {
        return Collections.unmodifiableSet(warMachines.getOrDefault(side, EnumSet.noneOf(WarMachine.class)));
    }

    /** Die Maschinen dieser Seite als Stacks auf dem Feld, auch die zerstörten. */
    public List<Stack> machinesOf(Side side) {
        return List.copyOf(machineStacks.getOrDefault(side, Map.of()).values());
    }

    /** Hat die Seite diese Maschine, und steht sie noch? */
    public boolean hasWorking(Side side, WarMachine machine) {
        Stack stack = machineStacks.getOrDefault(side, Map.of()).get(machine);
        return stack != null && stack.isAlive();
    }

    /** Die stehende Maschine auf diesem Feld, oder {@code null}. */
    public @Nullable Stack machineAt(Hex hex) {
        for (Map<WarMachine, Stack> placed : machineStacks.values()) {
            for (Stack machine : placed.values()) {
                if (machine.isAlive() && machine.occupies(hex)) {
                    return machine;
                }
            }
        }
        return null;
    }

    /**
     * Alles, was Flächenzauber und Chain Lightning treffen können: die lebenden Stacks beider
     * Seiten und die stehenden Kriegsmaschinen (heroes.thelazy.net, <em>War Machine</em>).
     */
    public List<Stack> spellTargets() {
        List<Stack> targets = new ArrayList<>(aliveStacks());
        for (Side side : Side.values()) {
            machinesOf(side).stream().filter(Stack::isAlive).forEach(targets::add);
        }
        return targets;
    }

    /** Die stehenden Maschinen der Gegenseite dieses Stacks. */
    public List<Stack> enemyMachinesOf(Stack stack) {
        Side enemy = stack.side() == Side.ATTACKER ? Side.DEFENDER : Side.ATTACKER;
        return machinesOf(enemy).stream().filter(Stack::isAlive).toList();
    }

    /**
     * Nach einem Treffer auf eine Maschine: ist der Munitionswagen zerstört, zählen die Schüsse
     * der Seite ab jetzt wieder (heroes.thelazy.net, <em>Ammo Cart</em>).
     */
    void machineHit(Stack machine) {
        if (!machine.isAlive() && machine.unit().equals(WarMachine.AMMO_CART.unit())) {
            stacksOf(machine.side()).forEach(Stack::revokeUnlimitedAmmo);
        }
    }

    /**
     * Halbiert die Mauer diesen Schuss? Die Hindernis-Strafe gibt es nur bei Belagerungen, wenn
     * die Schusslinie über eine stehende Mauer führt (heroes.thelazy.net, <em>Obstacle
     * penalty</em>); gewöhnliche Hindernisse halbieren nichts.
     */
    boolean shotCrossesWall(Stack shooter, Stack target) {
        return siege != null && siege.crossesWall(shooter.position(), target.position(), battlefield);
    }

    /** Die Belagerung, oder {@code null} für eine Feldschlacht. */
    public @Nullable Siege siege() {
        return siege;
    }

    /**
     * Das Feld aus Sicht dieser Seite: bei einer Belagerung mit den Mauern als Hindernis, für den
     * Angreifer auch mit dem geschlossenen Tor.
     */
    public Battlefield battlefieldFor(Side side) {
        return fieldFor(side);
    }

    private Battlefield fieldFor(Side side) {
        if (siege == null) {
            return battlefield;
        }
        if (siegeFieldsVersion != siege.version()) {
            siegeFields.clear();
            siegeFieldsVersion = siege.version();
        }
        return siegeFields.computeIfAbsent(side, s -> {
            Set<Hex> blocked = new HashSet<>(battlefield.obstacles());
            blocked.addAll(siege.blockedFor(s));
            return battlefield.withObstacles(blocked);
        });
    }

    /**
     * Führt die Zugbrücke nach dem Stand auf dem Feld nach; gefallene Stacks zählen mit.
     *
     * @return der neue Zustand, wenn er sich geändert hat, sonst {@code null}
     */
    Siege.@Nullable Gate updateGate() {
        if (siege == null) {
            return null;
        }
        Set<Hex> occupied = new HashSet<>();
        Set<Hex> defenders = new HashSet<>();
        attackerStacks.forEach(s -> occupied.addAll(s.hexes()));
        for (Stack s : defenderStacks) {
            occupied.addAll(s.hexes());
            if (s.isAlive()) {
                defenders.addAll(s.hexes());
            }
        }
        return siege.updateGate(occupied, defenders) ? siege.gate() : null;
    }

    /** Das Feld ohne Mauern, mit den Hindernissen, wie sie zu Kampfbeginn gemeldet werden. */
    public Battlefield baseBattlefield() {
        return battlefield;
    }

    /** Mana, das der Held dieser Seite noch hat; zu Beginn {@link Hero#maxMana()}, ohne Helden 0. */
    public int manaOf(Side side) {
        return mana.getOrDefault(side, 0);
    }

    void spendMana(Side side, int amount) {
        mana.put(side, Math.max(0, manaOf(side) - amount));
    }

    /**
     * Breite der Tactics-Zone dieser Seite in Hex-Spalten (Manual S. 42), 0 ohne
     * Aufstellungsphase. Höchstens eine Seite hat eine.
     */
    public int tacticsOf(Side side) {
        return side == Side.ATTACKER ? attackerTactics : defenderTactics;
    }

    /** Glücks-Rating der Armee (Manual S. 44), 0..3. Ohne Helden immer 0. */
    public int luckOf(Side side) {
        return side == Side.ATTACKER ? attackerLuck : defenderLuck;
    }

    /**
     * Moral-Rating der Armee (Manual S. 43), ohne den individuellen Bonus der einzelnen
     * Kreatur. Zu Kampfbeginn aus der Zusammenstellung berechnet und danach fest — dass ein
     * Stack im Verlauf fällt, ändert die Moral der Überlebenden in H3 nicht.
     */
    public int moraleOf(Side side) {
        return side == Side.ATTACKER ? attackerMorale : defenderMorale;
    }

    public @Nullable Hero attackerHero() {
        return attackerHero;
    }

    public @Nullable Hero defenderHero() {
        return defenderHero;
    }

    public @Nullable Hero heroOf(Side side) {
        return side == Side.ATTACKER ? attackerHero : defenderHero;
    }

    public List<Stack> attackerStacks() {
        return Collections.unmodifiableList(attackerStacks);
    }

    public List<Stack> defenderStacks() {
        return Collections.unmodifiableList(defenderStacks);
    }

    public List<Stack> stacksOf(Side side) {
        return Collections.unmodifiableList(side == Side.ATTACKER ? attackerStacks : defenderStacks);
    }

    /**
     * Elementar-Art, die diese Seite bereits beschworen hat — „Only one type of elemental can be
     * summoned" (Manual S. 66 ff.) —, oder {@code null}.
     */
    public @Nullable Unit summonedKindOf(Side side) {
        return summonedKind.get(side);
    }

    /**
     * Nimmt einen beschworenen Stack in die Armee auf: unter dem Helden der Seite, mit deren Moral
     * und Glück, im nächsten freien Slot.
     */
    void addSummoned(Stack stack) {
        List<Stack> own = side(stack.side());
        stack.assignCommander(heroOf(stack.side()));
        stack.assignArmyMorale(moraleOf(stack.side()));
        stack.assignArmyLuck(luckOf(stack.side()));
        stack.markSummoned();
        own.add(stack);
        summonedKind.put(stack.side(), stack.unit());
    }

    /** Nächster freier Slot dieser Seite (beschworene Stacks bekommen Slots ab 7). */
    int nextSlot(Side side) {
        return side(side).stream().mapToInt(Stack::slot).max().orElse(-1) + 1;
    }

    private List<Stack> side(Side side) {
        return side == Side.ATTACKER ? attackerStacks : defenderStacks;
    }

    /**
     * Freies Feld für einen beschworenen Stack: am eigenen Rand, von der Mitte nach außen, dann
     * eine Spalte weiter innen; {@code null}, wenn das Feld voll ist.
     */
    @Nullable Hex freeHexFor(Side side) {
        return freeHexFor(side, false);
    }

    /** Wie {@link #freeHexFor(Side)}; für eine zweifeldrige Kreatur muss auch das Heck frei sein. */
    @Nullable Hex freeHexFor(Side side, boolean doubleWide) {
        List<Integer> rows = rowsFromCenter();
        for (int column = 0; column < battlefield.width(); column++) {
            for (int r : rows) {
                int q = side == Side.ATTACKER ? battlefield.leftmostQ(r) + column : battlefield.rightmostQ(r) - column;
                Hex hex = new Hex(q, r);
                if (isFree(hex) && (!doubleWide || isFree(Stack.tailOf(hex, side)))) {
                    return hex;
                }
            }
        }
        return null;
    }

    /** Reihen von der Mitte nach außen, oben vor unten. */
    private List<Integer> rowsFromCenter() {
        int center = battlefield.centerRow();
        List<Integer> rows = new ArrayList<>(battlefield.height());
        rows.add(center);
        for (int offset = 1; rows.size() < battlefield.height(); offset++) {
            if (center - offset >= 0) {
                rows.add(center - offset);
            }
            if (center + offset < battlefield.height()) {
                rows.add(center + offset);
            }
        }
        return rows;
    }

    /**
     * Lässt sich der ganz gefallene Stack zurückholen? Nur, wenn sein Feld frei ist („corpse" in
     * H3, heroes.thelazy.net) und er nicht beschworen war.
     */
    boolean hasFreeCorpse(Stack stack) {
        return !stack.isAlive() && stack.startCount() > 0 && !stack.isSummoned() && isFree(stack.position());
    }

    boolean isFree(Hex hex) {
        return battlefield().isPassable(hex) && machineAt(hex) == null
                && aliveStacks().stream().noneMatch(st -> st.occupies(hex));
    }

    /**
     * Die Kopffelder, von denen aus {@code mover} {@code target} im Nahkampf erreicht und auf denen
     * er Platz hat: neben jedem Feld des Ziels, eine zweifeldrige Kreatur auch so, dass ihr Heck
     * daneben steht. Reihenfolge in Blickrichtung des Movers ({@link Hex#neighbors(boolean)}).
     */
    public List<Hex> attackHeads(Stack mover, Stack target) {
        List<Hex> result = new ArrayList<>();
        for (Hex head : headCandidates(mover, target)) {
            if (mover.hexesAt(head).stream().noneMatch(target::occupies) && touches(mover, head, target)
                    && fits(mover, head)) {
                result.add(head);
            }
        }
        return result;
    }

    /** Die freien Nachbarn der Zielfelder, für Zweifeldrige dazu die Köpfe, deren Heck dort läge. */
    private static Set<Hex> headCandidates(Stack mover, Stack target) {
        boolean leftward = Hex.isLeftward(mover.position(), target.position());
        int forward = mover.side() == Side.ATTACKER ? 1 : -1;
        Set<Hex> heads = new LinkedHashSet<>();
        for (Hex own : target.hexes()) {
            for (Hex n : own.neighbors(leftward)) {
                if (target.occupies(n)) {
                    continue;
                }
                heads.add(n);
                if (mover.isDoubleWide()) {
                    heads.add(new Hex(n.q() + forward, n.r()));
                }
            }
        }
        return heads;
    }

    /** Stünde {@code mover} mit dem Kopf auf {@code head} neben {@code target}? */
    public static boolean touches(Stack mover, Hex head, Stack target) {
        return mover.hexesAt(head).stream().anyMatch(h -> target.distanceTo(h) == 1);
    }

    /**
     * Passt {@code mover} mit dem Kopf auf {@code head}? Alle seine Felder müssen für seine Seite
     * begehbar sein und frei von anderen Stacks und Maschinen; was er selbst schon belegt, zählt
     * als frei.
     */
    // st != mover: Identitätsvergleich auf der mutable Stack-Entity.
    @SuppressWarnings("ReferenceEquality")
    public boolean fits(Stack mover, Hex head) {
        Battlefield field = battlefieldFor(mover.side());
        for (Hex hex : mover.hexesAt(head)) {
            Stack machine = machineAt(hex);
            if (!field.isPassable(hex) || (machine != null && machine != mover)
                    || aliveStacks().stream().anyMatch(st -> st != mover && st.occupies(hex))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Hat die Seite noch eigene, nicht beschworene Truppen? „If you killed all enemies but lost all
     * your army besides the summoned elementals, both sides lose the battle" (heroes.thelazy.net).
     */
    public boolean hasOwnTroops(Side side) {
        return side(side).stream().anyMatch(st -> st.isAlive() && !st.isSummoned());
    }

    /** Überlebende Kreaturen ohne Beschworene — sie verschwinden nach dem Kampf. */
    public int survivorsOf(Side side) {
        return side(side).stream().filter(st -> !st.isSummoned()).mapToInt(Stack::getCount).sum();
    }

    public List<Stack> aliveStacks() {
        List<Stack> alive = new ArrayList<>(attackerStacks.size() + defenderStacks.size());
        for (Stack s : attackerStacks) {
            if (s.isAlive()) {
                alive.add(s);
            }
        }
        for (Stack s : defenderStacks) {
            if (s.isAlive()) {
                alive.add(s);
            }
        }
        return alive;
    }

    public List<Stack> opponentsOf(Stack stack) {
        List<Stack> opponents = stack.side() == Side.ATTACKER ? defenderStacks : attackerStacks;
        List<Stack> alive = new ArrayList<>(opponents.size());
        for (Stack s : opponents) {
            if (s.isAlive()) {
                alive.add(s);
            }
        }
        return alive;
    }

    /**
     * Single-Battle-Convenience: liefert den ersten Attacker-Stack. Im Multi-Stack-Pfad
     * sollte stattdessen {@link #attackerStacks()} verwendet werden.
     */
    public Stack getAttacker() {
        return attackerStacks.getFirst();
    }

    /**
     * Single-Battle-Convenience: liefert den ersten Defender-Stack.
     */
    public Stack getDefender() {
        return defenderStacks.getFirst();
    }

    /** Das Feld mit den Mauern aus Sicht des Angreifers — das vorsichtigere der beiden. */
    public Battlefield battlefield() {
        return battlefieldFor(Side.ATTACKER);
    }

    public String getAttackerName() {
        return getAttacker().getName();
    }

    public String getDefenderName() {
        return getDefender().getName();
    }

    public boolean isAttackerAlive() {
        return attackerStacks.stream().anyMatch(Stack::isAlive);
    }

    public boolean isDefenderAlive() {
        return defenderStacks.stream().anyMatch(Stack::isAlive);
    }

    public boolean bothAlive() {
        return isAttackerAlive() && isDefenderAlive();
    }

    public int getAttackerCount() {
        return attackerStacks.stream().mapToInt(Stack::getCount).sum();
    }

    public int getDefenderCount() {
        return defenderStacks.stream().mapToInt(Stack::getCount).sum();
    }

    /**
     * Single-Battle-Convenience: liefert den einzigen lebenden Gegner. Wirft, wenn die Seite
     * mehr als einen lebenden Stack hat (Multi-Stack-Pfad soll {@link #opponentsOf(Stack)} nutzen).
     */
    public Stack getTarget(Stack activeStack) {
        List<Stack> opponents = opponentsOf(activeStack);
        if (opponents.size() != 1) {
            throw new IllegalStateException(
                    "getTarget(...) ist nur für 1-vs-1-Pfade definiert; gefunden: " + opponents.size()
                            + " lebende Gegner. Im Multi-Stack-Pfad opponentsOf(...) nutzen.");
        }
        return opponents.getFirst();
    }
}
