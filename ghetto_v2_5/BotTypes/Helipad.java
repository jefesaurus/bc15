package ghetto_v2_5.BotTypes;

import ghetto_v2_5.Messaging;
import ghetto_v2_5.RobotPlayer.BaseBot;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;


public class Helipad extends BaseBot {
  public static final RobotType[] types = {RobotType.DRONE};

  public Helipad(RobotController rc) {
	  super(rc);
  }

  public void execute() throws GameActionException {
    int unitToProduce = Messaging.getUnitToProduce();
    if (unitToProduce != -1 && unitToProduce != RobotType.DRONE.ordinal()) {
      return;
    }
    
    //Build units if queued
    for (int i=types.length; i-- > 0;) {
      RobotType curType = types[i];
      if (rc.isCoreReady() && rc.hasSpawnRequirements(curType) && Messaging.dequeueUnit(curType)) {
        Direction spawnDir = getDefensiveSpawnDirection(curType);
        if (spawnDir != null) {
          rc.spawn(spawnDir, curType);
        } else {
          System.out.println("WRITE CODE HERE, NEED TO FIND PLACE TO BUILD (HELIPAD)");
        }
      }
    }
    
    rc.yield();
  }
}
