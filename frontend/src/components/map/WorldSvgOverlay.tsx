import React, { useState, useEffect } from 'react';
import {
  WorldMapSnapshot,
  WorldOverlaysDto,
  MapSettlementDto,
  MapLairDto,
  MapAnomalyDto,
  MapExpeditionDto,
} from '../../types/api';
import { CameraState, WorldBounds, worldToScreen } from '../../utils/coordinates';
import { MapLayers, MapFilters, SelectedEntity, HoveredEntity, useMapStore } from '../../store/useMapStore';
import { SettlementIllustratedMarker } from './SettlementIllustratedMarker';

interface WorldSvgOverlayProps {
  mapSnapshot: WorldMapSnapshot;
  overlays?: WorldOverlaysDto | null;
  camera: CameraState;
  baseScale: number;
  bounds: WorldBounds;
  layers: MapLayers;
  filters: MapFilters;
  selectedEntity: SelectedEntity | null;
  onSelectEntity: (entity: SelectedEntity | null) => void;
  onHoverEntity: (entity: HoveredEntity | null) => void;
  isAwakened: boolean;
  width: number;
  height: number;
}

export const WorldSvgOverlay: React.FC<WorldSvgOverlayProps> = ({
  mapSnapshot,
  overlays,
  camera,
  baseScale,
  bounds,
  layers,
  filters,
  selectedEntity,
  onSelectEntity,
  onHoverEntity,
  isAwakened,
  width,
  height,
}) => {
  const openBattleDetail = useMapStore((s) => s.openBattleDetail);
  const enableAnimations = useMapStore((s) => s.enableAnimations);
  const reducedMotion = useMapStore((s) => s.reducedMotion);
  const animationSpeed = useMapStore((s) => s.animationSpeed);
  const dynamicOverlays = useMapStore((s) => s.dynamicOverlays);
  const hoveredEntity = useMapStore((s) => s.hoveredEntity);

  // Time ticker for animation phase offset
  const [animTick, setAnimTick] = useState<number>(0);

  useEffect(() => {
    if (!enableAnimations || reducedMotion) return;
    const interval = setInterval(() => {
      setAnimTick((t) => (t + 0.05 * animationSpeed) % 1);
    }, 50);
    return () => clearInterval(interval);
  }, [enableAnimations, reducedMotion, animationSpeed]);

  // Filter settlements
  const visibleSettlements = mapSnapshot.settlements.filter((s) => {
    if (!layers.settlements) return false;
    if (filters.settlementBiome !== 'ALL' && s.biome !== filters.settlementBiome) return false;
    if (filters.settlementTier !== 'ALL' && s.tier !== filters.settlementTier) return false;
    if (s.population < filters.minPopulation) return false;
    return true;
  });

  // Filter lairs
  const visibleLairs = mapSnapshot.lairs.filter((l) => {
    if (!layers.lairs) return false;
    if (filters.lairThreat !== 'ALL' && l.threatRank !== filters.lairThreat) return false;
    return true;
  });

  // Filter anomalies (backend already ensures observable)
  const visibleAnomalies = mapSnapshot.anomalies.filter((a) => {
    if (!layers.anomalies) return false;
    if (filters.anomalyGrade !== 'ALL' && a.resourceGrade !== filters.anomalyGrade) return false;
    return true;
  });

  // Expeditions
  const visibleExpeditions = layers.expeditions ? mapSnapshot.expeditions : [];

  return (
    <svg
      width={width}
      height={height}
      className="absolute top-0 left-0 w-full h-full pointer-events-auto"
      style={{ overflow: 'hidden' }}
    >
      <defs>
        {/* Glow Filters */}
        <filter id="glow-cyan" x="-50%" y="-50%" width="200%" height="200%">
          <feGaussianBlur stdDeviation="3" result="blur" />
          <feComposite in="SourceGraphic" in2="blur" operator="over" />
        </filter>
        <filter id="glow-gold" x="-50%" y="-50%" width="200%" height="200%">
          <feGaussianBlur stdDeviation="3" result="blur" />
          <feComposite in="SourceGraphic" in2="blur" operator="over" />
        </filter>
        <filter id="glow-red" x="-50%" y="-50%" width="200%" height="200%">
          <feGaussianBlur stdDeviation="4" result="blur" />
          <feComposite in="SourceGraphic" in2="blur" operator="over" />
        </filter>
      </defs>

      {/* 1. Trade Caravan Animated Route Flow */}
      {dynamicOverlays.tradeRoutes &&
        overlays?.diplomaticLinks?.map((link, idx) => {
          if (link.relationType !== 'ALLIED') return null;
          const p1 = worldToScreen(link.originX, link.originY, camera, baseScale, bounds);
          const p2 = worldToScreen(link.targetX, link.targetY, camera, baseScale, bounds);

          const curRatio = enableAnimations && !reducedMotion ? (animTick + (idx * 0.25) % 1) % 1 : 0.5;
          const dotX = p1.sx + (p2.sx - p1.sx) * curRatio;
          const dotY = p1.sy + (p2.sy - p1.sy) * curRatio;

          return (
            <g key={`trade-flow-${idx}`} className="pointer-events-none">
              <circle cx={dotX} cy={dotY} r="3" fill="#fbbf24" stroke="#78350f" strokeWidth="1" />
            </g>
          );
        })}

      {/* 2. Monster Invasion Wavefront Vectors */}
      {dynamicOverlays.monsterThreat &&
        overlays?.monsterInvasions?.map((inv) => {
          const src = worldToScreen(inv.sourceX, inv.sourceY, camera, baseScale, bounds);
          const tgt = worldToScreen(inv.targetX, inv.targetY, camera, baseScale, bounds);
          const cur = worldToScreen(inv.currentX, inv.currentY, camera, baseScale, bounds);

          return (
            <g key={`inv-${inv.tideId}`} className="pointer-events-none">
              {/* Invasion corridor */}
              <line
                x1={src.sx}
                y1={src.sy}
                x2={tgt.sx}
                y2={tgt.sy}
                stroke="rgba(239, 68, 68, 0.35)"
                strokeWidth="2.5"
                strokeDasharray="4 4"
              />
              {/* Vanguard Wavefront Marker */}
              <circle
                cx={cur.sx}
                cy={cur.sy}
                r="7"
                fill="none"
                stroke="#ef4444"
                strokeWidth="2"
                className={enableAnimations && !reducedMotion ? 'animate-ping' : ''}
              />
              <polygon
                points={`${cur.sx},${cur.sy - 6} ${cur.sx + 6},${cur.sy + 6} ${cur.sx - 6},${cur.sy + 6}`}
                fill="#b91c1c"
                stroke="#fca5a5"
                strokeWidth="1.5"
              />
            </g>
          );
        })}

      {/* 3. Expedition Routes & Moving Tokens */}
      {visibleExpeditions.map((exp) => {
        const originVillage = mapSnapshot.settlements.find((v) => String(v.id) === exp.originVillageId);
        const targetAnomaly = mapSnapshot.anomalies.find((a) => a.anomalyId === exp.targetAnomalyId);
        if (!originVillage) return null;

        const originScreen = worldToScreen(originVillage.x, originVillage.y, camera, baseScale, bounds);
        const curScreen = worldToScreen(exp.currentX, exp.currentY, camera, baseScale, bounds);
        const targetScreen = targetAnomaly
          ? worldToScreen(targetAnomaly.x, targetAnomaly.y, camera, baseScale, bounds)
          : null;

        return (
          <g key={`exp-route-${exp.expeditionId}`}>
            <line
              x1={originScreen.sx}
              y1={originScreen.sy}
              x2={curScreen.sx}
              y2={curScreen.sy}
              stroke="rgba(245, 158, 11, 0.4)"
              strokeWidth="2"
              strokeDasharray="4 4"
            />
            {targetScreen && (
              <line
                x1={curScreen.sx}
                y1={curScreen.sy}
                x2={targetScreen.sx}
                y2={targetScreen.sy}
                stroke="rgba(148, 163, 184, 0.25)"
                strokeWidth="1.5"
                strokeDasharray="2 4"
              />
            )}
          </g>
        );
      })}

      {/* 4. Active Battles & Conflicts Indicators */}
      {dynamicOverlays.activeConflicts &&
        overlays?.activeConflicts?.map((conflict) => {
          const { sx, sy } = worldToScreen(conflict.x, conflict.y, camera, baseScale, bounds);

          return (
            <g
              key={`conflict-${conflict.conflictId}`}
              transform={`translate(${sx}, ${sy})`}
              className="cursor-pointer transition-transform hover:scale-125"
              onClick={(e) => {
                e.stopPropagation();
                openBattleDetail(conflict.conflictId);
              }}
              onMouseEnter={() =>
                onHoverEntity({
                  type: 'SETTLEMENT',
                  id: conflict.conflictId,
                  name: `Active Conflict: ${conflict.name}`,
                  x: conflict.x,
                  y: conflict.y,
                  details: `Casualties: ${conflict.totalCombatants} | Type: ${conflict.conflictType} | Year ${conflict.year}`,
                  screenX: sx,
                  screenY: sy,
                })
              }
              onMouseLeave={() => onHoverEntity(null)}
            >
              {/* Pulsing Alert Ring */}
              <circle
                r="18"
                fill="none"
                stroke="#ef4444"
                strokeWidth="2"
                strokeDasharray="3 3"
                className={enableAnimations && !reducedMotion ? 'animate-spin' : ''}
              />
              <circle r="12" fill="#7f1d1d" stroke="#fca5a5" strokeWidth="1.5" />
              {/* Crossed swords symbol */}
              <text x="0" y="4" fontSize="11" textAnchor="middle" fill="#ffffff">
                ⚔
              </text>
              {/* Casualty Badge */}
              <rect x="5" y="-14" width="16" height="10" rx="3" fill="#1e1b4b" stroke="#f43f5e" strokeWidth="1" />
              <text x="13" y="-7" fontSize="8" fill="#fda4af" textAnchor="middle" fontWeight="bold">
                {conflict.totalCombatants}
              </text>
            </g>
          );
        })}

      {/* 5. Monster Lairs */}
      {visibleLairs.map((lair) => {
        const { sx, sy } = worldToScreen(lair.x, lair.y, camera, baseScale, bounds);
        const isSelected = selectedEntity?.type === 'LAIR' && selectedEntity.id === lair.lairId;
        const isDestroyed = lair.state === 'DESTROYED';

        let badgeColor = '#94a3b8'; // COMMON
        if (lair.threatRank === 'UNCOMMON') badgeColor = '#22c55e';
        else if (lair.threatRank === 'RARE') badgeColor = '#3b82f6';
        else if (lair.threatRank === 'LEGENDARY') badgeColor = '#eab308';
        else if (lair.threatRank === 'MYTHIC') badgeColor = '#ef4444';

        return (
          <g
            key={`lair-${lair.lairId}`}
            transform={`translate(${sx}, ${sy})`}
            className="cursor-pointer transition-transform hover:scale-110"
            onClick={(e) => {
              e.stopPropagation();
              onSelectEntity({ type: 'LAIR', id: lair.lairId });
            }}
            onMouseEnter={() =>
              onHoverEntity({
                type: 'LAIR',
                id: lair.lairId,
                name: `Monster Lair (${lair.primarySpecies})`,
                x: lair.x,
                y: lair.y,
                details: `Threat: ${lair.threatRank} | Monsters: ${lair.monsterCount} | State: ${lair.state}`,
                screenX: sx,
                screenY: sy,
              })
            }
            onMouseLeave={() => onHoverEntity(null)}
          >
            {isSelected && (
              <circle
                r="22"
                fill="none"
                stroke="#f43f5e"
                strokeWidth="2.5"
                strokeDasharray="4 2"
                className={enableAnimations && !reducedMotion ? 'animate-spin' : ''}
                style={{ transformOrigin: 'center' }}
              />
            )}

            {/* Illustrated Beast Den / Cavern Marker */}
            {isDestroyed ? (
              <g>
                <circle r="10" fill="#334155" stroke="#64748b" strokeWidth="1.5" />
                <line x1="-6" y1="-6" x2="6" y2="6" stroke="#94a3b8" strokeWidth="2" />
                <line x1="-6" y1="6" x2="6" y2="-6" stroke="#94a3b8" strokeWidth="2" />
              </g>
            ) : (
              <g filter="drop-shadow(0px 2px 5px rgba(0,0,0,0.8))">
                <polygon
                  points="-14,10 -11,-8 -2,-15 11,-8 14,10 0,6"
                  fill="#1c1917"
                  stroke={badgeColor}
                  strokeWidth="1.8"
                />
                <path d="M -7,8 Q 0,-2 7,8 Z" fill="#09090b" />
                <circle
                  cx="-2.5"
                  cy="4"
                  r="1.2"
                  fill={lair.threatRank === 'MYTHIC' ? '#ef4444' : '#f97316'}
                  className={enableAnimations && !reducedMotion ? 'animate-pulse' : ''}
                />
                <circle
                  cx="2.5"
                  cy="4"
                  r="1.2"
                  fill={lair.threatRank === 'MYTHIC' ? '#ef4444' : '#f97316'}
                  className={enableAnimations && !reducedMotion ? 'animate-pulse' : ''}
                />
              </g>
            )}

            {!isDestroyed && (
              <g transform="translate(10, -12)">
                <rect x="-4" y="-7" width="22" height="13" rx="3.5" fill="#0f172a" stroke={badgeColor} strokeWidth="1" />
                <text x="7" y="2" fontSize="8.5" fill="#ffffff" textAnchor="middle" fontWeight="bold">
                  {lair.monsterCount}
                </text>
              </g>
            )}

            {layers.labels && camera.zoom >= 0.8 && (
              <text
                x="0"
                y="24"
                textAnchor="middle"
                fontSize="10"
                fill={badgeColor}
                fontWeight="bold"
                className="drop-shadow select-none"
              >
                {lair.primarySpecies}
              </text>
            )}
          </g>
        );
      })}

      {/* 6. Aether Anomalies */}
      {visibleAnomalies.map((anom) => {
        const { sx, sy } = worldToScreen(anom.x, anom.y, camera, baseScale, bounds);
        const isSelected = selectedEntity?.type === 'ANOMALY' && selectedEntity.id === anom.anomalyId;

        let gradeColor = '#94a3b8';
        if (anom.resourceGrade === 'RARE') gradeColor = '#38bdf8';
        else if (anom.resourceGrade === 'LEGENDARY') gradeColor = '#fbbf24';
        else if (anom.resourceGrade === 'MYTHIC') gradeColor = '#f43f5e';

        return (
          <g
            key={`anom-${anom.anomalyId}`}
            transform={`translate(${sx}, ${sy})`}
            className="cursor-pointer transition-transform hover:scale-110"
            onClick={(e) => {
              e.stopPropagation();
              onSelectEntity({ type: 'ANOMALY', id: anom.anomalyId });
            }}
            onMouseEnter={() =>
              onHoverEntity({
                type: 'ANOMALY',
                id: anom.anomalyId,
                name: `${anom.resourceGrade} ${anom.resourceCategory}`,
                x: anom.x,
                y: anom.y,
                details: `State: ${anom.state} | Remaining: ${anom.remainingQuantity}/${anom.initialQuantity}`,
                screenX: sx,
                screenY: sy,
              })
            }
            onMouseLeave={() => onHoverEntity(null)}
          >
            {isSelected && (
              <circle
                r="18"
                fill="none"
                stroke={gradeColor}
                strokeWidth="2"
                strokeDasharray="4 2"
                className={enableAnimations && !reducedMotion ? 'animate-spin' : ''}
              />
            )}

            {/* Illustrated Ancient Arcane Obelisk */}
            <g filter={anom.resourceGrade === 'MYTHIC' ? 'url(#glow-cyan)' : 'drop-shadow(0px 2px 4px rgba(0,0,0,0.7))'}>
              <circle r="9" fill="none" stroke={gradeColor} strokeWidth="1" strokeDasharray="3 2" />
              <polygon
                points="0,-12 6,-3 4,7 0,11 -4,7 -6,-3"
                fill="#0f172a"
                stroke={gradeColor}
                strokeWidth="1.8"
              />
              <circle
                cx="0"
                cy="0"
                r="2.5"
                fill={gradeColor}
                className={enableAnimations && !reducedMotion ? 'animate-pulse' : ''}
              />
              <polygon points="8,-4 10,-2 8,0 6,-2" fill={gradeColor} />
              <polygon points="-8,2 -10,4 -8,6 -6,4" fill={gradeColor} />
            </g>

            {layers.labels && camera.zoom >= 0.9 && (
              <text
                x="0"
                y="18"
                textAnchor="middle"
                fontSize="9"
                fill={gradeColor}
                fontWeight="bold"
                className="drop-shadow select-none"
              >
                {anom.resourceCategory.replace(/_/g, ' ')}
              </text>
            )}
          </g>
        );
      })}

      {/* 7. Expeditions */}
      {visibleExpeditions.map((exp) => {
        const { sx, sy } = worldToScreen(exp.currentX, exp.currentY, camera, baseScale, bounds);
        const isSelected = selectedEntity?.type === 'EXPEDITION' && selectedEntity.id === exp.expeditionId;

        return (
          <g
            key={`exp-${exp.expeditionId}`}
            transform={`translate(${sx}, ${sy})`}
            className="cursor-pointer transition-transform hover:scale-110"
            onClick={(e) => {
              e.stopPropagation();
              onSelectEntity({ type: 'EXPEDITION', id: exp.expeditionId });
            }}
            onMouseEnter={() =>
              onHoverEntity({
                type: 'EXPEDITION',
                id: exp.expeditionId,
                name: `Expedition ${exp.expeditionId}`,
                x: exp.currentX,
                y: exp.currentY,
                details: `Party: ${exp.personnelCount} | Cargo: ${exp.carriedCargo} | Status: ${exp.status}`,
                screenX: sx,
                screenY: sy,
              })
            }
            onMouseLeave={() => onHoverEntity(null)}
          >
            {isSelected && (
              <circle r="14" fill="none" stroke="#f59e0b" strokeWidth="2" strokeDasharray="3 3" />
            )}
            <polygon points="-7,7 7,7 0,-8" fill="#d97706" stroke="#fef08a" strokeWidth="1.5" />
            <circle cx="0" cy="2" r="2.5" fill="#fef08a" />
          </g>
        );
      })}

      {/* 8. Settlements (Illustrated Architectural Markers with Progressive LOD) */}
      {visibleSettlements.map((s) => {
        const { sx, sy } = worldToScreen(s.x, s.y, camera, baseScale, bounds);
        const isSelected = selectedEntity?.type === 'SETTLEMENT' && selectedEntity.id === s.id;
        const isHovered = hoveredEntity?.type === 'SETTLEMENT' && hoveredEntity.id === s.id;

        // Label collision avoidance: hide small settlement labels when overcrowded unless hovered/selected
        const isLabelCrowded =
          camera.zoom < 1.4 &&
          visibleSettlements.some(
            (other) =>
              other.id !== s.id &&
              other.population > s.population &&
              Math.hypot(s.x - other.x, s.y - other.y) * baseScale * camera.zoom < 75
          );

        return (
          <g
            key={`settlement-${s.id}`}
            transform={`translate(${sx}, ${sy})`}
            className="transition-transform hover:scale-110"
            onClick={(e) => {
              e.stopPropagation();
              onSelectEntity({ type: 'SETTLEMENT', id: s.id });
            }}
            onMouseEnter={() =>
              onHoverEntity({
                type: 'SETTLEMENT',
                id: s.id,
                name: s.name,
                x: s.x,
                y: s.y,
                details: `${s.tier} (${s.biome}) | Pop: ${s.population.toLocaleString()} | Tech: Tier ${s.techLevel}${
                  s.dominantClanName ? ` | Clan: ${s.dominantClanName}` : ''
                }${s.inFamine ? ' | FAMINE!' : ''}`,
                screenX: sx,
                screenY: sy,
              })
            }
            onMouseLeave={() => onHoverEntity(null)}
          >
            <SettlementIllustratedMarker
              settlement={s}
              zoom={camera.zoom}
              isSelected={isSelected}
              isHovered={isHovered}
              isAwakened={isAwakened}
              enableAnimations={enableAnimations}
              reducedMotion={reducedMotion}
              showLabels={
                layers.labels &&
                (camera.zoom >= 0.75 || isSelected || isHovered) &&
                (!isLabelCrowded || isSelected || isHovered)
              }
            />
          </g>
        );
      })}
    </svg>
  );
};
