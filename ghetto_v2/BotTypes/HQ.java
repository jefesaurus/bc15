package ghetto_v2.BotTypes;

import ghetto_v2.Cache;
import ghetto_v2.Messaging;
import ghetto_v2.SupplyDistribution;
import ghetto_v2.Util;
import ghetto_v2.RobotPlayer.BaseBot;
import ghetto_v2.RobotPlayer.MovingBot;
import ghetto_v2.RobotPlayer.MovingBot.AttackMode;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class HQ extends BaseBot {
  public HighLevelStrat strat;
  public AttackMode currentFleetMode;
  public MapLocation currentRallyPoint = new MapLocation(0,0);
  public MapLocation currentTargetTower = new MapLocation(0,0);
  private int towersLeft = 6;
  
  public HQ(RobotController rc) {
    super(rc);
  }
  
  public void setup() throws GameActionException {
    SupplyDistribution.init(this);
    strat = HighLevelStrat.HARASS;
  }
  
  public enum HighLevelStrat {
    HARASS,
    BUILDING_FORCES,
    SWARMING,
    APPROACHING_TOWER,
    TOWER_DIVING,
    TOWER_DEFENDING
  };
  
  public static final int FLEET_COUNT_ATTACK_THRESHOLD = 15;

  public void execute() throws GameActionException {
    int numBeavers = rc.readBroadcast(Messaging.NUM_BEAVERS);
    SupplyDistribution.setBatteryMode();
    if (Clock.getRoundNum() > 300) {
      SupplyDistribution.manageSupply();
    }
    // This checks which enemy towers are still alive and broadcasts it to save bytecode across the fleet
    Messaging.setSurvivingEnemyTowers(Cache.getEnemyTowerLocationsDirect());
    MapLocation fleetCentroid = Messaging.getFleetCentroid();
    int fleetCount = Messaging.getFleetCount();
    Messaging.resetFleetCentroid();
    
    // Attack enemies if possible.
    RobotInfo[] enemies = getEnemiesInAttackingRange();
    if (enemies.length > 0) {
      if (rc.isWeaponReady()) {
        attackLeastHealthEnemy(enemies);
      }
    }
    
    // Spawn if possible
    if (rc.isCoreReady() && rc.getTeamOre() > 100 && numBeavers < 1) {
      Direction newDir = getOffensiveSpawnDirection(RobotType.BEAVER);
      if (newDir != null) {
        rc.spawn(newDir, RobotType.BEAVER);
        rc.broadcast(Messaging.NUM_BEAVERS, numBeavers + 1);
        Messaging.queueMiners(10);
      }
    }
    
    switch (strat) {
    case HARASS:
      if (Clock.getRoundNum() >= 600) {
        buildForces();
        break;
      }
      setFleetMode(MovingBot.AttackMode.HUNT_FOR_MINERS);
      break;
    case BUILDING_FORCES:
      if (Clock.getRoundNum() >= 600 && fleetCount > FLEET_COUNT_ATTACK_THRESHOLD) {
        if (weHaveMoreTowers()) {
          defendTowers();
        } else {
          approachTower(getNearestEnemyTower(Cache.getEnemyTowerLocationsDirect()));
        }
      }
      break;
    case SWARMING:
      // Set rally point anywhere
      setRallyPoint(new MapLocation((this.myHQ.x + this.enemyHQ.x) / 2,(this.myHQ.y + this.enemyHQ.y) / 2));
      setFleetMode(MovingBot.AttackMode.RALLYING);
      break;
    case APPROACHING_TOWER:
      // Set rally point to just in front of nearest tower.
      // Wait until ally centroid is epsilon close, then switch to tower diving
      if (fleetCentroid.distanceSquaredTo(currentTargetTower) < 50 && !weHaveMoreTowers()) {
        diveTower(currentTargetTower);
      }
      break;
    case TOWER_DIVING:
      // If we're winning in tower count, switch to TOWER_DEFENDING
      if (weHaveMoreTowers()) {
        defendTowers();
        
      // Retreat
      } else if (fleetCount < FLEET_COUNT_ATTACK_THRESHOLD/3) {
        buildForces();
      }
      break;
    case TOWER_DEFENDING:
      // Switch to tower diving if they have equal to or more towers
      if (!weHaveMoreTowers()) {
        buildForces();
      }
      break;
    }
    
    rc.yield();
  }
  
  public void approachTower(MapLocation towerLoc) throws GameActionException {
    strat = HighLevelStrat.APPROACHING_TOWER;
    currentTargetTower = towerLoc;
    setRallyPoint(currentTargetTower);
    setFleetMode(MovingBot.AttackMode.OFFENSIVE_SWARM);
  }
  
  public void diveTower(MapLocation towerLoc) throws GameActionException {
    strat = HighLevelStrat.TOWER_DIVING;
    setRallyPoint(towerLoc);
    setFleetMode(MovingBot.AttackMode.TOWER_DIVE);
  }
  
  public void defendTowers() throws GameActionException {
    strat = HighLevelStrat.TOWER_DEFENDING;
    setFleetMode(MovingBot.AttackMode.DEFEND_TOWERS);
    setRallyPoint(myHQ);
  }
  
  public void buildForces() throws GameActionException {
    strat = HighLevelStrat.BUILDING_FORCES;
    setFleetMode(MovingBot.AttackMode.OFFENSIVE_SWARM);
    setRallyPoint(myHQ);
  }
  
  
  // Messaging wrappers to save bytecode on redundant messages
  public void setFleetMode(AttackMode newMode) throws GameActionException {
    if (currentFleetMode != newMode) {
      Messaging.setFleetMode(newMode);
      currentFleetMode = newMode;
    }
  }
  
  public void setRallyPoint(MapLocation rallyPoint) throws GameActionException {
    if (rallyPoint.x != currentRallyPoint.x || rallyPoint.y != currentRallyPoint.y) {
      Messaging.setRallyPoint(rallyPoint);
      currentRallyPoint = rallyPoint;
    }
  }
  
  public boolean weHaveMoreTowers() throws GameActionException {
    return rc.senseTowerLocations().length > Cache.getEnemyTowerLocationsDirect().length;
  }
  
  /*
   * Senses enemy towers and sets the soldier rally point to the nearest one
   */
  private MapLocation getNearestEnemyTower(MapLocation[] enemyTowers) throws GameActionException {
    if (towersLeft <= 0) {
      return null;
    }
    double tempDist;

    double closestDist = myHQ.distanceSquaredTo(enemyTowers[0]);
    int closestIndex = 0;
    for (int i = 1; i < towersLeft; i++) {
      tempDist = myHQ.distanceSquaredTo(enemyTowers[i]);
      if (tempDist < closestDist) {
        closestDist = tempDist;
        closestIndex = i;
      }
    }
    return enemyTowers[closestIndex];
  }
}
