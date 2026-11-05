package civsim.model;

import civsim.spatial.Biome;

import java.util.*;

/**
 * Phase 2B.5 — Persistent Monster Lair spawned from mutated World Anomalies.
 * Acts as the autonomous ecological hub for reproduction, territorial pressure, and MonsterTide mobilization.
 */
public class MonsterLair {

    private final String lairId;
    private final String sourceAnomalyId;
    private final double posX;
    private final double posY;
    private final Biome biome;
    private final SupernaturalResourceType sourceResourceType;
    private final ResourceGrade sourceResourceGrade;
    private double aetherSignature;

    private String primarySpeciesId;
    private String secondarySpeciesId;
    private final Set<String> monsterIds = new LinkedHashSet<>();

    private int populationCapacity;
    private MonsterThreatRank dominantThreatRank;
    private double territoryRadius;
    private MonsterLairState state;

    private int lastSpawnMonth;
    private int nextScheduledSpawnMonth;
    private int lastInvasionMonth;
    private double suppressionLevel;

    public MonsterLair(
            String lairId,
            String sourceAnomalyId,
            double posX,
            double posY,
            Biome biome,
            SupernaturalResourceType sourceResourceType,
            ResourceGrade sourceResourceGrade,
            String primarySpeciesId,
            String secondarySpeciesId,
            int initialCapacity,
            double territoryRadius,
            MonsterThreatRank dominantThreatRank,
            int creationMonth) {

        this.lairId = lairId;
        this.sourceAnomalyId = sourceAnomalyId;
        this.posX = posX;
        this.posY = posY;
        this.biome = biome != null ? biome : Biome.FOREST;
        this.sourceResourceType = sourceResourceType != null ? sourceResourceType : SupernaturalResourceType.RAW_AETHER_CRYSTAL;
        this.sourceResourceGrade = sourceResourceGrade != null ? sourceResourceGrade : ResourceGrade.COMMON;

        this.primarySpeciesId = primarySpeciesId;
        this.secondarySpeciesId = secondarySpeciesId;
        this.populationCapacity = Math.max(10, initialCapacity);
        this.territoryRadius = Math.max(10.0, territoryRadius);
        this.dominantThreatRank = dominantThreatRank != null ? dominantThreatRank : MonsterThreatRank.STANDARD;

        this.state = MonsterLairState.ACTIVE;
        this.aetherSignature = 100.0 * (1.0 + 0.5 * this.sourceResourceGrade.getGradeStep());
        this.lastSpawnMonth = creationMonth;
        this.nextScheduledSpawnMonth = creationMonth + 6;
        this.lastInvasionMonth = -100;
        this.suppressionLevel = 0.0;
    }

    public String getLairId() { return lairId; }
    public String getSourceAnomalyId() { return sourceAnomalyId; }
    public double getPosX() { return posX; }
    public double getPosY() { return posY; }
    public Biome getBiome() { return biome; }
    public SupernaturalResourceType getSourceResourceType() { return sourceResourceType; }
    public ResourceGrade getSourceResourceGrade() { return sourceResourceGrade; }

    public double getAetherSignature() { return aetherSignature; }
    public void setAetherSignature(double sig) { this.aetherSignature = Math.max(0.0, sig); }

    public String getPrimarySpeciesId() { return primarySpeciesId; }
    public void setPrimarySpeciesId(String id) { this.primarySpeciesId = id; }
    public String getSecondarySpeciesId() { return secondarySpeciesId; }
    public void setSecondarySpeciesId(String id) { this.secondarySpeciesId = id; }

    public Set<String> getMonsterIds() { return Collections.unmodifiableSet(monsterIds); }
    public int getLivingMonsterCount() { return monsterIds.size(); }
    public int getMonsterCount() { return getLivingMonsterCount(); }
    public boolean isActive() { return state == MonsterLairState.ACTIVE; }

    public boolean addMonster(String monsterId) {
        if (monsterId != null) {
            return monsterIds.add(monsterId);
        }
        return false;
    }

    public boolean removeMonster(String monsterId) {
        return monsterIds.remove(monsterId);
    }

    public int getPopulationCapacity() { return populationCapacity; }
    public void setPopulationCapacity(int cap) { this.populationCapacity = cap; }
    public boolean isAtCapacity() { return monsterIds.size() >= populationCapacity; }

    public MonsterThreatRank getDominantThreatRank() { return dominantThreatRank; }
    public void setDominantThreatRank(MonsterThreatRank rank) { this.dominantThreatRank = rank; }

    public double getTerritoryRadius() { return territoryRadius; }
    public void setTerritoryRadius(double r) { this.territoryRadius = r; }

    public MonsterLairState getState() { return state; }
    public void setState(MonsterLairState state) { this.state = state; }

    public int getLastSpawnMonth() { return lastSpawnMonth; }
    public void setLastSpawnMonth(int m) { this.lastSpawnMonth = m; }

    public int getNextScheduledSpawnMonth() { return nextScheduledSpawnMonth; }
    public void setNextScheduledSpawnMonth(int m) { this.nextScheduledSpawnMonth = m; }

    public int getLastInvasionMonth() { return lastInvasionMonth; }
    public void setLastInvasionMonth(int m) { this.lastInvasionMonth = m; }

    public double getSuppressionLevel() { return suppressionLevel; }
    public void setSuppressionLevel(double sup) { this.suppressionLevel = Math.max(0.0, sup); }

    public boolean canSpawn() {
        return state == MonsterLairState.ACTIVE && !isAtCapacity();
    }

    public void transitionToDormant() {
        if (!state.isTerminal()) {
            this.state = MonsterLairState.DORMANT;
        }
    }

    public void transitionToActive() {
        if (!state.isTerminal()) {
            this.state = MonsterLairState.ACTIVE;
            this.suppressionLevel = 0.0;
        }
    }

    public void transitionToSuppressed(double level) {
        if (!state.isTerminal()) {
            this.state = MonsterLairState.SUPPRESSED;
            this.suppressionLevel = Math.max(this.suppressionLevel, level);
        }
    }

    public void transitionToDestroyed() {
        this.state = MonsterLairState.DESTROYED;
        this.aetherSignature = 0.0;
        this.territoryRadius = 0.0;
    }

    public void transitionToCollapsed() {
        this.state = MonsterLairState.COLLAPSED;
        this.aetherSignature = 0.0;
        this.territoryRadius = 0.0;
    }

    @Override
    public String toString() {
        return String.format("MonsterLair[%s (%s, %s): %s at (%.1f, %.1f) Pop:%d/%d Terr:%.1f]",
                lairId, sourceResourceType.getDisplayName(), sourceResourceGrade.getDisplayName(),
                state.getDisplayName(), posX, posY, monsterIds.size(), populationCapacity, territoryRadius);
    }
}

