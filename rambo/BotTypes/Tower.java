package rambo.BotTypes;

import rambo.Messaging;
import rambo.RobotPlayer.BaseBot;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class Tower extends BaseBot {
  public Tower(RobotController rc) {
    super(rc);
  }

  public void setup() throws GameActionException {
    broadcastHighOreLocs(); 
}
  
  public void execute() throws GameActionException {
    RobotInfo[] visibleEnemies = getVisibleEnemies();
    boolean hasLaunchers = false;
    boolean hasNotMinerAndBeaverUnits = false;
    if (visibleEnemies.length > 0) {
      MapLocation closest = null;
      for (int i=visibleEnemies.length; i-->0;) {
        MapLocation trialLoc = visibleEnemies[i].location;
        if (visibleEnemies[i].type != RobotType.MINER || visibleEnemies[i].type != RobotType.BEAVER) {
          hasNotMinerAndBeaverUnits = true;
        }
        if (visibleEnemies[i].type == RobotType.LAUNCHER || visibleEnemies[i].type == RobotType.LAUNCHER) {
          hasLaunchers = true;
        }
        if (closest == null) {
          closest = trialLoc;
        } else {
          if (this.curLoc.distanceSquaredTo(trialLoc) < this.curLoc.distanceSquaredTo(closest)) {
            closest = trialLoc;
          }
        }
      }
      Messaging.setDefendFront(closest);
    }
    if (hasLaunchers || (hasNotMinerAndBeaverUnits && visibleEnemies.length >= 3)) {
      Messaging.setTowerUnderAttack(this.curLoc);
    }
    RobotInfo[] attackableEnemies = this.getEnemiesInAttackingRange();
    if (attackableEnemies.length > 0 && rc.isWeaponReady()) {
      attackLeastHealthPrioritized(attackableEnemies);
    }
  }

  private void broadcastHighOreLocs() throws GameActionException {
    MapLocation[] locationsInVisionRange = MapLocation.getAllMapLocationsWithinRadiusSq(this.curLoc, RobotType.TOWER.sensorRadiusSquared);
    int x = this.curLoc.x;
    int y = this.curLoc.y;
    double currentMax = rc.senseOre(this.curLoc);
    for (int i = locationsInVisionRange.length; i-- > 0;) {
      MapLocation loc = locationsInVisionRange[i];
      double potentialMax = rc.senseOre(loc);
      if (!rc.isLocationOccupied(loc) && potentialMax > rc.senseOre(new MapLocation(x, y))) {
        x = loc.x;
        y = loc.y;
        currentMax = potentialMax;
      }
    }
    int numTowersCheckedIn = rc.readBroadcast(Messaging.HIGH_ORE_TOWERS_COUNT);

    rc.broadcast(Messaging.HIGH_ORE_TOWERS_LOCS + 3*numTowersCheckedIn, x);
    rc.broadcast(Messaging.HIGH_ORE_TOWERS_LOCS + 3*numTowersCheckedIn + 1, y);
    rc.broadcast(Messaging.HIGH_ORE_TOWERS_LOCS + 3*numTowersCheckedIn + 2, (int) currentMax);
    rc.broadcast(Messaging.HIGH_ORE_TOWERS_COUNT, numTowersCheckedIn + 1);
    rc.broadcast(Messaging.UNCLAIMED_HIGH_ORE_TOWERS_COUNT, numTowersCheckedIn + 1);

  }

}