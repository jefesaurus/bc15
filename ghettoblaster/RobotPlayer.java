package ghettoblaster;

import battlecode.common.*;
import ghettoblaster.BotTypes.Barracks;
import ghettoblaster.BotTypes.Beaver;
import ghettoblaster.BotTypes.HQ;
import ghettoblaster.BotTypes.Miner;
import ghettoblaster.BotTypes.MinerFactory;
import ghettoblaster.BotTypes.Soldier;
import ghettoblaster.BotTypes.Tower;
import ghettoblaster.Nav;

import java.util.*;

public class RobotPlayer {
  public static void run(RobotController rc) throws GameActionException {
    BaseBot myself;

    if (rc.getType() == RobotType.HQ) {
      myself = new HQ(rc);
    } else if (rc.getType() == RobotType.BEAVER) {
      myself = new Beaver(rc);
    } else if (rc.getType() == RobotType.BARRACKS) {
      myself = new Barracks(rc);
    } else if (rc.getType() == RobotType.SOLDIER) {
      myself = new Soldier(rc);
    } else if (rc.getType() == RobotType.TOWER) {
      myself = new Tower(rc);
    } else if (rc.getType() == RobotType.MINERFACTORY) {
      myself  = new MinerFactory(rc);
    } else if (rc.getType() == RobotType.MINER) {
      myself = new Miner(rc);
    } else {
      myself = new BaseBot(rc);
    }

    while (true) {
      try {
        myself.go();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public static class BaseBot {
    public RobotController rc;
    protected MapLocation myHQ;
    public MapLocation enemyHQ;
    protected Team myTeam, theirTeam;

    // Updated per turn
    public MapLocation curLoc;
    public int curRound;

    public BaseBot(RobotController rc) {
      this.rc = rc;
      this.myHQ = rc.senseHQLocation();
      this.enemyHQ = rc.senseEnemyHQLocation();
      this.myTeam = rc.getTeam();
      this.theirTeam = this.myTeam.opponent();
      Nav.init(this);
    }

    public Direction[] getDirectionsToward(MapLocation dest) {
      Direction toDest = rc.getLocation().directionTo(dest);
      Direction[] dirs = { toDest, toDest.rotateLeft(), toDest.rotateRight(),
          toDest.rotateLeft().rotateLeft(), toDest.rotateRight().rotateRight() };

      return dirs;
    }

    public Direction getMoveDir(MapLocation dest) {
      Direction[] dirs = getDirectionsToward(dest);
      for (Direction d : dirs) {
        if (rc.canMove(d)) {
          return d;
        }
      }
      return null;
    }

    public Direction getSpawnDirection(RobotType type) {
      Direction[] dirs = getDirectionsToward(this.enemyHQ);
      for (Direction d : dirs) {
        if (rc.canSpawn(d, type)) {
          return d;
        }
      }
      return null;
    }

    public Direction getBuildDirection(RobotType type) {
      Direction[] dirs = getDirectionsToward(this.enemyHQ);
      for (Direction d : dirs) {
        if (rc.canBuild(d, type)) {
          return d;
        }
      }
      return null;
    }

    public RobotInfo[] getAllies() {
      RobotInfo[] allies = rc.senseNearbyRobots(Integer.MAX_VALUE, myTeam);
      return allies;
    }

    public RobotInfo[] getEnemiesInAttackingRange() {
      RobotInfo[] enemies = rc.senseNearbyRobots(
          RobotType.SOLDIER.attackRadiusSquared, theirTeam);
      return enemies;
    }

    public void attackLeastHealthEnemy(RobotInfo[] enemies)
        throws GameActionException {
      if (enemies.length == 0) {
        return;
      }

      double minEnergon = Double.MAX_VALUE;
      MapLocation toAttack = null;
      for (RobotInfo info : enemies) {
        if (info.health < minEnergon) {
          toAttack = info.location;
          minEnergon = info.health;
        }
      }

      rc.attackLocation(toAttack);
    }

    public void beginningOfTurn() {
      updateRoundVariables();
    }

    public void endOfTurn() {
    }

    public void go() throws GameActionException {
      beginningOfTurn();
      execute();
      endOfTurn();
    }

    public void execute() throws GameActionException {
      rc.yield();
    }

    public void updateRoundVariables() {
      curRound = Clock.getRoundNum();
      curLoc = rc.getLocation();
    }
  }
}
