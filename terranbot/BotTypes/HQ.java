package terranbot.BotTypes;

import terranbot.Cache;
import terranbot.Messaging;
import terranbot.MovingBot;
import terranbot.SupplyDistribution;
import terranbot.Util;
import terranbot.MovingBot.AttackMode;
import terranbot.RobotPlayer.BaseBot;
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
  
  public int curNumHelipads = 0;
  public int curNumBarracks = 0;
  public int curNumTankFactories = 0;
  public int curNumMiners = 0;
  public int curNumBeavers = 0;
  public int curNumMinerFactories = 0;
  public int curNumDrones = 0;
  public int curNumTanks = 0;
  public int curNumSupplyDepots = 0;
  public HQ(RobotController rc) {
    super(rc);
  }
  
  public void setup() throws GameActionException {
    SupplyDistribution.init(this);
    strat = HighLevelStrat.HARASS;
    SupplyDistribution.setBatteryMode();
    //Harass force
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
    Messaging.setUnitToProduce(null);
    curNumHelipads = Messaging.checkTotalNumUnits(RobotType.HELIPAD);
    curNumBarracks = Messaging.checkTotalNumUnits(RobotType.BARRACKS);
    curNumTankFactories = Messaging.checkTotalNumUnits(RobotType.TANKFACTORY);
    curNumBeavers = Messaging.checkTotalNumUnits(RobotType.BEAVER);
    curNumMiners = Messaging.checkTotalNumUnits(RobotType.MINER);
    curNumMinerFactories = Messaging.checkTotalNumUnits(RobotType.MINERFACTORY);
    curNumDrones = Messaging.checkTotalNumUnits(RobotType.DRONE);
    curNumTanks = Messaging.checkTotalNumUnits(RobotType.TANK);
    curNumSupplyDepots = Messaging.checkTotalNumUnits(RobotType.SUPPLYDEPOT);
    //System.out.println("num tanks: " + curNumTanks);
    //System.out.println("curNumBeavers: " + curNumBeavers + ", " + curNumMinerFactories + ", " + curNumHelipads + ", " + curNumBarracks);
    
    SupplyDistribution.manageSupply();
    
    // This checks which enemy towers are still alive and broadcasts it to save bytecode across the fleet
    //Messaging.setSurvivingEnemyTowers(Cache.getEnemyTowerLocationsDirect());
    
    
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
    produceUnits();
    doMacro();
    
    // rc.setIndicatorString(0, strat.name());
    
    // If we are currently winning in towers, and we are under attack, pull back and defend.
    boolean haveMoreTowers = weHaveMoreTowers();
    boolean towersUnderAttack = Messaging.getClosestTowerUnderAttack() != null;
    if (strat != HighLevelStrat.TOWER_DEFENDING && (haveMoreTowers || strat != HighLevelStrat.TOWER_DIVING) && towersUnderAttack) {
      defendTowers();
      return;
    }
    
    switch (strat) {
    case HARASS:
      if (Clock.getRoundNum() >= 600) {
        buildForces();
        break;
      }
      setFleetMode(MovingBot.AttackMode.RALLYING);
      MapLocation[] towerLocs = rc.senseTowerLocations();
      if (towerLocs.length == 1) {
        Messaging.setRallyPoint(towerLocs[0]);
      } else {
        Messaging.setRallyPoint(new MapLocation(myHQ.x + ((enemyHQ.x - myHQ.x) / 3), myHQ.y + ((enemyHQ.y - myHQ.y) / 3)));
      }
      break;
    case BUILDING_FORCES:
      if (Clock.getRoundNum() >= 600 && Messaging.checkNumUnits(RobotType.TANK) > FLEET_COUNT_ATTACK_THRESHOLD) {

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
      if (haveDecentSurround(currentTargetTower)) {
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
          buildForces();
        } else if (!haveDecentSurround(currentTargetTower)) {
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
      if (!towersUnderAttack) {
        buildForces();
      }
      break;
    }
  }
  
  public void endOfTurn() throws GameActionException {
    Messaging.resetUnitCount(RobotType.HELIPAD);
    Messaging.resetUnitCount(RobotType.BARRACKS);
    Messaging.resetUnitCount(RobotType.TANKFACTORY);
    Messaging.resetUnitCount(RobotType.BEAVER);
    Messaging.resetUnitCount(RobotType.MINER);
    Messaging.resetUnitCount(RobotType.MINERFACTORY);
    Messaging.resetUnitCount(RobotType.TANK);
    Messaging.resetUnitCount(RobotType.DRONE);
    Messaging.resetUnitCount(RobotType.SUPPLYDEPOT);
    Messaging.resetTowersUnderAttack();
    super.endOfTurn();
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
      // System.out.println(rc.getLocation().distanceSquaredTo(toAttack));
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
    setRallyPoint(new MapLocation(myHQ.x + ((enemyHQ.x - myHQ.x) / 4), myHQ.y + ((enemyHQ.y - myHQ.y) / 4)));
  }
  
  public void buildForces() throws GameActionException {
    strat = HighLevelStrat.BUILDING_FORCES;
    setFleetMode(MovingBot.AttackMode.DEFENSIVE_SWARM);
    setRallyPoint(new MapLocation(myHQ.x + ((enemyHQ.x - myHQ.x) / 4), myHQ.y + ((enemyHQ.y - myHQ.y) / 4)));
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
  
  public static final double DEFENDERS_ADVANTAGE = 1.5;
  public static final int TOWER_DIVE_RADIUS = 49;
  public boolean haveDecentSurround(MapLocation loc) {
    double allyScore = Util.getDangerScore(rc.senseNearbyRobots(loc, TOWER_DIVE_RADIUS, myTeam));
    if (allyScore > 10.0) {
      double enemyScore = Util.getDangerScore(rc.senseNearbyRobots(loc, TOWER_DIVE_RADIUS, theirTeam));
      //rc.setIndicatorString(2, "Tower score, Ally: " + allyScore + ", enemy: " + enemyScore);

      return allyScore > DEFENDERS_ADVANTAGE*enemyScore;
    }
    //rc.setIndicatorString(2, "Tower score: no allies");
    return false;
  }
   
   
  public static final int NUM_BEAVERS = 2;
  public static final int NUM_MINER_FACTORIES = 1;
  public static final int NUM_BARRACKS = 1;
  public static final int NUM_TANK_FACTORIES = 10;
  public static final int NUM_HELIPADS = 1;
  public static final int NUM_SUPPLY_DEPOTS = 4;
  
  public void doMacro() throws GameActionException {
    /**if (Clock.getRoundNum() <= 1 && Messaging.checkTotalNumUnits(RobotType.DRONE) < 3) {
      Messaging.queueUnits(RobotType.DRONE, 3);
    }**/
    
    if (Messaging.peekQueueUnits(RobotType.TANK) < curNumTankFactories) {
      Messaging.queueUnits(RobotType.TANK, curNumTankFactories);
    }
    return;
  }
  

  public void maintainUnitComposition() throws GameActionException {
    if (maintainConstantUnits()) {
      maintainOreProduction();
      doBuildOrder();
    }
    return;
  }
  
  public void doBuildOrder() throws GameActionException {
    
   /** if (curNumHelipads < NUM_HELIPADS) {
      Messaging.queueUnits(RobotType.HELIPAD, NUM_HELIPADS - curNumHelipads);
    }**/
    
    if (curNumBarracks < NUM_BARRACKS /**&& Messaging.peekBuildingUnits(RobotType.HELIPAD) >= 1**/) {
      Messaging.queueUnits(RobotType.BARRACKS, NUM_BARRACKS - curNumBarracks);
    }

    if (curNumSupplyDepots < NUM_SUPPLY_DEPOTS) {
      Messaging.queueUnits(RobotType.SUPPLYDEPOT, NUM_SUPPLY_DEPOTS - curNumSupplyDepots);
    }

    if (curNumTankFactories < NUM_TANK_FACTORIES && Messaging.checkNumUnits(RobotType.BARRACKS) >= 1) {
      Messaging.queueUnits(RobotType.TANKFACTORY, NUM_TANK_FACTORIES - curNumTankFactories);
    }
    
  }
  
  // Returns true if constant requirements are met.
  public boolean maintainConstantUnits() throws GameActionException {
    if (curNumBeavers < NUM_BEAVERS && Clock.getRoundNum() > 21) {
      Messaging.setUnitToProduce(RobotType.BEAVER);
      Messaging.queueUnits(RobotType.BEAVER, NUM_BEAVERS - curNumBeavers);
      return false;
    } else if (curNumMinerFactories < NUM_MINER_FACTORIES) {
      Messaging.setUnitToProduce(RobotType.MINERFACTORY);
      Messaging.queueUnits(RobotType.MINERFACTORY, NUM_MINER_FACTORIES - curNumMinerFactories);
      return false;
    }
    
    if (Messaging.peekQueueUnits(RobotType.BEAVER) > 0 && !(Messaging.peekBuildingUnits(RobotType.BEAVER) > 0)) {
      Messaging.setUnitToProduce(RobotType.BEAVER);
      return false;
    }
    if (Messaging.peekQueueUnits(RobotType.MINERFACTORY) > 0 && !(Messaging.peekBuildingUnits(RobotType.MINERFACTORY) > 0)) {
      Messaging.setUnitToProduce(RobotType.MINERFACTORY);
      return false;
    }
    return true;
  }
  
  static final int MAX_NUM_MINERS = 20;
  // Get estimated ore production and compare it to the value required by our current unit output and/or desired future unit output.
  public boolean maintainOreProduction() throws GameActionException {
    int minersNeeded = MAX_NUM_MINERS - curNumMiners;
    if (minersNeeded > 0) {
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
  
  // HQ Only produces beavers.
  public void produceUnits() throws GameActionException {
    if (rc.isCoreReady()) {
      int unitToProduce = Messaging.getUnitToProduce();
      // System.out.println("Requested Unit: " + unitToProduce);
      if (unitToProduce == -1 || RobotType.BEAVER.ordinal() == unitToProduce) {
        if (rc.hasSpawnRequirements(RobotType.BEAVER) && Messaging.dequeueUnit(RobotType.BEAVER)) {
          Direction spawnDir = getOffensiveSpawnDirection(RobotType.BEAVER);
          if (spawnDir != null) {
            rc.spawn(spawnDir, RobotType.BEAVER);
          } else {
            // System.out.println("WRITE CODE HERE, NEED TO FIND PLACE TO BUILD");
          }
        }
      }
    }
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
