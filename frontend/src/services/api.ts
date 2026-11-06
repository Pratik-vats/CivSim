import {
  WorldSummaryDto,
  WorldMapSnapshot,
  SettlementDetailDto,
  SearchResultDto,
  SimulationStatusDto,
  CommandStatusDto,
  PagedResponse,
  EventDto,
  PersonSummaryDto,
  PersonDetailDto,
  FamilySummaryDto,
  FamilyDetailDto,
  GraphDataDto,
  OrganizationSummaryDto,
  OrganizationDetailDto,
  HeirloomSummaryDto,
  HeirloomDetailDto,
  EquipmentItemDto,
  LeaderboardDto,
  PersonComparisonDto,
  OrganizationComparisonDto,
  WorldActivityEventDto,
  BattleSummaryDto,
  BattleDetailDto,
  BattleRoundsDto,
  MonsterTideSummaryDto,
  MonsterTideDetailDto,
  ExpeditionSummaryDto,
  ExpeditionDetailDto,
  AnomalyDetailDto,
  AnomalyContestDto,
  FeudSummaryDto,
  FeudDetailDto,
  SettlementHistoryDto,
  NewWorldRequest,
  NewWorldResponse,
  WorldOverlaysDto,
} from '../types/api';

const API_BASE = '/api';

export async function fetchWorldSummary(): Promise<WorldSummaryDto> {
  const res = await fetch(`${API_BASE}/world/summary`);
  if (!res.ok) throw new Error(`Failed to fetch world summary: ${res.statusText}`);
  return res.json();
}

export async function fetchWorldMap(): Promise<WorldMapSnapshot> {
  const res = await fetch(`${API_BASE}/world/map`);
  if (!res.ok) throw new Error(`Failed to fetch world map snapshot: ${res.statusText}`);
  return res.json();
}

export async function fetchSettlementDetail(id: number): Promise<SettlementDetailDto> {
  const res = await fetch(`${API_BASE}/settlements/${id}`);
  if (!res.ok) throw new Error(`Failed to fetch settlement #${id}: ${res.statusText}`);
  return res.json();
}

export async function fetchSettlementEvents(villageName?: string): Promise<EventDto[]> {
  const url = villageName
    ? `${API_BASE}/events?size=25&type=${encodeURIComponent(villageName)}`
    : `${API_BASE}/events?size=25`;
  const res = await fetch(url);
  if (!res.ok) return [];
  const data: PagedResponse<EventDto> = await res.json();
  return data.content || [];
}

export async function searchWorld(q: string): Promise<SearchResultDto[]> {
  if (!q.trim()) return [];
  const res = await fetch(`${API_BASE}/world/search?q=${encodeURIComponent(q.trim())}&limit=15`);
  if (!res.ok) return [];
  return res.json();
}

export async function fetchSimulationStatus(): Promise<SimulationStatusDto> {
  const res = await fetch(`${API_BASE}/simulation/status`);
  if (!res.ok) throw new Error(`Failed to fetch simulation status: ${res.statusText}`);
  return res.json();
}

export async function pauseSimulation(): Promise<CommandStatusDto> {
  const res = await fetch(`${API_BASE}/simulation/pause`, { method: 'POST' });
  if (!res.ok) throw new Error(`Pause request failed: ${res.statusText}`);
  const command: CommandStatusDto = await res.json();
  return waitForLifecycleState('PAUSED', command);
}

export async function resumeSimulation(): Promise<CommandStatusDto> {
  const res = await fetch(`${API_BASE}/simulation/resume`, { method: 'POST' });
  if (!res.ok) throw new Error(`Resume request failed: ${res.statusText}`);
  const command: CommandStatusDto = await res.json();
  return waitForLifecycleState('RUNNING', command);
}

async function waitForLifecycleState(target: 'PAUSED' | 'RUNNING', command: CommandStatusDto): Promise<CommandStatusDto> {
  const deadline = Date.now() + 5000;
  while (Date.now() < deadline) {
    const status = await fetchSimulationStatus();
    if (status.lifecycleState === target) return command;
    if (status.lifecycleState === 'ERROR' || status.lifecycleState === 'STOPPED') {
      throw new Error(`Simulation is ${status.lifecycleState.toLowerCase()}; cannot ${target === 'PAUSED' ? 'pause' : 'resume'}.`);
    }
    await new Promise((resolve) => setTimeout(resolve, 100));
  }
  throw new Error(`Timed out waiting for the simulation to ${target === 'PAUSED' ? 'pause' : 'resume'}.`);
}

async function waitForCommand(command: CommandStatusDto): Promise<CommandStatusDto> {
  const deadline = Date.now() + 5000;
  while (Date.now() < deadline) {
    const res = await fetch(`${API_BASE}/simulation/commands/${encodeURIComponent(command.commandId)}`);
    if (!res.ok) throw new Error(`Could not confirm simulation command: ${res.statusText}`);
    const latest: CommandStatusDto = await res.json();
    if (latest.status === 'COMPLETED') return latest;
    if (latest.status === 'REJECTED' || latest.status === 'FAILED') {
      throw new Error(latest.message || `Simulation command ${latest.status.toLowerCase()}.`);
    }
    await new Promise((resolve) => setTimeout(resolve, 100));
  }
  throw new Error('Timed out waiting for the simulation command to complete.');
}

export async function stepMonth(): Promise<CommandStatusDto> {
  const res = await fetch(`${API_BASE}/simulation/step/month`, { method: 'POST' });
  if (!res.ok) throw new Error(`Step month failed: ${res.statusText}`);
  return waitForCommand(await res.json());
}

export async function stepYear(): Promise<CommandStatusDto> {
  const res = await fetch(`${API_BASE}/simulation/step/year`, { method: 'POST' });
  if (!res.ok) throw new Error(`Step year failed: ${res.statusText}`);
  return waitForCommand(await res.json());
}

export async function setSimulationSpeed(speed: string): Promise<CommandStatusDto> {
  const res = await fetch(`${API_BASE}/simulation/speed`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ speed }),
  });
  if (!res.ok) throw new Error(`Changing simulation speed failed: ${res.statusText}`);
  return waitForCommand(await res.json());
}

// =========================================================================
// Phase 3.3 Endpoints: People, Families, Clans, Heirlooms, Leaderboards & Comparison
// =========================================================================

export interface PeopleFilterParams {
  page?: number;
  size?: number;
  search?: string;
  settlementId?: number;
  familyId?: number;
  clanId?: string;
  alive?: boolean;
  awakened?: boolean;
  supernaturalClass?: string;
  tier?: number;
  sortBy?: string;
  sortDir?: 'asc' | 'desc';
}

export async function fetchPeople(params: PeopleFilterParams = {}): Promise<PagedResponse<PersonSummaryDto>> {
  const query = new URLSearchParams();
  if (params.page !== undefined) query.set('page', String(params.page));
  if (params.size !== undefined) query.set('size', String(params.size));
  if (params.search) query.set('search', params.search);
  if (params.settlementId !== undefined) query.set('settlementId', String(params.settlementId));
  if (params.familyId !== undefined) query.set('familyId', String(params.familyId));
  if (params.clanId) query.set('clanId', params.clanId);
  if (params.alive !== undefined) query.set('alive', String(params.alive));
  if (params.awakened !== undefined) query.set('awakened', String(params.awakened));
  if (params.supernaturalClass) query.set('supernaturalClass', params.supernaturalClass);
  if (params.tier !== undefined) query.set('tier', String(params.tier));
  if (params.sortBy) query.set('sortBy', params.sortBy);
  if (params.sortDir) query.set('sortDir', params.sortDir);

  const res = await fetch(`${API_BASE}/people?${query.toString()}`);
  if (!res.ok) throw new Error(`Failed to fetch people list: ${res.statusText}`);
  return res.json();
}

export async function fetchPersonDetail(id: number): Promise<PersonDetailDto> {
  const res = await fetch(`${API_BASE}/people/${id}`);
  if (!res.ok) throw new Error(`Failed to fetch person #${id}: ${res.statusText}`);
  return res.json();
}

export async function fetchPersonTree(id: number): Promise<GraphDataDto> {
  const res = await fetch(`${API_BASE}/people/${id}/tree`);
  if (!res.ok) throw new Error(`Failed to fetch person tree #${id}: ${res.statusText}`);
  return res.json();
}

export interface FamilyFilterParams {
  page?: number;
  size?: number;
  search?: string;
  sortBy?: string;
  sortDir?: 'asc' | 'desc';
}

export async function fetchFamilies(params: FamilyFilterParams = {}): Promise<PagedResponse<FamilySummaryDto>> {
  const query = new URLSearchParams();
  if (params.page !== undefined) query.set('page', String(params.page));
  if (params.size !== undefined) query.set('size', String(params.size));
  if (params.search) query.set('search', params.search);
  if (params.sortBy) query.set('sortBy', params.sortBy);
  if (params.sortDir) query.set('sortDir', params.sortDir);

  const res = await fetch(`${API_BASE}/families?${query.toString()}`);
  if (!res.ok) throw new Error(`Failed to fetch families list: ${res.statusText}`);
  return res.json();
}

export async function fetchFamilyDetail(id: number): Promise<FamilyDetailDto> {
  const res = await fetch(`${API_BASE}/families/${id}`);
  if (!res.ok) throw new Error(`Failed to fetch family #${id}: ${res.statusText}`);
  return res.json();
}

export async function fetchFamilyTree(id: number): Promise<GraphDataDto> {
  const res = await fetch(`${API_BASE}/families/${id}/tree`);
  if (!res.ok) throw new Error(`Failed to fetch family tree #${id}: ${res.statusText}`);
  return res.json();
}

export async function fetchOrganizations(page = 0, size = 20): Promise<PagedResponse<OrganizationSummaryDto>> {
  const res = await fetch(`${API_BASE}/organizations?page=${page}&size=${size}`);
  if (!res.ok) throw new Error(`Failed to fetch organizations: ${res.statusText}`);
  return res.json();
}

export async function fetchOrganizationDetail(id: string): Promise<OrganizationDetailDto> {
  const res = await fetch(`${API_BASE}/organizations/${id}`);
  if (!res.ok) throw new Error(`Failed to fetch organization #${id}: ${res.statusText}`);
  return res.json();
}

export async function fetchOrganizationDiplomacyGraph(id: string): Promise<GraphDataDto> {
  const res = await fetch(`${API_BASE}/organizations/${id}/diplomacy-graph`);
  if (!res.ok) throw new Error(`Failed to fetch diplomacy graph #${id}: ${res.statusText}`);
  return res.json();
}

export interface HeirloomFilterParams {
  page?: number;
  size?: number;
  clanId?: string;
  custodyStatus?: string;
  rarity?: string;
}

export async function fetchHeirlooms(params: HeirloomFilterParams = {}): Promise<PagedResponse<HeirloomSummaryDto>> {
  const query = new URLSearchParams();
  if (params.page !== undefined) query.set('page', String(params.page));
  if (params.size !== undefined) query.set('size', String(params.size));
  if (params.clanId) query.set('clanId', params.clanId);
  if (params.custodyStatus) query.set('custodyStatus', params.custodyStatus);
  if (params.rarity) query.set('rarity', params.rarity);

  const res = await fetch(`${API_BASE}/heirlooms?${query.toString()}`);
  if (!res.ok) throw new Error(`Failed to fetch heirlooms: ${res.statusText}`);
  return res.json();
}

export async function fetchHeirloomDetail(id: string): Promise<HeirloomDetailDto> {
  const res = await fetch(`${API_BASE}/heirlooms/${id}`);
  if (!res.ok) throw new Error(`Failed to fetch heirloom #${id}: ${res.statusText}`);
  return res.json();
}

export async function fetchHeirloomSuccessionGraph(id: string): Promise<GraphDataDto> {
  const res = await fetch(`${API_BASE}/heirlooms/${id}/succession-graph`);
  if (!res.ok) throw new Error(`Failed to fetch heirloom succession graph #${id}: ${res.statusText}`);
  return res.json();
}

export async function fetchEquipmentDetail(id: string): Promise<EquipmentItemDto> {
  const res = await fetch(`${API_BASE}/equipment/${id}`);
  if (!res.ok) throw new Error(`Failed to fetch equipment #${id}: ${res.statusText}`);
  return res.json();
}

export async function fetchLeaderboard(category = 'POWER', year?: number): Promise<LeaderboardDto> {
  const query = new URLSearchParams();
  query.set('category', category);
  if (year !== undefined) query.set('year', String(year));

  const res = await fetch(`${API_BASE}/leaderboards?${query.toString()}`);
  if (!res.ok) throw new Error(`Failed to fetch leaderboard: ${res.statusText}`);
  return res.json();
}

export async function comparePeople(id1: number, id2: number): Promise<PersonComparisonDto> {
  const res = await fetch(`${API_BASE}/compare/people?id1=${id1}&id2=${id2}`);
  if (!res.ok) throw new Error(`Failed to compare people: ${res.statusText}`);
  return res.json();
}

export async function compareOrganizations(id1: string, id2: string): Promise<OrganizationComparisonDto> {
  const res = await fetch(`${API_BASE}/compare/organizations?id1=${encodeURIComponent(id1)}&id2=${encodeURIComponent(id2)}`);
  if (!res.ok) throw new Error(`Failed to compare organizations: ${res.statusText}`);
  return res.json();
}

// =========================================================================
// Phase 3.4 Endpoints: World Activity, Chronicle, Battles, Tides, Expeditions, Anomalies, Feuds
// =========================================================================

export interface ActivityFilterParams {
  page?: number;
  size?: number;
  category?: string;
  severity?: string;
  search?: string;
  entityId?: string;
  year?: number;
}

export async function fetchWorldActivity(params: ActivityFilterParams = {}): Promise<PagedResponse<WorldActivityEventDto>> {
  const query = new URLSearchParams();
  if (params.page !== undefined) query.set('page', String(params.page));
  if (params.size !== undefined) query.set('size', String(params.size));
  if (params.category) query.set('category', params.category);
  if (params.severity) query.set('severity', params.severity);
  if (params.search) query.set('search', params.search);
  if (params.entityId) query.set('entityId', params.entityId);
  if (params.year !== undefined) {
    query.set('yearMin', String(params.year));
    query.set('yearMax', String(params.year));
  }

  const res = await fetch(`${API_BASE}/activity?${query.toString()}`);
  if (!res.ok) throw new Error(`Failed to fetch world activity: ${res.statusText}`);
  return res.json();
}

export async function fetchActivityEvent(id: string): Promise<WorldActivityEventDto> {
  const res = await fetch(`${API_BASE}/activity/${encodeURIComponent(id)}`);
  if (!res.ok) throw new Error(`Failed to fetch activity event #${id}: ${res.statusText}`);
  return res.json();
}

export async function fetchBattles(page = 0, size = 20): Promise<PagedResponse<BattleSummaryDto>> {
  const res = await fetch(`${API_BASE}/battles?page=${page}&size=${size}`);
  if (!res.ok) throw new Error(`Failed to fetch battles: ${res.statusText}`);
  return res.json();
}

export async function fetchBattleDetail(id: string): Promise<BattleDetailDto> {
  const res = await fetch(`${API_BASE}/battles/${encodeURIComponent(id)}`);
  if (!res.ok) throw new Error(`Failed to fetch battle detail #${id}: ${res.statusText}`);
  return res.json();
}

export async function fetchBattleRounds(id: string): Promise<BattleRoundsDto> {
  const res = await fetch(`${API_BASE}/battles/${encodeURIComponent(id)}/rounds`);
  if (!res.ok) throw new Error(`Failed to fetch battle rounds #${id}: ${res.statusText}`);
  return res.json();
}

export interface MonsterTideFilterParams {
  page?: number;
  size?: number;
  state?: string;
}

export async function fetchMonsterTides(params: MonsterTideFilterParams = {}): Promise<PagedResponse<MonsterTideSummaryDto>> {
  const query = new URLSearchParams();
  if (params.page !== undefined) query.set('page', String(params.page));
  if (params.size !== undefined) query.set('size', String(params.size));
  if (params.state) query.set('state', params.state);

  const res = await fetch(`${API_BASE}/tides?${query.toString()}`);
  if (!res.ok) throw new Error(`Failed to fetch monster tides: ${res.statusText}`);
  return res.json();
}

export async function fetchMonsterTideDetail(id: string): Promise<MonsterTideDetailDto> {
  const res = await fetch(`${API_BASE}/tides/${encodeURIComponent(id)}`);
  if (!res.ok) throw new Error(`Failed to fetch monster tide #${id}: ${res.statusText}`);
  return res.json();
}

export interface ExpeditionFilterParams {
  page?: number;
  size?: number;
  status?: string;
}

export async function fetchExpeditions(params: ExpeditionFilterParams = {}): Promise<PagedResponse<ExpeditionSummaryDto>> {
  const query = new URLSearchParams();
  if (params.page !== undefined) query.set('page', String(params.page));
  if (params.size !== undefined) query.set('size', String(params.size));
  if (params.status) query.set('status', params.status);

  const res = await fetch(`${API_BASE}/expeditions?${query.toString()}`);
  if (!res.ok) throw new Error(`Failed to fetch expeditions: ${res.statusText}`);
  return res.json();
}

export async function fetchExpeditionDetail(id: string): Promise<ExpeditionDetailDto> {
  const res = await fetch(`${API_BASE}/expeditions/${encodeURIComponent(id)}`);
  if (!res.ok) throw new Error(`Failed to fetch expedition #${id}: ${res.statusText}`);
  return res.json();
}

export interface AnomalyFilterParams {
  page?: number;
  size?: number;
  revealedOnly?: boolean;
}

export async function fetchAnomalies(params: AnomalyFilterParams = {}): Promise<PagedResponse<AnomalyDetailDto>> {
  const query = new URLSearchParams();
  if (params.page !== undefined) query.set('page', String(params.page));
  if (params.size !== undefined) query.set('size', String(params.size));
  if (params.revealedOnly !== undefined) query.set('revealedOnly', String(params.revealedOnly));

  const res = await fetch(`${API_BASE}/anomalies?${query.toString()}`);
  if (!res.ok) throw new Error(`Failed to fetch anomalies: ${res.statusText}`);
  return res.json();
}

export async function fetchAnomalyDetail(id: string): Promise<AnomalyDetailDto> {
  const res = await fetch(`${API_BASE}/anomalies/${encodeURIComponent(id)}`);
  if (!res.ok) throw new Error(`Failed to fetch anomaly #${id}: ${res.statusText}`);
  return res.json();
}

export async function fetchAnomalyContests(id: string): Promise<AnomalyContestDto[]> {
  const res = await fetch(`${API_BASE}/anomalies/${encodeURIComponent(id)}/contests`);
  if (!res.ok) throw new Error(`Failed to fetch anomaly contests #${id}: ${res.statusText}`);
  return res.json();
}

export interface FeudFilterParams {
  page?: number;
  size?: number;
  status?: string;
  escalationLevel?: string;
}

export async function fetchFeuds(params: FeudFilterParams = {}): Promise<PagedResponse<FeudSummaryDto>> {
  const query = new URLSearchParams();
  if (params.page !== undefined) query.set('page', String(params.page));
  if (params.size !== undefined) query.set('size', String(params.size));
  if (params.status) query.set('status', params.status);
  if (params.escalationLevel) query.set('escalationLevel', params.escalationLevel);

  const res = await fetch(`${API_BASE}/feuds?${query.toString()}`);
  if (!res.ok) throw new Error(`Failed to fetch feuds: ${res.statusText}`);
  return res.json();
}

export async function fetchFeudDetail(id: string): Promise<FeudDetailDto> {
  const res = await fetch(`${API_BASE}/feuds/${encodeURIComponent(id)}`);
  if (!res.ok) throw new Error(`Failed to fetch feud #${id}: ${res.statusText}`);
  return res.json();
}

// ==========================================
// Settlement history displayed in the live inspector.
// ==========================================

export async function fetchSettlementHistory(id: number): Promise<SettlementHistoryDto> {
  const res = await fetch(`${API_BASE}/historical/settlements/${id}/history`);
  if (!res.ok) throw new Error(`Failed to fetch settlement #${id} history: ${res.statusText}`);
  return res.json();
}
export async function createNewWorld(req?: NewWorldRequest): Promise<NewWorldResponse> {
  const res = await fetch(`${API_BASE}/worlds/new`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req || {}),
  });
  if (!res.ok) throw new Error(`Failed to create new world: ${res.statusText}`);
  return res.json();
}

export async function fetchWorldConfig(): Promise<Record<string, unknown>> {
  const res = await fetch(`${API_BASE}/worlds/config`);
  if (!res.ok) throw new Error(`Failed to fetch world config: ${res.statusText}`);
  return res.json();
}

// ==========================================
// Phase 3.6: World Overlays & Dynamic Heatmaps
// ==========================================

export async function fetchWorldOverlays(): Promise<WorldOverlaysDto> {
  const res = await fetch(`${API_BASE}/world/overlays`);
  if (!res.ok) throw new Error(`Failed to fetch world overlays: ${res.statusText}`);
  return res.json();
}


