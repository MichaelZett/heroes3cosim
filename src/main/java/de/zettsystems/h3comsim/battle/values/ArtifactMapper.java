package de.zettsystems.h3comsim.battle.values;

import de.zettsystems.h3comsim.battle.domain.Artifact;
import de.zettsystems.h3comsim.battle.domain.ArtifactEffect;
import de.zettsystems.h3comsim.battle.values.ArtifactEffectDto.EffectType;

import java.util.List;

public final class ArtifactMapper {

    private ArtifactMapper() {
    }

    public static ArtifactDto toDto(Artifact artifact) {
        return new ArtifactDto(
                artifact,
                artifact.displayName(),
                artifact.kind(),
                artifact.rarity(),
                artifact.value(),
                artifact.stats().attack(),
                artifact.stats().defense(),
                artifact.stats().power(),
                artifact.stats().knowledge(),
                artifact.components(),
                artifact.blocks(),
                artifact.effects().stream().map(ArtifactMapper::toDto).toList());
    }

    static ArtifactEffectDto toDto(ArtifactEffect effect) {
        return switch (effect) {
            case ArtifactEffect.Luck luck -> number(EffectType.LUCK, luck.value());
            case ArtifactEffect.Morale morale -> number(EffectType.MORALE, morale.value());
            case ArtifactEffect.SpellDuration duration -> number(EffectType.SPELL_DURATION, duration.rounds());
            case ArtifactEffect.MagicResistance resistance -> number(EffectType.MAGIC_RESISTANCE, resistance.percent());
            case ArtifactEffect.Archery archery -> number(EffectType.ARCHERY, archery.percent());
            case ArtifactEffect.Speed speed -> number(EffectType.SPEED, speed.value());
            case ArtifactEffect.Health health -> number(EffectType.HEALTH, health.value());
            case ArtifactEffect.LivingVitality vitality -> number(EffectType.LIVING_VITALITY, vitality.percent());
            case ArtifactEffect.SpellDamage orb ->
                    new ArtifactEffectDto(EffectType.SPELL_DAMAGE, orb.percent(), orb.school(), List.of(), null);
            case ArtifactEffect.SchoolSpells tome ->
                    new ArtifactEffectDto(EffectType.SCHOOL_SPELLS, null, tome.school(), List.of(), null);
            case ArtifactEffect.LevelSpells hat -> number(EffectType.LEVEL_SPELLS, hat.level());
            case ArtifactEffect.GrantsSpell granted ->
                    new ArtifactEffectDto(EffectType.GRANTS_SPELL, null, null, List.of(granted.spell()), granted.level());
            case ArtifactEffect.SpellImmunity immunity -> new ArtifactEffectDto(EffectType.SPELL_IMMUNITY, null, null,
                    immunity.spells().stream().sorted().toList(), null);
            case ArtifactEffect.LevelImmunity immunity -> number(EffectType.LEVEL_IMMUNITY, immunity.maxLevel());
            case ArtifactEffect.MindImmunity _ -> flag(EffectType.MIND_IMMUNITY);
            case ArtifactEffect.BlockMagic block -> number(EffectType.BLOCK_MAGIC, block.maxLevel());
            case ArtifactEffect.NegateImmunities _ -> flag(EffectType.NEGATE_IMMUNITIES);
            case ArtifactEffect.NegateLuck _ -> flag(EffectType.NEGATE_LUCK);
            case ArtifactEffect.NegateMorale _ -> flag(EffectType.NEGATE_MORALE);
            case ArtifactEffect.NoShootingPenalty _ -> flag(EffectType.NO_SHOOTING_PENALTY);
            case ArtifactEffect.FreeShooting _ -> flag(EffectType.FREE_SHOOTING);
            case ArtifactEffect.DragonBonus dragon -> number(EffectType.DRAGON_BONUS, dragon.value());
            case ArtifactEffect.OpeningSpell opening ->
                    new ArtifactEffectDto(EffectType.OPENING_SPELL, opening.rounds(), null, List.of(opening.spell()), null);
            case ArtifactEffect.AlignmentMix _ -> flag(EffectType.ALIGNMENT_MIX);
        };
    }

    private static ArtifactEffectDto number(EffectType type, int value) {
        return new ArtifactEffectDto(type, value, null, List.of(), null);
    }

    private static ArtifactEffectDto flag(EffectType type) {
        return new ArtifactEffectDto(type, null, null, List.of(), null);
    }
}
