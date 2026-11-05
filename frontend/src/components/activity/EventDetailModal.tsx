import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { fetchActivityEvent } from '../../services/api';
import { useMapStore } from '../../store/useMapStore';
import {
  X,
  Activity,
  Calendar,
  MapPin,
  Shield,
  Swords,
  Skull,
  Compass,
  Flame,
  Sparkles,
  Building,
  ExternalLink,
  Navigation,
} from 'lucide-react';

export const EventDetailModal: React.FC = () => {
  const selectedEventId = useMapStore((s) => s.selectedActivityEventId);
  const closeActivityEvent = useMapStore((s) => s.closeActivityEvent);

  const openPersonProfile = useMapStore((s) => s.openPersonProfile);
  const openClanProfile = useMapStore((s) => s.openClanProfile);
  const openBattleDetail = useMapStore((s) => s.openBattleDetail);
  const openTideDetail = useMapStore((s) => s.openTideDetail);
  const openExpeditionDetail = useMapStore((s) => s.openExpeditionDetail);
  const openFeudDetail = useMapStore((s) => s.openFeudDetail);
  const openAnomalyDetail = useMapStore((s) => s.openAnomalyDetail);
  const navigateToEntityOnMap = useMapStore((s) => s.navigateToEntityOnMap);

  const { data: event, isLoading } = useQuery({
    queryKey: ['activity-event-detail', selectedEventId],
    queryFn: () => (selectedEventId ? fetchActivityEvent(selectedEventId) : null),
    enabled: !!selectedEventId,
  });

  if (!selectedEventId) return null;

  const handleEntityJump = (type: string, id: string, posX?: number | null, posY?: number | null) => {
    closeActivityEvent();
    const upperType = type.toUpperCase();
    if (upperType === 'SETTLEMENT' || upperType === 'VILLAGE') {
      navigateToEntityOnMap('SETTLEMENT', Number(id) || id, posX ?? undefined, posY ?? undefined);
    } else if (upperType === 'LAIR') {
      navigateToEntityOnMap('LAIR', id, posX ?? undefined, posY ?? undefined);
    } else if (upperType === 'ANOMALY') {
      openAnomalyDetail(id);
    } else if (upperType === 'PERSON') {
      openPersonProfile(Number(id));
    } else if (upperType === 'ORGANIZATION' || upperType === 'CLAN') {
      openClanProfile(id);
    } else if (upperType === 'BATTLE') {
      openBattleDetail(id);
    } else if (upperType === 'MONSTER_TIDE') {
      openTideDetail(id);
    } else if (upperType === 'EXPEDITION') {
      openExpeditionDetail(id);
    } else if (upperType === 'FEUD') {
      openFeudDetail(id);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-sm select-none">
      <div className="w-full max-w-xl bg-slate-900 border border-slate-800 rounded-2xl shadow-2xl flex flex-col overflow-hidden max-h-[85vh]">
        {/* Header */}
        <div className="p-4 border-b border-slate-800 bg-slate-950 flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="p-1.5 rounded-lg bg-cyan-950 text-cyan-400 border border-cyan-800/60">
              <Activity className="w-4 h-4" />
            </div>
            <div>
              <h2 className="text-sm font-bold text-slate-100">Event Dossier</h2>
              <p className="text-[10px] font-mono text-slate-400">ID: {selectedEventId}</p>
            </div>
          </div>
          <button
            onClick={closeActivityEvent}
            className="p-1.5 rounded-lg text-slate-400 hover:text-slate-200 hover:bg-slate-800 transition"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-5 space-y-4">
          {isLoading && (
            <div className="py-12 text-center text-slate-400 text-xs">
              <div className="w-6 h-6 rounded-full border-2 border-cyan-500/20 border-t-cyan-400 animate-spin mx-auto mb-2" />
              Loading event record...
            </div>
          )}

          {event && (
            <>
              {/* Title & Metadata */}
              <div>
                <div className="flex items-center gap-2 mb-1.5">
                  <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-cyan-950 text-cyan-300 border border-cyan-800/60 uppercase">
                    {event.category}
                  </span>
                  <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-slate-800 text-slate-300 border border-slate-700 uppercase">
                    {event.severity}
                  </span>
                  {event.subtype && (
                    <span className="text-[10px] font-mono text-slate-400">({event.subtype})</span>
                  )}
                </div>
                <h3 className="text-base font-bold text-slate-100 leading-snug">{event.title}</h3>
                <div className="flex items-center gap-3 text-xs text-slate-400 font-mono mt-1">
                  <span className="flex items-center gap-1">
                    <Calendar className="w-3.5 h-3.5 text-slate-500" />
                    Year {event.year}, Month {event.month}
                  </span>
                  <span>Total Months: {event.totalMonths}</span>
                  <span>Sequence #{event.sequence}</span>
                </div>
              </div>

              {/* Narrative Summary */}
              <div className="p-3.5 rounded-xl bg-slate-950/70 border border-slate-800 text-xs text-slate-200 leading-relaxed">
                {event.summary}
              </div>

              {/* Spatial Location */}
              {event.location && (
                <div className="p-3 rounded-xl bg-slate-950/50 border border-slate-800 flex items-center justify-between">
                  <div className="flex items-center gap-2 text-xs">
                    <MapPin className="w-4 h-4 text-rose-400" />
                    <div>
                      <span className="text-slate-400">Location:</span>{' '}
                      <span className="text-slate-200 font-medium">{event.location}</span>
                      {event.posX !== null && event.posY !== null && (
                        <span className="text-slate-500 font-mono ml-2">
                          ({event.posX}, {event.posY})
                        </span>
                      )}
                    </div>
                  </div>
                  {event.posX !== null && event.posY !== null && (
                    <button
                      onClick={() =>
                        handleEntityJump(event.primaryEntityType, event.primaryEntityId, event.posX, event.posY)
                      }
                      className="px-2.5 py-1 rounded-lg bg-cyan-950 hover:bg-cyan-900 text-cyan-300 text-xs font-medium border border-cyan-800/60 flex items-center gap-1.5 transition"
                    >
                      <Navigation className="w-3 h-3" />
                      <span>Locate on Map</span>
                    </button>
                  )}
                </div>
              )}

              {/* Primary Entity */}
              <div className="space-y-1.5">
                <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">Primary Entity</span>
                <div className="p-3 rounded-xl bg-slate-950/50 border border-slate-800 flex items-center justify-between">
                  <div>
                    <span className="text-[10px] font-mono uppercase text-slate-500">{event.primaryEntityType}</span>
                    <div className="text-xs font-bold text-slate-200">{event.primaryEntityId}</div>
                  </div>
                  <button
                    onClick={() => handleEntityJump(event.primaryEntityType, event.primaryEntityId, event.posX, event.posY)}
                    className="px-2.5 py-1 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs flex items-center gap-1.5 transition"
                  >
                    <span>Inspect</span>
                    <ExternalLink className="w-3 h-3 text-slate-400" />
                  </button>
                </div>
              </div>

              {/* Related Entities */}
              {event.relatedEntityIds && event.relatedEntityIds.length > 0 && (
                <div className="space-y-1.5">
                  <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">
                    Related Entities ({event.relatedEntityIds.length})
                  </span>
                  <div className="flex flex-wrap gap-2">
                    {event.relatedEntityIds.map((relId) => (
                      <button
                        key={relId}
                        onClick={() => handleEntityJump('ENTITY', relId)}
                        className="px-2.5 py-1 rounded-lg bg-slate-950 hover:bg-slate-800 border border-slate-800 text-xs text-slate-300 font-mono flex items-center gap-1 transition"
                      >
                        <span>{relId}</span>
                        <ExternalLink className="w-2.5 h-2.5 text-slate-500" />
                      </button>
                    ))}
                  </div>
                </div>
              )}

              {/* Data Availability Note */}
              <div className="pt-2 border-t border-slate-800 flex items-center justify-between text-[11px] font-mono text-slate-500">
                <span>Data Completeness: {event.dataAvailability}</span>
                <span>World ID: {event.worldId}</span>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
};

