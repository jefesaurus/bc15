package ghettoblaster;

import battlecode.common.Clock;
import battlecode.common.Direction;

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
}