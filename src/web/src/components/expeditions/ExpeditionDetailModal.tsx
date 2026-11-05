import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { fetchExpeditionDetail } from '../../services/api';
import { useMapStore } from '../../store/useMapStore';
import {
  X,
  Compass,
  Calendar,
  MapPin,
  Users,
  Package,
  Navigation,
  Shield,
  ExternalLink,
  Target,
} from 'lucide-react';

export const ExpeditionDetailModal: React.FC = () => {
  const selectedExpeditionId = useMapStore((s) => s.selectedExpeditionId);
  const closeExpeditionDetail = useMapStore((s) => s.closeExpeditionDetail);
  const openPersonProfile = useMapStore((s) => s.openPersonProfile);
  const navigateToEntityOnMap = useMapStore((s) => s.navigateToEntityOnMap);

  const { data: exp, isLoading } = useQuery({
    queryKey: ['expedition-detail', selectedExpeditionId],
    queryFn: () => (selectedExpeditionId ? fetchExpeditionDetail(selectedExpeditionId) : null),
    enabled: !!selectedExpeditionId,
  });

  if (!selectedExpeditionId) return null;

  const cargoPercent = exp && exp.maxCapacity > 0 ? Math.min(100, (exp.carriedCargo / exp.maxCapacity) * 100) : 0;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm select-none">
      <div className="w-full max-w-2xl bg-slate-900 border border-slate-800 rounded-2xl shadow-2xl flex flex-col overflow-hidden max-h-[88vh]">
        {/* Header */}
        <div className="p-4 border-b border-slate-800 bg-slate-950 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-xl bg-emerald-950 text-emerald-400 border border-emerald-800/60">
              <Compass className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-base font-bold text-slate-100">Expedition Manifest</h2>
                <span className="text-xs font-mono text-slate-400">#{selectedExpeditionId}</span>
              </div>
              <p className="text-xs text-slate-400">
                Sponsor: {exp?.sponsorType} #{exp?.sponsorId}
              </p>
            </div>
          </div>

          <button
            onClick={closeExpeditionDetail}
            className="p-1.5 rounded-lg text-slate-400 hover:text-slate-200 hover:bg-slate-800 transition"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-5 space-y-5 scrollbar-thin">
          {isLoading && (
            <div className="py-20 text-center text-slate-400 text-xs font-mono">
              <div className="w-8 h-8 rounded-full border-2 border-emerald-500/20 border-t-emerald-400 animate-spin mx-auto mb-3" />
              Loading expedition manifest and telemetry...
            </div>
          )}

          {exp && (
            <>
              {/* Mission Objective */}
              <div className="p-4 rounded-xl bg-slate-950/70 border border-slate-800 space-y-2">
                <div className="flex items-center justify-between flex-wrap gap-2">
                  <div className="flex items-center gap-2">
                    <Target className="w-4 h-4 text-emerald-400" />
                    <span className="text-xs font-bold text-slate-200 uppercase tracking-wider">Mission Objective</span>
                  </div>
                  <span className="px-2 py-0.5 rounded text-xs font-bold uppercase bg-emerald-950 text-emerald-300 border border-emerald-800">
                    {exp.status}
                  </span>
                </div>

                <div className="text-sm font-semibold text-slate-100">
                  Target: {exp.anomalyName || exp.targetAnomalyId}
                </div>
                <p className="text-xs text-slate-300 leading-relaxed font-sans">{exp.claimObjective}</p>
              </div>

              {/* Spatial Routing */}
              <div className="p-4 rounded-xl bg-slate-950/50 border border-slate-800 space-y-3">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">Spatial Routing</span>
                  <button
                    onClick={() => {
                      closeExpeditionDetail();
                      navigateToEntityOnMap('ANOMALY', exp.targetAnomalyId, exp.targetX, exp.targetY);
                    }}
                    className="px-2.5 py-1 rounded-lg bg-emerald-950 hover:bg-emerald-900 text-emerald-300 text-xs font-medium border border-emerald-800 flex items-center gap-1.5 transition"
                  >
                    <Navigation className="w-3 h-3" />
                    <span>View Site on Map</span>
                  </button>
                </div>

                <div className="grid grid-cols-3 gap-2 text-xs font-mono">
                  <div className="p-2.5 rounded-lg bg-slate-900 border border-slate-800">
                    <span className="text-[10px] text-slate-500 uppercase">Origin</span>
                    <div className="text-slate-200 mt-0.5">({exp.originX}, {exp.originY})</div>
                  </div>
                  <div className="p-2.5 rounded-lg bg-slate-900 border border-slate-800">
                    <span className="text-[10px] text-slate-500 uppercase">Current</span>
                    <div className="text-emerald-400 mt-0.5 font-bold">({exp.currentX}, {exp.currentY})</div>
                  </div>
                  <div className="p-2.5 rounded-lg bg-slate-900 border border-slate-800">
                    <span className="text-[10px] text-slate-500 uppercase">Target</span>
                    <div className="text-slate-200 mt-0.5">({exp.targetX}, {exp.targetY})</div>
                  </div>
                </div>

                <div className="flex items-center justify-between text-xs text-slate-400 font-mono pt-1">
                  <span>Departure: Month {exp.departureMonth}</span>
                  <span>Arrival: Month {exp.arrivalMonth}</span>
                  <span>Return: Month {exp.returnArrivalMonth}</span>
                </div>
              </div>

              {/* Cargo & Supplies Logistics */}
              <div className="p-4 rounded-xl bg-slate-950/50 border border-slate-800 space-y-2">
                <div className="flex justify-between items-center text-xs">
                  <span className="font-bold text-slate-400 uppercase">Cargo Logistics</span>
                  <span className="font-mono text-amber-300">
                    {exp.carriedCargo.toFixed(1)} / {exp.maxCapacity.toFixed(1)} Weight Units ({cargoPercent.toFixed(0)}%)
                  </span>
                </div>
                <div className="w-full bg-slate-800 h-2 rounded-full overflow-hidden">
                  <div
                    className="bg-amber-500 h-full rounded-full transition-all"
                    style={{ width: `${cargoPercent}%` }}
                  />
                </div>
                <div className="text-xs text-slate-400 font-mono pt-1">
                  Survival Rations: <span className="text-emerald-400 font-bold">{exp.supplies} units</span>
                </div>
              </div>

              {/* Personnel Roster */}
              <div className="space-y-2">
                <span className="text-xs font-bold text-slate-400 uppercase tracking-wider flex items-center gap-1.5">
                  <Users className="w-3.5 h-3.5 text-emerald-400" />
                  Expedition Personnel ({exp.rosterPersonNames?.length || 0})
                </span>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 max-h-48 overflow-y-auto scrollbar-thin">
                  {exp.rosterPersonNames?.map((name, idx) => {
                    const personId = exp.rosterPersonIds?.[idx];
                    return (
                      <div
                        key={idx}
                        className="p-2.5 rounded-lg bg-slate-950 border border-slate-800 flex items-center justify-between text-xs"
                      >
                        <span className="font-medium text-slate-200">{name}</span>
                        {personId && (
                          <button
                            onClick={() => {
                              closeExpeditionDetail();
                              openPersonProfile(Number(personId));
                            }}
                            className="text-cyan-400 hover:text-cyan-300 flex items-center gap-1 text-[11px] font-mono transition"
                          >
                            <span>#{personId}</span>
                            <ExternalLink className="w-2.5 h-2.5" />
                          </button>
                        )}
                      </div>
                    );
                  })}
                </div>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
};

