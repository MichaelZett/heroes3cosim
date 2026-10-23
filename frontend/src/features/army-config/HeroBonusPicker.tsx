import { useTranslation } from 'react-i18next';

const LUCK_VALUES = [0, 1, 2, 3];
const MORALE_VALUES = [-3, -2, -1, 0, 1, 2, 3];
export const MAX_HERO_LEVEL = 99;

/** Was der Held ausserhalb der Schlacht erreicht hat: Level und gesammelte Boni. */
export interface HeroBuild {
  level: number;
  luck: number;
  morale: number;
}

interface HeroBonusPickerProps {
  value: HeroBuild;
  onChange: (patch: Partial<HeroBuild>) => void;
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
export default function HeroBonusPicker({ value, onChange }: Readonly<HeroBonusPickerProps>) {
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
              const level = Math.round(Number(e.target.value));
              if (Number.isFinite(level)) {
                onChange({ level: Math.min(MAX_HERO_LEVEL, Math.max(1, level)) });
              }
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
      <p className="mt-2 text-xs text-slate-500">{t('army.bonusHint')}</p>
    </div>
  );
}
