package ghetto_v2_5.BotTypes;

import ghetto_v2_5.RobotPlayer.BaseBot;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class Barracks extends BaseBot {
  public Barracks(RobotController rc) {
    super(rc);
  }

  public void execute() throws GameActionException {
    if (rc.isCoreReady() && rc.getTeamOre() > 200) {
      Direction newDir = getOffensiveSpawnDirection(RobotType.SOLDIER);
      if (newDir != null) {
        rc.spawn(newDir, RobotType.SOLDIER);
      }
    }

    rc.yield();
  }
}