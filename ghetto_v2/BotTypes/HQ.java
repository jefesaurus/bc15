package ghetto_v2.BotTypes;

import ghetto_v2.Cache;
import ghetto_v2.Messaging;
import ghetto_v2.SupplyDistribution;
import ghetto_v2.Util;
import ghetto_v2.RobotPlayer.BaseBot;
import ghetto_v2.RobotPlayer.MovingBot;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class HQ extends BaseBot {
  private MapLocation[] enemyTowers;
  private int towersLeft = 6;
  private SupplyDistribution supply;
  
  public HQ(RobotController rc) {
    super(rc);
    this.supply = new SupplyDistribution(rc);
  }
  
  public void setup() throws GameActionException {
    MapLocation rallyPoint = this.myHQ;
    Messaging.setRallyPoint(rallyPoint);
  }

  public void execute() throws GameActionException {
    int numBeavers = rc.readBroadcast(Messaging.NUM_BEAVERS);
    supply.setBatteryMode();
    supply.manageSupply();
    
    // This checks which enemy towers are still alive and broadcasts it to save bytecode across the fleet
    Messaging.setSurvivingEnemyTowers(Cache.getEnemyTowerLocationsDirect());
    
    // Attack enemies if possible.
    RobotInfo[] enemies = getEnemiesInAttackingRange();
    if (enemies.length > 0) {
      if (rc.isWeaponReady()) {
        attackLeastHealthEnemy(enemies);
      }
    }
    
    // Spawn if possible
    if (rc.isCoreReady() && rc.getTeamOre() > 100 && numBeavers < 1) {
      Direction newDir = getOffensiveSpawnDirection(RobotType.BEAVER);
      if (newDir != null) {
        rc.spawn(newDir, RobotType.BEAVER);
        rc.broadcast(Messaging.NUM_BEAVERS, numBeavers + 1);
        Messaging.queueMiners(10);
      }
    }

    if (Clock.getRoundNum() >= 600 && towersLeft > 0) {
      MapLocation[] enemyTowers = Cache.getEnemyTowerLocationsDirect();
      Messaging.setSoldierMode(MovingBot.AttackMode.TOWER_DIVE);
      towersLeft = enemyTowers.length;
      targetNearestEnemyTower(enemyTowers);
    } else {
      Messaging.setSoldierMode(MovingBot.AttackMode.DEFEND_TOWERS);
      Messaging.setRallyPoint(myHQ);
    }
    
    rc.yield();
  }
  
  /*
   * Senses enemy towers and sets the soldier rally point to the nearest one
   */
  private void targetNearestEnemyTower(MapLocation[] enemyTowers) throws GameActionException {
    if (towersLeft <= 0) {
      return;
    }
    double tempDist;

    double closestDist = myHQ.distanceSquaredTo(enemyTowers[0]);
    int closestIndex = 0;
    for (int i = 1; i < towersLeft; i++) {
      tempDist = myHQ.distanceSquaredTo(enemyTowers[i]);
      if (tempDist < closestDist) {
        closestDist = tempDist;
        closestIndex = i;
      }
    }
    Messaging.setRallyPoint(enemyTowers[closestIndex]);
  }
}
