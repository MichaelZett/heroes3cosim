import { useTranslation } from 'react-i18next';
import type { SecondarySkill, SkillLevel } from '../../shared/api/types';
import { BUILD_SKILLS, MAX_HERO_LEVEL, type HeroBuild } from './heroBuild';

const LUCK_VALUES = [0, 1, 2, 3];
const MORALE_VALUES = [-3, -2, -1, 0, 1, 2, 3];
const MAX_PRIMARY_BONUS = 99;
const LEVELS: SkillLevel[] = ['NONE', 'BASIC', 'ADVANCED', 'EXPERT'];

interface HeroBonusPickerProps {
  value: HeroBuild;
  /** Startfertigkeiten des gewaehlten Helden; darunter laesst sich keine Stufe waehlen. */
  startSkills: Partial<Record<SecondarySkill, SkillLevel>>;
  onChange: (patch: Partial<HeroBuild>) => void;
}

function clampInt(raw: string, min: number, max: number): number | null {
  const n = Math.round(Number(raw));
  return Number.isFinite(n) ? Math.min(max, Math.max(min, n)) : null;
}

function rank(level: SkillLevel | undefined): number {
  return LEVELS.indexOf(level ?? 'NONE');
}

function signed(value: number): string {
  return value > 0 ? `+${value}` : String(value);
}

/**
 * Level, Glueck und Moral, die der Held ausserhalb der Schlacht gesammelt hat — Artefakte,
 * Gebaeude, besuchte Orte. Die Simulation kennt die Quellen nicht einzeln, nur ihre Summe. Das
 * Level wirkt ueber die Spezialitaet. Negatives Glueck gibt es in SoD nicht, daher beginnt die
 * Glueck-Auswahl bei 0.
 */
export default function HeroBonusPicker({
  value,
  startSkills,
  onChange,
}: Readonly<HeroBonusPickerProps>) {
  const { t } = useTranslation();
  const fieldClass =
    'mt-1 w-full rounded-md border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100 focus:border-amber-500 focus:outline-none';
  return (
    <div className="rounded-md border border-slate-800 bg-slate-950 p-3">
      <div className="grid grid-cols-3 gap-3">
        <label className="block">
          <span className="text-sm text-slate-400">{t('army.heroLevel')}</span>
          <input
            type="number"
            min={1}
            max={MAX_HERO_LEVEL}
            className={fieldClass}
            value={value.level}
            onChange={(e) => {
              const level = clampInt(e.target.value, 1, MAX_HERO_LEVEL);
              if (level !== null) onChange({ level });
            }}
          />
        </label>
        <label className="block">
          <span className="text-sm text-slate-400">{t('army.attackBonus')}</span>
          <input
            type="number"
            min={0}
            max={MAX_PRIMARY_BONUS}
            className={fieldClass}
            value={value.attack}
            onChange={(e) => {
              const attack = clampInt(e.target.value, 0, MAX_PRIMARY_BONUS);
              if (attack !== null) onChange({ attack });
            }}
          />
        </label>
        <label className="block">
          <span className="text-sm text-slate-400">{t('army.defenseBonus')}</span>
          <input
            type="number"
            min={0}
            max={MAX_PRIMARY_BONUS}
            className={fieldClass}
            value={value.defense}
            onChange={(e) => {
              const defense = clampInt(e.target.value, 0, MAX_PRIMARY_BONUS);
              if (defense !== null) onChange({ defense });
            }}
          />
        </label>
        <label className="block">
          <span className="text-sm text-slate-400">{t('army.luckBonus')}</span>
          <select
            className={fieldClass}
            value={value.luck}
            onChange={(e) => onChange({ luck: Number(e.target.value) })}
          >
            {LUCK_VALUES.map((v) => (
              <option key={v} value={v}>
                {signed(v)}
              </option>
            ))}
          </select>
        </label>
        <label className="block">
          <span className="text-sm text-slate-400">{t('army.moraleBonus')}</span>
          <select
            className={fieldClass}
            value={value.morale}
            onChange={(e) => onChange({ morale: Number(e.target.value) })}
          >
            {MORALE_VALUES.map((v) => (
              <option key={v} value={v}>
                {signed(v)}
              </option>
            ))}
          </select>
        </label>
      </div>
      <div className="mt-3 grid grid-cols-3 gap-3">
        {BUILD_SKILLS.map((skill) => {
          const start = rank(startSkills[skill]);
          const current = Math.max(start, rank(value.skills[skill]));
          return (
            <label key={skill} className="block">
              <span className="text-sm text-slate-400">{t(`secondarySkill.${skill}`)}</span>
              <select
                className={fieldClass}
                value={LEVELS[current]}
                onChange={(e) =>
                  onChange({ skills: { ...value.skills, [skill]: e.target.value as SkillLevel } })
                }
              >
                {LEVELS.slice(start).map((level) => (
                  <option key={level} value={level}>
                    {t(`army.skillLevel${level}`)}
                  </option>
                ))}
              </select>
            </label>
          );
        })}
      </div>
      <p className="mt-2 text-xs text-slate-500">{t('army.bonusHint')}</p>
    </div>
  );
}
