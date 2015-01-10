package ghettoblaster.BotTypes;

import ghettoblaster.Messaging;
import ghettoblaster.Nav;
import ghettoblaster.RobotPlayer.BaseBot;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class Soldier extends BaseBot {
  public Soldier(RobotController rc) {
    super(rc);
  }

  protected MapLocation rallyPoint = null;

  public void execute() throws GameActionException {
    RobotInfo[] enemies = getEnemiesInAttackingRange();

    if (enemies.length > 0) {
      // attack!
      if (rc.isWeaponReady()) {
        attackLeastHealthEnemy(enemies);
      }
    } else if (rc.isCoreReady()) {
      if (rallyPoint == null) {
        int rallyX = rc.readBroadcast(Messaging.RALLY_POINT_X);
        int rallyY = rc.readBroadcast(Messaging.RALLY_POINT_Y);
        rallyPoint = new MapLocation(rallyX, rallyY);
      }

      Nav.goTo(rallyPoint);
    }
    rc.yield();
  }
}