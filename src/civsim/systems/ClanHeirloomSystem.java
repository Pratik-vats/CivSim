package civsim.systems;

import civsim.model.*;

import java.util.*;

/**
 * Phase 2C.1 -- Clan Heirloom System governing legal title, heritage chronicle,
 * bearer succession, durability wear, and battlefield capture/recovery.
 * Enforces the No-Teleport Rule on remote bearer death and emits grievances on heirloom theft.
 */
public class ClanHeirloomSystem {

    private final Map<String, ClanHeirloom> heirlooms = new LinkedHashMap<>();
    private final Map<String, Organization> registeredClans = new LinkedHashMap<>();
    private final List<HistoricalGrievanceRecord> heirloomGrievances = new ArrayList<>();

    // Sequence & Telemetry
    private int nextHeirloomSeq = 1;
    private int heirloomsRegisteredCount = 0;
    private int heirloomsInheritedCount = 0;
    private int heirloomsLostCount = 0;
    private int heirloomsStolenOrCapturedCount = 0;
    private int heirloomsRecoveredCount = 0;

    private static volatile ClanHeirloomSystem activeInstance = null;
    public static ClanHeirloomSystem getActiveInstance() { return activeInstance; }
    public static void setActiveInstance(ClanHeirloomSystem instance) { activeInstance = instance; }

    public ClanHeirloomSystem() {}

    public synchronized ClanHeirloom registerHeirloom(
            EquipmentItem item,
            Organization clan,
            String artifactTitle,
            HeirloomSuccessionPolicy policy,
            int currentYear,
            int currentMonth) {

        if (item == null || clan == null) return null;

        String heirloomId = "HEIRLOOM_" + (nextHeirloomSeq++);
        ClanHeirloom heirloom = new ClanHeirloom(
                heirloomId,
                item.getEquipmentId(),
                clan.getOrganizationId(),
                artifactTitle != null ? artifactTitle : item.getDisplayName(),
                currentYear,
                currentMonth,
                policy != null ? policy : HeirloomSuccessionPolicy.MERIT_FIRST
        );

        item.setRegisteredHeirloom(true);
        item.setHeirloomId(heirloomId);
        item.setCurrentOwnerId(clan.getOrganizationId());
        item.setArtifactTitle(heirloom.getArtifactTitle());

        clan.registerHeirloom(heirloom);
        registeredClans.put(clan.getOrganizationId(), clan);
        heirlooms.put(heirloomId, heirloom);
        heirloomsRegisteredCount++;

        return heirloom;
    }

    public ClanHeirloom getHeirloom(String heirloomId) {
        return heirlooms.get(heirloomId);
    }

    public Map<String, ClanHeirloom> getAllHeirlooms() {
        return Collections.unmodifiableMap(heirlooms);
    }

    public List<HistoricalGrievanceRecord> getHeirloomGrievances() {
        return Collections.unmodifiableList(heirloomGrievances);
    }

    /**
     * Assigns an heirloom to an active clan member as bearer.
     */
    public synchronized boolean assignBearer(ClanHeirloom heirloom, EquipmentItem item, Person bearer) {
        if (heirloom == null || item == null || bearer == null || !bearer.isAlive()) {
            return false;
        }

        // Check item compatibility with bearer
        if (!item.isCompatibleWith(bearer)) {
            return false;
        }

        bearer.equipItem(item.getSlot(), item.getEquipmentId(), item.isTwoHanded());
        item.setLifecycleStatus(EquipmentLifecycleStatus.EQUIPPED);
        item.setCurrentCustodianId("PERSON_" + bearer.getId());
        if (bearer.getVillage() != null) {
            item.setPosition(bearer.getVillage().getX(), bearer.getVillage().getY());
        }

        heirloom.assignBearer(String.valueOf(bearer.getId()));
        return true;
    }

    /**
     * Executes succession for an heirloom whose bearer has died or departed.
     */
    public synchronized Person executeSuccession(
            ClanHeirloom heirloom,
            EquipmentItem item,
            Organization clan,
            Map<Integer, Person> peopleMap,
            Random random) {

        if (heirloom == null || item == null || clan == null) return null;
        if (peopleMap == null) peopleMap = Collections.emptyMap();

        List<Person> eligibleCandidates = new ArrayList<>();
        for (MembershipRecord rec : clan.getAllMembershipRecords().values()) {
            if (rec.getStatus() == MembershipStatus.ACTIVE) {
                try {
                    int pId = Integer.parseInt(rec.getPersonId());
                    Person p = peopleMap.get(pId);
                    if (p != null && p.isAlive() && item.isCompatibleWith(p)) {
                        eligibleCandidates.add(p);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        if (eligibleCandidates.isEmpty()) {
            // No eligible bearer: return to clan vault
            item.setLifecycleStatus(EquipmentLifecycleStatus.IN_CLAN_VAULT);
            item.setCurrentCustodianId("VAULT_" + clan.getOrganizationId());
            heirloom.returnToVault();
            return null;
        }

        Person chosen = null;
        switch (heirloom.getSuccessionPolicy()) {
            case BLOODLINE_FIRST: {
                // Look for living children of current or past bearers
                String lastBearerId = heirloom.getCurrentBearerPersonId();
                if (lastBearerId == null && !heirloom.getPastBearerIds().isEmpty()) {
                    lastBearerId = heirloom.getPastBearerIds().get(heirloom.getPastBearerIds().size() - 1);
                }
                if (lastBearerId != null) {
                    for (Person candidate : eligibleCandidates) {
                        if (candidate.getFather() != null && String.valueOf(candidate.getFather().getId()).equals(lastBearerId)) {
                            chosen = candidate;
                            break;
                        }
                        if (candidate.getMother() != null && String.valueOf(candidate.getMother().getId()).equals(lastBearerId)) {
                            chosen = candidate;
                            break;
                        }
                    }
                }
                // Fall back to merit if no bloodline candidate
                if (chosen == null) {
                    chosen = selectHighestMeritCandidate(eligibleCandidates);
                }
                break;
            }

            case MERIT_FIRST: {
                chosen = selectHighestMeritCandidate(eligibleCandidates);
                break;
            }

            case LEADER_DESIGNATED:
            case COUNCIL_DECISION: {
                // If leader is eligible, leader claims it; else highest merit
                String leaderId = clan.getCurrentLeaderPersonId();
                if (leaderId != null) {
                    for (Person candidate : eligibleCandidates) {
                        if (String.valueOf(candidate.getId()).equals(leaderId)) {
                            chosen = candidate;
                            break;
                        }
                    }
                }
                if (chosen == null) {
                    chosen = selectHighestMeritCandidate(eligibleCandidates);
                }
                break;
            }
        }

        if (chosen != null) {
            assignBearer(heirloom, item, chosen);
            heirloomsInheritedCount++;
        } else {
            item.setLifecycleStatus(EquipmentLifecycleStatus.IN_CLAN_VAULT);
            item.setCurrentCustodianId("VAULT_" + clan.getOrganizationId());
            heirloom.returnToVault();
        }

        return chosen;
    }

    private Person selectHighestMeritCandidate(List<Person> candidates) {
        if (candidates.isEmpty()) return null;
        Person best = null;
        int bestTier = -1;
        double bestMastery = -1.0;

        for (Person p : candidates) {
            int tier = (p.isAwakened() && p.getAetherProfile() != null) ? p.getAetherProfile().getPowerTier() : 0;
            double mastery = (p.isAwakened() && p.getAetherProfile() != null) ? p.getAetherProfile().getMastery() : 0.0;

            if (tier > bestTier || (tier == bestTier && mastery > bestMastery)) {
                best = p;
                bestTier = tier;
                bestMastery = mastery;
            }
        }
        return best;
    }

    /**
     * Applies combat durability wear to a person's equipped items following an encounter.
     * Bounded between 0.5 and 2.5 points, mitigated by craftQuality and REINFORCEMENT enchantments.
     */
    public void applyCombatDurabilityWear(
            Person combatant,
            EquipmentCraftingSystem craftingSystem,
            Random random) {

        if (combatant == null || craftingSystem == null) return;
        Map<EquipmentSlot, String> equipped = combatant.getEquippedItemIds();

        for (String eqId : equipped.values()) {
            EquipmentItem item = craftingSystem.getEquipment(eqId);
            if (item == null) continue;

            double baseWear = 0.5 + (random.nextDouble() * 2.0); // [0.5, 2.5]
            double qualityMitigation = Math.max(0.70, 1.0 - (item.getCraftQuality() / 250.0));
            double encMitigation = 1.0;

            for (EnchantmentRecord enc : item.getEnchantments()) {
                if (enc.getFamily() == EnchantmentFamily.REINFORCEMENT) {
                    encMitigation = Math.max(0.50, 1.0 - (enc.getPrimaryBonus() / 50.0));
                    break;
                }
            }

            double actualWear = baseWear * qualityMitigation * encMitigation;
            item.applyDurabilityLoss(actualWear);
        }
    }

    /**
     * Handles bearer death on a remote battlefield or anomaly site.
     * Enforces the No-Teleport Rule: items remain at site as UNRECOVERED_AT_SITE.
     */
    public synchronized void handleBearerDeathAtSite(
            Person deceased,
            String siteOrBattleId,
            EquipmentCraftingSystem craftingSystem) {

        if (deceased == null || craftingSystem == null) return;

        Map<EquipmentSlot, String> equipped = new HashMap<>(deceased.getEquippedItemIds());
        for (String eqId : equipped.values()) {
            EquipmentItem item = craftingSystem.getEquipment(eqId);
            if (item == null) continue;

            item.setLifecycleStatus(EquipmentLifecycleStatus.UNRECOVERED_AT_SITE);
            item.setCurrentCustodianId(siteOrBattleId != null ? siteOrBattleId : "UNKNOWN_BATTLE");

            if (item.isRegisteredHeirloom() && item.getHeirloomId() != null) {
                ClanHeirloom heirloom = heirlooms.get(item.getHeirloomId());
                if (heirloom != null) {
                    heirloom.markLostAtSite(siteOrBattleId);
                    heirloomsLostCount++;
                }
            }
        }
        deceased.clearAllEquippedItems();
    }

    /**
     * Post-battle site securing resolution:
     * If friendly coalition secures site -> friendly recovery.
     * If enemy coalition secures site -> hostile capture & grievance emission.
     */
    public synchronized void resolveBattlefieldItemRecovery(
            String siteOrBattleId,
            boolean friendlySecured,
            String recoveringFactionId,
            Organization recoveringOrg,
            String capturingEnemySideId,
            List<String> enemyOrgIds,
            EquipmentCraftingSystem craftingSystem,
            OrganizationSystem organizationSystem,
            Map<Integer, Person> peopleMap,
            int currentYear,
            int currentMonth,
            Random random) {

        if (craftingSystem == null) return;

        for (EquipmentItem item : craftingSystem.getAllEquipment().values()) {
            if (item.getLifecycleStatus() == EquipmentLifecycleStatus.UNRECOVERED_AT_SITE
                    && siteOrBattleId.equals(item.getCurrentCustodianId())) {

                if (friendlySecured) {
                    // --- Friendly Recovery ---
                    if (item.isRegisteredHeirloom() && item.getHeirloomId() != null) {
                        ClanHeirloom heirloom = heirlooms.get(item.getHeirloomId());
                        if (heirloom != null) {
                            heirloom.markRecovered(recoveringFactionId);
                            heirloomsRecoveredCount++;

                            Organization owningClan = (recoveringOrg != null) ? recoveringOrg
                                    : (organizationSystem != null ? organizationSystem.getOrganization(heirloom.getOwningClanId())
                                    : registeredClans.get(heirloom.getOwningClanId()));
                            if (owningClan != null) {
                                executeSuccession(heirloom, item, owningClan, peopleMap, random);
                            } else {
                                item.setLifecycleStatus(EquipmentLifecycleStatus.IN_CLAN_VAULT);
                            }
                        }
                    } else {
                        // Personal/mundane item recovered into settlement/clan inventory
                        item.setLifecycleStatus(EquipmentLifecycleStatus.IN_INVENTORY);
                        item.setCurrentCustodianId(recoveringFactionId);
                    }
                } else {
                    // --- Hostile Capture / Heirloom Theft ---
                    item.setLifecycleStatus(EquipmentLifecycleStatus.IN_INVENTORY);
                    item.setCurrentCustodianId(capturingEnemySideId);

                    if (item.isRegisteredHeirloom() && item.getHeirloomId() != null) {
                        ClanHeirloom heirloom = heirlooms.get(item.getHeirloomId());
                        if (heirloom != null) {
                            heirloom.markCaptured(capturingEnemySideId, siteOrBattleId);
                            heirloomsStolenOrCapturedCount++;

                            // Emit HistoricalGrievanceRecord(HEIRLOOM_OR_EQUIPMENT_CAPTURED)
                            String grievanceId = "GRIEV_HEIRLOOM_" + item.getHeirloomId() + "_" + currentYear + "_" + currentMonth;
                            HistoricalGrievanceRecord grievance = new HistoricalGrievanceRecord(
                                    grievanceId,
                                    siteOrBattleId,
                                    null,
                                    null,
                                    currentYear,
                                    currentMonth,
                                    null,
                                    0,
                                    heirloom.getOwningClanId(),
                                    null,
                                    null,
                                    capturingEnemySideId,
                                    enemyOrgIds != null ? enemyOrgIds : Collections.emptyList(),
                                    "Heirloom " + heirloom.getArtifactTitle() + " captured by enemy in battle " + siteOrBattleId,
                                    "HEIRLOOM_OR_EQUIPMENT_CAPTURED"
                            );
                            heirloomGrievances.add(grievance);
                        }
                    }
                }
            }
        }
    }

    /**
     * Resolves natural/domestic death of a bearer in their home settlement.
     * Personal gear passes to heirs; Clan Heirlooms return to clan and execute succession.
     */
    public synchronized void handleBearerDomesticDeath(
            Person deceased,
            Person spouse,
            List<Person> children,
            Village homeVillage,
            EquipmentCraftingSystem craftingSystem,
            OrganizationSystem organizationSystem,
            Map<Integer, Person> peopleMap,
            Random random) {

        if (deceased == null || craftingSystem == null) return;

        Map<EquipmentSlot, String> equipped = new HashMap<>(deceased.getEquippedItemIds());
        for (String eqId : equipped.values()) {
            EquipmentItem item = craftingSystem.getEquipment(eqId);
            if (item == null) continue;

            if (item.isRegisteredHeirloom() && item.getHeirloomId() != null) {
                // Clan Heirloom: NEVER enters personal estate
                ClanHeirloom heirloom = heirlooms.get(item.getHeirloomId());
                if (heirloom != null) {
                    Organization owningClan = (organizationSystem != null)
                            ? organizationSystem.getOrganization(heirloom.getOwningClanId())
                            : registeredClans.get(heirloom.getOwningClanId());
                    if (owningClan != null) {
                        executeSuccession(heirloom, item, owningClan, peopleMap, random);
                    } else {
                        item.setLifecycleStatus(EquipmentLifecycleStatus.IN_CLAN_VAULT);
                        heirloom.returnToVault();
                    }
                }
            } else {
                // Personal Equipment: Passes to spouse or living child
                Person heir = (spouse != null && spouse.isAlive()) ? spouse : null;
                if (heir == null && children != null) {
                    for (Person c : children) {
                        if (c.isAlive()) {
                            heir = c;
                            break;
                        }
                    }
                }

                if (heir != null && item.isCompatibleWith(heir)) {
                    heir.equipItem(item.getSlot(), item.getEquipmentId(), item.isTwoHanded());
                    item.setCurrentOwnerId("PERSON_" + heir.getId());
                    item.setCurrentCustodianId("PERSON_" + heir.getId());
                    item.setLifecycleStatus(EquipmentLifecycleStatus.EQUIPPED);
                } else if (homeVillage != null) {
                    homeVillage.addEquipment(item);
                }
            }
        }
        deceased.clearAllEquippedItems();
    }

    // Telemetry Getters
    public int getHeirloomsRegisteredCount() { return heirloomsRegisteredCount; }
    public int getHeirloomsInheritedCount() { return heirloomsInheritedCount; }
    public int getHeirloomsLostCount() { return heirloomsLostCount; }
    public int getHeirloomsStolenOrCapturedCount() { return heirloomsStolenOrCapturedCount; }
    public int getHeirloomsRecoveredCount() { return heirloomsRecoveredCount; }
}
