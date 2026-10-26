import { describe, expect, it } from 'vitest';
import { TEST_ARTIFACTS } from '../../test/fixtures';
import {
  blockedSlots,
  candidatesFor,
  equipmentTotals,
  isValidEquipment,
  place,
  randomEquipment,
} from './artifacts';

const byId = (id: string) => TEST_ARTIFACTS.find((a) => a.id === id)!;

function seeded(seed: number): () => number {
  let state = seed;
  return () => {
    state = (state * 1103515245 + 12345) % 2147483648;
    return state / 2147483648;
  };
}

describe('artifacts', () => {
  it('lets a combination artifact block the slots of its other parts', () => {
    const equipped = { MISC_1: 'ELIXIR_OF_LIFE' };
    expect([...blockedSlots(equipped, TEST_ARTIFACTS).keys()]).toEqual(['RING_1', 'RING_2']);
    expect(candidatesFor('RING_1', equipped, TEST_ARTIFACTS)).toEqual([]);
    expect(isValidEquipment({ ...equipped, RING_1: 'RING_OF_VITALITY' }, TEST_ARTIFACTS)).toBe(
      false,
    );
  });

  it('rejects wrong slots, duplicates and parts next to their combination', () => {
    expect(isValidEquipment({ HEAD: 'SWORD_OF_HELLFIRE' }, TEST_ARTIFACTS)).toBe(false);
    expect(
      isValidEquipment(
        { MISC_1: 'CLOVER_OF_FORTUNE', MISC_2: 'CLOVER_OF_FORTUNE' },
        TEST_ARTIFACTS,
      ),
    ).toBe(false);
    expect(
      isValidEquipment({ MISC_1: 'ELIXIR_OF_LIFE', MISC_2: 'VIAL_OF_LIFEBLOOD' }, TEST_ARTIFACTS),
    ).toBe(false);
    expect(
      candidatesFor('MISC_2', { MISC_1: 'CLOVER_OF_FORTUNE' }, TEST_ARTIFACTS).map((a) => a.id),
    ).toEqual(['VIAL_OF_LIFEBLOOD', 'ELIXIR_OF_LIFE']);
  });

  it('places an artifact on the first free slot of its kind', () => {
    expect(place(byId('RING_OF_LIFE'), { RING_1: 'RING_OF_VITALITY' }, TEST_ARTIFACTS)).toEqual({
      RING_1: 'RING_OF_VITALITY',
      RING_2: 'RING_OF_LIFE',
    });
    expect(
      place(byId('ELIXIR_OF_LIFE'), { RING_1: 'RING_OF_VITALITY' }, TEST_ARTIFACTS),
    ).toBeNull();
  });

  it('rolls valid equipment by count and by gold value', () => {
    for (let seed = 1; seed <= 20; seed++) {
      const byCount = randomEquipment(TEST_ARTIFACTS, 'count', 3, seeded(seed));
      expect(Object.keys(byCount).length).toBeLessThanOrEqual(3);
      expect(isValidEquipment(byCount, TEST_ARTIFACTS)).toBe(true);
      const byValue = randomEquipment(TEST_ARTIFACTS, 'value', 12000, seeded(seed));
      expect(equipmentTotals(byValue, TEST_ARTIFACTS).value).toBeLessThanOrEqual(12000);
      expect(isValidEquipment(byValue, TEST_ARTIFACTS)).toBe(true);
    }
    expect(
      Object.keys(randomEquipment(TEST_ARTIFACTS, 'count', 99, seeded(7))).length,
    ).toBeGreaterThanOrEqual(4);
  });

  it('sums primary skills and gold', () => {
    expect(
      equipmentTotals({ RIGHT_HAND: 'SWORD_OF_HELLFIRE', HEAD: 'HELM_OF_CHAOS' }, TEST_ARTIFACTS),
    ).toEqual({ attack: 6, defense: 0, power: 0, knowledge: 3, value: 10000 });
  });
});
