import React, { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { WorldSummaryDto, SimulationStatusDto } from '../../types/api';
import { useMapStore } from '../../store/useMapStore';
import {
  pauseSimulation,
  resumeSimulation,
  stepMonth,
  stepYear,
  setSimulationSpeed,
} from '../../services/api';
import {
  Play,
  Pause,
  SkipForward,
  FastForward,
  Layers,
  Filter,
  HelpCircle,
  MapPin,
  Activity,
  ShieldAlert,
  Globe,
  Users,
  Sparkles,
  Skull,
  Castle,
  Crown,
} from 'lucide-react';

interface TopBarProps {
  summary?: WorldSummaryDto;
  status?: SimulationStatusDto;
  sseConnected: boolean;
  isAwakened: boolean;
}

export const TopBar: React.FC<TopBarProps> = ({ summary, status, sseConnected, isAwakened }) => {
  const queryClient = useQueryClient();
  const toggleLayers = useMapStore((s) => s.toggleLayers);
  const toggleFilters = useMapStore((s) => s.toggleFilters);
  const toggleLegend = useMapStore((s) => s.toggleLegend);
  const toggleMinimap = useMapStore((s) => s.toggleMinimap);
  const showLayers = useMapStore((s) => s.showLayers);
  const showFilters = useMapStore((s) => s.showFilters);
  const showLegend = useMapStore((s) => s.showLegend);
  const showMinimap = useMapStore((s) => s.showMinimap);

  // Phase 3.5 Store Selectors
  const setNewWorldModalOpen = useMapStore((s) => s.setNewWorldModalOpen);
  const [controlPending, setControlPending] = useState(false);
  const [controlError, setControlError] = useState<string | null>(null);
  const isRunning = status?.lifecycleState === 'RUNNING';
  const isPaused = status?.lifecycleState === 'PAUSED';

  const runControl = async (action: () => Promise<unknown>) => {
    if (controlPending) return;
    setControlPending(true);
    setControlError(null);
    try {
      await action();
      await queryClient.invalidateQueries({ queryKey: ['simulation-status'] });
    } catch (error) {
      setControlError(error instanceof Error ? error.message : 'Simulation control request failed.');
    } finally {
      setControlPending(false);
    }
  };

  const handleTogglePlay = async () => {
    if (controlPending || (!isRunning && !isPaused)) return;
    await runControl(() => isRunning ? pauseSimulation() : resumeSimulation());
  };

  const handleStepMonth = async () => {
    if (isPaused) await runControl(stepMonth);
  };

  const handleStepYear = async () => {
    if (isPaused) await runControl(stepYear);
  };

  const handleSpeedChange = async (e: React.ChangeEvent<HTMLSelectElement>) => {
    await runControl(() => setSimulationSpeed(e.target.value));
  };

  const eraText = summary?.currentEra || (isAwakened ? 'Awakening Era' : 'Mundane Era');

  const currentMonth = summary?.month ?? 1;
  const season = React.useMemo(() => {
    const cycle = ((currentMonth - 1) % 12) + 1;
    if (cycle <= 3) return { name: 'Spring', symbol: '🌱', color: 'text-emerald-400' };
    if (cycle <= 6) return { name: 'Summer', symbol: '☀️', color: 'text-amber-400' };
    if (cycle <= 9) return { name: 'Autumn', symbol: '🍂', color: 'text-orange-400' };
    return { name: 'Winter', symbol: '❄️', color: 'text-sky-300' };
  }, [currentMonth]);

  return (
    <header
      className={`h-14 backdrop-blur-md px-4 flex items-center justify-between text-xs z-30 select-none border-b transition-colors ${
        isAwakened
          ? 'bg-slate-950/95 border-indigo-900/60 shadow-[0_4px_20px_rgba(30,27,75,0.4)]'
          : 'bg-stone-950/95 border-amber-900/50 shadow-[0_4px_20px_rgba(28,25,23,0.6)]'
      }`}
    >
      {/* 1. Left: Branding, Heraldic Crest, Era, Date, Status */}
      <div className="flex items-center gap-3">
        <div className="flex items-center gap-2.5">
          <div
            className={`w-7 h-7 rounded-lg flex items-center justify-center font-serif font-black text-xs shadow-md border ${
              isAwakened
                ? 'bg-gradient-to-br from-indigo-900 to-purple-950 text-cyan-300 border-cyan-500/40'
                : 'bg-gradient-to-br from-amber-900 to-stone-900 text-amber-300 border-amber-600/40'
            }`}
          >
            M
          </div>
          <h1 className="font-bold text-sm tracking-wider uppercase text-slate-100 font-sans">
            MIE <span className={isAwakened ? 'text-cyan-400' : 'text-amber-400'}>CivSim</span>
          </h1>
        </div>

        {/* Era Badge */}
        <span
          className={`px-2.5 py-0.5 rounded font-serif font-semibold tracking-wide uppercase text-[10px] border shadow-sm ${
            isAwakened
              ? 'bg-purple-950/70 text-purple-200 border-purple-500/50'
              : 'bg-amber-950/70 text-amber-200 border-amber-600/50'
          }`}
        >
          {eraText}
        </span>

        {/* Season & Date Display */}
        <div className="flex items-center gap-2 px-2.5 py-1 rounded bg-slate-900/90 border border-slate-800 font-mono text-slate-200 shadow-inner">
            <span className="text-[12px]" title={season.name}>
              {season.symbol}
            </span>
            <span className={isAwakened ? 'text-cyan-400 font-bold' : 'text-amber-400 font-bold'}>
              Year {summary?.year ?? 0}
            </span>
            <span className="text-slate-500">/</span>
            <span className="text-slate-300">
              Month {summary?.month ?? 1} <span className={`text-[10px] font-normal ${season.color}`}>({season.name})</span>
            </span>
        </div>

        {/* Status Pill */}
        <div className="flex items-center gap-1.5 px-2 py-0.5 rounded text-[10px] font-mono border border-slate-800 bg-slate-900">
          <span
            className={`w-1.5 h-1.5 rounded-full ${
              isRunning ? 'bg-emerald-400' : isPaused ? 'bg-amber-400' : 'bg-slate-500'
            }`}
          />
          <span className="text-slate-300">{status?.lifecycleState ?? 'CONNECTING'}</span>
        </div>
      </div>

      {/* 2. Center: Demographic & World Metrics Strategy Capsules */}
      <div className="hidden lg:flex items-center gap-2.5 text-slate-300">
        <div className="flex items-center gap-1.5 px-2 py-1 rounded-md bg-slate-900/60 border border-slate-800">
          <Users className="w-3.5 h-3.5 text-emerald-400" />
          <span className="text-slate-400 text-[11px]">Humans:</span>
          <span className="font-bold text-emerald-300 font-mono">{summary?.humanPopulation?.toLocaleString() ?? 0}</span>
        </div>

        <div className="flex items-center gap-1.5 px-2 py-1 rounded-md bg-slate-900/60 border border-slate-800">
          <Sparkles className="w-3.5 h-3.5 text-purple-400" />
          <span className="text-slate-400 text-[11px]">Awakened:</span>
          <span className="font-bold text-purple-300 font-mono">{summary?.awakenedPopulation?.toLocaleString() ?? 0}</span>
        </div>

        <div className="flex items-center gap-1.5 px-2 py-1 rounded-md bg-slate-900/60 border border-slate-800">
          <Skull className="w-3.5 h-3.5 text-rose-400" />
          <span className="text-slate-400 text-[11px]">Monsters:</span>
          <span className="font-bold text-rose-300 font-mono">{summary?.livingMonsterCount?.toLocaleString() ?? 0}</span>
        </div>

        <div className="flex items-center gap-1.5 px-2 py-1 rounded-md bg-slate-900/60 border border-slate-800">
          <ShieldAlert className="w-3.5 h-3.5 text-amber-400" />
          <span className="text-slate-400 text-[11px]">Lairs:</span>
          <span className="font-bold text-amber-300 font-mono">{summary?.activeMonsterLairCount ?? 0}</span>
        </div>

        <div className="flex items-center gap-1.5 px-2 py-1 rounded-md bg-slate-900/60 border border-slate-800">
          <Crown className="w-3.5 h-3.5 text-sky-400" />
          <span className="text-slate-400 text-[11px]">Settlements:</span>
          <span className="font-bold text-sky-300 font-mono">{summary?.settlementCount ?? 0}</span>
        </div>
      </div>

      {/* 3. Right: Playback Controls & Toolbars */}
      <div className="flex items-center gap-2">
        {/* Historical Return Button */}
        {/* Playback Button Group */}
        <div className="flex items-center bg-slate-900 border border-slate-800 rounded-lg p-0.5">
          <button
            onClick={handleTogglePlay}
            disabled={controlPending || (!isRunning && !isPaused)}
            className={`px-2.5 py-1 rounded text-xs font-semibold flex items-center gap-1 transition ${
              isRunning ? 'bg-amber-600 hover:bg-amber-500 text-white' : 'bg-emerald-600 hover:bg-emerald-500 text-white'
            }`}
            title={isRunning ? 'Pause Simulation' : isPaused ? 'Resume Simulation' : `Simulation ${status?.lifecycleState ?? 'status unavailable'}`}
          >
            {isRunning ? <Pause className="w-3.5 h-3.5" /> : <Play className="w-3.5 h-3.5" />}
            <span>{controlPending ? 'Waiting…' : isRunning ? 'Pause' : isPaused ? 'Resume' : 'Unavailable'}</span>
          </button>

          <button
            onClick={handleStepMonth}
            disabled={!isPaused || controlPending}
            className="p-1.5 hover:bg-slate-800 text-slate-300 rounded transition disabled:opacity-40"
            title="Step 1 Month"
          >
            <SkipForward className="w-3.5 h-3.5" />
          </button>

          <button
            onClick={handleStepYear}
            disabled={!isPaused || controlPending}
            className="p-1.5 hover:bg-slate-800 text-slate-300 rounded transition disabled:opacity-40"
            title="Step 1 Year (12 Months)"
          >
            <FastForward className="w-3.5 h-3.5" />
          </button>
        </div>

        {/* Speed Selector */}
        <select
          onChange={handleSpeedChange}
          disabled={controlPending}
          defaultValue="NORMAL"
          className="bg-slate-900 border border-slate-800 text-slate-300 rounded-lg px-2 py-1 text-xs focus:outline-none focus:border-cyan-500 font-mono"
        >
          <option value="SLOW">0.5x</option>
          <option value="NORMAL">1.0x</option>
          <option value="FAST">2.0x</option>
          <option value="VERY_FAST">5.0x</option>
          <option value="MAXIMUM">10.0x</option>
        </select>

        <div className="flex items-center bg-slate-900 border border-slate-800 rounded-lg p-0.5 gap-0.5">
          <button
            onClick={() => setNewWorldModalOpen(true)}
            className="flex items-center gap-1 px-2 py-1 rounded hover:bg-slate-800 text-slate-300 hover:text-white transition"
            title="Initialize New Seeded Simulation World"
          >
            <Globe className="w-3.5 h-3.5 text-emerald-400" />
            <span className="hidden xl:inline text-[11px] font-medium">New World</span>
          </button>
        </div>

        {controlError && <span role="alert" className="max-w-48 text-[10px] text-rose-300" title={controlError}>{controlError}</span>}

        {/* View Toggles */}
        <div className="flex items-center bg-slate-900 border border-slate-800 rounded-lg p-0.5 gap-0.5">
          <button
            onClick={toggleLayers}
            className={`p-1.5 rounded transition ${showLayers ? 'bg-cyan-950 text-cyan-400' : 'text-slate-400 hover:text-slate-200'}`}
            title="Toggle Map Layers"
          >
            <Layers className="w-3.5 h-3.5" />
          </button>

          <button
            onClick={toggleFilters}
            className={`p-1.5 rounded transition ${showFilters ? 'bg-cyan-950 text-cyan-400' : 'text-slate-400 hover:text-slate-200'}`}
            title="Toggle Entity Filters"
          >
            <Filter className="w-3.5 h-3.5" />
          </button>

          <button
            onClick={toggleLegend}
            className={`p-1.5 rounded transition ${showLegend ? 'bg-cyan-950 text-cyan-400' : 'text-slate-400 hover:text-slate-200'}`}
            title="Toggle Map Legend"
          >
            <HelpCircle className="w-3.5 h-3.5" />
          </button>

          <button
            onClick={toggleMinimap}
            className={`p-1.5 rounded transition ${showMinimap ? 'bg-cyan-950 text-cyan-400' : 'text-slate-400 hover:text-slate-200'}`}
            title="Toggle World Minimap"
          >
            <MapPin className="w-3.5 h-3.5" />
          </button>
        </div>

        {/* SSE Live Connection Pill */}
        <div
          className={`flex items-center gap-1 px-2 py-1 rounded text-[10px] font-mono border ${
            sseConnected
              ? 'bg-emerald-950/40 text-emerald-400 border-emerald-800/40'
              : 'bg-rose-950/40 text-rose-400 border-rose-800/40'
          }`}
          title={sseConnected ? 'SSE Live Stream Active' : 'Connecting to SSE...'}
        >
          <Activity className="w-3 h-3" />
          <span className="hidden sm:inline">{sseConnected ? 'LIVE' : 'DISCONNECTED'}</span>
        </div>
      </div>
    </header>
  );
};

