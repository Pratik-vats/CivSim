package civsim.model;

import java.util.Objects;

/**
 * Stage 2A.6 — Explicit, idempotent focused attribute training event.
 * Represents a dedicated drill or conditioning session targeting a single core attribute.
 */
public final class AttributeTrainingEvent {

    private final String eventId;
    private final int personId;
    private final AttributeType targetAttribute;
    private final double trainingQuality;   // bounded in [0.50, 1.50]
    private final double baseTrainingValue; // default 0.30
    private final int simulationYear;
    private final int simulationMonth;

    public AttributeTrainingEvent(String eventId, int personId, AttributeType targetAttribute,
                                  double trainingQuality, double baseTrainingValue,
                                  int simulationYear, int simulationMonth) {
        this.eventId = Objects.requireNonNull(eventId, "eventId must not be null");
        this.personId = personId;
        this.targetAttribute = Objects.requireNonNull(targetAttribute, "targetAttribute must not be null");
        this.trainingQuality = Math.max(0.50, Math.min(1.50, trainingQuality));
        this.baseTrainingValue = Math.max(0.01, baseTrainingValue);
        this.simulationYear = simulationYear;
        this.simulationMonth = simulationMonth;
    }

    public AttributeTrainingEvent(String eventId, int personId, AttributeType targetAttribute,
                                  double trainingQuality, int simulationYear, int simulationMonth) {
        this(eventId, personId, targetAttribute, trainingQuality, 0.30, simulationYear, simulationMonth);
    }

    public String getEventId() {
        return eventId;
    }

    public int getPersonId() {
        return personId;
    }

    public AttributeType getTargetAttribute() {
        return targetAttribute;
    }

    public double getTrainingQuality() {
        return trainingQuality;
    }

    public double getBaseTrainingValue() {
        return baseTrainingValue;
    }

    public int getSimulationYear() {
        return simulationYear;
    }

    public int getSimulationMonth() {
        return simulationMonth;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AttributeTrainingEvent)) return false;
        AttributeTrainingEvent that = (AttributeTrainingEvent) o;
        return eventId.equals(that.eventId);
    }

    @Override
    public int hashCode() {
        return eventId.hashCode();
    }

    @Override
    public String toString() {
        return String.format("AttributeTrainingEvent[%s, pId=%d, %s, q=%.2f, val=%.2f, Y%d-M%d]",
                eventId, personId, targetAttribute.getDisplayName(), trainingQuality,
                baseTrainingValue, simulationYear, simulationMonth);
    }
}
