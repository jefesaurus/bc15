package ghetto_v2_miners;

import battlecode.common.*;
import ghetto_v2_miners.Nav;
import ghetto_v2_miners.BotTypes.Barracks;
import ghetto_v2_miners.BotTypes.Beaver;
import ghetto_v2_miners.BotTypes.Drone;
import ghetto_v2_miners.BotTypes.HQ;
import ghetto_v2_miners.BotTypes.Helipad;
import ghetto_v2_miners.BotTypes.Miner;
import ghetto_v2_miners.BotTypes.MinerFactory;
import ghetto_v2_miners.BotTypes.Soldier;
import ghetto_v2_miners.BotTypes.Tower;

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
    case HELIPAD:
        myself = new Helipad(rc);
        break;
    case DRONE:
    	myself = new Drone(rc);
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
    public MapLocation[] myTowers;
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
      this.myTowers = rc.senseTowerLocations();
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

    public Direction getOffensiveSpawnDirection(RobotType type) {
      Direction[] dirs = getDirectionsToward(this.enemyHQ);
      for (Direction d : dirs) {
        if (rc.canSpawn(d, type)) {
          return d;
        }
      }
      return null;
    }
    
    public Direction getDefensiveSpawnDirection(RobotType type) {
      Direction[] dirs = getDirectionsToward(this.myHQ);
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
    
    public RobotInfo[] getVisibleEnemies() {
      RobotInfo[] enemies = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, theirTeam);
      return enemies;
    }

    public void attackLeastHealthEnemy(RobotInfo[] enemies) throws GameActionException {
      if (enemies.length == 0) {
        return;
      }

      double minEnergon = Double.MAX_VALUE;
      MapLocation toAttack = null;
      for (int i = enemies.length; i-- > 0;) {
        if (enemies[i].health < minEnergon) {
          toAttack = enemies[i].location;
          minEnergon = enemies[i].health;
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
    
    public boolean roundChanged() {
      if (Clock.getRoundNum() != curRound) {
        return true;
      }
      return false;
    }
  }
  
  public static class MovingBot extends BaseBot {
	  
    public enum AttackMode {
	    OFFENSIVE_SWARM,
	    DEFENSIVE_SWARM,
	    CONCAVE,
	    TOWER_DIVE,
	    DEFEND_TOWERS,
	    RALLYING,
	    HUNT_FOR_MINERS
    }
		  
    protected RobotInfo[] currentEnemies;
    protected int currentEnemiesRound = -1;
    
    protected int[] cachedNumAttackingEnemyDirs;
    protected double[] cachedEnemyDangerValsDirs;
    protected int[] cachedNumAttackingTowerDirs;
    protected int[] cachedAttackingHQDirs;
    protected double[] cachedDangerVals;
    
    public MovingBot(RobotController rc) {
      super(rc);
    }
    
    public void init() throws GameActionException {
      Nav.init(this);

      // Hacky crap to get this huge chunk of bytecode out of the way.
      int[] i = Util.ATTACK_NOTES[0][0][0];
      super.init();
    }
    
    public void beginningOfTurn() {
      // Moving enemies only
      cachedNumAttackingEnemyDirs = null;
      cachedEnemyDangerValsDirs = null;
      
      // Number of enemy towers onl
      cachedNumAttackingTowerDirs = null;
      
      // Enemy HQ only
      cachedAttackingHQDirs = null;
      
      // All of the above
      cachedDangerVals = null;

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
        cachedNumAttackingEnemyDirs = new int[9];
        RobotInfo[] visibleEnemies = Cache.getEngagementEnemies();
        for (int i = visibleEnemies.length; i-- > 0;) {
          if (visibleEnemies[i].type == RobotType.TOWER || visibleEnemies[i].type == RobotType.HQ) {
            continue;
          }
          MapLocation enemyLoc = visibleEnemies[i].location;
          int[] attackedDirs = Util.ATTACK_NOTES[Util.RANGE_TYPE_MAP[visibleEnemies[i].type.ordinal()]][5 + enemyLoc.x - curLoc.x][5 + enemyLoc.y - curLoc.y];
          for (int j = attackedDirs.length; j-- > 0;) {
            cachedNumAttackingEnemyDirs[attackedDirs[j]]++;
          }
        }
      }
      return cachedNumAttackingEnemyDirs;
    }
    
    protected double[] calculateEnemyDangerValsDirs() throws GameActionException {
      if (cachedEnemyDangerValsDirs == null) {
        cachedEnemyDangerValsDirs = new double[9];
        RobotInfo[] visibleEnemies = Cache.getEngagementEnemies();
        int enemyType;
        double dangerVal;
        for (int i = visibleEnemies.length; i-- > 0;) {
          enemyType = visibleEnemies[i].type.ordinal();
          
          // Ordinal values of HQ and Tower
          if (enemyType == 1 || enemyType == 0) {
            continue;
          }
          MapLocation enemyLoc = visibleEnemies[i].location;
          dangerVal = (visibleEnemies[i].supplyLevel > 0) ? Util.DANGER_VALUE_MAP[enemyType] : Util.DANGER_VALUE_MAP[enemyType]/2;
          int[] attackedDirs = Util.ATTACK_NOTES[Util.RANGE_TYPE_MAP[enemyType]][5 + enemyLoc.x - curLoc.x][5 + enemyLoc.y - curLoc.y];
          for (int j = attackedDirs.length; j-- > 0;) {
            cachedEnemyDangerValsDirs[attackedDirs[j]] += dangerVal;
          }
        }
      }
      return cachedEnemyDangerValsDirs;
    }
    
    protected double[] getAllDangerVals() throws GameActionException {
      if (cachedDangerVals == null) {
        cachedDangerVals = calculateEnemyDangerValsDirs();
        
        // Do Towers
        double dangerVal = Util.DANGER_VALUE_MAP[RobotType.TOWER.ordinal()];
        int[] attackingTowerDirs = calculateNumAttackingTowerDirs(null);
        for (int i = attackingTowerDirs.length; i-- > 0;) {
          cachedDangerVals[attackingTowerDirs[i]] += dangerVal;
        }

        // Do HQ
        dangerVal = Util.DANGER_VALUE_MAP[RobotType.HQ.ordinal()];
        int[] attackingHQDirs = calculateAttackingHQDirs();
        for (int i = attackingHQDirs.length; i-- > 0;) {
          cachedDangerVals[attackingHQDirs[i]] += dangerVal;
        }
      }

      return cachedDangerVals;
    }
    
    protected int[] calculateNumAttackingTowerDirs(MapLocation ignoreTower) throws GameActionException {
      if (cachedNumAttackingTowerDirs == null) {
        cachedNumAttackingTowerDirs = new int[9];
        MapLocation[] enemyTowers = Cache.getEnemyTowerLocations();

        int xdiff;
        int ydiff;
        for (int i = enemyTowers.length; i-- > 0;) {
          if (enemyTowers[i] == null || (ignoreTower != null && enemyTowers[i].x == ignoreTower.x && enemyTowers[i].y == ignoreTower.y)) {
            continue;
          }

          xdiff = enemyTowers[i].x - curLoc.x;
          ydiff = enemyTowers[i].y - curLoc.y;
          if (xdiff <= 5 && xdiff >= -5 && ydiff <= 5 && ydiff >= -5) {
            int[] attackedDirs = Util.ATTACK_NOTES[Util.RANGE_TYPE_MAP[RobotType.TOWER.ordinal()]][5 + xdiff][5 + ydiff];
            for (int j = attackedDirs.length; j-- > 0;) {
              cachedNumAttackingTowerDirs[attackedDirs[j]]++;
            }
          }
        }
      }
      return cachedNumAttackingTowerDirs;
    }
    
    protected int[] calculateAttackingHQDirs() throws GameActionException {
      if (cachedAttackingHQDirs == null) {
        cachedAttackingHQDirs = new int[9];
        int xdiff, ydiff;

        xdiff = this.enemyHQ.x - curLoc.x;
        ydiff = this.enemyHQ.y - curLoc.y;
        if (xdiff <= 5 && xdiff >= -5 && ydiff <= 5 && ydiff >= -5) {
          int[] attackedDirs = Util.ATTACK_NOTES[Util.RANGE_TYPE_MAP[RobotType.HQ.ordinal()]][5 + xdiff][5 + ydiff];
          for (int j = attackedDirs.length; j-- > 0;) {
            cachedAttackingHQDirs[attackedDirs[j]]++;
          }
        }
      }
      return cachedAttackingHQDirs;
    }
  }
}
