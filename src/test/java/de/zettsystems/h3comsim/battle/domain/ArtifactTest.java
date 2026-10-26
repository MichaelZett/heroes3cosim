package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.BattleEvent;
import de.zettsystems.h3comsim.battle.domain.events.ListEventCollector;
import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Artefakt-Katalog, Plätze am Helden und die Werte, die Artefakte Helden und Kreaturen geben. */
class ArtifactTest {

    private static final Battlefield FIELD = Battlefield.STANDARD;

    @Test
    void combination_artifacts_carry_the_sum_of_their_parts() {
        for (Artifact combo : List.of(Artifact.ANGELIC_ALLIANCE, Artifact.TITANS_THUNDER, Artifact.ARMOR_OF_THE_DAMNED)) {
            assertThat(combo.stats()).as(combo.displayName()).isEqualTo(sum(combo.components()));
        }
        Artifact.Stats parts = sum(Artifact.POWER_OF_THE_DRAGON_FATHER.components());
        assertThat(Artifact.POWER_OF_THE_DRAGON_FATHER.stats())
                .as("eigene +6 auf alle vier Werte")
                .isEqualTo(new Artifact.Stats(parts.attack() + 6, parts.defense() + 6, parts.power() + 6,
                        parts.knowledge() + 6));
        assertThat(Arrays.stream(Artifact.values()).filter(Artifact::isCombination))
                .allMatch(combo -> combo.rarity() == Artifact.Rarity.RELIC);
    }

    private static Artifact.Stats sum(List<Artifact> artifacts) {
        return new Artifact.Stats(
                artifacts.stream().mapToInt(a -> a.stats().attack()).sum(),
                artifacts.stream().mapToInt(a -> a.stats().defense()).sum(),
                artifacts.stream().mapToInt(a -> a.stats().power()).sum(),
                artifacts.stream().mapToInt(a -> a.stats().knowledge()).sum());
    }

    @Test
    void combination_artifacts_block_the_slots_of_their_other_parts() {
        assertThat(Artifact.ANGELIC_ALLIANCE.blocks()).containsExactlyInAnyOrder(ArtifactSlot.Kind.HELMET,
                ArtifactSlot.Kind.NECKLACE, ArtifactSlot.Kind.TORSO, ArtifactSlot.Kind.FEET, ArtifactSlot.Kind.SHIELD);
        assertThat(Artifact.ELIXIR_OF_LIFE.blocks()).containsExactly(ArtifactSlot.Kind.RING, ArtifactSlot.Kind.RING);
        assertThat(Artifact.BOW_OF_THE_SHARPSHOOTER.blocks()).containsExactly(ArtifactSlot.Kind.MISC,
                ArtifactSlot.Kind.MISC);
        assertThat(Artifact.CENTAURS_AXE.blocks()).isEmpty();
    }

    @Test
    void equipment_accepts_only_what_the_hero_screen_allows() {
        assertThat(Equipment.of(Artifact.ELIXIR_OF_LIFE, Artifact.BOW_OF_THE_SHARPSHOOTER, Artifact.ORB_OF_SILT)
                .slots()).hasSize(3);
        assertThatThrownBy(() -> new Equipment(Map.of(ArtifactSlot.HEAD, Artifact.CENTAURS_AXE)))
                .hasMessageContaining("does not fit");
        assertThatThrownBy(() -> new Equipment(Map.of(ArtifactSlot.MISC_1, Artifact.CLOVER_OF_FORTUNE,
                ArtifactSlot.MISC_2, Artifact.CLOVER_OF_FORTUNE))).hasMessageContaining("twice");
        assertThatThrownBy(() -> Equipment.of(Artifact.ANGELIC_ALLIANCE, Artifact.HELM_OF_CHAOS))
                .as("die Allianz sperrt den Helm").hasMessageContaining("HELMET");
        assertThatThrownBy(() -> Equipment.of(Artifact.ELIXIR_OF_LIFE, Artifact.RING_OF_CONJURING))
                .as("das Elixir sperrt beide Ringe").hasMessageContaining("RING");
        assertThatThrownBy(() -> Equipment.of(Artifact.BOW_OF_THE_SHARPSHOOTER, Artifact.ORB_OF_SILT,
                Artifact.ORB_OF_THE_FIRMAMENT, Artifact.ORB_OF_DRIVING_RAIN)).hasMessageContaining("MISC");
        assertThatThrownBy(() -> Equipment.of(Artifact.BOW_OF_THE_SHARPSHOOTER, Artifact.GOLDEN_BOW,
                Artifact.BOW_OF_ELVEN_CHERRYWOOD)).hasMessageContaining("part of");
        assertThatThrownBy(() -> Equipment.of(Artifact.CENTAURS_AXE, Artifact.SWORD_OF_HELLFIRE))
                .hasMessageContaining("no free WEAPON");
        assertThat(Equipment.of(Artifact.SWORD_OF_HELLFIRE, Artifact.CLOVER_OF_FORTUNE).value()).isEqualTo(7000);
    }

    @Test
    void primary_skills_come_on_top_and_never_drop_below_zero() {
        Hero sorsha = HeroCatalog.SORSHA.withArtifacts(Equipment.of(Artifact.TITANS_GLADIUS, Artifact.HELM_OF_CHAOS));
        assertThat(List.of(sorsha.attack(), sorsha.defense(), sorsha.power(), sorsha.knowledge()))
                .containsExactly(14, 0, 1, 4);
        assertThat(sorsha.maxMana()).isEqualTo(40);
        assertThat(sorsha.withArtifacts(Equipment.NONE).attack()).as("Artefakte ersetzen, nicht addieren")
                .isEqualTo(2);
        assertThat(sorsha.withLevel(5).artifacts()).isEqualTo(sorsha.artifacts());
    }

    @Test
    void tomes_and_the_hat_teach_spells_beyond_wisdom() {
        Hero tome = HeroCatalog.SORSHA.withArtifacts(Equipment.of(Artifact.TOME_OF_FIRE_MAGIC));
        assertThat(tome.spells()).contains(Spell.FIREBALL, Spell.ARMAGEDDON, Spell.SACRIFICE)
                .doesNotContain(Spell.LIGHTNING_BOLT, Spell.FRENZY);
        Hero hat = HeroCatalog.SORSHA.withArtifacts(Equipment.of(Artifact.SPELLBINDERS_HAT));
        assertThat(hat.spells()).contains(Spell.IMPLOSION, Spell.MAGIC_MIRROR, Spell.SUMMON_WATER_ELEMENTAL)
                .doesNotContain(Spell.TITANS_LIGHTNING_BOLT, Spell.FIREBALL);
        Hero thunder = HeroCatalog.SORSHA.withArtifacts(Equipment.of(Artifact.TITANS_THUNDER));
        assertThat(thunder.spells()).containsExactly(Spell.TITANS_LIGHTNING_BOLT);
        assertThat(HeroCatalog.SORSHA.canLearn(Spell.TITANS_LIGHTNING_BOLT)).isFalse();
        Hero blade = HeroCatalog.SORSHA.withArtifacts(Equipment.of(Artifact.ARMAGEDDONS_BLADE));
        assertThat(blade.castLevel(Spell.ARMAGEDDON)).as("die Klinge wirkt auf Expert").isEqualTo(SkillLevel.EXPERT);
        assertThat(blade.castLevel(Spell.FIREBALL)).isEqualTo(SkillLevel.BASIC);
    }

    @Test
    void conjuring_resistance_and_archery_artifacts_add_to_the_skills() {
        Hero magi = HeroCatalog.SORSHA.withArtifacts(Equipment.of(Artifact.RING_OF_THE_MAGI));
        assertThat(magi.spellDuration()).isEqualTo(1 + 56);
        Hero resistant = HeroCatalog.SORSHA.withArtifacts(Equipment.of(Artifact.BOOTS_OF_POLARITY,
                Artifact.GARNITURE_OF_INTERFERENCE));
        assertThat(resistant.resistancePercent()).as("auch ohne Resistance").isEqualTo(20);
        Hero archer = HeroCatalog.JENOVA.withArtifacts(Equipment.of(Artifact.ANGEL_FEATHER_ARROWS));
        assertThat(archer.archeryBonusPerMille()).isEqualTo(250 + 150);
        Hero noArchery = HeroCatalog.SORSHA.withArtifacts(Equipment.of(Artifact.ANGEL_FEATHER_ARROWS));
        assertThat(noArchery.archeryBonusPerMille()).as("ohne Archery wirkungslos").isZero();
    }

    @Test
    void health_speed_and_dragon_blood_reach_the_creatures() {
        Hero hero = HeroCatalog.SORSHA.withArtifacts(Equipment.of(Artifact.ELIXIR_OF_LIFE, Artifact.CAPE_OF_VELOCITY,
                Artifact.VIAL_OF_DRAGON_BLOOD));
        Stack imps = stack(UnitCatalog.IMP, Side.ATTACKER);
        Stack skeletons = stack(UnitCatalog.SKELETON, Side.ATTACKER);
        Stack dragons = stack(UnitCatalog.RED_DRAGON, Side.ATTACKER);
        Stack flies = stack(UnitCatalog.DRAGON_FLY, Side.ATTACKER);
        new BattleSetup(List.of(imps, skeletons, dragons, flies), List.of(stack(UnitCatalog.PEASANT, Side.DEFENDER)),
                FIELD, hero, null);

        assertThat(imps.maxHealth()).as("4 + 4 + 25 % von 4").isEqualTo(9);
        assertThat(imps.getCurrentHealth()).isEqualTo(9);
        assertThat(skeletons.maxHealth()).as("Untote ohne die 25 %").isEqualTo(10);
        assertThat(imps.getSpeed()).isEqualTo(UnitCatalog.IMP.speed() + 2);
        assertThat(dragons.getAttack() - flies.getAttack())
                .isEqualTo(UnitCatalog.RED_DRAGON.attack() + 5 - UnitCatalog.DRAGON_FLY.attack());
        assertThat(UnitCatalog.DRAGON_FLY.isDragon()).isFalse();
    }

    @Test
    void the_elixir_regenerates_living_creatures() {
        Hero hero = HeroCatalog.SORSHA.withArtifacts(Equipment.of(Artifact.ELIXIR_OF_LIFE));
        Stack pikemen = stack(UnitCatalog.PIKEMAN, Side.ATTACKER);
        Stack skeletons = stack(UnitCatalog.SKELETON, Side.ATTACKER);
        new BattleSetup(List.of(pikemen, skeletons), List.of(stack(UnitCatalog.PEASANT, Side.DEFENDER)), FIELD,
                hero, null);
        pikemen.takeDamage(5, Set.of());
        skeletons.takeDamage(5, Set.of());
        pikemen.endTurn();
        skeletons.endTurn();

        assertThat(pikemen.getCurrentHealth()).isEqualTo(pikemen.maxHealth());
        assertThat(skeletons.getCurrentHealth()).isEqualTo(skeletons.maxHealth() - 5);
    }

    @Test
    void luck_and_morale_artifacts_count_and_the_negators_hit_both_sides() {
        Hero lucky = HeroCatalog.SORSHA.withArtifacts(Equipment.of(Artifact.PENDANT_OF_COURAGE));
        Stack pikemen = stack(UnitCatalog.PIKEMAN, Side.ATTACKER);
        Stack imps = stack(UnitCatalog.IMP, Side.DEFENDER);
        new BattleSetup(List.of(pikemen), List.of(imps), FIELD, lucky, HeroCatalog.NYMUS);
        assertThat(pikemen.getLuck()).isEqualTo(3);
        assertThat(pikemen.getMorale()).isEqualTo(3);

        Hero gloomy = HeroCatalog.NYMUS.withArtifacts(Equipment.of(Artifact.HOURGLASS_OF_THE_EVIL_HOUR,
                Artifact.SPIRIT_OF_OPPRESSION));
        Stack moreImps = stack(UnitCatalog.IMP, Side.DEFENDER);
        Stack zombies = stack(UnitCatalog.ZOMBIE, Side.DEFENDER);
        new BattleSetup(List.of(pikemen), List.of(moreImps, zombies), FIELD, lucky, gloomy);
        assertThat(pikemen.getLuck()).isZero();
        assertThat(pikemen.getMorale()).isZero();
        assertThat(moreImps.getMorale()).as("schlechte Moral bleibt").isNegative();
    }

    @Test
    void the_angelic_alliance_unites_good_and_neutral_towns() {
        List<Unit> mixed = List.of(UnitCatalog.PIKEMAN, UnitCatalog.DWARF, UnitCatalog.GREMLIN, UnitCatalog.GNOLL,
                UnitCatalog.GOBLIN);
        Hero allied = HeroCatalog.SORSHA.withArtifacts(Equipment.of(Artifact.ANGELIC_ALLIANCE));

        assertThat(moraleOf(mixed, allied) - moraleOf(mixed, HeroCatalog.SORSHA)).as("−3 wird zu +1").isEqualTo(4);
        List<Unit> withImps = List.of(UnitCatalog.PIKEMAN, UnitCatalog.DWARF, UnitCatalog.IMP);
        assertThat(moraleOf(withImps, allied)).as("zwei Städte: kein Bonus, keine Strafe")
                .isEqualTo(HeroCatalog.SORSHA.leadershipMoraleBonus());
    }

    private static int moraleOf(List<Unit> units, Hero hero) {
        List<Stack> stacks = units.stream().map(u -> stack(u, Side.ATTACKER)).toList();
        return ArmyMorale.ratingOf(stacks, hero, List.of());
    }

    @Test
    void recanters_cloak_and_the_orb_of_inhibition_silence_both_heroes() {
        BattleSetup cloak = setup(HeroCatalog.SORSHA.withArtifacts(Equipment.of(Artifact.RECANTERS_CLOAK)),
                HeroCatalog.NYMUS);
        assertThat(HeroSpellcasting.castable(Spell.WEAKNESS, cloak)).isTrue();
        assertThat(HeroSpellcasting.castable(Spell.FIREBALL, cloak)).isFalse();
        assertThat(cloak.creatureMagicBlocked()).isFalse();

        BattleSetup orb = setup(HeroCatalog.SORSHA, HeroCatalog.NYMUS.withArtifacts(Equipment.of(Artifact.ORB_OF_INHIBITION)));
        assertThat(HeroSpellcasting.castable(Spell.MAGIC_ARROW, orb)).isFalse();
        assertThat(orb.creatureMagicBlocked()).isTrue();
        assertThat(CreatureSpellcasting.blockedByArtifact(orb)).isTrue();
        assertThat(CreatureSpellcasting.blocked(orb)).as("Faerie Dragons zaubern weiter").isFalse();
    }

    @Test
    void the_orb_of_inhibition_stops_resurrecting_arch_angels() {
        Stack angels = new Stack(UnitCatalog.ARCH_ANGEL, 2, new Hex(3, 3), Side.ATTACKER, 0);
        Stack pikemen = new Stack(UnitCatalog.PIKEMAN, 20, new Hex(3, 5), Side.ATTACKER, 1);
        BattleSetup setup = new BattleSetup(List.of(angels, pikemen), List.of(stack(UnitCatalog.PEASANT, Side.DEFENDER)),
                FIELD, HeroCatalog.SORSHA, HeroCatalog.NYMUS.withArtifacts(Equipment.of(Artifact.ORB_OF_INHIBITION)));
        pikemen.takeDamage(50, Set.of());

        assertThat(Resurrection.isValidTarget(angels, pikemen, setup)).isFalse();
    }

    @Test
    void pendants_and_the_dragon_father_make_the_army_immune() {
        Hero hero = HeroCatalog.SORSHA.withArtifacts(Equipment.of(Artifact.PENDANT_OF_HOLINESS, Artifact.BADGE_OF_COURAGE,
                Artifact.SPHERE_OF_PERMANENCE));
        Stack pikemen = stack(UnitCatalog.PIKEMAN, Side.ATTACKER);
        setup(List.of(pikemen), hero, null);
        assertThat(SpellResistance.isImmune(pikemen, Spell.CURSE)).isTrue();
        assertThat(SpellResistance.isImmune(pikemen, Spell.BLIND)).as("Badge: Gedankenzauber").isTrue();
        assertThat(SpellResistance.isImmune(pikemen, Spell.DISPEL)).isTrue();
        assertThat(SpellResistance.isImmune(pikemen, Spell.SLOW)).isFalse();

        Hero dragonFather = HeroCatalog.SORSHA.withArtifacts(Equipment.of(Artifact.POWER_OF_THE_DRAGON_FATHER));
        Stack guarded = stack(UnitCatalog.PIKEMAN, Side.ATTACKER);
        setup(List.of(guarded), dragonFather, null);
        assertThat(SpellResistance.isImmune(guarded, Spell.PRAYER)).as("auch eigene Zauber").isTrue();
        assertThat(SpellResistance.isImmune(guarded, Spell.IMPLOSION)).isFalse();
    }

    @Test
    void the_orb_of_vulnerability_lifts_natural_immunity_one_sided() {
        Stack blackDragons = stack(UnitCatalog.BLACK_DRAGON, Side.DEFENDER);
        Stack dwarves = new Stack(UnitCatalog.DWARF, 10, new Hex(12, 7), Side.DEFENDER, 1);
        Stack ownDragons = stack(UnitCatalog.BLACK_DRAGON, Side.ATTACKER);
        BattleSetup setup = new BattleSetup(List.of(ownDragons), List.of(blackDragons, dwarves), FIELD,
                HeroCatalog.SORSHA.withArtifacts(Equipment.of(Artifact.ORB_OF_VULNERABILITY)), HeroCatalog.NYMUS);

        assertThat(SpellResistance.isImmune(blackDragons, Spell.IMPLOSION)).isTrue();
        assertThat(SpellResistance.isImmune(blackDragons, Spell.IMPLOSION, Side.ATTACKER, setup)).isFalse();
        assertThat(SpellResistance.isImmune(ownDragons, Spell.IMPLOSION, Side.DEFENDER, setup))
                .as("der Gegner trifft die Truppen des Besitzers").isFalse();
        assertThat(SpellResistance.isImmune(blackDragons, Spell.IMPLOSION, Side.DEFENDER, setup))
                .as("aber nicht seine eigenen").isTrue();
        assertThat(SpellResistance.resistancePercent(dwarves, Side.ATTACKER, setup)).isZero();
        assertThat(SpellResistance.resistancePercent(dwarves, Side.DEFENDER, setup)).isEqualTo(20);
    }

    @Test
    void the_orb_of_vulnerability_keeps_undead_and_war_machine_immunities() {
        Stack skeletons = stack(UnitCatalog.SKELETON, Side.DEFENDER);
        Stack giants = new Stack(UnitCatalog.GIANT, 5, new Hex(12, 7), Side.DEFENDER, 1);
        Hero orb = HeroCatalog.SORSHA.withArtifacts(Equipment.of(Artifact.ORB_OF_VULNERABILITY));
        BattleSetup setup = new BattleSetup(List.of(stack(UnitCatalog.PIKEMAN, Side.ATTACKER)),
                List.of(skeletons, giants), FIELD, orb, HeroCatalog.NYMUS).equip(Side.DEFENDER, Set.of(WarMachine.BALLISTA));
        Stack ballista = setup.machinesOf(Side.DEFENDER).getFirst();

        assertThat(SpellResistance.isImmune(skeletons, Spell.BLIND, Side.ATTACKER, setup)).isTrue();
        assertThat(SpellResistance.isImmune(skeletons, Spell.SLOW, Side.ATTACKER, setup)).isFalse();
        assertThat(SpellResistance.isImmune(giants, Spell.BLIND)).isTrue();
        assertThat(SpellResistance.isImmune(giants, Spell.BLIND, Side.ATTACKER, setup))
                .as("die Immunität der Giants ist natürlich").isFalse();
        assertThat(SpellResistance.isImmune(ballista, Spell.SLOW, Side.ATTACKER, setup)).isTrue();
        assertThat(SpellResistance.isImmune(ballista, Spell.CURSE, Side.ATTACKER, setup)).isFalse();
    }

    @Test
    void orbs_raise_spell_damage_and_the_titans_bolt_hits_for_600() {
        Hero fire = HeroCatalog.SORSHA.withArtifacts(Equipment.of(Artifact.ORB_OF_TEMPESTUOUS_FIRE));
        Stack pikemen = stack(UnitCatalog.PIKEMAN, Side.DEFENDER);
        setup(List.of(stack(UnitCatalog.PEASANT, Side.ATTACKER)), fire, null, pikemen);

        assertThat(SpellDamage.toTarget(Spell.FIREBALL, 100, fire, pikemen)).isEqualTo(150);
        assertThat(SpellDamage.toTarget(Spell.MAGIC_ARROW, 100, fire, pikemen)).as("alle vier Schulen").isEqualTo(150);
        assertThat(SpellDamage.toTarget(Spell.ICE_BOLT, 100, fire, pikemen)).isEqualTo(100);
        assertThat(SpellDamage.base(Spell.TITANS_LIGHTNING_BOLT, SkillLevel.BASIC, 1)).isEqualTo(600);
    }

    @Test
    void the_titans_bolt_costs_no_mana() {
        Hero thunder = HeroCatalog.SORSHA.withArtifacts(Equipment.of(Artifact.TITANS_THUNDER));
        Stack dragons = stack(UnitCatalog.RED_DRAGON, Side.DEFENDER);
        BattleSetup setup = setup(List.of(stack(UnitCatalog.PIKEMAN, Side.ATTACKER)), thunder, null, dragons);

        assertThat(HeroSpellcasting.cost(Spell.TITANS_LIGHTNING_BOLT, Side.ATTACKER, setup)).isZero();
        HeroSpellcasting.Cast cast = HeroSpellcasting.decide(thunder, Side.ATTACKER, 0, setup);
        assertThat(cast).isNotNull();
        assertThat(cast.spell()).isEqualTo(Spell.TITANS_LIGHTNING_BOLT);
    }

    @Test
    void bows_lift_the_shooting_penalties() {
        Hero bow = HeroCatalog.SORSHA.withArtifacts(Equipment.of(Artifact.BOW_OF_THE_SHARPSHOOTER));
        Stack archers = stack(UnitCatalog.ARCHER, Side.ATTACKER);
        Stack plain = stack(UnitCatalog.ARCHER, Side.DEFENDER);
        new BattleSetup(List.of(archers), List.of(plain), FIELD, bow, HeroCatalog.NYMUS);

        assertThat(archers.ignoresShootingPenalties()).isTrue();
        assertThat(archers.shootsWhenAdjacent()).isTrue();
        assertThat(plain.ignoresShootingPenalties()).isFalse();
        Stack pikemen = stack(UnitCatalog.PIKEMAN, Side.ATTACKER);
        new BattleSetup(List.of(pikemen), List.of(plain), FIELD, bow, null);
        assertThat(pikemen.shootsWhenAdjacent()).as("nur Schützen").isFalse();
    }

    @Test
    void shooters_with_the_sharpshooters_bow_shoot_point_blank() {
        Stack marksmen = new Stack(UnitCatalog.MARKSMAN, 20, new Hex(5, 5), Side.ATTACKER, 0);
        Stack pikemen = new Stack(UnitCatalog.PIKEMAN, 5, new Hex(6, 5), Side.DEFENDER, 0);
        Hero bow = HeroCatalog.SORSHA.withArtifacts(Equipment.of(Artifact.BOW_OF_THE_SHARPSHOOTER));
        ListEventCollector events = new ListEventCollector();
        new Battle(new Random(1), new GreedyAutoSolver(), events)
                .simulate(new BattleSetup(List.of(marksmen), List.of(pikemen), FIELD, bow, null));

        BattleEvent first = events.events().stream()
                .filter(e -> e instanceof BattleEvent.Shoot || e instanceof BattleEvent.Melee)
                .findFirst().orElseThrow();
        assertThat(first).isInstanceOfSatisfying(BattleEvent.Shoot.class, shot -> {
            assertThat(shot.actor()).isEqualTo(Side.ATTACKER);
            assertThat(shot.distance()).isEqualTo(1);
        });
    }

    @Test
    void the_armor_of_the_damned_waits_for_the_first_own_turn() {
        Stack pikemen = new Stack(UnitCatalog.PIKEMAN, 20, new Hex(2, 5), Side.ATTACKER, 0);
        Stack dwarves = new Stack(UnitCatalog.DWARF, 20, new Hex(14, 5), Side.DEFENDER, 0);
        Hero damned = HeroCatalog.SORSHA.withArtifacts(Equipment.of(Artifact.ARMOR_OF_THE_DAMNED));
        ListEventCollector events = new ListEventCollector();
        new Battle(new Random(3), new GreedyAutoSolver(), events)
                .simulate(new BattleSetup(List.of(pikemen), List.of(dwarves), FIELD, damned, null));

        List<BattleEvent.ArtifactSpell> casts = events.events().stream()
                .filter(BattleEvent.ArtifactSpell.class::isInstance).map(BattleEvent.ArtifactSpell.class::cast).toList();
        assertThat(casts).hasSize(4).allSatisfy(c -> {
            assertThat(c.artifact()).isEqualTo(Artifact.ARMOR_OF_THE_DAMNED);
            assertThat(c.level()).isEqualTo(SkillLevel.EXPERT);
        });
    }

    @Test
    void on_cursed_ground_the_armor_casts_only_its_first_level_spells_on_basic() {
        Stack pikemen = stack(UnitCatalog.PIKEMAN, Side.ATTACKER);
        Stack imps = stack(UnitCatalog.IMP, Side.DEFENDER);
        Hero damned = HeroCatalog.SORSHA.withArtifacts(Equipment.of(Artifact.ARMOR_OF_THE_DAMNED));
        BattleSetup setup = new BattleSetup(List.of(pikemen), List.of(imps), FIELD, damned, null)
                .ground(null, MagicTerrain.CURSED_GROUND);

        List<ArtifactSpells.Hit> hits = ArtifactSpells.cast(Side.ATTACKER, setup, new Random(1));
        assertThat(hits).extracting(ArtifactSpells.Hit::spell).containsExactlyInAnyOrder(Spell.SLOW, Spell.CURSE);
        assertThat(hits).allSatisfy(hit -> assertThat(hit.level()).isEqualTo(SkillLevel.BASIC));
        assertThat(imps.hasSpell(Spell.SLOW)).isTrue();
        assertThat(ArtifactSpells.cast(Side.DEFENDER, setup, new Random(1))).as("ohne Helden nichts").isEmpty();
    }

    @Test
    void equipment_compares_by_its_slots() {
        Equipment a = Equipment.of(Artifact.CLOVER_OF_FORTUNE);
        Equipment b = new Equipment(Map.of(ArtifactSlot.MISC_1, Artifact.CLOVER_OF_FORTUNE));
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b).isNotEqualTo(Equipment.NONE);
        assertThat(a.toString()).contains("CLOVER_OF_FORTUNE");
        assertThat(a.has(Artifact.CLOVER_OF_FORTUNE)).isTrue();
    }

    private static Stack stack(Unit unit, Side side) {
        return new Stack(unit, 10, new Hex(side == Side.ATTACKER ? 5 : 12, 5), side, 0);
    }

    private static BattleSetup setup(Hero attacker, Hero defender) {
        return new BattleSetup(List.of(stack(UnitCatalog.PIKEMAN, Side.ATTACKER)),
                List.of(stack(UnitCatalog.IMP, Side.DEFENDER)), FIELD, attacker, defender);
    }

    private static BattleSetup setup(List<Stack> attackers, Hero attacker, @Nullable Hero defender) {
        return setup(attackers, attacker, defender, stack(UnitCatalog.PEASANT, Side.DEFENDER));
    }

    private static BattleSetup setup(List<Stack> attackers, Hero attacker, @Nullable Hero defender, Stack enemy) {
        return new BattleSetup(attackers, List.of(enemy), FIELD, attacker, defender);
    }
}
