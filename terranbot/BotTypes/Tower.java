package terranbot.BotTypes;

import terranbot.Messaging;
import terranbot.RobotPlayer.BaseBot;
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
  }
}