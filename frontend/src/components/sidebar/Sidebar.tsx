import React, { useState } from 'react';
import { Activity, Building2, ChevronLeft, ChevronRight, Compass, Flame, Map, ScrollText, Shield, Skull, Sparkles, Swords, Trophy } from 'lucide-react';

interface SidebarProps {
  currentModule: string;
  onSelectModule: (module: string) => void;
  isAwakened: boolean;
}

const groups = [
  { label: 'World', items: [
    { id: 'world-map', label: 'World map', icon: Map },
    { id: 'families', label: 'Families & lineages', icon: Building2 },
    { id: 'organizations', label: 'Orders & factions', icon: Shield },
    { id: 'anomalies', label: 'Aether anomalies', icon: Sparkles },
    { id: 'expeditions', label: 'Expeditions', icon: Compass },
  ] },
  { label: 'Chronicle', items: [
    { id: 'activity', label: 'World activity', icon: Activity },
    { id: 'battles', label: 'Battles', icon: Swords },
    { id: 'tides', label: 'Monster tides', icon: Skull },
    { id: 'feuds', label: 'Blood feuds', icon: Flame },
  ] },
  { label: 'Archives', items: [
    { id: 'heirlooms', label: 'Heirlooms', icon: ScrollText },
    { id: 'leaderboards', label: 'Leaderboards', icon: Trophy },
  ] },
];

export const Sidebar: React.FC<SidebarProps> = ({ currentModule, onSelectModule, isAwakened }) => {
  const [collapsed, setCollapsed] = useState(() => sessionStorage.getItem('mie-sidebar-collapsed') === 'true');
  const toggleSidebar = () => setCollapsed((value) => {
    const next = !value;
    sessionStorage.setItem('mie-sidebar-collapsed', String(next));
    return next;
  });

  return (
    <aside className={`mie-sidebar ${collapsed ? 'is-collapsed' : ''} ${isAwakened ? 'is-awakened' : ''}`}>
      <div className="mie-sidebar-top">
        <div className="mie-sidebar-heading">
          {!collapsed && <div><span className="mie-sidebar-eyebrow">NAVIGATION</span><span className="mie-sidebar-title">Realm index</span></div>}
          <button onClick={toggleSidebar} className="mie-sidebar-toggle" title={collapsed ? 'Expand navigation' : 'Collapse navigation'} aria-label={collapsed ? 'Expand navigation' : 'Collapse navigation'}>
            {collapsed ? <ChevronRight size={16} /> : <ChevronLeft size={16} />}
          </button>
        </div>
        <nav aria-label="Main navigation" className="mie-nav-groups">
          {groups.map((group) => <div key={group.label} className="mie-nav-group">
            {!collapsed && <div className="mie-nav-group-label">{group.label}</div>}
            {group.items.map(({ id, label, icon: Icon }) => {
              const active = currentModule === id;
              return <button key={id} onClick={() => onSelectModule(id)} title={collapsed ? label : undefined}
                aria-current={active ? 'page' : undefined} aria-label={label}
                className={`mie-nav-item ${active ? 'is-active' : ''}`}>
                <span className="mie-nav-icon"><Icon size={17} strokeWidth={1.8} /></span>
                {!collapsed && <><span className="mie-nav-label">{label}</span>{active && <span className="mie-nav-active-mark" />}</>}
              </button>;
            })}
          </div>)}
        </nav>
      </div>
      <div className="mie-sidebar-bottom">
        <div className="mie-sidebar-status"><span className="mie-sidebar-status-dot" />{!collapsed && <span>{isAwakened ? 'Awakening active' : 'World stable'}</span>}</div>
        {!collapsed && <span className="mie-sidebar-version">MIE · SIM ENGINE 3.6</span>}
      </div>
    </aside>
  );
};
