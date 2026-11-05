package civsim.model;

import java.util.Collection;
import java.util.Random;

/**
 * Phase 2B.5 — Bounded evolutionary trait profile for an individual monster or monster generation.
 * All traits represent bounded percentage/additive modifiers around the species baseline,
 * clamped within [-15.0, +25.0].
 */
public final class MonsterTraitProfile {

    public static final double MIN_TRAIT_VALUE = -15.0;
    public static final double MAX_TRAIT_VALUE = 25.0;

    private final double ferocity;
    private final double naturalArmor;
    private final double speed;
    private final double perception;
    private final double aetherAffinity;
    private final double elementalResistance;
    private final double environmentalAdaptation;
    private final double groupCoordination;

    public MonsterTraitProfile(
            double ferocity,
            double naturalArmor,
            double speed,
            double perception,
            double aetherAffinity,
            double elementalResistance,
            double environmentalAdaptation,
            double groupCoordination) {
        this.ferocity = clamp(ferocity);
        this.naturalArmor = clamp(naturalArmor);
        this.speed = clamp(speed);
        this.perception = clamp(perception);
        this.aetherAffinity = clamp(aetherAffinity);
        this.elementalResistance = clamp(elementalResistance);
        this.environmentalAdaptation = clamp(environmentalAdaptation);
        this.groupCoordination = clamp(groupCoordination);
    }

    public static double clamp(double val) {
        return Math.max(MIN_TRAIT_VALUE, Math.min(MAX_TRAIT_VALUE, val));
    }

    public static MonsterTraitProfile createDefault() {
        return new MonsterTraitProfile(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
    }

    /**
     * Creates a seeded initial generation trait profile with slight random variance in [-5.0, +5.0].
     */
    public static MonsterTraitProfile createRandomInitial(Random rng) {
        if (rng == null) return createDefault();
        return new MonsterTraitProfile(
                (rng.nextDouble() * 10.0) - 5.0,
                (rng.nextDouble() * 10.0) - 5.0,
                (rng.nextDouble() * 10.0) - 5.0,
                (rng.nextDouble() * 10.0) - 5.0,
                (rng.nextDouble() * 10.0) - 5.0,
                (rng.nextDouble() * 10.0) - 5.0,
                (rng.nextDouble() * 6.0) - 3.0,
                (rng.nextDouble() * 10.0) - 5.0
        );
    }

    /**
     * Inherits baseline traits from a collection of surviving parents, applies a seeded mutation roll
     * in [-2.5, +2.5] per trait, and incorporates bounded environmental adaptation drift (<= +0.5).
     */
    public static MonsterTraitProfile inheritFromParents(
            Collection<MonsterTraitProfile> parents,
            Random rng,
            double environmentalDrift) {

        if (parents == null || parents.isEmpty()) {
            return (rng != null) ? createRandomInitial(rng) : createDefault();
        }

        double avgFer = 0.0, avgArm = 0.0, avgSpd = 0.0, avgPer = 0.0;
        double avgAeth = 0.0, avgElem = 0.0, avgEnv = 0.0, avgGrp = 0.0;
        int count = 0;

        for (MonsterTraitProfile p : parents) {
            if (p != null) {
                avgFer += p.ferocity;
                avgArm += p.naturalArmor;
                avgSpd += p.speed;
                avgPer += p.perception;
                avgAeth += p.aetherAffinity;
                avgElem += p.elementalResistance;
                avgEnv += p.environmentalAdaptation;
                avgGrp += p.groupCoordination;
                count++;
            }
        }

        if (count == 0) {
            return (rng != null) ? createRandomInitial(rng) : createDefault();
        }

        avgFer /= count;
        avgArm /= count;
        avgSpd /= count;
        avgPer /= count;
        avgAeth /= count;
        avgElem /= count;
        avgEnv /= count;
        avgGrp /= count;

        double drift = Math.max(0.0, Math.min(0.50, environmentalDrift));

        // Seeded mutation roll in [-2.5, +2.5] (beneficial, neutral, or harmful)
        double mFer = (rng != null) ? ((rng.nextDouble() * 5.0) - 2.5) : 0.0;
        double mArm = (rng != null) ? ((rng.nextDouble() * 5.0) - 2.5) : 0.0;
        double mSpd = (rng != null) ? ((rng.nextDouble() * 5.0) - 2.5) : 0.0;
        double mPer = (rng != null) ? ((rng.nextDouble() * 5.0) - 2.5) : 0.0;
        double mAeth = (rng != null) ? ((rng.nextDouble() * 5.0) - 2.5) : 0.0;
        double mElem = (rng != null) ? ((rng.nextDouble() * 5.0) - 2.5) : 0.0;
        double mEnv = (rng != null) ? ((rng.nextDouble() * 5.0) - 2.5) : 0.0;
        double mGrp = (rng != null) ? ((rng.nextDouble() * 5.0) - 2.5) : 0.0;

        return new MonsterTraitProfile(
                avgFer + mFer,
                avgArm + mArm,
                avgSpd + mSpd,
                avgPer + mPer,
                avgAeth + mAeth,
                avgElem + mElem,
                avgEnv + mEnv + drift,
                avgGrp + mGrp
        );
    }

    public double getFerocity() { return ferocity; }
    public double getNaturalArmor() { return naturalArmor; }
    public double getSpeed() { return speed; }
    public double getPerception() { return perception; }
    public double getAetherAffinity() { return aetherAffinity; }
    public double getElementalResistance() { return elementalResistance; }
    public double getEnvironmentalAdaptation() { return environmentalAdaptation; }
    public double getGroupCoordination() { return groupCoordination; }

    @Override
    public String toString() {
        return String.format("Traits[Fer:%+.1f Arm:%+.1f Spd:%+.1f Per:%+.1f Aeth:%+.1f Elem:%+.1f Env:%+.1f Grp:%+.1f]",
                ferocity, naturalArmor, speed, perception, aetherAffinity, elementalResistance, environmentalAdaptation, groupCoordination);
    }
}

