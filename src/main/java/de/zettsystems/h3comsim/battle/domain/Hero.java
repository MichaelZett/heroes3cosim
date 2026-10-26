package de.zettsystems.h3comsim.battle.domain;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Ein Held, der eine Armee in die Schlacht führt (Manual S. 33).
 *
 * <p>Die vier Primärwerte:
 * <ul>
 *   <li><strong>Attack</strong> — „A hero's Attack skill number is added to each of their
 *       creature's attack rating"; ausgewertet in {@link Stack#getAttack()}.</li>
 *   <li><strong>Defense</strong> — „is added to each of their army creature's defense rating";
 *       ausgewertet in {@link Stack#getDefense()}.</li>
 *   <li><strong>Power</strong> und <strong>Knowledge</strong> steuern ausschließlich das
 *       Zaubern: {@link #spellPower()} und {@link #maxMana()} (10 Mana je Punkt Knowledge),
 *       ausgewertet in {@link HeroSpellcasting} und {@link SpellDamage}.</li>
 * </ul>
 *
 * <p>Der Katalog führt die Startwerte, mit denen ein Held rekrutiert wird, auf Level 1. Das
 * Level wirkt nur über die Spezialität ({@link HeroSpecialty}); was ein Held beim Aufstieg an
 * Primärwerten und Fertigkeiten dazugewinnt, ist Zufall auf der Abenteuerkarte und wird nicht
 * nachgewürfelt.
 *
 * @param skills    Startfertigkeiten mit ihrer Ausbaustufe. Welche davon wirken, steht bei
 *                  {@link SecondarySkill}.
 * @param specialty Spezialfähigkeit („Unique Ability")
 * @param level     Heldenlevel, ab 1
 * @param spells    Zauberbuch: die Kampfzauber, die der Held kennt
 */
public record Hero(
        String name,
        HeroClass heroClass,
        Faction faction,
        int attack,
        int defense,
        int power,
        int knowledge,
        Map<SecondarySkill, SkillLevel> skills,
        HeroSpecialty specialty,
        int level,
        Set<Spell> spells,
        Equipment artifacts
) {
    public Hero {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(heroClass, "heroClass");
        Objects.requireNonNull(faction, "faction");
        Objects.requireNonNull(skills, "skills");
        Objects.requireNonNull(specialty, "specialty");
        Objects.requireNonNull(artifacts, "artifacts");
        if (level < 1) {
            throw new IllegalArgumentException("level must be at least 1, was " + level);
        }
        requireNonNegative(attack, "attack");
        requireNonNegative(defense, "defense");
        requireNonNegative(power, "power");
        requireNonNegative(knowledge, "knowledge");
        skills = Map.copyOf(skills);
        spells = spells.isEmpty() ? Set.of() : Collections.unmodifiableSet(EnumSet.copyOf(spells));
    }

    /** Held ohne Spezialität auf Level 1 — für Tests, die eine Wirkung isolieren. */
    public Hero(String name, HeroClass heroClass, Faction faction, int attack, int defense,
                int power, int knowledge, Map<SecondarySkill, SkillLevel> skills) {
        this(name, heroClass, faction, attack, defense, power, knowledge, skills,
                HeroSpecialty.Other.NONE, 1);
    }

    /** Held ohne Zauberbuch. */
    public Hero(String name, HeroClass heroClass, Faction faction, int attack, int defense,
                int power, int knowledge, Map<SecondarySkill, SkillLevel> skills,
                HeroSpecialty specialty, int level) {
        this(name, heroClass, faction, attack, defense, power, knowledge, skills, specialty, level, Set.of());
    }

    /** Held ohne Artefakte. */
    public Hero(String name, HeroClass heroClass, Faction faction, int attack, int defense,
                int power, int knowledge, Map<SecondarySkill, SkillLevel> skills,
                HeroSpecialty specialty, int level, Set<Spell> spells) {
        this(name, heroClass, faction, attack, defense, power, knowledge, skills, specialty, level, spells,
                Equipment.NONE);
    }

    /** Derselbe Held auf einem anderen Level. */
    public Hero withLevel(int newLevel) {
        return new Hero(name, heroClass, faction, attack, defense, power, knowledge, skills,
                specialty, newLevel, spells, artifacts);
    }

    /**
     * Derselbe Held mit diesen Zaubern zusätzlich zu denen, die er schon kennt. Seinen Startzauber
     * behält ein Spezialist auch über dem Wisdom-Gate.
     *
     * @throws IllegalArgumentException wenn er einen neuen Zauber nicht lernen kann ({@link #canLearn})
     */
    public Hero withSpellbook(Set<Spell> book) {
        Set<Spell> merged = EnumSet.noneOf(Spell.class);
        merged.addAll(spells);
        for (Spell spell : book) {
            if (!spells.contains(spell) && !canLearn(spell)) {
                throw new IllegalArgumentException(name + " cannot learn " + spell
                        + " (level " + spell.level() + ", Wisdom " + levelOf(SecondarySkill.WISDOM) + ")");
            }
            merged.add(spell);
        }
        return new Hero(name, heroClass, faction, attack, defense, power, knowledge, skills,
                specialty, level, merged, artifacts);
    }

    /**
     * Derselbe Held mit diesen Artefakten statt der bisherigen. Ihre Primärwerte kommen auf die
     * des Helden ({@link #attack()} usw.), ihre Zauber in sein Zauberbuch ({@link #spells()}).
     */
    public Hero withArtifacts(Equipment equipment) {
        return new Hero(name, heroClass, faction, attack, defense, power, knowledge, skills, specialty, level,
                spells, equipment);
    }

    /** Attack mit Artefakten, nicht unter 0. */
    @Override
    public int attack() {
        return Math.max(0, attack + artifacts.stats().attack());
    }

    /** Defense mit Artefakten, nicht unter 0. */
    @Override
    public int defense() {
        return Math.max(0, defense + artifacts.stats().defense());
    }

    /** Power mit Artefakten, nicht unter 0. */
    @Override
    public int power() {
        return Math.max(0, power + artifacts.stats().power());
    }

    /** Knowledge mit Artefakten, nicht unter 0. */
    @Override
    public int knowledge() {
        return Math.max(0, knowledge + artifacts.stats().knowledge());
    }

    /**
     * Zauberbuch mit den Zaubern der Artefakte: Tomes geben alle Zauber ihrer Schule,
     * Spellbinder's Hat alle der Stufe 5, beides ohne Rücksicht auf Wisdom (heroes.thelazy.net).
     */
    @Override
    public Set<Spell> spells() {
        if (artifacts.isEmpty()) {
            return spells;
        }
        Set<Spell> all = EnumSet.noneOf(Spell.class);
        all.addAll(spells);
        artifacts.effects(ArtifactEffect.GrantsSpell.class).forEach(granted -> all.add(granted.spell()));
        Set<MagicSchool> tomes = EnumSet.noneOf(MagicSchool.class);
        artifacts.effects(ArtifactEffect.SchoolSpells.class).forEach(tome -> tomes.add(tome.school()));
        Set<Integer> levels = new HashSet<>();
        artifacts.effects(ArtifactEffect.LevelSpells.class).forEach(hat -> levels.add(hat.level()));
        for (Spell spell : Spell.values()) {
            if (spell.learnable() && (tomes.contains(spell.school()) || levels.contains(spell.level()))) {
                all.add(spell);
            }
        }
        return Collections.unmodifiableSet(all);
    }

    /** Höchstens acht Sekundärfertigkeiten je Held (Manual S. 35). */
    public static final int MAX_SKILLS = 8;

    /**
     * Derselbe Held mit dem, was er unterwegs gesammelt hat: zusätzliche Attack- und
     * Defense-Punkte (Level-Aufstiege) und weitere oder verbesserte Fertigkeiten.
     * Fertigkeiten lassen sich nur hinzufügen oder heben, nie senken — eine einmal gelernte
     * Fertigkeit verliert ein Held in H3 nicht.
     *
     * @throws IllegalArgumentException wenn der Held danach mehr als {@value #MAX_SKILLS}
     *                                  Fertigkeiten hätte
     */
    public Hero withBuild(int attackBonus, int defenseBonus, Map<SecondarySkill, SkillLevel> learned) {
        return withBuild(attackBonus, defenseBonus, 0, 0, learned);
    }

    /** Wie {@link #withBuild(int, int, Map)}, dazu Power- und Knowledge-Punkte. */
    public Hero withBuild(int attackBonus, int defenseBonus, int powerBonus, int knowledgeBonus,
                          Map<SecondarySkill, SkillLevel> learned) {
        requireNonNegative(attackBonus, "attackBonus");
        requireNonNegative(defenseBonus, "defenseBonus");
        requireNonNegative(powerBonus, "powerBonus");
        requireNonNegative(knowledgeBonus, "knowledgeBonus");
        Map<SecondarySkill, SkillLevel> merged = new EnumMap<>(SecondarySkill.class);
        merged.putAll(skills);
        learned.forEach((skill, lvl) -> {
            if (lvl != SkillLevel.NONE) {
                merged.merge(skill, lvl, (a, b) -> a.compareTo(b) >= 0 ? a : b);
            }
        });
        if (merged.size() > MAX_SKILLS) {
            throw new IllegalArgumentException("a hero knows at most " + MAX_SKILLS
                    + " secondary skills, would have " + merged.size());
        }
        return new Hero(name, heroClass, faction, attack + attackBonus, defense + defenseBonus,
                power + powerBonus, knowledge + knowledgeBonus, merged, specialty, level, spells, artifacts);
    }

    /**
     * Höchste Zauberstufe, die der Held lernen kann (Manual S. 39): ohne Wisdom Stufe 2, mit
     * Basic/Advanced/Expert Wisdom Stufe 3/4/5.
     */
    public int maxSpellLevel() {
        return switch (levelOf(SecondarySkill.WISDOM)) {
            case NONE -> 2;
            case BASIC -> 3;
            case ADVANCED -> 4;
            case EXPERT -> 5;
        };
    }

    public boolean canLearn(Spell spell) {
        return spell.learnable() && spell.level() <= maxSpellLevel();
    }

    /** Beherrscht der Held eine Schule des Zaubers, auf welcher Stufe auch immer? */
    public boolean knowsSchoolOf(Spell spell) {
        return spell.schools().stream().anyMatch(school -> levelOf(schoolSkill(school)) != SkillLevel.NONE);
    }

    /**
     * Stufe, mit der der Held den Zauber wirkt: die Fertigkeit seiner Schule (bei Magic Arrow die
     * beste der vier). Ohne die Fertigkeit wirkt er auf Basic (Manual S. 35: die Fertigkeit hebt
     * auf „basic/advanced/expert level").
     */
    public SkillLevel castLevel(Spell spell) {
        SkillLevel best = artifacts.effects(ArtifactEffect.GrantsSpell.class)
                .filter(granted -> granted.spell() == spell)
                .map(ArtifactEffect.GrantsSpell::level)
                .findFirst().orElse(SkillLevel.BASIC);
        for (MagicSchool school : spell.schools()) {
            SkillLevel level = levelOf(schoolSkill(school));
            if (level.compareTo(best) > 0) {
                best = level;
            }
        }
        return best;
    }

    private static SecondarySkill schoolSkill(MagicSchool school) {
        return switch (school) {
            case AIR -> SecondarySkill.AIR_MAGIC;
            case EARTH -> SecondarySkill.EARTH_MAGIC;
            case FIRE -> SecondarySkill.FIRE_MAGIC;
            case WATER -> SecondarySkill.WATER_MAGIC;
        };
    }

    /** Spell Power = Power, mindestens 1. */
    public int spellPower() {
        return Math.max(1, power());
    }

    /**
     * Mana zu Kampfbeginn (Manual S. 33): 10 je Punkt Knowledge, Intelligence hebt das Maximum um
     * 25/50/100 % (S. 37), ein Intelligence-Spezialist um weitere 5 % je Level.
     */
    public int maxMana() {
        int percent = switch (levelOf(SecondarySkill.INTELLIGENCE)) {
            case NONE -> 0;
            case BASIC -> 25;
            case ADVANCED -> 50;
            case EXPERT -> 100;
        };
        percent = percent * specialty.skillFactorPerMille(SecondarySkill.INTELLIGENCE, level) / 1000;
        return knowledge() * 10 * (100 + percent) / 100;
    }

    /** Sorcery (Manual S. 38): +5/10/15 % Zauberschaden, ein Spezialist +5 % je Level darauf. */
    public int sorceryPercent() {
        int percent = switch (levelOf(SecondarySkill.SORCERY)) {
            case NONE -> 0;
            case BASIC -> 5;
            case ADVANCED -> 10;
            case EXPERT -> 15;
        };
        return percent * specialty.skillFactorPerMille(SecondarySkill.SORCERY, level) / 1000;
    }

    /**
     * Resistance (Manual S. 38): 5/10/20 % Chance, dass ein feindlicher Zauber auf die eigenen
     * Truppen versagt; ein Spezialist (Thorgrim) +5 % je Level darauf. Artefakte addieren ihre
     * Prozente, auch ohne die Fertigkeit (heroes.thelazy.net, <em>Garniture of Interference</em>).
     */
    public int resistancePercent() {
        int percent = switch (levelOf(SecondarySkill.RESISTANCE)) {
            case NONE -> 0;
            case BASIC -> 5;
            case ADVANCED -> 10;
            case EXPERT -> 20;
        };
        return percent * specialty.skillFactorPerMille(SecondarySkill.RESISTANCE, level) / 1000
                + artifacts.magicResistance();
    }

    private static void requireNonNegative(int value, String field) {
        if (value < 0) {
            throw new IllegalArgumentException(field + " must not be negative, was " + value);
        }
    }

    /** Ausbaustufe der Fertigkeit, {@link SkillLevel#NONE} wenn der Held sie nicht hat. */
    public SkillLevel levelOf(SecondarySkill skill) {
        return skills.getOrDefault(skill, SkillLevel.NONE);
    }

    /**
     * Offense, Manual S. 38: „Increases the amount of hand-to-hand damage the hero's troops
     * inflict in combat" — 10/20/30 %. Gilt ausdrücklich nur für Nahkampf.
     */
    public int offenseBonusPercent() {
        return switch (levelOf(SecondarySkill.OFFENSE)) {
            case NONE -> 0;
            case BASIC -> 10;
            case ADVANCED -> 20;
            case EXPERT -> 30;
        };
    }

    /**
     * Archery, Manual S. 35: „Increases the damage done by ranged attackers in the hero's army"
     * — 10/25/50 %. Die Stufen springen weiter als bei Offense; das ist so im Manual.
     */
    public int archeryBonusPercent() {
        return switch (levelOf(SecondarySkill.ARCHERY)) {
            case NONE -> 0;
            case BASIC -> 10;
            case ADVANCED -> 25;
            case EXPERT -> 50;
        };
    }

    /**
     * Offense in Promille, mit Spezialität (+5 % je Level auf den Effekt, siehe
     * {@link HeroSpecialty.Skill}). Crag Hack auf Level 1 mit Advanced Offense: 210 ‰.
     */
    public int offenseBonusPerMille() {
        return offenseBonusPercent() * specialty.skillFactorPerMille(SecondarySkill.OFFENSE, level) / 100;
    }

    /**
     * Archery in Promille, mit Spezialität. Die Bogen-Artefakte addieren ihre Prozente, aber nur,
     * wenn der Held Archery hat (heroes.thelazy.net, <em>Bow of Elven Cherrywood</em>).
     */
    public int archeryBonusPerMille() {
        int artifactPerMille = levelOf(SecondarySkill.ARCHERY) == SkillLevel.NONE ? 0 : artifacts.archery() * 10;
        return archeryBonusPercent() * specialty.skillFactorPerMille(SecondarySkill.ARCHERY, level) / 100
                + artifactPerMille;
    }

    /** Zusätzliche Runden für Zauber mit Dauer: „1 Round/Power" plus Artefakte. */
    public int spellDuration() {
        return spellPower() + artifacts.spellDuration();
    }

    /** Armorer in Promille, mit Spezialität. */
    public int armorerReductionPerMille() {
        return armorerReductionPercent() * specialty.skillFactorPerMille(SecondarySkill.ARMORER, level) / 100;
    }

    /** Ballista-Schüsse je Runde: zwei ab Advanced Artillery (SoD), sonst einer. */
    public int ballistaShots() {
        return levelOf(SecondarySkill.ARTILLERY).compareTo(SkillLevel.ADVANCED) >= 0 ? 2 : 1;
    }

    /** Artillery: Chance in Prozent, dass ein Ballista-Schuss doppelten Grundschaden macht. */
    public int artilleryDoubleDamagePercent() {
        return switch (levelOf(SecondarySkill.ARTILLERY)) {
            case NONE -> 0;
            case BASIC -> 50;
            case ADVANCED -> 75;
            case EXPERT -> 100;
        };
    }

    /**
     * Obergrenze der Heilung des Sanitätszelts (Manual S. 117 und S. 37): 25 ohne First Aid,
     * 50/75/100 mit der Fertigkeit; ein First-Aid-Spezialist hebt das um 5 % je Level.
     */
    public int firstAidMaxHeal() {
        int base = switch (levelOf(SecondarySkill.FIRST_AID)) {
            case NONE -> 25;
            case BASIC -> 50;
            case ADVANCED -> 75;
            case EXPERT -> 100;
        };
        return levelOf(SecondarySkill.FIRST_AID) == SkillLevel.NONE ? base
                : base * specialty.skillFactorPerMille(SecondarySkill.FIRST_AID, level) / 1000;
    }

    /**
     * Attack der Ballista dieses Helden: 10 plus seine Attack plus Ballista-Spezialität. Die
     * Schadenstabelle auf heroes.thelazy.net (<em>War Machine</em>, SoD-Werte) rechnet so: bei
     * Held-Attack 1 sind es 5 × 1.05 = 5.25 gegen Defense 10, bei 2 dann 7.5 × 1.10 = 8.25.
     */
    public int ballistaAttack() {
        return WarMachine.BALLISTA.attack() + attack() + specialty.ballistaAttackBonus(level);
    }

    /** Attack-Aufschlag der Spezialität für diese Kreatur. */
    public int specialtyAttackBonus(Unit unit) {
        return specialty.attackBonus(unit, level);
    }

    /** Defense-Aufschlag der Spezialität für diese Kreatur. */
    public int specialtyDefenseBonus(Unit unit) {
        return specialty.defenseBonus(unit, level);
    }

    /**
     * Armorer, Manual S. 35: „Reduces the amount of damage received by the hero's troops in
     * combat" — 5/10/15 %. Gilt für jeden eingehenden Schaden, egal ob Nah- oder Fernkampf.
     */
    public int armorerReductionPercent() {
        return switch (levelOf(SecondarySkill.ARMORER)) {
            case NONE -> 0;
            case BASIC -> 5;
            case ADVANCED -> 10;
            case EXPERT -> 15;
        };
    }

    /**
     * Leadership, Manual S. 37: „The hero's troops gain a morale rating bonus" — +1/2/3.
     * Anders als die drei Schadens-Fertigkeiten wirkt Leadership nicht auf den Schaden,
     * sondern auf das Armee-Moral-Rating; ausgewertet in {@link ArmyMorale}.
     *
     * <p>Das Manual schließt Necromancer und Death Knights von der Fertigkeit aus — der
     * Katalog bildet das über die Daten ab, nicht über eine Prüfung hier: kein
     * Necropolis-Held bringt Leadership mit.
     */
    public int leadershipMoraleBonus() {
        return switch (levelOf(SecondarySkill.LEADERSHIP)) {
            case NONE -> 0;
            case BASIC -> 1;
            case ADVANCED -> 2;
            case EXPERT -> 3;
        };
    }

    /**
     * Luck, Manual S. 37: „The hero's troops gain a bonus to their luck rating" — +1/2/3, geht in
     * das Armee-Glück ein ({@link ArmyLuck}).
     */
    public int luckBonus() {
        return switch (levelOf(SecondarySkill.LUCK)) {
            case NONE -> 0;
            case BASIC -> 1;
            case ADVANCED -> 2;
            case EXPERT -> 3;
        };
    }
}
