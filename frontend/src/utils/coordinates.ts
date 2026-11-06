/**
 * Precise, bidirectional coordinate transformations between
 * CivSim 2D World Coordinates and Screen Pixels.
 *
 * Guarantees:
 * 1. Aspect ratio is strictly preserved (no horizontal/vertical stretching).
 * 2. Canvas and SVG overlays use identical transformations.
 * 3. Fractional world coordinates supported.
 * 4. Cursor-centered zoom without camera jump.
 */

export interface WorldBounds {
  minX: number;
  maxX: number;
  minY: number;
  maxY: number;
  width: number;
  height: number;
}

export interface CameraState {
  panX: number;
  panY: number;
  zoom: number;
}

export const DEFAULT_WORLD_BOUNDS: WorldBounds = {
  minX: 0,
  maxX: 1000,
  minY: 0,
  maxY: 1000,
  width: 1000,
  height: 1000,
};

export const CIVILIZATION_BOUNDS: WorldBounds = { minX: 450, maxX: 550, minY: 450, maxY: 550, width: 100, height: 100 };
// Java snapshots now use native Map 1 coordinates; this identity adapter prevents double projection.
export function projectWorldToMap(wx: number, wy: number) { return { x: wx, y: wy }; }
export function unprojectMapToWorld(x: number, y: number) { return { wx: x, wy: y }; }

export const MIN_ZOOM = 0.5;
export const MAX_ZOOM = 20.0;

/**
 * Calculates the uniform base scale to fit the world into the viewport.
 */
export function calculateBaseScale(
  viewportWidth: number,
  viewportHeight: number,
  bounds: WorldBounds = DEFAULT_WORLD_BOUNDS,
  padding: number = 40
): number {
  const availW = Math.max(100, viewportWidth - padding * 2);
  const availH = Math.max(100, viewportHeight - padding * 2);
  const scaleX = availW / bounds.width;
  const scaleY = availH / bounds.height;
  return Math.min(scaleX, scaleY);
}

/**
 * Converts World Coordinates (wx, wy) to Viewport Screen Pixels (sx, sy).
 */
export function worldToScreen(
  wx: number,
  wy: number,
  camera: CameraState,
  baseScale: number,
  bounds: WorldBounds = DEFAULT_WORLD_BOUNDS
): { sx: number; sy: number } {
  const mapX = Number.isFinite(wx) ? wx : bounds.minX;
  const mapY = Number.isFinite(wy) ? wy : bounds.minY;
  const safeScale = Number.isFinite(baseScale) ? baseScale : 1;
  const safeZoom = Number.isFinite(camera?.zoom) ? camera.zoom : 1;
  const safePanX = Number.isFinite(camera?.panX) ? camera.panX : 0;
  const safePanY = Number.isFinite(camera?.panY) ? camera.panY : 0;

  const effectiveScale = safeScale * safeZoom;
  const sx = safePanX + (mapX - bounds.minX) * effectiveScale;
  const sy = safePanY + (mapY - bounds.minY) * effectiveScale;
  return { sx, sy };
}

/**
 * Converts Viewport Screen Pixels (sx, sy) to World Coordinates (wx, wy).
 */
export function screenToWorld(
  sx: number,
  sy: number,
  camera: CameraState,
  baseScale: number,
  bounds: WorldBounds = DEFAULT_WORLD_BOUNDS
): { wx: number; wy: number } {
  const safeScale = Number.isFinite(baseScale) ? baseScale : 1;
  const safeZoom = Number.isFinite(camera?.zoom) ? camera.zoom : 1;
  const safePanX = Number.isFinite(camera?.panX) ? camera.panX : 0;
  const safePanY = Number.isFinite(camera?.panY) ? camera.panY : 0;

  const effectiveScale = safeScale * safeZoom;
  if (effectiveScale <= 0) return { wx: bounds.minX, wy: bounds.minY };
  const safeSx = Number.isFinite(sx) ? sx : safePanX;
  const safeSy = Number.isFinite(sy) ? sy : safePanY;
  return { wx: bounds.minX + (safeSx - safePanX) / effectiveScale, wy: bounds.minY + (safeSy - safePanY) / effectiveScale };
}

export function getCoverCamera(viewportWidth: number, viewportHeight: number, bounds: WorldBounds = DEFAULT_WORLD_BOUNDS): CameraState {
  const fitScale = calculateBaseScale(viewportWidth, viewportHeight, bounds);
  const coverScale = Math.max(viewportWidth / bounds.width, viewportHeight / bounds.height);
  const zoom = fitScale > 0 ? coverScale / fitScale : 1;
  return { panX: (viewportWidth - bounds.width * coverScale) / 2, panY: (viewportHeight - bounds.height * coverScale) / 2, zoom };
}

export function getRegionFitCamera(viewportWidth: number, viewportHeight: number, region: WorldBounds, fullBounds: WorldBounds = DEFAULT_WORLD_BOUNDS, padding = 40): CameraState {
  const fullScale = calculateBaseScale(viewportWidth, viewportHeight, fullBounds, padding);
  const regionScale = calculateBaseScale(viewportWidth, viewportHeight, region, padding);
  const zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, regionScale / fullScale));
  const centerX = ((region.minX + region.maxX) / 2 - fullBounds.minX) * fullScale;
  const centerY = ((region.minY + region.maxY) / 2 - fullBounds.minY) * fullScale;
  return { panX: viewportWidth / 2 - centerX * zoom, panY: viewportHeight / 2 - centerY * zoom, zoom };
}

/**
 * Calculates initial camera position that perfectly centers and fits the world.
 */
export function getFitCamera(
  viewportWidth: number,
  viewportHeight: number,
  bounds: WorldBounds = DEFAULT_WORLD_BOUNDS,
  padding: number = 40,
  zoom: number = 1.0
): CameraState {
  const baseScale = calculateBaseScale(viewportWidth, viewportHeight, bounds, padding);
  const safeZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom));
  const worldPixelW = bounds.width * baseScale * safeZoom;
  const worldPixelH = bounds.height * baseScale * safeZoom;
  const panX = (viewportWidth - worldPixelW) / 2;
  const panY = (viewportHeight - worldPixelH) / 2;
  return {
    panX,
    panY,
    zoom: safeZoom,
  };
}

/**
 * Performs cursor-centered zooming.
 */
export function zoomAroundPoint(
  cursorScreenX: number,
  cursorScreenY: number,
  zoomDeltaFactor: number,
  currentCamera: CameraState
): CameraState {
  const targetZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, currentCamera.zoom * zoomDeltaFactor));
  if (targetZoom === currentCamera.zoom) return currentCamera;

  const ratio = targetZoom / currentCamera.zoom;
  const newPanX = cursorScreenX - (cursorScreenX - currentCamera.panX) * ratio;
  const newPanY = cursorScreenY - (cursorScreenY - currentCamera.panY) * ratio;

  return {
    panX: newPanX,
    panY: newPanY,
    zoom: targetZoom,
  };
}

