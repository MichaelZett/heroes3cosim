import { useTranslation } from 'react-i18next';
import type { TFunction } from 'i18next';
import type { Faction, HeroDto, HeroSpecialtyDto, SkillLevel, Spell } from '../../shared/api/types';

/**
 * Die drei Fertigkeiten, die als Prozent auf den Schaden der Kreaturen wirken. Leadership, Luck
 * und Tactics bekommen weiter unten eigene Zeilen; die Magie-Fertigkeiten zeigt der Aufbau beim
 * Zauberbuch.
 *
 * Der enge Typ haelt die Liste mit den i18n-Keys army.heroSkill* synchron: ein Skill ohne
 * passenden Key waere ein Compile-Fehler statt eines fehlenden Labels zur Laufzeit.
 */
type DamageSkill = 'OFFENSE' | 'ARCHERY' | 'ARMORER';

const DAMAGE_SKILLS: DamageSkill[] = ['OFFENSE', 'ARCHERY', 'ARMORER'];

/** Manual S. 35/38: Offense 10/20/30 %, Archery 10/25/50 %, Armorer 5/10/15 %. */
const PERCENT: Record<DamageSkill, Partial<Record<SkillLevel, number>>> = {
  OFFENSE: { BASIC: 10, ADVANCED: 20, EXPERT: 30 },
  ARCHERY: { BASIC: 10, ADVANCED: 25, EXPERT: 50 },
  ARMORER: { BASIC: 5, ADVANCED: 10, EXPERT: 15 },
};

/** Leadership und Luck, Manual S. 37: je +1/2/3 auf das Moral- bzw. Glücks-Rating der Armee. */
const RATING_BONUS: Partial<Record<SkillLevel, number>> = { BASIC: 1, ADVANCED: 2, EXPERT: 3 };

/** Tactics, Manual S. 42: Aufstellungszone in Spalten bei 1/2/3 Graden Vorsprung. */
const TACTICS_COLUMNS: Partial<Record<SkillLevel, number>> = { BASIC: 3, ADVANCED: 5, EXPERT: 7 };

/**
 * Der Katalog fuehrt 16 Helden je Fraktion; ohne Gruppen waere die Liste kaum zu ueberblicken.
 * Die Reihenfolge des Backends (Fraktion, martialische Klasse zuerst, Name) bleibt erhalten.
 */
function groupByFaction(heroes: HeroDto[]): [Faction, HeroDto[]][] {
  const groups = new Map<Faction, HeroDto[]>();
  for (const hero of heroes) {
    const group = groups.get(hero.faction);
    if (group) group.push(hero);
    else groups.set(hero.faction, [hero]);
  }
  return [...groups.entries()];
}

/** Text der Spezialitaet; die Felder des DTO je nach Art. */
function specialtyText(specialty: HeroSpecialtyDto, t: TFunction): string {
  if (specialty.kind === 'SPELL_UNIQUE') {
    return t('army.heroSpecialtySPELL_UNIQUE', {
      spell: specialty.description ? t(`spell.${specialty.description as Spell}`) : '',
    });
  }
  if (specialty.kind === 'SPELL') {
    return t(specialty.attack > 0 ? 'army.heroSpecialtySPELLFixed' : 'army.heroSpecialtySPELL', {
      spell: specialty.description ? t(`spell.${specialty.description as Spell}`) : '',
      percent: specialty.attack,
    });
  }
  return t(`army.heroSpecialty${specialty.kind}`, {
    unit: specialty.unitName ?? '',
    tier: specialty.creatureTier,
    skill: specialty.skill ? t(`secondarySkill.${specialty.skill}`) : '',
    attack: specialty.attack,
    defense: specialty.defense,
    damage: specialty.damage,
    description: specialty.description ?? '',
  });
}

interface HeroPickerProps {
  heroes: HeroDto[];
  selectedName: string | null;
  onChange: (name: string | null) => void;
}

/**
 * Auswahl des Helden, der diese Armee fuehrt. Jede Armee hat einen Helden: die leere Auswahl
 * ueberlaesst ihn dem Backend, das den Standardhelden der Fraktion mit dem groessten Goldwert
 * nimmt.
 *
 * Angezeigt werden Angriff und Verteidigung, Power und Knowledge fuer die Zauber sowie die
 * Fertigkeiten, die auf die Kreaturen wirken.
 */
export default function HeroPicker({ heroes, selectedName, onChange }: Readonly<HeroPickerProps>) {
  const { t } = useTranslation();
  const current = heroes.find((h) => h.name === selectedName) ?? null;
  const skillText = current
    ? DAMAGE_SKILLS.flatMap((skill) => {
        const level = current.skills[skill];
        if (!level || level === 'NONE') return [];
        const sign = skill === 'ARMORER' ? '-' : '+';
        const label = t(`army.heroSkill${skill}`);
        return [`${label} ${sign}${PERCENT[skill][level]}%`];
      }).join(', ')
    : '';
  const leadership = current ? RATING_BONUS[current.skills.LEADERSHIP ?? 'NONE'] : undefined;
  const luck = current ? RATING_BONUS[current.skills.LUCK ?? 'NONE'] : undefined;
  const tactics = current ? TACTICS_COLUMNS[current.skills.TACTICS ?? 'NONE'] : undefined;

  return (
    <div className="rounded-md border border-slate-800 bg-slate-950 p-3">
      <label className="block">
        <span className="text-sm text-slate-400">{t('army.hero')}</span>
        <select
          className="mt-1 w-full rounded-md border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100 focus:border-amber-500 focus:outline-none"
          value={selectedName ?? ''}
          onChange={(e) => onChange(e.target.value === '' ? null : e.target.value)}
        >
          <option value="">{t('army.heroNone')}</option>
          {groupByFaction(heroes).map(([faction, group]) => (
            <optgroup key={faction} label={t(`faction.${faction}`)}>
              {group.map((hero) => (
                <option key={hero.name} value={hero.name}>
                  {hero.name} ({t(`heroClass.${hero.heroClass}`)}) — {hero.attack}/{hero.defense}
                </option>
              ))}
            </optgroup>
          ))}
        </select>
      </label>
      {current && (
        <>
          <p className="mt-2 text-xs text-amber-300/80">
            {t('army.heroEffect', { attack: current.attack, defense: current.defense })}
          </p>
          <p className="mt-1 text-xs text-amber-300/80">
            {t('army.heroMagic', { power: current.power, knowledge: current.knowledge })}
          </p>
          {skillText && (
            <p className="mt-1 text-xs text-amber-300/80">
              {t('army.heroSkillEffective', { skills: skillText })}
            </p>
          )}
          {leadership !== undefined && (
            <p className="mt-1 text-xs text-amber-300/80">
              {t('army.heroSkillLEADERSHIP')}: {t('army.heroMorale', { value: `+${leadership}` })}
            </p>
          )}
          {tactics !== undefined && (
            <p className="mt-1 text-xs text-amber-300/80">
              {t('army.heroSkillTACTICS')}: {t('army.heroTactics', { columns: tactics })}
            </p>
          )}
          {current.specialty && (
            <p
              className={`mt-1 text-xs ${current.specialty.effective ? 'text-amber-300/80' : 'text-slate-500'}`}
            >
              {t('army.heroSpecialty')}: {specialtyText(current.specialty, t)}
              {!current.specialty.effective && ` — ${t('army.heroSpecialtyInert')}`}
            </p>
          )}
          {luck !== undefined && (
            <p className="mt-1 text-xs text-amber-300/80">
              {t('army.heroSkillLUCK')}: {t('army.heroLuck', { value: `+${luck}` })}
            </p>
          )}
        </>
      )}
    </div>
  );
}
