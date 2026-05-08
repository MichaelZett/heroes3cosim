import { useEffect, useRef } from 'react';
import type { BattleEvent } from '../api/types';
import { eventToText } from './eventText';

interface EventLogProps {
  events: readonly BattleEvent[];
  cursor: number;
}

export default function EventLog({ events, cursor }: EventLogProps) {
  const listRef = useRef<HTMLOListElement>(null);

  useEffect(() => {
    const list = listRef.current;
    if (!list) return;
    const lastLi = list.lastElementChild as HTMLLIElement | null;
    lastLi?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  }, [cursor]);

  if (cursor === 0) {
    return <p className="text-sm text-slate-500">Replay läuft gleich los…</p>;
  }

  return (
    <ol ref={listRef} className="max-h-72 space-y-1 overflow-y-auto text-sm">
      {events.slice(0, cursor).map((event, idx) => {
        const isCurrent = idx === cursor - 1;
        return (
          <li
            key={idx}
            className={
              isCurrent
                ? 'rounded bg-amber-500/10 px-2 py-1 text-amber-200'
                : 'px-2 py-1 text-slate-400'
            }
          >
            <span className="text-slate-600">▸</span> {eventToText(event)}
          </li>
        );
      })}
    </ol>
  );
}
