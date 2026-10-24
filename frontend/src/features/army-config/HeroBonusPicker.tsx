import { useTranslation } from 'react-i18next';
import type { SecondarySkill, SkillLevel, Spell } from '../../shared/api/types';
import {
  BUILD_SKILLS,
  HERO_SPELLS,
  MAX_HERO_LEVEL,
  WAR_MACHINES,
  higherLevel,
  learnableSpells,
  maxMana,
  maxSpellLevel,
  type HeroBuild,
} from './heroBuild';

const LUCK_VALUES = [0, 1, 2, 3];
const MORALE_VALUES = [-3, -2, -1, 0, 1, 2, 3];
const MAX_PRIMARY_BONUS = 99;
const LEVELS: SkillLevel[] = ['NONE', 'BASIC', 'ADVANCED', 'EXPERT'];
const SPELL_LEVELS = [1, 2, 3, 4, 5];
const FIELD_CLASS =
  'mt-1 w-full rounded-md border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100 focus:border-amber-500 focus:outline-none';

interface HeroBonusPickerProps {
  value: HeroBuild;
  /** Startfertigkeiten des gewaehlten Helden; darunter laesst sich keine Stufe waehlen. */
  startSkills: Partial<Record<SecondarySkill, SkillLevel>>;
  /** Power und Knowledge des gewaehlten Helden ohne Aufbau; ohne Auswahl 0. */
  basePower?: number;
  baseKnowledge?: number;
  /** Startzauber des Helden: immer im Buch, auch ueber dem Wisdom-Gate. */
  startSpells?: Spell[];
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

function NumberField({
  label,
  value,
  min,
  max,
  onChange,
}: Readonly<{
  label: string;
  value: number;
  min: number;
  max: number;
  onChange: (value: number) => void;
}>) {
  return (
    <label className="block">
      <span className="text-sm text-slate-400">{label}</span>
      <input
        type="number"
        min={min}
        max={max}
        className={FIELD_CLASS}
        value={value}
        onChange={(e) => {
          const n = clampInt(e.target.value, min, max);
          if (n !== null) onChange(n);
        }}
      />
    </label>
  );
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
  basePower = 0,
  baseKnowledge = 0,
  startSpells = [],
  onChange,
}: Readonly<HeroBonusPickerProps>) {
  const { t } = useTranslation();
  const wisdom = higherLevel(startSkills.WISDOM, value.skills.WISDOM);
  const maxLevel = maxSpellLevel(wisdom);
  const spellPower = Math.max(1, basePower + value.power);
  const mana = maxMana(
    baseKnowledge + value.knowledge,
    higherLevel(startSkills.INTELLIGENCE, value.skills.INTELLIGENCE),
  );
  const changeSkill = (skill: SecondarySkill, level: SkillLevel) => {
    const skills = { ...value.skills, [skill]: level };
    if (skill !== 'WISDOM') {
      onChange({ skills });
      return;
    }
    // Sinkt Wisdom, fallen Zauber aus dem Buch, die der Held nicht mehr lernen kann.
    onChange({
      skills,
      spells: learnableSpells(value.spells, higherLevel(startSkills.WISDOM, level)),
    });
  };
  const toggleSpell = (spell: Spell, on: boolean) =>
    onChange({
      spells: on ? [...value.spells, spell] : value.spells.filter((s) => s !== spell),
    });
  return (
    <div className="rounded-md border border-slate-800 bg-slate-950 p-3">
      <div className="grid grid-cols-3 gap-3">
        <NumberField
          label={t('army.heroLevel')}
          value={value.level}
          min={1}
          max={MAX_HERO_LEVEL}
          onChange={(level) => onChange({ level })}
        />
        <NumberField
          label={t('army.attackBonus')}
          value={value.attack}
          min={0}
          max={MAX_PRIMARY_BONUS}
          onChange={(attack) => onChange({ attack })}
        />
        <NumberField
          label={t('army.defenseBonus')}
          value={value.defense}
          min={0}
          max={MAX_PRIMARY_BONUS}
          onChange={(defense) => onChange({ defense })}
        />
        <NumberField
          label={t('army.powerBonus')}
          value={value.power}
          min={0}
          max={MAX_PRIMARY_BONUS}
          onChange={(power) => onChange({ power })}
        />
        <NumberField
          label={t('army.knowledgeBonus')}
          value={value.knowledge}
          min={0}
          max={MAX_PRIMARY_BONUS}
          onChange={(knowledge) => onChange({ knowledge })}
        />
        <label className="block">
          <span className="text-sm text-slate-400">{t('army.luckBonus')}</span>
          <select
            className={FIELD_CLASS}
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
            className={FIELD_CLASS}
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
      <label className="mt-3 flex items-center gap-2 text-sm text-slate-200">
        <input
          type="checkbox"
          checked={value.randomGrowth}
          onChange={(e) => onChange({ randomGrowth: e.target.checked })}
        />
        {t('army.randomGrowth')}
      </label>
      {value.randomGrowth && (
        <p className="mt-1 text-xs text-slate-500">{t('army.randomGrowthHint')}</p>
      )}
      <div className="mt-3 grid grid-cols-3 gap-3">
        {BUILD_SKILLS.map((skill) => {
          const start = rank(startSkills[skill]);
          const current = Math.max(start, rank(value.skills[skill]));
          return (
            <label key={skill} className="block">
              <span className="text-sm text-slate-400">{t(`secondarySkill.${skill}`)}</span>
              <select
                className={FIELD_CLASS}
                value={LEVELS[current]}
                onChange={(e) => changeSkill(skill, e.target.value as SkillLevel)}
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
      <fieldset className="mt-3">
        <legend className="text-sm text-slate-400">{t('army.warMachines')}</legend>
        <div className="mt-1 flex flex-wrap gap-4">
          {WAR_MACHINES.map((machine) => (
            <label key={machine} className="flex items-center gap-2 text-sm text-slate-200">
              <input
                type="checkbox"
                checked={value.warMachines.includes(machine)}
                onChange={(e) =>
                  onChange({
                    warMachines: e.target.checked
                      ? [...value.warMachines, machine]
                      : value.warMachines.filter((m) => m !== machine),
                  })
                }
              />
              {t(`warMachine.${machine}`)}
            </label>
          ))}
        </div>
      </fieldset>
      <fieldset className="mt-3">
        <legend className="text-sm text-slate-400">
          {t('army.spellbook', { power: spellPower, mana })}
        </legend>
        {SPELL_LEVELS.map((level) => (
          <div key={level} className="mt-1 flex flex-wrap items-center gap-x-4 gap-y-1">
            <span
              className={`w-14 text-xs ${level > maxLevel ? 'text-slate-600' : 'text-slate-500'}`}
            >
              {t('army.spellLevel', { level })}
            </span>
            {HERO_SPELLS.filter(([, l]) => l === level).map(([spell]) => {
              const start = startSpells.includes(spell);
              const locked = start || level > maxLevel;
              return (
                <label
                  key={spell}
                  className={`flex items-center gap-1 text-sm ${level > maxLevel && !start ? 'text-slate-600' : 'text-slate-200'}`}
                >
                  <input
                    type="checkbox"
                    disabled={locked}
                    checked={start || value.spells.includes(spell)}
                    onChange={(e) => toggleSpell(spell, e.target.checked)}
                  />
                  {t(`spell.${spell}`)}
                  {start && (
                    <span className="text-xs text-amber-300/80">{t('army.startSpell')}</span>
                  )}
                </label>
              );
            })}
          </div>
        ))}
      </fieldset>
      <p className="mt-2 text-xs text-slate-500">{t('army.bonusHint')}</p>
    </div>
  );
}
