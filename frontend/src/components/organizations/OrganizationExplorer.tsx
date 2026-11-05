import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { fetchOrganizations } from '../../services/api';
import { useMapStore } from '../../store/useMapStore';
import {
  Compass,
  Search,
  ChevronLeft,
  ChevronRight,
  Crown,
  Award,
  Swords,
  GitCompare,
} from 'lucide-react';
import { OrganizationSummaryDto } from '../../types/api';

export const OrganizationExplorer: React.FC = () => {
  const openClanProfile = useMapStore((s) => s.openClanProfile);
  const openPersonProfile = useMapStore((s) => s.openPersonProfile);
  const openCompareOrgs = useMapStore((s) => s.openCompareOrgs);

  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const pageSize = 12;

  const [selectedForCompare, setSelectedForCompare] = useState<string[]>([]);

  const { data, isLoading } = useQuery({
    queryKey: ['organizations', page, pageSize],
    queryFn: () => fetchOrganizations(page, pageSize),
  });

  const toggleCompareSelect = (id: string, e: React.MouseEvent) => {
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
      openCompareOrgs(selectedForCompare[0], selectedForCompare[1]);
    }
  };

  const filtered = data?.content.filter((o) =>
    !search.trim() ||
    o.name.toLowerCase().includes(search.toLowerCase()) ||
    o.type.toLowerCase().includes(search.toLowerCase()) ||
    o.id.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="flex-1 flex flex-col h-full bg-slate-950 text-slate-100 overflow-hidden">
      {/* Header & Controls */}
      <div className="p-4 bg-slate-900 border-b border-slate-800 space-y-3">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h1 className="text-xl font-bold text-slate-100 flex items-center gap-2">
              <Compass className="w-5 h-5 text-purple-400" />
              Clans & Organizations
            </h1>
            <p className="text-xs text-slate-400">
              Directory of {data?.totalElements ?? '...'} noble houses, awakened orders, covens, and companies
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
                  Compare Organizations
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

        {/* Search Input */}
        <div className="relative max-w-md">
          <Search className="w-4 h-4 absolute left-3 top-2.5 text-slate-400" />
          <input
            type="text"
            placeholder="Search organization by name, type, or ID..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full bg-slate-800 border border-slate-700 rounded-lg pl-9 pr-3 py-2 text-xs text-slate-100 placeholder-slate-500 focus:outline-none focus:border-purple-500"
          />
        </div>
      </div>

      {/* Cards Grid */}
      <div className="flex-1 overflow-y-auto p-4">
        {isLoading ? (
          <div className="py-24 text-center text-slate-400">Loading organization registers...</div>
        ) : !filtered || filtered.length === 0 ? (
          <div className="py-24 text-center text-slate-500">
            <Compass className="w-8 h-8 mx-auto text-slate-600 mb-2" />
            <p>No organizations found matching search criteria.</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {filtered.map((org: OrganizationSummaryDto) => {
              const isSelected = selectedForCompare.includes(org.id);
              return (
                <div
                  key={org.id}
                  onClick={() => openClanProfile(org.id)}
                  className={`bg-slate-900 border ${
                    isSelected ? 'border-indigo-500 ring-2 ring-indigo-500/20' : 'border-slate-800 hover:border-purple-500/50'
                  } rounded-xl p-4 transition shadow cursor-pointer flex flex-col justify-between`}
                >
                  <div>
                    <div className="flex items-start justify-between">
                      <div>
                        <h3 className="font-bold text-base text-slate-100">{org.name}</h3>
                        <p className="text-[11px] text-purple-400 font-semibold mt-0.5">
                          {org.type} &bull; {org.alignment}
                        </p>
                      </div>
                      <div className="flex items-center gap-1.5" onClick={(e) => toggleCompareSelect(org.id, e)}>
                        <input
                          type="checkbox"
                          checked={isSelected}
                          onChange={() => {}}
                          className="rounded border-slate-700 text-indigo-600 focus:ring-0 cursor-pointer"
                          title="Select for comparison"
                        />
                      </div>
                    </div>

                    {/* Leader & Founding */}
                    <div className="mt-3 text-xs space-y-1 text-slate-300">
                      {org.leaderName && (
                        <div className="flex items-center justify-between">
                          <span className="text-slate-400 flex items-center gap-1 text-[11px]">
                            <Crown className="w-3 h-3 text-amber-400" /> Leader:
                          </span>
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              if (org.leaderId) openPersonProfile(parseInt(org.leaderId, 10));
                            }}
                            className="font-medium text-sky-400 hover:underline truncate max-w-[140px]"
                          >
                            {org.leaderName}
                          </button>
                        </div>
                      )}
                      <div className="flex items-center justify-between text-[11px] text-slate-400">
                        <span>Founded: Year {org.foundingYear}</span>
                        <span className="text-slate-300 font-semibold">{org.status}</span>
                      </div>
                    </div>

                    {/* Metrics */}
                    <div className="grid grid-cols-3 gap-2 mt-4 text-center text-xs">
                      <div className="bg-slate-800/60 p-2 rounded-lg border border-slate-700/40">
                        <span className="text-slate-400 block text-[10px]">Members</span>
                        <span className="font-bold text-slate-100 text-sm">{org.memberCount}</span>
                      </div>
                      <div className="bg-slate-800/60 p-2 rounded-lg border border-slate-700/40">
                        <span className="text-slate-400 block text-[10px]">Influence</span>
                        <span className="font-bold text-amber-400 text-sm">{org.reputation.toFixed(1)}</span>
                      </div>
                      <div className="bg-slate-800/60 p-2 rounded-lg border border-slate-700/40">
                        <span className="text-slate-400 block text-[10px]">Heirlooms</span>
                        <span className="font-bold text-purple-300 text-sm">{org.heirloomCount}</span>
                      </div>
                    </div>
                  </div>

                  {/* Footer */}
                  <div className="mt-4 pt-3 border-t border-slate-800 flex items-center justify-between text-xs text-slate-400">
                    <span className="flex items-center gap-1 text-[11px]">
                      <Swords className="w-3.5 h-3.5 text-rose-400" /> {org.battlesWon} Victories
                    </span>
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        openClanProfile(org.id);
                      }}
                      className="text-xs font-semibold text-purple-400 hover:underline"
                    >
                      Dossier & Diplomacy &rarr;
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Pagination Footer */}
      {data && (
        <div className="p-3 bg-slate-900 border-t border-slate-800 flex items-center justify-between text-xs text-slate-400">
          <div>
            Showing {data.content.length > 0 ? page * pageSize + 1 : 0} to{' '}
            {Math.min((page + 1) * pageSize, data.totalElements)} of {data.totalElements} organizations
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

