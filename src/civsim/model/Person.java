package civsim.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Person {
    private final int id;
    private final String name;
    private final Gender gender;
    private final int birthYear;
    private final Village originVillage;
    private Person mother;
    private Person father;
    private final List<Person> children;

    private Family family;
    private int age;
    private Village village;
    private Occupation occupation;
    private double wealth;
    private double bankBalance;
    private double loanDebt;
    private int migrationCount;
    private boolean alive;
    private AetherProfile aetherProfile;
    private Integer deathYear;
    private Integer ageAtDeath;
    private String causeOfDeath;
    private HistoricalDeathRecord deathRecord;
    private boolean married;
    private Person spouse;
    private CombatState combatState;

    // Phase 2C.1: Persistent Equipment & Crafting Profile
    private final Map<EquipmentSlot, String> equippedItemIds = new EnumMap<>(EquipmentSlot.class);
    private boolean mainHandTwoHanded = false;
    private CrafterProfile crafterProfile;

    // Phase 2C.2: Feud & Grievance Disposition
    private FeudDispositionProfile feudDisposition;

    public Person(int id, String name, Gender gender, int age, int birthYear, Village village, Person mother, Person father, Family family, double initialWealth) {
        this.id = id;
        this.name = name;
        this.gender = gender;
        this.age = age;
        this.birthYear = birthYear;
        this.originVillage = village;
        this.village = village;
        this.mother = mother;
        this.father = father;
        this.family = family;
        this.children = new ArrayList<>();
        this.occupation = determineInitialOccupation(age);
        this.wealth = (age >= 19) ? initialWealth : 0.0;
        this.bankBalance = 0.0;
        this.loanDebt = 0.0;
        this.migrationCount = 0;
        this.alive = true;
        this.aetherProfile = null; // Set externally via setAetherProfile() or inheritance
        this.deathYear = null;
        this.ageAtDeath = null;
        this.married = false;
        this.spouse = null;
    }

    public Person(int id, String name, Gender gender, int age, int birthYear, Village village) {
        this(id, name, gender, age, birthYear, village, null, null, null, 50.0);
    }

    private Occupation determineInitialOccupation(int currentAge) {
        if (currentAge < 19) {
            return Occupation.CHILD;
        } else if (currentAge >= 65) {
            return Occupation.ELDER;
        } else {
            return Occupation.UNEMPLOYED;
        }
    }

    public void updateAgeAndOccupation() {
        if (!this.alive) {
            return;
        }
        this.age++;
        if (this.age < 19) {
            this.occupation = Occupation.CHILD;
        } else if (this.age >= 65) {
            this.occupation = Occupation.ELDER;
        } else if (this.occupation == Occupation.CHILD) {
            this.occupation = Occupation.UNEMPLOYED;
        }
    }

    public boolean markDeceased(int currentYear, String cause) {
        if (!this.alive) {
            return false;
        }
        this.alive = false;
        this.deathYear = currentYear;
        this.ageAtDeath = this.age;
        this.causeOfDeath = cause;
        this.occupation = Occupation.ELDER;
        this.married = false;
        this.spouse = null;
        return true;
    }

    public boolean markDeceased(int currentYear) {
        return markDeceased(currentYear, "natural causes / illness");
    }

    public void clearAfterDeath() {
        this.children.clear();
        this.spouse = null;
        this.clearOrganizations();
        if (this.aetherProfile != null && this.aetherProfile.getProgressionRecord() != null) {
            SupernaturalProgressionRecord rec = this.aetherProfile.getProgressionRecord();
            if (this.deathRecord != null) {
                rec.createDeathSummary(this.deathYear != null ? this.deathYear : 0,
                        deathRecord.getFinalTier(), deathRecord.getFinalLongevityOffset(),
                        deathRecord.getEffectiveNaturalLifespanCeilingAtDeath(),
                        deathRecord.getCauseOfDeath(), deathRecord.isNaturalDeath());
            } else {
                rec.createDeathSummary(this.deathYear != null ? this.deathYear : 0, this.aetherProfile.getPowerTier());
            }
            rec.clearEventIdsForHeapPruning();
        }
        if (this.aetherProfile != null) {
            this.aetherProfile.clearAttributeEventIdsForHeapPruning();
        }
        if (this.combatState != null) {
            this.combatState.clearEncounterEffects();
        }
    }

    public void marry(Person partner) {
        if (partner == null || this == partner) {
            throw new IllegalArgumentException("Invalid marriage partner.");
        }
        if (!this.alive || !partner.isAlive()) {
            throw new IllegalStateException("Dead people cannot marry.");
        }
        if (this.married || partner.isMarried()) {
            throw new IllegalStateException("One or both partners are already married.");
        }
        this.married = true;
        this.spouse = partner;
        partner.married = true;
        partner.spouse = this;
    }

    public void clearSpouseAfterPartnerDeath() {
        this.married = false;
        this.spouse = null;
    }

    public void addChild(Person child) {
        this.children.add(child);
    }

    public void recordMigration(Village destination) {
        this.village = destination;
        this.migrationCount++;
    }

    public void addWealth(double amount) {
        if (amount > 0) {
            this.wealth += amount;
        }
    }

    public boolean deductWealth(double amount) {
        if (amount > 0 && this.wealth >= amount) {
            this.wealth -= amount;
            return true;
        }
        return false;
    }

    public boolean isEligibleForMarriage(int minAgeExclusive) {
        return this.alive && !this.married && this.spouse == null && this.age > minAgeExclusive;
    }

    public boolean isCompatibleAgeWith(Person other, int maxAgeDifference) {
        if (other == null) {
            return false;
        }
        return Math.abs(this.age - other.age) <= maxAgeDifference;
    }

    public boolean isRelatedTo(Person other, int maxGenerations, boolean preventSameFamily) {
        if (other == null) return false;
        if (this.id == other.id) return true;

        if (preventSameFamily && this.family != null && other.family != null && this.family.getId() == other.family.getId()) {
            return true;
        }

        Map<Person, Integer> myAncestors = this.getAncestorsUpToDepth(maxGenerations);
        Map<Person, Integer> otherAncestors = other.getAncestorsUpToDepth(maxGenerations);

        for (Person ancestor : myAncestors.keySet()) {
            if (otherAncestors.containsKey(ancestor)) {
                return true;
            }
        }
        return false;
    }

    public Map<Person, Integer> getAncestorsUpToDepth(int maxDepth) {
        Map<Person, Integer> ancestors = new HashMap<>();
        collectAncestorsRecursive(this, 0, maxDepth, ancestors);
        return ancestors;
    }

    private void collectAncestorsRecursive(Person current, int currentDepth, int maxDepth, Map<Person, Integer> result) {
        if (current == null || currentDepth > maxDepth) {
            return;
        }
        Integer existingDepth = result.get(current);
        if (existingDepth == null || currentDepth < existingDepth) {
            result.put(current, currentDepth);
        }
        if (currentDepth < maxDepth) {
            collectAncestorsRecursive(current.mother, currentDepth + 1, maxDepth, result);
            collectAncestorsRecursive(current.father, currentDepth + 1, maxDepth, result);
        }
    }

    public String getFullName() {
        if (family != null) {
            return name + " " + family.getSurname();
        }
        return name;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public Gender getGender() { return gender; }
    public int getAge() { return age; }
    public int getBirthYear() { return birthYear; }
    public Village getOriginVillage() { return originVillage; }
    public Village getVillage() { return village; }
    public void setVillage(Village village) { this.village = village; }
    public Occupation getOccupation() { return occupation; }
    public void setOccupation(Occupation occupation) { this.occupation = occupation; }
    public double getWealth() { return wealth; }
    public double getBankBalance() { return bankBalance; }
    public void setBankBalance(double bankBalance) { this.bankBalance = bankBalance; }
    public double getLoanDebt() { return loanDebt; }
    public void setLoanDebt(double loanDebt) { this.loanDebt = loanDebt; }
    public int getMigrationCount() { return migrationCount; }
    public Family getFamily() { return family; }
    public void setFamily(Family family) { this.family = family; }
    public boolean isAlive() { return alive; }
    public Integer getDeathYear() { return deathYear; }
    public Integer getAgeAtDeath() { return ageAtDeath; }
    public String getCauseOfDeath() { return causeOfDeath; }
    public HistoricalDeathRecord getDeathRecord() { return deathRecord; }
    public void setDeathRecord(HistoricalDeathRecord deathRecord) { this.deathRecord = deathRecord; }
    public boolean isMarried() { return married; }
    public Person getSpouse() { return spouse; }
    public void setSpouse(Person spouse) {
        this.spouse = spouse;
        this.married = (spouse != null);
    }
    public Person getMother() { return mother; }
    public void setMother(Person mother) { this.mother = mother; }
    public Person getFather() { return father; }
    public void setFather(Person father) { this.father = father; }
    public List<Person> getChildren() { return Collections.unmodifiableList(children); }

    // --- Supernatural / Aether ---
    public AetherProfile getAetherProfile() { return aetherProfile; }
    public void setAetherProfile(AetherProfile aetherProfile) {
        this.aetherProfile = aetherProfile;
        if (this.combatState != null && aetherProfile != null) {
            double vit = aetherProfile.getEffectiveAttribute(AttributeType.VITALITY);
            double end = aetherProfile.getEffectiveAttribute(AttributeType.ENDURANCE);
            double aethCap = aetherProfile.getEffectiveAttribute(AttributeType.AETHER_CAPACITY);
            double aethCtrl = aetherProfile.getEffectiveAttribute(AttributeType.AETHER_CONTROL);
            boolean awk = isAwakened();
            if (!this.combatState.isInitializedInLife()) {
                this.combatState.initializeFullPools(vit, end, aethCap, aethCtrl, awk);
            } else {
                this.combatState.recalculateMaxPools(vit, end, aethCap, aethCtrl, awk);
            }
        }
    }
    public boolean isAwakened() { return aetherProfile != null && aetherProfile.isAwakened(); }

    // --- Tactical Combat ---
    public CombatState getCombatState() { return combatState; }
    public CombatState getOrCreateCombatState() {
        if (combatState == null) {
            combatState = new CombatState(this);
            double vit = (aetherProfile != null) ? aetherProfile.getEffectiveAttribute(AttributeType.VITALITY) : 20.0;
            double end = (aetherProfile != null) ? aetherProfile.getEffectiveAttribute(AttributeType.ENDURANCE) : 20.0;
            double aethCap = (aetherProfile != null) ? aetherProfile.getEffectiveAttribute(AttributeType.AETHER_CAPACITY) : 0.0;
            double aethCtrl = (aetherProfile != null) ? aetherProfile.getEffectiveAttribute(AttributeType.AETHER_CONTROL) : 0.0;
            boolean awk = isAwakened();
            combatState.initializeFullPools(vit, end, aethCap, aethCtrl, awk);
        } else if (combatState.getOwnerPerson() == null) {
            combatState.setOwnerPerson(this);
        }
        return combatState;
    }
    public void setCombatState(CombatState combatState) {
        this.combatState = combatState;
        if (combatState != null && combatState.getOwnerPerson() == null) {
            combatState.setOwnerPerson(this);
        }
    }

    public double getEffectiveAttribute(AttributeType attr) {
        if (aetherProfile != null) {
            return aetherProfile.getEffectiveAttributeDouble(attr);
        }
        return 20.0;
    }

    // --- Phase 2B.2: Organization & Faction Affiliations ---
    private String primaryOrganizationId;
    private final Set<String> activeOrganizationIds = new HashSet<>();

    public String getPrimaryOrganizationId() {
        return primaryOrganizationId;
    }

    public void setPrimaryOrganizationId(String primaryOrganizationId) {
        this.primaryOrganizationId = primaryOrganizationId;
        if (primaryOrganizationId != null) {
            this.activeOrganizationIds.add(primaryOrganizationId);
        }
    }

    public Set<String> getActiveOrganizationIds() {
        return Collections.unmodifiableSet(activeOrganizationIds);
    }

    public void addActiveOrganizationId(String orgId) {
        if (orgId != null) {
            this.activeOrganizationIds.add(orgId);
            if (this.primaryOrganizationId == null) {
                this.primaryOrganizationId = orgId;
            }
        }
    }

    public void removeActiveOrganizationId(String orgId) {
        if (orgId != null) {
            this.activeOrganizationIds.remove(orgId);
            if (orgId.equals(this.primaryOrganizationId)) {
                this.primaryOrganizationId = this.activeOrganizationIds.isEmpty() ? null : this.activeOrganizationIds.iterator().next();
            }
        }
    }

    public void clearOrganizations() {
        this.primaryOrganizationId = null;
        this.activeOrganizationIds.clear();
    }

    // --- Phase 2C.1 Equipment & Crafting ---
    public Map<EquipmentSlot, String> getEquippedItemIds() {
        return Collections.unmodifiableMap(equippedItemIds);
    }

    public String getEquippedItemId(EquipmentSlot slot) {
        if (slot == null) return null;
        return equippedItemIds.get(slot);
    }

    public boolean isMainHandTwoHanded() {
        return mainHandTwoHanded;
    }

    public String equipItem(EquipmentSlot slot, String equipmentId, boolean twoHanded) {
        if (slot == null || equipmentId == null) return null;
        String unequippedId = null;

        if (slot == EquipmentSlot.MAIN_HAND) {
            unequippedId = equippedItemIds.put(slot, equipmentId);
            this.mainHandTwoHanded = twoHanded;
            if (twoHanded) {
                // Equipping a two-handed weapon in MAIN_HAND unequips OFF_HAND
                equippedItemIds.remove(EquipmentSlot.OFF_HAND);
            }
        } else if (slot == EquipmentSlot.OFF_HAND) {
            if (this.mainHandTwoHanded) {
                // Equipping an item in OFF_HAND unequips two-handed MAIN_HAND weapon
                equippedItemIds.remove(EquipmentSlot.MAIN_HAND);
                this.mainHandTwoHanded = false;
            }
            unequippedId = equippedItemIds.put(slot, equipmentId);
        } else {
            unequippedId = equippedItemIds.put(slot, equipmentId);
        }
        return unequippedId;
    }

    public String unequipItem(EquipmentSlot slot) {
        if (slot == null) return null;
        if (slot == EquipmentSlot.MAIN_HAND) {
            this.mainHandTwoHanded = false;
        }
        return equippedItemIds.remove(slot);
    }

    public void clearAllEquippedItems() {
        equippedItemIds.clear();
        mainHandTwoHanded = false;
    }

    public CrafterProfile getCrafterProfile() {
        return crafterProfile;
    }

    public CrafterProfile getOrCreateCrafterProfile() {
        if (crafterProfile == null) {
            crafterProfile = new CrafterProfile();
        }
        return crafterProfile;
    }

    public void setCrafterProfile(CrafterProfile crafterProfile) {
        this.crafterProfile = crafterProfile;
    }

    public FeudDispositionProfile getFeudDisposition() {
        if (feudDisposition == null) {
            feudDisposition = FeudDispositionProfile.createDeterministic(this.id, 42L);
        }
        return feudDisposition;
    }

    public void setFeudDisposition(FeudDispositionProfile feudDisposition) {
        this.feudDisposition = feudDisposition;
    }
}
