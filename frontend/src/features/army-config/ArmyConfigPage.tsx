import type { ReactNode, SyntheticEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import LanguageSwitcher from '../../shared/ui/LanguageSwitcher';
import ModeSwitcher from '../../shared/ui/ModeSwitcher';
import { useArmyPresets, useHeroes, useSimulateArmyBattle, useUnits } from '../../shared/api/hooks';
import { useArmyConfigStore } from './armyConfigStore';
import { useArmyBattleStore } from './armyBattleStore';
import FactionPresetPicker from './FactionPresetPicker';
import HeroPicker from './HeroPicker';
import SlotEditor from './SlotEditor';
import type { Faction, FactionPresetDto, HeroDto, StackSpec } from '../../shared/api/types';

export default function ArmyConfigPage() {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const loadSimulation = useArmyBattleStore((s) => s.loadSimulation);
  const form = useArmyConfigStore((s) => s.form);
  const setForm = useArmyConfigStore((s) => s.setForm);
  const updateStack = useArmyConfigStore((s) => s.updateStack);

  const unitsQuery = useUnits();
  const presetsQuery = useArmyPresets();
  const heroesQuery = useHeroes();

  const simulate = useSimulateArmyBattle((sim, request) => {
    loadSimulation(sim, request);
    navigate('/army/battle');
  });

  function handleSubmit(e: SyntheticEvent) {
    e.preventDefault();
    const attackerStacks = trimEmpty(form.attackerStacks);
    const defenderStacks = trimEmpty(form.defenderStacks);
    if (attackerStacks.length === 0 || defenderStacks.length === 0) return;
    const seedNum = form.seedText.trim() === '' ? null : Number(form.seedText);
    simulate.mutate({
      attacker: { stacks: attackerStacks, heroName: form.attackerHeroName },
      defender: { stacks: defenderStacks, heroName: form.defenderHeroName },
      seed: seedNum !== null && Number.isFinite(seedNum) ? seedNum : null,
    });
  }

  function rollSeed() {
    setForm({ seedText: String(Math.floor(Math.random() * 1_000_000)) });
  }

  /** Das Preset bringt den Helden seiner Faktion mit — er laesst sich danach frei aendern. */
  function applyPreset(side: 'attacker' | 'defender', preset: FactionPresetDto) {
    const padded: StackSpec[] = [...preset.stacks];
    while (padded.length < 7) padded.push({ unitName: '', count: 1 });
    const heroName = preset.heroName ?? null;
    setForm(
      side === 'attacker'
        ? { attackerStacks: padded, attackerHeroName: heroName }
        : { defenderStacks: padded, defenderHeroName: heroName },
    );
  }

  if (unitsQuery.isPending || presetsQuery.isPending || heroesQuery.isPending) {
    return <CenteredMessage>{t('config.loading')}</CenteredMessage>;
  }
  if (unitsQuery.isError || presetsQuery.isError || heroesQuery.isError) {
    return (
      <CenteredMessage tone="error">
        {t('config.apiDown', { url: 'localhost:8080' })}
      </CenteredMessage>
    );
  }

  const presets = presetsQuery.data.presets;
  const units = unitsQuery.data;
  const heroes = heroesQuery.data;

  const attackerReady = trimEmpty(form.attackerStacks).length > 0;
  const defenderReady = trimEmpty(form.defenderStacks).length > 0;
  const submitDisabled = !attackerReady || !defenderReady || simulate.isPending;

  return (
    <main className="mx-auto max-w-6xl p-8">
      <header className="mb-6 flex items-start justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold text-slate-100">{t('army.title')}</h1>
          <p className="mt-2 text-slate-400">{t('army.subtitle')}</p>
        </div>
        <LanguageSwitcher />
      </header>

      <div className="mb-6">
        <ModeSwitcher current="mixedArmy" />
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        <div className="grid gap-6 md:grid-cols-2">
          <SideColumn
            side="attacker"
            title={t('army.attacker')}
            units={units}
            presets={presets}
            selectedFaction={form.attackerFaction}
            onFactionChange={(f: Faction | null) => setForm({ attackerFaction: f })}
            onPresetApply={(preset: FactionPresetDto) => applyPreset('attacker', preset)}
            stacks={form.attackerStacks}
            onSlotChange={(slot, patch) => updateStack('attacker', slot, patch)}
            heroes={heroes}
            heroName={form.attackerHeroName}
            onHeroChange={(name) => setForm({ attackerHeroName: name })}
          />
          <SideColumn
            side="defender"
            title={t('army.defender')}
            units={units}
            presets={presets}
            selectedFaction={form.defenderFaction}
            onFactionChange={(f: Faction | null) => setForm({ defenderFaction: f })}
            onPresetApply={(preset: FactionPresetDto) => applyPreset('defender', preset)}
            stacks={form.defenderStacks}
            onSlotChange={(slot, patch) => updateStack('defender', slot, patch)}
            heroes={heroes}
            heroName={form.defenderHeroName}
            onHeroChange={(name) => setForm({ defenderHeroName: name })}
          />
        </div>

        <section className="rounded-lg border border-slate-800 bg-slate-900 p-6">
          <h2 className="text-lg font-semibold text-slate-100">{t('config.seedTitle')}</h2>
          <div className="mt-4 flex items-end gap-3">
            <label className="flex-1">
              <span className="text-sm text-slate-400">{t('config.seedHint')}</span>
              <input
                type="number"
                inputMode="numeric"
                placeholder={t('config.seedPlaceholder')}
                className="mt-1 w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2 text-slate-100 focus:border-amber-500 focus:outline-none"
                value={form.seedText}
                onChange={(e) => setForm({ seedText: e.target.value })}
              />
            </label>
            <button
              type="button"
              onClick={rollSeed}
              className="rounded-md border border-slate-700 px-3 py-2 text-sm text-slate-300 hover:border-amber-500 hover:text-amber-400"
            >
              {t('config.rollSeed')}
            </button>
          </div>
        </section>

        {simulate.isError && (
          <p className="text-sm text-red-400">
            {t('config.simulationFailed', { message: simulate.error.message })}
          </p>
        )}

        <div className="flex justify-end">
          <button
            type="submit"
            disabled={submitDisabled}
            className="rounded-md bg-amber-500 px-6 py-3 text-base font-semibold text-slate-950 transition hover:bg-amber-400 disabled:cursor-not-allowed disabled:bg-slate-700 disabled:text-slate-400"
          >
            {simulate.isPending ? t('config.simulating') : t('army.startBattle')}
          </button>
        </div>
      </form>
    </main>
  );
}

/**
 * Jede Seite hat fest 7 Slots. Die Slot-Nummer ist die stabile Identität eines Editors —
 * die Liste wird nie umsortiert oder gekürzt, nur der Inhalt eines Slots wechselt.
 */
const SLOTS = [0, 1, 2, 3, 4, 5, 6];
const EMPTY_SLOT: StackSpec = { unitName: '', count: 1 };

interface SideColumnProps {
  side: 'attacker' | 'defender';
  title: string;
  units: import('../../shared/api/types').UnitDto[];
  presets: FactionPresetDto[];
  selectedFaction: Faction | null;
  onFactionChange: (faction: Faction | null) => void;
  onPresetApply: (preset: FactionPresetDto) => void;
  stacks: StackSpec[];
  onSlotChange: (slot: number, patch: Partial<StackSpec>) => void;
  heroes: HeroDto[];
  heroName: string | null;
  onHeroChange: (name: string | null) => void;
}

function SideColumn(props: Readonly<SideColumnProps>) {
  const { t } = useTranslation();
  return (
    <section className="space-y-3 rounded-lg border border-slate-800 bg-slate-900 p-4">
      <h2 className="text-lg font-semibold text-slate-100">{props.title}</h2>
      <FactionPresetPicker
        label={t('army.preset')}
        presets={props.presets}
        selectedFaction={props.selectedFaction}
        onFactionChange={props.onFactionChange}
        onApply={props.onPresetApply}
      />
      <HeroPicker
        heroes={props.heroes}
        selectedName={props.heroName}
        onChange={props.onHeroChange}
      />
      <div className="space-y-1.5">
        {SLOTS.map((slot) => (
          <SlotEditor
            key={`${props.side}-${slot}`}
            slot={slot}
            spec={props.stacks[slot] ?? EMPTY_SLOT}
            units={props.units}
            onChange={(patch) => props.onSlotChange(slot, patch)}
          />
        ))}
      </div>
    </section>
  );
}

function trimEmpty(stacks: StackSpec[]): StackSpec[] {
  return stacks.filter((s) => s.unitName.trim() !== '' && s.count >= 1);
}

function CenteredMessage({
  children,
  tone = 'info',
}: Readonly<{
  children: ReactNode;
  tone?: 'info' | 'error';
}>) {
  return (
    <main className="flex min-h-screen items-center justify-center p-8">
      <p className={tone === 'error' ? 'text-red-400' : 'text-slate-400'}>{children}</p>
    </main>
  );
}
