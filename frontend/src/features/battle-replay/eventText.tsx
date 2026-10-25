import { Trans, useTranslation } from 'react-i18next';
import type { TFunction } from 'i18next';
import type { BattleEvent, Side, WarMachine } from '../../shared/api/types';

export interface SideNames {
  /** Default-Name der Attacker-Seite (Single-Battle: Stack-Name). */
  attacker: string;
  /** Default-Name der Defender-Seite. */
  defender: string;
  /**
   * Optionaler Multi-Stack-Lookup: Key `${side}-${slot}`. Wird bevorzugt vor
   * {@link attacker}/{@link defender}; fehlt der Eintrag, fällt der Lookup auf
   * die Seiten-Defaults zurück.
   */
  bySlot?: ReadonlyMap<string, string>;
}

const SIDE_CLASS: Record<Side, string> = {
  ATTACKER: 'font-semibold text-amber-300',
  DEFENDER: 'font-semibold text-blue-300',
};

function sideName(side: Side, slot: number, names: SideNames): string {
  const perSlot = names.bySlot?.get(`${side}-${slot}`);
  if (perSlot) return perSlot;
  return side === 'ATTACKER' ? names.attacker : names.defender;
}

function sideSpan(side: Side) {
  return <span className={SIDE_CLASS[side]} />;
}

/** Moral mit Vorzeichen: `+1`, `0`, `-2` — das Vorzeichen ist hier die eigentliche Information. */
function signed(morale: number): string {
  return morale > 0 ? `+${morale}` : String(morale);
}

interface EventTextProps {
  event: BattleEvent;
  names: SideNames;
}

function machineList(machines: WarMachine[] | undefined, t: TFunction): string {
  return machines?.length ? machines.map((m) => t(`warMachine.${m}`)).join(', ') : '—';
}

/** Text eines Zauber-Treffers: abgewehrt, Schaden, Heilung oder liegende Wirkung. */
function spellHitKey(
  event: BattleEvent & { type: 'SpellHit' },
):
  | 'events.spellHitResisted'
  | 'events.spellHitDamage'
  | 'events.spellHitHealed'
  | 'events.spellHitEffect' {
  if (event.resisted) return 'events.spellHitResisted';
  if (event.damage > 0) return 'events.spellHitDamage';
  if (event.healed > 0) return 'events.spellHitHealed';
  return 'events.spellHitEffect';
}

export function EventText({ event, names }: Readonly<EventTextProps>) {
  const { t } = useTranslation();
  switch (event.type) {
    case 'BattleStart': {
      // Moral nur zeigen, wenn eine Seite überhaupt eine hat — bei 0 zu 0 wäre die
      // Zeile reines Rauschen.
      const attackerMorale = event.attackerMorale ?? 0;
      const defenderMorale = event.defenderMorale ?? 0;
      const attackerLuck = event.attackerLuck ?? 0;
      const defenderLuck = event.defenderLuck ?? 0;
      return (
        <>
          <Trans
            i18nKey="events.battleStart"
            values={{
              attacker: names.attacker,
              attackerCount: event.attacker.count,
              defender: names.defender,
              defenderCount: event.defender.count,
            }}
            components={{ actor: sideSpan('ATTACKER'), target: sideSpan('DEFENDER') }}
          />
          {(event.attackerHero || event.defenderHero) && (
            <>
              {' '}
              <Trans
                i18nKey="events.battleStartHeroes"
                values={{
                  attackerHero: event.attackerHero ?? '—',
                  defenderHero: event.defenderHero ?? '—',
                }}
                components={{ actor: sideSpan('ATTACKER'), target: sideSpan('DEFENDER') }}
              />
            </>
          )}
          {Boolean(event.attackerWarMachines?.length || event.defenderWarMachines?.length) && (
            <>
              {' '}
              <Trans
                i18nKey="events.battleStartWarMachines"
                values={{
                  attacker: machineList(event.attackerWarMachines, t),
                  defender: machineList(event.defenderWarMachines, t),
                }}
                components={{ actor: sideSpan('ATTACKER'), target: sideSpan('DEFENDER') }}
              />
            </>
          )}
          {(attackerLuck !== 0 || defenderLuck !== 0) && (
            <>
              {' '}
              <Trans
                i18nKey="events.battleStartLuck"
                values={{
                  attackerLuck: signed(attackerLuck),
                  defenderLuck: signed(defenderLuck),
                }}
                components={{ actor: sideSpan('ATTACKER'), target: sideSpan('DEFENDER') }}
              />
            </>
          )}
          {(attackerMorale !== 0 || defenderMorale !== 0) && (
            <>
              {' '}
              <Trans
                i18nKey="events.battleStartMorale"
                values={{
                  attackerMorale: signed(attackerMorale),
                  defenderMorale: signed(defenderMorale),
                }}
                components={{ actor: sideSpan('ATTACKER'), target: sideSpan('DEFENDER') }}
              />
            </>
          )}
        </>
      );
    }
    case 'Move':
      return (
        <Trans
          i18nKey="events.move"
          values={{
            actor: sideName(event.actor, event.actorSlot ?? 0, names),
            fromQ: event.fromQ,
            fromR: event.fromR,
            toQ: event.toQ,
            toR: event.toR,
          }}
          components={{ actor: sideSpan(event.actor) }}
        />
      );
    case 'MoveBack':
      return (
        <Trans
          i18nKey="events.moveBack"
          values={{
            actor: sideName(event.actor, event.actorSlot ?? 0, names),
            toQ: event.toQ,
            toR: event.toR,
          }}
          components={{ actor: sideSpan(event.actor) }}
        />
      );
    case 'Wait':
      return (
        <Trans
          i18nKey="events.wait"
          values={{ actor: sideName(event.actor, event.actorSlot ?? 0, names) }}
          components={{ actor: sideSpan(event.actor) }}
        />
      );
    case 'Shoot':
      return (
        <Trans
          i18nKey="events.shoot"
          values={{
            actor: sideName(event.actor, event.actorSlot ?? 0, names),
            target: sideName(event.target, event.targetSlot ?? 0, names),
            distance: event.distance,
            damage: event.damage,
            killed: event.killed,
          }}
          components={{ actor: sideSpan(event.actor), target: sideSpan(event.target) }}
        />
      );
    case 'Melee':
      return (
        <Trans
          i18nKey="events.melee"
          values={{
            actor: sideName(event.actor, event.actorSlot ?? 0, names),
            target: sideName(event.target, event.targetSlot ?? 0, names),
            damage: event.damage,
            killed: event.killed,
          }}
          components={{ actor: sideSpan(event.actor), target: sideSpan(event.target) }}
        />
      );
    case 'Retaliation':
      return (
        <Trans
          i18nKey="events.retaliation"
          values={{
            retaliator: sideName(event.retaliator, event.retaliatorSlot ?? 0, names),
            damage: event.damage,
            killed: event.killed,
          }}
          components={{ retaliator: sideSpan(event.retaliator) }}
        />
      );
    case 'TwoBlows':
      return (
        <Trans
          i18nKey="events.twoBlows"
          values={{ actor: sideName(event.actor, event.actorSlot ?? 0, names) }}
          components={{ actor: sideSpan(event.actor) }}
        />
      );
    case 'TwoShots':
      return (
        <Trans
          i18nKey="events.twoShots"
          values={{ actor: sideName(event.actor, event.actorSlot ?? 0, names) }}
          components={{ actor: sideSpan(event.actor) }}
        />
      );
    case 'GoodMorale':
      return (
        <Trans
          i18nKey="events.goodMorale"
          values={{ actor: sideName(event.actor, event.actorSlot ?? 0, names) }}
          components={{ actor: sideSpan(event.actor) }}
        />
      );
    case 'SpellCast':
      return (
        <Trans
          i18nKey={event.resisted ? 'events.spellResisted' : 'events.spellCast'}
          values={{
            actor: sideName(event.actor, event.actorSlot, names),
            spell: t(`spell.${event.spell}`),
            target: sideName(event.target, event.targetSlot, names),
          }}
          components={{ actor: sideSpan(event.actor), target: sideSpan(event.target) }}
        />
      );
    case 'HeroSpell':
      return (
        <Trans
          i18nKey="events.heroSpell"
          values={{
            spell: t(`spell.${event.spell}`),
            level: t(`army.skillLevel${event.level}`),
            mana: event.manaLeft,
          }}
          components={{ actor: sideSpan(event.actor) }}
          context={event.actor === 'ATTACKER' ? 'attacker' : 'defender'}
        />
      );
    case 'SpellHit':
      return (
        <>
          {event.reflected && `${t('events.spellMirrored')} `}
          <Trans
            i18nKey={spellHitKey(event)}
            values={{
              spell: t(`spell.${event.spell}`),
              target: sideName(event.target, event.targetSlot, names),
              damage: event.damage,
              killed: event.killed,
              healed: event.healed,
            }}
            components={{ target: sideSpan(event.target) }}
          />
        </>
      );
    case 'Summon':
      return (
        <Trans
          i18nKey="events.summon"
          values={{ count: event.count, unit: event.stack.unitName }}
          components={{ actor: sideSpan(event.actor) }}
          context={event.actor === 'ATTACKER' ? 'attacker' : 'defender'}
        />
      );
    case 'Dispel':
      return (
        <Trans
          i18nKey="events.dispel"
          values={{
            actor: sideName(event.actor, event.actorSlot, names),
            target: sideName(event.target, event.targetSlot, names),
            count: event.removed,
          }}
          components={{ actor: sideSpan(event.actor), target: sideSpan(event.target) }}
        />
      );
    case 'BallistaShot':
      return (
        <Trans
          i18nKey="events.ballistaShot"
          values={{
            target: sideName(event.target, event.targetSlot, names),
            damage: event.damage,
            killed: event.killed,
            extra: [
              event.doubled ? t('events.ballistaDoubled') : '',
              event.lucky ? t('events.ballistaLucky') : '',
            ]
              .filter(Boolean)
              .join(', '),
          }}
          components={{ actor: sideSpan(event.actor), target: sideSpan(event.target) }}
          context={event.actor === 'ATTACKER' ? 'attacker' : 'defender'}
        />
      );
    case 'CatapultShot':
      if (event.shooterSlot != null) {
        return (
          <Trans
            i18nKey="events.wallShot"
            values={{
              shooter: sideName('ATTACKER', event.shooterSlot, names),
              aimed: t(`wallPart.${event.aimed}`),
              hit: t(`wallPart.${event.hit}`),
              damage: event.damage,
              health: event.healthAfter,
            }}
            components={{ actor: sideSpan('ATTACKER') }}
          />
        );
      }
      return t('events.catapultShot', {
        aimed: t(`wallPart.${event.aimed}`),
        hit: t(`wallPart.${event.hit}`),
        damage: event.damage,
        health: event.healthAfter,
      });
    case 'TowerShot':
      return (
        <Trans
          i18nKey="events.towerShot"
          values={{
            tower: t(`wallPart.${event.tower}`),
            target: sideName('ATTACKER', event.targetSlot, names),
            damage: event.damage,
            killed: event.killed,
          }}
          components={{ target: sideSpan('ATTACKER') }}
        />
      );
    case 'QuakeHit':
      return t('events.quakeHit', { hit: t(`wallPart.${event.hit}`), health: event.healthAfter });
    case 'Drawbridge':
      return t(`events.drawbridge_${event.state}`);
    case 'MoatDamage':
      return (
        <Trans
          i18nKey={event.mine ? 'events.mineDamage' : 'events.moatDamage'}
          values={{
            target: sideName(event.side, event.slot, names),
            damage: event.damage,
            killed: event.killed,
          }}
          components={{ target: sideSpan(event.side) }}
        />
      );
    case 'FirstAid':
      return (
        <Trans
          i18nKey="events.firstAid"
          values={{ target: sideName(event.actor, event.targetSlot, names), healed: event.healed }}
          components={{ actor: sideSpan(event.actor), target: sideSpan(event.actor) }}
          context={event.actor === 'ATTACKER' ? 'attacker' : 'defender'}
        />
      );
    case 'TacticsPhase':
      return (
        <Trans
          i18nKey="events.tacticsPhase"
          values={{ columns: event.columns }}
          components={{ actor: sideSpan(event.actor) }}
          context={event.actor === 'ATTACKER' ? 'attacker' : 'defender'}
        />
      );
    case 'LuckyStrike':
      return (
        <Trans
          i18nKey="events.luckyStrike"
          values={{ actor: sideName(event.actor, event.actorSlot ?? 0, names) }}
          components={{ actor: sideSpan(event.actor) }}
        />
      );
    case 'BadMorale':
      return (
        <Trans
          i18nKey="events.badMorale"
          values={{ actor: sideName(event.actor, event.actorSlot ?? 0, names) }}
          components={{ actor: sideSpan(event.actor) }}
        />
      );
    case 'DeathStare':
      return (
        <Trans
          i18nKey="events.deathStare"
          values={{
            actor: sideName(event.actor, event.actorSlot ?? 0, names),
            target: sideName(event.target, event.targetSlot ?? 0, names),
            kills: event.kills,
          }}
          components={{ actor: sideSpan(event.actor), target: sideSpan(event.target) }}
        />
      );
    case 'Thunderbolts':
      return (
        <Trans
          i18nKey="events.thunderbolts"
          values={{
            actor: sideName(event.actor, event.actorSlot ?? 0, names),
            target: sideName(event.target, event.targetSlot ?? 0, names),
            damage: event.damage,
          }}
          components={{ actor: sideSpan(event.actor), target: sideSpan(event.target) }}
        />
      );
    case 'Petrifying':
      return (
        <Trans
          i18nKey="events.petrifying"
          values={{
            actor: sideName(event.actor, event.actorSlot ?? 0, names),
            target: sideName(event.target, event.targetSlot ?? 0, names),
          }}
          components={{ actor: sideSpan(event.actor), target: sideSpan(event.target) }}
        />
      );
    case 'Cursing':
      return (
        <Trans
          i18nKey="events.cursing"
          values={{
            actor: sideName(event.actor, event.actorSlot ?? 0, names),
            target: sideName(event.target, event.targetSlot ?? 0, names),
          }}
          components={{ actor: sideSpan(event.actor), target: sideSpan(event.target) }}
        />
      );
    case 'Poisoning':
      return (
        <Trans
          i18nKey="events.poisoning"
          values={{
            actor: sideName(event.actor, event.actorSlot ?? 0, names),
            target: sideName(event.target, event.targetSlot ?? 0, names),
          }}
          components={{ actor: sideSpan(event.actor), target: sideSpan(event.target) }}
        />
      );
    case 'Diseasing':
      return (
        <Trans
          i18nKey="events.diseasing"
          values={{
            actor: sideName(event.actor, event.actorSlot ?? 0, names),
            target: sideName(event.target, event.targetSlot ?? 0, names),
          }}
          components={{ actor: sideSpan(event.actor), target: sideSpan(event.target) }}
        />
      );
    case 'Aging':
      return (
        <Trans
          i18nKey="events.aging"
          values={{
            actor: sideName(event.actor, event.actorSlot ?? 0, names),
            target: sideName(event.target, event.targetSlot ?? 0, names),
          }}
          components={{ actor: sideSpan(event.actor), target: sideSpan(event.target) }}
        />
      );
    case 'FireShield':
      return (
        <Trans
          i18nKey="events.fireShield"
          values={{
            shielded: sideName(event.shielded, event.shieldedSlot ?? 0, names),
            attacker: sideName(event.attacker, event.attackerSlot ?? 0, names),
            damage: event.damage,
          }}
          components={{
            shielded: sideSpan(event.shielded),
            attacker: sideSpan(event.attacker),
          }}
        />
      );
    case 'Rebirth':
      return (
        <Trans
          i18nKey="events.rebirth"
          values={{
            actor: sideName(event.actor, event.actorSlot ?? 0, names),
            count: event.restoredCount,
          }}
          components={{ actor: sideSpan(event.actor) }}
        />
      );
    case 'Resurrection':
      return (
        <Trans
          i18nKey="events.resurrection"
          values={{
            actor: sideName(event.actor, event.actorSlot ?? 0, names),
            target: sideName(event.target, event.targetSlot ?? 0, names),
            count: event.restoredCount,
          }}
          components={{ actor: sideSpan(event.actor), target: sideSpan(event.target) }}
        />
      );
    case 'RaiseDemons':
      return (
        <Trans
          i18nKey="events.raiseDemons"
          values={{
            actor: sideName(event.actor, event.actorSlot ?? 0, names),
            count: event.raisedCount,
          }}
          components={{ actor: sideSpan(event.actor) }}
        />
      );
    case 'BattleEnd': {
      const winnerName =
        event.winner === 'DRAW' ? t('events.draw') : sideName(event.winner, 0, names);
      const winnerClass =
        event.winner === 'DRAW' ? 'font-semibold text-slate-300' : SIDE_CLASS[event.winner];
      return (
        <Trans
          i18nKey="events.battleEnd"
          values={{
            winner: winnerName,
            turns: event.turns,
            attackerSurvivors: event.attackerSurvivors,
            defenderSurvivors: event.defenderSurvivors,
          }}
          components={{ winner: <span className={winnerClass} /> }}
        />
      );
    }
  }
}
