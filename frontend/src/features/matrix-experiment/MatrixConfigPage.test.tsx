import {afterEach, beforeEach, describe, expect, it} from 'vitest';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {http, HttpResponse} from 'msw';
import MatrixConfigPage from './MatrixConfigPage';
import {useMatrixStore} from './matrixStore';
import {server} from '../../test/setup';

function renderConfig() {
    const queryClient = new QueryClient({
        defaultOptions: {queries: {retry: false}, mutations: {retry: false}},
    });
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/matrix']}>
                <Routes>
                    <Route path="/matrix" element={<MatrixConfigPage/>}/>
                    <Route path="/matrix/result" element={<div>MATRIX-RESULT-MARKER</div>}/>
                </Routes>
            </MemoryRouter>
        </QueryClientProvider>,
    );
}

async function waitForCatalog() {
    await waitFor(() => expect(screen.getByText('Faktionen')).toBeInTheDocument());
}

beforeEach(() => {
    useMatrixStore.setState({report: null, lastRequest: null});
});

afterEach(() => {
    server.resetHandlers();
});

describe('MatrixConfigPage', () => {
    it('renders one checkbox per faction', async () => {
        renderConfig();
        await waitForCatalog();
        // TEST_FACTIONS hat 3 Einträge — Castle, Rampart, Tower.
        expect(screen.getByLabelText('Castle')).toBeChecked();
        expect(screen.getByLabelText('Rampart')).toBeChecked();
        expect(screen.getByLabelText('Tower')).toBeChecked();
    });

    it('hides units when their faction is excluded', async () => {
        const user = userEvent.setup();
        renderConfig();
        await waitForCatalog();
        expect(screen.getByLabelText('Pikeman')).toBeInTheDocument();
        await user.click(screen.getByLabelText('Castle'));
        expect(screen.queryByLabelText('Pikeman')).not.toBeInTheDocument();
    });

    it('submits and hydrates the matrix store with the report and request', async () => {
        const user = userEvent.setup();
        renderConfig();
        await waitForCatalog();

        await user.click(screen.getByRole('button', {name: /Auswertung starten/}));

        await waitFor(() =>
            expect(screen.getByText('MATRIX-RESULT-MARKER')).toBeInTheDocument(),
        );
        const state = useMatrixStore.getState();
        expect(state.report).not.toBeNull();
        expect(state.lastRequest).not.toBeNull();
        expect(state.lastRequest!.unitCount).toBe(20);
    });

    it('shows an error when the experiment endpoint fails', async () => {
        server.use(
            http.post('*/api/experiments/matrix', () =>
                HttpResponse.json({message: 'boom'}, {status: 500}),
            ),
        );
        const user = userEvent.setup();
        renderConfig();
        await waitForCatalog();

        await user.click(screen.getByRole('button', {name: /Auswertung starten/}));

        await waitFor(() =>
            expect(screen.getByText(/Auswertung fehlgeschlagen/)).toBeInTheDocument(),
        );
    });
});
