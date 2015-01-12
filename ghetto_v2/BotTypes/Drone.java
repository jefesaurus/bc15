package ghetto_v2.BotTypes;

import ghetto_v2.Messaging;
import ghetto_v2.Nav;
import ghetto_v2.Nav.Engage;
import ghetto_v2.RobotPlayer.BaseBot;
import ghetto_v2.RobotPlayer.MovingBot;
import ghetto_v2.SupplyDistribution;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class Drone extends MovingBot {  

  private SupplyDistribution supply;
  private final int HIBERNATE_DISTANCE = 25;
  public Drone(RobotController rc) {
    super(rc);
    SupplyDistribution.init(this);
    SupplyDistribution.setBatteryMode();
  }
  
  public void setup() {
    
  }
  
  private void attackMicro(MapLocation loc) throws GameActionException {
    if (rc.isCoreReady()) {
      double[] dangerVals = this.getAllDangerVals();
      // If the center square is in danger, retreat
      if (dangerVals[8] > 0) {
        Nav.retreat(dangerVals);
      } else if (currentEnemies.length > 0 && rc.isWeaponReady()){
        attackLeastHealthEnemy(currentEnemies);
        
      // Can move, not in danger, can't attack: Advance
      } else {
        Nav.goTo(loc, Engage.UNITS);
      }

    // If we can't move, but we can attack, do so only if we aren't in danger.
    } else if (rc.isWeaponReady()) {
      double[] dangerVals = this.getAllDangerVals();
      // If the center square is in danger, retreat
      if (dangerVals[8] <= 0) {
        attackLeastHealthEnemy(currentEnemies);
      }
    }
  }

  protected MapLocation rallyPoint = null;
  protected MovingBot.AttackMode mode = MovingBot.AttackMode.OFFENSIVE_SWARM;

  public void execute() throws GameActionException {
    currentEnemies = getEnemiesInAttackingRange();
    rallyPoint = Messaging.readRallyPoint();
    mode = Messaging.getFleetMode();
    Messaging.addToFleetCentroid();
    
    rc.setIndicatorString(2, "Dest: " + rallyPoint + ", Mode: " + mode.name());

    if (mode == MovingBot.AttackMode.RALLYING || mode == MovingBot.AttackMode.DEFEND_TOWERS) {
       if (currentEnemies.length == 0 && (Nav.dest != null && this.curLoc.distanceSquaredTo(Nav.dest) < HIBERNATE_DISTANCE)) {
         //Hibernate
         return;
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
    case TOWER_DIVE:
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
          Nav.goTo(rallyPoint, Engage.TOWERS);
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
          MapLocation[] ourTowers = rc.senseTowerLocations();
          Nav.goTo(ourTowers[rc.getID()%ourTowers.length], Engage.UNITS);
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
      if (rc.isCoreReady()) {
        double[] dangerVals = this.getAllDangerVals();
        // If the center square is in danger, retreat
        if (dangerVals[8] > 0) {
          Nav.retreat(dangerVals);
        } else if (currentEnemies.length > 0 && rc.isWeaponReady()){
          attackLeastHealthEnemy(currentEnemies);
          
        // Can move, not in danger, can't attack: Advance
        } else {
          Nav.goTo(rallyPoint, Engage.NONE);
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
