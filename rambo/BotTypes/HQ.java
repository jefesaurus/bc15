package rambo.BotTypes;

import rambo.Cache;
import rambo.Messaging;
import rambo.MovingBot;
import rambo.SupplyDistribution;
import rambo.Util;
import rambo.MovingBot.AttackMode;
import rambo.RobotPlayer.BaseBot;
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
  
  public static RobotType[] robotTypes = RobotType.values();
  public boolean isSafeTowerDive = true;
  public MapLocation currentTargetTower = new MapLocation(0,0);  
  public int curNumHelipads = 0;
  public int curNumBarracks = 0;
  public int curNumTankFactories = 0;
  public int curNumMiners = 0;
  public int curNumBeavers = 0;
  public int curNumMinerFactories = 0;
  public int curNumDrones = 0;
  public int curNumTanks = 0;
  public int curNumSupplyDepots = 0;
  public int curNumTechInstitutes = 0;
  public int curNumTrainingFields = 0;
  public int curNumCommanders = 0;
  public int curNumSoldiers = 0;
  public int lastOreDifferential = 0;
  public double curOreDifferentialShift = 0;
  public static int teamSupplyCost = 0;
  public static int END_GAME_ROUND_NUM = 1500; 
  public static int distanceBetweenHQ;
  public static int MAX_NUM_MINERS;
  public static MapLocation splitPush1 = null;
  public static MapLocation splitPush2 = null;
  public static boolean lessThanOneTowerLeft = false;

  public HQ(RobotController rc) {
    super(rc);
  }
  
  public void setup() throws GameActionException {
    SupplyDistribution.init(this);
    SupplyDistribution.setBatteryMode();
    END_GAME_ROUND_NUM = rc.getRoundLimit() - 500;
    distanceBetweenHQ = myHQ.distanceSquaredTo(enemyHQ);
    MAX_NUM_MINERS = (int) Math.min((30 * distanceBetweenHQ / 5500), 50);
    System.out.println(distanceBetweenHQ);
    System.out.println(MAX_NUM_MINERS);
    buildForces();
  }
  
  public enum BaseState {
    UNDER_ATTACK,
    HARASSED,
    FINE
  }
  
  public enum HighLevelStrat {
    HARASS,
    BUILDING_FORCES,
    APPROACHING_TOWER,
    TOWER_DIVING,
    TOWER_DEFENDING,
    COUNTER_ATTACK,
    SPLIT_PUSH,
    TOWER_DIVING_SPLIT
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
    curNumTechInstitutes = Messaging.checkTotalNumUnits(RobotType.TECHNOLOGYINSTITUTE);
    curNumTrainingFields = Messaging.checkTotalNumUnits(RobotType.TRAININGFIELD);
    curNumCommanders = Messaging.checkTotalNumUnits(RobotType.COMMANDER);
    curNumSoldiers = Messaging.checkTotalNumUnits(RobotType.SOLDIER);

    SupplyDistribution.manageSupply();
    teamSupplyCost = getSupplyCost();
    
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
    

    
    maintainUnitComposition();
    produceUnits();
    doMacro();
    
    rc.setIndicatorString(0, strat.name() + ", targets: " + splitPush1 + ", " + splitPush2 + ", " + Clock.getRoundNum());
    
    // If we are currently winning in towers, and we are under attack, pull back and defend.
    boolean haveMoreTowers = weHaveMoreTowers();
    boolean towersUnderAttack = Messaging.getClosestTowerUnderAttack() != null;
    MapLocation[] enemyTowers = Cache.getEnemyTowerLocationsDirect();
    
    switch (strat) {
    case BUILDING_FORCES:
      if (Clock.getRoundNum() >= 600 && 
      (Messaging.checkNumUnits(RobotType.TANK) + Messaging.checkNumUnits(RobotType.SOLDIER)) > FLEET_COUNT_ATTACK_THRESHOLD) {
        if (distanceBetweenHQ <= 6000 || Cache.getEnemyTowerLocations().length < 2) {
          System.out.println("Not split pushing");
          setCurrentTowerTarget();
          approachTower(currentTargetTower);
        } else {
          if (splitPush1 == null || splitPush2 == null) {
            setSplitPushTargets();
            System.out.println("SHOULD ONLY BE CALLED ONCE");
            splitPush(true, true);
          } else {
            System.out.println("SHOULD NEVER BE CALLED");

            splitPush(true, true);
          }
        }
      }
      
      break;
    case APPROACHING_TOWER:
      // Set rally point to just in front of nearest tower.
      // Wait until ally centroid is epsilon close, then switch to tower diving
      if (currentTargetTowerIsDead(enemyTowers)) {
        setCurrentTowerTarget();
        buildForces();
      } else if (doDesperateDive() || haveDecentSurround(currentTargetTower)) {
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
      if (!doDesperateDive() && !haveDecentSurround(currentTargetTower)) {
        buildForces();
        break;
      } else if (currentTargetTowerIsDead(enemyTowers)) {
        setCurrentTowerTarget();
        if (doDesperateDive()) {
          approachTower(currentTargetTower);
        } else {
          buildForces();
        }
      }
      break;
    case TOWER_DIVING_SPLIT:
      // If we're winning in tower count, switch to TOWER_DEFENDING
      
      if (doDesperateDive() || haveDecentSurround(splitPush1)) {
        // If there are no more towers, then we are engaging the HQ
        if (enemyTowers.length == 0) {
          diveTowerUnsafeSplit(splitPush1);
        } else {
          if (isSafeTowerDive) {
            diveTowerSafeSplit(splitPush1);
          } else {
            diveTowerUnsafeSplit(splitPush1);
          }
        }
      }
      if (doDesperateDive() || haveDecentSurround(splitPush2)) {
        // If there are no more towers, then we are engaging the HQ
        if (enemyTowers.length == 0) {
          diveTowerUnsafeSplit(splitPush2);
        } else {
          if (isSafeTowerDive) {
            diveTowerSafeSplit(splitPush2);
          } else {
            diveTowerUnsafeSplit(splitPush2);
          }
        }
      }
      
      if (enemyTowers.length > 0) {
        // Check if our current target is dead yet:
        boolean targetIsDead = currentTargetTowerIsDead(splitPush1);
        boolean target2IsDead = currentTargetTowerIsDead(splitPush2);
        if (targetIsDead || target2IsDead) {
          // defendTowers();
          updateSplitPushTargets(targetIsDead, target2IsDead);
          if (doDesperateDive()) {
            splitPush(targetIsDead, target2IsDead);
          } else {
            splitPush(targetIsDead, target2IsDead);
          }
        }
      }
      break;
    case TOWER_DEFENDING:
      // Switch to tower diving if they have equal to or more towers
      if (!towersUnderAttack) {
        buildForces();
      }
      break;
    case COUNTER_ATTACK:
      setCurrentTowerTarget();
      setRallyPoint(currentTargetTower);
      if (haveDecentSurround(currentTargetTower)) {
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
    case SPLIT_PUSH:
      if (doDesperateDive() || haveDecentSurround(splitPush1)) {
        // If there are no more towers, then we are engaging the HQ
        if (enemyTowers.length == 0) {
          diveTowerUnsafeSplit(splitPush1);
        } else {
          if (isSafeTowerDive) {
            diveTowerSafeSplit(splitPush1);
          } else {
            diveTowerUnsafeSplit(splitPush1);
          }
        }
      }
      if (doDesperateDive() || haveDecentSurround(splitPush2)) {
        // If there are no more towers, then we are engaging the HQ
        if (enemyTowers.length == 0) {
          diveTowerUnsafeSplit(splitPush2);
        } else {
          if (isSafeTowerDive) {
            diveTowerSafeSplit(splitPush2);
          } else {
            diveTowerUnsafeSplit(splitPush2);
          }
        }
      }
      if (enemyTowers.length > 0) {
        // Check if our current target is dead yet:
        boolean targetIsDead = currentTargetTowerIsDead(splitPush1);
        boolean target2IsDead = currentTargetTowerIsDead(splitPush2);
        if (targetIsDead || target2IsDead) {
          // defendTowers();
          updateSplitPushTargets(targetIsDead, target2IsDead);
          if (doDesperateDive()) {
            splitPush(targetIsDead, target2IsDead);
          } else {
            splitPush(targetIsDead, target2IsDead);
          }
        }
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
    Messaging.resetUnitCount(RobotType.TECHNOLOGYINSTITUTE);
    Messaging.resetUnitCount(RobotType.TRAININGFIELD);
    Messaging.resetUnitCount(RobotType.COMMANDER);
    Messaging.resetUnitCount(RobotType.SOLDIER);
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
  
  public void splitPush(boolean target1, boolean target2) throws GameActionException {
    System.out.println("split push called with: "+ target1 + " " + target2);
    strat = HighLevelStrat.SPLIT_PUSH;
    if (target1) {
      setFleetMode(MovingBot.AttackMode.SPLIT_PUSH);
    }
    if (target2) {
      Messaging.setFleetMode2(MovingBot.AttackMode.SPLIT_PUSH);
    }
  }
  
  public void counterAttack(MapLocation towerLoc) throws GameActionException {
    strat = HighLevelStrat.COUNTER_ATTACK;
    currentTargetTower = towerLoc;
    setRallyPoint(currentTargetTower);
    setFleetMode(MovingBot.AttackMode.COUNTER_ATTACK);
  }
  
  public void approachTower(MapLocation towerLoc) throws GameActionException {
    strat = HighLevelStrat.APPROACHING_TOWER;
    currentTargetTower = towerLoc;
    setRallyPoint(currentTargetTower);
    Messaging.setRallyPoint2(currentTargetTower);
    setFleetMode(MovingBot.AttackMode.OFFENSIVE_SWARM);
    Messaging.setFleetMode2(MovingBot.AttackMode.OFFENSIVE_SWARM);

  }
  
  public void diveTowerSafe(MapLocation towerLoc) throws GameActionException {
    strat = HighLevelStrat.TOWER_DIVING;
    setRallyPoint(towerLoc);
    Messaging.setRallyPoint2(towerLoc);
    setFleetMode(MovingBot.AttackMode.SAFE_TOWER_DIVE);
    Messaging.setFleetMode2(MovingBot.AttackMode.SAFE_TOWER_DIVE);

  }
  
  public void diveTowerUnsafe(MapLocation towerLoc) throws GameActionException {
    strat = HighLevelStrat.TOWER_DIVING;
    setRallyPoint(towerLoc);
    Messaging.setRallyPoint2(towerLoc);
    setFleetMode(MovingBot.AttackMode.UNSAFE_TOWER_DIVE);
    Messaging.setFleetMode2(MovingBot.AttackMode.UNSAFE_TOWER_DIVE);
  }
  
  public void diveTowerSafeSplit(MapLocation towerLoc) throws GameActionException {
    strat = HighLevelStrat.TOWER_DIVING_SPLIT;
    if (towerLoc.equals(splitPush1)) {
      setFleetMode(MovingBot.AttackMode.SAFE_TOWER_DIVE_SPLIT);
    } else {
      Messaging.setFleetMode2(MovingBot.AttackMode.SAFE_TOWER_DIVE_SPLIT);     
    }
  }
  
  public void diveTowerUnsafeSplit(MapLocation towerLoc) throws GameActionException {
    strat = HighLevelStrat.TOWER_DIVING_SPLIT;
    if (towerLoc.equals(splitPush1)) {
      setFleetMode(MovingBot.AttackMode.UNSAFE_TOWER_DIVE_SPLIT);
    } else {
      Messaging.setFleetMode2(MovingBot.AttackMode.UNSAFE_TOWER_DIVE_SPLIT);     
    }
  }
  
  public void defendTowers() throws GameActionException {
    strat = HighLevelStrat.TOWER_DEFENDING;
    setFleetMode(MovingBot.AttackMode.DEFEND_TOWERS);
    Messaging.setFleetMode2(MovingBot.AttackMode.DEFEND_TOWERS);
    MapLocation loc = Messaging.getClosestTowerUnderAttack();
    setRallyPoint(loc);
    Messaging.setRallyPoint2(loc);
  }
  
  public MapLocation getTowerToDefend() throws GameActionException {
    MapLocation loc = Messaging.getClosestTowerUnderAttack();
    if (loc == null) {
      MapLocation[] towerLocs = rc.senseTowerLocations();
      MapLocation closest = this.myHQ;
      for (int i=towerLocs.length; i-- >0;) {
        if (towerLocs[i].distanceSquaredTo(enemyHQ) < closest.distanceSquaredTo(enemyHQ)) {
          closest = towerLocs[i];
        }
      }
      return closest;
    } else {
      return loc;
    }
  }
  
  public void buildForces() throws GameActionException {
    strat = HighLevelStrat.BUILDING_FORCES; 
    setFleetMode(MovingBot.AttackMode.DEFENSIVE_SWARM);
    Messaging.setFleetMode2(MovingBot.AttackMode.DEFENSIVE_SWARM);
    MapLocation[] towerLocs = rc.senseTowerLocations();
    if (towerLocs.length == 1) {
      setRallyPoint(towerLocs[0]);
      Messaging.setRallyPoint2(towerLocs[0]);

    } else {
      setRallyPoint(getTowerToDefend());
      Messaging.setRallyPoint2(getTowerToDefend());
    }
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
  
  public boolean doDesperateDive() throws GameActionException {
    return(!weHaveMoreTowers() && Clock.getRoundNum() > END_GAME_ROUND_NUM);
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
  
  public static MapLocation getTowerCentroid(RobotController rc) {
    MapLocation[] towerLocs = rc.senseTowerLocations();
    int x = rc.getLocation().x;
    int y = rc.getLocation().y;
    for (int i=towerLocs.length; i-->0;) {
      x += towerLocs[i].x;
      y += towerLocs[i].y;
    }
    return new MapLocation(x / (towerLocs.length+1), y / (towerLocs.length + 1));
  }
  

  public static int getOreDifferential() throws GameActionException {
    int netDifferential = 0;
    for (int i=robotTypes.length; i-->0;) {
      RobotType type = robotTypes[i];
      netDifferential += (Messaging.checkKillCount(type) - (Messaging.readUnitsBuilt(type) - Messaging.checkNumUnits(type))) * type.oreCost;
    }
    return netDifferential;
  }
  
  public static int getSupplyCost() throws GameActionException {
    int supplyCost = 0;
    for (int i=robotTypes.length; i-->0;) {
      RobotType type = robotTypes[i];
      supplyCost += (Messaging.checkNumUnits(type)) * type.supplyUpkeep;
    }
    return supplyCost;
  }
  
  /*
   * This sets two class scope variables: the target tower and whether or not to approach the target "safely"
   * 
   * It determines the target to be the tower with the Maximum minimum distance to another tower or HQ. That is, the one that is furthest away from the others
   * The safety metric is basically just to check whether this tower is directly adjacent to another tower, in which case it must ignore danger from untargeted towers.
   */
  public void setSplitPushTargets() throws GameActionException {
    MapLocation[] towerLocs = Cache.getEnemyTowerLocationsDirect();
    int maxDist = -1;
    MapLocation towerLoc1 = null;
    MapLocation towerLoc2 = null;
    for (int i=towerLocs.length; i-->0;) {
      for (int j=i-1; j>= 0; j--) {
        int trialDist = towerLocs[i].distanceSquaredTo(towerLocs[j]);
        if (trialDist > maxDist) {
          maxDist = trialDist;
          towerLoc1 = towerLocs[i];
          towerLoc2 = towerLocs[j];
        }
      }
    }
    splitPush1 = towerLoc1;
    splitPush2 = towerLoc2;
    
    Messaging.setRallyPoint(towerLoc1);
    Messaging.setRallyPoint2(towerLoc2);
  }
  
  public void updateSplitPushTargets(boolean t1, boolean t2) throws GameActionException {
    MapLocation aliveTower = splitPush1;
    if (t1 && t2) {
      System.out.println("setting both");
      setSplitPushTargets();
      return;
    } else if (t1) {
      System.out.println("splitpush 1 @ " + splitPush1 + " is dead");
      aliveTower = splitPush2;
    }
    MapLocation[] towerLocs = Cache.getEnemyTowerLocationsDirect();
    int maxDist = -1;
    MapLocation towerLoc = null;
    for (int i=towerLocs.length; i-->0;) {
        int trialDist = towerLocs[i].distanceSquaredTo(aliveTower);
        if (trialDist > maxDist) {
          maxDist = trialDist;
          towerLoc = towerLocs[i];
        }
    }
    if (t2) {
      Messaging.setRallyPoint2(towerLoc);
      splitPush2 = towerLoc;
    } else {
      Messaging.setRallyPoint(towerLoc);
      splitPush1 = towerLoc;
    }
  }
  
  public void setCurrentTowerTarget() throws GameActionException {
    enemyTowers = Cache.getEnemyTowerLocationsDirect();
    if (enemyTowers.length <= 0) {
      currentTargetTower = enemyHQ;
      return;
    }
    
    double maxiMinDist = 0;
    int chosenIndex = 0;
    
    // Keep already computed distances around.
    int[][] distMat = new int[enemyTowers.length][enemyTowers.length];
    
    int tempDist = 0;
    int minDist;
    int numAtDist;
    int tempNum;
    for (int i = enemyTowers.length; i-- > 0;) {
      minDist = enemyTowers[i].distanceSquaredTo(enemyHQ);
      numAtDist = 0;
      for (int j = enemyTowers.length; j-- > i + 1;) {
        if (distMat[i][j] < minDist) {
          minDist = distMat[i][j];
          numAtDist = 0;
        } else if (distMat[i][j] == minDist) {
           numAtDist++;
        }
      }
      tempNum = 0;
      for (int j = i; j-- > 0;) {
        tempDist = enemyTowers[i].distanceSquaredTo(enemyTowers[j]);
        distMat[j][i] = tempDist;
        if (tempDist < minDist) {
          minDist = tempDist;
          tempNum = 0;
        } else if (tempDist == minDist) {
          tempNum++;
        }
      }

      if ((minDist - maxiMinDist) < 2 && (minDist - maxiMinDist) > -2) {
        if (enemyTowers[i].distanceSquaredTo(myHQ) < enemyTowers[chosenIndex].distanceSquaredTo(myHQ)) {
          maxiMinDist = minDist;
          chosenIndex = i;
        }
      } else if ((minDist > maxiMinDist) || (minDist == maxiMinDist && tempNum < numAtDist)) {
        maxiMinDist = minDist;
        chosenIndex = i;
      }
    }    
    
    isSafeTowerDive = (maxiMinDist > 9);
    currentTargetTower = enemyTowers[chosenIndex];
  }
  
  public boolean currentTargetTowerIsDead(MapLocation[] enemyTowers) {
    if (currentTargetTower.equals(enemyHQ)) {
      return false;
    }
    for (int i = enemyTowers.length; i-- > 0;) {
      if (enemyTowers[i].equals(currentTargetTower)) {
        return false;
      }
    }
    return true;
  }
  
  public boolean currentTargetTowerIsDead(MapLocation loc) throws GameActionException {
    MapLocation[] enemyTowers = Cache.getEnemyTowerLocationsDirect();
    for (int i = enemyTowers.length; i-- > 0;) {
      if (enemyTowers[i].equals(loc)) {
        return false;
      }
    }
    return true;
  }
  
  public static final double DEFENDERS_ADVANTAGE = 1.5;
  public static final int TOWER_DIVE_RADIUS = 63;
  public boolean haveDecentSurround(MapLocation loc) {
    //double allyScore = Util.getDangerScore(rc.senseNearbyRobots(loc, TOWER_DIVE_RADIUS, myTeam));
    RobotInfo[] r = rc.senseNearbyRobots(loc, TOWER_DIVE_RADIUS, myTeam);
    double allyScore = 0;
    for (int i=r.length; i-->0;) {
      allyScore += Util.getDangerScore(r[i]);
    }
    if (allyScore > 24.0) {
      double enemyScore = Util.getDangerScore(rc.senseNearbyRobots(loc, TOWER_DIVE_RADIUS, theirTeam));
      //rc.setIndicatorString(2, "Tower score, Ally: " + allyScore + ", enemy: " + enemyScore);
      return allyScore > DEFENDERS_ADVANTAGE*enemyScore;
    }
    //rc.setIndicatorString(2, "Tower score: no allies");
    return false;
  }
   
   
  public static final int NUM_TECH_INSTITUTES = 1;
  public static final int NUM_TRAINING_FIELDS = 1;
  public static final int NUM_BEAVERS = 2;
  public static final int NUM_MINER_FACTORIES = 1;
  public static int NUM_BARRACKS = 2;
  public static final int NUM_TANK_FACTORIES = 5;
  public static final int NUM_HELIPADS = 1;
  public static int NUM_SUPPLY_DEPOTS = Math.max(4, teamSupplyCost / 90);
  
  public void doMacro() throws GameActionException {
    /**if (Clock.getRoundNum() <= 1 && Messaging.checkTotalNumUnits(RobotType.DRONE) < 3) {
      Messaging.queueUnits(RobotType.DRONE, 3);
    }**/
    
    
    if (Messaging.peekQueueUnits(RobotType.SOLDIER) < curNumBarracks) {
      Messaging.queueUnits(RobotType.SOLDIER, 2*curNumBarracks);
    }
    
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
    
    /**if (curNumHelipads < NUM_HELIPADS) {
      Messaging.queueUnits(RobotType.HELIPAD, NUM_HELIPADS - curNumHelipads);
    }**/
    /*
    if (curNumTechInstitutes < NUM_TECH_INSTITUTES) {
      Messaging.queueUnits(RobotType.TECHNOLOGYINSTITUTE, 1);
    }
    
    if (curNumTrainingFields < NUM_TRAINING_FIELDS && Messaging.peekBuildingUnits(RobotType.TECHNOLOGYINSTITUTE) >= 1) {
      Messaging.queueUnits(RobotType.TRAININGFIELD, 1);
      Messaging.queueUnits(RobotType.COMMANDER, 1);
    }
    */
    if (distanceBetweenHQ >= 6500) {
      NUM_SUPPLY_DEPOTS = Math.min(Math.max(4, teamSupplyCost / (65)), Clock.getRoundNum() / 75);
    } else {
      NUM_SUPPLY_DEPOTS = Math.min(Math.max(4, teamSupplyCost / (65)), Clock.getRoundNum() / 100);
    }
    
    if (curNumBarracks < NUM_BARRACKS /**&& Messaging.peekBuildingUnits(RobotType.SUPPLYDEPOT) >= 1*/) {
      Messaging.queueUnits(RobotType.BARRACKS, NUM_BARRACKS - curNumBarracks);
    }

    if (Clock.getRoundNum() >= 800 && Messaging.peekQueueUnits(RobotType.BARRACKS) >= 1 && Messaging.areWeFightingLaunchers()) {
      Messaging.queueUnits(RobotType.BARRACKS, NUM_BARRACKS - curNumBarracks);
      Messaging.setUnitToProduce(RobotType.BARRACKS);
    }
    
    if (curNumSupplyDepots < NUM_SUPPLY_DEPOTS /**&& Messaging.checkNumUnits(RobotType.TRAININGFIELD) >= 1*/) {
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
