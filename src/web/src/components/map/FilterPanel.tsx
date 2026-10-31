import React from 'react';
import { useMapStore } from '../../store/useMapStore';
import { Filter, X, RotateCcw } from 'lucide-react';

export const FilterPanel: React.FC = () => {
  const filters = useMapStore((s) => s.filters);
  const setFilter = useMapStore((s) => s.setFilter);
  const resetFilters = useMapStore((s) => s.resetFilters);
  const toggleFiltersPanel = useMapStore((s) => s.toggleFilters);

  return (
    <div className="absolute top-16 left-4 z-20 w-80 bg-slate-900/95 backdrop-blur-md border border-slate-700/80 rounded-xl p-3 shadow-2xl select-none animate-in fade-in duration-150">
      <div className="flex items-center justify-between pb-2 mb-2 border-b border-slate-800 text-xs">
        <div className="flex items-center gap-1.5 font-bold text-slate-100">
          <Filter className="w-3.5 h-3.5 text-cyan-400" />
          <span>ENTITY FILTERS</span>
        </div>
        <div className="flex items-center gap-1.5">
          <button
            onClick={resetFilters}
            className="text-[11px] text-cyan-400 hover:text-cyan-300 flex items-center gap-0.5"
            title="Reset Filters"
          >
            <RotateCcw className="w-3 h-3" />
            <span>Reset</span>
          </button>
          <button onClick={toggleFiltersPanel} className="text-slate-400 hover:text-slate-200">
            <X className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>

      <div className="space-y-3 text-xs">
        {/* Settlement Biome Filter */}
        <div>
          <label className="block text-[11px] font-semibold text-slate-300 mb-1">Settlement Biome</label>
          <select
            value={filters.settlementBiome}
            onChange={(e) => setFilter('settlementBiome', e.target.value)}
            className="w-full bg-slate-950 border border-slate-700 rounded-lg px-2 py-1 text-slate-200 text-xs focus:outline-none focus:border-cyan-500"
          >
            <option value="ALL">All Biomes</option>
            <option value="RIVER_VALLEY">River Valley</option>
            <option value="HIGHLAND_MINE">Highland Mine</option>
            <option value="FOREST">Forest</option>
            <option value="COASTAL">Coastal</option>
          </select>
        </div>

        {/* Settlement Tier Filter */}
        <div>
          <label className="block text-[11px] font-semibold text-slate-300 mb-1">Settlement Classification</label>
          <select
            value={filters.settlementTier}
            onChange={(e) => setFilter('settlementTier', e.target.value)}
            className="w-full bg-slate-950 border border-slate-700 rounded-lg px-2 py-1 text-slate-200 text-xs focus:outline-none focus:border-cyan-500"
          >
            <option value="ALL">All Tiers</option>
            <option value="HAMLET">Hamlet (&lt;50)</option>
            <option value="VILLAGE">Village (50-249)</option>
            <option value="TOWN">Town (250-999)</option>
            <option value="CITY">City (1,000-4,999)</option>
            <option value="MEGACITY">Megacity (5,000+)</option>
          </select>
        </div>

        {/* Min Population Slider */}
        <div>
          <div className="flex justify-between items-center mb-1">
            <label className="text-[11px] font-semibold text-slate-300">Min Population</label>
            <span className="font-mono text-cyan-400 font-bold">{filters.minPopulation}</span>
          </div>
          <input
            type="range"
            min="0"
            max="2000"
            step="50"
            value={filters.minPopulation}
            onChange={(e) => setFilter('minPopulation', parseInt(e.target.value, 10))}
            className="w-full accent-cyan-500 cursor-pointer h-1.5 bg-slate-800 rounded-lg"
          />
        </div>

        {/* Monster Lair Threat Filter */}
        <div>
          <label className="block text-[11px] font-semibold text-slate-300 mb-1">Monster Lair Threat Rank</label>
          <select
            value={filters.lairThreat}
            onChange={(e) => setFilter('lairThreat', e.target.value)}
            className="w-full bg-slate-950 border border-slate-700 rounded-lg px-2 py-1 text-slate-200 text-xs focus:outline-none focus:border-cyan-500"
          >
            <option value="ALL">All Threat Ranks</option>
            <option value="COMMON">Common</option>
            <option value="UNCOMMON">Uncommon</option>
            <option value="RARE">Rare</option>
            <option value="LEGENDARY">Legendary</option>
            <option value="MYTHIC">Mythic</option>
          </select>
        </div>

        {/* Anomaly Resource Grade Filter */}
        <div>
          <label className="block text-[11px] font-semibold text-slate-300 mb-1">Aether Anomaly Grade</label>
          <select
            value={filters.anomalyGrade}
            onChange={(e) => setFilter('anomalyGrade', e.target.value)}
            className="w-full bg-slate-950 border border-slate-700 rounded-lg px-2 py-1 text-slate-200 text-xs focus:outline-none focus:border-cyan-500"
          >
            <option value="ALL">All Grades</option>
            <option value="COMMON">Common</option>
            <option value="UNCOMMON">Uncommon</option>
            <option value="RARE">Rare</option>
            <option value="LEGENDARY">Legendary</option>
            <option value="MYTHIC">Mythic</option>
          </select>
        </div>
      </div>
    </div>
  );
};

