package ghetto_v2_miners.BotTypes;

import ghetto_v2_miners.RobotPlayer.BaseBot;
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
      Direction newDir = getDefensiveSpawnDirection(RobotType.DRONE);
      if (newDir != null) {
        rc.spawn(newDir, RobotType.DRONE);
      }
    }
    rc.yield();
  }
}
