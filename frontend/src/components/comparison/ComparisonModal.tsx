import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { comparePeople, compareOrganizations } from '../../services/api';
import { useMapStore } from '../../store/useMapStore';
import {
  X,
  GitCompare,
  Flame,
  Coins,
  Shield,
  Zap,
  Swords,
  Trophy,
  Users,
} from 'lucide-react';

export const ComparisonModal: React.FC = () => {
  const comparePeopleIds = useMapStore((s) => s.comparePeopleIds);
  const compareOrgIds = useMapStore((s) => s.compareOrgIds);
  const closeCompareModal = useMapStore((s) => s.closeCompareModal);

  const isPeople = !!comparePeopleIds;
  const isOrg = !!compareOrgIds;

  const { data: peopleData, isLoading: peopleLoading } = useQuery({
    queryKey: ['comparePeople', comparePeopleIds],
    queryFn: () =>
      comparePeopleIds ? comparePeople(comparePeopleIds.id1, comparePeopleIds.id2) : null,
    enabled: isPeople,
  });

  const { data: orgData, isLoading: orgLoading } = useQuery({
    queryKey: ['compareOrg', compareOrgIds],
    queryFn: () =>
      compareOrgIds ? compareOrganizations(compareOrgIds.id1, compareOrgIds.id2) : null,
    enabled: isOrg,
  });

  if (!isPeople && !isOrg) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 backdrop-blur-sm p-4 animate-in fade-in duration-200">
      <div className="bg-slate-900 border border-slate-700 rounded-xl shadow-2xl w-full max-w-4xl max-h-[90vh] flex flex-col overflow-hidden">
        {/* Header */}
        <div className="p-4 bg-slate-800/90 border-b border-slate-700 flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="p-2 bg-indigo-500/10 border border-indigo-500/30 rounded-lg text-indigo-400">
              <GitCompare className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-slate-100">
                {isPeople ? 'Side-by-Side Character Comparison' : 'Side-by-Side Organization Comparison'}
              </h2>
              <p className="text-xs text-slate-400">
                {peopleData?.comparisonDate || orgData?.comparisonDate || 'Synchronized Simulation Epoch'}
              </p>
            </div>
          </div>

          <button
            onClick={closeCompareModal}
            className="p-1 rounded-lg hover:bg-slate-700 text-slate-400 hover:text-slate-200 transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="p-6 overflow-y-auto space-y-6">
          {(peopleLoading || orgLoading) && (
            <div className="py-24 text-center text-slate-400">Loading side-by-side telemetry...</div>
          )}

          {/* PEOPLE COMPARISON */}
          {isPeople && peopleData && (
            <>
              {/* Executive Summary Cards */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                <div className="p-3 bg-slate-800/50 rounded-lg border border-slate-700/50 flex items-start gap-2.5">
                  <Flame className="w-5 h-5 text-rose-400 shrink-0 mt-0.5" />
                  <div>
                    <span className="text-xs text-slate-400 block font-semibold">Combat Power Assessment</span>
                    <p className="text-xs text-slate-200 mt-0.5">{peopleData.powerSummary}</p>
                  </div>
                </div>
                <div className="p-3 bg-slate-800/50 rounded-lg border border-slate-700/50 flex items-start gap-2.5">
                  <Coins className="w-5 h-5 text-amber-400 shrink-0 mt-0.5" />
                  <div>
                    <span className="text-xs text-slate-400 block font-semibold">Wealth Assessment</span>
                    <p className="text-xs text-slate-200 mt-0.5">{peopleData.wealthSummary}</p>
                  </div>
                </div>
              </div>

              {/* Side by Side Heads */}
              <div className="grid grid-cols-2 gap-4">
                <div className="p-4 bg-slate-850 rounded-xl border border-slate-800 text-center">
                  <div className="font-bold text-lg text-sky-300">{peopleData.person1.name}</div>
                  <div className="text-xs text-slate-400 mt-0.5 font-mono">#{peopleData.person1.id} &bull; Age {peopleData.person1.age}</div>
                  <div className="mt-2 text-xs font-semibold text-purple-300">
                    {peopleData.person1.isAwakened ? `Tier ${peopleData.person1.tier} ${peopleData.person1.supernaturalClass}` : 'Mundane Citizen'}
                  </div>
                  <div className="text-xs text-slate-400 mt-0.5">{peopleData.person1.villageName || 'Wandering'}</div>
                </div>

                <div className="p-4 bg-slate-850 rounded-xl border border-slate-800 text-center">
                  <div className="font-bold text-lg text-emerald-300">{peopleData.person2.name}</div>
                  <div className="text-xs text-slate-400 mt-0.5 font-mono">#{peopleData.person2.id} &bull; Age {peopleData.person2.age}</div>
                  <div className="mt-2 text-xs font-semibold text-purple-300">
                    {peopleData.person2.isAwakened ? `Tier ${peopleData.person2.tier} ${peopleData.person2.supernaturalClass}` : 'Mundane Citizen'}
                  </div>
                  <div className="text-xs text-slate-400 mt-0.5">{peopleData.person2.villageName || 'Wandering'}</div>
                </div>
              </div>

              {/* Comparative Core Metrics Table */}
              <div className="space-y-2">
                <h4 className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
                  Comparative Attributes & Combat Stats
                </h4>
                <div className="bg-slate-850 rounded-xl border border-slate-800 overflow-hidden text-xs">
                  <table className="w-full text-slate-300">
                    <tbody className="divide-y divide-slate-800">
                      <tr className="bg-slate-800/40 font-bold">
                        <td className="py-2 px-4 text-left font-mono text-sky-300">
                          {peopleData.person1.derivedPower.toFixed(1)}
                        </td>
                        <td className="py-2 px-4 text-center text-slate-400 font-sans">Derived Combat Power</td>
                        <td className="py-2 px-4 text-right font-mono text-emerald-300">
                          {peopleData.person2.derivedPower.toFixed(1)}
                        </td>
                      </tr>
                      <tr>
                        <td className="py-2 px-4 text-left font-mono">{peopleData.person1.wealth.toFixed(1)}s</td>
                        <td className="py-2 px-4 text-center text-slate-400">Personal Wealth</td>
                        <td className="py-2 px-4 text-right font-mono">{peopleData.person2.wealth.toFixed(1)}s</td>
                      </tr>
                      <tr>
                        <td className="py-2 px-4 text-left font-mono">{peopleData.person1.mastery.toFixed(1)}</td>
                        <td className="py-2 px-4 text-center text-slate-400">Aether Mastery</td>
                        <td className="py-2 px-4 text-right font-mono">{peopleData.person2.mastery.toFixed(1)}</td>
                      </tr>
                      <tr>
                        <td className="py-2 px-4 text-left font-mono">{peopleData.person1.combatProfile?.physicalAttack.toFixed(1)}</td>
                        <td className="py-2 px-4 text-center text-slate-400">Physical Attack</td>
                        <td className="py-2 px-4 text-right font-mono">{peopleData.person2.combatProfile?.physicalAttack.toFixed(1)}</td>
                      </tr>
                      <tr>
                        <td className="py-2 px-4 text-left font-mono">{peopleData.person1.combatProfile?.physicalDefense.toFixed(1)}</td>
                        <td className="py-2 px-4 text-center text-slate-400">Physical Defense</td>
                        <td className="py-2 px-4 text-right font-mono">{peopleData.person2.combatProfile?.physicalDefense.toFixed(1)}</td>
                      </tr>
                      <tr>
                        <td className="py-2 px-4 text-left font-mono">{peopleData.person1.combatProfile?.aetherPotency.toFixed(1)}</td>
                        <td className="py-2 px-4 text-center text-slate-400">Aether Potency</td>
                        <td className="py-2 px-4 text-right font-mono">{peopleData.person2.combatProfile?.aetherPotency.toFixed(1)}</td>
                      </tr>
                      {/* Attributes comparison */}
                      {peopleData.person1.attributes &&
                        Object.keys(peopleData.person1.attributes).map((key) => {
                          const val1 = peopleData.person1.attributes[key]?.effective ?? 0;
                          const val2 = peopleData.person2.attributes[key]?.effective ?? 0;
                          return (
                            <tr key={key}>
                              <td
                                className={`py-1.5 px-4 text-left font-mono ${
                                  val1 > val2 ? 'text-sky-300 font-bold' : 'text-slate-400'
                                }`}
                              >
                                {val1.toFixed(1)}
                              </td>
                              <td className="py-1.5 px-4 text-center text-slate-400">
                                {key.replace('_', ' ')}
                              </td>
                              <td
                                className={`py-1.5 px-4 text-right font-mono ${
                                  val2 > val1 ? 'text-emerald-300 font-bold' : 'text-slate-400'
                                }`}
                              >
                                {val2.toFixed(1)}
                              </td>
                            </tr>
                          );
                        })}
                    </tbody>
                  </table>
                </div>
              </div>
            </>
          )}

          {/* ORGANIZATION COMPARISON */}
          {isOrg && orgData && (
            <>
              {/* Executive Summary Cards */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                <div className="p-3 bg-slate-800/50 rounded-lg border border-slate-700/50 flex items-start gap-2.5">
                  <Trophy className="w-5 h-5 text-amber-400 shrink-0 mt-0.5" />
                  <div>
                    <span className="text-xs text-slate-400 block font-semibold">Influence Assessment</span>
                    <p className="text-xs text-slate-200 mt-0.5">{orgData.influenceSummary}</p>
                  </div>
                </div>
                <div className="p-3 bg-slate-800/50 rounded-lg border border-slate-700/50 flex items-start gap-2.5">
                  <Swords className="w-5 h-5 text-rose-400 shrink-0 mt-0.5" />
                  <div>
                    <span className="text-xs text-slate-400 block font-semibold">Military Record Assessment</span>
                    <p className="text-xs text-slate-200 mt-0.5">{orgData.militarySummary}</p>
                  </div>
                </div>
              </div>

              {/* Side by Side Heads */}
              <div className="grid grid-cols-2 gap-4">
                <div className="p-4 bg-slate-850 rounded-xl border border-slate-800 text-center">
                  <div className="font-bold text-lg text-purple-300">{orgData.org1.name}</div>
                  <div className="text-xs text-slate-400 mt-0.5 font-mono">#{orgData.org1.id} &bull; Founded Y{orgData.org1.foundingYear}</div>
                  <div className="mt-2 text-xs font-semibold text-slate-200">
                    {orgData.org1.type} &bull; {orgData.org1.alignment}
                  </div>
                </div>

                <div className="p-4 bg-slate-850 rounded-xl border border-slate-800 text-center">
                  <div className="font-bold text-lg text-amber-300">{orgData.org2.name}</div>
                  <div className="text-xs text-slate-400 mt-0.5 font-mono">#{orgData.org2.id} &bull; Founded Y{orgData.org2.foundingYear}</div>
                  <div className="mt-2 text-xs font-semibold text-slate-200">
                    {orgData.org2.type} &bull; {orgData.org2.alignment}
                  </div>
                </div>
              </div>

              {/* Metrics Table */}
              <div className="bg-slate-850 rounded-xl border border-slate-800 overflow-hidden text-xs">
                <table className="w-full text-slate-300">
                  <tbody className="divide-y divide-slate-800">
                    <tr>
                      <td className="py-2 px-4 text-left font-mono font-bold text-amber-300">
                        {orgData.org1.reputation.toFixed(1)}
                      </td>
                      <td className="py-2 px-4 text-center text-slate-400">Regional Influence / Rep</td>
                      <td className="py-2 px-4 text-right font-mono font-bold text-amber-300">
                        {orgData.org2.reputation.toFixed(1)}
                      </td>
                    </tr>
                    <tr>
                      <td className="py-2 px-4 text-left font-mono">{orgData.org1.memberCount}</td>
                      <td className="py-2 px-4 text-center text-slate-400">Active Membership</td>
                      <td className="py-2 px-4 text-right font-mono">{orgData.org2.memberCount}</td>
                    </tr>
                    <tr>
                      <td className="py-2 px-4 text-left font-mono">{orgData.org1.battlesWon} / {orgData.org1.battlesTotal}</td>
                      <td className="py-2 px-4 text-center text-slate-400">Battle Record (Won / Total)</td>
                      <td className="py-2 px-4 text-right font-mono">{orgData.org2.battlesWon} / {orgData.org2.battlesTotal}</td>
                    </tr>
                    <tr>
                      <td className="py-2 px-4 text-left font-mono">{orgData.org1.memberCasualties}</td>
                      <td className="py-2 px-4 text-center text-slate-400">Recorded Casualties</td>
                      <td className="py-2 px-4 text-right font-mono">{orgData.org2.memberCasualties}</td>
                    </tr>
                    <tr>
                      <td className="py-2 px-4 text-left font-mono">{orgData.org1.heirloomCount}</td>
                      <td className="py-2 px-4 text-center text-slate-400">Ancestral Heirlooms</td>
                      <td className="py-2 px-4 text-right font-mono">{orgData.org2.heirloomCount}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </>
          )}
        </div>

        {/* Footer */}
        <div className="p-3 bg-slate-850 border-t border-slate-800 text-right">
          <button
            onClick={closeCompareModal}
            className="px-4 py-1.5 bg-slate-700 hover:bg-slate-600 text-slate-200 rounded-lg text-xs font-medium transition"
          >
            Close Comparison
          </button>
        </div>
      </div>
    </div>
  );
};

