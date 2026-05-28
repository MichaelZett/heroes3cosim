import {useTranslation} from 'react-i18next';
import type {StackSpec, UnitDto} from '../../shared/api/types';

interface SlotEditorProps {
    slot: number;
    spec: StackSpec;
    units: UnitDto[];
    onChange: (patch: Partial<StackSpec>) => void;
}

export default function SlotEditor({slot, spec, units, onChange}: Readonly<SlotEditorProps>) {
    const {t} = useTranslation();
    const sorted = [...units].sort((a, b) => {
        if (a.faction !== b.faction) return a.faction.localeCompare(b.faction);
        if (a.tier !== b.tier) return a.tier - b.tier;
        if (a.upgrade !== b.upgrade) return a.upgrade ? 1 : -1;
        return a.name.localeCompare(b.name);
    });

    return (
        <div className="flex items-center gap-2 rounded-md border border-slate-800 bg-slate-950 p-2">
            <span className="w-8 shrink-0 text-xs font-semibold text-slate-500">
                {t('army.slotN', {slot: slot + 1})}
            </span>
            <select
                className="flex-1 rounded-md border border-slate-700 bg-slate-900 px-2 py-1.5 text-sm text-slate-100 focus:border-amber-500 focus:outline-none"
                value={spec.unitName}
                onChange={(e) => onChange({unitName: e.target.value})}
            >
                <option value="">{t('army.slotEmpty')}</option>
                {sorted.map((u) => (
                    <option key={u.name} value={u.name}>
                        {u.name}
                    </option>
                ))}
            </select>
            <input
                type="number"
                min={1}
                max={9999}
                className="w-20 rounded-md border border-slate-700 bg-slate-900 px-2 py-1.5 text-sm text-slate-100 focus:border-amber-500 focus:outline-none"
                value={spec.count}
                onChange={(e) => onChange({count: Math.max(1, Number(e.target.value) || 1)})}
            />
        </div>
    );
}
