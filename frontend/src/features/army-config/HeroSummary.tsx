import { useTranslation } from 'react-i18next';
import type { HeroDto, SecondarySkill, SkillLevel } from '../../shared/api/types';

/** Der Held, wie er gekaempft hat: Level, Primaerwerte, Fertigkeiten und Zauberbuch. */
export default function HeroSummary({ hero }: Readonly<{ hero: HeroDto }>) {
  const { t } = useTranslation();
  const skills = (Object.entries(hero.skills) as [SecondarySkill, SkillLevel][])
    .filter(([, level]) => level !== 'NONE')
    .map(([skill, level]) => `${t(`secondarySkill.${skill}`)} ${t(`army.skillLevel${level}`)}`)
    .join(', ');
  const spells = (hero.spells ?? []).map((spell) => t(`spell.${spell}`)).join(', ');
  const artifacts = (hero.artifacts ?? []).join(', ');
  return (
    <div className="mt-2 space-y-0.5 text-xs text-slate-400">
      <p className="text-slate-300">
        {t('army.heroSummary', {
          name: hero.name,
          level: hero.level ?? 1,
          attack: hero.attack,
          defense: hero.defense,
          power: hero.power,
          knowledge: hero.knowledge,
        })}
      </p>
      {skills && <p>{t('army.heroSummarySkills', { skills })}</p>}
      {spells && <p>{t('army.heroSummarySpells', { spells })}</p>}
      {artifacts && <p>{t('army.heroSummaryArtifacts', { artifacts })}</p>}
    </div>
  );
}
