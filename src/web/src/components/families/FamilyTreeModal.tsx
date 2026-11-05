import React, { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { fetchFamilyDetail, fetchFamilyTree } from '../../services/api';
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
import { X, Users, Skull, Zap, Calendar, Award } from 'lucide-react';

// Custom Node for Family Member in Tree
const FamilyMemberNode: React.FC<NodeProps> = ({ data }) => {
  const openPersonProfile = useMapStore((s) => s.openPersonProfile);
  const p = data as any;

  return (
    <div
      onClick={() => openPersonProfile(p.id)}
      className={`p-3 rounded-xl shadow-lg border cursor-pointer transition hover:scale-105 min-w-[170px] ${
        p.alive
          ? 'bg-slate-900 border-sky-500/50 hover:border-sky-400'
          : 'bg-slate-950 border-rose-900/60 hover:border-rose-700 opacity-90'
      }`}
    >
      <Handle type="target" position={Position.Top} className="!bg-slate-500" />
      <div className="flex items-center justify-between gap-1 mb-1">
        <span className="text-[10px] font-mono text-slate-400">#{p.id}</span>
        <span
          className={`px-1.5 py-0.5 rounded text-[9px] font-semibold ${
            p.alive
              ? 'bg-emerald-500/20 text-emerald-300'
              : 'bg-rose-500/20 text-rose-300 flex items-center gap-0.5'
          }`}
        >
          {p.alive ? `Age ${p.age}` : <><Skull className="w-2.5 h-2.5" /> Died {p.deathYear}</>}
        </span>
      </div>

      <div className="font-bold text-xs text-slate-100 truncate">{p.name}</div>
      <div className="text-[10px] text-slate-400 flex items-center justify-between mt-1">
        <span>Gen {p.generation || 1}</span>
        {p.isAwakened ? (
          <span className="text-purple-300 font-semibold flex items-center gap-0.5">
            <Zap className="w-2.5 h-2.5" /> T{p.tier}
          </span>
        ) : (
          <span className="text-slate-500">Mundane</span>
        )}
      </div>

      {p.spouseName && (
        <div className="text-[9px] text-pink-400/80 mt-1 truncate border-t border-slate-800 pt-1">
          &hearts; {p.spouseName}
        </div>
      )}

      <Handle type="source" position={Position.Bottom} className="!bg-slate-500" />
    </div>
  );
};

const nodeTypes = {
  familyMember: FamilyMemberNode,
};

export const FamilyTreeModal: React.FC = () => {
  const selectedFamilyId = useMapStore((s) => s.selectedFamilyId);
  const closeFamilyProfile = useMapStore((s) => s.closeFamilyProfile);

  const { data: detail } = useQuery({
    queryKey: ['familyDetail', selectedFamilyId],
    queryFn: () => (selectedFamilyId !== null ? fetchFamilyDetail(selectedFamilyId) : null),
    enabled: selectedFamilyId !== null,
  });

  const { data: treeData, isLoading } = useQuery({
    queryKey: ['familyTree', selectedFamilyId],
    queryFn: () => (selectedFamilyId !== null ? fetchFamilyTree(selectedFamilyId) : null),
    enabled: selectedFamilyId !== null,
  });

  // Transform backend graph DTO into React Flow nodes and edges
  const flowNodes: Node[] = useMemo(() => {
    if (!treeData?.nodes) return [];
    return treeData.nodes.map((n) => ({
      id: n.id,
      type: 'familyMember',
      position: { x: (n.data?.x as number) || 0, y: (n.data?.y as number) || 0 },
      data: n.data,
    }));
  }, [treeData]);

  const flowEdges: Edge[] = useMemo(() => {
    if (!treeData?.edges) return [];
    return treeData.edges.map((e) => ({
      id: e.id,
      source: e.source,
      target: e.target,
      label: e.label,
      animated: e.type === 'marriage',
      style: {
        stroke: e.type === 'marriage' ? '#f43f5e' : '#38bdf8',
        strokeWidth: 2,
      },
      labelStyle: { fill: '#94a3b8', fontSize: 10 },
    }));
  }, [treeData]);

  if (selectedFamilyId === null) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4 animate-in fade-in duration-200">
      <div className="bg-slate-900 border border-slate-700 rounded-xl shadow-2xl w-full max-w-6xl h-[90vh] flex flex-col overflow-hidden">
        {/* Header */}
        <div className="p-4 bg-slate-800/90 border-b border-slate-700 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="p-2.5 bg-amber-500/10 border border-amber-500/30 rounded-lg text-amber-400">
              <Users className="w-6 h-6" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-xl font-bold text-slate-100">
                  {detail?.summary.surname ? `The ${detail.summary.surname} Dynasty` : 'Family Dynasty Tree'}
                </h2>
                <span className="text-xs font-mono text-slate-400">#FAM-{selectedFamilyId}</span>
              </div>
              <p className="text-xs text-slate-400 flex items-center gap-2 mt-0.5">
                <span>Founded Year {detail?.summary.foundedYear ?? 0}</span>
                <span>&bull;</span>
                <span className="text-emerald-400 font-semibold">{detail?.summary.livingCount} Living</span>
                <span>&bull;</span>
                <span className="text-rose-400 font-semibold">{detail?.summary.deceasedCount} Deceased</span>
                <span>&bull;</span>
                <span>{detail?.summary.totalLineageCount} Total Lineage</span>
                <span>&bull;</span>
                <span>{detail?.summary.generationsCount} Generations</span>
              </p>
            </div>
          </div>

          <button
            onClick={closeFamilyProfile}
            className="p-1 rounded-lg hover:bg-slate-700 text-slate-400 hover:text-slate-200 transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Tree Canvas */}
        <div className="flex-1 bg-slate-950 relative">
          {isLoading ? (
            <div className="flex items-center justify-center h-full text-slate-400">
              Generating genealogical lineage tree...
            </div>
          ) : flowNodes.length === 0 ? (
            <div className="flex items-center justify-center h-full text-slate-500">
              No lineage tree data available for this family.
            </div>
          ) : (
            <ReactFlow
              nodes={flowNodes}
              edges={flowEdges}
              nodeTypes={nodeTypes}
              fitView
              minZoom={0.2}
              maxZoom={2.0}
            >
              <Background color="#334155" gap={20} size={1} />
              <Controls className="!bg-slate-800 !border-slate-700 !text-slate-200" />
              <MiniMap
                nodeColor={(n) => ((n.data as any)?.alive ? '#38bdf8' : '#f43f5e')}
                className="!bg-slate-900/90 !border-slate-800"
              />
            </ReactFlow>
          )}

          {/* Quick Legend */}
          <div className="absolute top-3 left-3 bg-slate-900/90 border border-slate-800 rounded-lg p-2.5 text-[11px] text-slate-300 space-y-1.5 shadow backdrop-blur pointer-events-none">
            <div className="font-semibold text-slate-200 mb-1">Genealogy Legend</div>
            <div className="flex items-center gap-2">
              <span className="w-3 h-3 rounded bg-sky-500/20 border border-sky-400" />
              <span>Living Citizen</span>
            </div>
            <div className="flex items-center gap-2">
              <span className="w-3 h-3 rounded bg-rose-500/20 border border-rose-500" />
              <span>Deceased Citizen</span>
            </div>
            <div className="flex items-center gap-2">
              <span className="w-4 h-0.5 bg-sky-400" />
              <span>Parent-Child Edge</span>
            </div>
            <div className="flex items-center gap-2">
              <span className="w-4 h-0.5 bg-pink-500" />
              <span>Marriage Union</span>
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="p-3 bg-slate-850 border-t border-slate-800 flex justify-between items-center text-xs text-slate-400">
          <span>Click any character node to inspect their complete profile and attributes.</span>
          <button
            onClick={closeFamilyProfile}
            className="px-4 py-1.5 bg-slate-700 hover:bg-slate-600 text-slate-200 rounded-lg text-xs font-medium transition"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};

