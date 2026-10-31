package civsim;

public class Main {
    public static void main(String[] args) {
        // Default configuration:
        // - 2 villages ("Village A", "Village B") with 20 people each (40 total)
        // - 1000ms (1 real second) per simulated month
        // - Death at age 75, marriage eligibility at age > 18 (19+)
        // - 2 to 5 planned children per couple, minimum 12 simulated months after marriage and between births
        SimulationConfig config = SimulationConfig.createDefault();

        SimulationEngine engine = new SimulationEngine(config);

        // Ensure clean shutdown if the user presses Ctrl+C
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (engine.isRunning()) {
                engine.stop();
            }
        }));

        // Initialize the world, print initial stats, match Year 0 marriages, and start background ticks
        engine.initializeWorld();
        engine.start();
    }
}