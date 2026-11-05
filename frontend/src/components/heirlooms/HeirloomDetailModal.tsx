import React, { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { fetchHeirloomDetail, fetchHeirloomSuccessionGraph } from '../../services/api';
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
  Award,
  Shield,
  Sparkles,
  User,
  History,
  Compass,
  AlertTriangle,
  Swords,
  Scroll,
} from 'lucide-react';

const BearerGraphNode: React.FC<NodeProps> = ({ data }) => {
  const openPersonProfile = useMapStore((s) => s.openPersonProfile);
  const b = data as any;

  return (
    <div
      onClick={() => b.id && openPersonProfile(parseInt(b.id, 10))}
      className="p-3 rounded-xl bg-slate-900 border border-amber-500/50 hover:border-amber-400 shadow-lg cursor-pointer transition hover:scale-105 min-w-[170px]"
    >
      <Handle type="target" position={Position.Top} className="!bg-slate-500" />
      <div className="flex items-center justify-between text-[10px] text-slate-400 mb-1">
        <span>Bearer</span>
        <span className="font-mono">#{b.id}</span>
      </div>
      <div className="font-bold text-xs text-slate-100 truncate">{b.name}</div>
      <div className="text-[10px] text-amber-300 mt-1">{b.tenure}</div>
      {b.reason && <div className="text-[9px] text-slate-400 italic mt-0.5">{b.reason}</div>}
      <Handle type="source" position={Position.Bottom} className="!bg-slate-500" />
    </div>
  );
};

const nodeTypes = {
  bearerNode: BearerGraphNode,
};

export const HeirloomDetailModal: React.FC = () => {
  const selectedHeirloomId = useMapStore((s) => s.selectedHeirloomId);
  const closeHeirloomProfile = useMapStore((s) => s.closeHeirloomProfile);
  const openPersonProfile = useMapStore((s) => s.openPersonProfile);
  const openClanProfile = useMapStore((s) => s.openClanProfile);

  const { data: heirloom, isLoading } = useQuery({
    queryKey: ['heirloomDetail', selectedHeirloomId],
    queryFn: () => (selectedHeirloomId ? fetchHeirloomDetail(selectedHeirloomId) : null),
    enabled: !!selectedHeirloomId,
  });

  const { data: successionGraph } = useQuery({
    queryKey: ['heirloomGraph', selectedHeirloomId],
    queryFn: () => (selectedHeirloomId ? fetchHeirloomSuccessionGraph(selectedHeirloomId) : null),
    enabled: !!selectedHeirloomId,
  });

  const flowNodes: Node[] = useMemo(() => {
    if (!successionGraph?.nodes) return [];
    return successionGraph.nodes.map((n) => ({
      id: n.id,
      type: 'bearerNode',
      position: { x: (n.data?.x as number) || 200, y: (n.data?.y as number) || 100 },
      data: n.data,
    }));
  }, [successionGraph]);

  const flowEdges: Edge[] = useMemo(() => {
    if (!successionGraph?.edges) return [];
    return successionGraph.edges.map((e) => ({
      id: e.id,
      source: e.source,
      target: e.target,
      label: e.label,
      animated: true,
      style: { stroke: '#f59e0b', strokeWidth: 2 },
      labelStyle: { fill: '#fcd34d', fontSize: 10 },
    }));
  }, [successionGraph]);

  if (!selectedHeirloomId) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4 animate-in fade-in duration-200">
      <div className="bg-slate-900 border border-slate-700 rounded-xl shadow-2xl w-full max-w-5xl h-[88vh] flex flex-col overflow-hidden">
        {/* Header */}
        <div className="p-4 bg-slate-800/90 border-b border-slate-700 flex items-start justify-between">
          <div className="flex items-center gap-3">
            <div className="p-2.5 bg-amber-500/10 border border-amber-500/30 rounded-lg text-amber-400">
              <Award className="w-6 h-6" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-xl font-bold text-slate-100">
                  {heirloom?.artifactTitle || 'Loading Heirloom...'}
                </h2>
                <span className="text-xs font-mono text-slate-400">#{selectedHeirloomId}</span>
                {heirloom && (
                  <span className="px-2 py-0.5 rounded text-xs font-semibold bg-amber-500/20 text-amber-300 border border-amber-500/30">
                    {heirloom.rarity} &bull; Tier {heirloom.tier}
                  </span>
                )}
              </div>
              <p className="text-xs text-slate-400 mt-0.5">
                Physical Equipment ID: <span className="font-mono text-slate-300">{heirloom?.equipmentId}</span> &bull;{' '}
                Legal Owning Clan:{' '}
                <button
                  onClick={() => heirloom?.owningClanId && openClanProfile(heirloom.owningClanId)}
                  className="text-amber-400 hover:underline font-semibold"
                >
                  {heirloom?.owningClanName || heirloom?.owningClanId}
                </button>
              </p>
            </div>
          </div>

          <button
            onClick={closeHeirloomProfile}
            className="p-1 rounded-lg hover:bg-slate-700 text-slate-400 hover:text-slate-200 transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="flex-1 flex flex-col md:flex-row overflow-hidden">
          {/* Left Panel: Stats, Custody & Chronicle */}
          <div className="w-full md:w-80 border-r border-slate-800 p-4 overflow-y-auto space-y-4 bg-slate-900/60 text-xs">
            {/* Custody Status Badge */}
            <div className="p-3 bg-slate-800/60 rounded-lg border border-slate-700/60 space-y-1.5">
              <span className="text-[10px] text-slate-400 uppercase tracking-wider block">Custody Status</span>
              <div className="flex items-center justify-between font-bold text-sm">
                <span
                  className={
                    heirloom?.custodyStatus === 'IN_CLAN_VAULT'
                      ? 'text-sky-400'
                      : heirloom?.custodyStatus === 'BEARER_HELD'
                      ? 'text-emerald-400'
                      : 'text-rose-400'
                  }
                >
                  {heirloom?.custodyStatus.replace('_', ' ')}
                </span>
                <Shield className="w-4 h-4 text-slate-400" />
              </div>
              {heirloom?.currentBearerName && (
                <div className="text-[11px] text-slate-300 pt-1 border-t border-slate-700/60">
                  Current Bearer:{' '}
                  <button
                    onClick={() =>
                      heirloom.currentBearerId && openPersonProfile(parseInt(heirloom.currentBearerId, 10))
                    }
                    className="text-sky-400 hover:underline font-semibold"
                  >
                    {heirloom.currentBearerName}
                  </button>
                </div>
              )}
            </div>

            {/* Durability */}
            {heirloom && (
              <div className="p-3 bg-slate-800/40 rounded-lg border border-slate-700/40 space-y-1">
                <div className="flex justify-between text-[11px] text-slate-300">
                  <span>Condition: <strong className="text-emerald-400">{heirloom.condition}</strong></span>
                  <span className="font-mono">{heirloom.durability} / {heirloom.maxDurability}</span>
                </div>
                <div className="w-full bg-slate-700 h-1.5 rounded-full overflow-hidden">
                  <div
                    className="bg-emerald-500 h-full"
                    style={{ width: `${Math.min(100, (heirloom.durability / Math.max(1, heirloom.maxDurability)) * 100)}%` }}
                  />
                </div>
              </div>
            )}

            {/* Succession Policy */}
            <div className="p-3 bg-slate-800/40 rounded-lg border border-slate-700/40">
              <span className="text-[10px] text-slate-400 block mb-0.5">Succession Policy</span>
              <span className="font-semibold text-slate-200">{heirloom?.successionPolicy}</span>
              <span className="text-[10px] text-slate-400 block mt-1">
                Registered: Year {heirloom?.registrationYear}, Month {heirloom?.registrationMonth}
              </span>
            </div>

            {/* Enchantments */}
            <div className="space-y-1.5">
              <span className="font-semibold text-slate-200 flex items-center gap-1">
                <Sparkles className="w-3.5 h-3.5 text-purple-400" /> Bound Enchantments
              </span>
              {heirloom?.enchantments.length === 0 ? (
                <p className="text-[11px] text-slate-500 italic">No supernatural enchantments.</p>
              ) : (
                heirloom?.enchantments.map((ench, idx) => (
                  <div key={idx} className="p-2 bg-purple-950/20 border border-purple-800/30 rounded text-purple-200 text-[11px]">
                    {ench}
                  </div>
                ))
              )}
            </div>

            {/* Historical Bearer Records */}
            <div className="space-y-1.5">
              <span className="font-semibold text-slate-200 flex items-center gap-1">
                <History className="w-3.5 h-3.5 text-amber-400" /> Bearer Chronicle ({heirloom?.pastBearers.length ?? 0})
              </span>
              <div className="space-y-1">
                {heirloom?.pastBearers.map((b, idx) => (
                  <div key={idx} className="p-2 bg-slate-800/50 rounded border border-slate-700/50 text-[11px]">
                    <div className="flex items-center justify-between font-semibold text-slate-200">
                      <span>{b.bearerName}</span>
                      <span className="text-amber-400 text-[10px]">{b.tenureInfo}</span>
                    </div>
                    {b.successionReason && (
                      <p className="text-[10px] text-slate-400 mt-0.5">{b.successionReason}</p>
                    )}
                  </div>
                ))}
              </div>
            </div>
          </div>

          {/* Right Panel: Succession Lineage Graph */}
          <div className="flex-1 flex flex-col bg-slate-950 relative">
            <div className="p-3 bg-slate-900/80 border-b border-slate-800 flex items-center justify-between text-xs">
              <span className="font-semibold text-slate-200 flex items-center gap-1.5">
                <Scroll className="w-4 h-4 text-amber-400" /> Heirloom Succession Provenance Graph
              </span>
              <span className="text-[11px] text-slate-400">Chronological custody transitions</span>
            </div>

            <div className="flex-1 relative">
              {flowNodes.length === 0 ? (
                <div className="flex items-center justify-center h-full text-slate-500">
                  No succession chronicle nodes recorded.
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
                    nodeColor="#f59e0b"
                    className="!bg-slate-900/90 !border-slate-800"
                  />
                </ReactFlow>
              )}
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="p-3 bg-slate-850 border-t border-slate-800 flex justify-between items-center text-xs text-slate-400">
          <span>Physical equipment ID and legal ownership strictly tracked across generations.</span>
          <button
            onClick={closeHeirloomProfile}
            className="px-4 py-1.5 bg-slate-700 hover:bg-slate-600 text-slate-200 rounded-lg text-xs font-medium transition"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};

