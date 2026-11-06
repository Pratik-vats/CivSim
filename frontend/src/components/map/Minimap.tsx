import React, { useRef } from 'react';
import { WorldMapSnapshot } from '../../types/api';
import { CameraState, WorldBounds, screenToWorld, projectWorldToMap, unprojectMapToWorld } from '../../utils/coordinates';

interface MinimapProps {
  mapSnapshot: WorldMapSnapshot;
  camera: CameraState;
  baseScale: number;
  bounds: WorldBounds;
  viewportWidth: number;
  viewportHeight: number;
  onNavigate: (targetWx: number, targetWy: number) => void;
  isAwakened: boolean;
}

export const Minimap: React.FC<MinimapProps> = ({
  mapSnapshot,
  camera,
  baseScale,
  bounds,
  viewportWidth,
  viewportHeight,
  onNavigate,
  isAwakened,
}) => {
  const minimapSize = 130;
  const padding = 6;
  const contentSize = minimapSize - padding * 2;
  const miniScale = contentSize / Math.max(bounds.width, bounds.height);

  const containerRef = useRef<HTMLDivElement | null>(null);

  // Calculate viewport rectangle in world coordinates
  const topLeftWorld = screenToWorld(0, 0, camera, baseScale, bounds);
  const btmRightWorld = screenToWorld(viewportWidth, viewportHeight, camera, baseScale, bounds);

  // Clamp viewport rect to minimap boundaries
  const tl = projectWorldToMap(topLeftWorld.wx, topLeftWorld.wy);
  const br = projectWorldToMap(btmRightWorld.wx, btmRightWorld.wy);
  const vx = padding + (Math.max(bounds.minX, Math.min(tl.x, br.x)) - bounds.minX) * miniScale;
  const vy = padding + (Math.max(bounds.minY, Math.min(tl.y, br.y)) - bounds.minY) * miniScale;
  const vw = Math.min(contentSize, Math.abs(br.x - tl.x) * miniScale);
  const vh = Math.min(contentSize, Math.abs(br.y - tl.y) * miniScale);

  const handleClick = (e: React.MouseEvent<SVGSVGElement>) => {
    e.stopPropagation();
    const rect = e.currentTarget.getBoundingClientRect();
    const clickX = e.clientX - rect.left - padding;
    const clickY = e.clientY - rect.top - padding;

    const target = unprojectMapToWorld(bounds.minX + clickX / miniScale, bounds.minY + clickY / miniScale);

    onNavigate(
      target.wx,
      target.wy
    );
  };

  return (
    <div
      ref={containerRef}
      className="absolute top-16 right-4 z-10 bg-slate-900/90 backdrop-blur border border-slate-700/80 rounded-lg p-1.5 shadow-2xl overflow-hidden cursor-crosshair select-none"
    >
      <div className="text-[10px] text-slate-400 font-mono mb-1 flex items-center justify-between">
        <span>WORLD MINIMAP</span>
        <span className="text-[9px] text-cyan-400">1000x1000</span>
      </div>

      <svg
        width={minimapSize}
        height={minimapSize}
        className="bg-slate-950/80 rounded border border-slate-800"
        onClick={handleClick}
      >
        {/* World Grid Lines */}
        <line
          x1={padding + contentSize / 2}
          y1={padding}
          x2={padding + contentSize / 2}
          y2={padding + contentSize}
          stroke="rgba(71, 85, 105, 0.2)"
        />
        <line
          x1={padding}
          y1={padding + contentSize / 2}
          x2={padding + contentSize}
          y2={padding + contentSize / 2}
          stroke="rgba(71, 85, 105, 0.2)"
        />

        {/* Lairs */}
        {mapSnapshot.lairs.map((l) => {
          const point = projectWorldToMap(l.x, l.y);
          const mx = padding + (point.x - bounds.minX) * miniScale;
          const my = padding + (point.y - bounds.minY) * miniScale;
          return <circle key={l.lairId} cx={mx} cy={my} r="2" fill="#ef4444" />;
        })}

        {/* Anomalies */}
        {mapSnapshot.anomalies.map((a) => {
          const point = projectWorldToMap(a.x, a.y);
          const mx = padding + (point.x - bounds.minX) * miniScale;
          const my = padding + (point.y - bounds.minY) * miniScale;
          return <rect key={a.anomalyId} x={mx - 1.5} y={my - 1.5} width="3" height="3" fill="#06b6d4" />;
        })}

        {/* Settlements */}
        {mapSnapshot.settlements.map((s) => {
          const point = projectWorldToMap(s.x, s.y);
          const mx = padding + (point.x - bounds.minX) * miniScale;
          const my = padding + (point.y - bounds.minY) * miniScale;
          return (
            <circle
              key={s.id}
              cx={mx}
              cy={my}
              r={s.tier === 'CITY' || s.tier === 'MEGACITY' ? '3.5' : '2.5'}
              fill={isAwakened ? '#38bdf8' : '#eab308'}
            />
          );
        })}

        {/* Camera Viewport Indicator */}
        <rect
          x={vx}
          y={vy}
          width={Math.max(4, vw)}
          height={Math.max(4, vh)}
          fill="rgba(56, 189, 248, 0.15)"
          stroke="#38bdf8"
          strokeWidth="1.5"
          pointerEvents="none"
        />
      </svg>
    </div>
  );
};

