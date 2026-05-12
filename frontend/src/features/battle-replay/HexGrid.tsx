import {useTranslation} from 'react-i18next';
import type {BattleState} from './state';
import {gridDimensions, HEX_SIZE, hexToPixel} from './hex';
import StackToken from './StackToken';

interface HexGridProps {
    state: BattleState;
    transitionMs: number;
}

export default function HexGrid({state, transitionMs}: HexGridProps) {
    const {t} = useTranslation();
    const {w, h} = gridDimensions(state.width, state.height);
    const padding = HEX_SIZE;
    const viewBox = `${-padding} ${-padding} ${w + padding} ${h + padding}`;

    const cells: { q: number; r: number; pixel: { x: number; y: number } }[] = [];
    for (let r = 0; r < state.height; r++) {
        for (let q = 0; q < state.width; q++) {
            cells.push({q, r, pixel: hexToPixel(q, r)});
        }
    }

    return (
        <svg
            viewBox={viewBox}
            className="w-full max-w-full select-none"
            role="img"
            aria-label={t('battle.fieldAria')}
        >
            <defs>
                <polygon id="hex-cell" points={hexPolygonPoints()}/>
            </defs>

            {cells.map((cell) => (
                <use
                    key={`${cell.q}-${cell.r}`}
                    href="#hex-cell"
                    x={cell.pixel.x}
                    y={cell.pixel.y}
                    fill="rgb(15 23 42)"
                    stroke="rgb(51 65 85)"
                    strokeWidth={0.5}
                />
            ))}

            {state.obstacles.map((obstacle) => {
                const {x, y} = hexToPixel(obstacle.q, obstacle.r);
                return (
                    <use
                        key={`obstacle-${obstacle.q}-${obstacle.r}`}
                        href="#hex-cell"
                        x={x}
                        y={y}
                        fill="rgb(71 85 105)"
                        stroke="rgb(100 116 139)"
                        strokeWidth={0.8}
                    />
                );
            })}

            <StackToken side={state.attacker} color="amber" transitionMs={transitionMs}/>
            <StackToken side={state.defender} color="blue" transitionMs={transitionMs}/>
        </svg>
    );
}

function hexPolygonPoints(): string {
    const corners: string[] = [];
    for (let i = 0; i < 6; i++) {
        const angle = (Math.PI / 180) * (60 * i - 30);
        corners.push(
            `${(HEX_SIZE * Math.cos(angle)).toFixed(3)},${(HEX_SIZE * Math.sin(angle)).toFixed(3)}`,
        );
    }
    return corners.join(' ');
}
