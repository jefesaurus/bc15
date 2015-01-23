package launcherrambobot.BotTypes;

import launcherrambobot.Cache;
import launcherrambobot.HibernateSystem;
import launcherrambobot.Messaging;
import launcherrambobot.MovingBot;
import launcherrambobot.Nav;
import launcherrambobot.SupplyDistribution;
import launcherrambobot.Nav.Engage;
import launcherrambobot.RobotPlayer.BaseBot;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class Launcher extends MovingBot {  
  
  public Launcher(RobotController rc) {
    super(rc);
    SupplyDistribution.init(this);
    SupplyDistribution.setBatteryMode();
    HibernateSystem.init(rc);
  }

  protected MapLocation rallyPoint = null;
  protected MovingBot.AttackMode mode = MovingBot.AttackMode.OFFENSIVE_SWARM;
  
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
          if (canLauncherAttack()) {
            System.out.println("can launch missiles");
            launchMissiles(attackableEnemies);
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
          SupplyDistribution.setDyingMode();
          SupplyDistribution.manageSupply();
          if (canLauncherAttack()) {
            launchMissiles(Cache.getAttackableEnemies());
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
  
  public boolean canLauncherAttack() {
    if (rc.getMissileCount() > 0) {
      return true;
    }
    return false;
  }
  
  public void launchMissiles(RobotInfo[] enemies) throws GameActionException {
    System.out.println("can launch missiles");

    if (enemies.length == 0) {
      return;
    }

    double minEnergon = Double.MAX_VALUE;
    MapLocation toAttack = null;
    for (int i = enemies.length; i-- > 0;) {
      if (enemies[i].health < minEnergon) {
        toAttack = enemies[i].location;
        minEnergon = enemies[i].health;
      }
    }

    if (rc.canLaunch(rc.getLocation().directionTo(toAttack))) {
      rc.launchMissile(rc.getLocation().directionTo(toAttack));    }
  }
  
  public void doDefensiveMicro(RobotInfo[] engageableEnemies, MapLocation rallyPoint) throws GameActionException {
    if (engageableEnemies.length > 0) {
      // returns {is winning, is lowest health and not alone}
      int[] metrics = getBattleMetrics(engageableEnemies);
      // rc.setIndicatorString(1, Arrays.toString(metrics));
      if (metrics[0] > 0) {
        System.out.println("we think winning in defensive");
        Messaging.setDefendFront(curLoc);
        RobotInfo[] attackableEnemies = Cache.getAttackableEnemies();
        if (attackableEnemies.length > 0) {
          System.out.println("trying to launch");
          if (canLauncherAttack()) {
            System.out.println("trying to launch");
            launchMissiles(attackableEnemies);
          }
        } else {
          if (metrics[1] != -1 || metrics[2] != -1) {
            Nav.goTo(new MapLocation(metrics[1], metrics[2]), Engage.UNITS);
          } else {
            MapLocation nearestBattle = Messaging.getClosestDefendFront(curLoc);
            if (nearestBattle != null) {
              Nav.goTo(nearestBattle, Engage.UNITS);
            }
          }
        }
      } else {
        // "are we definitely going to die?"
        if (metrics[1] > 0) {
          SupplyDistribution.setDyingMode();
          SupplyDistribution.manageSupply();
          if (canLauncherAttack()) {
            launchMissiles(Cache.getAttackableEnemies());
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
    default:
      System.out.println("No default behavior");
      break;
    }
  }
}
