import type { SideState } from './state';
import { HEX_SIZE, hexToPixel } from './hex';

interface StackTokenProps {
  side: SideState;
  color: 'amber' | 'blue';
  /** Animationsdauer für Move-Transitions in ms (an die Replay-Geschwindigkeit gekoppelt). */
  transitionMs: number;
}

const FIRST_MACHINE_SLOT = 20;

const COLOR_FILL: Record<'amber' | 'blue', string> = {
  amber: 'rgb(245 158 11)',
  blue: 'rgb(59 130 246)',
};
const COLOR_RING: Record<'amber' | 'blue', string> = {
  amber: 'rgb(252 211 77)',
  blue: 'rgb(147 197 253)',
};

export default function StackToken({ side, color, transitionMs }: Readonly<StackTokenProps>) {
  if (side.count <= 0) return null;
  const { x, y } = hexToPixel(side.q, side.r);
  const radius = HEX_SIZE * 0.7;

  const totalHp = (side.count - 1) * side.maxHp + side.topHp;
  const startTotalHp = side.startCount * side.maxHp;
  const hpRatio = startTotalHp === 0 ? 0 : Math.max(0, Math.min(1, totalHp / startTotalHp));
  const barWidth = HEX_SIZE * 1.6;
  const barHeight = 4;
  // Kriegsmaschinen stehen ab Slot 20 (Ballista, Ammo Cart, First Aid Tent).
  const machine = side.slot >= FIRST_MACHINE_SLOT;
  // Zweifeldrige Stacks reichen vom Kopf bis zum Feld dahinter, zum eigenen Rand hin.
  const tail = hexToPixel(side.side === 'ATTACKER' ? side.q - 1 : side.q + 1, side.r);
  const dx = side.doubleWide ? tail.x - x : 0;
  const left = Math.min(0, dx) - radius;
  const width = Math.abs(dx) + radius * 2;
  const middle = dx / 2;

  return (
    <g
      style={{ transition: `transform ${transitionMs}ms ease-in-out` }}
      transform={`translate(${x}, ${y})`}
    >
      {machine || side.doubleWide ? (
        <rect
          data-testid={side.doubleWide ? 'wide-token' : undefined}
          x={left}
          y={-radius}
          width={width}
          height={radius * 2}
          rx={machine ? 2 : radius}
          fill={COLOR_FILL[color]}
          stroke={COLOR_RING[color]}
          strokeWidth={1.5}
        />
      ) : (
        <circle r={radius} fill={COLOR_FILL[color]} stroke={COLOR_RING[color]} strokeWidth={1.5} />
      )}
      <text
        x={middle}
        textAnchor="middle"
        dominantBaseline="central"
        fontSize={HEX_SIZE * 0.65}
        fontWeight={700}
        fill="rgb(15 23 42)"
      >
        {machine ? side.unitName.charAt(0) : side.count}
      </text>
      {machine && <title>{side.unitName}</title>}
      <g transform={`translate(${middle - barWidth / 2}, ${radius + 2})`}>
        <rect width={barWidth} height={barHeight} fill="rgb(30 41 59)" rx={1} />
        <rect width={barWidth * hpRatio} height={barHeight} fill={COLOR_FILL[color]} rx={1} />
      </g>
    </g>
  );
}
