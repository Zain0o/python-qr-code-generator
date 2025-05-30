package RobotSim;

import javafx.scene.canvas.GraphicsContext;

/**
 * The {@code ArenaItem} class represents an abstract item within the {@link RobotArena}.
 * It provides basic properties and methods for drawing, updating state, and detecting collisions.
 * Subclasses need to implement specific behaviors for drawing and updating their state.
 *
 * <p>This class ensures each arena item has a unique ID and keeps track of its position and size.</p>
 *
 * @version 1.0
 */
public abstract class ArenaItem {

    /** The x-coordinate of this item's center. */
    private double x;

    /** The y-coordinate of this item's center. */
    private double y;

    /** The radius (size) of the item's bounding circle. */
    private double radius;

    /**
     * A static counter used to assign unique IDs to each {@code ArenaItem}.
     */
    private static int idCounter = 0;

    /**
     * The unique ID for this {@code ArenaItem}.
     */
    private final int id;

    /**
     * Indicates whether this item is currently selected.
     */
    private boolean selected = false;

    /**
     * Creates a new {@code ArenaItem} with the given position and size.
     *
     * @param x      the x-coordinate of the item's center
     * @param y      the y-coordinate of the item's center
     * @param radius the radius (size) of the item
     */
    public ArenaItem(double x, double y, double radius) {
        synchronized (ArenaItem.class) {
            this.id = idCounter++;
        }
        this.x = x;
        this.y = y;
        this.radius = radius;
    }

    /**
     * Calculates the distance between this item and another {@code ArenaItem}.
     *
     * @param other the other {@code ArenaItem}; can be {@code null}
     * @return the distance between centers, or {@link Double#MAX_VALUE} if {@code other} is {@code null}
     */
    public double calculateDistance(ArenaItem other) {
        if (other == null) {
            // If the other item is null, return a large distance
            return Double.MAX_VALUE;
        }
        double deltaX = this.x - other.x;
        double deltaY = this.y - other.y;
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }

    /**
     * Draws the item onto the given {@code GraphicsContext}.
     * Subclasses must provide their own implementation.
     *
     * @param gc the graphics context to draw on
     */
    public abstract void draw(GraphicsContext gc);

    /**
     * Updates the item's state within the specified {@code RobotArena}.
     * Subclasses must define how the state changes.
     *
     * @param arena the {@code RobotArena} where the item resides
     */
    public abstract void updateState(RobotArena arena);

    /**
     * Checks if this item is colliding with an object defined by its position and radius.
     *
     * @param otherX      the x-coordinate of the other object
     * @param otherY      the y-coordinate of the other object
     * @param otherRadius the radius of the other object
     * @return {@code true} if there is a collision, {@code false} otherwise
     */
    public boolean isColliding(double otherX, double otherY, double otherRadius) {
        double deltaX = otherX - this.x;
        double deltaY = otherY - this.y;
        double distanceSquared = deltaX * deltaX + deltaY * deltaY;
        double combinedRadius = this.radius + otherRadius;
        return distanceSquared < (combinedRadius * combinedRadius);
    }

    /**
     * Checks if this item is colliding with another {@code ArenaItem}.
     *
     * @param other the other {@code ArenaItem} to check
     * @return {@code true} if a collision is detected, {@code false} otherwise
     */
    public boolean isColliding(ArenaItem other) {
        if (other == null) {
            return false;
        }
        return isColliding(other.getX(), other.getY(), other.getRadius());
    }

    /**
     * Calculates the new movement angle for this item after colliding with another {@code ArenaItem}.
     *
     * @param currentAngle the current angle of movement in degrees
     * @param other        the other {@code ArenaItem} involved in the collision
     * @return the new angle of movement after the collision
     */
    public double calculateBounceAngle(double currentAngle, ArenaItem other) {
        // Calculate angle between centers in degrees
        double angleToOther = Math.toDegrees(Math.atan2(other.y - this.y, other.x - this.x));
        // Reflect the current angle over the collision normal
        return (2 * angleToOther - currentAngle + 180) % 360;
    }

    // -------------------------------------------------------------------------
    // Getters and Setters
    // -------------------------------------------------------------------------

    /**
     * Retrieves the x-coordinate of this item's center.
     *
     * @return the x-coordinate
     */
    public double getX() {
        return x;
    }

    /**
     * Sets the x-coordinate of this item's center.
     *
     * @param x the new x-coordinate
     */
    protected void setX(double x) {
        this.x = x;
    }

    /**
     * Retrieves the y-coordinate of this item's center.
     *
     * @return the y-coordinate
     */
    public double getY() {
        return y;
    }

    /**
     * Sets the y-coordinate of this item's center.
     *
     * @param y the new y-coordinate
     */
    protected void setY(double y) {
        this.y = y;
    }

    /**
     * Retrieves the radius (size) of this item's bounding circle.
     *
     * @return the radius
     */
    public double getRadius() {
        return radius;
    }

    /**
     * Sets the radius (size) of this item's bounding circle.
     *
     * @param radius the new radius
     */
    protected void setRadius(double radius) {
        this.radius = radius;
    }

    /**
     * Retrieves the unique identifier of this item.
     *
     * @return the unique ID
     */
    public int getId() {
        return id;
    }

    /**
     * Checks whether this item is currently selected.
     *
     * @return {@code true} if the item is selected, {@code false} otherwise
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * Sets the selection status of this item.
     *
     * @param selected {@code true} to select the item, {@code false} to deselect
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    /**
     * Returns a string describing this {@code ArenaItem}, including its type, ID, and position.
     *
     * @return a string representation of the item
     */
    @Override
    public String toString() {
        return String.format("%s (ID: %d) - Position: (%.2f, %.2f)",
                this.getClass().getSimpleName(), id, x, y);
    }
}
