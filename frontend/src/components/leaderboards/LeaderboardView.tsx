import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { fetchLeaderboard } from '../../services/api';
import { useMapStore } from '../../store/useMapStore';
import {
  Trophy,
  Calendar,
  AlertCircle,
  Medal,
  Users,
  Compass,
  Zap,
  Coins,
  Flame,
  Hammer,
  Skull,
  Shield,
  History,
} from 'lucide-react';
import { LeaderboardEntryDto } from '../../types/api';

const CATEGORIES = [
  { id: 'POWER', label: 'Combat Power', icon: Flame },
  { id: 'TIER', label: 'Supernatural Tier', icon: Zap },
  { id: 'WARRIORS', label: 'Martial Warriors', icon: Shield },
  { id: 'WEALTH', label: 'Personal Wealth', icon: Coins },
  { id: 'INFLUENCE', label: 'Influence & Prestige', icon: Trophy },
  { id: 'CLANS', label: 'Noble Clans', icon: Compass },
  { id: 'ORGANIZATIONS', label: 'Organizations', icon: Users },
  { id: 'CRAFTSMEN', label: 'Master Crafters', icon: Hammer },
  { id: 'MONSTER_SLAYERS', label: 'Monster Slayers', icon: Skull },
  { id: 'LONGEST_LIVING', label: 'Longest Living', icon: History },
  { id: 'LEADERS', label: 'Settlement Leaders', icon: Medal },
];

export const LeaderboardView: React.FC = () => {
  const openPersonProfile = useMapStore((s) => s.openPersonProfile);
  const openClanProfile = useMapStore((s) => s.openClanProfile);

  const [category, setCategory] = useState<string>('POWER');
  const [targetYear, setTargetYear] = useState<string>(''); // blank = current year

  const parsedYear = targetYear.trim() !== '' ? parseInt(targetYear, 10) : undefined;

  const { data: board, isLoading } = useQuery({
    queryKey: ['leaderboard', category, parsedYear],
    queryFn: () => fetchLeaderboard(category, parsedYear),
  });

  const handleEntityClick = (entityId: string) => {
    // If it's numeric, it's a person ID; otherwise an organization ID
    const numId = parseInt(entityId, 10);
    if (!isNaN(numId) && String(numId) === entityId) {
      openPersonProfile(numId);
    } else {
      openClanProfile(entityId);
    }
  };

  return (
    <div className="flex-1 flex flex-col h-full bg-slate-950 text-slate-100 overflow-hidden">
      {/* Header */}
      <div className="p-4 bg-slate-900 border-b border-slate-800 space-y-3">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h1 className="text-xl font-bold text-slate-100 flex items-center gap-2">
              <Trophy className="w-5 h-5 text-amber-400" />
              Global Civilization Leaderboards
            </h1>
            <p className="text-xs text-slate-400">
              Authority, combat prowess, wealth, craftsmanship, and organizational supremacy across epochs
            </p>
          </div>

          {/* Historical Year Filter */}
          <div className="flex items-center gap-2 bg-slate-800 border border-slate-700 px-3 py-1.5 rounded-lg text-xs">
            <Calendar className="w-4 h-4 text-amber-400" />
            <span className="text-slate-300 font-medium">Historical Year:</span>
            <input
              type="number"
              placeholder="Current"
              value={targetYear}
              onChange={(e) => setTargetYear(e.target.value)}
              className="w-20 bg-slate-900 border border-slate-700 rounded px-2 py-0.5 text-xs text-slate-100 font-mono focus:outline-none focus:border-amber-400"
            />
            {targetYear && (
              <button
                onClick={() => setTargetYear('')}
                className="text-slate-400 hover:text-slate-200 text-[10px] ml-1"
              >
                Reset
              </button>
            )}
          </div>
        </div>

        {/* Category Pills Bar */}
        <div className="flex gap-1.5 overflow-x-auto pb-1 text-xs no-scrollbar">
          {CATEGORIES.map((cat) => {
            const Icon = cat.icon;
            const isSelected = category === cat.id;
            return (
              <button
                key={cat.id}
                onClick={() => setCategory(cat.id)}
                className={`px-3 py-1.5 rounded-lg font-medium flex items-center gap-1.5 shrink-0 transition ${
                  isSelected
                    ? 'bg-amber-500/20 text-amber-300 border border-amber-500/40'
                    : 'bg-slate-800/80 text-slate-400 hover:text-slate-200 hover:bg-slate-800 border border-transparent'
                }`}
              >
                <Icon className="w-3.5 h-3.5" />
                <span>{cat.label}</span>
              </button>
            );
          })}
        </div>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-y-auto p-4 space-y-4">
        {isLoading ? (
          <div className="py-24 text-center text-slate-400">Computing civilization standings...</div>
        ) : !board?.historicalAvailable ? (
          /* Graceful handling of unrecorded historical boundary */
          <div className="p-6 bg-amber-950/20 border border-amber-800/40 rounded-xl space-y-3 max-w-xl mx-auto my-12 text-center">
            <AlertCircle className="w-10 h-10 text-amber-400 mx-auto" />
            <h3 className="font-bold text-slate-100 text-base">Historical Rankings Unavailable</h3>
            <p className="text-xs text-slate-300 leading-relaxed">
              {board?.unavailableMessage || 'Leaderboards for this specific historical period are unrecorded.'}
            </p>
            <div className="pt-2">
              <button
                onClick={() => setTargetYear('')}
                className="px-4 py-1.5 bg-amber-600 hover:bg-amber-500 text-white font-semibold rounded-lg text-xs transition"
              >
                View Current Standings (Year {board?.year ?? 0})
              </button>
            </div>
          </div>
        ) : board.entries.length === 0 ? (
          <div className="py-24 text-center text-slate-500 space-y-2">
            <Trophy className="w-8 h-8 mx-auto text-slate-600" />
            <p>No eligible entities recorded for this category in Year {board.year}.</p>
          </div>
        ) : (
          <div className="max-w-4xl mx-auto space-y-2">
            <div className="text-xs text-slate-400 flex justify-between items-center px-2 mb-2">
              <span>
                Ranking for <strong className="text-amber-400">{CATEGORIES.find((c) => c.id === category)?.label}</strong> &bull; Year {board.year}, Month {board.month}
              </span>
              <span>Top {board.entries.length} Entries</span>
            </div>

            <div className="divide-y divide-slate-800/80 bg-slate-900 border border-slate-800 rounded-xl overflow-hidden shadow">
              {board.entries.map((entry: LeaderboardEntryDto) => {
                const isGold = entry.rank === 1;
                const isSilver = entry.rank === 2;
                const isBronze = entry.rank === 3;

                return (
                  <div
                    key={entry.rank}
                    onClick={() => handleEntityClick(entry.entityId)}
                    className="p-3.5 hover:bg-slate-800/60 cursor-pointer transition flex items-center justify-between gap-4"
                  >
                    <div className="flex items-center gap-3">
                      {/* Rank Badge */}
                      <div
                        className={`w-7 h-7 rounded-full flex items-center justify-center font-bold text-xs shrink-0 ${
                          isGold
                            ? 'bg-amber-400 text-slate-950 ring-2 ring-amber-400/30'
                            : isSilver
                            ? 'bg-slate-300 text-slate-950'
                            : isBronze
                            ? 'bg-amber-700 text-slate-100'
                            : 'bg-slate-800 text-slate-400 border border-slate-700'
                        }`}
                      >
                        {entry.rank}
                      </div>

                      {/* Name & Subtitle */}
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="font-bold text-slate-100 text-sm hover:text-amber-400 transition">
                            {entry.entityName}
                          </span>
                          <span className="text-[10px] font-mono text-slate-500">#{entry.entityId}</span>
                        </div>
                        <div className="text-[11px] text-slate-400 flex items-center gap-2 mt-0.5">
                          <span className="text-purple-300 font-semibold">{entry.subtitle}</span>
                          <span>&bull;</span>
                          <span>{entry.details}</span>
                        </div>
                      </div>
                    </div>

                    {/* Score */}
                    <div className="text-right shrink-0">
                      <div className="font-mono font-bold text-base text-amber-400">
                        {entry.score.toLocaleString(undefined, { maximumFractionDigits: 1 })}
                      </div>
                      <span className="text-[10px] text-slate-500 uppercase">Score</span>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

