import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {http, HttpResponse} from 'msw';
import ConfigPage from './ConfigPage';
import {useBattleStore} from '../battle-replay/battleStore';
import {server} from '../../test/setup';

function renderConfigPage() {
    const queryClient = new QueryClient({
        defaultOptions: {queries: {retry: false}},
    });
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/']}>
                <Routes>
                    <Route path="/" element={<ConfigPage/>}/>
                    <Route path="/battle" element={<div>BATTLE-PAGE-MARKER</div>}/>
                </Routes>
            </MemoryRouter>
        </QueryClientProvider>,
    );
}

async function waitForCatalogToLoad() {
    await waitFor(() => expect(screen.getByText('Truppe 1 (Angreifer)')).toBeInTheDocument());
}

beforeEach(() => {
    useBattleStore.setState({simulation: null, cursor: 0, speedMs: 400, paused: false});
});

afterEach(() => {
    vi.useRealTimers();
});

describe('ConfigPage', () => {
    it('blocks submission until both armies are picked', async () => {
        const user = userEvent.setup();
        renderConfigPage();
        await waitForCatalogToLoad();

        const submit = screen.getByRole('button', {name: /Kampf starten/});
        expect(submit).toBeDisabled();

        const [attackerUnit, defenderUnit] = screen.getAllByLabelText('Einheit');
        await user.selectOptions(attackerUnit, 'Pikeman');
        expect(submit).toBeDisabled();
        await user.selectOptions(defenderUnit, 'Centaur');
        expect(submit).toBeEnabled();
    });

    it('runs the simulation, hydrates the store and navigates to the battle page', async () => {
        const user = userEvent.setup();
        renderConfigPage();
        await waitForCatalogToLoad();

        const [attackerUnit, defenderUnit] = screen.getAllByLabelText('Einheit');
        await user.selectOptions(attackerUnit, 'Pikeman');
        await user.selectOptions(defenderUnit, 'Centaur');
        await user.click(screen.getByRole('button', {name: /Kampf starten/}));

        await waitFor(() => expect(screen.getByText('BATTLE-PAGE-MARKER')).toBeInTheDocument());
        expect(useBattleStore.getState().simulation).not.toBeNull();
    });

    it('shows an error message when the simulate endpoint fails', async () => {
        server.use(
            http.post('*/api/battles/simulate', () =>
                HttpResponse.json({message: 'boom'}, {status: 500}),
            ),
        );
        const user = userEvent.setup();
        renderConfigPage();
        await waitForCatalogToLoad();

        const [attackerUnit, defenderUnit] = screen.getAllByLabelText('Einheit');
        await user.selectOptions(attackerUnit, 'Pikeman');
        await user.selectOptions(defenderUnit, 'Centaur');
        await user.click(screen.getByRole('button', {name: /Kampf starten/}));

        await waitFor(() =>
            expect(screen.getByText(/Simulation fehlgeschlagen/)).toBeInTheDocument(),
        );
    });

    it('fills the seed field when the dice button is clicked', async () => {
        const user = userEvent.setup();
        vi.spyOn(Math, 'random').mockReturnValue(0.123456);
        renderConfigPage();
        await waitForCatalogToLoad();

        await user.click(screen.getByRole('button', {name: 'Würfeln'}));

        const seedInput = screen.getByPlaceholderText('z. B. 42') as HTMLInputElement;
        expect(seedInput.value).toBe('123456');
    });

    it('renders an API-down message when /api/units fails', async () => {
        server.use(
            http.get('*/api/units', () => HttpResponse.json({}, {status: 500})),
            http.get('*/api/factions', () => HttpResponse.json({}, {status: 500})),
        );
        renderConfigPage();

        await waitFor(() =>
            expect(screen.getByText(/API nicht erreichbar/)).toBeInTheDocument(),
        );
    });
});
