import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { http, HttpResponse } from 'msw';
import type { FactionMatrixRequestDto } from '../../shared/api/types';
import FactionMatrixPage from './FactionMatrixPage';
import { useFactionMatrixStore } from './factionMatrixStore';
import { server } from '../../test/setup';

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/factions']}>
        <FactionMatrixPage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  useFactionMatrixStore.getState().reset();
});

afterEach(() => {
  server.resetHandlers();
});

describe('FactionMatrixPage', () => {
  it('counts the battles from towns, seeds and solvers', async () => {
    const user = userEvent.setup();
    renderPage();
    // 9 Städte × 9 × 10 Seeds × 2 Solver
    expect(screen.getByText('9 Städte, insgesamt 1620 Kämpfe.')).toBeInTheDocument();

    await user.click(screen.getByLabelText('Greedy'));
    await user.click(screen.getByLabelText('Castle'));

    expect(screen.getByText('8 Städte, insgesamt 640 Kämpfe.')).toBeInTheDocument();
  });

  it('disables the start without two towns or a solver', async () => {
    const user = userEvent.setup();
    renderPage();
    const start = screen.getByRole('button', { name: /Auswertung starten/ });

    await user.click(screen.getByLabelText('Greedy'));
    await user.click(screen.getByLabelText('Strategic'));
    expect(start).toBeDisabled();

    await user.click(screen.getByLabelText('Strategic'));
    useFactionMatrixStore.getState().setForm({ factions: ['CASTLE'] });
    await waitFor(() => expect(start).toBeDisabled());
  });

  it('sends the options and shows ranking and matrix per solver', async () => {
    let sent: FactionMatrixRequestDto | null = null;
    server.use(
      http.post('*/api/experiments/faction-matrix', async ({ request }) => {
        sent = (await request.json()) as FactionMatrixRequestDto;
        return HttpResponse.json({
          jobId: 'job',
          status: 'RUNNING',
          completed: 0,
          total: 8,
          report: null,
          error: null,
        });
      }),
    );
    const user = userEvent.setup();
    renderPage();
    await user.selectOptions(screen.getByLabelText('Gelände'), 'WATER');
    expect(screen.getByLabelText('Magisches Gelände')).toBeDisabled();
    await user.click(screen.getByLabelText('Mit Standardhelden'));

    await user.click(screen.getByRole('button', { name: /Auswertung starten/ }));

    const matrix = await screen.findByRole('table', { name: 'Siegquoten Strategic' });
    expect(sent).toMatchObject({ terrain: 'WATER', magicTerrain: null, heroes: false, weeks: 1 });
    expect(
      within(matrix)
        .getAllByRole('cell')
        .map((c) => c.textContent),
    ).toEqual(['50%', '75%', '25%', '100%']);
    expect(screen.getByText(/Seitenlast im Spiegel Ø\|Δ0,5\| = 0\.250/)).toBeInTheDocument();
    expect(useFactionMatrixStore.getState().report?.matrices).toHaveLength(1);
  });

  it('shows an error when the endpoint fails', async () => {
    server.use(
      http.post('*/api/experiments/faction-matrix', () =>
        HttpResponse.json({ message: 'boom' }, { status: 500 }),
      ),
    );
    const user = userEvent.setup();
    renderPage();

    await user.click(screen.getByRole('button', { name: /Auswertung starten/ }));

    await waitFor(() => expect(screen.getByText(/Auswertung fehlgeschlagen/)).toBeInTheDocument());
  });
});
