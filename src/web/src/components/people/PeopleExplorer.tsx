import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { fetchPeople, PeopleFilterParams } from '../../services/api';
import { useMapStore } from '../../store/useMapStore';
import {
  Search,
  Filter,
  User,
  Zap,
  Flame,
  Coins,
  ChevronLeft,
  ChevronRight,
  ArrowUpDown,
  Skull,
  GitCompare,
  RotateCcw,
} from 'lucide-react';
import { PersonSummaryDto } from '../../types/api';

export const PeopleExplorer: React.FC = () => {
  const openPersonProfile = useMapStore((s) => s.openPersonProfile);
  const openFamilyProfile = useMapStore((s) => s.openFamilyProfile);
  const openClanProfile = useMapStore((s) => s.openClanProfile);
  const openComparePeople = useMapStore((s) => s.openComparePeople);

  const [search, setSearch] = useState('');
  const [aliveFilter, setAliveFilter] = useState<'ALL' | 'ALIVE' | 'DECEASED'>('ALL');
  const [awakenedFilter, setAwakenedFilter] = useState<'ALL' | 'AWAKENED' | 'MUNDANE'>('ALL');
  const [classFilter, setClassFilter] = useState('ALL');
  const [tierFilter, setTierFilter] = useState<string>('ALL');
  const [sortBy, setSortBy] = useState('tier');
  const [sortDir, setSortDir] = useState<'asc' | 'desc'>('desc');
  const [page, setPage] = useState(0);
  const pageSize = 15;

  // Multi-selection for comparison
  const [selectedForCompare, setSelectedForCompare] = useState<number[]>([]);

  const queryParams: PeopleFilterParams = {
    page,
    size: pageSize,
    search: search.trim() || undefined,
    alive: aliveFilter === 'ALL' ? undefined : aliveFilter === 'ALIVE',
    awakened: awakenedFilter === 'ALL' ? undefined : awakenedFilter === 'AWAKENED',
    supernaturalClass: classFilter === 'ALL' ? undefined : classFilter,
    tier: tierFilter === 'ALL' ? undefined : parseInt(tierFilter, 10),
    sortBy,
    sortDir,
  };

  const { data, isLoading } = useQuery({
    queryKey: ['people', queryParams],
    queryFn: () => fetchPeople(queryParams),
  });

  const toggleCompareSelect = (id: number, e: React.MouseEvent) => {
    e.stopPropagation();
    if (selectedForCompare.includes(id)) {
      setSelectedForCompare(selectedForCompare.filter((i) => i !== id));
    } else {
      if (selectedForCompare.length >= 2) {
        setSelectedForCompare([selectedForCompare[1], id]);
      } else {
        setSelectedForCompare([...selectedForCompare, id]);
      }
    }
  };

  const handleLaunchCompare = () => {
    if (selectedForCompare.length === 2) {
      openComparePeople(selectedForCompare[0], selectedForCompare[1]);
    }
  };

  const handleResetFilters = () => {
    setSearch('');
    setAliveFilter('ALL');
    setAwakenedFilter('ALL');
    setClassFilter('ALL');
    setTierFilter('ALL');
    setSortBy('tier');
    setSortDir('desc');
    setPage(0);
  };

  return (
    <div className="flex-1 flex flex-col h-full bg-slate-950 text-slate-100 overflow-hidden">
      {/* Header & Controls */}
      <div className="p-4 bg-slate-900 border-b border-slate-800 space-y-3">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h1 className="text-xl font-bold text-slate-100 flex items-center gap-2">
              <User className="w-5 h-5 text-sky-400" />
              People & Character Explorer
            </h1>
            <p className="text-xs text-slate-400">
              Directory of {data?.totalElements ?? '...'} living and historical citizens
            </p>
          </div>

          {/* Comparison Action Bar */}
          {selectedForCompare.length > 0 && (
            <div className="flex items-center gap-2 bg-indigo-950/60 border border-indigo-700/50 px-3 py-1.5 rounded-lg text-xs animate-in fade-in">
              <GitCompare className="w-4 h-4 text-indigo-400" />
              <span>{selectedForCompare.length}/2 selected for comparison</span>
              {selectedForCompare.length === 2 && (
                <button
                  onClick={handleLaunchCompare}
                  className="px-2.5 py-1 bg-indigo-600 hover:bg-indigo-500 font-semibold text-white rounded transition shadow"
                >
                  Compare Side-by-Side
                </button>
              )}
              <button
                onClick={() => setSelectedForCompare([])}
                className="text-slate-400 hover:text-slate-200 ml-1"
              >
                Clear
              </button>
            </div>
          )}
        </div>

        {/* Search & Filter Bar */}
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-6 gap-2 text-xs">
          {/* Search Input */}
          <div className="relative md:col-span-2">
            <Search className="w-4 h-4 absolute left-3 top-2.5 text-slate-400" />
            <input
              type="text"
              placeholder="Search by name, ID, family..."
              value={search}
              onChange={(e) => {
                setSearch(e.target.value);
                setPage(0);
              }}
              className="w-full bg-slate-800 border border-slate-700 rounded-lg pl-9 pr-3 py-2 text-slate-100 placeholder-slate-500 focus:outline-none focus:border-sky-500"
            />
          </div>

          {/* Life Status */}
          <select
            value={aliveFilter}
            onChange={(e) => {
              setAliveFilter(e.target.value as any);
              setPage(0);
            }}
            className="bg-slate-800 border border-slate-700 rounded-lg px-2.5 py-2 text-slate-200 focus:outline-none focus:border-sky-500"
          >
            <option value="ALL">All Life Statuses</option>
            <option value="ALIVE">Living Citizens</option>
            <option value="DECEASED">Deceased (Historical)</option>
          </select>

          {/* Awakening */}
          <select
            value={awakenedFilter}
            onChange={(e) => {
              setAwakenedFilter(e.target.value as any);
              setPage(0);
            }}
            className="bg-slate-800 border border-slate-700 rounded-lg px-2.5 py-2 text-slate-200 focus:outline-none focus:border-sky-500"
          >
            <option value="ALL">All Awakening</option>
            <option value="AWAKENED">Awakened Only</option>
            <option value="MUNDANE">Mundane Only</option>
          </select>

          {/* Sort By */}
          <select
            value={sortBy}
            onChange={(e) => {
              setSortBy(e.target.value);
              setPage(0);
            }}
            className="bg-slate-800 border border-slate-700 rounded-lg px-2.5 py-2 text-slate-200 focus:outline-none focus:border-sky-500"
          >
            <option value="tier">Sort by Tier</option>
            <option value="power">Sort by Combat Power</option>
            <option value="wealth">Sort by Wealth</option>
            <option value="age">Sort by Age</option>
            <option value="name">Sort by Name</option>
          </select>

          {/* Sort Dir & Reset */}
          <div className="flex gap-1">
            <button
              onClick={() => setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'))}
              className="flex-1 bg-slate-800 hover:bg-slate-700 border border-slate-700 rounded-lg py-2 flex items-center justify-center gap-1 transition text-slate-300"
              title="Toggle Sort Direction"
            >
              <ArrowUpDown className="w-3.5 h-3.5" />
              <span>{sortDir.toUpperCase()}</span>
            </button>
            <button
              onClick={handleResetFilters}
              className="px-2.5 bg-slate-800 hover:bg-slate-700 border border-slate-700 rounded-lg flex items-center justify-center text-slate-400 hover:text-slate-200 transition"
              title="Reset Filters"
            >
              <RotateCcw className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>
      </div>

      {/* Directory Table */}
      <div className="flex-1 overflow-y-auto">
        {isLoading ? (
          <div className="py-24 text-center text-slate-400">Loading citizen records...</div>
        ) : !data || data.content.length === 0 ? (
          <div className="py-24 text-center text-slate-500 space-y-2">
            <User className="w-8 h-8 mx-auto text-slate-600" />
            <p>No individuals match the selected query and filters.</p>
            <button
              onClick={handleResetFilters}
              className="text-xs text-sky-400 hover:underline"
            >
              Reset all filters
            </button>
          </div>
        ) : (
          <table className="w-full text-left text-xs text-slate-300">
            <thead className="bg-slate-900/90 sticky top-0 z-10 border-b border-slate-800 text-slate-400 font-semibold uppercase text-[10px]">
              <tr>
                <th className="py-2.5 px-3 w-10 text-center">Cmp</th>
                <th className="py-2.5 px-3">Name & ID</th>
                <th className="py-2.5 px-3">Status</th>
                <th className="py-2.5 px-3">Age</th>
                <th className="py-2.5 px-3">Settlement</th>
                <th className="py-2.5 px-3">Family / Clan</th>
                <th className="py-2.5 px-3">Occupation</th>
                <th className="py-2.5 px-3">Tier & Class</th>
                <th className="py-2.5 px-3 text-right">Combat Power</th>
                <th className="py-2.5 px-3 text-right">Wealth</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60">
              {data.content.map((p: PersonSummaryDto) => {
                const isSelected = selectedForCompare.includes(p.id);
                return (
                  <tr
                    key={p.id}
                    onClick={() => openPersonProfile(p.id)}
                    className="hover:bg-slate-800/50 cursor-pointer transition"
                  >
                    <td className="py-2 px-3 text-center" onClick={(e) => toggleCompareSelect(p.id, e)}>
                      <input
                        type="checkbox"
                        checked={isSelected}
                        onChange={() => {}}
                        className="rounded border-slate-700 text-indigo-600 focus:ring-0 cursor-pointer"
                      />
                    </td>
                    <td className="py-2 px-3 font-medium text-slate-100 flex items-center gap-2">
                      <span className="font-bold text-sky-300 hover:underline">{p.name}</span>
                      <span className="text-[10px] text-slate-500 font-mono">#{p.id}</span>
                    </td>
                    <td className="py-2 px-3">
                      <span
                        className={`px-2 py-0.5 rounded text-[10px] font-semibold ${
                          p.alive
                            ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30'
                            : 'bg-rose-500/20 text-rose-300 border border-rose-500/30 flex items-center gap-1 w-fit'
                        }`}
                      >
                        {p.alive ? 'Living' : <><Skull className="w-2.5 h-2.5" /> Deceased</>}
                      </span>
                    </td>
                    <td className="py-2 px-3 font-mono text-slate-300">{p.age}</td>
                    <td className="py-2 px-3 text-slate-400">
                      {p.settlementName || '-'}
                    </td>
                    <td className="py-2 px-3 text-slate-300">
                      {p.familyName ? (
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            if (p.familyId) openFamilyProfile(p.familyId);
                          }}
                          className="hover:text-amber-400 transition"
                        >
                          {p.familyName}
                        </button>
                      ) : (
                        '-'
                      )}
                      {p.clanName && (
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            if (p.clanId) openClanProfile(p.clanId);
                          }}
                          className="text-[10px] text-purple-400 block hover:underline"
                        >
                          {p.clanName}
                        </button>
                      )}
                    </td>
                    <td className="py-2 px-3 text-slate-400">{p.occupation}</td>
                    <td className="py-2 px-3">
                      {p.awakened ? (
                        <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-purple-500/20 text-purple-300 border border-purple-500/30">
                          T{p.tier} {p.supernaturalClass}
                        </span>
                      ) : (
                        <span className="text-slate-500">Mundane</span>
                      )}
                    </td>
                    <td className="py-2 px-3 text-right font-mono text-rose-300 font-semibold">
                      {p.derivedPower.toFixed(1)}
                    </td>
                    <td className="py-2 px-3 text-right font-mono text-amber-300">
                      {p.wealth.toFixed(1)}s
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>

      {/* Pagination Footer */}
      {data && (
        <div className="p-3 bg-slate-900 border-t border-slate-800 flex items-center justify-between text-xs text-slate-400">
          <div>
            Showing {data.content.length > 0 ? page * pageSize + 1 : 0} to{' '}
            {Math.min((page + 1) * pageSize, data.totalElements)} of {data.totalElements} records
          </div>
          <div className="flex items-center gap-1.5">
            <button
              disabled={page === 0}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              className="p-1 rounded bg-slate-800 hover:bg-slate-700 disabled:opacity-40 disabled:cursor-not-allowed text-slate-200 transition"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            <span className="px-2">
              Page {page + 1} of {Math.max(1, data.totalPages)}
            </span>
            <button
              disabled={data.last || page + 1 >= data.totalPages}
              onClick={() => setPage((p) => p + 1)}
              className="p-1 rounded bg-slate-800 hover:bg-slate-700 disabled:opacity-40 disabled:cursor-not-allowed text-slate-200 transition"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

