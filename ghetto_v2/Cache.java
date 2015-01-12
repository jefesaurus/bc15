package ghetto_v2;

import ghetto_v2.RobotPlayer.BaseBot;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class Cache {
  static BaseBot br;
  static RobotController rc;
  public static void init(BaseBot brIn) {
    br = brIn;
    rc = brIn.rc;
  }
  
  /*
   * Cacheable stuff
   * 
   * The cache is set up with pairs of values:
   * First, the actual data that is cached (and retrieved)
   * Second, the last round it was refreshed. (*_last)
   */
  static RobotInfo[] visibleEnemies;
  static int visibleEnemies_last = -1;
  
  static RobotInfo[] soldierAttackableEnemies;
  static int soldierAttackableEnemies_last = -1;
  
  static RobotInfo[] engagementEnemies;
  static int engagementEnemies_last = -1;
  
  static MapLocation[] enemyTowerLocations;
  static int enemyTowerLocations_last = -1;
  
  public static RobotInfo[] getVisibleEnemies() {
    if (visibleEnemies_last < br.curRound) {
      visibleEnemies = rc.senseNearbyRobots(RobotType.SOLDIER.sensorRadiusSquared, br.theirTeam);
      visibleEnemies_last = br.curRound;
    }
    return visibleEnemies;
  }
  
  public static RobotInfo[] getSoldierAttackableEnemies() {
    if (soldierAttackableEnemies_last < br.curRound) {
      soldierAttackableEnemies = rc.senseNearbyRobots(RobotType.SOLDIER.attackRadiusSquared, br.theirTeam);
      soldierAttackableEnemies_last = br.curRound;
    }
    return soldierAttackableEnemies;
  }
  
  public static RobotInfo[] getEngagementEnemies() {
    if (engagementEnemies_last < br.curRound) {
      engagementEnemies = rc.senseNearbyRobots(35, br.theirTeam);
      engagementEnemies_last = br.curRound;
    }
    return engagementEnemies;
  }
  
  // Will return some null locations.
  public static MapLocation[] getEnemyTowerLocations() throws GameActionException {
    if (enemyTowerLocations_last < br.curRound) {
      enemyTowerLocations = Messaging.getSurvivingEnemyTowers();
      enemyTowerLocations_last = br.curRound;
    }
    return enemyTowerLocations;
  }
  
  // Same effect as above, but does it using the direct API call, so it doesn't return null locations.
  public static MapLocation[] getEnemyTowerLocationsDirect() throws GameActionException {
    if (enemyTowerLocations_last < br.curRound) {
      enemyTowerLocations = rc.senseEnemyTowerLocations();
      enemyTowerLocations_last = br.curRound;
    }
    return enemyTowerLocations;
  }
}
