import {useTranslation} from 'react-i18next';
import {SUPPORTED_LANGUAGES, type SupportedLanguage} from '../i18n';

export default function LanguageSwitcher() {
    const {t, i18n} = useTranslation();
    const current = (i18n.resolvedLanguage ?? 'de') as SupportedLanguage;

    return (
        <label className="flex items-center gap-2 text-xs text-slate-400">
            <span className="sr-only">{t('langSwitcher.label')}</span>
            <select
                value={current}
                onChange={(e) => void i18n.changeLanguage(e.target.value)}
                className="rounded-md border border-slate-700 bg-slate-900 px-2 py-1 text-slate-200 focus:border-amber-500 focus:outline-none"
            >
                {SUPPORTED_LANGUAGES.map((lng) => (
                    <option key={lng} value={lng}>
                        {t(`langSwitcher.${lng}`)}
                    </option>
                ))}
            </select>
        </label>
    );
}
