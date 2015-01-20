import java.util.ArrayList;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;


public class NoteGen {
  public static void dirNotes() {
    for (RobotType type : RobotType.values()) {
      int radius = type.attackRadiusSquared;

      System.out.print(radius);
      System.out.print(",");

    }

    System.out.println("{");

    for (RobotType type : RobotType.values()) {
      int radius = type.attackRadiusSquared;

      System.out.println(radius);

      System.out.print("{");
      MapLocation center = new MapLocation(0, 0);
      RobotType.values();
      for(int ex = -5; ex <= +5; ex++) {
        System.out.print("{");
        for(int ey = -5; ey <= +5; ey++) {
          if (radius == 0) {
            System.out.print("{},");
            continue;
          }
          MapLocation enemyLoc = new MapLocation(ex, ey);
          ArrayList<Integer> attacked = new ArrayList<Integer>();
          for(int dir = 0; dir < 8; dir++) {
            MapLocation moveLoc = center.add(Direction.values()[dir]);
            if(moveLoc.distanceSquaredTo(enemyLoc) <= radius) attacked.add(dir);
          }
          System.out.print("{");
          for(int i = 0; i < attacked.size(); i++) {
            System.out.print(attacked.get(i));
            System.out.print(",");
          }
          System.out.print("},");
        }
        System.out.print("},");
      }
      System.out.println("},");
    }
    System.out.print("};");
  }
  
  public static void centerNotes() {
    int[] attackRadii = {0, 2, 5, 10, 15, 24};
    System.out.print("{");

    for (int radius : attackRadii) {
      System.out.print("{");
      MapLocation center = new MapLocation(0, 0);
      RobotType.values();
      for(int ex = -5; ex <= +5; ex++) {
        System.out.print("{");
        for(int ey = -5; ey <= +5; ey++) {
          if (radius == 0) {
            System.out.print("{},");
            continue;
          }
          MapLocation enemyLoc = new MapLocation(ex, ey);
          ArrayList<Integer> attacked = new ArrayList<Integer>();
          for(int dir = 0; dir < 8; dir++) {
            MapLocation moveLoc = center.add(Direction.values()[dir]);
            if(moveLoc.distanceSquaredTo(enemyLoc) <= radius) attacked.add(dir);
          }
          // Add in center too
          if(center.distanceSquaredTo(enemyLoc) <= radius) attacked.add(8);

          System.out.print("{");
          for(int i = 0; i < attacked.size(); i++) {
            System.out.print(attacked.get(i));
            System.out.print(",");
          }
          System.out.print("},");
        }
        System.out.print("},");
      }
      System.out.println("},");
    }
    System.out.print("};");
  }
  
  public static void getOrdinalValues() {
    for (RobotType inf : RobotType.values()) {
      System.out.println(inf.name());
      System.out.println(inf.ordinal());

    }
      
  }
  
  public static void vectorMags() {
    System.out.print("{");
    RobotType.values();
    for (int ex = -4; ex <= +4; ex++) {
      System.out.print("{");
      for (int ey = -4; ey <= +4; ey++) {
        double mag = Math.sqrt(ex*ex + ey*ey);
        double xComp = ex/mag;
        double yComp = ey/mag;

        if (ex==0 && ey==0) {
          System.out.print("{" + 0 + ", " + 0 + ", " + 0 + "},");
        } else{
          System.out.print("{" + xComp + ", " + yComp + ", " + mag + "},");
        }
      }
      System.out.println("},");
    }
    System.out.println("},");
  }
  
  public static MapLocation getBuildLocation() {
    int size = 1;
    MapLocation center = new MapLocation(0,0);
    while (true) {
      int lx = center.x - size;
      int rx = center.x + size;
      int ty = center.y - size;
      int by = center.y + size;


      // Top side
      for (int i = rx + 1; i-- > lx;) {
        System.out.println("(" + i +  ", " + ty + ")");
        System.out.println("(" + i +  ", " + by + ")");
      }
      

      // Right side
      for (int i = by; i-- > ty + 1;) {
        System.out.println("(" + rx +  ", " + i + ")");
        System.out.println("(" + lx +  ", " + i + ")");
      }
      /*
      // Bottom side
      for (int i = size*2 + 1; i-- > 0;) {
      }
      
      // Right side
      for (int i = size*2; i-- > 1;) {
        System.out.println("(" + lx +  ", " + (ty + i) + ")");

      }
      */
      return center;
    }
  }
  
  public static void testLine() {
    int bitmask = 0b1;
    int index = 0;
    int val = 0b11111111111111111111111111111111;
    while ((val & bitmask) > 0) {
      bitmask <<= 1;
      index ++;
    }
    System.out.println(index);
  }

  public static void main(String[] args) {
    // centerNotes();
    // getOrdinalValues();
    // vectorMags();
    // getBuildLocation();
    testLine();
  }

}
