package ghettoblaster.BotTypes;

import ghettoblaster.RobotPlayer.BaseBot;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class Tower extends BaseBot {
  public Tower(RobotController rc) {
    super(rc);
  }

  public void execute() throws GameActionException {
    rc.yield();
  }
}