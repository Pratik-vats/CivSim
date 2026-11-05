import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { fetchPersonDetail } from '../../services/api';
import { useMapStore } from '../../store/useMapStore';
import {
  X,
  User,
  Shield,
  Zap,
  Sword,
  Heart,
  Flame,
  Award,
  Users,
  Compass,
  Coins,
  Skull,
  Activity,
  Layers,
} from 'lucide-react';
import {
  Radar,
  RadarChart,
  PolarGrid,
  PolarAngleAxis,
  ResponsiveContainer,
} from 'recharts';

export const PersonProfileModal: React.FC = () => {
  const selectedPersonId = useMapStore((s) => s.selectedPersonId);
  const closePersonProfile = useMapStore((s) => s.closePersonProfile);
  const openPersonProfile = useMapStore((s) => s.openPersonProfile);
  const openFamilyProfile = useMapStore((s) => s.openFamilyProfile);
  const openClanProfile = useMapStore((s) => s.openClanProfile);
  const openEquipmentModal = useMapStore((s) => s.openEquipmentModal);

  const [activeTab, setActiveTab] = useState<'ATTRIBUTES' | 'COMBAT' | 'EQUIPMENT' | 'LINEAGE'>('ATTRIBUTES');

  const { data: person, isLoading } = useQuery({
    queryKey: ['personDetail', selectedPersonId],
    queryFn: () => (selectedPersonId !== null ? fetchPersonDetail(selectedPersonId) : null),
    enabled: selectedPersonId !== null,
  });

  if (selectedPersonId === null) return null;

  // Prepare radar chart data for the 10 attributes
  const radarData = person?.attributes
    ? Object.entries(person.attributes).map(([key, attr]) => ({
        subject: key.replace('AETHER_', 'A_').substring(0, 7),
        fullName: key,
        effective: attr.effective,
        baseline: attr.baseline,
      }))
    : [];

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 animate-in fade-in duration-200">
      <div className="bg-slate-900 border border-slate-700 rounded-xl shadow-2xl w-full max-w-4xl max-h-[90vh] flex flex-col overflow-hidden">
        {/* Header */}
        <div className="p-4 bg-slate-800/90 border-b border-slate-700 flex items-start justify-between">
          <div className="flex items-center gap-3">
            <div className={`w-12 h-12 rounded-lg flex items-center justify-center font-bold text-lg border ${
              person?.alive
                ? 'bg-blue-950/60 border-blue-500/40 text-blue-300'
                : 'bg-rose-950/60 border-rose-500/40 text-rose-300'
            }`}>
              {person ? person.name.charAt(0) : '?'}
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-xl font-bold text-slate-100">{person?.name || 'Loading Character...'}</h2>
                <span className="text-xs font-mono text-slate-400">#{selectedPersonId}</span>
                {person && (
                  <span className={`px-2 py-0.5 rounded text-xs font-semibold ${
                    person.alive
                      ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30'
                      : 'bg-rose-500/20 text-rose-300 border border-rose-500/30 flex items-center gap-1'
                  }`}>
                    {person.alive ? 'Living' : <><Skull className="w-3 h-3" /> Deceased (Age {person.ageAtDeath || person.age})</>}
                  </span>
                )}
              </div>
              <p className="text-xs text-slate-400 mt-0.5">
                {person?.occupation} &bull; {person?.villageName ? `Settlement: ${person.villageName}` : 'No fixed abode'}
                {person?.familySurname && (
                  <>
                    {' '}&bull; Family:{' '}
                    <button
                      onClick={() => person.familyId && openFamilyProfile(person.familyId)}
                      className="text-amber-400 hover:underline"
                    >
                      {person.familySurname}
                    </button>
                  </>
                )}
              </p>
            </div>
          </div>

          <button
            onClick={closePersonProfile}
            className="p-1 rounded-lg hover:bg-slate-700 text-slate-400 hover:text-slate-200 transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Tab Controls */}
        <div className="flex border-b border-slate-700 bg-slate-800/40 text-xs font-medium px-4">
          <button
            onClick={() => setActiveTab('ATTRIBUTES')}
            className={`py-2.5 px-3 border-b-2 flex items-center gap-1.5 transition ${
              activeTab === 'ATTRIBUTES'
                ? 'border-amber-400 text-amber-300 bg-slate-800/60'
                : 'border-transparent text-slate-400 hover:text-slate-200'
            }`}
          >
            <Activity className="w-4 h-4" /> Attributes & Progression
          </button>
          <button
            onClick={() => setActiveTab('COMBAT')}
            className={`py-2.5 px-3 border-b-2 flex items-center gap-1.5 transition ${
              activeTab === 'COMBAT'
                ? 'border-amber-400 text-amber-300 bg-slate-800/60'
                : 'border-transparent text-slate-400 hover:text-slate-200'
            }`}
          >
            <Sword className="w-4 h-4" /> Combat Capability
          </button>
          <button
            onClick={() => setActiveTab('EQUIPMENT')}
            className={`py-2.5 px-3 border-b-2 flex items-center gap-1.5 transition ${
              activeTab === 'EQUIPMENT'
                ? 'border-amber-400 text-amber-300 bg-slate-800/60'
                : 'border-transparent text-slate-400 hover:text-slate-200'
            }`}
          >
            <Shield className="w-4 h-4" /> Equipment ({person ? Object.keys(person.equippedItems || {}).length : 0})
          </button>
          <button
            onClick={() => setActiveTab('LINEAGE')}
            className={`py-2.5 px-3 border-b-2 flex items-center gap-1.5 transition ${
              activeTab === 'LINEAGE'
                ? 'border-amber-400 text-amber-300 bg-slate-800/60'
                : 'border-transparent text-slate-400 hover:text-slate-200'
            }`}
          >
            <Users className="w-4 h-4" /> Lineage & Clans
          </button>
        </div>

        {/* Content */}
        <div className="p-6 overflow-y-auto space-y-6">
          {isLoading && (
            <div className="py-20 text-center text-slate-400">Loading complete citizen dossier...</div>
          )}

          {person && activeTab === 'ATTRIBUTES' && (
            <div className="space-y-6">
              {/* Supernatural Progression Summary */}
              <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
                <div className="p-3 bg-slate-800/50 rounded-lg border border-slate-700/60">
                  <span className="text-xs text-slate-400 block mb-1">Supernatural Status</span>
                  <div className="flex items-center gap-1.5">
                    <Zap className={`w-4 h-4 ${person.isAwakened ? 'text-amber-400' : 'text-slate-500'}`} />
                    <span className="font-bold text-slate-200">
                      {person.isAwakened ? `Tier ${person.tier} ${person.supernaturalClass}` : 'Mundane'}
                    </span>
                  </div>
                  {person.isAwakened && (
                    <span className="text-xs text-amber-400/80 block mt-0.5">
                      Max Ceiling: Tier {person.maxTier} &bull; {person.rarity}
                    </span>
                  )}
                </div>

                <div className="p-3 bg-slate-800/50 rounded-lg border border-slate-700/60">
                  <span className="text-xs text-slate-400 block mb-1">Aetheric Mastery</span>
                  <div className="flex items-center justify-between">
                    <span className="font-mono font-bold text-slate-200">{person.mastery.toFixed(1)} / 100</span>
                    <span className="text-xs text-purple-300 font-semibold">{person.aspect}</span>
                  </div>
                  <div className="w-full bg-slate-700 h-1.5 rounded-full mt-2 overflow-hidden">
                    <div className="bg-purple-500 h-full" style={{ width: `${Math.min(100, person.mastery)}%` }} />
                  </div>
                </div>

                <div className="p-3 bg-slate-800/50 rounded-lg border border-slate-700/60">
                  <span className="text-xs text-slate-400 block mb-1">Derived Combat Power</span>
                  <div className="flex items-center gap-1.5">
                    <Flame className="w-4 h-4 text-rose-400" />
                    <span className="font-bold text-slate-200 font-mono text-base">
                      {person.derivedPower.toFixed(1)}
                    </span>
                  </div>
                  <span className="text-xs text-slate-400 block mt-0.5">
                    Breakthroughs: {person.breakthroughSuccesses}/{person.breakthroughAttempts}
                  </span>
                </div>

                <div className="p-3 bg-slate-800/50 rounded-lg border border-slate-700/60">
                  <span className="text-xs text-slate-400 block mb-1">Personal Wealth</span>
                  <div className="flex items-center gap-1.5">
                    <Coins className="w-4 h-4 text-amber-400" />
                    <span className="font-bold text-slate-200 font-mono text-base">
                      {person.wealth.toFixed(1)} Silver
                    </span>
                  </div>
                  <span className="text-xs text-slate-400 block mt-0.5">
                    Bank: {person.bankBalance.toFixed(1)} | Debt: {person.loanDebt.toFixed(1)}
                  </span>
                </div>
              </div>

              {/* Attributes Radar Chart & 5-Layer Breakdown Table */}
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 items-center">
                {/* Radar Chart */}
                <div className="bg-slate-850 p-4 rounded-xl border border-slate-800 flex flex-col items-center">
                  <h4 className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-2">
                    Attribute Profile (Radar)
                  </h4>
                  <div className="w-full h-64">
                    <ResponsiveContainer width="100%" height="100%">
                      <RadarChart data={radarData}>
                        <PolarGrid stroke="#334155" />
                        <PolarAngleAxis dataKey="subject" stroke="#94a3b8" tick={{ fontSize: 10 }} />
                        <Radar
                          name="Effective"
                          dataKey="effective"
                          stroke="#38bdf8"
                          fill="#38bdf8"
                          fillOpacity={0.4}
                        />
                        <Radar
                          name="Baseline"
                          dataKey="baseline"
                          stroke="#64748b"
                          fill="#64748b"
                          fillOpacity={0.2}
                        />
                      </RadarChart>
                    </ResponsiveContainer>
                  </div>
                </div>

                {/* 10 Core Attributes Breakdown Table */}
                <div className="overflow-x-auto">
                  <table className="w-full text-xs text-left text-slate-300">
                    <thead className="bg-slate-800 text-slate-400 uppercase text-[10px] font-semibold">
                      <tr>
                        <th className="py-2 px-2.5">Attribute</th>
                        <th className="py-2 px-2 text-right">Base</th>
                        <th className="py-2 px-2 text-right">Awaken</th>
                        <th className="py-2 px-2 text-right">Dev</th>
                        <th className="py-2 px-2 text-right">Temp</th>
                        <th className="py-2 px-2.5 text-right font-bold text-amber-300">Effective</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-800">
                      {person.attributes &&
                        Object.entries(person.attributes).map(([key, attr]) => (
                          <tr key={key} className="hover:bg-slate-800/40">
                            <td className="py-1.5 px-2.5 font-medium text-slate-200">
                              {key.replace('_', ' ')}
                            </td>
                            <td className="py-1.5 px-2 text-right font-mono text-slate-400">{attr.baseline.toFixed(1)}</td>
                            <td className="py-1.5 px-2 text-right font-mono text-purple-300">
                              {attr.awakening > 0 ? `+${attr.awakening.toFixed(1)}` : '0.0'}
                            </td>
                            <td className="py-1.5 px-2 text-right font-mono text-blue-300">
                              {attr.development > 0 ? `+${attr.development.toFixed(1)}` : '0.0'}
                            </td>
                            <td className="py-1.5 px-2 text-right font-mono text-slate-500">
                              {attr.temporary !== 0 ? attr.temporary.toFixed(1) : '0.0'}
                            </td>
                            <td className="py-1.5 px-2.5 text-right font-mono font-bold text-sky-400">
                              {attr.effective.toFixed(1)}
                            </td>
                          </tr>
                        ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}

          {person && activeTab === 'COMBAT' && (
            <div className="space-y-6">
              {/* Combat Stats Grid */}
              <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3">
                <div className="p-3 bg-slate-800/50 rounded-lg border border-slate-700/60">
                  <span className="text-xs text-slate-400 block">Health Points</span>
                  <div className="flex items-center gap-1.5 mt-1 font-mono font-bold text-slate-200">
                    <Heart className="w-4 h-4 text-rose-500" />
                    <span>{person.combatProfile?.currentHealth.toFixed(0)} / {person.combatProfile?.maxHealth.toFixed(0)}</span>
                  </div>
                </div>
                <div className="p-3 bg-slate-800/50 rounded-lg border border-slate-700/60">
                  <span className="text-xs text-slate-400 block">Physical ATK / DEF</span>
                  <div className="mt-1 font-mono font-bold text-slate-200">
                    {person.combatProfile?.physicalAttack.toFixed(1)} / {person.combatProfile?.physicalDefense.toFixed(1)}
                  </div>
                </div>
                <div className="p-3 bg-slate-800/50 rounded-lg border border-slate-700/60">
                  <span className="text-xs text-slate-400 block">Accuracy / Evasion</span>
                  <div className="mt-1 font-mono font-bold text-slate-200">
                    {person.combatProfile?.accuracy.toFixed(1)} / {person.combatProfile?.evasion.toFixed(1)}
                  </div>
                </div>
                <div className="p-3 bg-slate-800/50 rounded-lg border border-slate-700/60">
                  <span className="text-xs text-slate-400 block">Aether Potency / Barrier</span>
                  <div className="mt-1 font-mono font-bold text-purple-300">
                    {person.combatProfile?.aetherPotency.toFixed(1)} / {person.combatProfile?.barrier.toFixed(1)}
                  </div>
                </div>
              </div>

              {/* Injuries */}
              <div className="p-4 bg-slate-850 rounded-xl border border-slate-800 space-y-2">
                <h4 className="text-xs font-semibold text-slate-300 uppercase tracking-wider flex items-center gap-1.5">
                  <Activity className="w-4 h-4 text-rose-400" />
                  Active Combat Injuries ({person.combatProfile?.injuries.length || 0})
                </h4>
                {person.combatProfile?.injuries.length === 0 ? (
                  <p className="text-xs text-emerald-400 italic">No lingering physical or aetheric injuries.</p>
                ) : (
                  <ul className="space-y-1">
                    {person.combatProfile?.injuries.map((inj, idx) => (
                      <li key={idx} className="text-xs text-rose-300 bg-rose-950/20 px-3 py-1.5 rounded border border-rose-900/30">
                        {inj}
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            </div>
          )}

          {person && activeTab === 'EQUIPMENT' && (
            <div className="space-y-4">
              <h4 className="text-xs font-semibold text-slate-300 uppercase tracking-wider">
                Equipped Arsenal & Relics
              </h4>
              {Object.keys(person.equippedItems || {}).length === 0 ? (
                <div className="py-12 text-center text-slate-500 italic bg-slate-850 rounded-xl border border-slate-800">
                  No equipped weapon or armor items.
                </div>
              ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                  {Object.entries(person.equippedItems).map(([slot, eqId]) => {
                    const eqDetail = person.equipmentDetails ? person.equipmentDetails[eqId] : null;
                    return (
                      <div
                        key={slot}
                        onClick={() => openEquipmentModal(eqId)}
                        className="p-3 bg-slate-800/60 hover:bg-slate-800 border border-slate-700/60 hover:border-amber-400/50 rounded-lg cursor-pointer transition flex items-center justify-between"
                      >
                        <div className="space-y-1">
                          <span className="text-[10px] font-semibold uppercase text-slate-400 block">{slot}</span>
                          <span className="text-sm font-bold text-slate-200 block">
                            {eqDetail?.displayName || eqId}
                          </span>
                          {eqDetail && (
                            <div className="flex gap-1.5 text-[10px]">
                              <span className="text-amber-400">{eqDetail.rarity}</span>
                              <span className="text-slate-500">&bull;</span>
                              <span className="text-blue-300">T{eqDetail.tier}</span>
                              <span className="text-slate-500">&bull;</span>
                              <span className="text-emerald-400">{eqDetail.condition}</span>
                            </div>
                          )}
                        </div>
                        <Shield className="w-5 h-5 text-slate-500" />
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          )}

          {person && activeTab === 'LINEAGE' && (
            <div className="space-y-6">
              {/* Family Relations */}
              <div className="p-4 bg-slate-850 rounded-xl border border-slate-800 space-y-3">
                <h4 className="text-xs font-semibold text-slate-300 uppercase tracking-wider flex items-center gap-1.5">
                  <Users className="w-4 h-4 text-blue-400" />
                  Immediate Kinship
                </h4>
                <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 text-xs">
                  <div className="p-2.5 bg-slate-800/40 rounded border border-slate-700/40">
                    <span className="text-slate-400 block text-[10px]">Father</span>
                    {person.fatherId ? (
                      <button
                        onClick={() => openPersonProfile(person.fatherId!)}
                        className="font-semibold text-sky-400 hover:underline"
                      >
                        {person.fatherName || `Person #${person.fatherId}`}
                      </button>
                    ) : (
                      <span className="text-slate-500 italic">Unknown</span>
                    )}
                  </div>
                  <div className="p-2.5 bg-slate-800/40 rounded border border-slate-700/40">
                    <span className="text-slate-400 block text-[10px]">Mother</span>
                    {person.motherId ? (
                      <button
                        onClick={() => openPersonProfile(person.motherId!)}
                        className="font-semibold text-sky-400 hover:underline"
                      >
                        {person.motherName || `Person #${person.motherId}`}
                      </button>
                    ) : (
                      <span className="text-slate-500 italic">Unknown</span>
                    )}
                  </div>
                  <div className="p-2.5 bg-slate-800/40 rounded border border-slate-700/40">
                    <span className="text-slate-400 block text-[10px]">Spouse</span>
                    {person.spouseId ? (
                      <button
                        onClick={() => openPersonProfile(person.spouseId!)}
                        className="font-semibold text-pink-400 hover:underline"
                      >
                        {person.spouseName || `Person #${person.spouseId}`}
                      </button>
                    ) : (
                      <span className="text-slate-500 italic">Unmarried</span>
                    )}
                  </div>
                </div>

                {person.childrenIds.length > 0 && (
                  <div className="pt-2">
                    <span className="text-xs text-slate-400 block mb-1.5">Children ({person.childrenIds.length}):</span>
                    <div className="flex flex-wrap gap-2">
                      {person.childrenIds.map((cid) => (
                        <button
                          key={cid}
                          onClick={() => openPersonProfile(cid)}
                          className="px-2.5 py-1 bg-slate-800 hover:bg-slate-700 text-xs text-slate-200 rounded border border-slate-700 transition"
                        >
                          Child #{cid}
                        </button>
                      ))}
                    </div>
                  </div>
                )}
              </div>

              {/* Clan Affiliations */}
              <div className="p-4 bg-slate-850 rounded-xl border border-slate-800 space-y-3">
                <h4 className="text-xs font-semibold text-slate-300 uppercase tracking-wider flex items-center gap-1.5">
                  <Compass className="w-4 h-4 text-amber-400" />
                  Clan & Organization Memberships
                </h4>
                {person.organizationMemberships.length === 0 ? (
                  <p className="text-xs text-slate-500 italic">No formal clan or order allegiances.</p>
                ) : (
                  <div className="flex flex-wrap gap-2">
                    {person.organizationMemberships.map((orgId, idx) => (
                      <button
                        key={idx}
                        onClick={() => openClanProfile(orgId)}
                        className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-xs text-amber-300 rounded border border-slate-700 transition"
                      >
                        {orgId}
                      </button>
                    ))}
                  </div>
                )}
              </div>

              {/* Achievements */}
              <div className="p-4 bg-slate-850 rounded-xl border border-slate-800 space-y-2">
                <h4 className="text-xs font-semibold text-slate-300 uppercase tracking-wider flex items-center gap-1.5">
                  <Award className="w-4 h-4 text-yellow-400" />
                  Historical Achievements ({person.achievements.length})
                </h4>
                {person.achievements.length === 0 ? (
                  <p className="text-xs text-slate-500 italic">No notable recorded heroic feats yet.</p>
                ) : (
                  <ul className="space-y-1">
                    {person.achievements.map((ach, idx) => (
                      <li key={idx} className="text-xs text-slate-300 bg-slate-800/40 px-3 py-1.5 rounded border border-slate-700/40">
                        {ach}
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="p-4 bg-slate-850 border-t border-slate-700 flex justify-between items-center text-xs text-slate-400">
          <span>Born Year {person?.birthYear} {person?.deathYear ? `&bull; Died Year ${person.deathYear}` : ''}</span>
          <button
            onClick={closePersonProfile}
            className="px-4 py-1.5 bg-slate-700 hover:bg-slate-600 text-slate-200 rounded-lg text-xs font-medium transition"
          >
            Close Dossier
          </button>
        </div>
      </div>
    </div>
  );
};

