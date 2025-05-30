package RobotSim;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Stop;

/**
 * The {@code Obstacle} class represents a static obstacle within the {@link RobotArena}.
 * It can detect collisions with robots and visually respond by highlighting itself.
 *
 * <p>Obstacles are circular entities that rotate over time and react visually when a collision occurs.
 * They serve as barriers that robots must navigate around within the arena.</p>
 *
 * @version 1.0
 */
public class Obstacle extends ArenaItem {
    /**
     * The base color of the obstacle.
     */
    private final Color baseColour = Color.GREY.darker();

    /**
     * Current rotation in degrees for animating the obstacle.
     */
    private double rotation = 0;

    /**
     * Indicates if the obstacle is currently colliding with a robot.
     */
    private boolean isColliding = false;

    /**
     * Timer to manage the duration of the collision visual effect.
     */
    private int collisionTimer = 0;

    // -----------------------------
    // Constants
    // -----------------------------

    /**
     * Number of update cycles the collision effect remains visible.
     */
    private static final int COLLISION_EFFECT_DURATION = 10;

    /**
     * Color used to outline the obstacle when colliding.
     */
    private static final Color COLLISION_OUTLINE_COLOUR = Color.YELLOW;

    /**
     * Normal outline color for the obstacle.
     */
    private static final Color NORMAL_OUTLINE_COLOUR = Color.BLACK;

    /**
     * Outline thickness while colliding.
     */
    private static final double COLLISION_OUTLINE_WIDTH = 2.0;

    /**
     * Normal outline thickness.
     */
    private static final double NORMAL_OUTLINE_WIDTH = 1.0;

    // -----------------------------
    // Constructor
    // -----------------------------

    /**
     * Constructs a new {@code Obstacle} with the specified position and size.
     *
     * @param x      the x-coordinate of the obstacle's center
     * @param y      the y-coordinate of the obstacle's center
     * @param radius the radius of the obstacle
     */
    public Obstacle(double x, double y, double radius) {
        super(x, y, radius);
    }

    // -----------------------------
    // Overridden Methods
    // -----------------------------

    /**
     * Draws the obstacle onto the provided {@code GraphicsContext}.
     * The obstacle is represented as a circle with a radial gradient and rotates over time.
     * It visually responds to collisions by highlighting itself with warning stripes.
     *
     * @param gc the {@code GraphicsContext} used for drawing
     */
    @Override
    public void draw(GraphicsContext gc) {
        // Draw shadow beneath the obstacle for a depth effect
        drawShadow(gc);

        // Apply rotation for animated warning stripes
        gc.save();
        gc.translate(getX(), getY());
        gc.rotate(rotation);

        // Create a radial gradient for a 3D appearance
        RadialGradient gradient = new RadialGradient(
                0, 0, 0, 0, getRadius(), false, CycleMethod.NO_CYCLE,
                new Stop(0, baseColour.brighter()),
                new Stop(1, baseColour.darker())
        );
        gc.setFill(gradient);
        gc.fillOval(-getRadius(), -getRadius(), getRadius() * 2, getRadius() * 2);

        // Draw obstacle outline, changing color based on collision state
        gc.setStroke(isColliding ? COLLISION_OUTLINE_COLOUR : NORMAL_OUTLINE_COLOUR);
        gc.setLineWidth(isColliding ? COLLISION_OUTLINE_WIDTH : NORMAL_OUTLINE_WIDTH);
        gc.strokeOval(-getRadius(), -getRadius(), getRadius() * 2, getRadius() * 2);

        // If colliding, draw rotating warning stripes
        if (isColliding) {
            drawWarningStripes(gc);
        }

        gc.restore(); // Restore the original state after rotation

        // Update collision effect timer
        if (collisionTimer > 0) {
            collisionTimer--;
            if (collisionTimer == 0) {
                isColliding = false;
            }
        }
    }

    /**
     * Updates the obstacle's state within the arena.
     * This includes handling rotation and collision checks.
     *
     * @param arena the {@code RobotArena} managing the environment
     */
    @Override
    public void updateState(RobotArena arena) {
        adjustItem();     // Update rotation for animation
        checkItem(arena); // Check for collisions with robots
    }

    /**
     * Provides a string representation of the {@code Obstacle}, including its position.
     *
     * @return a string describing the obstacle's type and position
     */
    @Override
    public String toString() {
        return String.format("Obstacle at (%.0f, %.0f)", getX(), getY());
    }

    // -----------------------------
    // Private Helper Methods
    // -----------------------------

    /**
     * Draws a shadow beneath the obstacle to add depth to the visual representation.
     *
     * @param gc the {@code GraphicsContext} used for drawing
     */
    private void drawShadow(GraphicsContext gc) {
        RadialGradient shadowGradient = new RadialGradient(
                0, 0, getX(), getY() + getRadius() + 10, getRadius() * 0.8, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(0, 0, 0, 0.3)),
                new Stop(1, Color.TRANSPARENT)
        );
        gc.setFill(shadowGradient);
        gc.fillOval(getX() - getRadius(), getY() + 10 - getRadius() * 0.3, getRadius() * 2, getRadius() * 1.5);
    }

    /**
     * Draws warning stripes around the obstacle to indicate a collision.
     * The stripes rotate to provide a dynamic visual effect.
     *
     * @param gc the {@code GraphicsContext} used for drawing
     */
    private void drawWarningStripes(GraphicsContext gc) {
        double stripeWidth = getRadius() * 0.4; // Width of each stripe
        gc.setFill(Color.YELLOW);
        gc.setGlobalAlpha(0.5); // Semi-transparent stripes

        // Draw multiple stripes around the obstacle
        for (double i = -getRadius(); i < getRadius() * 2; i += stripeWidth * 2) {
            gc.fillRect(-getRadius() + i, -getRadius(), stripeWidth, getRadius() * 2);
        }

        gc.setGlobalAlpha(1.0); // Reset transparency
    }

    /**
     * Slowly rotates the obstacle, animating any collision stripes.
     */
    private void adjustItem() {
        rotation += 0.5;
        rotation %= 360;
    }

    /**
     * Checks for collisions with robots in the arena and responds accordingly.
     *
     * @param arena the {@code RobotArena} managing the environment
     */
    private void checkItem(RobotArena arena) {
        // Iterate through all items in the arena to detect collisions with robots
        for (ArenaItem item : arena.getItems()) {
            if (item instanceof Robot robot && this.isColliding(robot)) {
                registerCollision();  // Mark the obstacle as colliding
                robot.registerCollision(); // Notify the robot of the collision (optional)
            }
        }
    }

    /**
     * Determines if this obstacle is colliding with the given robot.
     *
     * @param robot the {@code Robot} to check collision with
     * @return {@code true} if colliding, {@code false} otherwise
     */
    private boolean isColliding(Robot robot) {
        double distance = calculateDistance(robot);
        return distance < (getRadius() + robot.getRadius());
    }

    /**
     * Calculates the distance between this obstacle and the given robot.
     *
     * @param robot the {@code Robot} to calculate distance to
     * @return the distance between the obstacle and the robot
     */
    private double calculateDistance(Robot robot) {
        double dx = getX() - robot.getX();
        double dy = getY() - robot.getY();
        return Math.hypot(dx, dy);
    }

    // -----------------------------
    // Public Methods
    // -----------------------------

    /**
     * Registers a collision with a robot, triggering visual effects.
     * This method sets the collision state and initiates a timer for the collision effect duration.
     */
    public void registerCollision() {
        isColliding = true;
        collisionTimer = COLLISION_EFFECT_DURATION;
    }

    /**
     * Sets the X-coordinate of the obstacle's position.
     *
     * @param x The new X-coordinate.
     */
    @Override
    public void setX(double x) {
        super.setX(x);
    }

    /**
     * Sets the Y-coordinate of the obstacle's position.
     *
     * @param y The new Y-coordinate.
     */
    @Override
    public void setY(double y) {
        super.setY(y);
    }
}
