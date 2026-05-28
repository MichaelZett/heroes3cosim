package de.zettsystems.h3comsim.battle.domain.events;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = BattleEvent.BattleStart.class, name = "BattleStart"),
        @JsonSubTypes.Type(value = BattleEvent.Move.class, name = "Move"),
        @JsonSubTypes.Type(value = BattleEvent.Wait.class, name = "Wait"),
        @JsonSubTypes.Type(value = BattleEvent.Shoot.class, name = "Shoot"),
        @JsonSubTypes.Type(value = BattleEvent.Melee.class, name = "Melee"),
        @JsonSubTypes.Type(value = BattleEvent.Retaliation.class, name = "Retaliation"),
        @JsonSubTypes.Type(value = BattleEvent.TwoBlows.class, name = "TwoBlows"),
        @JsonSubTypes.Type(value = BattleEvent.TwoShots.class, name = "TwoShots"),
        @JsonSubTypes.Type(value = BattleEvent.GoodMorale.class, name = "GoodMorale"),
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
                BattleEvent.Shoot.class, BattleEvent.Melee.class, BattleEvent.Retaliation.class,
                BattleEvent.TwoBlows.class, BattleEvent.TwoShots.class, BattleEvent.GoodMorale.class,
                BattleEvent.MoveBack.class, BattleEvent.DeathStare.class, BattleEvent.Thunderbolts.class,
                BattleEvent.Petrifying.class, BattleEvent.Cursing.class, BattleEvent.Poisoning.class,
                BattleEvent.Diseasing.class, BattleEvent.Aging.class, BattleEvent.FireShield.class,
                BattleEvent.Rebirth.class, BattleEvent.BattleEnd.class
        },
        discriminatorProperty = "type",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "BattleStart", schema = BattleEvent.BattleStart.class),
                @DiscriminatorMapping(value = "Move", schema = BattleEvent.Move.class),
                @DiscriminatorMapping(value = "Wait", schema = BattleEvent.Wait.class),
                @DiscriminatorMapping(value = "Shoot", schema = BattleEvent.Shoot.class),
                @DiscriminatorMapping(value = "Melee", schema = BattleEvent.Melee.class),
                @DiscriminatorMapping(value = "Retaliation", schema = BattleEvent.Retaliation.class),
                @DiscriminatorMapping(value = "TwoBlows", schema = BattleEvent.TwoBlows.class),
                @DiscriminatorMapping(value = "TwoShots", schema = BattleEvent.TwoShots.class),
                @DiscriminatorMapping(value = "GoodMorale", schema = BattleEvent.GoodMorale.class),
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
                @DiscriminatorMapping(value = "BattleEnd", schema = BattleEvent.BattleEnd.class)
        })
public sealed interface BattleEvent {

    @Schema(name = "BattleStart",
            description = "Eröffnungs-Event. Liefert die Battlefield-Dimensionen, alle Obstacles, die initialen Single-Battle-Snapshots (slot 0) und für Army-Battles die vollständige Stack-Liste `stacks`.")
    record BattleStart(
            @Schema(description = "Anzahl Hex-Spalten des Battlefields", example = "15") int battlefieldWidth,
            @Schema(description = "Anzahl Hex-Reihen des Battlefields", example = "11") int battlefieldHeight,
            @Schema(description = "Liste aller blockenden Obstacles auf dem Feld") List<HexCoord> obstacles,
            @Schema(description = "Start-Snapshot des Attacker-Stacks (Slot 0 — für Single-Battle Vollständigkeit, für Army-Battles der Stack auf Slot 0)") StackSnapshot attacker,
            @Schema(description = "Start-Snapshot des Defender-Stacks (Slot 0)") StackSnapshot defender,
            @Schema(description = "Alle initialen Stacks beider Seiten (Army-Battle). Single-Battle: zwei Snapshots — Slot 0 Attacker + Slot 0 Defender.") List<StackSnapshot> stacks) implements BattleEvent {
        public BattleStart {
            obstacles = List.copyOf(obstacles);
            stacks = List.copyOf(stacks);
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
