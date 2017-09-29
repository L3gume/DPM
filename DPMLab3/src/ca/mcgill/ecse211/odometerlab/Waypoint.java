package ca.mcgill.ecse211.odometerlab;

// Simple data structure for representing the grid coordinates.
// Will be helpful when making the point arrays for the paths.
public class Waypoint {
  public int x;
  public int y;
  
  public Waypoint(int x, int y) {
    this.x = x;
    this.y = y;
  }
}
