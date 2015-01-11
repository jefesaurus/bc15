package ghettoblaster.BotTypes;

import ghettoblaster.Messaging;
import ghettoblaster.RobotPlayer.BaseBot;
import ghettoblaster.BotTypes.Soldier.SoldierMode;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class HQ extends BaseBot {
  private MapLocation[] enemyTowers;
  private int towersLeft = 6;
  
  public HQ(RobotController rc) {
    super(rc);
  }
  
  public void setup() throws GameActionException {
    MapLocation rallyPoint = new MapLocation((this.myHQ.x + this.enemyHQ.x) / 2, (this.myHQ.y + this.enemyHQ.y) / 2);
    Messaging.setRallyPoint(rallyPoint);
  }

  public void execute() throws GameActionException {
    int numBeavers = rc.readBroadcast(Messaging.NUM_BEAVERS);
    Messaging.setSurvivingEnemyTowers();

    if (rc.isCoreReady() && rc.getTeamOre() > 100 && numBeavers < 1) {
      Direction newDir = getSpawnDirection(RobotType.BEAVER);
      if (newDir != null) {
        rc.spawn(newDir, RobotType.BEAVER);
        rc.broadcast(Messaging.NUM_BEAVERS, numBeavers + 1);
      }
    }
    if (Clock.getRoundNum() >= 600) {
      Messaging.setSoldierMode(SoldierMode.TOWER_DIVE);
      targetNearestEnemyTower();
    }
  }
  
  /*
   * Senses enemy towers and sets the soldier rally point to the nearest one
   */
  private void targetNearestEnemyTower() throws GameActionException {
    if (towersLeft <= 0) {
      return;
    }
    enemyTowers = rc.senseEnemyTowerLocations();
    towersLeft = enemyTowers.length;
    
    if (towersLeft > 0) {
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
    } else {
      Messaging.setRallyPoint(enemyHQ);

    }
  }
}
