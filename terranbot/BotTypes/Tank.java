package terranbot.BotTypes;

import java.util.Arrays;

import com.sun.xml.internal.bind.v2.runtime.unmarshaller.XsiNilLoader.Array;

import terranbot.Cache;
import terranbot.HibernateSystem;
import terranbot.Messaging;
import terranbot.Nav;
import terranbot.SupplyDistribution;
import terranbot.Nav.Engage;
import terranbot.RobotPlayer.BaseBot;
import terranbot.MovingBot;
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
    currentEnemies = Cache.getEngagementEnemies();//getEnemiesInAttackingRange();
    
    rallyPoint = Messaging.readRallyPoint();
    mode = Messaging.getFleetMode();
    if (HibernateSystem.manageHibernation(mode, currentEnemies, rallyPoint)) {
      return;
    }
    rc.setIndicatorString(2, mode.name());
    SupplyDistribution.manageSupply();

    switch (mode) {
    case SAFE_TOWER_DIVE:
      if (currentEnemies.length > 0) {
        RobotInfo[] attackableEnemies = getEnemiesInAttackingRange();
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
        RobotInfo[] attackableEnemies = getEnemiesInAttackingRange();
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
      if (currentEnemies.length > 0) {
        // returns {is winning, is lowest health and not alone}
        int[] metrics = getBattleMetrics(currentEnemies);
        rc.setIndicatorString(1, Arrays.toString(metrics));
        if (metrics[0] > 0) {
          if (metrics[1] != -1 || metrics[2] != -1) {
            Messaging.setBattleFront(new MapLocation(metrics[1], metrics[2]));
          } else {
            Messaging.setBattleFront(curLoc);
          }
          RobotInfo[] attackableEnemies = getEnemiesInAttackingRange();
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
            // SupplyDistribution.setDyingMode();
            // SupplyDistribution.manageSupply();
            if (rc.isWeaponReady()) {
              attackLeastHealthEnemy(getEnemiesInAttackingRange());
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
      break;
    default:
      System.out.println("No default behavior");
      break;
    }
  }
}
