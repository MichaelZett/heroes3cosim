import type { ReactNode, SyntheticEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import LanguageSwitcher from '../../shared/ui/LanguageSwitcher';
import ModeSwitcher from '../../shared/ui/ModeSwitcher';
import {
  useArmyPresets,
  useArtifacts,
  useCreatureBanks,
  useHeroes,
  useSimulateArmyBattle,
  useUnits,
} from '../../shared/api/hooks';
import { useArmyConfigStore } from './armyConfigStore';
import { DEFAULT_BUILD, higherLevel, learnableSpells, type HeroBuild } from './heroBuild';
import { useArmyBattleStore } from './armyBattleStore';
import FactionPresetPicker from './FactionPresetPicker';
import ArtifactPicker from './ArtifactPicker';
import CreatureBankSection, { CreatureBankGuards } from './CreatureBankSection';
import { equipmentTotals } from './artifacts';
import HeroBonusPicker from './HeroBonusPicker';
import HeroPicker from './HeroPicker';
import SlotEditor from './SlotEditor';
import type {
  ArmySpec,
  ArtifactDto,
  Faction,
  FactionPresetDto,
  Fortification,
  HeroDto,
  MagicTerrain,
  StackSpec,
  Terrain,
} from '../../shared/api/types';

const FORTIFICATIONS: Fortification[] = ['FORT', 'CITADEL', 'CASTLE'];
const TERRAINS: Terrain[] = [
  'GRASS',
  'DIRT',
  'LAVA',
  'SUBTERRANEAN',
  'ROUGH',
  'SAND',
  'SNOW',
  'SWAMP',
  'WATER',
];
const MAGIC_TERRAINS: MagicTerrain[] = [
  'MAGIC_PLAINS',
  'CURSED_GROUND',
  'ROCKLANDS',
  'FIERY_FIELDS',
  'LUCID_POOLS',
  'MAGIC_CLOUDS',
  'HOLY_GROUND',
  'EVIL_FOG',
  'CLOVER_FIELD',
];
const TOWNS: Faction[] = [
  'CASTLE',
  'RAMPART',
  'TOWER',
  'INFERNO',
  'NECROPOLIS',
  'DUNGEON',
  'STRONGHOLD',
  'FORTRESS',
  'CONFLUX',
];

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
  const artifactsQuery = useArtifacts();
  const banksQuery = useCreatureBanks();

  const simulate = useSimulateArmyBattle((sim, request) => {
    loadSimulation(sim, request);
    navigate('/army/battle');
  });

  function heroByName(name: string | null): HeroDto | undefined {
    return heroesQuery.data?.find((h) => h.name === name);
  }

  function handleSubmit(e: SyntheticEvent) {
    e.preventDefault();
    const attackerStacks = trimEmpty(form.attackerStacks);
    const defenderStacks = trimEmpty(form.defenderStacks);
    if (attackerStacks.length === 0 || defenderStacks.length === 0) return;
    const seedNum = form.seedText.trim() === '' ? null : Number(form.seedText);
    const bank = form.creatureBank ?? null;
    simulate.mutate({
      attacker: armySpec(
        attackerStacks,
        form.attackerWithoutHero ?? false,
        form.attackerHeroName,
        form.attackerBuild,
      ),
      defender: bank
        ? null
        : armySpec(
            defenderStacks,
            form.defenderWithoutHero ?? false,
            form.defenderHeroName,
            form.defenderBuild,
          ),
      creatureBank: bank ? { bank, level: form.creatureBankLevel ?? null } : null,
      seed: seedNum !== null && Number.isFinite(seedNum) ? seedNum : null,
      fortification: form.fortification ?? null,
      town: form.fortification ? (form.town ?? null) : null,
      terrain: form.fortification ? null : (form.terrain ?? null),
      magicTerrain:
        form.fortification || form.terrain === 'WATER' ? null : (form.magicTerrain ?? null),
      sodSpellCosts: form.sodSpellCosts ?? false,
    });
  }

  function armySpec(
    stacks: StackSpec[],
    withoutHero: boolean,
    heroName: string | null,
    build: HeroBuild,
  ): ArmySpec {
    if (withoutHero) return { stacks, withoutHero: true };
    return { stacks, heroName, ...buildSpec(build, heroByName(heroName)) };
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
        ? { attackerStacks: padded, attackerHeroName: heroName, attackerWithoutHero: false }
        : { defenderStacks: padded, defenderHeroName: heroName, defenderWithoutHero: false },
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
  const banks = banksQuery.data ?? [];
  const selectedBank = form.creatureBank
    ? banks.find((b) => b.id === form.creatureBank)
    : undefined;
  const defenderReady = !!selectedBank || trimEmpty(form.defenderStacks).length > 0;
  const needsHero = !!form.fortification || !!selectedBank;
  const siegeWithoutHero = needsHero && (form.attackerWithoutHero ?? false);
  const submitDisabled = !attackerReady || !defenderReady || siegeWithoutHero || simulate.isPending;

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
            artifacts={artifactsQuery.data ?? []}
            heroName={form.attackerHeroName}
            onHeroChange={(name) => setForm({ attackerHeroName: name, attackerWithoutHero: false })}
            withoutHero={form.attackerWithoutHero ?? false}
            onWithoutHero={() => setForm({ attackerHeroName: null, attackerWithoutHero: true })}
            build={{ ...DEFAULT_BUILD, ...form.attackerBuild }}
            onBuildChange={(patch) =>
              setForm({ attackerBuild: { ...DEFAULT_BUILD, ...form.attackerBuild, ...patch } })
            }
          />
          {selectedBank ? (
            <CreatureBankGuards bank={selectedBank} level={form.creatureBankLevel ?? null} />
          ) : (
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
              artifacts={artifactsQuery.data ?? []}
              heroName={form.defenderHeroName}
              onHeroChange={(name) =>
                setForm({ defenderHeroName: name, defenderWithoutHero: false })
              }
              withoutHero={form.defenderWithoutHero ?? false}
              onWithoutHero={() => setForm({ defenderHeroName: null, defenderWithoutHero: true })}
              build={{ ...DEFAULT_BUILD, ...form.defenderBuild }}
              onBuildChange={(patch) =>
                setForm({ defenderBuild: { ...DEFAULT_BUILD, ...form.defenderBuild, ...patch } })
              }
            />
          )}
        </div>

        <section className="rounded-lg border border-slate-800 bg-slate-900 p-6">
          <h2 className="text-lg font-semibold text-slate-100">{t('army.siegeTitle')}</h2>
          <label className="mt-4 block">
            <span className="text-sm text-slate-400">{t('army.siegeHint')}</span>
            <select
              aria-label={t('army.siegeTitle')}
              className="mt-1 w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2 text-slate-100 focus:border-amber-500 focus:outline-none disabled:text-slate-500"
              value={form.fortification ?? ''}
              disabled={!!selectedBank}
              onChange={(e) =>
                setForm({
                  fortification: e.target.value === '' ? null : (e.target.value as Fortification),
                })
              }
            >
              <option value="">{t('army.siegeNone')}</option>
              {FORTIFICATIONS.map((f) => (
                <option key={f} value={f}>
                  {t(`fortification.${f}`)}
                </option>
              ))}
            </select>
          </label>
          {form.fortification && (
            <label className="mt-4 block">
              <span className="text-sm text-slate-400">{t('army.townHint')}</span>
              <select
                aria-label={t('army.town')}
                className="mt-1 w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2 text-slate-100 focus:border-amber-500 focus:outline-none"
                value={form.town ?? ''}
                onChange={(e) =>
                  setForm({ town: e.target.value === '' ? null : (e.target.value as Faction) })
                }
              >
                <option value="">{t('army.townDefault')}</option>
                {TOWNS.map((f) => (
                  <option key={f} value={f}>
                    {t(`faction.${f}`)}
                  </option>
                ))}
              </select>
            </label>
          )}
          {siegeWithoutHero && (
            <p className="mt-3 text-sm text-red-400">
              {t(selectedBank ? 'army.bankNeedsHero' : 'army.siegeNeedsHero')}
            </p>
          )}
        </section>

        {banks.length > 0 && (
          <CreatureBankSection
            banks={banks}
            bank={form.creatureBank ?? null}
            level={form.creatureBankLevel ?? null}
            disabled={!!form.fortification}
            onChange={(patch) =>
              setForm(
                patch.creatureBank === undefined ? patch : { ...patch, creatureBankLevel: null },
              )
            }
          />
        )}

        <section className="rounded-lg border border-slate-800 bg-slate-900 p-6">
          <h2 className="text-lg font-semibold text-slate-100">{t('army.terrainTitle')}</h2>
          <p className="mt-1 text-sm text-slate-400">{t('army.terrainHint')}</p>
          {form.fortification ? (
            <p className="mt-4 text-sm text-slate-300">{t('army.terrainSiege')}</p>
          ) : (
            <div className="mt-4 grid gap-4 md:grid-cols-2">
              <select
                aria-label={t('army.terrainTitle')}
                className="w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2 text-slate-100 focus:border-amber-500 focus:outline-none"
                value={form.terrain ?? ''}
                onChange={(e) =>
                  setForm({ terrain: e.target.value === '' ? null : (e.target.value as Terrain) })
                }
              >
                <option value="">{t('army.terrainNone')}</option>
                {TERRAINS.map((terrain) => (
                  <option key={terrain} value={terrain}>
                    {t(`terrain.${terrain}`)}
                  </option>
                ))}
              </select>
              <select
                aria-label={t('army.magicTerrain')}
                className="w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2 text-slate-100 focus:border-amber-500 focus:outline-none disabled:text-slate-500"
                value={form.terrain === 'WATER' ? '' : (form.magicTerrain ?? '')}
                disabled={form.terrain === 'WATER'}
                onChange={(e) =>
                  setForm({
                    magicTerrain: e.target.value === '' ? null : (e.target.value as MagicTerrain),
                  })
                }
              >
                <option value="">{t('army.magicTerrainNone')}</option>
                {MAGIC_TERRAINS.map((magic) => (
                  <option key={magic} value={magic}>
                    {t(`magicTerrain.${magic}`)}
                  </option>
                ))}
              </select>
            </div>
          )}
          {!form.fortification && form.terrain === 'WATER' && (
            <p className="mt-3 text-sm text-slate-300">{t('army.waterHint')}</p>
          )}
        </section>

        <section className="rounded-lg border border-slate-800 bg-slate-900 p-6">
          <h2 className="text-lg font-semibold text-slate-100">{t('army.rulesTitle')}</h2>
          <label className="mt-4 flex items-start gap-3">
            <input
              type="checkbox"
              className="mt-1 h-4 w-4 accent-amber-500"
              checked={form.sodSpellCosts ?? false}
              onChange={(e) => setForm({ sodSpellCosts: e.target.checked })}
            />
            <span className="text-slate-100">
              {t('army.sodSpellCosts')}
              <span className="block text-sm text-slate-400">{t('army.sodSpellCostsHint')}</span>
            </span>
          </label>
        </section>

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
  artifacts: ArtifactDto[];
  heroName: string | null;
  onHeroChange: (name: string | null) => void;
  withoutHero: boolean;
  onWithoutHero: () => void;
  build: HeroBuild;
  onBuildChange: (patch: Partial<HeroBuild>) => void;
}

function SideColumn(props: Readonly<SideColumnProps>) {
  const { t } = useTranslation();
  const selectedHero = props.heroes.find((h) => h.name === props.heroName);
  const artifactTotals = equipmentTotals(props.build.artifacts, props.artifacts);
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
        withoutHero={props.withoutHero}
        onWithoutHero={props.onWithoutHero}
      />
      {!props.withoutHero && (
        <HeroBonusPicker
          value={props.build}
          startSkills={selectedHero?.skills ?? {}}
          basePower={(selectedHero?.power ?? 0) + artifactTotals.power}
          baseKnowledge={(selectedHero?.knowledge ?? 0) + artifactTotals.knowledge}
          startSpells={selectedHero?.spells ?? []}
          onChange={props.onBuildChange}
        />
      )}
      {!props.withoutHero && props.artifacts.length > 0 && (
        <ArtifactPicker
          catalog={props.artifacts}
          value={props.build.artifacts}
          onChange={(artifacts) => props.onBuildChange({ artifacts })}
        />
      )}
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

/**
 * Request-Felder des Helden-Aufbaus. Aeltere, im Browser gespeicherte Formulare kennen den
 * Aufbau noch nicht — dann gelten die Defaults.
 */
function buildSpec(stored: Partial<HeroBuild> | undefined, hero: HeroDto | undefined) {
  const build = { ...DEFAULT_BUILD, ...stored };
  return {
    heroLevel: build.level,
    attackBonus: build.attack,
    defenseBonus: build.defense,
    randomGrowth: build.randomGrowth,
    powerBonus: build.power,
    knowledgeBonus: build.knowledge,
    // Ein Heldenwechsel kann Wisdom senken; was der Held nicht lernen kann, bleibt draussen.
    spells: learnableSpells(build.spells, higherLevel(hero?.skills.WISDOM, build.skills.WISDOM)),
    skills: build.skills,
    luckBonus: build.luck,
    moraleBonus: build.morale,
    warMachines: build.warMachines,
    artifacts: build.artifacts,
  };
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
