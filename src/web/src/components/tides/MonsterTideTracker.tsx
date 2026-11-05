import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { fetchMonsterTides } from '../../services/api';
import { useMapStore } from '../../store/useMapStore';
import { MonsterTideSummaryDto } from '../../types/api';
import {
  Skull,
  Calendar,
  ChevronLeft,
  ChevronRight,
  ShieldAlert,
  Eye,
  AlertTriangle,
  ArrowRight,
  Radio,
} from 'lucide-react';

const TIDE_STATES = [
  { id: 'ALL', label: 'All Invasions' },
  { id: 'MOBILIZING', label: 'Mobilizing' },
  { id: 'APPROACHING', label: 'Approaching' },
  { id: 'DETECTED', label: 'Detected' },
  { id: 'INVADING', label: 'Invading' },
  { id: 'RESOLVED', label: 'Resolved' },
];

export const MonsterTideTracker: React.FC = () => {
  const [page, setPage] = useState(0);
  const [selectedState, setSelectedState] = useState('ALL');
  const openTideDetail = useMapStore((s) => s.openTideDetail);

  const { data: pagedData, isLoading, isError } = useQuery({
    queryKey: ['monster-tides', page, selectedState],
    queryFn: () =>
      fetchMonsterTides({
        page,
        size: 20,
        state: selectedState === 'ALL' ? undefined : selectedState,
      }),
    refetchInterval: 3000,
  });

  const getStateBadge = (state: string) => {
    switch (state.toUpperCase()) {
      case 'MOBILIZING':
        return (
          <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase bg-amber-950/80 text-amber-300 border border-amber-700/60">
            Mobilizing
          </span>
        );
      case 'APPROACHING':
        return (
          <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase bg-purple-950/80 text-purple-300 border border-purple-700/60 animate-pulse">
            Approaching
          </span>
        );
      case 'DETECTED':
        return (
          <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase bg-rose-950/80 text-rose-300 border border-rose-700/60 animate-pulse">
            Detected
          </span>
        );
      case 'INVADING':
        return (
          <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase bg-rose-900 text-rose-100 border border-rose-600 animate-bounce">
            Invading
          </span>
        );
      case 'RESOLVED':
        return (
          <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase bg-slate-800 text-slate-300 border border-slate-700">
            Resolved
          </span>
        );
      default:
        return (
          <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase bg-slate-800 text-slate-300">
            {state}
          </span>
        );
    }
  };

  return (
    <div className="flex-1 flex flex-col h-full overflow-hidden bg-slate-950/90 text-slate-100 select-none">
      {/* Top Header */}
      <div className="p-4 border-b border-slate-800 bg-slate-900/60 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="p-2 rounded-xl bg-purple-950/60 text-purple-400 border border-purple-800/60">
            <Skull className="w-5 h-5" />
          </div>
          <div>
            <h1 className="text-base font-bold text-slate-100 flex items-center gap-2">
              Monster Tide Tracker
              <span className="flex items-center gap-1.5 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-purple-950/80 text-purple-300 border border-purple-700/60">
                <Radio className="w-3 h-3 text-purple-400" />
                Wilderness Threat Radar
              </span>
            </h1>
            <p className="text-xs text-slate-400">
              Live tracking of beast migrations, horde mobilization, seer detection warnings, and siege outcomes
            </p>
          </div>
        </div>
      </div>

      {/* State Filter Bar */}
      <div className="px-4 py-2.5 border-b border-slate-800 bg-slate-900/40 flex items-center gap-2 overflow-x-auto">
        {TIDE_STATES.map((st) => (
          <button
            key={st.id}
            onClick={() => {
              setSelectedState(st.id);
              setPage(0);
            }}
            className={`px-3 py-1 rounded-lg text-xs font-medium transition border ${
              selectedState === st.id
                ? 'bg-purple-950/80 text-purple-300 border-purple-700/70 shadow-sm'
                : 'bg-slate-900/60 text-slate-400 hover:text-slate-200 border-slate-800'
            }`}
          >
            {st.label}
          </button>
        ))}
      </div>

      {/* Tides List */}
      <div className="flex-1 overflow-y-auto p-4 space-y-3 scrollbar-thin">
        {isLoading && (
          <div className="py-20 text-center text-slate-400 text-xs font-mono">
            <div className="w-8 h-8 rounded-full border-2 border-purple-500/20 border-t-purple-400 animate-spin mx-auto mb-3" />
            Scanning wilderness zones for monster horde movements...
          </div>
        )}

        {isError && (
          <div className="p-4 rounded-xl bg-rose-950/40 border border-rose-800/60 text-center text-xs text-rose-300">
            Failed to query monster tide records from simulation backend.
          </div>
        )}

        {!isLoading && pagedData?.content?.length === 0 && (
          <div className="text-center py-20 text-slate-500">
            <ShieldAlert className="w-12 h-12 mx-auto mb-2 text-slate-600" />
            <p className="text-sm font-semibold">No Monster Tides Matching Filter</p>
            <p className="text-xs mt-1">Wilderness lairs are currently quiescent or outside selected radar state.</p>
          </div>
        )}

        {pagedData?.content?.map((tide: MonsterTideSummaryDto) => (
          <div
            key={tide.tideId}
            onClick={() => openTideDetail(tide.tideId)}
            className="p-4 rounded-xl bg-slate-900/60 hover:bg-slate-800/70 border border-slate-800 hover:border-purple-700/60 transition cursor-pointer flex flex-col gap-2.5 group"
          >
            {/* Header: ID, Target, State */}
            <div className="flex items-center justify-between gap-2 flex-wrap">
              <div className="flex items-center gap-2">
                <span className="font-mono text-xs font-bold text-slate-300">Tide #{tide.tideId}</span>
                <span className="text-slate-600">•</span>
                <span className="text-sm font-bold text-slate-100 group-hover:text-purple-300 transition">
                  Target: {tide.targetVillageName || `Settlement #${tide.targetVillageId}`}
                </span>
              </div>

              <div className="flex items-center gap-2">
                {getStateBadge(tide.state)}
                {tide.seerDetected && (
                  <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-cyan-950 text-cyan-300 border border-cyan-800 flex items-center gap-1">
                    <Eye className="w-3 h-3 text-cyan-400" />
                    Seer Warning: {tide.warningLeadMonths} Mo
                  </span>
                )}
              </div>
            </div>

            {/* Sub Info */}
            <div className="flex items-center justify-between text-xs text-slate-400 pt-1 border-t border-slate-800/40 flex-wrap gap-2">
              <div className="flex items-center gap-3 font-mono">
                <span>Lair: {tide.sourceLairId}</span>
                <span>•</span>
                <span className="text-purple-300 font-semibold">{tide.totalMobilizedCount} Beasts</span>
                <span>•</span>
                <span>
                  Mobilized Y{tide.mobilizedYear} M{tide.mobilizedMonth} ➜ Arrival Y{tide.arrivalYear} M{tide.arrivalMonth}
                </span>
              </div>

              <div className="flex items-center gap-2">
                {tide.outcome && (
                  <span className="text-xs font-semibold text-slate-300 bg-slate-950 px-2 py-0.5 rounded border border-slate-800">
                    {tide.outcome}
                  </span>
                )}
                <span className="text-purple-400 text-xs font-semibold flex items-center gap-1 group-hover:translate-x-1 transition">
                  <span>Invasion Dossier</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </span>
              </div>
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
            tides)
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

