import { useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { api } from './client';
import type {
  ArmyBattleRequest,
  ArmyBattleSimulation,
  BattleConfigRequest,
  BattleSimulationDto,
  FactionMatrixReport,
  FactionMatrixRequestDto,
  MatrixProgress,
  MatrixReport,
  MatrixRequestDto,
} from './types';

const POLL_INTERVAL_MS = 500;

export function useUnits() {
  return useQuery({ queryKey: ['units'], queryFn: api.listUnits });
}

export function useFactions() {
  return useQuery({ queryKey: ['factions'], queryFn: api.listFactions });
}

export function useHeroes() {
  return useQuery({ queryKey: ['heroes'], queryFn: api.listHeroes });
}

export function useArtifacts() {
  return useQuery({ queryKey: ['artifacts'], queryFn: api.listArtifacts });
}

export function useCreatureBanks() {
  return useQuery({ queryKey: ['creatureBanks'], queryFn: api.listCreatureBanks });
}

export function useSimulateBattle(
  onSuccess: (sim: BattleSimulationDto, request: BattleConfigRequest) => void,
) {
  return useMutation({
    mutationFn: (request: BattleConfigRequest) => api.simulateBattle(request),
    onSuccess: (sim, request) => onSuccess(sim, request),
  });
}

interface JobSnapshot<R> {
  jobId: string;
  status: 'RUNNING' | 'COMPLETED' | 'FAILED';
  completed: number;
  total: number;
  report: R | null;
  error: string | null;
}

/** Wartet auf das Ergebnis eines asynchronen Laufs und meldet den Fortschritt. */
async function awaitJob<R>(
  start: JobSnapshot<R>,
  poll: (jobId: string) => Promise<JobSnapshot<R>>,
  onProgress: (p: MatrixProgress) => void,
): Promise<R> {
  let snap = start;
  for (;;) {
    onProgress({ completed: snap.completed, total: snap.total });
    if (snap.status === 'COMPLETED') {
      if (!snap.report) throw new Error('Job completed without report');
      return snap.report;
    }
    if (snap.status === 'FAILED') {
      throw new Error(snap.error ?? 'Experiment failed');
    }
    await new Promise((r) => setTimeout(r, POLL_INTERVAL_MS));
    snap = await poll(snap.jobId);
  }
}

export function useArmyPresets() {
  return useQuery({ queryKey: ['armyPresets'], queryFn: api.listArmyPresets });
}

export function useSimulateArmyBattle(
  onSuccess: (sim: ArmyBattleSimulation, request: ArmyBattleRequest) => void,
) {
  return useMutation({
    mutationFn: (request: ArmyBattleRequest) => api.simulateArmyBattle(request),
    onSuccess: (sim, request) => onSuccess(sim, request),
  });
}

/** Startet einen asynchronen Lauf, pollt ihn bis zum Ende und hält den Fortschritt. */
function useJob<Q, R>(
  start: (request: Q) => Promise<JobSnapshot<R>>,
  poll: (jobId: string) => Promise<JobSnapshot<R>>,
  onSuccess: (report: R, request: Q) => void,
) {
  const [progress, setProgress] = useState<MatrixProgress | null>(null);

  const mutation = useMutation({
    mutationFn: async (request: Q) => awaitJob(await start(request), poll, setProgress),
    onSuccess: (report, request) => {
      setProgress(null);
      onSuccess(report, request);
    },
    onError: () => setProgress(null),
  });

  return Object.assign(mutation, { progress });
}

export function useRunMatrix(onSuccess: (report: MatrixReport, request: MatrixRequestDto) => void) {
  return useJob(api.startMatrix, api.getMatrixJob, onSuccess);
}

export function useRunFactionMatrix(
  onSuccess: (report: FactionMatrixReport, request: FactionMatrixRequestDto) => void,
) {
  return useJob(api.startFactionMatrix, api.getFactionMatrixJob, onSuccess);
}
