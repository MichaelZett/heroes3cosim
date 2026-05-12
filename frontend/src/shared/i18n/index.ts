import i18n from 'i18next';
import LanguageDetector from 'i18next-browser-languagedetector';
import {initReactI18next} from 'react-i18next';
import {de} from './locales/de';
import {en} from './locales/en';

export const SUPPORTED_LANGUAGES = ['de', 'en'] as const;
export type SupportedLanguage = (typeof SUPPORTED_LANGUAGES)[number];

void i18n
    .use(LanguageDetector)
    .use(initReactI18next)
    .init({
        resources: {
            de: {translation: de},
            en: {translation: en},
        },
        fallbackLng: 'de',
        supportedLngs: SUPPORTED_LANGUAGES,
        interpolation: {escapeValue: false},
        detection: {
            order: ['localStorage', 'navigator'],
            caches: ['localStorage'],
        },
    });

export default i18n;
