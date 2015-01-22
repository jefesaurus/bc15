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
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class Tank extends MovingBot {  
  
  public Tank(RobotController rc) {
    super(rc);
    SupplyDistribution.init(this);
    SupplyDistribution.setBatteryMode();
    HibernateSystem.init(rc);
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
