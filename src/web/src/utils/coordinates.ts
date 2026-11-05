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
  maxX: 120,
  minY: 0,
  maxY: 120,
  width: 120,
  height: 120,
};

export const MIN_ZOOM = 0.5;
export const MAX_ZOOM = 8.0;

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
  const safeX = Number.isFinite(wx) ? wx : bounds.minX;
  const safeY = Number.isFinite(wy) ? wy : bounds.minY;
  const safeScale = Number.isFinite(baseScale) ? baseScale : 1;
  const safeZoom = Number.isFinite(camera?.zoom) ? camera.zoom : 1;
  const safePanX = Number.isFinite(camera?.panX) ? camera.panX : 0;
  const safePanY = Number.isFinite(camera?.panY) ? camera.panY : 0;

  const effectiveScale = safeScale * safeZoom;
  const sx = safePanX + (safeX - bounds.minX) * effectiveScale;
  const sy = safePanY + (safeY - bounds.minY) * effectiveScale;
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
  const wx = bounds.minX + (safeSx - safePanX) / effectiveScale;
  const wy = bounds.minY + (safeSy - safePanY) / effectiveScale;
  return { wx, wy };
}

/**
 * Calculates initial camera position that perfectly centers and fits the world.
 */
export function getFitCamera(
  viewportWidth: number,
  viewportHeight: number,
  bounds: WorldBounds = DEFAULT_WORLD_BOUNDS,
  padding: number = 40
): CameraState {
  const baseScale = calculateBaseScale(viewportWidth, viewportHeight, bounds, padding);
  const worldPixelW = bounds.width * baseScale;
  const worldPixelH = bounds.height * baseScale;
  const panX = (viewportWidth - worldPixelW) / 2;
  const panY = (viewportHeight - worldPixelH) / 2;
  return {
    panX,
    panY,
    zoom: 1.0,
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

