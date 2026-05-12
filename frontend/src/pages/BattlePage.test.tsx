import {beforeEach, describe, expect, it} from 'vitest';
import {render, screen} from '@testing-library/react';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import BattlePage from './BattlePage';
import {useBattleStore} from '../store/battleStore';
import {simulationFixture} from '../test/fixtures';

function renderRoutes() {
    return render(
        <MemoryRouter initialEntries={['/battle']}>
            <Routes>
                <Route path="/" element={<div>CONFIG-PAGE-MARKER</div>}/>
                <Route path="/battle" element={<BattlePage/>}/>
            </Routes>
        </MemoryRouter>,
    );
}

beforeEach(() => {
    useBattleStore.setState({simulation: null, cursor: 0, speedMs: 400, paused: true});
});

describe('BattlePage', () => {
    it('redirects to the config page when no simulation is loaded', () => {
        renderRoutes();
        expect(screen.getByText('CONFIG-PAGE-MARKER')).toBeInTheDocument();
    });

    it('renders the replay grid once a simulation is hydrated', () => {
        useBattleStore.setState({simulation: simulationFixture(), cursor: 1, paused: true});
        renderRoutes();
        expect(screen.getByRole('img', {name: 'Hex-Schlachtfeld'})).toBeInTheDocument();
        expect(screen.getAllByText('Pikeman').length).toBeGreaterThan(0);
        expect(screen.getAllByText('Centaur').length).toBeGreaterThan(0);
    });
});
