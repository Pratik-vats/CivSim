import React, { useState, useEffect, useRef } from 'react';
import { searchWorld } from '../../services/api';
import { SearchResultDto } from '../../types/api';
import { useMapStore } from '../../store/useMapStore';
import { Search, X, MapPin, Skull, Sparkles, Users, User } from 'lucide-react';

interface SearchBarProps {
  onCenterEntity: (wx: number, wy: number) => void;
}

export const SearchBar: React.FC<SearchBarProps> = ({ onCenterEntity }) => {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<SearchResultDto[]>([]);
  const [isOpen, setIsOpen] = useState(false);
  const [loading, setLoading] = useState(false);

  const selectEntity = useMapStore((s) => s.selectEntity);
  const containerRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!query.trim()) {
      setResults([]);
      setLoading(false);
      return;
    }

    const timer = setTimeout(async () => {
      setLoading(true);
      try {
        const data = await searchWorld(query);
        setResults(data);
      } catch (e) {
        setResults([]);
      } finally {
        setLoading(false);
      }
    }, 250);

    return () => clearTimeout(timer);
  }, [query]);

  // Close dropdown on outside click
  useEffect(() => {
    const handleOutside = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };
    window.addEventListener('mousedown', handleOutside);
    return () => window.removeEventListener('mousedown', handleOutside);
  }, []);

  const handleSelect = (item: SearchResultDto) => {
    onCenterEntity(item.x, item.y);
    if (item.type === 'SETTLEMENT') {
      selectEntity({ type: 'SETTLEMENT', id: parseInt(item.id, 10) });
    } else if (item.type === 'LAIR') {
      selectEntity({ type: 'LAIR', id: item.id });
    } else if (item.type === 'ANOMALY') {
      selectEntity({ type: 'ANOMALY', id: item.id });
    }
    setIsOpen(false);
  };

  const getIcon = (type: string) => {
    switch (type) {
      case 'SETTLEMENT':
        return <MapPin className="w-3.5 h-3.5 text-emerald-400" />;
      case 'LAIR':
        return <Skull className="w-3.5 h-3.5 text-rose-400" />;
      case 'ANOMALY':
        return <Sparkles className="w-3.5 h-3.5 text-cyan-400" />;
      case 'ORGANIZATION':
        return <Users className="w-3.5 h-3.5 text-indigo-400" />;
      default:
        return <User className="w-3.5 h-3.5 text-slate-400" />;
    }
  };

  return (
    <div ref={containerRef} className="mie-search relative w-64 md:w-80 select-none">
      <div className="relative flex items-center">
        <Search className="w-3.5 h-3.5 absolute left-3 text-slate-400 pointer-events-none" />
        <input
          type="text"
          value={query}
          onChange={(e) => {
            setQuery(e.target.value);
            setIsOpen(true);
          }}
          onFocus={() => setIsOpen(true)}
          placeholder="Search settlements, lairs, anomalies..."
          className="mie-search-input w-full bg-slate-900/90 backdrop-blur border border-slate-700/80 rounded-lg pl-8 pr-8 py-1.5 text-xs text-slate-100 placeholder-slate-400 focus:outline-none focus:border-cyan-500 shadow-md font-sans"
        />
        {query && (
          <button
            onClick={() => {
              setQuery('');
              setResults([]);
            }}
            className="absolute right-2.5 text-slate-400 hover:text-slate-200"
          >
            <X className="w-3.5 h-3.5" />
          </button>
        )}
      </div>

      {/* Dropdown Results */}
      {isOpen && (query.trim().length > 0 || loading) && (
        <div className="absolute top-10 left-0 w-full max-h-72 overflow-y-auto bg-slate-900/95 backdrop-blur-md border border-slate-700 rounded-lg shadow-2xl py-1 z-40">
          {loading && <div className="px-3 py-2 text-xs text-slate-400 font-mono">Searching world index...</div>}

          {!loading && results.length === 0 && (
            <div className="px-3 py-2 text-xs text-slate-400">No matching entities found.</div>
          )}

          {!loading &&
            results.map((item) => (
              <div
                key={`${item.type}-${item.id}`}
                onClick={() => handleSelect(item)}
                className="px-3 py-2 hover:bg-slate-800/80 cursor-pointer border-b border-slate-800/50 last:border-none transition flex items-start gap-2.5"
              >
                <div className="mt-0.5">{getIcon(item.type)}</div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-semibold text-slate-200 truncate">{item.name}</span>
                    <span className="text-[10px] font-mono text-slate-400 bg-slate-800 px-1.5 py-0.5 rounded">
                      {item.type}
                    </span>
                  </div>
                  <div className="text-[11px] text-slate-300 truncate">{item.description}</div>
                  <div className="text-[10px] text-slate-400 font-mono mt-0.5 truncate">{item.subInfo}</div>
                </div>
              </div>
            ))}
        </div>
      )}
    </div>
  );
};

