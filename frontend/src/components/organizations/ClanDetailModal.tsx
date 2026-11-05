import React, { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { fetchOrganizationDetail, fetchOrganizationDiplomacyGraph } from '../../services/api';
import { useMapStore } from '../../store/useMapStore';
import {
  ReactFlow,
  Background,
  Controls,
  MiniMap,
  Handle,
  Position,
  Node,
  Edge,
  NodeProps,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import {
  X,
  Compass,
  Crown,
  Shield,
  Award,
  Swords,
  Coins,
  Calendar,
  Network,
  AlertTriangle,
} from 'lucide-react';

const OrgGraphNode: React.FC<NodeProps> = ({ data }) => {
  const openClanProfile = useMapStore((s) => s.openClanProfile);
  const o = data as any;
  const isTarget = o.isTarget;

  return (
    <div
      onClick={() => !isTarget && openClanProfile(o.id)}
      className={`p-3 rounded-xl shadow-lg border transition ${
        isTarget
          ? 'bg-amber-950/80 border-amber-500 ring-2 ring-amber-500/30'
          : 'bg-slate-900 border-slate-700 hover:border-slate-500 cursor-pointer'
      } min-w-[160px]`}
    >
      <Handle type="target" position={Position.Top} className="!bg-slate-500" />
      <div className="flex items-center justify-between text-[10px] text-slate-400 mb-1">
        <span>{o.type}</span>
        <span className="font-mono">#{o.id}</span>
      </div>
      <div className="font-bold text-xs text-slate-100 truncate">{o.name}</div>
      <div className="text-[10px] text-slate-400 flex items-center justify-between mt-1">
        <span>{o.members} Members</span>
        <span className="text-amber-400 font-semibold">{o.alignment}</span>
      </div>
      <Handle type="source" position={Position.Bottom} className="!bg-slate-500" />
    </div>
  );
};

const nodeTypes = {
  orgPeer: OrgGraphNode,
  orgTarget: OrgGraphNode,
};

export const ClanDetailModal: React.FC = () => {
  const selectedClanId = useMapStore((s) => s.selectedClanId);
  const closeClanProfile = useMapStore((s) => s.closeClanProfile);
  const openPersonProfile = useMapStore((s) => s.openPersonProfile);
  const openHeirloomProfile = useMapStore((s) => s.openHeirloomProfile);

  const { data: org, isLoading } = useQuery({
    queryKey: ['orgDetail', selectedClanId],
    queryFn: () => (selectedClanId ? fetchOrganizationDetail(selectedClanId) : null),
    enabled: !!selectedClanId,
  });

  const { data: diploGraph } = useQuery({
    queryKey: ['orgDiploGraph', selectedClanId],
    queryFn: () => (selectedClanId ? fetchOrganizationDiplomacyGraph(selectedClanId) : null),
    enabled: !!selectedClanId,
  });

  const flowNodes: Node[] = useMemo(() => {
    if (!diploGraph?.nodes) return [];
    return diploGraph.nodes.map((n) => ({
      id: n.id,
      type: n.type === 'orgTarget' ? 'orgTarget' : 'orgPeer',
      position: { x: (n.data?.x as number) || 200, y: (n.data?.y as number) || 150 },
      data: n.data,
    }));
  }, [diploGraph]);

  const flowEdges: Edge[] = useMemo(() => {
    if (!diploGraph?.edges) return [];
    return diploGraph.edges.map((e) => {
      const isHostile = e.type?.toLowerCase().includes('hostile') || e.type?.toLowerCase().includes('war');
      const isAlly = e.type?.toLowerCase().includes('allied') || e.type?.toLowerCase().includes('friendly');
      return {
        id: e.id,
        source: e.source,
        target: e.target,
        label: e.label,
        animated: isHostile,
        style: {
          stroke: isHostile ? '#ef4444' : isAlly ? '#10b981' : '#64748b',
          strokeWidth: 2,
        },
        labelStyle: { fill: '#cbd5e1', fontSize: 10 },
      };
    });
  }, [diploGraph]);

  if (!selectedClanId) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4 animate-in fade-in duration-200">
      <div className="bg-slate-900 border border-slate-700 rounded-xl shadow-2xl w-full max-w-5xl h-[88vh] flex flex-col overflow-hidden">
        {/* Header */}
        <div className="p-4 bg-slate-800/90 border-b border-slate-700 flex items-start justify-between">
          <div className="flex items-center gap-3">
            <div className="p-2.5 bg-purple-500/10 border border-purple-500/30 rounded-lg text-purple-400">
              <Compass className="w-6 h-6" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-xl font-bold text-slate-100">{org?.name || 'Loading Organization...'}</h2>
                <span className="text-xs font-mono text-slate-400">#{selectedClanId}</span>
                {org && (
                  <span className="px-2 py-0.5 rounded text-xs font-semibold bg-purple-500/20 text-purple-300 border border-purple-500/30">
                    {org.type} &bull; {org.alignment}
                  </span>
                )}
              </div>
              <p className="text-xs text-slate-400 mt-0.5">
                Founded Year {org?.foundingYear ?? 0} &bull; Status: <span className="text-slate-200 font-semibold">{org?.status}</span>
                {org?.leaderName && (
                  <>
                    {' '}&bull; Current Leader:{' '}
                    <button
                      onClick={() => org.leaderId && openPersonProfile(parseInt(org.leaderId, 10))}
                      className="text-amber-400 hover:underline font-semibold"
                    >
                      {org.leaderName}
                    </button>
                  </>
                )}
              </p>
            </div>
          </div>

          <button
            onClick={closeClanProfile}
            className="p-1 rounded-lg hover:bg-slate-700 text-slate-400 hover:text-slate-200 transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Body (Top stats + Diplomacy Graph) */}
        <div className="flex-1 flex flex-col md:flex-row overflow-hidden">
          {/* Left Panel: Statistics & Heirlooms */}
          <div className="w-full md:w-80 border-r border-slate-800 p-4 overflow-y-auto space-y-4 bg-slate-900/60 text-xs">
            {/* Quick Metrics */}
            <div className="grid grid-cols-2 gap-2">
              <div className="p-2.5 bg-slate-800/50 rounded-lg border border-slate-700/50">
                <span className="text-slate-400 block text-[10px]">Active Members</span>
                <span className="font-bold text-slate-100 text-base">{org?.memberCount ?? 0}</span>
              </div>
              <div className="p-2.5 bg-slate-800/50 rounded-lg border border-slate-700/50">
                <span className="text-slate-400 block text-[10px]">Influence / Rep</span>
                <span className="font-bold text-amber-400 text-base">{org?.reputation.toFixed(1) ?? '0.0'}</span>
              </div>
              <div className="p-2.5 bg-slate-800/50 rounded-lg border border-slate-700/50">
                <span className="text-slate-400 block text-[10px]">Battles Won</span>
                <span className="font-bold text-emerald-400 text-base">
                  {org?.battlesWon ?? 0} / {org?.battlesTotal ?? 0}
                </span>
              </div>
              <div className="p-2.5 bg-slate-800/50 rounded-lg border border-slate-700/50">
                <span className="text-slate-400 block text-[10px]">Casualties</span>
                <span className="font-bold text-rose-400 text-base">{org?.memberCasualties ?? 0}</span>
              </div>
            </div>

            {/* Clan Treasury / Vault Note */}
            <div className="p-3 bg-amber-950/20 border border-amber-800/30 rounded-lg space-y-1">
              <div className="font-semibold text-amber-300 flex items-center gap-1.5 text-xs">
                <Coins className="w-3.5 h-3.5" /> Collective Clan Treasury
              </div>
              <p className="text-[11px] text-slate-300 leading-relaxed">
                Organizational resources and vault equipment are strictly segregated from individual citizen personal wealth.
              </p>
            </div>

            {/* Heirlooms Section */}
            <div className="space-y-2">
              <div className="flex items-center justify-between font-semibold text-slate-200">
                <span className="flex items-center gap-1.5">
                  <Award className="w-3.5 h-3.5 text-amber-400" />
                  Registered Clan Heirlooms ({org?.heirloomNames.length ?? 0})
                </span>
              </div>
              {org?.heirloomNames.length === 0 ? (
                <p className="text-[11px] text-slate-500 italic p-2.5 bg-slate-800/30 rounded">
                  No ancestral heirlooms currently registered to this clan.
                </p>
              ) : (
                <div className="space-y-1.5">
                  {org?.heirloomNames.map((name, idx) => (
                    <div
                      key={idx}
                      className="p-2 bg-slate-800/60 rounded border border-slate-700/60 flex items-center justify-between"
                    >
                      <span className="font-medium text-slate-200 truncate">{name}</span>
                      <Award className="w-3.5 h-3.5 text-amber-400 shrink-0" />
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Active Feuds */}
            {org?.activeFeudIds && org.activeFeudIds.length > 0 && (
              <div className="p-3 bg-rose-950/30 border border-rose-800/40 rounded-lg space-y-1.5">
                <div className="font-semibold text-rose-300 flex items-center gap-1.5">
                  <AlertTriangle className="w-3.5 h-3.5 text-rose-400" /> Active Blood Feuds ({org.activeFeudIds.length})
                </div>
                <div className="flex flex-wrap gap-1">
                  {org.activeFeudIds.map((feudId) => (
                    <span key={feudId} className="px-2 py-0.5 rounded bg-rose-900/40 text-rose-200 text-[10px]">
                      {feudId}
                    </span>
                  ))}
                </div>
              </div>
            )}
          </div>

          {/* Right Panel: Inter-Organization Diplomacy Graph */}
          <div className="flex-1 flex flex-col bg-slate-950 relative">
            <div className="p-3 bg-slate-900/80 border-b border-slate-800 flex items-center justify-between text-xs">
              <span className="font-semibold text-slate-200 flex items-center gap-1.5">
                <Network className="w-4 h-4 text-sky-400" /> Inter-Organization Diplomacy Graph
              </span>
              <span className="text-[11px] text-slate-400">Click neighboring organizations to inspect</span>
            </div>

            <div className="flex-1 relative">
              {flowNodes.length === 0 ? (
                <div className="flex items-center justify-center h-full text-slate-500">
                  No diplomatic peer records found for this organization.
                </div>
              ) : (
                <ReactFlow
                  nodes={flowNodes}
                  edges={flowEdges}
                  nodeTypes={nodeTypes}
                  fitView
                  minZoom={0.3}
                  maxZoom={1.5}
                >
                  <Background color="#334155" gap={20} size={1} />
                  <Controls className="!bg-slate-800 !border-slate-700 !text-slate-200" />
                  <MiniMap
                    nodeColor={(n) => ((n.data as any)?.isTarget ? '#f59e0b' : '#38bdf8')}
                    className="!bg-slate-900/90 !border-slate-800"
                  />
                </ReactFlow>
              )}
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="p-3 bg-slate-850 border-t border-slate-800 flex justify-between items-center text-xs text-slate-400">
          <span>Simulation single source of truth &bull; Immutable snapshot records</span>
          <button
            onClick={closeClanProfile}
            className="px-4 py-1.5 bg-slate-700 hover:bg-slate-600 text-slate-200 rounded-lg text-xs font-medium transition"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};

