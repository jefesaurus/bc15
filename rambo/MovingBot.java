package rambo;

import java.util.Arrays;

import rambo.Nav.Engage;
import rambo.RobotPlayer.BaseBot;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class MovingBot extends BaseBot {
  public enum AttackMode {
      OFFENSIVE_SWARM,
      DEFENSIVE_SWARM,
      COUNTER_ATTACK,
      SAFE_TOWER_DIVE,
      UNSAFE_TOWER_DIVE,
      DEFEND_TOWERS,
      RALLYING,
      HUNT_FOR_MINERS,
      SPLIT_PUSH,
      SAFE_TOWER_DIVE_SPLIT,
      UNSAFE_TOWER_DIVE_SPLIT,
      HELP_DIVE
  }
        
  protected RobotInfo[] currentEnemies;
  protected int currentEnemiesRound = -1;
  
  protected int[] cachedNumAttackingEnemyDirs;
  protected double[] cachedEnemyDangerValsDirs;
  protected int[] cachedNumAttackingTowerDirs;
  protected boolean[] cachedAttackingHQDirs;
  protected double[] cachedDangerVals;
  
  public MovingBot(RobotController rc) {
    super(rc);
  }
  
  public void init() throws GameActionException {
    Nav.init(this);

    // Hacky crap to get this huge chunk of bytecode out of the way.
    int[] i = Util.ATTACK_NOTES[0][0][0];
    super.init();
  }
      
  public void beginningOfTurn() throws GameActionException {
    // Moving enemies only
    cachedNumAttackingEnemyDirs = null;
    cachedEnemyDangerValsDirs = null;
    
    // Number of enemy towers onl
    cachedNumAttackingTowerDirs = null;
    
    // Enemy HQ only
    cachedAttackingHQDirs = null;
    
    // All of the above
    cachedDangerVals = null;

    super.beginningOfTurn();
  }
  
  public static final int ALLY_INLCUDE_RADIUS_SQ = 29;
  public static final int ENEMY_INCLUDE_RADIUS_SQ = 49;
  
  // Returns in first slot whether fight is winning
  // In second slot, whether we're "definitely going to die"
  public boolean[] isFightWinning() {
    RobotInfo[] nearbyEnemies = Cache.getEngagementEnemies();
    if (nearbyEnemies.length == 0) {
      return new boolean[] {true};
    }
    
    RobotInfo closestEngageable = null;
    double closestDist = Double.MAX_VALUE;
    double tempDist = 0;
    for (RobotInfo bot : nearbyEnemies) {
      switch (bot.type) {
      case HQ:
      case TOWER:
        // TODO figure out how to deal with towers that turn up...
        return new boolean[] {false, false};
      case BEAVER:
      case DRONE:
      case SOLDIER:
      case TANK:
      case COMMANDER:
      case MINER:
      case BASHER:
      case MISSILE:
        tempDist = curLoc.distanceSquaredTo(bot.location);
        if (tempDist < closestDist) {
          closestDist = tempDist;
          closestEngageable = bot;
        }
        break;
      default:
        break;
      }
    }
    
    if (closestEngageable == null) {
      return new boolean[] {true};
    }
    
    RobotInfo[] allies = rc.senseNearbyRobots(closestEngageable.location, ALLY_INLCUDE_RADIUS_SQ, myTeam);
    double allyScore = Util.getDangerScore(allies);
    double enemyScore = Util.getDangerScore(rc.senseNearbyRobots(closestEngageable.location, ENEMY_INCLUDE_RADIUS_SQ, theirTeam));
    // rc.setIndicatorString(2, "Ally score: " + allyScore + ", Enemy score: " + enemyScore);
    if (allyScore > enemyScore) {
      return new boolean[] {true};
    } else {
      // Check if we are definitely going to die.
      double myHealth = rc.getHealth();
      int myID = rc.getID();
      boolean isAlone = true;
      for (int i = allies.length; i-- > 0;) {
        if (allies[i].ID != myID) {
          isAlone = false;
          if(allies[i].health < myHealth) {
            return new boolean[] {false, false};
          }
        }
      }
      // We didn't find any bots with lower health, so we are the lowest.
      // If we are alone, we should retreat anyways...
      return new boolean[] {false, !isAlone};
    }
  }
  
  /*
   returns {is winning, ...}
   if is_winning: {1, battlefront.x, battlefront.y}
   else: {0, lowesthealth and not alone}
  */
  public int[] getBattleMetrics(RobotInfo[] nearbyEnemies) throws GameActionException {
    RobotInfo closestEngageable = null;
    double closestDist = Double.MAX_VALUE;
    double tempDist = 0;
    //int enemyCentroidX = 0;
    //int enemyCentroidY = 0;
    //int numEnemies = 0;
    double enemyAttackPower = 0;
    for (int i = nearbyEnemies.length; i-- > 0;) {
      RobotInfo enemy = nearbyEnemies[i];
      if (enemy.type == RobotType.LAUNCHER) {
        Messaging.weAreFightingLaunchers();
      }
      if (enemy.type == RobotType.COMMANDER) {
        Messaging.weAreFightingCommander();
      }
      switch (nearbyEnemies[i].type) {
      case HQ:
      case TOWER:
        // TODO figure out how to deal with towers that turn up...
        return new int[] {0, 0};
      case BEAVER:
      case DRONE:
      case SOLDIER:
      case TANK:
      case COMMANDER:
      case MINER:
      case BASHER:
      case MISSILE:
      case LAUNCHER:
        if (enemy.weaponDelay < 1 && enemy.location.distanceSquaredTo(rc.getLocation()) <= enemy.type.attackRadiusSquared) {
          enemyAttackPower += enemy.type.attackPower;
        }
        tempDist = curLoc.distanceSquaredTo(nearbyEnemies[i].location);
        if (tempDist < closestDist) {
          closestDist = tempDist;
          closestEngageable = nearbyEnemies[i];
        }
        //enemyCentroidX += nearbyEnemies[i].location.x;
        //enemyCentroidY += nearbyEnemies[i].location.y;
        //numEnemies ++;
        break;
      default:
        break;
      }
    }
    
    if (closestEngageable == null) {
      return new int[] {1, -1, -1};
    }
   
    RobotInfo[] allies = rc.senseNearbyRobots(closestEngageable.location, ALLY_INLCUDE_RADIUS_SQ, myTeam);
    double allyScore = Util.getDangerScore(allies);
    double enemyScore = Util.getDangerScore(rc.senseNearbyRobots(closestEngageable.location, ENEMY_INCLUDE_RADIUS_SQ, theirTeam));
    // rc.setIndicatorString(0, "Ally score: " + allyScore + ", Enemy score: " + enemyScore);
    if (allyScore > enemyScore) {
      return new int[] {1, closestEngageable.location.x, closestEngageable.location.y};
    } else {
      // Check if we are definitely going to die.
      double myHealth = rc.getHealth();
      int myID = rc.getID();
      boolean isAlone = true;
      for (int i = allies.length; i-- > 0;) {
        if (allies[i].ID != myID) {
          isAlone = false;
          if(allies[i].health < myHealth) {
            return new int[] {0, 0};
          }
        }
      }
      // We didn't find any bots with lower health, so we are the lowest.
      // If we are alone, we should retreat anyways...
      return new int[] {0, (isAlone || enemyAttackPower < myHealth) ? 0 : 1};
    }
  }
  
  // return null if can't retreat or dead anyway
  public Direction getBestRetreatDir(RobotInfo[] enemies) throws GameActionException {
    Direction[] dirs = Direction.values();
    double minDmg = 999999999;
    Direction bestDir = null;
    for (int i=8; i-->0;){
      double trialDmg = 0;
      if (rc.canMove(dirs[i])) {
        MapLocation trialLoc = rc.getLocation().add(dirs[i]);
        for (int j=enemies.length; j-->0;) {
          RobotInfo info = enemies[j];
          if (info.location.distanceSquaredTo(trialLoc) <= info.type.attackRadiusSquared) {
            trialDmg += info.type.attackPower;
          }
        }
        if (trialDmg < minDmg) {
          minDmg = trialDmg;
          bestDir = dirs[i];
        }
      }
    }
    if (bestDir == null || minDmg >= rc.getHealth()) {
      return null;
    } else {
      return bestDir;
    }
  }
  
  public boolean canMoveSafely(Direction dir, boolean moveInForTower) throws GameActionException {
    if (rc.canMove(dir)) {
      //int bc = Clock.getBytecodeNum();
      int[] numAttackingEnemyDirs = calculateNumAttackingEnemyDirs();
      //System.out.println(Clock.getBytecodeNum() - bc);
      //bc = Clock.getBytecodeNum();
      if (numAttackingEnemyDirs[dir.ordinal()] == 0) {
        return rc.canMove(dir);
      }
    }
    return false;
  }
  
  // Might double count towers and HQ
  protected int[] calculateNumAttackingEnemyDirs() throws GameActionException {
    if (cachedNumAttackingEnemyDirs == null) {
      cachedNumAttackingEnemyDirs = new int[9];
      RobotInfo[] visibleEnemies = Cache.getEngagementEnemies();
      for (int i = visibleEnemies.length; i-- > 0;) {
        MapLocation enemyLoc = visibleEnemies[i].location;
        int[] attackedDirs = Util.ATTACK_NOTES[Util.RANGE_TYPE_MAP[visibleEnemies[i].type.ordinal()]][5 + enemyLoc.x - curLoc.x][5 + enemyLoc.y - curLoc.y];
        for (int j = attackedDirs.length; j-- > 0;) {
          cachedNumAttackingEnemyDirs[attackedDirs[j]]++;
        }
      }
    }
    return cachedNumAttackingEnemyDirs;
  }
  
  protected double[] calculateEnemyDangerValsDirs() throws GameActionException {
    if (cachedEnemyDangerValsDirs == null) {
      cachedEnemyDangerValsDirs = new double[9];
      RobotInfo[] visibleEnemies = Cache.getEngagementEnemies();
      int enemyType;
      double dangerVal;
      for (int i = visibleEnemies.length; i-- > 0;) {
        enemyType = visibleEnemies[i].type.ordinal();
        
        // Ordinal values of HQ and Tower
        if (enemyType == 1 || enemyType == 0) {
          continue;
        }
        MapLocation enemyLoc = visibleEnemies[i].location;
        dangerVal = (visibleEnemies[i].supplyLevel > 0) ? Util.DANGER_VALUE_MAP[enemyType] : Util.DANGER_VALUE_MAP[enemyType]/2;
        int[] attackedDirs = Util.ATTACK_NOTES[Util.RANGE_TYPE_MAP[enemyType]][5 + enemyLoc.x - curLoc.x][5 + enemyLoc.y - curLoc.y];
        for (int j = attackedDirs.length; j-- > 0;) {
          cachedEnemyDangerValsDirs[attackedDirs[j]] += dangerVal;
        }
      }
    }
    return cachedEnemyDangerValsDirs;
  }
  
  protected double[] getAllDangerVals() throws GameActionException {
    if (cachedDangerVals == null) {
      cachedDangerVals = calculateEnemyDangerValsDirs();
      
      // Do Towers
      double dangerVal = Util.DANGER_VALUE_MAP[RobotType.TOWER.ordinal()];
      int[] attackingTowerDirs = calculateNumAttackingTowerDirs(null);
      for (int i = attackingTowerDirs.length; i-- > 0;) {
        cachedDangerVals[i] += attackingTowerDirs[i]*dangerVal;
      }

      // Do HQ
      dangerVal = Util.DANGER_VALUE_MAP[RobotType.HQ.ordinal()];
      boolean[] attackingHQDirs = calculateAttackingHQDirs();
      for (int i = attackingHQDirs.length; i-- > 0;) {
        if (attackingHQDirs[i]) {
          cachedDangerVals[i] += dangerVal;
        }
      }
    }
    
    return cachedDangerVals;
  }
  
  protected int[] calculateNumAttackingTowerDirs(MapLocation ignoreTower) throws GameActionException {
    if (cachedNumAttackingTowerDirs == null) {
      cachedNumAttackingTowerDirs = new int[9];
      MapLocation[] enemyTowers = Cache.getEnemyTowerLocationsDirect();

      int xdiff;
      int ydiff;
      for (int i = enemyTowers.length; i-- > 0;) {
        xdiff = enemyTowers[i].x - curLoc.x;
        ydiff = enemyTowers[i].y - curLoc.y;
        if (xdiff <= 5 && xdiff >= -5 && ydiff <= 5 && ydiff >= -5) {
          if (ignoreTower != null && enemyTowers[i].equals(ignoreTower)) {
            continue;
          }
          int[] attackedDirs = Util.ATTACK_NOTES[Util.RANGE_TYPE_MAP[RobotType.TOWER.ordinal()]][5 + xdiff][5 + ydiff];
          for (int j = attackedDirs.length; j-- > 0;) {
            cachedNumAttackingTowerDirs[attackedDirs[j]]++;
          }
        }
      }
    }
    return cachedNumAttackingTowerDirs;
  }
  
  protected boolean[] calculateAttackingHQDirs() throws GameActionException {
    if (cachedAttackingHQDirs == null) {
      int curDist = curLoc.distanceSquaredTo(enemyHQ);
      int numEnemyTowers = Cache.getEnemyTowerLocationsDirect().length;
      cachedAttackingHQDirs = new boolean[9];
      if (numEnemyTowers >= 5) {
        
        if (curDist < 81) {
          int xdiff = curLoc.x - this.enemyHQ.x;
          int ydiff = curLoc.y - this.enemyHQ.y;
          int dx, dy;
          for (int i = 9; i-- > 0;) {
            dx = xdiff + Util.DIR_DX[i];
            dy = ydiff + Util.DIR_DY[i];
            dx = (dx > 0) ? dx : -dx;
            dy = (dy > 0) ? dy : -dy;
            cachedAttackingHQDirs[i] = (dx <= 6 && dy <= 6 && dx + dy <= 10);
          }
        }
      } else if (numEnemyTowers >= 2) {
        if (Math.abs(curLoc.x - enemyHQ.x) <= 6 || Math.abs(curLoc.y - enemyHQ.y) <= 6 ) {
          if (curDist <= 35) {
            cachedAttackingHQDirs[8] = true;
          }
          for (int i = 8; i-- > 0;) {
            cachedAttackingHQDirs[i] = curLoc.add(Util.REGULAR_DIRECTIONS[i]).distanceSquaredTo(enemyHQ) <= 35;
          }
        }
      } else {
        if (curDist < 36) {
          if (curDist <= 24) {
            cachedAttackingHQDirs[8] = true;
          }
          for (int i = 8; i-- > 0;) {
            cachedAttackingHQDirs[i] = curLoc.add(Util.REGULAR_DIRECTIONS[i]).distanceSquaredTo(enemyHQ) <= 24;
          }
        }
      }
    }
    //rc.setIndicatorString(1, Arrays.toString(cachedAttackingHQDirs) + ", " + Clock.getRoundNum());
    return cachedAttackingHQDirs;
  }
  
  public void doSneakyMove(MapLocation rallyPoint) throws GameActionException {
    Nav.goTo(rallyPoint, Engage.NONE);
  }
  
  public void doOffensiveMicro(RobotInfo[] engageableEnemies, MapLocation rallyPoint) throws GameActionException {
    //rc.setIndicatorString(0, "Offensive micro " + ", " + Clock.getRoundNum());

    if (engageableEnemies.length > 0) {
      // returns {is winning, is lowest health and not alone}
      int[] metrics = getBattleMetrics(engageableEnemies);
      if (metrics[0] > 0) {
        //rc.setIndicatorString(1, "winning..." + Clock.getRoundNum());

        if (metrics[1] != -1 || metrics[2] != -1) {
          Messaging.setBattleFront(new MapLocation(metrics[1], metrics[2]));
        }
        RobotInfo[] attackableEnemies = Cache.getAttackableEnemies();
        if (attackableEnemies.length > 0) {
          if (rc.isWeaponReady()) {
            attackLeastHealthPrioritized(attackableEnemies);
          }
        } else {
          if (metrics[1] != -1 || metrics[2] != -1) {
            Nav.goTo(new MapLocation(metrics[1], metrics[2]), Engage.UNITS);
          } else {
            MapLocation nearestBattle = Messaging.getClosestBattleFront(curLoc);
            if (nearestBattle != null) {
              //rc.setIndicatorString(1, "Going to battlefront: " + nearestBattle + ", " + Clock.getRoundNum());
              Nav.goTo(nearestBattle, Engage.UNITS);
            } else {
              Nav.goTo(rallyPoint, Engage.NONE);
            }
          }
        }
      } else {
        // "are we definitely going to die?"
        if (metrics[1] > 0) {
          SupplyDistribution.setDyingMode();
          SupplyDistribution.manageSupply();
          if (rc.isWeaponReady()) {
            attackLeastHealthPrioritized(Cache.getAttackableEnemies());
          }
          
        // Retreat
        } else {
          if (rc.isCoreReady()) {
            int[] attackingEnemyDirs = calculateNumAttackingEnemyDirs();
            Nav.retreat(attackingEnemyDirs);
          }
        }
      }
    } else {
      if (rc.isCoreReady()) {
        MapLocation nearestBattle = Messaging.getClosestBattleFront(curLoc);
        if (nearestBattle != null ) {
          //rc.setIndicatorString(1, "Going to battlefront: " + nearestBattle + ", " + Clock.getRoundNum());
          Nav.goTo(nearestBattle, Engage.UNITS);
        } else if (rallyPoint != null) {
          Nav.goTo(rallyPoint, Engage.NONE);
        }
      }
    }
  }
  public void doOffensiveMicroSplit(RobotInfo[] engageableEnemies, MapLocation rallyPoint) throws GameActionException {
    //rc.setIndicatorString(0, "Offensive micro " + ", " + Clock.getRoundNum());

    if (engageableEnemies.length > 0) {
      // returns {is winning, is lowest health and not alone}
      int[] metrics = getBattleMetrics(engageableEnemies);
      if (metrics[0] > 0) {
        //rc.setIndicatorString(1, "winning..." + Clock.getRoundNum());

        if (metrics[1] != -1 || metrics[2] != -1) {
          Messaging.setBattleFront(new MapLocation(metrics[1], metrics[2]));
        }
        RobotInfo[] attackableEnemies = Cache.getAttackableEnemies();
        if (attackableEnemies.length > 0) {
          if (rc.isWeaponReady()) {
            attackLeastHealthPrioritized(attackableEnemies);
          }
        } else {
          if (metrics[1] != -1 || metrics[2] != -1) {
            Nav.goTo(new MapLocation(metrics[1], metrics[2]), Engage.UNITS);
          } else {
            MapLocation nearestBattle = Messaging.getClosestBattleFront(curLoc);
            if (nearestBattle != null && rc.getLocation().distanceSquaredTo(nearestBattle) <= 25) {
              //rc.setIndicatorString(1, "Going to battlefront: " + nearestBattle + ", " + Clock.getRoundNum());
              Nav.goTo(nearestBattle, Engage.UNITS);
            } else {
              Nav.goTo(rallyPoint, Engage.NONE);
            }
          }
        }
      } else {
        // "are we definitely going to die?"
        if (metrics[1] > 0) {
          SupplyDistribution.setDyingMode();
          SupplyDistribution.manageSupply();
          if (rc.isWeaponReady()) {
            attackLeastHealthPrioritized(Cache.getAttackableEnemies());
          }
          
        // Retreat
        } else {
          if (rc.isCoreReady()) {
            int[] attackingEnemyDirs = calculateNumAttackingEnemyDirs();
            Nav.retreat(attackingEnemyDirs);
          }
        }
      }
    } else {
      if (rc.isCoreReady()) {
        MapLocation nearestBattle = Messaging.getClosestBattleFront(curLoc);
        if (nearestBattle != null && rc.getLocation().distanceSquaredTo(nearestBattle) <= 25) {
          //rc.setIndicatorString(1, "Going to battlefront: " + nearestBattle + ", " + Clock.getRoundNum());
          Nav.goTo(nearestBattle, Engage.UNITS);
        } else if (rallyPoint != null) {
          Nav.goTo(rallyPoint, Engage.NONE);
        }
      }
    }
  }
  
  public void doDefensiveMicro(RobotInfo[] engageableEnemies, MapLocation rallyPoint) throws GameActionException {
    if (engageableEnemies.length > 0) {
      // returns {is winning, is lowest health and not alone}
      int[] metrics = getBattleMetrics(engageableEnemies);
      // rc.setIndicatorString(1, Arrays.toString(metrics));
      if (metrics[0] > 0) {
        Messaging.setDefendFront(curLoc);
        RobotInfo[] attackableEnemies = Cache.getAttackableEnemies();
        if (attackableEnemies.length > 0) {
          if (rc.isWeaponReady()) {
            attackLeastHealthPrioritized(attackableEnemies);
          }
        } else {
          if (metrics[1] != -1 || metrics[2] != -1) {
            Nav.goTo(new MapLocation(metrics[1], metrics[2]), Engage.UNITS);
          } else {
            MapLocation nearestBattle = Messaging.getClosestDefendFront(curLoc);
            if (nearestBattle != null) {
              Nav.goTo(nearestBattle, Engage.UNITS);
            } else if (rallyPoint != null) {
              Nav.goTo(rallyPoint, Engage.UNITS);
            }
          }
        }
      } else {
        // "are we definitely going to die?"
        if (metrics[1] > 0) {
          SupplyDistribution.setDyingMode();
          SupplyDistribution.manageSupply();
          if (rc.isWeaponReady()) {
            attackLeastHealthPrioritized(Cache.getAttackableEnemies());
          }
          
        // Retreat
        } else {
          if (rc.isCoreReady()) {
            int[] attackingEnemyDirs = calculateNumAttackingEnemyDirs();
            Nav.retreat(attackingEnemyDirs);
          }
        }
      }
    } else {
      if (rc.isCoreReady()) {
        MapLocation nearestBattle = Messaging.getClosestDefendFront(curLoc);
        if (nearestBattle != null) {
          Nav.goTo(nearestBattle, Engage.UNITS);
        } else if (rallyPoint != null) {
          Nav.goTo(rallyPoint, Engage.UNITS);
        }
      }
    }
  }
}