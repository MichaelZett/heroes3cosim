import type {BattleEvent} from '../../shared/api/types';

/**
 * Zerlegt jeden Move/MoveBack-Event in eine Sequenz von 1-Hex-Schritten entlang seines Pfads.
 * Sub-Moves bekommen je einen eigenen Eintrag im Event-Stream — so wandert der Token im Replay
 * sichtbar Schritt für Schritt, statt vom Start- direkt zum Endhex zu gleiten.
 */
export function explodeMoves(events: readonly BattleEvent[]): BattleEvent[] {
    const out: BattleEvent[] = [];
    for (const event of events) {
        if (event.type === 'Move' && event.path.length > 0) {
            let prevQ = event.fromQ;
            let prevR = event.fromR;
            for (const step of event.path) {
                out.push({
                    type: 'Move',
                    actor: event.actor,
                    actorSlot: event.actorSlot,
                    fromQ: prevQ,
                    fromR: prevR,
                    toQ: step.q,
                    toR: step.r,
                    path: [step],
                });
                prevQ = step.q;
                prevR = step.r;
            }
        } else if (event.type === 'MoveBack' && event.path.length > 0) {
            for (const step of event.path) {
                out.push({
                    type: 'MoveBack',
                    actor: event.actor,
                    actorSlot: event.actorSlot,
                    toQ: step.q,
                    toR: step.r,
                    path: [step],
                });
            }
        } else {
            out.push(event);
        }
    }
    return out;
}
