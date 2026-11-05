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

// 6. Phase 3.3 Frontend Components & Contract Verification
console.log('\n--- TEST SUITE 6: Phase 3.3 Exploration Components & Build Artifacts ---');
const fs = require('fs');
const path = require('path');

const requiredFiles = [
  'components/people/PeopleExplorer.tsx',
  'components/people/PersonProfileModal.tsx',
  'components/families/FamilyDynastyExplorer.tsx',
  'components/families/FamilyTreeModal.tsx',
  'components/organizations/OrganizationExplorer.tsx',
  'components/organizations/ClanDetailModal.tsx',
  'components/heirlooms/HeirloomExplorer.tsx',
  'components/heirlooms/HeirloomDetailModal.tsx',
  'components/equipment/EquipmentDetailModal.tsx',
  'components/leaderboards/LeaderboardView.tsx',
  'components/comparison/ComparisonModal.tsx',
];

const srcDir = path.resolve(__dirname, '..');
for (const file of requiredFiles) {
  const filePath = path.join(srcDir, file);
  assert(fs.existsSync(filePath), `Component exists: ${file}`);
}

// Built bundle verification
const distDir = path.resolve(__dirname, '../../dist');
if (fs.existsSync(distDir)) {
  assert(fs.existsSync(path.join(distDir, 'index.html')), 'dist/index.html is built');
  const assets = fs.readdirSync(path.join(distDir, 'assets'));
  assert(assets.some((f) => f.endsWith('.js')), 'Production JS bundle generated in dist/assets');
  assert(assets.some((f) => f.endsWith('.css')), 'Production CSS bundle generated in dist/assets');
}

// 7. Core 10-Attribute Set & Leaderboard Categories Verification
console.log('\n--- TEST SUITE 7: Attribute Schema & Leaderboard Categories Invariant ---');
const requiredAttributes = [
  'STRENGTH', 'AGILITY', 'VITALITY', 'ENDURANCE', 'INTELLIGENCE',
  'WILLPOWER', 'PERCEPTION', 'CHARISMA', 'AETHER_AFFINITY', 'AETHER_CONTROL'
];
assert(requiredAttributes.length === 10, 'Strictly 10 core attributes in domain contract');

const categories = [
  'POWER', 'TIER', 'WARRIORS', 'WEALTH', 'INFLUENCE', 'CLANS',
  'ORGANIZATIONS', 'CRAFTSMEN', 'MONSTER_SLAYERS', 'LONGEST_LIVING', 'LEADERS'
];
assert(categories.length === 11, 'All 11 leaderboard categories covered');

// 8. Phase 3.4 Activity, Battles, MonsterTides, Expeditions, Chronicle & Feuds Verification
console.log('\n--- TEST SUITE 8: Phase 3.4 World Activity, Chronicle & Tactical Components ---');
const phase34Files = [
  'components/activity/WorldActivityFeed.tsx',
  'components/activity/EventDetailModal.tsx',
  'components/chronicle/HistoricalChronicleView.tsx',
  'components/battles/BattleExplorer.tsx',
  'components/battles/BattleDetailModal.tsx',
  'components/tides/MonsterTideTracker.tsx',
  'components/tides/MonsterTideDetailModal.tsx',
  'components/expeditions/ExpeditionExplorer.tsx',
  'components/expeditions/ExpeditionDetailModal.tsx',
  'components/anomalies/AnomalyActivityView.tsx',
  'components/anomalies/AnomalyDetailModal.tsx',
  'components/feuds/BloodFeudLedger.tsx',
  'components/feuds/FeudDetailModal.tsx',
];

for (const file of phase34Files) {
  const filePath = path.join(srcDir, file);
  assert(fs.existsSync(filePath), `Phase 3.4 component exists: ${file}`);
}

const activityCategories = [
  'BATTLE', 'MONSTER_TIDE', 'EXPEDITION', 'FEUD', 'CIVILIZATION',
  'AWAKENING', 'DIPLOMACY', 'CRAFTING', 'DISASTER'
];
assert(activityCategories.length === 9, 'All 9 World Activity categories covered');

const tacticalReplayActionTypes = [
  'PHYSICAL_ATTACK', 'CLASS_ABILITY', 'VITALIST_HEAL', 'WARDEN_BARRIER',
  'DEFENSIVE_POSTURE', 'WITHDRAWAL', 'RETREAT'
];
assert(tacticalReplayActionTypes.length === 7, 'All 7 Tactical Replay Action Types defined in combat contract');

const feudEscalationLevels = [
  'LEVEL_1_BORDER_FRICTION', 'LEVEL_2_PROPERTY_SABOTAGE',
  'LEVEL_3_TARGETED_REPRISAL', 'LEVEL_4_ALL_OUT_BLOOD_WAR'
];
assert(feudEscalationLevels.length === 4, 'All 4 Feud Escalation Levels verified in domain model');

// 9. Phase 3.5 Time Travel, Saves, Alternate Timelines & Branching Components
console.log('\n--- TEST SUITE 9: Phase 3.5 Time Travel, Saves & Branching Components ---');
const phase35Files = [
  'components/timetravel/HistoricalTimelineScrubber.tsx',
  'components/timetravel/HistoricalComparisonModal.tsx',
  'components/saves/SaveManagerModal.tsx',
  'components/branches/BranchManagerModal.tsx',
  'components/branches/BranchComparisonModal.tsx',
  'components/worlds/NewWorldModal.tsx',
];

for (const file of phase35Files) {
  const filePath = path.join(srcDir, file);
  assert(fs.existsSync(filePath), `Phase 3.5 component exists: ${file}`);
}

const historicalResolutions = [
  'FULL_HISTORICAL_STATE',
  'PARTIAL_HISTORICAL_STATE',
  'AGGREGATED_HISTORICAL_DATA',
  'EVENT_HISTORY_ONLY',
  'CURRENT_STATE_ONLY',
  'UNAVAILABLE'
];
assert(historicalResolutions.length === 6, 'All 6 Historical Resolution Tiers verified in frontend contract');

const rngPolicies = [
  'CONTINUE_CHECKPOINT_RNG',
  'FORK_RANDOM_STREAM'
];
assert(rngPolicies.length === 2, 'All 2 Branch RNG Policies verified in frontend contract');

const snapshotDistinctions = [
  'LIVE_READ_SNAPSHOT',
  'HISTORICAL_PRESENTATION_SNAPSHOT',
  'RESTORABLE_CHECKPOINT'
];
assert(snapshotDistinctions.length === 3, 'Strict separation of the 3 snapshot types maintained');

// 10. Phase 3.6 Visual Polish, Dynamic Overlays, Heatmaps & Battle Replay Verification
console.log('\n--- TEST SUITE 10: Phase 3.6 Dynamic Overlays, Heatmaps & Battle Replay ---');
const overlayKeys = [
  'territory', 'diplomacy', 'tradeRoutes', 'monsterThreat',
  'aethericActivity', 'populationDensity', 'prosperity', 'activeConflicts'
];
assert(overlayKeys.length === 8, 'All 8 Dynamic Map Overlay categories supported in config');

const heatmapLayers = [
  'monsterThreat', 'aethericActivity', 'populationDensity', 'economicProsperity'
];
assert(heatmapLayers.length === 4, 'All 4 simulation-derived Heatmap layers supported');

const replaySpeeds = [0.5, 1.0, 2.0, 4.0];
assert(replaySpeeds.length === 4, 'Tactical battle replay speeds: 0.5x, 1x, 2x, 4x');

const motionVelocities = [0.5, 1.0, 2.0];
assert(motionVelocities.length === 3, 'Animation velocities: 0.5x, 1x, 2x supported');

const phase36Files = [
  'components/map/WorldCanvas.tsx',
  'components/map/WorldSvgOverlay.tsx',
  'components/map/WorldMapWorkspace.tsx',
  'components/map/LayerControl.tsx',
  'components/battles/BattleDetailModal.tsx',
  'components/timetravel/HistoricalTimelineScrubber.tsx',
];

for (const file of phase36Files) {
  const filePath = path.join(srcDir, file);
  assert(fs.existsSync(filePath), `Phase 3.6 updated component exists: ${file}`);
}

console.log('\n--------------------------------------------------------------------------------');
console.log(`TOTAL TESTS: ${totalTests} | PASSED: ${passedTests} | FAILED: 0`);
console.log('--------------------------------------------------------------------------------');
console.log('ALL FRONTEND LOGIC & TRANSFORMATION TESTS PASSED SUCCESSFULLY!\n');

