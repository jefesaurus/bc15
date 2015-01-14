package ghetto_v2_5.BotTypes;

import ghetto_v2_5.Cache;
import ghetto_v2_5.Messaging;
import ghetto_v2_5.SupplyDistribution;
import ghetto_v2_5.Util;
import ghetto_v2_5.RobotPlayer.BaseBot;
import ghetto_v2_5.RobotPlayer.MovingBot;
import ghetto_v2_5.RobotPlayer.MovingBot.AttackMode;
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
  private int MAX_MINERS = 30;
  
  public HQ(RobotController rc) {
    super(rc);
  }
  
  public void setup() throws GameActionException {
    SupplyDistribution.init(this);
    strat = HighLevelStrat.HARASS;
    SupplyDistribution.setBatteryMode();
  }
  
  public enum BaseState {
    UNDER_ATTACK,
    HARASSED,
    FINE
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
    int numMiners = Messaging.checkNumMiners();
    int numBeavers = rc.readBroadcast(Messaging.NUM_BEAVERS);
    SupplyDistribution.manageSupply();
    
    // This checks which enemy towers are still alive and broadcasts it to save bytecode across the fleet
    Messaging.setSurvivingEnemyTowers(Cache.getEnemyTowerLocationsDirect());
    //MapLocation fleetCentroid = Messaging.getFleetCentroid();
    int fleetCount = Messaging.getFleetCount();
    Messaging.resetFleetCentroid();
    Messaging.resetTowersUnderAttack();
    
    // Attack enemies if possible.
    RobotInfo[] enemies = getEnemiesInAttackingRange();
    if (enemies.length > 0) {
      if (rc.isWeaponReady()) {
        attackLeastHealthEnemy(enemies);
      }
    }
    
    // Spawn if possible
    if (Clock.getRoundNum() < 100 && numBeavers < 1 && rc.isCoreReady() && rc.hasSpawnRequirements(RobotType.BEAVER)) {
      Direction newDir = getOffensiveSpawnDirection(RobotType.BEAVER);
      if (newDir != null) {
        rc.spawn(newDir, RobotType.BEAVER);
        rc.broadcast(Messaging.NUM_BEAVERS, numBeavers + 1);
        Messaging.queueMiners(MAX_MINERS);
      }
    }
    
    // If we are currently winning in towers, and we are under attack, pull back and defend.
    boolean haveMoreTowers = weHaveMoreTowers();
    boolean towersUnderAttack = Messaging.getClosestTowerUnderAttack() != null;
    if (strat != HighLevelStrat.TOWER_DEFENDING && haveMoreTowers && towersUnderAttack) {
      defendTowers();
      rc.yield();
      return;
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

        //approachTower(getNearestEnemyTower(Cache.getEnemyTowerLocationsDirect()));
        approachTower(getMostVulnerableEnemyTower(Cache.getEnemyTowerLocationsDirect()));
        
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
      if (haveDecentSurround(currentTargetTower)) {// fleetCentroid.distanceSquaredTo(currentTargetTower) < 50) {
        MapLocation[] enemyTowers = Cache.getEnemyTowerLocationsDirect();
        // If there are no more towers, then we are engaging the HQ
        if (enemyTowers.length == 0) {
          diveTowerUnsafe(currentTargetTower);
        } else {
          int minDistSquared = Integer.MAX_VALUE;
          int tempDist;
          for (int i = enemyTowers.length; i-- > 0;) {
            tempDist = currentTargetTower.distanceSquaredTo(enemyTowers[i]);
            if (tempDist > 0 && tempDist < minDistSquared) {
              minDistSquared = tempDist;
            }
          }
          if (minDistSquared < 9) {
            diveTowerUnsafe(currentTargetTower);
          } else {
            diveTowerSafe(currentTargetTower);
          }
        }
      }
      break;
    case TOWER_DIVING:
      // If we're winning in tower count, switch to TOWER_DEFENDING
      MapLocation[] enemyTowers = Cache.getEnemyTowerLocationsDirect();

      if (enemyTowers.length > 0) {
        // Check if our current target is dead yet:
        boolean targetIsDead = true;
        for (int i = enemyTowers.length; i-- > 0;) {
          if (enemyTowers[i].equals(currentTargetTower)) {
            targetIsDead = false;
          }
        }
        
        if (targetIsDead && weHaveMoreTowers()) {
          // defendTowers();
          approachTower(getMostVulnerableEnemyTower(enemyTowers));

        // Retreat
        } else if (fleetCount < FLEET_COUNT_ATTACK_THRESHOLD/3) {
          buildForces();
        }
      } else {
        // defendTowers();
        if (!currentTargetTower.equals(enemyHQ)) {
          approachTower(enemyHQ);
        }
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
  
  public void diveTowerSafe(MapLocation towerLoc) throws GameActionException {
    strat = HighLevelStrat.TOWER_DIVING;
    setRallyPoint(towerLoc);
    setFleetMode(MovingBot.AttackMode.SAFE_TOWER_DIVE);
  }
  
  public void diveTowerUnsafe(MapLocation towerLoc) throws GameActionException {
    strat = HighLevelStrat.TOWER_DIVING;
    setRallyPoint(towerLoc);
    setFleetMode(MovingBot.AttackMode.UNSAFE_TOWER_DIVE);
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
    if (enemyTowers.length <= 0) {
      return null;
    }
    double tempDist;

    double closestDist = myHQ.distanceSquaredTo(enemyTowers[0]);
    int closestIndex = 0;
    for (int i = 1; i < enemyTowers.length; i++) {
      tempDist = myHQ.distanceSquaredTo(enemyTowers[i]);
      if (tempDist < closestDist) {
        closestDist = tempDist;
        closestIndex = i;
      }
    }
    return enemyTowers[closestIndex];
  }
  
  private MapLocation getMostVulnerableEnemyTower(MapLocation[] enemyTowers) throws GameActionException {
    if (enemyTowers.length <= 0) {
      return null;
    }
    
    int towerX = enemyHQ.x;
    int towerY = enemyHQ.y;
    
    for (int i = enemyTowers.length; i-- > 0;) {
      towerX += enemyTowers[i].x;
      towerY += enemyTowers[i].y;
    }
    
    MapLocation towerCenter = new MapLocation(towerX/(1 + enemyTowers.length), towerY/(1 + enemyTowers.length));


    double tempDist;
    double tempDist2;
    double furthestDist = enemyTowers[0].distanceSquaredTo(enemyTowers[0]);
    int furthestIndex = 0;
    
    for (int i = 1; i < enemyTowers.length; i++) {
      tempDist = towerCenter.distanceSquaredTo(enemyTowers[i]);
      tempDist2 = furthestDist - tempDist;
      
      // If the difference is close, choose the one that is closer to our HQ
      if (tempDist2 < 1.0 && tempDist2 > -1.0) {
        if (enemyTowers[i].distanceSquaredTo(myHQ) < enemyTowers[furthestIndex].distanceSquaredTo(myHQ)) {
          furthestDist = tempDist;
          furthestIndex = i;
        }
      } else 
        if (tempDist > furthestDist) {
        furthestDist = tempDist;
        furthestIndex = i;
      }
    }
    System.out.println("Targeting: " + enemyTowers[furthestIndex]);
    return enemyTowers[furthestIndex];
  }
  
  public boolean haveDecentSurround(MapLocation loc) {
    return (rc.senseNearbyRobots(loc, 63, myTeam).length > FLEET_COUNT_ATTACK_THRESHOLD);
  }
}