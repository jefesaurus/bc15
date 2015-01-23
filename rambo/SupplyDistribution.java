package rambo;

import rambo.RobotPlayer.BaseBot;
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
  private static final int minSupplyLaunch = 10000;
  private static final int minSupplyBattle = 100;
  private static int minSupplyMiner = 14000;
  private static int minSupplyMinerWorking = 100;
  private static enum SupplyDistributionMode {
    //Pool supply at HQ
    NO_TRANSFER,
    //Distribute supply to surrounding attacking units
    BATTERY,
    //HQ give supply to reinforcement to eventually distribute
    POOL_REINFORCEMENT,
    //HQ give supply to worker/miner
    POOL_WORKER,
    //give all supply away
    DYING
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
  
  public static void setDyingMode() {
    mode = SupplyDistributionMode.DYING;
  }
  
  public static void manageSupply() throws GameActionException {
    switch (mode) {
    case BATTERY:
      if (rc.getType() == RobotType.HQ) {
        distributeBatteryHQ();
      } else if (rc.getType() == RobotType.MINER) {
        distributeBatteryMiner();
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
    case DYING:
      distributeDying();
    default:
      break;
    }
  }
  
  public static int updateMinMinerSupply() {
    int totalSuppyRounds = 1900;
    int minMinerSupply = (totalSuppyRounds - Clock.getRoundNum())*RobotType.MINER.supplyUpkeep;
    return Math.max(minMinerSupply, 0);
  }
  
  public static boolean isBattleUnit(RobotType type) {
    return type == RobotType.DRONE || type == RobotType.TANK || type == RobotType.COMMANDER || 
        type == RobotType.SOLDIER || type == RobotType.LAUNCHER;
  }
  
  public static boolean isNonBattleUnit(RobotType type) {
    return type == RobotType.MINER;
  }
  
  public static void distributeDying() throws GameActionException {
    int supplyToTransfer = (int) (rc.getSupplyLevel());
    
    if (supplyToTransfer <= 0) {
      return;
    }
    
    RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED, rc.getTeam());
    for (int i=robots.length-1; i >= 0 && supplyToTransfer > 0; i--) {
      if (Clock.getBytecodesLeft() < 550) {
        return;
      }
      RobotInfo info = robots[i];
      if (isBattleUnit(info.type) && info.supplyLevel < minSupplyBattle*2/3) {
        int x = (int) Math.min(supplyToTransfer, supplyToTransfer);
        rc.transferSupplies(x, info.location);
        supplyToTransfer -= x;
      } 
    }
  }
  
  public static void distributeBatteryHQ() throws GameActionException {
    RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED, rc.getTeam());
    for (int i=robots.length; i-- > 0;) {
      if (Clock.getBytecodesLeft() < 550) {
        return;
      }
      RobotInfo info = robots[i];
      if (isBattleUnit(info.type) && info.supplyLevel < minSupplyLaunch*2/3) {
        rc.transferSupplies((int) (minSupplyLaunch - info.supplyLevel), info.location);
      } else if (info.type == RobotType.MINER && info.supplyLevel < updateMinMinerSupply()) {
        rc.transferSupplies((int) (updateMinMinerSupply() - info.supplyLevel), info.location);
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
      if (isBattleUnit(info.type) && info.supplyLevel < minSupplyBattle*2/3) {
        int x = (int) Math.min(supplyToTransfer, minSupplyBattle - info.supplyLevel);
        rc.transferSupplies(x, info.location);
        supplyToTransfer -= x;
      } 
    }
  }
  
  public static void distributeBatteryMiner() throws GameActionException {
    int supplyToTransfer = (int) (rc.getSupplyLevel() - 2 * minSupplyMinerWorking);
    
    if (supplyToTransfer <= 0) {
      return;
    }
    
    RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED, rc.getTeam());
    for (int i=robots.length-1; i >= 0 && supplyToTransfer > 0; i--) {
      if (Clock.getBytecodesLeft() < 500) {
        return;
      }
      RobotInfo info = robots[i];
      if (isNonBattleUnit(info.type) && info.supplyLevel < minSupplyMinerWorking*2/3) {
        int x = (int) Math.min(supplyToTransfer, 5*minSupplyMinerWorking);
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
