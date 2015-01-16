package ghetto_v2_5.BotTypes;

import ghetto_v2_5.BuildOrder;
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
  
  
  public boolean isSafeTowerDive = true;
  public MapLocation currentTargetTower = new MapLocation(0,0);
  private int MAX_MINERS = 20;
  
  public HQ(RobotController rc) {
    super(rc);
    maintainUnitComposition();
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
    //Messaging.setSurvivingEnemyTowers(Cache.getEnemyTowerLocationsDirect());
    //MapLocation fleetCentroid = Messaging.getFleetCentroid();
    int fleetCount = Messaging.getFleetCount();
    Messaging.resetFleetCentroid();
    Messaging.resetTowersUnderAttack();
    
    int range_squared = 24;
    int numTowers = rc.senseTowerLocations().length;
    if (numTowers >= 5) {
      range_squared = 51;
    } else if (numTowers >= 2) {
      range_squared = 35;
    }
    
    // Attack enemies if possible.
    RobotInfo[] enemies = rc.senseNearbyRobots(range_squared, rc.getTeam().opponent());
    if (enemies.length > 0) {
      if (rc.isWeaponReady()) {
        hqAttack(enemies, range_squared);
      }
    }
    
    /*
    // Spawn if possible
    if (numBeavers < 1 && rc.isCoreReady() && rc.hasSpawnRequirements(RobotType.BEAVER)) {
      Direction newDir = getOffensiveSpawnDirection(RobotType.BEAVER);
      if (newDir != null) {
        rc.spawn(newDir, RobotType.BEAVER);
        rc.broadcast(Messaging.NUM_BEAVERS, numBeavers + 1);
      }
    }
    */
    maintainUnitComposition();
    
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
        setCurrentTowerTarget(Cache.getEnemyTowerLocationsDirect());
        approachTower(currentTargetTower);
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
          if (isSafeTowerDive) {
            diveTowerSafe(currentTargetTower);
          } else {
            diveTowerUnsafe(currentTargetTower);
          }
        }
      }
      break;
    case TOWER_DIVING:
      // If we're winning in tower count, switch to TOWER_DEFENDING
      MapLocation[] enemyTowers = Cache.getEnemyTowerLocationsDirect();
      if (enemyTowers.length > 0) {
        // Check if our current target is dead yet:
        boolean targetIsDead = currentTargetTowerIsDead(enemyTowers);
        
        if (targetIsDead) {
          // defendTowers();
          setCurrentTowerTarget(enemyTowers);
          approachTower(currentTargetTower);

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
  
  public void hqAttack(RobotInfo[] enemies, int range_squared) throws GameActionException {
    if (enemies.length == 0) {
      return;
    }

    double minEnergon = Double.MAX_VALUE;
    MapLocation toAttack = null;
    MapLocation toAttackInRange = null;
    boolean attackable = true;
    
    for (int i = enemies.length; i-- > 0;) {
      if (enemies[i].health < minEnergon) {
        toAttack = enemies[i].location;
        minEnergon = enemies[i].health;
        if (range_squared > 35 && enemies[i].location.distanceSquaredTo(this.myHQ) <= 35) {
          toAttackInRange = enemies[i].location;
        }
      }
    }
    
    if (rc.getLocation().distanceSquaredTo(toAttack) > 35) {
      attackable = false;
      Direction[] dirs = Direction.values();
      for (Direction d : dirs) {
        MapLocation trialToAttack = toAttack.add(d);
        if (trialToAttack.distanceSquaredTo(this.myHQ) <= 35) {
          toAttack = trialToAttack;
          attackable = true;
          break;
        }
      }
    }
    
    if (range_squared == 25) {
      System.out.println(rc.getLocation().distanceSquaredTo(toAttack));
    }
    
    if (attackable) {
      rc.attackLocation(toAttack);
    } else {
      if (toAttackInRange == null) {
        return;
      } else {
        rc.attackLocation(toAttackInRange);
      }
    }
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
  

  
  /*
   * This sets two class scope variables: the target tower and whether or not to approach the target "safely"
   * 
   * It determines the target to be the tower with the Maximum minimum distance to another tower or HQ. That is, the one that is furthest away from the others
   * The safety metric is basically just to check whether this tower is directly adjacent to another tower, in which case it must ignore danger from untargeted towers.
   */
  public void setCurrentTowerTarget(MapLocation[] enemyTowers) throws GameActionException {
    if (enemyTowers.length <= 0) {
      currentTargetTower = null;
    }
    
    double maxiMinDist = 0;
    int chosenIndex = 0;
    
    // Keep already computed distances around.
    int[][] distMat = new int[enemyTowers.length][enemyTowers.length];
    
    int tempDist = 0;
    int minDist;
    for (int i = enemyTowers.length; i-- > 0;) {
      minDist = enemyTowers[i].distanceSquaredTo(enemyHQ);
      for (int j = enemyTowers.length; j-- > i + 1;) {
        if (distMat[i][j] < minDist) {
          minDist = distMat[i][j];
        }
      }
      for (int j = i; j-- > 0;) {
        tempDist = enemyTowers[i].distanceSquaredTo(enemyTowers[j]);
        distMat[j][i] = tempDist;
        if (tempDist < minDist) {
          minDist = tempDist;
        }
      }

      if ((minDist - maxiMinDist) < 2 && (minDist - maxiMinDist) > -2) {
        if (enemyTowers[i].distanceSquaredTo(myHQ) < enemyTowers[chosenIndex].distanceSquaredTo(myHQ)) {
          maxiMinDist = minDist;
          chosenIndex = i;
        }
      } else if (minDist > maxiMinDist) {
        maxiMinDist = minDist;
        chosenIndex = i;
      }
    }    
    
    isSafeTowerDive = (maxiMinDist > 9);
    currentTargetTower = enemyTowers[chosenIndex];
  }
  
  public boolean currentTargetTowerIsDead(MapLocation[] enemyTowers) {
    for (int i = enemyTowers.length; i-- > 0;) {
      if (enemyTowers[i].equals(currentTargetTower)) {
        return false;
      }
    }
    return true;
  }
  
  public boolean haveDecentSurround(MapLocation loc) {
    return (rc.senseNearbyRobots(loc, 63, myTeam).length > 10);
  }

  
   /*     // Spawn if possible
    if (Clock.getRoundNum() < 100 && numBeavers < 1 && rc.isCoreReady() && rc.hasSpawnRequirements(RobotType.BEAVER)) {
      Direction newDir = getOffensiveSpawnDirection(RobotType.BEAVER);
      if (newDir != null) {
        rc.spawn(newDir, RobotType.BEAVER);
        rc.broadcast(Messaging.NUM_BEAVERS, numBeavers + 1);
        Messaging.queueMiners(MAX_MINERS);
      }
    }
    */
  
   
   
  public static final int NUM_BEAVERS = 1;
  public static final int NUM_MINER_FACTORIES = 1;
  public static final int NUM_BARRACKS = 1;
  public static final int NUM_TANK_FACTORIES = 1;
  

  public void maintainUnitComposition() throws GameActionException {
    if (maintainConstantUnits()) {
      maintainOreProduction();
    }
    return;
  }
  
  // Returns true if constant requirements are met.
  public boolean maintainConstantUnits() {
    if (Messaging.checkTotalNumUnits(RobotType.BEAVER) < NUM_BEAVERS) {
      Messaging.setUnitToProduce(RobotType.BEAVER);
      Messaging.queueUnits(RobotType.BEAVER, 1);
      return false;
    } else if (Messaging.checkTotalNumUnits(RobotType.MINERFACTORY) < NUM_MINER_FACTORIES) {
      Messaging.setUnitToProduce(RobotType.MINERFACTORY);
      Messaging.queueUnits(RobotType.MINERFACTORY, 1);
      return false;
    }
    return true;
  }
  
  static final int MAX_NUM_MINERS = 20;
  // Get estimated ore production and compare it to the value required by our current unit output and/or desired future unit output.
  public boolean maintainOreProduction() {
    int numMiners = Messaging.checkTotalNumUnits(RobotType.MINER);
    int minersNeeded = 20 - numMiners;
    if (numMiners < MAX_NUM_MINERS) {
      Messaging.setUnitToProduce(null);
      Messaging.queueUnits(RobotType.MINER, minersNeeded);
    }
    return true;
  }
  /*
  
  // Get estimated unit production and compare it to 
  public boolean maintainUnitProduction() {
    return true;
  }
  */
  
  public boolean produceUnits() {
    return true;
  }
 
  
  /*
   * Old code to find a vulnerable tower based on centroid. doesn't work very well.
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
    
    return enemyTowers[furthestIndex];
  }
  */
}
