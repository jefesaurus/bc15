package ghettoblaster;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;



public class Messaging {
  public final static int NUM_BEAVERS = 0;
  public final static int[] OUR_TOWERS = {1,2,3,4,5,6};
  public final static int[] ENEMY_TOWERS = {7,8,9,10,11,12};
  public final static int OUR_HQ = 13;
  public final static int ENEMY_HQ = 14;
  public final static int NUM_MINERS = 15;
  public final static int RALLY_POINT_X = 16;
  public final static int RALLY_POINT_Y = 17;
  
  public static int announceBeaver(RobotController rc) throws GameActionException {
    int numBeavers = rc.readBroadcast(NUM_BEAVERS);
    rc.broadcast(NUM_BEAVERS, numBeavers + 1);
    return numBeavers;
  }
  
}
