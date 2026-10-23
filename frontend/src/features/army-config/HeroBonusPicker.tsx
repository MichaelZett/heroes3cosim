import { useTranslation } from 'react-i18next';

const LUCK_VALUES = [0, 1, 2, 3];
const MORALE_VALUES = [-3, -2, -1, 0, 1, 2, 3];

interface HeroBonusPickerProps {
  luck: number;
  morale: number;
  onChange: (luck: number, morale: number) => void;
}

function signed(value: number): string {
  return value > 0 ? `+${value}` : String(value);
}

/**
 * Glueck und Moral, die der Held ausserhalb der Schlacht gesammelt hat — Artefakte, Gebaeude,
 * besuchte Orte. Die Simulation kennt die Quellen nicht einzeln, nur ihre Summe. Negatives
 * Glueck gibt es in SoD nicht, daher beginnt die Glueck-Auswahl bei 0.
 */
export default function HeroBonusPicker({
  luck,
  morale,
  onChange,
}: Readonly<HeroBonusPickerProps>) {
  const { t } = useTranslation();
  const selectClass =
    'mt-1 w-full rounded-md border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100 focus:border-amber-500 focus:outline-none';
  return (
    <div className="rounded-md border border-slate-800 bg-slate-950 p-3">
      <div className="grid grid-cols-2 gap-3">
        <label className="block">
          <span className="text-sm text-slate-400">{t('army.luckBonus')}</span>
          <select
            className={selectClass}
            value={luck}
            onChange={(e) => onChange(Number(e.target.value), morale)}
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
            className={selectClass}
            value={morale}
            onChange={(e) => onChange(luck, Number(e.target.value))}
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
