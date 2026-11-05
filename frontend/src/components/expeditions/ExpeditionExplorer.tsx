import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { fetchExpeditions } from '../../services/api';
import { useMapStore } from '../../store/useMapStore';
import { ExpeditionSummaryDto } from '../../types/api';
import {
  Compass,
  Calendar,
  ChevronLeft,
  ChevronRight,
  Package,
  Users,
  MapPin,
  ArrowRight,
  Shield,
} from 'lucide-react';

const EXPEDITION_STATUSES = [
  { id: 'ALL', label: 'All Statuses' },
  { id: 'PREPARING', label: 'Preparing' },
  { id: 'EN_ROUTE', label: 'En Route' },
  { id: 'EXPLORING', label: 'Exploring' },
  { id: 'RETURNING', label: 'Returning' },
  { id: 'COMPLETED', label: 'Completed' },
  { id: 'LOST', label: 'Lost' },
];

export const ExpeditionExplorer: React.FC = () => {
  const [page, setPage] = useState(0);
  const [selectedStatus, setSelectedStatus] = useState('ALL');
  const openExpeditionDetail = useMapStore((s) => s.openExpeditionDetail);

  const { data: pagedData, isLoading, isError } = useQuery({
    queryKey: ['expeditions', page, selectedStatus],
    queryFn: () =>
      fetchExpeditions({
        page,
        size: 20,
        status: selectedStatus === 'ALL' ? undefined : selectedStatus,
      }),
    refetchInterval: 3000,
  });

  const getStatusBadge = (status: string) => {
    switch (status.toUpperCase()) {
      case 'EN_ROUTE':
        return (
          <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase bg-emerald-950/80 text-emerald-300 border border-emerald-700/60 animate-pulse">
            En Route
          </span>
        );
      case 'EXPLORING':
        return (
          <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase bg-cyan-950/80 text-cyan-300 border border-cyan-700/60">
            Exploring
          </span>
        );
      case 'RETURNING':
        return (
          <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase bg-amber-950/80 text-amber-300 border border-amber-700/60">
            Returning
          </span>
        );
      case 'COMPLETED':
        return (
          <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase bg-slate-800 text-slate-300 border border-slate-700">
            Completed
          </span>
        );
      case 'LOST':
        return (
          <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase bg-rose-950/80 text-rose-300 border border-rose-700/60">
            Lost
          </span>
        );
      default:
        return (
          <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase bg-slate-800 text-slate-300">
            {status}
          </span>
        );
    }
  };

  return (
    <div className="flex-1 flex flex-col h-full overflow-hidden bg-slate-950/90 text-slate-100 select-none">
      {/* Top Header */}
      <div className="p-4 border-b border-slate-800 bg-slate-900/60 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="p-2 rounded-xl bg-emerald-950/60 text-emerald-400 border border-emerald-800/60">
            <Compass className="w-5 h-5" />
          </div>
          <div>
            <h1 className="text-base font-bold text-slate-100">Expedition Explorer</h1>
            <p className="text-xs text-slate-400">
              Wilderness survey ventures, anomaly extraction missions, and cargo transport logistics
            </p>
          </div>
        </div>
      </div>

      {/* Filter Bar */}
      <div className="px-4 py-2.5 border-b border-slate-800 bg-slate-900/40 flex items-center gap-2 overflow-x-auto">
        {EXPEDITION_STATUSES.map((st) => (
          <button
            key={st.id}
            onClick={() => {
              setSelectedStatus(st.id);
              setPage(0);
            }}
            className={`px-3 py-1 rounded-lg text-xs font-medium transition border ${
              selectedStatus === st.id
                ? 'bg-emerald-950/80 text-emerald-300 border-emerald-700/70 shadow-sm'
                : 'bg-slate-900/60 text-slate-400 hover:text-slate-200 border-slate-800'
            }`}
          >
            {st.label}
          </button>
        ))}
      </div>

      {/* Expeditions List */}
      <div className="flex-1 overflow-y-auto p-4 space-y-3 scrollbar-thin">
        {isLoading && (
          <div className="py-20 text-center text-slate-400 text-xs font-mono">
            <div className="w-8 h-8 rounded-full border-2 border-emerald-500/20 border-t-emerald-400 animate-spin mx-auto mb-3" />
            Scanning expedition manifests...
          </div>
        )}

        {isError && (
          <div className="p-4 rounded-xl bg-rose-950/40 border border-rose-800/60 text-center text-xs text-rose-300">
            Failed to query expeditions from simulation engine.
          </div>
        )}

        {!isLoading && pagedData?.content?.length === 0 && (
          <div className="text-center py-20 text-slate-500">
            <Compass className="w-12 h-12 mx-auto mb-2 text-slate-600" />
            <p className="text-sm font-semibold">No Expeditions Found</p>
            <p className="text-xs mt-1">Clans and settlements mobilize expeditions when anomalies are revealed.</p>
          </div>
        )}

        {pagedData?.content?.map((exp: ExpeditionSummaryDto) => (
          <div
            key={exp.expeditionId}
            onClick={() => openExpeditionDetail(exp.expeditionId)}
            className="p-4 rounded-xl bg-slate-900/60 hover:bg-slate-800/70 border border-slate-800 hover:border-emerald-700/60 transition cursor-pointer flex flex-col gap-2.5 group"
          >
            {/* Header: ID, Target, Status */}
            <div className="flex items-center justify-between gap-2 flex-wrap">
              <div className="flex items-center gap-2">
                <span className="font-mono text-xs font-bold text-slate-300">Expedition #{exp.expeditionId}</span>
                <span className="text-slate-600">•</span>
                <span className="text-sm font-bold text-slate-100 group-hover:text-emerald-300 transition">
                  Target: {exp.targetAnomalyId}
                </span>
              </div>

              <div className="flex items-center gap-2">
                {getStatusBadge(exp.status)}
                <span className="text-xs text-slate-400 font-mono">
                  Sponsor: {exp.sponsorType} #{exp.sponsorId}
                </span>
              </div>
            </div>

            {/* Metrics */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 pt-1 border-t border-slate-800/40 text-xs">
              <div className="flex items-center gap-1.5 text-slate-300">
                <Users className="w-3.5 h-3.5 text-emerald-400" />
                <span>{exp.personnelCount} Personnel</span>
              </div>

              <div className="flex items-center gap-1.5 text-slate-300">
                <Package className="w-3.5 h-3.5 text-amber-400" />
                <span>
                  Cargo: {exp.carriedCargo.toFixed(0)}/{exp.maxCapacity.toFixed(0)}
                </span>
              </div>

              <div className="flex items-center gap-1.5 text-slate-300 font-mono">
                <MapPin className="w-3.5 h-3.5 text-slate-500" />
                <span>
                  Pos: ({exp.currentX}, {exp.currentY})
                </span>
              </div>

              <div className="flex items-center justify-end text-emerald-400 font-semibold gap-1 group-hover:translate-x-1 transition">
                <span>Mission Manifest</span>
                <ArrowRight className="w-3.5 h-3.5" />
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
            expeditions)
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

