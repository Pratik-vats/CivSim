import React, { useState, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { fetchHistoricalTimeline } from '../../services/api';
import { useMapStore } from '../../store/useMapStore';
import {
  Clock,
  Sparkles,
  Bookmark,
  ChevronLeft,
  ChevronRight,
  Radio,
  GitCompare,
  RotateCcw,
  Play,
  Pause,
  FastForward,
} from 'lucide-react';

interface HistoricalTimelineScrubberProps {
  worldId?: string;
  currentLiveYear: number;
  currentLiveMonth: number;
}

export const HistoricalTimelineScrubber: React.FC<HistoricalTimelineScrubberProps> = ({
  worldId,
  currentLiveYear,
  currentLiveMonth,
}) => {
  const timeTravelMode = useMapStore((s) => s.timeTravelMode);
  const historicalYear = useMapStore((s) => s.historicalYear);
  const historicalMonth = useMapStore((s) => s.historicalMonth);
  const setHistoricalMode = useMapStore((s) => s.setHistoricalMode);
  const setLiveMode = useMapStore((s) => s.setLiveMode);
  const openHistoricalCompare = useMapStore((s) => s.openHistoricalCompare);

  const [isPlaying, setIsPlaying] = useState<boolean>(false);
  const [playbackSpeed, setPlaybackSpeed] = useState<number>(1.0); // 0.5x, 1.0x, 2.0x, 5.0x

  const { data: timeline } = useQuery({
    queryKey: ['historical-timeline', worldId],
    queryFn: () => fetchHistoricalTimeline(worldId),
    refetchInterval: timeTravelMode === 'LIVE' ? 5000 : false,
  });

  const isHistorical = timeTravelMode === 'HISTORICAL';
  const displayedYear = isHistorical ? (historicalYear ?? currentLiveYear) : currentLiveYear;
  const displayedMonth = isHistorical ? (historicalMonth ?? currentLiveMonth) : currentLiveMonth;

  const minYear = timeline?.minHistoricalYear ?? 0;
  const maxYear = Math.max(minYear, timeline?.currentLiveYear ?? currentLiveYear);

  const handleYearChange = (newYear: number) => {
    if (newYear >= maxYear) {
      setLiveMode();
    } else {
      setHistoricalMode(newYear, 0);
    }
  };

  const handleStepYear = (delta: number) => {
    const target = Math.max(minYear, Math.min(maxYear, displayedYear + delta));
    handleYearChange(target);
  };

  // Playback timer
  useEffect(() => {
    let timer: ReturnType<typeof setInterval>;
    if (isPlaying) {
      const intervalMs = Math.max(350, 1800 / playbackSpeed);
      timer = setInterval(() => {
        if (displayedYear >= maxYear) {
          setIsPlaying(false);
          setLiveMode();
        } else {
          handleYearChange(displayedYear + 1);
        }
      }, intervalMs);
    }
    return () => clearInterval(timer);
  }, [isPlaying, displayedYear, maxYear, playbackSpeed]);

  // Jump to next or previous milestone
  const handleJumpMilestone = (forward: boolean) => {
    if (!timeline?.significantMilestones || timeline.significantMilestones.length === 0) return;
    const sorted = [...timeline.significantMilestones].sort((a, b) => a.year - b.year);
    if (forward) {
      const next = sorted.find((m) => m.year > displayedYear);
      if (next) handleYearChange(next.year);
    } else {
      const prev = [...sorted].reverse().find((m) => m.year < displayedYear);
      if (prev) handleYearChange(prev.year);
    }
  };

  return (
    <div className="w-full bg-slate-950/90 backdrop-blur-md border-t border-slate-800 px-4 py-2.5 flex flex-col gap-2 select-none z-20">
      {/* Upper Status & Control Row */}
      <div className="flex items-center justify-between gap-4 flex-wrap">
        {/* Left: Mode Badge & Description */}
        <div className="flex items-center gap-3">
          {isHistorical ? (
            <div className="flex items-center gap-2 px-2.5 py-1 rounded-lg bg-amber-950/70 border border-amber-600/60 text-amber-300 font-mono text-xs">
              <Clock className="w-3.5 h-3.5 text-amber-400" />
              <span className="font-bold">HISTORICAL MODE:</span>
              <span>
                Year {displayedYear}, Month {displayedMonth}
              </span>
              <span className="text-[10px] text-amber-400/80 bg-amber-900/40 px-1.5 py-0.5 rounded border border-amber-700/50">
                READ-ONLY
              </span>
            </div>
          ) : (
            <div className="flex items-center gap-2 px-2.5 py-1 rounded-lg bg-emerald-950/60 border border-emerald-600/50 text-emerald-300 font-mono text-xs">
              <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
              <span className="font-bold">LIVE WORLD:</span>
              <span>
                Year {currentLiveYear}, Month {currentLiveMonth}
              </span>
            </div>
          )}

          {/* Quick Return to Live Button */}
          {isHistorical && (
            <button
              onClick={() => {
                setIsPlaying(false);
                setLiveMode();
              }}
              className="flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-cyan-700 hover:bg-cyan-600 text-white text-xs font-semibold shadow transition"
              title="Return to authoritative live simulation"
            >
              <RotateCcw className="w-3 h-3" />
              <span>Jump to Live</span>
            </button>
          )}
        </div>

        {/* Center: Playback & Year Stepper */}
        <div className="flex items-center gap-1.5 flex-wrap">
          {/* Automated Play/Pause */}
          <button
            onClick={() => setIsPlaying(!isPlaying)}
            className={`px-2.5 py-1 rounded-lg text-xs font-bold flex items-center gap-1 transition ${
              isPlaying
                ? 'bg-amber-950 text-amber-300 border border-amber-700'
                : 'bg-cyan-950 text-cyan-300 border border-cyan-800 hover:bg-cyan-900'
            }`}
            title={isPlaying ? 'Pause Historical Playback' : 'Play Historical Evolution'}
          >
            {isPlaying ? <Pause className="w-3.5 h-3.5" /> : <Play className="w-3.5 h-3.5" />}
            <span>{isPlaying ? 'Pause' : 'Play Timeline'}</span>
          </button>

          {/* Speed Selector */}
          <div className="flex items-center gap-0.5 bg-slate-900 border border-slate-800 rounded-lg p-0.5 text-[10px] font-mono">
            {[0.5, 1.0, 2.0, 5.0].map((spd) => (
              <button
                key={spd}
                onClick={() => setPlaybackSpeed(spd)}
                className={`px-1.5 py-0.5 rounded transition ${
                  playbackSpeed === spd ? 'bg-cyan-600 text-white font-bold' : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                {spd}x
              </button>
            ))}
          </div>

          {/* Step Back & Forward */}
          <div className="flex items-center gap-1 bg-slate-900 border border-slate-800 rounded-lg p-0.5 font-mono text-xs">
            <button
              onClick={() => handleStepYear(-1)}
              disabled={displayedYear <= minYear}
              className="p-1 hover:bg-slate-800 disabled:opacity-30 rounded text-slate-300 transition"
              title="Step Back 1 Year"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>

            <span className="px-2 font-bold text-slate-200">
              Year <span className="text-cyan-400">{displayedYear}</span>
            </span>

            <button
              onClick={() => handleStepYear(1)}
              disabled={displayedYear >= maxYear}
              className="p-1 hover:bg-slate-800 disabled:opacity-30 rounded text-slate-300 transition"
              title="Step Forward 1 Year"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Right: Actions */}
        <div className="flex items-center gap-2">
          {timeline?.significantMilestones && timeline.significantMilestones.length > 0 && (
            <button
              onClick={() => handleJumpMilestone(true)}
              className="flex items-center gap-1 px-2 py-1 bg-purple-950/60 hover:bg-purple-900/80 border border-purple-800/80 rounded-lg text-purple-300 text-xs font-medium transition"
              title="Jump to Next Milestone"
            >
              <FastForward className="w-3.5 h-3.5" />
              <span className="hidden sm:inline">Next Milestone</span>
            </button>
          )}

          <button
            onClick={() => openHistoricalCompare(displayedYear, currentLiveYear)}
            className="flex items-center gap-1.5 px-2.5 py-1 bg-slate-900 hover:bg-slate-800 border border-slate-700/80 rounded-lg text-slate-200 text-xs font-medium transition"
            title="Compare two historical dates"
          >
            <GitCompare className="w-3.5 h-3.5 text-purple-400" />
            <span className="hidden sm:inline">Compare Dates</span>
          </button>
        </div>
      </div>

      {/* Lower Row: Interactive Scrubber Slider with Milestones & Checkpoints */}
      <div className="flex items-center gap-3">
        <span className="text-[10px] font-mono text-slate-500 w-10 text-right">Y{minYear}</span>

        <div className="relative flex-1 flex items-center">
          {/* Slider input */}
          <input
            type="range"
            min={minYear}
            max={maxYear}
            value={displayedYear}
            onChange={(e) => handleYearChange(parseInt(e.target.value))}
            className="w-full h-1.5 bg-slate-800 rounded-lg appearance-none cursor-pointer accent-cyan-400"
          />

          {/* Awakening Milestone Marker (Year 50) */}
          {maxYear >= 50 && (
            <div
              className="absolute top-1/2 -translate-y-1/2 -translate-x-1/2 pointer-events-none flex flex-col items-center"
              style={{ left: `${((50 - minYear) / Math.max(1, maxYear - minYear)) * 100}%` }}
            >
              <div
                className="w-2.5 h-2.5 rounded-full bg-purple-500 border border-slate-950 shadow-sm"
                title="Year 50: Awakening Era"
              />
            </div>
          )}

          {/* Checkpoint Indicators */}
          {timeline?.checkpointYears?.map((chkYear) => (
            <div
              key={chkYear}
              className="absolute top-1/2 -translate-y-1/2 -translate-x-1/2 pointer-events-none flex flex-col items-center"
              style={{ left: `${((chkYear - minYear) / Math.max(1, maxYear - minYear)) * 100}%` }}
              title={`Save Checkpoint at Year ${chkYear}`}
            >
              <div className="w-2 h-2 rotate-45 bg-amber-400 border border-slate-950" />
            </div>
          ))}
        </div>

        <span className="text-[10px] font-mono text-slate-500 w-10">Y{maxYear}</span>
      </div>

      {/* Significant Milestone Chips */}
      {timeline?.significantMilestones && timeline.significantMilestones.length > 0 && (
        <div className="flex items-center gap-2 overflow-x-auto scrollbar-none pt-0.5 text-[11px]">
          <span className="text-slate-500 text-[10px] uppercase font-mono tracking-wider shrink-0">
            Milestones:
          </span>
          {timeline.significantMilestones.slice(0, 6).map((m, idx) => (
            <button
              key={idx}
              onClick={() => handleYearChange(m.year)}
              className={`px-2 py-0.5 rounded-full border text-[10px] shrink-0 font-medium transition flex items-center gap-1 ${
                displayedYear === m.year
                  ? 'bg-purple-950 text-purple-300 border-purple-500'
                  : 'bg-slate-900 text-slate-400 border-slate-800 hover:border-slate-700'
              }`}
            >
              <Sparkles className="w-2.5 h-2.5 text-purple-400" />
              <span>
                Y{m.year}: {m.title}
              </span>
            </button>
          ))}
        </div>
      )}
    </div>
  );
};
