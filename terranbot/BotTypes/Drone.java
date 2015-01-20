package terranbot.BotTypes;

import terranbot.Cache;
import terranbot.HibernateSystem;
import terranbot.Messaging;
import terranbot.Nav;
import terranbot.SupplyDistribution;
import terranbot.Nav.Engage;
import terranbot.RobotPlayer.BaseBot;
import terranbot.MovingBot;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class Drone extends MovingBot {   
  
  public Drone(RobotController rc) {
    super(rc);
    SupplyDistribution.init(this);
    SupplyDistribution.setBatteryMode();
    HibernateSystem.init(rc);
  }

  protected MapLocation rallyPoint = null;
  protected MovingBot.AttackMode mode = MovingBot.AttackMode.OFFENSIVE_SWARM;
  public int defendingTowerIndex = -1;
  public int rankSpot = Integer.MAX_VALUE;

  public static final double HALF_ANGLE_WIDTH = 3.14159/3.0;
  
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
    case OFFENSIVE_SWARM:
      doOffensiveMicro(currentEnemies, rallyPoint);
      break;
    case RALLYING:
    case FORM_UP:
      for (int i = 7; i-- > 0;) {
        if (Messaging.rankIsAttacking(i)) {
          int[] diff = Messaging.getRankTarget(i);
          MapLocation center = Messaging.getRankCenter(i);
          MapLocation target = new MapLocation(center.x + diff[0], center.y + diff[1]);
          droneAllInMicro(currentEnemies, target);
          return;
        }
      }
      rc.setIndicatorString(1, "No ranks attacking: " + Clock.getRoundNum());

      
      if (defendingTowerIndex < 0) {
        defendingTowerIndex = Messaging.getLeastDefendedTowerIndex();
      }
      if (defendingTowerIndex >= 0) {
        if (rankSpot == Integer.MAX_VALUE || !Messaging.refreshRankSpot(defendingTowerIndex, rankSpot)) {
          rankSpot = Messaging.claimRankSpot(defendingTowerIndex);
        }
        if (rankSpot >= 0) {
          // Now we should have a tower to defend and a spot in the rank reserved.
          int[] diff = Messaging.getRankTarget(defendingTowerIndex);
          int offset = rankSpot/2 + 1;
          rc.setIndicatorString(0, "Spot: " + rankSpot + ", Offset: " + offset + ", Target: " + diff[0] + ", " + diff[1]);
    
          MapLocation center = Messaging.getRankCenter(defendingTowerIndex);
          double angle = Math.atan2(diff[0], diff[1]);
          if (rankSpot % 2 == 0) {
            angle += HALF_ANGLE_WIDTH;
          } else {
            angle -= HALF_ANGLE_WIDTH;
          }
          
          MapLocation mySpot = getLineSpot(center, offset, (int)(Math.cos(angle)*30), (int)(Math.sin(angle)*30), false);
    
          if (!curLoc.equals(mySpot)) {
            Nav.goTo(mySpot, Engage.NONE);
          } else {
            RobotInfo[] attackableEnemies = Cache.getAttackableEnemies();
            if (attackableEnemies.length > 0) {
              if (rc.isWeaponReady()) {
                attackLeastHealthEnemy(attackableEnemies);
              }
            }
          }
        } else {
          MapLocation center = Messaging.getRankCenter(defendingTowerIndex);
          int[] diff = Messaging.getRankTarget(defendingTowerIndex);
          MapLocation mySpot = getLineSpot(center, 5, -diff[0], -diff[1], false);
          Nav.goTo(mySpot, Engage.NONE);
        }
      } else {
        RobotInfo[] attackableEnemies = Cache.getAttackableEnemies();
        if (attackableEnemies.length > 0) {
          if (rc.isWeaponReady()) {
            attackLeastHealthEnemy(attackableEnemies);
          }
        }
      }
      break;
    case DEFEND_TOWERS:
    case DEFENSIVE_SWARM:
      doDefensiveMicro(currentEnemies, rallyPoint);
      break;
    default:
      System.out.println("No default behavior");
      break;
    }
  }
}
