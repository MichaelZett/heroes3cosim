import {useTranslation} from 'react-i18next';
import type {Faction, FactionPresetDto, StackSpec} from '../../shared/api/types';

interface FactionPresetPickerProps {
    label: string;
    presets: FactionPresetDto[];
    selectedFaction: Faction | null;
    onFactionChange: (faction: Faction | null) => void;
    onApply: (stacks: StackSpec[]) => void;
}

export default function FactionPresetPicker({
                                                 label,
                                                 presets,
                                                 selectedFaction,
                                                 onFactionChange,
                                                 onApply,
                                             }: Readonly<FactionPresetPickerProps>) {
    const {t} = useTranslation();
    const current = presets.find((p) => p.faction === selectedFaction);

    return (
        <div className="flex flex-wrap items-end gap-3 rounded-md border border-slate-800 bg-slate-950 p-3">
            <label className="flex-1 min-w-[180px]">
                <span className="text-sm text-slate-400">{label}</span>
                <select
                    className="mt-1 w-full rounded-md border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100 focus:border-amber-500 focus:outline-none"
                    value={selectedFaction ?? ''}
                    onChange={(e) => {
                        const v = e.target.value;
                        onFactionChange(v === '' ? null : (v as Faction));
                    }}
                >
                    <option value="">{t('army.presetChoose')}</option>
                    {presets.map((p) => (
                        <option key={p.faction} value={p.faction}>
                            {t(`faction.${p.faction}`)}
                        </option>
                    ))}
                </select>
            </label>
            <button
                type="button"
                disabled={!current}
                onClick={() => current && onApply(current.stacks)}
                className="rounded-md border border-amber-500 px-3 py-2 text-sm text-amber-300 transition hover:bg-amber-500/10 disabled:cursor-not-allowed disabled:border-slate-700 disabled:text-slate-500"
            >
                {t('army.applyPreset')}
            </button>
        </div>
    );
}
