package civsim.systems;

import civsim.ProgressionConfig;
import civsim.SimulationConfig;
import civsim.model.AetherAllele;
import civsim.model.AetherProfile;
import civsim.model.AwakeningPathway;
import civsim.model.ClassRarity;
import civsim.model.CombatState;
import civsim.model.Occupation;
import civsim.model.Person;
import civsim.model.SupernaturalClass;
import civsim.model.SupernaturalProgressionRecord;
import civsim.model.Village;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;

/**
 * Manages all supernatural awakening logic for the post-Great Mutation era.
 *
 * Pre-Year 50: Seeds Aether alleles on all existing persons. Zero awakenings.
 * Year 50: Triggers The Great Mutation event chronicle and begins initial seeding.
 * Year 50+: Annual awakening checks for all eligible living persons.
 *
 * Awakening Pathways:
 *   - NORMAL: Ages 16–25, bell-curve probability peaked at age 20, Luck modifier
 *   - PRODIGY: Ages 6–15, very rare (0.5% base chance), multiplied by DD genotype
 *   - CRIMSON_CRUCIBLE: Ages 12+, triggered by near-death (famine survival, raid survivor, disaster)
 *   - TWILIGHT_EPIPHANY: Ages 50–74, occupational/leadership tenure bonus
 *   - DARK_AWAKENING: Ages 12+, when Greed + DarkResonance >= 1.2 threshold
 *
 * Global constraints:
 *   - Max 2 living Mythic-rarity beings at any time
 *   - Annual Greed/DarkResonance drift for all awakening-era citizens
 */
public class AwakeningSystem {

    /** Maximum number of living Mythic-rarity awakened beings globally */
    private static final int MAX_MYTHIC_CAP = 2;

    /** Base Normal awakening probability at peak age (20) for Dr genotype */
    private static final double NORMAL_BASE_PROBABILITY = 0.035;

    /** DD genotype multiplier for awakening probability */
    private static final double DD_MULTIPLIER = 2.0;

    /** Prodigy base probability per year (before genotype modifier) */
    private static final double PRODIGY_BASE_PROBABILITY = 0.005;

    /** Crimson Crucible base probability when trauma qualifier is met */
    private static final double CRIMSON_BASE_PROBABILITY = 0.12;

    /** Twilight Epiphany base probability per year for qualifying elders */
    private static final double TWILIGHT_BASE_PROBABILITY = 0.015;

    /** Dark Awakening probability when Greed + DarkResonance threshold is met */
    private static final double DARK_AWAKENING_PROBABILITY = 0.08;

    private boolean greatMutationTriggered = false;

    /**
     * Seeds Aether alleles on all existing persons who don't yet have an AetherProfile.
     * Called once at Year 50 (The Great Mutation) for the existing population,
     * and ongoing for any person born without a profile (edge case safety).
     *
     * Initial seeding distribution:
     *   - 25% DD (homozygous dominant — strong potential)
     *   - 45% Dr (heterozygous — viable potential)
     *   - 30% rr (homozygous recessive — dormant)
     */
    public void seedAetherOnExistingPopulation(List<Person> allPeople, Random random) {
        for (Person person : allPeople) {
            if (!person.isAlive()) continue;
            if (person.getAetherProfile() != null) continue;

            double roll = random.nextDouble();
            AetherAllele a1, a2;
            if (roll < 0.25) {
                a1 = AetherAllele.DOMINANT;
                a2 = AetherAllele.DOMINANT;
            } else if (roll < 0.70) {
                a1 = AetherAllele.DOMINANT;
                a2 = AetherAllele.RECESSIVE;
            } else {
                a1 = AetherAllele.RECESSIVE;
                a2 = AetherAllele.RECESSIVE;
            }

            AetherProfile profile = new AetherProfile(a1, a2, random);
            person.setAetherProfile(profile);
        }
    }

    /**
     * Triggers The Great Mutation at Year 50. This is a one-time global event that:
     * 1. Seeds Aether alleles on all living persons
     * 2. Records the chronicle narrative event
     * 3. Flips the era flag to enable subsequent awakening processing
     */
    public void triggerGreatMutation(List<Person> allPeople, int currentYear, Random random,
                                     SimulationConfig config, BiConsumer<String, Boolean> eventRecorder) {
        if (greatMutationTriggered) return;
        greatMutationTriggered = true;

        // Seed all living persons with Aether genotypes
        seedAetherOnExistingPopulation(allPeople, random);

        // Count genotype distribution for chronicle
        int ddCount = 0, drCount = 0, rrCount = 0, totalLiving = 0;
        for (Person p : allPeople) {
            if (!p.isAlive()) continue;
            totalLiving++;
            AetherProfile ap = p.getAetherProfile();
            if (ap != null) {
                String geno = ap.getGenotypeString();
                switch (geno) {
                    case "DD": ddCount++; break;
                    case "Dr": drCount++; break;
                    case "rr": rrCount++; break;
                }
            }
        }

        // Chronicle entry
        String chronicle = String.format(
                "═══════════════════════════════════════════════════════════════════════════════\n" +
                "  ╔═══════════════════════════════════════════════════════════════════════╗\n" +
                "  ║            T H E   G R E A T   M U T A T I O N   —   Year %d         ║\n" +
                "  ╚═══════════════════════════════════════════════════════════════════════╝\n" +
                "  A cataclysmic wave of energy sweeps across the world. Flora mutates,\n" +
                "  fauna transforms into strange beasts, and deep within humanity's blood,\n" +
                "  an ancient potential stirs — the Aether awakens.\n" +
                "\n" +
                "  Population at Mutation: %d souls\n" +
                "  Aether Genotype Distribution:\n" +
                "    DD (Strong Potential):  %d (%.1f%%)\n" +
                "    Dr (Viable Potential):  %d (%.1f%%)\n" +
                "    rr (Dormant):           %d (%.1f%%)\n" +
                "  Awakening-Viable: %d of %d (%.1f%%)\n" +
                "\n" +
                "  From this day forward, those who carry the Dominant allele may awaken\n" +
                "  supernatural powers — as Vanguards, Elementalists, Vitalists, Wardens,\n" +
                "  Seers, Beastbinders, or Riftwalkers.\n" +
                "\n" +
                "  But beware: Greed and Dark Resonance fester in those who fail or covet,\n" +
                "  and the Dark Awakening lurks for the corrupted...\n" +
                "═══════════════════════════════════════════════════════════════════════════════",
                currentYear,
                totalLiving,
                ddCount, (totalLiving > 0 ? (100.0 * ddCount / totalLiving) : 0.0),
                drCount, (totalLiving > 0 ? (100.0 * drCount / totalLiving) : 0.0),
                rrCount, (totalLiving > 0 ? (100.0 * rrCount / totalLiving) : 0.0),
                (ddCount + drCount), totalLiving,
                (totalLiving > 0 ? (100.0 * (ddCount + drCount) / totalLiving) : 0.0)
        );

        // Always print the Great Mutation chronicle regardless of config
        if (eventRecorder != null) {
            eventRecorder.accept(chronicle, true);
        }
    }

    /**
     * Processes annual awakening checks for all eligible living persons.
     * Only runs in the Awakened Era (year >= awakeningStartYear, after Great Mutation triggered).
     *
     * @return summary of this year's awakening results
     */
    public AwakeningYearSummary processAnnualAwakenings(
            List<Person> allPeople,
            List<Village> villages,
            int currentYear,
            Random random,
            SimulationConfig config,
            ProgressionConfig progConfig,
            BiConsumer<String, Boolean> eventRecorder) {

        if (!greatMutationTriggered) {
            return new AwakeningYearSummary();
        }

        // Count living Mythic beings for the global cap
        int livingMythicCount = countLivingMythic(allPeople);

        // Track this year's results
        AwakeningYearSummary summary = new AwakeningYearSummary();

        // Process Greed/DarkResonance drift for all living persons
        processGreedAndDarkDrift(allPeople, random);

        // Check each living, un-awakened person for awakening
        for (Person person : allPeople) {
            if (!person.isAlive()) continue;
            AetherProfile profile = person.getAetherProfile();
            if (profile == null || profile.isAwakened()) continue;
            if (!profile.hasAwakeningPotential()) continue;

            int age = person.getAge();

            // Try each pathway in priority order
            AwakeningPathway selectedPathway = null;
            double checkProbability = 0.0;

            // 1. Dark Awakening check (highest priority if qualified)
            if (profile.qualifiesForDarkAwakening(age)) {
                checkProbability = DARK_AWAKENING_PROBABILITY;
                if (random.nextDouble() < checkProbability) {
                    selectedPathway = AwakeningPathway.DARK_AWAKENING;
                }
            }

            // 2. Crimson Crucible — triggered by trauma (famine survival, disaster, raid)
            if (selectedPathway == null && AwakeningPathway.CRIMSON_CRUCIBLE.isInAgeRange(age)) {
                if (hasTraumaQualifier(person)) {
                    checkProbability = CRIMSON_BASE_PROBABILITY;
                    double genoMult = profile.isHomozygousDominant() ? DD_MULTIPLIER : 1.0;
                    if (random.nextDouble() < checkProbability * genoMult) {
                        selectedPathway = AwakeningPathway.CRIMSON_CRUCIBLE;
                    }
                }
            }

            // 3. Prodigy — ages 6–15, very rare
            if (selectedPathway == null && AwakeningPathway.PRODIGY.isInAgeRange(age)) {
                checkProbability = PRODIGY_BASE_PROBABILITY;
                double genoMult = profile.isHomozygousDominant() ? DD_MULTIPLIER * 1.5 : 1.0;
                if (random.nextDouble() < checkProbability * genoMult) {
                    selectedPathway = AwakeningPathway.PRODIGY;
                }
            }

            // 4. Normal — ages 16–25, bell curve peaked at 20, Luck modifier
            if (selectedPathway == null && AwakeningPathway.NORMAL.isInAgeRange(age)) {
                checkProbability = calculateNormalAwakeningProbability(age, profile);
                if (random.nextDouble() < checkProbability) {
                    selectedPathway = AwakeningPathway.NORMAL;
                }
            }

            // 5. Twilight Epiphany — ages 50–74, occupational/leadership tenure
            if (selectedPathway == null && AwakeningPathway.TWILIGHT_EPIPHANY.isInAgeRange(age)) {
                checkProbability = calculateTwilightProbability(age, person, profile);
                if (random.nextDouble() < checkProbability) {
                    selectedPathway = AwakeningPathway.TWILIGHT_EPIPHANY;
                }
            }

            // If no pathway triggered, apply failed-check Greed drift (small amount)
            if (selectedPathway == null) {
                if (AwakeningPathway.NORMAL.isInAgeRange(age) && profile.hasAwakeningPotential()) {
                    // Failed normal awakening check → slight greed/frustration
                    profile.driftGreed(0.01);
                    if (profile.getGreed() > 0.5) {
                        profile.driftDarkResonance(0.005);
                    }
                }
                continue;
            }

            // === AWAKENING TRIGGERED ===
            // Get parental classes for hereditary affinity
            SupernaturalClass motherClass = getParentClass(person.getMother());
            SupernaturalClass fatherClass = getParentClass(person.getFather());

            profile.awaken(selectedPathway, currentYear, age, random, motherClass, fatherClass,
                    livingMythicCount, MAX_MYTHIC_CAP);

            // Update Mythic count if this person rolled Mythic
            if (profile.getRarity() == ClassRarity.MYTHIC) {
                livingMythicCount++;
            }

            // Stage 2A.4: Compute and cache aptitude modifier
            if (progConfig != null) {
                SupernaturalProgressionRecord record = profile.getProgressionRecord();
                if (record != null) {
                    double aptitude = ProgressionSystem.computeAptitudeModifier(profile, progConfig);
                    record.setAptitudeModifier(aptitude);
                }
            }

            // Record event
            summary.recordAwakening(selectedPathway, profile.getSupernaturalClass(),
                    profile.getRarity(), profile.isDarkAwakened());

            String darkTag = profile.isDarkAwakened() ? " ◆DARK◆" : "";
            if (eventRecorder != null) {
                eventRecorder.accept(String.format(
                        "Year %d: ⚡ %s (age %d, %s) AWAKENED as %s %s [%s] — T%d %s in %s%s",
                        currentYear,
                        person.getFullName(),
                        age,
                        profile.getGenotypeString(),
                        profile.getRarity().getDisplayName(),
                        profile.getSupernaturalClass().getDisplayName(),
                        selectedPathway.getDisplayName(),
                        profile.getPowerTier(),
                        profile.getEffectiveAttributeString(),
                        person.getVillage() != null ? person.getVillage().getName() : "Unknown",
                        darkTag
                ), config.isPrintIndividualEvents());
            }
        }

        return summary;
    }

    /**
     * Normal awakening probability: bell-curve centered at age 20, width ±4 years,
     * modified by Luck and genotype.
     */
    private double calculateNormalAwakeningProbability(int age, AetherProfile profile) {
        // Gaussian bell curve: peak at 20, sigma = 3.0
        double ageFactor = Math.exp(-0.5 * Math.pow((age - 20.0) / 3.0, 2));
        double baseProbability = NORMAL_BASE_PROBABILITY * ageFactor;

        // Genotype multiplier
        double genoMult = profile.isHomozygousDominant() ? DD_MULTIPLIER : 1.0;

        // Luck modifier: Luck ranges from -1.0 to +1.0
        // +1.0 Luck → 1.5x probability; -1.0 Luck → 0.5x probability
        double luckMult = 1.0 + (profile.getLuck() * 0.5);

        return Math.min(0.40, baseProbability * genoMult * luckMult);
    }

    /**
     * Twilight Epiphany probability based on:
     * - Age (peaks at 60–65)
     * - Leadership tenure (current settlement leader gets bonus)
     * - Occupation tenure (merchants, soldiers get bonus for long service)
     * - DD genotype multiplier
     */
    private double calculateTwilightProbability(int age, Person person, AetherProfile profile) {
        // Age factor: peaks at 60–65
        double ageFactor;
        if (age <= 60) {
            ageFactor = 0.5 + (age - 50) * 0.05;
        } else if (age <= 65) {
            ageFactor = 1.0;
        } else {
            ageFactor = 1.0 - (age - 65) * 0.08;
        }
        ageFactor = Math.max(0.2, ageFactor);

        double baseProbability = TWILIGHT_BASE_PROBABILITY * ageFactor;

        // Leadership bonus
        Village village = person.getVillage();
        if (village != null && village.getLeader() == person) {
            baseProbability *= 1.8; // 80% boost for settlement leaders
        }

        // Occupation tenure bonus (approximated: older workers assumed longer tenure)
        if (person.getOccupation() != null && person.getOccupation().isWorker()) {
            baseProbability *= 1.3;
        }

        // Genotype multiplier
        double genoMult = profile.isHomozygousDominant() ? DD_MULTIPLIER : 1.0;

        return Math.min(0.15, baseProbability * genoMult);
    }

    /**
     * Checks if a person has a trauma qualifier for Crimson Crucible awakening.
     * Trauma = living in a settlement that suffered famine, disaster, or raid this year.
     */
    private boolean hasTraumaQualifier(Person person) {
        Village village = person.getVillage();
        if (village == null) return false;

        // Famine survival
        if (village.getFamineDeathsThisYear() > 0) return true;

        // Disaster survival
        if (village.getLastDisasterThisYear() != null &&
                village.getLastDisasterThisYear() != civsim.model.DisasterType.NONE) {
            return true;
        }

        // Low health / near-death: very old person surviving past typical death age
        if (person.getAge() > 70) return true;

        return false;
    }

    /**
     * Gets a parent's supernatural class if they have awakened.
     */
    private SupernaturalClass getParentClass(Person parent) {
        if (parent == null) return null;
        AetherProfile profile = parent.getAetherProfile();
        if (profile == null || !profile.isAwakened()) return null;
        return profile.getSupernaturalClass();
    }

    /**
     * Counts the number of living Mythic-rarity awakened beings.
     */
    private int countLivingMythic(List<Person> allPeople) {
        int count = 0;
        for (Person p : allPeople) {
            if (!p.isAlive()) continue;
            AetherProfile profile = p.getAetherProfile();
            if (profile != null && profile.isAwakened() && profile.getRarity() == ClassRarity.MYTHIC) {
                count++;
            }
        }
        return count;
    }

    /**
     * Annual Greed and Dark Resonance drift for all living persons.
     * Wealth-hoarding and envy drive Greed; excess Greed bleeds into Dark Resonance.
     */
    private void processGreedAndDarkDrift(List<Person> allPeople, Random random) {
        // Calculate global average wealth for relative comparison
        double totalWealth = 0.0;
        int livingCount = 0;
        for (Person p : allPeople) {
            if (!p.isAlive()) continue;
            livingCount++;
            totalWealth += p.getWealth() + p.getBankBalance();
        }
        double avgWealth = (livingCount > 0) ? totalWealth / livingCount : 1.0;

        for (Person p : allPeople) {
            if (!p.isAlive()) continue;
            AetherProfile profile = p.getAetherProfile();
            if (profile == null) continue;
            if (profile.isAwakened()) continue; // Awakened beings don't drift (stable)

            // Wealth-relative Greed drift
            double personalWealth = p.getWealth() + p.getBankBalance();
            double wealthRatio = personalWealth / Math.max(1.0, avgWealth);

            // High wealth → slight greed reduction (satisfied); low wealth → slight greed increase (envy)
            double greedDrift;
            if (wealthRatio > 2.0) {
                greedDrift = -0.005; // wealthy and content
            } else if (wealthRatio < 0.3) {
                greedDrift = 0.008 + random.nextDouble() * 0.005; // poor and envious
            } else {
                greedDrift = random.nextDouble() * 0.003 - 0.001; // neutral slight drift
            }
            profile.driftGreed(greedDrift);

            // High Greed → Dark Resonance bleed
            if (profile.getGreed() > 0.6) {
                double darkDrift = (profile.getGreed() - 0.6) * 0.02;
                profile.driftDarkResonance(darkDrift);
            }
        }
    }

    public boolean isGreatMutationTriggered() { return greatMutationTriggered; }

    /**
     * Phase 2B.1 — Explicit Crimson Crucible awakening check after surviving severe combat trauma.
     * Idempotent per eventId.
     */
    public boolean attemptCrimsonCrucibleCombat(Person person, int currentYear, String eventId,
                                                Random random, SimulationConfig config,
                                                ProgressionConfig progConfig,
                                                BiConsumer<String, Boolean> eventRecorder) {
        return attemptCrimsonCrucibleCombat(person, eventId, currentYear, random, progConfig, eventRecorder);
    }

    public boolean attemptCrimsonCrucibleCombat(Person person, String eventId, int currentYear,
                                                Random random, ProgressionConfig progConfig,
                                                BiConsumer<String, Boolean> eventRecorder) {
        if (person == null || !person.isAlive() || person.isAwakened()) return false;
        CombatState cs = person.getOrCreateCombatState();
        if (!cs.markEventProcessed(eventId)) {
            return false; // Idempotent: already processed
        }

        AetherProfile profile = person.getAetherProfile();
        if (profile == null) {
            profile = new AetherProfile(AetherAllele.RECESSIVE, AetherAllele.RECESSIVE, random);
            person.setAetherProfile(profile);
        }

        int age = person.getAge();
        if (!AwakeningPathway.CRIMSON_CRUCIBLE.isInAgeRange(age)) return false;
        if (!profile.hasAwakeningPotential()) return false;

        double genoMult = profile.isHomozygousDominant() ? DD_MULTIPLIER : 1.0;
        double checkProbability = CRIMSON_BASE_PROBABILITY * genoMult;

        if (random.nextDouble() < checkProbability) {
            SupernaturalClass motherClass = getParentClass(person.getMother());
            SupernaturalClass fatherClass = getParentClass(person.getFather());
            profile.awaken(AwakeningPathway.CRIMSON_CRUCIBLE, currentYear, age, random,
                    motherClass, fatherClass, 0, MAX_MYTHIC_CAP);

            if (progConfig != null) {
                SupernaturalProgressionRecord record = profile.getProgressionRecord();
                if (record != null) {
                    double aptitude = ProgressionSystem.computeAptitudeModifier(profile, progConfig);
                    record.setAptitudeModifier(aptitude);
                }
            }

            if (eventRecorder != null) {
                eventRecorder.accept(String.format(
                        "Year %d: ⚡ %s (age %d) survived mortal combat and AWAKENED via Crimson Crucible as %s %s [T%d]!",
                        currentYear, person.getFullName(), age,
                        profile.getRarity().getDisplayName(),
                        profile.getSupernaturalClass().getDisplayName(),
                        profile.getPowerTier()
                ), true);
            }
            return true;
        }
        return false;
    }

    // --- Inner summary class ---

    /**
     * Summary of awakening results for a single year, used by AnnualReport.
     */
    public static class AwakeningYearSummary {
        private int totalAwakenings = 0;
        private int normalAwakenings = 0;
        private int prodigyAwakenings = 0;
        private int crimsonAwakenings = 0;
        private int twilightAwakenings = 0;
        private int darkAwakenings = 0;
        private int mythicAwakenings = 0;
        private int legendaryAwakenings = 0;

        // Class distribution
        private final int[] classAwakenings = new int[SupernaturalClass.values().length];

        public void recordAwakening(AwakeningPathway pathway, SupernaturalClass cls,
                                    ClassRarity rarity, boolean isDark) {
            totalAwakenings++;
            switch (pathway) {
                case NORMAL: normalAwakenings++; break;
                case PRODIGY: prodigyAwakenings++; break;
                case CRIMSON_CRUCIBLE: crimsonAwakenings++; break;
                case TWILIGHT_EPIPHANY: twilightAwakenings++; break;
                case DARK_AWAKENING: darkAwakenings++; break;
            }
            if (rarity == ClassRarity.MYTHIC) mythicAwakenings++;
            if (rarity == ClassRarity.LEGENDARY) legendaryAwakenings++;
            classAwakenings[cls.ordinal()]++;
        }

        public int getTotalAwakenings() { return totalAwakenings; }
        public int getNormalAwakenings() { return normalAwakenings; }
        public int getProdigyAwakenings() { return prodigyAwakenings; }
        public int getCrimsonAwakenings() { return crimsonAwakenings; }
        public int getTwilightAwakenings() { return twilightAwakenings; }
        public int getDarkAwakenings() { return darkAwakenings; }
        public int getMythicAwakenings() { return mythicAwakenings; }
        public int getLegendaryAwakenings() { return legendaryAwakenings; }
        public int getClassAwakenings(SupernaturalClass cls) { return classAwakenings[cls.ordinal()]; }

        /** Compact summary string for annual report */
        public String toSummaryString() {
            if (totalAwakenings == 0) return "No awakenings";
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Awakenings: %d (N:%d P:%d C:%d T:%d D:%d)",
                    totalAwakenings, normalAwakenings, prodigyAwakenings,
                    crimsonAwakenings, twilightAwakenings, darkAwakenings));
            if (mythicAwakenings > 0) sb.append(" ★MYTHIC:").append(mythicAwakenings);
            if (legendaryAwakenings > 0) sb.append(" ◆LEG:").append(legendaryAwakenings);
            return sb.toString();
        }
    }
}
