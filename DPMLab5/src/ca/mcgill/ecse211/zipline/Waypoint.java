package ca.mcgill.ecse211.zipline;

// Simple data structure for representing the grid coordinates.
// Will be helpful when making the point arrays for the paths.

/**
 * Nothing special going on here. Just a little data type to represent the waypoints without having to deal with separate coordinates.
 * 
 * @author Justin Tremblay
 *
 */
public class Waypoint {
  public int x;
  public int y;
  
  /**
   * Constructor
   * @param x the position in x (in grid lines)
   * @param y the position in y (in grid lines)
   */
  public Waypoint(int x, int y) {
    this.x = x;
    this.y = y;
  }

  /**
   * Constructor
   * @param coords the position in x (index 0) and y (index 1) (in grid lines)
   */
  public Waypoint(int[] coords) {
      this.x = coords[0];
      this.y = coords[1];
  }
}
