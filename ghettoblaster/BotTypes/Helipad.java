package ghettoblaster.BotTypes;

import ghettoblaster.RobotPlayer.BaseBot;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class Helipad extends BaseBot {
  public Helipad(RobotController rc) {
	  super(rc);
  }

  public void execute() throws GameActionException {
    if (rc.isCoreReady() && rc.hasSpawnRequirements(RobotType.DRONE)) {
      Direction newDir = getSpawnDirection(RobotType.DRONE);
      if (newDir != null) {
        rc.spawn(newDir, RobotType.DRONE);
      }
    }

    rc.yield();
  }
}
