package civsim.systems;

import civsim.model.*;

import java.util.*;

/**
 * Phase 2C.2 — Multi-Generational Blood Feuds, Clan Revenge, Inherited Grievances,
 * and Intergenerational Conflict Subsystem.
 *
 * Implements persistent historical grievances, feud lifecycle management, deterministic
 * character disposition evaluation, clan feud policies, bounded retaliation with anti-loop
 * protection, single-combat engine integration, asset-based compensation, and peaceful reconciliation.
 */
public class BloodFeudSystem {

    // Registries
    private final Map<String, HistoricalGrievanceRecord> grievancesById = new LinkedHashMap<>();
    private final Map<String, FeudCase> feudsById = new LinkedHashMap<>();
    private final Map<String, InheritedGrievanceClaim> claimsById = new LinkedHashMap<>();
    private final Map<String, FeudAgreement> agreementsById = new LinkedHashMap<>();
    private final Set<String> processedEventIds = new HashSet<>();

    private void boundSet(Set<String> set, int maxSize) {
        if (set.size() > maxSize) {
            Iterator<String> it = set.iterator();
            int toRemove = maxSize / 4;
            while (it.hasNext() && toRemove-- > 0) {
                it.next();
                it.remove();
            }
        }
    }

    // Multi-generational decay rates
    public static final double PERSONAL_DECAY_RATE = 0.70;
    public static final double FAMILY_DECAY_RATE = 0.80;
    public static final double CLAN_DECAY_RATE = 0.90;

    // Telemetry & Statistics
    private int annualGrievancesCreated = 0;
    private int annualFeudsOpened = 0;
    private int annualFeudsSettled = 0;
    private int annualFeudsDormant = 0;
    private int annualRetaliationsAttempted = 0;
    private int annualRetaliationsSuccessful = 0;
    private int annualRetaliationsFailed = 0;
    private int annualClaimsInherited = 0;
    private int annualClaimsAccepted = 0;
    private int annualClaimsRejected = 0;
    private int annualClaimsAbandoned = 0;
    private int annualTrucesSigned = 0;
    private int annualTreatiesSigned = 0;
    private int annualTruceViolations = 0;
    private double cumulativeCompensationPaidIron = 0.0;
    private int cumulativeHeirloomsRecovered = 0;
    private int cumulativeFeudBattles = 0;
    private int cumulativeFeudCasualties = 0;

    public BloodFeudSystem() {}

    public synchronized void resetYearlyCounters() {
        annualGrievancesCreated = 0;
        annualFeudsOpened = 0;
        annualFeudsSettled = 0;
        annualFeudsDormant = 0;
        annualRetaliationsAttempted = 0;
        annualRetaliationsSuccessful = 0;
        annualRetaliationsFailed = 0;
        annualClaimsInherited = 0;
        annualClaimsAccepted = 0;
        annualClaimsRejected = 0;
        annualClaimsAbandoned = 0;
        annualTrucesSigned = 0;
        annualTreatiesSigned = 0;
        annualTruceViolations = 0;
    }

    public synchronized void clear() {
        grievancesById.clear();
        feudsById.clear();
        claimsById.clear();
        agreementsById.clear();
        processedEventIds.clear();
        resetYearlyCounters();
        cumulativeCompensationPaidIron = 0.0;
        cumulativeHeirloomsRecovered = 0;
        cumulativeFeudBattles = 0;
        cumulativeFeudCasualties = 0;
    }

    // =========================================================================
    // PART 1 & 2: GRIEVANCE RECORDING & FEUD INITIALIZATION
    // =========================================================================

    /**
     * Ingests a new historical grievance deterministically and links it to an existing
     * or new FeudCase. Grievances are immutable historical records that are never erased.
     */
    public synchronized FeudCase recordGrievance(HistoricalGrievanceRecord record, int currentYear, int currentMonth) {
        if (record == null) return null;

        String eventKey = "FEUD_CREATE:" + record.getGrievanceId();
        if (!processedEventIds.add(eventKey)) {
            // Already processed this grievance event idempotently
            return record.getFeudId() != null ? feudsById.get(record.getFeudId()) : null;
        }
        boundSet(processedEventIds, 1000);

        grievancesById.put(record.getGrievanceId(), record);
        annualGrievancesCreated++;
        int totalMonths = currentYear * 12 + currentMonth;

        // Minor ordinary wrongs or trivial severity (< 20.0) do NOT trigger a blood feud
        if (record.getSeverity() < 20.0 && !record.getCategory().isViolentOrDeadly()) {
            return null;
        }

        // Determine initiating side (victim side) and opposing side (culprit side)
        String victimSideId = record.getVictimPrimaryOrganizationId() != null && !record.getVictimPrimaryOrganizationId().equals("NONE")
                ? record.getVictimPrimaryOrganizationId()
                : (record.getVictimPersonId() != null ? "PERSON_" + record.getVictimPersonId() : "FAMILY_" + record.getVictimFamilyId());

        String culpritSideId = record.getKillerCoalitionSideId() != null
                ? record.getKillerCoalitionSideId()
                : (record.getKillerPersonId() != null ? "PERSON_" + record.getKillerPersonId() : null);

        // Check if an active feud already exists between these parties or battle
        FeudCase existingFeud = null;
        if (record.getBattleId() != null) {
            for (FeudCase f : feudsById.values()) {
                if (record.getBattleId().contains(f.getFeudId())
                        || (f.getActiveOperationId() != null && record.getBattleId().contains(f.getActiveOperationId()))) {
                    existingFeud = f;
                    break;
                }
            }
        }
        if (existingFeud == null) {
            existingFeud = findActiveFeudBetween(victimSideId, culpritSideId, record.getVictimFamilyId());
        }

        if (existingFeud != null) {
            // Link new grievance to existing feud
            existingFeud.addRelatedGrievance(record.getGrievanceId(), record.getSeverity(), totalMonths);
            record.setFeudId(existingFeud.getFeudId());

            // Check if this new grievance violates an active truce
            if (existingFeud.getCurrentFeudStatus() == FeudStatus.TRUCE) {
                existingFeud.transitionStatus(FeudStatus.ESCALATING, totalMonths, "Truce broken by new grievance " + record.getGrievanceId());
                annualTruceViolations++;
                if (existingFeud.getActiveAgreementId() != null) {
                    FeudAgreement agr = agreementsById.get(existingFeud.getActiveAgreementId());
                    if (agr != null) agr.markViolated("New grievance: " + record.getDescription());
                }
            } else if (existingFeud.getCurrentFeudStatus() == FeudStatus.DORMANT) {
                existingFeud.transitionStatus(FeudStatus.OPEN, totalMonths, "Reactivated by new grievance " + record.getGrievanceId());
            }

            return existingFeud;
        }

        // Create a new FeudCase
        String feudId = "FEUD_" + record.getGrievanceId();
        String feudName = "Feud of " + record.getDescription();
        FeudStatus initialStatus = record.getConfidence().isRetaliationPermitted() ? FeudStatus.OPEN : FeudStatus.PENDING_ATTRIBUTION;

        String initiatingOrg = record.getVictimPrimaryOrganizationId() != null && !record.getVictimPrimaryOrganizationId().equals("NONE")
                ? record.getVictimPrimaryOrganizationId() : null;

        String opposingOrg = !record.getResponsibleOrganizationIds().isEmpty()
                ? record.getResponsibleOrganizationIds().get(0) : null;

        String feudType = (initiatingOrg != null && opposingOrg != null) ? "CLAN"
                : (record.getVictimFamilyId() > 0 ? "FAMILY" : "PERSONAL");

        List<String> opposingSides = new ArrayList<>();
        if (culpritSideId != null) opposingSides.add(culpritSideId);
        if (opposingOrg != null && !opposingSides.contains(opposingOrg)) opposingSides.add(opposingOrg);

        FeudCase feud = new FeudCase(
                feudId, feudName, record.getGrievanceId(), totalMonths, record.getSeverity(),
                initialStatus, victimSideId, opposingSides, initiatingOrg, opposingOrg,
                record.getVictimFamilyId(), null, feudType
        );

        if (record.getVictimPersonId() != null) {
            feud.addClaimantPerson(record.getVictimPersonId());
        }
        if (record.getDirectPerpetratorId() != null) {
            feud.addResponsiblePerson(record.getDirectPerpetratorId());
        }

        record.setFeudId(feudId);
        feudsById.put(feudId, feud);
        annualFeudsOpened++;

        return feud;
    }

    private FeudCase findActiveFeudBetween(String sideA, String sideB, int familyIdA) {
        if (sideA == null && sideB == null && familyIdA <= 0) return null;
        for (FeudCase f : feudsById.values()) {
            if (!f.getCurrentFeudStatus().isActive() && f.getCurrentFeudStatus() != FeudStatus.DORMANT && f.getCurrentFeudStatus() != FeudStatus.TRUCE) {
                continue;
            }
            boolean matchDirect = isSideMatch(sideA, familyIdA, f.getInitiatingSideId(), f.getInitiatingOrgId(), f.getInitiatingFamilyId())
                    && isOpponentMatch(sideB, f.getOpposingSideIds(), f.getOpposingOrgId());

            boolean matchReverse = isSideMatch(sideB, -1, f.getInitiatingSideId(), f.getInitiatingOrgId(), f.getInitiatingFamilyId())
                    && isOpponentMatch(sideA, f.getOpposingSideIds(), f.getOpposingOrgId());

            if (matchDirect || matchReverse) {
                return f;
            }
        }
        return null;
    }

    private boolean isSideMatch(String side, int famId, String initiatingSideId, String initiatingOrgId, Integer feudFamId) {
        if (side != null && (side.equals(initiatingSideId) || side.equals(initiatingOrgId))) return true;
        if (famId > 0 && feudFamId != null && famId == feudFamId) return true;
        return false;
    }

    private boolean isOpponentMatch(String side, List<String> opposingSides, String opposingOrgId) {
        if (side != null && (opposingSides.contains(side) || (opposingOrgId != null && opposingOrgId.equals(side)))) return true;
        return false;
    }

    // =========================================================================
    // PART 3: MULTI-GENERATIONAL CLAIM INHERITANCE
    // =========================================================================

    /**
     * Handles death of a claimant, evaluating generational succession to adult heirs.
     * Enforces the non-negotiable rule: Perpetrator descendants NEVER inherit guilt.
     */
    public synchronized void handlePersonDeath(
            Person deceased,
            int currentYear,
            int currentMonth,
            Map<Integer, Person> peopleMap,
            OrganizationSystem organizationSystem) {

        if (deceased == null) return;
        String personId = String.valueOf(deceased.getId());
        int totalMonths = currentYear * 12 + currentMonth;

        // 1. If deceased was a target/perpetrator: Remove them from active living targets
        for (FeudCase feud : feudsById.values()) {
            if (feud.getResponsiblePersonIds().contains(personId)) {
                // Responsibility is historical fact, but live target is deceased.
                // Rule: Do NOT assign guilt to deceased's children, spouses, or descendants!
                // If no other living responsible persons or organizations exist, feud becomes DORMANT.
                boolean hasOtherLivingTargets = false;
                if (feud.getOpposingOrgId() != null) {
                    Organization org = organizationSystem != null ? organizationSystem.getOrganization(feud.getOpposingOrgId()) : null;
                    if (org != null && org.getStatus().isActive() && org.getActiveMemberCount() > 0) {
                        hasOtherLivingTargets = true;
                    }
                }
                if (!hasOtherLivingTargets) {
                    for (String otherRespId : feud.getResponsiblePersonIds()) {
                        if (!otherRespId.equals(personId)) {
                            Person other = peopleMap != null ? peopleMap.get(parsePersonId(otherRespId)) : null;
                            if (other != null && other.isAlive()) {
                                hasOtherLivingTargets = true;
                                break;
                            }
                        }
                    }
                }
                if (!hasOtherLivingTargets && feud.getCurrentFeudStatus().isActive()) {
                    feud.transitionStatus(FeudStatus.DORMANT, totalMonths, "Perpetrator deceased with no culpable institution");
                    annualFeudsDormant++;
                }
            }
        }

        // 2. If deceased was a claimant: Propagate inherited claim to eligible adult heir
        for (FeudCase feud : feudsById.values()) {
            if (feud.getClaimantPersonIds().contains(personId)) {
                feud.removeClaimantPerson(personId);

                Person eligibleSuccessor = selectSuccessor(deceased);
                if (eligibleSuccessor != null) {
                    int nextGen = feud.getCurrentClaimantGeneration() + 1;
                    feud.setCurrentClaimantGeneration(nextGen);

                    double decayMultiplier = "PERSONAL".equals(feud.getFeudType()) ? PERSONAL_DECAY_RATE
                            : ("FAMILY".equals(feud.getFeudType()) ? FAMILY_DECAY_RATE : CLAN_DECAY_RATE);

                    double inheritedStrength = feud.getCurrentSeverity() * Math.pow(decayMultiplier, nextGen);

                    InheritedGrievanceClaim.ClaimAcceptanceStatus acceptanceStatus;
                    if (eligibleSuccessor.getAge() < 19) {
                        // Minors are registered as pending adulthood; cannot participate in hostilities
                        acceptanceStatus = InheritedGrievanceClaim.ClaimAcceptanceStatus.PENDING_ADULTHOOD;
                    } else {
                        // Evaluate adult successor's disposition
                        FeudDispositionProfile disp = eligibleSuccessor.getFeudDisposition();
                        if (disp.getMercyDisposition() >= 75.0 && disp.getVengeanceDisposition() <= 25.0) {
                            acceptanceStatus = InheritedGrievanceClaim.ClaimAcceptanceStatus.ABANDONED;
                            annualClaimsAbandoned++;
                        } else if (disp.getVengeanceDisposition() < 30.0 && disp.getHonorDisposition() < 30.0) {
                            acceptanceStatus = InheritedGrievanceClaim.ClaimAcceptanceStatus.REJECTED;
                            annualClaimsRejected++;
                        } else {
                            acceptanceStatus = InheritedGrievanceClaim.ClaimAcceptanceStatus.ACCEPTED;
                            annualClaimsAccepted++;
                        }
                    }

                    String claimId = "CLAIM:" + feud.getFeudId() + ":" + eligibleSuccessor.getId() + ":G" + nextGen;
                    InheritedGrievanceClaim claim = new InheritedGrievanceClaim(
                            claimId, feud.getFeudId(), feud.getOriginGrievanceId(),
                            String.valueOf(eligibleSuccessor.getId()), feud.getInitiatingOrgId(),
                            nextGen, inheritedStrength, acceptanceStatus, true, currentYear, currentMonth
                    );

                    claimsById.put(claimId, claim);
                    feud.addInheritedClaimId(claimId);
                    annualClaimsInherited++;

                    if (acceptanceStatus == InheritedGrievanceClaim.ClaimAcceptanceStatus.ACCEPTED) {
                        feud.addClaimantPerson(String.valueOf(eligibleSuccessor.getId()));
                    }
                } else {
                    // No living bloodline heir: check institutional clan leadership
                    if (feud.getInitiatingOrgId() != null && organizationSystem != null) {
                        Organization org = organizationSystem.getOrganization(feud.getInitiatingOrgId());
                        if (org != null && org.getStatus().isActive() && org.getCurrentLeaderPersonId() != null) {
                            feud.addClaimantPerson(org.getCurrentLeaderPersonId());
                        } else if (feud.getCurrentFeudStatus().isActive()) {
                            feud.transitionStatus(FeudStatus.DORMANT, totalMonths, "No living claimant or active clan");
                            annualFeudsDormant++;
                        }
                    } else if (feud.getCurrentFeudStatus().isActive()) {
                        feud.transitionStatus(FeudStatus.DORMANT, totalMonths, "Claimant lineage extinct");
                        annualFeudsDormant++;
                    }
                }
            }
        }
    }

    private Person selectSuccessor(Person deceased) {
        if (deceased == null) return null;
        // Priority 1: Living adult children
        if (deceased.getChildren() != null) {
            for (Person child : deceased.getChildren()) {
                if (child.isAlive() && child.getAge() >= 19) {
                    return child;
                }
            }
            // Priority 2: Living minor children (registered as PENDING_ADULTHOOD)
            for (Person child : deceased.getChildren()) {
                if (child.isAlive()) {
                    return child;
                }
            }
        }
        // Priority 3: Living spouse
        if (deceased.getSpouse() != null && deceased.getSpouse().isAlive()) {
            return deceased.getSpouse();
        }
        // Priority 4: Living siblings
        if (deceased.getFather() != null && deceased.getFather().getChildren() != null) {
            for (Person sib : deceased.getFather().getChildren()) {
                if (sib.getId() != deceased.getId() && sib.isAlive() && sib.getAge() >= 19) {
                    return sib;
                }
            }
            for (Person sib : deceased.getFather().getChildren()) {
                if (sib.getId() != deceased.getId() && sib.isAlive()) {
                    return sib;
                }
            }
        }
        return null;
    }

    private int parsePersonId(String rawId) {
        if (rawId == null) return -1;
        try {
            if (rawId.startsWith("PERSON_")) return Integer.parseInt(rawId.substring(7));
            return Integer.parseInt(rawId);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // =========================================================================
    // PART 4 & 5: PERIODIC EVALUATION & RETALIATION DECISIONS
    // =========================================================================

    /**
     * Executes periodic feud evaluations (cadence: every 3 months).
     */
    public synchronized void processMonthlyTick(
            int totalMonths,
            int currentYear,
            int currentMonth,
            List<Village> settlements,
            OrganizationSystem organizationSystem,
            CombatEngine combatEngine,
            ClanHeirloomSystem clanHeirloomSystem,
            EquipmentCraftingSystem equipmentCraftingSystem,
            Random random) {

        // Periodic cadence: evaluate every 3 months
        if (totalMonths % 3 != 0) return;

        Map<Integer, Person> peopleMap = buildLivingPeopleMap(settlements);
        Map<Integer, Village> villageMap = new HashMap<>();
        if (settlements != null) {
            for (Village v : settlements) villageMap.put(v.getId(), v);
        }

        for (FeudCase feud : new ArrayList<>(feudsById.values())) {
            if (!feud.getCurrentFeudStatus().isActive() && feud.getCurrentFeudStatus() != FeudStatus.TRUCE) {
                continue;
            }

            // 1. Check active agreement expirations
            if (feud.getActiveAgreementId() != null) {
                FeudAgreement agr = agreementsById.get(feud.getActiveAgreementId());
                if (agr != null && agr.isActive() && agr.isExpired(totalMonths)) {
                    agr.terminate();
                    feud.setActiveAgreementId(null);
                    if (feud.getCurrentFeudStatus() == FeudStatus.TRUCE) {
                        feud.transitionStatus(FeudStatus.DIPLOMATIC_DISPUTE, totalMonths, "Truce period expired");
                    }
                }
            }

            // 2. Anti-Loop: check cooldowns
            if (feud.isCooldownActive(totalMonths)) continue;
            if (feud.getActiveOperationId() != null) continue;

            // 3. Evaluate Decision Score
            evaluateFeudDecisions(feud, totalMonths, currentYear, currentMonth,
                    organizationSystem, villageMap, peopleMap, combatEngine,
                    clanHeirloomSystem, equipmentCraftingSystem, random);
        }
    }

    private void evaluateFeudDecisions(
            FeudCase feud,
            int totalMonths,
            int currentYear,
            int currentMonth,
            OrganizationSystem organizationSystem,
            Map<Integer, Village> villageMap,
            Map<Integer, Person> peopleMap,
            CombatEngine combatEngine,
            ClanHeirloomSystem clanHeirloomSystem,
            EquipmentCraftingSystem equipmentCraftingSystem,
            Random random) {

        // Attribution check: If UNKNOWN or DISPUTED, cannot launch armed retaliation
        boolean attributionPermitted = isAttributionConfirmedOrSupported(feud);

        // Find primary active claimant and leader
        Person claimant = null;
        for (String cId : feud.getClaimantPersonIds()) {
            Person p = peopleMap.get(parsePersonId(cId));
            if (p != null && p.isAlive() && p.getAge() >= 19) {
                claimant = p;
                break;
            }
        }

        Organization initOrg = feud.getInitiatingOrgId() != null && organizationSystem != null
                ? organizationSystem.getOrganization(feud.getInitiatingOrgId()) : null;

        FeudDispositionProfile disp = claimant != null ? claimant.getFeudDisposition() : FeudDispositionProfile.createPeaceful();
        OrganizationalFeudPolicy policy = initOrg != null ? initOrg.getFeudPolicy()
                : (disp.getVengeanceDisposition() >= 50.0 ? OrganizationalFeudPolicy.TARGETED_RETALIATION : OrganizationalFeudPolicy.DEMAND_COMPENSATION);

        // Calculate Retaliation Decision Score [0, 100]
        double score = calculateDecisionScore(feud, disp, policy, totalMonths);

        // Choice 1: Voluntary Reconciliation or Truce (Score < 25 or Mercy >= 80 or Policy FORGIVE_AND_RECONCILE)
        if ((score < 25.0 || disp.getMercyDisposition() >= 80.0 || policy == OrganizationalFeudPolicy.FORGIVE_AND_RECONCILE)
                && feud.getCurrentEscalationLevel().getLevel() <= 1) {

            executePeacefulSettlement(feud, claimant, initOrg, totalMonths, currentYear, currentMonth, "Reconciliation agreed through forgiveness");
            return;
        }

        // Choice 2: Demand Compensation / Heirloom Restitution (Score 25–65 or Policy DEMAND_COMPENSATION/SEEK_JUSTICE)
        if (score < 65.0 || !attributionPermitted || !policy.permitsHostileRetaliation()) {
            executeDiplomaticDemands(feud, claimant, initOrg, organizationSystem, villageMap,
                    clanHeirloomSystem, totalMonths, currentYear, currentMonth);
            return;
        }

        // Choice 3: Armed Retaliation (Score >= 65 and Policy permits and attribution confirmed)
        if (score >= 65.0 && attributionPermitted && policy.permitsHostileRetaliation()) {
            // Anti-loop: 12-month retry cooldown check
            if (feud.isRetryCooldownActive(totalMonths)) {
                return; // Respect 12-month retry cooldown after failed attempt
            }

            executeArmedRetaliation(feud, claimant, initOrg, organizationSystem, villageMap,
                    peopleMap, combatEngine, clanHeirloomSystem, totalMonths, currentYear, currentMonth, random);
        }
    }

    private double calculateDecisionScore(FeudCase feud, FeudDispositionProfile disp, OrganizationalFeudPolicy policy, int totalMonths) {
        double base = feud.getCurrentSeverity() * 0.40;
        base += disp.getVengeanceDisposition() * 0.30;
        base -= disp.getMercyDisposition() * 0.25;
        base += disp.getHonorDisposition() * 0.15;
        base += disp.getRiskTolerance() * 0.10;

        if (policy.permitsHostileRetaliation()) base += 15.0;
        if (policy == OrganizationalFeudPolicy.FORGIVE_AND_RECONCILE) base -= 35.0;
        if (policy == OrganizationalFeudPolicy.DEFENSIVE_ONLY) base -= 20.0;

        // Decay based on generation
        base *= Math.pow(0.85, feud.getCurrentClaimantGeneration());

        return Math.max(0.0, Math.min(100.0, base));
    }

    private boolean isAttributionConfirmedOrSupported(FeudCase feud) {
        for (String gId : feud.getRelatedGrievanceIds()) {
            HistoricalGrievanceRecord rec = grievancesById.get(gId);
            if (rec != null && rec.getConfidence().isRetaliationPermitted()) {
                return true;
            }
        }
        return false;
    }

    // =========================================================================
    // PART 6 & 7: ARMED RETALIATION, COMBAT INTEGRATION & ANTI-LOOP
    // =========================================================================

    private void executeArmedRetaliation(
            FeudCase feud,
            Person claimant,
            Organization initOrg,
            OrganizationSystem organizationSystem,
            Map<Integer, Village> villageMap,
            Map<Integer, Person> peopleMap,
            CombatEngine combatEngine,
            ClanHeirloomSystem clanHeirloomSystem,
            int totalMonths,
            int currentYear,
            int currentMonth,
            Random random) {

        // Validate target eligibility: NEVER minors, uninvolved descendants, or uninvolved civilians
        Person targetPerson = null;
        for (String rId : feud.getResponsiblePersonIds()) {
            Person p = peopleMap.get(parsePersonId(rId));
            if (p != null && p.isAlive() && p.getAge() >= 19) {
                targetPerson = p;
                break;
            }
        }

        Organization targetOrg = feud.getOpposingOrgId() != null && organizationSystem != null
                ? organizationSystem.getOrganization(feud.getOpposingOrgId()) : null;

        if (targetPerson == null && (targetOrg == null || !targetOrg.getStatus().isActive())) {
            // No living responsible perpetrator or active culpable organization
            feud.transitionStatus(FeudStatus.DORMANT, totalMonths, "No living culpable target exists");
            annualFeudsDormant++;
            return;
        }

        // Determine base settlements for logistics and costs
        Village initVillage = claimant != null ? claimant.getVillage() : (villageMap.isEmpty() ? null : villageMap.values().iterator().next());
        if (initVillage == null) return;

        // Resource cost: Retaliation requires 50 Iron and 100 Food for logistics
        double ironCost = 50.0;
        double foodCost = 100.0;
        if (initVillage.getStockpile().getIronStoneStockpile() < ironCost || initVillage.getStockpile().getFoodStockpile() < foodCost) {
            // Cannot afford retaliation; economic constraints prevent operation
            return;
        }

        // Debit real resources
        initVillage.getStockpile().deductIronStone(ironCost);
        initVillage.getStockpile().deductFood(foodCost);

        // Mark operation active and apply 6-month cooldown
        String opId = "OP:" + feud.getFeudId() + ":" + totalMonths;
        feud.setActiveOperationId(opId);
        feud.recordMajorAction(totalMonths);
        annualRetaliationsAttempted++;

        // Escalate status
        feud.transitionStatus(FeudStatus.ACTIVE_RETALIATION, totalMonths, "Armed retaliation launched");
        feud.setEscalationLevel(FeudEscalationLevel.LEVEL_3_TARGETED_REPRISAL, totalMonths);

        // Assemble combatants
        List<Combatant> sideA = new ArrayList<>();
        if (claimant != null && claimant.isAlive()) {
            sideA.add(new PersonCombatant(claimant, "INITIATOR"));
        }

        List<Combatant> sideB = new ArrayList<>();
        if (targetPerson != null && targetPerson.isAlive()) {
            sideB.add(new PersonCombatant(targetPerson, "TARGET"));
        } else if (targetOrg != null) {
            // Find active warrior in target organization
            for (MembershipRecord rec : targetOrg.getActiveMembershipRecords()) {
                Person m = peopleMap.get(parsePersonId(rec.getPersonId()));
                if (m != null && m.isAlive() && m.getAge() >= 19 && m.getAge() < 60) {
                    sideB.add(new PersonCombatant(m, "TARGET_ORG"));
                    if (sideB.size() >= 3) break;
                }
            }
        }

        if (sideA.isEmpty() || sideB.isEmpty()) {
            feud.recordFailedRetaliation(totalMonths);
            annualRetaliationsFailed++;
            return;
        }

        List<Combatant> allCombatants = new ArrayList<>();
        allCombatants.addAll(sideA);
        allCombatants.addAll(sideB);

        // Execute battle via authoritative CombatEngine
        String battleId = "FEUD_BAT:" + feud.getFeudId() + ":" + totalMonths;
        BattleRequest request = new BattleRequest(
                battleId,
                currentYear,
                currentMonth,
                BattleType.FACTION_CONFLICT,
                initVillage.getX(),
                initVillage.getY(),
                1.0,
                allCombatants
        );

        Random rng = (random != null) ? random : new Random(((long) feud.getFeudId().hashCode() << 32) ^ totalMonths);
        BattleResult result = combatEngine != null ? combatEngine.resolveBattle(request, rng) : null;
        cumulativeFeudBattles++;

        boolean initiatorVictory = (result != null && "SIDE_A".equals(result.getVictoriousSideId()));
        int casualties = result != null ? result.getFatalities().size() : 0;
        cumulativeFeudCasualties += casualties;

        if (initiatorVictory) {
            annualRetaliationsSuccessful++;
            feud.recordSuccessRetaliation(totalMonths);
            feud.transitionStatus(FeudStatus.ESCALATING, totalMonths, "Retaliation succeeded; tension remains high");
        } else {
            annualRetaliationsFailed++;
            feud.recordFailedRetaliation(totalMonths); // Sets 12-month retry cooldown
            feud.transitionStatus(FeudStatus.ESCALATING, totalMonths, "Retaliation repulsed; cooldown enforced");
        }

        // Record action history
        FeudActionRecord action = new FeudActionRecord(
                "ACT_" + opId,
                feud.getFeudId(),
                FeudActionType.LIMITED_ARMED_EXPEDITION,
                currentYear,
                currentMonth,
                claimant != null ? String.valueOf(claimant.getId()) : null,
                feud.getInitiatingOrgId(),
                targetPerson != null ? String.valueOf(targetPerson.getId()) : null,
                feud.getOpposingOrgId(),
                "Armed clash in " + feud.getFeudId() + ": Casualties=" + casualties,
                ironCost,
                foodCost,
                initiatorVictory,
                "Battle " + battleId + " completed"
        );
        feud.addAction(action);
    }

    // =========================================================================
    // PART 8 & 9: DIPLOMATIC DEMANDS, COMPENSATION & RECONCILIATION
    // =========================================================================

    private void executeDiplomaticDemands(
            FeudCase feud,
            Person claimant,
            Organization initOrg,
            OrganizationSystem organizationSystem,
            Map<Integer, Village> villageMap,
            ClanHeirloomSystem clanHeirloomSystem,
            int totalMonths,
            int currentYear,
            int currentMonth) {

        feud.recordMajorAction(totalMonths);
        feud.setEscalationLevel(FeudEscalationLevel.LEVEL_1_DIPLOMATIC_TENSION, totalMonths);
        feud.transitionStatus(FeudStatus.DIPLOMATIC_DISPUTE, totalMonths, "Formal diplomatic demands presented");

        // Check if an heirloom restitution demand applies
        String stolenHeirloomId = null;
        for (String gId : feud.getRelatedGrievanceIds()) {
            HistoricalGrievanceRecord rec = grievancesById.get(gId);
            if (rec != null && rec.getCategory() == GrievanceCategory.HEIRLOOM_CAPTURED) {
                // Find heirloom
                if (clanHeirloomSystem != null) {
                    for (ClanHeirloom h : clanHeirloomSystem.getAllHeirlooms().values()) {
                        if (h.getCustodyStatus() == HeirloomCustodyStatus.STOLEN_OR_CAPTURED
                                && h.getOwningClanId().equals(feud.getInitiatingOrgId())) {
                            stolenHeirloomId = h.getHeirloomId();
                            break;
                        }
                    }
                }
            }
        }

        double demandedCompensationIron = Math.min(500.0, feud.getCurrentSeverity() * 5.0);

        FeudActionRecord demandAction = new FeudActionRecord(
                "DEMAND:" + feud.getFeudId() + ":" + totalMonths,
                feud.getFeudId(),
                stolenHeirloomId != null ? FeudActionType.HEIRLOOM_RESTITUTION_DEMAND : FeudActionType.COMPENSATION_DEMAND,
                currentYear,
                currentMonth,
                claimant != null ? String.valueOf(claimant.getId()) : null,
                feud.getInitiatingOrgId(),
                null,
                feud.getOpposingOrgId(),
                "Demanded " + (stolenHeirloomId != null ? "return of heirloom " + stolenHeirloomId : demandedCompensationIron + " Iron"),
                0.0,
                0.0,
                true,
                "Diplomatic demand delivered"
        );
        feud.addAction(demandAction);

        // Adjust inter-organization diplomacy score downwards
        if (organizationSystem != null && feud.getInitiatingOrgId() != null && feud.getOpposingOrgId() != null) {
            organizationSystem.adjustRelationship(
                    "FEUD_DIPLO:" + feud.getFeudId() + ":" + totalMonths,
                    feud.getInitiatingOrgId(),
                    feud.getOpposingOrgId(),
                    -10.0,
                    currentYear,
                    currentMonth
            );
        }
    }

    /**
     * Executes compensation payment and physically transfers assets/heirloom, settling the feud.
     */
    public synchronized boolean executeCompensationSettlement(
            String feudId,
            Village payingVillage,
            Village receivingVillage,
            double ironAmount,
            String returnedHeirloomId,
            ClanHeirloomSystem clanHeirloomSystem,
            int currentYear,
            int currentMonth) {

        FeudCase feud = feudsById.get(feudId);
        if (feud == null || !feud.getCurrentFeudStatus().isActive()) return false;

        // Validate physical ability to pay
        if (payingVillage != null && ironAmount > 0.0) {
            if (payingVillage.getStockpile().getIronStoneStockpile() < ironAmount) {
                return false; // Cannot afford compensation
            }
            payingVillage.getStockpile().deductIronStone(ironAmount);
            if (receivingVillage != null) {
                receivingVillage.getStockpile().addIronStone(ironAmount);
            }
            cumulativeCompensationPaidIron += ironAmount;
        }

        // Return stolen heirloom physically if required
        if (returnedHeirloomId != null && clanHeirloomSystem != null) {
            ClanHeirloom heirloom = clanHeirloomSystem.getHeirloom(returnedHeirloomId);
            if (heirloom != null && heirloom.getCustodyStatus() == HeirloomCustodyStatus.STOLEN_OR_CAPTURED) {
                heirloom.markRecovered(feud.getInitiatingOrgId());
                cumulativeHeirloomsRecovered++;
            }
        }

        int totalMonths = currentYear * 12 + currentMonth;
        String agrId = "SETTLE:" + feudId + ":" + totalMonths;
        FeudAgreement agreement = new FeudAgreement(
                agrId, feudId, FeudAgreement.AgreementType.SETTLEMENT,
                totalMonths, -1, // Permanent
                List.of(feud.getInitiatingSideId()),
                feud.getInitiatingOrgId() != null ? List.of(feud.getInitiatingOrgId()) : Collections.emptyList(),
                ironAmount, returnedHeirloomId
        );
        agreement.recordPayment(ironAmount);
        if (returnedHeirloomId != null) agreement.recordHeirloomReturned();

        agreementsById.put(agrId, agreement);
        feud.setActiveAgreementId(agrId);
        feud.transitionStatus(FeudStatus.SETTLED, totalMonths, "Compensation paid and terms satisfied");
        annualFeudsSettled++;

        FeudActionRecord action = new FeudActionRecord(
                "ACT_" + agrId,
                feudId,
                returnedHeirloomId != null ? FeudActionType.HEIRLOOM_RETURN : FeudActionType.COMPENSATION_PAYMENT,
                currentYear,
                currentMonth,
                null,
                feud.getOpposingOrgId(),
                null,
                feud.getInitiatingOrgId(),
                "Settled via compensation of " + ironAmount + " Iron",
                ironAmount,
                0.0,
                true,
                "Settlement agreement ratified"
        );
        feud.addAction(action);

        return true;
    }

    /**
     * Executes voluntary reconciliation or signing of a truce.
     */
    public synchronized boolean signTruce(
            String feudId,
            int durationMonths,
            int currentYear,
            int currentMonth) {

        FeudCase feud = feudsById.get(feudId);
        if (feud == null || feud.getCurrentFeudStatus().isTerminal()) return false;

        int totalMonths = currentYear * 12 + currentMonth;
        String agrId = "TRUCE:" + feudId + ":" + totalMonths;
        FeudAgreement agreement = new FeudAgreement(
                agrId, feudId, FeudAgreement.AgreementType.TRUCE,
                totalMonths, durationMonths,
                List.of(feud.getInitiatingSideId()),
                feud.getInitiatingOrgId() != null ? List.of(feud.getInitiatingOrgId()) : Collections.emptyList(),
                0.0, null
        );

        agreementsById.put(agrId, agreement);
        feud.setActiveAgreementId(agrId);
        feud.transitionStatus(FeudStatus.TRUCE, totalMonths, "Truce signed for " + durationMonths + " months");
        annualTrucesSigned++;

        FeudActionRecord action = new FeudActionRecord(
                "ACT_" + agrId,
                feudId,
                FeudActionType.TRUCE_ACCEPTED,
                currentYear,
                currentMonth,
                null,
                feud.getInitiatingOrgId(),
                null,
                feud.getOpposingOrgId(),
                "Truce established for " + durationMonths + " months",
                0.0,
                0.0,
                true,
                "Hostilities suspended"
        );
        feud.addAction(action);
        return true;
    }

    private void executePeacefulSettlement(
            FeudCase feud,
            Person claimant,
            Organization initOrg,
            int totalMonths,
            int currentYear,
            int currentMonth,
            String reason) {

        feud.transitionStatus(FeudStatus.SETTLED, totalMonths, reason);
        annualFeudsSettled++;

        FeudActionRecord action = new FeudActionRecord(
                "RECONCILE:" + feud.getFeudId() + ":" + totalMonths,
                feud.getFeudId(),
                FeudActionType.VOLUNTARY_RECONCILIATION,
                currentYear,
                currentMonth,
                claimant != null ? String.valueOf(claimant.getId()) : null,
                feud.getInitiatingOrgId(),
                null,
                feud.getOpposingOrgId(),
                reason,
                0.0,
                0.0,
                true,
                "Peace restored voluntarily"
        );
        feud.addAction(action);
    }

    private Map<Integer, Person> buildLivingPeopleMap(List<Village> settlements) {
        Map<Integer, Person> map = new HashMap<>();
        if (settlements != null) {
            for (Village v : settlements) {
                for (Person p : v.getResidents()) {
                    if (p.isAlive()) {
                        map.put(p.getId(), p);
                    }
                }
            }
        }
        return map;
    }

    // =========================================================================
    // GETTERS, QUERIES & TELEMETRY
    // =========================================================================

    public synchronized HistoricalGrievanceRecord getGrievance(String grievanceId) {
        return grievancesById.get(grievanceId);
    }

    public synchronized FeudCase getFeud(String feudId) {
        return feudsById.get(feudId);
    }

    public synchronized InheritedGrievanceClaim getClaim(String claimId) {
        return claimsById.get(claimId);
    }

    public synchronized FeudAgreement getAgreement(String agreementId) {
        return agreementsById.get(agreementId);
    }

    public synchronized List<HistoricalGrievanceRecord> getAllGrievances() {
        return Collections.unmodifiableList(new ArrayList<>(grievancesById.values()));
    }

    public synchronized List<FeudCase> getAllFeuds() {
        return Collections.unmodifiableList(new ArrayList<>(feudsById.values()));
    }

    public synchronized List<FeudCase> getActiveFeuds() {
        List<FeudCase> active = new ArrayList<>();
        for (FeudCase f : feudsById.values()) {
            if (f.getCurrentFeudStatus().isActive()) {
                active.add(f);
            }
        }
        return Collections.unmodifiableList(active);
    }

    public synchronized List<FeudCase> getSettledFeuds() {
        List<FeudCase> settled = new ArrayList<>();
        for (FeudCase f : feudsById.values()) {
            if (f.getCurrentFeudStatus() == FeudStatus.SETTLED) {
                settled.add(f);
            }
        }
        return Collections.unmodifiableList(settled);
    }

    public synchronized List<FeudCase> getDormantFeuds() {
        List<FeudCase> dormant = new ArrayList<>();
        for (FeudCase f : feudsById.values()) {
            if (f.getCurrentFeudStatus() == FeudStatus.DORMANT) {
                dormant.add(f);
            }
        }
        return Collections.unmodifiableList(dormant);
    }

    public synchronized int getCountByEscalationLevel(FeudEscalationLevel level) {
        int count = 0;
        for (FeudCase f : feudsById.values()) {
            if (f.getCurrentFeudStatus().isActive() && f.getCurrentEscalationLevel() == level) {
                count++;
            }
        }
        return count;
    }

    public synchronized int getMultiGenerationalFeudCount() {
        int count = 0;
        for (FeudCase f : feudsById.values()) {
            if (f.getCurrentClaimantGeneration() >= 2) {
                count++;
            }
        }
        return count;
    }

    public synchronized String generateAnnualSummary() {
        int l0 = getCountByEscalationLevel(FeudEscalationLevel.LEVEL_0_UNRESOLVED_GRIEVANCE);
        int l1 = getCountByEscalationLevel(FeudEscalationLevel.LEVEL_1_DIPLOMATIC_TENSION);
        int l2 = getCountByEscalationLevel(FeudEscalationLevel.LEVEL_2_FORMAL_HOSTILITY);
        int l3 = getCountByEscalationLevel(FeudEscalationLevel.LEVEL_3_TARGETED_REPRISAL);
        int l4 = getCountByEscalationLevel(FeudEscalationLevel.LEVEL_4_ORGANIZED_CLAN_CONFLICT);
        int l5 = getCountByEscalationLevel(FeudEscalationLevel.LEVEL_5_INTERGENERATIONAL_VENDETTA);

        return String.format(
                "Feuds[Griev:%d Act:%d(L0:%d L1:%d L2:%d L3:%d L4:%d L5:%d) Set:%d Drm:%d] | Retaliations[Att:%d Suc:%d Fail:%d] | Claims[Inh:%d Acc:%d Rej:%d Abn:%d]",
                grievancesById.size(),
                getActiveFeuds().size(),
                l0, l1, l2, l3, l4, l5,
                getSettledFeuds().size(),
                getDormantFeuds().size(),
                annualRetaliationsAttempted,
                annualRetaliationsSuccessful,
                annualRetaliationsFailed,
                annualClaimsInherited,
                annualClaimsAccepted,
                annualClaimsRejected,
                annualClaimsAbandoned
        );
    }

    // Telemetry getters
    public int getAnnualGrievancesCreated() { return annualGrievancesCreated; }
    public int getAnnualFeudsOpened() { return annualFeudsOpened; }
    public int getAnnualFeudsSettled() { return annualFeudsSettled; }
    public int getAnnualFeudsDormant() { return annualFeudsDormant; }
    public int getAnnualRetaliationsAttempted() { return annualRetaliationsAttempted; }
    public int getAnnualRetaliationsSuccessful() { return annualRetaliationsSuccessful; }
    public int getAnnualRetaliationsFailed() { return annualRetaliationsFailed; }
    public int getAnnualClaimsInherited() { return annualClaimsInherited; }
    public int getAnnualClaimsAccepted() { return annualClaimsAccepted; }
    public int getAnnualClaimsRejected() { return annualClaimsRejected; }
    public int getAnnualClaimsAbandoned() { return annualClaimsAbandoned; }
    public int getAnnualTrucesSigned() { return annualTrucesSigned; }
    public int getAnnualTreatiesSigned() { return annualTreatiesSigned; }
    public int getAnnualTruceViolations() { return annualTruceViolations; }
    public double getCumulativeCompensationPaidIron() { return cumulativeCompensationPaidIron; }
    public int getCumulativeHeirloomsRecovered() { return cumulativeHeirloomsRecovered; }
    public int getCumulativeFeudBattles() { return cumulativeFeudBattles; }
    public int getCumulativeFeudCasualties() { return cumulativeFeudCasualties; }
}
