package terranbot;

import terranbot.RobotPlayer.BaseBot;
import terranbot.MovingBot;
import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;


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
  
  public final static int UNIT_TO_PRODUCE = 27;
  
  public final static int SAFE_ZONES = 28; // channels 28-41
  public final static int NUM_ACTIVE_SAFEZONES = 42;
  public final static int HIGH_ORE_REQUEST = 43;
  public final static int HIGH_ORE_LOCS = 44; // channels 44-53

  public final static int COUNT_OFFSET = 100;
  public final static int KILLED_OFFSET = 1000;
  
  public final static int BATTLE_OFFSET = 2000;
  public final static int NUM_BATTLE_CHANNELS = 5;
  public final static int DEFEND_OFFSET = 3000;
  public final static int NUM_DEFEND_CHANNELS = 5;

  
  public static RobotController rc;
  public static BaseBot br;
  
  public final static int mask = 0x00FF;
  public final static int BATTLE_RANGE_SQUARED = 25;
  public final static int FRESHNESS_TOLERANCE = 2;
  
  // init needs to get called once at the beginning to set up some stuff.
  public static void init(BaseBot brIn) throws GameActionException {
    rc = brIn.rc;
    br = brIn;
  }
  
  public static int getCountChannel(RobotType type) {
    return type.ordinal() + COUNT_OFFSET;
  }
  
  public static int getKilledChannel(RobotType type) {
    return type.ordinal() + KILLED_OFFSET;
  }
  
  public static void setDefendFront(MapLocation loc) throws GameActionException {
    for (int i=NUM_DEFEND_CHANNELS; i-- > 0;) {
      int chan = DEFEND_OFFSET + i;
      if (rc.readBroadcast(chan) == 0) {
        writeLocation(chan, loc, Clock.getRoundNum());
      } else {
        MapLocation curLoc = readLocation(chan);
        if (curLoc.distanceSquaredTo(loc) <= BATTLE_RANGE_SQUARED) {
          if (isFresh(chan)) {
            MapLocation newLoc = new MapLocation((curLoc.x + loc.x) / 2, (curLoc.y + loc.y) / 2);
            writeLocation(chan, newLoc, Clock.getRoundNum());
            return;
          }
        }
        
        if (!isFresh(chan)) {
          writeLocation(chan, loc, Clock.getRoundNum());
        }
      }
    }
  }
  
  // Returns null if no battlefront
  public static MapLocation getClosestDefendFront(MapLocation loc) throws GameActionException {
    MapLocation closest = null;
    for (int i=NUM_DEFEND_CHANNELS; i-- > 0;) {
      int chan = DEFEND_OFFSET + i;
      if (isFresh(chan) && rc.readBroadcast(chan) != 0) {
        MapLocation trialLoc = readLocation(chan);
        if (closest == null) {
          closest = trialLoc;
        } else {
          if (loc.distanceSquaredTo(trialLoc) < loc.distanceSquaredTo(closest)) {
            closest = trialLoc;
          }
        }
      }
    }
    return closest;
  }
  
  public static void setBattleFront(MapLocation loc) throws GameActionException {
    for (int i=NUM_BATTLE_CHANNELS; i-- > 0;) {
      int chan = BATTLE_OFFSET + i;
      if (rc.readBroadcast(chan) == 0) {
        writeLocation(chan, loc, Clock.getRoundNum());
      } else {
        MapLocation curLoc = readLocation(chan);
        if (curLoc.distanceSquaredTo(loc) <= BATTLE_RANGE_SQUARED) {
          if (isFresh(chan)) {
            MapLocation newLoc = new MapLocation((curLoc.x + loc.x) / 2, (curLoc.y + loc.y) / 2);
            writeLocation(chan, newLoc, Clock.getRoundNum());
            return;
          }
        }
        
        if (!isFresh(chan)) {
          writeLocation(chan, loc, Clock.getRoundNum());
        }
      }
    }
  }
  
  // Returns null if no battlefront
  public static MapLocation getClosestBattleFront(MapLocation loc) throws GameActionException {
    MapLocation closest = null;
    for (int i=NUM_BATTLE_CHANNELS; i-- > 0;) {
      int chan = BATTLE_OFFSET + i;
      if (isFresh(chan) && rc.readBroadcast(chan) != 0) {
        MapLocation trialLoc = readLocation(chan);
        if (closest == null) {
          closest = trialLoc;
        } else {
          if (loc.distanceSquaredTo(trialLoc) < loc.distanceSquaredTo(closest)) {
            closest = trialLoc;
          }
        }
      }
    }
    return closest;
  }
  
  public static void initializeSafeZones() throws GameActionException { // TODO don't choose all towers maybe
    int safeZones = 7;
    int zonesUsed = 0;
    
    // Populate with initial mining locations (currently, HQ and towers)
    // Add HQ
    rc.broadcast(SAFE_ZONES, br.myHQ.x);
    rc.broadcast(SAFE_ZONES + 1, br.myHQ.y);
    zonesUsed++;
    
    // Add our corner
    //MapLocation safeDir = safestDirection();
    //rc.broadcast(SAFE_ZONES + 2, corner.x);
    //rc.broadcast(SAFE_ZONES + 3, corner.y);
    
    // Add towers
    for (int i = 0; i < br.myTowers.length && i < safeZones; i++) {
      MapLocation tower = br.myTowers[i];
      rc.broadcast(SAFE_ZONES + 2 + 2*i, tower.x);
      rc.broadcast(SAFE_ZONES + 2 + 2*i + 1, tower.y);
      zonesUsed++;
    }
    
    rc.broadcast(NUM_ACTIVE_SAFEZONES, zonesUsed);
  }
  
  private static MapLocation safestDirection() {
    int dir_x;
    int dir_y;
    
    // Corner closest to our HQ
    if (br.myHQ.x >= br.enemyHQ.x) { 
      dir_x = 1; 
     }
    else { 
      dir_x = -1; 
    }
    if (br.myHQ.y >= br.enemyHQ.y) { 
      dir_y = 1; 
    }
    else { 
      dir_y = -1; 
    }
    
    System.out.println("safe x: " + dir_x + ", safe y: " + dir_y);
    
    MapLocation corner = new MapLocation(dir_x, dir_y);
    return corner;
  }

//  public static MapLocation[] getTowersCloserToMyHQ() {
//  }
  
  public static void resetUnitCount(RobotType type) throws GameActionException {
    int chan = getCountChannel(type);
    int x = rc.readBroadcast(chan);
    rc.broadcast(chan, x & 0xFFFF00);
  }
  
  public static int checkNumUnits(RobotType type) throws GameActionException {
    int val = rc.readBroadcast(getCountChannel(type)) & mask;
    //System.out.println("" + type + ": " + val);
    return val;
  }
  
  public static void announceDoneBuilding(RobotType type) throws GameActionException {
    int chan = getCountChannel(type);
    int x = rc.readBroadcast(chan);
    rc.broadcast(chan, x - (1 << 16));
  }
  
  public static int peekBuildingUnits(RobotType type) throws GameActionException {
    int chan = getCountChannel(type);
    int x = rc.readBroadcast(chan);
    return x >> 16;
  }
  
  public static void announceBuilding(RobotType type) throws GameActionException {
    int chan = getCountChannel(type);
    int x = rc.readBroadcast(chan);
    rc.broadcast(chan, x + (1 << 16));
  }
  
  public static void announceUnit(RobotType type) throws GameActionException {
    int chan = getCountChannel(type);
    int x = rc.readBroadcast(chan);
    rc.broadcast(chan, x + 1);
  }
  
  public static void queueUnits(RobotType type, int quantity) throws GameActionException {
    int chan = getCountChannel(type);
    int x = rc.readBroadcast(chan);
    rc.broadcast(chan, x + (quantity << 8));
  }
  
  public static int peekQueueUnits(RobotType type) throws GameActionException {
    int chan = getCountChannel(type);
    return (rc.readBroadcast(chan) >> 8) & mask;
  }
  
  public static int checkTotalNumUnits(RobotType type) throws GameActionException {
    int chan = getCountChannel(type);
    int x = rc.readBroadcast(chan);
    //System.out.println("" + type + ": " + x);
    return (x & mask) + ((x >> 8) & mask) + ((x >> 16) & mask);
  }
  
  public static boolean dequeueUnit(RobotType type) throws GameActionException {
    int chan = getCountChannel(type);
    int x = rc.readBroadcast(chan);
    int numQueuedUnits = (x >> 8) & mask;
    if (numQueuedUnits > 0) {
      rc.broadcast(chan, x - (1 << 8) + (1 << 16));
      return true;
    } else {
      return false;
    }
  }
 
  public static void incrementKillCount(RobotType type) throws GameActionException {
    int chan = getKilledChannel(type);
    int x = rc.readBroadcast(chan);
    rc.broadcast(chan, x + 1);
  }
  
  public static int checkKillCount(RobotType type) throws GameActionException {
    int chan = getKilledChannel(type);
    return rc.readBroadcast(chan);
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
  
  public static MapLocation readRallyPoint() throws GameActionException {
    int x = rc.readBroadcast(RALLY_POINT_X);
    int y = rc.readBroadcast(RALLY_POINT_Y);
    return new MapLocation(x, y);
  }
  
  public static void setRallyPoint(MapLocation loc) throws GameActionException {
    rc.broadcast(RALLY_POINT_X, loc.x);
    rc.broadcast(RALLY_POINT_Y, loc.y);
  }
  
  public static void writeLocation(int register, MapLocation loc, int roundNum) throws GameActionException {
    // This math shifts the location by our HQ's vector and then into the positive quadrant by 120
    // This guarantees the coordinates will be small and positive. (0 <= x,y <= 240)
    int val = (roundNum << 20) + (loc.x - br.myHQ.x + 120) << 8 | (loc.y - br.myHQ.y + 120);
    rc.broadcast(register, val);
  }
  
  public static MapLocation readLocation(int register) throws GameActionException {
    int val = rc.readBroadcast(register);
    if (val == 0xFFFFFFFF) {
      return null;
    }
    return new MapLocation(((val >> 8) & 0x000000FF) - 120 + br.myHQ.x, (val & 0x000000FF) - 120 + br.myHQ.y);
  }
  
  public static boolean isFresh(int register) throws GameActionException {
    int val = rc.readBroadcast(register);
    return Clock.getRoundNum() - (val >> 20) <= FRESHNESS_TOLERANCE;
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
  
  public static void setUnitToProduce(RobotType type) throws GameActionException {
    if (type == null) {
      rc.broadcast(UNIT_TO_PRODUCE, -1);
    } else {
      rc.broadcast(UNIT_TO_PRODUCE, type.ordinal());
    }
  }
  
  public static int getUnitToProduce() throws GameActionException {
    return rc.readBroadcast(UNIT_TO_PRODUCE);
  }
}
