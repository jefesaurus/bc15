package rambo.BotTypes;

import rambo.Messaging;
import rambo.RobotPlayer.BaseBot;
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
    int unitToProduce = Messaging.getUnitToProduce();
    if (unitToProduce != -1 && unitToProduce != RobotType.TANK.ordinal()) {
      return;
    }
    //Build units if queued
    for (int i=types.length; i-- > 0;) {
      RobotType curType = types[i];
      if (rc.isCoreReady() && rc.hasSpawnRequirements(curType) && Messaging.dequeueUnit(curType)) {
        Direction spawnDir = getDefensiveSpawnDirection(curType);
        if (spawnDir != null) {
          rc.spawn(spawnDir, curType);
          Messaging.incrementUnitsBuilt(curType);
        }
      }
    }
  }
}