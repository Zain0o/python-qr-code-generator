package GUI;

import RobotSim.*;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.scene.input.MouseButton;

/**
 * The {@code MainApp} class serves as the entry point for the Robot Simulator application.
 * It initializes all GUI components, manages menus, animation loops, user interactions,
 * and orchestrates the {@link RobotArena} lifecycle.
 *
 * <p>The UI is structured using a {@link BorderPane}, with menus at the top, a central
 * drawing area, a control panel on the left, and a dynamic info area on the right. The
 * bottom area hosts additional control buttons.</p>
 *
 * @version 1.0
 */
public class MainApp extends Application {

    /** The primary RobotArena containing robots, obstacles, and logic. */
    private RobotArena robotArena;

    /** A custom view managing drawing of the RobotArena. */
    private ArenaView arenaView;

    /** A control panel for managing simulation features (load, save, speed, etc.). */
    private ControlPanel controlPanel;

    /** Label that displays the current number of robots. */
    private Label robotCountLabel;

    /** Label that displays the current number of obstacles. */
    private Label obstacleCountLabel;

    /** The global AnimationTimer controlling periodic updates. */
    private static AnimationTimer animationTimer;

    /** Flag indicating if the simulation is currently running. */
    private static boolean isAnimating = true;

    /** A VBox that dynamically displays info about robots in the arena. */
    private VBox robotInfoBox;

    @Override
    public void start(Stage primaryStage) {
        DebugUtils.log("Starting Robot Simulator application");

        // 1. Main layout using BorderPane
        BorderPane root = new BorderPane();
        root.setBackground(
                new Background(
                        new BackgroundFill(Color.web("#f5f6fa"), CornerRadii.EMPTY, Insets.EMPTY)
                )
        );
        DebugUtils.log("Main layout initialized with background color #f5f6fa");

        // Initialize the robotInfoBox here ONCE
        robotInfoBox = new VBox(10);

        // 2. Menu bar creation
        MenuBar menuBar = createMenuBar();
        root.setTop(menuBar);
        DebugUtils.log("Menu bar created and added to the layout");

        // 3. Arena initialization
        initializeArena();
        DebugUtils.logArenaState(robotArena);

        // 4. Initialize and place GUI components
        initializeGUIComponents(root);
        DebugUtils.log("GUI components initialized and added to the layout");

        // 5. Animation setup and start
        initializeAnimation();
        DebugUtils.log("Animation loop initialized");

        // 6. Main scene creation
        Scene scene = new Scene(root, 1400, 1000);
        DebugUtils.log("Main scene created with dimensions 1400x1000");

        primaryStage.setTitle("Robot Simulator");
        primaryStage.setScene(scene);
        primaryStage.show();
        DebugUtils.log("Primary stage set and displayed");

        // 7. Mouse and keyboard input handling
        initializeMouseControls();
        initializeKeyboardControls(scene);
        initializeKeyboardShortcuts(scene);
        DebugUtils.log("Mouse and keyboard controls initialized");

        // 8. Shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            DebugUtils.log("Shutting down Robot Simulator");
            DebugUtils.cleanup();
        }));
    }

    /**
     * Updates the count of robots and obstacles in the arena,
     * then refreshes any related UI labels.
     */
    private void updateCounts() {
        if (robotCountLabel != null) {
            robotCountLabel.setText("Robots: " + robotArena.getRobotCount());
        }
        if (obstacleCountLabel != null) {
            obstacleCountLabel.setText("Obstacles: " + robotArena.getObstacleCount());
        }
    }

    /**
     * Initializes and arranges the GUI components (arena view, control panel, etc.)
     * within the main layout.
     *
     * @param root The {@link BorderPane} serving as the main layout container.
     */
    private void initializeGUIComponents(BorderPane root) {
        // 1. Info box on the right
        robotInfoBox.setPadding(new Insets(10));
        robotInfoBox.setBackground(
                new Background(new BackgroundFill(Color.LIGHTGRAY, new CornerRadii(5), Insets.EMPTY))
        );

        ScrollPane robotInfoScrollPane = new ScrollPane(robotInfoBox);
        robotInfoScrollPane.setFitToWidth(true);
        robotInfoScrollPane.setFitToHeight(true);
        robotInfoScrollPane.setBackground(
                new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY))
        );

        // Right Pane
        root.setRight(robotInfoScrollPane);

        // Left Pane: ControlPanel
        root.setLeft(controlPanel);

        // Center Pane: Arena view
        VBox centerPanel = new VBox(10);
        centerPanel.setAlignment(Pos.CENTER);
        centerPanel.setPadding(new Insets(10));

        Label arenaTitle = new Label("Robot Arena Simulation");
        arenaTitle.setFont(Font.font("System", FontWeight.BOLD, 20));

        Canvas arenaCanvas = arenaView.getCanvas();
        centerPanel.getChildren().addAll(arenaTitle, arenaCanvas);
        root.setCenter(centerPanel);

        // Bottom Pane: Additional buttons
        HBox buttonBox = setButtons();
        root.setBottom(buttonBox);
    }

    /**
     * Creates the bottom {@link HBox} containing the control buttons and speed slider.
     *
     * @return A fully configured {@link HBox}.
     */
    private HBox setButtons() {
        // Light-Seeking Robot
        Button btnLightSeeker = new Button("Add LightSeekingRobot");
        btnLightSeeker.setOnAction(e -> {
            robotArena.addLightSeekingRobot();
            arenaView.render();
            updateStatus();
            updateRobotInfo();
            updateCounts();
            DebugUtils.log("LightSeekingRobot button clicked: Added a new LightSeekingRobot.");
        });

        // Light Source
        Button btnLight = new Button("Add Light Source");
        btnLight.setOnAction(e -> {
            robotArena.addLightSource();
            arenaView.render();
            updateStatus("Added new light source");
            DebugUtils.log("Light Source button clicked: Added a new light source.");
        });

        // PredatorRobot
        Button btnPredator = new Button("Add PredatorRobot");
        btnPredator.setOnAction(e -> {
            robotArena.addPredatorRobot();
            arenaView.render();
            updateStatus();
            updateRobotInfo();
            updateCounts();
            DebugUtils.log("PredatorRobot button clicked: Added a new PredatorRobot.");
        });

        // Speed Slider label (just an example—no actual “effectiveSpeed” var here)
        Label speedLabel = new Label("Speed: 1.0");
        speedLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));

        Slider speedSlider = new Slider(0.1, 5.0, 1.0);
        speedSlider.setPrefWidth(150);
        speedSlider.setShowTickLabels(true);
        speedSlider.setShowTickMarks(true);
        speedSlider.setMajorTickUnit(1.0);
        speedSlider.setBlockIncrement(0.1);

        Label speedValueLabel = new Label("1.0x");
        speedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double speed = newVal.doubleValue();
            speedValueLabel.setText(String.format("%.1fx", speed));
            robotArena.setSpeedFactor(speed);
            updateRobotInfo();
            updateStatus(String.format("Speed set to %.1fx", speed));
        });

        // Control label
        Label controlLabel = new Label("Control: ");
        controlLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));

        // Random Basic Robot
        Button btnRandom = new Button("Add Robot Ball");
        btnRandom.setOnAction(e -> {
            robotArena.addRobot(); // Basic Robot
            arenaView.render();
            updateStatus();
            updateRobotInfo();
            updateCounts();
            DebugUtils.log("Random Ball button clicked: Added a new robot.");
        });

        // ChaserRobot
        Button btnChaser = new Button("Add ChaserRobot");
        btnChaser.setOnAction(e -> {
            robotArena.addChaserRobot();
            arenaView.render();
            updateStatus();
            updateRobotInfo();
            updateCounts();
            DebugUtils.log("ChaserRobot button clicked: Added a new ChaserRobot.");
        });

        // BumpRobot
        Button btnBump = new Button("Add BumpRobot");
        btnBump.setOnAction(e -> {
            robotArena.addBumpRobot();
            arenaView.render();
            updateStatus();
            updateRobotInfo();
            updateCounts();
            DebugUtils.log("BumpRobot button clicked: Added a new BumpRobot.");
        });

        // BeamRobot
        Button btnBeam = new Button("Add BeamRobot");
        btnBeam.setOnAction(e -> {
            robotArena.addBeamRobot();
            arenaView.render();
            updateStatus();
            updateRobotInfo();
            updateCounts();
            DebugUtils.log("BeamRobot button clicked: Added a new BeamRobot.");
        });

        // Obstacle
        Button btnObstacle = new Button("Add Obstacle");
        btnObstacle.setOnAction(e -> {
            robotArena.addObstacle();
            arenaView.render();
            updateStatus();
            updateRobotInfo();
            updateCounts();
            DebugUtils.log("Add Obstacle button clicked: Added a new obstacle.");
        });

        // Clear Arena
        Button btnClear = new Button("Clear Arena");
        btnClear.setOnAction(e -> {
            robotArena.clearArena();
            arenaView.render();
            updateRobotInfo();
            updateCounts();
            updateStatus("Arena cleared");
            DebugUtils.log("Arena cleared");
        });

        // Save
        Button btnSave = new Button("Save Arena");
        btnSave.setOnAction(e -> {
            if (controlPanel != null) {
                controlPanel.handleSave();
                updateStatus("Arena saved.");
                DebugUtils.log("Save Arena button clicked: Arena saved.");
            }
        });

        // Load
        Button btnLoad = new Button("Load Arena");
        btnLoad.setOnAction(e -> {
            if (controlPanel != null) {
                controlPanel.handleLoad();
                arenaView.render();
                updateRobotInfo();
                updateCounts();
                updateStatus("Arena loaded.");
                DebugUtils.log("Load Arena button clicked: Arena loaded.");
            }
        });

        // Animation controls
        Label animationLabel = new Label(" Animation: ");
        animationLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));

        Button btnStart = new Button("Start");
        btnStart.setOnAction(e -> startAnimation());

        Button btnStop = new Button("Stop");
        btnStop.setOnAction(e -> stopAnimation());

        // Layout
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(10));
        buttonBox.setBackground(
                new Background(
                        new BackgroundFill(Color.web("#ecf0f1"), CornerRadii.EMPTY, Insets.EMPTY)
                )
        );

        buttonBox.getChildren().addAll(
                speedLabel, speedSlider, speedValueLabel,
                controlLabel, btnRandom, btnChaser, btnBump, btnBeam, btnObstacle,
                btnPredator, btnLightSeeker, btnLight,
                btnClear, btnSave, btnLoad,
                animationLabel, btnStart, btnStop
        );

        return buttonBox;
    }

    /**
     * Creates the menu bar, including 'File' and 'Help' menus with custom color styling.
     *
     * @return A fully configured {@link MenuBar}.
     */
    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();

        menuBar.setBackground(new Background(
                new BackgroundFill(Color.web("#34495e"), CornerRadii.EMPTY, Insets.EMPTY)
        ));

        // File Menu
        Menu fileMenu = createColoredMenu("File", Color.WHITE);

        MenuItem newArenaItem = createColoredMenuItem("New Arena", Color.BLACK);
        newArenaItem.setOnAction(e -> {
            resetArena();
            updateStatus("New arena initialized.");
            DebugUtils.log("Menu 'New Arena' selected: Arena reset.");
        });

        MenuItem saveArenaItem = createColoredMenuItem("Save Arena", Color.BLACK);
        saveArenaItem.setOnAction(e -> {
            controlPanel.handleSave();
            updateStatus("Arena saved.");
            DebugUtils.log("Menu 'Save Arena' selected: Arena saved.");
        });

        MenuItem loadArenaItem = createColoredMenuItem("Load Arena", Color.BLACK);
        loadArenaItem.setOnAction(e -> {
            controlPanel.handleLoad();
            arenaView.render();
            updateStatus("Arena loaded.");
            DebugUtils.log("Menu 'Load Arena' selected: Arena loaded.");
        });

        MenuItem exitItem = createColoredMenuItem("Exit", Color.BLACK);
        exitItem.setOnAction(e -> {
            DebugUtils.log("Menu 'Exit' selected: Application exiting.");
            Platform.exit();
        });

        fileMenu.getItems().addAll(
                newArenaItem,
                saveArenaItem,
                loadArenaItem,
                new SeparatorMenuItem(),
                exitItem
        );

        // Animation Menu
        Menu animationMenu = createColoredMenu("Animation", Color.WHITE);

        MenuItem startAnimationItem = createColoredMenuItem("Start Animation", Color.BLACK);
        MenuItem stopAnimationItem = createColoredMenuItem("Stop Animation", Color.BLACK);
        stopAnimationItem.setDisable(true);
        startAnimationItem.setOnAction(e -> {
            startAnimation();
            startAnimationItem.setDisable(true);
            stopAnimationItem.setDisable(false);
            updateStatus("Animation started via menu.");
            DebugUtils.log("Menu 'Start Animation' selected: Animation started.");
        });

        stopAnimationItem.setOnAction(e -> {
            stopAnimation();
            startAnimationItem.setDisable(false);
            stopAnimationItem.setDisable(true);
            updateStatus("Animation stopped via menu.");
            DebugUtils.log("Menu 'Stop Animation' selected: Animation stopped.");
        });

        animationMenu.getItems().addAll(startAnimationItem, stopAnimationItem);

        // Help Menu
        Menu helpMenu = createColoredMenu("Help", Color.WHITE);

        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction(e -> showAbout());

        MenuItem helpItem = createColoredMenuItem("Help", Color.BLACK);
        helpItem.setOnAction(e -> showHelpDialog());

        helpMenu.getItems().addAll(aboutItem, helpItem);

        menuBar.getMenus().addAll(fileMenu, animationMenu, helpMenu);
        return menuBar;
    }

    /**
     * Creates a {@link Menu} with colored text using a {@link Label} as its graphic.
     *
     * @param menuText  The text to display for the menu.
     * @param textColor The {@link Color} of the menu text.
     * @return A {@link Menu} instance with styled text.
     */
    private Menu createColoredMenu(String menuText, Color textColor) {
        Menu menu = new Menu();
        Label lbl = new Label(menuText);
        lbl.setTextFill(textColor);
        lbl.setFont(Font.font("System", FontWeight.NORMAL, 14));
        menu.setGraphic(lbl);
        return menu;
    }

    /**
     * Creates a {@link MenuItem} with colored text using a {@link Label} as its graphic.
     *
     * @param itemText  The text to display on the menu item.
     * @param textColor The {@link Color} of the item text.
     * @return A {@link MenuItem} instance with styled text.
     */
    private MenuItem createColoredMenuItem(String itemText, Color textColor) {
        MenuItem item = new MenuItem();
        Label lbl = new Label(itemText);
        lbl.setTextFill(textColor);
        lbl.setFont(Font.font("System", FontWeight.NORMAL, 14));
        item.setGraphic(lbl);
        return item;
    }

    /**
     * Initializes the {@link RobotArena} by creating it, adding default items,
     * and linking it with an {@link ArenaView} and {@link ControlPanel}.
     */
    private void initializeArena() {
        robotArena = new RobotArena(1200, 900);
        robotArena.addRobot();
        robotArena.addObstacle();
        robotArena.addChaserRobot();
        robotArena.addBumpRobot();
        robotArena.addBeamRobot();

        arenaView = new ArenaView(robotArena);
        controlPanel = new ControlPanel(robotArena, arenaView);
    }

    /**
     * Sets up the animation loop with an {@link AnimationTimer} for real-time updates.
     */
    private void initializeAnimation() {
        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (isAnimating) {
                    // Update robots
                    robotArena.moveRobots();

                    // Ensure UI updates occur on the JavaFX Application Thread
                    Platform.runLater(() -> {
                        arenaView.render();
                        updateStatus();
                        updateRobotInfo();
                    });
                }
            }
        };
        animationTimer.start();
    }

    /**
     * Rebuilds the dynamic list of robot info in the {@link #robotInfoBox}.
     */
    private void updateRobotInfo() {
        robotInfoBox.getChildren().clear();
        ArenaItem selectedItem = robotArena.getSelectedItem();

        for (ArenaItem item : robotArena.getItems()) {
            if (item instanceof Robot robot) {
                VBox robotBox = new VBox(5);
                robotBox.setPadding(new Insets(15));
                robotBox.setBackground(
                        new Background(new BackgroundFill(Color.LIGHTGRAY, new CornerRadii(10), Insets.EMPTY))
                );
                robotBox.setBorder(
                        new Border(new BorderStroke(
                                Color.DARKGRAY, BorderStrokeStyle.SOLID, new CornerRadii(10), BorderWidths.DEFAULT
                        ))
                );

                // Calculate the "effective" speed
                double effectiveSpeed = robot.getSpeed() * robotArena.getSpeedFactor();

                Label typeLabel = new Label("Type: " + robot.getClass().getSimpleName());
                Label positionLabel = new Label(
                        String.format("Position: (%.1f, %.1f)", robot.getX(), robot.getY())
                );
                Label destinationLabel = new Label(
                        String.format("Destination: (%.1f, %.1f)",
                                robot.getDestinationX(), robot.getDestinationY())
                );
                Label directionLabel = new Label(
                        String.format("Direction: %.1f°", robot.getDirection())
                );

                // We remove the old "Speed: X.XX" for robot.getSpeed()
                // and rename "Effective Speed" to just "Speed" but still
                // show the effective speed value:
                Label speedLabel = new Label(
                        String.format("Speed: %.2f", effectiveSpeed)
                );

                // Font styles
                typeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
                positionLabel.setFont(Font.font("Arial", 12));
                destinationLabel.setFont(Font.font("Arial", 12));
                directionLabel.setFont(Font.font("Arial", 12));
                speedLabel.setFont(Font.font("Arial", 12));

                // If this robot is selected, highlight it
                if (robot == selectedItem) {
                    robotBox.setBackground(new Background(
                            new BackgroundFill(Color.web("#e3f2fd"), new CornerRadii(10), Insets.EMPTY)
                    ));
                    robotBox.setBorder(new Border(new BorderStroke(
                            Color.web("#64b5f6"), BorderStrokeStyle.SOLID,
                            new CornerRadii(10), new BorderWidths(2)
                    )));
                }

                robotBox.getChildren().addAll(
                        typeLabel,
                        positionLabel,
                        destinationLabel,
                        directionLabel,
                        speedLabel  // The newly renamed "Speed: ???" label
                );

                robotInfoBox.getChildren().add(robotBox);
            }
        }

        robotInfoBox.setPrefHeight(400);
    }

    /**
     * Sets up mouse controls for selecting items in the arena.
     * Clicking on an item sets it as selected, updating the UI as needed.
     */
    private void initializeMouseControls() {
        if (arenaView.getCanvas() == null) {
            throw new IllegalStateException("ArenaView or its Canvas is not initialized.");
        }
        if (robotArena == null) {
            throw new IllegalStateException("RobotArena is not initialized.");
        }

        arenaView.getCanvas().setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                ArenaItem selectedItem = robotArena.findItemAt(e.getX(), e.getY());
                robotArena.setSelectedItem(selectedItem);
                arenaView.render();

                if (selectedItem != null) {
                    updateStatus("Selected: " + selectedItem.toString());
                    DebugUtils.log("Item selected: " + selectedItem.toString());
                } else {
                    updateStatus("No item selected.");
                    DebugUtils.log("No item selected.");
                }
            }
        });
    }

    /**
     * Sets up keyboard controls for moving the selected robot, toggling animation,
     * and deleting the selected item.
     *
     * @param scene The main application {@link Scene}.
     */
    private void initializeKeyboardControls(Scene scene) {
        scene.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case UP -> moveSelectedRobot(0, -10);
                case DOWN -> moveSelectedRobot(0, 10);
                case LEFT -> moveSelectedRobot(-10, 0);
                case RIGHT -> moveSelectedRobot(10, 0);
                case SPACE -> toggleAnimation();
                case DELETE -> {
                    robotArena.deleteSelectedItem();
                    arenaView.render();
                    updateStatus("Selected item deleted.");
                    DebugUtils.log("Selected item deleted.");
                }
                default -> {
                    // No action for other keys
                }
            }
            arenaView.render();
        });
    }

    /**
     * Sets up keyboard shortcuts for file-related operations and exiting the app.
     *
     * @param scene The main application {@link Scene}.
     */
    private void initializeKeyboardShortcuts(Scene scene) {
        scene.setOnKeyPressed(event -> {
            if (event.isControlDown()) {
                switch (event.getCode()) {
                    case N -> {
                        resetArena();
                        updateStatus("New arena initialized via shortcut.");
                        DebugUtils.log("Shortcut Ctrl+N: Arena reset.");
                    }
                    case S -> {
                        controlPanel.handleSave();
                        updateStatus("Arena saved via shortcut.");
                        DebugUtils.log("Shortcut Ctrl+S: Arena saved.");
                    }
                    case O -> {
                        controlPanel.handleLoad();
                        arenaView.render();
                        updateStatus("Arena loaded via shortcut.");
                        DebugUtils.log("Shortcut Ctrl+O: Arena loaded.");
                    }
                    case Q -> {
                        DebugUtils.log("Shortcut Ctrl+Q: Application exiting.");
                        Platform.exit();
                    }
                    default -> {
                        // No action for other shortcuts
                    }
                }
            }
        });
    }

    /**
     * Moves the selected robot by the specified deltas if it doesn't cause collisions.
     *
     * @param dx The change in the X-coordinate.
     * @param dy The change in the Y-coordinate.
     */
    private void moveSelectedRobot(double dx, double dy) {
        if (robotArena.getSelectedItem() instanceof Robot selectedRobot) {
            double newX = Math.max(
                    selectedRobot.getRadius(),
                    Math.min(
                            robotArena.getWidth() - selectedRobot.getRadius(),
                            selectedRobot.getX() + dx
                    )
            );
            double newY = Math.max(
                    selectedRobot.getRadius(),
                    Math.min(
                            robotArena.getHeight() - selectedRobot.getRadius(),
                            selectedRobot.getY() + dy
                    )
            );

            if (!robotArena.isColliding(newX, newY, selectedRobot.getRadius(), selectedRobot.getId())) {
                selectedRobot.setX(newX);
                selectedRobot.setY(newY);
                updateStatus("Moved " + selectedRobot.getClass().getSimpleName() +
                        " to (" + newX + ", " + newY + ")");
                DebugUtils.log("Moved " + selectedRobot.getClass().getSimpleName() +
                        " to (" + newX + ", " + newY + ")");
            } else {
                updateStatus("Cannot move " + selectedRobot.getClass().getSimpleName() +
                        " to (" + newX + ", " + newY + ") - Collision detected.");
                DebugUtils.log("Collision detected: Cannot move " + selectedRobot.getClass().getSimpleName() +
                        " to (" + newX + ", " + newY + ")");
            }
        }
    }

    /**
     * Updates the UI status display, typically called after each animation frame.
     */
    private void updateStatus() {
        updateRobotInfo(); // Refresh the info panel
        DebugUtils.log("Status updated: Robot information refreshed.");
    }

    /**
     * Updates the UI status display with a custom message.
     *
     * @param message The message to display in the status area.
     */
    private void updateStatus(String message) {
        controlPanel.updateStatus(message);
    }

    /**
     * Resets the arena to a default state, populating it with a few pre-defined items.
     */
    private void resetArena() {
        robotArena.clearArena();
        robotArena.addRobot();
        robotArena.addObstacle();
        robotArena.addChaserRobot();
        robotArena.addBumpRobot();
        robotArena.addBeamRobot();
        robotArena.addPredatorRobot();

        arenaView.render();
        updateStatus("Arena reset to default state.");
        DebugUtils.log("Arena reset to default state.");
    }

    /**
     * Starts the simulation's animation timer if it isn't already running.
     */
    private void startAnimation() {
        if (!isAnimating) {
            isAnimating = true;
            animationTimer.start();
            updateStatus("Animation started.");
            DebugUtils.log("Start button: Animation started.");
        }
    }

    /**
     * Stops the simulation's animation timer if it is currently running.
     */
    private void stopAnimation() {
        if (isAnimating) {
            isAnimating = false;
            animationTimer.stop();
            updateStatus("Animation stopped.");
            DebugUtils.log("Stop button: Animation stopped.");
        }
    }

    /**
     * Toggles the animation timer on and off.
     */
    private void toggleAnimation() {
        if (isAnimating) {
            stopAnimation();
        } else {
            startAnimation();
        }
    }

    /**
     * Displays the 'About' dialog with application information.
     */
    private void showAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Advanced Robot Simulator");
        alert.setHeaderText("Robot Simulator");

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        Label studentInfo = new Label("Developer ID: 32019071\nModule: CS2OP - Object-Oriented Programming");
        studentInfo.setFont(Font.font("System", FontWeight.BOLD, 12));

        Label description = new Label(
                "Welcome to the Advanced Robot Simulator, a sophisticated environment for simulating " +
                        "various autonomous robotic behaviors and interactions.\n\n" +
                        "• Basic Robot: Demonstrates fundamental movement and collision detection.\n" +
                        "• Chaser Robot: Implements advanced tracking and pursuit algorithms.\n" +
                        "• Beam Robot: Utilizes multiple sensor arrays for environmental awareness.\n" +
                        "• Bump Robot: Features sophisticated collision response mechanisms.\n" +
                        "• Predator Robot: Exhibits complex hunting and target acquisition behaviors.\n" +
                        "• Light-Seeking Robot: Demonstrates phototropic navigation capabilities.\n\n" +
                        "This real-time simulator displays robot interactions, environmental obstacles, " +
                        "and emergent behaviors in a dynamic arena setting."
        );
        description.setWrapText(true);

        content.getChildren().addAll(studentInfo, new Separator(), description);
        alert.getDialogPane().setContent(content);
        alert.showAndWait();
        DebugUtils.log("About dialog displayed.");
    }

    /**
     * Displays a comprehensive help dialog with usage instructions.
     */
    private void showHelpDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Robot Simulator - User Guide");
        alert.setHeaderText("Comprehensive User Guide");

        String helpText =
                "Robot Management\n" +
                        "• Adding Robots: Choose among Basic, Chaser, Beam, Bump, Predator, Light-Seeking.\n" +
                        "• Environmental Objects: Add obstacles or light sources.\n\n" +
                        "Arena Controls\n" +
                        "• Selection: Click items to select.\n" +
                        "• Movement: Use arrow keys for small manual moves.\n" +
                        "• Deletion: Press DELETE.\n" +
                        "• Speed Slider: Adjust from 0.1x to 5.0x.\n\n" +
                        "Simulation Controls\n" +
                        "• Start/Stop: Animation toggles.\n" +
                        "• Clear Arena: Removes all items.\n" +
                        "• Save/Load: Save or load arena states.\n\n" +
                        "Keyboard Shortcuts\n" +
                        "• Ctrl+N: New arena.\n" +
                        "• Ctrl+S: Save arena.\n" +
                        "• Ctrl+O: Load arena.\n" +
                        "• Ctrl+Q: Quit application.\n" +
                        "• SPACE: Toggle simulation.\n" +
                        "• DELETE: Remove selected item.\n\n" +
                        "Additional Features\n" +
                        "• Real-time collision detection.\n" +
                        "• Automatic or manual robot movement.\n" +
                        "• Quick debugging logs via DebugUtils.\n";

        TextArea textArea = new TextArea(helpText);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefRowCount(20);
        textArea.setPrefColumnCount(50);

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.getChildren().add(textArea);

        alert.getDialogPane().setContent(content);
        alert.showAndWait();
        DebugUtils.log("Help dialog displayed.");
    }

    /**
     * Launches the JavaFX application.
     *
     * @param args Command-line arguments.
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Provides external toggle control of the animation state.
     *
     * @param start True to start the animation; false to stop it.
     */
    public static void toggleAnimation(boolean start) {
        if (start) {
            isAnimating = true;
            if (animationTimer != null) {
                animationTimer.start();
            }
        } else {
            isAnimating = false;
            if (animationTimer != null) {
                animationTimer.stop();
            }
        }
    }
}
