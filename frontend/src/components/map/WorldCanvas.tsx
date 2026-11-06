import React, { useEffect, useRef } from 'react';
import { WorldMapSnapshot, WorldOverlaysDto } from '../../types/api';
import { CameraState, WorldBounds, worldToScreen } from '../../utils/coordinates';
import { MapLayers, useMapStore } from '../../store/useMapStore';

interface WorldCanvasProps {
  mapSnapshot: WorldMapSnapshot;
  overlays?: WorldOverlaysDto | null;
  camera: CameraState;
  baseScale: number;
  bounds: WorldBounds;
  layers: MapLayers;
  isAwakened: boolean;
  width: number;
  height: number;
}

function safeRadialGradient(
  ctx: CanvasRenderingContext2D,
  x0: number,
  y0: number,
  r0: number,
  x1: number,
  y1: number,
  r1: number
): CanvasGradient | null {
  if (
    !Number.isFinite(x0) ||
    !Number.isFinite(y0) ||
    !Number.isFinite(r0) ||
    !Number.isFinite(x1) ||
    !Number.isFinite(y1) ||
    !Number.isFinite(r1) ||
    r0 < 0 ||
    r1 < 0 ||
    (r0 === 0 && r1 === 0)
  ) {
    return null;
  }
  try {
    return ctx.createRadialGradient(x0, y0, r0, x1, y1, r1);
  } catch {
    return null;
  }
}

export const WorldCanvas: React.FC<WorldCanvasProps> = ({
  mapSnapshot,
  overlays,
  camera,
  baseScale,
  bounds,
  layers,
  isAwakened,
  width,
  height,
}) => {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);

  const dynamicOverlays = useMapStore((s) => s.dynamicOverlays);
  const heatmapOpacity = useMapStore((s) => s.heatmapOpacity);
  const selectedEntity = useMapStore((s) => s.selectedEntity);
  const hoveredEntity = useMapStore((s) => s.hoveredEntity);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    if (width <= 0 || height <= 0 || !Number.isFinite(width) || !Number.isFinite(height)) return;

    // Support High-DPI screens
    const dpr = window.devicePixelRatio || 1;
    canvas.width = width * dpr;
    canvas.height = height * dpr;
    ctx.scale(dpr, dpr);

    // Clear Canvas
    ctx.clearRect(0, 0, width, height);

    // Dynamic overlays are transparent above the static SVG atlas.
    const origin = { sx: camera.panX, sy: camera.panY };
    const worldW = bounds.width * baseScale * camera.zoom;
    const worldH = bounds.height * baseScale * camera.zoom;
    ctx.save();
    ctx.beginPath();
    ctx.rect(0, 0, width, height);
    ctx.clip();

    // -------------------------------------------------------------
    // Dynamic Heatmap Layers
    // -------------------------------------------------------------
    if (overlays?.heatmaps) {
      const hm = overlays.heatmaps;

      // Population Density Heatmap
      if (dynamicOverlays.populationDensity && hm.populationDensity?.length > 0) {
        for (const pt of hm.populationDensity) {
          const { sx, sy } = worldToScreen(pt.x, pt.y, camera, baseScale, bounds);
          const r = Math.max(12, pt.radius * baseScale * camera.zoom);
          const alpha = Math.min(1.0, pt.intensity * heatmapOpacity * 0.7);

          const grad = safeRadialGradient(ctx, sx, sy, 0, sx, sy, r);
          if (grad) {
            grad.addColorStop(0, `rgba(34, 197, 94, ${alpha})`);
            grad.addColorStop(0.5, `rgba(74, 222, 128, ${alpha * 0.45})`);
            grad.addColorStop(1, 'rgba(34, 197, 94, 0)');

            ctx.fillStyle = grad;
            ctx.beginPath();
            ctx.arc(sx, sy, r, 0, Math.PI * 2);
            ctx.fill();
          }
        }
      }

      // Economic Prosperity Heatmap
      if (dynamicOverlays.prosperity && hm.economicProsperity?.length > 0) {
        for (const pt of hm.economicProsperity) {
          const { sx, sy } = worldToScreen(pt.x, pt.y, camera, baseScale, bounds);
          const r = Math.max(10, pt.radius * baseScale * camera.zoom);
          const alpha = Math.min(1.0, pt.intensity * heatmapOpacity * 0.75);

          const grad = safeRadialGradient(ctx, sx, sy, 0, sx, sy, r);
          if (grad) {
            grad.addColorStop(0, `rgba(234, 179, 8, ${alpha})`);
            grad.addColorStop(0.6, `rgba(245, 158, 11, ${alpha * 0.4})`);
            grad.addColorStop(1, 'rgba(234, 179, 8, 0)');

            ctx.fillStyle = grad;
            ctx.beginPath();
            ctx.arc(sx, sy, r, 0, Math.PI * 2);
            ctx.fill();
          }
        }
      }

      // Aetheric Activity Leyline Heatmap
      if (dynamicOverlays.aethericActivity && hm.aethericActivity?.length > 0) {
        for (const pt of hm.aethericActivity) {
          const { sx, sy } = worldToScreen(pt.x, pt.y, camera, baseScale, bounds);
          const r = Math.max(15, pt.radius * baseScale * camera.zoom);
          const alpha = Math.min(1.0, pt.intensity * heatmapOpacity * 0.8);

          const grad = safeRadialGradient(ctx, sx, sy, 0, sx, sy, r);
          if (grad) {
            grad.addColorStop(0, `rgba(168, 85, 247, ${alpha})`);
            grad.addColorStop(0.5, `rgba(56, 189, 248, ${alpha * 0.35})`);
            grad.addColorStop(1, 'rgba(168, 85, 247, 0)');

            ctx.fillStyle = grad;
            ctx.beginPath();
            ctx.arc(sx, sy, r, 0, Math.PI * 2);
            ctx.fill();
          }
        }
      }

      // Monster Threat Heatmap (On-demand layer)
      if (dynamicOverlays.monsterThreat && hm.monsterThreat?.length > 0) {
        for (const pt of hm.monsterThreat) {
          const { sx, sy } = worldToScreen(pt.x, pt.y, camera, baseScale, bounds);
          const r = Math.max(15, pt.radius * baseScale * camera.zoom);
          const alpha = Math.min(1.0, pt.intensity * heatmapOpacity * 0.85);

          const grad = safeRadialGradient(ctx, sx, sy, 0, sx, sy, r);
          if (grad) {
            grad.addColorStop(0, `rgba(239, 68, 68, ${alpha})`);
            grad.addColorStop(0.5, `rgba(249, 115, 22, ${alpha * 0.4})`);
            grad.addColorStop(1, 'rgba(239, 68, 68, 0)');

            ctx.fillStyle = grad;
            ctx.beginPath();
            ctx.arc(sx, sy, r, 0, Math.PI * 2);
            ctx.fill();
          }
        }
      }
    }

    // -------------------------------------------------------------
    // Settlement Territories & Borders
    // -------------------------------------------------------------
    if (dynamicOverlays.territory && overlays?.territories) {
      for (const terr of overlays.territories) {
        const terrX = Number.isFinite(terr.x) ? (terr.x as number) : ((terr as any).centerX ?? 0);
        const terrY = Number.isFinite(terr.y) ? (terr.y as number) : ((terr as any).centerY ?? 0);
        const { sx, sy } = worldToScreen(terrX, terrY, camera, baseScale, bounds);
        const pixelRadius = Math.max(10, (terr.territoryRadius || 15) * baseScale * camera.zoom);

        if (!Number.isFinite(sx) || !Number.isFinite(sy) || pixelRadius <= 0) continue;

        // Soft zone fill
        const grad = safeRadialGradient(ctx, sx, sy, pixelRadius * 0.2, sx, sy, pixelRadius);
        if (grad) {
          grad.addColorStop(0, 'rgba(56, 189, 248, 0.06)');
          grad.addColorStop(0.8, 'rgba(56, 189, 248, 0.03)');
          grad.addColorStop(1, 'rgba(56, 189, 248, 0.12)');

          ctx.fillStyle = grad;
          ctx.beginPath();
          ctx.arc(sx, sy, pixelRadius, 0, Math.PI * 2);
          ctx.fill();
        }

        // Territory outline
        ctx.strokeStyle = isAwakened ? 'rgba(56, 189, 248, 0.35)' : 'rgba(217, 119, 6, 0.35)';
        ctx.lineWidth = 1;
        ctx.setLineDash([6, 3]);
        ctx.beginPath();
        ctx.arc(sx, sy, pixelRadius, 0, Math.PI * 2);
        ctx.stroke();
        ctx.setLineDash([]);
      }
    }

    // -------------------------------------------------------------
    // Diplomatic Links & Commercial Trade Routes
    // -------------------------------------------------------------
    if ((dynamicOverlays.diplomacy || dynamicOverlays.tradeRoutes) && overlays?.diplomaticLinks) {
      for (const link of overlays.diplomaticLinks) {
        const p1 = worldToScreen(link.originX, link.originY, camera, baseScale, bounds);
        const p2 = worldToScreen(link.targetX, link.targetY, camera, baseScale, bounds);

        if (!Number.isFinite(p1.sx) || !Number.isFinite(p1.sy) || !Number.isFinite(p2.sx) || !Number.isFinite(p2.sy)) continue;

        if (link.relationType === 'ALLIED' && dynamicOverlays.tradeRoutes) {
          ctx.strokeStyle = 'rgba(245, 158, 11, 0.6)';
          ctx.lineWidth = Math.min(3, Math.max(1.2, link.tradeVolume / 15.0));
          ctx.setLineDash([5, 5]);
          ctx.beginPath();
          ctx.moveTo(p1.sx, p1.sy);
          ctx.lineTo(p2.sx, p2.sy);
          ctx.stroke();
          ctx.setLineDash([]);
        } else if (link.relationType === 'AT_WAR' && dynamicOverlays.diplomacy) {
          ctx.strokeStyle = 'rgba(239, 68, 68, 0.7)';
          ctx.lineWidth = 2;
          ctx.setLineDash([4, 2]);
          ctx.beginPath();
          ctx.moveTo(p1.sx, p1.sy);
          ctx.lineTo(p2.sx, p2.sy);
          ctx.stroke();
          ctx.setLineDash([]);
        }
      }
    }

    // -------------------------------------------------------------
    // Restrained Monster Lair Territory Visualization
    // -------------------------------------------------------------
    if (layers.territoryRadius && mapSnapshot.lairs.length > 0) {
      for (const lair of mapSnapshot.lairs) {
        if (lair.state === 'DESTROYED') continue;
        const { sx, sy } = worldToScreen(lair.x, lair.y, camera, baseScale, bounds);
        const pixelRadius = lair.territoryRadius * baseScale * camera.zoom;

        if (!Number.isFinite(sx) || !Number.isFinite(sy) || pixelRadius <= 2) continue;

        const isLairSelected = selectedEntity?.type === 'LAIR' && selectedEntity.id === lair.lairId;
        const isLairHovered = hoveredEntity?.type === 'LAIR' && hoveredEntity.id === lair.lairId;
        const shouldShowFullHalo = isLairSelected || isLairHovered || dynamicOverlays.monsterThreat;

        if (shouldShowFullHalo) {
          const grad = safeRadialGradient(ctx, sx, sy, pixelRadius * 0.4, sx, sy, pixelRadius);
          if (grad) {
            grad.addColorStop(0, 'rgba(239, 68, 68, 0.02)');
            grad.addColorStop(0.8, 'rgba(239, 68, 68, 0.08)');
            grad.addColorStop(1, 'rgba(239, 68, 68, 0.25)');

            ctx.fillStyle = grad;
            ctx.beginPath();
            ctx.arc(sx, sy, pixelRadius, 0, Math.PI * 2);
            ctx.fill();
          }
        }

        // Low-opacity, non-intrusive perimeter ring
        ctx.strokeStyle = shouldShowFullHalo
          ? (lair.threatRank === 'MYTHIC' ? 'rgba(239, 68, 68, 0.8)' : 'rgba(249, 115, 22, 0.7)')
          : 'rgba(239, 68, 68, 0.2)';
        ctx.lineWidth = shouldShowFullHalo ? 1.5 : 1;
        ctx.setLineDash([4, 4]);
        ctx.beginPath();
        ctx.arc(sx, sy, pixelRadius, 0, Math.PI * 2);
        ctx.stroke();
        ctx.setLineDash([]);
      }
    }

    // -------------------------------------------------------------
    // Coordinate Grid (Off by default, cleanly rendered when toggled)
    // -------------------------------------------------------------
    if (layers.grid) {
      ctx.strokeStyle = isAwakened ? 'rgba(71, 85, 105, 0.2)' : 'rgba(120, 100, 80, 0.2)';
      ctx.lineWidth = 1;
      ctx.font = '10px monospace';
      ctx.fillStyle = isAwakened ? 'rgba(148, 163, 184, 0.5)' : 'rgba(168, 150, 130, 0.6)';

      const gridStep = 20;
      for (let mapX = bounds.minX; mapX <= bounds.maxX; mapX += gridStep) {
        const top = worldToScreen(mapX, bounds.minY, camera, baseScale, bounds);
        const btm = worldToScreen(mapX, bounds.maxY, camera, baseScale, bounds);
        if (Number.isFinite(top.sx) && Number.isFinite(top.sy) && Number.isFinite(btm.sx) && Number.isFinite(btm.sy)) {
          ctx.beginPath();
          ctx.moveTo(top.sx, top.sy);
          ctx.lineTo(btm.sx, btm.sy);
          ctx.stroke();
          ctx.fillText(`X:${mapX}`, top.sx + 4, origin.sy + 14);
        }
      }

      for (let mapY = bounds.minY; mapY <= bounds.maxY; mapY += gridStep) {
        const left = worldToScreen(bounds.minX, mapY, camera, baseScale, bounds);
        const right = worldToScreen(bounds.maxX, mapY, camera, baseScale, bounds);
        if (Number.isFinite(left.sx) && Number.isFinite(left.sy) && Number.isFinite(right.sx) && Number.isFinite(right.sy)) {
          ctx.beginPath();
          ctx.moveTo(left.sx, left.sy);
          ctx.lineTo(right.sx, right.sy);
          ctx.stroke();
          ctx.fillText(`Y:${mapY}`, origin.sx + 4, left.sy - 4);
        }
      }
    }

    ctx.restore();
  }, [
    mapSnapshot,
    overlays,
    camera,
    baseScale,
    bounds,
    layers,
    dynamicOverlays,
    heatmapOpacity,
    isAwakened,
    width,
    height,
    selectedEntity,
    hoveredEntity,
  ]);

  return (
    <canvas
      ref={canvasRef}
      style={{ width: `${width}px`, height: `${height}px` }}
      className="absolute top-0 left-0 pointer-events-none"
    />
  );
};
