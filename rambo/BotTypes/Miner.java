package rambo.BotTypes;

import rambo.Cache;
import rambo.HibernateSystem;
import rambo.Messaging;
import rambo.MovingBot;
import rambo.Nav;
import rambo.SupplyDistribution;
import rambo.Util;
import rambo.Nav.Engage;
import rambo.RobotPlayer.BaseBot;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class Miner extends rambo.MovingBot {
  public static final int MINING_HORIZON = 5;
  public static final int SAFE_RADIUS = 24;
  public static int miningTurns = 0;
  public static boolean minedOnLastTurn = false;
  public static int MINER_ID = -1;
  public static boolean disperseMode = true;
  public static int disperseModeMiningTurns = 5;

  public static boolean mineWhileMoveMode = true;
  
  public static boolean requestedHighOreLoc = false;
  public static MapLocation destination = null;

  public static int allInPushRound;
    
  public Miner(RobotController rc) {
    super(rc);
    SupplyDistribution.init((BaseBot) this);
    SupplyDistribution.setBatteryMode();
  }
  
  public void setup() throws GameActionException {
    super.setup();
    if (MINER_ID == -1) {
      MINER_ID = Messaging.getNumMiners();
    }
    allInPushRound = rc.getRoundLimit() - 200;
  }
  
  public boolean friendInAttackRange(RobotInfo[] enemiesInSightRange) {
    if (enemiesInSightRange.length == 0) return false;
    RobotInfo[] friendsInSightRange = rc.senseNearbyRobots(this.curLoc, RobotType.MINER.sensorRadiusSquared, rc.getTeam());
    for (int i = 0; i < friendsInSightRange.length; i++) {
      if ((friendsInSightRange[i].type == RobotType.MINER) || (friendsInSightRange[i].type == RobotType.HQ) || 
          (friendsInSightRange[i].type == RobotType.TOWER)) {
        if (enemiesInSightRange[0].location.distanceSquaredTo(friendsInSightRange[i].location) <= friendsInSightRange[i].type.attackRadiusSquared) {
          return true;
        }
      }
    }
    return false;
  }
  
  public boolean inAttackRange(RobotInfo[] enemiesInSightRange) {
    if (enemiesInSightRange[0].location.distanceSquaredTo(this.curLoc) <= RobotType.MINER.attackRadiusSquared) {
      return true;
    } else {
      return false;
    }
  }
  
  public void selfPreservation() throws GameActionException {
    //if ( rc.readBroadcast(Messaging.MINER_ATTACK_MODE) == 10 ) {
    //  int x = rc.readBroadcast(Messaging.MINER_ATTACK_X);
    //  int y = rc.readBroadcast(Messaging.MINER_ATTACK_Y);
    //  MapLocation attackLoc = new MapLocation(x,y);
    //  assistAttack(attackLoc);
    //}
    
    int minerDangerRadius = RobotType.MINER.attackRadiusSquared + 3;  // Radius for which they'll detect an enemy as a threat
    RobotInfo[] enemiesinDangerRange = rc.senseNearbyRobots(this.curLoc, minerDangerRadius, rc.getTeam().opponent());   
    if (enemiesinDangerRange.length == 0) {
      return;
    } else if (enemiesinDangerRange.length > 0) {
      //System.out.println("Miner sees danger");
      defensiveAction(enemiesinDangerRange);
    } else {
      return;
    }
  }
  
  private void defensiveAction(RobotInfo[] enemiesInDangerRange) throws GameActionException {
    int range = 50; // default at 50 for miner
    switch (enemiesInDangerRange[0].type) {
    case DRONE:
      range = (int) (rc.getHealth() / RobotType.DRONE.attackPower * RobotType.DRONE.attackDelay / RobotType.MINER.movementDelay)^2 +1;
      moveTowardsFriend(range, enemiesInDangerRange);
      break;
    case SOLDIER:
      range = (int) (rc.getHealth() / RobotType.SOLDIER.attackPower * RobotType.SOLDIER.attackDelay / RobotType.SOLDIER.movementDelay)^2 +1;
      moveTowardsFriend(range, enemiesInDangerRange);
      break;
    case BASHER:
      range = (int) (rc.getHealth() / RobotType.SOLDIER.attackPower * RobotType.SOLDIER.attackDelay / RobotType.SOLDIER.movementDelay)^2 +1;
      moveTowardsFriend(range, enemiesInDangerRange);
      break;
    case MINER:
      range = (int) (rc.getHealth() / RobotType.MINER.attackPower * RobotType.MINER.attackDelay / RobotType.MINER.movementDelay)^2 +1;
      moveTowardsFriend(range, enemiesInDangerRange);
      break;
    case BEAVER: 
      moveTowardsFriend(range, enemiesInDangerRange);
      break;
    default:
      moveTowardsFriend(range, enemiesInDangerRange);
      break;
    }
  }
    
  private void moveTowardsFriend(int range, RobotInfo[] enemiesInDangerRange) throws GameActionException {
    // Find closest friend
    RobotInfo[] friendsInSightRange = rc.senseNearbyRobots(this.curLoc, range, rc.getTeam());
    RobotInfo closestFriend = null; 
    int closestDist = range + 10;
    for (int i = 0; i < friendsInSightRange.length; i++) {
      if ((friendsInSightRange[i].type == RobotType.MINER) || (friendsInSightRange[i].type == RobotType.HQ) || 
          (friendsInSightRange[i].type == RobotType.TOWER)) {
        int dist = this.curLoc.distanceSquaredTo(friendsInSightRange[i].location);
        if (dist < closestDist) {
          closestFriend = friendsInSightRange[i];
        }
      }
    }
    if (closestFriend == null) {
      int[] attackingEnemyDirs = this.calculateNumAttackingEnemyDirs();
      //System.out.println("no closest friend");
      if (rc.isCoreReady()) {
        Nav.retreat(attackingEnemyDirs);
      }
      return;
    }
    else if (closestFriend.location.distanceSquaredTo(this.curLoc) <= 8) {  // Decided he was close enough to another miner
       //System.out.println("Close friend found, and attacking");
        minerAttack(enemiesInDangerRange[0]);
    } else {
      //System.out.println("Close friend found, moving to friend");
      //Nav.goTo(closestFriend.location, Engage.NONE);
      minerNavSingleMove(curLoc.directionTo(closestFriend.location));
      return;
    }
  }
  
  private void minerAttack(RobotInfo enemyRobot) throws GameActionException {
    MapLocation attackLoc = enemyRobot.location;
    if (rc.isWeaponReady()) {
      //System.out.println("attack location: " + attackLoc.x + "," + attackLoc.y);
      if (rc.isCoreReady() && rc.canAttackLocation(attackLoc) ) {
        System.out.println("actually attacking");
        rc.attackLocation(attackLoc);
      } else if (rc.isCoreReady() && attackLoc != null ) {
        //System.out.println("Moving to attack");
        minerNavSingleMove(curLoc.directionTo(attackLoc));
      } else {
        //System.out.println("something isn't ready: " + rc.getCoreDelay() + " " + rc.isCoreReady());
      }
    }
  }
  
//  public void minerAttack(RobotInfo enemyInDangerRange) throws GameActionException {
//    // 1. store location
//    // 2. call surrounding miners to attack
//    // 3. attack (with good concave)
//    // 4. return to location
//
//    
//    rc.broadcast(Messaging.MINER_ATTACK_X, enemyInDangerRange.location.x);
//    rc.broadcast(Messaging.MINER_ATTACK_Y, enemyInDangerRange.location.x);
//    rc.broadcast(Messaging.MINER_ATTACK_MODE, 10);
//    System.out.println("Attack Mode on");
//    
//    assistAttack(enemyInDangerRange.location);
//    
//    //rc.broadcast(Messaging.MINER_ATTACK_MODE, 0);
//  }
//  
//  public void assistAttack(MapLocation attackLoc) throws GameActionException {
//    if (!attackMode) { 
//      miningLocation = rc.getLocation(); 
//      attackMode = true;
//    }
//    if (rc.isWeaponReady()) {
//      System.out.println("attack location: " + attackLoc.x + "," + attackLoc.y);
//      if (rc.isCoreReady() && rc.canAttackLocation(attackLoc) ) {
//        System.out.println("actually attacking");
//        rc.attackLocation(attackLoc);
//      } else if (rc.isCoreReady() && attackLoc != null ) {
//          //&& this.curLoc.distanceSquaredTo(attackLoc) < 1000 
//          //&& miningLocation.distanceSquaredTo(attackLoc) < 1000 ) {
//        System.out.println("Moving to attack");
//        Nav.goTo(attackLoc, Engage.UNITS);
//      } else {
//        System.out.println("something isn't ready: " + rc.getCoreDelay() + " " + rc.isCoreReady());
//      }
//    }
//  }
  

  public void doAllInPush() throws GameActionException {
    currentEnemies = Cache.getEngagementEnemies();
    
    MapLocation rallyPoint = Messaging.readRallyPoint();
    MovingBot.AttackMode mode = Messaging.getFleetMode();
    if (currentEnemies.length > 0) {
      RobotInfo[] attackableEnemies = Cache.getAttackableEnemies();
      if (attackableEnemies.length > 0) {
        if (rc.isWeaponReady()) {
          if (rc.canAttackLocation(rallyPoint)) {
            rc.attackLocation(rallyPoint);
          } else {
            attackLeastHealthEnemy(attackableEnemies);
          }
        }
      } else {
        if (rc.isCoreReady() && rallyPoint != null) {
          Nav.goTo(rallyPoint, Engage.ALL_TOWERS);
        }
      }
    } else if (rc.isCoreReady()) {
      if (rallyPoint != null) {
        Nav.goTo(rallyPoint, Engage.ALL_TOWERS);
      }
    }
  }
  
  public void execute() throws GameActionException {
    SupplyDistribution.manageSupply();
    selfPreservation();
    if (Clock.getRoundNum() > allInPushRound) {
      doAllInPush();
      return;
    }
    if (destination!=null) {
      if (this.curLoc.distanceSquaredTo(destination) < 5) {
        destination = null;
        if (rc.isCoreReady()) {
          rc.mine();
        }
      } else {
        minerNavSingleMove(curLoc.directionTo(destination));
      }
    }

    else if (disperseMode) {
      disperseMine();
    } else {
      ventureMineMethod();
    }
  }
  
  public void endOfTurn() {
  }

  private void disperseMine() throws GameActionException {
    this.broadcastHighestOreInSensorRange();
    if (rc.isCoreReady()) {
      if (disperseModeMiningTurns == 0) {
        disperseMode = false;
        return;
      }
      if (minedOnLastTurn) {
        int directionIndex = MINER_ID % 8;
        Direction dir = Util.REGULAR_DIRECTIONS[directionIndex];
        minerNavSingleMove(dir);
        minedOnLastTurn = false;
        return;
      } else {
        rc.mine();
        minedOnLastTurn = true;
        disperseModeMiningTurns-=1;
        return;
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
  
  public boolean currentLocGivesMinOre(double multiplier) {
    double unsuppliedCoeff = 1;
    if (rc.getSupplyLevel() < RobotType.MINER.supplyUpkeep) {
      unsuppliedCoeff = 0.5;
    }
    return (rc.senseOre(curLoc) / (GameConstants.MINER_MINE_RATE) * unsuppliedCoeff <= GameConstants.MINIMUM_MINE_AMOUNT*multiplier);
  }
  
  public void broadcastHighestOreInSensorRange() throws GameActionException {
    int radius = RobotType.MINER.sensorRadiusSquared;
    if (Clock.getBytecodesLeft() < 5000) {
      radius = radius/2;
    }
    
    MapLocation[] locationsInVisionRange = MapLocation.getAllMapLocationsWithinRadiusSq(this.curLoc, radius);
    int currentX = rc.readBroadcast(Messaging.HIGH_ORE_LOCS);
    int currentY = rc.readBroadcast(Messaging.HIGH_ORE_LOCS + 1);
    int x = currentX;
    int y = currentY;
    for (int i = locationsInVisionRange.length; i-- > 0;) {
      MapLocation loc = locationsInVisionRange[i];
      if (!rc.isLocationOccupied(loc) && rc.senseOre(loc) > rc.senseOre(new MapLocation(currentX, currentY))) {
        x = loc.x;
        y = loc.y;
      }
    }
    rc.broadcast(Messaging.HIGH_ORE_LOCS, x);
    rc.broadcast(Messaging.HIGH_ORE_LOCS + 1, y);
    rc.broadcast(Messaging.HIGH_ORE_REQUEST, 0);
    
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
  
  public void basicMineAlgorithm() throws GameActionException {
    if (rc.readBroadcast(Messaging.HIGH_ORE_REQUEST) == 1) {
      broadcastHighestOreInSensorRange();
    }
    if (currentLocGivesMaxOre()) {
      if (rc.isCoreReady()) {
        rc.mine();
      }
      return;
    } else if (currentLocGivesMinOre(1)) {
      if (rc.isCoreReady()) {
        if (MINER_ID < 8) { // TODO, maybe by round number?
          minerNavSingleMove(Util.REGULAR_DIRECTIONS[MINER_ID % 7]);
        } else if (!requestedHighOreLoc) { // request other miners
          rc.broadcast(Messaging.HIGH_ORE_REQUEST, 1);
          requestedHighOreLoc = true;
//          if (towersSeeMoreOreThanMe()) { // read tower board
//            minerNavSingleMove(this.curLoc.directionTo(destination));
//          } else {
//            
//          }
        } else if (requestedHighOreLoc) { // read what other miners said
          if (towersSeeMoreOreThanMe()) { // read tower board
            if (Clock.getBytecodesLeft() > 1500) {
              minerNavSingleMove(this.curLoc.directionTo(destination));
            }

          } else {
            int x = rc.readBroadcast(Messaging.HIGH_ORE_LOCS);
            int y = rc.readBroadcast(Messaging.HIGH_ORE_LOCS + 1);
            
            MapLocation newLoc = new MapLocation(x,y);
            
            if (rc.senseOre(newLoc) < 1) {
              disperseMode = true;
            }
            if (Clock.getBytecodesLeft() > 1500){
              Nav.goTo(newLoc, Engage.NONE);

            }
          }
          
          
          
          
          requestedHighOreLoc = false;
          rc.broadcast(Messaging.HIGH_ORE_REQUEST, 0);
          return;
        }
      }
      return;
    } else {
      
      if (towersSeeMoreOreThanMe()) { // read tower board
        int start = Clock.getBytecodesLeft();

        if (start > 1500)

        minerNavSingleMove(this.curLoc.directionTo(destination));
      } 
      
      double[] dangerVals = this.getAllDangerVals();
      double curAmount = getOreAmount(this.curLoc, MINING_HORIZON);
      double maxAmount = curAmount;
      MapLocation bestLoc = this.curLoc;
      int numMaxes = 1;
      Direction[] directions = Direction.values();
      for (int i=0; i<8; i++) {
        if (rc.canMove(directions[i]) && dangerVals[directions[i].ordinal()] < 0.01) { // this part is for safe zone mining only
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
        Miner.miningTurns = MINING_HORIZON;
        rc.mine();
      }
      
      if (bestLoc != this.curLoc && rc.isCoreReady()) {
        Miner.miningTurns = MINING_HORIZON;
        minerNavSingleMove(getMoveDir(bestLoc));
      }
    }
  }
  
//  private void mineToHighOreTower() throws GameActionException {
//    if (rc.isCoreReady()) {
//      if (highTowerOreDestination!=null) {
//        Nav.goTo(highTowerOreDestination, Engage.NONE);
//      }
//      
//    }
//  }

  private boolean towersSeeMoreOreThanMe() throws GameActionException {
    if (rc.readBroadcast(Messaging.UNCLAIMED_HIGH_ORE_TOWERS_COUNT) == 0) return false;
    
    int averageOreISee = (int) getMaxOreNearMe(); // round to int since tower ore values are rounded to ints
    
    double maxAmt = 0;
    int towerIndex = 0;
    int numHighOreTowers = rc.readBroadcast(Messaging.HIGH_ORE_TOWERS_COUNT);
    for (int i=0; i < numHighOreTowers; i++) {
      int ore = rc.readBroadcast(Messaging.HIGH_ORE_TOWERS_LOCS + 3*i + 2);
      if (ore != 0 && ore > maxAmt) {
        maxAmt = ore;
        towerIndex = i;
      }
    }
    
    if (averageOreISee < maxAmt) {
      int x = rc.readBroadcast(Messaging.HIGH_ORE_TOWERS_LOCS + 3*towerIndex);
      int y = rc.readBroadcast(Messaging.HIGH_ORE_TOWERS_LOCS + 3*towerIndex + 1);
      destination = new MapLocation(x, y);
      
      rc.broadcast(Messaging.HIGH_ORE_TOWERS_LOCS + 3*towerIndex + 2, 0);
      int unclaimedTowers = rc.readBroadcast(Messaging.UNCLAIMED_HIGH_ORE_TOWERS_COUNT);
      rc.broadcast(Messaging.UNCLAIMED_HIGH_ORE_TOWERS_COUNT, unclaimedTowers-1);

      return true;
    }
    
    return false;
  }
  
  private double getMaxOreNearMe() throws GameActionException {
    Direction[] dirs = Util.REGULAR_DIRECTIONS;
    double max = rc.senseOre(this.curLoc);    
    for (Direction dir : dirs) {
      MapLocation testLoc = this.curLoc.add(dir);
      if (!rc.isLocationOccupied(testLoc)) {
        double test = rc.senseOre(testLoc);
        if (test > max) {
          max = test;
        }
      }
    }
    return max;
  }
  
  
  private double getAverageOreNearMe() throws GameActionException {
    Direction[] dirs = Util.REGULAR_DIRECTIONS;
    double cum = 0;
    int totalLocs = 0;
    for (Direction dir : dirs) {
      MapLocation testLoc = this.curLoc.add(dir);
      if (!rc.isLocationOccupied(testLoc)) {
        cum = cum + rc.senseOre(testLoc);
        totalLocs = totalLocs + 1;
      }
    }
    return cum/totalLocs;
  }

  private void minerNavSingleMove(Direction dir) throws GameActionException {
    int bestDir = dir.ordinal() + 8;

    boolean[] canMove = new boolean[8];
    //check that squares aren't occupied
    for (int i = 8; i-- > 0;) {
      if (!rc.isLocationOccupied(curLoc.add(Util.REGULAR_DIRECTIONS[i])) && rc.canMove(Util.REGULAR_DIRECTIONS[i])) {
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
      
      if (canMove[tempDir] && dangerVals[tempDir] < 0.01) {
        if (rc.isCoreReady()) {
          rc.move(Util.REGULAR_DIRECTIONS[tempDir]);
          return;
        }
      }
    }
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