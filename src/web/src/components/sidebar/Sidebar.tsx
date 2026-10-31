import React from 'react';
import {
  Map,
  Building2,
  Users,
  Shield,
  Trophy,
  Swords,
  Scroll,
  BarChart3,
  Globe,
} from 'lucide-react';

interface SidebarProps {
  currentModule: string;
  onSelectModule: (module: string) => void;
  isAwakened: boolean;
}

export const Sidebar: React.FC<SidebarProps> = ({ currentModule, onSelectModule, isAwakened }) => {
  const menuItems = [
    { id: 'world-overview', label: 'World Overview', icon: Globe },
    { id: 'world-map', label: 'Interactive Map', icon: Map, active: true },
    { id: 'settlements', label: 'Settlements', icon: Building2 },
    { id: 'people', label: 'People & Dynasties', icon: Users },
    { id: 'organizations', label: 'Clans & Orders', icon: Shield },
    { id: 'leaderboards', label: 'Leaderboards', icon: Trophy },
    { id: 'battles', label: 'Combat & Battles', icon: Swords },
    { id: 'history', label: 'Chronicle History', icon: Scroll },
    { id: 'statistics', label: 'World Statistics', icon: BarChart3 },
  ];

  return (
    <aside className="w-14 lg:w-48 bg-slate-950/95 border-r border-slate-800 flex flex-col justify-between py-3 select-none z-10">
      <div className="space-y-1 px-2">
        <div className="hidden lg:block px-3 py-1.5 text-[10px] font-bold text-slate-500 uppercase tracking-widest font-mono">
          Exploration
        </div>

        {menuItems.map((item) => {
          const isSelected = item.id === currentModule;
          const Icon = item.icon;

          return (
            <button
              key={item.id}
              onClick={() => onSelectModule(item.id)}
              className={`w-full flex items-center gap-3 px-3 py-2 rounded-xl text-xs font-medium transition ${
                isSelected
                  ? 'bg-cyan-950/60 text-cyan-300 border border-cyan-800/60 font-semibold shadow-sm'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900/60 border border-transparent'
              }`}
              title={item.label}
            >
              <Icon className={`w-4 h-4 shrink-0 ${isSelected ? 'text-cyan-400' : 'text-slate-400'}`} />
              <span className="hidden lg:inline truncate">{item.label}</span>
            </button>
          );
        })}
      </div>

      {/* Footer Info */}
      <div className="hidden lg:block px-4 py-2 border-t border-slate-900 text-[10px] font-mono text-slate-500">
        <div>CivSim Engine v3.2</div>
        <div className="text-[9px] text-slate-600">Deterministic Sim</div>
      </div>
    </aside>
  );
};

