import {beforeEach, describe, expect, it} from 'vitest';
import {render, screen, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import MatrixResultPage from './MatrixResultPage';
import {useMatrixStore} from './matrixStore';
import {matrixReportFixture} from '../../test/fixtures';

function renderRoutes() {
    return render(
        <MemoryRouter initialEntries={['/matrix/result']}>
            <Routes>
                <Route path="/matrix" element={<div>MATRIX-CONFIG-MARKER</div>}/>
                <Route path="/matrix/result" element={<MatrixResultPage/>}/>
            </Routes>
        </MemoryRouter>,
    );
}

beforeEach(() => {
    useMatrixStore.getState().reset();
});

describe('MatrixResultPage', () => {
    it('redirects to the matrix config page when no report is loaded', () => {
        renderRoutes();
        expect(screen.getByText('MATRIX-CONFIG-MARKER')).toBeInTheDocument();
    });

    it('renders stats and anomalies from the store', () => {
        useMatrixStore.setState({report: matrixReportFixture(), lastRequest: null});
        renderRoutes();

        // Anomalie-Liste enthält den eindeutig betroffenen Archer-Eintrag.
        const anomaliesSection = screen.getByText('Anomalien').closest('section')!;
        expect(within(anomaliesSection).getByText('Archer')).toBeInTheDocument();

        // Stats-Tabelle enthält alle drei Einheiten.
        const statsSection = screen.getByText('Alle Einheiten').closest('section')!;
        expect(within(statsSection).getByText('Halberdier')).toBeInTheDocument();
        expect(within(statsSection).getByText('Pikeman')).toBeInTheDocument();
        expect(within(statsSection).getByText('Archer')).toBeInTheDocument();
    });

    it('sorts the table by win-rate descending by default', () => {
        useMatrixStore.setState({report: matrixReportFixture(), lastRequest: null});
        renderRoutes();
        const statsSection = screen.getByText('Alle Einheiten').closest('section')!;
        const rows = within(statsSection).getAllByRole('row');
        // Erste Zeile ist Header; Halberdier (75%) > Pikeman (50%) > Archer (25%).
        expect(rows[1]).toHaveTextContent('Halberdier');
        expect(rows[2]).toHaveTextContent('Pikeman');
        expect(rows[3]).toHaveTextContent('Archer');
    });

    it('filters rows by unit name', async () => {
        useMatrixStore.setState({report: matrixReportFixture(), lastRequest: null});
        const user = userEvent.setup();
        renderRoutes();

        const filter = screen.getByPlaceholderText('Filtern…');
        await user.type(filter, 'pike');

        const statsSection = screen.getByText('Alle Einheiten').closest('section')!;
        expect(within(statsSection).getByText('Pikeman')).toBeInTheDocument();
        expect(within(statsSection).queryByText('Halberdier')).not.toBeInTheDocument();
        expect(within(statsSection).queryByText('Archer')).not.toBeInTheDocument();
    });

    it('flips sort direction when the same header is clicked again', async () => {
        useMatrixStore.setState({report: matrixReportFixture(), lastRequest: null});
        const user = userEvent.setup();
        renderRoutes();
        const statsSection = screen.getByText('Alle Einheiten').closest('section')!;
        // Win-Rate steht auch in den Anomalien — gezielt den Tabellen-Header treffen.
        await user.click(within(statsSection).getByText(/Win-Rate/));
        const rows = within(statsSection).getAllByRole('row');
        // Nach ascending: Archer (25%) zuerst.
        expect(rows[1]).toHaveTextContent('Archer');
        expect(rows[3]).toHaveTextContent('Halberdier');
    });
});
