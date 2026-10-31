import React from 'react';
import { useMapStore, MapLayers } from '../../store/useMapStore';
import { Layers, Check, X } from 'lucide-react';

export const LayerControl: React.FC = () => {
  const layers = useMapStore((s) => s.layers);
  const toggleLayer = useMapStore((s) => s.toggleLayer);
  const toggleLayersPanel = useMapStore((s) => s.toggleLayers);

  const layerItems: { key: keyof MapLayers; label: string; desc: string }[] = [
    { key: 'settlements', label: 'Settlements', desc: 'Villages, towns, and cities' },
    { key: 'lairs', label: 'Monster Lairs', desc: 'Active monster spawn points' },
    { key: 'anomalies', label: 'Aether Anomalies', desc: 'Observable supernatural resource deposits' },
    { key: 'expeditions', label: 'Expeditions', desc: 'Active trade and exploration parties' },
    { key: 'territoryRadius', label: 'Territory Danger Rings', desc: 'Hazard radius around monster lairs' },
    { key: 'biomeBackground', label: 'Biome Visuals', desc: 'Terrain ambient influences' },
    { key: 'grid', label: 'Coordinate Grid', desc: 'World coordinate reference lines' },
    { key: 'labels', label: 'Entity Labels', desc: 'Names and population badges' },
  ];

  return (
    <div className="absolute top-16 left-4 z-20 w-72 bg-slate-900/95 backdrop-blur-md border border-slate-700/80 rounded-xl p-3 shadow-2xl select-none animate-in fade-in duration-150">
      <div className="flex items-center justify-between pb-2 mb-2 border-b border-slate-800 text-xs">
        <div className="flex items-center gap-1.5 font-bold text-slate-100">
          <Layers className="w-3.5 h-3.5 text-cyan-400" />
          <span>MAP LAYERS</span>
        </div>
        <button onClick={toggleLayersPanel} className="text-slate-400 hover:text-slate-200">
          <X className="w-3.5 h-3.5" />
        </button>
      </div>

      <div className="space-y-1.5">
        {layerItems.map((item) => {
          const active = layers[item.key];
          return (
            <div
              key={item.key}
              onClick={() => toggleLayer(item.key)}
              className="flex items-center justify-between p-1.5 rounded-lg hover:bg-slate-800/60 cursor-pointer transition"
            >
              <div>
                <div className="text-xs font-medium text-slate-200">{item.label}</div>
                <div className="text-[10px] text-slate-400">{item.desc}</div>
              </div>
              <div
                className={`w-4 h-4 rounded border flex items-center justify-center transition ${
                  active ? 'bg-cyan-600 border-cyan-400 text-white' : 'border-slate-600 bg-slate-800/40'
                }`}
              >
                {active && <Check className="w-3 h-3 stroke-[3]" />}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

