package ghettoblaster.BotTypes;

import ghettoblaster.RobotPlayer.BaseBot;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class Beaver extends BaseBot {
  public Beaver(RobotController rc) {
    super(rc);
  }

  public void execute() throws GameActionException {
    if (rc.isCoreReady()) {
      if (rc.getTeamOre() < 500) {
        // mine
        if (rc.senseOre(rc.getLocation()) > 0) {
          rc.mine();
        } else {
          Direction newDir = getMoveDir(this.theirHQ);

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