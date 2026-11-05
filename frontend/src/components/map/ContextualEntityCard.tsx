import React from 'react';
import { WorldMapSnapshot, MapSettlementDto, MapLairDto, MapAnomalyDto, MapExpeditionDto } from '../../types/api';
import { useMapStore } from '../../store/useMapStore';
import {
  X,
  Maximize2,
  Users,
  Shield,
  Coins,
  Crown,
  MapPin,
  AlertTriangle,
  Skull,
  Compass,
  Sparkles,
  ExternalLink,
  Crosshair,
  TrendingUp,
} from 'lucide-react';

interface ContextualEntityCardProps {
  mapSnapshot: WorldMapSnapshot;
  isAwakened: boolean;
  onCenterEntity: (x: number, y: number) => void;
  onOpenLedger: () => void;
}

export const ContextualEntityCard: React.FC<ContextualEntityCardProps> = ({
  mapSnapshot,
  isAwakened,
  onCenterEntity,
  onOpenLedger,
}) => {
  const selectedEntity = useMapStore((s) => s.selectedEntity);
  const selectEntity = useMapStore((s) => s.selectEntity);
  const openClanProfile = useMapStore((s) => s.openClanProfile);
  const openAnomalyDetail = useMapStore((s) => s.openAnomalyDetail);
  const openTideDetail = useMapStore((s) => s.openTideDetail);
  const openExpeditionDetail = useMapStore((s) => s.openExpeditionDetail);

  if (!selectedEntity) return null;

  // 1. SETTLEMENT CARD
  if (selectedEntity.type === 'SETTLEMENT') {
    const s = mapSnapshot.settlements.find((item) => item.id === Number(selectedEntity.id));
    if (!s) return null;

    const tierBadges: Record<string, { label: string; color: string; border: string }> = {
      HAMLET: { label: 'Rustic Hamlet', color: 'text-amber-300 bg-amber-950/60', border: 'border-amber-700/50' },
      VILLAGE: { label: 'Farming Village', color: 'text-emerald-300 bg-emerald-950/60', border: 'border-emerald-700/50' },
      TOWN: { label: 'Commercial Town', color: 'text-sky-300 bg-sky-950/60', border: 'border-sky-700/50' },
      CITY: { label: 'Fortified City', color: 'text-purple-300 bg-purple-950/60', border: 'border-purple-700/50' },
      MEGACITY: { label: 'Imperial Metropolis', color: 'text-rose-300 bg-rose-950/60', border: 'border-rose-700/50' },
    };

    const badge = tierBadges[s.tier] || tierBadges.VILLAGE;

    return (
      <div className="absolute bottom-6 right-6 z-20 w-80 rounded-xl bg-slate-950/90 backdrop-blur-md border border-slate-700/80 shadow-2xl overflow-hidden text-slate-100 animate-in fade-in slide-in-from-bottom-4 duration-200">
        {/* Top Header Banner */}
        <div className="relative px-4 py-3 border-b border-slate-800 bg-gradient-to-r from-slate-900 via-slate-850 to-slate-900 flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-amber-500/10 border border-amber-500/30 flex items-center justify-center text-amber-400 font-serif font-bold text-sm shadow-inner">
              {s.name.charAt(0)}
            </div>
            <div>
              <h3 className="font-serif font-bold text-sm tracking-wide text-white leading-tight flex items-center gap-1.5">
                {s.name}
                {s.inFamine && (
                  <span className="flex items-center text-rose-400 text-[10px] font-sans font-semibold bg-rose-950/80 px-1.5 py-0.2 rounded border border-rose-800 animate-pulse">
                    <AlertTriangle className="w-3 h-3 mr-0.5" /> FAMINE
                  </span>
                )}
              </h3>
              <div className="flex items-center gap-2 mt-0.5">
                <span className={`text-[10px] px-1.5 py-0.2 rounded font-semibold uppercase tracking-wider border ${badge.color} ${badge.border}`}>
                  {badge.label}
                </span>
                <span className="text-[10px] text-slate-400 font-mono">
                  ({s.x.toFixed(0)}, {s.y.toFixed(0)})
                </span>
              </div>
            </div>
          </div>

          <button
            onClick={() => selectEntity(null)}
            className="p-1 text-slate-400 hover:text-white rounded-md hover:bg-slate-800/60 transition"
            title="Dismiss Card"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Card Body Metrics */}
        <div className="p-4 space-y-3 text-xs">
          {/* Dominant Clan / Ruler */}
          <div className="flex items-center justify-between px-2.5 py-1.5 rounded-lg bg-slate-900/60 border border-slate-800/80">
            <div className="flex items-center gap-1.5 text-slate-400">
              <Crown className="w-3.5 h-3.5 text-amber-400" />
              <span>Ruling Clan:</span>
            </div>
            <span className="font-semibold text-amber-300">
              {s.dominantClanName || 'Unallied Elder Council'}
            </span>
          </div>

          {/* Core Stats Grid */}
          <div className="grid grid-cols-2 gap-2 text-slate-300">
            <div className="flex items-center gap-2 p-2 rounded-lg bg-slate-900/40 border border-slate-800/60">
              <Users className="w-4 h-4 text-emerald-400" />
              <div>
                <div className="text-[10px] text-slate-400 uppercase font-mono">Population</div>
                <div className="font-bold text-emerald-300 text-sm">{s.population.toLocaleString()}</div>
              </div>
            </div>

            <div className="flex items-center gap-2 p-2 rounded-lg bg-slate-900/40 border border-slate-800/60">
              <Shield className="w-4 h-4 text-sky-400" />
              <div>
                <div className="text-[10px] text-slate-400 uppercase font-mono">Fortification</div>
                <div className="font-bold text-sky-300 text-sm">Level {s.fortificationLevel ?? 0}</div>
              </div>
            </div>

            <div className="flex items-center gap-2 p-2 rounded-lg bg-slate-900/40 border border-slate-800/60">
              <Coins className="w-4 h-4 text-amber-400" />
              <div>
                <div className="text-[10px] text-slate-400 uppercase font-mono">Prosperity</div>
                <div className="font-bold text-amber-300 text-sm">{(s.prosperityScore ?? 0).toFixed(0)}</div>
              </div>
            </div>

            <div className="flex items-center gap-2 p-2 rounded-lg bg-slate-900/40 border border-slate-800/60">
              <TrendingUp className="w-4 h-4 text-purple-400" />
              <div>
                <div className="text-[10px] text-slate-400 uppercase font-mono">Tech Tier</div>
                <div className="font-bold text-purple-300 text-sm">Tier {s.techLevel ?? 1}</div>
              </div>
            </div>
          </div>
        </div>

        {/* Card Footer Actions */}
        <div className="px-4 py-2.5 bg-slate-900/80 border-t border-slate-800 flex items-center justify-between gap-2">
          <button
            onClick={() => onCenterEntity(s.x, s.y)}
            className="flex-1 flex items-center justify-center gap-1.5 py-1.5 px-2.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-medium transition"
            title="Center Camera on Settlement"
          >
            <Crosshair className="w-3.5 h-3.5 text-cyan-400" />
            <span>Center</span>
          </button>

          <button
            onClick={onOpenLedger}
            className="flex-1 flex items-center justify-center gap-1.5 py-1.5 px-2.5 rounded-lg bg-cyan-700 hover:bg-cyan-600 text-white text-xs font-semibold shadow-md transition"
            title="Open Full Settlement Ledger & Charts"
          >
            <ExternalLink className="w-3.5 h-3.5" />
            <span>Ledger</span>
          </button>
        </div>
      </div>
    );
  }

  // 2. MONSTER LAIR CARD
  if (selectedEntity.type === 'LAIR') {
    const lair = mapSnapshot.lairs.find((item) => item.lairId === selectedEntity.id);
    if (!lair) return null;

    const rankColors: Record<string, { color: string; badge: string }> = {
      MINOR: { color: 'text-slate-300', badge: 'bg-slate-800 text-slate-300 border-slate-700' },
      DANGEROUS: { color: 'text-amber-400', badge: 'bg-amber-950 text-amber-300 border-amber-800' },
      LEGENDARY: { color: 'text-orange-400', badge: 'bg-orange-950 text-orange-300 border-orange-700' },
      MYTHIC: { color: 'text-rose-400', badge: 'bg-rose-950 text-rose-300 border-rose-700' },
    };

    const rank = rankColors[lair.threatRank] || rankColors.DANGEROUS;

    return (
      <div className="absolute bottom-6 right-6 z-20 w-80 rounded-xl bg-slate-950/90 backdrop-blur-md border border-rose-900/60 shadow-2xl overflow-hidden text-slate-100 animate-in fade-in slide-in-from-bottom-4 duration-200">
        <div className="px-4 py-3 border-b border-rose-950/80 bg-gradient-to-r from-rose-950/40 via-slate-900 to-slate-900 flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-rose-500/10 border border-rose-500/30 flex items-center justify-center text-rose-400 font-bold">
              <Skull className="w-4 h-4" />
            </div>
            <div>
              <h3 className="font-serif font-bold text-sm text-rose-200 leading-tight">
                {lair.primarySpecies} Den
              </h3>
              <div className="flex items-center gap-2 mt-0.5">
                <span className={`text-[10px] px-1.5 py-0.2 rounded font-semibold uppercase tracking-wider border ${rank.badge}`}>
                  {lair.threatRank} THREAT
                </span>
                <span className="text-[10px] text-slate-400 font-mono">
                  ({lair.x.toFixed(0)}, {lair.y.toFixed(0)})
                </span>
              </div>
            </div>
          </div>

          <button
            onClick={() => selectEntity(null)}
            className="p-1 text-slate-400 hover:text-white rounded-md hover:bg-slate-800/60 transition"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        <div className="p-4 space-y-3 text-xs">
          <div className="grid grid-cols-2 gap-2">
            <div className="p-2 rounded-lg bg-slate-900/60 border border-slate-800/80">
              <div className="text-[10px] text-slate-400 uppercase font-mono">Monster Pack</div>
              <div className="font-bold text-rose-400 text-sm mt-0.5">
                {lair.monsterCount} {lair.primarySpecies}
              </div>
            </div>

            <div className="p-2 rounded-lg bg-slate-900/60 border border-slate-800/80">
              <div className="text-[10px] text-slate-400 uppercase font-mono">Territory Area</div>
              <div className="font-bold text-amber-300 text-sm mt-0.5">
                {lair.territoryRadius.toFixed(0)} km radius
              </div>
            </div>
          </div>

          <div className="p-2.5 rounded-lg bg-rose-950/30 border border-rose-900/40 text-[11px] text-rose-300">
            {lair.state === 'DESTROYED' ? (
              <span className="text-slate-400">This lair was eradicated in a past expedition.</span>
            ) : (
              <span>Hostile den radiating supernatural predator instincts. Poses threat to nearby settlements.</span>
            )}
          </div>
        </div>

        <div className="px-4 py-2.5 bg-slate-900/80 border-t border-slate-800 flex items-center gap-2">
          <button
            onClick={() => onCenterEntity(lair.x, lair.y)}
            className="flex-1 flex items-center justify-center gap-1.5 py-1.5 px-2.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-medium transition"
          >
            <Crosshair className="w-3.5 h-3.5 text-cyan-400" />
            <span>Center</span>
          </button>
        </div>
      </div>
    );
  }

  // 3. ANOMALY CARD
  if (selectedEntity.type === 'ANOMALY') {
    const anom = mapSnapshot.anomalies.find((item) => item.anomalyId === selectedEntity.id);
    if (!anom) return null;

    return (
      <div className="absolute bottom-6 right-6 z-20 w-80 rounded-xl bg-slate-950/90 backdrop-blur-md border border-cyan-800/60 shadow-2xl overflow-hidden text-slate-100 animate-in fade-in slide-in-from-bottom-4 duration-200">
        <div className="px-4 py-3 border-b border-cyan-900/60 bg-gradient-to-r from-cyan-950/40 via-slate-900 to-slate-900 flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-cyan-500/10 border border-cyan-500/30 flex items-center justify-center text-cyan-400">
              <Sparkles className="w-4 h-4" />
            </div>
            <div>
              <h3 className="font-serif font-bold text-sm text-cyan-200 leading-tight">
                {anom.resourceGrade} {anom.resourceCategory.replace(/_/g, ' ')}
              </h3>
              <div className="flex items-center gap-2 mt-0.5">
                <span className="text-[10px] px-1.5 py-0.2 rounded font-semibold uppercase tracking-wider bg-cyan-950 text-cyan-300 border border-cyan-700">
                  {anom.state}
                </span>
                <span className="text-[10px] text-slate-400 font-mono">
                  ({anom.x.toFixed(0)}, {anom.y.toFixed(0)})
                </span>
              </div>
            </div>
          </div>

          <button
            onClick={() => selectEntity(null)}
            className="p-1 text-slate-400 hover:text-white rounded-md hover:bg-slate-800/60 transition"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        <div className="p-4 space-y-3 text-xs">
          <div className="flex items-center justify-between p-2.5 rounded-lg bg-slate-900/60 border border-slate-800/80">
            <span className="text-slate-400">Harvestable Yield:</span>
            <span className="font-bold text-cyan-300">
              {anom.remainingQuantity} / {anom.initialQuantity} Units
            </span>
          </div>

          <div className="w-full bg-slate-800 h-2 rounded-full overflow-hidden">
            <div
              className="bg-cyan-500 h-full rounded-full transition-all"
              style={{ width: `${Math.max(5, (anom.remainingQuantity / anom.initialQuantity) * 100)}%` }}
            />
          </div>
        </div>

        <div className="px-4 py-2.5 bg-slate-900/80 border-t border-slate-800 flex items-center gap-2">
          <button
            onClick={() => onCenterEntity(anom.x, anom.y)}
            className="flex-1 flex items-center justify-center gap-1.5 py-1.5 px-2.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-medium transition"
          >
            <Crosshair className="w-3.5 h-3.5 text-cyan-400" />
            <span>Center</span>
          </button>
          <button
            onClick={() => openAnomalyDetail(anom.anomalyId)}
            className="flex-1 flex items-center justify-center gap-1.5 py-1.5 px-2.5 rounded-lg bg-cyan-700 hover:bg-cyan-600 text-white text-xs font-semibold shadow-md transition"
          >
            <ExternalLink className="w-3.5 h-3.5" />
            <span>Investigate</span>
          </button>
        </div>
      </div>
    );
  }

  // 4. EXPEDITION CARD
  if (selectedEntity.type === 'EXPEDITION') {
    const exp = mapSnapshot.expeditions.find((item) => item.expeditionId === selectedEntity.id);
    if (!exp) return null;

    return (
      <div className="absolute bottom-6 right-6 z-20 w-80 rounded-xl bg-slate-950/90 backdrop-blur-md border border-amber-800/60 shadow-2xl overflow-hidden text-slate-100 animate-in fade-in slide-in-from-bottom-4 duration-200">
        <div className="px-4 py-3 border-b border-amber-900/60 bg-gradient-to-r from-amber-950/40 via-slate-900 to-slate-900 flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-amber-500/10 border border-amber-500/30 flex items-center justify-center text-amber-400">
              <Compass className="w-4 h-4" />
            </div>
            <div>
              <h3 className="font-serif font-bold text-sm text-amber-200 leading-tight">
                Expedition Caravan
              </h3>
              <div className="flex items-center gap-2 mt-0.5">
                <span className="text-[10px] px-1.5 py-0.2 rounded font-semibold uppercase tracking-wider bg-amber-950 text-amber-300 border border-amber-700">
                  {exp.status}
                </span>
                <span className="text-[10px] text-slate-400 font-mono">
                  ({exp.currentX.toFixed(0)}, {exp.currentY.toFixed(0)})
                </span>
              </div>
            </div>
          </div>

          <button
            onClick={() => selectEntity(null)}
            className="p-1 text-slate-400 hover:text-white rounded-md hover:bg-slate-800/60 transition"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        <div className="p-4 space-y-3 text-xs">
          <div className="grid grid-cols-2 gap-2">
            <div className="p-2 rounded-lg bg-slate-900/60 border border-slate-800/80">
              <div className="text-[10px] text-slate-400 uppercase font-mono">Personnel</div>
              <div className="font-bold text-slate-200 text-sm mt-0.5">
                {exp.personnelCount} Explorers
              </div>
            </div>

            <div className="p-2 rounded-lg bg-slate-900/60 border border-slate-800/80">
              <div className="text-[10px] text-slate-400 uppercase font-mono">Carried Cargo</div>
              <div className="font-bold text-amber-300 text-sm mt-0.5">
                {exp.carriedCargo} Units
              </div>
            </div>
          </div>
        </div>

        <div className="px-4 py-2.5 bg-slate-900/80 border-t border-slate-800 flex items-center gap-2">
          <button
            onClick={() => onCenterEntity(exp.currentX, exp.currentY)}
            className="flex-1 flex items-center justify-center gap-1.5 py-1.5 px-2.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-medium transition"
          >
            <Crosshair className="w-3.5 h-3.5 text-cyan-400" />
            <span>Center</span>
          </button>
          <button
            onClick={() => openExpeditionDetail(exp.expeditionId)}
            className="flex-1 flex items-center justify-center gap-1.5 py-1.5 px-2.5 rounded-lg bg-amber-700 hover:bg-amber-600 text-white text-xs font-semibold shadow-md transition"
          >
            <ExternalLink className="w-3.5 h-3.5" />
            <span>Logbook</span>
          </button>
        </div>
      </div>
    );
  }

  return null;
};

