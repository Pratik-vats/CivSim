package civsim.dto;

/**
 * Real-time diagnostic and operational status of the central simulation runner.
 */
public record SimulationStatusDto(
        String worldId,
        String lifecycleState,
        String speed,
        long tickIntervalMillis,
        int currentYear,
        int currentMonth,
        long totalMonths,
        int livingHumans,
        int livingMonsters,
        int activeLairs,
        int pendingCommandQueueSize,
        int pendingQueryQueueSize,
        int activeSseSubscribers,
        double usedHeapMb
) {}

