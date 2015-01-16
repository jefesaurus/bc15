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
  public static boolean wasBuildingSomething = false;
  public static RobotType wasBuilding = null;
  
  public Beaver(RobotController rc) throws GameActionException {
    super(rc);
  }
  
  public void setup() throws GameActionException {
  }

  public void execute() throws GameActionException {
    Messaging.announceUnit(rc.getType());
    int unitToProduce = Messaging.getUnitToProduce();
    
    if (!rc.isBuildingSomething() && wasBuildingSomething) {
      wasBuildingSomething = false;
      Messaging.announceUnit(wasBuilding);
      Messaging.announceDoneBuilding(wasBuilding);
    }
    
    //Build structures if queued
    for (int i = types.length; i-- > 0;) {
      RobotType curType = types[i];
      if (unitToProduce != -1 && unitToProduce != curType.ordinal()) {
        System.out.println("Locked out of producing " + curType);
        continue;
      }
      if (rc.isCoreReady() && rc.hasBuildRequirements(curType) && Messaging.dequeueUnit(curType)) {
          System.out.println("producing: " + curType);
          while (!rc.isCoreReady() && !rc.hasBuildRequirements(curType)) {
            rc.yield();
            Messaging.announceUnit(rc.getType());
          };
          Direction buildDir = getBuildDirection(curType);
          if (buildDir != null) {
            System.out.println("Building " + curType);
            rc.build(buildDir, curType);
            wasBuildingSomething = true;
            wasBuilding = curType;
            break;
          } else {
            System.out.println("WRITE CODE HERE, NEED TO FIND PLACE TO BUILD (BEAVER)");
          }
        }
      }
    
    
    //Otherwise mine where you are
    if (rc.isCoreReady()) {
      rc.mine();
    }
    rc.yield();
  }
  /*
  public MapLocation getBuildLocation() {
    MapLocation[] candidates = MapLocation.getAllMapLocationsWithinRadiusSq(this.myHQ, 15);
    int size = 1;
    while (true) {
      // Top side
      for (int i = -size; i++ < size;) {
        
      }
    }

    for (int x = width; x-- > 0)
    for (int i = candidates.length; i-- > 0;) {
      if () {
        
      }
    }
  }
  */
}
