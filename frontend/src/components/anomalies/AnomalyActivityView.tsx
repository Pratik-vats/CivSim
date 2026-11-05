import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { fetchAnomalies } from '../../services/api';
import { useMapStore } from '../../store/useMapStore';
import { AnomalyDetailDto } from '../../types/api';
import {
  Sparkles,
  MapPin,
  ChevronLeft,
  ChevronRight,
  Shield,
  Layers,
  ArrowRight,
  Navigation,
  Swords,
} from 'lucide-react';

export const AnomalyActivityView: React.FC = () => {
  const [page, setPage] = useState(0);
  const openAnomalyDetail = useMapStore((s) => s.openAnomalyDetail);
  const navigateToEntityOnMap = useMapStore((s) => s.navigateToEntityOnMap);

  const { data: pagedData, isLoading, isError } = useQuery({
    queryKey: ['anomalies-list', page],
    queryFn: () => fetchAnomalies({ page, size: 20, revealedOnly: true }),
    refetchInterval: 3000,
  });

  const getGradeBadge = (grade: string) => {
    switch (grade.toUpperCase()) {
      case 'LEGENDARY':
        return (
          <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase bg-amber-950 text-amber-300 border border-amber-600 shadow-[0_0_8px_rgba(245,158,11,0.3)]">
            Legendary
          </span>
        );
      case 'EPIC':
        return (
          <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase bg-purple-950 text-purple-300 border border-purple-700">
            Epic
          </span>
        );
      case 'RARE':
        return (
          <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase bg-cyan-950 text-cyan-300 border border-cyan-700">
            Rare
          </span>
        );
      default:
        return (
          <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase bg-slate-800 text-slate-300 border border-slate-700">
            {grade}
          </span>
        );
    }
  };

  return (
    <div className="flex-1 flex flex-col h-full overflow-hidden bg-slate-950/90 text-slate-100 select-none">
      {/* Top Header */}
      <div className="p-4 border-b border-slate-800 bg-slate-900/60 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="p-2 rounded-xl bg-cyan-950/60 text-cyan-400 border border-cyan-800/60">
            <Sparkles className="w-5 h-5" />
          </div>
          <div>
            <h1 className="text-base font-bold text-slate-100">Supernatural Anomaly Conflict Ledger</h1>
            <p className="text-xs text-slate-400">
              Discovered supernatural resource nodes, extraction claims, and inter-clan territory skirmishes
            </p>
          </div>
        </div>
      </div>

      {/* Anomalies List */}
      <div className="flex-1 overflow-y-auto p-4 space-y-3 scrollbar-thin">
        {isLoading && (
          <div className="py-20 text-center text-slate-400 text-xs font-mono">
            <div className="w-8 h-8 rounded-full border-2 border-cyan-500/20 border-t-cyan-400 animate-spin mx-auto mb-3" />
            Querying revealed world anomalies...
          </div>
        )}

        {isError && (
          <div className="p-4 rounded-xl bg-rose-950/40 border border-rose-800/60 text-center text-xs text-rose-300">
            Failed to query anomaly records from simulation backend.
          </div>
        )}

        {!isLoading && pagedData?.content?.length === 0 && (
          <div className="text-center py-20 text-slate-500">
            <Sparkles className="w-12 h-12 mx-auto mb-2 text-slate-600" />
            <p className="text-sm font-semibold">No Revealed Anomalies Discovered</p>
            <p className="text-xs mt-1">Supernatural anomalies surface as civilization advances toward the Awakening Era.</p>
          </div>
        )}

        {pagedData?.content?.map((anom: AnomalyDetailDto) => {
          const reservePercent =
            anom.initialQuantity > 0 ? Math.min(100, (anom.remainingQuantity / anom.initialQuantity) * 100) : 0;

          return (
            <div
              key={anom.anomalyId}
              onClick={() => openAnomalyDetail(anom.anomalyId)}
              className="p-4 rounded-xl bg-slate-900/60 hover:bg-slate-800/70 border border-slate-800 hover:border-cyan-700/60 transition cursor-pointer flex flex-col gap-2.5 group"
            >
              {/* Header */}
              <div className="flex items-center justify-between gap-2 flex-wrap">
                <div className="flex items-center gap-2">
                  <span className="font-mono text-xs font-bold text-slate-300">#{anom.anomalyId}</span>
                  <span className="text-slate-600">•</span>
                  <span className="text-sm font-bold text-slate-100 group-hover:text-cyan-300 transition">
                    {anom.resourceCategory.replace('_', ' ')}
                  </span>
                </div>

                <div className="flex items-center gap-2">
                  {getGradeBadge(anom.resourceGrade)}
                  <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase bg-slate-800 text-slate-300 border border-slate-700">
                    {anom.state}
                  </span>
                </div>
              </div>

              {/* Resource Reserve Bar */}
              <div className="space-y-1">
                <div className="flex justify-between text-xs font-mono text-slate-400">
                  <span>
                    Reserve: {anom.remainingQuantity.toFixed(0)} / {anom.initialQuantity.toFixed(0)} units
                  </span>
                  <span className="text-cyan-400 font-bold">{reservePercent.toFixed(0)}%</span>
                </div>
                <div className="w-full bg-slate-800 h-1.5 rounded-full overflow-hidden">
                  <div
                    className="bg-cyan-500 h-full rounded-full transition-all"
                    style={{ width: `${reservePercent}%` }}
                  />
                </div>
              </div>

              {/* Footer */}
              <div className="flex items-center justify-between pt-1 border-t border-slate-800/40 text-xs text-slate-400 flex-wrap gap-2">
                <div className="flex items-center gap-3">
                  <span className="font-mono">
                    Coord: ({anom.x}, {anom.y})
                  </span>
                  {anom.controllingOwnerId && (
                    <span className="text-indigo-400 flex items-center gap-1">
                      <Shield className="w-3 h-3" />
                      Claimed by: {anom.controllingOwnerId}
                    </span>
                  )}
                  {anom.contestsCount > 0 && (
                    <span className="text-rose-400 font-semibold flex items-center gap-1">
                      <Swords className="w-3 h-3" />
                      {anom.contestsCount} Contests
                    </span>
                  )}
                </div>

                <span className="text-cyan-400 text-xs font-semibold flex items-center gap-1 group-hover:translate-x-1 transition">
                  <span>Site Ledger</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </span>
              </div>
            </div>
          );
        })}
      </div>

      {/* Pagination Footer */}
      {pagedData && pagedData.totalPages > 1 && (
        <div className="p-3 border-t border-slate-800 bg-slate-900/70 flex items-center justify-between text-xs text-slate-400">
          <div>
            Page <span className="font-semibold text-slate-200">{pagedData.page + 1}</span> of{' '}
            <span className="font-semibold text-slate-200">{pagedData.totalPages}</span> ({pagedData.totalElements}{' '}
            anomalies)
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

