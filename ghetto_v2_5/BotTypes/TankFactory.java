package ghetto_v2_5.BotTypes;

import ghetto_v2_5.Messaging;
import ghetto_v2_5.RobotPlayer.BaseBot;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class TankFactory extends BaseBot {
  public static final RobotType[] types = {RobotType.TANK};
  
  public TankFactory(RobotController rc) {
    super(rc);
  }

  public void execute() throws GameActionException {
    Messaging.announceUnit(rc.getType());
    int unitToProduce = Messaging.getUnitToProduce();
    if (unitToProduce != -1 || unitToProduce != RobotType.TANK.ordinal()) {
      return;
    }
    
    //Build units if queued
    for (int i=types.length; i-- > 0;) {
      RobotType curType = types[i];
      if (Messaging.dequeueUnit(curType)) {
        while (!rc.isCoreReady() && !rc.hasSpawnRequirements(curType)) {
          rc.yield();
          Messaging.announceUnit(rc.getType());
        };
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
          System.out.println("WRITE CODE HERE, NEED TO FIND PLACE TO BUILD (TANKFACTORY)");
        }
      }
    }
    
    rc.yield();
  }
}