package rambo.BotTypes;

import rambo.Messaging;
import rambo.RobotPlayer.BaseBot;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class Tower extends BaseBot {
  public Tower(RobotController rc) {
    super(rc);
  }

  public void execute() throws GameActionException {
    RobotInfo[] visibleEnemies = getVisibleEnemies();
    boolean hasLaunchers = false;
    boolean hasNotMinerAndBeaverUnits = false;
    if (visibleEnemies.length > 0) {
      MapLocation closest = null;
      for (int i=visibleEnemies.length; i-->0;) {
        MapLocation trialLoc = visibleEnemies[i].location;
        if (visibleEnemies[i].type != RobotType.MINER || visibleEnemies[i].type != RobotType.BEAVER) {
          hasNotMinerAndBeaverUnits = true;
        }
        if (visibleEnemies[i].type == RobotType.LAUNCHER) {
          hasLaunchers = true;
        }
        if (closest == null) {
          closest = trialLoc;
        } else {
          if (this.curLoc.distanceSquaredTo(trialLoc) < this.curLoc.distanceSquaredTo(closest)) {
            closest = trialLoc;
          }
        }
      }
      Messaging.setDefendFront(closest);
    }
    if (hasLaunchers || (hasNotMinerAndBeaverUnits && visibleEnemies.length >= 3)) {
      Messaging.setTowerUnderAttack(this.curLoc);
      RobotInfo[] attackableEnemies = this.getEnemiesInAttackingRange();
      if (attackableEnemies.length > 0 && rc.isWeaponReady()) {
        attackLeastHealthEnemy(attackableEnemies);
      }
    }
  }
}