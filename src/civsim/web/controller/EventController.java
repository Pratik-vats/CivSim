package civsim.web.controller;

import civsim.dto.EventDto;
import civsim.dto.PagedResponse;
import civsim.runner.SimulationRunner;
import civsim.web.sse.EventBroadcaster;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Historical event records and live Server-Sent Events (SSE) notification stream.
 */
@RestController
@RequestMapping("/api/events")
public class EventController {

    private final SimulationRunner runner;
    private final EventBroadcaster broadcaster;

    public EventController(SimulationRunner runner, EventBroadcaster broadcaster) {
        this.runner = runner;
        this.broadcaster = broadcaster;
    }

    @GetMapping
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
                    // Extract Year if line begins with Year X:
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

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeLiveEvents(@RequestHeader(value = "Last-Event-ID", required = false) String lastEventId) {
        return broadcaster.subscribe(lastEventId);
    }
}

