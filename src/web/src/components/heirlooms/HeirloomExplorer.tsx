import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { fetchHeirlooms, HeirloomFilterParams } from '../../services/api';
import { useMapStore } from '../../store/useMapStore';
import {
  Award,
  Search,
  Shield,
  Filter,
  Sparkles,
  ChevronLeft,
  ChevronRight,
} from 'lucide-react';
import { HeirloomSummaryDto } from '../../types/api';

export const HeirloomExplorer: React.FC = () => {
  const openHeirloomProfile = useMapStore((s) => s.openHeirloomProfile);
  const openClanProfile = useMapStore((s) => s.openClanProfile);
  const openPersonProfile = useMapStore((s) => s.openPersonProfile);

  const [custodyStatus, setCustodyStatus] = useState<string>('ALL');
  const [rarity, setRarity] = useState<string>('ALL');
  const [search, setSearch] = useState<string>('');
  const [page, setPage] = useState(0);
  const pageSize = 12;

  const queryParams: HeirloomFilterParams = {
    page,
    size: pageSize,
    custodyStatus: custodyStatus === 'ALL' ? undefined : custodyStatus,
    rarity: rarity === 'ALL' ? undefined : rarity,
  };

  const { data, isLoading } = useQuery({
    queryKey: ['heirlooms', queryParams],
    queryFn: () => fetchHeirlooms(queryParams),
  });

  const filtered = data?.content.filter((h) =>
    !search.trim() ||
    h.artifactTitle.toLowerCase().includes(search.toLowerCase()) ||
    h.equipmentId.toLowerCase().includes(search.toLowerCase()) ||
    h.owningClanName.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="flex-1 flex flex-col h-full bg-slate-950 text-slate-100 overflow-hidden">
      {/* Header & Controls */}
      <div className="p-4 bg-slate-900 border-b border-slate-800 space-y-3">
        <div>
          <h1 className="text-xl font-bold text-slate-100 flex items-center gap-2">
            <Award className="w-5 h-5 text-amber-400" />
            Clan Heirlooms & Legendary Equipment
          </h1>
          <p className="text-xs text-slate-400">
            Registered ancestral weapons, armor, and relics with multi-generational bearer custody chronicles
          </p>
        </div>

        {/* Filter Toolbar */}
        <div className="flex flex-wrap gap-2 text-xs">
          <div className="relative flex-1 min-w-[200px]">
            <Search className="w-4 h-4 absolute left-3 top-2.5 text-slate-400" />
            <input
              type="text"
              placeholder="Search by title, equipment ID, or clan..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full bg-slate-800 border border-slate-700 rounded-lg pl-9 pr-3 py-2 text-slate-100 placeholder-slate-500 focus:outline-none focus:border-amber-500"
            />
          </div>

          <select
            value={custodyStatus}
            onChange={(e) => {
              setCustodyStatus(e.target.value);
              setPage(0);
            }}
            className="bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-slate-200 focus:outline-none focus:border-amber-500"
          >
            <option value="ALL">All Custody Statuses</option>
            <option value="IN_CLAN_VAULT">In Clan Vault</option>
            <option value="BEARER_HELD">Bearer Held</option>
            <option value="BATTLEFIELD_LOST">Battlefield Lost</option>
            <option value="STOLEN">Stolen</option>
            <option value="DESTROYED">Destroyed</option>
          </select>

          <select
            value={rarity}
            onChange={(e) => {
              setRarity(e.target.value);
              setPage(0);
            }}
            className="bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-slate-200 focus:outline-none focus:border-amber-500"
          >
            <option value="ALL">All Rarities</option>
            <option value="COMMON">Common</option>
            <option value="UNCOMMON">Uncommon</option>
            <option value="RARE">Rare</option>
            <option value="LEGENDARY">Legendary</option>
            <option value="MYTHIC">Mythic</option>
          </select>
        </div>
      </div>

      {/* Grid */}
      <div className="flex-1 overflow-y-auto p-4">
        {isLoading ? (
          <div className="py-24 text-center text-slate-400">Loading heirloom registers...</div>
        ) : !filtered || filtered.length === 0 ? (
          <div className="py-24 text-center text-slate-500">
            <Award className="w-8 h-8 mx-auto text-slate-600 mb-2" />
            <p>No clan heirlooms found matching criteria.</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {filtered.map((h: HeirloomSummaryDto) => (
              <div
                key={h.heirloomId}
                onClick={() => openHeirloomProfile(h.heirloomId)}
                className="bg-slate-900 border border-slate-800 hover:border-amber-500/50 rounded-xl p-4 transition shadow cursor-pointer flex flex-col justify-between"
              >
                <div>
                  <div className="flex items-start justify-between">
                    <div>
                      <h3 className="font-bold text-base text-slate-100 flex items-center gap-1.5">
                        <span>{h.artifactTitle}</span>
                      </h3>
                      <p className="text-[11px] text-slate-400 font-mono mt-0.5">
                        {h.equipmentId} &bull; {h.equipmentType}
                      </p>
                    </div>
                    <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-amber-500/20 text-amber-300 border border-amber-500/30">
                      {h.rarity} &bull; T{h.tier}
                    </span>
                  </div>

                  {/* Owning Clan & Custody */}
                  <div className="mt-3 text-xs space-y-1.5 text-slate-300">
                    <div className="flex items-center justify-between">
                      <span className="text-slate-400 text-[11px]">Owning Clan:</span>
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          openClanProfile(h.owningClanId);
                        }}
                        className="font-medium text-amber-400 hover:underline truncate max-w-[150px]"
                      >
                        {h.owningClanName}
                      </button>
                    </div>

                    <div className="flex items-center justify-between">
                      <span className="text-slate-400 text-[11px]">Custody Status:</span>
                      <span
                        className={`font-semibold text-[11px] ${
                          h.custodyStatus === 'IN_CLAN_VAULT'
                            ? 'text-sky-400'
                            : h.custodyStatus === 'BEARER_HELD'
                            ? 'text-emerald-400'
                            : 'text-rose-400'
                        }`}
                      >
                        {h.custodyStatus.replace('_', ' ')}
                      </span>
                    </div>

                    {h.currentBearerName && (
                      <div className="flex items-center justify-between">
                        <span className="text-slate-400 text-[11px]">Bearer:</span>
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            if (h.currentBearerId) openPersonProfile(parseInt(h.currentBearerId, 10));
                          }}
                          className="font-medium text-sky-400 hover:underline truncate max-w-[150px]"
                        >
                          {h.currentBearerName}
                        </button>
                      </div>
                    )}
                  </div>

                  {/* Durability */}
                  <div className="mt-3 pt-2 border-t border-slate-800 text-[11px] text-slate-400 flex items-center justify-between">
                    <span>Durability: {h.durability}/{h.maxDurability}</span>
                    <span className="text-emerald-400 font-semibold">{h.condition}</span>
                  </div>
                </div>

                {/* Footer Action */}
                <div className="mt-4 pt-3 border-t border-slate-800 flex items-center justify-between text-xs text-slate-400">
                  <span>Registered Y{h.registrationYear}</span>
                  <span className="font-semibold text-amber-400 hover:underline flex items-center gap-1">
                    Provenance & Provenance Graph &rarr;
                  </span>
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
            {Math.min((page + 1) * pageSize, data.totalElements)} of {data.totalElements} heirlooms
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

