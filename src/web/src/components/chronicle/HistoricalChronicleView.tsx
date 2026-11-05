import React, { useState, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { fetchChronicleYears, fetchChronicleAnnual, fetchChronicleMilestones } from '../../services/api';
import { useMapStore } from '../../store/useMapStore';
import {
  BookOpen,
  Calendar,
  Users,
  Heart,
  Skull,
  Building,
  Shield,
  Swords,
  Sparkles,
  Flame,
  Award,
  ChevronLeft,
  ChevronRight,
  TrendingUp,
  TrendingDown,
  Layers,
  Clock,
} from 'lucide-react';

export const HistoricalChronicleView: React.FC = () => {
  const selectedYear = useMapStore((s) => s.selectedChronicleYear);
  const setChronicleYear = useMapStore((s) => s.setChronicleYear);
  const [activeTab, setActiveTab] = useState<'ANNUAL' | 'MILESTONES'>('ANNUAL');

  // Fetch available chronicle years
  const { data: yearsList, isLoading: yearsLoading } = useQuery({
    queryKey: ['chronicle-years'],
    queryFn: fetchChronicleYears,
  });

  // Set default year to the latest year available
  useEffect(() => {
    if (yearsList && yearsList.length > 0 && selectedYear === null) {
      setChronicleYear(yearsList[yearsList.length - 1]);
    }
  }, [yearsList, selectedYear, setChronicleYear]);

  const currentYear = selectedYear ?? (yearsList && yearsList.length > 0 ? yearsList[yearsList.length - 1] : 0);

  // Fetch annual chronicle record
  const {
    data: annual,
    isLoading: annualLoading,
    isError: annualError,
  } = useQuery({
    queryKey: ['chronicle-annual', currentYear],
    queryFn: () => fetchChronicleAnnual(currentYear),
    enabled: currentYear !== null && currentYear !== undefined,
  });

  // Fetch civilization milestones
  const { data: milestones, isLoading: milestonesLoading } = useQuery({
    queryKey: ['chronicle-milestones'],
    queryFn: fetchChronicleMilestones,
  });

  const isAwakening = annual?.era === 'Awakening Era' || currentYear >= 50;

  const popDelta =
    annual && annual.openingPopulation > 0
      ? (((annual.closingPopulation - annual.openingPopulation) / annual.openingPopulation) * 100).toFixed(1)
      : '0.0';

  const isPopGrowing = Number(popDelta) >= 0;

  return (
    <div className="flex-1 flex flex-col h-full overflow-hidden bg-slate-950 select-none text-slate-100">
      {/* Top Header */}
      <div className="p-4 border-b border-slate-800 bg-slate-900/60 flex flex-wrap items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="p-2 rounded-xl bg-amber-950/60 text-amber-400 border border-amber-800/60">
            <BookOpen className="w-5 h-5" />
          </div>
          <div>
            <h1 className="text-base font-bold text-slate-100 flex items-center gap-2">
              Historical Chronicle
              <span
                className={`px-2 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider border ${
                  isAwakening
                    ? 'bg-cyan-950/80 text-cyan-300 border-cyan-700/60 shadow-[0_0_10px_rgba(6,182,212,0.2)]'
                    : 'bg-amber-950/80 text-amber-300 border-amber-700/60'
                }`}
              >
                {annual?.era || (currentYear >= 50 ? 'Awakening Era' : 'Mundane Era')}
              </span>
            </h1>
            <p className="text-xs text-slate-400">
              Immutable annual civilization annals, demographic progression, and historical milestones
            </p>
          </div>
        </div>

        {/* Tab Switcher */}
        <div className="flex items-center p-1 rounded-xl bg-slate-900 border border-slate-800">
          <button
            onClick={() => setActiveTab('ANNUAL')}
            className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition ${
              activeTab === 'ANNUAL'
                ? 'bg-amber-950/80 text-amber-300 border border-amber-800/60 shadow-sm'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            Annual Annals
          </button>
          <button
            onClick={() => setActiveTab('MILESTONES')}
            className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition ${
              activeTab === 'MILESTONES'
                ? 'bg-cyan-950/80 text-cyan-300 border border-cyan-800/60 shadow-sm'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            Epochal Milestones
          </button>
        </div>
      </div>

      {/* Main Body */}
      {activeTab === 'ANNUAL' && (
        <div className="flex-1 flex flex-col overflow-hidden">
          {/* Year Navigation Bar */}
          <div className="px-6 py-3 border-b border-slate-800 bg-slate-900/40 flex items-center justify-between gap-4 flex-wrap">
            <div className="flex items-center gap-2">
              <button
                onClick={() => {
                  if (yearsList) {
                    const idx = yearsList.indexOf(currentYear);
                    if (idx > 0) setChronicleYear(yearsList[idx - 1]);
                  }
                }}
                disabled={!yearsList || yearsList.indexOf(currentYear) <= 0}
                className="p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 disabled:opacity-40 disabled:cursor-not-allowed text-slate-300 transition"
                title="Previous Year"
              >
                <ChevronLeft className="w-4 h-4" />
              </button>

              <div className="flex items-center gap-2">
                <Calendar className="w-4 h-4 text-amber-400" />
                <span className="text-sm font-bold text-slate-100 font-mono">Year {currentYear}</span>
              </div>

              <button
                onClick={() => {
                  if (yearsList) {
                    const idx = yearsList.indexOf(currentYear);
                    if (idx >= 0 && idx < yearsList.length - 1) setChronicleYear(yearsList[idx + 1]);
                  }
                }}
                disabled={!yearsList || yearsList.indexOf(currentYear) >= (yearsList?.length ?? 0) - 1}
                className="p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 disabled:opacity-40 disabled:cursor-not-allowed text-slate-300 transition"
                title="Next Year"
              >
                <ChevronRight className="w-4 h-4" />
              </button>
            </div>

            {/* Quick jump buttons */}
            <div className="flex items-center gap-2">
              {yearsList && yearsList.includes(0) && (
                <button
                  onClick={() => setChronicleYear(0)}
                  className={`px-2 py-1 rounded text-xs font-mono transition border ${
                    currentYear === 0
                      ? 'bg-amber-950 text-amber-300 border-amber-700'
                      : 'bg-slate-900 text-slate-400 hover:text-slate-200 border-slate-800'
                  }`}
                >
                  Year 0 (Genesis)
                </button>
              )}
              {yearsList && yearsList.some((y) => y >= 50) && (
                <button
                  onClick={() => setChronicleYear(50)}
                  className={`px-2 py-1 rounded text-xs font-mono transition border ${
                    currentYear === 50
                      ? 'bg-cyan-950 text-cyan-300 border-cyan-700'
                      : 'bg-slate-900 text-slate-400 hover:text-slate-200 border-slate-800'
                  }`}
                >
                  Year 50 (Awakening)
                </button>
              )}
              {yearsList && yearsList.length > 0 && (
                <button
                  onClick={() => setChronicleYear(yearsList[yearsList.length - 1])}
                  className={`px-2 py-1 rounded text-xs font-mono transition border ${
                    currentYear === yearsList[yearsList.length - 1]
                      ? 'bg-emerald-950 text-emerald-300 border-emerald-700'
                      : 'bg-slate-900 text-slate-400 hover:text-slate-200 border-slate-800'
                  }`}
                >
                  Current (Year {yearsList[yearsList.length - 1]})
                </button>
              )}

              {/* Year Dropdown */}
              <select
                value={currentYear}
                onChange={(e) => setChronicleYear(Number(e.target.value))}
                className="px-2.5 py-1 rounded-lg bg-slate-900 border border-slate-700 text-xs font-mono text-slate-200 focus:outline-none focus:border-amber-500"
              >
                {yearsList?.map((yr) => (
                  <option key={yr} value={yr}>
                    Year {yr}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* Annual Content Scroll Area */}
          <div className="flex-1 overflow-y-auto p-6 space-y-6 scrollbar-thin">
            {annualLoading && (
              <div className="py-20 text-center text-slate-400 text-xs font-mono">
                <div className="w-8 h-8 rounded-full border-2 border-amber-500/20 border-t-amber-400 animate-spin mx-auto mb-3" />
                Retrieving annual historical archive for Year {currentYear}...
              </div>
            )}

            {annualError && (
              <div className="p-4 rounded-xl bg-rose-950/40 border border-rose-800/60 text-center text-xs text-rose-300">
                Annual chronicle record not found for Year {currentYear}.
              </div>
            )}

            {annual && (
              <>
                {/* 1. Key Metrics Cards */}
                <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
                  {/* Population */}
                  <div className="p-3.5 rounded-xl bg-slate-900/70 border border-slate-800 flex flex-col justify-between">
                    <span className="text-[11px] font-bold text-slate-400 uppercase">Population</span>
                    <div className="text-xl font-bold font-mono text-slate-100 my-1">
                      {annual.closingPopulation}
                    </div>
                    <div className="flex items-center gap-1 text-[11px] font-semibold">
                      {isPopGrowing ? (
                        <span className="text-emerald-400 flex items-center gap-0.5">
                          <TrendingUp className="w-3 h-3" /> +{popDelta}%
                        </span>
                      ) : (
                        <span className="text-rose-400 flex items-center gap-0.5">
                          <TrendingDown className="w-3 h-3" /> {popDelta}%
                        </span>
                      )}
                      <span className="text-slate-500 text-[10px] font-mono">from {annual.openingPopulation}</span>
                    </div>
                  </div>

                  {/* Demographics: Births / Deaths */}
                  <div className="p-3.5 rounded-xl bg-slate-900/70 border border-slate-800 flex flex-col justify-between">
                    <span className="text-[11px] font-bold text-slate-400 uppercase">Births / Deaths</span>
                    <div className="text-base font-bold font-mono text-slate-100 my-1 flex items-center gap-2">
                      <span className="text-emerald-400">+{annual.births}</span>
                      <span className="text-slate-600">/</span>
                      <span className="text-rose-400">-{annual.deaths}</span>
                    </div>
                    <div className="text-[11px] font-mono text-slate-400">Marriages: {annual.marriages}</div>
                  </div>

                  {/* Settlements */}
                  <div className="p-3.5 rounded-xl bg-slate-900/70 border border-slate-800 flex flex-col justify-between">
                    <span className="text-[11px] font-bold text-slate-400 uppercase">Settlements</span>
                    <div className="text-xl font-bold font-mono text-slate-100 my-1">
                      {annual.settlementsCount}
                    </div>
                    <div className="text-[11px] font-mono text-slate-400">Migrations: {annual.migrations}</div>
                  </div>

                  {/* Clans */}
                  <div className="p-3.5 rounded-xl bg-slate-900/70 border border-slate-800 flex flex-col justify-between">
                    <span className="text-[11px] font-bold text-slate-400 uppercase">Active Clans</span>
                    <div className="text-xl font-bold font-mono text-slate-100 my-1">
                      {annual.activeClansCount}
                    </div>
                    <div className="text-[11px] font-mono text-slate-400">Organizations</div>
                  </div>

                  {/* Battles & Warfare */}
                  <div className="p-3.5 rounded-xl bg-slate-900/70 border border-slate-800 flex flex-col justify-between">
                    <span className="text-[11px] font-bold text-slate-400 uppercase">Battles Recorded</span>
                    <div className="text-xl font-bold font-mono text-rose-300 my-1">{annual.battlesCount}</div>
                    <div className="text-[11px] font-mono text-slate-400">Clashes & Raids</div>
                  </div>

                  {/* Monster Tides */}
                  <div className="p-3.5 rounded-xl bg-slate-900/70 border border-slate-800 flex flex-col justify-between">
                    <span className="text-[11px] font-bold text-slate-400 uppercase">Monster Tides</span>
                    <div className="text-xl font-bold font-mono text-purple-300 my-1">{annual.tidesCount}</div>
                    <div className="text-[11px] font-mono text-slate-400">
                      Anomalies: {annual.anomaliesCount}
                    </div>
                  </div>
                </div>

                {/* 2. Village Population Breakdown */}
                {annual.villagePopulations && Object.keys(annual.villagePopulations).length > 0 && (
                  <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800">
                    <h3 className="text-xs font-bold text-slate-300 uppercase tracking-wider mb-3 flex items-center gap-2">
                      <Building className="w-3.5 h-3.5 text-blue-400" />
                      Settlement Populations at Year End
                    </h3>
                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
                      {Object.entries(annual.villagePopulations).map(([vName, vPop]) => (
                        <div
                          key={vName}
                          className="p-3 rounded-lg bg-slate-950/70 border border-slate-800/80 flex items-center justify-between"
                        >
                          <span className="text-xs font-medium text-slate-200">{vName}</span>
                          <span className="text-xs font-bold font-mono text-cyan-300">{vPop} citizens</span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* 3. Narrative Summaries (6 Pillars) */}
                <div className="space-y-4">
                  <h3 className="text-xs font-bold text-slate-400 uppercase tracking-wider flex items-center gap-2">
                    <Layers className="w-3.5 h-3.5 text-amber-400" />
                    Annual Historical Narratives
                  </h3>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {/* Awakening Summary */}
                    <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 flex flex-col gap-2">
                      <div className="flex items-center gap-2 text-xs font-bold text-cyan-300 uppercase">
                        <Sparkles className="w-4 h-4 text-cyan-400" />
                        Awakening & Supernatural State
                      </div>
                      <p className="text-xs text-slate-300 leading-relaxed font-sans">
                        {annual.awakeningSummary || 'No supernatural incidents recorded this annual cycle.'}
                      </p>
                    </div>

                    {/* Combat Summary */}
                    <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 flex flex-col gap-2">
                      <div className="flex items-center gap-2 text-xs font-bold text-rose-300 uppercase">
                        <Swords className="w-4 h-4 text-rose-400" />
                        Military Engagements & Battles
                      </div>
                      <p className="text-xs text-slate-300 leading-relaxed font-sans">
                        {annual.combatSummary || 'Peace prevailed across civilized boundaries without major warfare.'}
                      </p>
                    </div>

                    {/* Organization Summary */}
                    <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 flex flex-col gap-2">
                      <div className="flex items-center gap-2 text-xs font-bold text-indigo-300 uppercase">
                        <Shield className="w-4 h-4 text-indigo-400" />
                        Clan Politics & Organizations
                      </div>
                      <p className="text-xs text-slate-300 leading-relaxed font-sans">
                        {annual.organizationSummary || 'Civic order and organizational structures remained stable.'}
                      </p>
                    </div>

                    {/* Monster Ecology Summary */}
                    <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 flex flex-col gap-2">
                      <div className="flex items-center gap-2 text-xs font-bold text-purple-300 uppercase">
                        <Skull className="w-4 h-4 text-purple-400" />
                        Monster Lairs & Ecology
                      </div>
                      <p className="text-xs text-slate-300 leading-relaxed font-sans">
                        {annual.monsterEcologySummary || 'Beast populations were contained within their wilderness lairs.'}
                      </p>
                    </div>

                    {/* Equipment Summary */}
                    <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 flex flex-col gap-2">
                      <div className="flex items-center gap-2 text-xs font-bold text-amber-300 uppercase">
                        <Award className="w-4 h-4 text-amber-400" />
                        Equipment & Crafting Mastery
                      </div>
                      <p className="text-xs text-slate-300 leading-relaxed font-sans">
                        {annual.equipmentSummary || 'Standard maintenance was conducted across village workshops.'}
                      </p>
                    </div>

                    {/* Blood Feuds Summary */}
                    <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 flex flex-col gap-2">
                      <div className="flex items-center gap-2 text-xs font-bold text-amber-400 uppercase">
                        <Flame className="w-4 h-4 text-amber-500" />
                        Grievances & Blood Feuds
                      </div>
                      <p className="text-xs text-slate-300 leading-relaxed font-sans">
                        {annual.bloodFeudSummary || 'No generational blood feuds escalated this year.'}
                      </p>
                    </div>
                  </div>
                </div>
              </>
            )}
          </div>
        </div>
      )}

      {/* Epochal Milestones Tab */}
      {activeTab === 'MILESTONES' && (
        <div className="flex-1 overflow-y-auto p-6 scrollbar-thin">
          <div className="max-w-4xl mx-auto space-y-4">
            <div className="mb-4">
              <h2 className="text-sm font-bold text-slate-200 uppercase tracking-wider">Civilization Milestones</h2>
              <p className="text-xs text-slate-400">
                Pivotal historical occurrences that marked civilizational transitions and epochal shifts
              </p>
            </div>

            {milestonesLoading && (
              <div className="py-20 text-center text-slate-400 text-xs font-mono">
                <div className="w-8 h-8 rounded-full border-2 border-cyan-500/20 border-t-cyan-400 animate-spin mx-auto mb-3" />
                Loading epochal milestones...
              </div>
            )}

            {milestones?.map((m, idx) => (
              <div
                key={idx}
                className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 flex items-start gap-4 hover:border-cyan-800/60 transition group"
              >
                <div className="flex flex-col items-center justify-center p-2.5 rounded-lg bg-slate-950 border border-slate-800 shrink-0 font-mono text-center min-w-[70px]">
                  <span className="text-[10px] text-slate-500 uppercase">Year</span>
                  <span className="text-sm font-bold text-amber-400">{m.year}</span>
                  <span className="text-[10px] text-slate-500">M {m.month}</span>
                </div>

                <div className="flex-1 space-y-1">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase bg-cyan-950 text-cyan-300 border border-cyan-800/60">
                      {m.category}
                    </span>
                    <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase bg-slate-800 text-slate-300 border border-slate-700">
                      {m.era}
                    </span>
                    <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase bg-amber-950 text-amber-300 border border-amber-800/60">
                      {m.importance}
                    </span>
                  </div>

                  <h3 className="text-sm font-bold text-slate-100 group-hover:text-cyan-300 transition">{m.title}</h3>
                  <p className="text-xs text-slate-300 leading-relaxed font-sans">{m.description}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

