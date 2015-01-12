package ghetto_v2_dummy;

import battlecode.common.Clock;
import battlecode.common.Direction;
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
  
  
  public static final double UNSUPPLIED_COEFF = .5; // How much less dangerous is an unsupplied unit?
  
  // Normal units
  public static final double BEAVER_DANGER = RobotType.BEAVER.attackPower/RobotType.BEAVER.attackDelay;
  public static final double DRONE_DANGER = RobotType.DRONE.attackPower/RobotType.DRONE.attackDelay;
  public static final double SOLDIER_DANGER = RobotType.SOLDIER.attackPower/RobotType.SOLDIER.attackDelay;
  public static final double MINER_DANGER = RobotType.MINER.attackPower/RobotType.MINER.attackDelay;
  public static final double TANK_DANGER = RobotType.TANK.attackPower/RobotType.TANK.attackDelay;
  public static final double COMMANDER_DANGER = RobotType.COMMANDER.attackPower/RobotType.COMMANDER.attackDelay;

  // Splash damage. This probably needs some tuning...
  public static final double MISSILE_DANGER = RobotType.MISSILE.attackPower;
  public static final double BASHER_DANGER = RobotType.BASHER.attackPower/RobotType.BASHER.attackDelay;
  
  // Buildings
  public static final double TOWER_DANGER = RobotType.TOWER.attackPower/RobotType.TOWER.attackDelay;
  public static final double HQ_DANGER = RobotType.HQ.attackPower/RobotType.HQ.attackDelay;
  
  public static double getDangerScore(RobotInfo[] bots) {
    int dangerMetric = 0;
    for (RobotInfo bot : bots) {
      switch (bot.type) {
      case BEAVER:
        if (bot.supplyLevel > RobotType.BEAVER.supplyUpkeep) {
          dangerMetric += Util.BEAVER_DANGER;
        } else {
          dangerMetric += Util.BEAVER_DANGER*Util.UNSUPPLIED_COEFF;
        }
        break;
      case DRONE:
        if (bot.supplyLevel > RobotType.DRONE.supplyUpkeep) {
          dangerMetric += Util.DRONE_DANGER;
        } else {
          dangerMetric += Util.DRONE_DANGER*Util.UNSUPPLIED_COEFF;
        }
      case SOLDIER:
        if (bot.supplyLevel > RobotType.SOLDIER.supplyUpkeep) {
          dangerMetric += Util.SOLDIER_DANGER;
        } else {
          dangerMetric += Util.SOLDIER_DANGER*Util.UNSUPPLIED_COEFF;
        }
        break;
      case TANK:
        if (bot.supplyLevel > RobotType.TANK.supplyUpkeep) {
          dangerMetric += Util.TANK_DANGER;
        } else {
          dangerMetric += Util.TANK_DANGER*Util.UNSUPPLIED_COEFF;
        }
        break;
      case COMMANDER:
        if (bot.supplyLevel > RobotType.COMMANDER.supplyUpkeep) {
          dangerMetric += Util.COMMANDER_DANGER;
        } else {
          dangerMetric += Util.COMMANDER_DANGER*Util.UNSUPPLIED_COEFF;
        }
        break;
      case MINER:
        if (bot.supplyLevel > RobotType.MINER.supplyUpkeep) {
          dangerMetric += Util.MINER_DANGER;
        } else {
          dangerMetric += Util.MINER_DANGER*Util.UNSUPPLIED_COEFF;
        }
        break;
      case BASHER:
        if (bot.supplyLevel > RobotType.BASHER.supplyUpkeep) {
          dangerMetric += Util.BASHER_DANGER;
        } else {
          dangerMetric += Util.BASHER_DANGER*Util.UNSUPPLIED_COEFF;
        }
        break;
      case MISSILE:
        if (bot.supplyLevel > RobotType.MISSILE.supplyUpkeep) {
          dangerMetric += Util.MISSILE_DANGER;
        } else {
          dangerMetric += Util.MISSILE_DANGER*Util.UNSUPPLIED_COEFF;
        }
        break;
      case HQ:
        if (bot.supplyLevel > RobotType.HQ.supplyUpkeep) {
          dangerMetric += Util.HQ_DANGER;
        } else {
          dangerMetric += Util.HQ_DANGER*Util.UNSUPPLIED_COEFF;
        }
        break;
      case TOWER:
        if (bot.supplyLevel > RobotType.TOWER.supplyUpkeep) {
          dangerMetric += Util.TOWER_DANGER;
        } else {
          dangerMetric += Util.TOWER_DANGER*Util.UNSUPPLIED_COEFF;
        }
        break;
      default:
        break;
      }
    }
    return dangerMetric;
  }
  
  // 
  public final static int[] RANGE_TYPE_MAP = {5,5,0,0,0,0,0,0,0,0,0,2,0,2,1,2,3,4,3,0,1,};
  
  // [unit type ordinal][x][y][dir]
  public final static int[][][][] ATTACK_NOTES = {
  {{{},{},{},{},{},{},{},{},{},{},{},},{{},{},{},{},{},{},{},{},{},{},{},},{{},{},{},{},{},{},{},{},{},{},{},},{{},{},{},{},{},{},{},{},{},{},{},},{{},{},{},{},{},{},{},{},{},{},{},},{{},{},{},{},{},{},{},{},{},{},{},},{{},{},{},{},{},{},{},{},{},{},{},},{{},{},{},{},{},{},{},{},{},{},{},},{{},{},{},{},{},{},{},{},{},{},{},},{{},{},{},{},{},{},{},{},{},{},{},},{{},{},{},{},{},{},{},{},{},{},{},},},
  {{{},{},{},{},{},{},{},{},{},{},{},},{{},{},{},{},{},{},{},{},{},{},{},},{{},{},{},{},{},{},{},{},{},{},{},},{{},{},{},{7,},{6,7,},{5,6,7,},{5,6,},{5,},{},{},{},},{{},{},{},{0,7,},{0,6,7,},{0,4,5,6,7,},{4,5,6,},{4,5,},{},{},{},},{{},{},{},{0,1,7,},{0,1,2,6,7,},{0,1,2,3,4,5,6,7,},{2,3,4,5,6,},{3,4,5,},{},{},{},},{{},{},{},{0,1,},{0,1,2,},{0,1,2,3,4,},{2,3,4,},{3,4,},{},{},{},},{{},{},{},{1,},{1,2,},{1,2,3,},{2,3,},{3,},{},{},{},},{{},{},{},{},{},{},{},{},{},{},{},},{{},{},{},{},{},{},{},{},{},{},{},},{{},{},{},{},{},{},{},{},{},{},{},},},
  {{{},{},{},{},{},{},{},{},{},{},{},},{{},{},{},{},{},{},{},{},{},{},{},},{{},{},{},{7,},{6,7,},{5,6,7,},{5,6,},{5,},{},{},{},},{{},{},{7,},{0,6,7,},{0,5,6,7,},{0,4,5,6,7,},{4,5,6,7,},{4,5,6,},{5,},{},{},},{{},{},{0,7,},{0,1,6,7,},{0,1,2,4,5,6,7,},{0,1,2,3,4,5,6,7,},{0,2,3,4,5,6,7,},{3,4,5,6,},{4,5,},{},{},},{{},{},{0,1,7,},{0,1,2,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{2,3,4,5,6,},{3,4,5,},{},{},},{{},{},{0,1,},{0,1,2,7,},{0,1,2,3,4,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,},{2,3,4,5,},{3,4,},{},{},},{{},{},{1,},{0,1,2,},{0,1,2,3,},{0,1,2,3,4,},{1,2,3,4,},{2,3,4,},{3,},{},{},},{{},{},{},{1,},{1,2,},{1,2,3,},{2,3,},{3,},{},{},{},},{{},{},{},{},{},{},{},{},{},{},{},},{{},{},{},{},{},{},{},{},{},{},{},},},
  {{{},{},{},{},{},{},{},{},{},{},{},},{{},{},{},{7,},{6,7,},{5,6,7,},{5,6,},{5,},{},{},{},},{{},{},{7,},{0,6,7,},{0,5,6,7,},{0,4,5,6,7,},{4,5,6,7,},{4,5,6,},{5,},{},{},},{{},{7,},{0,6,7,},{0,1,5,6,7,},{0,1,2,4,5,6,7,},{0,1,2,3,4,5,6,7,},{0,2,3,4,5,6,7,},{3,4,5,6,7,},{4,5,6,},{5,},{},},{{},{0,7,},{0,1,6,7,},{0,1,2,4,5,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{0,2,3,4,5,6,7,},{3,4,5,6,},{4,5,},{},},{{},{0,1,7,},{0,1,2,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{2,3,4,5,6,},{3,4,5,},{},},{{},{0,1,},{0,1,2,7,},{0,1,2,3,4,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,},{2,3,4,5,},{3,4,},{},},{{},{1,},{0,1,2,},{0,1,2,3,7,},{0,1,2,3,4,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,},{1,2,3,4,5,},{2,3,4,},{3,},{},},{{},{},{1,},{0,1,2,},{0,1,2,3,},{0,1,2,3,4,},{1,2,3,4,},{2,3,4,},{3,},{},{},},{{},{},{},{1,},{1,2,},{1,2,3,},{2,3,},{3,},{},{},{},},{{},{},{},{},{},{},{},{},{},{},{},},},
  {{{},{},{},{},{},{},{},{},{},{},{},},{{},{},{7,},{6,7,},{5,6,7,},{5,6,7,},{5,6,7,},{5,6,},{5,},{},{},},{{},{7,},{0,6,7,},{0,5,6,7,},{0,4,5,6,7,},{0,4,5,6,7,},{0,4,5,6,7,},{4,5,6,7,},{4,5,6,},{5,},{},},{{},{0,7,},{0,1,6,7,},{0,1,2,4,5,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{0,2,3,4,5,6,7,},{3,4,5,6,},{4,5,},{},},{{},{0,1,7,},{0,1,2,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{2,3,4,5,6,},{3,4,5,},{},},{{},{0,1,7,},{0,1,2,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{2,3,4,5,6,},{3,4,5,},{},},{{},{0,1,7,},{0,1,2,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{2,3,4,5,6,},{3,4,5,},{},},{{},{0,1,},{0,1,2,7,},{0,1,2,3,4,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,},{2,3,4,5,},{3,4,},{},},{{},{1,},{0,1,2,},{0,1,2,3,},{0,1,2,3,4,},{0,1,2,3,4,},{0,1,2,3,4,},{1,2,3,4,},{2,3,4,},{3,},{},},{{},{},{1,},{1,2,},{1,2,3,},{1,2,3,},{1,2,3,},{2,3,},{3,},{},{},},{{},{},{},{},{},{},{},{},{},{},{},},},
  {{{},{},{7,},{6,7,},{5,6,7,},{5,6,7,},{5,6,7,},{5,6,},{5,},{},{},},{{},{7,},{0,6,7,},{0,5,6,7,},{0,4,5,6,7,},{0,4,5,6,7,},{0,4,5,6,7,},{4,5,6,7,},{4,5,6,},{5,},{},},{{7,},{0,6,7,},{0,1,5,6,7,},{0,1,2,4,5,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{0,2,3,4,5,6,7,},{3,4,5,6,7,},{4,5,6,},{5,},},{{0,7,},{0,1,6,7,},{0,1,2,4,5,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{0,2,3,4,5,6,7,},{3,4,5,6,},{4,5,},},{{0,1,7,},{0,1,2,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{2,3,4,5,6,},{3,4,5,},},{{0,1,7,},{0,1,2,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{2,3,4,5,6,},{3,4,5,},},{{0,1,7,},{0,1,2,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{2,3,4,5,6,},{3,4,5,},},{{0,1,},{0,1,2,7,},{0,1,2,3,4,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,},{2,3,4,5,},{3,4,},},{{1,},{0,1,2,},{0,1,2,3,7,},{0,1,2,3,4,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,7,},{0,1,2,3,4,5,6,},{1,2,3,4,5,},{2,3,4,},{3,},},{{},{1,},{0,1,2,},{0,1,2,3,},{0,1,2,3,4,},{0,1,2,3,4,},{0,1,2,3,4,},{1,2,3,4,},{2,3,4,},{3,},{},},{{},{},{1,},{1,2,},{1,2,3,},{1,2,3,},{1,2,3,},{2,3,},{3,},{},{},},},
  };
}