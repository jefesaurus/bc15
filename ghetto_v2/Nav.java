package ghetto_v2;

import ghetto_v2.Util;
import ghetto_v2.RobotPlayer.BaseBot;
import ghetto_v2.RobotPlayer.MovingBot;
import battlecode.common.*;
import battlecode.world.Robot;

public class Nav {
  private static MapLocation dest;
  private static MovingBot br;
  private static RobotController rc;

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
    if (rc.canMove(dir) && moveIsAllowedByEngagementRules(dir)) {
      rc.move(dir);
      return true;
    }
    
    Direction leftDir = dir.rotateLeft();
    Direction rightDir = dir.rotateRight();
    if (br.curLoc.add(leftDir).distanceSquaredTo(dest) < br.curLoc
        .add(rightDir).distanceSquaredTo(dest)) {
      if (rc.canMove(leftDir) && moveIsAllowedByEngagementRules(leftDir)) {
        rc.move(leftDir);
        return true;
      } else if (rc.canMove(rightDir)
          && moveIsAllowedByEngagementRules(rightDir)) {
        rc.move(rightDir);
        return true;
      }
    } else {
      if (rc.canMove(rightDir) && moveIsAllowedByEngagementRules(rightDir)) {
        rc.move(rightDir);
        return true;
      } else if (rc.canMove(leftDir)
          && moveIsAllowedByEngagementRules(leftDir)) {
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
      if (!rc.canMove(leftTryDir)
          || !moveIsAllowedByEngagementRules(leftTryDir))
        leftTryDir = leftTryDir.rotateLeft();
      else
        break;
    }
    Direction rightTryDir = bugLastMoveDir.rotateRight();
    for (int i = 0; i < 3; i++) {
      if (!rc.canMove(rightTryDir)
          || !moveIsAllowedByEngagementRules(rightTryDir))
        rightTryDir = rightTryDir.rotateRight();
      else
        break;
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
      if (rc.canMove(dir) && moveIsAllowedByEngagementRules(dir))
        return dir;
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
    HQ, TOWERS, UNITS, NONE
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
  
  private static boolean moveIsTowerSafe(Direction dir) throws GameActionException {
    int[] numAttackingTowerDirs = br.calculateNumAttackingTowerDirs();
    return numAttackingTowerDirs[dir.ordinal()] == 0;
  }

  private static boolean moveIsUnitSafe(Direction dir) throws GameActionException {
    int[] numAttackingEnemyDirs = br.calculateNumAttackingEnemyDirs();
    return numAttackingEnemyDirs[dir.ordinal()] == 0;
  }
  
  private static boolean moveIsHQSafe(Direction dir) throws GameActionException {
    return br.curLoc.add(dir).distanceSquaredTo(br.enemyHQ) > 24;
  }
  
  private static boolean prevMoveCheckStillValid(Direction dir) {
    if (br.roundChanged()) {
      return rc.canMove(dir);
    }
    return true;
  }
  
  private static boolean moveIsAllowedByEngagementRules(Direction dir) throws GameActionException {
    if (!prevMoveCheckStillValid(dir)) {
      return false;
    }
    switch (engage) {
    case NONE:
      return moveIsTowerSafe(dir) && moveIsUnitSafe(dir) && moveIsHQSafe(dir) && prevMoveCheckStillValid(dir);
    case UNITS:
      if (moveIsTowerSafe(dir) && moveIsHQSafe(dir)) {
        if (fightDecisionIsCached) {
          return fightIsWinningDecision && (br.roundChanged() && prevMoveCheckStillValid(dir));
        }
        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(br.curLoc.add(dir), RobotType.TOWER.attackRadiusSquared, br.theirTeam); //Cache.getEngagementEnemies();
        if (nearbyEnemies.length == 0) {
          return prevMoveCheckStillValid(dir);
        }
        
        RobotInfo closestEngageable = null;
        double closestDist = Double.MAX_VALUE;
        double tempDist = 0;
        for (RobotInfo bot : nearbyEnemies) {
          switch (bot.type) {
          case HQ:
          case TOWER:
            if (engage != Engage.TOWERS && engage != Engage.HQ) {
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
          return prevMoveCheckStillValid(dir);
        }
        
        double allyScore = Util.getDangerScore(rc.senseNearbyRobots(closestEngageable.location, allyIncludeRadius, br.myTeam));
        double enemyScore = Util.getDangerScore(rc.senseNearbyRobots(closestEngageable.location, enemyIncludeRadius, br.theirTeam));
        rc.setIndicatorString(1, Double.toString(enemyScore));
        rc.setIndicatorString(0, Double.toString(allyScore));
        fightIsWinningDecision = (allyScore > enemyScore);
        fightDecisionIsCached = true;
        
        return fightIsWinningDecision && prevMoveCheckStillValid(dir);
      }
      return false;
    case TOWERS:
      return prevMoveCheckStillValid(dir);
    case HQ:
      return prevMoveCheckStillValid(dir);
    }
    return false;
  }
}