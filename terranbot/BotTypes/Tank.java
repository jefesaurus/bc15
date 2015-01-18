package terranbot.BotTypes;

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
    currentEnemies = getEnemiesInAttackingRange();
    
    rallyPoint = Messaging.readRallyPoint();
    mode = Messaging.getFleetMode();
    if (HibernateSystem.manageHibernation(mode, currentEnemies, rallyPoint)) {
      return;
    }
    rc.setIndicatorString(1, mode.name());
    SupplyDistribution.manageSupply();

    switch (mode) {
    case SAFE_TOWER_DIVE:
      if (currentEnemies.length > 0) {
        if (rc.isWeaponReady()) {
          if (rc.canAttackLocation(rallyPoint)) {
            rc.attackLocation(rallyPoint);
          } else {
            attackLeastHealthEnemy(currentEnemies);
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
        if (rc.isWeaponReady()) {
          if (rc.canAttackLocation(rallyPoint)) {
            rc.attackLocation(rallyPoint);
          } else {
            attackLeastHealthEnemy(currentEnemies);
          }
        }
      } else if (rc.isCoreReady()) {
        if (rallyPoint != null) {
          Nav.goTo(rallyPoint, Engage.ALL_TOWERS);
        }
      }
      break;
    case OFFENSIVE_SWARM:
      if (currentEnemies.length > 0) {
        
        // returns {is winning, is lowest health and not alone}
        boolean[] winning = isFightWinning();
        if (winning[0]) {
          if (rc.isWeaponReady()) {
            // TODO If there is no least health enemy, attack enemy closest to us.
            attackLeastHealthEnemy(currentEnemies);
          }
        } else {
          // "are we definitely going to die?"
          if (winning[1]) {
            // SupplyDistribution.setDyingMode();
            // SupplyDistribution.manageSupply();
            if (rc.isWeaponReady()) {
              attackLeastHealthEnemy(currentEnemies);
            }
            
          // Retreat
          } else {
            if (rc.isCoreReady()) {
              int[] attackingEnemyDirs = calculateNumAttackingEnemyDirs();
              Nav.retreat(attackingEnemyDirs);
            }
          }
        }
      }
      break;
    default:
      if (currentEnemies.length > 0) {
        if (rc.isWeaponReady()) {
          attackLeastHealthEnemy(currentEnemies);
        }
      } else if (rc.isCoreReady()) {

        if (rallyPoint != null) {

          Nav.goTo(rallyPoint, Engage.NONE);

        }
      }
      break;
    }
  }
}
