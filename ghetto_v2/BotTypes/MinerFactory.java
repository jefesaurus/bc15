package ghetto_v2.BotTypes;

import ghetto_v2.Messaging;
import ghetto_v2.RobotPlayer.BaseBot;
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
    int numMiners = 2;
//    System.out.println("miner goal: " + numMiners);
//    System.out.println("core ready: " + rc.isCoreReady());
//    System.out.println("has reqs: " + rc.hasBuildRequirements(RobotType.MINER));
//    System.out.println("ore: " + rc.getTeamOre());


    if (numMiners > 0 && rc.isCoreReady() && rc.hasSpawnRequirements(RobotType.MINER)) {

      Direction newDir = getSpawnDirection(RobotType.MINER);
      if (newDir != null) {
        rc.spawn(newDir, RobotType.MINER);
        rc.broadcast(Messaging.NUM_MINERS, numMiners-1);
      }
    }
    rc.yield();
  }
}
