package ghetto_v2_5;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import ghetto_v2_5.RobotPlayer.BaseBot;
import ghetto_v2_5.RobotPlayer.MovingBot;


public class Messaging {
  public final static int NUM_BEAVERS = 0;
  public final static int ENEMY_TOWERS = 1;
  public final static int OUR_HQ = 13;
  public final static int ENEMY_HQ = 14;
  public final static int QUEUED_MINERS = 15;
  public final static int RALLY_POINT_X = 16;
  public final static int RALLY_POINT_Y = 17;

  public final static int FLEET_MODE = 18;
  
  public final static int FLEET_COUNT = 19;
  public final static int FLEET_CENTROID_X = 20;
  public final static int FLEET_CENTROID_Y = 21;

  public final static int HQ_UNDER_ATTACK = 22;
  public final static int TOWERS_UNDER_ATTACK = 23;
  
  public final static int VULNERABLE_TOWER_COMPUTATION = 24;
  public final static int VULNERABLE_TOWER_LIST = 25;
  
  public final static int NUM_MINERS = 26;

  public static RobotController rc;
  public static BaseBot br;
  
  // init needs to get called once at the beginning to set up some stuff.
  public static void init(BaseBot brIn) throws GameActionException {
    rc = brIn.rc;
    br = brIn;
  }
  
  public static int checkNumMiners() throws GameActionException {
    int x = rc.readBroadcast(NUM_MINERS);
    rc.broadcast(NUM_MINERS, 0);
    return x;
  }
  
  public static void announceMiner() throws GameActionException {
    int x = rc.readBroadcast(NUM_MINERS);
    rc.broadcast(NUM_MINERS, ++x);
  }
  
  public static boolean queueVulnerableTowerComputation() throws GameActionException {
    int x = rc.readBroadcast(VULNERABLE_TOWER_COMPUTATION);
    if (x == 1) {
      return false;
    } else {
      rc.broadcast(VULNERABLE_TOWER_COMPUTATION, 1);
      return true;
    }
  }

  public static void queueMiners(int quantity) throws GameActionException {
    int x = rc.readBroadcast(QUEUED_MINERS);
    rc.broadcast(QUEUED_MINERS, quantity + x);
  }
  
  public static boolean dequeueMiner() throws GameActionException {
    int numQueuedMiners = rc.readBroadcast(QUEUED_MINERS);
    if (numQueuedMiners > 0) {
      rc.broadcast(QUEUED_MINERS, numQueuedMiners - 1);
      return true;
    } else {
      return false;
    }
  }
  
  public static int peekQueuedMiners() throws GameActionException {
    return rc.readBroadcast(QUEUED_MINERS);
  }
  
  public static int announceBeaver() throws GameActionException {
    int numBeavers = rc.readBroadcast(NUM_BEAVERS);
    rc.broadcast(NUM_BEAVERS, numBeavers + 1);
    return numBeavers;
  }
  
  public static MapLocation readRallyPoint() throws GameActionException {
    int x = rc.readBroadcast(RALLY_POINT_X);
    int y = rc.readBroadcast(RALLY_POINT_Y);
    return new MapLocation(x, y);
  }
  
  public static void setRallyPoint(MapLocation loc) throws GameActionException {
    rc.broadcast(RALLY_POINT_X, loc.x);
    rc.broadcast(RALLY_POINT_Y, loc.y);
  }
  
  public static void writeLocation(int register, MapLocation loc) throws GameActionException {
    // This math shifts the location by our HQ's vector and then into the positive quadrant by 120
    // This guarantees the coordinates will be small and positive. (0 <= x,y <= 240)
    int val = (loc.x - br.myHQ.x + 120) << 16 | (loc.y - br.myHQ.y + 120);
    rc.broadcast(register, val);
  }
  
  public static MapLocation readLocation(int register) throws GameActionException {
    int val = rc.readBroadcast(register);
    if (val == 0xFFFFFFFF) {
      return null;
    }
    return new MapLocation((val >> 16) - 120 + br.myHQ.x, (val & 0x0000FFFF) - 120 + br.myHQ.y);
  }
  
  public static void setFleetMode(MovingBot.AttackMode mode) throws GameActionException {
    rc.broadcast(FLEET_MODE, mode.ordinal());
  }
  
  public static MovingBot.AttackMode getFleetMode() throws GameActionException {
    return MovingBot.AttackMode.values()[rc.readBroadcast(FLEET_MODE)];
  }
  
  public static void setSurvivingEnemyTowers(MapLocation[] curEnemyTowers) throws GameActionException {
    int val = 0;
    for (int i = 0; i < br.enemyTowers.length; i++) {
      val <<= 1;
      for (int j = curEnemyTowers.length; j-- > 0;) {
        if (curEnemyTowers[j].x == br.enemyTowers[i].x && curEnemyTowers[j].y == br.enemyTowers[i].y) {
          val |= 1;
        }
      }
    }
    rc.broadcast(ENEMY_TOWERS, val);
  }
  
  public static MapLocation[] getSurvivingEnemyTowers() throws GameActionException {
    int val = rc.readBroadcast(ENEMY_TOWERS);
    MapLocation[] enemyTowers = new MapLocation[br.enemyTowers.length];
    for (int i = br.enemyTowers.length; i-- > 0;) {
      if ((val & 0x1) > 0) {
        enemyTowers[i] = br.enemyTowers[i];
      }
      val >>= 1;
    }
    return enemyTowers;
  }
  
  public static int getNumSurvivingEnemyTowers() throws GameActionException {
    int val = rc.readBroadcast(ENEMY_TOWERS);
    return Integer.bitCount(val);
  }
  
  // Adds this unit to the array for fleet centroid.
  public static void addToFleetCentroid() throws GameActionException {
    int count = rc.readBroadcast(FLEET_COUNT);
    rc.broadcast(FLEET_COUNT, count+1);
    int centroidX = rc.readBroadcast(FLEET_CENTROID_X);
    rc.broadcast(FLEET_CENTROID_X, centroidX + br.curLoc.x);
    int centroidY = rc.readBroadcast(FLEET_CENTROID_Y);
    rc.broadcast(FLEET_CENTROID_Y, centroidY + br.curLoc.y);
  }
  
  public static int getFleetCount() throws GameActionException {
    return rc.readBroadcast(FLEET_COUNT);
  }
  
  public static MapLocation getFleetCentroid() throws GameActionException {
    int count = rc.readBroadcast(FLEET_COUNT);
    if (count == 0) {
      return new MapLocation(0,0);
    }
    int centroidX = rc.readBroadcast(FLEET_CENTROID_X);
    int centroidY = rc.readBroadcast(FLEET_CENTROID_Y);
    return new MapLocation(centroidX/count, centroidY/count);
  }
  
  public static void resetFleetCentroid() throws GameActionException {
    rc.broadcast(FLEET_COUNT, 0);
    rc.broadcast(FLEET_CENTROID_X, 0);
    rc.broadcast(FLEET_CENTROID_Y, 0);
  }
  
  public static void resetTowersUnderAttack() throws GameActionException {
    rc.broadcast(TOWERS_UNDER_ATTACK, 0);
  }
  
  public static void setTowerUnderAttack(MapLocation towerLoc) throws GameActionException {
    int curVal = rc.readBroadcast(TOWERS_UNDER_ATTACK);
    int mask = 0x1;
    for (int j = br.myTowers.length; j-- > 0;) {
      if (br.myTowers[j].x == towerLoc.x && br.myTowers[j].y == towerLoc.y) {
        curVal |= mask;
      }
      mask <<= 1;
    }
    rc.broadcast(TOWERS_UNDER_ATTACK, curVal);
  }
  
  public static MapLocation getClosestTowerUnderAttack() throws GameActionException {
    int val = rc.readBroadcast(TOWERS_UNDER_ATTACK);
    MapLocation closest = null;
    double closestDist = Double.MAX_VALUE;
    double tempDist;
    for (int i = br.myTowers.length; i-- > 0;) {
      if ((val & 0x1) > 0) {
        tempDist = br.myTowers[i].distanceSquaredTo(br.curLoc);
        if (tempDist < closestDist) {
          closestDist = tempDist;
          closest = br.myTowers[i];
        }
      }
      val >>= 1;
    }
    return closest;
  }
}
