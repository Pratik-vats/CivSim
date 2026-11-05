import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  fetchSaves,
  createSave,
  restoreSave,
  validateSave,
  deleteSave,
  renameSave,
} from '../../services/api';
import { useMapStore } from '../../store/useMapStore';
import { WorldSaveDetailDto } from '../../types/api';
import {
  X,
  Save,
  CheckCircle,
  AlertTriangle,
  RotateCcw,
  ShieldCheck,
  Trash2,
  Edit2,
  GitBranch,
  Plus,
  RefreshCw,
  HardDrive,
  Users,
  Building2,
} from 'lucide-react';

export const SaveManagerModal: React.FC = () => {
  const isOpen = useMapStore((s) => s.saveManagerOpen);
  const setOpen = useMapStore((s) => s.setSaveManagerOpen);
  const setBranchManagerOpen = useMapStore((s) => s.setBranchManagerOpen);
  const setLiveMode = useMapStore((s) => s.setLiveMode);

  const queryClient = useQueryClient();

  const [newSaveName, setNewSaveName] = useState('');
  const [editingSaveId, setEditingSaveId] = useState<string | null>(null);
  const [renameValue, setRenameValue] = useState('');
  const [feedbackMessage, setFeedbackMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
  const [validationResult, setValidationResult] = useState<{ saveId: string; valid: boolean; message: string } | null>(null);

  const { data: saves, isLoading, refetch } = useQuery({
    queryKey: ['world-saves'],
    queryFn: fetchSaves,
    enabled: isOpen,
  });

  const createMutation = useMutation({
    mutationFn: (name: string) => createSave(undefined, name.trim() || undefined),
    onSuccess: (data) => {
      setNewSaveName('');
      setFeedbackMessage({ type: 'success', text: `Save checkpoint created successfully: ${data.worldName} (Year ${data.year})` });
      queryClient.invalidateQueries({ queryKey: ['world-saves'] });
      queryClient.invalidateQueries({ queryKey: ['historical-timeline'] });
    },
    onError: (err: any) => {
      setFeedbackMessage({ type: 'error', text: err.message || 'Failed to create save checkpoint.' });
    },
  });

  const restoreMutation = useMutation({
    mutationFn: (saveId: string) => restoreSave(saveId),
    onSuccess: () => {
      setLiveMode();
      setFeedbackMessage({ type: 'success', text: 'World state restored successfully. Simulation running from checkpoint.' });
      queryClient.invalidateQueries();
    },
    onError: (err: any) => {
      setFeedbackMessage({ type: 'error', text: err.message || 'Failed to restore world save.' });
    },
  });

  const validateMutation = useMutation({
    mutationFn: (saveId: string) => validateSave(saveId),
    onSuccess: (res) => {
      setValidationResult(res);
      refetch();
    },
    onError: (err: any) => {
      setFeedbackMessage({ type: 'error', text: err.message || 'Integrity validation failed.' });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (saveId: string) => deleteSave(saveId),
    onSuccess: () => {
      setFeedbackMessage({ type: 'success', text: 'Save checkpoint deleted.' });
      queryClient.invalidateQueries({ queryKey: ['world-saves'] });
      queryClient.invalidateQueries({ queryKey: ['historical-timeline'] });
    },
    onError: (err: any) => {
      setFeedbackMessage({ type: 'error', text: err.message || 'Could not delete save (it may have active branch dependents).' });
    },
  });

  const renameMutation = useMutation({
    mutationFn: ({ saveId, newName }: { saveId: string; newName: string }) => renameSave(saveId, newName),
    onSuccess: () => {
      setEditingSaveId(null);
      setRenameValue('');
      setFeedbackMessage({ type: 'success', text: 'Save renamed successfully.' });
      queryClient.invalidateQueries({ queryKey: ['world-saves'] });
    },
    onError: (err: any) => {
      setFeedbackMessage({ type: 'error', text: err.message || 'Failed to rename save.' });
    },
  });

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 backdrop-blur-sm p-4 animate-in fade-in duration-200 select-none">
      <div className="w-full max-w-4xl bg-slate-950 border border-slate-700/80 rounded-2xl shadow-2xl overflow-hidden flex flex-col max-h-[90vh]">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-800 bg-slate-900/50">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-cyan-950/60 border border-cyan-800/50 text-cyan-400">
              <HardDrive className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-base font-bold text-slate-100 flex items-center gap-2">
                World Checkpoint & Save Manager
              </h2>
              <p className="text-xs text-slate-400">Atomic checkpoints, deterministic restoration, and SHA-256 integrity</p>
            </div>
          </div>
          <button onClick={() => setOpen(false)} className="text-slate-400 hover:text-slate-200 p-1.5 rounded-lg hover:bg-slate-800 transition">
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Feedback Banner */}
        {feedbackMessage && (
          <div
            className={`px-6 py-2.5 text-xs flex items-center justify-between border-b ${
              feedbackMessage.type === 'success'
                ? 'bg-emerald-950/60 text-emerald-300 border-emerald-800/50'
                : 'bg-rose-950/60 text-rose-300 border-rose-800/50'
            }`}
          >
            <span>{feedbackMessage.text}</span>
            <button onClick={() => setFeedbackMessage(null)} className="text-xs opacity-70 hover:opacity-100">
              Dismiss
            </button>
          </div>
        )}

        {/* Create Manual Save Strip */}
        <div className="p-4 px-6 border-b border-slate-800 bg-slate-900/30 flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-2 flex-1 max-w-md">
            <input
              type="text"
              placeholder="Enter manual checkpoint title (optional)..."
              value={newSaveName}
              onChange={(e) => setNewSaveName(e.target.value)}
              className="flex-1 px-3 py-1.5 bg-slate-900 border border-slate-700 rounded-lg text-xs text-slate-200 focus:outline-none focus:border-cyan-500"
            />
            <button
              onClick={() => createMutation.mutate(newSaveName)}
              disabled={createMutation.isPending}
              className="flex items-center gap-1.5 px-3 py-1.5 bg-cyan-600 hover:bg-cyan-500 disabled:opacity-50 text-white rounded-lg text-xs font-semibold shadow transition shrink-0"
            >
              <Plus className="w-3.5 h-3.5" />
              <span>Create Save</span>
            </button>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={() => refetch()}
              className="p-1.5 bg-slate-900 hover:bg-slate-800 border border-slate-700 rounded-lg text-slate-300 transition"
              title="Refresh saves"
            >
              <RefreshCw className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>

        {/* Saves List */}
        <div className="p-6 overflow-y-auto space-y-3 flex-1 text-xs">
          {isLoading && (
            <div className="py-12 flex flex-col items-center justify-center text-slate-400 font-mono">
              <div className="w-8 h-8 rounded-full border-2 border-cyan-500/20 border-t-cyan-400 animate-spin mb-3" />
              <span>Loading saved checkpoints...</span>
            </div>
          )}

          {!isLoading && (!saves || saves.length === 0) && (
            <div className="py-12 text-center text-slate-500 font-mono">
              No saved checkpoints found. Create a manual save checkpoint above to preserve current world state.
            </div>
          )}

          {saves &&
            saves.map((save) => {
              const isEditing = editingSaveId === save.saveId;
              const formattedDate = new Date(save.saveTimestamp).toLocaleString();
              const sizeKb = (save.fileSizeBytes / 1024).toFixed(1);

              return (
                <div
                  key={save.saveId}
                  className="bg-slate-900/60 border border-slate-800 hover:border-slate-700/80 rounded-xl p-4 transition space-y-3"
                >
                  <div className="flex flex-wrap items-start justify-between gap-2">
                    <div>
                      {isEditing ? (
                        <div className="flex items-center gap-2">
                          <input
                            type="text"
                            value={renameValue}
                            onChange={(e) => setRenameValue(e.target.value)}
                            className="px-2.5 py-1 bg-slate-950 border border-cyan-500 rounded text-xs text-white"
                          />
                          <button
                            onClick={() => renameMutation.mutate({ saveId: save.saveId, newName: renameValue })}
                            className="px-2 py-1 bg-cyan-600 text-white rounded text-[11px] font-semibold"
                          >
                            Save
                          </button>
                          <button
                            onClick={() => setEditingSaveId(null)}
                            className="px-2 py-1 bg-slate-800 text-slate-300 rounded text-[11px]"
                          >
                            Cancel
                          </button>
                        </div>
                      ) : (
                        <div className="flex items-center gap-2">
                          <h3 className="font-bold text-sm text-slate-100">{save.worldName}</h3>
                          <button
                            onClick={() => {
                              setEditingSaveId(save.saveId);
                              setRenameValue(save.worldName);
                            }}
                            className="text-slate-500 hover:text-slate-300"
                            title="Rename"
                          >
                            <Edit2 className="w-3.5 h-3.5" />
                          </button>
                          {save.isBranch && (
                            <span className="px-1.5 py-0.5 rounded text-[10px] font-mono bg-purple-950 text-purple-300 border border-purple-800">
                              BRANCH
                            </span>
                          )}
                        </div>
                      )}

                      <div className="flex flex-wrap items-center gap-2 mt-1 text-[11px] text-slate-400 font-mono">
                        <span className="text-cyan-400 font-bold">Year {save.year}, Month {save.month}</span>
                        <span>•</span>
                        <span>{formattedDate}</span>
                        <span>•</span>
                        <span>{sizeKb} KB</span>
                        <span>•</span>
                        <span className="text-slate-500">v{save.schemaVersion}</span>
                      </div>
                    </div>

                    {/* SHA-256 Checksum Status Badge */}
                    <div className="flex items-center gap-2">
                      <span
                        className={`px-2 py-0.5 rounded text-[10px] font-mono flex items-center gap-1 border ${
                          save.isValidChecksum
                            ? 'bg-emerald-950/60 text-emerald-400 border-emerald-800/50'
                            : 'bg-rose-950/60 text-rose-400 border-rose-800/50'
                        }`}
                        title={save.isValidChecksum ? 'Checksum matches snapshot contents' : 'Checksum mismatch or corrupt'}
                      >
                        {save.isValidChecksum ? <CheckCircle className="w-3 h-3" /> : <AlertTriangle className="w-3 h-3" />}
                        <span>{save.isValidChecksum ? 'INTEGRITY VERIFIED' : 'CHECKSUM MISMATCH'}</span>
                      </span>
                    </div>
                  </div>

                  {/* Summary Demographics Strip */}
                  <div className="flex items-center gap-6 py-2 px-3 rounded-lg bg-slate-950/60 border border-slate-800/60 text-[11px] font-mono text-slate-300">
                    <div className="flex items-center gap-1.5">
                      <Users className="w-3.5 h-3.5 text-emerald-400" />
                      <span>Living: <strong className="text-white">{save.livingPopulation}</strong></span>
                    </div>
                    <div className="flex items-center gap-1.5">
                      <Building2 className="w-3.5 h-3.5 text-sky-400" />
                      <span>Settlements: <strong className="text-white">{save.settlementCount}</strong></span>
                    </div>
                    <div className="text-slate-500 truncate max-w-xs" title={save.checksum}>
                      SHA-256: {save.checksum ? `${save.checksum.substring(0, 16)}...` : 'N/A'}
                    </div>
                  </div>

                  {/* Action Buttons */}
                  <div className="flex flex-wrap items-center justify-between gap-2 pt-1">
                    <div className="flex items-center gap-2">
                      {/* Restore */}
                      <button
                        onClick={() => {
                          if (confirm(`Restore simulation world to "${save.worldName}" (Year ${save.year})?`)) {
                            restoreMutation.mutate(save.saveId);
                          }
                        }}
                        disabled={restoreMutation.isPending}
                        className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-cyan-700 hover:bg-cyan-600 disabled:opacity-50 text-white font-semibold text-xs transition shadow"
                      >
                        <RotateCcw className="w-3.5 h-3.5" />
                        <span>Restore Checkpoint</span>
                      </button>

                      {/* Fork Branch */}
                      <button
                        onClick={() => {
                          setOpen(false);
                          setBranchManagerOpen(true);
                        }}
                        className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-purple-900/60 hover:bg-purple-800/80 border border-purple-700/60 text-purple-200 font-semibold text-xs transition"
                        title="Fork an independent alternate timeline from this checkpoint"
                      >
                        <GitBranch className="w-3.5 h-3.5" />
                        <span>Fork Branch</span>
                      </button>
                    </div>

                    <div className="flex items-center gap-2">
                      {/* Validate Integrity */}
                      <button
                        onClick={() => validateMutation.mutate(save.saveId)}
                        disabled={validateMutation.isPending}
                        className="flex items-center gap-1 px-2.5 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs transition"
                        title="Run SHA-256 validation check"
                      >
                        <ShieldCheck className="w-3.5 h-3.5 text-emerald-400" />
                        <span>Validate</span>
                      </button>

                      {/* Delete */}
                      <button
                        onClick={() => {
                          if (confirm(`Are you sure you want to delete checkpoint "${save.worldName}"?`)) {
                            deleteMutation.mutate(save.saveId);
                          }
                        }}
                        disabled={deleteMutation.isPending}
                        className="p-1.5 rounded-lg bg-slate-800 hover:bg-rose-950 hover:text-rose-400 text-slate-400 transition"
                        title="Delete Checkpoint"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>
                </div>
              );
            })}
        </div>

        {/* Footer */}
        <div className="px-6 py-3 border-t border-slate-800 bg-slate-900/40 flex justify-between items-center">
          <span className="text-[11px] text-slate-500 font-mono">
            Checkpoints preserve entire world state including couple scheduling & RNG seeds.
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
  );
};

