package RobotSim;

import javafx.scene.input.MouseEvent;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DebugUtils provides utility methods for logging debug information
 * for the Robot Simulator application. This class supports logging
 * robot states, file operations, and other events.
 */
public class DebugUtils {

    /** Flag to enable or disable debug logging. */
    private static final boolean DEBUG_ENABLED = true;

    /** Path to the log file for storing debug information. */
    private static final String LOG_FILE = "robot_sim_debug.log";

    /** Writer to append log messages to the log file. */
    private static PrintWriter logWriter;

    /** Tracks the last update time of robots using their IDs. */
    private static final Map<Integer, Long> lastRobotUpdate = new ConcurrentHashMap<>();

    /** Formatter for timestamps in log messages. */
    private static final DateTimeFormatter timeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    // Static block to initialize the log writer
    static {
        try {
            logWriter = new PrintWriter(new FileWriter(LOG_FILE, true));
        } catch (IOException e) {
            System.err.println("Failed to initialize debug log: " + e.getMessage());
        }
    }

    /**
     * Logs a general message with the timestamp, class, and method name.
     *
     * @param message the message to log
     */
    public static void log(String message) {
        if (!DEBUG_ENABLED) return;

        // Get the caller's class and method for context
        StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
        String className = caller.getClassName();
        String methodName = caller.getMethodName();
        String timestamp = LocalDateTime.now().format(timeFormatter);

        // Format the log message
        String logMessage = String.format("[%s] %s.%s: %s",
                timestamp, className, methodName, message);

        // Print and write the log message
        System.out.println(logMessage);
        logWriter.println(logMessage);
        logWriter.flush();
    }

    /**
     * Logs an event with a category and message.
     *
     * @param category the category of the event (e.g., "FILE", "SENSOR")
     * @param msg the message describing the event
     */
    public static void logEvent(String category, String msg) {
        if (!DEBUG_ENABLED) return;
        String time = LocalDateTime.now().format(timeFormatter);
        String output = String.format("[%s] %s: %s", time, category, msg);
        System.out.println(output);
        logWriter.println(output);
        logWriter.flush();
    }

    /**
     * Logs the result of a file operation (e.g., read, write, delete).
     *
     * @param operation the type of file operation (e.g., "WRITE", "READ")
     * @param filename the name of the file involved
     * @param success whether the operation was successful
     */
    public static void logFileOperation(String operation, String filename, boolean success) {
        logEvent("FILE", String.format("%s %s: %s",
                operation,
                success ? "succeeded" : "failed",
                filename));
    }

    /**
     * Logs the current state of a robot, including its position, direction, and speed.
     *
     * @param robot the robot whose state is logged
     */
    public static void logRobotState(Robot robot) {
        if (!DEBUG_ENABLED) return;
        log(String.format("Robot[id=%d] state: pos=(%.2f,%.2f), direction=%.2f°, speed=%.2f",
                robot.getId(), robot.getX(), robot.getY(),
                robot.getDirection(), robot.getSpeed()));
    }

    /**
     * Logs significant changes in a robot's speed.
     *
     * @param robot the robot whose speed changed
     * @param oldSpeed the previous speed of the robot
     * @param newSpeed the new speed of the robot
     */
    public static void logSpeedUpdate(Robot robot, double oldSpeed, double newSpeed) {
        if (!DEBUG_ENABLED) return;
        if (Math.abs(newSpeed - oldSpeed) > 0.1) { // Only log significant changes
            log(String.format("Robot[id=%d] speed changed: %.2f -> %.2f",
                    robot.getId(), oldSpeed, newSpeed));
        }
    }

    /**
     * Logs a collision between two arena items.
     *
     * @param item1 the first item involved in the collision
     * @param item2 the second item involved in the collision
     */
    public static void logCollision(ArenaItem item1, ArenaItem item2) {
        if (!DEBUG_ENABLED) return;
        log(String.format("Collision between %s[%d] and %s[%d]",
                item1.getClass().getSimpleName(), item1.getId(),
                item2.getClass().getSimpleName(), item2.getId()));
    }

    /**
     * Logs a mouse event, including its type and coordinates.
     *
     * @param mouseEvent the mouse event to log
     * @param eventType the type of the mouse event (e.g., "clicked", "moved")
     */
    public static void logMouseEvent(MouseEvent mouseEvent, String eventType) {
        if (!DEBUG_ENABLED) return;
        log(String.format("Mouse %s at (%.2f, %.2f)",
                eventType, mouseEvent.getX(), mouseEvent.getY()));
    }

    /**
     * Logs the current state of the arena, including the number of items and dimensions.
     *
     * @param arena the arena whose state is logged
     */
    public static void logArenaState(RobotArena arena) {
        if (!DEBUG_ENABLED) return;
        log(String.format("Arena state: %d items, selected=%s, dimensions=%.0fx%.0f",
                arena.getItems().size(),
                arena.getSelectedItem() != null ?
                        arena.getSelectedItem().getClass().getSimpleName() + "[" +
                                arena.getSelectedItem().getId() + "]" : "none",
                arena.getWidth(),
                arena.getHeight()));
    }

    /**
     * Logs whether a robot's sensor detected an obstacle.
     *
     * @param robot the robot whose sensor is logging the detection
     * @param sensorType the type of sensor (e.g., "whisker")
     * @param detected whether the obstacle was detected
     */
    public static void logSensorDetection(Robot robot, String sensorType, boolean detected) {
        if (!DEBUG_ENABLED) return;
        log(String.format("%s[%d] %s sensor detected obstacle: %b",
                robot.getClass().getSimpleName(), robot.getId(),
                sensorType, detected));
    }

    /**
     * Logs the detection of an obstacle by a robot's beam sensor.
     *
     * @param robotId the ID of the robot
     * @param beamId the ID of the beam sensor
     * @param detected whether the beam sensor detected an obstacle
     */
    public static void logBeamSensor(int robotId, int beamId, boolean detected) {
        if (detected) {
            logEvent("SENSOR", String.format("Robot[%d] BEAM_%d detected obstacle", robotId, beamId));
        }
    }

    /**
     * Cleans up resources by closing the log writer.
     */
    public static void cleanup() {
        if (logWriter != null) {
            logWriter.close();
        }
    }
}
