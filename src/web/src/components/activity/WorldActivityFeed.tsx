import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { fetchWorldActivity } from '../../services/api';
import { useMapStore } from '../../store/useMapStore';
import { WorldActivityEventDto } from '../../types/api';
import {
  Activity,
  Play,
  Pause,
  Filter,
  Search,
  ChevronLeft,
  ChevronRight,
  Swords,
  Skull,
  Compass,
  Flame,
  Sparkles,
  Building,
  Shield,
  HelpCircle,
  MapPin,
  Clock,
  Eye,
} from 'lucide-react';

const CATEGORIES = [
  { id: 'ALL', label: 'All Categories' },
  { id: 'BATTLE', label: 'Battles' },
  { id: 'MONSTER_TIDE', label: 'Monster Tides' },
  { id: 'EXPEDITION', label: 'Expeditions' },
  { id: 'FEUD', label: 'Blood Feuds' },
  { id: 'CIVILIZATION', label: 'Civilization' },
  { id: 'AWAKENING', label: 'Awakening' },
  { id: 'DIPLOMACY', label: 'Diplomacy' },
  { id: 'CRAFTING', label: 'Crafting & Heirlooms' },
  { id: 'DISASTER', label: 'Disasters' },
];

const SEVERITIES = [
  { id: 'ALL', label: 'All Severities' },
  { id: 'CRITICAL', label: 'Critical' },
  { id: 'IMPORTANT', label: 'Important' },
  { id: 'NORMAL', label: 'Normal' },
  { id: 'TRIVIAL', label: 'Trivial' },
];

export const WorldActivityFeed: React.FC = () => {
  const [page, setPage] = useState(0);
  const [selectedCategory, setSelectedCategory] = useState('ALL');
  const [selectedSeverity, setSelectedSeverity] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [isLiveStreaming, setIsLiveStreaming] = useState(true);

  const openActivityEvent = useMapStore((s) => s.openActivityEvent);
  const openPersonProfile = useMapStore((s) => s.openPersonProfile);
  const openClanProfile = useMapStore((s) => s.openClanProfile);
  const openBattleDetail = useMapStore((s) => s.openBattleDetail);
  const openTideDetail = useMapStore((s) => s.openTideDetail);
  const openExpeditionDetail = useMapStore((s) => s.openExpeditionDetail);
  const openFeudDetail = useMapStore((s) => s.openFeudDetail);
  const navigateToEntityOnMap = useMapStore((s) => s.navigateToEntityOnMap);

  const { data: pagedData, isLoading, isError, refetch } = useQuery({
    queryKey: ['world-activity', page, selectedCategory, selectedSeverity, searchQuery],
    queryFn: () =>
      fetchWorldActivity({
        page,
        size: 25,
        category: selectedCategory === 'ALL' ? undefined : selectedCategory,
        severity: selectedSeverity === 'ALL' ? undefined : selectedSeverity,
        search: searchQuery.trim() || undefined,
      }),
    refetchInterval: isLiveStreaming ? 2000 : false,
  });

  const getCategoryIcon = (category: string) => {
    switch (category.toUpperCase()) {
      case 'BATTLE':
        return <Swords className="w-4 h-4 text-rose-400" />;
      case 'MONSTER_TIDE':
        return <Skull className="w-4 h-4 text-purple-400" />;
      case 'EXPEDITION':
        return <Compass className="w-4 h-4 text-emerald-400" />;
      case 'FEUD':
        return <Flame className="w-4 h-4 text-amber-400" />;
      case 'AWAKENING':
        return <Sparkles className="w-4 h-4 text-cyan-400" />;
      case 'CIVILIZATION':
        return <Building className="w-4 h-4 text-blue-400" />;
      case 'DIPLOMACY':
        return <Shield className="w-4 h-4 text-indigo-400" />;
      default:
        return <Activity className="w-4 h-4 text-slate-400" />;
    }
  };

  const getSeverityBadge = (severity: string) => {
    switch (severity.toUpperCase()) {
      case 'CRITICAL':
        return (
          <span className="px-2 py-0.5 text-[10px] font-bold uppercase rounded-md bg-rose-950/80 text-rose-300 border border-rose-700/60 animate-pulse">
            Critical
          </span>
        );
      case 'IMPORTANT':
        return (
          <span className="px-2 py-0.5 text-[10px] font-bold uppercase rounded-md bg-amber-950/80 text-amber-300 border border-amber-700/60">
            Important
          </span>
        );
      case 'NORMAL':
        return (
          <span className="px-2 py-0.5 text-[10px] font-medium uppercase rounded-md bg-slate-800 text-slate-300 border border-slate-700">
            Normal
          </span>
        );
      default:
        return (
          <span className="px-2 py-0.5 text-[10px] font-medium uppercase rounded-md bg-slate-900 text-slate-400 border border-slate-800">
            {severity}
          </span>
        );
    }
  };

  const handleEntityJump = (e: React.MouseEvent, type: string, id: string, posX?: number | null, posY?: number | null) => {
    e.stopPropagation();
    const upperType = type.toUpperCase();
    if (upperType === 'SETTLEMENT' || upperType === 'VILLAGE') {
      navigateToEntityOnMap('SETTLEMENT', Number(id) || id, posX ?? undefined, posY ?? undefined);
    } else if (upperType === 'LAIR') {
      navigateToEntityOnMap('LAIR', id, posX ?? undefined, posY ?? undefined);
    } else if (upperType === 'ANOMALY') {
      navigateToEntityOnMap('ANOMALY', id, posX ?? undefined, posY ?? undefined);
    } else if (upperType === 'PERSON') {
      openPersonProfile(Number(id));
    } else if (upperType === 'ORGANIZATION' || upperType === 'CLAN') {
      openClanProfile(id);
    } else if (upperType === 'BATTLE') {
      openBattleDetail(id);
    } else if (upperType === 'MONSTER_TIDE') {
      openTideDetail(id);
    } else if (upperType === 'EXPEDITION') {
      openExpeditionDetail(id);
    } else if (upperType === 'FEUD') {
      openFeudDetail(id);
    }
  };

  return (
    <div className="flex-1 flex flex-col h-full overflow-hidden bg-slate-950/90 text-slate-100 select-none">
      {/* Top Header & Feed Controls */}
      <div className="p-4 border-b border-slate-800 bg-slate-900/60 flex flex-wrap items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="p-2 rounded-xl bg-cyan-950/60 text-cyan-400 border border-cyan-800/60">
            <Activity className="w-5 h-5" />
          </div>
          <div>
            <h1 className="text-base font-bold text-slate-100 flex items-center gap-2">
              World Activity Feed
              {isLiveStreaming ? (
                <span className="flex items-center gap-1.5 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-emerald-950/80 text-emerald-300 border border-emerald-700/60">
                  <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-ping" />
                  Live Sync
                </span>
              ) : (
                <span className="px-2 py-0.5 rounded-full text-[10px] font-semibold bg-amber-950/80 text-amber-300 border border-amber-700/60">
                  Paused
                </span>
              )}
            </h1>
            <p className="text-xs text-slate-400">
              Canonical chronologically sequenced event stream of all significant historical actions
            </p>
          </div>
        </div>

        {/* Live / Pause Button */}
        <div className="flex items-center gap-2">
          <button
            onClick={() => setIsLiveStreaming(!isLiveStreaming)}
            className={`px-3 py-1.5 rounded-xl text-xs font-semibold flex items-center gap-2 transition border ${
              isLiveStreaming
                ? 'bg-amber-950/50 hover:bg-amber-900/50 text-amber-300 border-amber-700/60'
                : 'bg-emerald-950/50 hover:bg-emerald-900/50 text-emerald-300 border-emerald-700/60'
            }`}
          >
            {isLiveStreaming ? (
              <>
                <Pause className="w-3.5 h-3.5" />
                <span>Pause Stream</span>
              </>
            ) : (
              <>
                <Play className="w-3.5 h-3.5" />
                <span>Resume Stream</span>
              </>
            )}
          </button>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="px-4 py-3 border-b border-slate-800/80 bg-slate-900/40 flex flex-wrap items-center gap-3">
        {/* Category Selector */}
        <div className="flex items-center gap-1.5 overflow-x-auto max-w-full pb-1 scrollbar-thin">
          {CATEGORIES.map((cat) => (
            <button
              key={cat.id}
              onClick={() => {
                setSelectedCategory(cat.id);
                setPage(0);
              }}
              className={`px-2.5 py-1 rounded-lg text-xs font-medium whitespace-nowrap transition border ${
                selectedCategory === cat.id
                  ? 'bg-cyan-950/80 text-cyan-300 border-cyan-700/70 shadow-sm'
                  : 'bg-slate-900/60 text-slate-400 hover:text-slate-200 border-slate-800'
              }`}
            >
              {cat.label}
            </button>
          ))}
        </div>

        <div className="flex-1 flex items-center gap-2 min-w-[240px]">
          {/* Severity Dropdown */}
          <select
            value={selectedSeverity}
            onChange={(e) => {
              setSelectedSeverity(e.target.value);
              setPage(0);
            }}
            className="px-2.5 py-1 rounded-lg bg-slate-900 border border-slate-700 text-xs text-slate-300 focus:outline-none focus:border-cyan-500"
          >
            {SEVERITIES.map((sev) => (
              <option key={sev.id} value={sev.id}>
                {sev.label}
              </option>
            ))}
          </select>

          {/* Search Input */}
          <div className="relative flex-1">
            <Search className="w-3.5 h-3.5 text-slate-500 absolute left-2.5 top-2" />
            <input
              type="text"
              placeholder="Search title, summary, entities..."
              value={searchQuery}
              onChange={(e) => {
                setSearchQuery(e.target.value);
                setPage(0);
              }}
              className="w-full pl-8 pr-3 py-1 rounded-lg bg-slate-900 border border-slate-700 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-cyan-500"
            />
          </div>
        </div>
      </div>

      {/* Events Feed Content */}
      <div className="flex-1 overflow-y-auto p-4 space-y-2.5 scrollbar-thin">
        {isLoading && (
          <div className="flex flex-col items-center justify-center py-16 text-slate-400">
            <div className="w-8 h-8 rounded-full border-2 border-cyan-500/20 border-t-cyan-400 animate-spin mb-3" />
            <p className="text-xs font-mono">Loading activity records...</p>
          </div>
        )}

        {isError && (
          <div className="p-4 rounded-xl bg-rose-950/40 border border-rose-800/60 text-center text-xs text-rose-300">
            Failed to load activity events from simulation engine.
          </div>
        )}

        {!isLoading && pagedData?.content?.length === 0 && (
          <div className="text-center py-16 text-slate-500">
            <Activity className="w-10 h-10 mx-auto mb-2 text-slate-600" />
            <p className="text-sm font-semibold">No Activity Events Found</p>
            <p className="text-xs mt-1">Adjust filters or allow the simulation to advance time.</p>
          </div>
        )}

        {pagedData?.content?.map((event: WorldActivityEventDto) => (
          <div
            key={event.id}
            onClick={() => openActivityEvent(event.id)}
            className="p-3.5 rounded-xl bg-slate-900/60 hover:bg-slate-800/70 border border-slate-800 hover:border-cyan-800/60 transition cursor-pointer flex flex-col gap-2 group"
          >
            {/* Header: Category, Severity, Timestamp, Replay badge */}
            <div className="flex items-center justify-between gap-2 flex-wrap">
              <div className="flex items-center gap-2">
                <div className="p-1 rounded-md bg-slate-950 border border-slate-800">
                  {getCategoryIcon(event.category)}
                </div>
                <span className="text-[11px] font-semibold text-slate-300 uppercase tracking-wider">
                  {event.category.replace('_', ' ')}
                </span>
                <span className="text-slate-600">•</span>
                <span className="text-xs font-bold text-slate-100 group-hover:text-cyan-300 transition">
                  {event.title}
                </span>
              </div>

              <div className="flex items-center gap-2">
                {getSeverityBadge(event.severity)}
                <div className="flex items-center gap-1 text-[11px] font-mono text-slate-400">
                  <Clock className="w-3 h-3 text-slate-500" />
                  <span>
                    Year {event.year}, Month {event.month}
                  </span>
                </div>
              </div>
            </div>

            {/* Summary text */}
            <p className="text-xs text-slate-300 leading-relaxed">{event.summary}</p>

            {/* Footer chips: location, primary entity, related entities */}
            <div className="flex items-center justify-between pt-1 border-t border-slate-800/50 flex-wrap gap-2 text-[11px]">
              <div className="flex items-center gap-2 flex-wrap">
                {event.location && (
                  <span className="flex items-center gap-1 text-slate-400 bg-slate-950/60 px-2 py-0.5 rounded border border-slate-800 font-mono">
                    <MapPin className="w-3 h-3 text-slate-500" />
                    {event.location}
                  </span>
                )}

                {/* Primary Entity Link */}
                {event.primaryEntityId && (
                  <button
                    onClick={(e) =>
                      handleEntityJump(e, event.primaryEntityType, event.primaryEntityId, event.posX, event.posY)
                    }
                    className="flex items-center gap-1 px-2 py-0.5 rounded bg-cyan-950/40 hover:bg-cyan-900/60 text-cyan-300 border border-cyan-800/50 transition font-mono"
                    title={`Inspect ${event.primaryEntityType} #${event.primaryEntityId}`}
                  >
                    <span>
                      {event.primaryEntityType}: {event.primaryEntityId}
                    </span>
                  </button>
                )}

                {/* Related Entities Links */}
                {event.relatedEntityIds?.map((relId) => (
                  <button
                    key={relId}
                    onClick={(e) => handleEntityJump(e, 'ENTITY', relId)}
                    className="px-1.5 py-0.5 rounded bg-slate-800 hover:bg-slate-700 text-slate-300 text-[10px] font-mono transition"
                  >
                    {relId}
                  </button>
                ))}
              </div>

              <div className="flex items-center gap-2 text-slate-500 text-[10px] font-mono">
                <span>Seq: {event.sequence}</span>
                <span className="text-slate-600">|</span>
                <span className="text-slate-400">{event.dataAvailability}</span>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Pagination Footer */}
      {pagedData && pagedData.totalPages > 1 && (
        <div className="p-3 border-t border-slate-800 bg-slate-900/70 flex items-center justify-between text-xs text-slate-400">
          <div>
            Page <span className="font-semibold text-slate-200">{pagedData.page + 1}</span> of{' '}
            <span className="font-semibold text-slate-200">{pagedData.totalPages}</span> ({pagedData.totalElements}{' '}
            events)
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={() => setPage(Math.max(0, page - 1))}
              disabled={page === 0}
              className="p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 disabled:opacity-40 disabled:cursor-not-allowed text-slate-300"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            <button
              onClick={() => setPage(Math.min(pagedData.totalPages - 1, page + 1))}
              disabled={pagedData.last}
              className="p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 disabled:opacity-40 disabled:cursor-not-allowed text-slate-300"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

