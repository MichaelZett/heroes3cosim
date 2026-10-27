import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { TEST_BANKS } from '../../test/fixtures';
import CreatureBankSection, { CreatureBankGuards } from './CreatureBankSection';

describe('CreatureBankSection', () => {
  it('chooses a bank and then its level or a random one', async () => {
    const onChange = vi.fn();
    const { rerender } = render(
      <CreatureBankSection
        banks={TEST_BANKS}
        bank={null}
        level={null}
        disabled={false}
        onChange={onChange}
      />,
    );
    expect(screen.queryByRole('combobox', { name: 'Stufe' })).not.toBeInTheDocument();
    await userEvent.selectOptions(screen.getByRole('combobox', { name: 'Kreaturenbank' }), 'CRYPT');
    expect(onChange).toHaveBeenCalledWith({ creatureBank: 'CRYPT' });

    rerender(
      <CreatureBankSection
        banks={TEST_BANKS}
        bank="CYCLOPS_STOCKPILE"
        level={null}
        disabled={false}
        onChange={onChange}
      />,
    );
    const level = screen.getByRole('combobox', { name: 'Stufe' });
    expect(Array.from(level.querySelectorAll('option')).map((o) => o.textContent)).toEqual([
      'Zufällig nach Anteil auf der Karte',
      'Stufe 1 (30 % der Banken)',
      'Stufe 2 (30 % der Banken)',
      'Stufe 3 (30 % der Banken)',
      'Stufe 4 (10 % der Banken)',
    ]);
    await userEvent.selectOptions(level, '4');
    expect(onChange).toHaveBeenLastCalledWith({ creatureBankLevel: 4 });
  });

  it('is locked during a siege', () => {
    render(
      <CreatureBankSection
        banks={TEST_BANKS}
        bank={null}
        level={null}
        disabled
        onChange={vi.fn()}
      />,
    );
    expect(screen.getByRole('combobox', { name: 'Kreaturenbank' })).toBeDisabled();
    expect(screen.getByText(/Eine Bank ist keine Stadt/)).toBeInTheDocument();
  });

  it('shows the guards of the chosen level with the upgrade chance', () => {
    render(<CreatureBankGuards bank={TEST_BANKS[0]} level={2} />);
    expect(screen.getByText('Wächter: Cyclops Stockpile')).toBeInTheDocument();
    expect(screen.getAllByText(/6 × Cyclops/)).toHaveLength(5);
    expect(screen.getByText('(50 %: Cyclops King)')).toBeInTheDocument();
    expect(screen.queryByText(/Stufe 1/)).not.toBeInTheDocument();
  });
});
