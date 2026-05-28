import {useState} from 'react';
import {useMutation, useQuery} from '@tanstack/react-query';
import {api} from './client';
import type {
    ArmyBattleRequest,
    ArmyBattleSimulation,
    BattleConfigRequest,
    BattleSimulationDto,
    MatrixProgress,
    MatrixReport,
    MatrixRequestDto,
} from './types';

const POLL_INTERVAL_MS = 500;

export function useUnits() {
    return useQuery({queryKey: ['units'], queryFn: api.listUnits});
}

export function useFactions() {
    return useQuery({queryKey: ['factions'], queryFn: api.listFactions});
}

export function useSimulateBattle(
    onSuccess: (sim: BattleSimulationDto, request: BattleConfigRequest) => void,
) {
    return useMutation({
        mutationFn: (request: BattleConfigRequest) => api.simulateBattle(request),
        onSuccess: (sim, request) => onSuccess(sim, request),
    });
}

async function pollMatrixJob(
    jobId: string,
    onProgress: (p: MatrixProgress) => void,
): Promise<MatrixReport> {
    for (; ;) {
        await new Promise((r) => setTimeout(r, POLL_INTERVAL_MS));
        const snap = await api.getMatrixJob(jobId);
        onProgress({completed: snap.completed, total: snap.total});
        if (snap.status === 'COMPLETED') {
            if (!snap.report) throw new Error('Matrix job completed without report');
            return snap.report;
        }
        if (snap.status === 'FAILED') {
            throw new Error(snap.error ?? 'Matrix experiment failed');
        }
    }
}

export function useArmyPresets() {
    return useQuery({queryKey: ['armyPresets'], queryFn: api.listArmyPresets});
}

export function useSimulateArmyBattle(
    onSuccess: (sim: ArmyBattleSimulation, request: ArmyBattleRequest) => void,
) {
    return useMutation({
        mutationFn: (request: ArmyBattleRequest) => api.simulateArmyBattle(request),
        onSuccess: (sim, request) => onSuccess(sim, request),
    });
}

export function useRunMatrix(
    onSuccess: (report: MatrixReport, request: MatrixRequestDto) => void,
) {
    const [progress, setProgress] = useState<MatrixProgress | null>(null);

    const mutation = useMutation({
        mutationFn: async (request: MatrixRequestDto) => {
            const start = await api.startMatrix(request);
            setProgress({completed: start.completed, total: start.total});
            if (start.status === 'COMPLETED' && start.report) {
                return start.report;
            }
            if (start.status === 'FAILED') {
                throw new Error(start.error ?? 'Matrix experiment failed');
            }
            return pollMatrixJob(start.jobId, setProgress);
        },
        onSuccess: (report, request) => {
            setProgress(null);
            onSuccess(report, request);
        },
        onError: () => setProgress(null),
    });

    return Object.assign(mutation, {progress});
}
