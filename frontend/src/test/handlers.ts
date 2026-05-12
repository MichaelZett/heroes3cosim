import {http, HttpResponse} from 'msw';
import {simulationFixture, TEST_FACTIONS, TEST_UNITS} from './fixtures';

export const defaultHandlers = [
    http.get('*/api/units', () => HttpResponse.json(TEST_UNITS)),
    http.get('*/api/factions', () => HttpResponse.json(TEST_FACTIONS)),
    http.post('*/api/battles/simulate', () => HttpResponse.json(simulationFixture())),
];
