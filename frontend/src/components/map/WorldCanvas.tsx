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

    // 1. World Bounds Background & Outer Fill
    // Outside world: dark letterbox frame
    ctx.fillStyle = isAwakened ? '#030712' : '#0c0a09';
    ctx.fillRect(0, 0, width, height);

    // World Map Area
    const origin = worldToScreen(bounds.minX, bounds.minY, camera, baseScale, bounds);
    const corner = worldToScreen(bounds.maxX, bounds.maxY, camera, baseScale, bounds);
    const worldW = corner.sx - origin.sx;
    const worldH = corner.sy - origin.sy;

    if (!Number.isFinite(worldW) || !Number.isFinite(worldH) || worldW <= 0 || worldH <= 0) return;

    // World Surface Fill & Clip
    ctx.save();
    ctx.beginPath();
    ctx.rect(origin.sx, origin.sy, worldW, worldH);
    ctx.clip();

    // -------------------------------------------------------------
    // Illustrated Fantasy Map Surface (Parchment vs Astral Obsidian)
    // -------------------------------------------------------------
    const bgGrad = ctx.createLinearGradient(origin.sx, origin.sy, corner.sx, corner.sy);
    if (isAwakened) {
      bgGrad.addColorStop(0, '#090d16');
      bgGrad.addColorStop(0.5, '#0d1322');
      bgGrad.addColorStop(1, '#060a12');
    } else {
      bgGrad.addColorStop(0, '#1c1917');
      bgGrad.addColorStop(0.5, '#292524');
      bgGrad.addColorStop(1, '#1f1c19');
    }
    ctx.fillStyle = bgGrad;
    ctx.fillRect(origin.sx, origin.sy, worldW, worldH);

    // Subtle natural topographic elevation lines (fantasy atlas contours)
    if (layers.biomeBackground) {
      ctx.save();
      ctx.strokeStyle = isAwakened ? 'rgba(56, 189, 248, 0.05)' : 'rgba(217, 119, 6, 0.07)';
      ctx.lineWidth = 1;

      // Deterministic elevation contours across world units
      const numContours = 6;
      for (let i = 1; i <= numContours; i++) {
        const contourY = bounds.minY + (bounds.height / (numContours + 1)) * i;
        ctx.beginPath();
        for (let wx = bounds.minX; wx <= bounds.maxX; wx += 10) {
          // Soft mountain wave deformation
          const wave = Math.sin(wx * 0.06 + i * 1.5) * 6 + Math.cos(wx * 0.03 + i) * 8;
          const pt = worldToScreen(wx, contourY + wave, camera, baseScale, bounds);
          if (wx === bounds.minX) {
            ctx.moveTo(pt.sx, pt.sy);
          } else {
            ctx.lineTo(pt.sx, pt.sy);
          }
        }
        ctx.stroke();
      }
      ctx.restore();
    }

    // -------------------------------------------------------------
    // Natural Biome Zones (Lush Valleys, Rocky Ridges, Canopies, Shores)
    // -------------------------------------------------------------
    if (layers.biomeBackground && mapSnapshot.settlements.length > 0) {
      for (const s of mapSnapshot.settlements) {
        const { sx, sy } = worldToScreen(s.x, s.y, camera, baseScale, bounds);
        const radius = Math.max(35, 28 * baseScale * camera.zoom);

        if (!Number.isFinite(sx) || !Number.isFinite(sy) || radius <= 0) continue;

        // Biome radial influence gradient
        const grad = safeRadialGradient(ctx, sx, sy, 5, sx, sy, radius);
        if (grad) {
          if (s.biome === 'RIVER_VALLEY') {
            grad.addColorStop(0, isAwakened ? 'rgba(34, 197, 94, 0.16)' : 'rgba(74, 222, 128, 0.20)');
            grad.addColorStop(0.6, isAwakened ? 'rgba(21, 128, 61, 0.08)' : 'rgba(34, 197, 94, 0.10)');
            grad.addColorStop(1, 'rgba(0, 0, 0, 0)');
          } else if (s.biome === 'HIGHLAND_MINE') {
            grad.addColorStop(0, isAwakened ? 'rgba(148, 163, 184, 0.18)' : 'rgba(217, 119, 6, 0.18)');
            grad.addColorStop(0.6, isAwakened ? 'rgba(100, 116, 139, 0.08)' : 'rgba(180, 83, 9, 0.08)');
            grad.addColorStop(1, 'rgba(0, 0, 0, 0)');
          } else if (s.biome === 'FOREST') {
            grad.addColorStop(0, isAwakened ? 'rgba(16, 185, 129, 0.18)' : 'rgba(22, 101, 52, 0.24)');
            grad.addColorStop(0.7, isAwakened ? 'rgba(5, 150, 105, 0.08)' : 'rgba(20, 83, 45, 0.10)');
            grad.addColorStop(1, 'rgba(0, 0, 0, 0)');
          } else if (s.biome === 'COASTAL') {
            grad.addColorStop(0, isAwakened ? 'rgba(14, 165, 233, 0.20)' : 'rgba(2, 132, 199, 0.22)');
            grad.addColorStop(0.7, isAwakened ? 'rgba(2, 132, 199, 0.08)' : 'rgba(14, 165, 233, 0.08)');
            grad.addColorStop(1, 'rgba(0, 0, 0, 0)');
          }

          ctx.fillStyle = grad;
          ctx.beginPath();
          ctx.arc(sx, sy, radius, 0, Math.PI * 2);
          ctx.fill();
        }

        // Feature-specific illustrations
        if (s.biome === 'RIVER_VALLEY' && camera.zoom >= 0.8) {
          // Meandering river ribbon
          ctx.save();
          ctx.strokeStyle = isAwakened ? 'rgba(56, 189, 248, 0.45)' : 'rgba(56, 189, 248, 0.55)';
          ctx.lineWidth = Math.max(2, 2.5 * camera.zoom);
          ctx.lineCap = 'round';
          ctx.beginPath();
          ctx.moveTo(sx - radius * 0.9, sy + radius * 0.3);
          ctx.bezierCurveTo(
            sx - radius * 0.4,
            sy - radius * 0.2,
            sx + radius * 0.2,
            sy + radius * 0.5,
            sx + radius * 0.9,
            sy - radius * 0.4
          );
          ctx.stroke();

          // Delicate sparkling water crest highlight
          ctx.strokeStyle = 'rgba(255, 255, 255, 0.4)';
          ctx.lineWidth = 1;
          ctx.stroke();
          ctx.restore();
        } else if (s.biome === 'HIGHLAND_MINE' && camera.zoom >= 0.8) {
          // Mountain ridge hachures
          ctx.save();
          ctx.strokeStyle = isAwakened ? 'rgba(148, 163, 184, 0.35)' : 'rgba(168, 162, 158, 0.4)';
          ctx.lineWidth = 1.2;
          for (let m = -2; m <= 2; m++) {
            const mx = sx + m * 14 * camera.zoom;
            const my = sy - radius * 0.4;
            ctx.beginPath();
            ctx.moveTo(mx - 8, my + 6);
            ctx.lineTo(mx, my - 6);
            ctx.lineTo(mx + 8, my + 6);
            ctx.stroke();
          }
          ctx.restore();
        } else if (s.biome === 'FOREST' && camera.zoom >= 0.8) {
          // Woodland tree canopy clusters
          ctx.save();
          ctx.fillStyle = isAwakened ? 'rgba(5, 150, 105, 0.35)' : 'rgba(21, 128, 61, 0.4)';
          const treeOffsets = [
            [-18, -12],
            [14, -14],
            [-16, 12],
            [18, 14],
          ];
          for (const [tx, ty] of treeOffsets) {
            ctx.beginPath();
            ctx.arc(sx + tx, sy + ty, 4 * Math.min(1.5, camera.zoom), 0, Math.PI * 2);
            ctx.fill();
          }
          ctx.restore();
        } else if (s.biome === 'COASTAL' && camera.zoom >= 0.8) {
          // Shoreline surf ripples
          ctx.save();
          ctx.strokeStyle = 'rgba(255, 255, 255, 0.3)';
          ctx.lineWidth = 1;
          ctx.setLineDash([4, 4]);
          ctx.beginPath();
          ctx.arc(sx, sy, radius * 0.75, 0, Math.PI * 2);
          ctx.stroke();
          ctx.setLineDash([]);
          ctx.restore();
        }
      }
    }

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
      for (let wx = bounds.minX; wx <= bounds.maxX; wx += gridStep) {
        const top = worldToScreen(wx, bounds.minY, camera, baseScale, bounds);
        const btm = worldToScreen(wx, bounds.maxY, camera, baseScale, bounds);
        if (Number.isFinite(top.sx) && Number.isFinite(top.sy) && Number.isFinite(btm.sx) && Number.isFinite(btm.sy)) {
          ctx.beginPath();
          ctx.moveTo(top.sx, top.sy);
          ctx.lineTo(btm.sx, btm.sy);
          ctx.stroke();
          ctx.fillText(`X:${wx}`, top.sx + 4, origin.sy + 14);
        }
      }

      for (let wy = bounds.minY; wy <= bounds.maxY; wy += gridStep) {
        const left = worldToScreen(bounds.minX, wy, camera, baseScale, bounds);
        const right = worldToScreen(bounds.maxX, wy, camera, baseScale, bounds);
        if (Number.isFinite(left.sx) && Number.isFinite(left.sy) && Number.isFinite(right.sx) && Number.isFinite(right.sy)) {
          ctx.beginPath();
          ctx.moveTo(left.sx, left.sy);
          ctx.lineTo(right.sx, right.sy);
          ctx.stroke();
          ctx.fillText(`Y:${wy}`, origin.sx + 4, left.sy - 4);
        }
      }
    }

    // -------------------------------------------------------------
    // Vintage Cartographic Compass Rose
    // -------------------------------------------------------------
    const compassX = corner.sx - 60;
    const compassY = origin.sy + 60;
    if (compassX > origin.sx + 80 && compassY < corner.sy - 80) {
      ctx.save();
      ctx.translate(compassX, compassY);
      const compassRadius = 30;

      // Outer compass ring
      ctx.strokeStyle = isAwakened ? 'rgba(56, 189, 248, 0.3)' : 'rgba(217, 119, 6, 0.35)';
      ctx.lineWidth = 1;
      ctx.beginPath();
      ctx.arc(0, 0, compassRadius, 0, Math.PI * 2);
      ctx.stroke();

      ctx.beginPath();
      ctx.arc(0, 0, compassRadius - 4, 0, Math.PI * 2);
      ctx.stroke();

      // 4 Main Cardinal Points (North, South, East, West)
      const points = [
        [0, -compassRadius + 2], // N
        [compassRadius - 2, 0],  // E
        [0, compassRadius - 2],  // S
        [-compassRadius + 2, 0], // W
      ];

      for (let p = 0; p < points.length; p++) {
        const [px, py] = points[p];
        ctx.beginPath();
        ctx.moveTo(0, 0);
        ctx.lineTo(px, py);
        ctx.strokeStyle = isAwakened ? 'rgba(56, 189, 248, 0.45)' : 'rgba(217, 119, 6, 0.5)';
        ctx.stroke();
      }

      // North Needle Arrow
      ctx.fillStyle = isAwakened ? '#38bdf8' : '#d97706';
      ctx.beginPath();
      ctx.moveTo(0, -compassRadius + 2);
      ctx.lineTo(4, 0);
      ctx.lineTo(0, -4);
      ctx.fill();

      ctx.fillStyle = isAwakened ? '#0284c7' : '#92400e';
      ctx.beginPath();
      ctx.moveTo(0, -compassRadius + 2);
      ctx.lineTo(-4, 0);
      ctx.lineTo(0, -4);
      ctx.fill();

      // 'N' Label
      ctx.font = 'bold 9px serif';
      ctx.fillStyle = isAwakened ? '#38bdf8' : '#d97706';
      ctx.textAlign = 'center';
      ctx.fillText('N', 0, -compassRadius - 5);

      ctx.restore();
    }

    // -------------------------------------------------------------
    // Cartographic Atlas Frame & Corner Fleurons
    // -------------------------------------------------------------
    ctx.strokeStyle = isAwakened ? 'rgba(56, 189, 248, 0.6)' : 'rgba(217, 119, 6, 0.6)';
    ctx.lineWidth = 2;
    ctx.strokeRect(origin.sx, origin.sy, worldW, worldH);

    // Inner thin border
    ctx.strokeStyle = isAwakened ? 'rgba(56, 189, 248, 0.25)' : 'rgba(217, 119, 6, 0.25)';
    ctx.lineWidth = 1;
    ctx.strokeRect(origin.sx + 4, origin.sy + 4, worldW - 8, worldH - 8);

    // Corner brackets
    const bracketSize = 12;
    const corners = [
      [origin.sx + 4, origin.sy + 4, 1, 1],
      [corner.sx - 4, origin.sy + 4, -1, 1],
      [origin.sx + 4, corner.sy - 4, 1, -1],
      [corner.sx - 4, corner.sy - 4, -1, -1],
    ];
    ctx.strokeStyle = isAwakened ? '#38bdf8' : '#d97706';
    ctx.lineWidth = 1.5;
    for (const [cx, cy, dx, dy] of corners) {
      ctx.beginPath();
      ctx.moveTo(cx, cy + dy * bracketSize);
      ctx.lineTo(cx, cy);
      ctx.lineTo(cx + dx * bracketSize, cy);
      ctx.stroke();
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
