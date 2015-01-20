package terranbot.BotTypes;

import terranbot.Cache;
import terranbot.Util;
import terranbot.Messaging;
import terranbot.RobotPlayer.BaseBot;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class Tower extends BaseBot {
  public Tower(RobotController rc) {
    super(rc);
  }
  
  public int myTowerID = -1;
  
  public void setup() throws GameActionException {
    int bc = Clock.getBytecodeNum();
    myTowerID = getMyTowerIndex();
    // setDefensePoints();
    Messaging.setRankCenter(myTowerID, Util.getPoint(curLoc, 1, curLoc.x - enemyHQ.x, curLoc.y - enemyHQ.y));
    Messaging.setRankTarget(myTowerID, enemyHQ.x - curLoc.x, enemyHQ.y - curLoc.y);
    System.out.println("ID: " + myTowerID + ", Tower: " + (Clock.getBytecodeNum() - bc));
  }
  
  public int getMyTowerIndex() {
    MapLocation[] towers = rc.senseTowerLocations();
    int numBefore = 0;
    int myUniqueTowerID = curLoc.x*GameConstants.MAP_MAX_WIDTH + curLoc.y;
    int tempID;
    for (int i = towers.length; i-- > 0;) {
      tempID = towers[i].x*GameConstants.MAP_MAX_WIDTH + towers[i].y;
      if (tempID < myUniqueTowerID) {
        numBefore++;
      }
    }
    return 1 + numBefore;
  }
  
  public void beginningOfTurn() throws GameActionException {
    // Messaging.keepDefenseRankActive(myTowerID);
    Messaging.completeRankRefresh(myTowerID);
    super.beginningOfTurn();
  }

  public boolean IS_ATTACKING = false;
  public void execute() throws GameActionException {    
    RobotInfo[] visibleEnemies = rc.senseNearbyRobots(100, theirTeam);//Cache.getEngagementEnemies();
    if (visibleEnemies.length > 0) {
      int centerX = 0;
      int centerY = 0;
      for (int i = visibleEnemies.length; i-- > 0;) {
        centerX += visibleEnemies[i].location.x;
        centerY += visibleEnemies[i].location.y;
      }
      centerX /= visibleEnemies.length;
      centerY /= visibleEnemies.length;
      rc.setIndicatorString(1, "Centroid: X: " + centerX + ", Y: " + centerY + ", " + Clock.getRoundNum());
      MapLocation rankBase = Util.getPoint(curLoc, 1 , centerX - curLoc.x, centerY - curLoc.y);
      Messaging.setRankCenter(myTowerID, rankBase);
      Messaging.setRankTarget(myTowerID, centerX - rankBase.x, centerY - rankBase.y);
      
      RobotInfo[] attackableEnemies = this.getEnemiesInAttackingRange();
      if (IS_ATTACKING || attackableEnemies.length > 3 && rc.isWeaponReady()) {
        rc.setIndicatorString(1, "Am attacking. Centroid: X: " + centerX + ", Y: " + centerY + ", " + Clock.getRoundNum());

        attackLeastHealthEnemy(attackableEnemies);
        Messaging.setTowerUnderAttack(this.curLoc);
        Messaging.setRankIsAttacking(myTowerID);
        IS_ATTACKING = true;
      }
    } else {
      IS_ATTACKING = false;
    }
  }
  
  /*
  public void setDefensePoints() throws GameActionException {
    MapLocation[] adjacent = new MapLocation[Util.REGULAR_DIRECTIONS.length];
    for (int i = adjacent.length; i-- > 0;) {
      adjacent[i] = curLoc.add(Util.REGULAR_DIRECTIONS[i]);
    }
    MapLocation center = curLoc.add(curLoc.directionTo(enemyHQ));
    MapLocation[] spots = getLinePoints(center.x, center.y, curLoc.y - enemyHQ.y, curLoc.x - enemyHQ.x, 10);
    Messaging.setRankData(spots, myTowerID);
  }
  */
  
  public MapLocation[] getLinePoints(int midX, int midY, int dx, int dy, int numSpots) {
    if (dy > dx) {
      dx = -(int)(dx*((numSpots/(double)dy)));
      dy = numSpots;
    } else {
      dy = (int)(dy*((numSpots/(double)dx)));
      dx = -numSpots;
    }
    System.out.println("dx: " + dx + ", dy: " + dy);
    int currentNumSpots = 0;
    if (numSpots % 2 == 1) {
      numSpots += 1;
    }
    MapLocation[] locs = new MapLocation[numSpots];
    
    int x_step = (dx > 0) ? 1 : -1;
    dx *= x_step;
    int y_step = (dy > 0) ? 1 : -1;
    dy *= y_step;
    int tdx = 0;
    int tdy = 0;
    MapLocation temp;
    if (dx > dy) {
      int err = dx;
      while (currentNumSpots < numSpots) {
        //SetCell(x, y, value);
        temp = new MapLocation(midX + tdx, midY + tdy);
        if (rc.canSenseLocation(temp) && rc.senseTerrainTile(temp).isTraversable()) {
          locs[currentNumSpots++] = temp;
        }
        temp = new MapLocation(midX - tdx, midY - tdy);
        if (rc.canSenseLocation(temp) && rc.senseTerrainTile(temp).isTraversable()) {
          locs[currentNumSpots++] = temp;
        }
        err -= 2*dy;
        if (err < 0) {
          tdy += y_step;
          err += 2*dx;
        }
        tdx += x_step;
      }
    } else {
      int err = dy;
      while (currentNumSpots < numSpots) {
        //SetCell(x, y, value);
        temp = new MapLocation(midX + tdx, midY + tdy);
        if (rc.canSenseLocation(temp) && rc.senseTerrainTile(temp).isTraversable()) {
          locs[currentNumSpots++] = temp;
        }
        temp = new MapLocation(midX - tdx, midY - tdy);
        if (rc.canSenseLocation(temp) && rc.senseTerrainTile(temp).isTraversable()) {
          locs[currentNumSpots++] = temp;
        }
        err -= 2*dx;
        if (err < 0) {
          tdx += x_step;
          err += 2*dy;
        }
        tdy += y_step;
      }
    }
    return locs;
  }
}