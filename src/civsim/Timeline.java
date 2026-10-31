package civsim;

public class Timeline {
    private int year;
    private int month;
    private long totalElapsedMonths;

    public Timeline() {
        this.year = 1;
        this.month = 0;
        this.totalElapsedMonths = 0;
    }

    public synchronized boolean isYearEnd() {
        return month == 11;
    }

    /**
     * Advances simulation time by 1 month.
     * @return true if a full year (12 months) just completed and rolled over to a new year.
     */
    public synchronized boolean advanceMonth() {
        totalElapsedMonths++;
        month++;
        if (month >= 12) {
            month = 0;
            year++;
            return true;
        }
        return false;
    }

    public synchronized int getYear() {
        return year;
    }

    public synchronized void setYearAndMonth(int year, int month) {
        this.year = year;
        this.month = month;
        this.totalElapsedMonths = (long) Math.max(0, year - 1) * 12 + month;
    }

    public synchronized int getMonth() {
        return month;
    }

    public synchronized long getTotalElapsedMonths() {
        return totalElapsedMonths;
    }

    public synchronized Season getCurrentSeason() {
        return Season.forMonth(month);
    }

    @Override
    public synchronized String toString() {
        return String.format("Year %d, Month %d (Total Months: %d)", year, month, totalElapsedMonths);
    }
}
