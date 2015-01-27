package rambo.BotTypes;

import rambo.Cache;
import rambo.HibernateSystem;
import rambo.Messaging;
import rambo.MovingBot;
import rambo.Nav;
import rambo.SupplyDistribution;
import rambo.Util;
import rambo.Nav.Engage;
import rambo.RobotPlayer.BaseBot;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class Tank extends MovingBot {  
  public static int id;
  public static MapLocation divable = null;
  public Tank(RobotController rc) {
    super(rc);
    SupplyDistribution.init(this);
    SupplyDistribution.setBatteryMode();
    HibernateSystem.init(rc);
    Util.randInit(rc.getID(), Clock.getRoundNum());
    id = Util.randInt() % 23;
  }

  protected MapLocation rallyPoint = null;
  protected MovingBot.AttackMode mode = MovingBot.AttackMode.OFFENSIVE_SWARM;

  
  public void execute() throws GameActionException {

    currentEnemies = Cache.getEngagementEnemies();
    
    rallyPoint = Messaging.readRallyPoint();
    mode = Messaging.getFleetMode();
    if (id % 2 != 0) {
      rallyPoint = Messaging.readRallyPoint2();
      mode = Messaging.getFleetMode2();
    } 
    
    divable = Messaging.getDivable(rc.getLocation());
    if (divable != null && rallyPoint != divable) {
      mode = MovingBot.AttackMode.HELP_DIVE;
    }
    rc.setIndicatorString(2, "Mode: " + mode.name() + ", Rally point: " + rallyPoint);
    if (HibernateSystem.manageHibernation(mode, currentEnemies, rallyPoint)) {
      //rc.setIndicatorString(2, "hibernating");
      return;
    }
    SupplyDistribution.manageSupply();

    switch (mode) {
    case HELP_DIVE:
      if (currentEnemies.length > 0) {
        RobotInfo[] attackableEnemies = Cache.getAttackableEnemies();
        if (attackableEnemies.length > 0) {
          if (rc.isWeaponReady()) {
            if (rc.canAttackLocation(divable)) {
              rc.attackLocation(divable);
            } else {
              attackLeastHealthEnemy(attackableEnemies);
            }
          }
        } else {
          if (rc.isCoreReady() && rallyPoint != null) {
            Nav.goTo(divable, Engage.ONE_TOWER);
          }
        }
      } else if (rc.isCoreReady()) {
        if (rallyPoint != null) {
          Nav.goTo(divable, Engage.ONE_TOWER);
        }
      }
      break;
    case SPLIT_PUSH:
      if (rallyPoint.equals(new MapLocation(8209, 12025))) {
        System.out.println("in split push");
      }
      //System.out.println("rallyPoint: " + rallyPoint);
      rc.setIndicatorString(2, "Mode: " + mode.name() + ", Rally point: " + rallyPoint);

      doOffensiveMicroSplit(currentEnemies, rallyPoint);
      break;
    case SAFE_TOWER_DIVE_SPLIT:
      rc.setIndicatorString(2, "Mode: " + mode.name() + ", Rally point: " + rallyPoint);
      if (currentEnemies.length > 0) {
        RobotInfo[] attackableEnemies = Cache.getAttackableEnemies();
        if (attackableEnemies.length > 0) {
          if (rc.isWeaponReady()) {
            if (rc.canAttackLocation(rallyPoint)) {
              rc.attackLocation(rallyPoint);
              if (rallyPoint.equals(new MapLocation(8209, 12025))) {
                System.out.println("attacking tower");
              }

            } else {
              attackLeastHealthEnemy(attackableEnemies);
              if (rallyPoint.equals(new MapLocation(8209, 12025))) {
                System.out.println("attacking units");
              }
            }
          }
        } else {
          if (rc.isCoreReady() && rallyPoint != null) {
            Nav.goTo(rallyPoint, Engage.ONE_TOWER);
            if (rallyPoint.equals(new MapLocation(8209, 12025))) {
              System.out.println("no attackable enmies");
            }

          }
        }
      } else if (rc.isCoreReady()) {
        if (rallyPoint != null) {
          Nav.goTo(rallyPoint, Engage.ONE_TOWER);
          if (rallyPoint.equals(new MapLocation(8209, 12025))) {
            System.out.println("no visible enemies");
          }
        }
      }
      break;
    case UNSAFE_TOWER_DIVE_SPLIT:
      if (rallyPoint.equals(new MapLocation(8209, 12025))) {
        System.out.println("in un safe tower dive split");
      }
      //System.out.println("rallyPoint: " + rallyPoint);

      rc.setIndicatorString(2, "Mode: " + mode.name() + ", Rally point: " + rallyPoint);

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
      rc.setIndicatorString(1, "rally: " + rallyPoint + Clock.getRoundNum());
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
