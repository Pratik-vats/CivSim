package civsim.web.controller;

import civsim.dto.*;
import civsim.persistence.WorldPersistenceService;
import civsim.runner.SimulationRunner;
import civsim.runner.SnapshotBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

/**
 * Historical time-travel, historical map, and chronology exploration endpoints.
 * All historical queries are strictly read-only and never mutate or rewind the active simulation.
 */
@RestController
@RequestMapping("/api/historical")
public class HistoricalController {

    private final SimulationRunner runner;
    private final WorldPersistenceService persistenceService;

    public HistoricalController(SimulationRunner runner, WorldPersistenceService persistenceService) {
        this.runner = runner;
        this.persistenceService = persistenceService;
    }

    @GetMapping("/timeline")
    public ResponseEntity<HistoricalTimelineDto> getTimeline() {
        try {
            HistoricalTimelineDto dto = runner.submitQuery(engine ->
                    SnapshotBuilder.buildHistoricalTimeline(engine, runner.getWorldId(), persistenceService)
            ).get(2, TimeUnit.SECONDS);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/summary")
    public ResponseEntity<HistoricalWorldSummaryDto> getHistoricalSummary(
            @RequestParam(name = "year", defaultValue = "0") int year,
            @RequestParam(name = "month", defaultValue = "1") int month) {
        try {
            HistoricalWorldSummaryDto dto = runner.submitQuery(engine ->
                    SnapshotBuilder.buildHistoricalWorldSummary(engine, year, month, runner.getWorldId())
            ).get(2, TimeUnit.SECONDS);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/map")
    public ResponseEntity<HistoricalMapSnapshotDto> getHistoricalMap(
            @RequestParam(name = "year", defaultValue = "0") int year,
            @RequestParam(name = "month", defaultValue = "1") int month) {
        try {
            HistoricalMapSnapshotDto dto = runner.submitQuery(engine ->
                    SnapshotBuilder.buildHistoricalMapSnapshot(engine, year, month, runner.getWorldId())
            ).get(2, TimeUnit.SECONDS);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/settlements/{id}/history")
    public ResponseEntity<SettlementHistoryDto> getSettlementHistory(@PathVariable("id") int settlementId) {
        try {
            SettlementHistoryDto dto = runner.submitQuery(engine ->
                    SnapshotBuilder.buildSettlementHistory(engine, settlementId)
            ).get(2, TimeUnit.SECONDS);
            return (dto != null) ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/people/{id}")
    public ResponseEntity<PersonDetailDto> getHistoricalPerson(
            @PathVariable("id") int personId,
            @RequestParam(name = "year", defaultValue = "0") int year) {
        try {
            PersonDetailDto dto = runner.submitQuery(engine ->
                    SnapshotBuilder.buildHistoricalPersonProfile(engine, personId, year)
            ).get(2, TimeUnit.SECONDS);
            return (dto != null) ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/compare/dates")
    public ResponseEntity<HistoricalDateComparisonDto> compareDates(
            @RequestParam(name = "year1", defaultValue = "0") int year1,
            @RequestParam(name = "year2", defaultValue = "1") int year2) {
        try {
            HistoricalDateComparisonDto dto = runner.submitQuery(engine ->
                    SnapshotBuilder.buildHistoricalDateComparison(engine, year1, year2, runner.getWorldId())
            ).get(2, TimeUnit.SECONDS);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

