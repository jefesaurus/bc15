package launcherbot.BotTypes;

import launcherbot.Messaging;
import launcherbot.MovingBot;
import launcherbot.Nav;
import launcherbot.Nav.Engage;
import launcherbot.RobotPlayer.BaseBot;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.TerrainTile;

public class Beaver extends MovingBot {
  public static final RobotType[] types = {RobotType.MINERFACTORY, RobotType.HELIPAD, RobotType.BARRACKS, RobotType.TANKFACTORY, RobotType.SUPPLYDEPOT, RobotType.AEROSPACELAB};
  public static boolean wasBuildingSomething = false;
  public static RobotType wasBuilding = null;
  
  public Beaver(RobotController rc) throws GameActionException {
    super(rc);
  }
  
  public enum BuildingStage {
    IDLE, MOVING_TO_LOCATION, BUILDING, WAITING_TO_FINISH_BUILDING
  };
  
  MapLocation targetBuildSpot;
  boolean isMovingToBuildSpot;
  RobotType robotToBuild;
  BuildingStage buildingStage;
  
  
  public void setup() throws GameActionException {
    isMovingToBuildSpot = false;
    buildingStage = BuildingStage.IDLE;
    super.setup();
  }

  public void execute() throws GameActionException {
    switch (buildingStage) {
    case IDLE:

      robotToBuild = dequeueBuildJob();
      //System.out.println("robot to build: " + robotToBuild);

      if (robotToBuild != null) {

        buildingStage = BuildingStage.MOVING_TO_LOCATION;
        //System.out.println("Moving to: " + targetBuildSpot);

        continueMoveToBuildLocation();
      } else if (rc.isCoreReady()) {
        //System.out.println("mining");

        rc.mine();
      }
      break;
    case MOVING_TO_LOCATION:
      //System.out.println("Moving to: " + targetBuildSpot);

      if (continueMoveToBuildLocation()) {
        buildingStage = BuildingStage.BUILDING;
      }
      break;
    case BUILDING:
      if (rc.isCoreReady() && rc.hasBuildRequirements(robotToBuild)) {
        if (rc.canBuild(curLoc.directionTo(targetBuildSpot), robotToBuild)) {
          rc.build(curLoc.directionTo(targetBuildSpot), robotToBuild);
          buildingStage = BuildingStage.WAITING_TO_FINISH_BUILDING;
        } else {
          targetBuildSpot = null;
          buildingStage = BuildingStage.MOVING_TO_LOCATION;
          continueMoveToBuildLocation();
        }
      }
      break;
    case WAITING_TO_FINISH_BUILDING:
      if (!rc.isBuildingSomething()) {
        buildingStage = BuildingStage.IDLE;
      } else {
        targetBuildSpot = getBuildLocation(myHQ);
      }
      break;
    }
  }
  
  // Returns true when it is ready to build (adjacent to target location)
  public boolean continueMoveToBuildLocation() throws GameActionException {
    if (targetBuildSpot == null) {
      targetBuildSpot = getBuildLocation(myHQ);
    } else if (curLoc.isAdjacentTo(targetBuildSpot)) {
      return true;
    } else {
      Nav.goTo(targetBuildSpot, Engage.NONE);
    }
    return false;
  }
  
  public RobotType dequeueBuildJob() throws GameActionException {
    int unitToProduce = Messaging.getUnitToProduce();
    //Build structures if queued
    for (int i = types.length; i-- > 0;) {
      RobotType curType = types[i];
      if (unitToProduce == -1 || unitToProduce == curType.ordinal()) {
        if (Messaging.dequeueUnit(curType)) {
          return curType;
        }
      }
    }
    return null;
  }
  

  // Get a location to build centered around the provided location, which should be something like and HQ or tower.
  public MapLocation getBuildLocation(MapLocation center) throws GameActionException {
    int size = 1;
    int lx, rx, ty, by;
    MapLocation current;
    
    MapLocation candidate = null;
    int numOccupied, numSquares;
    
    while (true) {
      lx = center.x - size;
      rx = center.x + size;
      ty = center.y - size;
      by = center.y + size;
      numOccupied = 0;
      numSquares = 0;

      // Top / bottom
      //for (int i = rx + 1; i-- > lx;) {
      for (int i = rx; i >= lx; i -= 2) {
        current = new MapLocation(i, ty);

        if (rc.senseTerrainTile(current) == TerrainTile.NORMAL && !rc.isLocationOccupied(current)) {
          if (candidate == null) {
            candidate = current;
          }
        } else {
          numOccupied++;
        }
        current = new MapLocation(i, by);

        if (rc.senseTerrainTile(current) == TerrainTile.NORMAL && !rc.isLocationOccupied(current)) {
          if (candidate == null) {
            candidate = current;
          }
        } else {
          numOccupied++;
        }
        numSquares += 2;
      }

   // Left and right
      //for (int i = by; i-- > ty + 1;) {
      for (int i = by - 2; i >= ty + 2; i -= 2) {
        current = new MapLocation(rx, i);

        if (rc.senseTerrainTile(current) == TerrainTile.NORMAL && !rc.isLocationOccupied(current)) {
          if (candidate == null) {
            candidate = current;
          }
        } else {
          numOccupied++;
        }
        current = new MapLocation(lx, i);

        if (rc.senseTerrainTile(current) == TerrainTile.NORMAL && !rc.isLocationOccupied(current)) {
          if (candidate == null) {
            candidate = current;
          }
        } else {
          numOccupied++;
        }
        numSquares += 2;
      }

      // System.out.println("Candidate: " + candidate + " occupancy: " + numOccupied + " num squares: " + numSquares);
      if (candidate != null && numOccupied/(double)numSquares <= .5) {
        return candidate;
      }
      candidate = null;
      size ++;
    }
  }
}
