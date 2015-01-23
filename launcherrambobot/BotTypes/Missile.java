package launcherrambobot.BotTypes;

import launcherrambobot.Cache;
import launcherrambobot.HibernateSystem;
import launcherrambobot.Messaging;
import launcherrambobot.MovingBot;
import launcherrambobot.Nav;
import launcherrambobot.SupplyDistribution;
import launcherrambobot.Nav.Engage;
import launcherrambobot.RobotPlayer.BaseBot;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;


public class Missile extends MovingBot {  
  public static int turnsLeft = GameConstants.MISSILE_LIFESPAN;

  public Missile(RobotController rc) {
    super(rc);
  }
  
  public void setup() throws GameActionException{}

  protected MapLocation rallyPoint = null;
  protected MovingBot.AttackMode mode = MovingBot.AttackMode.OFFENSIVE_SWARM;

  public void execute() throws GameActionException {
    int radiusSquared = turnsLeft*turnsLeft;
    MapLocation curLoc = rc.getLocation();
    MapLocation target = null;
    RobotInfo[] enemies = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, rc.getTeam().opponent());
    for (int i=enemies.length; i-->0;) {
      RobotInfo info = enemies[i];
      if (curLoc.distanceSquaredTo(info.location) <= 2) {
        rc.explode();
      }
      if (curLoc.distanceSquaredTo(info.location) <= radiusSquared && rc.canMove(curLoc.directionTo(info.location))) {
        target = info.location;
        break;
      }
    }
    turnsLeft--;
    if (target != null) {
      rc.move(curLoc.directionTo(target));
    }
  }
}
