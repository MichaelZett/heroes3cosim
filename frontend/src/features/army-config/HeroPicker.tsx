import {useTranslation} from 'react-i18next';
import type {HeroDto, SkillLevel} from '../../shared/api/types';

/**
 * Die drei Fertigkeiten, die als Prozent auf den Schaden wirken. Scholar und Necromancy wirken
 * ausserhalb des Kampfes, Tactics hat kein Katalog-Held — sie hier zu zeigen waere ein
 * Versprechen, das die Simulation nicht einloest. Leadership wirkt zwar, aber nicht auf den
 * Schaden; es bekommt weiter unten eine eigene Zeile.
 *
 * Der enge Typ haelt die Liste mit den i18n-Keys army.heroSkill* synchron: ein Skill ohne
 * passenden Key waere ein Compile-Fehler statt eines fehlenden Labels zur Laufzeit.
 */
type DamageSkill = 'OFFENSE' | 'ARCHERY' | 'ARMORER';

const DAMAGE_SKILLS: DamageSkill[] = ['OFFENSE', 'ARCHERY', 'ARMORER'];

/** Manual S. 35/38: Offense 10/20/30 %, Archery 10/25/50 %, Armorer 5/10/15 %. */
const PERCENT: Record<DamageSkill, Partial<Record<SkillLevel, number>>> = {
    OFFENSE: {BASIC: 10, ADVANCED: 20, EXPERT: 30},
    ARCHERY: {BASIC: 10, ADVANCED: 25, EXPERT: 50},
    ARMORER: {BASIC: 5, ADVANCED: 10, EXPERT: 15},
};

/** Leadership, Manual S. 37: +1/2/3 auf das Moral-Rating der Armee. */
const LEADERSHIP_MORALE: Partial<Record<SkillLevel, number>> = {BASIC: 1, ADVANCED: 2, EXPERT: 3};

interface HeroPickerProps {
    heroes: HeroDto[];
    selectedName: string | null;
    onChange: (name: string | null) => void;
}

/**
 * Auswahl des Helden, der diese Armee fuehrt.
 *
 * Angezeigt werden Angriff und Verteidigung sowie die vier Fertigkeiten, die die Engine
 * auswertet. Power und Knowledge bleiben aussen vor: sie steuern ausschliesslich das Zaubern
 * und haben ohne Zaubersystem keinen Effekt — sie hier zu zeigen waere ein Versprechen, das
 * die Simulation nicht einloest.
 */
export default function HeroPicker({heroes, selectedName, onChange}: Readonly<HeroPickerProps>) {
    const {t} = useTranslation();
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
    const leadership = current ? LEADERSHIP_MORALE[current.skills.LEADERSHIP ?? 'NONE'] : undefined;

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
                    {heroes.map((hero) => (
                        <option key={hero.name} value={hero.name}>
                            {hero.name} ({t(`faction.${hero.faction}`)}) — {hero.attack}/{hero.defense}
                        </option>
                    ))}
                </select>
            </label>
            {current && (
                <>
                    <p className="mt-2 text-xs text-amber-300/80">
                        {t('army.heroEffect', {attack: current.attack, defense: current.defense})}
                    </p>
                    {skillText && (
                        <p className="mt-1 text-xs text-amber-300/80">
                            {t('army.heroSkillEffective', {skills: skillText})}
                        </p>
                    )}
                    {leadership !== undefined && (
                        <p className="mt-1 text-xs text-amber-300/80">
                            {t('army.heroSkillLEADERSHIP')}:{' '}
                            {t('army.heroMorale', {value: `+${leadership}`})}
                        </p>
                    )}
                </>
            )}
        </div>
    );
}
