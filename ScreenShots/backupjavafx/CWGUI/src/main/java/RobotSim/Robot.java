package RobotSim;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import org.json.JSONObject;

/**
 * The {@code Robot} class represents a mobile robot within the {@link RobotArena}.
 * It includes a "whisker" line sensor to detect obstacles, a translucent sensor range circle ("beam"),
 * and two small black "wheels" (or treads). The robot can move autonomously, detect collisions,
 * and respond to user interactions.
 *
 * <p>The Robot class handles movement, collision detection with both arena boundaries and other robots,
 * and provides methods to save and load its state using JSON objects.</p>
 *
 * @version 1.0
 */
public class Robot extends ArenaItem {
    // -----------------------------
    // Constants
    // -----------------------------
    private static final double AVOIDANCE_RADIUS = 120.0;
    private static final double TURN_RATE = 7.5;
    private static final double MIN_BOUNCE_ANGLE = 35.0;
    private static final int MAX_STUCK_COUNT = 5;
    private static final int COLLISION_RECOVERY_TIME = 15;
    private static final double HALF_ROTATION = 180.0;
    private static final double FULL_ROTATION = 360.0;
    private static final double EYE_OFFSET_X = 0.25;
    private static final double EYE_OFFSET_Y = -0.2;
    private static final double EYE_SIZE = 0.2;
    private static final long BLINK_INTERVAL = 3000; // 3 seconds
    private static final long BLINK_DURATION = 150;   // 150 ms

    // -----------------------------
    // Movement Properties
    // -----------------------------
    private double speed;
    private double direction;
    private double targetDirection;
    private final double maxSpeed = 4.0;
    private final double minSpeed = 0.5;
    private final double acceleration = 0.1;
    private final double deceleration = 0.2;
    private double destinationX;
    private double destinationY;

    // -----------------------------
    // Sensor Properties
    // -----------------------------
    private double sensorRange;
    private double whiskerLength;
    private final int numWhiskers = 3;
    private final double whiskerSpread = 45.0;

    // -----------------------------
    // Collision Handling
    // -----------------------------
    private int stuckCounter;
    private int collisionCooldown;
    private double lastX, lastY;
    private boolean isStuck;

    // -----------------------------
    // Animation and Timing Fields
    // -----------------------------
    private boolean isBlinking = false;
    private long lastBlinkTime = System.currentTimeMillis();

    // -----------------------------
    // Constructor
    // -----------------------------

    /**
     * Constructs a new {@code Robot} with the specified position and size.
     *
     * @param x      the x-coordinate of the robot's centre
     * @param y      the y-coordinate of the robot's centre
     * @param radius the radius of the robot's bounding circle
     */
    public Robot(double x, double y, double radius) {
        super(x, y, radius);
        this.speed = 2.0;
        this.direction = Math.random() * 360;
        this.targetDirection = this.direction;
        this.sensorRange = radius * 2.5;
        this.whiskerLength = radius * 2;
        this.lastX = x;
        this.lastY = y;
        this.destinationX = x;
        this.destinationY = y;
        this.stuckCounter = 0;
        this.collisionCooldown = 0;
        this.isStuck = false;
    }

    // -----------------------------
    // Public Methods
    // -----------------------------

    /**
     * Updates the state of the {@code Robot} within the arena.
     * Handles collisions, movement, and state changes.
     *
     * @param arena the {@code RobotArena} managing the environment
     */
    @Override
    public void updateState(RobotArena arena) {
        // If we're in collision cooldown, decrement it
        if (collisionCooldown > 0) {
            collisionCooldown--;
        }

        // Store current position
        double oldX = getX();
        double oldY = getY();
        double rawSpeed = getSpeed();
        // Multiply by the global speedFactor
        double effectiveSpeed = rawSpeed * arena.getSpeedFactor();

        // Calculate the new position based on direction and effective speed
        double radians = Math.toRadians(getDirection());
        double newX = getX() + effectiveSpeed * Math.cos(radians);
        double newY = getY() + effectiveSpeed * Math.sin(radians);

        // Check if new position would collide
        if (!arena.isColliding(newX, newY, getRadius(), getId())) {
            // Safe to move, so update position
            setX(newX);
            setY(newY);

            // Gradually accelerate if not in collision cooldown
            if (getSpeed() < maxSpeed && collisionCooldown == 0) {
                setSpeed(Math.min(maxSpeed, getSpeed() + acceleration));
            }
        } else {
            // Try to find a clear path by testing multiple angles
            boolean foundClearPath = false;
            double testDistance = getRadius() * 2;

            for (int i = 0; i < 8; i++) {
                double testAngle = Math.toRadians(getDirection() + (45 * i));
                double testX = oldX + testDistance * Math.cos(testAngle);
                double testY = oldY + testDistance * Math.sin(testAngle);

                if (!arena.isColliding(testX, testY, getRadius(), getId())) {
                    setX(testX);
                    setY(testY);
                    setDirection(Math.toDegrees(testAngle));
                    foundClearPath = true;
                    break;
                }
            }

            if (!foundClearPath) {
                // If no clear path, make a dramatic change
                setDirection(getDirection() + 180 + (Math.random() - 0.5) * 90);
                setSpeed(maxSpeed * 0.5);
                collisionCooldown = COLLISION_RECOVERY_TIME;

                // Force a small move in the new direction
                radians = Math.toRadians(getDirection());
                setX(oldX + getRadius() * Math.cos(radians));
                setY(oldY + getRadius() * Math.sin(radians));
            }
        }

        // Check if we're stuck (didn't move enough)
        if (Math.abs(getX() - oldX) < 0.1 && Math.abs(getY() - oldY) < 0.1) {
            stuckCounter++;
            if (stuckCounter > MAX_STUCK_COUNT) {
                // Force an escape
                setDirection(Math.random() * 360);
                setSpeed(maxSpeed);
                double escapeAngle = Math.toRadians(getDirection());
                setX(oldX + getRadius() * 2 * Math.cos(escapeAngle));
                setY(oldY + getRadius() * 2 * Math.sin(escapeAngle));
                stuckCounter = 0;
                collisionCooldown = 0;
            }
        } else {
            stuckCounter = 0;
        }

        // Finally, check boundary collisions
        checkBoundaryCollisions(arena);
    }

    /**
     * Registers a collision with another robot, triggering a collision response.
     * This method reverses the robot's direction, reduces its speed, and logs the collision.
     */
    public void registerCollision() {
        // Reverse direction by 180° and reduce speed without dropping below the minimum
        setDirection(normalizeDirection(getDirection() + HALF_ROTATION));
        setSpeed(Math.max(minSpeed, getSpeed() - 1.0));


    }

    /**
     * Saves the current state of the robot as a JSON object.
     *
     * @return a {@code JSONObject} representing the robot's state
     */
    public JSONObject saveState() {
        JSONObject state = new JSONObject();
        state.put("x", getX());
        state.put("y", getY());
        state.put("direction", direction);
        state.put("speed", speed);
        state.put("radius", getRadius());
        state.put("type", this.getClass().getSimpleName());
        return state;
    }

    /**
     * Loads the robot's state from a JSON object.
     *
     * @param state a {@code JSONObject} representing the robot's state
     */
    public void loadState(JSONObject state) {
        setX(state.getDouble("x"));
        setY(state.getDouble("y"));
        setDirection(state.getDouble("direction"));
        setSpeed(state.getDouble("speed"));
        setRadius(state.getDouble("radius"));
    }

    /**
     * Provides a string representation of the {@code Robot}, including its type, ID, position, speed, and direction.
     *
     * @return a string describing the robot's type, ID, position, speed, and direction
     */
    @Override
    public String toString() {
        return String.format("%s (ID: %d)\nPosition: (%.2f, %.2f)\nSpeed: %.2f\nDirection: %.2f°",
                this.getClass().getSimpleName(),
                getId(),
                getX(),
                getY(),
                speed,
                direction);
    }

    // -----------------------------
    // Protected Methods
    // -----------------------------

    /**
     * Normalises any angle into the [0..360) degree range.
     *
     * @param angle the angle in degrees to normalise
     * @return the normalised angle
     */
    protected double normalizeDirection(double angle) {
        angle %= FULL_ROTATION;
        if (angle < 0) {
            angle += FULL_ROTATION;
        }
        return angle;
    }

    // -----------------------------
    // Private Helper Methods
    // -----------------------------

    /**
     * Checks for collisions with arena boundaries and adjusts direction accordingly.
     *
     * @param arena the {@code RobotArena} managing the environment
     */
    private void checkBoundaryCollisions(RobotArena arena) {
        boolean hadCollision = false;
        double newDirection = getDirection();
        double buffer = getRadius() * 0.1; // Small buffer to prevent edge cases

        // Check and handle X-axis boundaries
        if (getX() - getRadius() < 0) {
            setX(getRadius() + buffer);
            newDirection = normalizeDirection(180 - getDirection());
            hadCollision = true;
        } else if (getX() + getRadius() > arena.getWidth()) {
            setX(arena.getWidth() - getRadius() - buffer);
            newDirection = normalizeDirection(180 - getDirection());
            hadCollision = true;
        }

        // Check and handle Y-axis boundaries
        if (getY() - getRadius() < 0) {
            setY(getRadius() + buffer);
            newDirection = normalizeDirection(360 - getDirection());
            hadCollision = true;
        } else if (getY() + getRadius() > arena.getHeight()) {
            setY(arena.getHeight() - getRadius() - buffer);
            newDirection = normalizeDirection(360 - getDirection());
            hadCollision = true;
        }

        if (hadCollision) {
            // Add randomization to avoid corner sticking
            newDirection += (Math.random() - 0.5) * 45; // ±22.5 degrees randomness
            setDirection(normalizeDirection(newDirection));

            // Reduce speed on boundary collision
            setSpeed(Math.max(minSpeed, getSpeed() * 0.7));

            // Force a small move in the new direction
            double pushDistance = getRadius() * 0.5;
            double radians = Math.toRadians(newDirection);
            setX(getX() + pushDistance * Math.cos(radians));
            setY(getY() + pushDistance * Math.sin(radians));

            // Reset collision state
            collisionCooldown = COLLISION_RECOVERY_TIME;
            stuckCounter = 0;


        }
    }

    // -----------------------------
    // Overridden Public Methods
    // -----------------------------

    /**
     * Determines if this robot is colliding with the given {@code ArenaItem}.
     *
     * @param item the {@code ArenaItem} to check collision with
     * @return {@code true} if colliding, {@code false} otherwise
     */
    @Override
    public boolean isColliding(ArenaItem item) {
        double distance = calculateDistance(item);
        return distance < (getRadius() + item.getRadius());
    }

    /**
     * Sets the X-coordinate of the robot's position.
     *
     * @param x The new X-coordinate.
     */
    @Override
    public void setX(double x) {
        super.setX(x);
    }

    /**
     * Sets the Y-coordinate of the robot's position.
     *
     * @param y The new Y-coordinate.
     */
    @Override
    public void setY(double y) {
        super.setY(y);
    }

    /**
     * Returns the current destination X-coordinate.
     *
     * @return the destination X value
     */
    public double getDestinationX() {
        return this.destinationX;
    }

    /**
     * Returns the current destination Y-coordinate.
     *
     * @return the destination Y value
     */
    public double getDestinationY() {
        return this.destinationY;
    }

    /**
     * Calculates the distance between this robot and the given {@code ArenaItem}.
     *
     * @param item the {@code ArenaItem} to calculate distance to
     * @return the distance between the robot and the item
     */
    @Override
    public double calculateDistance(ArenaItem item) {
        double dx = getX() - item.getX();
        double dy = getY() - item.getY();
        return Math.hypot(dx, dy);
    }

    // -----------------------------
    // Getters and Setters
    // -----------------------------

    /**
     * Retrieves the sensor range of the robot.
     *
     * @return the sensor range
     */
    public double getSensorRange() {
        return sensorRange;
    }

    /**
     * Sets the sensor range of the robot.
     *
     * @param sensorRange the new sensor range
     */
    public void setSensorRange(double sensorRange) {
        this.sensorRange = sensorRange;
    }

    /**
     * Retrieves the current movement speed of the robot.
     *
     * @return the current speed
     */
    public double getSpeed() {
        return speed;
    }

    /**
     * Sets the movement speed of the robot.
     *
     * @param speed the new speed value
     */
    public void setSpeed(double speed) {
        // Ensure speed stays within bounds
        if (speed < minSpeed) {
            this.speed = minSpeed;
        } else if (speed > maxSpeed) {
            this.speed = maxSpeed;
        } else {
            this.speed = speed;
        }
    }

    /**
     * Retrieves the current movement direction of the robot in degrees.
     *
     * @return the current direction
     */
    public double getDirection() {
        return direction;
    }

    /**
     * Sets the movement direction of the robot.
     *
     * @param direction the new direction in degrees
     */
    public void setDirection(double direction) {
        this.direction = normalizeDirection(direction);
    }

    /**
     * Increments the stuck counter.
     */
    protected void incrementStuckCounter() {
        stuckCounter++;
    }

    /**
     * Retrieves the maximum stuck count.
     *
     * @return the maximum stuck count
     */
    protected int getMaxStuckCount() {
        return MAX_STUCK_COUNT;
    }

    /**
     * Retrieves the current target direction of the robot.
     *
     * @return the target direction
     */
    public double getTargetDirection() {
        return targetDirection;
    }

    /**
     * Sets the target direction of the robot.
     *
     * @param targetDirection the new target direction in degrees
     */
    public void setTargetDirection(double targetDirection) {
        this.targetDirection = normalizeDirection(targetDirection);
    }

    /**
     * Retrieves the current length of the whisker line sensor.
     *
     * @return the whisker length
     */
    public double getWhiskerLength() {
        return whiskerLength;
    }

    /**
     * Sets the length of the whisker line sensor.
     *
     * @param whiskerLength the new whisker length
     */
    public void setWhiskerLength(double whiskerLength) {
        this.whiskerLength = whiskerLength;
    }

    /**
     * Retrieves the number of whiskers the robot has.
     *
     * @return the number of whiskers
     */
    public int getNumWhiskers() {
        return numWhiskers;
    }

    /**
     * Retrieves the spread angle of the whiskers in degrees.
     *
     * @return the whisker spread angle
     */
    public double getWhiskerSpread() {
        return whiskerSpread;
    }

    /**
     * Retrieves the maximum speed of the robot.
     *
     * @return the maximum speed
     */
    public double getMaxSpeed() {
        return maxSpeed;
    }

    /**
     * Retrieves the minimum speed of the robot.
     *
     * @return the minimum speed
     */
    public double getMinSpeed() {
        return minSpeed;
    }

    /**
     * Retrieves the current stuck counter value.
     *
     * @return the stuck counter
     */
    public int getStuckCounter() {
        return stuckCounter;
    }

    /**
     * Sets the stuck counter value.
     *
     * @param stuckCounter the new stuck counter value
     */
    public void setStuckCounter(int stuckCounter) {
        this.stuckCounter = stuckCounter;
    }

    /**
     * Checks if the robot is currently stuck.
     *
     * @return {@code true} if the robot is stuck, {@code false} otherwise
     */
    public boolean isStuck() {
        return isStuck;
    }

    /**
     * Sets the stuck status of the robot.
     *
     * @param isStuck {@code true} to mark as stuck, {@code false} otherwise
     */
    public void setStuck(boolean isStuck) {
        this.isStuck = isStuck;
    }

    /**
     * Sets the destination X-coordinate.
     *
     * @param destinationX the new X destination
     */
    public void setDestinationX(double destinationX) {
        this.destinationX = destinationX;
    }

    /**
     * Sets the destination Y-coordinate.
     *
     * @param destinationY the new Y destination
     */
    public void setDestinationY(double destinationY) {
        this.destinationY = destinationY;
    }

    /**
     * Moves the robot to the specified coordinates, updating its direction.
     *
     * @param x the target X-coordinate
     * @param y the target Y-coordinate
     */
    public void moveTo(double x, double y) {
        this.destinationX = x;
        this.destinationY = y;
        // Update direction based on the new destination
        this.direction = Math.toDegrees(Math.atan2(destinationY - getY(), destinationX - getX()));
    }

    // -----------------------------
    // Drawing Methods
    // -----------------------------

    /**
     * Draws the robot on the given GraphicsContext with enhanced aesthetics for its sensors.
     * The robot is rendered as:
     * 1. A red circle representing the body.
     * 2. Two small, rounded black rectangles representing wheels.
     * 3. Two diagonal sensor lines extending outward from the edges of the body with improved styling.
     *
     * @param gc The GraphicsContext used for drawing.
     */
    @Override
    public void draw(GraphicsContext gc) {
        gc.save();
        gc.translate(getX(), getY());
        gc.rotate(direction);  // Rotate based on robot's direction

        // Body
        gc.setFill(Color.ROYALBLUE);
        gc.fillOval(-getRadius(), -getRadius(), getRadius() * 2, getRadius() * 2);

        // Draw eyes and smile with adjusted rotation
        gc.save();
        gc.rotate(-90); // Additional rotation for facial features
        drawEyes(gc);
        drawSmileWithTeeth(gc);
        gc.restore();

        // Draw wheels
        drawWheels(gc);

        gc.restore();
    }

    /**
     * Draws the robot's wheels on the GraphicsContext.
     *
     * @param gc The GraphicsContext used for drawing.
     */
    private void drawWheels(GraphicsContext gc) {
        double wheelWidth = getRadius() * 0.3;
        double wheelHeight = getRadius() * 0.2;
        double arcSize = wheelHeight * 0.5;

        gc.setFill(Color.BLACK);
        gc.fillRoundRect(
                -wheelWidth / 2,
                -getRadius() - wheelHeight,
                wheelWidth,
                wheelHeight,
                arcSize,
                arcSize
        );

        gc.fillRoundRect(
                -wheelWidth / 2,
                getRadius(),
                wheelWidth,
                wheelHeight,
                arcSize,
                arcSize
        );
    }

    /**
     * Draws the robot's eyes with blinking capability.
     *
     * @param gc The GraphicsContext used for drawing.
     */
    private void drawEyes(GraphicsContext gc) {
        double eyeSep = getRadius() * EYE_OFFSET_X;
        double eyeY = getRadius() * EYE_OFFSET_Y;
        double eyeDiameter = getRadius() * EYE_SIZE;

        if (isBlinking) {
            // Draw closed eyes as lines
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(2);
            gc.strokeLine(-eyeSep - getRadius() * 0.1, eyeY, -eyeSep + getRadius() * 0.1, eyeY);
            gc.strokeLine(eyeSep - getRadius() * 0.1, eyeY, eyeSep + getRadius() * 0.1, eyeY);
        } else {
            // Draw open eyes as circles
            gc.setFill(Color.WHITE);
            // Left eye
            gc.fillOval(-eyeSep - eyeDiameter / 2, eyeY - eyeDiameter / 2, eyeDiameter, eyeDiameter);
            gc.setFill(Color.BLACK);
            gc.fillOval(-eyeSep - eyeDiameter / 4, eyeY - eyeDiameter / 4, eyeDiameter / 2, eyeDiameter / 2);
            gc.setFill(Color.WHITE);
            // Right eye
            gc.fillOval(eyeSep - eyeDiameter / 2, eyeY - eyeDiameter / 2, eyeDiameter, eyeDiameter);
            gc.setFill(Color.BLACK);
            gc.fillOval(eyeSep - eyeDiameter / 4, eyeY - eyeDiameter / 4, eyeDiameter / 2, eyeDiameter / 2);
        }

        // Manage blinking timing
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
     * Draws a smile with teeth on the robot's face.
     *
     * @param gc The GraphicsContext used for drawing.
     */
    private void drawSmileWithTeeth(GraphicsContext gc) {
        double smileWidth = getRadius() * 0.6;
        double smileHeight = getRadius() * 0.4;
        double teethHeight = smileHeight * 0.4;

        gc.setFill(Color.BLACK);
        gc.fillOval(-smileWidth / 2, 0, smileWidth, smileHeight);

        gc.setFill(Color.WHITE);
        double toothWidth = smileWidth / 4;
        for (int i = 0; i < 4; i++) {
            gc.fillRect(-smileWidth / 2 + i * toothWidth, teethHeight / 2, toothWidth, teethHeight);
        }
    }
}
