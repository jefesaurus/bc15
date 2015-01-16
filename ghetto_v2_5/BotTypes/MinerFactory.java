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
  public static RobotType[] types = {RobotType.MINER};
  public MinerFactory(RobotController rc) {
    super(rc);
  }

  public void execute() throws GameActionException {
    int unitToProduce = Messaging.getUnitToProduce();
    if (unitToProduce != -1 || unitToProduce != RobotType.MINER.ordinal()) {
      return;
    }
    
    //Build units if queued
    for (int i=types.length; i-- > 0;) {
      RobotType curType = types[i];
      if (Messaging.dequeueUnit(curType)) {
        while (!rc.isCoreReady() && !rc.hasBuildRequirements(curType)) {rc.yield();};
        Direction spawnDir = getDefensiveSpawnDirection(curType);
        if (spawnDir != null) {
          rc.spawn(spawnDir, curType);
        } else {
          System.out.println("WRITE CODE HERE, NEED TO FIND PLACE TO BUILD (MINERFACTORY)");
        }
      }
    }
    
    rc.yield();
  }
}
