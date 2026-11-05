import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { fetchHistoricalDateComparison } from '../../services/api';
import { useMapStore } from '../../store/useMapStore';
import { X, Calendar, ArrowRight, TrendingUp, TrendingDown, Users, Building2, Shield, Swords, AlertCircle } from 'lucide-react';

export const HistoricalComparisonModal: React.FC = () => {
  const isOpen = useMapStore((s) => s.historicalCompareModalOpen);
  const close = useMapStore((s) => s.closeHistoricalCompare);
  const compareDates = useMapStore((s) => s.compareDates);

  const [year1, setYear1] = useState<number>(compareDates?.year1 ?? 1);
  const [year2, setYear2] = useState<number>(compareDates?.year2 ?? 50);

  // Synchronize initial dates if opened via store
  React.useEffect(() => {
    if (compareDates) {
      setYear1(compareDates.year1);
      setYear2(compareDates.year2);
    }
  }, [compareDates]);

  const { data: comp, isLoading, error } = useQuery({
    queryKey: ['historical-date-comparison', year1, year2],
    queryFn: () => fetchHistoricalDateComparison(year1, year2),
    enabled: isOpen && year1 >= 0 && year2 >= 0,
  });

  if (!isOpen) return null;

  const isGrowth = (comp?.populationDelta ?? 0) >= 0;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 backdrop-blur-sm p-4 animate-in fade-in duration-200 select-none">
      <div className="w-full max-w-3xl bg-slate-950 border border-slate-700/80 rounded-2xl shadow-2xl overflow-hidden flex flex-col max-h-[90vh]">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-800 bg-slate-900/50">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-purple-950/60 border border-purple-800/50 text-purple-400">
              <Calendar className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-base font-bold text-slate-100 flex items-center gap-2">
                Historical Epoch Comparison
              </h2>
              <p className="text-xs text-slate-400">Compare world state across two simulation historical years</p>
            </div>
          </div>
          <button onClick={close} className="text-slate-400 hover:text-slate-200 p-1.5 rounded-lg hover:bg-slate-800 transition">
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Date Selectors */}
        <div className="p-6 border-b border-slate-800/80 bg-slate-900/20">
          <div className="flex flex-wrap items-center justify-between gap-4">
            <div className="flex items-center gap-2">
              <label className="text-xs font-semibold text-slate-400">Base Year:</label>
              <input
                type="number"
                min={0}
                max={500}
                value={year1}
                onChange={(e) => setYear1(Math.max(0, parseInt(e.target.value) || 0))}
                className="w-24 px-3 py-1.5 bg-slate-900 border border-slate-700 rounded-lg text-sm font-mono text-cyan-300 font-bold focus:outline-none focus:border-cyan-500"
              />
            </div>

            <ArrowRight className="w-5 h-5 text-slate-500 hidden sm:block" />

            <div className="flex items-center gap-2">
              <label className="text-xs font-semibold text-slate-400">Comparison Year:</label>
              <input
                type="number"
                min={0}
                max={500}
                value={year2}
                onChange={(e) => setYear2(Math.max(0, parseInt(e.target.value) || 0))}
                className="w-24 px-3 py-1.5 bg-slate-900 border border-slate-700 rounded-lg text-sm font-mono text-purple-300 font-bold focus:outline-none focus:border-purple-500"
              />
            </div>

            <div className="flex items-center gap-2">
              <span className="text-xs text-slate-500 font-mono">
                Timespan: {Math.abs(year2 - year1)} years
              </span>
            </div>
          </div>
        </div>

        {/* Body Content */}
        <div className="p-6 overflow-y-auto space-y-6 flex-1 text-xs">
          {isLoading && (
            <div className="py-12 flex flex-col items-center justify-center text-slate-400 font-mono">
              <div className="w-8 h-8 rounded-full border-2 border-cyan-500/20 border-t-cyan-400 animate-spin mb-3" />
              <span>Querying historical epochs...</span>
            </div>
          )}

          {error && (
            <div className="p-4 bg-rose-950/40 border border-rose-800/60 rounded-xl text-rose-300 flex items-center gap-3">
              <AlertCircle className="w-5 h-5 shrink-0" />
              <span>Failed to fetch historical comparison data for these years.</span>
            </div>
          )}

          {comp && !isLoading && (
            <>
              {/* Resolution Badges */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="bg-slate-900/60 p-3.5 rounded-xl border border-slate-800">
                  <div className="text-[11px] text-slate-400 font-mono">Year {comp.year1} Resolution</div>
                  <div className="text-cyan-400 font-semibold mt-1 flex items-center gap-1.5">
                    <span className="w-2 h-2 rounded-full bg-cyan-400" />
                    {comp.resolutionYear1}
                  </div>
                </div>
                <div className="bg-slate-900/60 p-3.5 rounded-xl border border-slate-800">
                  <div className="text-[11px] text-slate-400 font-mono">Year {comp.year2} Resolution</div>
                  <div className="text-purple-400 font-semibold mt-1 flex items-center gap-1.5">
                    <span className="w-2 h-2 rounded-full bg-purple-400" />
                    {comp.resolutionYear2}
                  </div>
                </div>
              </div>

              {/* Side-by-Side Metric Cards */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {/* Human Population */}
                <div className="bg-slate-900/80 rounded-xl p-4 border border-slate-800 space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="text-slate-400 flex items-center gap-1.5 font-medium">
                      <Users className="w-4 h-4 text-emerald-400" /> Human Population
                    </span>
                    <span
                      className={`px-2 py-0.5 rounded text-[11px] font-mono font-bold flex items-center gap-1 ${
                        isGrowth ? 'bg-emerald-950/80 text-emerald-400 border border-emerald-800/50' : 'bg-rose-950/80 text-rose-400 border border-rose-800/50'
                      }`}
                    >
                      {isGrowth ? <TrendingUp className="w-3.5 h-3.5" /> : <TrendingDown className="w-3.5 h-3.5" />}
                      {isGrowth ? '+' : ''}
                      {comp.populationDelta} ({comp.populationGrowthPercent.toFixed(1)}%)
                    </span>
                  </div>

                  <div className="grid grid-cols-2 gap-3 pt-2 border-t border-slate-800 font-mono">
                    <div>
                      <div className="text-[10px] text-slate-500">Year {comp.year1}</div>
                      <div className="text-base font-bold text-slate-200">{comp.population1}</div>
                    </div>
                    <div>
                      <div className="text-[10px] text-slate-500">Year {comp.year2}</div>
                      <div className="text-base font-bold text-purple-300">{comp.population2}</div>
                    </div>
                  </div>
                </div>

                {/* Settlements Count */}
                <div className="bg-slate-900/80 rounded-xl p-4 border border-slate-800 space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="text-slate-400 flex items-center gap-1.5 font-medium">
                      <Building2 className="w-4 h-4 text-sky-400" /> Active Settlements
                    </span>
                    <span className="px-2 py-0.5 rounded text-[11px] font-mono font-bold bg-slate-800 text-sky-300 border border-slate-700">
                      Delta: {comp.settlementsDelta >= 0 ? '+' : ''}{comp.settlementsDelta}
                    </span>
                  </div>

                  <div className="grid grid-cols-2 gap-3 pt-2 border-t border-slate-800 font-mono">
                    <div>
                      <div className="text-[10px] text-slate-500">Year {comp.year1}</div>
                      <div className="text-base font-bold text-slate-200">{comp.settlements1}</div>
                    </div>
                    <div>
                      <div className="text-[10px] text-slate-500">Year {comp.year2}</div>
                      <div className="text-base font-bold text-purple-300">{comp.settlements2}</div>
                    </div>
                  </div>
                </div>

                {/* Clans & Organizations */}
                <div className="bg-slate-900/80 rounded-xl p-4 border border-slate-800 space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="text-slate-400 flex items-center gap-1.5 font-medium">
                      <Shield className="w-4 h-4 text-amber-400" /> Clans & Organizations
                    </span>
                    <span className="px-2 py-0.5 rounded text-[11px] font-mono font-bold bg-slate-800 text-amber-300 border border-slate-700">
                      Delta: {comp.clansDelta >= 0 ? '+' : ''}{comp.clansDelta}
                    </span>
                  </div>

                  <div className="grid grid-cols-2 gap-3 pt-2 border-t border-slate-800 font-mono">
                    <div>
                      <div className="text-[10px] text-slate-500">Year {comp.year1}</div>
                      <div className="text-base font-bold text-slate-200">{comp.clans1}</div>
                    </div>
                    <div>
                      <div className="text-[10px] text-slate-500">Year {comp.year2}</div>
                      <div className="text-base font-bold text-purple-300">{comp.clans2}</div>
                    </div>
                  </div>
                </div>

                {/* Military Battles */}
                <div className="bg-slate-900/80 rounded-xl p-4 border border-slate-800 space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="text-slate-400 flex items-center gap-1.5 font-medium">
                      <Swords className="w-4 h-4 text-rose-400" /> Warfare Engagements
                    </span>
                    <span className="px-2 py-0.5 rounded text-[11px] font-mono font-bold bg-slate-800 text-rose-300 border border-slate-700">
                      In Period: {comp.battlesInPeriod}
                    </span>
                  </div>

                  <div className="grid grid-cols-2 gap-3 pt-2 border-t border-slate-800 font-mono">
                    <div>
                      <div className="text-[10px] text-slate-500">Cumulative Y{comp.year1}</div>
                      <div className="text-base font-bold text-slate-200">{comp.totalBattles1}</div>
                    </div>
                    <div>
                      <div className="text-[10px] text-slate-500">Cumulative Y{comp.year2}</div>
                      <div className="text-base font-bold text-purple-300">{comp.totalBattles2}</div>
                    </div>
                  </div>
                </div>
              </div>

              {/* Historical Context Notice */}
              <div className="p-3 bg-purple-950/20 border border-purple-800/30 rounded-xl text-purple-300/80 text-[11px] leading-relaxed">
                Comparison aggregates data from recorded annual archives and historical snapshots. In-depth event chronicles and individual combat logs are recorded during active simulation progression.
              </div>
            </>
          )}
        </div>

        {/* Footer */}
        <div className="px-6 py-3 border-t border-slate-800 bg-slate-900/40 flex justify-end">
          <button
            onClick={close}
            className="px-4 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-medium transition"
          >
            Close Comparison
          </button>
        </div>
      </div>
    </div>
  );
};

