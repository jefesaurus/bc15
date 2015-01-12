package ghetto_v2.BotTypes;

import ghetto_v2.Messaging;
import ghetto_v2.RobotPlayer.BaseBot;
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
    RobotInfo[] visibleEnemies = getVisibleEnemies();
    if (visibleEnemies.length > 0) {
      Messaging.setTowerUnderAttack(this.curLoc);
      RobotInfo[] attackableEnemies = this.getEnemiesInAttackingRange();
      if (attackableEnemies.length > 0 && rc.isWeaponReady()) {
        attackLeastHealthEnemy(attackableEnemies);
      }
    }
    rc.yield();
  }
}