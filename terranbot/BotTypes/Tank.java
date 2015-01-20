package terranbot.BotTypes;

import java.util.Arrays;

import com.sun.xml.internal.bind.v2.runtime.unmarshaller.XsiNilLoader.Array;

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

public class Tank extends MovingBot {  
  
  public Tank(RobotController rc) {
    super(rc);
    SupplyDistribution.init(this);
    SupplyDistribution.setBatteryMode();
    HibernateSystem.init(rc);
  }

  protected MapLocation rallyPoint = null;
  protected MovingBot.AttackMode mode = MovingBot.AttackMode.OFFENSIVE_SWARM;
  public int defendingTowerIndex = -1;
  public int rankSpot = Integer.MAX_VALUE;

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
      if (defendingTowerIndex < 0) {
        defendingTowerIndex = Messaging.getLeastDefendedTowerIndex();
      }
      
      if (rankSpot == Integer.MAX_VALUE || !Messaging.refreshRankSpot(defendingTowerIndex, rankSpot)) {
        rankSpot = Messaging.claimRankSpot(defendingTowerIndex);
      }
      
      // Now we should have a tower to defend and a spot in the rank reserved.
      int[] diff = Messaging.getRankTarget(defendingTowerIndex);
      int offset = rankSpot/2 + 1;
      rc.setIndicatorString(0, "Spot: " + rankSpot + ", Offset: " + offset + ", Target: " + diff[0] + ", " + diff[1]);

      MapLocation center = Messaging.getRankCenter(defendingTowerIndex);
      MapLocation mySpot = getLineSpot(center, offset, -diff[1], diff[0], (rankSpot % 2 == 0));

      if (!curLoc.equals(mySpot)) {
        Nav.goTo(mySpot, Engage.NONE);
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
