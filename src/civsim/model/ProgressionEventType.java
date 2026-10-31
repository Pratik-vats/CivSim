package civsim.model;

/**
 * Stage 2A.4 — Categories of explicit progression events.
 * Mastery, Training Progress, and Experience only increase through
 * event-driven submissions — never through passive aging.
 */
public enum ProgressionEventType {
    /** Structured personal practice of the primary ability */
    DELIBERATE_TRAINING("Deliberate Training", 1.5, true, false),

    /** Applying the ability during relevant settlement work */
    OCCUPATIONAL_ABILITY_USE("Occupational Ability Use", 1.0, false, true),

    /** Overcoming a civic, environmental, or technical challenge */
    PROBLEM_SOLVING("Problem Solving", 1.8, false, true),

    /** Surviving a severe disaster, famine, or combat encounter */
    DEMANDING_EVENT_SURVIVAL("Demanding Event Survival", 2.5, false, true);

    private final String displayName;
    private final double baseEventValue;
    private final boolean contributesToTrainingProgress;
    private final boolean contributesToExperience;

    ProgressionEventType(String displayName, double baseEventValue,
                         boolean contributesToTrainingProgress,
                         boolean contributesToExperience) {
        this.displayName = displayName;
        this.baseEventValue = baseEventValue;
        this.contributesToTrainingProgress = contributesToTrainingProgress;
        this.contributesToExperience = contributesToExperience;
    }

    public String getDisplayName() { return displayName; }
    public double getBaseEventValue() { return baseEventValue; }
    public boolean contributesToTrainingProgress() { return contributesToTrainingProgress; }
    public boolean contributesToExperience() { return contributesToExperience; }
}
