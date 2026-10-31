package civsim.systems;

import civsim.SimulationConfig;
import civsim.model.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Phase 2B.2 — Master Organization, Clan, Allegiance, and Inter-Organization Diplomacy System.
 * Coordinates social organizations, leadership successions, pairwise treaties, loyalty conflict detection,
 * and tactical combat allegiance resolution.
 */
public class OrganizationSystem {

    // Authoritative organization registries
    private final Map<String, Organization> organizations = new LinkedHashMap<>();
    private final Map<String, Organization> dissolvedOrganizations = new LinkedHashMap<>();
    private final Map<String, OrganizationRelationship> relationships = new LinkedHashMap<>();
    private final List<LoyaltyConflictRecord> loyaltyConflicts = new ArrayList<>();
    private final List<MembershipConflict> membershipConflicts = new ArrayList<>();

    // Idempotency caches
    private final Set<String> processedSystemEventIds = new HashSet<>();
    private final Set<String> processedBattleIds = new HashSet<>();

    // Annual activity telemetry counters
    private int annualFoundings = 0;
    private int annualInvitations = 0;
    private int annualAcceptances = 0;
    private int annualRejections = 0;
    private int annualVoluntaryJoins = 0;
    private int annualDepartures = 0;
    private int annualSuspensions = 0;
    private int annualExpulsions = 0;
    private int annualSuccessions = 0;
    private int annualDissolutions = 0;
    private int annualTreatiesSigned = 0;
    private int annualTreatiesBroken = 0;

    public OrganizationSystem() {}

    public synchronized void clear() {
        organizations.clear();
        dissolvedOrganizations.clear();
        relationships.clear();
        loyaltyConflicts.clear();
        membershipConflicts.clear();
        processedSystemEventIds.clear();
        processedBattleIds.clear();
        resetYearlyCounters();
    }

    public synchronized void resetYearlyCounters() {
        annualFoundings = 0;
        annualInvitations = 0;
        annualAcceptances = 0;
        annualRejections = 0;
        annualVoluntaryJoins = 0;
        annualDepartures = 0;
        annualSuspensions = 0;
        annualExpulsions = 0;
        annualSuccessions = 0;
        annualDissolutions = 0;
        annualTreatiesSigned = 0;
        annualTreatiesBroken = 0;
    }

    // --- Registry Queries ---

    public synchronized Organization getOrganization(String organizationId) {
        if (organizationId == null) return null;
        Organization org = organizations.get(organizationId);
        if (org == null) {
            org = dissolvedOrganizations.get(organizationId);
        }
        return org;
    }

    public synchronized void registerOrganization(Organization org) {
        if (org != null) {
            organizations.put(org.getOrganizationId(), org);
        }
    }

    public synchronized List<Organization> getActiveOrganizations() {
        List<Organization> active = new ArrayList<>();
        for (Organization o : organizations.values()) {
            if (o.getStatus().isActive()) {
                active.add(o);
            }
        }
        return Collections.unmodifiableList(active);
    }

    public synchronized List<Organization> getAllOrganizations() {
        return Collections.unmodifiableList(new ArrayList<>(organizations.values()));
    }

    public synchronized List<Organization> getDissolvedOrganizations() {
        return Collections.unmodifiableList(new ArrayList<>(dissolvedOrganizations.values()));
    }

    public synchronized List<LoyaltyConflictRecord> getLoyaltyConflicts() {
        return Collections.unmodifiableList(loyaltyConflicts);
    }

    public synchronized List<MembershipConflict> getMembershipConflicts() {
        return Collections.unmodifiableList(membershipConflicts);
    }

    // --- Organization Founding ---

    /**
     * Founds a new Organization deterministically from an explicit founding event.
     * Enforces era-gating and validation rules.
     */
    public synchronized Organization foundOrganization(
            OrganizationFoundingEvent event,
            Collection<Person> livingPeople,
            SimulationConfig config,
            BiConsumer<String, Boolean> eventRecorder) {

        if (event == null || event.getOrganizationId() == null || event.getOrganizationId().trim().isEmpty()) {
            return null;
        }

        // Idempotency: Reject duplicate eventId
        if (event.getEventId() != null && !processedSystemEventIds.add(event.getEventId())) {
            return organizations.get(event.getOrganizationId());
        }

        // Invariant: OrganizationId must be unique and never reused
        if (organizations.containsKey(event.getOrganizationId()) || dissolvedOrganizations.containsKey(event.getOrganizationId())) {
            return null;
        }

        int awakeningStart = (config != null) ? config.getAwakeningStartYear() : 50;

        // Era Gating: Supernatural organization types or alignments require Year 50+
        if (event.getOrganizationType().isSupernatural() || event.getAlignment().isSupernatural()) {
            if (event.getYear() < awakeningStart) {
                return null; // Blocked in Mundane Era
            }
        }

        Map<String, Person> peopleMap = buildPersonMap(livingPeople);
        Person founder = (event.getFounderPersonId() != null) ? peopleMap.get(event.getFounderPersonId()) : null;

        // Validate founder prerequisites if person-founded
        if (event.getFounderPersonId() != null) {
            if (founder == null || !founder.isAlive()) {
                return null; // Founder must be alive
            }
            if (event.getOrganizationType() == OrganizationType.AWAKENED_ORDER && !founder.isAwakened()) {
                return null; // Awakened order requires awakened founder
            }
        }

        Organization org = new Organization(
                event.getOrganizationId(),
                event.getDisplayName(),
                event.getOrganizationType(),
                event.getYear(),
                event.getMonth(),
                event.getFounderPersonId(),
                OrganizationStatus.ACTIVE,
                event.getAlignment(),
                event.getPolicyProfile()
        );

        // Enroll founder as LEADER
        if (founder != null) {
            MembershipRecord founderRecord = new MembershipRecord(
                    founder.getFullName() != null ? String.valueOf(founder.getId()) : event.getFounderPersonId(),
                    org.getOrganizationId(),
                    event.getYear(),
                    event.getMonth(),
                    OrganizationRole.LEADER,
                    MembershipStatus.ACTIVE,
                    MembershipOrigin.FOUNDING,
                    100.0
            );
            org.addMembershipRecord(founderRecord);
            founder.addActiveOrganizationId(org.getOrganizationId());
            if (founder.getPrimaryOrganizationId() == null) {
                founder.setPrimaryOrganizationId(org.getOrganizationId());
            }
        }

        // Enroll initial members
        for (String memberId : event.getInitialMemberIds()) {
            if (memberId.equals(event.getFounderPersonId())) continue;
            Person p = peopleMap.get(memberId);
            if (p != null && p.isAlive()) {
                MembershipRecord rec = new MembershipRecord(
                        memberId,
                        org.getOrganizationId(),
                        event.getYear(),
                        event.getMonth(),
                        OrganizationRole.MEMBER,
                        MembershipStatus.ACTIVE,
                        MembershipOrigin.FOUNDING,
                        70.0
                );
                org.addMembershipRecord(rec);
                p.addActiveOrganizationId(org.getOrganizationId());
                if (p.getPrimaryOrganizationId() == null) {
                    p.setPrimaryOrganizationId(org.getOrganizationId());
                }
            }
        }

        org.addEvent(new OrganizationEvent(
                event.getEventId() != null ? event.getEventId() : "FOUND:" + org.getOrganizationId(),
                org.getOrganizationId(),
                event.getYear(),
                event.getMonth(),
                OrganizationEvent.EventType.FOUNDING,
                String.format("Organization %s (%s, %s) founded by %s with %d initial members",
                        org.getDisplayName(), org.getOrganizationType().getDisplayName(),
                        org.getAlignment().getDisplayName(),
                        event.getFounderPersonId() != null ? event.getFounderPersonId() : "Institution",
                        org.getActiveMemberCount())
        ));

        organizations.put(org.getOrganizationId(), org);
        annualFoundings++;

        if (eventRecorder != null) {
            eventRecorder.accept(String.format("Year %d: %s founded (%s, %s).",
                    event.getYear(), org.getDisplayName(), org.getOrganizationType().getDisplayName(), org.getAlignment().getDisplayName()), true);
        }

        return org;
    }

    // --- Membership Operations ---

    public synchronized boolean inviteMember(
            String eventId,
            String orgId,
            String personId,
            OrganizationRole proposedRole,
            int year,
            int month,
            Collection<Person> livingPeople) {

        if (eventId != null && !processedSystemEventIds.add(eventId)) return false;
        Organization org = getOrganization(orgId);
        if (org == null || !org.getStatus().canRecruit()) return false;

        Map<String, Person> peopleMap = buildPersonMap(livingPeople);
        Person person = peopleMap.get(personId);
        if (person == null || !person.isAlive()) return false;

        // Check if already active/invited/suspended
        MembershipRecord existing = org.getMembershipRecord(personId);
        if (existing != null && existing.getStatus().isCurrentAffiliation()) {
            return false;
        }

        // Verify compatibility
        CompatibilityResult comp = evaluateMembershipCompatibility(person, org);
        if (!comp.isCompatible()) {
            return false;
        }

        MembershipRecord invite = new MembershipRecord(
                personId, orgId, year, month,
                proposedRole != null ? proposedRole : OrganizationRole.RECRUIT,
                MembershipStatus.INVITED,
                MembershipOrigin.INVITATION,
                50.0
        );
        boolean added = org.addMembershipRecord(invite);
        if (added) {
            annualInvitations++;
            org.addEvent(new OrganizationEvent(
                    eventId != null ? eventId : "INV:" + orgId + ":" + personId,
                    orgId, year, month, OrganizationEvent.EventType.MEMBER_JOINED,
                    String.format("Invited %s as %s", person.getFullName(), invite.getRole().getDisplayName()),
                    personId, null
            ));
        }
        return added;
    }

    public synchronized boolean acceptInvitation(
            String eventId,
            String orgId,
            String personId,
            int year,
            int month,
            Collection<Person> livingPeople) {

        if (eventId != null && !processedSystemEventIds.add(eventId)) return false;
        Organization org = getOrganization(orgId);
        if (org == null || org.getStatus().isPermanentlyClosed()) return false;

        MembershipRecord rec = org.getMembershipRecord(personId);
        if (rec == null || rec.getStatus() != MembershipStatus.INVITED) return false;

        Map<String, Person> peopleMap = buildPersonMap(livingPeople);
        Person person = peopleMap.get(personId);
        if (person == null || !person.isAlive()) return false;

        // Check exclusivity conflicts with person's existing active organizations
        OrganizationPolicyProfile policy = org.getPolicyProfile();
        for (String activeOrgId : person.getActiveOrganizationIds()) {
            Organization other = getOrganization(activeOrgId);
            if (other != null && other.getStatus().isActive()) {
                if (policy.isExclusiveMembershipRequired() || other.getPolicyProfile().isExclusiveMembershipRequired()) {
                    membershipConflicts.add(new MembershipConflict(personId, activeOrgId, orgId, "Exclusivity policy violated"));
                    return false;
                }
                if (policy.getForbiddenOrganizationTypes().contains(other.getOrganizationType())
                        || other.getPolicyProfile().getForbiddenOrganizationTypes().contains(org.getOrganizationType())) {
                    membershipConflicts.add(new MembershipConflict(personId, activeOrgId, orgId, "Forbidden organization type"));
                    return false;
                }
                if (policy.getForbiddenAlignments().contains(other.getAlignment())
                        || other.getPolicyProfile().getForbiddenAlignments().contains(org.getAlignment())) {
                    membershipConflicts.add(new MembershipConflict(personId, activeOrgId, orgId, "Forbidden alignment"));
                    return false;
                }
            }
        }

        rec.activateFromInvitation(rec.getRole() != null ? rec.getRole() : OrganizationRole.MEMBER, year, month);
        person.addActiveOrganizationId(orgId);
        if (person.getPrimaryOrganizationId() == null) {
            person.setPrimaryOrganizationId(orgId);
        }

        annualAcceptances++;
        org.addEvent(new OrganizationEvent(
                eventId != null ? eventId : "ACC:" + orgId + ":" + personId,
                orgId, year, month, OrganizationEvent.EventType.MEMBER_JOINED,
                String.format("%s accepted invitation -> ACTIVE as %s", person.getFullName(), rec.getRole().getDisplayName()),
                personId, null
        ));
        return true;
    }

    public synchronized boolean rejectInvitation(
            String eventId,
            String orgId,
            String personId,
            int year,
            int month,
            String reason) {

        if (eventId != null && !processedSystemEventIds.add(eventId)) return false;
        Organization org = getOrganization(orgId);
        if (org == null) return false;

        MembershipRecord rec = org.getMembershipRecord(personId);
        if (rec == null || rec.getStatus() != MembershipStatus.INVITED) return false;

        rec.terminate(MembershipStatus.LEFT, year, month, reason != null ? reason : "Invitation rejected");
        annualRejections++;
        return true;
    }

    public synchronized boolean voluntaryJoin(
            String eventId,
            String orgId,
            String personId,
            int year,
            int month,
            Collection<Person> livingPeople) {

        if (eventId != null && !processedSystemEventIds.add(eventId)) return false;
        Organization org = getOrganization(orgId);
        if (org == null || !org.getStatus().canRecruit()) return false;
        if (!org.getPolicyProfile().isOpenRecruitment()) return false;

        Map<String, Person> peopleMap = buildPersonMap(livingPeople);
        Person person = peopleMap.get(personId);
        if (person == null || !person.isAlive()) return false;

        MembershipRecord existing = org.getMembershipRecord(personId);
        if (existing != null && existing.getStatus().isCurrentAffiliation()) return false;

        CompatibilityResult comp = evaluateMembershipCompatibility(person, org);
        if (!comp.isCompatible()) return false;

        MembershipRecord rec = new MembershipRecord(
                personId, orgId, year, month,
                OrganizationRole.RECRUIT,
                MembershipStatus.ACTIVE,
                MembershipOrigin.OPEN_JOIN,
                55.0
        );
        org.addMembershipRecord(rec);
        person.addActiveOrganizationId(orgId);
        if (person.getPrimaryOrganizationId() == null) {
            person.setPrimaryOrganizationId(orgId);
        }

        annualVoluntaryJoins++;
        org.addEvent(new OrganizationEvent(
                eventId != null ? eventId : "JOIN:" + orgId + ":" + personId,
                orgId, year, month, OrganizationEvent.EventType.MEMBER_JOINED,
                String.format("%s joined voluntarily as RECRUIT", person.getFullName()),
                personId, null
        ));
        return true;
    }

    public synchronized boolean changePrimaryAllegiance(
            String eventId,
            String personId,
            String newPrimaryOrgId,
            Collection<Person> livingPeople) {

        if (eventId != null && !processedSystemEventIds.add(eventId)) return false;
        Map<String, Person> peopleMap = buildPersonMap(livingPeople);
        Person person = peopleMap.get(personId);
        if (person == null || !person.isAlive()) return false;

        Organization org = getOrganization(newPrimaryOrgId);
        if (org == null || !org.isMemberActive(personId)) {
            return false; // Can only set primary to an organization where the person is ACTIVE
        }

        person.setPrimaryOrganizationId(newPrimaryOrgId);
        return true;
    }

    public synchronized boolean promoteOrChangeRole(
            String eventId,
            String orgId,
            String personId,
            OrganizationRole newRole,
            int year,
            int month) {

        if (eventId != null && !processedSystemEventIds.add(eventId)) return false;
        Organization org = getOrganization(orgId);
        if (org == null || org.getStatus().isPermanentlyClosed()) return false;

        MembershipRecord rec = org.getMembershipRecord(personId);
        if (rec == null || !rec.isActive()) return false;

        if (newRole == OrganizationRole.LEADER) {
            org.setLeader(personId, year, month, "Promotion to Leader");
            annualSuccessions++;
        } else {
            rec.setRole(newRole, year, month);
        }
        return true;
    }

    public synchronized boolean suspendMember(
            String eventId,
            String orgId,
            String personId,
            int year,
            int month,
            String reason,
            Collection<Person> livingPeople) {

        if (eventId != null && !processedSystemEventIds.add(eventId)) return false;
        Organization org = getOrganization(orgId);
        if (org == null) return false;

        MembershipRecord rec = org.getMembershipRecord(personId);
        if (rec == null || !rec.isActive()) return false;

        rec.suspend(year, month, reason);
        annualSuspensions++;

        // If the suspended member was leader, trigger succession immediately
        if (personId.equals(org.getCurrentLeaderPersonId())) {
            org.clearLeader();
            org.evaluateSuccession(buildPersonMap(livingPeople), year, month);
            annualSuccessions++;
        }
        return true;
    }

    public synchronized boolean reinstateMember(String eventId, String orgId, String personId, int year, int month) {
        if (eventId != null && !processedSystemEventIds.add(eventId)) return false;
        Organization org = getOrganization(orgId);
        if (org == null || org.getStatus().isPermanentlyClosed()) return false;

        MembershipRecord rec = org.getMembershipRecord(personId);
        if (rec == null || rec.getStatus() != MembershipStatus.SUSPENDED) return false;

        rec.reinstate(year, month);
        return true;
    }

    public synchronized boolean leaveOrganization(
            String eventId,
            String orgId,
            String personId,
            int year,
            int month,
            String reason,
            Collection<Person> livingPeople) {

        if (eventId != null && !processedSystemEventIds.add(eventId)) return false;
        Organization org = getOrganization(orgId);
        if (org == null) return false;

        MembershipRecord rec = org.getMembershipRecord(personId);
        if (rec == null || !rec.getStatus().isCurrentAffiliation()) return false;

        rec.terminate(MembershipStatus.LEFT, year, month, reason != null ? reason : "Voluntary departure");
        annualDepartures++;

        Map<String, Person> peopleMap = buildPersonMap(livingPeople);
        Person person = peopleMap.get(personId);
        if (person != null) {
            person.removeActiveOrganizationId(orgId);
        }

        // Immediate leader clearing and succession
        if (personId.equals(org.getCurrentLeaderPersonId())) {
            org.clearLeader();
            org.evaluateSuccession(peopleMap, year, month);
            annualSuccessions++;
        }

        if (org.getActiveMemberCount() == 0) {
            org.setStatus(OrganizationStatus.DISSOLVED, year, month, "Zero active members remaining");
            dissolvedOrganizations.put(org.getOrganizationId(), org);
            annualDissolutions++;
        }
        return true;
    }

    public synchronized boolean expelMember(
            String eventId,
            String orgId,
            String personId,
            int year,
            int month,
            String reason,
            Collection<Person> livingPeople) {

        if (eventId != null && !processedSystemEventIds.add(eventId)) return false;
        Organization org = getOrganization(orgId);
        if (org == null) return false;

        MembershipRecord rec = org.getMembershipRecord(personId);
        if (rec == null || !rec.getStatus().isCurrentAffiliation()) return false;

        rec.terminate(MembershipStatus.EXPELLED, year, month, reason != null ? reason : "Expulsion by leadership");
        annualExpulsions++;

        Map<String, Person> peopleMap = buildPersonMap(livingPeople);
        Person person = peopleMap.get(personId);
        if (person != null) {
            person.removeActiveOrganizationId(orgId);
        }

        if (personId.equals(org.getCurrentLeaderPersonId())) {
            org.clearLeader();
            org.evaluateSuccession(peopleMap, year, month);
            annualSuccessions++;
        }

        if (org.getActiveMemberCount() == 0) {
            org.setStatus(OrganizationStatus.DISSOLVED, year, month, "Zero active members remaining");
            dissolvedOrganizations.put(org.getOrganizationId(), org);
            annualDissolutions++;
        }
        return true;
    }

    // --- Death Processing & Heap-Pruning ---

    /**
     * Authoritative death hook called when a person dies.
     * Transitions active memberships to terminal death status, immediately clears leadership if applicable,
     * triggers succession, and releases all references.
     */
    public synchronized void processMemberDeath(
            Person deceased,
            int currentYear,
            int currentMonth,
            String cause,
            Collection<Person> livingPeople) {

        if (deceased == null) return;
        String pId = String.valueOf(deceased.getId());
        Map<String, Person> peopleMap = buildPersonMap(livingPeople);

        for (Organization org : organizations.values()) {
            MembershipRecord rec = org.getMembershipRecord(pId);
            if (rec != null && rec.getStatus().isCurrentAffiliation()) {
                rec.markEndedByDeath(currentYear, currentMonth, cause);

                // If the deceased was leader, immediately clear and evaluate succession
                if (pId.equals(org.getCurrentLeaderPersonId())) {
                    org.clearLeader();
                    org.evaluateSuccession(peopleMap, currentYear, currentMonth);
                    annualSuccessions++;
                }

                if (org.getActiveMemberCount() == 0) {
                    org.setStatus(OrganizationStatus.DISSOLVED, currentYear, currentMonth, "All members deceased");
                    dissolvedOrganizations.put(org.getOrganizationId(), org);
                    annualDissolutions++;
                }
            }
        }

        // Release person's internal affiliation set for heap pruning
        deceased.clearOrganizations();
    }

    // --- Candidate Compatibility Evaluation ---

    public static class CompatibilityResult {
        private final boolean compatible;
        private final double affinityScore;
        private final String reason;

        public CompatibilityResult(boolean compatible, double affinityScore, String reason) {
            this.compatible = compatible;
            this.affinityScore = affinityScore;
            this.reason = reason != null ? reason : "";
        }

        public boolean isCompatible() { return compatible; }
        public double getAffinityScore() { return affinityScore; }
        public String getReason() { return reason; }
    }

    public CompatibilityResult evaluateMembershipCompatibility(Person person, Organization org) {
        if (person == null || !person.isAlive() || org == null) {
            return new CompatibilityResult(false, 0.0, "Person not alive or organization null");
        }

        OrganizationPolicyProfile policy = org.getPolicyProfile();

        // 1. Minimum age check
        if (person.getAge() < policy.getMinAge()) {
            return new CompatibilityResult(false, 0.0, "Under minimum age " + policy.getMinAge());
        }

        // 2. Settlement residency requirement
        if (policy.getRequiredSettlementId() != null) {
            if (person.getVillage() == null || person.getVillage().getId() != policy.getRequiredSettlementId()) {
                return new CompatibilityResult(false, 0.0, "Residency required in settlement " + policy.getRequiredSettlementId());
            }
        }

        // 3. Supernatural Awakening / Aspect requirement
        AetherAspect personAspect = (person.getAetherProfile() != null && person.getAetherProfile().isDarkAwakened())
                ? AetherAspect.DARK : AetherAspect.AETHERIC;

        if (policy.isRequiresAwakened() && !person.isAwakened()) {
            return new CompatibilityResult(false, 0.0, "Organization requires awakened supernatural state");
        }

        if (person.isAwakened() && !policy.isAspectAllowed(personAspect)) {
            return new CompatibilityResult(false, 0.0, "Supernatural aspect " + personAspect + " not permitted by policy");
        }

        // 4. Tier & Mastery requirements
        if (person.isAwakened() && person.getAetherProfile() != null) {
            if (person.getAetherProfile().getPowerTier() < policy.getMinTier()) {
                return new CompatibilityResult(false, 0.0, "Tier " + person.getAetherProfile().getPowerTier() + " below required " + policy.getMinTier());
            }
            if (person.getAetherProfile().getMastery() < policy.getMinMastery()) {
                return new CompatibilityResult(false, 0.0, "Mastery " + person.getAetherProfile().getMastery() + " below required " + policy.getMinMastery());
            }
        }

        // 5. Exclusivity check
        if (policy.isExclusiveMembershipRequired() && !person.getActiveOrganizationIds().isEmpty()) {
            return new CompatibilityResult(false, 0.0, "Exclusive membership required; person has active organizations");
        }

        // Base affinity score calculation
        double affinity = 50.0;
        if (org.getFounderPersonId() != null && person.getFamily() != null) {
            // Kinship affinity
            String fId = org.getFounderPersonId();
            if (fId.equals(String.valueOf(person.getId()))) {
                affinity += 50.0;
            }
        }
        if (org.getAlignment() == OrganizationAlignment.DARK_ALIGNED) {
            if (person.getAetherProfile() != null && person.getAetherProfile().isDarkAwakened()) {
                affinity += 30.0;
            }
        } else if (org.getAlignment() == OrganizationAlignment.AETHERIC_ALIGNED) {
            if (person.getAetherProfile() != null && !person.getAetherProfile().isDarkAwakened() && person.isAwakened()) {
                affinity += 20.0;
            }
        }

        return new CompatibilityResult(true, affinity, "Compatible");
    }

    // --- Inter-Organization Diplomacy ---

    public synchronized OrganizationRelationship getRelationship(String orgIdA, String orgIdB) {
        if (orgIdA == null || orgIdB == null) return null;
        return relationships.get(OrganizationRelationship.canonicalKey(orgIdA, orgIdB));
    }

    public synchronized double getRelationshipScore(String orgIdA, String orgIdB) {
        OrganizationRelationship rel = getRelationship(orgIdA, orgIdB);
        return rel != null ? rel.getRelationshipScore() : 0.0;
    }

    public synchronized DiplomaticStatus getDiplomaticStatus(String orgIdA, String orgIdB) {
        OrganizationRelationship rel = getRelationship(orgIdA, orgIdB);
        return rel != null ? rel.getDiplomaticStatus() : DiplomaticStatus.NEUTRAL;
    }

    public synchronized boolean areAllied(String orgIdA, String orgIdB) {
        if (orgIdA == null || orgIdB == null) return false;
        if (orgIdA.equals(orgIdB)) return true;
        OrganizationRelationship rel = getRelationship(orgIdA, orgIdB);
        return rel != null && rel.getDiplomaticStatus() == DiplomaticStatus.ALLIED && rel.isTreatyActive();
    }

    public synchronized boolean areHostile(String orgIdA, String orgIdB) {
        if (orgIdA == null || orgIdB == null || orgIdA.equals(orgIdB)) return false;
        OrganizationRelationship rel = getRelationship(orgIdA, orgIdB);
        return rel != null && (rel.getDiplomaticStatus().isHostile() || rel.getRelationshipScore() <= -75.0);
    }

    public synchronized OrganizationRelationship getOrCreateRelationship(String orgIdA, String orgIdB, int year, int month) {
        String key = OrganizationRelationship.canonicalKey(orgIdA, orgIdB);
        OrganizationRelationship rel = relationships.get(key);
        if (rel == null) {
            rel = new OrganizationRelationship(orgIdA, orgIdB, year, month);
            relationships.put(key, rel);
        }
        return rel;
    }

    public synchronized void adjustRelationship(
            String eventId,
            String orgIdA,
            String orgIdB,
            double delta,
            int year,
            int month) {

        if (eventId != null && !processedSystemEventIds.add(eventId)) return;
        OrganizationRelationship rel = getOrCreateRelationship(orgIdA, orgIdB, year, month);
        rel.adjustScore(delta, year, month);
        scanLoyaltyConflicts(year, month);
    }

    public synchronized void setDiplomaticStatus(
            String eventId,
            String orgIdA,
            String orgIdB,
            DiplomaticStatus status,
            int year,
            int month) {

        if (eventId != null && !processedSystemEventIds.add(eventId)) return;
        OrganizationRelationship rel = getOrCreateRelationship(orgIdA, orgIdB, year, month);
        rel.setDiplomaticStatus(status, year, month);
        scanLoyaltyConflicts(year, month);
    }

    public synchronized void signTreaty(
            String eventId,
            String orgIdA,
            String orgIdB,
            DiplomaticStatus treatyStatus,
            int year,
            int month) {

        if (eventId != null && !processedSystemEventIds.add(eventId)) return;
        OrganizationRelationship rel = getOrCreateRelationship(orgIdA, orgIdB, year, month);
        rel.setDiplomaticStatus(treatyStatus, year, month);
        annualTreatiesSigned++;

        Organization oA = getOrganization(orgIdA);
        Organization oB = getOrganization(orgIdB);
        if (oA != null) {
            oA.addEvent(new OrganizationEvent(eventId, orgIdA, year, month, OrganizationEvent.EventType.TREATY_SIGNED,
                    String.format("Signed formal treaty %s with %s", treatyStatus.getDisplayName(), orgIdB), null, orgIdB));
        }
        if (oB != null) {
            oB.addEvent(new OrganizationEvent(eventId, orgIdB, year, month, OrganizationEvent.EventType.TREATY_SIGNED,
                    String.format("Signed formal treaty %s with %s", treatyStatus.getDisplayName(), orgIdA), null, orgIdA));
        }
    }

    public synchronized void breakTreaty(
            String eventId,
            String orgIdA,
            String orgIdB,
            int year,
            int month,
            String reason) {

        if (eventId != null && !processedSystemEventIds.add(eventId)) return;
        OrganizationRelationship rel = getOrCreateRelationship(orgIdA, orgIdB, year, month);
        if (rel.isTreatyActive()) {
            rel.breakTreaty(year, month, 25.0);
            annualTreatiesBroken++;

            Organization oA = getOrganization(orgIdA);
            Organization oB = getOrganization(orgIdB);
            if (oA != null) {
                oA.addEvent(new OrganizationEvent(eventId, orgIdA, year, month, OrganizationEvent.EventType.TREATY_BROKEN,
                        String.format("Treaty with %s broken (%s)", orgIdB, reason != null ? reason : "hostilities"), null, orgIdB));
            }
            if (oB != null) {
                oB.addEvent(new OrganizationEvent(eventId, orgIdB, year, month, OrganizationEvent.EventType.TREATY_BROKEN,
                        String.format("Treaty with %s broken (%s)", orgIdA, reason != null ? reason : "hostilities"), null, orgIdA));
            }
        }
        scanLoyaltyConflicts(year, month);
    }

    // --- Multi-Membership Loyalty Conflict Detection ---

    public synchronized void scanLoyaltyConflicts(int year, int month) {
        for (OrganizationRelationship rel : relationships.values()) {
            if (rel.getDiplomaticStatus().isHostile() || rel.getRelationshipScore() <= -75.0) {
                Organization oA = getOrganization(rel.getOrganizationIdA());
                Organization oB = getOrganization(rel.getOrganizationIdB());
                if (oA == null || oB == null || !oA.getStatus().isActive() || !oB.getStatus().isActive()) continue;

                // Scan members who hold ACTIVE status in both
                for (MembershipRecord recA : oA.getActiveMembershipRecords()) {
                    if (oB.isMemberActive(recA.getPersonId())) {
                        String conflictId = "CONF:" + recA.getPersonId() + ":" + oA.getOrganizationId() + ":" + oB.getOrganizationId();
                        boolean alreadyLogged = false;
                        for (LoyaltyConflictRecord existing : loyaltyConflicts) {
                            if (existing.getConflictId().equals(conflictId) && !existing.isResolved()) {
                                alreadyLogged = true;
                                break;
                            }
                        }
                        if (!alreadyLogged) {
                            loyaltyConflicts.add(new LoyaltyConflictRecord(
                                    conflictId, recA.getPersonId(), oA.getOrganizationId(), oB.getOrganizationId(),
                                    year, month, "Active member in hostile organizations"
                            ));
                        }
                    }
                }
            }
        }
    }

    // --- Phase 2B.1 Combat Integration Bridge ---

    /**
     * Resolves encounter side grouping for proposed combatants deterministically.
     * Precedence 1: Explicit side if specified and not "DEFAULT".
     * Precedence 2: Primary organization allegiance.
     * Precedence 3: Active secondary organization or settlement ID.
     * Coalition Merging: Merges sides that share active ALLIED treaty status without conflict.
     */
    public synchronized Map<Combatant, String> resolveEncounterSides(List<Combatant> combatants) {
        Map<Combatant, String> resolvedSides = new LinkedHashMap<>();
        if (combatants == null || combatants.isEmpty()) return resolvedSides;

        // Step 1: Assign initial raw sides by precedence
        for (Combatant c : combatants) {
            if (c == null) continue;

            String explicit = c.getSideId();
            if (explicit != null && !explicit.trim().isEmpty() && !explicit.equals("DEFAULT")) {
                resolvedSides.put(c, explicit);
                continue;
            }

            Person p = c.getPerson();
            if (p != null) {
                if (p.getPrimaryOrganizationId() != null) {
                    resolvedSides.put(c, "ORG_" + p.getPrimaryOrganizationId());
                    continue;
                }
                if (!p.getActiveOrganizationIds().isEmpty()) {
                    resolvedSides.put(c, "ORG_" + p.getActiveOrganizationIds().iterator().next());
                    continue;
                }
                if (p.getVillage() != null) {
                    resolvedSides.put(c, "VILLAGE_" + p.getVillage().getId());
                    continue;
                }
            }

            resolvedSides.put(c, "SIDE_" + c.getCombatantId());
        }

        // Step 2: Merge allied coalitions where formal ALLIED treaties exist
        Map<String, String> sideAliases = new HashMap<>();
        List<String> distinctSides = new ArrayList<>(new HashSet<>(resolvedSides.values()));

        for (int i = 0; i < distinctSides.size(); i++) {
            String sideA = distinctSides.get(i);
            if (!sideA.startsWith("ORG_")) continue;
            String orgIdA = sideA.substring(4);

            for (int j = i + 1; j < distinctSides.size(); j++) {
                String sideB = distinctSides.get(j);
                if (!sideB.startsWith("ORG_")) continue;
                String orgIdB = sideB.substring(4);

                OrganizationRelationship rel = relationships.get(OrganizationRelationship.canonicalKey(orgIdA, orgIdB));
                if (rel != null && rel.getDiplomaticStatus() == DiplomaticStatus.ALLIED && rel.isTreatyActive()) {
                    // Merge sideB into sideA canonical coalition
                    String rootA = findRootAlias(sideA, sideAliases);
                    String rootB = findRootAlias(sideB, sideAliases);
                    if (!rootA.equals(rootB)) {
                        sideAliases.put(rootB, rootA);
                    }
                }
            }
        }

        // Apply aliasing to combatants
        for (Map.Entry<Combatant, String> entry : resolvedSides.entrySet()) {
            String initialSide = entry.getValue();
            String root = findRootAlias(initialSide, sideAliases);
            entry.setValue(root);
            entry.getKey().setSideId(root);
        }

        return resolvedSides;
    }

    private String findRootAlias(String side, Map<String, String> aliases) {
        String curr = side;
        while (aliases.containsKey(curr)) {
            curr = aliases.get(curr);
        }
        return curr;
    }

    /**
     * Consumes a completed Phase 2B.1 BattleResult idempotently.
     * Updates organization telemetry, applies relationship penalties for hostile engagements,
     * and ensures deceased member records are archived without double-killing combatants.
     */
    public synchronized void processCompletedBattleResult(
            BattleResult result,
            int year,
            int month,
            Collection<Person> livingPeople) {

        if (result == null || result.getBattleId() == null) return;
        if (!processedBattleIds.add(result.getBattleId())) {
            return; // Idempotent: already processed
        }

        String victorSide = result.getVictoriousSideId();
        Set<String> involvedOrgIds = new HashSet<>();

        // Identify participating organizations
        for (String side : result.getInitialCountsPerSide().keySet()) {
            if (side.startsWith("ORG_")) {
                involvedOrgIds.add(side.substring(4));
            }
        }

        // Update organization battle records
        for (String orgId : involvedOrgIds) {
            Organization org = getOrganization(orgId);
            if (org != null) {
                boolean isVictor = ("ORG_" + orgId).equals(victorSide);
                boolean isWithdrawn = (result.getOutcome() == BattleOutcome.WITHDRAWAL || result.getOutcome() == BattleOutcome.PARTIAL_WITHDRAWAL) && !isVictor;
                org.recordBattleEngagement(isVictor, isWithdrawn, result.getFatalities().size());
            }
        }

        // Process hostile relationship penalties and treaty breaks between combatant sides
        List<String> orgList = new ArrayList<>(involvedOrgIds);
        for (int i = 0; i < orgList.size(); i++) {
            for (int j = i + 1; j < orgList.size(); j++) {
                String orgA = orgList.get(i);
                String orgB = orgList.get(j);
                OrganizationRelationship rel = getOrCreateRelationship(orgA, orgB, year, month);

                if (rel.isTreatyActive()) {
                    breakTreaty("BATTLE_BREAK:" + result.getBattleId(), orgA, orgB, year, month,
                            "Armed combat engagement (" + result.getBattleId() + ")");
                }
                double penalty = -15.0 - (result.getFatalities().size() * 10.0);
                rel.adjustScore(penalty, year, month);
            }
        }

        // Verify deceased member records and succession for leaders
        Map<String, Person> peopleMap = buildPersonMap(livingPeople);
        for (String deadId : result.getFatalities()) {
            String pId = deadId.startsWith("PERSON_") ? deadId.substring(7) : deadId;
            for (String orgId : involvedOrgIds) {
                Organization org = getOrganization(orgId);
                if (org != null) {
                    MembershipRecord rec = org.getMembershipRecord(pId);
                    if (rec != null && rec.getStatus().isCurrentAffiliation()) {
                        rec.markEndedByDeath(year, month, "Slain in combat (" + result.getBattleId() + ")");
                        if (pId.equals(org.getCurrentLeaderPersonId())) {
                            org.clearLeader();
                            org.evaluateSuccession(peopleMap, year, month);
                            annualSuccessions++;
                        }
                    }
                }
            }
        }
    }

    // --- Annual Telemetry Summary ---

    public synchronized String generateAnnualOrganizationSummary(int currentYear, Collection<Person> livingPeople) {
        if (organizations.isEmpty() && dissolvedOrganizations.isEmpty()) {
            return null;
        }

        int activeOrgs = 0;
        int activeClans = 0;
        int activeOrders = 0;
        int activeCovens = 0;
        int activeGuards = 0;
        int activeOther = 0;

        int aethericAligned = 0;
        int darkAligned = 0;
        int neutralAligned = 0;
        int mixedAligned = 0;

        int totalActiveMemberships = 0;
        Set<String> uniqueAffiliatedPeople = new HashSet<>();
        int darkOrgMembers = 0;

        for (Organization o : organizations.values()) {
            if (o.getStatus().isActive()) {
                activeOrgs++;
                switch (o.getOrganizationType()) {
                    case CLAN: activeClans++; break;
                    case AWAKENED_ORDER: activeOrders++; break;
                    case DARK_COVEN: activeCovens++; break;
                    case SETTLEMENT_GUARD: activeGuards++; break;
                    default: activeOther++; break;
                }
                switch (o.getAlignment()) {
                    case AETHERIC_ALIGNED: aethericAligned++; break;
                    case DARK_ALIGNED: darkAligned++; break;
                    case NEUTRAL: neutralAligned++; break;
                    case MIXED: mixedAligned++; break;
                }
                for (MembershipRecord rec : o.getActiveMembershipRecords()) {
                    totalActiveMemberships++;
                    uniqueAffiliatedPeople.add(rec.getPersonId());
                    if (o.getAlignment() == OrganizationAlignment.DARK_ALIGNED) {
                        darkOrgMembers++;
                    }
                }
            }
        }

        int activeAlliances = 0;
        int activeHostilities = 0;
        for (OrganizationRelationship rel : relationships.values()) {
            if (rel.getDiplomaticStatus() == DiplomaticStatus.ALLIED) activeAlliances++;
            else if (rel.getDiplomaticStatus().isHostile()) activeHostilities++;
        }

        int unresolvedConflicts = 0;
        for (LoyaltyConflictRecord c : loyaltyConflicts) {
            if (!c.isResolved()) unresolvedConflicts++;
        }

        return String.format(
                "Orgs: %d [Clans:%d Orders:%d Covens:%d Grd:%d] | Align[Aeth:%d Dark:%d Neut:%d Mix:%d] | Memb:%d (Uniq:%d, DarkMemb:%d) | Diplo[Ally:%d Hostile:%d Conf:%d]",
                activeOrgs, activeClans, activeOrders, activeCovens, activeGuards,
                aethericAligned, darkAligned, neutralAligned, mixedAligned,
                totalActiveMemberships, uniqueAffiliatedPeople.size(), darkOrgMembers,
                activeAlliances, activeHostilities, unresolvedConflicts
        );
    }

    private Supplier<Collection<Person>> peopleSupplier;

    public void setPeopleSupplier(Supplier<Collection<Person>> peopleSupplier) {
        this.peopleSupplier = peopleSupplier;
    }

    public Supplier<Collection<Person>> getPeopleSupplier() {
        return peopleSupplier;
    }

    private Map<String, Person> buildPersonMap(Collection<Person> people) {
        Map<String, Person> map = new HashMap<>();
        Collection<Person> pool = (people != null && !people.isEmpty())
                ? people
                : (peopleSupplier != null ? peopleSupplier.get() : null);
        if (pool == null) return map;
        for (Person p : pool) {
            if (p != null) {
                map.put(String.valueOf(p.getId()), p);
                map.put("PERSON_" + p.getId(), p);
            }
        }
        return map;
    }
}
