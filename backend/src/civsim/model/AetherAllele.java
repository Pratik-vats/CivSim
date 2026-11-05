package civsim.model;

/**
 * Two-allele Mendelian model for hereditary Aether potential.
 * Each person carries two alleles. At least one DOMINANT allele
 * is required for viable awakening potential.
 *
 * Inheritance: Each parent contributes one random allele to the child.
 * - DD × DD → always DD (100% awakening-viable)
 * - DD × Dr → 50% DD, 50% Dr (all viable)
 * - Dr × Dr → 25% DD, 50% Dr, 25% rr (75% viable)
 * - Dr × rr → 50% Dr, 50% rr (50% viable)
 * - rr × rr → always rr (0% viable, dormant lineage)
 */
public enum AetherAllele {
    /** Dominant allele — carries active Aether potential */
    DOMINANT("D"),
    /** Recessive allele — dormant, no Aether potential alone */
    RECESSIVE("r");

    private final String symbol;

    AetherAllele(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }

    public boolean isDominant() {
        return this == DOMINANT;
    }
}
