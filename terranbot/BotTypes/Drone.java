package terranbot.BotTypes;

import terranbot.HibernateSystem;
import terranbot.Messaging;
import terranbot.MovingBot;
import terranbot.Nav;
import terranbot.SupplyDistribution;
import terranbot.Nav.Engage;
import terranbot.RobotPlayer.BaseBot;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class Drone extends MovingBot {   
  
  public Drone(RobotController rc) {
    super(rc);
    SupplyDistribution.init(this);
    SupplyDistribution.setBatteryMode();
    HibernateSystem.init(rc);
  }

  
  private void attackMicro(MapLocation loc) throws GameActionException {
    if (rc.isCoreReady()) {
      double[] dangerVals = this.getAllDangerVals();
      // If the center square is in danger, retreat
      if (dangerVals[8] > 0) {
        if (Nav.moveToSafetyIfPossible(dangerVals)) {
          return;
        } else if (currentEnemies.length > 0 && rc.isWeaponReady()) {
          attackLeastHealthEnemy(currentEnemies);
        }
      } else if (currentEnemies.length > 0 && rc.isWeaponReady()) {
        attackLeastHealthEnemy(currentEnemies);
      } else {
        Nav.goTo(loc, Engage.UNITS);
      }
    }
  }

  protected MapLocation rallyPoint = null;
  protected MovingBot.AttackMode mode = MovingBot.AttackMode.OFFENSIVE_SWARM;
  public MapLocation towerToHelp = null;

  public void execute() throws GameActionException {
    mode = MovingBot.AttackMode.HUNT_FOR_MINERS;
    currentEnemies = getEnemiesInAttackingRange();
    rallyPoint = Messaging.readRallyPoint();
    rc.setIndicatorString(1, rallyPoint.toString());
    
    if (HibernateSystem.manageHibernation(mode, currentEnemies, rallyPoint)) {
      return;
    }
    
    SupplyDistribution.manageSupply();
    
    switch (mode) {
    case HUNT_FOR_MINERS:
      doOffensiveMicro(currentEnemies, this.enemyHQ);
      break;
    case RALLYING:
      if (currentEnemies.length < 0) {
        Nav.goTo(rallyPoint, Engage.UNITS);
      } else {
        attackMicro(rallyPoint);
      }
      break;
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
    case DEFEND_TOWERS:
      if (rc.isCoreReady()) {
        int[] attackingEnemyDirs = this.calculateNumAttackingEnemyDirs();
        // If the center square is in danger, retreat
        if (attackingEnemyDirs[8] > 0) {
          Nav.retreat(attackingEnemyDirs);
        } else if (currentEnemies.length > 0 && rc.isWeaponReady()){
          attackLeastHealthEnemy(currentEnemies);
        // Can move, not in danger, can't attack: Advance
        } else {
          towerToHelp = Messaging.getClosestTowerUnderAttack();
          if (towerToHelp != null) {
            Nav.goTo(towerToHelp, Engage.UNITS);
          } else {
            MapLocation[] ourTowers = rc.senseTowerLocations();
            Nav.goTo(ourTowers[rc.getID()%ourTowers.length], Engage.UNITS);
          }
        }
      // If we can't move, but we can attack, do so only if we aren't in danger.
      } else if (rc.isWeaponReady()) {
        double[] dangerVals = this.getAllDangerVals();
        // If the center square is in danger, retreat
        if (dangerVals[8] <= 0) {
          attackLeastHealthEnemy(currentEnemies);
        }
      }
      
      break;
      
    case OFFENSIVE_SWARM:
      // Potentially change to engage none.
      attackMicro(rallyPoint);
      break;
    default:
      if (currentEnemies.length > 0) {
        if (rc.isWeaponReady()) {
          attackLeastHealthEnemy(currentEnemies);
        }
      } else if (rc.isCoreReady()) {
        if (rallyPoint != null) {
          Nav.goTo(rallyPoint, Engage.UNITS);
        }
      }
      break;
    }
  }
}
