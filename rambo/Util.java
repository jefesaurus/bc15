package rambo;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class Util {

  static int m_z = Clock.getBytecodeNum();
  static int m_w = Clock.getRoundNum();

  /**
   * sets up our RNG given two seeds
   * 
   * @param seed1
   * @param seed2
   */
  public static void randInit(int seed1, int seed2) {
    m_z = seed1;
    m_w = seed2;
  }

  private static int gen() {
    m_z = 36969 * (m_z & 65535) + (m_z >> 16);
    m_w = 18000 * (m_w & 65535) + (m_w >> 16);
    return (m_z << 16) + m_w;
  }

  /**
   * @return a random integer between {@link Integer#MIN_VALUE} and
   *         {@link Integer#MAX_VALUE}
   */
  public static int randInt() {
    return gen();
  }

  /** @return a double between 0 - 1.0 */
  public static double randDouble() {
    return (gen() * 2.32830644e-10 + 0.5);
  }

  public static final Direction[] REGULAR_DIRECTIONS = { Direction.NORTH,
      Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
      Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST,
      Direction.NORTH_WEST };

  public static final Direction[] REGULAR_DIRECTIONS_WITH_NONE = {
      Direction.NORTH, Direction.NORTH_EAST, Direction.EAST,
      Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST,
      Direction.WEST, Direction.NORTH_WEST, Direction.NONE };
  
  public static final int[] DIR_DX = {0, 1, 1, 1, 0, -1, -1, -1, 0};
  public static final int[] DIR_DY = {-1, -1, 0, 1, 1, 1, 0, -1, 0};
  
  public static final double UNSUPPLIED_COEFF = .5; // How much less dangerous is an unsupplied unit?
  
  // Normal units
  public static final double BEAVER_DANGER = RobotType.BEAVER.attackPower/(RobotType.BEAVER.attackDelay + 0.5 * RobotType.BEAVER.loadingDelay);
  public static final double DRONE_DANGER = RobotType.DRONE.attackPower/(RobotType.DRONE.attackDelay + 0.5 * RobotType.DRONE.loadingDelay);
  public static final double SOLDIER_DANGER = RobotType.SOLDIER.attackPower/(RobotType.SOLDIER.attackDelay + 0.5 * RobotType.SOLDIER.loadingDelay);
  public static final double MINER_DANGER = RobotType.MINER.attackPower/(RobotType.MINER.attackDelay + 0.5 * RobotType.MINER.loadingDelay);
  public static final double TANK_DANGER = RobotType.TANK.attackPower/(RobotType.TANK.attackDelay + 0.5 * RobotType.TANK.loadingDelay);
  public static final double COMMANDER_DANGER = RobotType.COMMANDER.attackPower/(RobotType.COMMANDER.attackDelay + 0.5 * RobotType.COMMANDER.loadingDelay);

  // Splash damage. This probably needs some tuning...
  public static final double MISSILE_DANGER = RobotType.MISSILE.attackPower;
  public static final double BASHER_DANGER = RobotType.BASHER.attackPower/RobotType.BASHER.attackDelay;
  
  // Buildings
  public static final double TOWER_DANGER = RobotType.TOWER.attackPower/RobotType.TOWER.attackDelay;
  public static final double HQ_DANGER = RobotType.HQ.attackPower/RobotType.HQ.attackDelay;
  
  public static double getDangerScore(RobotInfo[] bots) {
    double dangerMetric = 0;
    for (int i = bots.length; i-- > 0;) {
      dangerMetric += getDangerScore(bots[i]);
    }
    return dangerMetric;
  }
  
  public static double getDangerScore(RobotInfo bot) {
    double danger = 0;
    switch (bot.type) {
    case LAUNCHER:
      if (bot.supplyLevel <= 0) {
        danger = Util.MISSILE_DANGER/16.0;
      } else {
        danger = Util.MISSILE_DANGER/8.0;
      }
      return danger + bot.missileCount*Util.MISSILE_DANGER*(bot.health/144.0)/2.0;
    case TOWER:
      return TANK_DANGER*4;
    case HQ:
      return TANK_DANGER*4;
    default:
      danger = DANGER_VALUE_MAP[bot.type.ordinal()]*bot.health/144.0;
      if (bot.supplyLevel <= 0) {
        danger *= .5;
      }
      return danger;
    }
  }
  
  /*
    0: HQ
    1: TOWER
    2: SUPPLYDEPOT
    3: TECHNOLOGYINSTITUTE
    4: BARRACKS
    5: HELIPAD
    6: TRAININGFIELD
    7: TANKFACTORY
    8: MINERFACTORY
    9: HANDWASHSTATION
    10: AEROSPACELAB
    11: BEAVER
    12: COMPUTER
    13: SOLDIER
    14: BASHER
    15: MINER
    16: DRONE
    17: TANK
    18: COMMANDER
    19: LAUNCHER
    20: MISSILE
   */
  public final static double[] DANGER_VALUE_MAP = {Util.HQ_DANGER, Util.TOWER_DANGER, 0, 0, 0, 0, 0, 0, 0, 0, 0, Util.BEAVER_DANGER, 0, Util.SOLDIER_DANGER,
                                                   Util.BASHER_DANGER, Util.MINER_DANGER, Util.DRONE_DANGER, Util.TANK_DANGER, Util.COMMANDER_DANGER, 0, 0};
  // 
  public final static int[] RANGE_TYPE_MAP = {5,5,0,0,0,0,0,0,0,0,0,2,0,2,1,2,3,4,3,5,1,};
  
  // [unit type ordinal][x][y][dir]
  public final static int[][][][] ATTACK_NOTES = {
    {{{},{},{},{},{},{},{},{},{},{},{},},{{},{},{},{},{},{},{},{},{},{},{},},{{},{},{},{},{},{},{},{},{},{},{},},{{},{},{},{},{},{},{},{},{},{},{},},{{},{},{},{},{},{},{},{},{},{},{},},{{},{},{},{},{},{},{},{},{},{},{},},{{},{},{},{},{},{},{},{},{},{},{},},{{},{},{},{},{},{},{},{},{},{},{},},{{},{},{},{},{},{},{},{},{},{},{},},{{},{},{},{},{},{},{},{},{},{},{},},{{},{},{},{},{},{},{},{},{},{},{},},},
    {{{},{},{},{},{},{},{},{},{},{},{},},{{},{},{},{},{},{},{},{},{},{},{},},{{},{},{},{},{},{},{},{},{},{},{},},{{},{},{},{7,},{6,7,},{5,6,7,},{5,6,},{5,},{},{},{},},{{},{},{},{0,7,},{0,6,7,8,},{0,4,5,6,7,8,},{4,5,6,8,},{4,5,},{},{},{},},{{},{},{},{0,1,7,},{0,1,2,6,7,8,},{0,1,2,3,4,5,6,7,8,},{2,3,4,5,6,8,},{3,4,5,},{},{},{},},{{},{},{},{0,1,},{0,1,2,8,},{0,1,2,3,4,8,},{2,3,4,8,},{3,4,},{},{},{},},{{},{},{},{1,},{1,2,},{1,2,3,},{2,3,},{3,},{},{},{},},{{},{},{},{},{},{},{},{},{},{},{},},{{},{},{},{},{},{},{},{},{},{},{},},{{},{},{},{},{},{},{},{},{},{},{},},},
    {{{},{},{},{},{},{},{},{},{},{},{},},{{},{},{},{},{},{},{},{},{},{},{},},{{},{},{},{7,},{6,7,},{5,6,7,},{5,6,},{5,},{},{},{},},{{},{},{7,},{0,6,7,},{0,5,6,7,8,},{0,4,5,6,7,8,},{4,5,6,7,8,},{4,5,6,},{5,},{},{},},{{},{},{0,7,},{0,1,6,7,8,},{0,1,2,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,2,3,4,5,6,7,8,},{3,4,5,6,8,},{4,5,},{},{},},{{},{},{0,1,7,},{0,1,2,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{2,3,4,5,6,8,},{3,4,5,},{},{},},{{},{},{0,1,},{0,1,2,7,8,},{0,1,2,3,4,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,8,},{2,3,4,5,8,},{3,4,},{},{},},{{},{},{1,},{0,1,2,},{0,1,2,3,8,},{0,1,2,3,4,8,},{1,2,3,4,8,},{2,3,4,},{3,},{},{},},{{},{},{},{1,},{1,2,},{1,2,3,},{2,3,},{3,},{},{},{},},{{},{},{},{},{},{},{},{},{},{},{},},{{},{},{},{},{},{},{},{},{},{},{},},},
    {{{},{},{},{},{},{},{},{},{},{},{},},{{},{},{},{7,},{6,7,},{5,6,7,},{5,6,},{5,},{},{},{},},{{},{},{7,},{0,6,7,},{0,5,6,7,8,},{0,4,5,6,7,8,},{4,5,6,7,8,},{4,5,6,},{5,},{},{},},{{},{7,},{0,6,7,},{0,1,5,6,7,8,},{0,1,2,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,2,3,4,5,6,7,8,},{3,4,5,6,7,8,},{4,5,6,},{5,},{},},{{},{0,7,},{0,1,6,7,8,},{0,1,2,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,2,3,4,5,6,7,8,},{3,4,5,6,8,},{4,5,},{},},{{},{0,1,7,},{0,1,2,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{2,3,4,5,6,8,},{3,4,5,},{},},{{},{0,1,},{0,1,2,7,8,},{0,1,2,3,4,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,8,},{2,3,4,5,8,},{3,4,},{},},{{},{1,},{0,1,2,},{0,1,2,3,7,8,},{0,1,2,3,4,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,8,},{1,2,3,4,5,8,},{2,3,4,},{3,},{},},{{},{},{1,},{0,1,2,},{0,1,2,3,8,},{0,1,2,3,4,8,},{1,2,3,4,8,},{2,3,4,},{3,},{},{},},{{},{},{},{1,},{1,2,},{1,2,3,},{2,3,},{3,},{},{},{},},{{},{},{},{},{},{},{},{},{},{},{},},},
    {{{},{},{},{},{},{},{},{},{},{},{},},{{},{},{7,},{6,7,},{5,6,7,},{5,6,7,},{5,6,7,},{5,6,},{5,},{},{},},{{},{7,},{0,6,7,},{0,5,6,7,8,},{0,4,5,6,7,8,},{0,4,5,6,7,8,},{0,4,5,6,7,8,},{4,5,6,7,8,},{4,5,6,},{5,},{},},{{},{0,7,},{0,1,6,7,8,},{0,1,2,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,2,3,4,5,6,7,8,},{3,4,5,6,8,},{4,5,},{},},{{},{0,1,7,},{0,1,2,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{2,3,4,5,6,8,},{3,4,5,},{},},{{},{0,1,7,},{0,1,2,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{2,3,4,5,6,8,},{3,4,5,},{},},{{},{0,1,7,},{0,1,2,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{2,3,4,5,6,8,},{3,4,5,},{},},{{},{0,1,},{0,1,2,7,8,},{0,1,2,3,4,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,8,},{2,3,4,5,8,},{3,4,},{},},{{},{1,},{0,1,2,},{0,1,2,3,8,},{0,1,2,3,4,8,},{0,1,2,3,4,8,},{0,1,2,3,4,8,},{1,2,3,4,8,},{2,3,4,},{3,},{},},{{},{},{1,},{1,2,},{1,2,3,},{1,2,3,},{1,2,3,},{2,3,},{3,},{},{},},{{},{},{},{},{},{},{},{},{},{},{},},},
    {{{},{},{7,},{6,7,},{5,6,7,},{5,6,7,},{5,6,7,},{5,6,},{5,},{},{},},{{},{7,},{0,6,7,},{0,5,6,7,8,},{0,4,5,6,7,8,},{0,4,5,6,7,8,},{0,4,5,6,7,8,},{4,5,6,7,8,},{4,5,6,},{5,},{},},{{7,},{0,6,7,},{0,1,5,6,7,8,},{0,1,2,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,2,3,4,5,6,7,8,},{3,4,5,6,7,8,},{4,5,6,},{5,},},{{0,7,},{0,1,6,7,8,},{0,1,2,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,2,3,4,5,6,7,8,},{3,4,5,6,8,},{4,5,},},{{0,1,7,},{0,1,2,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{2,3,4,5,6,8,},{3,4,5,},},{{0,1,7,},{0,1,2,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{2,3,4,5,6,8,},{3,4,5,},},{{0,1,7,},{0,1,2,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{2,3,4,5,6,8,},{3,4,5,},},{{0,1,},{0,1,2,7,8,},{0,1,2,3,4,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,8,},{2,3,4,5,8,},{3,4,},},{{1,},{0,1,2,},{0,1,2,3,7,8,},{0,1,2,3,4,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,7,8,},{0,1,2,3,4,5,6,8,},{1,2,3,4,5,8,},{2,3,4,},{3,},},{{},{1,},{0,1,2,},{0,1,2,3,8,},{0,1,2,3,4,8,},{0,1,2,3,4,8,},{0,1,2,3,4,8,},{1,2,3,4,8,},{2,3,4,},{3,},{},},{{},{},{1,},{1,2,},{1,2,3,},{1,2,3,},{1,2,3,},{2,3,},{3,},{},{},},},
    };
  
  // [unitX][unitY][mag]
  public final static double[][][] VECTOR_MAGS = {{{-0.7071067811865475, -0.7071067811865475, 5.656854249492381},{-0.8, -0.6, 5.0},{-0.8944271909999159, -0.4472135954999579, 4.47213595499958},{-0.9701425001453319, -0.24253562503633297, 4.123105625617661},{-1.0, 0.0, 4.0},{-0.9701425001453319, 0.24253562503633297, 4.123105625617661},{-0.8944271909999159, 0.4472135954999579, 4.47213595499958},{-0.8, 0.6, 5.0},{-0.7071067811865475, 0.7071067811865475, 5.656854249492381},},
    {{-0.6, -0.8, 5.0},{-0.7071067811865476, -0.7071067811865476, 4.242640687119285},{-0.8320502943378437, -0.5547001962252291, 3.605551275463989},{-0.9486832980505138, -0.31622776601683794, 3.1622776601683795},{-1.0, 0.0, 3.0},{-0.9486832980505138, 0.31622776601683794, 3.1622776601683795},{-0.8320502943378437, 0.5547001962252291, 3.605551275463989},{-0.7071067811865476, 0.7071067811865476, 4.242640687119285},{-0.6, 0.8, 5.0},},
    {{-0.4472135954999579, -0.8944271909999159, 4.47213595499958},{-0.5547001962252291, -0.8320502943378437, 3.605551275463989},{-0.7071067811865475, -0.7071067811865475, 2.8284271247461903},{-0.8944271909999159, -0.4472135954999579, 2.23606797749979},{-1.0, 0.0, 2.0},{-0.8944271909999159, 0.4472135954999579, 2.23606797749979},{-0.7071067811865475, 0.7071067811865475, 2.8284271247461903},{-0.5547001962252291, 0.8320502943378437, 3.605551275463989},{-0.4472135954999579, 0.8944271909999159, 4.47213595499958},},
    {{-0.24253562503633297, -0.9701425001453319, 4.123105625617661},{-0.31622776601683794, -0.9486832980505138, 3.1622776601683795},{-0.4472135954999579, -0.8944271909999159, 2.23606797749979},{-0.7071067811865475, -0.7071067811865475, 1.4142135623730951},{-1.0, 0.0, 1.0},{-0.7071067811865475, 0.7071067811865475, 1.4142135623730951},{-0.4472135954999579, 0.8944271909999159, 2.23606797749979},{-0.31622776601683794, 0.9486832980505138, 3.1622776601683795},{-0.24253562503633297, 0.9701425001453319, 4.123105625617661},},
    {{0.0, -1.0, 4.0},{0.0, -1.0, 3.0},{0.0, -1.0, 2.0},{0.0, -1.0, 1.0},{0, 0, 0},{0.0, 1.0, 1.0},{0.0, 1.0, 2.0},{0.0, 1.0, 3.0},{0.0, 1.0, 4.0},},
    {{0.24253562503633297, -0.9701425001453319, 4.123105625617661},{0.31622776601683794, -0.9486832980505138, 3.1622776601683795},{0.4472135954999579, -0.8944271909999159, 2.23606797749979},{0.7071067811865475, -0.7071067811865475, 1.4142135623730951},{1.0, 0.0, 1.0},{0.7071067811865475, 0.7071067811865475, 1.4142135623730951},{0.4472135954999579, 0.8944271909999159, 2.23606797749979},{0.31622776601683794, 0.9486832980505138, 3.1622776601683795},{0.24253562503633297, 0.9701425001453319, 4.123105625617661},},
    {{0.4472135954999579, -0.8944271909999159, 4.47213595499958},{0.5547001962252291, -0.8320502943378437, 3.605551275463989},{0.7071067811865475, -0.7071067811865475, 2.8284271247461903},{0.8944271909999159, -0.4472135954999579, 2.23606797749979},{1.0, 0.0, 2.0},{0.8944271909999159, 0.4472135954999579, 2.23606797749979},{0.7071067811865475, 0.7071067811865475, 2.8284271247461903},{0.5547001962252291, 0.8320502943378437, 3.605551275463989},{0.4472135954999579, 0.8944271909999159, 4.47213595499958},},
    {{0.6, -0.8, 5.0},{0.7071067811865476, -0.7071067811865476, 4.242640687119285},{0.8320502943378437, -0.5547001962252291, 3.605551275463989},{0.9486832980505138, -0.31622776601683794, 3.1622776601683795},{1.0, 0.0, 3.0},{0.9486832980505138, 0.31622776601683794, 3.1622776601683795},{0.8320502943378437, 0.5547001962252291, 3.605551275463989},{0.7071067811865476, 0.7071067811865476, 4.242640687119285},{0.6, 0.8, 5.0},},
    {{0.7071067811865475, -0.7071067811865475, 5.656854249492381},{0.8, -0.6, 5.0},{0.8944271909999159, -0.4472135954999579, 4.47213595499958},{0.9701425001453319, -0.24253562503633297, 4.123105625617661},{1.0, 0.0, 4.0},{0.9701425001453319, 0.24253562503633297, 4.123105625617661},{0.8944271909999159, 0.4472135954999579, 4.47213595499958},{0.8, 0.6, 5.0},{0.7071067811865475, 0.7071067811865475, 5.656854249492381},},
    };
  
  public static int getNumAttackUnits(RobotInfo[] bots) {
    int numAttackers = 0;
    for (int i = bots.length; i-- > 0;) {
      switch (bots[i].type) {
      case DRONE:
      case SOLDIER:
      case TANK:
      case COMMANDER:
      case BASHER:
      case LAUNCHER:
        numAttackers ++;
        break;
      default:
        break;
      }
    }
    return numAttackers;
  }
}