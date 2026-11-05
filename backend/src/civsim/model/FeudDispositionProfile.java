package civsim.model;

import java.util.Random;

/**
 * Phase 2C.2 — Bounded character disposition profile [0–100] governing feud-related decisions.
 * Values are initialized deterministically once per person and remain stable over their lifetime.
 */
public final class FeudDispositionProfile {
    private final double vengeanceDisposition;
    private final double mercyDisposition;
    private final double honorDisposition;
    private final double riskTolerance;
    private final double familyLoyalty;

    public FeudDispositionProfile(
            double vengeanceDisposition,
            double mercyDisposition,
            double honorDisposition,
            double riskTolerance,
            double familyLoyalty) {
        this.vengeanceDisposition = clamp(vengeanceDisposition);
        this.mercyDisposition = clamp(mercyDisposition);
        this.honorDisposition = clamp(honorDisposition);
        this.riskTolerance = clamp(riskTolerance);
        this.familyLoyalty = clamp(familyLoyalty);
    }

    public static FeudDispositionProfile createDeterministic(int personId, long worldSeed) {
        // High-entropy 64-bit mixer for deterministic pseudo-random initialization
        long h = ((long) personId * 0x9E3779B97F4A7C15L) ^ (worldSeed * 0xC6A4A7935BD1E995L);
        Random rng = new Random(h);
        double vengeance = 20.0 + rng.nextDouble() * 60.0;
        double mercy = 20.0 + rng.nextDouble() * 60.0;
        double honor = 20.0 + rng.nextDouble() * 60.0;
        double risk = 15.0 + rng.nextDouble() * 70.0;
        double loyalty = 40.0 + rng.nextDouble() * 55.0;
        return new FeudDispositionProfile(vengeance, mercy, honor, risk, loyalty);
    }

    public static FeudDispositionProfile createPeaceful() {
        return new FeudDispositionProfile(10.0, 85.0, 70.0, 20.0, 60.0);
    }

    public static FeudDispositionProfile createVindictive() {
        return new FeudDispositionProfile(90.0, 10.0, 60.0, 75.0, 90.0);
    }

    private static double clamp(double val) {
        return Math.max(0.0, Math.min(100.0, val));
    }

    public double getVengeanceDisposition() { return vengeanceDisposition; }
    public double getMercyDisposition() { return mercyDisposition; }
    public double getHonorDisposition() { return honorDisposition; }
    public double getRiskTolerance() { return riskTolerance; }
    public double getFamilyLoyalty() { return familyLoyalty; }

    @Override
    public String toString() {
        return String.format("FeudDisposition[V:%.1f, M:%.1f, H:%.1f, R:%.1f, L:%.1f]",
                vengeanceDisposition, mercyDisposition, honorDisposition, riskTolerance, familyLoyalty);
    }
}

