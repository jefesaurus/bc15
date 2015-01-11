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

    switch (rc.getType()) {
    case HQ:
      myself = new HQ(rc);
      break;
    case BEAVER:
      myself = new Beaver(rc);
      break;
    case BARRACKS:
      myself = new Barracks(rc);
      break;
    case SOLDIER:
      myself = new Soldier(rc);
      break;
    case TOWER:
      myself = new Tower(rc);
      break;
    case MINERFACTORY:
      myself  = new MinerFactory(rc);
      break;
    case MINER:
      myself = new Miner(rc);
      break;
    default:
      myself = new BaseBot(rc);
      break;
    }
    
    // Initialize internal stuff
    myself.init();
    
    // Meant to be overridden by subclasses
    myself.setup();

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
    public MapLocation[] enemyTowers;
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
      this.enemyTowers = rc.senseEnemyTowerLocations();
    }
    
    public void init() throws GameActionException {
      updateRoundVariables();
      Messaging.init(this);
      Cache.init(this);
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
      RobotInfo[] enemies = rc.senseNearbyRobots(rc.getType().attackRadiusSquared, theirTeam);
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

    // Override this in subclasses to do class specific setups procedures(only called once).
    // This is different from init, which sets up Nav and Messaging type stuff.
    public void setup() throws GameActionException {
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
  
  public static class MovingBot extends BaseBot {
    protected RobotInfo[] currentEnemies;
    protected int currentEnemiesRound = -1;
    
    protected int[] cachedNumAttackingEnemyDirs;
    protected int[] cachedNumAttackingTowerDirs;
    
    public MovingBot(RobotController rc) {
      super(rc);
    }
    
    public void init() throws GameActionException {
      Nav.init(this);
      super.init();
    }
    
    public void beginningOfTurn() {
      cachedNumAttackingEnemyDirs = null;
      cachedNumAttackingTowerDirs = null;

      super.beginningOfTurn();
    }
    
    public boolean canMoveSafely(Direction dir, boolean moveInForTower) throws GameActionException {
      if (rc.canMove(dir)) {
        //int bc = Clock.getBytecodeNum();
        int[] numAttackingEnemyDirs = calculateNumAttackingEnemyDirs();
        //System.out.println(Clock.getBytecodeNum() - bc);
        //bc = Clock.getBytecodeNum();
        if (numAttackingEnemyDirs[dir.ordinal()] == 0) {
          return rc.canMove(dir);
        }
      }
      return false;
    }
    
    // Doesn't count towers or HQ
    protected int[] calculateNumAttackingEnemyDirs() throws GameActionException {
      if (cachedNumAttackingEnemyDirs == null) {
        cachedNumAttackingEnemyDirs = new int[8];
        RobotInfo[] visibleEnemies = Cache.getVisibleEnemies();
        for (int i = visibleEnemies.length; i-- > 0;) {
          if (visibleEnemies[i].type == RobotType.TOWER || visibleEnemies[i].type == RobotType.HQ) {
            continue;
          }
          MapLocation enemyLoc = visibleEnemies[i].location;
          int[] attackedDirs = Util.ATTACK_NOTES[visibleEnemies[i].type.ordinal()][5 + enemyLoc.x - curLoc.x][5 + enemyLoc.y - curLoc.y];
          for (int j = attackedDirs.length; j-- > 0;) {
            cachedNumAttackingEnemyDirs[attackedDirs[j]]++;
          }
        }
      }

      return cachedNumAttackingEnemyDirs;
    }
    
    protected int[] calculateNumAttackingTowerDirs() throws GameActionException {
      if (cachedNumAttackingTowerDirs == null) {
        cachedNumAttackingTowerDirs = new int[8];
        MapLocation[] enemyTowers = Cache.getEnemyTowerLocations();
        
        int xdiff;
        int ydiff;
        for (int i = enemyTowers.length; i-- > 0;) {
          if (enemyTowers[i] == null) {
            // Tower wasn't there or is dead.
            continue;
          }
          xdiff = enemyTowers[i].x - curLoc.x;
          ydiff = enemyTowers[i].y - curLoc.y;
          if (xdiff <= 5 && xdiff >= -5 && ydiff <= 5 && ydiff >= -5) {
            int[] attackedDirs = Util.ATTACK_NOTES[RobotType.TOWER.ordinal()][5 + xdiff][5 + ydiff];
            for (int j = attackedDirs.length; j-- > 0;) {
              cachedNumAttackingTowerDirs[attackedDirs[j]]++;
            }
          }
        }
      }
      return cachedNumAttackingTowerDirs;
    }
  }
}
