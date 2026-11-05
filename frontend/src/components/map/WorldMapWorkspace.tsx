import React, { useRef, useState, useEffect, useCallback } from 'react';
import { WorldMapSnapshot, WorldOverlaysDto } from '../../types/api';
import {
  WorldBounds,
  DEFAULT_WORLD_BOUNDS,
  calculateBaseScale,
  getFitCamera,
  zoomAroundPoint,
  screenToWorld,
  worldToScreen,
} from '../../utils/coordinates';
import { useMapStore } from '../../store/useMapStore';
import { WorldCanvas } from './WorldCanvas';
import { WorldSvgOverlay } from './WorldSvgOverlay';
import { Minimap } from './Minimap';
import { MapControlDock } from './MapControlDock';
import { ContextualEntityCard } from './ContextualEntityCard';
import { Crosshair } from 'lucide-react';

interface WorldMapWorkspaceProps {
  mapSnapshot: WorldMapSnapshot;
  overlays?: WorldOverlaysDto | null;
  isAwakened: boolean;
}

export const WorldMapWorkspace: React.FC<WorldMapWorkspaceProps> = ({ mapSnapshot, overlays, isAwakened }) => {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const [dimensions, setDimensions] = useState<{ width: number; height: number }>({
    width: 800,
    height: 600,
  });

  const camera = useMapStore((s) => s.camera);
  const setCamera = useMapStore((s) => s.setCamera);
  const selectedEntity = useMapStore((s) => s.selectedEntity);
  const selectEntity = useMapStore((s) => s.selectEntity);
  const hoveredEntity = useMapStore((s) => s.hoveredEntity);
  const setHoveredEntity = useMapStore((s) => s.setHoveredEntity);
  const cursorWorldPos = useMapStore((s) => s.cursorWorldPos);
  const setCursorWorldPos = useMapStore((s) => s.setCursorWorldPos);
  const layers = useMapStore((s) => s.layers);
  const filters = useMapStore((s) => s.filters);
  const showMinimap = useMapStore((s) => s.showMinimap);
  const drawerOpen = useMapStore((s) => s.drawerOpen);
  const setDrawerOpen = useMapStore((s) => s.setDrawerOpen);

  // ResizeObserver to track container dimensions
  useEffect(() => {
    const el = containerRef.current;
    if (!el) return;

    const ro = new ResizeObserver((entries) => {
      for (const entry of entries) {
        const { width, height } = entry.contentRect;
        if (width > 0 && height > 0) {
          setDimensions({ width, height });
        }
      }
    });

    ro.observe(el);
    return () => ro.disconnect();
  }, []);

  const bounds: WorldBounds = {
    minX: 0,
    maxX: mapSnapshot.width || DEFAULT_WORLD_BOUNDS.maxX,
    minY: 0,
    maxY: mapSnapshot.height || DEFAULT_WORLD_BOUNDS.maxY,
    width: mapSnapshot.width || DEFAULT_WORLD_BOUNDS.width,
    height: mapSnapshot.height || DEFAULT_WORLD_BOUNDS.height,
  };

  const baseScale = calculateBaseScale(dimensions.width, dimensions.height, bounds);

  // Center & Fit World on initial load
  const initializedRef = useRef(false);
  useEffect(() => {
    if (!initializedRef.current && dimensions.width > 100 && dimensions.height > 100) {
      setCamera(getFitCamera(dimensions.width, dimensions.height, bounds));
      initializedRef.current = true;
    }
  }, [dimensions, bounds, setCamera]);

  // Center selected entity if camera centering requested
  const centerEntity = useCallback(
    (wx: number, wy: number) => {
      const targetScreen = worldToScreen(wx, wy, { panX: 0, panY: 0, zoom: camera.zoom }, baseScale, bounds);
      const newPanX = dimensions.width / 2 - targetScreen.sx;
      const newPanY = dimensions.height / 2 - targetScreen.sy;
      setCamera((prev) => ({ ...prev, panX: newPanX, panY: newPanY }));
    },
    [dimensions, camera.zoom, baseScale, bounds, setCamera]
  );

  // Dragging state
  const isDraggingRef = useRef(false);
  const dragStartRef = useRef<{ x: number; y: number; panX: number; panY: number }>({
    x: 0,
    y: 0,
    panX: 0,
    panY: 0,
  });

  const handleMouseDown = (e: React.MouseEvent) => {
    // Only left click drags
    if (e.button !== 0) return;
    isDraggingRef.current = true;
    dragStartRef.current = {
      x: e.clientX,
      y: e.clientY,
      panX: camera.panX,
      panY: camera.panY,
    };
  };

  const handleMouseMove = (e: React.MouseEvent) => {
    const rect = containerRef.current?.getBoundingClientRect();
    if (rect) {
      const sx = e.clientX - rect.left;
      const sy = e.clientY - rect.top;
      const worldPos = screenToWorld(sx, sy, camera, baseScale, bounds);
      setCursorWorldPos({
        wx: Math.round(worldPos.wx * 10) / 10,
        wy: Math.round(worldPos.wy * 10) / 10,
      });
    }

    if (!isDraggingRef.current) return;
    const dx = e.clientX - dragStartRef.current.x;
    const dy = e.clientY - dragStartRef.current.y;
    setCamera((prev) => ({
      ...prev,
      panX: dragStartRef.current.panX + dx,
      panY: dragStartRef.current.panY + dy,
    }));
  };

  const handleMouseUp = () => {
    isDraggingRef.current = false;
  };

  // Cursor-centered Wheel Zoom
  const handleWheel = (e: React.WheelEvent) => {
    e.preventDefault();
    const rect = containerRef.current?.getBoundingClientRect();
    if (!rect) return;

    const cursorScreenX = e.clientX - rect.left;
    const cursorScreenY = e.clientY - rect.top;
    const zoomFactor = e.deltaY < 0 ? 1.15 : 0.85;

    setCamera((prev) => zoomAroundPoint(cursorScreenX, cursorScreenY, zoomFactor, prev));
  };

  // Touch Pinch & Pan handling
  const touchStartRef = useRef<{ dist: number; panX: number; panY: number; touchX: number; touchY: number } | null>(
    null
  );

  const handleTouchStart = (e: React.TouchEvent) => {
    if (e.touches.length === 1) {
      touchStartRef.current = {
        dist: 0,
        panX: camera.panX,
        panY: camera.panY,
        touchX: e.touches[0].clientX,
        touchY: e.touches[0].clientY,
      };
    } else if (e.touches.length === 2) {
      const dx = e.touches[0].clientX - e.touches[1].clientX;
      const dy = e.touches[0].clientY - e.touches[1].clientY;
      touchStartRef.current = {
        dist: Math.hypot(dx, dy),
        panX: camera.panX,
        panY: camera.panY,
        touchX: (e.touches[0].clientX + e.touches[1].clientX) / 2,
        touchY: (e.touches[0].clientY + e.touches[1].clientY) / 2,
      };
    }
  };

  const handleTouchMove = (e: React.TouchEvent) => {
    if (!touchStartRef.current) return;

    if (e.touches.length === 1) {
      const dx = e.touches[0].clientX - touchStartRef.current.touchX;
      const dy = e.touches[0].clientY - touchStartRef.current.touchY;
      setCamera((prev) => ({
        ...prev,
        panX: touchStartRef.current!.panX + dx,
        panY: touchStartRef.current!.panY + dy,
      }));
    } else if (e.touches.length === 2) {
      const dx = e.touches[0].clientX - e.touches[1].clientX;
      const dy = e.touches[0].clientY - e.touches[1].clientY;
      const currentDist = Math.hypot(dx, dy);
      const ratio = currentDist / touchStartRef.current.dist;

      const rect = containerRef.current?.getBoundingClientRect();
      if (rect && touchStartRef.current.dist > 0) {
        const midX = (e.touches[0].clientX + e.touches[1].clientX) / 2 - rect.left;
        const midY = (e.touches[0].clientY + e.touches[1].clientY) / 2 - rect.top;
        setCamera((prev) => zoomAroundPoint(midX, midY, ratio, prev));
        touchStartRef.current.dist = currentDist;
      }
    }
  };

  const handleTouchEnd = () => {
    touchStartRef.current = null;
  };

  // Keyboard navigation
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement) return;

      const panStep = 40;
      if (e.key === 'ArrowUp') setCamera((c) => ({ ...c, panY: c.panY + panStep }));
      else if (e.key === 'ArrowDown') setCamera((c) => ({ ...c, panY: c.panY - panStep }));
      else if (e.key === 'ArrowLeft') setCamera((c) => ({ ...c, panX: c.panX + panStep }));
      else if (e.key === 'ArrowRight') setCamera((c) => ({ ...c, panX: c.panX - panStep }));
      else if (e.key === '+' || e.key === '=') {
        setCamera((c) => zoomAroundPoint(dimensions.width / 2, dimensions.height / 2, 1.2, c));
      } else if (e.key === '-' || e.key === '_') {
        setCamera((c) => zoomAroundPoint(dimensions.width / 2, dimensions.height / 2, 0.8, c));
      } else if (e.key === '0') {
        setCamera(getFitCamera(dimensions.width, dimensions.height, bounds));
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [dimensions, bounds, setCamera]);

  return (
    <div
      ref={containerRef}
      className="relative w-full h-full overflow-hidden bg-[#0a0d14] cursor-grab active:cursor-grabbing select-none"
      onMouseDown={handleMouseDown}
      onMouseMove={handleMouseMove}
      onMouseUp={handleMouseUp}
      onMouseLeave={() => {
        handleMouseUp();
        setHoveredEntity(null);
        setCursorWorldPos(null);
      }}
      onWheel={handleWheel}
      onTouchStart={handleTouchStart}
      onTouchMove={handleTouchMove}
      onTouchEnd={handleTouchEnd}
      onClick={() => {
        // Clicking on canvas background deselects
        selectEntity(null);
      }}
    >
      {/* 1. Canvas Layer (Terrain, Grid, Territories, Heatmaps) */}
      <WorldCanvas
        mapSnapshot={mapSnapshot}
        overlays={overlays}
        camera={camera}
        baseScale={baseScale}
        bounds={bounds}
        layers={layers}
        isAwakened={isAwakened}
        width={dimensions.width}
        height={dimensions.height}
      />

      {/* 2. SVG Overlay Layer (Interactive Markers, Routes, Animated Flows, Conflicts) */}
      <WorldSvgOverlay
        mapSnapshot={mapSnapshot}
        overlays={overlays}
        camera={camera}
        baseScale={baseScale}
        bounds={bounds}
        layers={layers}
        filters={filters}
        selectedEntity={selectedEntity}
        onSelectEntity={selectEntity}
        onHoverEntity={setHoveredEntity}
        isAwakened={isAwakened}
        width={dimensions.width}
        height={dimensions.height}
      />

      {/* 3. Floating Hover Tooltip */}
      {hoveredEntity && (
        <div
          className="absolute z-20 pointer-events-none px-3 py-1.5 rounded-lg shadow-xl text-xs backdrop-blur-md border border-slate-700 bg-slate-900/90 text-slate-100 transition-opacity"
          style={{
            left: `${Math.min(dimensions.width - 200, Math.max(10, hoveredEntity.screenX + 15))}px`,
            top: `${Math.min(dimensions.height - 80, Math.max(10, hoveredEntity.screenY - 35))}px`,
          }}
        >
          <div className="font-bold text-sky-400">{hoveredEntity.name}</div>
          <div className="text-[11px] text-slate-300">{hoveredEntity.details}</div>
          <div className="text-[10px] text-slate-400 font-mono mt-0.5">
            Coord: ({hoveredEntity.x.toFixed(1)}, {hoveredEntity.y.toFixed(1)})
          </div>
        </div>
      )}

      {/* 4. Coordinate & Status HUD (Bottom Left) */}
      <div className="absolute bottom-4 left-4 z-10 flex items-center gap-2 pointer-events-none text-xs">
        <div className="bg-slate-900/80 backdrop-blur border border-slate-700/60 rounded-md px-2.5 py-1 text-slate-300 font-mono flex items-center gap-2 shadow-lg">
          <Crosshair className="w-3.5 h-3.5 text-cyan-400" />
          <span>
            {cursorWorldPos ? `X: ${cursorWorldPos.wx.toFixed(1)}  Y: ${cursorWorldPos.wy.toFixed(1)}` : 'World (0, 0)'}
          </span>
          <span className="text-slate-500">|</span>
          <span>Zoom: {camera.zoom.toFixed(2)}x</span>
        </div>
      </div>

      {/* 5. Modern Map Control Dock */}
      <MapControlDock
        onZoomIn={() => setCamera((c) => zoomAroundPoint(dimensions.width / 2, dimensions.height / 2, 1.25, c))}
        onZoomOut={() => setCamera((c) => zoomAroundPoint(dimensions.width / 2, dimensions.height / 2, 0.8, c))}
        onFitWorld={() => setCamera(getFitCamera(dimensions.width, dimensions.height, bounds))}
        onResetView={() => setCamera({ panX: 0, panY: 0, zoom: 1.0 })}
        hasSelectedEntity={!!selectedEntity}
        onCenterSelected={() => {
          if (!selectedEntity) return;
          if (selectedEntity.type === 'SETTLEMENT') {
            const s = mapSnapshot.settlements.find((v) => v.id === selectedEntity.id);
            if (s) centerEntity(s.x, s.y);
          } else if (selectedEntity.type === 'LAIR') {
            const l = mapSnapshot.lairs.find((v) => v.lairId === selectedEntity.id);
            if (l) centerEntity(l.x, l.y);
          } else if (selectedEntity.type === 'ANOMALY') {
            const a = mapSnapshot.anomalies.find((v) => v.anomalyId === selectedEntity.id);
            if (a) centerEntity(a.x, a.y);
          } else if (selectedEntity.type === 'EXPEDITION') {
            const exp = mapSnapshot.expeditions.find((v) => v.expeditionId === selectedEntity.id);
            if (exp) centerEntity(exp.currentX, exp.currentY);
          }
        }}
        isAwakened={isAwakened}
      />

      {/* 6. Contextual Entity Card (Appears on selection if full ledger is closed) */}
      {!drawerOpen && selectedEntity && (
        <ContextualEntityCard
          mapSnapshot={mapSnapshot}
          isAwakened={isAwakened}
          onCenterEntity={(wx, wy) => centerEntity(wx, wy)}
          onOpenLedger={() => setDrawerOpen(true)}
        />
      )}

      {/* 6. Minimap (Top Right) */}
      {showMinimap && (
        <Minimap
          mapSnapshot={mapSnapshot}
          camera={camera}
          baseScale={baseScale}
          bounds={bounds}
          viewportWidth={dimensions.width}
          viewportHeight={dimensions.height}
          onNavigate={(targetWx, targetWy) => centerEntity(targetWx, targetWy)}
          isAwakened={isAwakened}
        />
      )}
    </div>
  );
};

