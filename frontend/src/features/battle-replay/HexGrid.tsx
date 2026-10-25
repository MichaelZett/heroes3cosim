import { useTranslation } from 'react-i18next';
import type { BattleState } from './state';
import { fieldCells, gridDimensions, HEX_SIZE, hexToPixel } from './hex';
import StackToken from './StackToken';

interface HexGridProps {
  state: BattleState;
  transitionMs: number;
}

export default function HexGrid({ state, transitionMs }: Readonly<HexGridProps>) {
  const { t } = useTranslation();
  const { x0, w, h } = gridDimensions(state.width, state.height);
  const padding = HEX_SIZE;
  const viewBox = `${x0 - padding} ${-padding} ${w + padding} ${h + padding}`;

  const cells = fieldCells(state.width, state.height).map(({ q, r }) => ({
    q,
    r,
    pixel: hexToPixel(q, r),
  }));

  return (
    <svg
      viewBox={viewBox}
      className="w-full max-w-full select-none"
      aria-labelledby="battlefield-title"
    >
      <title id="battlefield-title">{t('battle.fieldAria')}</title>
      <defs>
        <polygon id="hex-cell" points={hexPolygonPoints()} />
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
        const { x, y } = hexToPixel(obstacle.q, obstacle.r);
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

      {state.siege?.moat.map((hex) => {
        const { x, y } = hexToPixel(hex.q, hex.r);
        return (
          <use
            key={`moat-${hex.q}-${hex.r}`}
            href="#hex-cell"
            x={x}
            y={y}
            fill="rgb(30 58 138)"
            stroke="rgb(59 130 246)"
            strokeWidth={0.5}
          >
            <title>{t('battle.moat')}</title>
          </use>
        );
      })}

      {state.siege?.permanentWall.map((hex) => (
        <WallHex
          key={`wall-${hex.q}-${hex.r}`}
          q={hex.q}
          r={hex.r}
          standing
          label={t('battle.wall')}
        />
      ))}

      {state.siege?.parts.map((part) =>
        part.hex ? (
          <WallHex
            key={`part-${part.part}`}
            q={part.hex.q}
            r={part.hex.r}
            standing={part.health > 0}
            label={`${t(`wallPart.${part.part}`)}: ${part.health}`}
          />
        ) : null,
      )}

      {[...state.stacks.values()].map((s) => (
        <StackToken
          key={`${s.side}-${s.slot}`}
          side={s}
          color={s.side === 'ATTACKER' ? 'amber' : 'blue'}
          transitionMs={transitionMs}
        />
      ))}
    </svg>
  );
}

/** Ein Mauerfeld: stehend in Stein, zerstört nur als Umriss. */
function WallHex({
  q,
  r,
  standing,
  label,
}: Readonly<{ q: number; r: number; standing: boolean; label: string }>) {
  const { x, y } = hexToPixel(q, r);
  return (
    <use
      href="#hex-cell"
      x={x}
      y={y}
      fill={standing ? 'rgb(120 113 108)' : 'rgb(41 37 36)'}
      stroke={standing ? 'rgb(214 211 209)' : 'rgb(120 113 108)'}
      strokeWidth={standing ? 1.2 : 0.8}
      strokeDasharray={standing ? undefined : '2 2'}
    >
      <title>{label}</title>
    </use>
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
