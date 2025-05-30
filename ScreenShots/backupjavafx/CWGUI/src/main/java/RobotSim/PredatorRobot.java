package RobotSim;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Line;
import java.util.ArrayList;

/**
 * The PredatorRobot class represents a specialized Robot that hunts other robots
 * within a defined range. It includes sensor-based obstacle detection and
 * dynamic speed adjustments when pursuing its target.
 *
 * <p>This class extends {@link Robot} and overrides its methods to include
 * predator-specific behavior, such as hunting, prey detection, and sensor-based
 * obstacle avoidance.</p>
 */
public class PredatorRobot extends Robot {

    /** The maximum distance within which the PredatorRobot detects prey. */
    private static final double HUNT_RANGE = 200.0;

    /** The maximum possible speed of the PredatorRobot when chasing. */
    private static final double MAX_SPEED = 5.0;

    /** Stores the current target robot being hunted. */
    private Robot target;

    /** Tracks how long the PredatorRobot must wait before hunting again. */
    private double huntTimer = 0;

    /** Cooldown period (in update cycles) after a successful hunt. */
    private static final double HUNT_COOLDOWN = 100;

    /** Number of sensors used by the PredatorRobot for obstacle detection. */
    private static final int NUM_SENSORS = 2;

    /** Spread angle (in degrees) for the sensors. */
    private static final double SENSOR_SPREAD = 60;

    /** Maximum length of each sensor line. */
    private static final double SENSOR_LENGTH = 100;

    /** Holds the line objects representing each sensor. */
    private final ArrayList<Line> sensors;

    /** Holds the detection states (true/false) for each sensor. */
    private final ArrayList<Boolean> sensorDetections;

    /**
     * Constructs a PredatorRobot at the specified x/y coordinates with a given radius.
     *
     * @param x       The initial x-coordinate of the robot.
     * @param y       The initial y-coordinate of the robot.
     * @param radius  The radius of the robot.
     */
    public PredatorRobot(double x, double y, double radius) {
        super(x, y, radius);
        setSpeed(3.0);

        sensors = new ArrayList<>();
        sensorDetections = new ArrayList<>();

        // Initialize the sensors and detection flags
        for (int i = 0; i < NUM_SENSORS; i++) {
            sensors.add(new Line());
            sensorDetections.add(false);
        }
    }

    /**
     * Updates the state of the PredatorRobot, including hunting behavior,
     * sensor detection, and movement.
     *
     * @param arena  The current RobotArena which contains all items and obstacles.
     */
    @Override
    public void updateState(RobotArena arena) {
        // Decrease hunt cooldown timer if it's above zero
        if (huntTimer > 0) {
            huntTimer--;
        }

        // Attempt to find a new target if not currently hunting and the cooldown is over
        if (target == null && huntTimer == 0) {
            target = findClosestPrey(arena);
            if (target != null) {
                DebugUtils.log("PredatorRobot found a target!");
            }
        }

        // If a target exists, perform hunting logic
        if (target != null) {
            double dx = target.getX() - getX();
            double dy = target.getY() - getY();
            double distance = Math.sqrt(dx * dx + dy * dy);

            // Move towards target if within hunt range
            if (distance < HUNT_RANGE) {
                // Predict movement if target is a ChaserRobot
                if (target instanceof ChaserRobot) {
                    double predictedX = target.getX() + target.getSpeed() *
                            Math.cos(Math.toRadians(target.getDirection()));
                    double predictedY = target.getY() + target.getSpeed() *
                            Math.sin(Math.toRadians(target.getDirection()));
                    setDirection(Math.toDegrees(Math.atan2(predictedY - getY(), predictedX - getX())));
                } else {
                    // Otherwise, just aim directly at the target
                    setDirection(Math.toDegrees(Math.atan2(dy, dx)));
                }

                // Apply dynamic speed boost based on distance to target
                double speedBoost = (HUNT_RANGE - distance) / HUNT_RANGE;
                setSpeed(Math.min(MAX_SPEED, 3.0 + speedBoost * 2.0));

                // If within a collision range, remove the target from the arena
                if (distance < (getRadius() + target.getRadius() * 1.2)) {
                    arena.deleteItem(target);  // Removes the target from the arena
                    target = null;             // Reset target
                    huntTimer = HUNT_COOLDOWN; // Apply cooldown
                    setSpeed(2.0);            // Reset speed
                    DebugUtils.log("PredatorRobot caught its prey!");
                }
            } else {
                // Target out of range, stop hunting
                target = null;
            }
        }

        // Update sensors to check for and avoid obstacles
        updateSensors(arena);

        // Perform the default movement update from the Robot class
        super.updateState(arena);
    }

    /**
     * Finds the closest prey in the arena. Priority is given to {@link ChaserRobot},
     * and then any other Robot that is not a PredatorRobot.
     *
     * @param arena  The RobotArena to scan for potential targets.
     * @return       The closest suitable Robot target, or null if none found.
     */
    private Robot findClosestPrey(RobotArena arena) {
        Robot closest = null;
        double minDistance = HUNT_RANGE;

        // Loop through all items in the arena
        for (ArenaItem item : arena.getItems()) {
            // Prioritize ChaserRobot
            if (item instanceof ChaserRobot) {
                double distance = calculateDistance(item);
                if (distance < minDistance) {
                    minDistance = distance;
                    closest = (Robot) item;
                }
            }
            // Next, consider other Robots (except PredatorRobots and itself)
            else if (item instanceof Robot && !(item instanceof PredatorRobot) && item != this) {
                double distance = calculateDistance(item);
                if (distance < minDistance) {
                    minDistance = distance;
                    closest = (Robot) item;
                }
            }
        }
        return closest;
    }

    /**
     * Updates the sensor lines for obstacle detection and adjusts direction
     * if any sensor detects an obstacle.
     *
     * @param arena  The RobotArena that may contain obstacles or walls.
     */
    private void updateSensors(RobotArena arena) {
        // Determine the starting angle and angle step for each sensor
        double startAngle = getDirection() - SENSOR_SPREAD / 2;
        double angleStep = (NUM_SENSORS > 1) ? SENSOR_SPREAD / (NUM_SENSORS - 1) : 0;

        for (int i = 0; i < NUM_SENSORS; i++) {
            double sensorAngle = Math.toRadians(startAngle + i * angleStep);

            // Start position at the robot's edge
            double startX = getX() + getRadius() * Math.cos(sensorAngle);
            double startY = getY() + getRadius() * Math.sin(sensorAngle);

            // End position extended by SENSOR_LENGTH
            double endX = startX + SENSOR_LENGTH * Math.cos(sensorAngle);
            double endY = startY + SENSOR_LENGTH * Math.sin(sensorAngle);

            // Update the sensor line object
            Line sensor = sensors.get(i);
            sensor.setStartX(startX);
            sensor.setStartY(startY);
            sensor.setEndX(endX);
            sensor.setEndY(endY);

            // Check if this sensor line intersects any obstacle
            boolean detected = arena.intersectsAnyObstacle(sensor);
            sensorDetections.set(i, detected);

            // If an obstacle is detected, adjust direction slightly
            if (detected) {
                setDirection(getDirection() + (i == 0 ? -10 : 10));
            }
        }
    }

    /**
     * Draws the PredatorRobot and its sensors on the provided GraphicsContext.
     *
     * @param gc  The GraphicsContext onto which the PredatorRobot is drawn.
     */
    @Override
    public void draw(GraphicsContext gc) {
        gc.save();
        gc.translate(getX(), getY());
        gc.rotate(getDirection());

        // Create a radial gradient for the robot's body
        RadialGradient gradient = new RadialGradient(
                0, 0, 0, 0, getRadius(),
                false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.DARKRED),
                new Stop(0.7, Color.RED),
                new Stop(1, Color.BLACK)
        );
        gc.setFill(gradient);
        gc.fillOval(-getRadius(), -getRadius(), getRadius() * 2, getRadius() * 2);

        // Draw wheels
        drawWheels(gc);

        gc.restore();

        // Draw sensor lines in either red (if detecting) or green (if clear)
        for (int i = 0; i < NUM_SENSORS; i++) {
            Line sensor = sensors.get(i);
            gc.setStroke(sensorDetections.get(i) ? Color.RED : Color.GREEN);
            gc.setLineWidth(2);
            gc.strokeLine(sensor.getStartX(), sensor.getStartY(), sensor.getEndX(), sensor.getEndY());
        }

        // Draw glowing eyes on the PredatorRobot
        gc.setFill(Color.YELLOW);
        double eyeSize = getRadius() * 0.3;
        gc.fillOval(-getRadius() * 0.5 - eyeSize / 2, -getRadius() * 0.4, eyeSize, eyeSize);
        gc.fillOval(getRadius() * 0.5 - eyeSize / 2, -getRadius() * 0.4, eyeSize, eyeSize);

        // Add pupils to the eyes
        gc.setFill(Color.RED);
        double pupilSize = eyeSize * 0.4;
        gc.fillOval(-getRadius() * 0.5 - pupilSize / 2, -getRadius() * 0.35, pupilSize, pupilSize);
        gc.fillOval(getRadius() * 0.5 - pupilSize / 2, -getRadius() * 0.35, pupilSize, pupilSize);
    }

    /**
     * Draws the wheels of the PredatorRobot.
     *
     * @param gc  The GraphicsContext used to draw the wheels.
     */
    private void drawWheels(GraphicsContext gc) {
        double wheelWidth = getRadius() * 0.3;
        double wheelHeight = getRadius() * 0.2;
        double arcSize = wheelHeight * 0.5;

        gc.setFill(Color.BLACK);

        // Top wheel
        gc.fillRoundRect(
                -wheelWidth / 2,
                -getRadius() - wheelHeight,
                wheelWidth,
                wheelHeight,
                arcSize,
                arcSize
        );

        // Bottom wheel
        gc.fillRoundRect(
                -wheelWidth / 2,
                getRadius(),
                wheelWidth,
                wheelHeight,
                arcSize,
                arcSize
        );
    }
}
