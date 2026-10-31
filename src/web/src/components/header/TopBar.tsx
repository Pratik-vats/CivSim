import React from 'react';
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
} from 'lucide-react';

interface TopBarProps {
  summary?: WorldSummaryDto;
  status?: SimulationStatusDto;
  sseConnected: boolean;
  isAwakened: boolean;
}

export const TopBar: React.FC<TopBarProps> = ({ summary, status, sseConnected, isAwakened }) => {
  const toggleLayers = useMapStore((s) => s.toggleLayers);
  const toggleFilters = useMapStore((s) => s.toggleFilters);
  const toggleLegend = useMapStore((s) => s.toggleLegend);
  const toggleMinimap = useMapStore((s) => s.toggleMinimap);
  const showLayers = useMapStore((s) => s.showLayers);
  const showFilters = useMapStore((s) => s.showFilters);
  const showLegend = useMapStore((s) => s.showLegend);
  const showMinimap = useMapStore((s) => s.showMinimap);

  const isRunning = status?.state === 'RUNNING';

  const handleTogglePlay = async () => {
    if (isRunning) {
      await pauseSimulation();
    } else {
      await resumeSimulation();
    }
  };

  const handleStepMonth = async () => {
    await stepMonth();
  };

  const handleStepYear = async () => {
    await stepYear();
  };

  const handleSpeedChange = async (e: React.ChangeEvent<HTMLSelectElement>) => {
    await setSimulationSpeed(e.target.value);
  };

  const eraText = summary?.currentEra || (isAwakened ? 'Awakening Era' : 'Mundane Era');

  return (
    <header className="h-14 bg-slate-950/90 backdrop-blur-md border-b border-slate-800 px-4 flex items-center justify-between text-xs z-30 select-none">
      {/* 1. Left: Branding, Era, Date, Status */}
      <div className="flex items-center gap-3">
        <div className="flex items-center gap-2">
          <div className="w-2.5 h-2.5 rounded-full bg-cyan-400 animate-pulse" />
          <h1 className="font-bold text-sm tracking-wider uppercase text-slate-100 font-sans">
            MIE <span className="text-cyan-400">CivSim</span>
          </h1>
        </div>

        {/* Era Badge */}
        <span
          className={`px-2 py-0.5 rounded font-semibold tracking-wide uppercase text-[10px] border ${
            isAwakened
              ? 'bg-purple-950/60 text-purple-300 border-purple-500/40'
              : 'bg-amber-950/60 text-amber-300 border-amber-600/40'
          }`}
        >
          {eraText}
        </span>

        {/* Date Display */}
        <div className="flex items-center gap-1.5 px-2.5 py-1 rounded bg-slate-900 border border-slate-800 font-mono text-slate-200">
          <span className="text-cyan-400 font-bold">Y{summary?.year ?? 0}</span>
          <span className="text-slate-500">/</span>
          <span>M{summary?.month ?? 1}</span>
          <span className="text-slate-600 text-[10px]">({summary?.totalMonths ?? 0}m)</span>
        </div>

        {/* Status Pill */}
        <div className="flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-mono border border-slate-800 bg-slate-900">
          <span
            className={`w-1.5 h-1.5 rounded-full ${
              isRunning ? 'bg-emerald-400' : status?.state === 'PAUSED' ? 'bg-amber-400' : 'bg-slate-500'
            }`}
          />
          <span className="text-slate-300">{status?.state ?? 'READY'}</span>
        </div>
      </div>

      {/* 2. Center: Demographic & World Metrics */}
      <div className="hidden lg:flex items-center gap-4 text-slate-300">
        <div className="flex items-center gap-1.5">
          <span className="text-slate-500">Humans:</span>
          <span className="font-bold text-emerald-400">{summary?.humanPopulation?.toLocaleString() ?? 0}</span>
        </div>

        <div className="flex items-center gap-1.5">
          <span className="text-slate-500">Awakened:</span>
          <span className="font-bold text-purple-400">{summary?.awakenedPopulation?.toLocaleString() ?? 0}</span>
        </div>

        <div className="flex items-center gap-1.5">
          <span className="text-slate-500">Monsters:</span>
          <span className="font-bold text-rose-400">{summary?.livingMonsterCount?.toLocaleString() ?? 0}</span>
        </div>

        <div className="flex items-center gap-1.5">
          <span className="text-slate-500">Lairs:</span>
          <span className="font-bold text-amber-400">{summary?.activeMonsterLairCount ?? 0}</span>
        </div>

        <div className="flex items-center gap-1.5">
          <span className="text-slate-500">Settlements:</span>
          <span className="font-bold text-sky-400">{summary?.settlementCount ?? 0}</span>
        </div>
      </div>

      {/* 3. Right: Playback Controls & Toolbars */}
      <div className="flex items-center gap-2">
        {/* Playback Button Group */}
        <div className="flex items-center bg-slate-900 border border-slate-800 rounded-lg p-0.5">
          <button
            onClick={handleTogglePlay}
            className={`px-2.5 py-1 rounded text-xs font-semibold flex items-center gap-1 transition ${
              isRunning ? 'bg-amber-600 hover:bg-amber-500 text-white' : 'bg-emerald-600 hover:bg-emerald-500 text-white'
            }`}
            title={isRunning ? 'Pause Simulation' : 'Resume Simulation'}
          >
            {isRunning ? <Pause className="w-3.5 h-3.5" /> : <Play className="w-3.5 h-3.5" />}
            <span>{isRunning ? 'Pause' : 'Play'}</span>
          </button>

          <button
            onClick={handleStepMonth}
            className="p-1.5 hover:bg-slate-800 text-slate-300 rounded transition"
            title="Step 1 Month"
          >
            <SkipForward className="w-3.5 h-3.5" />
          </button>

          <button
            onClick={handleStepYear}
            className="p-1.5 hover:bg-slate-800 text-slate-300 rounded transition"
            title="Step 1 Year (12 Months)"
          >
            <FastForward className="w-3.5 h-3.5" />
          </button>
        </div>

        {/* Speed Selector */}
        <select
          onChange={handleSpeedChange}
          defaultValue="NORMAL"
          className="bg-slate-900 border border-slate-800 text-slate-300 rounded-lg px-2 py-1 text-xs focus:outline-none focus:border-cyan-500 font-mono"
        >
          <option value="SLOW">0.5x</option>
          <option value="NORMAL">1.0x</option>
          <option value="FAST">2.0x</option>
          <option value="VERY_FAST">5.0x</option>
          <option value="MAXIMUM">10.0x</option>
        </select>

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

