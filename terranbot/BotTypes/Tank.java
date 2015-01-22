package terranbot.BotTypes;

import terranbot.Cache;
import terranbot.HibernateSystem;
import terranbot.Messaging;
import terranbot.MovingBot;
import terranbot.Nav;
import terranbot.SupplyDistribution;
import terranbot.Nav.Engage;
import terranbot.RobotPlayer.BaseBot;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class Tank extends MovingBot {  
  
  public Tank(RobotController rc) {
    super(rc);
    SupplyDistribution.init(this);
    SupplyDistribution.setBatteryMode();
    HibernateSystem.init(rc);
  }

  protected MapLocation rallyPoint = null;
  protected MovingBot.AttackMode mode = MovingBot.AttackMode.OFFENSIVE_SWARM;
  public int rankIndex = -1;

  public void execute() throws GameActionException {
    currentEnemies = Cache.getEngagementEnemies();
    
    rallyPoint = Messaging.readRallyPoint();
    mode = Messaging.getFleetMode();
    if (HibernateSystem.manageHibernation(mode, currentEnemies, rallyPoint)) {
      return;
    }
    rc.setIndicatorString(2, "Mode: " + mode.name() + ", Rally point: " + rallyPoint);
    SupplyDistribution.manageSupply();

    switch (mode) {
    case SAFE_TOWER_DIVE:
      if (currentEnemies.length > 0) {
        RobotInfo[] attackableEnemies = Cache.getAttackableEnemies();
        if (attackableEnemies.length > 0) {
          if (rc.isWeaponReady()) {
            if (rc.canAttackLocation(rallyPoint)) {
              rc.attackLocation(rallyPoint);
            } else {
              attackLeastHealthEnemy(attackableEnemies);
            }
          }
        } else {
          if (rc.isCoreReady() && rallyPoint != null) {
            Nav.goTo(rallyPoint, Engage.ONE_TOWER);
          }
        }
      } else if (rc.isCoreReady()) {
        if (rallyPoint != null) {
          Nav.goTo(rallyPoint, Engage.ONE_TOWER);
        }
      }
      break;
    case UNSAFE_TOWER_DIVE:
      if (currentEnemies.length > 0) {
        RobotInfo[] attackableEnemies = Cache.getAttackableEnemies();
        if (attackableEnemies.length > 0) {
          if (rc.isWeaponReady()) {
            if (rc.canAttackLocation(rallyPoint)) {
              rc.attackLocation(rallyPoint);
            } else {
              attackLeastHealthEnemy(attackableEnemies);
            }
          }
        } else {
          if (rc.isCoreReady() && rallyPoint != null) {
            Nav.goTo(rallyPoint, Engage.ALL_TOWERS);
          }
        }
      } else if (rc.isCoreReady()) {
        if (rallyPoint != null) {
          Nav.goTo(rallyPoint, Engage.ALL_TOWERS);
        }
      }
      break;
    case RALLYING:
    case OFFENSIVE_SWARM:
    case DEFEND_TOWERS:
    case DEFENSIVE_SWARM:
      if (currentEnemies.length > 0) {
        doOffensiveMicro(currentEnemies, rallyPoint);
      } else {
        if (rankIndex < 0 || !Messaging.rankIsActive(rankIndex)) {
          rankIndex = Messaging.getActiveRank();
        }
        if (rankIndex >= 0) {
          rc.setIndicatorString(0, "Rank Found");

          //System.out.println("Active rank");

          Messaging.addToRank(rankIndex);
        
          // Now we should have a tower to defend and a spot in the rank reserved.
          MapLocation target = Messaging.getRankTarget(rankIndex);
          MapLocation center = Messaging.getRankCenter(rankIndex);
          int width = Messaging.getRankWidth(rankIndex);
          if (width == 0) {
            width = 5;
          }

          int dx = target.x - center.x;
          int dy = target.y - center.y;
          double midX = (target.x + center.x)/2.0;
          double midY = (target.y + center.y)/2.0;
          double length = Math.sqrt(dx*dx + dy*dy);
          MapLocation ep1, ep2;
          if (dy == 0) {
            ep1 = new MapLocation((int)midX, (int)(midY + width/2));
            ep2 = new MapLocation((int)midX, (int)(midY - width/2));
          } else if (dx == 0) {
            ep1 = new MapLocation((int)(midX + width/2), (int)midY);
            ep2 = new MapLocation((int)(midX - width/2), (int)midY);
          } else {
            double xW = width*dy/(2*length);
            double yW = width*dx/(2*length);

            ep1 = new MapLocation((int)(midX + xW/2), (int)(midY + yW/2));
            ep2 = new MapLocation((int)(midX - xW/2), (int)(midY - yW/2));
          }
          
          int testVecX = ep2.x - ep1.x;
          int testVecY = ep2.y - ep1.y;

          
          double targetMidDist = (testVecX*(target.y - ep1.y) - testVecY*(target.x - ep1.x))/width;
          double currentMidDist = (testVecY*(curLoc.y - ep1.y) - testVecY*(curLoc.x - ep1.x))/width;
          // If behind our centroid.
          if ((currentMidDist > 0) != (targetMidDist > 0)) {
            rc.setIndicatorString(0, "Behind the line");
            testVecX = target.x - center.x;
            testVecY = target.y - center.y;
            boolean ep1SignCenter = (testVecX*(ep1.y - center.y) - testVecY*(ep1.x - center.x)) > 0;
            if ((testVecX*(curLoc.y - center.y) - testVecY*(curLoc.x - center.x)) > 0 == ep1SignCenter) {
              Nav.goTo(ep1, Engage.NONE);
            } else {
              Nav.goTo(ep2, Engage.NONE);
            }
          } else {
            rc.setIndicatorString(0, "In front of the line");
            double destCoeff = currentMidDist / length;
            Nav.goTo(new MapLocation((int)(midX + dx*destCoeff), (int)(midY + dy*destCoeff)), Engage.UNITS);
          }          
        } else {
          rc.setIndicatorString(0, "No Rank");
          Nav.goTo(rallyPoint, Engage.NONE);
        }
      }
      break;
    default:
      System.out.println("No default behavior");
      break;
    }
  }
}
