package terranbot.BotTypes;

import terranbot.Cache;
import terranbot.Messaging;
import terranbot.Nav;
import terranbot.Util;
import terranbot.Nav.Engage;
import terranbot.RobotPlayer.BaseBot;
import terranbot.MovingBot;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.TerrainTile;
  /*
   * TODO: 
   * 3. Mid game - supplying, moving from initial safe locations
   * 2. Messaging - report unsafe locations, account for gaps in messaging array killed and does not exist (initialization also) / areas of high ore
   * 1. Choose initial safe locations more intelligently
   * 4. Initial supply
   * KN 3. Venture miners / finding high ore areas (scouting)
   * -- 5. Protection? 
   */
public class Miner extends terranbot.MovingBot {
  public static final int MINING_HORIZON = 5;
  public static final int SAFE_RADIUS = 24;
  public static int miningTurns = 0;
  public static boolean minedOnLastTurn = false;
  public static int MINER_ID = -1;

  public static boolean mineWhileMoveMode = true;
  public static MapLocation safeZoneCenter = null;
  
  public Miner(RobotController rc) {
    super(rc);
  }
  
  public void setup() throws GameActionException {
    int chan = Messaging.getCountChannel(RobotType.MINER);
	MINER_ID = rc.readBroadcast(chan);
  }

  public void execute() throws GameActionException {
    //mineMicro(this.curLoc);
    if (Clock.getRoundNum() < 1000) {
      mineSafely();
    } else {
      safeZoneCenter = null;
      ventureMineMethod();
    }
    rc.yield();
  }
  
  public void endOfTurn() {
	  
  }
  
  private void mineSafely() throws GameActionException {
    safeZoneCenter = pickSafeZone(); // TODO choose more intelligently
    int dist = SAFE_RADIUS;
    if (rc.isCoreReady()) {
      if (inSafeArea(safeZoneCenter, dist)) {
        mineArea(safeZoneCenter, dist);
      } else {
        moveToSafeArea(safeZoneCenter, dist);
      }
    }
  }
  
  private MapLocation getAnyTower() {
    MapLocation[] towers = this.myTowers;
    int index = (int) Math.random()*towers.length;
    return towers[5];
  }
  
  private MapLocation pickSafeZone() throws GameActionException {
    int index = (MINER_ID % (this.myTowers.length + 1)); // towers + HQ
    int x = rc.readBroadcast(Messaging.SAFE_ZONES + 2*index);
    int y = rc.readBroadcast(Messaging.SAFE_ZONES + 2*index + 1);
    
    if ((Integer) x==null || (Integer) y==null)    System.out.println("Coordinate was null");
    
    MapLocation mineDest = new MapLocation(x, y);
    return mineDest;
  }

  private boolean inSafeArea(MapLocation safeLoc, int dist) {
    return (this.curLoc.distanceSquaredTo(safeLoc) <= dist);
  }

/**
 * Move to the safe area
 * @param loc center of the safe area
 * @param dist around the center
 * @throws GameActionException 
 */
  private void moveToSafeArea(MapLocation loc, int dist) throws GameActionException {
    rc.setIndicatorString(0, "center of safe area: " + loc);
    if (rc.isCoreReady()) {
      if (mineWhileMoveMode) { // mine while move, alternately TODO: more intelligently TODO: buggy
        if (minedOnLastTurn) {
          rc.setIndicatorString(1, "desired dir: " + getMoveDir(loc));
          // TODO this throws an error
          Nav.goTo(loc, Engage.NONE);
//          minerNavSingleMove(this.curLoc.directionTo(loc));
//          rc.move(getMoveDir(loc));
          minedOnLastTurn = false;
        } else {
          rc.mine();
          minedOnLastTurn = true;
        }
      } else {
        Nav.goTo(loc, Engage.NONE);
      }
//      rc.move(getMoveDir(loc));
    }
  }
  
/**
 * Continue to mine if already in a safe area
 * @param center of the mining area
 * @param dist from the center
 * @throws GameActionException 
 */
  private void mineArea(MapLocation center, int radius) throws GameActionException {
    Direction[] directions = Direction.values();
    for (int i=0; i<8; i++) {
      if (rc.canMove(directions[i])) {
        MapLocation trialLoc = this.curLoc.add(directions[i]);
        if (trialLoc.distanceSquaredTo(center) <= radius) {
          if (rc.isCoreReady()) {
            // TODO mining algorithm
            mineMethodSafe();
//            rc.move(directions[i]);
            continue;
          }
        }
      }
    }
    
  }
  
  private void ventureMineMethod() throws GameActionException {
    if (rc.isCoreReady()) {
      int[] attackingEnemyDirs = this.calculateNumAttackingEnemyDirs();
      // If the center square is in danger, retreat
      if (attackingEnemyDirs[8] > 0) {
        Nav.retreat(attackingEnemyDirs);
      } else {
        // mine
        mineMethod();
      }
    }
  }
  
  public void mineMethod() throws GameActionException {
    if (rc.isCoreReady()) {
//      // check the number of turns left for this miner to mine on this space      
//      if (miningTurns > 0) {
//          miningTurns--;
//          rc.mine();
//      } else {
//        // no more mining turns left for this location; determine next best course of action
//        
        basicMineAlgorithm();
//        
//      }
    }
  }
  
  public void mineMethodSafe() throws GameActionException {
    if (rc.isCoreReady()) {
      // check the number of turns left for this miner to mine on this space      
      if (miningTurns > 0) {
          miningTurns--;
          rc.mine();
      } else {
        // no more mining turns left for this location; determine next best course of action
        
        basicMineAlgorithm();
        
      }
    }
  }
  
  public boolean currentLocGivesMaxOre() {
    double unsuppliedCoeff = 1;
    if (rc.getSupplyLevel() < RobotType.MINER.supplyUpkeep) {
      unsuppliedCoeff = 0.5;
    }
    return (rc.senseOre(curLoc) / (GameConstants.MINER_MINE_RATE) * unsuppliedCoeff > GameConstants.MINER_MINE_MAX);
  }
  
  public MapLocation calculateNextLocationUsingGradient(int radius) {
    MapLocation[] locationsInVisionRange = MapLocation.getAllMapLocationsWithinRadiusSq(this.curLoc, radius);

    // calculate the ore in x- and y- directions, weighted by distance
    double xWeightedOreTotal = 0;
    double yWeightedOreTotal = 0; 

    for (int i = locationsInVisionRange.length; i-- > 0;) {
        int xDiff = (locationsInVisionRange[i].x - this.curLoc.x)+4;
        int yDiff = (locationsInVisionRange[i].y - this.curLoc.y)+4;  
        double xUnitCmpt = Util.VECTOR_MAGS[xDiff][yDiff][0];
        double yUnitCmpt = Util.VECTOR_MAGS[xDiff][yDiff][1];
        double oreAtLoc = rc.senseOre(locationsInVisionRange[i]);
        xWeightedOreTotal += xUnitCmpt*oreAtLoc;
        yWeightedOreTotal += yUnitCmpt*oreAtLoc; 
    }

    // calculate the desired next square to move to
    double mag = Math.sqrt(xWeightedOreTotal*xWeightedOreTotal + yWeightedOreTotal*yWeightedOreTotal);
    MapLocation desiredLoc = new MapLocation( this.curLoc.x + (int) Math.round(xWeightedOreTotal/mag), 
                                            this.curLoc.y + (int) Math.round(yWeightedOreTotal/mag)); 
    return desiredLoc;
  }
  
  public void determineNewCourse() throws GameActionException {
    if (currentLocGivesMaxOre()) {
      if (rc.isCoreReady()) {
        rc.mine();
      }
      return;
    } else {
      // get all visible squares within range of 15 units squared TODO possibly do every other square
      MapLocation desiredLoc = calculateNextLocationUsingGradient(15);   
      
      if (desiredLoc.equals(this.curLoc) && rc.isCoreReady()) {
        rc.mine();
      }      

      else { // otherwise need to move to the next best square
        Direction dir = this.curLoc.directionTo(desiredLoc);
        minerNavSingleMove(dir);
      }
    }  
  }
  
  // NOTE: Only used for mining in the safe zone
  public void basicMineAlgorithm() throws GameActionException {
    if (currentLocGivesMaxOre()) {
      if (rc.isCoreReady()) {
        rc.mine();
      }
      return;
    } else {
      double[] dangerVals = this.getAllDangerVals();
      double curAmount = getOreAmount(this.curLoc, MINING_HORIZON);
      double maxAmount = curAmount;
      MapLocation bestLoc = this.curLoc;
      int numMaxes = 1;
      Direction[] directions = Direction.values();
      for (int i=0; i<8; i++) {
        if (rc.canMove(directions[i]) && dangerVals[directions[i].ordinal()] < 0.01 &&
            ((safeZoneCenter != null && inSafeArea(safeZoneCenter, SAFE_RADIUS) && // this part is for safe zone mining only
            this.curLoc.add(directions[i]).distanceSquaredTo(safeZoneCenter) <= SAFE_RADIUS) ||
            safeZoneCenter==null)) { // this part is for safe zone mining only
          MapLocation trialLoc = this.curLoc.add(directions[i]);
          
          
          
          double adjAmount = getOreAmount(trialLoc, MINING_HORIZON - 1);
          if (maxAmount < adjAmount) {
            maxAmount = adjAmount;
            bestLoc = trialLoc;
            numMaxes = 1;
          } else if (maxAmount == adjAmount) {
            numMaxes += 1;
            if (Math.random() > 1.0 / numMaxes) {
              bestLoc = trialLoc;
            }
          }
        }
      }
      
      if (maxAmount == curAmount) {
        bestLoc = this.curLoc;
      }
      
      if (bestLoc == this.curLoc && rc.isCoreReady()) {
        this.miningTurns = MINING_HORIZON;
        rc.mine();
      }
      
      if (bestLoc != this.curLoc && rc.isCoreReady()) {
        this.miningTurns = MINING_HORIZON;
        rc.move(getMoveDir(bestLoc));
      }
    }
  }
  
  private void minerNavSingleMove(Direction dir) throws GameActionException {
    int bestDir = dir.ordinal() + 8;

    boolean[] canMove = new boolean[8];
    //check that squares aren't occupied
    for (int i = 8; i-- > 0;) {
      if (rc.canMove(Util.REGULAR_DIRECTIONS[i])) {
        canMove[i] = true;
      } else {
        canMove[i] = false;
      }
    }
                
    int tempDir;
    for (int i = 0; i < 8; i++) {
      if (i%2 == 0) {
        tempDir = (bestDir - i)%8;
      } else {
        tempDir = (bestDir + i)%8;
      }
      
      double[] dangerVals = this.getAllDangerVals();
      
      // in safe zone, and want to stay in it
      if (safeZoneCenter != null && inSafeArea(safeZoneCenter, SAFE_RADIUS)) {
        if (canMove[tempDir] && dangerVals[tempDir] < 0.01 && 
            this.curLoc.add(Util.REGULAR_DIRECTIONS[tempDir]).distanceSquaredTo(safeZoneCenter) <= SAFE_RADIUS) {
          if (rc.isCoreReady())
            rc.move(Util.REGULAR_DIRECTIONS[tempDir]);
        }
      } else { // not in safe zone yet, but do have a safe zone center, or no safe zone center at all
        if (canMove[tempDir] && dangerVals[tempDir] < 0.01) {
          if (rc.isCoreReady())
            rc.move(Util.REGULAR_DIRECTIONS[tempDir]);
        }
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