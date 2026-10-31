import React from 'react';
import {
  WorldMapSnapshot,
  MapSettlementDto,
  MapLairDto,
  MapAnomalyDto,
  MapExpeditionDto,
} from '../../types/api';
import { CameraState, WorldBounds, worldToScreen } from '../../utils/coordinates';
import { MapLayers, MapFilters, SelectedEntity, HoveredEntity } from '../../store/useMapStore';

interface WorldSvgOverlayProps {
  mapSnapshot: WorldMapSnapshot;
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

      {/* 1. Expedition Routes (Lines) */}
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

      {/* 2. Monster Lairs */}
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
            {/* Selection Aura */}
            {isSelected && (
              <circle
                r="22"
                fill="none"
                stroke="#f43f5e"
                strokeWidth="2.5"
                strokeDasharray="4 2"
                className="animate-spin"
                style={{ transformOrigin: 'center' }}
              />
            )}

            {/* Lair Shape: Diamond / Spiked Node */}
            <polygon
              points="0,-14 12,0 0,14 -12,0"
              fill={isDestroyed ? '#334155' : '#1e1b4b'}
              stroke={isDestroyed ? '#64748b' : badgeColor}
              strokeWidth="2"
              filter={!isDestroyed && lair.threatRank === 'MYTHIC' ? 'url(#glow-red)' : undefined}
            />

            {/* Monster skull/threat symbol */}
            <circle r="4" fill={isDestroyed ? '#64748b' : badgeColor} />

            {/* Count Badge */}
            <rect x="6" y="-12" width="16" height="10" rx="3" fill="#0f172a" stroke={badgeColor} strokeWidth="1" />
            <text x="14" y="-5" fontSize="8" fill="#ffffff" textAnchor="middle" fontWeight="bold">
              {lair.monsterCount}
            </text>

            {/* Lair Label */}
            {layers.labels && camera.zoom >= 0.8 && (
              <text
                x="0"
                y="24"
                textAnchor="middle"
                fontSize="10"
                fill={isAwakened ? '#f87171' : '#fca5a5'}
                fontWeight="600"
                className="drop-shadow-md select-none"
              >
                {lair.primarySpecies} Lair
              </text>
            )}
          </g>
        );
      })}

      {/* 3. Supernatural Anomalies */}
      {visibleAnomalies.map((anom) => {
        const { sx, sy } = worldToScreen(anom.x, anom.y, camera, baseScale, bounds);
        const isSelected = selectedEntity?.type === 'ANOMALY' && selectedEntity.id === anom.anomalyId;

        let gradeColor = '#06b6d4'; // Cyan default
        if (anom.resourceGrade === 'LEGENDARY') gradeColor = '#f59e0b';
        else if (anom.resourceGrade === 'MYTHIC') gradeColor = '#ec4899';
        else if (anom.resourceGrade === 'RARE') gradeColor = '#8b5cf6';

        return (
          <g
            key={`anom-${anom.anomalyId}`}
            transform={`translate(${sx}, ${sy})`}
            className="cursor-pointer transition-transform hover:scale-125"
            onClick={(e) => {
              e.stopPropagation();
              onSelectEntity({ type: 'ANOMALY', id: anom.anomalyId });
            }}
            onMouseEnter={() =>
              onHoverEntity({
                type: 'ANOMALY',
                id: anom.anomalyId,
                name: `Aether Anomaly [${anom.resourceGrade}]`,
                x: anom.x,
                y: anom.y,
                details: `${anom.resourceCategory} | Reserves: ${anom.remainingQuantity} | State: ${anom.state}`,
                screenX: sx,
                screenY: sy,
              })
            }
            onMouseLeave={() => onHoverEntity(null)}
          >
            {/* Selection Beacon */}
            {isSelected && (
              <circle
                r="18"
                fill="none"
                stroke={gradeColor}
                strokeWidth="2"
                strokeDasharray="3 3"
                className="animate-spin"
                style={{ transformOrigin: 'center' }}
              />
            )}

            {/* Glowing Diamond */}
            <polygon
              points="0,-10 9,0 0,10 -9,0"
              fill={gradeColor}
              fillOpacity="0.85"
              stroke="#ffffff"
              strokeWidth="1.5"
              filter={anom.resourceGrade === 'MYTHIC' || anom.resourceGrade === 'LEGENDARY' ? 'url(#glow-gold)' : 'url(#glow-cyan)'}
            />

            {/* Internal Core */}
            <circle r="3" fill="#ffffff" />

            {/* Label */}
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

      {/* 4. Active Expeditions */}
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

      {/* 5. Settlements (Top Layer for interaction priority) */}
      {visibleSettlements.map((s) => {
        const { sx, sy } = worldToScreen(s.x, s.y, camera, baseScale, bounds);
        const isSelected = selectedEntity?.type === 'SETTLEMENT' && selectedEntity.id === s.id;

        // Bounded logarithmic / square-root scaling:
        // Min radius: 10px, Max radius: 30px
        const radius = Math.min(30, Math.max(10, 8 + Math.sqrt(Math.max(1, s.population)) * 0.35));

        // Tier Color & Outer Ring
        let tierColor = '#3b82f6'; // HAMLET
        let tierBorder = '#60a5fa';
        if (s.tier === 'VILLAGE') { tierColor = '#10b981'; tierBorder = '#34d399'; }
        else if (s.tier === 'TOWN') { tierColor = '#8b5cf6'; tierBorder = '#a78bfa'; }
        else if (s.tier === 'CITY') { tierColor = '#f59e0b'; tierBorder = '#fbbf24'; }
        else if (s.tier === 'MEGACITY') { tierColor = '#ec4899'; tierBorder = '#f472b6'; }

        return (
          <g
            key={`settlement-${s.id}`}
            transform={`translate(${sx}, ${sy})`}
            className="cursor-pointer transition-transform hover:scale-115"
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
                details: `${s.tier} (${s.biome}) | Pop: ${s.population} | Tech: ${s.techLevel}${s.inFamine ? ' | FAMINE!' : ''}`,
                screenX: sx,
                screenY: sy,
              })
            }
            onMouseLeave={() => onHoverEntity(null)}
          >
            {/* Selection Pulse Ring */}
            {isSelected && (
              <circle
                r={radius + 8}
                fill="none"
                stroke={isAwakened ? '#06b6d4' : '#eab308'}
                strokeWidth="2.5"
                strokeDasharray="4 2"
                className="animate-spin"
                style={{ transformOrigin: 'center' }}
              />
            )}

            {/* Settlement Outer Body */}
            <circle
              r={radius}
              fill={tierColor}
              stroke={tierBorder}
              strokeWidth="2.5"
              fillOpacity={0.88}
              filter="drop-shadow(0px 2px 4px rgba(0,0,0,0.5))"
            />

            {/* Multi-ring Citadel indicator for City/Megacity */}
            {(s.tier === 'CITY' || s.tier === 'MEGACITY') && (
              <circle r={radius * 0.65} fill="none" stroke="#ffffff" strokeWidth="1.5" strokeDasharray="3 2" />
            )}

            {/* Inner Core */}
            <circle r={radius * 0.35} fill="#ffffff" />

            {/* Famine Warning Alert */}
            {s.inFamine && (
              <g transform={`translate(${radius * 0.7}, -${radius * 0.7})`}>
                <circle r="7" fill="#ef4444" stroke="#ffffff" strokeWidth="1" />
                <text x="0" y="3" fill="#ffffff" fontSize="8" fontWeight="bold" textAnchor="middle">
                  !
                </text>
              </g>
            )}

            {/* Settlement Name & Population Label */}
            {layers.labels && (
              <g transform={`translate(0, ${radius + 12})`}>
                <rect
                  x="-45"
                  y="-10"
                  width="90"
                  height="16"
                  rx="4"
                  fill="rgba(15, 23, 42, 0.85)"
                  stroke={isSelected ? '#38bdf8' : 'rgba(71, 85, 105, 0.5)'}
                  strokeWidth="1"
                />
                <text
                  x="0"
                  y="2"
                  textAnchor="middle"
                  fontSize="9.5"
                  fill="#f8fafc"
                  fontWeight="bold"
                  className="select-none"
                >
                  {s.name} ({s.population})
                </text>
              </g>
            )}
          </g>
        );
      })}
    </svg>
  );
};

