package civsim.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Phase 2C.1 -- Formal Clan Heirloom wrapper referencing a persistent EquipmentItem.
 * Not a duplicate item: tracks legal title, heritage chronicle, bearer custody, and succession.
 */
public class ClanHeirloom {

    private final String heirloomId;
    private final String equipmentId;
    private final String owningClanId;
    private String artifactTitle;
    private final int registrationYear;
    private final int registrationMonth;

    private HeirloomSuccessionPolicy successionPolicy;
    private String currentBearerPersonId;
    private HeirloomCustodyStatus custodyStatus;

    private final List<String> pastBearerIds = new ArrayList<>();
    private final List<String> battleChronicle = new ArrayList<>();

    public ClanHeirloom(
            String heirloomId,
            String equipmentId,
            String owningClanId,
            String artifactTitle,
            int registrationYear,
            int registrationMonth,
            HeirloomSuccessionPolicy successionPolicy) {
        if (heirloomId == null || equipmentId == null || owningClanId == null) {
            throw new IllegalArgumentException("Heirloom ID, Equipment ID, and Owning Clan ID cannot be null.");
        }
        this.heirloomId = heirloomId;
        this.equipmentId = equipmentId;
        this.owningClanId = owningClanId;
        this.artifactTitle = artifactTitle != null ? artifactTitle : "Heirloom Relic";
        this.registrationYear = registrationYear;
        this.registrationMonth = registrationMonth;
        this.successionPolicy = successionPolicy != null ? successionPolicy : HeirloomSuccessionPolicy.MERIT_FIRST;
        this.currentBearerPersonId = null;
        this.custodyStatus = HeirloomCustodyStatus.IN_CLAN_VAULT;
    }

    public String getHeirloomId() { return heirloomId; }
    public String getEquipmentId() { return equipmentId; }
    public String getOwningClanId() { return owningClanId; }
    public String getArtifactTitle() { return artifactTitle; }
    public String getCustomName() { return artifactTitle; }
    public void setArtifactTitle(String title) { this.artifactTitle = title; }
    public int getRegistrationYear() { return registrationYear; }
    public int getRegistrationMonth() { return registrationMonth; }

    public HeirloomSuccessionPolicy getSuccessionPolicy() { return successionPolicy; }
    public void setSuccessionPolicy(HeirloomSuccessionPolicy policy) {
        if (policy != null) this.successionPolicy = policy;
    }

    public String getCurrentBearerPersonId() { return currentBearerPersonId; }
    public HeirloomCustodyStatus getCustodyStatus() { return custodyStatus; }

    public List<String> getPastBearerIds() { return Collections.unmodifiableList(pastBearerIds); }
    public List<String> getBattleChronicle() { return Collections.unmodifiableList(battleChronicle); }

    public void assignBearer(String bearerPersonId) {
        if (this.currentBearerPersonId != null && !this.currentBearerPersonId.equals(bearerPersonId)) {
            if (!this.pastBearerIds.contains(this.currentBearerPersonId)) {
                this.pastBearerIds.add(this.currentBearerPersonId);
            }
        }
        this.currentBearerPersonId = bearerPersonId;
        this.custodyStatus = (bearerPersonId != null) ? HeirloomCustodyStatus.BORNE_BY_MEMBER : HeirloomCustodyStatus.IN_CLAN_VAULT;
    }

    public void returnToVault() {
        if (this.currentBearerPersonId != null && !this.pastBearerIds.contains(this.currentBearerPersonId)) {
            this.pastBearerIds.add(this.currentBearerPersonId);
        }
        this.currentBearerPersonId = null;
        this.custodyStatus = HeirloomCustodyStatus.IN_CLAN_VAULT;
    }

    public void markLostAtSite(String siteId) {
        this.custodyStatus = HeirloomCustodyStatus.LOST_AT_SITE;
        recordDeed("Lost at battle/encounter site: " + siteId);
    }

    public void markCaptured(String capturerId, String context) {
        this.custodyStatus = HeirloomCustodyStatus.STOLEN_OR_CAPTURED;
        recordDeed("Captured by " + capturerId + " in " + context);
    }

    public void markRecovered(String recoveredByFactionId) {
        this.custodyStatus = HeirloomCustodyStatus.IN_CLAN_VAULT;
        this.currentBearerPersonId = null;
        recordDeed("Recovered by faction " + recoveredByFactionId);
    }

    public void recordDeed(String deed) {
        if (deed != null && !deed.trim().isEmpty()) {
            battleChronicle.add(deed);
        }
    }
}

