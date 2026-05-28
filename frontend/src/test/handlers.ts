import {http, HttpResponse} from 'msw';
import type {ArmyPresetsResponse, MatrixJobSnapshot} from '../shared/api/types';
import {matrixReportFixture, simulationFixture, TEST_FACTIONS, TEST_UNITS} from './fixtures';

const COMPLETED_JOB: MatrixJobSnapshot = {
    jobId: 'test-job',
    status: 'COMPLETED',
    completed: 100,
    total: 100,
    report: matrixReportFixture(),
    error: null,
};

const ARMY_PRESETS: ArmyPresetsResponse = {
    presets: [
        {
            faction: 'CASTLE',
            stacks: [
                {unitName: 'Halberdier', count: 14},
                {unitName: 'Marksman', count: 9},
            ],
        },
    ],
};

export const defaultHandlers = [
    http.get('*/api/units', () => HttpResponse.json(TEST_UNITS)),
    http.get('*/api/factions', () => HttpResponse.json(TEST_FACTIONS)),
    http.post('*/api/battles/simulate', () => HttpResponse.json(simulationFixture())),
    http.post('*/api/experiments/matrix', () => HttpResponse.json(COMPLETED_JOB)),
    http.get('*/api/experiments/matrix/:jobId', () => HttpResponse.json(COMPLETED_JOB)),
    http.get('*/api/army-battles/presets', () => HttpResponse.json(ARMY_PRESETS)),
    http.post('*/api/army-battles/simulate', () => HttpResponse.json(simulationFixture())),
];
