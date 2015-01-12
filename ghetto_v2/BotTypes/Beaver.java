package ghetto_v2.BotTypes;

import ghetto_v2.Messaging;
import ghetto_v2.Nav;
import ghetto_v2.RobotPlayer.BaseBot;
import ghetto_v2.RobotPlayer.MovingBot;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class Beaver extends MovingBot {
  public static final int MINING_HORIZON = 5;
  public static int MINING_TURNS = 0;
  public static int beaverId;
  public static boolean minerFactoryBuilt = false;
  public static final Direction[] directions = Direction.values();

  public Beaver(RobotController rc) throws GameActionException {
    super(rc);
  }
  
  public void setup() throws GameActionException {
    beaverId = Messaging.announceBeaver();
  }

  public void execute() throws GameActionException {

    if (beaverId == 1 && minerFactoryBuilt == false) {
      for (int i=0; i<8; i++) {
        if (rc.canMove(directions[i]) && rc.hasBuildRequirements(RobotType.MINERFACTORY) && rc.isCoreReady()) {
          rc.build(directions[i], RobotType.MINERFACTORY);
          minerFactoryBuilt = true;
        }
      }
    } else if (rc.isCoreReady()) {
      if (rc.getTeamOre() < 500) {
        // mine
        if (MINING_TURNS > 0) {
          if (rc.isCoreReady()) {
            MINING_TURNS--;
            rc.mine();
          }
        } else {
          double curAmount = getOreAmount(this.curLoc, MINING_HORIZON);
          double maxAmount = curAmount;
          MapLocation bestLoc = this.curLoc;
          int numMaxes = 1;
          for (int i=0; i<8; i++) {
            if (rc.canMove(directions[i])) {
              MapLocation trialLoc = this.curLoc.add(directions[i]);
              double adjAmount = getOreAmount(trialLoc, MINING_HORIZON - 1);
              if (maxAmount < adjAmount) {
                maxAmount = adjAmount;
                bestLoc = trialLoc;
                numMaxes = 1;
              } else if (maxAmount == adjAmount) {
                numMaxes += 1;
                if (Math.random() > 1.0 / numMaxes) {
                  bestLoc = trialLoc;
                }
              }
            }
          }
          
          if (maxAmount == curAmount) {
            bestLoc = this.curLoc;
          }
          
          if (bestLoc == this.curLoc && rc.isCoreReady()) {
            this.MINING_TURNS = MINING_HORIZON;
            rc.mine();
          }
          
          if (bestLoc != this.curLoc && rc.isCoreReady()) {
            this.MINING_TURNS = MINING_HORIZON;
            rc.move(getMoveDir(bestLoc));
          }
        }
      } else {
        // build helipad
        Direction newDir = getBuildDirection(RobotType.HELIPAD);
        if (newDir != null) {
          rc.build(newDir, RobotType.HELIPAD);
        }
      }
    }

    rc.yield();
  }
  
  public double getOreAmount(MapLocation loc, int horizon) {
    double startAmount = rc.senseOre(loc);
    double currentAmount = startAmount;
    double total = 0;
    for (int i=0; i<horizon; i++) {
      double amountMined = Math.max(
                     Math.min(GameConstants.BEAVER_MINE_MAX, currentAmount/GameConstants.BEAVER_MINE_RATE), 
                     GameConstants.MINIMUM_MINE_AMOUNT);
      currentAmount -= amountMined; 
      total += amountMined;
    }
    return total;
  }
}
