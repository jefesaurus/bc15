package ghettoblaster;

import ghettoblaster.RobotPlayer.BaseBot;
import battlecode.common.*;
import battlecode.world.Robot;

public class Nav {
  private static MapLocation dest;
  private static BaseBot br;
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

  public static void init(BaseBot theBr) {
    br = theBr;
    rc = theBr.rc;
    bugState = BugState.DIRECT;
  }

  private static boolean tryMoveDirect() throws GameActionException {
    Direction dir = br.curLoc.directionTo(dest);
    if (canMoveSafely(dir) && moveIsAllowedByEngagementRules(dir)) {
      rc.move(dir);
      System.out.println(dir);
      return true;
    }
    Direction leftDir = dir.rotateLeft();
    Direction rightDir = dir.rotateRight();
    if (br.curLoc.add(leftDir).distanceSquaredTo(dest) < br.curLoc
        .add(rightDir).distanceSquaredTo(dest)) {
      if (canMoveSafely(leftDir) && moveIsAllowedByEngagementRules(leftDir)) {
        rc.move(leftDir);
        return true;
      } else if (canMoveSafely(rightDir)
          && moveIsAllowedByEngagementRules(rightDir)) {
        rc.move(rightDir);
        return true;
      }
    } else {
      if (canMoveSafely(rightDir) && moveIsAllowedByEngagementRules(rightDir)) {
        rc.move(rightDir);
        return true;
      } else if (canMoveSafely(leftDir)
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
      if (!canMoveSafely(leftTryDir)
          || !moveIsAllowedByEngagementRules(leftTryDir))
        leftTryDir = leftTryDir.rotateLeft();
      else
        break;
    }
    Direction rightTryDir = bugLastMoveDir.rotateRight();
    for (int i = 0; i < 3; i++) {
      if (!canMoveSafely(rightTryDir)
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
      if (canMoveSafely(dir) && moveIsAllowedByEngagementRules(dir))
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

  private static boolean detectBugIntoEdge() {
    if (rc.senseTerrainTile(rc.getLocation().add(bugLastMoveDir)) != TerrainTile.OFF_MAP)
      return false;

    if (bugLastMoveDir.isDiagonal()) {
      if (bugWallSide == WallSide.LEFT) {
        return !canMoveSafely(bugLastMoveDir.rotateLeft());
      } else {
        return !canMoveSafely(bugLastMoveDir.rotateRight());
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
    YES, NO
  }

  public static void goTo(MapLocation theDest) throws GameActionException {
    // Debug.indicate("nav", 2, "goTo " + theDest.toString());

    // Debug.indicate("nav", 1, "");

    if (!theDest.equals(dest)) {
      dest = theDest;
      bugState = BugState.DIRECT;
      // Debug.indicateAppend("nav", 1, "new dest: resetting bug to direct; ");
    }

    if (br.curLoc.equals(theDest))
      return;

    if (!rc.isCoreReady()) {
      return;
    }

    bugTo(dest);
  }

  private static boolean canMoveSafely(Direction dir) {
    return rc.canMove(dir);// &&
                           // !Bot.isInTheirHQAttackRange(rc.getLocation().add(dir));
  }

  private static boolean moveIsAllowedByEngagementRules(Direction dir)
      throws GameActionException {
    return true;
    /*
     * if (numEnemiesAttackingMoveDirs[dir.ordinal()] == 0) return true; if
     * (!engage) return false;
     * 
     * if (fightDecisionIsCached) return fightIsWinningDecision; return true;
     * 
     * Robot[] allEngagedEnemies = rc.senseNearbyGameObjects(Robot.class,
     * rc.getLocation().add(dir), RobotType.SOLDIER.attackRadiusMaxSquared,
     * rc.getTeam() .opponent()); RobotInfo anEngagedEnemy =
     * Util.findANonConstructingSoldier(allEngagedEnemies, rc); if
     * (anEngagedEnemy == null) return true;
     * 
     * int numNearbyAllies = 1 +
     * Util.countNonConstructingSoldiers(rc.senseNearbyGameObjects(Robot.class,
     * anEngagedEnemy.location, 29, rc.getTeam()), rc); int numNearbyEnemies =
     * Util.countNonConstructingSoldiers(rc.senseNearbyGameObjects(Robot.class,
     * anEngagedEnemy.location, 49, rc.getTeam().opponent()), rc); boolean ret =
     * numNearbyAllies > numNearbyEnemies; fightIsWinningDecision = ret;
     * fightDecisionIsCached = true; return ret;
     */
  }
}