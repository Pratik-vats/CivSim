package civsim.model;

/**
 * Record representing a casualty resulting from a monthly environmental or economic hazard
 * (e.g., famine starvation, winter frost hypothermia).
 */
public record MonthlyCasualty(Person person, String cause) {
    public Person getPerson() { return person; }
    public String getCause() { return cause; }
}
