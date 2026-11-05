import React, { useState } from 'react';
import { useMapStore, MapLayers, DynamicOverlaysConfig } from '../../store/useMapStore';
import { Layers, Check, X, Flame, Eye, Activity, Sliders, Zap } from 'lucide-react';

export const LayerControl: React.FC = () => {
  const [tab, setTab] = useState<'LAYERS' | 'OVERLAYS' | 'PREFS'>('OVERLAYS');

  const layers = useMapStore((s) => s.layers);
  const toggleLayer = useMapStore((s) => s.toggleLayer);
  const toggleLayersPanel = useMapStore((s) => s.toggleLayers);

  const dynamicOverlays = useMapStore((s) => s.dynamicOverlays);
  const toggleDynamicOverlay = useMapStore((s) => s.toggleDynamicOverlay);
  const heatmapOpacity = useMapStore((s) => s.heatmapOpacity);
  const setHeatmapOpacity = useMapStore((s) => s.setHeatmapOpacity);

  const enableAnimations = useMapStore((s) => s.enableAnimations);
  const setEnableAnimations = useMapStore((s) => s.setEnableAnimations);
  const reducedMotion = useMapStore((s) => s.reducedMotion);
  const setReducedMotion = useMapStore((s) => s.setReducedMotion);
  const animationSpeed = useMapStore((s) => s.animationSpeed);
  const setAnimationSpeed = useMapStore((s) => s.setAnimationSpeed);

  const layerItems: { key: keyof MapLayers; label: string; desc: string }[] = [
    { key: 'settlements', label: 'Settlements', desc: 'Villages, towns, and cities' },
    { key: 'lairs', label: 'Monster Lairs', desc: 'Active monster spawn points' },
    { key: 'anomalies', label: 'Aether Anomalies', desc: 'Supernatural resource deposits' },
    { key: 'expeditions', label: 'Expeditions', desc: 'Trade & exploration parties' },
    { key: 'territoryRadius', label: 'Danger Rings', desc: 'Monster lair territory radius' },
    { key: 'biomeBackground', label: 'Biome Visuals', desc: 'Terrain ambient influences' },
    { key: 'grid', label: 'Coordinate Grid', desc: 'World coordinate reference lines' },
    { key: 'labels', label: 'Entity Labels', desc: 'Names and population badges' },
  ];

  const overlayItems: { key: keyof DynamicOverlaysConfig; label: string; desc: string; color: string }[] = [
    { key: 'territory', label: 'Settlement Territories', desc: 'Clan influence and borders', color: 'text-sky-400' },
    { key: 'diplomacy', label: 'Diplomacy & Border Feuds', desc: 'Alliances and war frontiers', color: 'text-rose-400' },
    { key: 'tradeRoutes', label: 'Caravan Trade Corridors', desc: 'Animated commercial flows', color: 'text-amber-400' },
    { key: 'monsterThreat', label: 'Monster Threat Heatmap', desc: 'Lair danger & invasion vectors', color: 'text-rose-500' },
    { key: 'aethericActivity', label: 'Aetheric Activity Heatmap', desc: 'Supernatural anomaly energy', color: 'text-purple-400' },
    { key: 'populationDensity', label: 'Population Density Heatmap', desc: 'Demographic concentration', color: 'text-emerald-400' },
    { key: 'prosperity', label: 'Economic Prosperity Heatmap', desc: 'Liquid wealth and trade treasuries', color: 'text-yellow-400' },
    { key: 'activeConflicts', label: 'Active Battle Clash Icons', desc: 'Live tactical combat sites', color: 'text-red-400' },
  ];

  return (
    <div className="absolute top-16 left-4 z-20 w-80 bg-slate-900/95 backdrop-blur-md border border-slate-700/80 rounded-xl p-3 shadow-2xl select-none animate-in fade-in duration-150">
      {/* Header */}
      <div className="flex items-center justify-between pb-2 mb-2 border-b border-slate-800 text-xs">
        <div className="flex items-center gap-1.5 font-bold text-slate-100">
          <Layers className="w-3.5 h-3.5 text-cyan-400" />
          <span>VISUALIZATION CONTROLS</span>
        </div>
        <button onClick={toggleLayersPanel} className="text-slate-400 hover:text-slate-200">
          <X className="w-3.5 h-3.5" />
        </button>
      </div>

      {/* Tabs */}
      <div className="grid grid-cols-3 gap-1 p-1 bg-slate-950 rounded-lg mb-2.5 text-[11px] font-semibold">
        <button
          onClick={() => setTab('OVERLAYS')}
          className={`py-1 rounded text-center transition ${
            tab === 'OVERLAYS' ? 'bg-cyan-900/60 text-cyan-300 border border-cyan-700/50' : 'text-slate-400 hover:text-slate-200'
          }`}
        >
          Overlays
        </button>
        <button
          onClick={() => setTab('LAYERS')}
          className={`py-1 rounded text-center transition ${
            tab === 'LAYERS' ? 'bg-cyan-900/60 text-cyan-300 border border-cyan-700/50' : 'text-slate-400 hover:text-slate-200'
          }`}
        >
          Entities
        </button>
        <button
          onClick={() => setTab('PREFS')}
          className={`py-1 rounded text-center transition ${
            tab === 'PREFS' ? 'bg-cyan-900/60 text-cyan-300 border border-cyan-700/50' : 'text-slate-400 hover:text-slate-200'
          }`}
        >
          Motion
        </button>
      </div>

      {/* Tab 1: Dynamic Overlays & Heatmaps */}
      {tab === 'OVERLAYS' && (
        <div className="space-y-2">
          {/* Heatmap Opacity Slider */}
          <div className="p-2 rounded-lg bg-slate-950/60 border border-slate-800">
            <div className="flex items-center justify-between text-[11px] mb-1">
              <span className="text-slate-300 font-medium flex items-center gap-1">
                <Sliders className="w-3 h-3 text-cyan-400" /> Heatmap Intensity
              </span>
              <span className="font-mono text-cyan-400 font-bold">{Math.round(heatmapOpacity * 100)}%</span>
            </div>
            <input
              type="range"
              min={0.1}
              max={1.0}
              step={0.05}
              value={heatmapOpacity}
              onChange={(e) => setHeatmapOpacity(parseFloat(e.target.value))}
              className="w-full h-1.5 bg-slate-800 rounded-lg appearance-none cursor-pointer accent-cyan-400"
            />
          </div>

          {/* Overlay Toggles */}
          <div className="space-y-1 max-h-56 overflow-y-auto scrollbar-thin pr-1">
            {overlayItems.map((item) => {
              const active = dynamicOverlays[item.key];
              return (
                <div
                  key={item.key}
                  onClick={() => toggleDynamicOverlay(item.key)}
                  className="flex items-center justify-between p-1.5 rounded-lg hover:bg-slate-800/60 cursor-pointer transition"
                >
                  <div>
                    <div className={`text-xs font-medium ${item.color}`}>{item.label}</div>
                    <div className="text-[10px] text-slate-400">{item.desc}</div>
                  </div>
                  <div
                    className={`w-4 h-4 rounded border flex items-center justify-center transition shrink-0 ${
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
      )}

      {/* Tab 2: Standard Map Layers */}
      {tab === 'LAYERS' && (
        <div className="space-y-1 max-h-64 overflow-y-auto scrollbar-thin pr-1">
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
                  className={`w-4 h-4 rounded border flex items-center justify-center transition shrink-0 ${
                    active ? 'bg-cyan-600 border-cyan-400 text-white' : 'border-slate-600 bg-slate-800/40'
                  }`}
                >
                  {active && <Check className="w-3 h-3 stroke-[3]" />}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Tab 3: Motion & Accessibility Preferences */}
      {tab === 'PREFS' && (
        <div className="space-y-3 p-1 text-xs">
          {/* Animations Toggle */}
          <div
            onClick={() => setEnableAnimations(!enableAnimations)}
            className="flex items-center justify-between p-2 rounded-lg bg-slate-950/60 border border-slate-800 cursor-pointer hover:bg-slate-800/60 transition"
          >
            <div>
              <div className="font-semibold text-slate-200">Animated Map Activity</div>
              <div className="text-[10px] text-slate-400">Expeditions, trade pulses & invasion waves</div>
            </div>
            <div
              className={`w-4 h-4 rounded border flex items-center justify-center transition ${
                enableAnimations ? 'bg-cyan-600 border-cyan-400 text-white' : 'border-slate-600 bg-slate-800/40'
              }`}
            >
              {enableAnimations && <Check className="w-3 h-3 stroke-[3]" />}
            </div>
          </div>

          {/* Reduced Motion Toggle */}
          <div
            onClick={() => setReducedMotion(!reducedMotion)}
            className="flex items-center justify-between p-2 rounded-lg bg-slate-950/60 border border-slate-800 cursor-pointer hover:bg-slate-800/60 transition"
          >
            <div>
              <div className="font-semibold text-slate-200">Reduced Motion Mode</div>
              <div className="text-[10px] text-slate-400">Static geometry, disable spinning/pulsing</div>
            </div>
            <div
              className={`w-4 h-4 rounded border flex items-center justify-center transition ${
                reducedMotion ? 'bg-cyan-600 border-cyan-400 text-white' : 'border-slate-600 bg-slate-800/40'
              }`}
            >
              {reducedMotion && <Check className="w-3 h-3 stroke-[3]" />}
            </div>
          </div>

          {/* Animation Speed */}
          <div className="p-2 rounded-lg bg-slate-950/60 border border-slate-800 space-y-1.5">
            <span className="font-semibold text-slate-300 block text-[11px]">Animation Velocity</span>
            <div className="grid grid-cols-3 gap-1">
              {[0.5, 1.0, 2.0].map((spd) => (
                <button
                  key={spd}
                  onClick={() => setAnimationSpeed(spd)}
                  className={`py-1 rounded text-center text-xs font-mono font-bold transition ${
                    animationSpeed === spd
                      ? 'bg-cyan-600 text-white'
                      : 'bg-slate-900 text-slate-400 hover:text-slate-200'
                  }`}
                >
                  {spd}x
                </button>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
