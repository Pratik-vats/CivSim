import React from 'react';
import { MapSettlementDto } from '../../types/api';

interface SettlementIllustratedMarkerProps {
  settlement: MapSettlementDto;
  zoom: number;
  isSelected: boolean;
  isHovered: boolean;
  isAwakened: boolean;
  enableAnimations: boolean;
  reducedMotion: boolean;
  showLabels: boolean;
}

export const SettlementIllustratedMarker: React.FC<SettlementIllustratedMarkerProps> = ({
  settlement,
  zoom,
  isSelected,
  isHovered,
  isAwakened,
  enableAnimations,
  reducedMotion,
  showLabels,
}) => {
  const { tier, biome, population, fortificationLevel, inFamine, name, dominantClanName } = settlement;

  // Biome color palettes
  const palette = React.useMemo(() => {
    switch (biome) {
      case 'HIGHLAND_MINE':
        return {
          wall: '#475569', // Dark granite
          wallTrim: '#334155',
          roof: '#3b82f6', // Slate blue
          roofTrim: '#1d4ed8',
          accent: '#94a3b8',
          ground: '#334155',
          banner: '#60a5fa',
        };
      case 'FOREST':
        return {
          wall: '#78350f', // Deep timber
          wallTrim: '#451a03',
          roof: '#15803d', // Moss green
          roofTrim: '#166534',
          accent: '#84cc16',
          ground: '#27272a',
          banner: '#22c55e',
        };
      case 'COASTAL':
        return {
          wall: '#e2e8f0', // Whitewashed stone
          wallTrim: '#cbd5e1',
          roof: '#ea580c', // Terracotta orange
          roofTrim: '#c2410c',
          accent: '#38bdf8',
          ground: '#71717a',
          banner: '#0ea5e9',
        };
      case 'RIVER_VALLEY':
      default:
        return {
          wall: '#854d0e', // Timber & plaster
          wallTrim: '#713f12',
          roof: '#d97706', // Thatch & clay
          roofTrim: '#b45309',
          accent: '#fbbf24',
          ground: '#3f3f46',
          banner: '#eab308',
        };
    }
  }, [biome]);

  // Dynamic scale based on zoom (normalized to preserve clarity)
  // Low zoom: compact heraldic icon. Medium zoom: architectural cluster. High zoom: detailed view.
  const isWorldLOD = zoom < 0.75;
  const isHighLOD = zoom > 1.7;

  // Render Low LOD (World Zoom: Heraldic Silhouette Crest)
  if (isWorldLOD) {
    const crestRadius = tier === 'MEGACITY' ? 16 : tier === 'CITY' ? 14 : tier === 'TOWN' ? 11 : tier === 'VILLAGE' ? 9 : 7;
    return (
      <g className="cursor-pointer">
        {/* Glow halo on selection */}
        {isSelected && (
          <circle
            r={crestRadius + 6}
            fill="none"
            stroke={isAwakened ? '#38bdf8' : '#fbbf24'}
            strokeWidth="2"
            strokeDasharray="4 2"
            className={enableAnimations && !reducedMotion ? 'animate-spin' : ''}
          />
        )}
        {/* Outer shield/circle */}
        <circle
          r={crestRadius}
          fill={palette.ground}
          stroke={isSelected ? '#ffffff' : palette.roof}
          strokeWidth="2"
          filter="drop-shadow(0px 2px 4px rgba(0,0,0,0.6))"
        />
        {/* Tier symbol inside crest */}
        {tier === 'HAMLET' && (
          <path d="M -3 2 L 0 -3 L 3 2 Z" fill={palette.accent} />
        )}
        {tier === 'VILLAGE' && (
          <path d="M -4 3 L -4 -1 L 0 -4 L 4 -1 L 4 3 Z" fill={palette.roof} />
        )}
        {tier === 'TOWN' && (
          <path d="M -5 4 L -5 -2 L -2 -2 L -2 -4 L 2 -4 L 2 -2 L 5 -2 L 5 4 Z" fill={palette.accent} />
        )}
        {tier === 'CITY' && (
          <g>
            <path d="M -6 5 L -6 -3 L -3 -3 L -3 -5 L 0 -6 L 3 -5 L 3 -3 L 6 -3 L 6 5 Z" fill={palette.roof} />
            <circle cx="0" cy="0" r="1.5" fill="#ffffff" />
          </g>
        )}
        {tier === 'MEGACITY' && (
          <g>
            <path d="M -8 6 L -8 -4 L -4 -4 L -4 -7 L 0 -9 L 4 -7 L 4 -4 L 8 -4 L 8 6 Z" fill={palette.accent} />
            <circle cx="0" cy="-2" r="2" fill="#ffffff" />
          </g>
        )}
        {/* Settlement name plate on world zoom only if selected or hovered */}
        {(isSelected || isHovered) && (
          <g transform={`translate(0, ${crestRadius + 10})`}>
            <rect
              x="-40"
              y="-7"
              width="80"
              height="14"
              rx="3"
              fill="rgba(10, 15, 26, 0.92)"
              stroke={palette.roof}
              strokeWidth="1"
            />
            <text x="0" y="3" fill="#ffffff" fontSize="8" fontWeight="bold" textAnchor="middle">
              {name}
            </text>
          </g>
        )}
      </g>
    );
  }

  // Medium and High LOD: Architectural procedural rendering
  return (
    <g className="cursor-pointer select-none" filter="drop-shadow(0px 3px 6px rgba(0,0,0,0.7))">
      {/* Selection Animated Aura / Sunburst Ring */}
      {isSelected && (
        <g>
          <circle
            r={tier === 'MEGACITY' ? 44 : tier === 'CITY' ? 38 : tier === 'TOWN' ? 30 : tier === 'VILLAGE' ? 24 : 18}
            fill="none"
            stroke={isAwakened ? '#06b6d4' : '#f59e0b'}
            strokeWidth="2.5"
            strokeDasharray="6 3"
            className={enableAnimations && !reducedMotion ? 'animate-spin' : ''}
            style={{ transformOrigin: 'center', animationDuration: '10s' }}
          />
          <circle
            r={tier === 'MEGACITY' ? 48 : tier === 'CITY' ? 42 : tier === 'TOWN' ? 34 : tier === 'VILLAGE' ? 28 : 22}
            fill="none"
            stroke={isAwakened ? 'rgba(6, 182, 212, 0.3)' : 'rgba(245, 158, 11, 0.3)'}
            strokeWidth="1"
          />
        </g>
      )}

      {/* Fortification Outer Defensive Ring / Wall */}
      {fortificationLevel > 0 && (
        <g>
          {fortificationLevel <= 2 ? (
            /* Wooden Palisade Fence (Dotted timber posts) */
            <circle
              r={tier === 'MEGACITY' ? 40 : tier === 'CITY' ? 34 : tier === 'TOWN' ? 26 : tier === 'VILLAGE' ? 20 : 16}
              fill="none"
              stroke="#78350f"
              strokeWidth="2.5"
              strokeDasharray="2 3"
            />
          ) : (
            /* Heavy Stone Curtain Wall with Bastions */
            <g>
              <circle
                r={tier === 'MEGACITY' ? 42 : tier === 'CITY' ? 35 : 27}
                fill="none"
                stroke="#64748b"
                strokeWidth="3.5"
                strokeDasharray="8 3"
              />
              {/* Corner Bastions (Round stone towers) */}
              <circle cx="-25" cy="-20" r="3" fill="#475569" stroke="#94a3b8" strokeWidth="1" />
              <circle cx="25" cy="-20" r="3" fill="#475569" stroke="#94a3b8" strokeWidth="1" />
              <circle cx="-25" cy="20" r="3" fill="#475569" stroke="#94a3b8" strokeWidth="1" />
              <circle cx="25" cy="20" r="3" fill="#475569" stroke="#94a3b8" strokeWidth="1" />
            </g>
          )}
        </g>
      )}

      {/* Ground Clearing / Plaza Base */}
      <ellipse
        cx="0"
        cy="2"
        rx={tier === 'MEGACITY' ? 36 : tier === 'CITY' ? 30 : tier === 'TOWN' ? 22 : tier === 'VILLAGE' ? 16 : 12}
        ry={tier === 'MEGACITY' ? 24 : tier === 'CITY' ? 20 : tier === 'TOWN' ? 15 : tier === 'VILLAGE' ? 11 : 8}
        fill={isAwakened ? '#111827' : '#292524'}
        fillOpacity="0.85"
      />

      {/* ------------------------------------------------------------- */}
      {/* TIER 1: HAMLET (< 50) — Small rustic cluster of thatched huts */}
      {/* ------------------------------------------------------------- */}
      {tier === 'HAMLET' && (
        <g>
          {/* Cottage 1 (Left) */}
          <rect x="-9" y="-4" width="7" height="6" fill={palette.wall} stroke={palette.wallTrim} strokeWidth="0.8" />
          <polygon points="-11,-4 -5.5,-9 0,-4" fill={palette.roof} stroke={palette.roofTrim} strokeWidth="0.8" />

          {/* Cottage 2 (Right, slightly behind) */}
          <rect x="2" y="-7" width="8" height="7" fill={palette.wall} stroke={palette.wallTrim} strokeWidth="0.8" />
          <polygon points="0,-7 6,-12 12,-7" fill={palette.roof} stroke={palette.roofTrim} strokeWidth="0.8" />

          {/* Wooden Cart / Barrel in front */}
          <rect x="-3" y="3" width="3" height="2" fill="#78350f" rx="0.5" />
          <circle cx="-1.5" cy="4" r="1" fill="#451a03" />

          {/* High LOD: Chimney smoke */}
          {isHighLOD && enableAnimations && !reducedMotion && (
            <circle cx="8" cy="-14" r="1.5" fill="rgba(255,255,255,0.4)" className="animate-pulse" />
          )}
        </g>
      )}

      {/* ------------------------------------------------------------- */}
      {/* TIER 2: VILLAGE (50+) — Thatched/timber houses & longhouse */}
      {/* ------------------------------------------------------------- */}
      {tier === 'VILLAGE' && (
        <g>
          {/* Left Farmhouse */}
          <rect x="-14" y="-3" width="8" height="7" fill={palette.wall} stroke={palette.wallTrim} strokeWidth="0.8" />
          <polygon points="-16,-3 -10,-8 -4,-3" fill={palette.roof} stroke={palette.roofTrim} strokeWidth="0.8" />

          {/* Central Longhouse / Granary */}
          <rect x="-6" y="-8" width="12" height="9" fill={palette.wall} stroke={palette.wallTrim} strokeWidth="1" />
          <polygon points="-8,-8 0,-14 8,-8" fill={palette.roof} stroke={palette.roofTrim} strokeWidth="1" />
          {/* Longhouse door */}
          <rect x="-1.5" y="-2" width="3" height="3" fill="#1c1917" />

          {/* Right Workshop / Cottage */}
          <rect x="7" y="-2" width="8" height="6" fill={palette.wall} stroke={palette.wallTrim} strokeWidth="0.8" />
          <polygon points="5,-2 11,-7 17,-2" fill={palette.roof} stroke={palette.roofTrim} strokeWidth="0.8" />

          {/* Fenced Paddock / Haystack */}
          <rect x="-12" y="5" width="7" height="4" fill="none" stroke="#78350f" strokeWidth="0.8" strokeDasharray="1 1" />
          <circle cx="-8" cy="7" r="1.5" fill="#ca8a04" />
        </g>
      )}

      {/* ------------------------------------------------------------- */}
      {/* TIER 3: TOWN (250+) — Cobblestone market plaza, town hall & gate */}
      {/* ------------------------------------------------------------- */}
      {tier === 'TOWN' && (
        <g>
          {/* Paved Town Square Base */}
          <rect x="-10" y="-2" width="20" height="10" fill="#3f3f46" rx="2" stroke="#52525b" strokeWidth="0.5" />

          {/* Left Quarter Townhouses */}
          <rect x="-19" y="-6" width="9" height="9" fill={palette.wall} stroke={palette.wallTrim} strokeWidth="0.8" />
          <polygon points="-21,-6 -14.5,-12 -8,-6" fill={palette.roof} stroke={palette.roofTrim} strokeWidth="0.8" />

          {/* Right Quarter Townhouses */}
          <rect x="10" y="-6" width="9" height="9" fill={palette.wall} stroke={palette.wallTrim} strokeWidth="0.8" />
          <polygon points="8,-6 14.5,-12 21,-6" fill={palette.roof} stroke={palette.roofTrim} strokeWidth="0.8" />

          {/* Central Town Hall / Guildhall with Bell Tower */}
          <rect x="-6" y="-12" width="12" height="13" fill={palette.wall} stroke={palette.wallTrim} strokeWidth="1" />
          <polygon points="-8,-12 0,-18 8,-12" fill={palette.roof} stroke={palette.roofTrim} strokeWidth="1" />
          {/* Bell Tower Peak */}
          <rect x="-2" y="-21" width="4" height="4" fill={palette.wall} stroke={palette.wallTrim} strokeWidth="0.8" />
          <polygon points="-3,-21 0,-25 3,-21" fill={palette.accent} />

          {/* Colorful Market Canopy Stalls */}
          <rect x="-6" y="2" width="5" height="4" fill="#ef4444" rx="1" />
          <rect x="1" y="2" width="5" height="4" fill="#3b82f6" rx="1" />
        </g>
      )}

      {/* ------------------------------------------------------------- */}
      {/* TIER 4: CITY (1,000+) — Multi-district urban citadel & keep */}
      {/* ------------------------------------------------------------- */}
      {tier === 'CITY' && (
        <g>
          {/* Outer City Housing Blocks (Background row) */}
          <rect x="-26" y="-8" width="10" height="10" fill={palette.wall} stroke={palette.wallTrim} strokeWidth="0.8" />
          <polygon points="-28,-8 -21,-14 -14,-8" fill={palette.roof} stroke={palette.roofTrim} strokeWidth="0.8" />

          <rect x="16" y="-8" width="10" height="10" fill={palette.wall} stroke={palette.wallTrim} strokeWidth="0.8" />
          <polygon points="14,-8 21,-14 28,-8" fill={palette.roof} stroke={palette.roofTrim} strokeWidth="0.8" />

          {/* Front Housing & Artisan Districts */}
          <rect x="-20" y="0" width="11" height="8" fill={palette.wall} stroke={palette.wallTrim} strokeWidth="0.8" />
          <polygon points="-22,0 -14.5,-5 -7,0" fill={palette.roof} stroke={palette.roofTrim} strokeWidth="0.8" />

          <rect x="9" y="0" width="11" height="8" fill={palette.wall} stroke={palette.wallTrim} strokeWidth="0.8" />
          <polygon points="7,0 14.5,-5 22,0" fill={palette.roof} stroke={palette.roofTrim} strokeWidth="0.8" />

          {/* Elevated Central Stone Keep / Citadel */}
          <rect x="-9" y="-18" width="18" height="20" fill="#475569" stroke="#64748b" strokeWidth="1.2" />
          {/* Battlements / Crenellations */}
          <path d="M -9 -18 L -9 -21 L -6 -21 L -6 -19 L -3 -19 L -3 -21 L 0 -21 L 0 -19 L 3 -19 L 3 -21 L 6 -21 L 6 -19 L 9 -19 L 9 -18 Z" fill="#64748b" />

          {/* High Spire / Keep Tower */}
          <rect x="-4" y="-27" width="8" height="8" fill="#334155" stroke="#64748b" strokeWidth="1" />
          <polygon points="-5,-27 0,-33 5,-27" fill={palette.accent} stroke="#e2e8f0" strokeWidth="0.8" />

          {/* Citadel Pennant / Banner fluttering */}
          <line x1="0" y1="-33" x2="0" y2="-38" stroke="#cbd5e1" strokeWidth="1" />
          <polygon points="0,-38 7,-36 0,-34" fill={palette.banner} />
        </g>
      )}

      {/* ------------------------------------------------------------- */}
      {/* TIER 5: MEGACITY (5,000+) — Concentric walls, imperial grand palace */}
      {/* ------------------------------------------------------------- */}
      {tier === 'MEGACITY' && (
        <g>
          {/* Outer Ring Wards & Mansions */}
          <rect x="-32" y="-10" width="12" height="11" fill={palette.wall} stroke={palette.wallTrim} strokeWidth="0.8" />
          <polygon points="-34,-10 -26,-17 -18,-10" fill={palette.roof} stroke={palette.roofTrim} strokeWidth="0.8" />

          <rect x="20" y="-10" width="12" height="11" fill={palette.wall} stroke={palette.wallTrim} strokeWidth="0.8" />
          <polygon points="18,-10 26,-17 34,-10" fill={palette.roof} stroke={palette.roofTrim} strokeWidth="0.8" />

          <rect x="-26" y="2" width="13" height="9" fill={palette.wall} stroke={palette.wallTrim} strokeWidth="0.8" />
          <polygon points="-28,2 -19.5,-4 -11,2" fill={palette.roof} stroke={palette.roofTrim} strokeWidth="0.8" />

          <rect x="13" y="2" width="13" height="9" fill={palette.wall} stroke={palette.wallTrim} strokeWidth="0.8" />
          <polygon points="11,2 19.5,-4 28,2" fill={palette.roof} stroke={palette.roofTrim} strokeWidth="0.8" />

          {/* Tiered Mid Bastions */}
          <rect x="-14" y="-15" width="28" height="18" fill="#334155" stroke="#64748b" strokeWidth="1.2" />
          <path d="M -14 -15 L -14 -18 L -10 -18 L -10 -16 L -5 -16 L -5 -18 L 0 -18 L 0 -16 L 5 -16 L 5 -18 L 10 -18 L 10 -16 L 14 -16 L 14 -15 Z" fill="#64748b" />

          {/* Monumental Grand Imperial Palace / Arcane Spire */}
          <rect x="-8" y="-28" width="16" height="14" fill="#1e293b" stroke="#94a3b8" strokeWidth="1.2" />
          <polygon points="-10,-28 0,-38 10,-28" fill={palette.accent} stroke="#f8fafc" strokeWidth="1" />

          {/* Twin Arcane/Watch Turrets */}
          <rect x="-12" y="-24" width="4" height="8" fill="#1e293b" />
          <polygon points="-13,-24 -10,-28 -7,-24" fill={palette.roof} />

          <rect x="8" y="-24" width="4" height="8" fill="#1e293b" />
          <polygon points="7,-24 10,-28 13,-24" fill={palette.roof} />

          {/* Imperial Banner Mast */}
          <line x1="0" y1="-38" x2="0" y2="-45" stroke="#f1f5f9" strokeWidth="1.2" />
          <polygon points="0,-45 10,-42 0,-39" fill={palette.banner} />
        </g>
      )}

      {/* Famine Warning Alert Badge */}
      {inFamine && (
        <g transform="translate(18, -20)">
          <circle r="7" fill="#ef4444" stroke="#ffffff" strokeWidth="1.2" filter="drop-shadow(0px 1px 3px rgba(0,0,0,0.5))" />
          <text x="0" y="3" fill="#ffffff" fontSize="9" fontWeight="bold" textAnchor="middle">
            !
          </text>
        </g>
      )}

      {/* Settlement Name & Dominant Clan Labelplate */}
      {showLabels && (
        <g
          transform={`translate(0, ${
            tier === 'MEGACITY' ? 24 : tier === 'CITY' ? 20 : tier === 'TOWN' ? 16 : 14
          })`}
        >
          {/* Backplate */}
          <rect
            x="-55"
            y="-1"
            width="110"
            height={dominantClanName ? 22 : 14}
            rx="4"
            fill="rgba(15, 23, 42, 0.9)"
            stroke={isSelected ? '#38bdf8' : 'rgba(100, 116, 139, 0.4)'}
            strokeWidth={isSelected ? 1.5 : 1}
          />
          {/* Name & Pop */}
          <text x="0" y="9" fill="#f8fafc" fontSize="10" fontWeight="bold" textAnchor="middle" letterSpacing="0.3">
            {name} <tspan fill="#94a3b8" fontSize="8 font-normal">({population})</tspan>
          </text>
          {/* Dominant Clan */}
          {dominantClanName && (
            <text x="0" y="18" fill="#fbbf24" fontSize="8" fontWeight="600" textAnchor="middle">
              {dominantClanName}
            </text>
          )}
        </g>
      )}
    </g>
  );
};

