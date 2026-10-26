import type { TFunction } from 'i18next';
import type { ArtifactDto, ArtifactEffectDto } from '../../shared/api/types';

function signed(value: number): string {
  return value > 0 ? `+${value}` : String(value);
}

/** Eine Wirkung als kurzer Text, etwa „Glück +1" oder „immun gegen Curse". */
export function effectText(effect: ArtifactEffectDto, t: TFunction): string {
  const spells = effect.spells.map((spell) => t(`spell.${spell}`)).join(', ');
  const key = effect.type === 'BLOCK_MAGIC' && effect.value === 0 ? 'BLOCK_MAGIC_ALL' : effect.type;
  return t(`artifactEffect.${key}`, {
    value: effect.value ?? 0,
    signed: signed(effect.value ?? 0),
    school: effect.school ? t(`magicSchool.${effect.school}`) : '',
    spells,
  });
}

/** Primaerwerte und Wirkungen eines Artefakts als eine Zeile. */
export function artifactText(artifact: ArtifactDto, t: TFunction): string {
  const stats = (
    [
      ['attack', artifact.attack],
      ['defense', artifact.defense],
      ['power', artifact.power],
      ['knowledge', artifact.knowledge],
    ] as const
  )
    .filter(([, v]) => v !== 0)
    .map(([key, v]) => `${signed(v)} ${t(`army.primary.${key}`)}`);
  return [...stats, ...artifact.effects.map((e) => effectText(e, t))].join(', ');
}
