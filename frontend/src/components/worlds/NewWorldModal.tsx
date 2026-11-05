import React, { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { createNewWorld } from '../../services/api';
import { useMapStore } from '../../store/useMapStore';
import {
  X,
  Globe,
  Dices,
  Sparkles,
  ShieldAlert,
  Flame,
  Check,
} from 'lucide-react';

export const NewWorldModal: React.FC = () => {
  const isOpen = useMapStore((s) => s.newWorldModalOpen);
  const setOpen = useMapStore((s) => s.setNewWorldModalOpen);
  const setLiveMode = useMapStore((s) => s.setLiveMode);

  const queryClient = useQueryClient();

  const [worldName, setWorldName] = useState('New Genesis World');
  const [seed, setSeed] = useState<number>(() => Math.floor(Math.random() * 999999) + 1);
  const [fantasyMode, setFantasyMode] = useState<boolean>(true);
  const [awakeningThreshold, setAwakeningThreshold] = useState<number>(50);
  const [extinctionPolicy, setExtinctionPolicy] = useState<string>('RESPAWN_NOMADS');
  const [errorText, setErrorText] = useState<string | null>(null);

  const randomizeSeed = () => {
    setSeed(Math.floor(Math.random() * 999999) + 1);
  };

  const createWorldMutation = useMutation({
    mutationFn: () =>
      createNewWorld({
        worldName: worldName.trim() || 'New Genesis World',
        seed,
        fantasyMode,
        awakeningYearThreshold: awakeningThreshold,
        extinctionRecoveryPolicy: extinctionPolicy,
      }),
    onSuccess: () => {
      setLiveMode();
      setOpen(false);
      queryClient.invalidateQueries();
    },
    onError: (err: any) => {
      setErrorText(err.message || 'Failed to initialize new seeded world.');
    },
  });

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 backdrop-blur-sm p-4 animate-in fade-in duration-200 select-none">
      <div className="w-full max-w-lg bg-slate-950 border border-slate-700/80 rounded-2xl shadow-2xl overflow-hidden flex flex-col">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-800 bg-slate-900/50">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-cyan-950/60 border border-cyan-800/50 text-cyan-400">
              <Globe className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-base font-bold text-slate-100">Initialize New Simulation World</h2>
              <p className="text-xs text-slate-400">Deterministic seed control and epoch parameters</p>
            </div>
          </div>
          <button
            onClick={() => setOpen(false)}
            className="text-slate-400 hover:text-slate-200 p-1.5 rounded-lg hover:bg-slate-800 transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Form Body */}
        <div className="p-6 space-y-4 text-xs">
          {errorText && (
            <div className="p-3 bg-rose-950/50 border border-rose-800/60 rounded-xl text-rose-300">
              {errorText}
            </div>
          )}

          {/* World Name */}
          <div>
            <label className="text-slate-300 font-semibold block mb-1.5">World Name</label>
            <input
              type="text"
              value={worldName}
              onChange={(e) => setWorldName(e.target.value)}
              className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-xl text-slate-100 font-medium focus:outline-none focus:border-cyan-500"
            />
          </div>

          {/* Seed Input with Randomizer */}
          <div>
            <label className="text-slate-300 font-semibold block mb-1.5">RNG Seed (Deterministic)</label>
            <div className="flex items-center gap-2">
              <input
                type="number"
                value={seed}
                onChange={(e) => setSeed(parseInt(e.target.value) || 0)}
                className="flex-1 px-3 py-2 bg-slate-900 border border-slate-700 rounded-xl font-mono text-cyan-300 font-bold focus:outline-none focus:border-cyan-500"
              />
              <button
                type="button"
                onClick={randomizeSeed}
                className="flex items-center gap-1.5 px-3 py-2 bg-slate-800 hover:bg-slate-700 border border-slate-700 rounded-xl text-slate-300 font-medium transition"
                title="Generate Random Seed"
              >
                <Dices className="w-4 h-4 text-amber-400" />
                <span>Randomize</span>
              </button>
            </div>
            <p className="text-[10px] text-slate-500 mt-1 font-mono">
              The exact same seed guarantees reproducible geography, settlements, and monster spawns.
            </p>
          </div>

          {/* Fantasy Mode Toggle */}
          <div className="p-3 bg-slate-900/60 border border-slate-800 rounded-xl flex items-center justify-between">
            <div className="space-y-0.5">
              <div className="text-slate-200 font-semibold flex items-center gap-1.5">
                <Sparkles className="w-3.5 h-3.5 text-purple-400" /> Fantasy & Supernatural Systems
              </div>
              <div className="text-[11px] text-slate-400">
                Enables anomaly spawning, awakening progression, monster tides, and legendary heirlooms.
              </div>
            </div>
            <button
              type="button"
              onClick={() => setFantasyMode(!fantasyMode)}
              className={`w-12 h-6 rounded-full transition-colors relative flex items-center px-1 ${
                fantasyMode ? 'bg-cyan-600' : 'bg-slate-800'
              }`}
            >
              <div
                className={`w-4 h-4 rounded-full bg-white transition-transform ${
                  fantasyMode ? 'translate-x-6' : 'translate-x-0'
                }`}
              />
            </button>
          </div>

          {/* Awakening Year Threshold */}
          <div>
            <label className="text-slate-300 font-semibold block mb-1.5">Awakening Epoch Threshold (Years)</label>
            <input
              type="number"
              min={1}
              max={200}
              value={awakeningThreshold}
              onChange={(e) => setAwakeningThreshold(Math.max(1, parseInt(e.target.value) || 50))}
              className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-xl font-mono text-purple-300 font-bold focus:outline-none focus:border-cyan-500"
            />
            <p className="text-[10px] text-slate-500 mt-1 font-mono">
              Simulation transitions from Mundane Era to Awakening Era upon reaching this year (default: 50).
            </p>
          </div>
        </div>

        {/* Footer */}
        <div className="px-6 py-4 border-t border-slate-800 bg-slate-900/40 flex justify-between items-center">
          <button
            type="button"
            onClick={() => setOpen(false)}
            className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 font-medium transition"
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={() => createWorldMutation.mutate()}
            disabled={createWorldMutation.isPending}
            className="flex items-center gap-1.5 px-5 py-2 bg-cyan-600 hover:bg-cyan-500 disabled:opacity-50 text-white font-semibold rounded-xl shadow-lg transition"
          >
            <Check className="w-4 h-4" />
            <span>{createWorldMutation.isPending ? 'Generating World...' : 'Initialize Simulation'}</span>
          </button>
        </div>
      </div>
    </div>
  );
};

