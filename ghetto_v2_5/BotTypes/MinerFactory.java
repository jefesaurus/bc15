package ghetto_v2_5.BotTypes;

import ghetto_v2_5.Messaging;
import ghetto_v2_5.RobotPlayer.BaseBot;
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
    if (Messaging.dequeueMiner()) {
      while (!rc.isCoreReady() || !rc.hasSpawnRequirements(RobotType.MINER)) {
        rc.yield();
      };
      Direction newDir = getOffensiveSpawnDirection(RobotType.MINER);
      if (newDir != null) {
        rc.spawn(newDir, RobotType.MINER);
      }
    }
    rc.yield();
  }
}
