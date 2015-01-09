package ghettoblaster.BotTypes;

import ghettoblaster.Messaging;
import ghettoblaster.RobotPlayer.BaseBot;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class MinerFactory extends BaseBot {
  public static int targetNumMiners;
  
  public MinerFactory(RobotController rc) {
    super(rc);
  }

  public void execute() throws GameActionException {
    int numMiners = rc.readBroadcast(Messaging.NUM_MINERS);

    if (numMiners > 0 && rc.isCoreReady() && rc.hasBuildRequirements(RobotType.MINER)) {
      Direction newDir = getSpawnDirection(RobotType.MINER);
      if (newDir != null) {
        rc.spawn(newDir, RobotType.MINER);
        rc.broadcast(Messaging.NUM_MINERS, numMiners--);
      }
    }
  }
}
