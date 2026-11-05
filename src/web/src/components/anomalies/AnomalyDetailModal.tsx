import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { fetchAnomalyDetail, fetchAnomalyContests } from '../../services/api';
import { useMapStore } from '../../store/useMapStore';
import {
  X,
  Sparkles,
  MapPin,
  Shield,
  Swords,
  Calendar,
  Navigation,
  Trophy,
  ExternalLink,
  Layers,
} from 'lucide-react';

export const AnomalyDetailModal: React.FC = () => {
  const selectedAnomalyId = useMapStore((s) => s.selectedAnomalyId);
  const closeAnomalyDetail = useMapStore((s) => s.closeAnomalyDetail);
  const openBattleDetail = useMapStore((s) => s.openBattleDetail);
  const navigateToEntityOnMap = useMapStore((s) => s.navigateToEntityOnMap);

  const { data: anomaly, isLoading: anomLoading } = useQuery({
    queryKey: ['anomaly-detail', selectedAnomalyId],
    queryFn: () => (selectedAnomalyId ? fetchAnomalyDetail(selectedAnomalyId) : null),
    enabled: !!selectedAnomalyId,
  });

  const { data: contests, isLoading: contestsLoading } = useQuery({
    queryKey: ['anomaly-contests', selectedAnomalyId],
    queryFn: () => (selectedAnomalyId ? fetchAnomalyContests(selectedAnomalyId) : null),
    enabled: !!selectedAnomalyId,
  });

  if (!selectedAnomalyId) return null;

  const reservePercent =
    anomaly && anomaly.initialQuantity > 0
      ? Math.min(100, (anomaly.remainingQuantity / anomaly.initialQuantity) * 100)
      : 0;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm select-none">
      <div className="w-full max-w-2xl bg-slate-900 border border-slate-800 rounded-2xl shadow-2xl flex flex-col overflow-hidden max-h-[88vh]">
        {/* Header */}
        <div className="p-4 border-b border-slate-800 bg-slate-950 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-xl bg-cyan-950 text-cyan-400 border border-cyan-800/60">
              <Sparkles className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-base font-bold text-slate-100">Supernatural Site Ledger</h2>
                <span className="text-xs font-mono text-slate-400">#{selectedAnomalyId}</span>
              </div>
              <p className="text-xs text-slate-400">
                Resource extraction and inter-faction territorial conflict records
              </p>
            </div>
          </div>

          <button
            onClick={closeAnomalyDetail}
            className="p-1.5 rounded-lg text-slate-400 hover:text-slate-200 hover:bg-slate-800 transition"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-5 space-y-5 scrollbar-thin">
          {anomLoading && (
            <div className="py-20 text-center text-slate-400 text-xs font-mono">
              <div className="w-8 h-8 rounded-full border-2 border-cyan-500/20 border-t-cyan-400 animate-spin mx-auto mb-3" />
              Retrieving anomaly ledger...
            </div>
          )}

          {anomaly && (
            <>
              {/* Site Overview */}
              <div className="p-4 rounded-xl bg-slate-950/70 border border-slate-800 space-y-3">
                <div className="flex items-center justify-between flex-wrap gap-2">
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-bold text-slate-100">
                      {anomaly.resourceCategory.replace('_', ' ')}
                    </span>
                    <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase bg-cyan-950 text-cyan-300 border border-cyan-800">
                      {anomaly.resourceGrade}
                    </span>
                    <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase bg-slate-800 text-slate-300 border border-slate-700">
                      {anomaly.state}
                    </span>
                  </div>

                  <button
                    onClick={() => {
                      closeAnomalyDetail();
                      navigateToEntityOnMap('ANOMALY', anomaly.anomalyId, anomaly.x, anomaly.y);
                    }}
                    className="px-2.5 py-1 rounded-lg bg-cyan-950 hover:bg-cyan-900 text-cyan-300 text-xs font-medium border border-cyan-800 flex items-center gap-1.5 transition"
                  >
                    <Navigation className="w-3 h-3" />
                    <span>View on Map</span>
                  </button>
                </div>

                {/* Reserves */}
                <div className="space-y-1.5 pt-1">
                  <div className="flex justify-between text-xs font-mono text-slate-400">
                    <span>
                      Remaining Reserves: {anomaly.remainingQuantity.toFixed(1)} / {anomaly.initialQuantity.toFixed(1)} units
                    </span>
                    <span className="text-cyan-400 font-bold">{reservePercent.toFixed(0)}%</span>
                  </div>
                  <div className="w-full bg-slate-800 h-2 rounded-full overflow-hidden">
                    <div
                      className="bg-cyan-500 h-full rounded-full transition-all"
                      style={{ width: `${reservePercent}%` }}
                    />
                  </div>
                </div>

                <div className="flex items-center justify-between text-xs text-slate-400 font-mono pt-1">
                  <span>Coordinates: ({anomaly.x}, {anomaly.y})</span>
                  <span>Revealed Year: {anomaly.revealedYear}</span>
                  {anomaly.controllingOwnerId && (
                    <span className="text-indigo-400 font-medium">Controlling Owner: {anomaly.controllingOwnerId}</span>
                  )}
                </div>
              </div>

              {/* Contests History */}
              <div className="space-y-2">
                <span className="text-xs font-bold text-slate-400 uppercase tracking-wider flex items-center gap-1.5">
                  <Swords className="w-3.5 h-3.5 text-rose-400" />
                  Site Contests & Skirmishes ({contests?.length || 0})
                </span>

                {contestsLoading && (
                  <div className="py-6 text-center text-xs text-slate-500 font-mono">Loading contests...</div>
                )}

                {contests && contests.length === 0 && (
                  <div className="p-4 rounded-xl bg-slate-950 border border-slate-800 text-center text-xs text-slate-500">
                    No contested skirmishes or hostile claim disputes recorded at this site.
                  </div>
                )}

                <div className="space-y-2 max-h-60 overflow-y-auto scrollbar-thin">
                  {contests?.map((c) => (
                    <div
                      key={c.contestId}
                      className="p-3 rounded-lg bg-slate-950 border border-slate-800 flex flex-col gap-2 text-xs"
                    >
                      <div className="flex items-center justify-between gap-2 flex-wrap">
                        <div className="flex items-center gap-2">
                          <span className="font-mono text-slate-500 text-[10px]">#{c.contestId}</span>
                          <span className="text-slate-300 font-mono">
                            Year {c.year}, Month {c.month}
                          </span>
                        </div>
                        {c.winnerId ? (
                          <span className="text-emerald-400 font-medium flex items-center gap-1">
                            <Trophy className="w-3 h-3" /> Victor: {c.winnerId}
                          </span>
                        ) : (
                          <span className="text-slate-500 italic">Unresolved</span>
                        )}
                      </div>

                      <div className="flex items-center justify-between text-xs pt-1 border-t border-slate-800/40">
                        <span className="text-slate-400">
                          Contestants: <span className="text-slate-200 font-medium">{c.contestants.join(', ')}</span>
                        </span>

                        {c.battleId && (
                          <button
                            onClick={() => {
                              closeAnomalyDetail();
                              openBattleDetail(c.battleId!);
                            }}
                            className="px-2 py-0.5 rounded bg-rose-950 hover:bg-rose-900 text-rose-300 border border-rose-800 text-[11px] font-semibold flex items-center gap-1 transition"
                          >
                            <span>Inspect Battle</span>
                            <ExternalLink className="w-2.5 h-2.5" />
                          </button>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
};

