import React from 'react';
import { useMapStore } from '../../store/useMapStore';
import { HelpCircle, X } from 'lucide-react';

export const MapLegend: React.FC = () => {
  const toggleLegend = useMapStore((s) => s.toggleLegend);

  return (
    <div className="absolute top-16 left-4 z-20 w-80 bg-slate-900/95 backdrop-blur-md border border-slate-700/80 rounded-xl p-3 shadow-2xl select-none animate-in fade-in duration-150">
      <div className="flex items-center justify-between pb-2 mb-2 border-b border-slate-800 text-xs">
        <div className="flex items-center gap-1.5 font-bold text-slate-100">
          <HelpCircle className="w-3.5 h-3.5 text-cyan-400" />
          <span>MAP LEGEND</span>
        </div>
        <button onClick={toggleLegend} className="text-slate-400 hover:text-slate-200">
          <X className="w-3.5 h-3.5" />
        </button>
      </div>

      <div className="space-y-2.5 text-[11px] max-h-96 overflow-y-auto pr-1">
        {/* Settlement Tiers */}
        <div>
          <div className="font-semibold text-slate-300 mb-1 uppercase tracking-wider text-[10px]">Settlements</div>
          <div className="grid grid-cols-2 gap-1.5">
            <div className="flex items-center gap-2">
              <span className="w-3 h-3 rounded-full bg-blue-500 border border-blue-300" />
              <span>Hamlet (&lt;50)</span>
            </div>
            <div className="flex items-center gap-2">
              <span className="w-3.5 h-3.5 rounded-full bg-emerald-500 border border-emerald-300" />
              <span>Village (50+)</span>
            </div>
            <div className="flex items-center gap-2">
              <span className="w-4 h-4 rounded-full bg-purple-500 border border-purple-300" />
              <span>Town (250+)</span>
            </div>
            <div className="flex items-center gap-2">
              <span className="w-4.5 h-4.5 rounded-full bg-amber-500 border border-amber-300" />
              <span>City (1k+)</span>
            </div>
            <div className="flex items-center gap-2 col-span-2">
              <span className="w-5 h-5 rounded-full bg-pink-500 border border-pink-300" />
              <span>Megacity (5k+)</span>
            </div>
          </div>
        </div>

        {/* Biome Types */}
        <div className="border-t border-slate-800/80 pt-2">
          <div className="font-semibold text-slate-300 mb-1 uppercase tracking-wider text-[10px]">Biomes</div>
          <div className="grid grid-cols-2 gap-1.5 text-slate-400">
            <div className="flex items-center gap-2">
              <span className="w-2.5 h-2.5 rounded bg-green-500/70" />
              <span>River Valley</span>
            </div>
            <div className="flex items-center gap-2">
              <span className="w-2.5 h-2.5 rounded bg-amber-600/70" />
              <span>Highland Mine</span>
            </div>
            <div className="flex items-center gap-2">
              <span className="w-2.5 h-2.5 rounded bg-emerald-700/70" />
              <span>Forest</span>
            </div>
            <div className="flex items-center gap-2">
              <span className="w-2.5 h-2.5 rounded bg-sky-600/70" />
              <span>Coastal</span>
            </div>
          </div>
        </div>

        {/* Monster Lairs */}
        <div className="border-t border-slate-800/80 pt-2">
          <div className="font-semibold text-slate-300 mb-1 uppercase tracking-wider text-[10px]">Monster Lairs</div>
          <div className="grid grid-cols-2 gap-1.5">
            <div className="flex items-center gap-2">
              <span className="w-2.5 h-2.5 rotate-45 bg-slate-400" />
              <span>Common</span>
            </div>
            <div className="flex items-center gap-2">
              <span className="w-2.5 h-2.5 rotate-45 bg-green-500" />
              <span>Uncommon</span>
            </div>
            <div className="flex items-center gap-2">
              <span className="w-2.5 h-2.5 rotate-45 bg-blue-500" />
              <span>Rare</span>
            </div>
            <div className="flex items-center gap-2">
              <span className="w-2.5 h-2.5 rotate-45 bg-amber-500" />
              <span>Legendary</span>
            </div>
            <div className="flex items-center gap-2 col-span-2">
              <span className="w-3 h-3 rotate-45 bg-rose-500 border border-white" />
              <span className="text-rose-400 font-semibold">Mythic / Alpha Lair</span>
            </div>
          </div>
        </div>

        {/* Supernatural Anomalies & Expeditions */}
        <div className="border-t border-slate-800/80 pt-2">
          <div className="font-semibold text-slate-300 mb-1 uppercase tracking-wider text-[10px]">Anomalies & Routes</div>
          <div className="space-y-1 text-slate-400">
            <div className="flex items-center gap-2">
              <span className="w-3 h-3 rotate-45 bg-cyan-400" />
              <span>Aether Deposit (Observable)</span>
            </div>
            <div className="flex items-center gap-2">
              <span className="w-4 border-b border-dashed border-amber-400" />
              <span>Active Expedition Supply Route</span>
            </div>
            <div className="flex items-center gap-2">
              <span className="w-3 h-3 rounded-full border border-dashed border-rose-500" />
              <span>Lair Hazard Territory Radius</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

