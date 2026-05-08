import type {BattleConfigRequest, BattleSimulationDto, Faction, UnitDto} from './types';

// Im Dev-Modus reicht Vite das `/api`-Präfix transparent an Spring auf 8080 weiter
// (siehe vite.config.ts), in Prod liegt das Frontend hinter dem gleichen Origin.
const API_BASE = '/api';

async function jsonFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    ...init,
  });
  if (!response.ok) {
    const text = await response.text();
    throw new Error(`API ${response.status} ${response.statusText}: ${text || path}`);
  }
  return (await response.json()) as T;
}

export const api = {
  listUnits(): Promise<UnitDto[]> {
    return jsonFetch<UnitDto[]>('/units');
  },
  listFactions(): Promise<Faction[]> {
    return jsonFetch<Faction[]>('/factions');
  },
  simulateBattle(request: BattleConfigRequest): Promise<BattleSimulationDto> {
    return jsonFetch<BattleSimulationDto>('/battles/simulate', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  },
};
