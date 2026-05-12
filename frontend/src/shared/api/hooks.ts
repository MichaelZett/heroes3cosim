import {useMutation, useQuery} from '@tanstack/react-query';
import {api} from './client';
import type {BattleConfigRequest, BattleSimulationDto} from './types';

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
