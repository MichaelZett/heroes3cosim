import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { TEST_ARTIFACTS } from '../../test/fixtures';
import ArtifactPicker from './ArtifactPicker';
import { isValidEquipment } from './artifacts';

describe('ArtifactPicker', () => {
  it('offers one list per slot with only the fitting artifacts', async () => {
    const onChange = vi.fn();
    render(<ArtifactPicker catalog={TEST_ARTIFACTS} value={{}} onChange={onChange} />);

    expect(screen.getAllByRole('combobox')).toHaveLength(15);
    const weapon = screen.getByRole('combobox', { name: 'Waffe' });
    expect(Array.from(weapon.querySelectorAll('option')).map((o) => o.textContent)).toEqual([
      '—',
      'Sword of Hellfire',
    ]);
    await userEvent.selectOptions(weapon, 'SWORD_OF_HELLFIRE');
    expect(onChange).toHaveBeenCalledWith({ RIGHT_HAND: 'SWORD_OF_HELLFIRE' });
  });

  it('locks the slots a combination artifact blocks and lists the effects', () => {
    render(
      <ArtifactPicker
        catalog={TEST_ARTIFACTS}
        value={{ MISC_1: 'ELIXIR_OF_LIFE' }}
        onChange={vi.fn()}
      />,
    );

    const ring = screen.getByRole('combobox', { name: 'Ring 1' });
    expect(ring).toBeDisabled();
    expect(ring).toHaveTextContent('gesperrt: Elixir of Life');
    expect(
      screen.getByText(/\+4 Lebenspunkte für alle Kreaturen, immun gegen Curse/),
    ).toBeInTheDocument();
    expect(screen.getByText('Artefakte — 1 getragen, 20000 Gold')).toBeInTheDocument();
  });

  it('rolls a random equipment and clears it again', async () => {
    const onChange = vi.fn();
    render(
      <ArtifactPicker
        catalog={TEST_ARTIFACTS}
        value={{ HEAD: 'HELM_OF_CHAOS' }}
        onChange={onChange}
      />,
    );

    await userEvent.click(screen.getByRole('button', { name: 'Würfeln' }));
    const rolled = onChange.mock.calls[0][0];
    expect(Object.keys(rolled).length).toBeGreaterThan(0);
    expect(isValidEquipment(rolled, TEST_ARTIFACTS)).toBe(true);

    await userEvent.selectOptions(screen.getByRole('combobox', { name: 'Zufällig' }), 'value');
    expect(screen.getByRole('spinbutton', { name: 'Gold höchstens' })).toHaveValue(30000);
    await userEvent.click(screen.getByRole('button', { name: 'Leeren' }));
    expect(onChange).toHaveBeenLastCalledWith({});
  });
});
