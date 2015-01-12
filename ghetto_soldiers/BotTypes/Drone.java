package ghetto_soldiers.BotTypes;

import ghetto_soldiers.Messaging;
import ghetto_soldiers.Nav;
import ghetto_soldiers.Nav.Engage;
import ghetto_soldiers.RobotPlayer.BaseBot;
import ghetto_soldiers.RobotPlayer.MovingBot;
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
