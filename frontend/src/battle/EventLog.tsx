import {useEffect, useRef} from 'react';
import {useTranslation} from 'react-i18next';
import type {BattleEvent} from '../api/types';
import type {SideNames} from './eventText';
import {EventText} from './eventText';

interface EventLogProps {
  events: readonly BattleEvent[];
  cursor: number;
  names: SideNames;
}

export default function EventLog({ events, cursor, names }: EventLogProps) {
  const {t} = useTranslation();
  const listRef = useRef<HTMLOListElement>(null);

  useEffect(() => {
    const list = listRef.current;
    if (!list) return;
    const lastLi = list.lastElementChild as HTMLLIElement | null;
    lastLi?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  }, [cursor]);

  if (cursor === 0) {
    return <p className="text-sm text-slate-500">{t('battle.waitingForReplay')}</p>;
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
                ? 'rounded bg-amber-500/10 px-2 py-1 text-slate-200'
                : 'px-2 py-1 text-slate-400'
            }
          >
            <span className="text-slate-600">▸</span> <EventText event={event} names={names}/>
          </li>
        );
      })}
    </ol>
  );
}
