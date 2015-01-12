package ghetto_v2_dummy.BotTypes;

import ghetto_v2_dummy.Messaging;
import ghetto_v2_dummy.Nav;
import ghetto_v2_dummy.Nav.Engage;
import ghetto_v2_dummy.RobotPlayer.BaseBot;
import ghetto_v2_dummy.RobotPlayer.MovingBot;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class Soldier extends MovingBot {  
  
  public Soldier(RobotController rc) {
    super(rc);
  }
  
  public void setup() {
    
  }

  protected MapLocation rallyPoint = null;
  protected MovingBot.AttackMode mode = MovingBot.AttackMode.OFFENSIVE_SWARM;

  public void execute() throws GameActionException {
    currentEnemies = getEnemiesInAttackingRange();

    rc.setIndicatorString(0, Integer.toString(Clock.getBytecodeNum()));

    
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
          Nav.goTo(rallyPoint, Engage.TOWERS);
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
    rc.setIndicatorString(1, Integer.toString(Clock.getBytecodeNum()));

    rc.yield();
  }
}
