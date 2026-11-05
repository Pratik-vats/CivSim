package civsim.systems;

import civsim.ProgressionConfig;
import civsim.model.AetherProfile;
import civsim.model.DerivedPowerProfile;
import civsim.model.Person;
import civsim.model.SupernaturalClass;
import civsim.model.SupernaturalProgressionRecord;

/**
 * Stage 2A.4 — Centralized dynamic power assessment calculator.
 * Computes a character's DerivedPowerProfile from their live state.
 *
 * Uses Effective Attributes (Base + AwakeningBonus + Temporary), Current Tier
 * (via configurable tier potency multiplier), Current Mastery (0.50x at 0 to 1.50x at 100),
 * and Class-specific weighting profiles.
 */
public class PowerAssessmentCalculator {

    /**
     * Compute a character's dynamic power profile from their current live state.
     *
     * @param person the awakened person
     * @param config progression configuration (for tier potency multipliers)
     * @return computed DerivedPowerProfile, or null if person is not awakened
     */
    public static DerivedPowerProfile compute(Person person, ProgressionConfig config) {
        AetherProfile ap = person.getAetherProfile();
        if (ap == null || !ap.isAwakened()) return null;

        SupernaturalProgressionRecord record = ap.getProgressionRecord();
        SupernaturalClass cls = ap.getSupernaturalClass();

        // Effective attributes
        double str = ap.getEffectiveStrength();
        double intel = ap.getEffectiveIntelligence();
        double end = ap.getEffectiveEndurance();
        double wis = ap.getEffectiveWisdom();
        double per = ap.getEffectivePerception();
        double aet = ap.getEffectiveAether();

        // Tier potency multiplier
        double tierMult = config.getTierPotencyMultiplier(ap.getPowerTier());

        // Mastery efficiency: 0.50 at mastery=0, 1.50 at mastery=100
        double mastery = (record != null) ? record.getMastery() : 0.0;
        double masteryMult = 0.50 + (mastery / 100.0);

        // Combined multiplier
        double globalMult = tierMult * masteryMult;

        // Class-specific sub-score computation
        double offensive, defensive, mobility, perception, sustain;

        switch (cls) {
            case VANGUARD:
                offensive = (str * 0.50 + end * 0.25 + aet * 0.15 + wis * 0.10) * globalMult;
                defensive = (end * 0.40 + str * 0.30 + wis * 0.20 + aet * 0.10) * globalMult;
                mobility = (str * 0.30 + end * 0.30 + per * 0.20 + aet * 0.20) * globalMult * 0.7;
                perception = (per * 0.50 + wis * 0.30 + intel * 0.20) * globalMult * 0.5;
                sustain = (end * 0.50 + str * 0.20 + wis * 0.20 + aet * 0.10) * globalMult;
                break;

            case ELEMENTALIST:
                offensive = (aet * 0.45 + intel * 0.30 + per * 0.15 + wis * 0.10) * globalMult * 1.2;
                defensive = (end * 0.30 + wis * 0.30 + aet * 0.25 + intel * 0.15) * globalMult * 0.6;
                mobility = (per * 0.30 + intel * 0.25 + aet * 0.25 + end * 0.20) * globalMult * 0.6;
                perception = (intel * 0.40 + per * 0.35 + aet * 0.25) * globalMult * 0.8;
                sustain = (aet * 0.40 + end * 0.30 + wis * 0.20 + intel * 0.10) * globalMult * 0.8;
                break;

            case VITALIST:
                offensive = (aet * 0.30 + wis * 0.30 + intel * 0.25 + per * 0.15) * globalMult * 0.4;
                defensive = (aet * 0.35 + end * 0.30 + wis * 0.25 + str * 0.10) * globalMult * 1.1;
                mobility = (end * 0.30 + per * 0.30 + wis * 0.20 + aet * 0.20) * globalMult * 0.5;
                perception = (per * 0.40 + wis * 0.35 + aet * 0.25) * globalMult * 0.7;
                sustain = (aet * 0.40 + wis * 0.30 + end * 0.20 + intel * 0.10) * globalMult * 1.3;
                break;

            case WARDEN:
                offensive = (str * 0.30 + aet * 0.30 + end * 0.25 + wis * 0.15) * globalMult * 0.5;
                defensive = (end * 0.35 + aet * 0.30 + wis * 0.25 + str * 0.10) * globalMult * 1.4;
                mobility = (end * 0.30 + str * 0.25 + per * 0.25 + aet * 0.20) * globalMult * 0.4;
                perception = (per * 0.40 + wis * 0.35 + aet * 0.25) * globalMult * 0.6;
                sustain = (end * 0.40 + aet * 0.30 + wis * 0.20 + str * 0.10) * globalMult * 1.2;
                break;

            case SEER:
                offensive = (intel * 0.35 + per * 0.30 + aet * 0.25 + wis * 0.10) * globalMult * 0.5;
                defensive = (wis * 0.35 + per * 0.30 + end * 0.20 + aet * 0.15) * globalMult * 0.7;
                mobility = (per * 0.35 + intel * 0.25 + aet * 0.20 + end * 0.20) * globalMult * 0.7;
                perception = (per * 0.45 + intel * 0.30 + aet * 0.25) * globalMult * 1.5;
                sustain = (wis * 0.35 + per * 0.25 + aet * 0.25 + end * 0.15) * globalMult * 0.8;
                break;

            case BEASTBINDER:
                offensive = (wis * 0.30 + aet * 0.30 + str * 0.25 + per * 0.15) * globalMult * 0.8;
                defensive = (end * 0.30 + wis * 0.30 + aet * 0.25 + per * 0.15) * globalMult * 0.8;
                mobility = (per * 0.35 + wis * 0.25 + aet * 0.25 + end * 0.15) * globalMult * 0.9;
                perception = (per * 0.40 + wis * 0.35 + aet * 0.25) * globalMult * 1.1;
                sustain = (wis * 0.35 + aet * 0.30 + end * 0.25 + per * 0.10) * globalMult * 0.9;
                break;

            case RIFTWALKER:
                offensive = (aet * 0.40 + intel * 0.30 + per * 0.20 + wis * 0.10) * globalMult * 0.9;
                defensive = (aet * 0.30 + per * 0.25 + end * 0.25 + wis * 0.20) * globalMult * 0.6;
                mobility = (aet * 0.40 + per * 0.30 + intel * 0.20 + end * 0.10) * globalMult * 1.5;
                perception = (per * 0.40 + aet * 0.30 + intel * 0.30) * globalMult * 1.0;
                sustain = (aet * 0.40 + end * 0.25 + wis * 0.20 + intel * 0.15) * globalMult * 0.7;
                break;

            default:
                offensive = defensive = mobility = perception = sustain = 0.0;
        }

        double overall = offensive * 0.25 + defensive * 0.25 + mobility * 0.15
                + perception * 0.15 + sustain * 0.20;

        return new DerivedPowerProfile(overall, offensive, defensive, mobility, perception, sustain);
    }
}
