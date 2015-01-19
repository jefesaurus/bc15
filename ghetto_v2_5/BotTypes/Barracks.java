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
          rc.spawn(spawnDir, curType);
        }
      }
    }    
  }
}