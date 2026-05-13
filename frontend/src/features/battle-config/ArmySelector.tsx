import {useTranslation} from 'react-i18next';
import type {Faction, UnitDto} from '../../shared/api/types';

interface ArmySelectorProps {
    title: string;
    factions: Faction[];
    units: UnitDto[];
    selectedFaction: Faction | 'ALL';
    selectedTier: number | 'ALL';
    selectedUnit: string;
    count: number;
    onFactionChange: (faction: Faction | 'ALL') => void;
    onTierChange: (tier: number | 'ALL') => void;
    onUnitChange: (unitName: string) => void;
    onCountChange: (count: number) => void;
}

const TIERS: Array<number | 'ALL'> = ['ALL', 1, 2, 3, 4, 5, 6, 7];

export default function ArmySelector(props: Readonly<ArmySelectorProps>) {
    const {t} = useTranslation();

    const filteredUnits = props.units.filter((u) => {
        if (props.selectedFaction !== 'ALL' && u.faction !== props.selectedFaction) return false;
        if (props.selectedTier !== 'ALL' && u.tier !== props.selectedTier) return false;
        return true;
    });
    // Bei einer einzelnen Faktion nach Tier (1→7, basic vor upgrade) sortieren —
    // bei "Alle Faktionen" alphabetisch, sonst wäre die Liste chaotisch.
    const sorted = [...filteredUnits].sort((a, b) => {
        if (props.selectedFaction === 'ALL') return a.name.localeCompare(b.name);
        if (a.tier !== b.tier) return a.tier - b.tier;
        if (a.upgrade !== b.upgrade) return a.upgrade ? 1 : -1;
        return a.name.localeCompare(b.name);
    });
    const selected = props.units.find((u) => u.name === props.selectedUnit);

    return (
        <section className="rounded-lg border border-slate-800 bg-slate-900 p-6">
            <h2 className="text-lg font-semibold text-slate-100">{props.title}</h2>

            <div className="mt-4 space-y-4">
                <div className="grid grid-cols-2 gap-3">
                    <label className="block">
                        <span className="text-sm text-slate-400">{t('common.factionLabel')}</span>
                        <select
                            className="mt-1 w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2 text-slate-100 focus:border-amber-500 focus:outline-none"
                            value={props.selectedFaction}
                            onChange={(e) => {
                                props.onFactionChange(e.target.value as Faction | 'ALL');
                                props.onUnitChange('');
                            }}
                        >
                            <option value="ALL">{t('common.factionAll')}</option>
                            {props.factions.map((f) => (
                                <option key={f} value={f}>
                                    {t(`faction.${f}`)}
                                </option>
                            ))}
                        </select>
                    </label>

                    <label className="block">
                        <span className="text-sm text-slate-400">{t('common.tierLabel')}</span>
                        <select
                            className="mt-1 w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2 text-slate-100 focus:border-amber-500 focus:outline-none"
                            value={String(props.selectedTier)}
                            onChange={(e) => {
                                const v = e.target.value;
                                props.onTierChange(v === 'ALL' ? 'ALL' : Number(v));
                                props.onUnitChange('');
                            }}
                        >
                            {TIERS.map((tier) => (
                                <option key={String(tier)} value={String(tier)}>
                                    {tier === 'ALL' ? t('common.tierAll') : t('common.tierN', {tier})}
                                </option>
                            ))}
                        </select>
                    </label>
                </div>

                <label className="block">
                    <span className="text-sm text-slate-400">{t('common.unitLabel')}</span>
                    <select
                        className="mt-1 w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2 text-slate-100 focus:border-amber-500 focus:outline-none"
                        value={props.selectedUnit}
                        onChange={(e) => props.onUnitChange(e.target.value)}
                    >
                        <option value="">{t('common.unitChoose')}</option>
                        {sorted.map((u) => (
                            <option key={u.name} value={u.name}>
                                {formatUnitOption(u)}
                            </option>
                        ))}
                    </select>
                </label>

                <label className="block">
                    <span className="text-sm text-slate-400">{t('common.countLabel')}</span>
                    <input
                        type="number"
                        min={1}
                        max={999}
                        className="mt-1 w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2 text-slate-100 focus:border-amber-500 focus:outline-none"
                        value={props.count}
                        onChange={(e) => props.onCountChange(Math.max(1, Number(e.target.value) || 1))}
                    />
                </label>

                {selected && <UnitStats unit={selected}/>}
            </div>
        </section>
    );
}

function formatUnitOption(unit: UnitDto): string {
    const tier = `T${unit.tier}`;
    const upgrade = unit.upgrade ? ' ★' : '';
    return `${unit.name} — ${tier}${upgrade}`;
}

function UnitStats({unit}: Readonly<{ unit: UnitDto }>) {
    const {t} = useTranslation();
    return (
        <div className="rounded-md border border-slate-800 bg-slate-950 p-3 text-sm text-slate-300">
            <div className="mb-2 flex items-center gap-2">
        <span className="rounded bg-amber-500/20 px-2 py-0.5 text-xs font-semibold text-amber-300">
          {t('common.tierN', {tier: unit.tier})}
        </span>
                {unit.upgrade && (
                    <span className="rounded bg-emerald-500/20 px-2 py-0.5 text-xs font-semibold text-emerald-300">
            {t('common.upgradeBadge')}
          </span>
                )}
            </div>
            <div className="grid grid-cols-2 gap-x-4 gap-y-1">
                <span className="text-slate-500">{t('common.statAttackDefense')}</span>
                <span>
          {unit.attack} / {unit.defense}
        </span>
                <span className="text-slate-500">{t('common.statHealth')}</span>
                <span>{unit.health}</span>
                <span className="text-slate-500">{t('common.statDamage')}</span>
                <span>
          {t('common.statDamageRange', {min: unit.minDamage, max: unit.maxDamage})}
                    {unit.shots > 0 ? t('common.statShotsSuffix', {shots: unit.shots}) : ''}
        </span>
                <span className="text-slate-500">{t('common.statSpeed')}</span>
                <span>
          {unit.speed} ({unit.movement === 'FLYING' ? t('common.statSpeedFlying') : t('common.statSpeedGround')})
        </span>
                <span className="text-slate-500">{t('common.statCost')}</span>
                <span>{t('common.statCostValue', {cost: unit.cost})}</span>
            </div>
            {unit.specialities.length > 0 && (
                <div className="mt-2 flex flex-wrap gap-1">
                    {unit.specialities.map((s) => (
                        <span key={s} className="rounded bg-slate-800 px-2 py-0.5 text-xs text-slate-300">
              {s}
            </span>
                    ))}
                </div>
            )}
        </div>
    );
}
