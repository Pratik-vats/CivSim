package civsim.web.controller;

import civsim.dto.EventDto;
import civsim.dto.PagedResponse;
import civsim.dto.WorldActivityEventDto;
import civsim.runner.SimulationRunner;
import civsim.runner.SnapshotBuilder;
import civsim.web.sse.EventBroadcaster;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Historical event records, world activity feed, and live Server-Sent Events (SSE) notification stream.
 */
@RestController
@RequestMapping
public class EventController {

    private final SimulationRunner runner;
    private final EventBroadcaster broadcaster;

    public EventController(SimulationRunner runner, EventBroadcaster broadcaster) {
        this.runner = runner;
        this.broadcaster = broadcaster;
    }

    @GetMapping("/api/events")
    public ResponseEntity<PagedResponse<EventDto>> listEvents(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "yearMin", required = false) Integer yearMin,
            @RequestParam(name = "yearMax", required = false) Integer yearMax,
            @RequestParam(name = "type", required = false) String type) {

        try {
            List<EventDto> events = runner.submitQuery(engine -> {
                List<EventDto> dtos = new ArrayList<>();
                List<String> raw = engine.getEventHistory();
                int id = 1;
                for (String line : raw) {
                    int year = 0;
                    if (line.startsWith("Year ")) {
                        try {
                            int colonIdx = line.indexOf(':');
                            if (colonIdx > 5) {
                                String yStr = line.substring(5, colonIdx).trim();
                                int parenIdx = yStr.indexOf('(');
                                if (parenIdx > 0) yStr = yStr.substring(0, parenIdx).trim();
                                year = Integer.parseInt(yStr);
                            }
                        } catch (Exception ignored) {}
                    }

                    if (yearMin != null && year < yearMin) continue;
                    if (yearMax != null && year > yearMax) continue;
                    if (type != null && !line.toLowerCase().contains(type.toLowerCase())) continue;

                    dtos.add(new EventDto(id++, year, 0, year * 12L, "GENERAL", line, "INFO"));
                }
                return dtos;
            }).get(3, TimeUnit.SECONDS);

            return ResponseEntity.ok(PagedResponse.of(events, page, size));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping(value = {"/api/activity", "/api/events/activity"})
    public ResponseEntity<PagedResponse<WorldActivityEventDto>> listActivity(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "severity", required = false) String severity,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "entityId", required = false) String entityId,
            @RequestParam(name = "yearMin", required = false) Integer yearMin,
            @RequestParam(name = "yearMax", required = false) Integer yearMax) {

        try {
            List<WorldActivityEventDto> filtered = runner.submitQuery(engine -> {
                List<WorldActivityEventDto> all = SnapshotBuilder.buildWorldActivityEvents(engine);
                List<WorldActivityEventDto> result = new ArrayList<>();

                for (WorldActivityEventDto ev : all) {
                    if (category != null && !category.isBlank() && !category.equalsIgnoreCase("ALL")) {
                        if (!ev.category().equalsIgnoreCase(category)) continue;
                    }
                    if (severity != null && !severity.isBlank() && !severity.equalsIgnoreCase("ALL")) {
                        if (!ev.severity().equalsIgnoreCase(severity)) continue;
                    }
                    if (yearMin != null && ev.year() < yearMin) continue;
                    if (yearMax != null && ev.year() > yearMax) continue;
                    if (entityId != null && !entityId.isBlank()) {
                        boolean match = (ev.primaryEntityId() != null && ev.primaryEntityId().equalsIgnoreCase(entityId))
                                || (ev.relatedEntityIds() != null && ev.relatedEntityIds().stream().anyMatch(r -> r.equalsIgnoreCase(entityId)));
                        if (!match) continue;
                    }
                    if (search != null && !search.isBlank()) {
                        String q = search.toLowerCase();
                        boolean match = ev.title().toLowerCase().contains(q)
                                || ev.summary().toLowerCase().contains(q)
                                || (ev.primaryEntityId() != null && ev.primaryEntityId().toLowerCase().contains(q))
                                || (ev.location() != null && ev.location().toLowerCase().contains(q));
                        if (!match) continue;
                    }
                    result.add(ev);
                }
                return result;
            }).get(3, TimeUnit.SECONDS);

            return ResponseEntity.ok(PagedResponse.of(filtered, page, size));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping(value = {"/api/activity/{id}", "/api/events/{id}"})
    public ResponseEntity<WorldActivityEventDto> getActivityEvent(@PathVariable("id") String id) {
        try {
            return runner.submitQuery(engine -> {
                List<WorldActivityEventDto> all = SnapshotBuilder.buildWorldActivityEvents(engine);
                for (WorldActivityEventDto ev : all) {
                    if (ev.id().equalsIgnoreCase(id) || ev.id().endsWith("_" + id)) {
                        return ResponseEntity.ok(ev);
                    }
                }
                return ResponseEntity.notFound().<WorldActivityEventDto>build();
            }).get(3, TimeUnit.SECONDS);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping(value = "/api/events/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeLiveEvents(@RequestHeader(value = "Last-Event-ID", required = false) String lastEventId) {
        return broadcaster.subscribe(lastEventId);
    }
}

