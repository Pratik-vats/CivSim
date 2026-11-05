import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { compareBranches, fetchBranches } from '../../services/api';
import {
  X,
  GitBranch,
  TrendingUp,
  AlertCircle,
  Calendar,
  Users,
  Building2,
  Swords,
} from 'lucide-react';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  CartesianGrid,
  Legend,
  ReferenceLine,
} from 'recharts';

interface BranchComparisonModalProps {
  isOpen: boolean;
  onClose: () => void;
  initialWorld1Id?: string;
  initialWorld2Id?: string;
}

export const BranchComparisonModal: React.FC<BranchComparisonModalProps> = ({
  isOpen,
  onClose,
  initialWorld1Id = 'main',
  initialWorld2Id,
}) => {
  const [world1Id, setWorld1Id] = useState<string>(initialWorld1Id);
  const [world2Id, setWorld2Id] = useState<string>(initialWorld2Id || '');
  const [activeMetric, setActiveMetric] = useState<'population' | 'settlements' | 'battles'>('population');

  const { data: branchList } = useQuery({
    queryKey: ['branches-list'],
    queryFn: fetchBranches,
    enabled: isOpen,
  });

  // If initialWorld2Id was not set, default to first available branch if present
  React.useEffect(() => {
    if (!world2Id && branchList && branchList.length > 0) {
      setWorld2Id(branchList[0].branchId);
    }
  }, [branchList, world2Id]);

  const { data: comparison, isLoading, error } = useQuery({
    queryKey: ['branch-comparison', world1Id, world2Id],
    queryFn: () => compareBranches(world1Id, world2Id),
    enabled: isOpen && !!world1Id && !!world2Id,
  });

  if (!isOpen) return null;

  // Prepare chart data by aligning time points across both branches
  const chartDataMap = new Map<number, { year: number; val1?: number; val2?: number }>();

  if (comparison) {
    const getValue = (pt: any) => {
      if (activeMetric === 'population') return pt.humanPopulation;
      if (activeMetric === 'settlements') return pt.settlementCount;
      return pt.cumulativeBattles;
    };

    comparison.world1Series.forEach((pt) => {
      chartDataMap.set(pt.year, { year: pt.year, val1: getValue(pt) });
    });

    comparison.world2Series.forEach((pt) => {
      const existing = chartDataMap.get(pt.year) || { year: pt.year };
      existing.val2 = getValue(pt);
      chartDataMap.set(pt.year, existing);
    });
  }

  const chartData = Array.from(chartDataMap.values()).sort((a, b) => a.year - b.year);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 backdrop-blur-sm p-4 animate-in fade-in duration-200 select-none">
      <div className="w-full max-w-4xl bg-slate-950 border border-slate-700/80 rounded-2xl shadow-2xl overflow-hidden flex flex-col max-h-[90vh]">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-800 bg-slate-900/50">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-purple-950/60 border border-purple-800/50 text-purple-400">
              <GitBranch className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-base font-bold text-slate-100 flex items-center gap-2">
                Timeline Divergence & Branch Comparison
              </h2>
              <p className="text-xs text-slate-400">Compare developmental trajectories of alternate simulation branches</p>
            </div>
          </div>
          <button onClick={onClose} className="text-slate-400 hover:text-slate-200 p-1.5 rounded-lg hover:bg-slate-800 transition">
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Branch Selectors & Metric Tabs */}
        <div className="p-4 px-6 border-b border-slate-800 bg-slate-900/30 flex flex-wrap items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="flex items-center gap-2">
              <label className="text-xs font-semibold text-cyan-400 font-mono">World A:</label>
              <select
                value={world1Id}
                onChange={(e) => setWorld1Id(e.target.value)}
                className="bg-slate-900 border border-slate-700 rounded-lg px-2.5 py-1 text-xs text-slate-200 focus:outline-none focus:border-cyan-500 font-mono"
              >
                <option value="main">Main Simulation</option>
                {branchList?.map((b) => (
                  <option key={b.branchId} value={b.branchId}>
                    {b.branchName} ({b.branchId})
                  </option>
                ))}
              </select>
            </div>

            <span className="text-slate-500 font-mono">vs</span>

            <div className="flex items-center gap-2">
              <label className="text-xs font-semibold text-purple-400 font-mono">World B:</label>
              <select
                value={world2Id}
                onChange={(e) => setWorld2Id(e.target.value)}
                className="bg-slate-900 border border-slate-700 rounded-lg px-2.5 py-1 text-xs text-slate-200 focus:outline-none focus:border-purple-500 font-mono"
              >
                <option value="">Select Branch...</option>
                <option value="main">Main Simulation</option>
                {branchList?.map((b) => (
                  <option key={b.branchId} value={b.branchId}>
                    {b.branchName} ({b.branchId})
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* Metric Selector Buttons */}
          <div className="flex items-center bg-slate-900 border border-slate-800 rounded-lg p-0.5 text-xs">
            <button
              onClick={() => setActiveMetric('population')}
              className={`px-3 py-1 rounded transition flex items-center gap-1.5 ${
                activeMetric === 'population' ? 'bg-cyan-600 text-white font-semibold' : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <Users className="w-3.5 h-3.5" />
              <span>Population</span>
            </button>
            <button
              onClick={() => setActiveMetric('settlements')}
              className={`px-3 py-1 rounded transition flex items-center gap-1.5 ${
                activeMetric === 'settlements' ? 'bg-cyan-600 text-white font-semibold' : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <Building2 className="w-3.5 h-3.5" />
              <span>Settlements</span>
            </button>
            <button
              onClick={() => setActiveMetric('battles')}
              className={`px-3 py-1 rounded transition flex items-center gap-1.5 ${
                activeMetric === 'battles' ? 'bg-cyan-600 text-white font-semibold' : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <Swords className="w-3.5 h-3.5" />
              <span>Battles</span>
            </button>
          </div>
        </div>

        {/* Chart Viewport */}
        <div className="p-6 flex-1 overflow-y-auto space-y-4">
          {isLoading && (
            <div className="py-24 flex flex-col items-center justify-center text-slate-400 font-mono">
              <div className="w-8 h-8 rounded-full border-2 border-purple-500/20 border-t-purple-400 animate-spin mb-3" />
              <span>Computing branch divergence time series...</span>
            </div>
          )}

          {error && (
            <div className="p-4 bg-rose-950/40 border border-rose-800/60 rounded-xl text-rose-300 flex items-center gap-3">
              <AlertCircle className="w-5 h-5 shrink-0" />
              <span>Could not compare branches. Ensure both branches exist and share a lineage.</span>
            </div>
          )}

          {comparison && !isLoading && (
            <>
              {/* Divergence Point Banner */}
              <div className="p-3 bg-purple-950/30 border border-purple-800/40 rounded-xl flex items-center justify-between text-xs font-mono">
                <div className="flex items-center gap-2">
                  <span className="w-2.5 h-2.5 rounded-full bg-purple-400" />
                  <span className="text-purple-300 font-bold">Divergence Checkpoint:</span>
                  <span className="text-slate-200">Year {comparison.divergenceYear}, Month {comparison.divergenceMonth}</span>
                </div>
                <div className="text-slate-400 text-[11px]">
                  Ancestor Save: <span className="text-slate-300">{comparison.commonAncestorCheckpointId || 'Original Checkpoint'}</span>
                </div>
              </div>

              {/* Time Series Recharts Graph */}
              <div className="h-72 w-full bg-slate-900/60 border border-slate-800 rounded-xl p-4">
                <ResponsiveContainer width="100%" height="100%">
                  <LineChart data={chartData} margin={{ top: 10, right: 30, left: 10, bottom: 5 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
                    <XAxis
                      dataKey="year"
                      stroke="#64748b"
                      fontSize={11}
                      tickFormatter={(val) => `Y${val}`}
                    />
                    <YAxis stroke="#64748b" fontSize={11} />
                    <Tooltip
                      contentStyle={{ backgroundColor: '#090d16', borderColor: '#334155', borderRadius: '0.75rem' }}
                      labelStyle={{ color: '#94a3b8', fontFamily: 'monospace' }}
                      labelFormatter={(label) => `Simulation Year ${label}`}
                    />
                    <Legend wrapperStyle={{ fontSize: '11px', paddingTop: '10px' }} />
                    {comparison.divergenceYear > 0 && (
                      <ReferenceLine
                        x={comparison.divergenceYear}
                        stroke="#a855f7"
                        strokeDasharray="4 4"
                        label={{
                          value: `Fork Y${comparison.divergenceYear}`,
                          fill: '#c084fc',
                          fontSize: 10,
                          position: 'top',
                        }}
                      />
                    )}
                    <Line
                      type="monotone"
                      dataKey="val1"
                      name={comparison.world1Name || 'World A'}
                      stroke="#22d3ee"
                      strokeWidth={2.5}
                      dot={false}
                    />
                    <Line
                      type="monotone"
                      dataKey="val2"
                      name={comparison.world2Name || 'World B'}
                      stroke="#c084fc"
                      strokeWidth={2.5}
                      dot={false}
                    />
                  </LineChart>
                </ResponsiveContainer>
              </div>
            </>
          )}
        </div>

        {/* Footer */}
        <div className="px-6 py-3 border-t border-slate-800 bg-slate-900/40 flex justify-end">
          <button
            onClick={onClose}
            className="px-4 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-medium transition"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};

