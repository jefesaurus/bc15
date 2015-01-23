package rambo.BotTypes;

import rambo.Cache;
import rambo.HibernateSystem;
import rambo.Messaging;
import rambo.MovingBot;
import rambo.Nav;
import rambo.SupplyDistribution;
import rambo.Nav.Engage;
import rambo.RobotPlayer.BaseBot;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class Commander extends MovingBot {  
  
  public Commander(RobotController rc) {
    super(rc);
    SupplyDistribution.init(this);
    SupplyDistribution.setBatteryMode();
    HibernateSystem.init(rc);
  }
  
  public MapLocation defensiveFlash() throws GameActionException {
    //TODO insert !rc.isCoreReady()
    if (rc.getFlashCooldown() >= 1) {
      return null;
    } else {
      System.out.println("defensive flashing!");
      RobotInfo[] visibleEnemies = Cache.getVisibleEnemies();
      int numEnemiesInRange = 0;
      int x = 0;
      int y = 0;
      for (int i=visibleEnemies.length;i-->0;) {
        RobotInfo info = visibleEnemies[i];
        if (rc.getLocation().distanceSquaredTo(info.location) <= info.type.attackRadiusSquared) {
          numEnemiesInRange++;
          x += info.location.x;
          y += info.location.y;
        }
      }
      if (numEnemiesInRange > 0) {
        MapLocation enemyCentroid = new MapLocation(x / numEnemiesInRange, y / numEnemiesInRange);
        Direction escape = enemyCentroid.directionTo(rc.getLocation());
        MapLocation escapeLoc = rc.getLocation();
        while (rc.getLocation().distanceSquaredTo(escapeLoc.add(escape)) <= GameConstants.FLASH_RANGE_SQUARED) {
          escapeLoc.add(escape);
        }
        return escapeLoc;
      } else {
        return null;
      }
    }
  }
  
  /**public MapLocation offensiveFlash(MapLocation loc) throws GameActionException {
    
  }
   * @throws GameActionException **/
  
  public void doHarassMicro(RobotInfo[] engageableEnemies, MapLocation rallyPoint) throws GameActionException {
    if (rc.getSupplyLevel() <= 2000) {
      return;
    }
    if (engageableEnemies.length > 0) {
      int[] metrics = getBattleMetrics(engageableEnemies);
      if (metrics[1] > 0) {
        MapLocation flashLoc = defensiveFlash();
        Direction retreatDir = getBestRetreatDir(engageableEnemies);
        if (flashLoc == null && retreatDir == null) {
          SupplyDistribution.setDyingMode();
          SupplyDistribution.manageSupply();
          if (rc.isWeaponReady()) {
            attackLeastHealthEnemy(Cache.getAttackableEnemies());
          }
        } else if (flashLoc != null) {
          rc.castFlash(flashLoc);
        } else {
          rc.move(retreatDir);
        }
      }
      RobotInfo[] attackableEnemies = Cache.getAttackableEnemies();
      if (attackableEnemies.length > 0) {
        if (rc.isWeaponReady()) {
          attackLeastHealthEnemy(attackableEnemies);
        }
      } else {
        if (rc.isCoreReady())  {
          Nav.goTo(enemyHQ, Engage.UNITS);
        }
      }
    } else {
      if (rc.isCoreReady())  {
        Nav.goTo(enemyHQ, Engage.UNITS);
      }
    }
  }

  public void doOffensiveMicro(RobotInfo[] engageableEnemies, MapLocation rallyPoint) throws GameActionException {
    if (engageableEnemies.length > 0) {
      // returns {is winning, is lowest health and not alone}
      int[] metrics = getBattleMetrics(engageableEnemies);
      // rc.setIndicatorString(1, Arrays.toString(metrics));
      if (metrics[0] > 0) {
        if (metrics[1] != -1 || metrics[2] != -1) {
          Messaging.setBattleFront(new MapLocation(metrics[1], metrics[2]));
        } else {
          Messaging.setBattleFront(curLoc);
        }
        RobotInfo[] attackableEnemies = Cache.getAttackableEnemies();
        if (attackableEnemies.length > 0) {
          if (rc.isWeaponReady()) {
            attackLeastHealthEnemy(attackableEnemies);
          }
        } else {
          if (metrics[1] != -1 || metrics[2] != -1) {
            Nav.goTo(new MapLocation(metrics[1], metrics[2]), Engage.UNITS);
          } else {
            MapLocation nearestBattle = Messaging.getClosestBattleFront(curLoc);
            if (nearestBattle != null) {
              Nav.goTo(nearestBattle, Engage.UNITS);
            }
          }
        }
      } else {
        // "are we definitely going to die?"
        if (metrics[1] > 0) {
          MapLocation flashLoc = defensiveFlash();
          if (flashLoc == null) {
            SupplyDistribution.setDyingMode();
            SupplyDistribution.manageSupply();
            if (rc.isWeaponReady()) {
              attackLeastHealthEnemy(Cache.getAttackableEnemies());
            }
          } else {
            rc.castFlash(flashLoc);
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
          Nav.goTo(nearestBattle, Engage.UNITS);
        } else if (rallyPoint != null) {
          Nav.goTo(rallyPoint, Engage.UNITS);
        }
      }
    }
  }
  
  protected MapLocation rallyPoint = null;
  protected MovingBot.AttackMode mode = MovingBot.AttackMode.OFFENSIVE_SWARM;

  public void execute() throws GameActionException {
    currentEnemies = Cache.getEngagementEnemies();
    
    rallyPoint = Messaging.readRallyPoint();
    mode = Messaging.getFleetMode();
    if (HibernateSystem.manageHibernation(mode, currentEnemies, rallyPoint)) {
      return;
    }
    rc.setIndicatorString(2, "Mode: " + mode.name() + ", Rally point: " + rallyPoint);
    SupplyDistribution.manageSupply();

    switch (mode) {
    case SAFE_TOWER_DIVE:
      if (currentEnemies.length > 0) {
        RobotInfo[] attackableEnemies = Cache.getAttackableEnemies();
        if (attackableEnemies.length > 0) {
          if (rc.isWeaponReady()) {
            if (rc.canAttackLocation(rallyPoint)) {
              rc.attackLocation(rallyPoint);
            } else {
              attackLeastHealthEnemy(attackableEnemies);
            }
          }
        } else {
          if (rc.isCoreReady() && rallyPoint != null) {
            Nav.goTo(rallyPoint, Engage.ONE_TOWER);
          }
        }
      } else if (rc.isCoreReady()) {
        if (rallyPoint != null) {
          Nav.goTo(rallyPoint, Engage.ONE_TOWER);
        }
      }
      break;
    case UNSAFE_TOWER_DIVE:
      if (currentEnemies.length > 0) {
        RobotInfo[] attackableEnemies = Cache.getAttackableEnemies();
        if (attackableEnemies.length > 0) {
          if (rc.isWeaponReady()) {
            if (rc.canAttackLocation(rallyPoint)) {
              rc.attackLocation(rallyPoint);
            } else {
              attackLeastHealthEnemy(attackableEnemies);
            }
          }
        } else {
          if (rc.isCoreReady() && rallyPoint != null) {
            Nav.goTo(rallyPoint, Engage.ALL_TOWERS);
          }
        }
      } else if (rc.isCoreReady()) {
        if (rallyPoint != null) {
          Nav.goTo(rallyPoint, Engage.ALL_TOWERS);
        }
      }
      break;
    case RALLYING:
    case OFFENSIVE_SWARM:
        doOffensiveMicro(currentEnemies, rallyPoint);
      break;
    case DEFEND_TOWERS:
    case DEFENSIVE_SWARM:
      doDefensiveMicro(currentEnemies, rallyPoint);
      break;
    case COUNTER_ATTACK:
      doSneakyMove(rallyPoint);
      break;
    default:
      System.out.println("No default behavior");
      break;
    }
  }
}
