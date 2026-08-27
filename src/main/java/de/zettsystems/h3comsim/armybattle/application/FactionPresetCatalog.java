package de.zettsystems.h3comsim.armybattle.application;

import de.zettsystems.h3comsim.armybattle.values.FactionPresetDto;
import de.zettsystems.h3comsim.armybattle.values.StackSpec;
import de.zettsystems.h3comsim.battle.domain.Faction;
import de.zettsystems.h3comsim.battle.domain.Hero;
import de.zettsystems.h3comsim.battle.domain.HeroCatalog;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Hartkodierte Wochenproduktions-Compositions pro Faction. Slot-Reihenfolge T7 → T1
 * (stärkster Tier auf Slot 0 / oberster Spawn-Reihe r=0). NEUTRAL hat keinen Preset.
 *
 * <p>Counts entsprechen der H3-Town-Wochenproduktion mit Standard-Dwellings (ohne
 * Horde-Gebäude). Standard-Growth pro Tier: 14, 9, 7, 4, 3, 2, 1; faktions-spezifische
 * Abweichungen siehe Plan-Datei.
 */
@Component
public class FactionPresetCatalog {

    private final Map<Faction, FactionPresetDto> presets = new EnumMap<>(Faction.class);

    public FactionPresetCatalog() {
        presets.put(Faction.CASTLE, preset(Faction.CASTLE, List.of(
                new StackSpec("Arch Angel", 1),
                new StackSpec("Champion", 2),
                new StackSpec("Zealot", 3),
                new StackSpec("Crusader", 4),
                new StackSpec("Royal Griffin", 7),
                new StackSpec("Marksman", 9),
                new StackSpec("Halberdier", 14))));

        presets.put(Faction.RAMPART, preset(Faction.RAMPART, List.of(
                new StackSpec("Gold Dragon", 1),
                new StackSpec("War Unicorn", 2),
                new StackSpec("Dendroid Soldier", 3),
                new StackSpec("Silver Pegasus", 4),
                new StackSpec("Grand Elf", 7),
                new StackSpec("Battle Dwarf", 8),
                new StackSpec("Centaur Captain", 14))));

        presets.put(Faction.TOWER, preset(Faction.TOWER, List.of(
                new StackSpec("Titan", 1),
                new StackSpec("Naga Queen", 2),
                new StackSpec("Master Genie", 3),
                new StackSpec("Arch Magi", 4),
                new StackSpec("Iron Golem", 6),
                new StackSpec("Obsidian Gargoyle", 9),
                new StackSpec("Master Gremlin", 16))));

        presets.put(Faction.INFERNO, preset(Faction.INFERNO, List.of(
                new StackSpec("Arch Devil", 1),
                new StackSpec("Efreet Sultan", 2),
                new StackSpec("Pit Fiend", 3),
                new StackSpec("Horned Demon", 4),
                new StackSpec("Cerberus", 5),
                new StackSpec("Magog", 8),
                new StackSpec("Familiar", 15))));

        presets.put(Faction.NECROPOLIS, preset(Faction.NECROPOLIS, List.of(
                new StackSpec("Ghost Dragon", 1),
                new StackSpec("Dread Knight", 2),
                new StackSpec("Power Lich", 3),
                new StackSpec("Vampire Lord", 4),
                new StackSpec("Wraith", 7),
                new StackSpec("Zombie", 8),
                new StackSpec("Skeleton Warrior", 12))));

        presets.put(Faction.DUNGEON, preset(Faction.DUNGEON, List.of(
                new StackSpec("Black Dragon", 1),
                new StackSpec("Scorpicore", 2),
                new StackSpec("Minotaur King", 3),
                new StackSpec("Medusa Queen", 4),
                new StackSpec("Evil Eye", 7),
                new StackSpec("Harpy Hag", 8),
                new StackSpec("Infernal Troglodyte", 14))));

        presets.put(Faction.STRONGHOLD, preset(Faction.STRONGHOLD, List.of(
                new StackSpec("Ancient Behemoth", 1),
                new StackSpec("Cyclops King", 2),
                new StackSpec("Thunderbird", 3),
                new StackSpec("Ogre Magi", 4),
                new StackSpec("Orc Chieftain", 7),
                new StackSpec("Wolf Raider", 9),
                new StackSpec("Hobgoblin", 15))));

        presets.put(Faction.FORTRESS, preset(Faction.FORTRESS, List.of(
                new StackSpec("Chaos Hydra", 1),
                new StackSpec("Wyvern Monarch", 2),
                new StackSpec("Mighty Gorgon", 3),
                new StackSpec("Greater Basilisk", 4),
                new StackSpec("Serpent Fly", 8),
                new StackSpec("Lizard Warrior", 8),
                new StackSpec("Gnoll Marauder", 12))));

        presets.put(Faction.CONFLUX, preset(Faction.CONFLUX, List.of(
                new StackSpec("Phoenix", 2),
                new StackSpec("Magic Elemental", 2),
                new StackSpec("Magma Elemental", 4),
                new StackSpec("Energy Elemental", 5),
                new StackSpec("Ice Elemental", 6),
                new StackSpec("Storm Elemental", 6),
                new StackSpec("Sprite", 20))));
    }

    public List<FactionPresetDto> all() {
        return List.copyOf(presets.values());
    }

    public FactionPresetDto byFaction(Faction faction) {
        FactionPresetDto preset = presets.get(faction);
        if (preset == null) {
            throw new IllegalArgumentException("No preset for faction " + faction);
        }
        return preset;
    }

    /**
     * Der vorgeschlagene Held kommt aus dem {@link HeroCatalog} statt hier hartkodiert zu
     * stehen — so bleibt die Zuordnung Faktion → Held an genau einer Stelle.
     */
    private static FactionPresetDto preset(Faction faction, List<StackSpec> stacks) {
        return new FactionPresetDto(faction, stacks,
                HeroCatalog.byFaction(faction).map(Hero::name).orElse(null));
    }
}
