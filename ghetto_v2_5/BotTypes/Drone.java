package ghetto_v2_5.BotTypes;

import ghetto_v2_5.Messaging;
import ghetto_v2_5.Nav;
import ghetto_v2_5.SupplyDistribution;
import ghetto_v2_5.Nav.Engage;
import ghetto_v2_5.RobotPlayer.BaseBot;
import ghetto_v2_5.RobotPlayer.MovingBot;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class Drone extends MovingBot {    
  private final int HIBERNATE_DISTANCE = 25;
  private boolean TRYING_TO_HIBERNATE = false;
  private int HIBERNATE_COUNT_DOWN = 5;
  
  public Drone(RobotController rc) {
    super(rc);
    SupplyDistribution.init(this);
    SupplyDistribution.setBatteryMode();
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
    currentEnemies = getEnemiesInAttackingRange();
    rallyPoint = Messaging.readRallyPoint();
    mode = Messaging.getFleetMode();
    Messaging.addToFleetCentroid();
    
    if (mode == MovingBot.AttackMode.RALLYING || mode == MovingBot.AttackMode.DEFEND_TOWERS) {
       towerToHelp = Messaging.getClosestTowerUnderAttack();
       if (currentEnemies.length == 0 && towerToHelp == null && (Nav.dest == null || this.curLoc.distanceSquaredTo(Nav.dest) < HIBERNATE_DISTANCE)) {
         //Hibernate
         if (TRYING_TO_HIBERNATE) {
           if (HIBERNATE_COUNT_DOWN > 0) {
             HIBERNATE_COUNT_DOWN--;
           } else {
             rc.yield();
             return;
           }
         } else {
           TRYING_TO_HIBERNATE = true;
         }
       } else {
         TRYING_TO_HIBERNATE = false;
         HIBERNATE_COUNT_DOWN = 5;
       }
    }
    
    SupplyDistribution.manageSupply();
    
    switch (mode) {
    case HUNT_FOR_MINERS:
      attackMicro(this.enemyHQ);
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
        double[] dangerVals = this.getAllDangerVals();
        // If the center square is in danger, retreat
        if (dangerVals[8] > 0) {
          Nav.retreat(dangerVals);
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

    rc.yield();
  }
}
