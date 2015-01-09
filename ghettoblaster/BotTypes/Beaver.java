package ghettoblaster.BotTypes;

import ghettoblaster.Nav;
import ghettoblaster.Nav.Engage;
import ghettoblaster.RobotPlayer.BaseBot;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class Beaver extends BaseBot {
  public Beaver(RobotController rc) {
    super(rc);
  }

  public void execute() throws GameActionException {
    Nav.goTo(new MapLocation(14087, 13464));
    if (rc.isCoreReady()) {
      if (rc.getTeamOre() < 500) {
        // mine
        if (rc.senseOre(rc.getLocation()) > 0) {
          rc.mine();
        } else {
          Direction newDir = getMoveDir(this.enemyHQ);

          if (newDir != null) {
            rc.move(newDir);
          }
        }
      } else {
        // build barracks
        Direction newDir = getBuildDirection(RobotType.BARRACKS);
        if (newDir != null) {
          rc.build(newDir, RobotType.BARRACKS);
        }
      }
    }

    rc.yield();
  }
}