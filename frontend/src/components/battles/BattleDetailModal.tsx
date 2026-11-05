import React, { useState, useEffect, useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { fetchBattleDetail, fetchBattleRounds } from '../../services/api';
import { useMapStore } from '../../store/useMapStore';
import { BattleRoundRecordDto, BattleParticipantDto } from '../../types/api';
import {
  X,
  Swords,
  Calendar,
  Trophy,
  Skull,
  UserX,
  Footprints,
  Shield,
  Play,
  Pause,
  SkipBack,
  SkipForward,
  ChevronLeft,
  ChevronRight,
  AlertTriangle,
  Heart,
  Sparkles,
  Zap,
  Activity,
} from 'lucide-react';

export const BattleDetailModal: React.FC = () => {
  const selectedBattleId = useMapStore((s) => s.selectedBattleId);
  const closeBattleDetail = useMapStore((s) => s.closeBattleDetail);

  const [currentRound, setCurrentRound] = useState<number>(1);
  const [isPlaying, setIsPlaying] = useState<boolean>(false);
  const [replaySpeed, setReplaySpeed] = useState<number>(1.0); // 0.5x, 1x, 2x, 4x

  const { data: battle, isLoading: battleLoading } = useQuery({
    queryKey: ['battle-detail', selectedBattleId],
    queryFn: () => (selectedBattleId ? fetchBattleDetail(selectedBattleId) : null),
    enabled: !!selectedBattleId,
  });

  const { data: roundsData, isLoading: roundsLoading } = useQuery({
    queryKey: ['battle-rounds', selectedBattleId],
    queryFn: () => (selectedBattleId ? fetchBattleRounds(selectedBattleId) : null),
    enabled: !!selectedBattleId,
  });

  // Reset round when battle changes
  useEffect(() => {
    setCurrentRound(1);
    setIsPlaying(false);
  }, [selectedBattleId]);

  const maxRounds =
    battle?.totalRounds || (roundsData?.rounds ? Math.max(1, ...roundsData.rounds.map((r) => r.roundNumber)) : 1);

  // Playback timer
  useEffect(() => {
    let timer: ReturnType<typeof setInterval>;
    if (isPlaying) {
      const intervalMs = Math.max(250, 1500 / replaySpeed);
      timer = setInterval(() => {
        setCurrentRound((prev) => {
          if (prev >= maxRounds) {
            setIsPlaying(false);
            return prev;
          }
          return prev + 1;
        });
      }, intervalMs);
    }
    return () => clearInterval(timer);
  }, [isPlaying, maxRounds, replaySpeed]);

  // Derive participant health and states dynamically for the current round
  const combatantStateMap = useMemo(() => {
    const states: Record<
      string,
      {
        name: string;
        sideId: string;
        maxHp: number;
        currentHp: number;
        isDefeated: boolean;
        isEscaped: boolean;
        entityType: string;
        supernaturalClass: string;
        tier: number;
      }
    > = {};

    // 1. Initialize from participant DTOs if available
    if (roundsData?.participants && roundsData.participants.length > 0) {
      for (const p of roundsData.participants) {
        states[p.name] = {
          name: p.name,
          sideId: p.sideId,
          maxHp: p.maxHealth || 100,
          currentHp: p.initialHealth || p.maxHealth || 100,
          isDefeated: false,
          isEscaped: false,
          entityType: p.entityType,
          supernaturalClass: p.supernaturalClass,
          tier: p.tier,
        };
      }
    }

    // 2. Fallback: discover participants from actions if not provided
    if (roundsData?.rounds) {
      for (const r of roundsData.rounds) {
        if (r.actorName && !states[r.actorName]) {
          states[r.actorName] = {
            name: r.actorName,
            sideId: 'SIDE_A',
            maxHp: 100,
            currentHp: 100,
            isDefeated: false,
            isEscaped: false,
            entityType: 'PERSON',
            supernaturalClass: 'WARRIOR',
            tier: 1,
          };
        }
        if (r.targetName && !states[r.targetName]) {
          states[r.targetName] = {
            name: r.targetName,
            sideId: 'SIDE_B',
            maxHp: 100,
            currentHp: 100,
            isDefeated: false,
            isEscaped: false,
            entityType: 'MONSTER',
            supernaturalClass: 'BEAST',
            tier: 1,
          };
        }
      }

      // 3. Replay actions up to currentRound
      for (const r of roundsData.rounds) {
        if (r.roundNumber > currentRound) break;

        // Damage inflicted on target
        if (r.targetName && states[r.targetName] && r.damage > 0) {
          states[r.targetName].currentHp = Math.max(0, states[r.targetName].currentHp - r.damage);
        }

        // Healing on target or actor
        if (r.healing > 0) {
          const recipient = r.targetName && states[r.targetName] ? states[r.targetName] : states[r.actorName];
          if (recipient) {
            recipient.currentHp = Math.min(recipient.maxHp, recipient.currentHp + r.healing);
          }
        }

        // Status flags
        if (r.targetDefeated && r.targetName && states[r.targetName]) {
          states[r.targetName].isDefeated = true;
          states[r.targetName].currentHp = 0;
        }
        if (r.actorEscaped && r.actorName && states[r.actorName]) {
          states[r.actorName].isEscaped = true;
        }
      }
    }

    return states;
  }, [roundsData, currentRound]);

  // Split participants by side
  const { sideACombatants, sideBCombatants, sideAName, sideBName } = useMemo(() => {
    const list = Object.values(combatantStateMap);
    const sideA: typeof list = [];
    const sideB: typeof list = [];

    const sideNames = Array.from(new Set(list.map((c) => c.sideId)));
    const aName = sideNames[0] || 'Side A';
    const bName = sideNames[1] || 'Side B';

    for (const c of list) {
      if (c.sideId === aName) sideA.push(c);
      else sideB.push(c);
    }

    return {
      sideACombatants: sideA,
      sideBCombatants: sideB,
      sideAName: aName,
      sideBName: bName,
    };
  }, [combatantStateMap]);

  if (!selectedBattleId) return null;

  const currentRoundActions = roundsData?.rounds?.filter((r) => r.roundNumber === currentRound) || [];

  const getActionBadge = (actionType: string) => {
    switch (actionType.toUpperCase()) {
      case 'PHYSICAL_ATTACK':
        return (
          <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-rose-950 text-rose-300 border border-rose-800">
            Physical Attack
          </span>
        );
      case 'CLASS_ABILITY':
        return (
          <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-cyan-950 text-cyan-300 border border-cyan-800">
            Class Ability
          </span>
        );
      case 'VITALIST_HEAL':
        return (
          <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-950 text-emerald-300 border border-emerald-800">
            Vitalist Heal
          </span>
        );
      case 'WARDEN_BARRIER':
        return (
          <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-indigo-950 text-indigo-300 border border-indigo-800">
            Warden Barrier
          </span>
        );
      case 'DEFENSIVE_POSTURE':
        return (
          <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-slate-800 text-slate-300 border border-slate-700">
            Defensive Posture
          </span>
        );
      case 'WITHDRAWAL':
      case 'RETREAT':
        return (
          <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-amber-950 text-amber-300 border border-amber-800">
            Retreat
          </span>
        );
      default:
        return (
          <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-slate-800 text-slate-300 border border-slate-700">
            {actionType}
          </span>
        );
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 bg-black/85 backdrop-blur-md select-none animate-in fade-in duration-150">
      <div className="w-full max-w-5xl bg-slate-900 border border-slate-800 rounded-2xl shadow-2xl flex flex-col overflow-hidden max-h-[92vh]">
        {/* Header */}
        <div className="p-4 border-b border-slate-800 bg-slate-950 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-rose-950 text-rose-400 border border-rose-800/60 shadow-lg">
              <Swords className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-base font-bold text-slate-100">Tactical Battle Replay & Arena Dossier</h2>
                <span className="text-xs font-mono text-cyan-400 font-bold">#{selectedBattleId}</span>
              </div>
              <div className="flex items-center gap-3 text-xs text-slate-400 font-mono mt-0.5">
                {battle && (
                  <>
                    <span className="flex items-center gap-1">
                      <Calendar className="w-3.5 h-3.5 text-slate-500" />
                      Year {battle.year}, Month {battle.month}
                    </span>
                    <span>Type: {battle.battleType}</span>
                    <span className="text-emerald-400 font-semibold">Outcome: {battle.outcome}</span>
                    {battle.victoriousSideId && (
                      <span className="text-yellow-400 font-bold">Victor: {battle.victoriousSideId}</span>
                    )}
                  </>
                )}
              </div>
            </div>
          </div>

          <button
            onClick={closeBattleDetail}
            className="p-1.5 rounded-lg text-slate-400 hover:text-slate-200 hover:bg-slate-800 transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Modal Scroll Content */}
        <div className="flex-1 overflow-y-auto p-5 space-y-4 scrollbar-thin">
          {battleLoading && (
            <div className="py-20 text-center text-slate-400 text-xs font-mono">
              <div className="w-8 h-8 rounded-full border-2 border-rose-500/20 border-t-rose-400 animate-spin mx-auto mb-3" />
              Loading combat telemetry & participants...
            </div>
          )}

          {battle && (
            <>
              {/* Encounter Summary Banner */}
              <div className="p-3.5 rounded-xl bg-slate-950/70 border border-slate-800 text-xs text-slate-200 leading-relaxed">
                <div className="font-semibold text-slate-400 uppercase tracking-wider mb-1 text-[10px]">
                  Encounter Narrative
                </div>
                {battle.summaryText}
              </div>

              {/* TACTICAL ARENA: Side A vs Side B Participant Cards with Dynamic HP */}
              {roundsData?.hasRoundReplay && (sideACombatants.length > 0 || sideBCombatants.length > 0) && (
                <div className="p-4 rounded-xl bg-slate-950 border border-slate-800 space-y-3">
                  <div className="flex items-center justify-between pb-2 border-b border-slate-800 text-xs">
                    <span className="font-bold text-slate-300 uppercase tracking-wider flex items-center gap-1.5">
                      <Activity className="w-4 h-4 text-cyan-400" />
                      Tactical Arena Status — Round {currentRound} of {maxRounds}
                    </span>
                    <span className="text-[11px] font-mono text-slate-400">
                      Live Vitality & Casualty Telemetry
                    </span>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {/* Side A */}
                    <div className="space-y-2 p-3 rounded-xl bg-slate-900/60 border border-cyan-900/40">
                      <div className="flex items-center justify-between text-xs font-bold text-cyan-300 border-b border-cyan-950 pb-1.5">
                        <span className="flex items-center gap-1.5">
                          <Shield className="w-3.5 h-3.5 text-cyan-400" />
                          {sideAName}
                        </span>
                        <span className="text-[10px] font-mono text-cyan-400">
                          {sideACombatants.filter((c) => !c.isDefeated).length} Active
                        </span>
                      </div>

                      <div className="space-y-2 max-h-48 overflow-y-auto scrollbar-thin pr-1">
                        {sideACombatants.map((c) => {
                          const pct = Math.max(0, Math.min(100, (c.currentHp / c.maxHp) * 100));
                          let hpColor = 'bg-emerald-500';
                          if (pct < 30) hpColor = 'bg-rose-500';
                          else if (pct < 65) hpColor = 'bg-amber-500';

                          return (
                            <div
                              key={c.name}
                              className={`p-2 rounded-lg border text-xs transition ${
                                c.isDefeated
                                  ? 'bg-rose-950/20 border-rose-950/40 opacity-50'
                                  : 'bg-slate-950/70 border-slate-800'
                              }`}
                            >
                              <div className="flex items-center justify-between text-[11px] mb-1">
                                <span className="font-bold text-slate-200 flex items-center gap-1">
                                  {c.name}
                                  {c.tier > 1 && (
                                    <span className="text-[9px] px-1 rounded bg-purple-950 text-purple-300 border border-purple-800">
                                      T{c.tier}
                                    </span>
                                  )}
                                </span>
                                <span className="font-mono text-[10px] text-slate-400">
                                  {c.isDefeated ? (
                                    <span className="text-rose-400 font-bold flex items-center gap-0.5">
                                      <Skull className="w-3 h-3" /> DEFEATED
                                    </span>
                                  ) : c.isEscaped ? (
                                    <span className="text-emerald-400 font-bold flex items-center gap-0.5">
                                      <Footprints className="w-3 h-3" /> ESCAPED
                                    </span>
                                  ) : (
                                    `${Math.round(c.currentHp)} / ${c.maxHp} HP`
                                  )}
                                </span>
                              </div>
                              <div className="w-full h-1.5 bg-slate-800 rounded-full overflow-hidden">
                                <div
                                  className={`h-full ${hpColor} transition-all duration-300`}
                                  style={{ width: `${pct}%` }}
                                />
                              </div>
                            </div>
                          );
                        })}
                      </div>
                    </div>

                    {/* Side B */}
                    <div className="space-y-2 p-3 rounded-xl bg-slate-900/60 border border-rose-900/40">
                      <div className="flex items-center justify-between text-xs font-bold text-rose-300 border-b border-rose-950 pb-1.5">
                        <span className="flex items-center gap-1.5">
                          <Swords className="w-3.5 h-3.5 text-rose-400" />
                          {sideBName}
                        </span>
                        <span className="text-[10px] font-mono text-rose-400">
                          {sideBCombatants.filter((c) => !c.isDefeated).length} Active
                        </span>
                      </div>

                      <div className="space-y-2 max-h-48 overflow-y-auto scrollbar-thin pr-1">
                        {sideBCombatants.map((c) => {
                          const pct = Math.max(0, Math.min(100, (c.currentHp / c.maxHp) * 100));
                          let hpColor = 'bg-emerald-500';
                          if (pct < 30) hpColor = 'bg-rose-500';
                          else if (pct < 65) hpColor = 'bg-amber-500';

                          return (
                            <div
                              key={c.name}
                              className={`p-2 rounded-lg border text-xs transition ${
                                c.isDefeated
                                  ? 'bg-rose-950/20 border-rose-950/40 opacity-50'
                                  : 'bg-slate-950/70 border-slate-800'
                              }`}
                            >
                              <div className="flex items-center justify-between text-[11px] mb-1">
                                <span className="font-bold text-slate-200 flex items-center gap-1">
                                  {c.name}
                                  {c.tier > 1 && (
                                    <span className="text-[9px] px-1 rounded bg-purple-950 text-purple-300 border border-purple-800">
                                      T{c.tier}
                                    </span>
                                  )}
                                </span>
                                <span className="font-mono text-[10px] text-slate-400">
                                  {c.isDefeated ? (
                                    <span className="text-rose-400 font-bold flex items-center gap-0.5">
                                      <Skull className="w-3 h-3" /> DEFEATED
                                    </span>
                                  ) : c.isEscaped ? (
                                    <span className="text-emerald-400 font-bold flex items-center gap-0.5">
                                      <Footprints className="w-3 h-3" /> ESCAPED
                                    </span>
                                  ) : (
                                    `${Math.round(c.currentHp)} / ${c.maxHp} HP`
                                  )}
                                </span>
                              </div>
                              <div className="w-full h-1.5 bg-slate-800 rounded-full overflow-hidden">
                                <div
                                  className={`h-full ${hpColor} transition-all duration-300`}
                                  style={{ width: `${pct}%` }}
                                />
                              </div>
                            </div>
                          );
                        })}
                      </div>
                    </div>
                  </div>
                </div>
              )}

              {/* REPLAY CONTROLLER & ROUND ACTIONS */}
              <div className="p-4 rounded-xl bg-slate-950 border border-slate-800 space-y-3">
                <div className="flex items-center justify-between flex-wrap gap-2">
                  <div className="flex items-center gap-2">
                    <Zap className="w-4 h-4 text-cyan-400" />
                    <h3 className="text-xs font-bold text-slate-200 uppercase tracking-wider">
                      Tactical Action Playback
                    </h3>
                  </div>

                  {roundsData?.hasRoundReplay && (
                    <div className="flex items-center gap-1.5 flex-wrap">
                      {/* Speed selector */}
                      <div className="flex items-center gap-0.5 bg-slate-900 border border-slate-800 rounded-lg p-0.5 text-[10px] font-mono mr-1">
                        {[0.5, 1.0, 2.0, 4.0].map((spd) => (
                          <button
                            key={spd}
                            onClick={() => setReplaySpeed(spd)}
                            className={`px-1.5 py-0.5 rounded transition ${
                              replaySpeed === spd
                                ? 'bg-cyan-600 text-white font-bold'
                                : 'text-slate-400 hover:text-slate-200'
                            }`}
                          >
                            {spd}x
                          </button>
                        ))}
                      </div>

                      {/* Step Controls */}
                      <button
                        onClick={() => setCurrentRound(1)}
                        disabled={currentRound <= 1}
                        className="p-1 rounded bg-slate-900 hover:bg-slate-800 disabled:opacity-40 text-slate-300"
                        title="First Round"
                      >
                        <SkipBack className="w-3.5 h-3.5" />
                      </button>
                      <button
                        onClick={() => setCurrentRound((p) => Math.max(1, p - 1))}
                        disabled={currentRound <= 1}
                        className="p-1 rounded bg-slate-900 hover:bg-slate-800 disabled:opacity-40 text-slate-300"
                        title="Previous Round"
                      >
                        <ChevronLeft className="w-3.5 h-3.5" />
                      </button>

                      <button
                        onClick={() => setIsPlaying(!isPlaying)}
                        className={`px-2.5 py-1 rounded text-xs font-semibold flex items-center gap-1 transition ${
                          isPlaying
                            ? 'bg-amber-950 text-amber-300 border border-amber-800'
                            : 'bg-cyan-950 text-cyan-300 border border-cyan-800'
                        }`}
                      >
                        {isPlaying ? <Pause className="w-3 h-3" /> : <Play className="w-3 h-3" />}
                        <span>{isPlaying ? 'Pause' : 'Play'}</span>
                      </button>

                      <button
                        onClick={() => setCurrentRound((p) => Math.min(maxRounds, p + 1))}
                        disabled={currentRound >= maxRounds}
                        className="p-1 rounded bg-slate-900 hover:bg-slate-800 disabled:opacity-40 text-slate-300"
                        title="Next Round"
                      >
                        <ChevronRight className="w-3.5 h-3.5" />
                      </button>
                      <button
                        onClick={() => setCurrentRound(maxRounds)}
                        disabled={currentRound >= maxRounds}
                        className="p-1 rounded bg-slate-900 hover:bg-slate-800 disabled:opacity-40 text-slate-300"
                        title="Last Round"
                      >
                        <SkipForward className="w-3.5 h-3.5" />
                      </button>

                      <span className="text-xs font-mono font-bold text-cyan-400 ml-2">
                        Round {currentRound} / {maxRounds}
                      </span>
                    </div>
                  )}
                </div>

                {/* Case 1: Replay Unavailable for Historical Battles */}
                {roundsData && !roundsData.hasRoundReplay && (
                  <div className="p-4 rounded-xl bg-slate-900/80 border border-slate-800 text-center space-y-1.5 py-6">
                    <AlertTriangle className="w-6 h-6 text-amber-400 mx-auto" />
                    <div className="text-xs font-bold text-slate-200">
                      {roundsData.unavailableMessage || 'Detailed replay unavailable for this historical battle'}
                    </div>
                    <p className="text-[11px] text-slate-400 max-w-md mx-auto">
                      Tactical round logs are preserved for recent live combat operations. Summary combat metrics and casualty archives remain permanently authoritative above.
                    </p>
                  </div>
                )}

                {/* Case 2: Round Actions List with Floating Numbers */}
                {roundsData?.hasRoundReplay && (
                  <div className="space-y-2 mt-2">
                    <input
                      type="range"
                      min={1}
                      max={maxRounds}
                      value={currentRound}
                      onChange={(e) => setCurrentRound(Number(e.target.value))}
                      className="w-full h-1.5 bg-slate-800 rounded-lg appearance-none cursor-pointer accent-cyan-400"
                    />

                    {currentRoundActions.length === 0 ? (
                      <div className="py-6 text-center text-xs text-slate-500 italic">
                        No tactical actions logged in Round {currentRound}.
                      </div>
                    ) : (
                      <div className="space-y-2 max-h-64 overflow-y-auto scrollbar-thin pt-1">
                        {currentRoundActions.map((action, idx) => (
                          <div
                            key={idx}
                            className="p-3 rounded-lg bg-slate-900/90 border border-slate-800/80 flex flex-col gap-1.5 text-xs"
                          >
                            <div className="flex items-center justify-between gap-2 flex-wrap">
                              <div className="flex items-center gap-2">
                                <span className="font-mono text-slate-500 text-[10px]">
                                  #{action.actionSequence}
                                </span>
                                <span className="font-bold text-slate-200">{action.actorName}</span>
                                {action.targetName && (
                                  <>
                                    <span className="text-slate-500">➜</span>
                                    <span className="font-medium text-slate-300">{action.targetName}</span>
                                  </>
                                )}
                              </div>
                              {getActionBadge(action.actionType)}
                            </div>

                            <p className="text-slate-300 text-[11px] leading-relaxed font-sans">
                              {action.description}
                            </p>

                            <div className="flex items-center gap-3 text-[10px] font-mono text-slate-400 pt-1 border-t border-slate-800/40">
                              {action.damage > 0 && (
                                <span className="text-rose-400 font-bold px-1.5 py-0.5 rounded bg-rose-950/60 border border-rose-900">
                                  -{action.damage} DMG
                                </span>
                              )}
                              {action.healing > 0 && (
                                <span className="text-emerald-400 font-bold px-1.5 py-0.5 rounded bg-emerald-950/60 border border-emerald-900">
                                  +{action.healing} HP
                                </span>
                              )}
                              {action.staminaCost > 0 && <span>{action.staminaCost} Stamina</span>}
                              {action.aetherCost > 0 && (
                                <span className="text-cyan-400">{action.aetherCost} Aether</span>
                              )}
                              {action.injuryInflicted && (
                                <span className="text-amber-400 font-medium">
                                  Injury: {action.injuryInflicted}
                                </span>
                              )}
                              {action.targetDefeated && (
                                <span className="text-rose-400 font-bold uppercase animate-pulse">
                                  Defeated
                                </span>
                              )}
                              {action.actorEscaped && (
                                <span className="text-emerald-400 font-bold uppercase">
                                  Escaped
                                </span>
                              )}
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                )}
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
};
