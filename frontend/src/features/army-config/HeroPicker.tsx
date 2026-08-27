import {useTranslation} from 'react-i18next';
import type {HeroDto} from '../../shared/api/types';

interface HeroPickerProps {
    heroes: HeroDto[];
    selectedName: string | null;
    onChange: (name: string | null) => void;
}

/**
 * Auswahl des Helden, der diese Armee fuehrt.
 *
 * Angezeigt werden bewusst nur Angriff und Verteidigung: nur diese beiden Primaerwerte
 * wirken heute. Power, Knowledge und die Sekundaerfertigkeiten liegen zwar im Katalog und
 * kommen ueber die API mit, haben aber noch keinen Effekt im Kampf — sie hier zu zeigen
 * waere ein Versprechen, das die Engine nicht einloest.
 */
export default function HeroPicker({heroes, selectedName, onChange}: Readonly<HeroPickerProps>) {
    const {t} = useTranslation();
    const current = heroes.find((h) => h.name === selectedName) ?? null;

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
                <p className="mt-2 text-xs text-amber-300/80">
                    {t('army.heroEffect', {attack: current.attack, defense: current.defense})}
                </p>
            )}
        </div>
    );
}
