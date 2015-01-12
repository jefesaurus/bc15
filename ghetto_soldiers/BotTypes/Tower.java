package ghetto_soldiers.BotTypes;

import ghetto_soldiers.RobotPlayer.BaseBot;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class Tower extends BaseBot {
  public Tower(RobotController rc) {
    super(rc);
  }

  public void execute() throws GameActionException {
    RobotInfo[] enemies = getEnemiesInAttackingRange();

    if (enemies.length > 0) {
      // attack!
      if (rc.isWeaponReady()) {
        attackLeastHealthEnemy(enemies);
      }
    }
    rc.yield();
  }
}