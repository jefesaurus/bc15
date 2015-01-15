package ghetto_v2_5.BotTypes;

import ghetto_v2_5.Messaging;
import ghetto_v2_5.Nav;
import ghetto_v2_5.RobotPlayer.BaseBot;
import ghetto_v2_5.RobotPlayer.MovingBot;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class Beaver extends MovingBot {
  public static final RobotType[] types = {RobotType.MINERFACTORY, RobotType.HELIPAD, RobotType.BARRACKS, RobotType.TANKFACTORY};
  public Beaver(RobotController rc) throws GameActionException {
    super(rc);
  }
  
  public void setup() throws GameActionException {
  }

  public void execute() throws GameActionException {
    int unitToProduce = Messaging.getUnitToProduce();
    
    //Build structures if queued
    for (int i=types.length; i-- > 0;) {
      RobotType curType = types[i];
      if (Messaging.dequeueUnit(curType)) {
        if (unitToProduce != -1 || unitToProduce != curType.ordinal()) {
          continue;
        }
        while (!rc.isCoreReady() && !rc.hasBuildRequirements(curType)) {rc.yield();};
        Direction buildDir = getBuildDirection(curType);
        if (buildDir != null) {
          rc.build(buildDir, curType);
        } else {
          System.out.println("WRITE CODE HERE, NEED TO FIND PLACE TO BUILD (BEAVER)");
        }
      }
    }
    
    //Otherwise mine where you are
    rc.mine();
    rc.yield();
  }
}
