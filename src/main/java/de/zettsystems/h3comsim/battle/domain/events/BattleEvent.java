package de.zettsystems.h3comsim.battle.domain.events;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import de.zettsystems.h3comsim.battle.domain.SkillLevel;
import de.zettsystems.h3comsim.battle.domain.Spell;
import de.zettsystems.h3comsim.battle.domain.WallPart;
import de.zettsystems.h3comsim.battle.domain.WarMachine;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = BattleEvent.BattleStart.class, name = "BattleStart"),
        @JsonSubTypes.Type(value = BattleEvent.Move.class, name = "Move"),
        @JsonSubTypes.Type(value = BattleEvent.Wait.class, name = "Wait"),
        @JsonSubTypes.Type(value = BattleEvent.Defend.class, name = "Defend"),
        @JsonSubTypes.Type(value = BattleEvent.Shoot.class, name = "Shoot"),
        @JsonSubTypes.Type(value = BattleEvent.Melee.class, name = "Melee"),
        @JsonSubTypes.Type(value = BattleEvent.Retaliation.class, name = "Retaliation"),
        @JsonSubTypes.Type(value = BattleEvent.TwoBlows.class, name = "TwoBlows"),
        @JsonSubTypes.Type(value = BattleEvent.TwoShots.class, name = "TwoShots"),
        @JsonSubTypes.Type(value = BattleEvent.GoodMorale.class, name = "GoodMorale"),
        @JsonSubTypes.Type(value = BattleEvent.BadMorale.class, name = "BadMorale"),
        @JsonSubTypes.Type(value = BattleEvent.LuckyStrike.class, name = "LuckyStrike"),
        @JsonSubTypes.Type(value = BattleEvent.TacticsPhase.class, name = "TacticsPhase"),
        @JsonSubTypes.Type(value = BattleEvent.BallistaShot.class, name = "BallistaShot"),
        @JsonSubTypes.Type(value = BattleEvent.FirstAid.class, name = "FirstAid"),
        @JsonSubTypes.Type(value = BattleEvent.CatapultShot.class, name = "CatapultShot"),
        @JsonSubTypes.Type(value = BattleEvent.SpellCast.class, name = "SpellCast"),
        @JsonSubTypes.Type(value = BattleEvent.Dispel.class, name = "Dispel"),
        @JsonSubTypes.Type(value = BattleEvent.HeroSpell.class, name = "HeroSpell"),
        @JsonSubTypes.Type(value = BattleEvent.SpellHit.class, name = "SpellHit"),
        @JsonSubTypes.Type(value = BattleEvent.Summon.class, name = "Summon"),
        @JsonSubTypes.Type(value = BattleEvent.MoveBack.class, name = "MoveBack"),
        @JsonSubTypes.Type(value = BattleEvent.DeathStare.class, name = "DeathStare"),
        @JsonSubTypes.Type(value = BattleEvent.Thunderbolts.class, name = "Thunderbolts"),
        @JsonSubTypes.Type(value = BattleEvent.Petrifying.class, name = "Petrifying"),
        @JsonSubTypes.Type(value = BattleEvent.Cursing.class, name = "Cursing"),
        @JsonSubTypes.Type(value = BattleEvent.Poisoning.class, name = "Poisoning"),
        @JsonSubTypes.Type(value = BattleEvent.Diseasing.class, name = "Diseasing"),
        @JsonSubTypes.Type(value = BattleEvent.Aging.class, name = "Aging"),
        @JsonSubTypes.Type(value = BattleEvent.FireShield.class, name = "FireShield"),
        @JsonSubTypes.Type(value = BattleEvent.Rebirth.class, name = "Rebirth"),
        @JsonSubTypes.Type(value = BattleEvent.Resurrection.class, name = "Resurrection"),
        @JsonSubTypes.Type(value = BattleEvent.RaiseDemons.class, name = "RaiseDemons"),
        @JsonSubTypes.Type(value = BattleEvent.BattleEnd.class, name = "BattleEnd"),
})
@Schema(
        description = """
                Polymorphes Battle-Event. Der Diskriminator `type` enthält den Subtyp-Namen
                (z.B. `Move`, `Shoot`, `BattleEnd`). Jeder Event-Stream beginnt mit
                `BattleStart` und endet mit `BattleEnd`; dazwischen liegen Bewegungen,
                Angriffe, Retaliations und ausgelöste Skills. Für Multi-Stack-Modi tragen
                Action-Events einen `actorSlot` und ggf. `targetSlot` (0..6); im
                Single-Battle-Pfad ist der Slot stets 0.
                """,
        oneOf = {
                BattleEvent.BattleStart.class, BattleEvent.Move.class, BattleEvent.Wait.class,
                BattleEvent.Defend.class,
                BattleEvent.Shoot.class, BattleEvent.Melee.class, BattleEvent.Retaliation.class,
                BattleEvent.TwoBlows.class, BattleEvent.TwoShots.class, BattleEvent.GoodMorale.class,
                BattleEvent.BadMorale.class, BattleEvent.LuckyStrike.class, BattleEvent.TacticsPhase.class,
                BattleEvent.BallistaShot.class, BattleEvent.FirstAid.class, BattleEvent.CatapultShot.class,
                BattleEvent.SpellCast.class, BattleEvent.Dispel.class,
                BattleEvent.HeroSpell.class, BattleEvent.SpellHit.class, BattleEvent.Summon.class,
                BattleEvent.MoveBack.class, BattleEvent.DeathStare.class, BattleEvent.Thunderbolts.class,
                BattleEvent.Petrifying.class, BattleEvent.Cursing.class, BattleEvent.Poisoning.class,
                BattleEvent.Diseasing.class, BattleEvent.Aging.class, BattleEvent.FireShield.class,
                BattleEvent.Rebirth.class, BattleEvent.Resurrection.class, BattleEvent.RaiseDemons.class,
                BattleEvent.BattleEnd.class
        },
        discriminatorProperty = "type",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "BattleStart", schema = BattleEvent.BattleStart.class),
                @DiscriminatorMapping(value = "Move", schema = BattleEvent.Move.class),
                @DiscriminatorMapping(value = "Wait", schema = BattleEvent.Wait.class),
                @DiscriminatorMapping(value = "Defend", schema = BattleEvent.Defend.class),
                @DiscriminatorMapping(value = "Shoot", schema = BattleEvent.Shoot.class),
                @DiscriminatorMapping(value = "Melee", schema = BattleEvent.Melee.class),
                @DiscriminatorMapping(value = "Retaliation", schema = BattleEvent.Retaliation.class),
                @DiscriminatorMapping(value = "TwoBlows", schema = BattleEvent.TwoBlows.class),
                @DiscriminatorMapping(value = "TwoShots", schema = BattleEvent.TwoShots.class),
                @DiscriminatorMapping(value = "GoodMorale", schema = BattleEvent.GoodMorale.class),
                @DiscriminatorMapping(value = "BadMorale", schema = BattleEvent.BadMorale.class),
                @DiscriminatorMapping(value = "LuckyStrike", schema = BattleEvent.LuckyStrike.class),
                @DiscriminatorMapping(value = "TacticsPhase", schema = BattleEvent.TacticsPhase.class),
                @DiscriminatorMapping(value = "BallistaShot", schema = BattleEvent.BallistaShot.class),
                @DiscriminatorMapping(value = "FirstAid", schema = BattleEvent.FirstAid.class),
                @DiscriminatorMapping(value = "CatapultShot", schema = BattleEvent.CatapultShot.class),
                @DiscriminatorMapping(value = "SpellCast", schema = BattleEvent.SpellCast.class),
                @DiscriminatorMapping(value = "Dispel", schema = BattleEvent.Dispel.class),
                @DiscriminatorMapping(value = "HeroSpell", schema = BattleEvent.HeroSpell.class),
                @DiscriminatorMapping(value = "SpellHit", schema = BattleEvent.SpellHit.class),
                @DiscriminatorMapping(value = "Summon", schema = BattleEvent.Summon.class),
                @DiscriminatorMapping(value = "MoveBack", schema = BattleEvent.MoveBack.class),
                @DiscriminatorMapping(value = "DeathStare", schema = BattleEvent.DeathStare.class),
                @DiscriminatorMapping(value = "Thunderbolts", schema = BattleEvent.Thunderbolts.class),
                @DiscriminatorMapping(value = "Petrifying", schema = BattleEvent.Petrifying.class),
                @DiscriminatorMapping(value = "Cursing", schema = BattleEvent.Cursing.class),
                @DiscriminatorMapping(value = "Poisoning", schema = BattleEvent.Poisoning.class),
                @DiscriminatorMapping(value = "Diseasing", schema = BattleEvent.Diseasing.class),
                @DiscriminatorMapping(value = "Aging", schema = BattleEvent.Aging.class),
                @DiscriminatorMapping(value = "FireShield", schema = BattleEvent.FireShield.class),
                @DiscriminatorMapping(value = "Rebirth", schema = BattleEvent.Rebirth.class),
                @DiscriminatorMapping(value = "Resurrection", schema = BattleEvent.Resurrection.class),
                @DiscriminatorMapping(value = "RaiseDemons", schema = BattleEvent.RaiseDemons.class),
                @DiscriminatorMapping(value = "BattleEnd", schema = BattleEvent.BattleEnd.class)
        })
public sealed interface BattleEvent {

    @Schema(name = "BattleStart",
            description = "Eröffnungs-Event. Liefert die Battlefield-Dimensionen, alle Obstacles, die initialen Single-Battle-Snapshots (slot 0), für Army-Battles die vollständige Stack-Liste `stacks` sowie das Moral-Rating beider Armeen.")
    record BattleStart(
            @Schema(description = "Anzahl Hex-Spalten des Battlefields", example = "15") int battlefieldWidth,
            @Schema(description = "Anzahl Hex-Reihen des Battlefields", example = "11") int battlefieldHeight,
            @Schema(description = "Liste aller blockenden Obstacles auf dem Feld") List<HexCoord> obstacles,
            @Schema(description = "Start-Snapshot des Attacker-Stacks (Slot 0 — für Single-Battle Vollständigkeit, für Army-Battles der Stack auf Slot 0)") StackSnapshot attacker,
            @Schema(description = "Start-Snapshot des Defender-Stacks (Slot 0)") StackSnapshot defender,
            @Schema(description = "Alle initialen Stacks beider Seiten (Army-Battle). Single-Battle: zwei Snapshots — Slot 0 Attacker + Slot 0 Defender.") List<StackSnapshot> stacks,
            @Schema(description = "Moral-Rating der Angreifer-Armee (Manual S. 43): Stadt-Typen, Untoten-Mischung, Leadership des Helden und Angel-Bonus zusammengerechnet. Untote und Elementare bleiben davon unberührt und haben immer Moral 0.", example = "1") int attackerMorale,
            @Schema(description = "Moral-Rating der Verteidiger-Armee, analog zu `attackerMorale`.", example = "0") int defenderMorale,
            @Schema(description = "Held der Angreifer-Armee. Im Army-Battle immer gesetzt (ohne Auswahl der Standardheld), im Single-Battle null.", example = "Crag Hack", nullable = true) @Nullable String attackerHero,
            @Schema(description = "Held der Verteidiger-Armee, analog zu `attackerHero`.", example = "Tazar", nullable = true) @Nullable String defenderHero,
            @Schema(description = "Glücks-Rating der Angreifer-Armee (Manual S. 44), 0..3: Luck-Fertigkeit des Helden plus gesammeltes Glück, minus eins gegen Devils. Ohne Helden 0.", example = "1") int attackerLuck,
            @Schema(description = "Glücks-Rating der Verteidiger-Armee, analog zu `attackerLuck`.", example = "0") int defenderLuck,
            @Schema(description = "Kriegsmaschinen des Angreifers") List<WarMachine> attackerWarMachines,
            @Schema(description = "Kriegsmaschinen des Verteidigers") List<WarMachine> defenderWarMachines,
            @Schema(description = "Die Kriegsmaschinen beider Seiten auf dem Feld, als Stacks ab Slot 20 (Ballista 20, Ammo Cart 21, First Aid Tent 22). Sie lassen sich angreifen; Treffer auf sie kommen als Melee- oder Shoot-Events.") List<StackSnapshot> warMachineStacks,
            @Schema(description = "Die Befestigung bei einer Belagerung, sonst null.", nullable = true) @Nullable SiegeSnapshot siege) implements BattleEvent {
        public BattleStart {
            obstacles = List.copyOf(obstacles);
            stacks = List.copyOf(stacks);
            warMachineStacks = List.copyOf(warMachineStacks);
            attackerWarMachines = List.copyOf(attackerWarMachines);
            defenderWarMachines = List.copyOf(defenderWarMachines);
        }
    }

    @Schema(name = "Move",
            description = "Bewegung eines Stacks von (fromQ, fromR) nach (toQ, toR) entlang `path`. Bei Flyern überspringt `path` Obstacles.")
    record Move(
            @Schema(description = "Ziehende Seite") Side actor,
            @Schema(description = "Slot des ziehenden Stacks (0..6)", example = "0") int actorSlot,
            @Schema(description = "Start-q") int fromQ,
            @Schema(description = "Start-r") int fromR,
            @Schema(description = "Ziel-q") int toQ,
            @Schema(description = "Ziel-r") int toR,
            @Schema(description = "Ordentlicher Pfad inklusive Start und Ziel — fürs UI-Animations-Replay")
            List<HexCoord> path) implements BattleEvent {
        public Move {
            path = List.copyOf(path);
        }
    }

    @Schema(name = "Wait",
            description = "Stack hat Wait gewählt und wird in der späten Phase der Runde erneut aufgerufen.")
    record Wait(
            @Schema(description = "Wartende Seite") Side actor,
            @Schema(description = "Slot des wartenden Stacks") int actorSlot) implements BattleEvent {
    }

    @Schema(name = "Defend",
            description = "Stack hat Defend gewählt: bewegt sich nicht, erhält bis Rundenende +20 % Defense (Manual S. 47). Wird auch als Engine-Fallback emittiert, wenn ein Solver versucht auf einen besetzten Hex zu ziehen oder ein zweites Mal in derselben Runde zu warten.")
    record Defend(
            @Schema(description = "Verteidigende Seite") Side actor,
            @Schema(description = "Slot des verteidigenden Stacks") int actorSlot) implements BattleEvent {
    }

    @Schema(name = "Shoot",
            description = "Fernkampf-Angriff. Schadens-/Kill-Werte beziehen sich auf das Ziel. `targetAfter` ist der Stack-Snapshot des Ziels NACH dem Treffer.")
    record Shoot(
            @Schema(description = "Schießende Seite") Side actor,
            @Schema(description = "Slot des Schützen") int actorSlot,
            @Schema(description = "Getroffene Seite") Side target,
            @Schema(description = "Slot des getroffenen Stacks") int targetSlot,
            @Schema(description = "Hex-Distanz zwischen Schütze und Ziel — bestimmt evtl. Damage-Penalty", example = "8") int distance,
            @Schema(description = "Insgesamt zugefügter Schaden", example = "47") int damage,
            @Schema(description = "Komplett getötete Einheiten im Ziel-Stack", example = "3") int killed,
            @Schema(description = "Ziel-Snapshot direkt nach dem Schuss") StackSnapshot targetAfter) implements BattleEvent {
    }

    @Schema(name = "Melee",
            description = "Nahkampf-Angriff. Vor dem Treffer wurden ggf. `hexesMoved` Hexe in Richtung Ziel zurückgelegt.")
    record Melee(
            @Schema(description = "Angreifende Seite") Side actor,
            @Schema(description = "Slot des Angreifers") int actorSlot,
            @Schema(description = "Getroffene Seite") Side target,
            @Schema(description = "Slot des getroffenen Stacks") int targetSlot,
            @Schema(description = "Hexe Bewegung zum Anlauf vor dem Hieb", example = "3") int hexesMoved,
            @Schema(description = "Insgesamt zugefügter Schaden", example = "62") int damage,
            @Schema(description = "Komplett getötete Einheiten im Ziel-Stack", example = "4") int killed,
            @Schema(description = "Ziel-Snapshot direkt nach dem Treffer") StackSnapshot targetAfter) implements BattleEvent {
    }

    @Schema(name = "Retaliation",
            description = "Konter eines getroffenen Nahkampf-Stacks. Nicht jeder Stack hat Retaliation (z.B. ausgeschöpft, kein Retaliation-Skill).")
    record Retaliation(
            @Schema(description = "Konternde Seite") Side retaliator,
            @Schema(description = "Slot des konternden Stacks") int retaliatorSlot,
            @Schema(description = "Ursprünglich angreifende Seite (jetzt Konter-Ziel)") Side target,
            @Schema(description = "Slot des Konter-Ziels") int targetSlot,
            @Schema(description = "Zugefügter Schaden") int damage,
            @Schema(description = "Komplett getötete Einheiten beim Original-Angreifer") int killed,
            @Schema(description = "Snapshot des Original-Angreifers nach dem Konter") StackSnapshot targetAfter) implements BattleEvent {
    }

    @Schema(name = "TwoBlows",
            description = "Marker: dieser Stack besitzt Double-Attack und führt zwei aufeinanderfolgende Schläge aus (z.B. Black Knight). Es folgen zwei reguläre Melee-Events.")
    record TwoBlows(
            @Schema(description = "Stack mit Double-Attack") Side actor,
            @Schema(description = "Slot des Stacks") int actorSlot) implements BattleEvent {
    }

    @Schema(name = "TwoShots",
            description = "Marker: dieser Stack besitzt Two-Shots (z.B. Marksman) und führt zwei aufeinanderfolgende Schüsse aus.")
    record TwoShots(
            @Schema(description = "Stack mit Two-Shots") Side actor,
            @Schema(description = "Slot des Stacks") int actorSlot) implements BattleEvent {
    }

    @Schema(name = "GoodMorale",
            description = "Marker: Good-Morale-Trigger — der Stack erhält direkt im Anschluss eine zweite Aktion.")
    record GoodMorale(
            @Schema(description = "Stack mit Morale-Bonus") Side actor,
            @Schema(description = "Slot des Stacks") int actorSlot) implements BattleEvent {
    }

    @Schema(name = "LuckyStrike",
            description = "Marker: Glückstreffer (Manual S. 44) — der unmittelbar folgende Melee-, Shoot- oder Retaliation-Event dieses Stacks richtet doppelten Grundschaden an.")
    record LuckyStrike(
            @Schema(description = "Stack mit dem Glückstreffer") Side actor,
            @Schema(description = "Slot des Stacks") int actorSlot) implements BattleEvent {
    }

    @Schema(name = "TacticsPhase",
            description = "Tactics (Manual S. 42): diese Seite hat ihre Nahkämpfer vor Kampfbeginn innerhalb der ersten `columns` Hex-Spalten aufgestellt. Die Positionen in `BattleStart` sind bereits die aufgestellten.")
    record TacticsPhase(
            @Schema(description = "Aufstellende Seite") Side actor,
            @Schema(description = "Breite der Zone in Hex-Spalten: 3, 5 oder 7", example = "5") int columns) implements BattleEvent {
    }

    @Schema(name = "BallistaShot",
            description = "Schuss der Ballista einer Seite am Rundenende (Manual S. 117). `lucky` = Glückstreffer, `doubled` = doppelter Grundschaden durch Artillery.")
    record BallistaShot(
            @Schema(description = "Seite der Ballista") Side actor,
            @Schema(description = "Getroffene Seite") Side target,
            @Schema(description = "Slot des getroffenen Stacks") int targetSlot,
            @Schema(description = "Verursachter Schaden") int damage,
            @Schema(description = "Getötete Kreaturen") int killed,
            @Schema(description = "Glückstreffer") boolean lucky,
            @Schema(description = "Doppelter Grundschaden durch Artillery") boolean doubled,
            @Schema(description = "Ziel nach dem Treffer") StackSnapshot targetAfter) implements BattleEvent {
    }

    @Schema(name = "FirstAid",
            description = "Das Sanitätszelt einer Seite heilt am Rundenende die oberste Kreatur eines eigenen Stacks (Manual S. 117).")
    record FirstAid(
            @Schema(description = "Seite des Zelts") Side actor,
            @Schema(description = "Slot des geheilten Stacks") int targetSlot,
            @Schema(description = "Geheilte Lebenspunkte") int healed,
            @Schema(description = "Stack nach der Heilung") StackSnapshot targetAfter) implements BattleEvent {
    }

    @Schema(name = "CatapultShot",
            description = "Das Katapult des Angreifers schießt zu Rundenbeginn auf die Befestigung. Ein Fehlschuss trifft ein anderes Mauersegment (`hit` ≠ `aimed`).")
    record CatapultShot(
            @Schema(description = "Worauf gezielt wurde") WallPart aimed,
            @Schema(description = "Was getroffen wurde") WallPart hit,
            @Schema(description = "Abgezogene Treffer, 0..2") int damage,
            @Schema(description = "Verbleibende Treffer des getroffenen Teils; 0 = zerstört") int healthAfter) implements BattleEvent {
    }

    @Schema(name = "SpellCast",
            description = "Eine Kreatur wirkt einen Zauber: Ogre Magi und Master Genie als Aktion, Unicorn (Blind) und Dragon Fly (Weakness) beim Angriff. `resisted` = das Ziel hat den Zauber abgewehrt.")
    record SpellCast(
            @Schema(description = "Seite des Zaubernden") Side actor,
            @Schema(description = "Slot des Zaubernden") int actorSlot,
            @Schema(description = "Zauber") Spell spell,
            @Schema(description = "Seite des Ziels") Side target,
            @Schema(description = "Slot des Ziels") int targetSlot,
            @Schema(description = "Abgewehrt durch Magieresistenz") boolean resisted,
            @Schema(description = "Ziel danach") StackSnapshot targetAfter) implements BattleEvent {
    }

    @Schema(name = "Dispel",
            description = "Serpent oder Dragon Fly hebt beim Angriff die nützlichen Zauber des Ziels auf.")
    record Dispel(
            @Schema(description = "Seite des Angreifers") Side actor,
            @Schema(description = "Slot des Angreifers") int actorSlot,
            @Schema(description = "Seite des Ziels") Side target,
            @Schema(description = "Slot des Ziels") int targetSlot,
            @Schema(description = "Anzahl aufgehobener Zauber") int removed,
            @Schema(description = "Ziel danach") StackSnapshot targetAfter) implements BattleEvent {
    }

    @Schema(name = "HeroSpell",
            description = "Der Held einer Seite wirkt einen Zauber (einmal pro Runde, Manual S. 45). Die Wirkung auf die einzelnen Stacks folgt als `SpellHit`.")
    record HeroSpell(
            @Schema(description = "Seite des Helden") Side actor,
            @Schema(description = "Zauber") Spell spell,
            @Schema(description = "Stufe, mit der er gewirkt wird") SkillLevel level,
            @Schema(description = "Mana danach") int manaLeft) implements BattleEvent {
    }

    @Schema(name = "SpellHit",
            description = "Wirkung eines Heldenzaubers auf einen Stack: Schaden, Heilung oder ein liegender Zauber. `resisted` = immun oder durch Magieresistenz abgewehrt, `reflected` = von Magic Mirror auf einen Stack des Zaubernden umgelenkt.")
    record SpellHit(
            @Schema(description = "Seite des zaubernden Helden") Side actor,
            @Schema(description = "Zauber") Spell spell,
            @Schema(description = "Seite des Ziels") Side target,
            @Schema(description = "Slot des Ziels") int targetSlot,
            @Schema(description = "Verursachter Schaden") int damage,
            @Schema(description = "Getötete Kreaturen") int killed,
            @Schema(description = "Geheilte Lebenspunkte") int healed,
            @Schema(description = "Immun oder abgewehrt") boolean resisted,
            @Schema(description = "Von Magic Mirror auf diesen Stack des Zaubernden umgelenkt") boolean reflected,
            @Schema(description = "Ziel danach") StackSnapshot targetAfter) implements BattleEvent {
    }

    @Schema(name = "Summon",
            description = "Ein Held beschwört Elementare (Manual S. 66 ff.): ein neuer Stack auf seiner Seite, ab Slot 7, oder Verstärkung des schon beschworenen. Er handelt ab der nächsten Runde und zählt am Ende nicht als Überlebender.")
    record Summon(
            @Schema(description = "Seite des Helden") Side actor,
            @Schema(description = "Beschwörungszauber") Spell spell,
            @Schema(description = "Beschworene Kreaturen") int count,
            @Schema(description = "Der beschworene Stack danach") StackSnapshot stack) implements BattleEvent {
    }

    @Schema(name = "BadMorale",
            description = "Marker: Bad-Morale-Trigger — der Stack ist eingefroren und verliert seine Aktion in dieser Runde vollständig. Er verteidigt auch nicht ersatzweise.")
    record BadMorale(
            @Schema(description = "Stack mit negativer Moral") Side actor,
            @Schema(description = "Slot des Stacks") int actorSlot) implements BattleEvent {
    }

    @Schema(name = "MoveBack",
            description = "Spezielle Bewegung: ein Stack mit `NO_MELEE_PENALTY`/Schütze zieht sich von einem angreifenden Nahkämpfer zurück und schießt aus sicherer Distanz weiter.")
    record MoveBack(
            @Schema(description = "Zurückweichende Seite") Side actor,
            @Schema(description = "Slot des zurückweichenden Stacks") int actorSlot,
            @Schema(description = "Ziel-q nach Rückzug") int toQ,
            @Schema(description = "Ziel-r nach Rückzug") int toR,
            @Schema(description = "Pfad-Hexe für die Animation") List<HexCoord> path) implements BattleEvent {
        public MoveBack {
            path = List.copyOf(path);
        }
    }

    @Schema(name = "DeathStare",
            description = "Skill-Event: Death-Stare des Mighty Gorgon hat eine bestimmte Anzahl Einheiten direkt getötet.")
    record DeathStare(
            @Schema(description = "Stack mit Death-Stare-Skill") Side actor,
            @Schema(description = "Slot des Auslösers") int actorSlot,
            @Schema(description = "Betroffene Seite") Side target,
            @Schema(description = "Slot des Ziels") int targetSlot,
            @Schema(description = "Anzahl direkt durch den Stare getöteter Einheiten", example = "2") int kills,
            @Schema(description = "Ziel-Snapshot nach dem Stare") StackSnapshot targetAfter) implements BattleEvent {
    }

    @Schema(name = "Thunderbolts",
            description = "Skill-Event: Thunderbolt-Auslösung (z.B. Titans) — magischer Schaden zusätzlich zum normalen Angriff.")
    record Thunderbolts(
            @Schema(description = "Stack mit Thunderbolt-Skill") Side actor,
            @Schema(description = "Slot des Auslösers") int actorSlot,
            @Schema(description = "Betroffene Seite") Side target,
            @Schema(description = "Slot des Ziels") int targetSlot,
            @Schema(description = "Magischer Zusatzschaden") int damage,
            @Schema(description = "Ziel-Snapshot nach dem Bolt") StackSnapshot targetAfter) implements BattleEvent {
    }

    @Schema(name = "Petrifying",
            description = "Skill-Event: Versteinerung (Medusa) auf einen Ziel-Stack ausgelöst. Reduziert dessen Defense temporär drastisch / setzt Aktion aus.")
    record Petrifying(
            @Schema(description = "Auslösender Stack") Side actor,
            @Schema(description = "Slot des Auslösers") int actorSlot,
            @Schema(description = "Versteinerte Seite") Side target,
            @Schema(description = "Slot des Ziels") int targetSlot) implements BattleEvent {
    }

    @Schema(name = "Cursing",
            description = "Skill-Event: Curse-Auslösung (z.B. Black Dragon Aura) auf einen Ziel-Stack.")
    record Cursing(
            @Schema(description = "Auslösender Stack") Side actor,
            @Schema(description = "Slot des Auslösers") int actorSlot,
            @Schema(description = "Verfluchte Seite") Side target,
            @Schema(description = "Slot des Ziels") int targetSlot) implements BattleEvent {
    }

    @Schema(name = "Poisoning",
            description = "Skill-Event: Vergiftung auf einen Ziel-Stack ausgelöst. Verursacht über mehrere Runden Schaden.")
    record Poisoning(
            @Schema(description = "Auslösender Stack") Side actor,
            @Schema(description = "Slot des Auslösers") int actorSlot,
            @Schema(description = "Vergiftete Seite") Side target,
            @Schema(description = "Slot des Ziels") int targetSlot) implements BattleEvent {
    }

    @Schema(name = "Diseasing",
            description = "Skill-Event: Krankheit (z.B. Zombie) auf einen Ziel-Stack ausgelöst. Senkt Attack/Defense des Ziels.")
    record Diseasing(
            @Schema(description = "Auslösender Stack") Side actor,
            @Schema(description = "Slot des Auslösers") int actorSlot,
            @Schema(description = "Erkrankte Seite") Side target,
            @Schema(description = "Slot des Ziels") int targetSlot) implements BattleEvent {
    }

    @Schema(name = "Aging",
            description = "Skill-Event: Aging (Ghost Dragon) — halbiert die HP der obersten Einheit im Ziel-Stack.")
    record Aging(
            @Schema(description = "Auslösender Stack") Side actor,
            @Schema(description = "Slot des Auslösers") int actorSlot,
            @Schema(description = "Gealterte Seite") Side target,
            @Schema(description = "Slot des Ziels") int targetSlot) implements BattleEvent {
    }

    @Schema(name = "FireShield",
            description = "Skill-Event: Fire-Shield-Reflektion. Der angreifende Stack erleidet Reflexschaden auf seinen eigenen Treffer.")
    record FireShield(
            @Schema(description = "Stack mit Fire-Shield (der reflektiert)") Side shielded,
            @Schema(description = "Slot des Fire-Shield-Stacks") int shieldedSlot,
            @Schema(description = "Stack, der angegriffen hat und jetzt Reflexschaden bekommt") Side attacker,
            @Schema(description = "Slot des Angreifers") int attackerSlot,
            @Schema(description = "Reflektierter Schaden") int damage,
            @Schema(description = "Snapshot des Angreifers nach dem Reflexschaden") StackSnapshot attackerAfter) implements BattleEvent {
    }

    @Schema(name = "Rebirth",
            description = "Skill-Event: Rebirth (Phoenix) — eine Phoenix-Einheit wird wieder ins Leben gerufen.")
    record Rebirth(
            @Schema(description = "Wiederbelebter Stack") Side actor,
            @Schema(description = "Slot des wiederbelebten Stacks") int actorSlot,
            @Schema(description = "Zahl der wiederbelebten Einheiten", example = "1") int restoredCount,
            @Schema(description = "Stack-Snapshot nach der Wiederbelebung") StackSnapshot actorAfter) implements BattleEvent {
    }

    @Schema(name = "Resurrection",
            description = "Skill-Event: Resurrection (Arch Angel) — ein verbündeter Stack bekommt gefallene Einheiten zurück, einmal pro Kampf.")
    record Resurrection(
            @Schema(description = "Wirkender Arch-Angel-Stack") Side actor,
            @Schema(description = "Slot des Arch-Angel-Stacks") int actorSlot,
            @Schema(description = "Seite des Ziel-Stacks (immer dieselbe wie actor)") Side target,
            @Schema(description = "Slot des Ziel-Stacks") int targetSlot,
            @Schema(description = "Zahl der zurückgeholten Einheiten (0, wenn nur die oberste Einheit aufgefüllt wurde)", example = "2") int restoredCount,
            @Schema(description = "Snapshot des Ziel-Stacks nach der Resurrection") StackSnapshot targetAfter) implements BattleEvent {
    }

    @Schema(name = "RaiseDemons",
            description = "Skill-Event: Pit Lords beschwören aus einem ganz gefallenen verbündeten Stack Dämonen, einmal pro Kampf. Der Stack kehrt an seinem Platz und in seinem Slot als Dämonen-Stack zurück.")
    record RaiseDemons(
            @Schema(description = "Beschwörender Pit-Lord-Stack") Side actor,
            @Schema(description = "Slot des Pit-Lord-Stacks") int actorSlot,
            @Schema(description = "Seite des gefallenen Stacks (immer dieselbe wie actor)") Side target,
            @Schema(description = "Slot des gefallenen Stacks, der jetzt die Dämonen trägt") int targetSlot,
            @Schema(description = "Zahl der beschworenen Dämonen", example = "4") int raisedCount,
            @Schema(description = "Snapshot des Stacks nach der Beschwörung, unitName ist Demon") StackSnapshot targetAfter) implements BattleEvent {
    }

    @Schema(name = "BattleEnd",
            description = "Abschluss-Event. Liefert Gewinner und finale Survivor-Counts; entspricht typischerweise dem aggregierten BattleResult. `finalStacks` listet den Endstand aller Stacks für Multi-Stack-Auswertungen.")
    record BattleEnd(
            @Schema(description = "Gewinner") Winner winner,
            @Schema(description = "Überlebende Attacker (Summe über alle Stacks)") int attackerSurvivors,
            @Schema(description = "Überlebende Defender (Summe über alle Stacks)") int defenderSurvivors,
            @Schema(description = "Anzahl ausgeführter Runden") int turns,
            @Schema(description = "Endstand aller Stacks. Single-Battle: zwei Snapshots, Army-Battle: bis zu 14.") List<StackSnapshot> finalStacks) implements BattleEvent {
        public BattleEnd {
            finalStacks = List.copyOf(finalStacks);
        }
    }
}
