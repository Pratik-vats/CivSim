import { useEffect, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useMapStore } from '../store/useMapStore';

export interface SSEState {
  connected: boolean;
  lastTick: number | null;
  lastYear: number | null;
  lastMonth: number | null;
  notification: string | null;
}

export function useSimulationSSE() {
  const queryClient = useQueryClient();
  const setIsAwakenedBackend = useMapStore((s) => s.setIsAwakenedBackend);
  const selectedEntity = useMapStore((s) => s.selectedEntity);

  const [state, setState] = useState<SSEState>({
    connected: false,
    lastTick: null,
    lastYear: null,
    lastMonth: null,
    notification: null,
  });

  useEffect(() => {
    let eventSource: EventSource | null = null;
    let reconnectTimeout: any = null;

    function connect() {
      eventSource = new EventSource('/api/events/stream');

      eventSource.onopen = () => {
        setState((prev) => ({ ...prev, connected: true }));
      };

      eventSource.addEventListener('tick-completed', (e: MessageEvent) => {
        try {
          const data = JSON.parse(e.data);
          setState((prev) => ({
            ...prev,
            lastTick: data.tickId,
            lastYear: data.year,
            lastMonth: data.month,
          }));

          // Invalidate relevant server-state queries
          queryClient.invalidateQueries({ queryKey: ['world-summary'] });
          queryClient.invalidateQueries({ queryKey: ['world-map'] });
          queryClient.invalidateQueries({ queryKey: ['simulation-status'] });

          if (selectedEntity && selectedEntity.type === 'SETTLEMENT') {
            queryClient.invalidateQueries({
              queryKey: ['settlement-detail', selectedEntity.id],
            });
          }
        } catch (err) {
          console.error('Failed to parse tick-completed SSE:', err);
        }
      });

      eventSource.addEventListener('simulation-status-changed', (e: MessageEvent) => {
        try {
          const data = JSON.parse(e.data);
          queryClient.invalidateQueries({ queryKey: ['simulation-status'] });
          queryClient.invalidateQueries({ queryKey: ['world-summary'] });
        } catch (err) {
          console.error('Failed to parse status SSE:', err);
        }
      });

      eventSource.addEventListener('major-world-event', (e: MessageEvent) => {
        try {
          const data = JSON.parse(e.data);
          setState((prev) => ({
            ...prev,
            notification: `[${data.type}] ${data.summary}`,
          }));
        } catch (err) {
          console.error('Failed to parse major-world-event SSE:', err);
        }
      });

      eventSource.addEventListener('world-snapshot-updated', () => {
        queryClient.invalidateQueries({ queryKey: ['world-map'] });
      });

      eventSource.addEventListener('resync-required', () => {
        queryClient.invalidateQueries();
      });

      eventSource.onerror = () => {
        setState((prev) => ({ ...prev, connected: false }));
        if (eventSource) {
          eventSource.close();
          eventSource = null;
        }
        // Attempt reconnection after 3 seconds
        reconnectTimeout = setTimeout(connect, 3000);
      };
    }

    connect();

    return () => {
      if (eventSource) {
        eventSource.close();
        eventSource = null;
      }
      if (reconnectTimeout) {
        clearTimeout(reconnectTimeout);
      }
    };
  }, [queryClient, selectedEntity, setIsAwakenedBackend]);

  return state;
}

