import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { fetchWorldSummary, fetchWorldMap, fetchSimulationStatus } from './services/api';
import { useSimulationSSE } from './hooks/useSimulationSSE';
import { useMapStore } from './store/useMapStore';
import { TopBar } from './components/header/TopBar';
import { Sidebar } from './components/sidebar/Sidebar';
import { WorldMapWorkspace } from './components/map/WorldMapWorkspace';
import { SearchBar } from './components/map/SearchBar';
import { LayerControl } from './components/map/LayerControl';
import { FilterPanel } from './components/map/FilterPanel';
import { MapLegend } from './components/map/MapLegend';
import { InspectionDrawer } from './components/drawer/InspectionDrawer';
import { AlertCircle, RefreshCw } from 'lucide-react';

export const App: React.FC = () => {
  const [currentModule, setCurrentModule] = useState('world-map');

  // SSE Live Notification Feed
  const sseState = useSimulationSSE();

  // Queries for authoritative state
  const {
    data: summary,
    isLoading: summaryLoading,
    error: summaryError,
    refetch: refetchSummary,
  } = useQuery({
    queryKey: ['world-summary'],
    queryFn: fetchWorldSummary,
    refetchInterval: 2500,
  });

  const {
    data: mapSnapshot,
    isLoading: mapLoading,
    error: mapError,
    refetch: refetchMap,
  } = useQuery({
    queryKey: ['world-map'],
    queryFn: fetchWorldMap,
    refetchInterval: 2500,
  });

  const { data: simStatus } = useQuery({
    queryKey: ['simulation-status'],
    queryFn: fetchSimulationStatus,
    refetchInterval: 1500,
  });

  const showLayers = useMapStore((s) => s.showLayers);
  const showFilters = useMapStore((s) => s.showFilters);
  const showLegend = useMapStore((s) => s.showLegend);
  const setCamera = useMapStore((s) => s.setCamera);

  // Era Detection: derived from backend response
  const isAwakened =
    summary?.currentEra === 'Awakening Era' ||
    (summary?.year !== undefined && summary.year >= 50);

  // Loading state
  if ((summaryLoading || mapLoading) && !mapSnapshot) {
    return (
      <div className="h-screen w-screen flex flex-col items-center justify-center bg-[#07090e] text-slate-100 font-sans select-none">
        <div className="w-12 h-12 rounded-full border-4 border-cyan-500/20 border-t-cyan-400 animate-spin mb-4" />
        <h2 className="text-base font-bold tracking-wider uppercase">Loading Civilization World...</h2>
        <p className="text-xs text-slate-500 font-mono mt-1">Connecting to Java Simulation Engine</p>
      </div>
    );
  }

  // Error state
  if ((summaryError || mapError) && !mapSnapshot) {
    return (
      <div className="h-screen w-screen flex flex-col items-center justify-center bg-[#07090e] text-slate-100 p-6 text-center select-none">
        <div className="p-4 rounded-2xl bg-rose-950/40 border border-rose-800/60 mb-4 text-rose-400">
          <AlertCircle className="w-8 h-8" />
        </div>
        <h2 className="text-lg font-bold">Simulation Backend Unavailable</h2>
        <p className="text-xs text-slate-400 max-w-md mt-1 mb-4">
          Could not communicate with the MIE backend service. Ensure the Java simulation server is running on port 8080.
        </p>
        <button
          onClick={() => {
            refetchSummary();
            refetchMap();
          }}
          className="px-4 py-2 rounded-xl bg-cyan-600 hover:bg-cyan-500 text-white font-semibold text-xs flex items-center gap-2 transition shadow-lg"
        >
          <RefreshCw className="w-4 h-4" />
          <span>Retry Connection</span>
        </button>
      </div>
    );
  }

  return (
    <div
      className={`h-screen w-screen flex flex-col overflow-hidden font-sans transition-colors duration-500 ${
        isAwakened ? 'bg-[#090d16] text-slate-100' : 'bg-[#151310] text-[#f2e8dc]'
      }`}
    >
      {/* 1. World Top Status Bar */}
      <TopBar
        summary={summary}
        status={simStatus}
        sseConnected={sseState.connected}
        isAwakened={isAwakened}
      />

      {/* 2. Main Workspace Layout */}
      <div className="flex-1 flex overflow-hidden relative">
        {/* Left Sidebar */}
        <Sidebar
          currentModule={currentModule}
          onSelectModule={setCurrentModule}
          isAwakened={isAwakened}
        />

        {/* Map Workspace */}
        <main className="flex-1 relative overflow-hidden flex flex-col">
          {/* Floating Search Bar (Top Left over map) */}
          <div className="absolute top-4 left-4 z-10">
            <SearchBar
              onCenterEntity={(wx, wy) => {
                // Handled via centerEntity logic in workspace
                const workspaceEl = document.querySelector('canvas');
                if (workspaceEl) {
                  const boundsW = mapSnapshot?.width || 120;
                  const boundsH = mapSnapshot?.height || 120;
                  const baseScale = Math.min((window.innerWidth - 300) / boundsW, (window.innerHeight - 150) / boundsH);
                  const newPanX = (window.innerWidth - 300) / 2 - wx * baseScale;
                  const newPanY = (window.innerHeight - 150) / 2 - wy * baseScale;
                  setCamera({ panX: newPanX, panY: newPanY, zoom: 1.5 });
                }
              }}
            />
          </div>

          {/* Map Overlay Panels */}
          {showLayers && <LayerControl />}
          {showFilters && <FilterPanel />}
          {showLegend && <MapLegend />}

          {/* Interactive World Map */}
          {mapSnapshot && (
            <WorldMapWorkspace mapSnapshot={mapSnapshot} isAwakened={isAwakened} />
          )}

          {/* Inspection Drawer */}
          {mapSnapshot && (
            <InspectionDrawer mapSnapshot={mapSnapshot} isAwakened={isAwakened} />
          )}
        </main>
      </div>
    </div>
  );
};

