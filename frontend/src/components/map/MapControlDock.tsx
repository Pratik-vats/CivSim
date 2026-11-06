import React, { useState } from 'react';
import { useMapStore } from '../../store/useMapStore';
import {
  ZoomIn,
  ZoomOut,
  Maximize2,
  RotateCcw,
  Crosshair,
  Layers,
  Sparkles,
  Map as MapIcon,
  Eye,
  Sliders,
  Compass,
} from 'lucide-react';

interface MapControlDockProps {
  onZoomIn: () => void;
  onZoomOut: () => void;
  onFitWorld: () => void;
  onFocusCivilization: () => void;
  onResetView: () => void;
  onCenterSelected?: () => void;
  hasSelectedEntity: boolean;
  isAwakened: boolean;
}

export const MapControlDock: React.FC<MapControlDockProps> = ({
  onZoomIn,
  onZoomOut,
  onFitWorld,
  onFocusCivilization,
  onResetView,
  onCenterSelected,
  hasSelectedEntity,
  isAwakened,
}) => {
  const [layersOpen, setLayersOpen] = useState(false);
  const [overlaysOpen, setOverlaysOpen] = useState(false);

  const layers = useMapStore((s) => s.layers);
  const toggleLayer = useMapStore((s) => s.toggleLayer);
  const dynamicOverlays = useMapStore((s) => s.dynamicOverlays);
  const toggleDynamicOverlay = useMapStore((s) => s.toggleDynamicOverlay);
  const showMinimap = useMapStore((s) => s.showMinimap);
  const toggleMinimap = useMapStore((s) => s.toggleMinimap);
  const camera = useMapStore((s) => s.camera);

  return (
    <div className="absolute bottom-6 right-6 z-10 flex flex-col items-end gap-2">
      {/* Overlays Popover Panel */}
      {overlaysOpen && (
        <div className="w-56 p-3 rounded-xl bg-slate-950/95 backdrop-blur-md border border-slate-700 shadow-2xl text-xs space-y-2 text-slate-200 animate-in fade-in slide-in-from-bottom-2 duration-150">
          <div className="flex items-center justify-between pb-1.5 border-b border-slate-800 font-serif font-bold text-amber-400">
            <span className="flex items-center gap-1.5">
              <Sparkles className="w-3.5 h-3.5 text-cyan-400" /> Dynamic Overlays
            </span>
          </div>

          <div className="space-y-1">
            <label className="flex items-center justify-between p-1 rounded hover:bg-slate-800/60 cursor-pointer">
              <span className="text-slate-300">Territory Borders</span>
              <input
                type="checkbox"
                checked={dynamicOverlays.territory}
                onChange={() => toggleDynamicOverlay('territory')}
                className="rounded border-slate-700 text-cyan-500 focus:ring-0"
              />
            </label>

            <label className="flex items-center justify-between p-1 rounded hover:bg-slate-800/60 cursor-pointer">
              <span className="text-slate-300">Trade Corridors</span>
              <input
                type="checkbox"
                checked={dynamicOverlays.tradeRoutes}
                onChange={() => toggleDynamicOverlay('tradeRoutes')}
                className="rounded border-slate-700 text-amber-500 focus:ring-0"
              />
            </label>

            <label className="flex items-center justify-between p-1 rounded hover:bg-slate-800/60 cursor-pointer">
              <span className="text-slate-300">Diplomatic Feuds</span>
              <input
                type="checkbox"
                checked={dynamicOverlays.diplomacy}
                onChange={() => toggleDynamicOverlay('diplomacy')}
                className="rounded border-slate-700 text-rose-500 focus:ring-0"
              />
            </label>

            <label className="flex items-center justify-between p-1 rounded hover:bg-slate-800/60 cursor-pointer">
              <span className="text-slate-300">Monster Threat Map</span>
              <input
                type="checkbox"
                checked={dynamicOverlays.monsterThreat}
                onChange={() => toggleDynamicOverlay('monsterThreat')}
                className="rounded border-slate-700 text-rose-500 focus:ring-0"
              />
            </label>

            <label className="flex items-center justify-between p-1 rounded hover:bg-slate-800/60 cursor-pointer">
              <span className="text-slate-300">Aetheric Leylines</span>
              <input
                type="checkbox"
                checked={dynamicOverlays.aethericActivity}
                onChange={() => toggleDynamicOverlay('aethericActivity')}
                className="rounded border-slate-700 text-purple-500 focus:ring-0"
              />
            </label>

            <label className="flex items-center justify-between p-1 rounded hover:bg-slate-800/60 cursor-pointer">
              <span className="text-slate-300">Population Heatmap</span>
              <input
                type="checkbox"
                checked={dynamicOverlays.populationDensity}
                onChange={() => toggleDynamicOverlay('populationDensity')}
                className="rounded border-slate-700 text-emerald-500 focus:ring-0"
              />
            </label>
          </div>
        </div>
      )}

      {/* Layers Popover Panel */}
      {layersOpen && (
        <div className="w-56 p-3 rounded-xl bg-slate-950/95 backdrop-blur-md border border-slate-700 shadow-2xl text-xs space-y-2 text-slate-200 animate-in fade-in slide-in-from-bottom-2 duration-150">
          <div className="flex items-center justify-between pb-1.5 border-b border-slate-800 font-serif font-bold text-amber-400">
            <span className="flex items-center gap-1.5">
              <Layers className="w-3.5 h-3.5 text-amber-400" /> Map Cartography
            </span>
          </div>

          <div className="space-y-1">
            <label className="flex items-center justify-between p-1 rounded hover:bg-slate-800/60 cursor-pointer">
              <span className="text-slate-300">Settlements</span>
              <input
                type="checkbox"
                checked={layers.settlements}
                onChange={() => toggleLayer('settlements')}
                className="rounded border-slate-700 text-amber-500 focus:ring-0"
              />
            </label>

            <label className="flex items-center justify-between p-1 rounded hover:bg-slate-800/60 cursor-pointer">
              <span className="text-slate-300">Monster Lairs</span>
              <input
                type="checkbox"
                checked={layers.lairs}
                onChange={() => toggleLayer('lairs')}
                className="rounded border-slate-700 text-rose-500 focus:ring-0"
              />
            </label>

            <label className="flex items-center justify-between p-1 rounded hover:bg-slate-800/60 cursor-pointer">
              <span className="text-slate-300">Aether Anomalies</span>
              <input
                type="checkbox"
                checked={layers.anomalies}
                onChange={() => toggleLayer('anomalies')}
                className="rounded border-slate-700 text-cyan-500 focus:ring-0"
              />
            </label>

            <label className="flex items-center justify-between p-1 rounded hover:bg-slate-800/60 cursor-pointer">
              <span className="text-slate-300">Expeditions</span>
              <input
                type="checkbox"
                checked={layers.expeditions}
                onChange={() => toggleLayer('expeditions')}
                className="rounded border-slate-700 text-amber-500 focus:ring-0"
              />
            </label>

            <label className="flex items-center justify-between p-1 rounded hover:bg-slate-800/60 cursor-pointer">
              <span className="text-slate-300">Biome Shading</span>
              <input
                type="checkbox"
                checked={layers.biomeBackground}
                onChange={() => toggleLayer('biomeBackground')}
                className="rounded border-slate-700 text-emerald-500 focus:ring-0"
              />
            </label>

            <label className="flex items-center justify-between p-1 rounded hover:bg-slate-800/60 cursor-pointer">
              <span className="text-slate-300">Settlement Labels</span>
              <input
                type="checkbox"
                checked={layers.labels}
                onChange={() => toggleLayer('labels')}
                className="rounded border-slate-700 text-sky-500 focus:ring-0"
              />
            </label>

            <label className="flex items-center justify-between p-1 rounded hover:bg-slate-800/60 cursor-pointer">
              <span className="text-slate-300">Coordinate Grid</span>
              <input
                type="checkbox"
                checked={layers.grid}
                onChange={() => toggleLayer('grid')}
                className="rounded border-slate-700 text-slate-500 focus:ring-0"
              />
            </label>
          </div>
        </div>
      )}

      {/* Floating Dock Bar */}
      <div className="flex items-center gap-1.5 p-1.5 rounded-xl bg-slate-950/85 backdrop-blur-md border border-slate-700/80 shadow-2xl text-slate-200">
        <button
          onClick={onZoomIn}
          className="p-2 hover:bg-slate-800/80 active:bg-slate-700 rounded-lg transition text-slate-200 hover:text-white"
          title="Zoom In (+)"
        >
          <ZoomIn className="w-4 h-4" />
        </button>

        <button
          onClick={onZoomOut}
          className="p-2 hover:bg-slate-800/80 active:bg-slate-700 rounded-lg transition text-slate-200 hover:text-white"
          title="Zoom Out (-)"
        >
          <ZoomOut className="w-4 h-4" />
        </button>

        <button
          onClick={onFitWorld}
          className="p-2 hover:bg-slate-800/80 active:bg-slate-700 rounded-lg transition text-slate-200 hover:text-white"
          title="Fit Entire Continent"
        >
          <Maximize2 className="w-4 h-4" />
        </button>

        <button onClick={onFocusCivilization} className="p-2 hover:bg-emerald-900/50 rounded-lg transition text-emerald-300" title="Focus Civilization">
          <Compass className="w-4 h-4" />
        </button>

        <button
          onClick={onResetView}
          className="p-2 hover:bg-slate-800/80 active:bg-slate-700 rounded-lg transition text-slate-200 hover:text-white"
          title="Reset Camera"
        >
          <RotateCcw className="w-4 h-4" />
        </button>

        {hasSelectedEntity && onCenterSelected && (
          <button
            onClick={onCenterSelected}
            className="p-2 hover:bg-cyan-900/50 active:bg-cyan-800 text-cyan-400 rounded-lg transition"
            title="Center Selected Entity"
          >
            <Crosshair className="w-4 h-4" />
          </button>
        )}

        <div className="w-[1px] h-5 bg-slate-800 mx-0.5" />

        <button
          onClick={() => {
            setLayersOpen((prev) => !prev);
            setOverlaysOpen(false);
          }}
          className={`p-2 rounded-lg transition ${
            layersOpen ? 'bg-amber-500/20 text-amber-300' : 'hover:bg-slate-800/80 text-slate-300'
          }`}
          title="Toggle Map Layers"
        >
          <Layers className="w-4 h-4" />
        </button>

        <button
          onClick={() => {
            setOverlaysOpen((prev) => !prev);
            setLayersOpen(false);
          }}
          className={`p-2 rounded-lg transition ${
            overlaysOpen ? 'bg-cyan-500/20 text-cyan-300' : 'hover:bg-slate-800/80 text-slate-300'
          }`}
          title="Toggle Dynamic Overlays"
        >
          <Sparkles className="w-4 h-4" />
        </button>

        <button
          onClick={toggleMinimap}
          className={`p-2 rounded-lg transition ${
            showMinimap ? 'bg-indigo-500/20 text-indigo-300' : 'hover:bg-slate-800/80 text-slate-300'
          }`}
          title="Toggle Strategic Minimap"
        >
          <MapIcon className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
};

