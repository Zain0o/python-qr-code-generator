package RobotSim;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.Glow;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Line;

import java.util.ArrayList;

/**
 * The {@code LightSeekingRobot} is a specialized robot that seeks out light sources in the arena.
 * It uses multiple sensors, each detecting the intensity of nearby light. The robot then turns
 * toward the strongest light reading and adjusts its speed accordingly. This class also contains
 * beams for additional detection or visual effects.
 *
 * <p>Key updates include improved sensor performance by skipping distant lights and storing
 * sensor data in arrays to optimize computations.</p>
 *
 * @version 1.0
 */
public class LightSeekingRobot extends Robot {

    // -----------------------------
    // Sensor Fields
    // -----------------------------

    /** The number of sensors used for detecting light. */
    private static final int NUM_SENSORS = 3;

    /** Angular spread in degrees for the sensors. */
    private static final double SENSOR_SPREAD = 90;

    /** Maximum range of each sensor in units. */
    private static final double SENSOR_RANGE = 150;

    /** Holds the computed light intensity for each sensor. */
    private final double[] sensorReadings;

    // -----------------------------
    // Movement & Behavior Fields
    // -----------------------------

    /** Turn rate (degrees per update) for smooth turning. */
    private static final double TURN_RATE = 1.0;

    /** Base movement speed when no significant light is detected. */
    private static final double BASE_SPEED = 1.0;

    /** Maximum speed boost applied when strong light is detected. */
    private static final double MAX_BOOST = 2.0;

    /** Tracks a glow intensity factor for visual feedback when light is detected. */
    private double glowIntensity = 0.0;

    // -----------------------------
    // Visual/Rendering Fields
    // -----------------------------

    /** Base body color of the robot. */
    private static final Color BODY_COLOR = Color.CORNFLOWERBLUE;

    /** Sensor color, later blended with readings for visual effect. */
    private static final Color SENSOR_COLOR = Color.ORANGE;

    // -----------------------------
    // Beam Fields
    // -----------------------------

    /** Number of beams for additional detection or visuals. */
    private static final int NUM_BEAMS = 2;

    /** Maximum length of each beam. */
    private static final double BEAM_LENGTH = 100;

    /** The angular spread (in degrees) for the beams. */
    private static final double BEAM_SPREAD = 60;

    /** Stores the line objects for each beam. */
    private final ArrayList<Line> beams;

    /** Tracks whether each beam is detecting an obstacle. */
    private final ArrayList<Boolean> beamDetections;

    /**
     * Constructs a new {@code LightSeekingRobot} at the specified position and radius.
     *
     * @param x      Initial x-coordinate of the robot.
     * @param y      Initial y-coordinate of the robot.
     * @param radius The radius of the robot's bounding circle.
     */
    public LightSeekingRobot(double x, double y, double radius) {
        super(x, y, radius);

        // Initialize sensor readings
        this.sensorReadings = new double[NUM_SENSORS];
        setSpeed(BASE_SPEED);

        // Initialize beams
        beams = new ArrayList<>();
        beamDetections = new ArrayList<>();
        for (int i = 0; i < NUM_BEAMS; i++) {
            beams.add(new Line(0, 0, 0, 0));
            beamDetections.add(false);
        }
    }

    // -----------------------------
    // Overridden Methods
    // -----------------------------

    /**
     * Updates the robot's state, including sensor readings, movement logic, and beam positioning.
     * This method calls the parent class for collision handling and default movement.
     *
     * @param arena The current {@code RobotArena} containing items and obstacles.
     */
    @Override
    public void updateState(RobotArena arena) {
        // 1. Update sensor readings based on the arena's light sources
        updateSensorReadings(arena);

        // 2. Determine which sensor detects the strongest light
        int maxSensorIndex = 0;
        double maxReading = sensorReadings[0];
        for (int i = 1; i < NUM_SENSORS; i++) {
            if (sensorReadings[i] > maxReading) {
                maxReading = sensorReadings[i];
                maxSensorIndex = i;
            }
        }

        // 3. Perform default movement logic from the parent class (collision, boundaries)
        super.updateState(arena);

        // 4. Movement logic based on light detection
        if (maxReading > 0.005) {
            // Calculate the offset angle for the sensor with maximum reading
            double sensorAngleOffset = -(SENSOR_SPREAD / 2)
                    + (SENSOR_SPREAD / (NUM_SENSORS - 1)) * maxSensorIndex;
            double targetAngle = getDirection() + sensorAngleOffset;

            // Normalize angle difference and clamp turning rate
            double angleDiff = normalizeDirection(targetAngle - getDirection());
            double turnThisFrame = Math.signum(angleDiff)
                    * Math.min(Math.abs(angleDiff), TURN_RATE);
            setDirection(getDirection() + turnThisFrame);

            // Speed based on light strength
            double newSpeed = BASE_SPEED + maxReading * MAX_BOOST;
            setSpeed(Math.min(getMaxSpeed(), newSpeed));

            // Glow intensity for visuals
            glowIntensity = maxReading;
        } else {
            // If light is weak, slow down and slowly rotate to search
            setSpeed(BASE_SPEED);
            setDirection(getDirection() + 1.0);
            glowIntensity = 0.1;
        }

        // 5. Update beam positions
        updateBeams();

        // (Optional) If beam collision detection is needed:
        // for (int i = 0; i < NUM_BEAMS; i++) {
        //     boolean detected = arena.intersectsAnyObstacle(beams.get(i));
        //     beamDetections.set(i, detected);
        // }
    }

    /**
     * Renders the LightSeekingRobot on the specified {@code GraphicsContext}, including a glow effect,
     * body, sensors, wheels, beams, and sensor range indicators when selected.
     *
     * @param gc The {@code GraphicsContext} on which to draw the robot.
     */
    @Override
    public void draw(GraphicsContext gc) {
        gc.save();
        gc.translate(getX(), getY());
        gc.rotate(getDirection());

        // Apply a glow effect if there's enough detected light
        if (glowIntensity > 0.1) {
            Glow glow = new Glow(glowIntensity);
            gc.setEffect(glow);
        }

        // Draw the main body with a radial gradient
        RadialGradient gradient = new RadialGradient(
                0, 0, 0, 0, getRadius(),
                false, CycleMethod.NO_CYCLE,
                new Stop(0, BODY_COLOR.brighter()),
                new Stop(0.8, BODY_COLOR),
                new Stop(1, BODY_COLOR.darker())
        );
        gc.setFill(gradient);
        gc.fillOval(-getRadius(), -getRadius(), getRadius() * 2, getRadius() * 2);

        // Draw sensors and wheels
        drawSensors(gc);
        drawWheels(gc);

        // Reset effect
        gc.setEffect(null);
        gc.restore();

        // Draw sensor ranges if this robot is selected
        if (isSelected()) {
            drawSensorRanges(gc);
        }

        // Draw beams in absolute coordinates
        drawBeams(gc);
    }

    // -----------------------------
    // Private Helper Methods
    // -----------------------------

    /**
     * Updates the sensor readings by scanning through all items in the arena for active light sources.
     * Skips processing lights that are beyond a certain bounding distance to improve performance.
     *
     * @param arena The current {@code RobotArena}.
     */
    private void updateSensorReadings(RobotArena arena) {
        // Reset sensor readings
        for (int i = 0; i < NUM_SENSORS; i++) {
            sensorReadings[i] = 0.0;
        }

        // Large bounding distance to skip obviously distant lights
        double boundingDistance = 1000.0;

        // Scan for LightSources
        for (ArenaItem item : arena.getItems()) {
            if (item instanceof LightSource light && light.isActive()) {
                double dx = light.getX() - getX();
                double dy = light.getY() - getY();
                double distSq = dx * dx + dy * dy;

                // Skip this light if beyond boundingDistance
                if (distSq > boundingDistance * boundingDistance) {
                    continue;
                }

                // For each sensor, compute position and accumulate light intensity
                for (int i = 0; i < NUM_SENSORS; i++) {
                    double sensorAngle = Math.toRadians(
                            getDirection() - (SENSOR_SPREAD / 2)
                                    + (SENSOR_SPREAD / (NUM_SENSORS - 1)) * i
                    );
                    double sensorX = getX() + getRadius() * Math.cos(sensorAngle);
                    double sensorY = getY() + getRadius() * Math.sin(sensorAngle);

                    sensorReadings[i] += light.getLightIntensityAt(sensorX, sensorY);
                }
            }
        }
    }

    /**
     * Updates the beam lines so they fan out around the robot's current heading.
     */
    private void updateBeams() {
        double startAngle = getDirection() - BEAM_SPREAD / 2;
        double angleStep = (NUM_BEAMS > 1) ? (BEAM_SPREAD / (NUM_BEAMS - 1)) : 0;

        for (int i = 0; i < NUM_BEAMS; i++) {
            double beamAngle = Math.toRadians(startAngle + i * angleStep);

            double endX = getX() + BEAM_LENGTH * Math.cos(beamAngle);
            double endY = getY() + BEAM_LENGTH * Math.sin(beamAngle);

            Line beam = beams.get(i);
            beam.setStartX(getX());
            beam.setStartY(getY());
            beam.setEndX(endX);
            beam.setEndY(endY);
        }
    }

    /**
     * Draws the robot's sensor points on the {@code GraphicsContext}, each tinted based on current
     * light reading.
     *
     * @param gc The {@code GraphicsContext} to draw upon.
     */
    private void drawSensors(GraphicsContext gc) {
        double sensorSize = getRadius() * 0.2;
        for (int i = 0; i < NUM_SENSORS; i++) {
            double angle = Math.toRadians(
                    -SENSOR_SPREAD / 2 + (SENSOR_SPREAD / (NUM_SENSORS - 1)) * i
            );
            double sx = getRadius() * Math.cos(angle);
            double sy = getRadius() * Math.sin(angle);

            // Blend sensor color with white based on intensity
            Color sensorGlow = SENSOR_COLOR.interpolate(Color.WHITE, sensorReadings[i]);
            gc.setFill(sensorGlow);
            gc.fillOval(sx - sensorSize / 2, sy - sensorSize / 2, sensorSize, sensorSize);

            // Highlight center of the sensor
            gc.setFill(Color.WHITE.deriveColor(1, 1, 1, 0.5));
            gc.fillOval(sx - sensorSize / 4, sy - sensorSize / 4, sensorSize / 2, sensorSize / 2);
        }
    }

    /**
     * Draws the robot's wheels.
     *
     * @param gc The {@code GraphicsContext} to draw upon.
     */
    private void drawWheels(GraphicsContext gc) {
        double wheelWidth = getRadius() * 0.3;
        double wheelHeight = getRadius() * 0.2;
        gc.setFill(Color.DARKGRAY);

        // Top wheel
        gc.fillRect(
                -wheelWidth / 2, -getRadius() - wheelHeight,
                wheelWidth, wheelHeight
        );

        // Bottom wheel
        gc.fillRect(
                -wheelWidth / 2, getRadius(),
                wheelWidth, wheelHeight
        );
    }

    /**
     * Draws each sensor’s range as a line from the robot’s center if the robot is selected.
     * The color intensity is based on the respective sensor reading.
     *
     * @param gc The {@code GraphicsContext} to draw upon.
     */
    private void drawSensorRanges(GraphicsContext gc) {
        gc.setLineWidth(1);
        for (int i = 0; i < NUM_SENSORS; i++) {
            double angle = Math.toRadians(
                    getDirection() - SENSOR_SPREAD / 2
                            + (SENSOR_SPREAD / (NUM_SENSORS - 1)) * i
            );
            gc.setStroke(Color.YELLOW.deriveColor(1, 1, 1, 0.2 + sensorReadings[i] * 0.8));
            gc.strokeLine(
                    getX(),
                    getY(),
                    getX() + SENSOR_RANGE * Math.cos(angle),
                    getY() + SENSOR_RANGE * Math.sin(angle)
            );
        }
    }

    /**
     * Draws each beam line in absolute/world coordinates, distinguishing between beams that detect
     * obstacles (optional logic) and those that do not.
     *
     * @param gc The {@code GraphicsContext} to draw upon.
     */
    private void drawBeams(GraphicsContext gc) {
        gc.setLineWidth(2);
        for (int i = 0; i < NUM_BEAMS; i++) {
            Line beam = beams.get(i);
            gc.setStroke(beamDetections.get(i) ? Color.RED : Color.GREEN);
            gc.strokeLine(
                    beam.getStartX(), beam.getStartY(),
                    beam.getEndX(), beam.getEndY()
            );
        }
    }
}
