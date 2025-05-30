package RobotSim;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The RobotArena class manages the items (robots and obstacles) in the arena and their interactions.
 * It provides methods to add new items, render them, handle their movement and collisions,
 * and manage the selection of items.
 *
 * @version 1.0
 */
public class RobotArena {

    /**
     * A collection of all items (robots, obstacles, etc.) in the arena.
     */
    private final ArrayList<ArenaItem> items;

    /**
     * The width of the arena.
     */
    private double width;

    /**
     * The height of the arena.
     */
    private double height;

    /**
     * The currently selected item in the arena.
     */
    private ArenaItem selectedItem;

    /**
     * The currently hovered item in the arena.
     */
    private ArenaItem hoveredItem; // For hover handling

    /**
     * A multiplier that adjusts the speed of all robots in the arena.
     */
    private double speedFactor = 1.0;

    /**
     * Consumer callback invoked when a robot is selected.
     */
    private Consumer<Robot> onRobotSelected;

    /**
     * Size of each cell in the spatial grid.
     */
    private static final int GRID_SIZE = 50;

    /**
     * The spatial grid used for optimized collision detection.
     */
    private List<List<List<ArenaItem>>> spatialGrid; // Grid for collision detection

    /**
     * Constructs a new RobotArena with specified dimensions.
     *
     * @param width  The width of the arena.
     * @param height The height of the arena.
     * @throws IllegalArgumentException if width or height is non-positive.
     */
    public RobotArena(double width, double height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Arena dimensions must be positive.");
        }
        this.width = width;
        this.height = height;
        initializeSpatialGrid();
        items = new ArrayList<>();
    }

    /**
     * Sets the global speed factor for all robots in the arena.
     * E.g., 0.5 = half speed, 2.0 = double speed.
     *
     * @param factor The desired speed factor.
     */
    public void setSpeedFactor(double factor) {
        this.speedFactor = factor;
    }

    /**
     * Retrieves the current speed factor.
     *
     * @return The current speed factor.
     */
    public double getSpeedFactor() {
        return speedFactor;
    }

    /**
     * Updates the dimensions of the arena.
     *
     * @param width  The new width of the arena.
     * @param height The new height of the arena.
     */
    public void updateDimensions(double width, double height) {
        this.width = width;
        this.height = height;
    }

    /**
     * Adds a new PredatorRobot at a random non-overlapping position.
     */
    public void addPredatorRobot() {
        double x, y;
        do {
            x = Math.random() * width;
            y = Math.random() * height;
        } while (isOverlapping(x, y, 20));
        PredatorRobot robot = new PredatorRobot(x, y, 20);
        items.add(robot);
        DebugUtils.log("Added new PredatorRobot at (" + x + "," + y + ")");
    }

    /**
     * Adds a new method to help PredatorRobot find potential targets.
     *
     * @return A list of robots that can be targeted.
     */
    public List<Robot> getPotentialTargets() {
        List<Robot> targets = new ArrayList<>();
        for (ArenaItem item : items) {
            if (item instanceof Robot && !(item instanceof PredatorRobot)) {
                targets.add((Robot) item);
            }
        }
        return targets;
    }

    /**
     * Adds an ArenaItem to the arena.
     *
     * @param item The ArenaItem to add.
     */
    public void addItem(ArenaItem item) {
        if (item != null) {
            items.add(item);
            DebugUtils.log("Added " + item.getClass().getSimpleName() + " at (" + item.getX() + ", " + item.getY() + ")");
        }
    }

    /**
     * Updates the simulation speed by setting the speed of all robots.
     *
     * @param multiplier The speed multiplier to apply.
     */
    public void updateSimulationSpeed(double multiplier) {
        synchronized (items) {
            for (ArenaItem item : items) {
                if (item instanceof Robot robot) {
                    robot.setSpeed(multiplier);
                }
            }
        }
    }

    /**
     * Clears all items from the arena, resetting it to an empty state.
     */
    public void clearArena() {
        items.clear();
        selectedItem = null;
        hoveredItem = null; // Clear hovered item as well
        DebugUtils.log("Arena cleared. All items removed.");
    }

    /**
     * Adds a new basic Robot at a random non-overlapping position.
     */
    public void addRobot() {
        double x, y;
        do {
            x = Math.random() * width;
            y = Math.random() * height;
        } while (isOverlapping(x, y, 20));
        Robot robot = new Robot(x, y, 20);
        items.add(robot);
        DebugUtils.log("Added new Robot at (" + x + "," + y + ")");
    }

    /**
     * Adds a new ChaserRobot at a random non-overlapping position.
     */
    public void addChaserRobot() {
        double x, y;
        do {
            x = Math.random() * width;
            y = Math.random() * height;
        } while (isOverlapping(x, y, 20));
        ChaserRobot robot = new ChaserRobot(x, y, 20);
        items.add(robot);
        DebugUtils.log("Added new ChaserRobot at (" + x + "," + y + ")");
    }

    /**
     * Adds a new BumpRobot at a random non-overlapping position.
     */
    public void addBumpRobot() {
        double x, y;
        do {
            x = Math.random() * width;
            y = Math.random() * height;
        } while (isOverlapping(x, y, 20));
        BumpRobot robot = new BumpRobot(x, y, 20);
        items.add(robot);
        DebugUtils.log("Added new BumpRobot at (" + x + "," + y + ")");
    }

    /**
     * Sets the callback to be invoked when a robot is selected.
     *
     * @param listener A Consumer that accepts the selected Robot.
     */
    public void setOnRobotSelected(Consumer<Robot> listener) {
        this.onRobotSelected = listener;
    }

    /**
     * Selects a robot in the arena and invokes the onRobotSelected callback.
     *
     * @param robot The Robot to select.
     */
    public void selectRobot(Robot robot) {
        this.selectedItem = robot;
        if (onRobotSelected != null) {
            onRobotSelected.accept(robot);
        }
    }

    /**
     * Returns the count of all Robot instances in the arena.
     *
     * @return Number of Robot items.
     */
    public int getRobotCount() {
        return (int) items.stream().filter(item -> item instanceof Robot).count();
    }

    /**
     * Adds a new BeamRobot at a random non-overlapping position.
     */
    public void addBeamRobot() {
        double x, y;
        do {
            x = Math.random() * width;
            y = Math.random() * height;
        } while (isOverlapping(x, y, 20));
        BeamRobot robot = new BeamRobot(x, y, 20);
        items.add(robot);
        DebugUtils.log("Added new BeamRobot at (" + x + "," + y + ")");
    }

    /**
     * Adds a new Obstacle at a random non-overlapping position.
     */
    public void addObstacle() {
        double x, y;
        do {
            x = Math.random() * width;
            y = Math.random() * height;
        } while (isOverlapping(x, y, 15));
        Obstacle obstacle = new Obstacle(x, y, 15);
        items.add(obstacle);
        DebugUtils.log("Added new Obstacle at (" + x + "," + y + ")");
    }

    /**
     * Updates all Robot-derived items in the arena by invoking their updateState() methods.
     */
    public void moveRobots() {
        for (ArenaItem item : items) {
            if (item instanceof Robot robot) {
                // Update the robot's state within this arena
                robot.updateState(this);
                DebugUtils.log("Updated Robot [" + robot.getId() + "] to (" + robot.getX() + ", " + robot.getY() + ")");
            }
        }
    }

    /**
     * Finds and returns the topmost ArenaItem at the specified (x, y) coordinates.
     *
     * @param x The x-coordinate to search.
     * @param y The y-coordinate to search.
     * @return The ArenaItem at the specified location, or null if none found.
     */
    public ArenaItem findItemAt(double x, double y) {
        // Iterate from the topmost item to the bottom
        for (int i = items.size() - 1; i >= 0; i--) {
            ArenaItem item = items.get(i);
            double dist = Math.hypot(x - item.getX(), y - item.getY());
            if (dist <= item.getRadius()) {
                return item;
            }
        }
        return null;
    }

    /**
     * Checks if placing a new item at (x, y) with radius 'radius' would overlap existing items.
     *
     * @param x      The x-coordinate of the new position.
     * @param y      The y-coordinate of the new position.
     * @param radius The radius of the item being placed.
     * @return True if overlapping occurs, false otherwise.
     */
    private boolean isOverlapping(double x, double y, double radius) {
        // Using Robot as a generic ArenaItem for overlap checking
        ArenaItem tempItem = new Robot(x, y, radius);
        for (ArenaItem item : items) {
            if (isColliding(tempItem, item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines whether two ArenaItems are colliding based on their distances.
     *
     * @param a The first ArenaItem.
     * @param b The second ArenaItem.
     * @return True if the two items' bounding circles intersect, false otherwise.
     */
    private boolean isColliding(ArenaItem a, ArenaItem b) {
        double distance = a.calculateDistance(b);
        return distance < (a.getRadius() + b.getRadius());
    }

    /**
     * Checks if placing an item at (x, y) with radius 'radius' would cause a collision
     * with existing items, excluding the item with the specified robotId.
     *
     * @param x        The x-coordinate of the new position.
     * @param y        The y-coordinate of the new position.
     * @param radius   The radius of the item being placed.
     * @param robotId  The unique identifier of the item being moved (to exclude from collision check).
     * @return True if a collision would occur, false otherwise.
     */
    public boolean isColliding(double x, double y, double radius, int robotId) {
        for (ArenaItem item : items) {
            // Skip if the item is the same robot
            if (item instanceof Robot r) {
                if (r.getId() == robotId) {
                    continue;
                }
            }
            double dx = x - item.getX();
            double dy = y - item.getY();
            double distSq = dx * dx + dy * dy;
            double combinedRadius = radius + item.getRadius();
            if (distSq < combinedRadius * combinedRadius) {
                return true; // Collision detected
            }
        }
        return false; // No collision
    }

    /**
     * Checks if any obstacle is within the combined radius of the robot's sensor.
     *
     * @param robot The Robot to check against obstacles.
     * @return True if an obstacle is nearby, false otherwise.
     */
    public boolean isObstacleNearby(Robot robot) {
        for (ArenaItem item : items) {
            if (item instanceof Obstacle obstacle) {
                // Use the calculateDistance method to check how close the obstacle is
                double distance = robot.calculateDistance(obstacle);
                if (distance <= (robot.getRadius() + robot.getSensorRange() + obstacle.getRadius())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Initializes the spatial grid based on arena dimensions.
     */
    private void initializeSpatialGrid() {
        int cols = (int) Math.ceil(width / GRID_SIZE);
        int rows = (int) Math.ceil(height / GRID_SIZE);
        spatialGrid = new ArrayList<>(cols);

        for (int i = 0; i < cols; i++) {
            spatialGrid.add(new ArrayList<>(rows));
            for (int j = 0; j < rows; j++) {
                spatialGrid.get(i).add(new ArrayList<>());
            }
        }
    }

    /**
     * Updates the spatial grid by assigning items to their respective grid cells.
     */
    private void updateSpatialGrid() {
        // Clear the grid
        for (List<List<ArenaItem>> col : spatialGrid) {
            for (List<ArenaItem> cell : col) {
                cell.clear();
            }
        }

        // Place items into the correct grid cells
        for (ArenaItem item : items) {
            int gridX = (int) (item.getX() / GRID_SIZE);
            int gridY = (int) (item.getY() / GRID_SIZE);
            if (gridX >= 0 && gridX < spatialGrid.size() && gridY >= 0 && gridY < spatialGrid.get(0).size()) {
                spatialGrid.get(gridX).get(gridY).add(item);
            }
        }
    }

    /**
     * Renders all items onto the given GraphicsContext.
     *
     * @param gc The GraphicsContext to draw on.
     */
    public void drawItems(GraphicsContext gc) {
        for (ArenaItem item : items) {
            item.draw(gc);
        }

        // Highlight the selected item with a yellow circle
        if (selectedItem != null) {
            gc.setStroke(Color.YELLOW);
            gc.setLineWidth(2);
            gc.strokeOval(
                    selectedItem.getX() - selectedItem.getRadius() - 5,
                    selectedItem.getY() - selectedItem.getRadius() - 5,
                    (selectedItem.getRadius() + 5) * 2,
                    (selectedItem.getRadius() + 5) * 2
            );
        }
    }

    /**
     * Returns the count of all Obstacle items in the arena.
     *
     * @return The number of obstacles.
     */
    public int getObstacleCount() {
        return (int) items.stream().filter(item -> item instanceof Obstacle).count();
    }

    /**
     * Draws a grid on the arena for better visualization.
     *
     * @param gc The GraphicsContext of the arena.
     */
    private void drawGrid(GraphicsContext gc) {
        // Draw grid lines
        gc.setStroke(Color.LIGHTGRAY);
        gc.setLineWidth(0.5);
        double spacing = 20;

        for (double x = 0; x <= width; x += spacing) {
            gc.strokeLine(x, 0, x, height);
        }
        for (double y = 0; y <= height; y += spacing) {
            gc.strokeLine(0, y, width, y);
        }

        // Draw border around the arena
        gc.setStroke(Color.DARKGRAY);
        gc.setLineWidth(2);
        gc.strokeRect(0, 0, width, height);
    }

    /**
     * Draws the entire arena, including the grid and all items.
     *
     * @param gc The GraphicsContext to draw on.
     */
    public void drawArena(GraphicsContext gc) {
        // Clear the canvas area
        gc.clearRect(0, 0, width, height);
        // Draw the arena grid
        drawGrid(gc);
        // Draw the items
        drawItems(gc);
    }

    /**
     * Serializes the arena dimensions and each ArenaItem to a String.
     *
     * @return A String representing the serialized state of the arena.
     */
    public String saveArenaState() {
        StringBuilder sb = new StringBuilder();

        // Save arena dimensions with explicit formatting
        sb.append(String.format("%.2f %.2f\n", width, height));

        // Save each item's state with additional properties
        for (ArenaItem item : items) {
            if (item != null) {
                if (item instanceof LightSource light) {
                    sb.append(String.format("LightSource %.2f %.2f %.2f %.2f %.2f\n",
                            light.getX(), light.getY(), light.getRadius(),
                            light.getIntensity(), light.getRange()));
                } else if (item instanceof LightSeekingRobot robot) {
                    sb.append(String.format("LightSeekingRobot %.2f %.2f %.2f %.2f\n",
                            robot.getX(), robot.getY(), robot.getRadius(), robot.getSpeed()));
                } else {
                    sb.append(String.format("%s %.2f %.2f %.2f\n",
                            item.getClass().getSimpleName(),
                            item.getX(), item.getY(), item.getRadius()));
                }
            }
        }
        return sb.toString();
    }

    /**
     * Deserializes the arena dimensions and items from a String.
     *
     * @param data The serialized state of the arena.
     * @throws IllegalArgumentException if the data is invalid or blank.
     * @throws RuntimeException if loading fails due to parsing errors.
     */
    public void loadArenaState(String data) {
        if (data == null || data.isBlank()) {
            throw new IllegalArgumentException("No data to load.");
        }

        try {
            items.clear();
            selectedItem = null;
            hoveredItem = null;

            String[] lines = data.split("\n");
            if (lines.length < 1) {
                throw new IllegalArgumentException("Invalid data format");
            }

            // Parse dimensions
            String[] dimensions = lines[0].split(" ");
            if (dimensions.length != 2) {
                throw new IllegalArgumentException("Invalid dimensions format");
            }

            width = Double.parseDouble(dimensions[0]);
            height = Double.parseDouble(dimensions[1]);

            // Parse and create items
            for (int i = 1; i < lines.length; i++) {
                String[] parts = lines[i].split(" ");
                if (parts.length < 4) {
                    continue; // Skip invalid lines
                }

                String type = parts[0];
                double x = Double.parseDouble(parts[1]);
                double y = Double.parseDouble(parts[2]);
                double radius = Double.parseDouble(parts[3]);

                switch (type) {
                    case "Robot" -> items.add(new Robot(x, y, radius));
                    case "ChaserRobot" -> items.add(new ChaserRobot(x, y, radius));
                    case "BeamRobot" -> items.add(new BeamRobot(x, y, radius));
                    case "BumpRobot" -> items.add(new BumpRobot(x, y, radius));
                    case "Obstacle" -> items.add(new Obstacle(x, y, radius));
                    case "PredatorRobot" -> items.add(new PredatorRobot(x, y, radius));
                    case "LightSource" -> {
                        if (parts.length >= 6) {
                            double intensity = Double.parseDouble(parts[4]);
                            double range = Double.parseDouble(parts[5]);
                            items.add(new LightSource(x, y, radius, intensity, range));
                        }
                    }
                    case "LightSeekingRobot" -> {
                        if (parts.length >= 5) {
                            LightSeekingRobot robot = new LightSeekingRobot(x, y, radius);
                            robot.setSpeed(Double.parseDouble(parts[4]));
                            items.add(robot);
                        }
                    }
                    default -> DebugUtils.log("Unknown item type: " + type);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load arena state: " + e.getMessage());
        }
    }

    /**
     * Provides a status string listing all items by class name and (x, y) coordinates.
     *
     * @return A String representing the current status of the arena.
     */
    public String getStatus() {
        StringBuilder status = new StringBuilder();
        for (ArenaItem item : items) {
            status.append(item.getClass().getSimpleName())
                    .append(" at (")
                    .append(Math.round(item.getX())).append(", ")
                    .append(Math.round(item.getY())).append(")\n");
        }
        return status.toString();
    }

    /**
     * Adds a new LightSeekingRobot at a random non-overlapping position.
     */
    public void addLightSeekingRobot() {
        double x, y;
        do {
            x = Math.random() * width;
            y = Math.random() * height;
        } while (isOverlapping(x, y, 20));
        LightSeekingRobot robot = new LightSeekingRobot(x, y, 20);
        items.add(robot);
        DebugUtils.log("Added new LightSeekingRobot at (" + x + "," + y + ")");
    }

    /**
     * Adds a new LightSource at a random non-overlapping position.
     */
    public void addLightSource() {
        double x, y;
        do {
            x = Math.random() * width;
            y = Math.random() * height;
        } while (isOverlapping(x, y, 15));

        // Create a light source with default intensity and range
        LightSource light = new LightSource(x, y, 15, 0.8, 200);
        items.add(light);
        DebugUtils.log("Added new LightSource at (" + x + "," + y + ")");
    }

    /**
     * Deletes the currently selected item from the arena, if any.
     */
    public void deleteSelectedItem() {
        if (selectedItem != null) {
            items.remove(selectedItem);
            selectedItem = null;
            DebugUtils.log("Deleted selected item.");
        }
    }

    /**
     * Deletes a specific ArenaItem from the arena.
     *
     * @param item The ArenaItem to be deleted.
     */
    public void deleteItem(ArenaItem item) {
        if (items.contains(item)) {
            items.remove(item);
            if (item.equals(selectedItem)) {
                selectedItem = null;
            }
            DebugUtils.log("Deleted " + item.getClass().getSimpleName() + " at (" + item.getX() + ", " + item.getY() + ")");
        }
    }

    /**
     * Checks if the provided line intersects ANY obstacle in the arena.
     *
     * @param line A line to test (e.g., a "whisker" from a robot).
     * @return True if the line intersects any obstacle, false otherwise.
     */
    public boolean intersectsAnyObstacle(Line line) {
        for (ArenaItem item : items) {
            if (item instanceof Obstacle obstacle) {
                // Check intersection with the bounding box of the obstacle
                if (intersectsSquare(line, obstacle)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Helper method to determine if a line intersects an Obstacle.
     *
     * @param line     The line to test for intersection.
     * @param obstacle The Obstacle to check against.
     * @return True if the line intersects the obstacle, false otherwise.
     */
    private boolean intersectsSquare(Line line, Obstacle obstacle) {
        double ox = obstacle.getX();
        double oy = obstacle.getY();
        double r  = obstacle.getRadius();

        // Define the bounding box of the obstacle
        double left   = ox - r;
        double right  = ox + r;
        double top    = oy - r;
        double bottom = oy + r;

        // Define the edges of the bounding box as lines
        Line topEdge    = new Line(left,  top,    right,  top);
        Line bottomEdge = new Line(left,  bottom, right,  bottom);
        Line leftEdge   = new Line(left,  top,    left,   bottom);
        Line rightEdge  = new Line(right, top,    right,  bottom);

        // Check for intersection with any edge
        return  linesIntersect(line, topEdge)
                || linesIntersect(line, bottomEdge)
                || linesIntersect(line, leftEdge)
                || linesIntersect(line, rightEdge);
    }

    /**
     * Determines whether two lines intersect.
     *
     * @param line1 The first line.
     * @param line2 The second line.
     * @return True if the lines intersect, false otherwise.
     */
    private boolean linesIntersect(Line line1, Line line2) {
        double x1 = line1.getStartX();
        double y1 = line1.getStartY();
        double x2 = line1.getEndX();
        double y2 = line1.getEndY();

        double x3 = line2.getStartX();
        double y3 = line2.getStartY();
        double x4 = line2.getEndX();
        double y4 = line2.getEndY();

        double denominator = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);

        if (denominator == 0) {
            return false; // Lines are parallel or coincident
        }

        double t = ((x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4)) / denominator;
        double u = ((x1 - x3) * (y1 - y2) - (y1 - y3) * (x1 - x2)) / denominator;

        return (t >= 0 && t <= 1) && (u >= 0 && u <= 1);
    }

    /**
     * Retrieves the list of all ArenaItems in the arena.
     *
     * @return An ArrayList of ArenaItems.
     */
    public ArrayList<ArenaItem> getItems() {
        return items;
    }

    /**
     * Retrieves the currently selected ArenaItem.
     *
     * @return The selected ArenaItem, or null if none is selected.
     */
    public ArenaItem getSelectedItem() {
        return selectedItem;
    }

    /**
     * Sets the currently selected ArenaItem.
     *
     * @param item The ArenaItem to select, or null to clear selection.
     */
    public void setSelectedItem(ArenaItem item) {
        this.selectedItem = item;
    }

    /**
     * Retrieves the currently hovered ArenaItem.
     *
     * @return The hovered ArenaItem, or null if none is hovered.
     */
    public ArenaItem getHoveredItem() {
        return hoveredItem;
    }

    /**
     * Sets the currently hovered ArenaItem.
     *
     * @param item The ArenaItem to set as hovered, or null to clear hover.
     */
    public void setHoveredItem(ArenaItem item) {
        this.hoveredItem = item;
    }

    /**
     * Retrieves the width of the arena.
     *
     * @return The width of the arena.
     */
    public double getWidth() {
        return width;
    }

    /**
     * Retrieves the height of the arena.
     *
     * @return The height of the arena.
     */
    public double getHeight() {
        return height;
    }

    /**
     * Provides information about the selected item as a String.
     *
     * @return A String containing details of the selected item, or "None" if no item is selected.
     */
    public String getSelectedItemInfo() {
        if (selectedItem != null) {
            return selectedItem.toString();
        }
        return "None";
    }
}
