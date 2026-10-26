import type { ArtifactDto, ArtifactKind, ArtifactSlot } from '../../shared/api/types';

/** Die Plaetze am Helden in der Reihenfolge des Heldenfensters, je mit ihrer Art. */
export const ARTIFACT_SLOTS: readonly [ArtifactSlot, ArtifactKind][] = [
  ['HEAD', 'HELMET'],
  ['NECK', 'NECKLACE'],
  ['SHOULDERS', 'CAPE'],
  ['RIGHT_HAND', 'WEAPON'],
  ['LEFT_HAND', 'SHIELD'],
  ['TORSO', 'TORSO'],
  ['RING_1', 'RING'],
  ['RING_2', 'RING'],
  ['FEET', 'FEET'],
  ['MISC_1', 'MISC'],
  ['MISC_2', 'MISC'],
  ['MISC_3', 'MISC'],
  ['MISC_4', 'MISC'],
  ['MISC_5', 'MISC'],
];

const KIND_OF = new Map<ArtifactSlot, ArtifactKind>(ARTIFACT_SLOTS);

export type Equipped = Partial<Record<ArtifactSlot, string>>;

export function slotKind(slot: ArtifactSlot): ArtifactKind {
  return KIND_OF.get(slot) ?? 'MISC';
}

function byId(catalog: ArtifactDto[]): Map<string, ArtifactDto> {
  return new Map(catalog.map((a) => [a.id, a]));
}

/** Die Artefakte in der Ausruestung, ohne unbekannte Identifier. */
export function equippedArtifacts(equipped: Equipped, catalog: ArtifactDto[]): ArtifactDto[] {
  const index = byId(catalog);
  return ARTIFACT_SLOTS.flatMap(([slot]) => {
    const artifact = index.get(equipped[slot] ?? '');
    return artifact ? [artifact] : [];
  });
}

/**
 * Plaetze, die Kombi-Artefakte sperren, mit dem sperrenden Artefakt: je gesperrter Art der
 * naechste freie Platz dieser Art, wie im Heldenfenster.
 */
export function blockedSlots(
  equipped: Equipped,
  catalog: ArtifactDto[],
): Map<ArtifactSlot, ArtifactDto> {
  const blocked = new Map<ArtifactSlot, ArtifactDto>();
  for (const artifact of equippedArtifacts(equipped, catalog)) {
    for (const kind of artifact.blocks) {
      const free = ARTIFACT_SLOTS.find(
        ([slot, k]) => k === kind && !equipped[slot] && !blocked.has(slot),
      );
      if (free) blocked.set(free[0], artifact);
    }
  }
  return blocked;
}

/** Passt die Ausruestung so ins Heldenfenster? Gleiche Regeln wie im Backend. */
export function isValidEquipment(equipped: Equipped, catalog: ArtifactDto[]): boolean {
  const worn = equippedArtifacts(equipped, catalog);
  const ids = new Set(worn.map((a) => a.id));
  if (ids.size !== worn.length) return false;
  if (worn.some((a) => a.components.some((part) => ids.has(part)))) return false;
  const index = byId(catalog);
  const needed = new Map<ArtifactKind, number>();
  for (const [slot] of ARTIFACT_SLOTS) {
    const artifact = index.get(equipped[slot] ?? '');
    if (!artifact) continue;
    if (artifact.kind !== slotKind(slot)) return false;
    for (const kind of [artifact.kind, ...artifact.blocks]) {
      needed.set(kind, (needed.get(kind) ?? 0) + 1);
    }
  }
  return [...needed].every(
    ([kind, count]) => count <= ARTIFACT_SLOTS.filter(([, k]) => k === kind).length,
  );
}

/** Die Artefakte, die auf diesem Platz noch moeglich sind (das gewaehlte eingeschlossen). */
export function candidatesFor(
  slot: ArtifactSlot,
  equipped: Equipped,
  catalog: ArtifactDto[],
): ArtifactDto[] {
  const kind = slotKind(slot);
  return catalog.filter(
    (artifact) =>
      artifact.kind === kind &&
      (equipped[slot] === artifact.id ||
        isValidEquipment({ ...equipped, [slot]: artifact.id }, catalog)),
  );
}

/** Legt das Artefakt auf den ersten freien Platz seiner Art, oder null, wenn es nicht passt. */
export function place(
  artifact: ArtifactDto,
  equipped: Equipped,
  catalog: ArtifactDto[],
): Equipped | null {
  const blocked = blockedSlots(equipped, catalog);
  for (const [slot, kind] of ARTIFACT_SLOTS) {
    if (kind !== artifact.kind || equipped[slot] || blocked.has(slot)) continue;
    const next = { ...equipped, [slot]: artifact.id };
    return isValidEquipment(next, catalog) ? next : null;
  }
  return null;
}

export type RandomMode = 'count' | 'value';

/**
 * Zufaellige Ausruestung: in zufaelliger Reihenfolge alles, was noch passt, bis die Zahl der
 * Artefakte erreicht ist bzw. bis kein Artefakt mehr in den Goldwert passt.
 */
export function randomEquipment(
  catalog: ArtifactDto[],
  mode: RandomMode,
  limit: number,
  random: () => number = Math.random,
): Equipped {
  const shuffled = [...catalog];
  for (let i = shuffled.length - 1; i > 0; i--) {
    const j = Math.floor(random() * (i + 1));
    [shuffled[i], shuffled[j]] = [shuffled[j], shuffled[i]];
  }
  let equipped: Equipped = {};
  let count = 0;
  let value = 0;
  for (const artifact of shuffled) {
    if (mode === 'count' && count >= limit) break;
    if (mode === 'value' && value + artifact.value > limit) continue;
    const next = place(artifact, equipped, catalog);
    if (next) {
      equipped = next;
      count++;
      value += artifact.value;
    }
  }
  return equipped;
}

export interface EquipmentTotals {
  attack: number;
  defense: number;
  power: number;
  knowledge: number;
  value: number;
}

export function equipmentTotals(equipped: Equipped, catalog: ArtifactDto[]): EquipmentTotals {
  return equippedArtifacts(equipped, catalog).reduce(
    (sum, a) => ({
      attack: sum.attack + a.attack,
      defense: sum.defense + a.defense,
      power: sum.power + a.power,
      knowledge: sum.knowledge + a.knowledge,
      value: sum.value + a.value,
    }),
    { attack: 0, defense: 0, power: 0, knowledge: 0, value: 0 },
  );
}
