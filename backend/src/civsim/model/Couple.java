package civsim.model;

public class Couple {
    private final int id;
    private final Person husband;
    private final Person wife;
    private final int marriageYear;
    private final long marriageTotalMonths;
    private final int targetChildren;

    private int childrenBorn;
    private Long lastBirthTotalMonths;
    private long nextScheduledBirthMonth;
    private boolean active;

    public Couple(int id, Person husband, Person wife, int marriageYear, long marriageTotalMonths, int targetChildren, int minBirthIntervalMonths) {
        this.id = id;
        this.husband = husband;
        this.wife = wife;
        this.marriageYear = marriageYear;
        this.marriageTotalMonths = marriageTotalMonths;
        this.targetChildren = targetChildren;
        this.childrenBorn = 0;
        this.lastBirthTotalMonths = null;
        this.nextScheduledBirthMonth = marriageTotalMonths + minBirthIntervalMonths;
        this.active = true;
    }

    public boolean isFertile(int maxFemaleFertilityAge) {
        if (!active) {
            return false;
        }
        if (!husband.isAlive() || !wife.isAlive()) {
            return false;
        }
        if (childrenBorn >= targetChildren) {
            return false;
        }
        return wife.getAge() <= maxFemaleFertilityAge;
    }

    public boolean canHaveChildNow(long currentTotalMonths, int minBirthIntervalMonths, int maxFemaleFertilityAge) {
        if (!active) {
            return false;
        }
        if (!husband.isAlive() || !wife.isAlive()) {
            deactivate();
            return false;
        }
        if (!isFertile(maxFemaleFertilityAge)) {
            return false;
        }
        if (currentTotalMonths < marriageTotalMonths + minBirthIntervalMonths) {
            return false;
        }
        if (lastBirthTotalMonths != null && (currentTotalMonths - lastBirthTotalMonths) < minBirthIntervalMonths) {
            return false;
        }
        return currentTotalMonths >= nextScheduledBirthMonth;
    }

    public void recordBirth(long currentTotalMonths, int minBirthIntervalMonths) {
        this.childrenBorn++;
        this.lastBirthTotalMonths = currentTotalMonths;
        this.nextScheduledBirthMonth = currentTotalMonths + minBirthIntervalMonths;
    }

    public void deactivate() {
        this.active = false;
    }

    public int getId() { return id; }
    public Person getHusband() { return husband; }
    public Person getWife() { return wife; }
    public int getMarriageYear() { return marriageYear; }
    public long getMarriageTotalMonths() { return marriageTotalMonths; }
    public int getTargetChildren() { return targetChildren; }
    public int getChildrenBorn() { return childrenBorn; }
    public void setChildrenBorn(int childrenBorn) { this.childrenBorn = childrenBorn; }
    public Long getLastBirthTotalMonths() { return lastBirthTotalMonths; }
    public void setLastBirthTotalMonths(Long lastBirthTotalMonths) { this.lastBirthTotalMonths = lastBirthTotalMonths; }
    public long getNextScheduledBirthMonth() { return nextScheduledBirthMonth; }
    public void setNextScheduledBirthMonth(long nextScheduledBirthMonth) { this.nextScheduledBirthMonth = nextScheduledBirthMonth; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
