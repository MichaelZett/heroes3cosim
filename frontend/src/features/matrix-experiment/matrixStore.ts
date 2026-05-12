import {create} from 'zustand';
import type {MatrixReport, MatrixRequestDto} from '../../shared/api/types';

export interface MatrixStore {
    report: MatrixReport | null;
    lastRequest: MatrixRequestDto | null;
    loadReport: (report: MatrixReport, request: MatrixRequestDto) => void;
    reset: () => void;
}

export const useMatrixStore = create<MatrixStore>((set) => ({
    report: null,
    lastRequest: null,
    loadReport: (report, request) => set({report, lastRequest: request}),
    reset: () => set({report: null, lastRequest: null}),
}));
