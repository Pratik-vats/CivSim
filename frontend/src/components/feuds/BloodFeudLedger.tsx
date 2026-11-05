import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { fetchFeuds } from '../../services/api';
import { useMapStore } from '../../store/useMapStore';
import { FeudSummaryDto } from '../../types/api';
import {
  Flame,
  Calendar,
  ChevronLeft,
  ChevronRight,
  ShieldAlert,
  ArrowRight,
  Swords,
  Clock,
} from 'lucide-react';

const FEUD_STATUSES = [
  { id: 'ALL', label: 'All Feuds' },
  { id: 'OPEN', label: 'Open' },
  { id: 'ESCALATING', label: 'Escalating' },
  { id: 'ACTIVE_RETALIATION', label: 'Active Retaliation' },
  { id: 'TRUCE', label: 'Truce' },
  { id: 'SETTLED', label: 'Settled' },
  { id: 'DORMANT', label: 'Dormant' },
];

export const BloodFeudLedger: React.FC = () => {
  const [page, setPage] = useState(0);
  const [selectedStatus, setSelectedStatus] = useState('ALL');
  const openFeudDetail = useMapStore((s) => s.openFeudDetail);

  const { data: pagedData, isLoading, isError } = useQuery({
    queryKey: ['feuds-list', page, selectedStatus],
    queryFn: () =>
      fetchFeuds({
        page,
        size: 20,
        status: selectedStatus === 'ALL' ? undefined : selectedStatus,
      }),
    refetchInterval: 3000,
  });

  const getEscalationBadge = (level: string) => {
    switch (level.toUpperCase()) {
      case 'LEVEL_4_ALL_OUT_BLOOD_WAR':
        return (
          <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase bg-rose-950 text-rose-300 border border-rose-700 animate-pulse">
            Level 4: Blood War
          </span>
        );
      case 'LEVEL_3_TARGETED_REPRISAL':
        return (
          <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase bg-amber-950 text-amber-300 border border-amber-700">
            Level 3: Reprisal
          </span>
        );
      case 'LEVEL_2_PROPERTY_SABOTAGE':
        return (
          <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase bg-amber-900/60 text-amber-200 border border-amber-800">
            Level 2: Sabotage
          </span>
        );
      case 'LEVEL_1_BORDER_FRICTION':
        return (
          <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase bg-slate-800 text-slate-300 border border-slate-700">
            Level 1: Friction
          </span>
        );
      default:
        return (
          <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase bg-slate-800 text-slate-300">
            {level}
          </span>
        );
    }
  };

  return (
    <div className="flex-1 flex flex-col h-full overflow-hidden bg-slate-950/90 text-slate-100 select-none">
      {/* Top Header */}
      <div className="p-4 border-b border-slate-800 bg-slate-900/60 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="p-2 rounded-xl bg-amber-950/60 text-amber-400 border border-amber-800/60">
            <Flame className="w-5 h-5" />
          </div>
          <div>
            <h1 className="text-base font-bold text-slate-100">Blood Feud & Vendetta Ledger</h1>
            <p className="text-xs text-slate-400">
              Multi-generational clan grievances, retaliatory escalations, and inter-family blood feuds
            </p>
          </div>
        </div>
      </div>

      {/* Filter Bar */}
      <div className="px-4 py-2.5 border-b border-slate-800 bg-slate-900/40 flex items-center gap-2 overflow-x-auto">
        {FEUD_STATUSES.map((st) => (
          <button
            key={st.id}
            onClick={() => {
              setSelectedStatus(st.id);
              setPage(0);
            }}
            className={`px-3 py-1 rounded-lg text-xs font-medium transition border ${
              selectedStatus === st.id
                ? 'bg-amber-950/80 text-amber-300 border-amber-700/70 shadow-sm'
                : 'bg-slate-900/60 text-slate-400 hover:text-slate-200 border-slate-800'
            }`}
          >
            {st.label}
          </button>
        ))}
      </div>

      {/* Feuds List */}
      <div className="flex-1 overflow-y-auto p-4 space-y-3 scrollbar-thin">
        {isLoading && (
          <div className="py-20 text-center text-slate-400 text-xs font-mono">
            <div className="w-8 h-8 rounded-full border-2 border-amber-500/20 border-t-amber-400 animate-spin mx-auto mb-3" />
            Loading historical vendettas and blood feuds...
          </div>
        )}

        {isError && (
          <div className="p-4 rounded-xl bg-rose-950/40 border border-rose-800/60 text-center text-xs text-rose-300">
            Failed to query blood feuds from simulation backend.
          </div>
        )}

        {!isLoading && pagedData?.content?.length === 0 && (
          <div className="text-center py-20 text-slate-500">
            <Flame className="w-12 h-12 mx-auto mb-2 text-slate-600" />
            <p className="text-sm font-semibold">No Active Blood Feuds Recorded</p>
            <p className="text-xs mt-1">Inter-family and inter-clan relations are currently peaceful or resolved.</p>
          </div>
        )}

        {pagedData?.content?.map((feud: FeudSummaryDto) => (
          <div
            key={feud.feudId}
            onClick={() => openFeudDetail(feud.feudId)}
            className="p-4 rounded-xl bg-slate-900/60 hover:bg-slate-800/70 border border-slate-800 hover:border-amber-700/60 transition cursor-pointer flex flex-col gap-2.5 group"
          >
            {/* Header: Parties and Escalation */}
            <div className="flex items-center justify-between gap-2 flex-wrap">
              <div className="flex items-center gap-2">
                <span className="font-mono text-xs font-bold text-slate-400">Feud #{feud.feudId}</span>
                <span className="text-slate-600">•</span>
                <span className="text-sm font-bold text-slate-100 group-hover:text-amber-300 transition">
                  {feud.initiatorPartyId} <span className="text-rose-400">vs</span> {feud.targetPartyId}
                </span>
              </div>

              <div className="flex items-center gap-2">
                {getEscalationBadge(feud.escalationLevel)}
                <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase bg-slate-800 text-slate-300 border border-slate-700">
                  {feud.status}
                </span>
              </div>
            </div>

            {/* Footer */}
            <div className="flex items-center justify-between pt-1 border-t border-slate-800/40 text-xs text-slate-400 flex-wrap gap-2">
              <div className="flex items-center gap-4 font-mono">
                <span className="flex items-center gap-1">
                  <Calendar className="w-3.5 h-3.5 text-slate-500" />
                  Origin: Year {feud.startingYear} (Active through Year {feud.lastActionYear})
                </span>
                <span>•</span>
                <span className="text-amber-300 font-semibold">{feud.actionsCount} Hostile Actions Logged</span>
              </div>

              <span className="text-amber-400 text-xs font-semibold flex items-center gap-1 group-hover:translate-x-1 transition">
                <span>Feud Docket</span>
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
            feuds)
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

