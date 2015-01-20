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
  
  // RESERVED for rank locations
  // First spot is whether the tower is active.
  // Second spot is number of locations after.
  public final static int[] DEFENSE_RANK_REGISTERS = {200, 220, 240, 260, 280, 300, 320};
  public final static int TOWER_ALIVE_OFFSET = 0;
  public final static int OCCUPANCY_COUNT_OFFSET = 1;
  public final static int OCCUPANCY_MAP_OFFSET = 2;
  public final static int DX_OFFSET = 3;
  public final static int DY_OFFSET = 4;
  public final static int CENTERX_OFFSET = 5;
  public final static int CENTERY_OFFSET = 6;
  public final static int TOWER_ATTACK_OFFSET = 7;



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
    MapLocation corner = findOurCorner();
    rc.broadcast(SAFE_ZONES + 2, corner.x);
    rc.broadcast(SAFE_ZONES + 3, corner.y);
    
    // Add towers
    for (int i = 0; i < br.myTowers.length && i < safeZones; i++) {
      MapLocation tower = br.myTowers[i];
      rc.broadcast(SAFE_ZONES + 4 + 2*i, tower.x);
      rc.broadcast(SAFE_ZONES + 4 + 2*i + 1, tower.y);
      zonesUsed++;
    }
    
    rc.broadcast(NUM_ACTIVE_SAFEZONES, zonesUsed);
  }
  
  private static MapLocation findOurCorner() {
    int corner_x;
    int corner_y;
    int maxWidth = 120;
    
    // Corner closest to our HQ
    if (br.myHQ.x >= br.enemyHQ.x) { corner_x = br.myHQ.x + maxWidth; }
    else { corner_x = br.myHQ.x - maxWidth; }
    if (br.myHQ.y >= br.enemyHQ.y) { corner_y = br.myHQ.y + maxWidth; }
    else { corner_y = br.myHQ.y - maxWidth; }
    
    MapLocation corner = new MapLocation(corner_x, corner_y);
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
  
  public static void updateRoundNumber(int register) throws GameActionException {
    int val = rc.readBroadcast(register);
    val &= 0x00FFFF;
    val |= (Clock.getRoundNum() << 16);
    rc.broadcast(val, register);
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
  
  /*
  public static int setRoundNumber(int val) throws GameActionException {
    val &= 0x0000FFFF;
    val |= (Clock.getRoundNum() << 16);
    return val;
  }
  
  public static int getRoundNumber(int val) {
    return (val >> 16) & 0xFFFF;
  }
  
  public static int packLocation(MapLocation loc) {
    return (loc.x - br.myHQ.x + 120) << 8 | (loc.y - br.myHQ.y + 120);
  }
  public static MapLocation unpackLocation(int packedLoc) {
    if ((packedLoc & 0xFFFF) == 0xFFFF) {
      return null;
    }
    return new MapLocation(((packedLoc >> 8) & 0x000000FF) - 120 + br.myHQ.x, (packedLoc & 0x000000FF) - 120 + br.myHQ.y);
  }
  
  public static boolean isRankSpotReserved(int val) {
    int roundDiff = Clock.getRoundNum() - getRoundNumber(val);
    return roundDiff >= 0 && roundDiff < 5;
  }
  
  public static void keepDefenseRankActive(int towerID) throws GameActionException {
    rc.broadcast(DEFENSE_RANK_ACTIVE_REGISTERS[towerID], Clock.getRoundNum());
  }
  
  public static boolean isDefenseRankActive(int towerID) throws GameActionException {
    if (towerID >= 0 && towerID <= 6) {
      return (Clock.getRoundNum() - rc.readBroadcast(DEFENSE_RANK_ACTIVE_REGISTERS[towerID])) < 3;
    } else {
      return false;
    }
  }
  */
  /*
  public static void setRankData(MapLocation[] rankLocs, int towerID) throws GameActionException {
    rc.broadcast(DEFENSE_RANK_NUMSPOT_REGISTERS[towerID], rankLocs.length);
    int val;
    for (int i = rankLocs.length; i-- > 0;) {
      val = packLocation(rankLocs[i]) | 0xFFFF0000;
      rc.broadcast(DEFENSE_RANK_LOC_REGISTER_STARTS[towerID] + i, val);
    }
  }
  */
  

  
  /*
  public static void reserveRankSpot() throws GameActionException {
    int start = Util.randInt()%6;
    int tempTowerID = 0;
    for (int j = 0; j < 6; j++) {
      tempTowerID = (start + j) % 6 + 1;
      if (isDefenseRankActive(tempTowerID)) {
        int numRanks = rc.readBroadcast(DEFENSE_RANK_NUMSPOT_REGISTERS[tempTowerID]);
        for (int i = numRanks; i-- > 0;) {
          int val = rc.readBroadcast(DEFENSE_RANK_LOC_REGISTER_STARTS[tempTowerID] + i);
          if (!isRankSpotReserved(val)) {
            MapLocation loc = unpackLocation(val);
            rc.broadcast(DEFENSE_RANK_LOC_REGISTER_STARTS[tempTowerID] + i, setRoundNumber(val));
            RESERVED_SLOT = DEFENSE_RANK_LOC_REGISTER_STARTS[tempTowerID] + i;
            DEFENSE_RANK_ID = tempTowerID;
            MY_SPOT = loc;
            return;
          }
        }
      }
    }
    RESERVED_SLOT = -1;
    DEFENSE_RANK_ID = -1;
    MY_SPOT = null;
    return;
  }
  */
  /*
  public final static int[] DEFENSE_RANK_REGISTERS = {200, 220, 240, 260, 280, 300, 320};
  public final static int TOWER_ALIVE_OFFSET = 0;
  public final static int OCCUPANCY_COUNT_OFFSET = 1;
  public final static int OCCUPANCY_MAP_OFFSET = 2;
  public final static int DX_OFFSET_OFFSET = 3;
  public final static int DY_OFFSET_OFFSET = 4;
   */
  static int RESERVED_SLOT = -1;
  static int DEFENSE_RANK_ID = -1;
  static MapLocation MY_SPOT = null;
  
  public static int claimRankSpot(int towerIndex) throws GameActionException {
    int val = rc.readBroadcast(DEFENSE_RANK_REGISTERS[towerIndex] + OCCUPANCY_MAP_OFFSET);
    if (val == 0b11111111111111111111111111111111) {
      return -1;
    }
    int bitmask = 0b1;
    int index = 0;
    while ((val & bitmask) > 0 && index < 31) {
      bitmask <<= 1;
      index ++;
    }
    rc.broadcast(DEFENSE_RANK_REGISTERS[towerIndex] + OCCUPANCY_MAP_OFFSET, val | bitmask);
    return index;
  }
  
  // Returns false if spot is already taken.
  public static boolean refreshRankSpot(int towerIndex, int spotIndex) throws GameActionException {
    int val = rc.readBroadcast(DEFENSE_RANK_REGISTERS[towerIndex] + OCCUPANCY_MAP_OFFSET);
    int mask = 0b1 << spotIndex;
    // If someone already set it this turn, look for a new spot.
    if ((val & mask) > 0) {
      return false;
    }
    rc.broadcast(DEFENSE_RANK_REGISTERS[towerIndex] + OCCUPANCY_MAP_OFFSET, val | mask);
    return true;
  }
  
  public static int[] getRankTarget(int towerIndex) throws GameActionException {
    int x = rc.readBroadcast(DEFENSE_RANK_REGISTERS[towerIndex] + DX_OFFSET);
    int y = rc.readBroadcast(DEFENSE_RANK_REGISTERS[towerIndex] + DY_OFFSET);
    return new int[] {x, y};
  }
  
  public static MapLocation getRankCenter(int towerIndex) throws GameActionException {
    int x = rc.readBroadcast(DEFENSE_RANK_REGISTERS[towerIndex] + CENTERX_OFFSET);
    int y = rc.readBroadcast(DEFENSE_RANK_REGISTERS[towerIndex] + CENTERY_OFFSET);
    return new MapLocation(x,y);
  }
  
  public static void setRankTarget(int rankIndex, int dx, int dy) throws GameActionException {
    rc.broadcast(DEFENSE_RANK_REGISTERS[rankIndex] + DX_OFFSET, dx);
    rc.broadcast(DEFENSE_RANK_REGISTERS[rankIndex] + DY_OFFSET, dy);
  }
  
  public static void setRankCenter(int rankIndex, MapLocation center) throws GameActionException {
    rc.broadcast(DEFENSE_RANK_REGISTERS[rankIndex] + CENTERX_OFFSET, center.x);
    rc.broadcast(DEFENSE_RANK_REGISTERS[rankIndex] + CENTERY_OFFSET, center.y);
  }
  
  public static int getRankOccupancyCount(int towerIndex) throws GameActionException {
    int fromTower = rc.readBroadcast(DEFENSE_RANK_REGISTERS[towerIndex] + OCCUPANCY_COUNT_OFFSET);
    int fromBits = Integer.bitCount(rc.readBroadcast(DEFENSE_RANK_REGISTERS[towerIndex] + OCCUPANCY_MAP_OFFSET));
    return (fromTower > fromBits) ? fromTower : fromBits;
  }
  
  public static int completeRankRefresh(int towerIndex) throws GameActionException {
    int count = Integer.bitCount(rc.readBroadcast(DEFENSE_RANK_REGISTERS[towerIndex] + OCCUPANCY_MAP_OFFSET));
    rc.broadcast(DEFENSE_RANK_REGISTERS[towerIndex] + OCCUPANCY_COUNT_OFFSET, count);
    rc.broadcast(DEFENSE_RANK_REGISTERS[towerIndex] + OCCUPANCY_MAP_OFFSET, 0b0);
    rc.broadcast(DEFENSE_RANK_REGISTERS[towerIndex] + TOWER_ALIVE_OFFSET, Clock.getRoundNum());
    return count;
  }
  
  public static boolean rankIsActive(int towerIndex) throws GameActionException {
    return (Clock.getRoundNum() - rc.readBroadcast(DEFENSE_RANK_REGISTERS[towerIndex] + TOWER_ALIVE_OFFSET) < 5);
  }
  
  public static boolean rankIsAttacking(int towerIndex) throws GameActionException {
    return (Clock.getRoundNum() - rc.readBroadcast(DEFENSE_RANK_REGISTERS[towerIndex] + TOWER_ATTACK_OFFSET) < 3);
  }
  
  public static void setRankIsAttacking(int towerIndex) throws GameActionException {
    rc.broadcast(DEFENSE_RANK_REGISTERS[towerIndex] + TOWER_ATTACK_OFFSET, Clock.getRoundNum());
  }
  
  public static int getLeastDefendedTowerIndex() throws GameActionException {
    int leastOccupancy = Integer.MAX_VALUE;
    int leastOccupiedIndex = -1;
    int tempCount;
    for (int i = 7; i-- > 1;) {
      if (rankIsActive(i)) {
        tempCount = getRankOccupancyCount(i);
        if (tempCount < leastOccupancy) {
          leastOccupiedIndex = i;
          leastOccupancy = tempCount;
        }
      }
    }
    return leastOccupiedIndex;
  }
}
