import React, { useEffect, useRef } from 'react';
import { WorldMapSnapshot } from '../../types/api';
import { CameraState, WorldBounds, worldToScreen } from '../../utils/coordinates';
import { MapLayers } from '../../store/useMapStore';

interface WorldCanvasProps {
  mapSnapshot: WorldMapSnapshot;
  camera: CameraState;
  baseScale: number;
  bounds: WorldBounds;
  layers: MapLayers;
  isAwakened: boolean;
  width: number;
  height: number;
}

export const WorldCanvas: React.FC<WorldCanvasProps> = ({
  mapSnapshot,
  camera,
  baseScale,
  bounds,
  layers,
  isAwakened,
  width,
  height,
}) => {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    // Support High-DPI screens
    const dpr = window.devicePixelRatio || 1;
    canvas.width = width * dpr;
    canvas.height = height * dpr;
    ctx.scale(dpr, dpr);

    // Clear Canvas
    ctx.clearRect(0, 0, width, height);

    // 1. World Bounds Background & Outer Fill
    // Outside world: very dark letterbox
    ctx.fillStyle = isAwakened ? '#05070c' : '#12100d';
    ctx.fillRect(0, 0, width, height);

    // World Map Area
    const origin = worldToScreen(bounds.minX, bounds.minY, camera, baseScale, bounds);
    const corner = worldToScreen(bounds.maxX, bounds.maxY, camera, baseScale, bounds);
    const worldW = corner.sx - origin.sx;
    const worldH = corner.sy - origin.sy;

    // World Surface Fill
    ctx.save();
    ctx.beginPath();
    ctx.rect(origin.sx, origin.sy, worldW, worldH);
    ctx.clip();

    ctx.fillStyle = isAwakened ? '#0e1422' : '#221e18';
    ctx.fillRect(origin.sx, origin.sy, worldW, worldH);

    // 2. Subtle Biome Influence Visualization
    if (layers.biomeBackground && mapSnapshot.settlements.length > 0) {
      for (const s of mapSnapshot.settlements) {
        const { sx, sy } = worldToScreen(s.x, s.y, camera, baseScale, bounds);
        const radius = Math.max(30, 20 * baseScale * camera.zoom);

        const grad = ctx.createRadialGradient(sx, sy, 5, sx, sy, radius);
        let colorCenter = 'rgba(74, 222, 128, 0.15)'; // RIVER_VALLEY
        if (s.biome === 'HIGHLAND_MINE') colorCenter = 'rgba(217, 119, 6, 0.12)';
        else if (s.biome === 'FOREST') colorCenter = 'rgba(22, 101, 52, 0.18)';
        else if (s.biome === 'COASTAL') colorCenter = 'rgba(14, 165, 233, 0.15)';

        grad.addColorStop(0, colorCenter);
        grad.addColorStop(1, 'rgba(0, 0, 0, 0)');

        ctx.fillStyle = grad;
        ctx.beginPath();
        ctx.arc(sx, sy, radius, 0, Math.PI * 2);
        ctx.fill();
      }
    }

    // 3. Monster Lair Territory Radius Rings
    if (layers.territoryRadius && mapSnapshot.lairs.length > 0) {
      for (const lair of mapSnapshot.lairs) {
        if (lair.state === 'DESTROYED') continue;
        const { sx, sy } = worldToScreen(lair.x, lair.y, camera, baseScale, bounds);
        const pixelRadius = lair.territoryRadius * baseScale * camera.zoom;

        if (pixelRadius > 2) {
          // Radial red warning gradient
          const grad = ctx.createRadialGradient(sx, sy, pixelRadius * 0.4, sx, sy, pixelRadius);
          grad.addColorStop(0, 'rgba(239, 68, 68, 0.02)');
          grad.addColorStop(0.8, 'rgba(239, 68, 68, 0.08)');
          grad.addColorStop(1, 'rgba(239, 68, 68, 0.22)');

          ctx.fillStyle = grad;
          ctx.beginPath();
          ctx.arc(sx, sy, pixelRadius, 0, Math.PI * 2);
          ctx.fill();

          // Border ring
          ctx.strokeStyle = lair.threatRank === 'MYTHIC'
            ? 'rgba(239, 68, 68, 0.7)'
            : lair.threatRank === 'LEGENDARY'
            ? 'rgba(249, 115, 22, 0.6)'
            : 'rgba(239, 68, 68, 0.35)';
          ctx.lineWidth = 1;
          ctx.setLineDash([4, 4]);
          ctx.beginPath();
          ctx.arc(sx, sy, pixelRadius, 0, Math.PI * 2);
          ctx.stroke();
          ctx.setLineDash([]);
        }
      }
    }

    // 4. Coordinate Grid Lines & Labels
    if (layers.grid) {
      ctx.strokeStyle = isAwakened ? 'rgba(71, 85, 105, 0.25)' : 'rgba(120, 100, 80, 0.25)';
      ctx.lineWidth = 1;
      ctx.font = '10px monospace';
      ctx.fillStyle = isAwakened ? 'rgba(148, 163, 184, 0.5)' : 'rgba(168, 150, 130, 0.6)';

      const gridStep = 20; // Every 20 world units
      for (let wx = bounds.minX; wx <= bounds.maxX; wx += gridStep) {
        const top = worldToScreen(wx, bounds.minY, camera, baseScale, bounds);
        const btm = worldToScreen(wx, bounds.maxY, camera, baseScale, bounds);
        ctx.beginPath();
        ctx.moveTo(top.sx, top.sy);
        ctx.lineTo(btm.sx, btm.sy);
        ctx.stroke();

        ctx.fillText(`X:${wx}`, top.sx + 4, origin.sy + 14);
      }

      for (let wy = bounds.minY; wy <= bounds.maxY; wy += gridStep) {
        const left = worldToScreen(bounds.minX, wy, camera, baseScale, bounds);
        const right = worldToScreen(bounds.maxX, wy, camera, baseScale, bounds);
        ctx.beginPath();
        ctx.moveTo(left.sx, left.sy);
        ctx.lineTo(right.sx, right.sy);
        ctx.stroke();

        ctx.fillText(`Y:${wy}`, origin.sx + 4, left.sy - 4);
      }
    }

    // 5. Outer World Boundary Frame
    ctx.strokeStyle = isAwakened ? '#38bdf8' : '#d97706';
    ctx.lineWidth = 1.5;
    ctx.strokeRect(origin.sx, origin.sy, worldW, worldH);

    ctx.restore();
  }, [mapSnapshot, camera, baseScale, bounds, layers, isAwakened, width, height]);

  return (
    <canvas
      ref={canvasRef}
      style={{ width: `${width}px`, height: `${height}px` }}
      className="absolute top-0 left-0 pointer-events-none"
    />
  );
};

