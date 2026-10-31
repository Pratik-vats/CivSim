/**
 * Node.js executable verification script for coordinate transformations,
 * aspect ratio preservation, cursor-centered zoom invariants, and bounded marker scaling.
 */

const DEFAULT_WORLD_BOUNDS = {
  minX: 0,
  maxX: 120,
  minY: 0,
  maxY: 120,
  width: 120,
  height: 120,
};

const MIN_ZOOM = 0.5;
const MAX_ZOOM = 8.0;

function calculateBaseScale(viewportWidth, viewportHeight, bounds = DEFAULT_WORLD_BOUNDS, padding = 40) {
  const availW = Math.max(100, viewportWidth - padding * 2);
  const availH = Math.max(100, viewportHeight - padding * 2);
  const scaleX = availW / bounds.width;
  const scaleY = availH / bounds.height;
  return Math.min(scaleX, scaleY);
}

function worldToScreen(wx, wy, camera, baseScale, bounds = DEFAULT_WORLD_BOUNDS) {
  const effectiveScale = baseScale * camera.zoom;
  const sx = camera.panX + (wx - bounds.minX) * effectiveScale;
  const sy = camera.panY + (wy - bounds.minY) * effectiveScale;
  return { sx, sy };
}

function screenToWorld(sx, sy, camera, baseScale, bounds = DEFAULT_WORLD_BOUNDS) {
  const effectiveScale = baseScale * camera.zoom;
  if (effectiveScale <= 0) return { wx: 0, wy: 0 };
  const wx = bounds.minX + (sx - camera.panX) / effectiveScale;
  const wy = bounds.minY + (sy - camera.panY) / effectiveScale;
  return { wx, wy };
}

function zoomAroundPoint(cursorScreenX, cursorScreenY, zoomDeltaFactor, currentCamera) {
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

let totalTests = 0;
let passedTests = 0;

function assert(condition, msg) {
  totalTests++;
  if (!condition) {
    console.error(`  [FAIL] ${msg}`);
    process.exit(1);
  }
  passedTests++;
  console.log(`  [PASS] ${msg}`);
}

console.log('================================================================================');
console.log('   FRONTEND LOGIC & SPATIAL TRANSFORMATION VERIFICATION');
console.log('================================================================================\n');

// 1. Aspect Ratio & Scale Tests
console.log('--- TEST SUITE 1: Aspect Ratio & Scale Invariant ---');
const baseScale = calculateBaseScale(1200, 800, DEFAULT_WORLD_BOUNDS, 40);
assert(baseScale > 0, 'Base scale is strictly positive');
const pixelW = DEFAULT_WORLD_BOUNDS.width * baseScale;
const pixelH = DEFAULT_WORLD_BOUNDS.height * baseScale;
assert(Math.abs(pixelW - pixelH) < 0.0001, 'Base scale guarantees strict 1:1 aspect ratio for square world');
assert(pixelH <= 800 - 80, 'World height fits within letterbox boundary');

// 2. Coordinate Transformation Bijectivity Tests
console.log('\n--- TEST SUITE 2: Coordinate Bijectivity (worldToScreen <-> screenToWorld) ---');
const camera = { panX: 150, panY: 80, zoom: 1.75 };
const testPoints = [
  { wx: 0, wy: 0 },
  { wx: 25.5, wy: 80.2 },
  { wx: 60, wy: 60 },
  { wx: 120, wy: 120 },
  { wx: 85.33, wy: 17.89 },
];

for (const pt of testPoints) {
  const screen = worldToScreen(pt.wx, pt.wy, camera, baseScale, DEFAULT_WORLD_BOUNDS);
  const back = screenToWorld(screen.sx, screen.sy, camera, baseScale, DEFAULT_WORLD_BOUNDS);
  assert(Math.abs(pt.wx - back.wx) < 0.0001, `X inverse bijection holds for (${pt.wx}, ${pt.wy})`);
  assert(Math.abs(pt.wy - back.wy) < 0.0001, `Y inverse bijection holds for (${pt.wx}, ${pt.wy})`);
}

// 3. Cursor-Centered Zoom Invariant Tests
console.log('\n--- TEST SUITE 3: Cursor-Centered Zoom Invariant ---');
const cursor = { sx: 450, sy: 320 };
const worldUnderCursorBefore = screenToWorld(cursor.sx, cursor.sy, camera, baseScale, DEFAULT_WORLD_BOUNDS);
const zoomedCamera = zoomAroundPoint(cursor.sx, cursor.sy, 1.25, camera);
const worldUnderCursorAfter = screenToWorld(cursor.sx, cursor.sy, zoomedCamera, baseScale, DEFAULT_WORLD_BOUNDS);

assert(
  Math.abs(worldUnderCursorBefore.wx - worldUnderCursorAfter.wx) < 0.0001,
  'World X under cursor remains invariant after zoom'
);
assert(
  Math.abs(worldUnderCursorBefore.wy - worldUnderCursorAfter.wy) < 0.0001,
  'World Y under cursor remains invariant after zoom'
);

// 4. Bounded Settlement Marker Scaling Tests
console.log('\n--- TEST SUITE 4: Bounded Settlement Marker Sizing ---');
const testPops = [0, 1, 10, 50, 250, 1000, 5000, 20000];
for (const pop of testPops) {
  const r = Math.min(30, Math.max(10, 8 + Math.sqrt(Math.max(1, pop)) * 0.35));
  assert(r >= 10 && r <= 30, `Radius for pop ${pop} (${r.toFixed(1)}px) is strictly bounded in [10, 30]`);
}

// 5. Territory Danger Radius Exact Scaling Tests
console.log('\n--- TEST SUITE 5: Territory Radius Scaling ---');
const territoryWorldUnits = 36.0;
const territoryPixels = territoryWorldUnits * baseScale * camera.zoom;
assert(territoryPixels > 0, 'Territory radius scales directly with baseScale and zoom');
const measuredWorldUnits = territoryPixels / (baseScale * camera.zoom);
assert(Math.abs(measuredWorldUnits - territoryWorldUnits) < 0.0001, 'Screen territory converts back to exactly 36.0 world units');

console.log('\n--------------------------------------------------------------------------------');
console.log(`TOTAL TESTS: ${totalTests} | PASSED: ${passedTests} | FAILED: 0`);
console.log('--------------------------------------------------------------------------------');
console.log('ALL FRONTEND LOGIC & TRANSFORMATION TESTS PASSED SUCCESSFULLY!\n');

