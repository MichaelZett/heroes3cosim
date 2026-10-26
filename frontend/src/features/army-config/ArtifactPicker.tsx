import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { ArtifactDto, ArtifactSlot } from '../../shared/api/types';
import { artifactText } from './artifactText';
import {
  ARTIFACT_SLOTS,
  blockedSlots,
  candidatesFor,
  equipmentTotals,
  equippedArtifacts,
  randomEquipment,
  type Equipped,
  type RandomMode,
} from './artifacts';

const FIELD_CLASS =
  'mt-1 w-full rounded-md border border-slate-700 bg-slate-900 px-2 py-1.5 text-sm text-slate-100 focus:border-amber-500 focus:outline-none disabled:text-slate-500';
const DEFAULT_LIMITS: Record<RandomMode, number> = { count: 5, value: 30000 };

interface ArtifactPickerProps {
  catalog: ArtifactDto[];
  value: Equipped;
  onChange: (artifacts: Equipped) => void;
}

/**
 * Die Artefakte des Helden je Platz, wie im Heldenfenster: jede Auswahlliste bietet nur, was
 * dort noch passt; Kombi-Artefakte sperren die Plaetze ihrer Teile. Dazu eine Zufallsauswahl
 * nach Anzahl oder nach Goldwert.
 */
export default function ArtifactPicker({
  catalog,
  value,
  onChange,
}: Readonly<ArtifactPickerProps>) {
  const { t } = useTranslation();
  const [mode, setMode] = useState<RandomMode>('count');
  const [limit, setLimit] = useState(DEFAULT_LIMITS.count);
  const blocked = blockedSlots(value, catalog);
  const worn = equippedArtifacts(value, catalog);
  const totals = equipmentTotals(value, catalog);
  const change = (slot: ArtifactSlot, id: string) => {
    const next = { ...value };
    if (id) next[slot] = id;
    else delete next[slot];
    onChange(next);
  };
  return (
    <details className="mt-3 rounded-md border border-slate-800 p-2" open={worn.length > 0}>
      <summary className="cursor-pointer text-sm text-slate-300">
        {t('army.artifacts', { count: worn.length, value: totals.value })}
      </summary>
      <div className="mt-2 grid grid-cols-2 gap-2 sm:grid-cols-3">
        {ARTIFACT_SLOTS.map(([slot]) => {
          const blocker = blocked.get(slot);
          return (
            <label key={slot} className="block">
              <span className="text-xs text-slate-400">{t(`artifactSlot.${slot}`)}</span>
              <select
                className={FIELD_CLASS}
                value={value[slot] ?? ''}
                disabled={blocker !== undefined}
                onChange={(e) => change(slot, e.target.value)}
              >
                <option value="">
                  {blocker
                    ? t('army.artifactBlocked', { name: blocker.name })
                    : t('army.artifactNone')}
                </option>
                {!blocker &&
                  candidatesFor(slot, value, catalog).map((artifact) => (
                    <option key={artifact.id} value={artifact.id} title={artifactText(artifact, t)}>
                      {artifact.name}
                    </option>
                  ))}
              </select>
            </label>
          );
        })}
      </div>
      <div className="mt-3 flex flex-wrap items-end gap-2">
        <label className="block">
          <span className="text-xs text-slate-400">{t('army.artifactRandomMode')}</span>
          <select
            className={FIELD_CLASS}
            value={mode}
            onChange={(e) => {
              const next = e.target.value as RandomMode;
              setMode(next);
              setLimit(DEFAULT_LIMITS[next]);
            }}
          >
            <option value="count">{t('army.artifactRandomCount')}</option>
            <option value="value">{t('army.artifactRandomValue')}</option>
          </select>
        </label>
        <label className="block w-28">
          <span className="text-xs text-slate-400">
            {mode === 'count' ? t('army.artifactCount') : t('army.artifactGold')}
          </span>
          <input
            type="number"
            min={0}
            step={mode === 'count' ? 1 : 1000}
            className={FIELD_CLASS}
            value={limit}
            onChange={(e) => setLimit(Math.max(0, Math.round(Number(e.target.value) || 0)))}
          />
        </label>
        <button
          type="button"
          className="rounded-md bg-slate-700 px-3 py-1.5 text-sm text-slate-100 hover:bg-slate-600"
          onClick={() => onChange(randomEquipment(catalog, mode, limit))}
        >
          {t('army.artifactRandom')}
        </button>
        <button
          type="button"
          className="rounded-md border border-slate-700 px-3 py-1.5 text-sm text-slate-300 hover:bg-slate-800"
          onClick={() => onChange({})}
        >
          {t('army.artifactClear')}
        </button>
      </div>
      {worn.length > 0 && (
        <ul className="mt-3 space-y-0.5 text-xs text-slate-400">
          {worn.map((artifact) => (
            <li key={artifact.id}>
              <span className="text-slate-200">{artifact.name}</span>: {artifactText(artifact, t)}
            </li>
          ))}
        </ul>
      )}
    </details>
  );
}
