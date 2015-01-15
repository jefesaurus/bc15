package ghetto_v2_5;

import battlecode.common.*;
import ghetto_v2_5.Cache;
import ghetto_v2_5.Util;
import ghetto_v2_5.Nav;
import ghetto_v2_5.BotTypes.Barracks;
import ghetto_v2_5.BotTypes.Beaver;
import ghetto_v2_5.BotTypes.Drone;
import ghetto_v2_5.BotTypes.HQ;
import ghetto_v2_5.BotTypes.Helipad;
import ghetto_v2_5.BotTypes.Miner;
import ghetto_v2_5.BotTypes.MinerFactory;
import ghetto_v2_5.BotTypes.Soldier;
import ghetto_v2_5.BotTypes.Tower;

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
    
    public RobotInfo[] getEnemiesInAttackingRangeHQ() {
      int range = RobotType.HQ.attackRadiusSquared;
      if (rc.senseTowerLocations().length >= 2) {
        range = 35;
      }
      RobotInfo[] enemies = rc.senseNearbyRobots(range, theirTeam);
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
      curLoc = rc.getLocation();
      curRound = Clock.getRoundNum();

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
	    SAFE_TOWER_DIVE,
	    UNSAFE_TOWER_DIVE,
	    DEFEND_TOWERS,
	    RALLYING,
	    HUNT_FOR_MINERS
    }
		  
    protected RobotInfo[] currentEnemies;
    protected int currentEnemiesRound = -1;
    
    protected int[] cachedNumAttackingEnemyDirs;
    protected double[] cachedEnemyDangerValsDirs;
    protected int[] cachedNumAttackingTowerDirs;
    protected boolean[] cachedAttackingHQDirs;
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
          cachedDangerVals[i] += attackingTowerDirs[i]*dangerVal;
        }

        // Do HQ
        dangerVal = Util.DANGER_VALUE_MAP[RobotType.HQ.ordinal()];
        boolean[] attackingHQDirs = calculateAttackingHQDirs();
        //rc.setIndicatorString(0, "HQ Dirs: " + Arrays.toString(attackingHQDirs));
        for (int i = attackingHQDirs.length; i-- > 0;) {
          if (attackingHQDirs[i]) {
            cachedDangerVals[i] += dangerVal;
          }
        }
      }
      
      return cachedDangerVals;
    }
    
    protected int[] calculateNumAttackingTowerDirs(MapLocation ignoreTower) throws GameActionException {
      if (cachedNumAttackingTowerDirs == null) {
        cachedNumAttackingTowerDirs = new int[9];
        MapLocation[] enemyTowers = Cache.getEnemyTowerLocationsDirect();

        int xdiff;
        int ydiff;
        for (int i = enemyTowers.length; i-- > 0;) {
          xdiff = enemyTowers[i].x - curLoc.x;
          ydiff = enemyTowers[i].y - curLoc.y;
          if (xdiff <= 5 && xdiff >= -5 && ydiff <= 5 && ydiff >= -5) {
            if (ignoreTower != null && enemyTowers[i].equals(ignoreTower)) {
              continue;
            }
            int[] attackedDirs = Util.ATTACK_NOTES[Util.RANGE_TYPE_MAP[RobotType.TOWER.ordinal()]][5 + xdiff][5 + ydiff];
            for (int j = attackedDirs.length; j-- > 0;) {
              cachedNumAttackingTowerDirs[attackedDirs[j]]++;
            }
          }
        }
        //rc.setIndicatorString(2, "Tower found: " + Arrays.toString(enemyTowers));

      }
      return cachedNumAttackingTowerDirs;
    }
    
    protected boolean[] calculateAttackingHQDirs() throws GameActionException {
      if (cachedAttackingHQDirs == null) {
        int curDist = curLoc.distanceSquaredTo(enemyHQ);
        int numEnemyTowers = Cache.getEnemyTowerLocationsDirect().length;
        cachedAttackingHQDirs = new boolean[9];
        if (numEnemyTowers >= 5) {
          
          if (curDist < 81) {
            int xdiff = curLoc.x - this.enemyHQ.x;
            int ydiff = curLoc.y - this.enemyHQ.y;
            int dx, dy;
            String dbug = "";
            for (int i = 9; i-- > 0;) {
              dx = xdiff + Util.DIR_DX[i];
              dy = ydiff + Util.DIR_DY[i];
              dx = (dx > 0) ? dx : -dx;
              dy = (dy > 0) ? dy : -dy;
              cachedAttackingHQDirs[i] = (dx <= 6 && dy <= 6 && dx + dy <= 10);
              dbug += Util.REGULAR_DIRECTIONS_WITH_NONE[i].name() + ": (" + dx + ", " + dy + ")," + cachedAttackingHQDirs[i] + ", ";
            }
            rc.setIndicatorString(2, dbug + "... Round: " + Clock.getRoundNum());

          }
        } else if (numEnemyTowers >= 2) {
          if (curDist < 49) {
            if (curDist <= 35) {
              cachedAttackingHQDirs[8] = true;
            }
            for (int i = 8; i-- > 0;) {
              cachedAttackingHQDirs[i] = curLoc.add(Util.REGULAR_DIRECTIONS[i]).distanceSquaredTo(enemyHQ) < 35;
            }
          }
        } else {
          if (curDist < 36) {
            if (curDist <= 24) {
              cachedAttackingHQDirs[8] = true;
            }
            for (int i = 8; i-- > 0;) {
              cachedAttackingHQDirs[i] = curLoc.add(Util.REGULAR_DIRECTIONS[i]).distanceSquaredTo(enemyHQ) < 24;
            }
          }
        }
      }
      return cachedAttackingHQDirs;
    }
  }
}
