import { create } from 'zustand';
import { CameraState } from '../utils/coordinates';
import { HistoricalDataResolution } from '../types/api';

export type EntityType = 'SETTLEMENT' | 'LAIR' | 'ANOMALY' | 'EXPEDITION';

export interface SelectedEntity {
  type: EntityType;
  id: string | number;
}

export interface HoveredEntity {
  type: EntityType;
  id: string | number;
  name: string;
  x: number;
  y: number;
  details: string;
  screenX: number;
  screenY: number;
}

export interface MapLayers {
  settlements: boolean;
  lairs: boolean;
  anomalies: boolean;
  expeditions: boolean;
  territoryRadius: boolean;
  biomeBackground: boolean;
  grid: boolean;
  labels: boolean;
}

export interface MapFilters {
  settlementBiome: string;
  minPopulation: number;
  settlementTier: string;
  lairThreat: string;
  anomalyGrade: string;
}

export interface DynamicOverlaysConfig {
  territory: boolean;
  diplomacy: boolean;
  tradeRoutes: boolean;
  monsterThreat: boolean;
  aethericActivity: boolean;
  populationDensity: boolean;
  prosperity: boolean;
  activeConflicts: boolean;
}

export const DEFAULT_DYNAMIC_OVERLAYS: DynamicOverlaysConfig = {
  territory: true,
  diplomacy: true,
  tradeRoutes: true,
  monsterThreat: false,
  aethericActivity: true,
  populationDensity: false,
  prosperity: false,
  activeConflicts: true,
};

export type DrawerTab = 'OVERVIEW' | 'ECONOMY' | 'POPULATION' | 'MILITARY' | 'HISTORY';
export type NavigationModule =
  | 'map'
  | 'people'
  | 'families'
  | 'clans'
  | 'heirlooms'
  | 'leaderboards'
  | 'activity'
  | 'chronicle'
  | 'battles'
  | 'tides'
  | 'expeditions'
  | 'feuds';

interface MapState {
  activeModule: NavigationModule;
  selectedPersonId: number | null;
  selectedFamilyId: number | null;
  selectedClanId: string | null;
  selectedHeirloomId: string | null;
  selectedEquipmentId: string | null;
  comparePeopleIds: { id1: number; id2: number } | null;
  compareOrgIds: { id1: string; id2: string } | null;

  // Phase 3.4 Selection States
  selectedActivityEventId: string | null;
  selectedBattleId: string | null;
  selectedTideId: string | null;
  selectedExpeditionId: string | null;
  selectedAnomalyId: string | null;
  selectedFeudId: string | null;
  selectedChronicleYear: number | null;

  // Phase 3.5 Time Travel, Persistence & Branching States
  timeTravelMode: 'LIVE' | 'HISTORICAL';
  historicalYear: number | null;
  historicalMonth: number | null;
  historicalResolution: HistoricalDataResolution | null;
  saveManagerOpen: boolean;
  branchManagerOpen: boolean;
  newWorldModalOpen: boolean;
  historicalCompareModalOpen: boolean;
  compareDates: { year1: number; year2: number } | null;

  camera: CameraState;
  selectedEntity: SelectedEntity | null;
  hoveredEntity: HoveredEntity | null;
  cursorWorldPos: { wx: number; wy: number } | null;

  layers: MapLayers;
  filters: MapFilters;

  drawerOpen: boolean;
  activeDrawerTab: DrawerTab;

  themeMode: 'AUTO' | 'MUNDANE' | 'AWAKENING';
  isAwakenedBackend: boolean;

  showMinimap: boolean;
  showLegend: boolean;
  showFilters: boolean;
  showLayers: boolean;

  // Phase 3.6 Visual Polish, Overlays & Animation Preferences
  enableAnimations: boolean;
  reducedMotion: boolean;
  animationSpeed: number;
  heatmapOpacity: number;
  dynamicOverlays: DynamicOverlaysConfig;
  historicalPlaybackActive: boolean;
  historicalPlaybackSpeed: number;

  // Actions
  setActiveModule: (module: NavigationModule) => void;
  openPersonProfile: (id: number) => void;
  closePersonProfile: () => void;
  openFamilyProfile: (id: number) => void;
  closeFamilyProfile: () => void;
  openClanProfile: (id: string) => void;
  closeClanProfile: () => void;
  openHeirloomProfile: (id: string) => void;
  closeHeirloomProfile: () => void;
  openEquipmentModal: (id: string) => void;
  closeEquipmentModal: () => void;
  openComparePeople: (id1: number, id2: number) => void;
  openCompareOrgs: (id1: string, id2: string) => void;
  closeCompareModal: () => void;

  // Phase 3.4 Modal & Navigation Actions
  openActivityEvent: (id: string) => void;
  closeActivityEvent: () => void;
  openBattleDetail: (id: string) => void;
  closeBattleDetail: () => void;
  openTideDetail: (id: string) => void;
  closeTideDetail: () => void;
  openExpeditionDetail: (id: string) => void;
  closeExpeditionDetail: () => void;
  openAnomalyDetail: (id: string) => void;
  closeAnomalyDetail: () => void;
  openFeudDetail: (id: string) => void;
  closeFeudDetail: () => void;
  setChronicleYear: (year: number | null) => void;
  navigateToEntityOnMap: (type: EntityType, id: string | number, x?: number, y?: number) => void;

  // Phase 3.5 Actions
  setHistoricalMode: (year: number, month?: number, resolution?: HistoricalDataResolution) => void;
  setLiveMode: () => void;
  setSaveManagerOpen: (open: boolean) => void;
  setBranchManagerOpen: (open: boolean) => void;
  setNewWorldModalOpen: (open: boolean) => void;
  openHistoricalCompare: (year1: number, year2: number) => void;
  closeHistoricalCompare: () => void;

  setCamera: (camera: CameraState | ((prev: CameraState) => CameraState)) => void;
  selectEntity: (entity: SelectedEntity | null) => void;
  setHoveredEntity: (entity: HoveredEntity | null) => void;
  setCursorWorldPos: (pos: { wx: number; wy: number } | null) => void;
  toggleLayer: (layer: keyof MapLayers) => void;
  setFilter: <K extends keyof MapFilters>(key: K, value: MapFilters[K]) => void;
  resetFilters: () => void;
  setDrawerOpen: (open: boolean) => void;
  setActiveDrawerTab: (tab: DrawerTab) => void;
  setThemeMode: (mode: 'AUTO' | 'MUNDANE' | 'AWAKENING') => void;
  setIsAwakenedBackend: (awakened: boolean) => void;
  toggleMinimap: () => void;
  toggleLegend: () => void;
  toggleFilters: () => void;
  toggleLayers: () => void;

  // Phase 3.6 Actions
  setEnableAnimations: (enabled: boolean) => void;
  setReducedMotion: (reduced: boolean) => void;
  setAnimationSpeed: (speed: number) => void;
  setHeatmapOpacity: (opacity: number) => void;
  toggleDynamicOverlay: (key: keyof DynamicOverlaysConfig) => void;
  setHistoricalPlaybackActive: (active: boolean) => void;
  setHistoricalPlaybackSpeed: (speed: number) => void;
}

const DEFAULT_LAYERS: MapLayers = {
  settlements: true,
  lairs: true,
  anomalies: true,
  expeditions: true,
  territoryRadius: true,
  biomeBackground: true,
  grid: false,
  labels: true,
};

const DEFAULT_FILTERS: MapFilters = {
  settlementBiome: 'ALL',
  minPopulation: 0,
  settlementTier: 'ALL',
  lairThreat: 'ALL',
  anomalyGrade: 'ALL',
};

export const useMapStore = create<MapState>((set) => ({
  activeModule: 'map',
  selectedPersonId: null,
  selectedFamilyId: null,
  selectedClanId: null,
  selectedHeirloomId: null,
  selectedEquipmentId: null,
  comparePeopleIds: null,
  compareOrgIds: null,

  // Phase 3.4 Selection States
  selectedActivityEventId: null,
  selectedBattleId: null,
  selectedTideId: null,
  selectedExpeditionId: null,
  selectedAnomalyId: null,
  selectedFeudId: null,
  selectedChronicleYear: null,

  // Phase 3.5 Time Travel & Persistence Defaults
  timeTravelMode: 'LIVE',
  historicalYear: null,
  historicalMonth: null,
  historicalResolution: null,
  saveManagerOpen: false,
  branchManagerOpen: false,
  newWorldModalOpen: false,
  historicalCompareModalOpen: false,
  compareDates: null,

  camera: { panX: 0, panY: 0, zoom: 1.0 },
  selectedEntity: null,
  hoveredEntity: null,
  cursorWorldPos: null,

  layers: DEFAULT_LAYERS,
  filters: DEFAULT_FILTERS,

  drawerOpen: false,
  activeDrawerTab: 'OVERVIEW',

  themeMode: 'AUTO',
  isAwakenedBackend: false,

  showMinimap: true,
  showLegend: false,
  showFilters: false,
  showLayers: false,

  // Phase 3.6 Defaults
  enableAnimations: true,
  reducedMotion: false,
  animationSpeed: 1.0,
  heatmapOpacity: 0.6,
  dynamicOverlays: DEFAULT_DYNAMIC_OVERLAYS,
  historicalPlaybackActive: false,
  historicalPlaybackSpeed: 1.0,

  setActiveModule: (activeModule) => set({ activeModule }),
  openPersonProfile: (selectedPersonId) => set({ selectedPersonId }),
  closePersonProfile: () => set({ selectedPersonId: null }),
  openFamilyProfile: (selectedFamilyId) => set({ selectedFamilyId }),
  closeFamilyProfile: () => set({ selectedFamilyId: null }),
  openClanProfile: (selectedClanId) => set({ selectedClanId }),
  closeClanProfile: () => set({ selectedClanId: null }),
  openHeirloomProfile: (selectedHeirloomId) => set({ selectedHeirloomId }),
  closeHeirloomProfile: () => set({ selectedHeirloomId: null }),
  openEquipmentModal: (selectedEquipmentId) => set({ selectedEquipmentId }),
  closeEquipmentModal: () => set({ selectedEquipmentId: null }),
  openComparePeople: (id1, id2) => set({ comparePeopleIds: { id1, id2 }, compareOrgIds: null }),
  openCompareOrgs: (id1, id2) => set({ compareOrgIds: { id1, id2 }, comparePeopleIds: null }),
  closeCompareModal: () => set({ comparePeopleIds: null, compareOrgIds: null }),

  // Phase 3.4 Modal & Navigation Actions
  openActivityEvent: (selectedActivityEventId) => set({ selectedActivityEventId }),
  closeActivityEvent: () => set({ selectedActivityEventId: null }),
  openBattleDetail: (selectedBattleId) => set({ selectedBattleId }),
  closeBattleDetail: () => set({ selectedBattleId: null }),
  openTideDetail: (selectedTideId) => set({ selectedTideId }),
  closeTideDetail: () => set({ selectedTideId: null }),
  openExpeditionDetail: (selectedExpeditionId) => set({ selectedExpeditionId }),
  closeExpeditionDetail: () => set({ selectedExpeditionId: null }),
  openAnomalyDetail: (selectedAnomalyId) => set({ selectedAnomalyId }),
  closeAnomalyDetail: () => set({ selectedAnomalyId: null }),
  openFeudDetail: (selectedFeudId) => set({ selectedFeudId }),
  closeFeudDetail: () => set({ selectedFeudId: null }),
  setChronicleYear: (selectedChronicleYear) => set({ selectedChronicleYear }),

  // Phase 3.5 Actions
  setHistoricalMode: (year, month = 0, resolution = 'AGGREGATED_HISTORICAL_DATA') =>
    set({
      timeTravelMode: 'HISTORICAL',
      historicalYear: year,
      historicalMonth: month,
      historicalResolution: resolution,
    }),
  setLiveMode: () =>
    set({
      timeTravelMode: 'LIVE',
      historicalYear: null,
      historicalMonth: null,
      historicalResolution: null,
    }),
  setSaveManagerOpen: (saveManagerOpen) => set({ saveManagerOpen }),
  setBranchManagerOpen: (branchManagerOpen) => set({ branchManagerOpen }),
  setNewWorldModalOpen: (newWorldModalOpen) => set({ newWorldModalOpen }),
  openHistoricalCompare: (year1, year2) =>
    set({
      historicalCompareModalOpen: true,
      compareDates: { year1, year2 },
    }),
  closeHistoricalCompare: () =>
    set({
      historicalCompareModalOpen: false,
      compareDates: null,
    }),

  navigateToEntityOnMap: (type, id, x, y) =>
    set((state) => ({
      activeModule: 'map',
      selectedEntity: { type, id },
      drawerOpen: true,
      camera:
        x !== undefined && y !== undefined
          ? { ...state.camera, panX: -x * 32 + window.innerWidth / 2, panY: -y * 32 + window.innerHeight / 2 }
          : state.camera,
    })),

  setCamera: (cameraOrUpdater) =>
    set((state) => ({
      camera: typeof cameraOrUpdater === 'function' ? cameraOrUpdater(state.camera) : cameraOrUpdater,
    })),

  selectEntity: (entity) =>
    set({
      selectedEntity: entity,
      drawerOpen: entity !== null,
    }),

  setHoveredEntity: (hoveredEntity) => set({ hoveredEntity }),

  setCursorWorldPos: (cursorWorldPos) => set({ cursorWorldPos }),

  toggleLayer: (layer) =>
    set((state) => ({
      layers: {
        ...state.layers,
        [layer]: !state.layers[layer],
      },
    })),

  setFilter: (key, value) =>
    set((state) => ({
      filters: {
        ...state.filters,
        [key]: value,
      },
    })),

  resetFilters: () => set({ filters: DEFAULT_FILTERS }),

  setDrawerOpen: (drawerOpen) => set({ drawerOpen }),

  setActiveDrawerTab: (activeDrawerTab) => set({ activeDrawerTab }),

  setThemeMode: (themeMode) => set({ themeMode }),

  setIsAwakenedBackend: (isAwakenedBackend) => set({ isAwakenedBackend }),

  toggleMinimap: () => set((s) => ({ showMinimap: !s.showMinimap })),
  toggleLegend: () => set((s) => ({ showLegend: !s.showLegend })),
  toggleFilters: () => set((s) => ({ showFilters: !s.showFilters, showLayers: false })),
  toggleLayers: () => set((s) => ({ showLayers: !s.showLayers, showFilters: false })),

  // Phase 3.6 Action implementations
  setEnableAnimations: (enableAnimations) => set({ enableAnimations }),
  setReducedMotion: (reducedMotion) => set({ reducedMotion }),
  setAnimationSpeed: (animationSpeed) => set({ animationSpeed }),
  setHeatmapOpacity: (heatmapOpacity) => set({ heatmapOpacity }),
  toggleDynamicOverlay: (key) =>
    set((state) => ({
      dynamicOverlays: {
        ...state.dynamicOverlays,
        [key]: !state.dynamicOverlays[key],
      },
    })),
  setHistoricalPlaybackActive: (historicalPlaybackActive) => set({ historicalPlaybackActive }),
  setHistoricalPlaybackSpeed: (historicalPlaybackSpeed) => set({ historicalPlaybackSpeed }),
}));

