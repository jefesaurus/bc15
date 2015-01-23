package rambo;

import rambo.MovingBot;
import rambo.Util;
import rambo.RobotPlayer.BaseBot;
import battlecode.common.*;
import battlecode.world.Robot;

public class Nav {
  public static MapLocation dest;
  private static RobotController rc;
  private static MovingBot br;

  private enum BugState {
    BUG, DIRECT
  }

  private enum WallSide {
    LEFT, RIGHT
  }

  private static BugState bugState;
  private static WallSide bugWallSide = WallSide.LEFT;
  private static int bugStartDistSq;
  private static Direction bugLastMoveDir;
  private static Direction bugLookStartDir;
  private static int bugRotationCount;
  private static int bugMovesSinceSeenObstacle = 0;
  
  public static void init(MovingBot theBr) {
    br = theBr;
    rc = theBr.rc;
  }

  private static boolean tryMoveDirect() throws GameActionException {
    
    Direction dir = br.curLoc.directionTo(dest);
    if (completeSafeMoveCheck(dir)) {
      rc.move(dir);
      return true;
    }
    
    Direction leftDir = dir.rotateLeft();
    Direction rightDir = dir.rotateRight();
    if (br.curLoc.add(leftDir).distanceSquaredTo(dest) < br.curLoc
        .add(rightDir).distanceSquaredTo(dest)) {
      if (completeSafeMoveCheck(leftDir)) {
        rc.move(leftDir);
        return true;
      } else if (completeSafeMoveCheck(rightDir)) {
        rc.move(rightDir);
        return true;
      }
    } else {
      if (completeSafeMoveCheck(rightDir)) {
        rc.move(rightDir);
        return true;
      } else if (completeSafeMoveCheck(leftDir)) {
        rc.move(leftDir);
        return true;
      }
    }
    return false;
  }

  private static void startBug() throws GameActionException {
    bugStartDistSq = rc.getLocation().distanceSquaredTo(dest);
    bugLastMoveDir = rc.getLocation().directionTo(dest);
    bugLookStartDir = rc.getLocation().directionTo(dest);
    bugRotationCount = 0;
    bugMovesSinceSeenObstacle = 0;

    // try to intelligently choose on which side we will keep the wall
    Direction leftTryDir = bugLastMoveDir.rotateLeft();
    for (int i = 0; i < 3; i++) {
      if (!completeSafeMoveCheck(leftTryDir)) {
        leftTryDir = leftTryDir.rotateLeft();
      } else {
        break;
      }
    }
    Direction rightTryDir = bugLastMoveDir.rotateRight();
    for (int i = 0; i < 3; i++) {
      if (!completeSafeMoveCheck(rightTryDir)) {
        rightTryDir = rightTryDir.rotateRight();
      } else {
        break;
      }
    }
    if (dest.distanceSquaredTo(rc.getLocation().add(leftTryDir)) < dest
        .distanceSquaredTo(rc.getLocation().add(rightTryDir))) {
      bugWallSide = WallSide.RIGHT;
    } else {
      bugWallSide = WallSide.LEFT;
    }
  }

  private static Direction findBugMoveDir() throws GameActionException {
    bugMovesSinceSeenObstacle++;
    Direction dir = bugLookStartDir;
    for (int i = 8; i-- > 0;) {
      if (completeSafeMoveCheck(dir)) {
        return dir;
      }
      dir = (bugWallSide == WallSide.LEFT ? dir.rotateRight() : dir
          .rotateLeft());
      bugMovesSinceSeenObstacle = 0;
    }
    return null;
  }

  private static int numRightRotations(Direction start, Direction end) {
    return (end.ordinal() - start.ordinal() + 8) % 8;
  }

  private static int numLeftRotations(Direction start, Direction end) {
    return (-end.ordinal() + start.ordinal() + 8) % 8;
  }

  private static int calculateBugRotation(Direction moveDir) {
    if (bugWallSide == WallSide.LEFT) {
      return numRightRotations(bugLookStartDir, moveDir)
          - numRightRotations(bugLookStartDir, bugLastMoveDir);
    } else {
      return numLeftRotations(bugLookStartDir, moveDir)
          - numLeftRotations(bugLookStartDir, bugLastMoveDir);
    }
  }

  private static void bugMove(Direction dir) throws GameActionException {
    rc.move(dir);
    bugRotationCount += calculateBugRotation(dir);
    bugLastMoveDir = dir;
    // if (bugWallSide == WallSide.LEFT) bugLookStartDir = dir.isDiagonal() ?
    // dir.rotateLeft().rotateLeft() : dir.rotateLeft();
    // else bugLookStartDir = dir.isDiagonal() ? dir.rotateRight().rotateRight()
    // : dir.rotateRight();
    if (bugWallSide == WallSide.LEFT)
      bugLookStartDir = dir.rotateLeft().rotateLeft();
    else
      bugLookStartDir = dir.rotateRight().rotateRight();
  }

  private static boolean detectBugIntoEdge() throws GameActionException {
    if (rc.senseTerrainTile(rc.getLocation().add(bugLastMoveDir)) != TerrainTile.OFF_MAP)
      return false;

    if (bugLastMoveDir.isDiagonal()) {
      if (bugWallSide == WallSide.LEFT) {
        return !rc.canMove(bugLastMoveDir.rotateLeft());
      } else {
        return !rc.canMove(bugLastMoveDir.rotateRight());
      }
    } else {
      return true;
    }
  }

  private static void reverseBugWallFollowDir() throws GameActionException {
    bugWallSide = (bugWallSide == WallSide.LEFT ? WallSide.RIGHT
        : WallSide.LEFT);
    startBug();
  }

  private static void bugTurn() throws GameActionException {
    if (detectBugIntoEdge()) {
      reverseBugWallFollowDir();
    }
    Direction dir = findBugMoveDir();
    if (dir != null) {
      bugMove(dir);
    }
  }

  private static boolean canEndBug() {
    if (bugMovesSinceSeenObstacle >= 4)
      return true;
    return (bugRotationCount <= 0 || bugRotationCount >= 8)
        && rc.getLocation().distanceSquaredTo(dest) <= bugStartDistSq;
  }

  private static void bugTo(MapLocation theDest) throws GameActionException {
    // Check if we can stop bugging at the *beginning* of the turn
    if (bugState == BugState.BUG) {
      if (canEndBug()) {
        // Debug.indicateAppend("nav", 1, "ending bug; ");
        bugState = BugState.DIRECT;
      }
    }

    // If DIRECT mode, try to go directly to target
    if (bugState == BugState.DIRECT) {

      if (!tryMoveDirect()) {
        // Debug.indicateAppend("nav", 1, "starting to bug; ");
        bugState = BugState.BUG;
        startBug();
      } else {
        // Debug.indicateAppend("nav", 1, "successful direct move; ");
      }
    }

    // If that failed, or if bugging, bug
    if (bugState == BugState.BUG) {
      // Debug.indicateAppend("nav", 1, "bugging; ");
      bugTurn();
    }
  }

  public enum Engage {
    HQ, ONE_TOWER, ALL_TOWERS, UNITS, NONE
  }
  
  public static Engage engage;

  public static void goTo(MapLocation destIn, Engage engageIn) throws GameActionException {
    engage = engageIn;

    fightDecisionIsCached = false;

    if (!destIn.equals(dest)) {
      dest = destIn;
      bugState = BugState.DIRECT;
      // Debug.indicateAppend("nav", 1, "new dest: resetting bug to direct; ");
    }

    if (br.curLoc.equals(destIn))
      return;

    if (!rc.isCoreReady()) {
      return;
    }    
    bugTo(dest);
  }

  
  private static boolean fightIsWinningDecision = false;
  private static boolean fightDecisionIsCached = false;
  private static int allyIncludeRadius = 29;
  private static int enemyIncludeRadius = 49;
  
  private static boolean moveIsTowerSafe(Direction dir, MapLocation ignoreTower) throws GameActionException {
    int[] numAttackingTowerDirs = br.calculateNumAttackingTowerDirs(ignoreTower);
    return numAttackingTowerDirs[dir.ordinal()] == 0;
  }

  private static boolean moveIsUnitSafe(Direction dir) throws GameActionException {
    int[] numAttackingEnemyDirs = br.calculateNumAttackingEnemyDirs();
    return numAttackingEnemyDirs[dir.ordinal()] <= numAttackingEnemyDirs[8];
  }
  
  private static boolean moveIsHQSafe(Direction dir) throws GameActionException {
    boolean[] attackingHQDirs = br.calculateAttackingHQDirs();
    return !attackingHQDirs[dir.ordinal()];
  }
  
  private static boolean completeSafeMoveCheck(Direction dir) throws GameActionException {
    if (rc.canMove(dir) && moveIsAllowedByEngagementRules(dir)) {
      if (br.roundChanged()) {
        return rc.canMove(dir);
      } else {
        return true;
      }
    } else {
      return false;
    }
  }
  
  private static boolean moveIsAllowedByEngagementRules(Direction dir) throws GameActionException {
    switch (engage) {
    case NONE:
      return moveIsTowerSafe(dir, null) && moveIsUnitSafe(dir) && moveIsHQSafe(dir);
    case UNITS:
      return moveIsTowerSafe(dir, null) && moveIsHQSafe(dir);
        /*
        if (fightDecisionIsCached) {
          return fightIsWinningDecision;
        }
        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(br.curLoc.add(dir), RobotType.TOWER.attackRadiusSquared, br.theirTeam); //Cache.getEngagementEnemies();
        if (nearbyEnemies.length == 0) {
          return true;
        }
        
        RobotInfo closestEngageable = null;
        double closestDist = Double.MAX_VALUE;
        double tempDist = 0;
        for (RobotInfo bot : nearbyEnemies) {
          switch (bot.type) {
          case HQ:
          case TOWER:
            if (engage != Engage.ONE_TOWER && engage != Engage.ALL_TOWERS && engage != Engage.HQ) {
              return false;
            }
          case BEAVER:
          case DRONE:
          case SOLDIER:
          case TANK:
          case COMMANDER:
          case MINER:
          case BASHER:
          case MISSILE:
            tempDist = br.curLoc.distanceSquaredTo(bot.location);
            if (tempDist < closestDist) {
              closestDist = tempDist;
              closestEngageable = bot;
            }
            break;
          default:
            break;
          }
        }
        
        if (closestEngageable == null) {
          return true;
        }
        
        double allyScore = Util.getDangerScore(rc.senseNearbyRobots(closestEngageable.location, allyIncludeRadius, br.myTeam));
        double enemyScore = Util.getDangerScore(rc.senseNearbyRobots(closestEngageable.location, enemyIncludeRadius, br.theirTeam));
        fightIsWinningDecision = (allyScore > enemyScore);
        fightDecisionIsCached = true;
 
        return fightIsWinningDecision;
               */
    case ONE_TOWER:
      // Filter out the tower we are diving.
      return moveIsTowerSafe(dir, dest) && moveIsHQSafe(dir);
    case ALL_TOWERS:
      return true;
    case HQ:
      return true;
    }
    return false;
  }
  
  public static boolean moveToSafetyIfPossible(double[] dangerVals) throws GameActionException {
    for (int i = 8; i-- > 0;) {
      if (dangerVals[i] == 0 && rc.canMove(Util.REGULAR_DIRECTIONS[i])) {
        rc.move(Util.REGULAR_DIRECTIONS[i]);
        return true;
      }
    }
    return false;
  }
  
  public static boolean retreat(int[] attackingEnemyDirs) throws GameActionException {
    if (!moveIsTowerSafe(Util.REGULAR_DIRECTIONS_WITH_NONE[8], null) || !moveIsHQSafe(Util.REGULAR_DIRECTIONS_WITH_NONE[8])) {
      Nav.goTo(br.myHQ, Engage.HQ);
      return true;
    } else {
      int bestDir;
      RobotInfo[] enemies = Cache.getEngagementEnemies();
      if (enemies.length > 0) {
        int centerX = 0;
        int centerY = 0;
        for (int i = enemies.length; i-- > 0;) {
          centerX += enemies[i].location.x;
          centerY += enemies[i].location.y;
        }
        MapLocation enemyCentroid = new MapLocation(centerX/enemies.length, centerY/enemies.length);
        bestDir = enemyCentroid.directionTo(br.curLoc).ordinal() + 8;
      } else {
        bestDir = br.curLoc.directionTo(br.myHQ).ordinal() + 8;
      }
      int fewestAttackers = attackingEnemyDirs[8];
      int chosenDir = -1;
      int tempDir;
      for (int i = 0; i < 8; i++) {
        if (i%2 == 0) {
          tempDir = (bestDir - i)%8;
        } else {
          tempDir = (bestDir + i)%8;
        }
        if (attackingEnemyDirs[i] < fewestAttackers &&
            rc.canMove(Util.REGULAR_DIRECTIONS[i]) &&
            moveIsHQSafe(Util.REGULAR_DIRECTIONS[tempDir]) &&
            moveIsTowerSafe(Util.REGULAR_DIRECTIONS[tempDir], null)) {
          chosenDir = i;
          fewestAttackers = attackingEnemyDirs[i];
        }
      }
      if (chosenDir > 0) {
        rc.move(Util.REGULAR_DIRECTIONS[chosenDir]);
        return true;
      } else {
        Nav.goTo(br.myHQ, Engage.NONE);
        return true;
      }
    }
  }
}