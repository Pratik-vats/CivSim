import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { fetchEquipmentDetail } from '../../services/api';
import { useMapStore } from '../../store/useMapStore';
import { Shield, Sparkles, X, Hammer, Award } from 'lucide-react';

export const EquipmentDetailModal: React.FC = () => {
  const selectedEquipmentId = useMapStore((s) => s.selectedEquipmentId);
  const closeEquipmentModal = useMapStore((s) => s.closeEquipmentModal);

  const { data: item, isLoading } = useQuery({
    queryKey: ['equipment', selectedEquipmentId],
    queryFn: () => (selectedEquipmentId ? fetchEquipmentDetail(selectedEquipmentId) : null),
    enabled: !!selectedEquipmentId,
  });

  if (!selectedEquipmentId) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 animate-in fade-in duration-200">
      <div className="bg-slate-900 border border-slate-700 rounded-xl shadow-2xl w-full max-w-lg overflow-hidden flex flex-col max-h-[85vh]">
        {/* Header */}
        <div className="p-4 bg-slate-800/80 border-b border-slate-700 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Shield className="w-5 h-5 text-amber-400" />
            <div>
              <h3 className="font-bold text-slate-100 text-lg leading-tight">
                {isLoading ? 'Loading Equipment...' : item?.displayName || 'Equipment Details'}
              </h3>
              <p className="text-xs text-slate-400 font-mono">ID: {selectedEquipmentId}</p>
            </div>
          </div>
          <button
            onClick={closeEquipmentModal}
            className="p-1 rounded-lg hover:bg-slate-700 text-slate-400 hover:text-slate-200 transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Body */}
        <div className="p-5 overflow-y-auto space-y-4 text-sm">
          {isLoading && (
            <div className="py-12 text-center text-slate-400">Loading equipment specifications...</div>
          )}

          {item && (
            <>
              {/* Badges */}
              <div className="flex flex-wrap gap-2">
                <span className="px-2.5 py-0.5 rounded text-xs font-semibold bg-amber-500/20 text-amber-300 border border-amber-500/40">
                  {item.rarity}
                </span>
                <span className="px-2.5 py-0.5 rounded text-xs font-semibold bg-blue-500/20 text-blue-300 border border-blue-500/40">
                  Tier {item.tier}
                </span>
                <span className="px-2.5 py-0.5 rounded text-xs font-semibold bg-purple-500/20 text-purple-300 border border-purple-500/40">
                  {item.slot}
                </span>
                <span className="px-2.5 py-0.5 rounded text-xs font-semibold bg-emerald-500/20 text-emerald-300 border border-emerald-500/40">
                  {item.condition}
                </span>
                {item.isRegisteredHeirloom && (
                  <span className="px-2.5 py-0.5 rounded text-xs font-semibold bg-rose-500/20 text-rose-300 border border-rose-500/40 flex items-center gap-1">
                    <Award className="w-3 h-3" /> Clan Heirloom
                  </span>
                )}
              </div>

              {/* Durability Bar */}
              <div className="p-3 bg-slate-800/60 rounded-lg border border-slate-700/60 space-y-1">
                <div className="flex justify-between text-xs text-slate-300">
                  <span>Durability</span>
                  <span className="font-mono">{item.durability} / {item.maxDurability}</span>
                </div>
                <div className="w-full bg-slate-700 h-2 rounded-full overflow-hidden">
                  <div
                    className="bg-emerald-500 h-full transition-all"
                    style={{ width: `${Math.min(100, (item.durability / Math.max(1, item.maxDurability)) * 100)}%` }}
                  />
                </div>
              </div>

              {/* Item Info */}
              <div className="grid grid-cols-2 gap-3 text-xs">
                <div className="p-3 bg-slate-800/40 rounded-lg border border-slate-700/40">
                  <span className="text-slate-400 block mb-0.5">Category / Type</span>
                  <span className="text-slate-200 font-semibold">{item.category} &bull; {item.equipmentType}</span>
                </div>
                <div className="p-3 bg-slate-800/40 rounded-lg border border-slate-700/40">
                  <span className="text-slate-400 block mb-0.5">Crafted</span>
                  <span className="text-slate-200 font-semibold">
                    Year {item.creationYear} {item.creatorPersonId ? `by #${item.creatorPersonId}` : ''}
                  </span>
                </div>
              </div>

              {/* Enchantments */}
              <div className="space-y-2">
                <div className="flex items-center gap-1.5 text-xs font-semibold text-slate-300">
                  <Sparkles className="w-4 h-4 text-purple-400" />
                  <span>Enchantments ({item.enchantments.length})</span>
                </div>
                {item.enchantments.length === 0 ? (
                  <p className="text-xs text-slate-500 italic p-3 bg-slate-800/20 rounded border border-slate-800">
                    No supernatural enchantments bound.
                  </p>
                ) : (
                  <div className="space-y-1.5">
                    {item.enchantments.map((ench, idx) => (
                      <div
                        key={idx}
                        className="px-3 py-2 bg-purple-950/30 border border-purple-800/40 rounded text-xs text-purple-200 flex items-center justify-between"
                      >
                        <span>{ench}</span>
                        <Sparkles className="w-3.5 h-3.5 text-purple-400" />
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </>
          )}
        </div>

        {/* Footer */}
        <div className="p-4 bg-slate-850 border-t border-slate-700 text-right">
          <button
            onClick={closeEquipmentModal}
            className="px-4 py-1.5 bg-slate-700 hover:bg-slate-600 text-slate-200 rounded-lg text-xs font-medium transition"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};

