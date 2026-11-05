import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { fetchFeudDetail } from '../../services/api';
import { useMapStore } from '../../store/useMapStore';
import {
  X,
  Flame,
  Calendar,
  ShieldAlert,
  Swords,
  Users,
  Shield,
  ExternalLink,
  AlertTriangle,
  History,
} from 'lucide-react';

export const FeudDetailModal: React.FC = () => {
  const selectedFeudId = useMapStore((s) => s.selectedFeudId);
  const closeFeudDetail = useMapStore((s) => s.closeFeudDetail);
  const openClanProfile = useMapStore((s) => s.openClanProfile);
  const openFamilyProfile = useMapStore((s) => s.openFamilyProfile);
  const openBattleDetail = useMapStore((s) => s.openBattleDetail);

  const { data: feud, isLoading } = useQuery({
    queryKey: ['feud-detail', selectedFeudId],
    queryFn: () => (selectedFeudId ? fetchFeudDetail(selectedFeudId) : null),
    enabled: !!selectedFeudId,
  });

  if (!selectedFeudId) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm select-none">
      <div className="w-full max-w-3xl bg-slate-900 border border-slate-800 rounded-2xl shadow-2xl flex flex-col overflow-hidden max-h-[88vh]">
        {/* Header */}
        <div className="p-4 border-b border-slate-800 bg-slate-950 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-xl bg-amber-950 text-amber-400 border border-amber-800/60">
              <Flame className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-base font-bold text-slate-100">Blood Feud Dossier</h2>
                <span className="text-xs font-mono text-slate-400">#{selectedFeudId}</span>
              </div>
              <p className="text-xs text-slate-400">
                {feud?.initiatorPartyId} <span className="text-rose-400">vs</span> {feud?.targetPartyId}
              </p>
            </div>
          </div>

          <button
            onClick={closeFeudDetail}
            className="p-1.5 rounded-lg text-slate-400 hover:text-slate-200 hover:bg-slate-800 transition"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-5 space-y-5 scrollbar-thin">
          {isLoading && (
            <div className="py-20 text-center text-slate-400 text-xs font-mono">
              <div className="w-8 h-8 rounded-full border-2 border-amber-500/20 border-t-amber-400 animate-spin mx-auto mb-3" />
              Retrieving blood feud history and grievances...
            </div>
          )}

          {feud && (
            <>
              {/* Origin Grievance */}
              <div className="p-4 rounded-xl bg-slate-950/70 border border-slate-800 space-y-2">
                <div className="flex items-center justify-between flex-wrap gap-2">
                  <div className="flex items-center gap-2">
                    <ShieldAlert className="w-4 h-4 text-amber-400" />
                    <span className="text-xs font-bold text-slate-300 uppercase tracking-wider">
                      Origin Grievance Incident
                    </span>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase bg-amber-950 text-amber-300 border border-amber-800">
                      Severity: {feud.grievanceSeverity}
                    </span>
                    <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase bg-slate-800 text-slate-300 border border-slate-700">
                      {feud.status}
                    </span>
                  </div>
                </div>

                <blockquote className="text-xs italic text-slate-300 border-l-2 border-amber-500 pl-3 py-1 font-serif leading-relaxed">
                  "{feud.grievanceIncident}"
                </blockquote>

                <div className="text-[11px] font-mono text-slate-500 pt-1">
                  Escalation State: <span className="text-amber-400 font-semibold">{feud.escalationLevel}</span>
                </div>
              </div>

              {/* Involved Clans & Families */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                {/* Clans */}
                <div className="p-3.5 rounded-xl bg-slate-950/50 border border-slate-800">
                  <span className="text-xs font-bold text-slate-400 uppercase tracking-wider flex items-center gap-1.5 mb-2">
                    <Shield className="w-3.5 h-3.5 text-indigo-400" />
                    Involved Clans ({feud.involvedClanIds?.length || 0})
                  </span>
                  <div className="flex flex-wrap gap-2">
                    {feud.involvedClanIds?.map((cId) => (
                      <button
                        key={cId}
                        onClick={() => {
                          closeFeudDetail();
                          openClanProfile(cId);
                        }}
                        className="px-2.5 py-1 rounded bg-slate-900 hover:bg-slate-800 border border-slate-800 text-xs text-indigo-300 font-medium flex items-center gap-1 transition"
                      >
                        <span>Clan #{cId}</span>
                        <ExternalLink className="w-2.5 h-2.5" />
                      </button>
                    ))}
                  </div>
                </div>

                {/* Families */}
                <div className="p-3.5 rounded-xl bg-slate-950/50 border border-slate-800">
                  <span className="text-xs font-bold text-slate-400 uppercase tracking-wider flex items-center gap-1.5 mb-2">
                    <Users className="w-3.5 h-3.5 text-amber-400" />
                    Involved Lineages ({feud.involvedFamilyIds?.length || 0})
                  </span>
                  <div className="flex flex-wrap gap-2">
                    {feud.involvedFamilyIds?.map((fId) => (
                      <button
                        key={fId}
                        onClick={() => {
                          closeFeudDetail();
                          openFamilyProfile(Number(fId));
                        }}
                        className="px-2.5 py-1 rounded bg-slate-900 hover:bg-slate-800 border border-slate-800 text-xs text-amber-300 font-medium flex items-center gap-1 transition"
                      >
                        <span>Family #{fId}</span>
                        <ExternalLink className="w-2.5 h-2.5" />
                      </button>
                    ))}
                  </div>
                </div>
              </div>

              {/* Action History Timeline */}
              <div className="space-y-2">
                <span className="text-xs font-bold text-slate-400 uppercase tracking-wider flex items-center gap-1.5">
                  <History className="w-3.5 h-3.5 text-rose-400" />
                  Chronological Hostile Action Timeline ({feud.actionHistory?.length || 0})
                </span>

                <div className="space-y-2 max-h-64 overflow-y-auto scrollbar-thin">
                  {feud.actionHistory?.map((action, idx) => (
                    <div
                      key={action.actionId || idx}
                      className="p-3 rounded-lg bg-slate-950 border border-slate-800 flex flex-col gap-1.5 text-xs"
                    >
                      <div className="flex items-center justify-between gap-2 flex-wrap">
                        <div className="flex items-center gap-2">
                          <span className="font-mono text-slate-500 text-[10px]">#{action.actionId}</span>
                          <span className="text-slate-300 font-mono">
                            Year {action.year}, Month {action.month}
                          </span>
                          <span className="text-slate-600">•</span>
                          <span className="font-semibold text-slate-200">
                            {action.actorId} ➜ {action.targetId}
                          </span>
                        </div>

                        <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase bg-amber-950 text-amber-300 border border-amber-800">
                          {action.actionType.replace('_', ' ')}
                        </span>
                      </div>

                      <p className="text-slate-300 text-xs leading-relaxed font-sans">{action.description}</p>

                      {action.battleId && (
                        <div className="pt-1 border-t border-slate-800/40 flex justify-end">
                          <button
                            onClick={() => {
                              closeFeudDetail();
                              openBattleDetail(action.battleId!);
                            }}
                            className="px-2.5 py-1 rounded bg-rose-950 hover:bg-rose-900 text-rose-300 border border-rose-800 text-[11px] font-semibold flex items-center gap-1 transition"
                          >
                            <Swords className="w-3 h-3" />
                            <span>Inspect Resulting Battle Replay</span>
                            <ExternalLink className="w-2.5 h-2.5" />
                          </button>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
};

