package civsim.model;

/**
 * Phase 2B.5 — Supernatural and biological materials dropped by monsters upon combat defeat.
 */
public enum MonsterMaterialType {
    MONSTER_CORE("Monster Core", true),
    BEAST_HIDE("Beast Hide", false),
    MONSTER_BONE("Monster Bone", false),
    AETHER_RESIDUE("Aether Residue", true),
    CHITIN_PLATE("Chitin Plate", false),
    VENOM_SAC("Venom Sac", false),
    HYDRA_BLOOD("Hydra Blood", true),
    SIREN_VOCAL_CORD("Siren Vocal Cord", true),
    SPECTRAL_DUST("Spectral Dust", true),
    SPATIAL_SHARD("Spatial Shard", true);

    private final String displayName;
    private final boolean supernatural;

    MonsterMaterialType(String displayName, boolean supernatural) {
        this.displayName = displayName;
        this.supernatural = supernatural;
    }

    public String getDisplayName() { return displayName; }
    public boolean isSupernatural() { return supernatural; }
}

