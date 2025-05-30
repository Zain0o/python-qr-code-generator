package RobotSim;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.Glow;
import javafx.scene.paint.Color;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Stop;

/**
 * The {@code BumpRobot} class extends {@link Robot} to add collision detection and response behaviour.
 * This robot is equipped with eight "bump sensors" around its perimeter that visually indicate collision states.
 *
 * <p>The BumpRobot detects collisions with obstacles and arena boundaries, responds by reversing direction,
 * and visually indicates collisions through colour changes and glowing effects.</p>
 *
 * @version 1.0
 */
public class BumpRobot extends Robot {

    /** Indicates if a collision has occurred. */
    private boolean hasCollision = false;

    /** Tracks cooldown period after a collision. */
    private int collisionCooldown = 0;

    /** Time to recover from collision (in update cycles). */
    private static final int COLLISION_RECOVERY_TIME = 30;

    /** Color when no collision has occurred. */
    private static final Color NORMAL_COLOR = Color.DARKGREEN;

    /** Color to display during collision. */
    private static final Color COLLISION_COLOR = Color.RED;

    /**
     * Constructs a new {@code BumpRobot} with the specified position and size.
     *
     * @param x      the x-coordinate of the robot's centre
     * @param y      the y-coordinate of the robot's centre
     * @param radius the radius of the robot's bounding circle
     */
    public BumpRobot(double x, double y, double radius) {
        super(x, y, radius);
        setSpeed(3.0); // Default movement speed
    }

    /**
     * Updates the state of the {@code BumpRobot} within the arena.
     * This includes moving the robot, detecting collisions, handling collision responses,
     * and managing collision cooldown periods.
     *
     * @param arena the {@code RobotArena} managing the environment
     */
    @Override
    public void updateState(RobotArena arena) {
        // If we are still in cooldown, decrement and possibly end collision state
        if (collisionCooldown > 0) {
            collisionCooldown--;
            if (collisionCooldown == 0) {
                hasCollision = false;
            }
            // Even during cooldown, still handle boundary collisions
            handleBoundaryCollision(arena);
            return;
        }

        double currentX = getX();
        double currentY = getY();
        double radians = Math.toRadians(getDirection());
        double newX = currentX + getSpeed() * Math.cos(radians);
        double newY = currentY + getSpeed() * Math.sin(radians);

        // Check for collisions with obstacles
        if (!arena.isColliding(newX, newY, getRadius(), getId())) {
            // No collision: move forward
            setX(newX);
            setY(newY);

            // Gradually accelerate if not colliding
            if (getSpeed() < getMaxSpeed()) {
                setSpeed(Math.min(getMaxSpeed(), getSpeed() + 0.1));
            }
        } else {
            // Collision occurred
            hasCollision = true;

            // Try up to 8 directions (increments of 45°) to find a clear path
            boolean foundPath = false;
            for (int i = 0; i < 8; i++) {
                double testAngle = getDirection() + (i * 45);
                radians = Math.toRadians(testAngle);
                newX = currentX + getRadius() * Math.cos(radians);
                newY = currentY + getRadius() * Math.sin(radians);

                if (!arena.isColliding(newX, newY, getRadius(), getId())) {
                    setDirection(testAngle);
                    setX(newX);
                    setY(newY);
                    // Adjust speed upon finding a path
                    setSpeed(getMaxSpeed() * 0.7);
                    foundPath = true;
                    break;
                }
            }

            if (!foundPath) {
                // If no clear path, bounce back with random angle
                double bounceAngle = getDirection() + 180 + (Math.random() - 0.5) * 90;
                setDirection(normalizeDirection(bounceAngle));
                setSpeed(getSpeed() * 0.5);
                collisionCooldown = COLLISION_RECOVERY_TIME;
            }
        }

        // Always check boundary collisions
        handleBoundaryCollision(arena);
    }

    /**
     * Handles collisions with the arena boundaries by adjusting the robot's position
     * and reversing its direction as necessary.
     *
     * @param arena the {@code RobotArena} managing the environment
     */
    private void handleBoundaryCollision(RobotArena arena) {
        boolean collisionOccurred = false;

        // Left boundary
        if (getX() - getRadius() < 0) {
            setX(getRadius());
            setDirection(normalizeDirection(180 - getDirection()));
            collisionOccurred = true;
        }
        // Right boundary
        else if (getX() + getRadius() > arena.getWidth()) {
            setX(arena.getWidth() - getRadius());
            setDirection(normalizeDirection(180 - getDirection()));
            collisionOccurred = true;
        }

        // Top boundary
        if (getY() - getRadius() < 0) {
            setY(getRadius());
            setDirection(normalizeDirection(360 - getDirection()));
            collisionOccurred = true;
        }
        // Bottom boundary
        else if (getY() + getRadius() > arena.getHeight()) {
            setY(arena.getHeight() - getRadius());
            setDirection(normalizeDirection(360 - getDirection()));
            collisionOccurred = true;
        }

        if (collisionOccurred) {
            hasCollision = true;
            collisionCooldown = COLLISION_RECOVERY_TIME;
        }
    }

    /**
     * Draws the {@code BumpRobot} onto the provided {@code GraphicsContext}.
     * This includes visual representations of the robot's body, collision effects, and bump sensors.
     *
     * @param gc the {@code GraphicsContext} used for drawing
     */
    @Override
    public void draw(GraphicsContext gc) {
        // Draw collision effect if in cooldown
        if (collisionCooldown > 0) {
            drawCollisionEffect(gc);
        }

        // Draw the robot's main body
        drawBody(gc);

        // Draw bump sensors around the perimeter
        drawBumpSensors(gc);
    }

    /**
     * Draws the robot's body using a radial gradient to indicate its current state.
     * The gradient changes colour based on whether a collision has occurred.
     *
     * @param gc the {@code GraphicsContext} used for drawing
     */
    private void drawBody(GraphicsContext gc) {
        RadialGradient bodyGradient = new RadialGradient(
                0, 0, getX(), getY(), getRadius(),
                false, CycleMethod.NO_CYCLE,
                new Stop(0, hasCollision ? Color.ORANGE : Color.LIGHTBLUE),
                new Stop(1, hasCollision ? COLLISION_COLOR : NORMAL_COLOR)
        );
        gc.setFill(bodyGradient);
        gc.fillOval(getX() - getRadius(), getY() - getRadius(), getRadius() * 2, getRadius() * 2);

        // Outline
        gc.setStroke(hasCollision ? Color.YELLOW : Color.WHITE);
        gc.setLineWidth(3);
        gc.strokeOval(getX() - getRadius(), getY() - getRadius(), getRadius() * 2, getRadius() * 2);
    }

    /**
     * Draws a pulsing collision effect using expanding rings to indicate recent collisions.
     *
     * @param gc the {@code GraphicsContext} used for drawing
     */
    private void drawCollisionEffect(GraphicsContext gc) {
        double progress = (COLLISION_RECOVERY_TIME - collisionCooldown)
                / (double) COLLISION_RECOVERY_TIME;
        // Expand ring up to 30% bigger than robot's radius
        double pulseSize = getRadius() * (1.0 + 0.3 * progress);

        for (int i = 0; i < 3; i++) {
            double ringSize = pulseSize * (1 + 0.1 * i);
            gc.setStroke(Color.rgb(255, 69, 0, 0.3 * (1 - i * 0.3))); // Fading effect
            gc.setLineWidth(2);
            gc.strokeOval(getX() - ringSize, getY() - ringSize, ringSize * 2, ringSize * 2);
        }
    }

    /**
     * Draws the bump sensors around the robot's perimeter as glowing dots.
     * The sensors change colour based on the collision state.
     *
     * @param gc the {@code GraphicsContext} used for drawing
     */
    private void drawBumpSensors(GraphicsContext gc) {
        double sensorSize = getRadius() * 0.2;

        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(i * 45 + getDirection());
            double sx = getX() + (getRadius() - sensorSize / 2) * Math.cos(angle);
            double sy = getY() + (getRadius() - sensorSize / 2) * Math.sin(angle);

            // Outer sensor color
            gc.setFill(hasCollision ? Color.RED : Color.LIME);
            gc.fillOval(sx - sensorSize / 2, sy - sensorSize / 2, sensorSize, sensorSize);

            // Glowing inner portion
            gc.setEffect(new Glow(0.5));
            gc.setFill(Color.LIMEGREEN);
            gc.fillOval(sx - sensorSize / 3, sy - sensorSize / 3, sensorSize / 1.5, sensorSize / 1.5);
            gc.setEffect(null);
        }
    }

    /**
     * Normalises any angle into the [0..360) degree range.
     *
     * @param angle the angle in degrees to normalise
     * @return the normalised angle
     */
    @Override
    protected double normalizeDirection(double angle) {
        angle %= 360.0;
        if (angle < 0) {
            angle += 360.0;
        }
        return angle;
    }

    /**
     * Provides a string representation of the {@code BumpRobot}, including its position and collision state.
     *
     * @return a string describing the robot's type, position, and collision status
     */
    @Override
    public String toString() {
        return String.format("BumpRobot at (%.0f, %.0f)%s",
                getX(), getY(), hasCollision ? " [COLLISION]" : "");
    }
}
