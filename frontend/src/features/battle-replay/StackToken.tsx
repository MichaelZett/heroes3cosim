import type {SideState} from './state';
import {HEX_SIZE, hexToPixel} from './hex';

interface StackTokenProps {
    side: SideState;
    color: 'amber' | 'blue';
    /** Animationsdauer für Move-Transitions in ms (an die Replay-Geschwindigkeit gekoppelt). */
    transitionMs: number;
}

const COLOR_FILL: Record<'amber' | 'blue', string> = {
    amber: 'rgb(245 158 11)',
    blue: 'rgb(59 130 246)',
};
const COLOR_RING: Record<'amber' | 'blue', string> = {
    amber: 'rgb(252 211 77)',
    blue: 'rgb(147 197 253)',
};

export default function StackToken({side, color, transitionMs}: Readonly<StackTokenProps>) {
    if (side.count <= 0) return null;
    const {x, y} = hexToPixel(side.q, side.r);
    const radius = HEX_SIZE * 0.7;

    const totalHp = (side.count - 1) * side.maxHp + side.topHp;
    const startTotalHp = side.startCount * side.maxHp;
    const hpRatio = startTotalHp === 0 ? 0 : Math.max(0, Math.min(1, totalHp / startTotalHp));
    const barWidth = HEX_SIZE * 1.6;
    const barHeight = 4;

    return (
        <g
            style={{transition: `transform ${transitionMs}ms ease-in-out`}}
            transform={`translate(${x}, ${y})`}
        >
            <circle r={radius} fill={COLOR_FILL[color]} stroke={COLOR_RING[color]} strokeWidth={1.5}/>
            <text
                textAnchor="middle"
                dominantBaseline="central"
                fontSize={HEX_SIZE * 0.65}
                fontWeight={700}
                fill="rgb(15 23 42)"
            >
                {side.count}
            </text>
            <g transform={`translate(${-barWidth / 2}, ${radius + 2})`}>
                <rect width={barWidth} height={barHeight} fill="rgb(30 41 59)" rx={1}/>
                <rect width={barWidth * hpRatio} height={barHeight} fill={COLOR_FILL[color]} rx={1}/>
            </g>
        </g>
    );
}
