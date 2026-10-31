package civsim.model;

/**
 * Phase 2C.1 -- Custody status of a registered Clan Heirloom.
 */
public enum HeirloomCustodyStatus {
    IN_CLAN_VAULT("In Clan Vault"),
    BORNE_BY_MEMBER("Borne by Member"),
    LOST_AT_SITE("Lost at Battle / Site"),
    STOLEN_OR_CAPTURED("Stolen or Captured"),
    DISPUTED("Disputed Title");

    private final String displayName;

    HeirloomCustodyStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isHeldByClan() {
        return this == IN_CLAN_VAULT || this == BORNE_BY_MEMBER;
    }
}

