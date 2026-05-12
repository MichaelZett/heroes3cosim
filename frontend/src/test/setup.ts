import '@testing-library/jest-dom/vitest';
import {afterAll, afterEach, beforeAll, beforeEach} from 'vitest';
import {setupServer} from 'msw/node';
import i18n from 'i18next';
import {initReactI18next} from 'react-i18next';
import {defaultHandlers} from './handlers';
import {de} from '../shared/i18n/locales/de';
import {en} from '../shared/i18n/locales/en';

void i18n.use(initReactI18next).init({
    resources: {de: {translation: de}, en: {translation: en}},
    lng: 'de',
    fallbackLng: 'de',
    interpolation: {escapeValue: false},
});

export const server = setupServer(...defaultHandlers);

beforeAll(() => server.listen({onUnhandledRequest: 'error'}));
beforeEach(() => {
    void i18n.changeLanguage('de');
});
afterEach(() => server.resetHandlers());
afterAll(() => server.close());
