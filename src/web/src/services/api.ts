import {
  WorldSummaryDto,
  WorldMapSnapshot,
  SettlementDetailDto,
  SearchResultDto,
  SimulationStatusDto,
  CommandStatusDto,
  PagedResponse,
  EventDto,
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
  return res.json();
}

export async function resumeSimulation(): Promise<CommandStatusDto> {
  const res = await fetch(`${API_BASE}/simulation/resume`, { method: 'POST' });
  return res.json();
}

export async function stepMonth(): Promise<CommandStatusDto> {
  const res = await fetch(`${API_BASE}/simulation/step/month`, { method: 'POST' });
  return res.json();
}

export async function stepYear(): Promise<CommandStatusDto> {
  const res = await fetch(`${API_BASE}/simulation/step/year`, { method: 'POST' });
  return res.json();
}

export async function setSimulationSpeed(speed: string): Promise<CommandStatusDto> {
  const res = await fetch(`${API_BASE}/simulation/speed`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ speed }),
  });
  return res.json();
}

