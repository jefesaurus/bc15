package ghetto_v2_5.BotTypes;

import ghetto_v2_5.Messaging;
import ghetto_v2_5.Nav;
import ghetto_v2_5.Nav.Engage;
import ghetto_v2_5.RobotPlayer.BaseBot;
import ghetto_v2_5.RobotPlayer.MovingBot;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.TerrainTile;

public class Beaver extends MovingBot {
  public static final RobotType[] types = {RobotType.MINERFACTORY, RobotType.HELIPAD, RobotType.BARRACKS, RobotType.TANKFACTORY};
  public Beaver(RobotController rc) throws GameActionException {
    super(rc);
  }
  
  public enum BuildingStage {
    IDLE, MOVING_TO_LOCATION, BUILDING
  };
  
  MapLocation targetBuildSpot;
  boolean isMovingToBuildSpot;
  RobotType robotToBuild;
  BuildingStage buildingStage;
  
  
  public void setup() throws GameActionException {
    isMovingToBuildSpot = false;
    buildingStage = BuildingStage.IDLE;
  }

  public void execute() throws GameActionException {
    if (!rc.isBuildingSomething()) {
      switch (buildingStage) {
      case IDLE:
        System.out.println("Idle");

        robotToBuild = dequeueBuildJob();
        System.out.println("robot to build: " + robotToBuild);

        if (robotToBuild != null) {

          buildingStage = BuildingStage.MOVING_TO_LOCATION;
          System.out.println("Moving to: " + targetBuildSpot);

          continueMoveToBuildLocation();
        } else if (rc.isCoreReady()) {
          System.out.println("mining");

          rc.mine();
        }
        break;
      case MOVING_TO_LOCATION:
        System.out.println("Moving to: " + targetBuildSpot);

        if (continueMoveToBuildLocation()) {
          buildingStage = BuildingStage.BUILDING;
        }
        break;
      case BUILDING:
        System.out.println("build");
        if (rc.isCoreReady() && rc.hasBuildRequirements(robotToBuild)) {
          rc.build(curLoc.directionTo(targetBuildSpot), robotToBuild);
          buildingStage = BuildingStage.IDLE;
        }
        break;
      }
    } else {
      targetBuildSpot = getBuildLocation();
    }

    rc.yield();
  }
  
  // Returns true when it is ready to build (adjacent to target location)
  public boolean continueMoveToBuildLocation() throws GameActionException {
    if (targetBuildSpot == null) {
      targetBuildSpot = getBuildLocation();
    } else if (curLoc.isAdjacentTo(targetBuildSpot)) {
      return true;
    } else {
      System.out.println("Actually moving");
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
          /*
            System.out.println("Building " + curType);
            Messaging.announceBuilding(rc.getType());
            rc.build(buildDir, curType);
            Messaging.announceDoneBuilding(rc.getType());
            Messaging.announceDoneBuilding(curType);
            Messaging.announceUnit(rc.getType());
            //Have to announce it for that unit because of spawn sickness
            Messaging.announceUnit(curType);
            */
  }
  

  public MapLocation getBuildLocation() throws GameActionException {
    int size = 1;
    int lx, rx, ty, by;
    MapLocation current;
    
    MapLocation candidate = null;
    int numOccupied, numSquares;
    
    while (true) {
      lx = myHQ.x - size;
      rx = myHQ.x + size;
      ty = myHQ.y - size;
      by = myHQ.y + size;
      numOccupied = 0;
      numSquares = 0;

      for (int i = rx + 1; i-- > lx;) {
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

      // Right side
      for (int i = by; i-- > ty + 1;) {
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
