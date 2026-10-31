package civsim.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Phase 2B.1 — Immutable combat encounter request submitted to the tactical combat engine.
 */
public final class BattleRequest {
    private final String battleId;
    private final int simulationYear;
    private final int simulationMonth;
    private final BattleType battleType;
    private final double posX;
    private final double posY;
    private final double terrainFortificationModifier;
    private final List<Combatant> combatants;

    public BattleRequest(String battleId, int simulationYear, int simulationMonth,
                         BattleType battleType, double posX, double posY,
                         double terrainFortificationModifier, List<Combatant> combatants) {
        this.battleId = battleId;
        this.simulationYear = simulationYear;
        this.simulationMonth = simulationMonth;
        this.battleType = battleType != null ? battleType : BattleType.SETTLEMENT_WARFARE;
        this.posX = posX;
        this.posY = posY;
        this.terrainFortificationModifier = terrainFortificationModifier > 0.0 ? terrainFortificationModifier : 1.0;
        this.combatants = (combatants != null) ? Collections.unmodifiableList(new ArrayList<>(combatants)) : Collections.emptyList();
    }

    public BattleRequest(String battleId, int simulationYear, int simulationMonth,
                         BattleType battleType, List<Combatant> combatants) {
        this(battleId, simulationYear, simulationMonth, battleType, 0.0, 0.0, 1.0, combatants);
    }

    public String getBattleId() { return battleId; }
    public int getSimulationYear() { return simulationYear; }
    public int getSimulationMonth() { return simulationMonth; }
    public BattleType getBattleType() { return battleType; }
    public double getPosX() { return posX; }
    public double getPosY() { return posY; }
    public double getTerrainFortificationModifier() { return terrainFortificationModifier; }
    public List<Combatant> getCombatants() { return combatants; }
}
