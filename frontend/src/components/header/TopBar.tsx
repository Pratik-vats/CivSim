import React, { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { Activity, AlertTriangle, ArrowRight, FastForward, Globe2, Pause, Play, SkipForward, Sparkles, Users, Skull, Shield, Landmark, Filter, BookOpen } from 'lucide-react';
import { WorldSummaryDto, SimulationStatusDto } from '../../types/api';
import { useMapStore } from '../../store/useMapStore';
import { pauseSimulation, resumeSimulation, stepMonth, stepYear, setSimulationSpeed } from '../../services/api';

interface TopBarProps {
  summary?: WorldSummaryDto;
  status?: SimulationStatusDto;
  sseConnected: boolean;
  isAwakened: boolean;
}

const Metric: React.FC<{ icon: React.ReactNode; label: string; value: string | number; tint: string }> = ({ icon, label, value, tint }) => (
  <div className="mie-metric" title={label}>
    <span className={tint}>{icon}</span>
    <span className="mie-metric-label">{label}</span>
    <strong>{value}</strong>
  </div>
);

export const TopBar: React.FC<TopBarProps> = ({ summary, status, sseConnected, isAwakened }) => {
  const queryClient = useQueryClient();
  const setNewWorldModalOpen = useMapStore((s) => s.setNewWorldModalOpen);
  const toggleFilters = useMapStore((s) => s.toggleFilters);
  const toggleLegend = useMapStore((s) => s.toggleLegend);
  const showFilters = useMapStore((s) => s.showFilters);
  const showLegend = useMapStore((s) => s.showLegend);
  const [controlPending, setControlPending] = useState(false);
  const [controlError, setControlError] = useState<string | null>(null);
  const isRunning = status?.lifecycleState === 'RUNNING';
  const isPaused = status?.lifecycleState === 'PAUSED';
  const isComplete = status?.lifecycleState === 'COMPLETED';
  const eraName = isAwakened ? 'The Awakening' : 'Age of Founding';
  const year = isComplete ? 300 : summary?.year ?? status?.currentYear ?? 0;
  const month = isComplete ? 12 : (summary?.month ?? status?.currentMonth ?? 0) + 1;

  const runControl = async (action: () => Promise<unknown>) => {
    if (controlPending) return;
    setControlPending(true);
    setControlError(null);
    try {
      await action();
      await queryClient.invalidateQueries({ queryKey: ['simulation-status'] });
      await queryClient.invalidateQueries({ queryKey: ['world-summary'] });
    } catch (error) {
      setControlError(error instanceof Error ? error.message : 'Simulation control request failed.');
    } finally {
      setControlPending(false);
    }
  };

  const playbackButton = 'mie-icon-button';
  const liveColor = isRunning ? 'bg-emerald-400' : isPaused ? 'bg-amber-400' : isComplete ? 'bg-cyan-400' : 'bg-slate-500';

  return (
    <header className={`mie-topbar ${isAwakened ? 'mie-topbar-awakened' : 'mie-topbar-mundane'}`}>
      <div className="mie-topbar-main">
        <div className="mie-brand-block">
          <div className="mie-brand-emblem"><span>M</span><i /></div>
          <div className="mie-brand-copy">
            <div className="mie-brand-title">MIE <span>WORLD ATLAS</span></div>
            <div className="mie-brand-subtitle">A living civilization</div>
          </div>
        </div>

        <div className="mie-date-block">
          <span className={`mie-era-tag ${isAwakened ? 'is-awakened' : ''}`}><span className="mie-era-dot" />{eraName}</span>
          <div className="mie-date"><span>YEAR</span><strong>{year.toLocaleString()}</strong><span className="mie-date-divider" /><span>MONTH {month}</span></div>
        </div>

        <div className="mie-playback-block">
          <div className={`mie-sim-state ${isRunning ? 'is-running' : isPaused ? 'is-paused' : ''}`} title={status?.lifecycleState ?? 'Connecting'}>
            <span className={`h-2 w-2 rounded-full ${liveColor} ${isRunning ? 'animate-pulse' : ''}`} />
            <span>{isComplete ? 'SIMULATION COMPLETE' : isRunning ? 'SIMULATING' : isPaused ? 'PAUSED' : status?.lifecycleState ?? 'CONNECTING'}</span>
          </div>
          <div className="mie-playback-controls">
            <button onClick={() => runControl(() => isRunning ? pauseSimulation() : resumeSimulation())}
              disabled={controlPending || (!isRunning && !isPaused)} className="mie-play-button" title={isRunning ? 'Pause simulation' : 'Resume simulation'}>
              {isRunning ? <Pause size={15} /> : <Play size={15} fill="currentColor" />}
              <span>{controlPending ? 'Working' : isComplete ? 'Complete' : isRunning ? 'Pause' : isPaused ? 'Resume' : 'Unavailable'}</span>
            </button>
            <button onClick={() => isPaused && runControl(stepMonth)} disabled={!isPaused || controlPending} className={playbackButton} title="Advance one month"><SkipForward size={15} /></button>
            <button onClick={() => isPaused && runControl(stepYear)} disabled={!isPaused || controlPending} className={playbackButton} title="Advance one year"><FastForward size={15} /></button>
          </div>
          <select aria-label="Simulation speed" onChange={(e) => runControl(() => setSimulationSpeed(e.target.value))} disabled={controlPending}
            value={status?.speed ?? 'NORMAL'} className="mie-speed-select">
            <option value="SLOW">0.5×</option><option value="NORMAL">1×</option><option value="FAST">2×</option><option value="VERY_FAST">5×</option><option value="MAXIMUM">10×</option>
          </select>
          <button onClick={() => setNewWorldModalOpen(true)} className="mie-new-world"><Globe2 size={15} /><span>New world</span><ArrowRight size={13} /></button>
        </div>
      </div>

      <div className="mie-topbar-foot">
        <div className="mie-world-identity"><span className="mie-world-mark"><Landmark size={13} /></span><span>{summary?.worldId ?? status?.worldId ?? 'WORLD INITIALIZING'}</span></div>
        <div className="mie-metrics-row">
          <Metric icon={<Users size={14} />} label="Population" value={(summary?.humanPopulation ?? 0).toLocaleString()} tint="text-emerald-300" />
          <Metric icon={<Sparkles size={14} />} label="Awakened" value={(summary?.awakenedPopulation ?? 0).toLocaleString()} tint="text-violet-300" />
          <Metric icon={<Skull size={14} />} label="Living monsters" value={(summary?.livingMonsterCount ?? 0).toLocaleString()} tint="text-rose-300" />
          <Metric icon={<Shield size={14} />} label="Active lairs" value={summary?.activeMonsterLairCount ?? 0} tint="text-amber-300" />
          <Metric icon={<Landmark size={14} />} label="Settlements" value={summary?.settlementCount ?? 0} tint="text-sky-300" />
        </div>
        <div className="mie-quick-tools">
          <button onClick={toggleFilters} aria-pressed={showFilters} className={showFilters ? 'is-active' : ''} title="Toggle map filters"><Filter size={14} /></button>
          <button onClick={toggleLegend} aria-pressed={showLegend} className={showLegend ? 'is-active' : ''} title="Toggle map legend"><BookOpen size={14} /></button>
        </div>
        <div className={`mie-stream-state ${sseConnected ? 'is-connected' : ''}`} title={sseConnected ? 'Live event stream connected' : 'Live event stream disconnected'}>
          {controlError ? <><AlertTriangle size={13} /><span className="mie-stream-error">{controlError}</span></> : <><Activity size={13} /><span className="hidden sm:inline">{sseConnected ? 'LIVE FEED' : 'RECONNECTING'}</span></>}
          <span className={`mie-stream-dot ${sseConnected ? 'is-connected' : ''}`} />
        </div>
      </div>
    </header>
  );
};
