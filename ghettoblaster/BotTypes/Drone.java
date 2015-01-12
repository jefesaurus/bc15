package ghettoblaster.BotTypes;

import ghettoblaster.Messaging;
import ghettoblaster.Nav;
import ghettoblaster.Nav.Engage;
import ghettoblaster.RobotPlayer.BaseBot;
import ghettoblaster.RobotPlayer.MovingBot;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class Drone extends MovingBot {  

  public Drone(RobotController rc) {
    super(rc);
  }
  
  public void setup() {
    
  }

  protected MapLocation rallyPoint = null;
  protected MovingBot.AttackMode mode = MovingBot.AttackMode.OFFENSIVE_SWARM;

  public void execute() throws GameActionException {
    currentEnemies = getEnemiesInAttackingRange();
    
    rallyPoint = Messaging.readRallyPoint();
    mode = Messaging.getSoldierMode();
    
    switch (mode) {
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
          Nav.goTo(rallyPoint, Engage.UNITS);
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

    rc.yield();
  }
}
