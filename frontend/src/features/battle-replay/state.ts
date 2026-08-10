import type { BattleEvent, HexCoord, Side, StackSnapshot } from '../../shared/api/types';

export interface SideState {
  side: Side;
  slot: number;
  unitName: string;
  count: number;
  topHp: number;
  maxHp: number;
  startCount: number;
  q: number;
  r: number;
}

export interface BattleState {
  width: number;
  height: number;
  obstacles: readonly HexCoord[];
  /** Schlüssel: `${side}-${slot}`. Quelle der Wahrheit für alle Stacks. */
  stacks: ReadonlyMap<string, SideState>;
  /** Convenience: Slot-0 Attacker (für Single-Battle-Konsumenten). */
  attacker: SideState;
  /** Convenience: Slot-0 Defender. */
  defender: SideState;
}

export function stackKey(side: Side, slot: number): string {
  return `${side}-${slot}`;
}

function snapshotToSideState(snap: StackSnapshot, maxHp: number, startCount: number): SideState {
  return {
    side: snap.side,
    slot: snap.slot ?? 0,
    unitName: snap.unitName,
    count: snap.count,
    topHp: snap.topHp,
    maxHp,
    startCount,
    q: snap.q,
    r: snap.r,
  };
}

function applySnapshot(side: SideState, snap: StackSnapshot): SideState {
  return {
    ...side,
    count: snap.count,
    topHp: snap.topHp,
    q: snap.q,
    r: snap.r,
  };
}

function snapshotInto(stacks: Map<string, SideState>, snap: StackSnapshot): void {
  const key = stackKey(snap.side, snap.slot ?? 0);
  const existing = stacks.get(key);
  if (!existing) return;
  stacks.set(key, applySnapshot(existing, snap));
}

function movePosition(
  stacks: Map<string, SideState>,
  side: Side,
  slot: number,
  q: number,
  r: number,
): void {
  const key = stackKey(side, slot);
  const existing = stacks.get(key);
  if (!existing) return;
  stacks.set(key, { ...existing, q, r });
}

function applyEvent(stacks: Map<string, SideState>, event: BattleEvent): void {
  switch (event.type) {
    case 'Move':
    case 'MoveBack':
      movePosition(stacks, event.actor, event.actorSlot ?? 0, event.toQ, event.toR);
      return;
    case 'Shoot':
    case 'Melee':
    case 'DeathStare':
    case 'Thunderbolts':
    case 'Retaliation':
      snapshotInto(stacks, event.targetAfter);
      return;
    case 'FireShield':
      snapshotInto(stacks, event.attackerAfter);
      return;
    case 'Rebirth':
      snapshotInto(stacks, event.actorAfter);
      return;
    default:
      // Marker-Events (Wait, TwoBlows, TwoShots, GoodMorale, Petrifying, Cursing,
      // Poisoning, Diseasing, Aging, BattleStart, BattleEnd) — kein Grid-State.
      return;
  }
}

function initialStacks(start: BattleEvent & { type: 'BattleStart' }): Map<string, SideState> {
  const stacks = new Map<string, SideState>();
  const initials: StackSnapshot[] = start.stacks?.length
    ? start.stacks
    : [start.attacker, start.defender];
  for (const snap of initials) {
    const slot = snap.slot ?? 0;
    stacks.set(stackKey(snap.side, slot), snapshotToSideState(snap, snap.topHp, snap.count));
  }
  return stacks;
}

/**
 * Pure reducer: takes the event list and a cursor (number of events played) and
 * derives the resulting battlefield snapshot. The frontend renders strictly from
 * this — no engine logic is duplicated client-side.
 */
export function reduceEvents(events: readonly BattleEvent[], cursor: number): BattleState | null {
  const start = events[0];
  if (start?.type !== 'BattleStart') return null;
  const stacks = initialStacks(start);
  const end = Math.min(cursor, events.length);
  for (let i = 1; i < end; i++) {
    applyEvent(stacks, events[i]);
  }
  const attacker =
    stacks.get(stackKey('ATTACKER', start.attacker.slot ?? 0)) ??
    snapshotToSideState(start.attacker, start.attacker.topHp, start.attacker.count);
  const defender =
    stacks.get(stackKey('DEFENDER', start.defender.slot ?? 0)) ??
    snapshotToSideState(start.defender, start.defender.topHp, start.defender.count);
  return {
    width: start.battlefieldWidth,
    height: start.battlefieldHeight,
    obstacles: start.obstacles ?? [],
    stacks,
    attacker,
    defender,
  };
}
