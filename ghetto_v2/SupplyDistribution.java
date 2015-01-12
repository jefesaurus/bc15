package ghetto_v2;

import ghetto_v2.RobotPlayer.BaseBot;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class SupplyDistribution {
  private final RobotController rc;
  private SupplyDistributionMode mode;
  private final int minSupplyLaunch = 5000;
  
  private enum SupplyDistributionMode {
    //Pool supply at HQ
    NO_TRANSFER,
    //Distribute supply to surrounding attacking units
    BATTERY,
    //HQ give supply to reinforcement to eventually distribute
    POOL_REINFORCEMENT,
    //HQ give supply to worker/miner
    POOL_WORKER
  }
  
  public SupplyDistribution(RobotController rc) {
    this.rc = rc;
    mode = SupplyDistributionMode.POOL_WORKER;
  }
  
  public void setBatteryMode() {
    mode = SupplyDistributionMode.BATTERY;
  }
  
  public void setReinforcementMode() {
    mode = SupplyDistributionMode.POOL_REINFORCEMENT;
  }
  
  public void setWorkerMode() {
    mode = SupplyDistributionMode.POOL_WORKER;
  }
  
  public void disable() {
    mode = SupplyDistributionMode.NO_TRANSFER;
  }
  
  public void manageSupply() throws GameActionException {
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
  
  public void distributeBatteryHQ() throws GameActionException {
    RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED, rc.getTeam());
    for (int i=robots.length; i-- > 0;) {
      RobotInfo info = robots[i];
      if (info.type == RobotType.DRONE && info.supplyLevel < minSupplyLaunch) {
        rc.transferSupplies((int) (minSupplyLaunch - info.supplyLevel), info.location);
      }
    }
  }
  
  public void distributeBatteryUnit() {
    
  }
  
  public void distributeReinforcement() {
  }
  
  public void distributeWorker() {
  
  }
  
  
  
}
