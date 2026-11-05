import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { fetchBranches, createBranch, fetchSaves } from '../../services/api';
import { useMapStore } from '../../store/useMapStore';
import { BranchSummaryDto } from '../../types/api';
import { BranchComparisonModal } from './BranchComparisonModal';
import {
  X,
  GitBranch,
  Plus,
  GitCompare,
  Shuffle,
  Calendar,
  Users,
  Building2,
  HardDrive,
  CheckCircle2,
  AlertCircle,
} from 'lucide-react';

export const BranchManagerModal: React.FC = () => {
  const isOpen = useMapStore((s) => s.branchManagerOpen);
  const setOpen = useMapStore((s) => s.setBranchManagerOpen);

  const queryClient = useQueryClient();

  const [branchName, setBranchName] = useState('');
  const [sourceCheckpointId, setSourceCheckpointId] = useState('');
  const [rngPolicy, setRngPolicy] = useState<'CONTINUE_CHECKPOINT_RNG' | 'FORK_RANDOM_STREAM'>('CONTINUE_CHECKPOINT_RNG');
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

  // Comparison sub-modal
  const [comparingBranchId, setComparingBranchId] = useState<string | null>(null);

  const { data: branches, isLoading: branchesLoading, refetch: refetchBranches } = useQuery({
    queryKey: ['branches-list'],
    queryFn: fetchBranches,
    enabled: isOpen,
  });

  const { data: saves } = useQuery({
    queryKey: ['world-saves'],
    queryFn: fetchSaves,
    enabled: isOpen,
  });

  // Default source checkpoint
  React.useEffect(() => {
    if (!sourceCheckpointId && saves && saves.length > 0) {
      setSourceCheckpointId(saves[0].saveId);
    }
  }, [saves, sourceCheckpointId]);

  const forkMutation = useMutation({
    mutationFn: () =>
      createBranch({
        sourceCheckpointId,
        branchName: branchName.trim() || `Timeline Branch ${new Date().toLocaleTimeString()}`,
        rngPolicy,
      }),
    onSuccess: (newBranch) => {
      setBranchName('');
      setFeedback({
        type: 'success',
        message: `Branch "${newBranch.branchName}" created from checkpoint ${newBranch.parentCheckpointId} at Year ${newBranch.divergenceYear}!`,
      });
      queryClient.invalidateQueries({ queryKey: ['branches-list'] });
      queryClient.invalidateQueries({ queryKey: ['world-saves'] });
    },
    onError: (err: any) => {
      setFeedback({
        type: 'error',
        message: err.message || 'Failed to create alternate timeline branch.',
      });
    },
  });

  if (!isOpen) return null;

  return (
    <>
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
                  Alternate Timelines & World Branching
                </h2>
                <p className="text-xs text-slate-400">
                  Fork independent simulation realities from validated checkpoints with controlled RNG seeds
                </p>
              </div>
            </div>
            <button
              onClick={() => setOpen(false)}
              className="text-slate-400 hover:text-slate-200 p-1.5 rounded-lg hover:bg-slate-800 transition"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Feedback */}
          {feedback && (
            <div
              className={`px-6 py-2.5 text-xs flex items-center justify-between border-b ${
                feedback.type === 'success'
                  ? 'bg-emerald-950/60 text-emerald-300 border-emerald-800/50'
                  : 'bg-rose-950/60 text-rose-300 border-rose-800/50'
              }`}
            >
              <span>{feedback.message}</span>
              <button onClick={() => setFeedback(null)} className="opacity-70 hover:opacity-100">
                Dismiss
              </button>
            </div>
          )}

          {/* Create Branch Accordion / Box */}
          <div className="p-5 border-b border-slate-800 bg-slate-900/40 space-y-3">
            <div className="text-xs font-bold text-purple-300 uppercase font-mono tracking-wider">
              Create Independent Timeline Branch
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
              {/* Parent Checkpoint Selector */}
              <div>
                <label className="text-[11px] text-slate-400 font-medium block mb-1">Source Save Checkpoint</label>
                <select
                  value={sourceCheckpointId}
                  onChange={(e) => setSourceCheckpointId(e.target.value)}
                  className="w-full bg-slate-900 border border-slate-700 rounded-lg px-2.5 py-1.5 text-xs text-slate-200 focus:outline-none focus:border-purple-500 font-mono"
                >
                  {saves?.map((s) => (
                    <option key={s.saveId} value={s.saveId}>
                      {s.worldName} (Y{s.year} M{s.month})
                    </option>
                  ))}
                  {(!saves || saves.length === 0) && <option value="">No checkpoints available</option>}
                </select>
              </div>

              {/* Branch Name Input */}
              <div>
                <label className="text-[11px] text-slate-400 font-medium block mb-1">Timeline Branch Name</label>
                <input
                  type="text"
                  placeholder="e.g. Alternate Year 50 Timeline..."
                  value={branchName}
                  onChange={(e) => setBranchName(e.target.value)}
                  className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-1.5 text-xs text-slate-200 focus:outline-none focus:border-purple-500"
                />
              </div>

              {/* RNG Policy */}
              <div>
                <label className="text-[11px] text-slate-400 font-medium block mb-1">RNG Continuation Policy</label>
                <select
                  value={rngPolicy}
                  onChange={(e) => setRngPolicy(e.target.value as any)}
                  className="w-full bg-slate-900 border border-slate-700 rounded-lg px-2.5 py-1.5 text-xs text-slate-200 focus:outline-none focus:border-purple-500 font-mono"
                >
                  <option value="CONTINUE_CHECKPOINT_RNG">Continue Exact Seed (Deterministic)</option>
                  <option value="FORK_RANDOM_STREAM">Fork New Seed (Stochastic Divergence)</option>
                </select>
              </div>
            </div>

            <div className="flex justify-end pt-1">
              <button
                onClick={() => forkMutation.mutate()}
                disabled={forkMutation.isPending || !sourceCheckpointId}
                className="flex items-center gap-1.5 px-4 py-1.5 bg-purple-700 hover:bg-purple-600 disabled:opacity-50 text-white rounded-lg text-xs font-semibold shadow transition"
              >
                <GitBranch className="w-3.5 h-3.5" />
                <span>Fork Timeline</span>
              </button>
            </div>
          </div>

          {/* Active Branches List */}
          <div className="p-6 overflow-y-auto space-y-3 flex-1 text-xs">
            <div className="flex items-center justify-between">
              <span className="font-bold text-slate-300 font-mono uppercase text-[11px]">
                Active Alternate Timelines ({branches?.length ?? 0})
              </span>
            </div>

            {branchesLoading && (
              <div className="py-12 flex flex-col items-center justify-center text-slate-400 font-mono">
                <div className="w-8 h-8 rounded-full border-2 border-purple-500/20 border-t-purple-400 animate-spin mb-3" />
                <span>Scanning timeline registry...</span>
              </div>
            )}

            {!branchesLoading && (!branches || branches.length === 0) && (
              <div className="py-12 text-center text-slate-500 font-mono">
                No active branch timelines created yet. Fork a branch from any saved checkpoint above.
              </div>
            )}

            {branches &&
              branches.map((b) => (
                <div
                  key={b.branchId}
                  className="bg-slate-900/60 border border-slate-800 hover:border-slate-700 rounded-xl p-4 transition space-y-3"
                >
                  <div className="flex flex-wrap items-start justify-between gap-2">
                    <div>
                      <div className="flex items-center gap-2">
                        <h3 className="font-bold text-sm text-slate-100">{b.branchName}</h3>
                        <span className="px-1.5 py-0.5 rounded text-[10px] font-mono bg-purple-950 text-purple-300 border border-purple-800">
                          {b.branchId}
                        </span>
                      </div>
                      <div className="flex flex-wrap items-center gap-2 mt-1 text-[11px] text-slate-400 font-mono">
                        <span className="text-purple-400 font-bold">
                          Diverged: Y{b.divergenceYear} M{b.divergenceMonth}
                        </span>
                        <span>•</span>
                        <span>Parent Save: {b.parentCheckpointId}</span>
                        <span>•</span>
                        <span className="text-slate-500">{b.rngPolicy}</span>
                      </div>
                    </div>

                    <div className="flex items-center gap-2">
                      <button
                        onClick={() => setComparingBranchId(b.branchId)}
                        className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-medium transition"
                      >
                        <GitCompare className="w-3.5 h-3.5 text-cyan-400" />
                        <span>Compare Trajectory</span>
                      </button>
                    </div>
                  </div>

                  {/* Metrics snapshot */}
                  <div className="flex items-center gap-6 py-2 px-3 rounded-lg bg-slate-950/60 border border-slate-800/60 text-[11px] font-mono text-slate-300">
                    <div className="flex items-center gap-1.5">
                      <Calendar className="w-3.5 h-3.5 text-cyan-400" />
                      <span>Current Date: Y{b.currentYear} M{b.currentMonth}</span>
                    </div>
                    <div className="flex items-center gap-1.5">
                      <Users className="w-3.5 h-3.5 text-emerald-400" />
                      <span>Population: {b.currentPopulation}</span>
                    </div>
                    <div className="flex items-center gap-1.5">
                      <Building2 className="w-3.5 h-3.5 text-sky-400" />
                      <span>Settlements: {b.currentSettlementCount}</span>
                    </div>
                  </div>
                </div>
              ))}
          </div>

          {/* Footer */}
          <div className="px-6 py-3 border-t border-slate-800 bg-slate-900/40 flex justify-between items-center">
            <span className="text-[11px] text-slate-500 font-mono">
              Parent checkpoints cannot be deleted while dependent branches exist.
            </span>
            <button
              onClick={() => setOpen(false)}
              className="px-4 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-medium transition"
            >
              Close
            </button>
          </div>
        </div>
      </div>

      {/* Comparison Modal */}
      {comparingBranchId && (
        <BranchComparisonModal
          isOpen={true}
          onClose={() => setComparingBranchId(null)}
          initialWorld1Id="main"
          initialWorld2Id={comparingBranchId}
        />
      )}
    </>
  );
};

