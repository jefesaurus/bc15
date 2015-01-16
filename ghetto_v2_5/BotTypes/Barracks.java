package ghetto_v2_5.BotTypes;

import ghetto_v2_5.Messaging;
import ghetto_v2_5.RobotPlayer.BaseBot;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class Barracks extends BaseBot {
  public static final RobotType[] types = {RobotType.SOLDIER, RobotType.BASHER};
  
  public Barracks(RobotController rc) {
    super(rc);
  }

  public void execute() throws GameActionException {
    Messaging.announceUnit(rc.getType());
    //Build units if queued
    for (int i=types.length; i-- > 0;) {
      RobotType curType = types[i];
      int unitToProduce = Messaging.getUnitToProduce();
      if (unitToProduce != -1 && unitToProduce != curType.ordinal()) {
        return;
      }
      if (rc.isCoreReady() && rc.hasSpawnRequirements(curType) && Messaging.dequeueUnit(curType)) {
        Direction spawnDir = getDefensiveSpawnDirection(curType);
        if (spawnDir != null) {
          Messaging.announceBuilding(rc.getType());
          rc.spawn(spawnDir, curType);
          Messaging.announceDoneBuilding(rc.getType());
          Messaging.announceDoneBuilding(curType);
          Messaging.announceUnit(rc.getType());
          //Have to announce it for that unit because of spawn sickness
          Messaging.announceUnit(curType);
        } else {
          System.out.println("WRITE CODE HERE, NEED TO FIND PLACE TO BUILD (BARRACKS)");
        }
      }
    }
    
    rc.yield();
  }
}