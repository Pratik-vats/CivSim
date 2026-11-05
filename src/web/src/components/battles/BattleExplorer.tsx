import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { fetchBattles } from '../../services/api';
import { useMapStore } from '../../store/useMapStore';
import { BattleSummaryDto } from '../../types/api';
import {
  Swords,
  Calendar,
  ChevronLeft,
  ChevronRight,
  ShieldAlert,
  Skull,
  Trophy,
  ArrowRight,
  UserX,
  Footprints,
} from 'lucide-react';

export const BattleExplorer: React.FC = () => {
  const [page, setPage] = useState(0);
  const openBattleDetail = useMapStore((s) => s.openBattleDetail);

  const { data: pagedData, isLoading, isError } = useQuery({
    queryKey: ['battles-list', page],
    queryFn: () => fetchBattles(page, 20),
  });

  const getOutcomeBadge = (outcome: string) => {
    switch (outcome.toUpperCase()) {
      case 'VICTORY':
        return (
          <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase bg-emerald-950/80 text-emerald-300 border border-emerald-700/60">
            Victory
          </span>
        );
      case 'DEFEAT':
        return (
          <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase bg-rose-950/80 text-rose-300 border border-rose-700/60">
            Defeat
          </span>
        );
      case 'DRAW':
        return (
          <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase bg-slate-800 text-slate-300 border border-slate-700">
            Draw
          </span>
        );
      case 'MUTUAL_ANNIHILATION':
        return (
          <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase bg-purple-950/80 text-purple-300 border border-purple-700/60">
            Annihilation
          </span>
        );
      default:
        return (
          <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase bg-slate-800 text-slate-300 border border-slate-700">
            {outcome}
          </span>
        );
    }
  };

  return (
    <div className="flex-1 flex flex-col h-full overflow-hidden bg-slate-950/90 text-slate-100 select-none">
      {/* Top Header */}
      <div className="p-4 border-b border-slate-800 bg-slate-900/60 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="p-2 rounded-xl bg-rose-950/60 text-rose-400 border border-rose-800/60">
            <Swords className="w-5 h-5" />
          </div>
          <div>
            <h1 className="text-base font-bold text-slate-100">Tactical Battle Inspector</h1>
            <p className="text-xs text-slate-400">
              Historical combat encounters, martial engagements, and round-by-round tactical replays
            </p>
          </div>
        </div>
      </div>

      {/* Battles List Scroll Area */}
      <div className="flex-1 overflow-y-auto p-4 space-y-3 scrollbar-thin">
        {isLoading && (
          <div className="py-20 text-center text-slate-400 text-xs font-mono">
            <div className="w-8 h-8 rounded-full border-2 border-rose-500/20 border-t-rose-400 animate-spin mx-auto mb-3" />
            Loading combat records...
          </div>
        )}

        {isError && (
          <div className="p-4 rounded-xl bg-rose-950/40 border border-rose-800/60 text-center text-xs text-rose-300">
            Failed to retrieve battle records from simulation engine.
          </div>
        )}

        {!isLoading && pagedData?.content?.length === 0 && (
          <div className="text-center py-20 text-slate-500">
            <Swords className="w-12 h-12 mx-auto mb-2 text-slate-600" />
            <p className="text-sm font-semibold">No Combat Encounters Recorded Yet</p>
            <p className="text-xs mt-1">Battles will be recorded when skirmishes, monster tides, or clan wars erupt.</p>
          </div>
        )}

        {pagedData?.content?.map((battle: BattleSummaryDto) => (
          <div
            key={battle.battleId}
            onClick={() => openBattleDetail(battle.battleId)}
            className="p-4 rounded-xl bg-slate-900/60 hover:bg-slate-800/70 border border-slate-800 hover:border-rose-700/60 transition cursor-pointer flex flex-col gap-2.5 group"
          >
            {/* Header: Type, Outcome, Date */}
            <div className="flex items-center justify-between gap-2 flex-wrap">
              <div className="flex items-center gap-2">
                <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase bg-slate-800 text-slate-300 border border-slate-700">
                  {battle.battleType.replace('_', ' ')}
                </span>
                {getOutcomeBadge(battle.outcome)}
                <span className="text-xs font-bold text-slate-100 font-mono group-hover:text-rose-300 transition">
                  Encounter #{battle.battleId}
                </span>
              </div>

              <div className="flex items-center gap-3 text-xs text-slate-400 font-mono">
                <span className="flex items-center gap-1">
                  <Calendar className="w-3.5 h-3.5 text-slate-500" />
                  Year {battle.year}, Month {battle.month}
                </span>
                <span className="px-2 py-0.5 rounded bg-slate-950 text-slate-300 text-[10px] border border-slate-800">
                  {battle.totalRounds} Rounds
                </span>
              </div>
            </div>

            {/* Summary Text */}
            <p className="text-xs text-slate-300 leading-relaxed font-sans">{battle.summaryText}</p>

            {/* Metrics Footer */}
            <div className="flex items-center justify-between pt-2 border-t border-slate-800/50 flex-wrap gap-2 text-xs">
              <div className="flex items-center gap-4 text-slate-400">
                {battle.victoriousSideId && (
                  <span className="flex items-center gap-1 text-emerald-400 font-medium">
                    <Trophy className="w-3.5 h-3.5" />
                    Victor: {battle.victoriousSideId}
                  </span>
                )}
                <span className="flex items-center gap-1 text-rose-400">
                  <Skull className="w-3.5 h-3.5" />
                  {battle.fatalitiesCount} Fatalities
                </span>
                <span className="flex items-center gap-1 text-amber-400">
                  <UserX className="w-3.5 h-3.5" />
                  {battle.incapacitatedCount} Incapacitated
                </span>
                <span className="flex items-center gap-1 text-slate-400">
                  <Footprints className="w-3.5 h-3.5" />
                  {battle.escapedCount} Escaped
                </span>
              </div>

              <span className="text-rose-400 text-xs font-semibold flex items-center gap-1 group-hover:translate-x-1 transition">
                <span>Tactical Analysis</span>
                <ArrowRight className="w-3.5 h-3.5" />
              </span>
            </div>
          </div>
        ))}
      </div>

      {/* Pagination Footer */}
      {pagedData && pagedData.totalPages > 1 && (
        <div className="p-3 border-t border-slate-800 bg-slate-900/70 flex items-center justify-between text-xs text-slate-400">
          <div>
            Page <span className="font-semibold text-slate-200">{pagedData.page + 1}</span> of{' '}
            <span className="font-semibold text-slate-200">{pagedData.totalPages}</span> ({pagedData.totalElements}{' '}
            battles)
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={() => setPage(Math.max(0, page - 1))}
              disabled={page === 0}
              className="p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 disabled:opacity-40 disabled:cursor-not-allowed text-slate-300"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            <button
              onClick={() => setPage(Math.min(pagedData.totalPages - 1, page + 1))}
              disabled={pagedData.last}
              className="p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 disabled:opacity-40 disabled:cursor-not-allowed text-slate-300"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

