package ghettoblaster.BotTypes;

import ghettoblaster.Messaging;
import ghettoblaster.RobotPlayer.BaseBot;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class HQ extends BaseBot {
  public HQ(RobotController rc) throws GameActionException {
    super(rc);
    int numMiners = 5;
    rc.broadcast(Messaging.NUM_MINERS, numMiners);
  }

  public void execute() throws GameActionException {
    int numBeavers = rc.readBroadcast(Messaging.NUM_BEAVERS);

    if (rc.isCoreReady() && rc.getTeamOre() > 100 && numBeavers < 1) {
      Direction newDir = getSpawnDirection(RobotType.BEAVER);
      if (newDir != null) {
        rc.spawn(newDir, RobotType.BEAVER);
        rc.broadcast(Messaging.NUM_BEAVERS, numBeavers + 1);
      }
    }
    
    MapLocation rallyPoint;
    if (Clock.getRoundNum() < 600) {
      rallyPoint = new MapLocation((this.myHQ.x + this.enemyHQ.x) / 2,
          (this.myHQ.y + this.enemyHQ.y) / 2);
    } else {
      rallyPoint = this.enemyHQ;
    }
    rc.broadcast(Messaging.RALLY_POINT_X, rallyPoint.x);
    rc.broadcast(Messaging.RALLY_POINT_Y, rallyPoint.y);

    rc.yield();
  }
}
