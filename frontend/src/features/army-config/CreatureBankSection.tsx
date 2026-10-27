import { useTranslation } from 'react-i18next';
import type { CreatureBankDto } from '../../shared/api/types';

const FIELD_CLASS =
  'mt-1 w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2 text-slate-100 focus:border-amber-500 focus:outline-none disabled:text-slate-500';

interface CreatureBankSectionProps {
  banks: CreatureBankDto[];
  bank: string | null;
  level: number | null;
  /** Eine Bank ist keine Stadt: waehrend einer Belagerung gesperrt. */
  disabled: boolean;
  onChange: (patch: { creatureBank?: string | null; creatureBankLevel?: number | null }) => void;
}

/**
 * Kampf um eine Kreaturenbank: Bank und Stufe (oder gewuerfelt). Die Waechter ersetzen die
 * Verteidiger-Armee und stehen in der Aufstellung der Bank.
 */
export default function CreatureBankSection({
  banks,
  bank,
  level,
  disabled,
  onChange,
}: Readonly<CreatureBankSectionProps>) {
  const { t } = useTranslation();
  const selected = banks.find((b) => b.id === bank);
  return (
    <section className="rounded-lg border border-slate-800 bg-slate-900 p-6">
      <h2 className="text-lg font-semibold text-slate-100">{t('army.bankTitle')}</h2>
      <p className="mt-1 text-sm text-slate-400">{t('army.bankHint')}</p>
      <div className="mt-4 grid gap-4 md:grid-cols-2">
        <select
          aria-label={t('army.bankTitle')}
          className={FIELD_CLASS}
          value={bank ?? ''}
          disabled={disabled}
          onChange={(e) =>
            onChange({ creatureBank: e.target.value === '' ? null : e.target.value })
          }
        >
          <option value="">{t('army.bankNone')}</option>
          {banks.map((b) => (
            <option key={b.id} value={b.id}>
              {b.name}
            </option>
          ))}
        </select>
        {selected && (
          <select
            aria-label={t('army.bankLevel')}
            className={FIELD_CLASS}
            value={level ?? ''}
            onChange={(e) =>
              onChange({ creatureBankLevel: e.target.value === '' ? null : Number(e.target.value) })
            }
          >
            <option value="">{t('army.bankLevelRandom')}</option>
            {selected.levels.map((l) => (
              <option key={l.level} value={l.level}>
                {t('army.bankLevelN', { level: l.level, chance: l.chance })}
              </option>
            ))}
          </select>
        )}
      </div>
      {disabled && <p className="mt-3 text-sm text-slate-400">{t('army.bankNoSiege')}</p>}
    </section>
  );
}

/** Die Waechter der Bank, je Stufe oder fuer die gewaehlte; anstelle der Verteidiger-Armee. */
export function CreatureBankGuards({
  bank,
  level,
}: Readonly<{ bank: CreatureBankDto; level: number | null }>) {
  const { t } = useTranslation();
  const levels = level === null ? bank.levels : bank.levels.filter((l) => l.level === level);
  return (
    <section className="space-y-3 rounded-lg border border-slate-800 bg-slate-900 p-4">
      <h2 className="text-lg font-semibold text-slate-100">
        {t('army.bankGuardsTitle', { name: bank.name })}
      </h2>
      <p className="text-xs text-slate-400">{t('army.bankGuardsHint')}</p>
      {levels.map((l) => (
        <div key={l.level}>
          <p className="text-sm text-slate-300">
            {t('army.bankLevelN', { level: l.level, chance: l.chance })}
          </p>
          <ul className="mt-1 space-y-0.5 text-sm text-slate-200">
            {l.guards.map((g, index) => (
              <li key={`${l.level}-${index}-${g.unitName}`}>
                {g.count} × {g.unitName}
                {g.upgradeName && (
                  <span className="text-xs text-slate-400">
                    {' '}
                    {t('army.bankUpgrade', { unit: g.upgradeName })}
                  </span>
                )}
              </li>
            ))}
          </ul>
        </div>
      ))}
    </section>
  );
}
