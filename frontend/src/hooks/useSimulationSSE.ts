import { useEffect, useRef, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useMapStore } from '../store/useMapStore';

export interface SSEState {
  connected: boolean;
}

export function useSimulationSSE() {
  const queryClient = useQueryClient();
  const selectedEntityRef = useRef(useMapStore.getState().selectedEntity);

  const [state, setState] = useState<SSEState>({
    connected: false,
  });

  useEffect(() => {
    const unsubscribe = useMapStore.subscribe((storeState) => {
      selectedEntityRef.current = storeState.selectedEntity;
    });
    let eventSource: EventSource | null = null;
    let reconnectTimeout: ReturnType<typeof setTimeout> | null = null;

    function connect() {
      eventSource = new EventSource('/api/events/stream');

      eventSource.onopen = () => {
        setState((prev) => ({ ...prev, connected: true }));
      };

      eventSource.addEventListener('tick-completed', () => {
        queryClient.invalidateQueries({ queryKey: ['world-summary'] });
        queryClient.invalidateQueries({ queryKey: ['world-map'] });
        queryClient.invalidateQueries({ queryKey: ['simulation-status'] });

        const selectedEntity = selectedEntityRef.current;
        if (selectedEntity?.type === 'SETTLEMENT') {
          queryClient.invalidateQueries({ queryKey: ['settlement-detail', selectedEntity.id] });
        }
      });

      eventSource.addEventListener('simulation-status-changed', () => {
        queryClient.invalidateQueries({ queryKey: ['simulation-status'] });
        queryClient.invalidateQueries({ queryKey: ['world-summary'] });
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
      unsubscribe();
    };
  }, [queryClient]);

  return state;
}

