package RobotSim;

/**
 * A simple geometry utility class for representing and working with a line
 * between two points (x1, y1) and (x2, y2). Provides methods for checking intersections,
 * calculating distances from a point, and performing other geometric calculations.
 *
 * <p>This class is useful for detecting collisions and managing beam sensors in the RobotArena.</p>
 *
 * @version 1.0
 */
public class Line {

    /**
     * The x-coordinate of the start point of the line.
     */
    private double x1;

    /**
     * The y-coordinate of the start point of the line.
     */
    private double y1;

    /**
     * The x-coordinate of the end point of the line.
     */
    private double x2;

    /**
     * The y-coordinate of the end point of the line.
     */
    private double y2;

    /**
     * An array to store the line coordinates for easier access.
     */
    private double[] coords;

    /**
     * An array to store the intersection point (x, y) when lines intersect.
     */
    private final double[] intersectionPoint;

    /**
     * The gradient (slope) of the line, updated when {@link #getGradient()} is called.
     */
    private double gradient;

    /**
     * The y-intercept (offset) of the line, updated when {@link #getOffset()} is called.
     */
    private double offset;

    /**
     * Constructs a line from point (x1, y1) to point (x2, y2).
     *
     * @param x1 the x-coordinate of the start point
     * @param y1 the y-coordinate of the start point
     * @param x2 the x-coordinate of the end point
     * @param y2 the y-coordinate of the end point
     */
    public Line(double x1, double y1, double x2, double y2) {
        setLine(x1, y1, x2, y2);
        intersectionPoint = new double[]{0, 0};
    }

    /**
     * Retrieves the x-coordinate of the start point.
     *
     * @return the x1 coordinate
     */
    public double getX1() {
        return x1;
    }

    /**
     * Retrieves the y-coordinate of the start point.
     *
     * @return the y1 coordinate
     */
    public double getY1() {
        return y1;
    }

    /**
     * Retrieves the x-coordinate of the end point.
     *
     * @return the x2 coordinate
     */
    public double getX2() {
        return x2;
    }

    /**
     * Retrieves the y-coordinate of the end point.
     *
     * @return the y2 coordinate
     */
    public double getY2() {
        return y2;
    }

    /**
     * Sets the coordinates of the line to new values.
     *
     * @param x1 the new x-coordinate of the start point
     * @param y1 the new y-coordinate of the start point
     * @param x2 the new x-coordinate of the end point
     * @param y2 the new y-coordinate of the end point
     */
    public void setLine(double x1, double y1, double x2, double y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.coords = new double[]{x1, y1, x2, y2};
    }

    /**
     * Calculates the length of the line segment.
     *
     * @return the length of the line
     */
    public double length() {
        return distance(x1, y1, x2, y2);
    }

    /**
     * Calculates the distance between two points (x1, y1) and (x2, y2).
     *
     * @param x1 the x-coordinate of the first point
     * @param y1 the y-coordinate of the first point
     * @param x2 the x-coordinate of the second point
     * @param y2 the y-coordinate of the second point
     * @return the distance between the two points
     */
    public static double distance(double x1, double y1, double x2, double y2) {
        double deltaX = x2 - x1;
        double deltaY = y2 - y1;
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }

    /**
     * Calculates and returns the gradient (slope) of the line.
     *
     * @return the gradient of the line
     * @throws ArithmeticException if the line is vertical and the gradient is undefined
     */
    public double getGradient() {
        if (x1 == x2) {
            throw new ArithmeticException("Vertical line has undefined gradient.");
        }
        gradient = (y2 - y1) / (x2 - x1);
        return gradient;
    }

    /**
     * Calculates and returns the y-intercept (offset) of the line.
     *
     * @return the y-intercept of the line
     * @throws ArithmeticException if the line is vertical and has no y-intercept
     */
    public double getOffset() {
        if (x1 == x2) {
            throw new ArithmeticException("Vertical line has no y-offset.");
        }
        offset = y1 - (getGradient() * x1);
        return offset;
    }

    /**
     * Determines whether this line intersects with another line.
     *
     * @param otherLine the other {@code Line} to check for intersection
     * @return {@code true} if the lines intersect; {@code false} otherwise
     */
    public boolean findIntersection(Line otherLine) {
        // Calculate the denominator for the line intersection formula
        double denom = (otherLine.y2 - otherLine.y1) * (x2 - x1)
                - (otherLine.x2 - otherLine.x1) * (y2 - y1);

        if (denom == 0) {
            // Lines are parallel or coincident
            return false;
        }

        // Calculate intersection parameters ua and ub
        double ua = ((otherLine.x2 - otherLine.x1) * (y1 - otherLine.y1)
                - (otherLine.y2 - otherLine.y1) * (x1 - otherLine.x1)) / denom;

        double ub = ((x2 - x1) * (y1 - otherLine.y1)
                - (y2 - y1) * (x1 - otherLine.x1)) / denom;

        // Check if intersection point lies on both line segments
        if (ua >= 0 && ua <= 1 && ub >= 0 && ub <= 1) {
            intersectionPoint[0] = x1 + ua * (x2 - x1);
            intersectionPoint[1] = y1 + ua * (y2 - y1);
            return true;
        }
        return false;
    }

    /**
     * Retrieves the intersection point with another line, if it exists.
     *
     * @param otherLine the other {@code Line} to check for intersection
     * @return an array containing the intersection point [x, y], or {@code null} if no intersection exists
     */
    public double[] getIntersectionPoint(Line otherLine) {
        if (findIntersection(otherLine)) {
            // Return a cloned copy so the internal array is not exposed
            return intersectionPoint.clone();
        }
        return null;
    }

    /**
     * Calculates the shortest distance from a given point to this line segment.
     *
     * @param x the x-coordinate of the point
     * @param y the y-coordinate of the point
     * @return the shortest distance from the point to the line segment
     */
    public double distanceFrom(double x, double y) {
        double lengthSquared = Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2);

        if (lengthSquared == 0) {
            // The line segment is effectively a single point
            return distance(x, y, x1, y1);
        }

        // Calculate the projection parameter t
        double t = ((x - x1) * (x2 - x1) + (y - y1) * (y2 - y1)) / lengthSquared;
        t = Math.max(0, Math.min(1, t)); // Clamp t to [0, 1]

        // Compute the closest point on the segment
        double projX = x1 + t * (x2 - x1);
        double projY = y1 + t * (y2 - y1);

        return distance(x, y, projX, projY);
    }

    /**
     * Returns a string representation of the line, showing its start and end points.
     *
     * @return a string describing the line
     */
    @Override
    public String toString() {
        return String.format("Line[(%.2f, %.2f) -> (%.2f, %.2f)]", x1, y1, x2, y2);
    }
}
