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

  public static void main(String[] args) {
    // centerNotes();
    getOrdinalValues();
  }

}
