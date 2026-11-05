import React, { useEffect, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useMapStore, DrawerTab } from '../../store/useMapStore';
import {
  fetchSettlementDetail,
  fetchSettlementEvents,
  fetchSettlementHistory,
} from '../../services/api';
import {
  WorldMapSnapshot,
  SettlementDetailDto,
  EventDto,
  SettlementHistoryDto,
} from '../../types/api';
import {
  X,
  MapPin,
  Users,
  Coins,
  Shield,
  History,
  AlertTriangle,
  Skull,
  Sparkles,
  Compass,
  Hammer,
  Crown,
  Clock,
  TrendingUp,
} from 'lucide-react';
import {
  ResponsiveContainer,
  LineChart,
  Line,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
} from 'recharts';

interface InspectionDrawerProps {
  mapSnapshot: WorldMapSnapshot;
  isAwakened: boolean;
}

export const InspectionDrawer: React.FC<InspectionDrawerProps> = ({ mapSnapshot, isAwakened }) => {
  const selectedEntity = useMapStore((s) => s.selectedEntity);
  const selectEntity = useMapStore((s) => s.selectEntity);
  const drawerOpen = useMapStore((s) => s.drawerOpen);
  const setDrawerOpen = useMapStore((s) => s.setDrawerOpen);
  const activeTab = useMapStore((s) => s.activeDrawerTab);
  const setActiveTab = useMapStore((s) => s.setActiveDrawerTab);
  const timeTravelMode = useMapStore((s) => s.timeTravelMode);
  const historicalResolution = useMapStore((s) => s.historicalResolution);
  const historicalYear = useMapStore((s) => s.historicalYear);
  const isHistorical = timeTravelMode === 'HISTORICAL';

  // Keyboard Escape to close
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setDrawerOpen(false);
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [setDrawerOpen]);

  // Query settlement details when selectedEntity.type === 'SETTLEMENT'
  const settlementId = selectedEntity?.type === 'SETTLEMENT' ? Number(selectedEntity.id) : null;
  const {
    data: settlementDetail,
    isLoading: detailLoading,
    error: detailError,
  } = useQuery({
    queryKey: ['settlement-detail', settlementId],
    queryFn: () => fetchSettlementDetail(settlementId!),
    enabled: settlementId !== null,
    staleTime: 5000,
  });

  // Query settlement events
  const { data: settlementEvents, isLoading: eventsLoading } = useQuery({
    queryKey: ['settlement-events', settlementDetail?.name],
    queryFn: () => fetchSettlementEvents(settlementDetail?.name),
    enabled: !!settlementDetail?.name && activeTab === 'HISTORY',
    staleTime: 10000,
  });

  // Phase 3.5: Query settlement historical progression time-series
  const { data: settlementHistory, isLoading: historyLoading } = useQuery({
    queryKey: ['settlement-history', settlementId],
    queryFn: () => fetchSettlementHistory(settlementId!),
    enabled: settlementId !== null && activeTab === 'HISTORY',
    staleTime: 10000,
  });

  if (!selectedEntity || !drawerOpen) return null;

  // Lair Entity View
  if (selectedEntity.type === 'LAIR') {
    const lair = mapSnapshot.lairs.find((l) => l.lairId === selectedEntity.id);
    if (!lair) return null;

    const fillRatio = Math.min(100, Math.round((lair.monsterCount / Math.max(1, lair.capacity)) * 100));

    return (
      <aside className="absolute top-16 right-4 bottom-4 w-96 bg-slate-950/95 backdrop-blur-md border border-slate-700/80 rounded-2xl p-4 shadow-2xl flex flex-col z-20 select-none animate-in slide-in-from-right duration-200">
        <div className="flex items-start justify-between pb-3 border-b border-slate-800">
          <div className="flex items-center gap-2">
            <div className="p-2 rounded-xl bg-rose-950/60 border border-rose-800/50 text-rose-400">
              <Skull className="w-5 h-5" />
            </div>
            <div>
              <h2 className="font-bold text-sm text-slate-100">{lair.primarySpecies} Lair</h2>
              <div className="flex items-center gap-2 mt-0.5 text-[11px] text-slate-400 font-mono">
                <span>{lair.lairId}</span>
                <span>•</span>
                <span>({lair.x.toFixed(1)}, {lair.y.toFixed(1)})</span>
              </div>
            </div>
          </div>
          <button onClick={() => setDrawerOpen(false)} className="text-slate-400 hover:text-slate-200 p-1">
            <X className="w-4 h-4" />
          </button>
        </div>

        <div className="mt-4 space-y-4 text-xs overflow-y-auto pr-1 flex-1">
          {/* Status & Threat Card */}
          <div className="bg-slate-900/80 rounded-xl p-3 border border-slate-800 space-y-2">
            <div className="flex justify-between items-center">
              <span className="text-slate-400">Dominant Threat Rank</span>
              <span className="font-bold text-rose-400 font-mono">{lair.threatRank}</span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-slate-400">Lair State</span>
              <span className="font-semibold text-slate-200">{lair.state}</span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-slate-400">Territory Danger Radius</span>
              <span className="font-mono text-cyan-400">{lair.territoryRadius.toFixed(1)} world units</span>
            </div>
          </div>

          {/* Monster Population Gauge */}
          <div className="bg-slate-900/80 rounded-xl p-3 border border-slate-800 space-y-2">
            <div className="flex justify-between items-center">
              <span className="font-semibold text-slate-300">Monster Population</span>
              <span className="font-mono text-slate-100 font-bold">
                {lair.monsterCount} / {lair.capacity}
              </span>
            </div>
            <div className="w-full h-2 bg-slate-800 rounded-full overflow-hidden">
              <div
                className="h-full bg-rose-500 rounded-full transition-all duration-300"
                style={{ width: `${fillRatio}%` }}
              />
            </div>
            <div className="text-[10px] text-slate-500 text-right">{fillRatio}% of maximum capacity</div>
          </div>

          {/* Species Information */}
          <div className="bg-slate-900/80 rounded-xl p-3 border border-slate-800 space-y-1.5">
            <div className="text-slate-400 text-[11px] font-semibold uppercase tracking-wider">Ecology Breakdown</div>
            <div className="text-slate-300">
              Primary Species: <span className="font-semibold text-rose-300">{lair.primarySpecies}</span>
            </div>
            {lair.secondarySpecies && (
              <div className="text-slate-300">
                Secondary Species: <span className="font-semibold text-slate-400">{lair.secondarySpecies}</span>
              </div>
            )}
            <div className="text-slate-400 text-[11px] pt-1">
              Source Anomaly: <span className="font-mono text-slate-300">{lair.sourceAnomalyId}</span>
            </div>
          </div>
        </div>
      </aside>
    );
  }

  // Anomaly Entity View
  if (selectedEntity.type === 'ANOMALY') {
    const anom = mapSnapshot.anomalies.find((a) => a.anomalyId === selectedEntity.id);
    if (!anom) return null;

    const remainingRatio = Math.min(
      100,
      Math.round((anom.remainingQuantity / Math.max(1, anom.initialQuantity)) * 100)
    );

    return (
      <aside className="absolute top-16 right-4 bottom-4 w-96 bg-slate-950/95 backdrop-blur-md border border-slate-700/80 rounded-2xl p-4 shadow-2xl flex flex-col z-20 select-none animate-in slide-in-from-right duration-200">
        <div className="flex items-start justify-between pb-3 border-b border-slate-800">
          <div className="flex items-center gap-2">
            <div className="p-2 rounded-xl bg-cyan-950/60 border border-cyan-800/50 text-cyan-400">
              <Sparkles className="w-5 h-5" />
            </div>
            <div>
              <h2 className="font-bold text-sm text-slate-100">{anom.resourceCategory.replace(/_/g, ' ')}</h2>
              <div className="flex items-center gap-2 mt-0.5 text-[11px] text-slate-400 font-mono">
                <span>{anom.anomalyId}</span>
                <span>•</span>
                <span>({anom.x.toFixed(1)}, {anom.y.toFixed(1)})</span>
              </div>
            </div>
          </div>
          <button onClick={() => setDrawerOpen(false)} className="text-slate-400 hover:text-slate-200 p-1">
            <X className="w-4 h-4" />
          </button>
        </div>

        <div className="mt-4 space-y-4 text-xs overflow-y-auto pr-1 flex-1">
          {/* Grade & State */}
          <div className="bg-slate-900/80 rounded-xl p-3 border border-slate-800 space-y-2">
            <div className="flex justify-between items-center">
              <span className="text-slate-400">Resource Grade</span>
              <span className="font-bold text-cyan-400 font-mono">{anom.resourceGrade}</span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-slate-400">Status</span>
              <span className="font-semibold text-slate-200">{anom.state}</span>
            </div>
            {anom.controllingOwnerId && (
              <div className="flex justify-between items-center">
                <span className="text-slate-400">Controlling Faction</span>
                <span className="font-semibold text-emerald-400">{anom.controllingOwnerId}</span>
              </div>
            )}
          </div>

          {/* Deposit Reserves Gauge */}
          <div className="bg-slate-900/80 rounded-xl p-3 border border-slate-800 space-y-2">
            <div className="flex justify-between items-center">
              <span className="font-semibold text-slate-300">Remaining Reserves</span>
              <span className="font-mono text-slate-100 font-bold">
                {anom.remainingQuantity.toFixed(1)} / {anom.initialQuantity.toFixed(1)}
              </span>
            </div>
            <div className="w-full h-2 bg-slate-800 rounded-full overflow-hidden">
              <div
                className="h-full bg-cyan-400 rounded-full transition-all duration-300"
                style={{ width: `${remainingRatio}%` }}
              />
            </div>
            <div className="text-[10px] text-slate-500 text-right">{remainingRatio}% reserves remaining</div>
          </div>
        </div>
      </aside>
    );
  }

  // Expedition Entity View
  if (selectedEntity.type === 'EXPEDITION') {
    const exp = mapSnapshot.expeditions.find((e) => e.expeditionId === selectedEntity.id);
    if (!exp) return null;

    return (
      <aside className="absolute top-16 right-4 bottom-4 w-96 bg-slate-950/95 backdrop-blur-md border border-slate-700/80 rounded-2xl p-4 shadow-2xl flex flex-col z-20 select-none animate-in slide-in-from-right duration-200">
        <div className="flex items-start justify-between pb-3 border-b border-slate-800">
          <div className="flex items-center gap-2">
            <div className="p-2 rounded-xl bg-amber-950/60 border border-amber-800/50 text-amber-400">
              <Compass className="w-5 h-5" />
            </div>
            <div>
              <h2 className="font-bold text-sm text-slate-100">Expedition Party</h2>
              <div className="flex items-center gap-2 mt-0.5 text-[11px] text-slate-400 font-mono">
                <span>{exp.expeditionId}</span>
              </div>
            </div>
          </div>
          <button onClick={() => setDrawerOpen(false)} className="text-slate-400 hover:text-slate-200 p-1">
            <X className="w-4 h-4" />
          </button>
        </div>

        <div className="mt-4 space-y-3 text-xs flex-1">
          <div className="bg-slate-900/80 rounded-xl p-3 border border-slate-800 space-y-2">
            <div className="flex justify-between items-center">
              <span className="text-slate-400">Status</span>
              <span className="font-bold text-amber-400 font-mono">{exp.status}</span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-slate-400">Origin Settlement</span>
              <span className="font-semibold text-slate-200">{exp.originVillageId}</span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-slate-400">Target Site</span>
              <span className="font-semibold text-cyan-400">{exp.targetAnomalyId}</span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-slate-400">Party Size</span>
              <span className="font-semibold text-slate-200">{exp.personnelCount} Personnel</span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-slate-400">Carried Cargo</span>
              <span className="font-mono text-emerald-400">{exp.carriedCargo.toFixed(1)} units</span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-slate-400">Current Position</span>
              <span className="font-mono text-slate-300">
                ({exp.currentX.toFixed(1)}, {exp.currentY.toFixed(1)})
              </span>
            </div>
          </div>
        </div>
      </aside>
    );
  }

  // Settlement Comprehensive View
  const sMarker = mapSnapshot.settlements.find((v) => v.id === settlementId);

  return (
    <aside className="absolute top-16 right-4 bottom-4 w-96 md:w-[420px] bg-slate-950/95 backdrop-blur-md border border-slate-700/80 rounded-2xl p-4 shadow-2xl flex flex-col z-20 select-none animate-in slide-in-from-right duration-200">
      {/* Header */}
      <div className="flex items-start justify-between pb-3 border-b border-slate-800">
        <div className="flex items-center gap-2.5">
          <div className="p-2.5 rounded-xl bg-sky-950/60 border border-sky-800/50 text-sky-400">
            <MapPin className="w-5 h-5" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h2 className="font-bold text-base text-slate-100">{settlementDetail?.name || sMarker?.name}</h2>
              <span className="px-2 py-0.5 rounded text-[10px] font-mono font-bold bg-slate-800 text-sky-300 border border-slate-700">
                {settlementDetail?.tier || sMarker?.tier}
              </span>
            </div>
            <div className="flex items-center gap-2 mt-0.5 text-[11px] text-slate-400 font-mono">
              <span>{settlementDetail?.biome || sMarker?.biome}</span>
              <span>•</span>
              <span>
                Coord: ({(settlementDetail?.x || sMarker?.x || 0).toFixed(1)},{' '}
                {(settlementDetail?.y || sMarker?.y || 0).toFixed(1)})
              </span>
            </div>
          </div>
        </div>

        <button onClick={() => setDrawerOpen(false)} className="text-slate-400 hover:text-slate-200 p-1">
          <X className="w-4 h-4" />
        </button>
      </div>

      {/* Historical Mode Read-Only Banner */}
      {isHistorical && (
        <div className="mt-3 px-3 py-2 rounded-xl bg-amber-950/70 border border-amber-600/60 text-amber-300 text-xs space-y-1">
          <div className="flex items-center justify-between font-mono font-bold text-[11px]">
            <span className="flex items-center gap-1.5">
              <Clock className="w-3.5 h-3.5 text-amber-400" /> HISTORICAL ARCHIVE (Y{historicalYear})
            </span>
            <span className="px-1.5 py-0.5 rounded bg-amber-900/60 border border-amber-600/50 text-[10px]">
              {historicalResolution || 'AGGREGATED_HISTORICAL_DATA'}
            </span>
          </div>
          <div className="text-[10px] text-amber-400/80">
            Immutable presentation snapshot. Live actions and mutating commands are disabled.
          </div>
        </div>
      )}

      {/* Famine Warning */}
      {(settlementDetail?.inFamine || sMarker?.inFamine) && (
        <div className="mt-3 px-3 py-2 rounded-xl bg-rose-950/70 border border-rose-800/60 text-rose-300 text-xs flex items-center gap-2">
          <AlertTriangle className="w-4 h-4 text-rose-400 shrink-0" />
          <span>Famine Warning: Food reserves critically depleted!</span>
        </div>
      )}

      {/* Navigation Tabs */}
      <div className="flex items-center border-b border-slate-800 mt-3 text-xs gap-1">
        <button
          onClick={() => setActiveTab('OVERVIEW')}
          className={`px-3 py-1.5 border-b-2 font-medium transition ${
            activeTab === 'OVERVIEW'
              ? 'border-cyan-400 text-cyan-400'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          Overview
        </button>

        <button
          onClick={() => setActiveTab('ECONOMY')}
          className={`px-3 py-1.5 border-b-2 font-medium transition ${
            activeTab === 'ECONOMY'
              ? 'border-cyan-400 text-cyan-400'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          Economy
        </button>

        <button
          onClick={() => setActiveTab('POPULATION')}
          className={`px-3 py-1.5 border-b-2 font-medium transition ${
            activeTab === 'POPULATION'
              ? 'border-cyan-400 text-cyan-400'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          Society
        </button>

        <button
          onClick={() => setActiveTab('MILITARY')}
          className={`px-3 py-1.5 border-b-2 font-medium transition ${
            activeTab === 'MILITARY'
              ? 'border-cyan-400 text-cyan-400'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          Defense
        </button>

        <button
          onClick={() => setActiveTab('HISTORY')}
          className={`px-3 py-1.5 border-b-2 font-medium transition ${
            activeTab === 'HISTORY'
              ? 'border-cyan-400 text-cyan-400'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          History
        </button>
      </div>

      {/* Tab Body */}
      <div className="flex-1 overflow-y-auto mt-3 pr-1 text-xs space-y-3">
        {detailLoading && <div className="text-slate-400 py-6 text-center font-mono">Loading settlement records...</div>}

        {detailError && (
          <div className="text-rose-400 py-4 text-center">Failed to load detailed records for this settlement.</div>
        )}

        {settlementDetail && !detailLoading && (
          <>
            {/* 1. OVERVIEW TAB */}
            {activeTab === 'OVERVIEW' && (
              <div className="space-y-3">
                {/* Population Card */}
                <div className="bg-slate-900/80 rounded-xl p-3 border border-slate-800 space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="text-slate-400 flex items-center gap-1.5">
                      <Users className="w-3.5 h-3.5 text-emerald-400" /> Living Population
                    </span>
                    <span className="font-bold text-base text-emerald-400 font-mono">
                      {settlementDetail.population}
                    </span>
                  </div>
                  <div className="grid grid-cols-2 gap-2 text-[11px] pt-1 border-t border-slate-800/60">
                    <div>
                      Males: <span className="font-mono text-slate-200">{settlementDetail.livingMales}</span>
                    </div>
                    <div>
                      Females: <span className="font-mono text-slate-200">{settlementDetail.livingFemales}</span>
                    </div>
                  </div>
                </div>

                {/* Leader Card */}
                <div className="bg-slate-900/80 rounded-xl p-3 border border-slate-800 flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <Crown className="w-4 h-4 text-amber-400" />
                    <div>
                      <div className="text-[10px] text-slate-400 uppercase tracking-wider">Settlement Leader</div>
                      <div className="font-semibold text-slate-200">{settlementDetail.leaderName}</div>
                    </div>
                  </div>
                  {settlementDetail.leaderId && (
                    <span className="font-mono text-[10px] text-slate-500">ID: {settlementDetail.leaderId}</span>
                  )}
                </div>

                {/* Development & Tech */}
                <div className="bg-slate-900/80 rounded-xl p-3 border border-slate-800 grid grid-cols-2 gap-3">
                  <div>
                    <div className="text-slate-400 text-[10px]">Technology Level</div>
                    <div className="text-base font-bold text-cyan-400 font-mono">
                      Lv. {settlementDetail.techLevel}
                    </div>
                  </div>
                  <div>
                    <div className="text-slate-400 text-[10px]">Housing Capacity</div>
                    <div className="text-base font-bold text-slate-200 font-mono">
                      {settlementDetail.housingCapacity}
                    </div>
                  </div>
                </div>
              </div>
            )}

            {/* 2. ECONOMY TAB */}
            {activeTab === 'ECONOMY' && (
              <div className="space-y-3">
                {/* Stockpile Breakdown */}
                <div className="bg-slate-900/80 rounded-xl p-3 border border-slate-800 space-y-2.5">
                  <div className="font-semibold text-slate-300 text-xs mb-1">Resource Stockpiles</div>

                  <div>
                    <div className="flex justify-between text-[11px] mb-0.5">
                      <span className="text-emerald-400">Food Stockpile</span>
                      <span className="font-mono font-bold text-slate-200">
                        {settlementDetail.foodStockpile.toLocaleString()}
                      </span>
                    </div>
                    <div className="w-full h-1.5 bg-slate-800 rounded-full overflow-hidden">
                      <div
                        className="h-full bg-emerald-500 rounded-full"
                        style={{
                          width: `${Math.min(100, (settlementDetail.foodStockpile / 1000) * 100)}%`,
                        }}
                      />
                    </div>
                  </div>

                  <div>
                    <div className="flex justify-between text-[11px] mb-0.5">
                      <span className="text-amber-500">Wood Stockpile</span>
                      <span className="font-mono font-bold text-slate-200">
                        {settlementDetail.woodStockpile.toLocaleString()}
                      </span>
                    </div>
                    <div className="w-full h-1.5 bg-slate-800 rounded-full overflow-hidden">
                      <div
                        className="h-full bg-amber-600 rounded-full"
                        style={{
                          width: `${Math.min(100, (settlementDetail.woodStockpile / 1000) * 100)}%`,
                        }}
                      />
                    </div>
                  </div>

                  <div>
                    <div className="flex justify-between text-[11px] mb-0.5">
                      <span className="text-slate-400">Ore Stockpile</span>
                      <span className="font-mono font-bold text-slate-200">
                        {settlementDetail.oreStockpile.toLocaleString()}
                      </span>
                    </div>
                    <div className="w-full h-1.5 bg-slate-800 rounded-full overflow-hidden">
                      <div
                        className="h-full bg-slate-400 rounded-full"
                        style={{
                          width: `${Math.min(100, (settlementDetail.oreStockpile / 1000) * 100)}%`,
                        }}
                      />
                    </div>
                  </div>

                  <div>
                    <div className="flex justify-between text-[11px] mb-0.5">
                      <span className="text-cyan-400">Tools Stockpile</span>
                      <span className="font-mono font-bold text-slate-200">
                        {settlementDetail.toolsStockpile.toLocaleString()}
                      </span>
                    </div>
                    <div className="w-full h-1.5 bg-slate-800 rounded-full overflow-hidden">
                      <div
                        className="h-full bg-cyan-500 rounded-full"
                        style={{
                          width: `${Math.min(100, (settlementDetail.toolsStockpile / 1000) * 100)}%`,
                        }}
                      />
                    </div>
                  </div>
                </div>

                {/* Treasury & Supernatural Wealth */}
                <div className="bg-slate-900/80 rounded-xl p-3 border border-slate-800 grid grid-cols-2 gap-3">
                  <div>
                    <div className="text-slate-400 text-[10px] flex items-center gap-1">
                      <Coins className="w-3 h-3 text-amber-400" /> Civic Treasury
                    </div>
                    <div className="text-sm font-bold text-amber-400 font-mono mt-0.5">
                      {settlementDetail.treasury.toLocaleString()} Iron
                    </div>
                  </div>

                  <div>
                    <div className="text-slate-400 text-[10px]">Monster Cores</div>
                    <div className="text-sm font-bold text-purple-400 font-mono mt-0.5">
                      {settlementDetail.monsterCores} Cores
                    </div>
                  </div>
                </div>

                {/* Crafting Facilities */}
                <div className="bg-slate-900/80 rounded-xl p-3 border border-slate-800 space-y-1.5">
                  <div className="text-slate-400 text-[11px] font-semibold flex items-center gap-1.5">
                    <Hammer className="w-3.5 h-3.5 text-cyan-400" /> Crafting Facilities
                  </div>
                  {settlementDetail.craftingFacilities.length === 0 ? (
                    <div className="text-slate-500 text-[11px]">No high-tier facilities established</div>
                  ) : (
                    <div className="flex flex-wrap gap-1.5 mt-1">
                      {settlementDetail.craftingFacilities.map((fac) => (
                        <span
                          key={fac}
                          className="px-2 py-0.5 rounded bg-slate-800 text-[10px] text-cyan-300 border border-slate-700"
                        >
                          {fac}
                        </span>
                      ))}
                    </div>
                  )}
                  <div className="text-[11px] text-slate-400 pt-1">
                    Equipment in Storage: <span className="font-mono text-slate-200">{settlementDetail.storedEquipmentCount} items</span>
                  </div>
                </div>
              </div>
            )}

            {/* 3. POPULATION & DIPLOMACY TAB */}
            {activeTab === 'POPULATION' && (
              <div className="space-y-3">
                <div className="bg-slate-900/80 rounded-xl p-3 border border-slate-800 space-y-2">
                  <div className="flex justify-between items-center">
                    <span className="text-slate-400">Peak Historic Population</span>
                    <span className="font-mono text-slate-200 font-bold">{settlementDetail.peakPopulation}</span>
                  </div>
                </div>

                <div className="bg-slate-900/80 rounded-xl p-3 border border-slate-800 space-y-1.5">
                  <div className="text-slate-400 text-[11px] font-semibold">Diplomatic Standing</div>
                  <div className="text-slate-300 text-xs leading-relaxed bg-slate-950 p-2.5 rounded-lg border border-slate-800 font-mono">
                    {settlementDetail.diplomaticSummary || 'Peaceful / Neutral relations'}
                  </div>
                </div>
              </div>
            )}

            {/* 4. MILITARY TAB */}
            {activeTab === 'MILITARY' && (
              <div className="space-y-3">
                <div className="bg-slate-900/80 rounded-xl p-3 border border-slate-800 space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="text-slate-400 flex items-center gap-1.5">
                      <Shield className="w-3.5 h-3.5 text-cyan-400" /> Fortification Level
                    </span>
                    <span className="font-mono font-bold text-cyan-400">
                      {settlementDetail.fortificationLevel.toFixed(1)} / 100.0
                    </span>
                  </div>
                  <div className="w-full h-1.5 bg-slate-800 rounded-full overflow-hidden">
                    <div
                      className="h-full bg-cyan-500 rounded-full"
                      style={{ width: `${Math.min(100, settlementDetail.fortificationLevel)}%` }}
                    />
                  </div>
                </div>

                <div className="bg-slate-900/80 rounded-xl p-3 border border-slate-800 space-y-2">
                  <div className="text-slate-400 text-[11px] font-semibold">Defensive Posture</div>
                  <div className="text-slate-300 text-xs">
                    Town guards and awakening protectors are integrated into the central garrison framework.
                  </div>
                </div>
              </div>
            )}

            {/* 5. HISTORY TAB */}
            {activeTab === 'HISTORY' && (
              <div className="space-y-4">
                {/* Historical Population Progression Chart */}
                {settlementHistory && settlementHistory.history && settlementHistory.history.length > 0 && (
                  <div className="bg-slate-900/80 rounded-xl p-3 border border-slate-800 space-y-2">
                    <div className="flex items-center justify-between">
                      <span className="text-slate-300 font-semibold flex items-center gap-1.5">
                        <TrendingUp className="w-3.5 h-3.5 text-cyan-400" /> Historical Population Trend
                      </span>
                      <span className="text-[10px] font-mono text-slate-400">
                        Founded Y{settlementHistory.foundedYear}
                      </span>
                    </div>

                    <div className="h-36 w-full pt-1">
                      <ResponsiveContainer width="100%" height="100%">
                        <LineChart data={settlementHistory.history} margin={{ top: 5, right: 10, left: -20, bottom: 0 }}>
                          <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
                          <XAxis dataKey="year" stroke="#64748b" fontSize={10} tickFormatter={(y) => `Y${y}`} />
                          <YAxis stroke="#64748b" fontSize={10} />
                          <Tooltip
                            contentStyle={{ backgroundColor: '#090d16', borderColor: '#334155', borderRadius: '0.5rem', fontSize: '11px' }}
                            labelFormatter={(l) => `Year ${l}`}
                          />
                          <Line type="monotone" dataKey="population" name="Population" stroke="#38bdf8" strokeWidth={2} dot={false} />
                        </LineChart>
                      </ResponsiveContainer>
                    </div>
                  </div>
                )}

                <div className="text-slate-400 font-semibold text-[11px] pt-1">Chronicle Annals & Recorded Events</div>

                {eventsLoading && (
                  <div className="text-slate-400 py-4 text-center font-mono">Loading event chronicles...</div>
                )}
                {!eventsLoading && (!settlementEvents || settlementEvents.length === 0) && (
                  <div className="text-slate-500 py-4 text-center">No recent chronicles recorded for this settlement.</div>
                )}
                {settlementEvents &&
                  settlementEvents.map((evt) => (
                    <div key={evt.id} className="p-2 bg-slate-900/70 border border-slate-800/80 rounded-lg space-y-1">
                      <div className="flex items-center justify-between text-[10px] text-slate-500 font-mono">
                        <span>Year {evt.year}</span>
                        <span className="text-cyan-400">{evt.type}</span>
                      </div>
                      <div className="text-slate-300 text-[11px] leading-snug">{evt.summary}</div>
                    </div>
                  ))}
              </div>
            )}
          </>
        )}
      </div>
    </aside>
  );
};

