package RobotSim;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.Glow;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.ArcType;

/**
 * The {@code ChaserRobot} class represents a robot that chases the nearest robot in the arena.
 * It extends the {@link Robot} class and overrides the {@code updateState} and {@code draw} behaviors
 * to implement specific functionalities such as chasing, collision detection, and animated features.
 *
 * <p>The ChaserRobot continuously seeks out the closest robot, moves towards it, and handles collisions
 * with both obstacles and arena boundaries. It features animated components like radar sweeps and swinging arms.</p>
 *
 * @version 1.0
 */
public class ChaserRobot extends Robot {

    // -----------------------------
    // Animation and Timing Fields
    // -----------------------------
    /** Current radar sweep angle for the chaser's detection animation. */
    private double radarAngle = 0;

    /** Indicates whether the robot's eyes are currently blinking. */
    private boolean isBlinking = false;

    /** Timestamp of the last blink event. */
    private long lastBlinkTime = 0;

    /** Interval (in ms) between blinks. */
    private static final long BLINK_INTERVAL = 3000; // 3 seconds

    /** Duration (in ms) of each blink. */
    private static final long BLINK_DURATION = 200;   // 200 ms

    /** Current angle of the antenna swing animation. */
    private double antennaSwingAngle = 0;

    /** Tracks the antenna's swing direction (true = up, false = down). */
    private boolean swingDirection = true;

    /** Current angle of the arms' swing animation. */
    private double armSwingAngle = 0;

    /** Tracks the arms' swing direction (true = forward, false = backward). */
    private boolean armSwingDirection = true;

    // -----------------------------
    // Proportional Dimensions
    // -----------------------------
    /** Proportion of radius used for antenna height. */
    private final double ANTENNA_HEIGHT;
    /** Proportion of radius used for antenna width. */
    private final double ANTENNA_WIDTH;
    /** Proportion of radius used for arm length. */
    private final double ARM_LENGTH;
    /** Proportion of radius used for arm width. */
    private final double ARM_WIDTH;
    /** Proportion of radius used for gripper size. */
    private final double GRIPPER_SIZE;
    /** Proportion of radius used for eye size. */
    private final double EYE_SIZE;
    /** Proportion of radius used for eye offset along the x-axis. */
    private final double EYE_OFFSET_X;
    /** Proportion of radius used for eye offset along the y-axis. */
    private final double EYE_OFFSET_Y;
    /** Proportion of radius used for the x-offset of the arms. */
    private final double ARM_OFFSET_X;
    /** Proportion of radius used for the y-offset of the arms. */
    private final double ARM_OFFSET_Y;

    /**
     * Constructs a new {@code ChaserRobot} with the specified position and size.
     *
     * @param x      The x-coordinate of the robot's center.
     * @param y      The y-coordinate of the robot's center.
     * @param radius The radius of the robot's bounding circle.
     */
    public ChaserRobot(double x, double y, double radius) {
        super(x, y, radius);

        // Initialize component dimensions based on radius proportions
        this.ANTENNA_HEIGHT = 0.5;  // Proportion of radius
        this.ANTENNA_WIDTH = 0.1;   // Proportion of radius
        this.ARM_LENGTH = 0.8;      // Proportion of radius
        this.ARM_WIDTH = 0.2;       // Proportion of radius
        this.GRIPPER_SIZE = 0.1;    // Proportion of radius
        this.EYE_SIZE = 0.2;        // Proportion of radius
        this.EYE_OFFSET_X = 0.25;   // Proportion of radius
        this.EYE_OFFSET_Y = -0.2;   // Proportion of radius
        this.ARM_OFFSET_X = 0.6;    // Proportion of radius
        this.ARM_OFFSET_Y = 0.0;    // Proportion of radius
    }

    /**
     * Updates the state of the {@code ChaserRobot} within the arena.
     * Moves toward the nearest robot, checks for boundary collisions, and updates animations.
     *
     * @param arena The {@code RobotArena} managing the environment.
     */
    @Override
    public void updateState(RobotArena arena) {
        // Find the nearest robot to chase
        Robot nearestRobot = findNearestRobot(arena);

        if (nearestRobot != null) {
            double dx = nearestRobot.getX() - getX();
            double dy = nearestRobot.getY() - getY();
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance > 0) {
                // Normalize direction vector
                double normalizedDx = dx / distance;
                double normalizedDy = dy / distance;

                // Calculate potential new position
                double potentialX = getX() + normalizedDx * getSpeed();
                double potentialY = getY() + normalizedDy * getSpeed();

                // Check collisions at the new position
                if (!arena.isColliding(potentialX, potentialY, getRadius(), getId())) {
                    setX(potentialX);
                    setY(potentialY);
                    // Update direction to face target
                    setDirection(Math.toDegrees(Math.atan2(dy, dx)));
                } else {
                    // If collision, revert and handle
                    handleCollision(getX(), getY());
                }

                // Prevent overlap if too close to the target
                if (distance < getRadius() + nearestRobot.getRadius()) {
                    setX(getX() - normalizedDx * getSpeed());
                    setY(getY() - normalizedDy * getSpeed());
                }
            }
        }

        // Check collisions with arena boundaries
        handleBoundaryCollision(arena);

        // Update animations
        updateRadar();
        updateArmSwing();

        // Manage blinking
        manageBlinking();
    }

    /**
     * Draws the {@code ChaserRobot} onto the provided {@code GraphicsContext}.
     * Includes main body, eyes, radar sweep, arms, shadow, glow, and any active animations.
     *
     * @param gc The {@code GraphicsContext} used for drawing.
     */
    @Override
    public void draw(GraphicsContext gc) {
        gc.save(); // Save the current state

        // Translate and rotate to the robot's position/direction
        gc.translate(getX(), getY());
        // Rotate so the front of the robot is oriented upward
        gc.rotate(getDirection() - 90);

        // Draw shadow and glow for visual appeal
        drawShadow(gc);
        drawGlow(gc);

        // Draw main body
        drawBody(gc);

        // Draw eyes
        drawEyes(gc);

        // Draw radar sweep
        drawRadarSweep(gc);

        // Draw arms with grippers
        drawArms(gc);

        gc.restore(); // Restore the original state
    }

    /**
     * Draws the main body of the robot with a radial gradient.
     *
     * @param gc The {@code GraphicsContext} used for drawing.
     */
    private void drawBody(GraphicsContext gc) {
        // Use a radial gradient from bright red to dark red
        RadialGradient gradient = new RadialGradient(
                0, 0, 0, 0, getRadius(), false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.RED),
                new Stop(1, Color.DARKRED)
        );
        gc.setFill(gradient);
        gc.fillOval(-getRadius(), -getRadius(), getRadius() * 2, getRadius() * 2);

        // Outline the body
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2);
        gc.strokeOval(-getRadius(), -getRadius(), getRadius() * 2, getRadius() * 2);
    }

    /**
     * Draws the robot's eyes, which may be open or closed depending on the blinking state.
     *
     * @param gc The {@code GraphicsContext} used for drawing.
     */
    private void drawEyes(GraphicsContext gc) {
        double eyeSep = getRadius() * EYE_OFFSET_X; // Horizontal offset for eyes
        double eyeY = getRadius() * EYE_OFFSET_Y;   // Vertical offset
        double eyeDiameter = getRadius() * EYE_SIZE;

        if (isBlinking) {
            // Draw closed eyes as lines
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(2);
            gc.strokeLine(-eyeSep - getRadius() * 0.1, eyeY,
                    -eyeSep + getRadius() * 0.1, eyeY);
            gc.strokeLine(eyeSep - getRadius() * 0.1, eyeY,
                    eyeSep + getRadius() * 0.1, eyeY);
        } else {
            // Draw open eyes as white circles with black pupils
            gc.setFill(Color.WHITE);
            // Left eye
            gc.fillOval(-eyeSep - eyeDiameter / 2, eyeY - eyeDiameter / 2,
                    eyeDiameter, eyeDiameter);
            gc.setFill(Color.BLACK);
            gc.fillOval(-eyeSep - eyeDiameter / 4, eyeY - eyeDiameter / 4,
                    eyeDiameter / 2, eyeDiameter / 2);

            // Right eye
            gc.setFill(Color.WHITE);
            gc.fillOval(eyeSep - eyeDiameter / 2, eyeY - eyeDiameter / 2,
                    eyeDiameter, eyeDiameter);
            gc.setFill(Color.BLACK);
            gc.fillOval(eyeSep - eyeDiameter / 4, eyeY - eyeDiameter / 4,
                    eyeDiameter / 2, eyeDiameter / 2);
        }
    }

    /**
     * Draws the radar sweep arc to simulate sensor scanning.
     *
     * @param gc The {@code GraphicsContext} used for drawing.
     */
    private void drawRadarSweep(GraphicsContext gc) {
        gc.save();
        gc.setStroke(Color.LIGHTGREEN);
        gc.setLineWidth(2);
        gc.setGlobalAlpha(0.5); // Semi-transparent sweep

        // Draw a 30-degree arc to represent the radar sweep
        gc.strokeArc(-getRadius(), -getRadius(), getRadius() * 2, getRadius() * 2,
                radarAngle, 30, ArcType.OPEN);

        gc.restore();
    }

    /**
     * Draws the robot's arms with swinging grippers.
     *
     * @param gc The {@code GraphicsContext} used for drawing.
     */
    private void drawArms(GraphicsContext gc) {
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2);
        gc.setFill(Color.DARKGRAY);

        // Convert current arm swing to radians
        double swingRadians = Math.toRadians(armSwingAngle);

        // Arm pivot positions
        double armOffsetX = getRadius() * ARM_OFFSET_X;
        double armOffsetY = getRadius() * ARM_OFFSET_Y;

        // Left arm
        double leftArmStartX = -armOffsetX;
        double leftArmStartY = armOffsetY;
        double leftArmEndX = leftArmStartX - ARM_LENGTH * getRadius() * Math.cos(swingRadians);
        double leftArmEndY = leftArmStartY + ARM_WIDTH * getRadius() * Math.sin(swingRadians);

        // Right arm
        double rightArmStartX = armOffsetX;
        double rightArmStartY = armOffsetY;
        double rightArmEndX = rightArmStartX + ARM_LENGTH * getRadius() * Math.cos(swingRadians);
        double rightArmEndY = rightArmStartY - ARM_WIDTH * getRadius() * Math.sin(swingRadians);

        // Draw left arm + gripper
        gc.strokeLine(leftArmStartX, leftArmStartY, leftArmEndX, leftArmEndY);
        gc.fillOval(leftArmEndX - GRIPPER_SIZE * getRadius(), leftArmEndY - GRIPPER_SIZE * getRadius(),
                GRIPPER_SIZE * 2 * getRadius(), GRIPPER_SIZE * 2 * getRadius());

        // Draw right arm + gripper
        gc.strokeLine(rightArmStartX, rightArmStartY, rightArmEndX, rightArmEndY);
        gc.fillOval(rightArmEndX - GRIPPER_SIZE * getRadius(), rightArmEndY - GRIPPER_SIZE * getRadius(),
                GRIPPER_SIZE * 2 * getRadius(), GRIPPER_SIZE * 2 * getRadius());
    }

    /**
     * Draws a shadow beneath the robot for a depth effect.
     *
     * @param gc The {@code GraphicsContext} used for drawing.
     */
    private void drawShadow(GraphicsContext gc) {
        RadialGradient shadowGradient = new RadialGradient(
                0, 0, 0, getRadius() + 10, getRadius() * 0.8, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(0, 0, 0, 0.3)),
                new Stop(1, Color.TRANSPARENT)
        );
        gc.setFill(shadowGradient);
        gc.fillOval(-getRadius(), getRadius() + 10 - getRadius() * 0.3,
                getRadius() * 2, getRadius() * 1.5);
    }

    /**
     * Draws a subtle glow effect around the robot.
     *
     * @param gc The {@code GraphicsContext} used for drawing.
     */
    private void drawGlow(GraphicsContext gc) {
        RadialGradient glowGradient = new RadialGradient(
                0, 0, 0, 0, getRadius() * 1.5, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(255, 165, 0, 0.3)), // Orange glow
                new Stop(1, Color.TRANSPARENT)
        );
        gc.setFill(glowGradient);
        gc.fillOval(-getRadius() * 1.5, -getRadius() * 1.5,
                getRadius() * 3, getRadius() * 3);
    }

    /**
     * Updates the radar sweep animation by incrementing the radar angle.
     */
    private void updateRadar() {
        radarAngle += 2; // Speed of radar rotation
        if (radarAngle >= 360) {
            radarAngle -= 360;
        }
    }

    /**
     * Updates the arm swing animation, causing the arms to swing back and forth.
     */
    private void updateArmSwing() {
        if (armSwingDirection) {
            armSwingAngle += 2;
            if (armSwingAngle >= 30) { // Swing limit
                armSwingDirection = false;
            }
        } else {
            armSwingAngle -= 2;
            if (armSwingAngle <= -30) { // Swing limit
                armSwingDirection = true;
            }
        }
    }

    /**
     * Manages the blinking animation of the robot's eyes based on timing intervals.
     */
    private void manageBlinking() {
        long currentTime = System.currentTimeMillis();
        if (!isBlinking && currentTime - lastBlinkTime >= BLINK_INTERVAL) {
            isBlinking = true;
            lastBlinkTime = currentTime;
        }
        if (isBlinking && currentTime - lastBlinkTime >= BLINK_DURATION) {
            isBlinking = false;
            lastBlinkTime = currentTime;
        }
    }

    /**
     * Locates the nearest robot (excluding other ChaserRobots) within the arena.
     *
     * @param arena The {@code RobotArena} instance containing all items.
     * @return The nearest {@code Robot} object, or null if no suitable target is found.
     */
    private Robot findNearestRobot(RobotArena arena) {
        Robot nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (ArenaItem item : arena.getItems()) {
            if (item instanceof Robot && !(item instanceof ChaserRobot)) {
                double distance = calculateDistance(item);
                if (distance < minDistance) {
                    minDistance = distance;
                    nearest = (Robot) item;
                }
            }
        }
        return nearest;
    }

    /**
     * Handles collision by reverting to the previous position, reversing direction
     * (with some randomization), and optionally applying cooldown or speed changes.
     *
     * @param oldX The previous x-coordinate before collision.
     * @param oldY The previous y-coordinate before collision.
     */
    private void handleCollision(double oldX, double oldY) {
        setX(oldX);
        setY(oldY);

        // Reverse direction with slight randomization to avoid repetitive bouncing
        double randomAngle = (Math.random() - 0.5) * 30; // ±15 degrees
        setDirection(normalizeDirection(getDirection() + 180 + randomAngle));
    }

    /**
     * Checks for boundary collisions and adjusts the robot's position/direction accordingly.
     *
     * @param arena The {@code RobotArena} managing the environment.
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
            // Optional: Additional collision logic
        }
    }

    /**
     * Normalizes any angle into the [0..360) range.
     *
     * @param angle The angle in degrees to normalize.
     * @return The normalized angle.
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
     * Returns a string representation of the {@code ChaserRobot}, including its position.
     *
     * @return A string describing the robot's type and position.
     */
    @Override
    public String toString() {
        return String.format("ChaserRobot at (%.0f, %.0f)", getX(), getY());
    }
}
