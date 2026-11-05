import React, { useState, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  fetchWorldSummary,
  fetchWorldMap,
  fetchSimulationStatus,
  fetchHistoricalMap,
  fetchHistoricalSummary,
  fetchWorldOverlays,
  fetchHistoricalOverlays,
} from './services/api';
import { useSimulationSSE } from './hooks/useSimulationSSE';
import { useMapStore, NavigationModule } from './store/useMapStore';
import { TopBar } from './components/header/TopBar';
import { Sidebar } from './components/sidebar/Sidebar';
import { WorldMapWorkspace } from './components/map/WorldMapWorkspace';
import { SearchBar } from './components/map/SearchBar';
import { LayerControl } from './components/map/LayerControl';
import { FilterPanel } from './components/map/FilterPanel';
import { MapLegend } from './components/map/MapLegend';
import { InspectionDrawer } from './components/drawer/InspectionDrawer';

// Phase 3.3 Explorer Modules & Modals
import { PeopleExplorer } from './components/people/PeopleExplorer';
import { PersonProfileModal } from './components/people/PersonProfileModal';
import { FamilyDynastyExplorer } from './components/families/FamilyDynastyExplorer';
import { FamilyTreeModal } from './components/families/FamilyTreeModal';
import { OrganizationExplorer } from './components/organizations/OrganizationExplorer';
import { ClanDetailModal } from './components/organizations/ClanDetailModal';
import { HeirloomExplorer } from './components/heirlooms/HeirloomExplorer';
import { HeirloomDetailModal } from './components/heirlooms/HeirloomDetailModal';
import { LeaderboardView } from './components/leaderboards/LeaderboardView';
import { EquipmentDetailModal } from './components/equipment/EquipmentDetailModal';
import { ComparisonModal } from './components/comparison/ComparisonModal';

// Phase 3.4 Modules & Modals
import { WorldActivityFeed } from './components/activity/WorldActivityFeed';
import { EventDetailModal } from './components/activity/EventDetailModal';
import { HistoricalChronicleView } from './components/chronicle/HistoricalChronicleView';
import { BattleExplorer } from './components/battles/BattleExplorer';
import { BattleDetailModal } from './components/battles/BattleDetailModal';
import { MonsterTideTracker } from './components/tides/MonsterTideTracker';
import { MonsterTideDetailModal } from './components/tides/MonsterTideDetailModal';
import { ExpeditionExplorer } from './components/expeditions/ExpeditionExplorer';
import { ExpeditionDetailModal } from './components/expeditions/ExpeditionDetailModal';
import { AnomalyActivityView } from './components/anomalies/AnomalyActivityView';
import { AnomalyDetailModal } from './components/anomalies/AnomalyDetailModal';
import { BloodFeudLedger } from './components/feuds/BloodFeudLedger';
import { FeudDetailModal } from './components/feuds/FeudDetailModal';

// Phase 3.5 Time Travel, Persistence & Branching Components
import { HistoricalTimelineScrubber } from './components/timetravel/HistoricalTimelineScrubber';
import { HistoricalComparisonModal } from './components/timetravel/HistoricalComparisonModal';
import { SaveManagerModal } from './components/saves/SaveManagerModal';
import { BranchManagerModal } from './components/branches/BranchManagerModal';
import { NewWorldModal } from './components/worlds/NewWorldModal';

import { AlertCircle, RefreshCw } from 'lucide-react';

export const App: React.FC = () => {
  const activeModule = useMapStore((s) => s.activeModule);
  const setActiveModule = useMapStore((s) => s.setActiveModule);

  const [currentModule, setCurrentModule] = useState<string>('world-map');

  // Synchronize store and local state
  useEffect(() => {
    if (activeModule === 'map' && currentModule !== 'world-map') {
      setCurrentModule('world-map');
    } else if (activeModule !== 'map' && currentModule !== activeModule) {
      setCurrentModule(activeModule);
    }
  }, [activeModule]);

  const handleSelectModule = (mod: string) => {
    setCurrentModule(mod);
    if (mod === 'world-map') {
      setActiveModule('map');
    } else {
      setActiveModule(mod as NavigationModule);
    }
  };

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

  // Phase 3.5 Historical Queries & Adapters
  const timeTravelMode = useMapStore((s) => s.timeTravelMode);
  const historicalYear = useMapStore((s) => s.historicalYear);
  const historicalMonth = useMapStore((s) => s.historicalMonth);
  const isHistorical = timeTravelMode === 'HISTORICAL';

  const { data: historicalMapData } = useQuery({
    queryKey: ['historical-map', historicalYear, historicalMonth],
    queryFn: () => fetchHistoricalMap(historicalYear!, historicalMonth ?? 0),
    enabled: isHistorical && historicalYear !== null,
  });

  const { data: historicalSummaryData } = useQuery({
    queryKey: ['historical-summary', historicalYear, historicalMonth],
    queryFn: () => fetchHistoricalSummary(historicalYear!, historicalMonth ?? 0),
    enabled: isHistorical && historicalYear !== null,
  });

  // Phase 3.6 World Overlays & Dynamic Heatmaps
  const { data: liveOverlays } = useQuery({
    queryKey: ['world-overlays'],
    queryFn: fetchWorldOverlays,
    refetchInterval: 2500,
    enabled: !isHistorical,
  });

  const { data: historicalOverlays } = useQuery({
    queryKey: ['historical-overlays', historicalYear, historicalMonth],
    queryFn: () => fetchHistoricalOverlays(historicalYear!, historicalMonth ?? 0),
    enabled: isHistorical && historicalYear !== null,
  });

  const activeOverlays = isHistorical ? historicalOverlays : liveOverlays;

  // Adaptive map snapshot for live or historical visualization
  const activeMapSnapshot =
    isHistorical && historicalMapData
      ? {
          worldId: historicalMapData.worldId,
          snapshotVersion: -1,
          tickId: -1,
          year: historicalMapData.year,
          month: historicalMapData.month,
          width: mapSnapshot?.width || 120,
          height: mapSnapshot?.height || 120,
          settlements: historicalMapData.settlements,
          lairs: historicalMapData.lairs,
          anomalies: historicalMapData.anomalies,
          expeditions: historicalMapData.expeditions,
        }
      : mapSnapshot;

  // Adaptive summary for live or historical status
  const activeSummary =
    isHistorical && historicalSummaryData
      ? {
          worldId: historicalSummaryData.worldId,
          year: historicalSummaryData.year,
          month: historicalSummaryData.month,
          totalMonths: historicalSummaryData.year * 12 + historicalSummaryData.month,
          status: 'HISTORICAL_VIEW',
          humanPopulation: historicalSummaryData.humanPopulation,
          livingMalePopulation: Math.round(historicalSummaryData.humanPopulation / 2),
          livingFemalePopulation: Math.round(historicalSummaryData.humanPopulation / 2),
          settlementCount: historicalSummaryData.settlementCount,
          activeOrganizationCount: historicalSummaryData.activeOrganizationCount,
          awakenedPopulation: historicalSummaryData.awakenedPopulation,
          livingMonsterCount: historicalSummaryData.livingMonsterCount,
          activeMonsterLairCount: historicalSummaryData.activeMonsterLairCount,
          totalFoodStockpile: historicalSummaryData.totalFoodStockpile,
          totalWoodStockpile: historicalSummaryData.totalWoodStockpile,
          totalOreStockpile: historicalSummaryData.totalOreStockpile,
          totalToolsStockpile: historicalSummaryData.totalToolsStockpile,
          totalCitizenWealth: 0,
          totalCivicTreasury: 0,
          currentEra: historicalSummaryData.currentEra,
        }
      : summary;

  const showLayers = useMapStore((s) => s.showLayers);
  const showFilters = useMapStore((s) => s.showFilters);
  const showLegend = useMapStore((s) => s.showLegend);
  const setCamera = useMapStore((s) => s.setCamera);

  // Era Detection: derived from backend response
  const isAwakened =
    activeSummary?.currentEra === 'Awakening Era' ||
    (activeSummary?.year !== undefined && activeSummary.year >= 50);

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
          Could not communicate with the MIE backend service. Ensure the Java simulation server is running on port 8085.
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
        summary={activeSummary}
        status={simStatus}
        sseConnected={sseState.connected}
        isAwakened={isAwakened}
      />

      {/* 2. Main Workspace Layout */}
      <div className="flex-1 flex overflow-hidden relative">
        {/* Left Sidebar */}
        <Sidebar
          currentModule={currentModule}
          onSelectModule={handleSelectModule}
          isAwakened={isAwakened}
        />

        {/* Dynamic Center Workspace */}
        <main className="flex-1 relative overflow-hidden flex flex-col">
          {currentModule === 'world-map' && (
            <div className="flex-1 flex flex-col relative overflow-hidden">
              <div className="flex-1 relative overflow-hidden">
                {/* Floating Search Bar (Top Left over map) */}
                <div className="absolute top-4 left-4 z-10">
                  <SearchBar
                    onCenterEntity={(wx, wy) => {
                      const boundsW = activeMapSnapshot?.width || 120;
                      const boundsH = activeMapSnapshot?.height || 120;
                      const baseScale = Math.min((window.innerWidth - 300) / boundsW, (window.innerHeight - 150) / boundsH);
                      const newPanX = (window.innerWidth - 300) / 2 - wx * baseScale;
                      const newPanY = (window.innerHeight - 150) / 2 - wy * baseScale;
                      setCamera({ panX: newPanX, panY: newPanY, zoom: 1.5 });
                    }}
                  />
                </div>

                {/* Map Overlay Panels */}
                {showLayers && <LayerControl />}
                {showFilters && <FilterPanel />}
                {showLegend && <MapLegend />}

                {/* Interactive World Map */}
                {activeMapSnapshot && (
                  <WorldMapWorkspace
                    mapSnapshot={activeMapSnapshot}
                    overlays={activeOverlays}
                    isAwakened={isAwakened}
                  />
                )}

                {/* Inspection Drawer */}
                {activeMapSnapshot && (
                  <InspectionDrawer mapSnapshot={activeMapSnapshot} isAwakened={isAwakened} />
                )}
              </div>

              {/* Bottom Docked Historical Timeline Scrubber */}
              <HistoricalTimelineScrubber
                worldId={summary?.worldId}
                currentLiveYear={summary?.year ?? 0}
                currentLiveMonth={summary?.month ?? 1}
              />
            </div>
          )}

          {currentModule === 'people' && <PeopleExplorer />}
          {currentModule === 'families' && <FamilyDynastyExplorer />}
          {currentModule === 'organizations' && <OrganizationExplorer />}
          {currentModule === 'heirlooms' && <HeirloomExplorer />}
          {currentModule === 'leaderboards' && <LeaderboardView />}

          {/* Phase 3.4 Modules */}
          {currentModule === 'activity' && <WorldActivityFeed />}
          {currentModule === 'chronicle' && <HistoricalChronicleView />}
          {currentModule === 'battles' && <BattleExplorer />}
          {currentModule === 'tides' && <MonsterTideTracker />}
          {currentModule === 'expeditions' && <ExpeditionExplorer />}
          {currentModule === 'anomalies' && <AnomalyActivityView />}
          {currentModule === 'feuds' && <BloodFeudLedger />}
        </main>
      </div>

      {/* Global Modals for Detail Inspection */}
      <PersonProfileModal />
      <FamilyTreeModal />
      <ClanDetailModal />
      <HeirloomDetailModal />
      <EquipmentDetailModal />
      <ComparisonModal />

      {/* Phase 3.4 Inspection Modals */}
      <EventDetailModal />
      <BattleDetailModal />
      <MonsterTideDetailModal />
      <ExpeditionDetailModal />
      <AnomalyDetailModal />
      <FeudDetailModal />

      {/* Phase 3.5 Time Travel, Save & Branch Modals */}
      <HistoricalComparisonModal />
      <SaveManagerModal />
      <BranchManagerModal />
      <NewWorldModal />
    </div>
  );
};

