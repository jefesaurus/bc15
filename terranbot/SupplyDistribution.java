package terranbot;

import terranbot.RobotPlayer.BaseBot;
import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class SupplyDistribution {
  private static RobotController rc;
  private static BaseBot br;
  private static SupplyDistributionMode mode;
  private static final int minSupplyLaunch = 5000;
  private static final int minSupplyMiner = 5000;
  private static final int minSupplyBattle = 500;
  private static enum SupplyDistributionMode {
    //Pool supply at HQ
    NO_TRANSFER,
    //Distribute supply to surrounding attacking units
    BATTERY,
    //HQ give supply to reinforcement to eventually distribute
    POOL_REINFORCEMENT,
    //HQ give supply to worker/miner
    POOL_WORKER
  }
  
  public static void init(BaseBot br) {
    SupplyDistribution.br = br;
    SupplyDistribution.rc = br.rc;
    mode = SupplyDistributionMode.POOL_WORKER;
  }
  
  public static void setBatteryMode() {
    mode = SupplyDistributionMode.BATTERY;
  }
  
  public static void setReinforcementMode() {
    mode = SupplyDistributionMode.POOL_REINFORCEMENT;
  }
  
  public static void setWorkerMode() {
    mode = SupplyDistributionMode.POOL_WORKER;
  }
  
  public static void disable() {
    mode = SupplyDistributionMode.NO_TRANSFER;
  }
  
  public static void manageSupply() throws GameActionException {
    switch (mode) {
    case BATTERY:
      if (rc.getType() == RobotType.HQ) {
        distributeBatteryHQ();
      } else {
        distributeBatteryUnit();
      }
      break;
    case POOL_REINFORCEMENT:
      distributeReinforcement();
      break;
    case POOL_WORKER:
      distributeWorker();
      break;
    default:
      break;
    }
  }
  
  public static void distributeBatteryHQ() throws GameActionException {
    RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED, rc.getTeam());
    for (int i=robots.length; i-- > 0;) {
      if (Clock.getBytecodesLeft() < 550) {
        return;
      }
      RobotInfo info = robots[i];
      if (info.type == RobotType.DRONE || info.type == RobotType.TANK && info.supplyLevel < minSupplyLaunch*2/3) {
        rc.transferSupplies((int) (minSupplyLaunch - info.supplyLevel), info.location);
      } else if (info.type == RobotType.MINER && info.supplyLevel < minSupplyMiner*2/3) {
        rc.transferSupplies((int) (minSupplyMiner - info.supplyLevel), info.location);
      }
    }
  }
  
  public static void distributeBatteryUnit() throws GameActionException {
    int supplyToTransfer = (int) (rc.getSupplyLevel() - 2 * minSupplyBattle);
    
    if (supplyToTransfer <= 0) {
      return;
    }
    
    RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED, rc.getTeam());
    for (int i=robots.length-1; i >= 0 && supplyToTransfer > 0; i--) {
      if (Clock.getBytecodesLeft() < 500) {
        return;
      }
      RobotInfo info = robots[i];
      if (info.type == RobotType.DRONE && info.supplyLevel < minSupplyBattle*2/3) {
        int x = (int) Math.min(supplyToTransfer, minSupplyBattle - info.supplyLevel);
        rc.transferSupplies(x, info.location);
        supplyToTransfer -= x;
      } 
    }
  }
  
  public static void distributeReinforcement() {
  }
  
  public static void distributeWorker() {
  
  }
  
  
  
}
