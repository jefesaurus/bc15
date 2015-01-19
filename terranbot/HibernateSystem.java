package terranbot;

import terranbot.RobotPlayer.BaseBot;
import terranbot.MovingBot;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class HibernateSystem {
  private static final int HIBERNATE_DISTANCE = 25;
  private static boolean TRYING_TO_HIBERNATE = false;
  private static int HIBERNATE_COUNT_DOWN = 5;
  private static RobotController rc;
  
  public static void init(RobotController rc) {
    HibernateSystem.rc = rc;
  }
  
  public static boolean manageHibernation(MovingBot.AttackMode mode, RobotInfo[] currentEnemies, MapLocation rallyPoint) throws GameActionException {
    if (mode == MovingBot.AttackMode.RALLYING || mode == MovingBot.AttackMode.DEFEND_TOWERS || mode == MovingBot.AttackMode.DEFENSIVE_SWARM) {
      MapLocation towerToHelp = Messaging.getClosestTowerUnderAttack();
      if (currentEnemies.length == 0 && towerToHelp == null && (Nav.dest == null || HibernateSystem.rc.getLocation().distanceSquaredTo(Nav.dest) <= HIBERNATE_DISTANCE)) {
        //Hibernate
        if (TRYING_TO_HIBERNATE) {
          if (HIBERNATE_COUNT_DOWN > 0) {
            HIBERNATE_COUNT_DOWN--;
          } else {
            return true;
          }
        } else {
          TRYING_TO_HIBERNATE = true;
        }
      } else {
        TRYING_TO_HIBERNATE = false;
        HIBERNATE_COUNT_DOWN = 5;
      }
    }
    return false;
  }
}
