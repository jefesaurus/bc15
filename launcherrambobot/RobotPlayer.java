package launcherrambobot;

import battlecode.common.*;

import java.util.*;

import launcherrambobot.Cache;
import launcherrambobot.Nav;
import launcherrambobot.Util;
import launcherrambobot.BotTypes.AerospaceLab;
import launcherrambobot.BotTypes.Barracks;
import launcherrambobot.BotTypes.Beaver;
import launcherrambobot.BotTypes.Commander;
import launcherrambobot.BotTypes.Drone;
import launcherrambobot.BotTypes.HQ;
import launcherrambobot.BotTypes.Helipad;
import launcherrambobot.BotTypes.Launcher;
import launcherrambobot.BotTypes.Miner;
import launcherrambobot.BotTypes.MinerFactory;
import launcherrambobot.BotTypes.Soldier;
import launcherrambobot.BotTypes.Tank;
import launcherrambobot.BotTypes.TankFactory;
import launcherrambobot.BotTypes.Tower;
import launcherrambobot.BotTypes.TrainingField;

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
    case TANKFACTORY:
      myself = new TankFactory(rc);
      break;
    case TANK:
      myself = new Tank(rc);
      break;
    case TRAININGFIELD:
      myself = new TrainingField(rc);
      break;
    case AEROSPACELAB:
      myself = new AerospaceLab(rc);
      break;
    case LAUNCHER:
      myself = new Launcher(rc);
      break;
    case COMMANDER:
      myself = new Commander(rc);
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
      //System.out.println("Done building; " +rc.getType() );
      if (rc.getType() != RobotType.MISSILE) {
        Messaging.announceDoneBuilding(rc.getType());
      }
    }
    
    public void beginningOfTurn() throws GameActionException {
      updateRoundVariables();
      if (rc.getType() != RobotType.MISSILE) {
        Messaging.announceUnit(rc.getType());
      }
    }

    public void endOfTurn() throws GameActionException {
    }

    public void go() throws GameActionException {
      beginningOfTurn();
      execute();
      endOfTurn();
      rc.yield();
    }

    public void execute() throws GameActionException {
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
}
