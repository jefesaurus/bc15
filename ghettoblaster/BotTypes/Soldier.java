package ghettoblaster.BotTypes;
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

  public void execute() throws GameActionException {
    RobotInfo[] enemies = getEnemiesInAttackingRange();

    if (enemies.length > 0) {
      // attack!
      if (rc.isWeaponReady()) {
        attackLeastHealthEnemy(enemies);
      }
    } else if (rc.isCoreReady()) {
      int rallyX = rc.readBroadcast(0);
      int rallyY = rc.readBroadcast(1);
      MapLocation rallyPoint = new MapLocation(rallyX, rallyY);

      Direction newDir = getMoveDir(rallyPoint);

      if (newDir != null) {
        rc.move(newDir);
      }
    }
    rc.yield();
  }
}