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

const ICON_BY_TIER: Record<string, string> = {
  HAMLET: 'hamlet',
  VILLAGE: 'village',
  TOWN: 'town',
  CITY: 'city',
  MEGACITY: 'megacity',
};

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
  const icon = ICON_BY_TIER[settlement.tier] ?? 'hamlet';
  const iconSize = zoom < 0.75 ? 28 : zoom > 1.7 ? 62 : 44;
  const accent = isAwakened ? '#38bdf8' : '#fbbf24';

  return (
    <g className="cursor-pointer select-none" style={{ transformBox: 'fill-box', transformOrigin: 'center' }}>
      {isSelected && (
        <circle
          r={iconSize * 0.62}
          fill="none"
          stroke={accent}
          strokeWidth="2.5"
          strokeDasharray="6 3"
          className={enableAnimations && !reducedMotion ? 'animate-spin' : ''}
          style={{ transformBox: 'fill-box', transformOrigin: 'center', animationDuration: '10s' }}
        />
      )}
      {settlement.fortificationLevel > 0 && (
        <circle
          r={iconSize * 0.52}
          fill="none"
          stroke="#78350f"
          strokeWidth="1.5"
          strokeDasharray="2 3"
          opacity="0.8"
        />
      )}
      <image
        href={`/map-icons/${icon}.svg`}
        x={-iconSize / 2}
        y={-iconSize}
        width={iconSize}
        height={iconSize}
        preserveAspectRatio="xMidYMax meet"
      />
      {settlement.inFamine && (
        <circle cx={iconSize * 0.34} cy={-iconSize * 0.82} r="4" fill="#ef4444" stroke="#fee2e2" strokeWidth="1" />
      )}
      {(showLabels || isHovered || isSelected) && (
        <g transform={`translate(0 ${iconSize * 0.25})`}>
          <rect
            x={-Math.max(38, settlement.name.length * 3.3)}
            y="0"
            width={Math.max(76, settlement.name.length * 6.6)}
            height="16"
            rx="4"
            fill="rgba(10, 15, 26, 0.92)"
            stroke={isSelected ? accent : '#475569'}
            strokeWidth="1"
          />
          <text x="0" y="11" fill="#fff" fontSize="9" fontWeight="bold" textAnchor="middle">
            {settlement.name} ({settlement.population.toLocaleString()})
          </text>
        </g>
      )}
    </g>
  );
};
