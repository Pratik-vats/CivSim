package civsim.model;

/**
 * Stage 2A.4 — Immutable progression event submitted to the mastery system.
 * Each event has a unique eventId to ensure idempotent processing.
 * Duplicate eventIds are rejected by the progression processor.
 */
public final class ProgressionEvent {
    private final String eventId;
    private final int personId;
    private final ProgressionEventType type;
    private final int year;
    private final int month;
    private final double trainingQuality;  // 0.5–2.0 multiplier (environment, tools, mentor quality)

    public ProgressionEvent(String eventId, int personId, ProgressionEventType type,
                            int year, int month, double trainingQuality) {
        if (eventId == null || eventId.isEmpty()) {
            throw new IllegalArgumentException("ProgressionEvent requires a non-empty eventId");
        }
        this.eventId = eventId;
        this.personId = personId;
        this.type = type;
        this.year = year;
        this.month = month;
        this.trainingQuality = Math.max(0.1, Math.min(3.0, trainingQuality));
    }

    public ProgressionEvent(String eventId, int personId, ProgressionEventType type,
                            int year, int month) {
        this(eventId, personId, type, year, month, 1.0);
    }

    public String getEventId() { return eventId; }
    public int getPersonId() { return personId; }
    public ProgressionEventType getType() { return type; }
    public int getYear() { return year; }
    public int getMonth() { return month; }
    public double getTrainingQuality() { return trainingQuality; }

    @Override
    public String toString() {
        return String.format("ProgressionEvent[%s, person=%d, %s, Y%dM%d, quality=%.2f]",
                eventId, personId, type.getDisplayName(), year, month, trainingQuality);
    }
}
