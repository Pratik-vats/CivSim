package civsim.economy;

/**
 * Multi-tiered currency system with strict 1:100 exchange ratios:
 * - 100 Iron (i)     = 1 Silver (s)
 * - 100 Silver (s)   = 1 Gold (g)      [10,000 Iron]
 * - 100 Gold (g)     = 1 Platinum (p)  [1,000,000 Iron]
 * - 100 Platinum (p) = 1 Astral (a)    [100,000,000 Iron]
 */
public final class Currency {
    public static final double IRON_BASE = 1.0;
    public static final double SILVER_BASE = 100.0;
    public static final double GOLD_BASE = 10_000.0;
    public static final double PLATINUM_BASE = 1_000_000.0;
    public static final double ASTRAL_BASE = 100_000_000.0;

    private Currency() {}

    /**
     * Compact representation designed for fixed-width table columns (e.g. 5-7 chars):
     * Examples: "45i", "12.4s", "54.2g", "18.5p", "42.1a"
     */
    public static String formatCompact(double amountInIron) {
        if (Double.isNaN(amountInIron) || amountInIron <= 0.0) {
            return "0i";
        }
        if (amountInIron >= ASTRAL_BASE) {
            double v = amountInIron / ASTRAL_BASE;
            return v >= 1000 ? String.format("%.0fa", v) : String.format("%.1fa", v);
        } else if (amountInIron >= PLATINUM_BASE) {
            double v = amountInIron / PLATINUM_BASE;
            return v >= 1000 ? String.format("%.0fp", v) : String.format("%.1fp", v);
        } else if (amountInIron >= GOLD_BASE) {
            double v = amountInIron / GOLD_BASE;
            return v >= 1000 ? String.format("%.0fg", v) : String.format("%.1fg", v);
        } else if (amountInIron >= SILVER_BASE) {
            double v = amountInIron / SILVER_BASE;
            return v >= 1000 ? String.format("%.0fs", v) : String.format("%.1fs", v);
        } else {
            return String.format("%.0fi", amountInIron);
        }
    }

    /**
     * Human-readable formatted string for summary banners and economic reports:
     * Examples: "50 Iron", "12.5 Silv", "450.0 Gold", "12.4 Plat", "2.1 Astral"
     */
    public static String formatHeader(double amountInIron) {
        if (Double.isNaN(amountInIron) || amountInIron <= 0.0) {
            return "0 Iron";
        }
        if (amountInIron >= ASTRAL_BASE) {
            double v = amountInIron / ASTRAL_BASE;
            return v >= 1000 ? String.format("%.1fk Ast", v / 1000.0) : String.format("%.1f Ast", v);
        } else if (amountInIron >= PLATINUM_BASE) {
            double v = amountInIron / PLATINUM_BASE;
            return v >= 1000 ? String.format("%.1fk Plat", v / 1000.0) : String.format("%.1f Plat", v);
        } else if (amountInIron >= GOLD_BASE) {
            double v = amountInIron / GOLD_BASE;
            return v >= 1000 ? String.format("%.1fk Gold", v / 1000.0) : String.format("%.1f Gold", v);
        } else if (amountInIron >= SILVER_BASE) {
            double v = amountInIron / SILVER_BASE;
            return v >= 1000 ? String.format("%.1fk Silv", v / 1000.0) : String.format("%.1f Silv", v);
        } else {
            return String.format("%.1f Iron", amountInIron);
        }
    }

    /**
     * Multi-denomination breakdown:
     * Example: "2p 45g 10s 5i"
     */
    public static String formatDetailed(double amountInIron) {
        long remaining = (long) Math.floor(Math.max(0.0, amountInIron));
        long astral = remaining / (long) ASTRAL_BASE;
        remaining %= (long) ASTRAL_BASE;
        long plat = remaining / (long) PLATINUM_BASE;
        remaining %= (long) PLATINUM_BASE;
        long gold = remaining / (long) GOLD_BASE;
        remaining %= (long) GOLD_BASE;
        long silver = remaining / (long) SILVER_BASE;
        long iron = remaining % (long) SILVER_BASE;

        StringBuilder sb = new StringBuilder();
        if (astral > 0) sb.append(astral).append("a ");
        if (plat > 0 || astral > 0) sb.append(plat).append("p ");
        if (gold > 0 || plat > 0) sb.append(gold).append("g ");
        if (silver > 0 || gold > 0) sb.append(silver).append("s ");
        sb.append(iron).append("i");
        return sb.toString().trim();
    }
}
