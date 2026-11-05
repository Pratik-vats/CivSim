import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { fetchFamilies, FamilyFilterParams } from '../../services/api';
import { useMapStore } from '../../store/useMapStore';
import {
  Users,
  Search,
  ChevronLeft,
  ChevronRight,
  ArrowUpDown,
  Zap,
  Crown,
  Network,
  Calendar,
} from 'lucide-react';
import { FamilySummaryDto } from '../../types/api';

export const FamilyDynastyExplorer: React.FC = () => {
  const openFamilyProfile = useMapStore((s) => s.openFamilyProfile);
  const openPersonProfile = useMapStore((s) => s.openPersonProfile);

  const [search, setSearch] = useState('');
  const [sortBy, setSortBy] = useState('lineage');
  const [sortDir, setSortDir] = useState<'asc' | 'desc'>('desc');
  const [page, setPage] = useState(0);
  const pageSize = 12;

  const queryParams: FamilyFilterParams = {
    page,
    size: pageSize,
    search: search.trim() || undefined,
    sortBy,
    sortDir,
  };

  const { data, isLoading } = useQuery({
    queryKey: ['families', queryParams],
    queryFn: () => fetchFamilies(queryParams),
  });

  return (
    <div className="flex-1 flex flex-col h-full bg-slate-950 text-slate-100 overflow-hidden">
      {/* Header & Controls */}
      <div className="p-4 bg-slate-900 border-b border-slate-800 space-y-3">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h1 className="text-xl font-bold text-slate-100 flex items-center gap-2">
              <Users className="w-5 h-5 text-amber-400" />
              Family Dynasties & Bloodlines
            </h1>
            <p className="text-xs text-slate-400">
              Tracking {data?.totalElements ?? '...'} noble houses, lineages, and generational trees
            </p>
          </div>
        </div>

        {/* Filter Toolbar */}
        <div className="flex flex-wrap gap-2 text-xs">
          <div className="relative flex-1 min-w-[200px]">
            <Search className="w-4 h-4 absolute left-3 top-2.5 text-slate-400" />
            <input
              type="text"
              placeholder="Search family surname..."
              value={search}
              onChange={(e) => {
                setSearch(e.target.value);
                setPage(0);
              }}
              className="w-full bg-slate-800 border border-slate-700 rounded-lg pl-9 pr-3 py-2 text-slate-100 placeholder-slate-500 focus:outline-none focus:border-amber-500"
            />
          </div>

          <select
            value={sortBy}
            onChange={(e) => {
              setSortBy(e.target.value);
              setPage(0);
            }}
            className="bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-slate-200 focus:outline-none focus:border-amber-500"
          >
            <option value="lineage">Sort by Total Lineage</option>
            <option value="living">Sort by Living Members</option>
            <option value="founded">Sort by Founded Year</option>
            <option value="tier">Sort by Highest Tier</option>
            <option value="generations">Sort by Generations Count</option>
            <option value="surname">Sort by Surname</option>
          </select>

          <button
            onClick={() => setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'))}
            className="px-3 bg-slate-800 hover:bg-slate-700 border border-slate-700 rounded-lg py-2 flex items-center gap-1 transition text-slate-300"
          >
            <ArrowUpDown className="w-3.5 h-3.5" />
            <span>{sortDir.toUpperCase()}</span>
          </button>
        </div>
      </div>

      {/* Dynasties Grid */}
      <div className="flex-1 overflow-y-auto p-4">
        {isLoading ? (
          <div className="py-24 text-center text-slate-400">Loading family dynasty registries...</div>
        ) : !data || data.content.length === 0 ? (
          <div className="py-24 text-center text-slate-500">
            <Users className="w-8 h-8 mx-auto text-slate-600 mb-2" />
            <p>No family dynasties match the current query.</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {data.content.map((fam: FamilySummaryDto) => (
              <div
                key={fam.id}
                className="bg-slate-900 border border-slate-800 hover:border-amber-500/50 rounded-xl p-4 transition shadow flex flex-col justify-between"
              >
                <div>
                  <div className="flex items-start justify-between">
                    <div>
                      <h3 className="font-bold text-base text-slate-100 flex items-center gap-1.5">
                        <span>The {fam.surname} Dynasty</span>
                      </h3>
                      <p className="text-[11px] text-slate-400 flex items-center gap-1 mt-0.5">
                        <Calendar className="w-3 h-3 text-slate-500" />
                        Founded Year {fam.foundedYear}
                        {fam.founderName && ` by ${fam.founderName}`}
                      </p>
                    </div>
                    <span className="text-[10px] font-mono text-slate-500 bg-slate-800 px-2 py-0.5 rounded">
                      #FAM-{fam.id}
                    </span>
                  </div>

                  {/* Demographic counts */}
                  <div className="grid grid-cols-3 gap-2 mt-4 text-center text-xs">
                    <div className="bg-slate-800/60 p-2 rounded-lg border border-slate-700/40">
                      <span className="text-slate-400 block text-[10px]">Living</span>
                      <span className="font-bold text-emerald-400 text-sm">{fam.livingCount}</span>
                    </div>
                    <div className="bg-slate-800/60 p-2 rounded-lg border border-slate-700/40">
                      <span className="text-slate-400 block text-[10px]">Deceased</span>
                      <span className="font-bold text-rose-400 text-sm">{fam.deceasedCount}</span>
                    </div>
                    <div className="bg-slate-800/60 p-2 rounded-lg border border-slate-700/40">
                      <span className="text-slate-400 block text-[10px]">Generations</span>
                      <span className="font-bold text-sky-400 text-sm">{fam.generationsCount}</span>
                    </div>
                  </div>

                  {/* Highlights */}
                  <div className="mt-3 space-y-1.5 text-xs">
                    {fam.mostInfluentialPersonName && (
                      <div className="flex items-center justify-between text-slate-300">
                        <span className="text-slate-400 flex items-center gap-1 text-[11px]">
                          <Crown className="w-3 h-3 text-amber-400" /> Most Influential:
                        </span>
                        <button
                          onClick={() => fam.mostInfluentialPersonId && openPersonProfile(fam.mostInfluentialPersonId)}
                          className="font-medium text-sky-400 hover:underline truncate max-w-[130px]"
                        >
                          {fam.mostInfluentialPersonName}
                        </button>
                      </div>
                    )}
                    {fam.highestTierPersonName && (
                      <div className="flex items-center justify-between text-slate-300">
                        <span className="text-slate-400 flex items-center gap-1 text-[11px]">
                          <Zap className="w-3 h-3 text-purple-400" /> Highest Tier:
                        </span>
                        <button
                          onClick={() => fam.highestTierPersonId && openPersonProfile(fam.highestTierPersonId)}
                          className="font-medium text-purple-300 hover:underline truncate max-w-[130px]"
                        >
                          T{fam.highestTier} {fam.highestTierPersonName}
                        </button>
                      </div>
                    )}
                  </div>
                </div>

                {/* Footer Action */}
                <div className="mt-4 pt-3 border-t border-slate-800/80 flex items-center justify-between">
                  <span className="text-[11px] text-slate-400">
                    Total recorded lineage: <strong className="text-slate-200">{fam.totalLineageCount}</strong>
                  </span>
                  <button
                    onClick={() => openFamilyProfile(fam.id)}
                    className="px-3 py-1.5 bg-amber-500/10 hover:bg-amber-500/20 text-amber-300 border border-amber-500/30 rounded-lg text-xs font-semibold flex items-center gap-1.5 transition"
                  >
                    <Network className="w-3.5 h-3.5" />
                    <span>View Tree</span>
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Pagination Footer */}
      {data && (
        <div className="p-3 bg-slate-900 border-t border-slate-800 flex items-center justify-between text-xs text-slate-400">
          <div>
            Showing {data.content.length > 0 ? page * pageSize + 1 : 0} to{' '}
            {Math.min((page + 1) * pageSize, data.totalElements)} of {data.totalElements} families
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

