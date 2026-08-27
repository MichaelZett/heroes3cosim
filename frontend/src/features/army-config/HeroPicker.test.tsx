import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';
import HeroPicker from './HeroPicker';
import {TEST_HEROES} from '../../test/fixtures';

describe('HeroPicker', () => {
    it('offers every hero plus the option to fight without one', () => {
        render(<HeroPicker heroes={TEST_HEROES} selectedName={null} onChange={vi.fn()}/>);

        const options = screen.getAllByRole('option');
        expect(options).toHaveLength(TEST_HEROES.length + 1);
        expect(options[0]).toHaveValue('');
    });

    it('reports the selected hero by name', async () => {
        const onChange = vi.fn();
        render(<HeroPicker heroes={TEST_HEROES} selectedName={null} onChange={onChange}/>);

        await userEvent.selectOptions(screen.getByRole('combobox'), 'Tazar');

        expect(onChange).toHaveBeenCalledWith('Tazar');
    });

    it('reports null when the hero is removed again', async () => {
        const onChange = vi.fn();
        render(<HeroPicker heroes={TEST_HEROES} selectedName="Tazar" onChange={onChange}/>);

        await userEvent.selectOptions(screen.getByRole('combobox'), '');

        expect(onChange).toHaveBeenCalledWith(null);
    });

    it('shows the effect of the selected hero on the whole army', () => {
        render(<HeroPicker heroes={TEST_HEROES} selectedName="Crag Hack" onChange={vi.fn()}/>);

        // Crag Hack: Attack 4, Defense 0 - der Hinweis nennt beide Werte.
        expect(screen.getByText(/\+4/)).toBeInTheDocument();
    });

    it('stays silent while no hero is chosen', () => {
        render(<HeroPicker heroes={TEST_HEROES} selectedName={null} onChange={vi.fn()}/>);

        expect(screen.queryByText(/\+4/)).not.toBeInTheDocument();
    });
});
