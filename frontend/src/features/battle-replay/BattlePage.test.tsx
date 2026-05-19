import {beforeEach, describe, expect, it} from 'vitest';
import {render, screen} from '@testing-library/react';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import BattlePage from './BattlePage';
import {useBattleStore} from './battleStore';
import {simulationFixture} from '../../test/fixtures';

function renderRoutes() {
    const queryClient = new QueryClient({
        defaultOptions: {queries: {retry: false}, mutations: {retry: false}},
    });
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/battle']}>
                <Routes>
                    <Route path="/" element={<div>CONFIG-PAGE-MARKER</div>}/>
                    <Route path="/battle" element={<BattlePage/>}/>
                </Routes>
            </MemoryRouter>
        </QueryClientProvider>,
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
        expect(screen.getByLabelText('Hex-Schlachtfeld')).toBeInTheDocument();
        expect(screen.getAllByText('Pikeman').length).toBeGreaterThan(0);
        expect(screen.getAllByText('Centaur').length).toBeGreaterThan(0);
    });
});
