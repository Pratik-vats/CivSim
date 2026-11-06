import React, { useState } from 'react';
import {
  Map,
  Building2,
  Shield,
  Trophy,
  Swords,
  Scroll,
  Activity,
  Skull,
  Compass,
  Flame,
  ChevronLeft,
  ChevronRight,
} from 'lucide-react';

interface SidebarProps {
  currentModule: string;
  onSelectModule: (module: string) => void;
  isAwakened: boolean;
}

export const Sidebar: React.FC<SidebarProps> = ({ currentModule, onSelectModule, isAwakened }) => {
  const [collapsed, setCollapsed] = useState(() => sessionStorage.getItem('mie-sidebar-collapsed') === 'true');
  const toggleSidebar = () => setCollapsed((value) => {
    const next = !value;
    sessionStorage.setItem('mie-sidebar-collapsed', String(next));
    return next;
  });
  const explorationItems = [
    { id: 'world-map', label: 'Interactive Map', icon: Map },
    { id: 'families', label: 'Family Dynasties', icon: Building2 },
    { id: 'organizations', label: 'Clans & Orders', icon: Shield },
    { id: 'heirlooms', label: 'Clan Heirlooms', icon: Scroll },
    { id: 'leaderboards', label: 'Leaderboards', icon: Trophy },
  ];

  const activityItems = [
    { id: 'activity', label: 'Live Activity', icon: Activity },
    { id: 'battles', label: 'Tactical Battles', icon: Swords },
    { id: 'tides', label: 'Monster Tides', icon: Skull },
    { id: 'expeditions', label: 'Expeditions', icon: Compass },
    { id: 'feuds', label: 'Blood Feuds', icon: Flame },
  ];

  const activeBg = isAwakened
    ? 'bg-cyan-950/60 text-cyan-300 border-cyan-800/60'
    : 'bg-amber-950/60 text-amber-300 border-amber-800/60';
  const activeIcon = isAwakened ? 'text-cyan-400' : 'text-amber-400';

  return (
    <aside
      className={`${collapsed ? 'w-14' : 'w-48'} shrink-0 flex flex-col justify-between py-3 select-none z-10 overflow-y-auto scrollbar-thin border-r transition-[width] duration-200 ${
        isAwakened ? 'bg-slate-950/95 border-slate-800' : 'bg-stone-950/95 border-amber-900/30'
      }`}
    >
      <div className="space-y-4 px-2">
        <button onClick={toggleSidebar} className="ml-auto flex p-1.5 rounded-lg text-slate-400 hover:bg-slate-800" title={collapsed ? 'Expand sidebar' : 'Collapse sidebar'} aria-label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}>
          {collapsed ? <ChevronRight className="w-4 h-4" /> : <ChevronLeft className="w-4 h-4" />}
        </button>
        <div>
          {!collapsed && <div className="px-3 py-1.5 text-[10px] font-bold text-slate-500 uppercase tracking-widest font-mono">
            Exploration
          </div>}

          <div className="space-y-1">
            {explorationItems.map((item) => {
              const isSelected = item.id === currentModule;
              const Icon = item.icon;

              return (
                <button
                  key={item.id}
                  onClick={() => onSelectModule(item.id)}
                  className={`w-full flex items-center gap-3 px-3 py-2 rounded-xl text-xs font-medium transition ${
                    isSelected
                      ? `${activeBg} font-semibold shadow-sm border`
                      : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900/60 border border-transparent'
                  }`}
                  title={item.label}
                >
                  <Icon className={`w-4 h-4 shrink-0 ${isSelected ? activeIcon : 'text-slate-400'}`} />
                  {!collapsed && <span className="truncate">{item.label}</span>}
                </button>
              );
            })}
          </div>
        </div>

        <div>
          {!collapsed && <div className="px-3 py-1.5 text-[10px] font-bold text-slate-500 uppercase tracking-widest font-mono">
            Chronicle & Events
          </div>}

          <div className="space-y-1">
            {activityItems.map((item) => {
              const isSelected = item.id === currentModule;
              const Icon = item.icon;

              return (
                <button
                  key={item.id}
                  onClick={() => onSelectModule(item.id)}
                  className={`w-full flex items-center gap-3 px-3 py-2 rounded-xl text-xs font-medium transition ${
                    isSelected
                      ? `${activeBg} font-semibold shadow-sm border`
                      : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900/60 border border-transparent'
                  }`}
                  title={item.label}
                >
                  <Icon className={`w-4 h-4 shrink-0 ${isSelected ? activeIcon : 'text-slate-400'}`} />
                  {!collapsed && <span className="truncate">{item.label}</span>}
                </button>
              );
            })}
          </div>
        </div>

      </div>

      {/* Footer Info */}
      {!collapsed && <div className="px-4 py-2 border-t border-slate-900 text-[10px] font-mono text-slate-500 mt-2">
        <div>CivSim Engine v3.5</div>
        <div className="text-[9px] text-slate-600">Deterministic Sim</div>
      </div>}
    </aside>
  );
};

