package RobotSim;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import java.util.ArrayList;

/**
 * The {@code BeamRobot} class extends {@link Robot} to include multiple beam sensors
 * for obstacle detection. These sensors are represented by {@link Line} objects
 * that fan out at specific angles from the robot's heading.
 *
 * <p>The BeamRobot uses these beams to check for obstacles in its path and
 * adjust its direction if collisions are detected.</p>
 *
 * @version 1.0
 */
public class BeamRobot extends Robot {

    /** The number of beam sensors. */
    private static final int NUM_BEAMS = 2; // Changed from 5 to 2

    /** The length of each beam sensor. */
    private static final double BEAM_LENGTH = 100;

    /** The total angular spread (in degrees) covered by the beams. */
    private static final double BEAM_SPREAD = 60;

    /** A list of beams (as {@link Line} objects). */
    private final ArrayList<Line> beams;

    /** A parallel list indicating whether each beam has detected an obstacle. */
    private final ArrayList<Boolean> beamDetections;

    /**
     * Creates a new {@code BeamRobot} with the specified position and size.
     *
     * @param x      The x-coordinate of the robot's center.
     * @param y      The y-coordinate of the robot's center.
     * @param radius The radius (size) of the robot.
     */
    public BeamRobot(double x, double y, double radius) {
        super(x, y, radius);

        beams = new ArrayList<>();
        beamDetections = new ArrayList<>();

        // Initialize beams and detection flags
        for (int i = 0; i < NUM_BEAMS; i++) {
            beams.add(new Line(0, 0, 0, 0)); // Default start/end
            beamDetections.add(false);
        }
    }

    /**
     * Updates the state of the {@code BeamRobot} within the arena.
     * This includes updating beam positions, checking beam collisions,
     * and adjusting the robot's direction as needed.
     *
     * @param arena The {@code RobotArena} managing all items in the environment.
     */
    @Override
    public void updateState(RobotArena arena) {
        // 1. Update beam lines based on current direction
        updateBeams();

        boolean collisionDetected = false;

        // 2. Check each beam for collisions with obstacles
        for (int i = 0; i < NUM_BEAMS; i++) {
            boolean detected = arena.intersectsAnyObstacle(beams.get(i));
            beamDetections.set(i, detected);

            if (detected) {
                collisionDetected = true;
                DebugUtils.log("Beam " + i + " detected collision");
            }
        }

        // 3. If any collision is detected, adjust robot's direction
        if (collisionDetected) {
            int clearestBeam = findClearestDirection();
            if (clearestBeam != -1) {
                // Calculate new direction based on the clearest beam
                double targetAngle = getDirection() - BEAM_SPREAD / 2
                        + (BEAM_SPREAD / (NUM_BEAMS - 1)) * clearestBeam;
                setDirection(targetAngle);
            } else {
                // All beams detect collisions: reverse direction
                setDirection(getDirection() + 180);
            }
        }

        // 4. Proceed with the default robot update (movement, boundary checks, etc.)
        super.updateState(arena);
    }

    /**
     * Updates the positions of the beam lines to match the robot's current heading.
     */
    private void updateBeams() {
        double startAngle = getDirection() - BEAM_SPREAD / 2;
        double angleStep = (NUM_BEAMS > 1) ? BEAM_SPREAD / (NUM_BEAMS - 1) : 0;

        for (int i = 0; i < NUM_BEAMS; i++) {
            double beamAngle = Math.toRadians(startAngle + i * angleStep);

            double endX = getX() + BEAM_LENGTH * Math.cos(beamAngle);
            double endY = getY() + BEAM_LENGTH * Math.sin(beamAngle);

            // Update the beam line
            Line beam = beams.get(i);
            beam.setStartX(getX());
            beam.setStartY(getY());
            beam.setEndX(endX);
            beam.setEndY(endY);
        }
    }

    /**
     * Finds the index of the beam that does not detect a collision.
     * If all beams detect collisions, returns -1.
     *
     * @return The index of the clearest beam, or -1 if none are clear.
     */
    private int findClearestDirection() {
        for (int i = 0; i < NUM_BEAMS; i++) {
            if (!beamDetections.get(i)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Draws the {@code BeamRobot} on the specified {@code GraphicsContext},
     * including its body, wheels, and beam lines.
     *
     * @param gc The {@code GraphicsContext} used to draw.
     */
    @Override
    public void draw(GraphicsContext gc) {
        // Save current transform
        gc.save();
        gc.translate(getX(), getY());
        gc.rotate(getDirection());

        // Draw the robot's body
        gc.setFill(Color.YELLOW);
        gc.fillOval(-getRadius(), -getRadius(), getRadius() * 2, getRadius() * 2);

        // Draw wheels
        drawWheels(gc);

        // Restore transform before drawing beams
        gc.restore();

        // Draw beam lines in world coordinates
        for (int i = 0; i < NUM_BEAMS; i++) {
            Line beam = beams.get(i);
            gc.setStroke(beamDetections.get(i) ? Color.RED : Color.GREEN);
            gc.setLineWidth(2);
            gc.strokeLine(
                    beam.getStartX(), beam.getStartY(),
                    beam.getEndX(), beam.getEndY()
            );
        }

        // Highlight the robot's identifier or center
        gc.setFill(Color.ORANGE);
        gc.fillOval(
                getX() - getRadius() / 3,
                getY() - getRadius() / 3,
                getRadius() / 1.5,
                getRadius() / 1.5
        );
    }

    /**
     * Draws the wheels of the robot on the provided {@code GraphicsContext}.
     *
     * @param gc The {@code GraphicsContext} used to draw.
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
