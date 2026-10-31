export type SettlementTier = 'HAMLET' | 'VILLAGE' | 'TOWN' | 'CITY' | 'MEGACITY' | string;
export type BiomeType = 'RIVER_VALLEY' | 'HIGHLAND_MINE' | 'FOREST' | 'COASTAL' | string;
export type ThreatRank = 'COMMON' | 'UNCOMMON' | 'RARE' | 'LEGENDARY' | 'MYTHIC' | string;
export type AnomalyState = 'FORMING' | 'REVEALED' | 'DISCOVERED' | 'SECURING' | 'CONTROLLED' | 'CONTESTED' | 'DEPLETED' | 'MUTATED' | string;

export interface MapSettlementDto {
  id: number;
  name: string;
  x: number;
  y: number;
  biome: BiomeType;
  tier: SettlementTier;
  population: number;
  fortificationLevel: number;
  techLevel: number;
  leaderName: string;
  inFamine: boolean;
}

export interface MapLairDto {
  lairId: string;
  sourceAnomalyId: string;
  x: number;
  y: number;
  biome: BiomeType;
  threatRank: ThreatRank;
  state: string;
  monsterCount: number;
  capacity: number;
  territoryRadius: number;
  primarySpecies: string;
  secondarySpecies: string;
}

export interface MapAnomalyDto {
  anomalyId: string;
  x: number;
  y: number;
  state: AnomalyState;
  resourceCategory: string;
  resourceGrade: string;
  initialQuantity: number;
  remainingQuantity: number;
  observable: boolean;
  controllingOwnerId?: string | null;
  securingRemainingMonths: number;
}

export interface MapExpeditionDto {
  expeditionId: string;
  originVillageId: string;
  targetAnomalyId: string;
  currentX: number;
  currentY: number;
  status: string;
  personnelCount: number;
  carriedCargo: number;
}

export interface WorldMapSnapshot {
  worldId: string;
  snapshotVersion: number;
  tickId: number;
  year: number;
  month: number;
  width: number;
  height: number;
  settlements: MapSettlementDto[];
  lairs: MapLairDto[];
  anomalies: MapAnomalyDto[];
  expeditions: MapExpeditionDto[];
}

export interface WorldSummaryDto {
  worldId: string;
  year: number;
  month: number;
  totalMonths: number;
  status: string;
  humanPopulation: number;
  livingMalePopulation: number;
  livingFemalePopulation: number;
  settlementCount: number;
  activeOrganizationCount: number;
  awakenedPopulation: number;
  livingMonsterCount: number;
  activeMonsterLairCount: number;
  totalFoodStockpile: number;
  totalWoodStockpile: number;
  totalOreStockpile: number;
  totalToolsStockpile: number;
  totalCitizenWealth: number;
  totalCivicTreasury: number;
  currentEra: string;
}

export interface SettlementDetailDto {
  id: number;
  name: string;
  x: number;
  y: number;
  biome: BiomeType;
  tier: SettlementTier;
  population: number;
  livingMales: number;
  livingFemales: number;
  leaderId: number | null;
  leaderName: string;
  foodStockpile: number;
  woodStockpile: number;
  oreStockpile: number;
  toolsStockpile: number;
  treasury: number;
  techLevel: number;
  fortificationLevel: number;
  housingCapacity: number;
  peakPopulation: number;
  monsterCores: number;
  inFamine: boolean;
  diplomaticSummary: string;
  craftingFacilities: string[];
  storedEquipmentCount: number;
}

export interface SearchResultDto {
  id: string;
  name: string;
  type: 'SETTLEMENT' | 'LAIR' | 'ANOMALY' | 'ORGANIZATION' | 'PERSON';
  x: number;
  y: number;
  description: string;
  subInfo: string;
}

export interface SimulationStatusDto {
  state: 'STOPPED' | 'RUNNING' | 'PAUSED' | 'ERROR';
  simulationSpeed: string;
  ticksPerSecond: number;
  totalElapsedTicks: number;
  pendingCommands: number;
  pendingQueries: number;
  workerAlive: boolean;
  activeWorldId: string;
}

export interface CommandStatusDto {
  commandId: string;
  type: string;
  status: 'SUBMITTED' | 'QUEUED' | 'EXECUTING' | 'COMPLETED' | 'REJECTED' | 'FAILED';
  message?: string;
}

export interface EventDto {
  id: number;
  year: number;
  month: number;
  totalMonths: number;
  type: string;
  summary: string;
  severity: string;
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

