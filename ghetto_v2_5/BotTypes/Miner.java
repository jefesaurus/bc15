package ghetto_v2_5.BotTypes;

import ghetto_v2_5.Messaging;
import ghetto_v2_5.Nav;
import ghetto_v2_5.Util;
import ghetto_v2_5.Nav.Engage;
import ghetto_v2_5.RobotPlayer.BaseBot;
import ghetto_v2_5.RobotPlayer.MovingBot;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.TerrainTile;

public class Miner extends MovingBot {
  public static final int MINING_HORIZON = 5;
  public static int MINING_TURNS = 0;
  
  public Miner(RobotController rc) {
    super(rc);
  }

  public void execute() throws GameActionException {
    mineMicro(this.curLoc);
    rc.yield();
  }
  
  
  private void mineMicro(MapLocation loc) throws GameActionException {
    if (rc.isCoreReady()) {
      double[] dangerVals = this.getAllDangerVals();
      // If the center square is in danger, retreat
      if (dangerVals[8] > 0) {
        Nav.retreat(dangerVals);
      } else {
        // mine
        mineMethod();
        
      }
    }
  }
  
  public void mineMethod() throws GameActionException {
    if (rc.isCoreReady()) {
      // check the number of turns left for this miner to mine on this space      
      if (MINING_TURNS > 0) {
          MINING_TURNS--;
          rc.mine();
      } else {
        // no more mining turns left for this location; determine next best course of action
        
        determineNewCourse();
        
      }
    }
  }
  
  public void determineNewCourse() throws GameActionException {
    double curAmount = getOreAmount(this.curLoc, MINING_HORIZON);
    double maxAmount = curAmount;
    MapLocation bestLoc = this.curLoc;
    int numMaxes = 1;

    // get all visible squares
    MapLocation[] locationsInVisionRange = MapLocation.getAllMapLocationsWithinRadiusSq(this.curLoc, 15);
    

    
    // calculate the ore in x- and y- directions, weighted by distance
    double xWeightedOreTotal = 0;
    double yWeightedOreTotal = 0; 

//    int bc = Clock.getBytecodeNum();

    for (int i = locationsInVisionRange.length; i-- > 0;) {
        
        int xDiff = (locationsInVisionRange[i].x - this.curLoc.x)+4;
        int yDiff = (locationsInVisionRange[i].y - this.curLoc.y)+4;  
        
        double xUnitCmpt = Util.VECTOR_MAGS[xDiff][yDiff][0];
        double yUnitCmpt = Util.VECTOR_MAGS[xDiff][yDiff][1];
        double oreAtLoc = rc.senseOre(locationsInVisionRange[i]);
        xWeightedOreTotal += xUnitCmpt*oreAtLoc;
        yWeightedOreTotal += yUnitCmpt*oreAtLoc; 
         
    }
//    int bc2 = Clock.getBytecodeNum();
//    System.out.println("bc num for calculation(): "+ (bc2-bc));

    // calculate the desired next square to move to
    double mag = Math.sqrt(xWeightedOreTotal*xWeightedOreTotal + yWeightedOreTotal*yWeightedOreTotal);
    MapLocation desiredLoc = new MapLocation( this.curLoc.x + (int) Math.round(xWeightedOreTotal/mag), 
                                            this.curLoc.y + (int) Math.round(yWeightedOreTotal/mag));    
    
    
    
    bestLoc = desiredLoc;
//    double adjAmount = getOreAmount(desiredLoc, MINING_HORIZON - this.curLoc.distanceSquaredTo(desiredLoc));
//    
//    if (maxAmount < adjAmount) {
//      maxAmount = adjAmount;
//      bestLoc = desiredLoc;
//      numMaxes = 1;
//    } else if (maxAmount == adjAmount) {
//      numMaxes += 1;
//      if (Math.random() > 1.0 / numMaxes) {
//        bestLoc = desiredLoc;
//      }
//    }
//    
//    if (maxAmount == curAmount) {
//      bestLoc = this.curLoc;
//    }
//  
      if (bestLoc == this.curLoc && rc.isCoreReady()) {
        this.MINING_TURNS = MINING_HORIZON;
        rc.mine();
      }
      
//      // if square is empty and we can move, then move
//      if (!rc.isLocationOccupied(bestLoc) && rc.isCoreReady()) {
//        this.MINING_TURNS = MINING_HORIZON;
//        rc.move(getMoveDir(bestLoc));
//      } 
      else if (!this.curLoc.equals(bestLoc)) { // otherwise need to move to the next best square
//        double[] dangerVals = this.getAllDangerVals();
        boolean[] canMove = new boolean[8];
        for (int i = 8; i-- > 0;) {
          if (rc.canMove(Util.REGULAR_DIRECTIONS[i])) {
//            if (dangerVals[i] == 0) {
//              if (rc.isCoreReady())
//                rc.move(Util.REGULAR_DIRECTIONS[i]);
//              return true;
//            }
            canMove[i] = true;
          } else {
            canMove[i] = false;
          }
        }
                
        this.MINING_TURNS = MINING_HORIZON;
        
        int bestDir = this.curLoc.directionTo(bestLoc).ordinal() + 8;
        
        int tempDir;
        for (int i = 0; i < 8; i++) {
          if (i%2 == 0) {
            tempDir = (bestDir - i)%8;
          } else {
            tempDir = (bestDir + i)%8;
          }
          
          if (canMove[tempDir]) {
            if (rc.isCoreReady())
              rc.move(Util.REGULAR_DIRECTIONS[tempDir]);
          }
        
      }
    }

    /////OLD////
    
//      double curAmount = getOreAmount(this.curLoc, MINING_HORIZON);
//      double maxAmount = curAmount;
//      MapLocation bestLoc = this.curLoc;
//      int numMaxes = 1;
//      Direction[] directions = Direction.values();
//      for (int i=0; i<8; i++) {
//        if (rc.canMove(directions[i])) {
//          MapLocation trialLoc = this.curLoc.add(directions[i]);
//          double adjAmount = getOreAmount(trialLoc, MINING_HORIZON - 1);
//          if (maxAmount < adjAmount) {
//            maxAmount = adjAmount;
//            bestLoc = trialLoc;
//            numMaxes = 1;
//          } else if (maxAmount == adjAmount) {
//            numMaxes += 1;
//            if (Math.random() > 1.0 / numMaxes) {
//              bestLoc = trialLoc;
//            }
//          }
//        }
//      }
//      
//      if (maxAmount == curAmount) {
//        bestLoc = this.curLoc;
//      }
//      
//      if (bestLoc == this.curLoc && rc.isCoreReady()) {
//        this.MINING_TURNS = MINING_HORIZON;
//        rc.mine();
//      }
//      
//      if (bestLoc != this.curLoc && rc.isCoreReady()) {
//        this.MINING_TURNS = MINING_HORIZON;
//        rc.move(getMoveDir(bestLoc));
//      }
      /////END OLD/////
  }

  
    
  public double getOreAmount(MapLocation loc, int horizon) {
    double startAmount = rc.senseOre(loc);
    double currentAmount = startAmount;
    double total = 0;
    for (int i=0; i<horizon; i++) {
      double amountMined = Math.max(
                     Math.min(GameConstants.MINER_MINE_MAX, currentAmount/GameConstants.MINER_MINE_RATE), 
                     GameConstants.MINIMUM_MINE_AMOUNT);
      currentAmount -= amountMined; 
      total += amountMined;
    }
    return total;
  }
}