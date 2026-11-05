import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { fetchMonsterTideDetail } from '../../services/api';
import { useMapStore } from '../../store/useMapStore';
import {
  X,
  Skull,
  Calendar,
  MapPin,
  Shield,
  Eye,
  AlertTriangle,
  Building,
  Flame,
  Swords,
  Layers,
  ExternalLink,
  Navigation,
} from 'lucide-react';

export const MonsterTideDetailModal: React.FC = () => {
  const selectedTideId = useMapStore((s) => s.selectedTideId);
  const closeTideDetail = useMapStore((s) => s.closeTideDetail);
  const openBattleDetail = useMapStore((s) => s.openBattleDetail);
  const navigateToEntityOnMap = useMapStore((s) => s.navigateToEntityOnMap);

  const { data: tide, isLoading } = useQuery({
    queryKey: ['tide-detail', selectedTideId],
    queryFn: () => (selectedTideId ? fetchMonsterTideDetail(selectedTideId) : null),
    enabled: !!selectedTideId,
  });

  if (!selectedTideId) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm select-none">
      <div className="w-full max-w-3xl bg-slate-900 border border-slate-800 rounded-2xl shadow-2xl flex flex-col overflow-hidden max-h-[88vh]">
        {/* Header */}
        <div className="p-4 border-b border-slate-800 bg-slate-950 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-xl bg-purple-950 text-purple-400 border border-purple-800/60">
              <Skull className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-base font-bold text-slate-100">Monster Tide Dossier</h2>
                <span className="text-xs font-mono text-slate-400">#{selectedTideId}</span>
              </div>
              <p className="text-xs text-slate-400">
                Wilderness beast mobilization and siege logistics assessment
              </p>
            </div>
          </div>

          <button
            onClick={closeTideDetail}
            className="p-1.5 rounded-lg text-slate-400 hover:text-slate-200 hover:bg-slate-800 transition"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-5 space-y-5 scrollbar-thin">
          {isLoading && (
            <div className="py-20 text-center text-slate-400 text-xs font-mono">
              <div className="w-8 h-8 rounded-full border-2 border-purple-500/20 border-t-purple-400 animate-spin mx-auto mb-3" />
              Retrieving invasion telemetry...
            </div>
          )}

          {tide && (
            <>
              {/* Route & Spatial Status */}
              <div className="p-4 rounded-xl bg-slate-950/70 border border-slate-800 space-y-3">
                <div className="flex items-center justify-between flex-wrap gap-2">
                  <div className="flex items-center gap-2">
                    <span className="px-2.5 py-0.5 rounded text-xs font-bold uppercase bg-purple-950 text-purple-300 border border-purple-800">
                      {tide.state}
                    </span>
                    <span className="text-xs text-slate-400 font-mono">
                      Y{tide.mobilizedYear} M{tide.mobilizedMonth} ➜ Arrival Y{tide.arrivalYear} M{tide.arrivalMonth}
                    </span>
                  </div>

                  {tide.seerDetected && (
                    <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-cyan-950 text-cyan-300 border border-cyan-800 flex items-center gap-1">
                      <Eye className="w-3 h-3 text-cyan-400" />
                      Seer Early Warning: {tide.warningLeadMonths} Months
                    </span>
                  )}
                </div>

                {/* Spatial Nodes */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-2">
                  {/* Source Lair */}
                  <div className="p-3 rounded-lg bg-slate-900 border border-slate-800 flex items-center justify-between">
                    <div>
                      <span className="text-[10px] font-bold text-slate-400 uppercase">Source Monster Lair</span>
                      <div className="text-xs font-bold text-purple-300 mt-0.5 font-mono">{tide.sourceLairId}</div>
                      <span className="text-[10px] text-slate-500 font-mono">
                        ({tide.sourceX}, {tide.sourceY})
                      </span>
                    </div>
                    <button
                      onClick={() => {
                        closeTideDetail();
                        navigateToEntityOnMap('LAIR', tide.sourceLairId, tide.sourceX, tide.sourceY);
                      }}
                      className="px-2.5 py-1 rounded bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-medium flex items-center gap-1 transition"
                    >
                      <Navigation className="w-3 h-3 text-purple-400" />
                      <span>View Lair</span>
                    </button>
                  </div>

                  {/* Target Settlement */}
                  <div className="p-3 rounded-lg bg-slate-900 border border-slate-800 flex items-center justify-between">
                    <div>
                      <span className="text-[10px] font-bold text-slate-400 uppercase">Target Settlement</span>
                      <div className="text-xs font-bold text-blue-300 mt-0.5">
                        {tide.targetVillageName || `Settlement #${tide.targetVillageId}`}
                      </div>
                      <span className="text-[10px] text-slate-500 font-mono">
                        ({tide.targetX}, {tide.targetY})
                      </span>
                    </div>
                    <button
                      onClick={() => {
                        closeTideDetail();
                        navigateToEntityOnMap('SETTLEMENT', tide.targetVillageId, tide.targetX, tide.targetY);
                      }}
                      className="px-2.5 py-1 rounded bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-medium flex items-center gap-1 transition"
                    >
                      <Navigation className="w-3 h-3 text-blue-400" />
                      <span>View Village</span>
                    </button>
                  </div>
                </div>

                <div className="text-[11px] text-slate-400 font-mono flex items-center gap-4 pt-1">
                  <span>Current Beast Vanguard Pos: ({tide.currentX}, {tide.currentY})</span>
                  <span>Travel Duration: {tide.travelDurationMonths} Months</span>
                </div>
              </div>

              {/* Horde Waves Composition */}
              <div className="space-y-2">
                <h3 className="text-xs font-bold text-slate-400 uppercase tracking-wider flex items-center gap-2">
                  <Layers className="w-3.5 h-3.5 text-purple-400" />
                  Horde Wave Formations ({tide.totalMobilized} Total Mobilized)
                </h3>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                  {/* Wave 1 */}
                  <div className="p-3.5 rounded-xl bg-slate-950/50 border border-slate-800">
                    <span className="text-[10px] font-bold text-slate-400 uppercase">Wave 1 — Vanguard</span>
                    <div className="text-lg font-bold font-mono text-purple-300 my-1">
                      {tide.wave1MonsterIds?.length || 0} Beasts
                    </div>
                    <span className="text-[10px] text-slate-500">Fast skirmishers & scouts</span>
                  </div>

                  {/* Wave 2 */}
                  <div className="p-3.5 rounded-xl bg-slate-950/50 border border-slate-800">
                    <span className="text-[10px] font-bold text-slate-400 uppercase">Wave 2 — Main Host</span>
                    <div className="text-lg font-bold font-mono text-purple-300 my-1">
                      {tide.wave2MonsterIds?.length || 0} Beasts
                    </div>
                    <span className="text-[10px] text-slate-500">Core horde assault line</span>
                  </div>

                  {/* Wave 3 */}
                  <div className="p-3.5 rounded-xl bg-slate-950/50 border border-slate-800">
                    <span className="text-[10px] font-bold text-slate-400 uppercase">Wave 3 — Apex Beasts</span>
                    <div className="text-lg font-bold font-mono text-rose-400 my-1">
                      {tide.wave3MonsterIds?.length || 0} Beasts
                    </div>
                    <span className="text-[10px] text-slate-500">Heavy siege & apex monstrosities</span>
                  </div>
                </div>
              </div>

              {/* Devastation & Casualties Assessment */}
              <div className="p-4 rounded-xl bg-slate-950/70 border border-slate-800 space-y-3">
                <h3 className="text-xs font-bold text-slate-300 uppercase tracking-wider flex items-center gap-2">
                  <Flame className="w-3.5 h-3.5 text-rose-400" />
                  Aftermath & Casualties Assessment
                </h3>

                <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                  <div className="p-2.5 rounded-lg bg-slate-900 border border-slate-800">
                    <span className="text-[10px] text-slate-400 uppercase">Defender Fatalities</span>
                    <div className="text-base font-bold font-mono text-rose-400 mt-0.5">
                      {tide.defenderFatalities}
                    </div>
                  </div>
                  <div className="p-2.5 rounded-lg bg-slate-900 border border-slate-800">
                    <span className="text-[10px] text-slate-400 uppercase">Civilian Fatalities</span>
                    <div className="text-base font-bold font-mono text-rose-400 mt-0.5">
                      {tide.civilianFatalities}
                    </div>
                  </div>
                  <div className="p-2.5 rounded-lg bg-slate-900 border border-slate-800">
                    <span className="text-[10px] text-slate-400 uppercase">Beasts Slain</span>
                    <div className="text-base font-bold font-mono text-emerald-400 mt-0.5">
                      {tide.slainCount}
                    </div>
                  </div>
                  <div className="p-2.5 rounded-lg bg-slate-900 border border-slate-800">
                    <span className="text-[10px] text-slate-400 uppercase">Food Destroyed</span>
                    <div className="text-base font-bold font-mono text-amber-400 mt-0.5">
                      {tide.foodDestroyed} units
                    </div>
                  </div>
                </div>

                {/* Structural Damage Progress */}
                <div className="space-y-1.5 pt-1">
                  <div className="flex justify-between text-xs font-semibold">
                    <span className="text-slate-300">Settlement Structural Damage</span>
                    <span className="font-mono text-rose-400">{tide.structuralDamagePercent}%</span>
                  </div>
                  <div className="w-full bg-slate-800 h-2 rounded-full overflow-hidden">
                    <div
                      className="bg-rose-500 h-full rounded-full transition-all duration-500"
                      style={{ width: `${Math.min(100, Math.max(0, tide.structuralDamagePercent))}%` }}
                    />
                  </div>
                </div>

                {/* Associated Battle Link */}
                {tide.associatedBattleId && (
                  <div className="p-3 rounded-lg bg-slate-900 border border-slate-800 flex items-center justify-between mt-2">
                    <div className="flex items-center gap-2">
                      <Swords className="w-4 h-4 text-rose-400" />
                      <div>
                        <span className="text-xs font-bold text-slate-200">Associated Tactical Battle Encounter</span>
                        <div className="text-[10px] font-mono text-slate-400">Battle #{tide.associatedBattleId}</div>
                      </div>
                    </div>
                    <button
                      onClick={() => {
                        closeTideDetail();
                        openBattleDetail(tide.associatedBattleId!);
                      }}
                      className="px-3 py-1.5 rounded-lg bg-rose-950 hover:bg-rose-900 text-rose-300 border border-rose-800 text-xs font-semibold flex items-center gap-1.5 transition"
                    >
                      <span>Inspect Battle Replay</span>
                      <ExternalLink className="w-3 h-3" />
                    </button>
                  </div>
                )}
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
};

