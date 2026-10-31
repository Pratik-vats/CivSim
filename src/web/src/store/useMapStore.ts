import { create } from 'zustand';
import { CameraState } from '../utils/coordinates';

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

export type DrawerTab = 'OVERVIEW' | 'ECONOMY' | 'POPULATION' | 'MILITARY' | 'HISTORY';

interface MapState {
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

  // Actions
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
}

const DEFAULT_LAYERS: MapLayers = {
  settlements: true,
  lairs: true,
  anomalies: true,
  expeditions: true,
  territoryRadius: true,
  biomeBackground: true,
  grid: true,
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
  showLegend: true,
  showFilters: false,
  showLayers: false,

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
}));

