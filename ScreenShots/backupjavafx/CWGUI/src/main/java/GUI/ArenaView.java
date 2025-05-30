package GUI;

import RobotSim.*;
import javafx.geometry.Point2D;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.geometry.*;

/**
 * The {@code ArenaView} class manages the visual representation of the {@link RobotArena}.
 * It handles rendering, user interactions such as dragging items and showing context menus,
 * and provides a UI for adjusting item properties (e.g., robot speed).
 *
 * <p>This class contains a {@link Canvas} where all arena items are drawn. It also manages
 * hover effects, selection glow, and context menus for items within the arena.</p>
 *
 * @version 1.0
 */
public class ArenaView {

    /** The default width of the arena canvas. */
    private static final double DEFAULT_WIDTH = 1200;

    /** The default height of the arena canvas. */
    private static final double DEFAULT_HEIGHT = 900;

    /** Spacing (in pixels) for the rendered grid lines. */
    private static final double GRID_SPACING = 40;

    /** The radius of the glow effect applied to selected items. */
    private static final double GLOW_RADIUS = 15;

    /** The canvas on which the RobotArena is drawn. */
    private final Canvas canvas;

    /** A reference to the RobotArena managed by this view. */
    private final RobotArena robotArena;

    /** A DropShadow effect used to highlight selected items with a glow. */
    private final DropShadow selectionGlow;

    /** The item currently being dragged by the user, if any. */
    private ArenaItem draggedItem;

    /** Stores the offset of the mouse from the item’s center when dragging. */
    private Point2D dragOffset;

    /**
     * Constructs an {@code ArenaView} for the given {@link RobotArena}.
     *
     * @param robotArena The {@code RobotArena} instance to visualize.
     */
    public ArenaView(RobotArena robotArena) {
        this.robotArena = robotArena;
        this.canvas = new Canvas(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        this.canvas.setFocusTraversable(true);

        this.selectionGlow = createSelectionGlow();

        initializeInteractions();
    }

    /**
     * Creates a DropShadow glow effect for selected items.
     *
     * @return The configured DropShadow effect.
     */
    private DropShadow createSelectionGlow() {
        DropShadow glow = new DropShadow();
        glow.setColor(Color.YELLOW);
        glow.setRadius(GLOW_RADIUS);
        return glow;
    }

    /**
     * Initializes all user interactions, including item dragging,
     * context menus, and hover effects.
     */
    private void initializeInteractions() {
        initializeDragHandling();
        initializeContextMenu();
        initializeHoverEffects();
    }

    /**
     * Re-renders the entire arena view by clearing the canvas,
     * drawing the background, grid, items, and any status overlays.
     */
    public void render() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        clearCanvas(gc);
        drawBackground(gc);
        drawGrid(gc);
        drawItems(gc);
        drawStats(gc);
        drawBorder(gc);
    }

    /**
     * Draws a light gray background across the canvas.
     *
     * @param gc The {@link GraphicsContext} used for drawing.
     */
    private void drawBackground(GraphicsContext gc) {
        gc.setFill(Color.LIGHTGRAY);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    /**
     * Clears the canvas and fills it with a default background color.
     *
     * @param gc The {@link GraphicsContext} used for drawing.
     */
    private void clearCanvas(GraphicsContext gc) {
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.setFill(Color.web("#f8f9fa"));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    /**
     * Draws all items in the arena, along with selection or hover effects.
     *
     * @param gc The {@link GraphicsContext} used for drawing.
     */
    private void drawItems(GraphicsContext gc) {
        // Draw all items via the RobotArena's drawing method
        robotArena.drawItems(gc);

        ArenaItem selectedItem = robotArena.getSelectedItem();
        ArenaItem hoveredItem = robotArena.getHoveredItem();

        // If an item is selected, highlight it
        if (selectedItem != null) {
            drawSelectionEffect(gc, selectedItem);
        }

        // If an item is hovered, highlight it unless it's already selected
        if (hoveredItem != null && hoveredItem != selectedItem) {
            drawHoverEffect(gc, hoveredItem);
        }
    }

    /**
     * Draws a selection effect (glow) around the currently selected item.
     *
     * @param gc   The {@link GraphicsContext} used for drawing.
     * @param item The selected {@link ArenaItem}.
     */
    private void drawSelectionEffect(GraphicsContext gc, ArenaItem item) {
        double padding = 10;
        drawHighlightCircle(gc, item, selectionGlow, Color.ORANGE, 4, padding);
    }

    /**
     * Draws a hover effect around the currently hovered item.
     *
     * @param gc   The {@link GraphicsContext} used for drawing.
     * @param item The hovered {@link ArenaItem}.
     */
    private void drawHoverEffect(GraphicsContext gc, ArenaItem item) {
        double padding = 5;
        DropShadow hoverGlow = new DropShadow(10, Color.LIGHTGREEN);
        drawHighlightCircle(gc, item, hoverGlow, Color.LIGHTGREEN, 2, padding);
    }

    /**
     * Draws a highlighted circle around a specific item using a given effect, color, and stroke width.
     *
     * @param gc        The {@link GraphicsContext} used for drawing.
     * @param item      The {@link ArenaItem} to highlight.
     * @param effect    The effect to apply, such as a {@link DropShadow}.
     * @param color     The stroke color of the highlight.
     * @param lineWidth The width of the highlight's stroke.
     * @param padding   Additional padding beyond the item's radius.
     */
    private void drawHighlightCircle(GraphicsContext gc, ArenaItem item, DropShadow effect,
                                     Color color, double lineWidth, double padding) {
        gc.setEffect(effect);
        gc.setStroke(color);
        gc.setLineWidth(lineWidth);

        double x = item.getX();
        double y = item.getY();
        double radius = item.getRadius() + padding;

        gc.strokeOval(x - radius, y - radius, radius * 2, radius * 2);
        gc.setEffect(null);
    }

    /**
     * Draws statistical information such as arena size and selected item details.
     *
     * @param gc The {@link GraphicsContext} used for drawing.
     */
    private void drawStats(GraphicsContext gc) {
        gc.setFill(Color.web("#2c3e50"));
        gc.setFont(Font.font("System", FontWeight.BOLD, 14));

        String dimensions = String.format("Arena Size: %.0fx%.0f", canvas.getWidth(), canvas.getHeight());
        gc.fillText(dimensions, 15, 25);

        ArenaItem selectedItem = robotArena.getSelectedItem();
        if (selectedItem != null) {
            String itemInfo = String.format("%s (ID: %d) at (%.0f, %.0f)",
                    selectedItem.getClass().getSimpleName(),
                    selectedItem.getId(),
                    selectedItem.getX(),
                    selectedItem.getY());
            gc.fillText(itemInfo, 15, 45);
        }
    }

    /**
     * Draws a grid across the canvas to help visualize positioning.
     *
     * @param gc The {@link GraphicsContext} used for drawing.
     */
    private void drawGrid(GraphicsContext gc) {
        gc.setStroke(Color.web("#e8e8e8"));
        gc.setLineWidth(0.5);

        // Vertical lines
        for (double x = 0; x <= canvas.getWidth(); x += GRID_SPACING) {
            gc.strokeLine(x, 0, x, canvas.getHeight());
        }

        // Horizontal lines
        for (double y = 0; y <= canvas.getHeight(); y += GRID_SPACING) {
            gc.strokeLine(0, y, canvas.getWidth(), y);
        }
    }

    /**
     * Draws a rounded border around the canvas.
     *
     * @param gc The {@link GraphicsContext} used for drawing.
     */
    private void drawBorder(GraphicsContext gc) {
        gc.setStroke(Color.LIGHTGRAY);
        gc.setLineWidth(2);
        gc.strokeRoundRect(1, 1, canvas.getWidth() - 2, canvas.getHeight() - 2, 15, 15);
    }

    /**
     * Initializes mouse event handlers for item dragging.
     */
    private void initializeDragHandling() {
        canvas.setOnMousePressed(this::handleMousePressed);
        canvas.setOnMouseDragged(this::handleMouseDragged);
        canvas.setOnMouseReleased(e -> draggedItem = null);
    }

    /**
     * Handles {@link MouseEvent}s for item selection and starting a drag operation.
     *
     * @param e The mouse event containing position and button data.
     */
    private void handleMousePressed(MouseEvent e) {
        if (e.getButton() != MouseButton.PRIMARY) return;

        ArenaItem clicked = robotArena.findItemAt(e.getX(), e.getY());
        if (clicked != null) {
            draggedItem = clicked;
            dragOffset = new Point2D(e.getX() - clicked.getX(), e.getY() - clicked.getY());
            robotArena.setSelectedItem(clicked);
        } else {
            robotArena.setSelectedItem(null);
        }
        render();
    }

    /**
     * Handles {@link MouseEvent}s for dragging a selected item within the arena.
     *
     * @param e The mouse event containing updated position data.
     */
    private void handleMouseDragged(MouseEvent e) {
        if (draggedItem == null) return;

        double newX = e.getX() - dragOffset.getX();
        double newY = e.getY() - dragOffset.getY();

        newX = clampToBounds(newX, draggedItem.getRadius(), robotArena.getWidth());
        newY = clampToBounds(newY, draggedItem.getRadius(), robotArena.getHeight());

        // Identify if the dragged item is a Robot
        int robotId = -1;
        if (draggedItem instanceof Robot robot) {
            robotId = robot.getId();
        }

        // Only move the item if it won't collide at the new position
        if (!robotArena.isColliding(newX, newY, draggedItem.getRadius(), robotId)) {
            boolean moved = false;

            if (draggedItem instanceof Robot robot) {
                // Robot's setX/setY are protected in the hierarchy
                robot.setX(newX);
                robot.setY(newY);
                moved = true;
            } else if (draggedItem instanceof Obstacle obstacle) {
                obstacle.setX(newX);
                obstacle.setY(newY);
                moved = true;
            }

            if (moved) {
                render();
            } else {
                DebugUtils.log("Attempted to move a non-movable item: " + draggedItem);
            }
        }
    }

    /**
     * Clamps a given value to ensure it remains within the arena boundaries.
     *
     * @param value The value to clamp.
     * @param min   The minimum allowed value (normally the item’s radius).
     * @param max   The maximum allowed value (the arena dimension).
     * @return The clamped value.
     */
    private double clampToBounds(double value, double min, double max) {
        return Math.max(min, Math.min(max - min, value));
    }

    /**
     * Initializes a {@link ContextMenu} for right-click actions on items.
     */
    private void initializeContextMenu() {
        ContextMenu menu = new ContextMenu();

        canvas.setOnContextMenuRequested(e -> {
            menu.getItems().clear();
            ArenaItem item = robotArena.findItemAt(e.getX(), e.getY());

            if (item != null) {
                populateContextMenu(menu, item);
                menu.show(canvas, e.getScreenX(), e.getScreenY());
            }
        });
    }

    /**
     * Adds menu items (Delete, Info, Speed) to the {@link ContextMenu} based on the item type.
     *
     * @param menu The {@link ContextMenu} to populate.
     * @param item The {@link ArenaItem} that was right-clicked.
     */
    private void populateContextMenu(ContextMenu menu, ArenaItem item) {
        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(e -> {
            robotArena.deleteItem(item);
            render();
        });

        MenuItem infoItem = new MenuItem("Info");
        infoItem.setOnAction(e -> showItemInfo(item));

        menu.getItems().addAll(deleteItem, infoItem);

        // If item is a Robot, add an option to adjust speed
        if (item instanceof Robot robot) {
            MenuItem speedItem = new MenuItem("Adjust Speed");
            speedItem.setOnAction(e -> showSpeedDialog(robot));
            menu.getItems().add(speedItem);
        }
    }

    /**
     * Displays a dialog for adjusting the speed of the given robot.
     *
     * @param robot The {@link Robot} whose speed should be adjusted.
     */
    private void showSpeedDialog(Robot robot) {
        Dialog<Double> dialog = createSpeedDialog(robot);
        dialog.showAndWait().ifPresent(speed -> {
            robot.setSpeed(speed);
            render();
        });
    }

    /**
     * Creates a dialog (with a slider) for adjusting a robot's speed.
     *
     * @param robot The {@link Robot} to configure.
     * @return A {@link Dialog} that returns the selected speed as a Double.
     */
    private Dialog<Double> createSpeedDialog(Robot robot) {
        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle("Adjust Speed");

        Slider speedSlider = new Slider(0.1, 5.0, robot.getSpeed());
        configureSpeedSlider(speedSlider);

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.getChildren().addAll(new Label("Adjust Robot Speed:"), speedSlider);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(button ->
                button == ButtonType.OK ? speedSlider.getValue() : null
        );

        return dialog;
    }

    /**
     * Configures a slider with tick marks and a tooltip to adjust speed values.
     *
     * @param slider The {@link Slider} to configure.
     */
    private void configureSpeedSlider(Slider slider) {
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(1.0);
        slider.setMinorTickCount(4);
        slider.setBlockIncrement(0.1);
        slider.setPrefWidth(300);

        Tooltip tooltip = new Tooltip("Slide to adjust the robot's speed.");
        Tooltip.install(slider, tooltip);
    }

    /**
     * Displays a dialog showing information about the specified item.
     *
     * @param item The {@link ArenaItem} to describe.
     */
    private void showItemInfo(ArenaItem item) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Item Info");
        alert.setHeaderText(null);
        alert.setContentText(item.toString());
        alert.showAndWait();
    }

    /**
     * Sets up hover effects that highlight items when the mouse moves over them.
     */
    private void initializeHoverEffects() {
        canvas.setOnMouseMoved(e -> {
            ArenaItem hoveredItem = robotArena.findItemAt(e.getX(), e.getY());
            if (hoveredItem != robotArena.getSelectedItem()) {
                robotArena.setHoveredItem(hoveredItem);
                render();
            }
        });

        canvas.setOnMouseExited(e -> {
            robotArena.setHoveredItem(null);
            render();
        });
    }

    /**
     * Adjusts the canvas size and triggers a re-render to fit the new dimensions.
     *
     * @param width  The new width of the canvas.
     * @param height The new height of the canvas.
     */
    public void updateCanvasSize(double width, double height) {
        canvas.setWidth(width);
        canvas.setHeight(height);
        render();
    }

    /**
     * Changes the glow color used for highlighting selected items.
     *
     * @param color The desired {@link Color} for the selection glow.
     */
    public void setGlowColor(Color color) {
        selectionGlow.setColor(color);
    }

    /**
     * Retrieves the {@link Canvas} used for rendering the arena.
     *
     * @return The {@link Canvas} where items are drawn.
     */
    public Canvas getCanvas() {
        return canvas;
    }
}
