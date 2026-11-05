package civsim.web.sse;

import civsim.dto.EventDto;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe, non-blocking Server-Sent Events (SSE) broadcaster.
 * Dispatches lightweight event notifications to connected frontend clients asynchronously.
 * Enforces bounded subscriber limits, bounded buffers, reconnection replay, and cleanup.
 */
@Service
public class EventBroadcaster {

    public static final int MAX_SUBSCRIBERS = 50;
    public static final int MAX_BUFFERED_EVENTS = 100;
    public static final int MAX_EVENT_BUFFER_SIZE = MAX_BUFFERED_EVENTS;
    public static final long EMITTER_TIMEOUT_MILLIS = 300_000L; // 5 minutes

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final Map<Long, SseEventRecord> eventRingBuffer = Collections.synchronizedMap(
            new LinkedHashMap<Long, SseEventRecord>(MAX_BUFFERED_EVENTS + 1, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Long, SseEventRecord> eldest) {
                    return size() > MAX_BUFFERED_EVENTS;
                }
            }
    );

    private final AtomicLong nextEventSequence = new AtomicLong(1);

    private final ThreadPoolExecutor dispatchExecutor = new ThreadPoolExecutor(
            1, 2, 60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(128),
            new ThreadPoolExecutor.DiscardOldestPolicy()
    );

    public record SseEventRecord(long id, String eventName, Map<String, Object> data) {}

    public SseEmitter subscribe(String lastEventIdHeader) {
        if (emitters.size() >= MAX_SUBSCRIBERS) {
            throw new IllegalStateException("Maximum SSE subscriber capacity reached (" + MAX_SUBSCRIBERS + ")");
        }

        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MILLIS);
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            emitter.complete();
        });
        emitter.onError(e -> emitters.remove(emitter));

        // Reconnection replay support
        if (lastEventIdHeader != null && !lastEventIdHeader.isBlank()) {
            try {
                long lastId = Long.parseLong(lastEventIdHeader.trim());
                dispatchExecutor.submit(() -> replayMissedEvents(emitter, lastId));
            } catch (NumberFormatException ignored) {}
        }

        return emitter;
    }

    public void broadcastTickCompleted(String worldId, long tickId, long version, int year, int month, String status) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("worldId", worldId);
        data.put("tickId", tickId);
        data.put("snapshotVersion", version);
        data.put("year", year);
        data.put("month", month);
        data.put("simulationStatus", status);

        broadcast("tick-completed", data);
    }

    public void broadcastStatusChanged(String worldId, String oldStatus, String newStatus) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("worldId", worldId);
        data.put("previousStatus", oldStatus);
        data.put("newStatus", newStatus);

        broadcast("simulation-status-changed", data);
    }

    public void broadcastMajorEvent(String worldId, String eventType, String summary) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("worldId", worldId);
        data.put("type", eventType);
        data.put("summary", summary);

        broadcast("major-world-event", data);
    }

    public void broadcastSnapshotUpdated(String worldId, long version) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("worldId", worldId);
        data.put("snapshotVersion", version);

        broadcast("world-snapshot-updated", data);
    }

    private void broadcast(String eventName, Map<String, Object> data) {
        long eventId = nextEventSequence.getAndIncrement();
        SseEventRecord record = new SseEventRecord(eventId, eventName, data);
        eventRingBuffer.put(eventId, record);

        if (emitters.isEmpty()) return;

        dispatchExecutor.submit(() -> {
            List<SseEmitter> deadEmitters = new ArrayList<>();
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .id(String.valueOf(eventId))
                            .name(eventName)
                            .data(data));
                } catch (Exception ex) {
                    deadEmitters.add(emitter);
                }
            }
            emitters.removeAll(deadEmitters);
        });
    }

    private void replayMissedEvents(SseEmitter emitter, long lastEventId) {
        synchronized (eventRingBuffer) {
            if (eventRingBuffer.isEmpty()) return;
            Long oldestRetained = eventRingBuffer.keySet().iterator().next();
            if (lastEventId < oldestRetained) {
                // Client is too far behind buffer: send resync-required instruction
                try {
                    emitter.send(SseEmitter.event()
                            .name("resync-required")
                            .data(Map.of("message", "Missed events outside replay window. Fetch /api/world/snapshot")));
                } catch (Exception ignored) {}
                return;
            }

            for (Map.Entry<Long, SseEventRecord> entry : eventRingBuffer.entrySet()) {
                if (entry.getKey() > lastEventId) {
                    try {
                        SseEventRecord rec = entry.getValue();
                        emitter.send(SseEmitter.event()
                                .id(String.valueOf(rec.id()))
                                .name(rec.eventName())
                                .data(rec.data()));
                    } catch (Exception e) {
                        emitters.remove(emitter);
                        break;
                    }
                }
            }
        }
    }

    public int getActiveSubscriberCount() {
        return emitters.size();
    }

    public int getSubscriberCount() {
        return getActiveSubscriberCount();
    }

    public int getBroadcastHistorySize() {
        return eventRingBuffer.size();
    }

    public void broadcast(EventDto event) {
        if (event == null) return;
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("eventId", event.eventId());
        data.put("year", event.year());
        data.put("month", event.month());
        data.put("totalMonths", event.totalMonths());
        data.put("category", event.category());
        data.put("summary", event.summary());
        data.put("severity", event.severity());
        broadcast(event.category(), data);
    }

    public void shutdown() {
        closeAll();
    }

    public void closeAll() {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.complete();
            } catch (Exception ignored) {}
        }
        emitters.clear();
        eventRingBuffer.clear();
        dispatchExecutor.shutdownNow();
    }
}
