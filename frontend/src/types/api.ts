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
  territoryRadius?: number;
  militaryStrength?: number;
  prosperityScore?: number;
  threatLevel?: number;
  dominantClanName?: string;
  alliedSettlementIds?: number[];
  hostileSettlementIds?: number[];
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
  worldId: string;
  lifecycleState: 'INITIALIZING' | 'STOPPED' | 'RUNNING' | 'PAUSED' | 'HUMAN_EXTINCT' | 'ERROR';
  speed: string;
  tickIntervalMillis: number;
  currentYear: number;
  currentMonth: number;
  totalMonths: number;
  livingHumans: number;
  livingMonsters: number;
  activeLairs: number;
  pendingCommandQueueSize: number;
  pendingQueryQueueSize: number;
  activeSseSubscribers: number;
  usedHeapMb: number;
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

// ==========================================
// Phase 3.3 Types: People, Lineage, Clans, Heirlooms & Leaderboards
// ==========================================

export interface AttributeScoresDto {
  name: string;
  baseline: number;
  awakening: number;
  development: number;
  temporary: number;
  effective: number;
}

export interface EquipmentItemDto {
  equipmentId: string;
  displayName: string;
  slot: string;
  equipmentType: string;
  category: string;
  rarity: string;
  tier: number;
  condition: string;
  durability: number;
  maxDurability: number;
  creatorPersonId?: string | null;
  creationYear: number;
  isRegisteredHeirloom: boolean;
  registeredHeirloomId?: string | null;
  enchantments: string[];
}

export interface PersonCombatProfileDto {
  physicalAttack: number;
  physicalDefense: number;
  accuracy: number;
  evasion: number;
  aetherPotency: number;
  barrier: number;
  currentHealth: number;
  maxHealth: number;
  currentStamina: number;
  maxStamina: number;
  injuries: string[];
}

export interface PersonSummaryDto {
  id: number;
  name: string;
  settlementId?: number | null;
  settlementName?: string | null;
  familyId?: number | null;
  familyName?: string | null;
  clanId?: string | null;
  clanName?: string | null;
  age: number;
  alive: boolean;
  gender: string;
  occupation: string;
  awakened: boolean;
  supernaturalClass: string;
  aspect: string;
  tier: number;
  rarity: string;
  derivedPower: number;
  wealth: number;
}

export interface PersonDetailDto {
  id: number;
  name: string;
  gender: string;
  age: number;
  alive: boolean;
  birthYear: number;
  deathYear?: number | null;
  ageAtDeath?: number | null;
  causeOfDeath?: string | null;
  villageId?: number | null;
  villageName?: string | null;
  occupation: string;
  wealth: number;
  bankBalance: number;
  loanDebt: number;
  isAwakened: boolean;
  supernaturalClass: string;
  aspect: string;
  rarity: string;
  tier: number;
  maxTier: number;
  mastery: number;
  luck: number;
  greed: number;
  darkResonance: number;
  derivedPower: number;
  breakthroughAttempts: number;
  breakthroughSuccesses: number;
  equippedItems: Record<string, string>;
  equipmentDetails: Record<string, EquipmentItemDto>;
  combatProfile: PersonCombatProfileDto;
  attributes: Record<string, AttributeScoresDto>;
  familyId?: number | null;
  familySurname?: string | null;
  fatherId?: number | null;
  fatherName?: string | null;
  motherId?: number | null;
  motherName?: string | null;
  spouseId?: number | null;
  spouseName?: string | null;
  childrenIds: number[];
  organizationMemberships: string[];
  achievements: string[];
}

export interface FamilySummaryDto {
  id: number;
  surname: string;
  foundedYear: number;
  founderId?: number | null;
  founderName?: string | null;
  livingCount: number;
  deceasedCount: number;
  totalLineageCount: number;
  generationsCount: number;
  mostInfluentialPersonId?: number | null;
  mostInfluentialPersonName?: string | null;
  highestTierPersonId?: number | null;
  highestTierPersonName?: string | null;
  highestTier: number;
}

export interface FamilyDetailDto {
  summary: FamilySummaryDto;
  livingMembers: PersonSummaryDto[];
  deceasedMembers: PersonSummaryDto[];
  clanAffiliations: string[];
}

export interface GraphNodeDto {
  id: string;
  label: string;
  type: string;
  data: Record<string, any>;
}

export interface GraphEdgeDto {
  id: string;
  source: string;
  target: string;
  type: string;
  label: string;
}

export interface GraphDataDto {
  nodes: GraphNodeDto[];
  edges: GraphEdgeDto[];
}

export interface BearerRecordDto {
  bearerId: string;
  bearerName: string;
  tenureInfo: string;
  successionReason: string;
}

export interface HeirloomSummaryDto {
  heirloomId: string;
  equipmentId: string;
  artifactTitle: string;
  equipmentType: string;
  rarity: string;
  tier: number;
  condition: string;
  durability: number;
  maxDurability: number;
  owningClanId: string;
  owningClanName: string;
  currentBearerId?: string | null;
  currentBearerName?: string | null;
  custodyStatus: string;
  successionPolicy: string;
  registrationYear: number;
}

export interface HeirloomDetailDto {
  heirloomId: string;
  equipmentId: string;
  artifactTitle: string;
  equipmentType: string;
  rarity: string;
  tier: number;
  craftQuality: string;
  condition: string;
  durability: number;
  maxDurability: number;
  enchantments: string[];
  creatorPersonId?: string | null;
  creatorPersonName?: string | null;
  owningClanId: string;
  owningClanName: string;
  currentBearerId?: string | null;
  currentBearerName?: string | null;
  custodyStatus: string;
  successionPolicy: string;
  registrationYear: number;
  registrationMonth: number;
  pastBearers: BearerRecordDto[];
  battleChronicle: string[];
}

export interface OrganizationSummaryDto {
  id: string;
  name: string;
  type: string;
  alignment: string;
  status: string;
  foundingYear: number;
  leaderId?: string | null;
  leaderName?: string | null;
  reputation: number;
  memberCount: number;
  heirloomCount: number;
  battlesWon: number;
  battlesTotal: number;
}

export interface OrganizationDetailDto {
  id: string;
  name: string;
  type: string;
  alignment: string;
  status: string;
  foundingYear: number;
  foundingMonth: number;
  founderPersonId?: string | null;
  leaderId?: string | null;
  leaderName?: string | null;
  reputation: number;
  memberCount: number;
  heirloomCount: number;
  heirloomNames: string[];
  battlesWon: number;
  battlesTotal: number;
  memberCasualties: number;
  activeFeudIds: string[];
  recentEvents: string[];
}

export interface LeaderboardEntryDto {
  rank: number;
  entityId: string;
  entityName: string;
  score: number;
  subtitle: string;
  details: string;
}

export interface LeaderboardDto {
  category: string;
  year: number;
  month: number;
  historicalAvailable: boolean;
  unavailableMessage?: string | null;
  entries: LeaderboardEntryDto[];
}

export interface PersonComparisonDto {
  person1: PersonDetailDto;
  person2: PersonDetailDto;
  comparisonDate: string;
  powerSummary: string;
  wealthSummary: string;
}

export interface OrganizationComparisonDto {
  org1: OrganizationDetailDto;
  org2: OrganizationDetailDto;
  comparisonDate: string;
  influenceSummary: string;
  militarySummary: string;
}

// ==========================================
// Phase 3.4 World Activity & Historical DTOs
// ==========================================

export interface WorldActivityEventDto {
  id: string;
  worldId: string;
  year: number;
  month: number;
  totalMonths: number;
  sequence: number;
  category: string;
  subtype: string;
  title: string;
  summary: string;
  severity: string;
  location?: string | null;
  posX?: number | null;
  posY?: number | null;
  primaryEntityId: string;
  primaryEntityType: string;
  relatedEntityIds: string[];
  dataAvailability: string;
}

export interface BattleRoundRecordDto {
  roundNumber: number;
  actionSequence: number;
  actorId: string;
  actorName: string;
  targetId?: string | null;
  targetName?: string | null;
  actionType: string;
  damage: number;
  healing: number;
  staminaCost: number;
  aetherCost: number;
  injuryInflicted?: string | null;
  targetDefeated: boolean;
  actorEscaped: boolean;
  description: string;
}

export interface BattleSummaryDto {
  battleId: string;
  year: number;
  month: number;
  battleType: string;
  outcome: string;
  victoriousSideId: string;
  totalRounds: number;
  fatalitiesCount: number;
  incapacitatedCount: number;
  escapedCount: number;
  summaryText: string;
}

export interface BattleDetailDto {
  battleId: string;
  year: number;
  month: number;
  battleType: string;
  outcome: string;
  victoriousSideId: string;
  totalRounds: number;
  totalInjuriesInflicted: number;
  initialCountsPerSide: Record<string, number>;
  finalLivingCountsPerSide: Record<string, number>;
  damageDealtPerSide: Record<string, number>;
  fatalities: string[];
  incapacitated: string[];
  escaped: string[];
  summaryText: string;
}

export interface BattleParticipantDto {
  combatantId: string;
  name: string;
  sideId: string;
  entityType: string;
  supernaturalClass: string;
  initialHealth: number;
  maxHealth: number;
  tier: number;
  isAliveAtEnd: boolean;
}

export interface BattleRoundsDto {
  battleId: string;
  hasRoundReplay: boolean;
  unavailableMessage?: string | null;
  rounds: BattleRoundRecordDto[];
  participants?: BattleParticipantDto[];
}

export interface MonsterTideSummaryDto {
  tideId: string;
  sourceLairId: string;
  targetVillageId: number;
  targetVillageName: string;
  state: string;
  mobilizedYear: number;
  mobilizedMonth: number;
  arrivalYear: number;
  arrivalMonth: number;
  totalMobilizedCount: number;
  outcome: string;
  seerDetected: boolean;
  warningLeadMonths: number;
}

export interface MonsterTideDetailDto {
  tideId: string;
  sourceLairId: string;
  targetVillageId: number;
  targetVillageName: string;
  sourceX: number;
  sourceY: number;
  targetX: number;
  targetY: number;
  currentX: number;
  currentY: number;
  state: string;
  mobilizedYear: number;
  mobilizedMonth: number;
  arrivalYear: number;
  arrivalMonth: number;
  travelDurationMonths: number;
  seerDetected: boolean;
  warningLeadMonths: number;
  outcome: string;
  wave1MonsterIds: string[];
  wave2MonsterIds: string[];
  wave3MonsterIds: string[];
  totalMobilized: number;
  slainCount: number;
  retreatCount: number;
  defenderFatalities: number;
  civilianFatalities: number;
  structuralDamagePercent: number;
  foodDestroyed: number;
  associatedBattleId?: string | null;
}

export interface ExpeditionSummaryDto {
  expeditionId: string;
  sponsorId: string;
  sponsorType: string;
  targetAnomalyId: string;
  status: string;
  personnelCount: number;
  currentX: number;
  currentY: number;
  carriedCargo: number;
  maxCapacity: number;
  supplies: number;
  departureMonth: number;
  arrivalMonth: number;
}

export interface ExpeditionDetailDto {
  expeditionId: string;
  sponsorId: string;
  sponsorType: string;
  targetAnomalyId: string;
  anomalyName: string;
  originX: number;
  originY: number;
  targetX: number;
  targetY: number;
  currentX: number;
  currentY: number;
  status: string;
  claimObjective: string;
  rosterPersonIds: string[];
  rosterPersonNames: string[];
  carriedCargo: number;
  maxCapacity: number;
  supplies: number;
  departureMonth: number;
  arrivalMonth: number;
  returnArrivalMonth: number;
}

export interface AnomalyDetailDto {
  anomalyId: string;
  resourceCategory: string;
  resourceGrade: string;
  x: number;
  y: number;
  state: string;
  initialQuantity: number;
  remainingQuantity: number;
  controllingOwnerId?: string | null;
  observable: boolean;
  revealedYear: number;
  discoveredYear: number;
  contestsCount: number;
}

export interface AnomalyContestDto {
  contestId: string;
  anomalyId: string;
  year: number;
  month: number;
  contestants: string[];
  winnerId?: string | null;
  resolved: boolean;
  skirmishOccurred: boolean;
  battleId?: string | null;
}

export interface FeudActionRecordDto {
  actionId: string;
  actionType: string;
  actorId: string;
  targetId: string;
  year: number;
  month: number;
  description: string;
  battleId?: string | null;
}

export interface FeudSummaryDto {
  feudId: string;
  originGrievanceId: string;
  initiatorPartyId: string;
  targetPartyId: string;
  escalationLevel: string;
  status: string;
  actionsCount: number;
  startingYear: number;
  lastActionYear: number;
}

export interface FeudDetailDto {
  feudId: string;
  originGrievanceId: string;
  grievanceIncident: string;
  grievanceSeverity: string;
  initiatorPartyId: string;
  targetPartyId: string;
  escalationLevel: string;
  status: string;
  actionHistory: FeudActionRecordDto[];
  involvedClanIds: string[];
  involvedFamilyIds: string[];
}

// Settlement history shown in the active settlement inspector.
export interface SettlementHistoryPointDto {
  year: number;
  population: number;
  births: number;
  deaths: number;
  food: number;
  wood: number;
  ore: number;
  tools: number;
  fortificationLevel: number;
  inFamine: boolean;
}

export interface SettlementHistoryDto {
  settlementId: number;
  settlementName: string;
  foundedYear: number;
  destroyedYear?: number | null;
  currentPopulation: number;
  history: SettlementHistoryPointDto[];
}

export interface NewWorldRequest {
  seed?: number;
  worldName?: string;
  fantasyMode?: boolean;
  awakeningThreshold?: number;
  extinctionPolicy?: string;
}

export interface NewWorldResponse {
  worldId: string;
  worldName: string;
  seed: number;
  awakeningStartYear: number;
  status: string;
}

// Stage 3.6 Overlays, Heatmaps & Replay Types
export interface SettlementTerritoryDto {
  settlementId: number;
  settlementName: string;
  x?: number;
  y?: number;
  centerX?: number;
  centerY?: number;
  territoryRadius: number;
  tier: string;
  dominantClan: string;
  alliedSettlementIds: number[];
  hostileSettlementIds: number[];
}

export interface DiplomaticLinkDto {
  originSettlementId: number;
  originName: string;
  originX: number;
  originY: number;
  targetSettlementId: number;
  targetName: string;
  targetX: number;
  targetY: number;
  relationType: string;
  tradeVolume: number;
  primaryTradedGood: string;
}

export interface ActiveConflictDto {
  conflictId: string;
  name: string;
  x: number;
  y: number;
  conflictType: string;
  totalCombatants: number;
  year: number;
  month: number;
  outcome: string;
}

export interface MonsterInvasionVectorDto {
  tideId: string;
  sourceX: number;
  sourceY: number;
  targetX: number;
  targetY: number;
  currentX: number;
  currentY: number;
  state: string;
  monsterCount: number;
  targetSettlementName: string;
  targetSettlementId: number;
}

export interface HeatmapPointDto {
  x: number;
  y: number;
  intensity: number;
  radius: number;
  sourceId: string;
  label: string;
}

export interface HeatmapsDto {
  monsterThreat: HeatmapPointDto[];
  aethericActivity: HeatmapPointDto[];
  populationDensity: HeatmapPointDto[];
  economicProsperity: HeatmapPointDto[];
}

export interface WorldOverlaysDto {
  worldId: string;
  year: number;
  month: number;
  isHistorical: boolean;
  territories: SettlementTerritoryDto[];
  diplomaticLinks: DiplomaticLinkDto[];
  activeConflicts: ActiveConflictDto[];
  monsterInvasions: MonsterInvasionVectorDto[];
  heatmaps: HeatmapsDto;
}





