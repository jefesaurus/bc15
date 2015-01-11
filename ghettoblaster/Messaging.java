package ghettoblaster;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import ghettoblaster.RobotPlayer.BaseBot;
import ghettoblaster.BotTypes.Soldier.SoldierMode;

public class Messaging {
  public final static int NUM_BEAVERS = 0;
  public final static int ENEMY_TOWERS = 1;
  public final static int OUR_HQ = 13;
  public final static int ENEMY_HQ = 14;
  public final static int NUM_MINERS = 15;
  public final static int RALLY_POINT = 16;
  public final static int SOLDIER_MODE = 17;
  
  public static RobotController rc;
  public static BaseBot br;
  
  // init needs to get called once at the beginning to set up some stuff.
  public static void init(BaseBot brIn) throws GameActionException {
    rc = brIn.rc;
    br = brIn;
  }

  
  public static int announceBeaver() throws GameActionException {
    int numBeavers = rc.readBroadcast(NUM_BEAVERS);
    rc.broadcast(NUM_BEAVERS, numBeavers + 1);
    return numBeavers;
  }
  
  public static MapLocation readRallyPoint() throws GameActionException {
    return readLocation(RALLY_POINT);
  }
  
  public static void setRallyPoint(MapLocation loc) throws GameActionException {
    writeLocation(RALLY_POINT, loc);
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
  
  public static void setSoldierMode(SoldierMode mode) throws GameActionException {
    rc.broadcast(SOLDIER_MODE, mode.ordinal());
  }
  
  public static SoldierMode getSoldierMode() throws GameActionException {
    return SoldierMode.values()[rc.readBroadcast(SOLDIER_MODE)];
  }
  
  public static void setSurvivingEnemyTowers() throws GameActionException {
    MapLocation[] curEnemyTowers = rc.senseEnemyTowerLocations();
    int val = 0;
    for (int i = br.enemyTowers.length; i-- > 0;) {
      val = val << 1;
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
    }
    return enemyTowers;
  }
}
