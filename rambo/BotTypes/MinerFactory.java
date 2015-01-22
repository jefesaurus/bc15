package rambo.BotTypes;

import rambo.Messaging;
import rambo.RobotPlayer.BaseBot;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class MinerFactory extends BaseBot {
  public static int targetNumMiners;
  public static boolean initializeZones = true;
  public static RobotType[] types = {RobotType.MINER};
  public static int activeSafeZones = 0;
  
  public MinerFactory(RobotController rc) {
    super(rc);
  }

  public void setup() throws GameActionException {
    initializeSafeZones();
  }
  
  public void initializeSafeZones() throws GameActionException {
    if (initializeZones) {
      Messaging.initializeSafeZones();
      initializeZones = false;
    }
  }
  
  public void execute() throws GameActionException {
    int unitToProduce = Messaging.getUnitToProduce();
    if (unitToProduce != -1 && unitToProduce != RobotType.MINER.ordinal()) {
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
