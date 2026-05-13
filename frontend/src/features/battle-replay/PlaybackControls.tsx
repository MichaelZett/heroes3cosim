import {useTranslation} from 'react-i18next';

interface PlaybackControlsProps {
    speedMs: number;
    onSpeedChange: (ms: number) => void;
    paused: boolean;
    onTogglePaused: () => void;
    onRestart: () => void;
    onStep: () => void;
    onRematch?: () => void;
    rematchPending?: boolean;
    finished: boolean;
}

const MIN_MS = 50;
const MAX_MS = 2000;
const REFERENCE_MS = 1000; // 1× Geschwindigkeit = 1000 ms zwischen Events

export default function PlaybackControls(props: Readonly<PlaybackControlsProps>) {
    const {t} = useTranslation();
    // Logarithmischer Slider, invertiert: links = langsam (lange Pause),
    // rechts = schnell (kurze Pause).
    const minLog = Math.log(MIN_MS);
    const maxLog = Math.log(MAX_MS);
    const sliderValue = Math.round(
        (1 - (Math.log(props.speedMs) - minLog) / (maxLog - minLog)) * 100,
    );

    function handleSliderChange(v: number) {
        const ms = Math.round(Math.exp(minLog + ((100 - v) / 100) * (maxLog - minLog)));
        props.onSpeedChange(ms);
    }

    const speedFactor = REFERENCE_MS / props.speedMs;

    return (
        <div className="flex flex-wrap items-center gap-4 rounded-lg border border-slate-800 bg-slate-900 p-4">
            <label className="flex flex-1 items-center gap-3 min-w-64">
                <span className="text-sm text-slate-400">{t('playback.speedLabel')}</span>
                <input
                    type="range"
                    min={0}
                    max={100}
                    value={sliderValue}
                    onChange={(e) => handleSliderChange(Number(e.target.value))}
                    className="flex-1 accent-amber-500"
                />
                <span className="w-16 text-right font-mono text-xs text-slate-500">
          {speedFactor >= 1 ? `${speedFactor.toFixed(1)}×` : `${speedFactor.toFixed(2)}×`}
        </span>
            </label>

            <div className="flex gap-2">
                <button
                    type="button"
                    onClick={props.onTogglePaused}
                    disabled={props.finished}
                    className="rounded-md border border-slate-700 px-3 py-1.5 text-sm text-slate-200 hover:border-amber-500 hover:text-amber-400 disabled:cursor-not-allowed disabled:opacity-40"
                >
                    {props.paused ? t('playback.play') : t('playback.pause')}
                </button>
                <button
                    type="button"
                    onClick={props.onStep}
                    disabled={props.finished}
                    className="rounded-md border border-slate-700 px-3 py-1.5 text-sm text-slate-200 hover:border-amber-500 hover:text-amber-400 disabled:cursor-not-allowed disabled:opacity-40"
                >
                    {t('playback.step')}
                </button>
                <button
                    type="button"
                    onClick={props.onRestart}
                    className="rounded-md border border-slate-700 px-3 py-1.5 text-sm text-slate-200 hover:border-amber-500 hover:text-amber-400"
                >
                    {t('playback.restart')}
                </button>
                {props.onRematch && (
                    <button
                        type="button"
                        onClick={props.onRematch}
                        disabled={props.rematchPending}
                        className="rounded-md border border-slate-700 px-3 py-1.5 text-sm text-slate-200 hover:border-amber-500 hover:text-amber-400 disabled:cursor-not-allowed disabled:opacity-40"
                    >
                        {props.rematchPending ? t('playback.rematchPending') : t('playback.rematch')}
                    </button>
                )}
            </div>
        </div>
    );
}
