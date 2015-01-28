package launcherrambobot.BotTypes;

import launcherrambobot.Messaging;
import launcherrambobot.RobotPlayer.BaseBot;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class Barracks extends BaseBot {
  public static final int cutoff = 40;
  public static int lastProduced = 0;
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
      if (Clock.getRoundNum() - lastProduced <= cutoff) {
        return;
      }
      if (rc.isCoreReady() && rc.hasSpawnRequirements(curType) && Messaging.dequeueUnit(curType)) {
        lastProduced = Clock.getRoundNum();
        Direction spawnDir = getDefensiveSpawnDirection(curType);
        if (spawnDir != null) {
          rc.spawn(spawnDir, curType);
          Messaging.incrementUnitsBuilt(curType);
        }
      }
    }    
  }
}