package GUI;

import RobotSim.RobotArena;
import RobotSim.TextFile;
import RobotSim.DebugUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The {@code ControlPanel} class provides user interface controls for managing the RobotArena
 * simulation. It includes sections for adding various robots or obstacles, saving/loading arena
 * configurations, adjusting simulation speed, and displaying status information.
 *
 * <p>The panel is arranged horizontally, housing two vertical sections:
 * <ul>
 *   <li>Robot Controls</li>
 *   <li>Simulation Controls</li>
 * </ul>
 *
 * @version 1.0
 */
public class ControlPanel extends HBox {

    /** The main horizontal layout for the entire control panel. */
    private final HBox panel;

    /** Reference to the {@link RobotArena} whose items are controlled. */
    private final RobotArena robotArena;

    /** Reference to the {@link ArenaView} for rendering updates. */
    private final ArenaView arenaView;

    /** A slider to control the speed of the simulation. */
    private final Slider speedControl;

    /** Label to display basic status messages. */
    private final Label statusLabel;

    /** TextArea for displaying more detailed status or logs. */
    private final TextArea detailedStatusArea;

    /**
     * Constructs a new {@code ControlPanel} for the specified RobotArena and ArenaView.
     *
     * @param robotArena The {@code RobotArena} instance to be managed.
     * @param arenaView  The {@code ArenaView} instance for rendering updates.
     */
    public ControlPanel(RobotArena robotArena, ArenaView arenaView) {
        this.robotArena = robotArena;
        this.arenaView = arenaView;
        this.panel = new HBox(20); // Horizontal layout with spacing
        this.speedControl = createStyledSlider();
        this.statusLabel = new Label("Status: Ready");
        this.detailedStatusArea = new TextArea(); // Initialize the detailed status area

        initializePanel();
    }

    /**
     * Initializes the main layout of the control panel, setting up background styling,
     * alignment, and child sections.
     */
    private void initializePanel() {
        // Panel background and alignment
        panel.setPadding(new Insets(10));
        panel.setAlignment(Pos.TOP_LEFT);
        panel.setBackground(new Background(
                new BackgroundFill(Color.rgb(245, 245, 245), new CornerRadii(10), Insets.EMPTY)
        ));

        // Left section: Robot Controls (placeholder if needed)
        // Right section: Simulation Controls

        VBox robotSection = new VBox(10);
        robotSection.setPadding(new Insets(10));
        robotSection.setBackground(
                new Background(new BackgroundFill(Color.WHITE, new CornerRadii(5), Insets.EMPTY))
        );
        robotSection.setBorder(
                new Border(new BorderStroke(
                        Color.LIGHTGRAY, BorderStrokeStyle.SOLID, new CornerRadii(5), new BorderWidths(1)
                ))
        );
        robotSection.setPrefWidth(250);
        robotSection.setMaxWidth(250);
        robotSection.setMinWidth(200);

        Label robotLabel = new Label("Robot Controls");
        robotLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        robotLabel.setTextFill(Color.web("#2c3e50"));
        robotLabel.setAlignment(Pos.CENTER);
        robotLabel.setMaxWidth(Double.MAX_VALUE);

        // Add any robot control UI to 'robotSection' if needed.

        // Right section: Simulation Controls
        VBox simulationSection = new VBox(10);
        simulationSection.setPadding(new Insets(10));
        simulationSection.setBackground(
                new Background(new BackgroundFill(Color.WHITE, new CornerRadii(5), Insets.EMPTY))
        );
        simulationSection.setBorder(
                new Border(new BorderStroke(
                        Color.LIGHTGRAY, BorderStrokeStyle.SOLID, new CornerRadii(5), new BorderWidths(1)
                ))
        );
        simulationSection.setPrefWidth(300);
        simulationSection.setMaxWidth(300);
        simulationSection.setMinWidth(250);

        Label simulationLabel = new Label("Simulation Controls");
        simulationLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        simulationLabel.setTextFill(Color.web("#2c3e50"));
        simulationLabel.setAlignment(Pos.CENTER);
        simulationLabel.setMaxWidth(Double.MAX_VALUE);

        simulationSection.getChildren().addAll(
                simulationLabel,
                new Separator(),
                createSimulationSection(),
                createArenaControlsSection(),
                new Separator(),
                createStatusSection()
        );

        // Add both sections to the main horizontal panel
        panel.getChildren().addAll(robotSection, simulationSection);
    }

    /**
     * Creates a styled button for a specific robot type with an emoji.
     * Adjust as needed in your UI flow.
     *
     * @param robotType The type/name of the robot.
     * @param icon      The emoji representing the robot.
     * @return A styled {@link Button} instance.
     */
    private Button createRobotButton(String robotType, String icon) {
        Button button = new Button();

        // Container for the icon and text
        VBox content = new VBox(5);
        content.setAlignment(Pos.CENTER);

        Label iconLabel = new Label(icon);
        iconLabel.setFont(Font.font("System", 24));  // Larger emoji for visibility

        Label nameLabel = new Label(robotType.replace(" Robot", ""));
        nameLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));

        content.getChildren().addAll(iconLabel, nameLabel);
        button.setGraphic(content);

        button.setPrefSize(80, 80);

        // Button backgrounds for normal and hover states
        Background normalBackground = new Background(
                new BackgroundFill(Color.WHITE, new CornerRadii(5), Insets.EMPTY)
        );
        Border normalBorder = new Border(
                new BorderStroke(Color.web("#e0e0e0"), BorderStrokeStyle.SOLID,
                        new CornerRadii(5), new BorderWidths(1))
        );
        Background hoverBackground = new Background(
                new BackgroundFill(Color.web("#f5f5f5"), new CornerRadii(5), Insets.EMPTY)
        );

        button.setBackground(normalBackground);
        button.setBorder(normalBorder);

        // Hover effect
        button.setOnMouseEntered(e -> {
            button.setBackground(hoverBackground);
            button.setBorder(normalBorder);
        });
        button.setOnMouseExited(e -> {
            button.setBackground(normalBackground);
            button.setBorder(normalBorder);
        });

        // Click handler
        button.setOnAction(e -> {
            try {
                switch (robotType) {
                    case "Basic Robot" -> {
                        robotArena.addRobot();
                        updateStatus("Added new Basic Robot");
                        DebugUtils.log("Added Basic Robot to arena");
                    }
                    case "Chaser Robot" -> {
                        robotArena.addChaserRobot();
                        updateStatus("Added new Chaser Robot");
                        DebugUtils.log("Added Chaser Robot to arena");
                    }
                    case "Beam Robot" -> {
                        robotArena.addBeamRobot();
                        updateStatus("Added new Beam Robot");
                        DebugUtils.log("Added Beam Robot to arena");
                    }
                    case "Bump Robot" -> {
                        robotArena.addBumpRobot();
                        updateStatus("Added new Bump Robot");
                        DebugUtils.log("Added BumpRobot to arena");
                    }
                    case "Transformer Robot" -> {
                        updateStatus("Added new Transformer Robot");
                        DebugUtils.log("Added Transformer Robot to arena");
                    }
                    default -> {
                        showAlert(AlertType.WARNING, "Unknown Robot Type",
                                "The selected robot type is not recognized.");
                        DebugUtils.log("Attempted to add unknown robot type: " + robotType);
                        return;
                    }
                }
                arenaView.render();
            } catch (Exception ex) {
                showAlert(AlertType.ERROR, "Error Adding Robot",
                        "Failed to add " + robotType + ": " + ex.getMessage());
                DebugUtils.log("Error adding " + robotType + ": " + ex.getMessage());
            }
        });

        return button;
    }

    /**
     * Creates a {@link VBox} containing arena control buttons (add obstacle, clear, save, load).
     *
     * @return A {@link VBox} representing the arena controls section.
     */
    private VBox createArenaControlsSection() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));
        box.setBackground(new Background(
                new BackgroundFill(Color.WHITE, new CornerRadii(5), Insets.EMPTY)
        ));

        Label sectionLabel = new Label("Arena Controls");
        sectionLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        sectionLabel.setTextFill(Color.web("#34495e"));

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        // Buttons for add obstacle, clear arena, save, load
        Button addObstacle = createStyledButton(
                "Add Obstacle",
                "Click to add a new obstacle",
                e -> {
                    try {
                        robotArena.addObstacle();
                        arenaView.render();
                        updateStatus("Added new obstacle");
                        DebugUtils.log("Add Obstacle clicked: Added a new obstacle");
                    } catch (Exception ex) {
                        DebugUtils.log("Error adding obstacle: " + ex.getMessage());
                        showAlert(AlertType.ERROR, "Add Obstacle Error",
                                "Failed to add obstacle: " + ex.getMessage());
                    }
                }
        );

        Button clearButton = createStyledButton(
                "Clear Arena",
                "Click to remove all items from the arena",
                e -> {
                    try {
                        robotArena.clearArena();
                        robotArena.setSelectedItem(null);
                        arenaView.render();
                        updateStatus("Arena cleared");
                        DebugUtils.log("Arena cleared successfully");
                    } catch (Exception ex) {
                        DebugUtils.log("Error clearing arena: " + ex.getMessage());
                        showAlert(AlertType.ERROR, "Clear Error",
                                "Failed to clear arena: " + ex.getMessage());
                    }
                }
        );

        Button saveButton = createStyledButton(
                "Save Arena",
                "Click to save the current arena configuration",
                e -> {
                    FileChooser fileChooser = new FileChooser();
                    fileChooser.setTitle("Save Arena Configuration");
                    fileChooser.getExtensionFilters().addAll(
                            new FileChooser.ExtensionFilter("Robot Arena Files", "*.arena"),
                            new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                            new FileChooser.ExtensionFilter("All Files", "*.*")
                    );

                    File file = fileChooser.showSaveDialog(panel.getScene().getWindow());
                    if (file != null) {
                        try {
                            String state = robotArena.saveArenaState();
                            if (TextFile.writeFile(file.getAbsolutePath(), state)) {
                                updateStatus("Arena saved successfully to " + file.getName());
                                DebugUtils.log("Arena saved to: " + file.getAbsolutePath());
                            } else {
                                throw new IOException("Failed to write file");
                            }
                        } catch (Exception ex) {
                            String errorMessage = "Error saving arena: " + ex.getMessage();
                            updateStatus(errorMessage);
                            DebugUtils.log(errorMessage);
                            showAlert(Alert.AlertType.ERROR, "Save Error", errorMessage);
                        }
                    }
                }
        );

        Button loadButton = createStyledButton(
                "Load Arena",
                "Click to load a saved arena configuration",
                e -> {
                    FileChooser fileChooser = new FileChooser();
                    fileChooser.setTitle("Load Arena Configuration");
                    fileChooser.getExtensionFilters().addAll(
                            new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                            new FileChooser.ExtensionFilter("All Files", "*.*")
                    );

                    File file = fileChooser.showOpenDialog(panel.getScene().getWindow());
                    if (file != null) {
                        try {
                            String content = TextFile.readFile(file.getAbsolutePath());
                            if (content != null && !content.isBlank()) {
                                robotArena.loadArenaState(content);
                                arenaView.render();
                                updateStatus("Arena loaded successfully from " + file.getName());
                                DebugUtils.log("Arena loaded from: " + file.getAbsolutePath());
                            } else {
                                throw new IOException("File is empty or invalid");
                            }
                        } catch (Exception ex) {
                            String errorMessage = "Error loading arena: " + ex.getMessage();
                            updateStatus(errorMessage);
                            DebugUtils.log(errorMessage);
                            showAlert(Alert.AlertType.ERROR, "Load Error", errorMessage);
                        }
                    }
                }
        );

        buttonBox.getChildren().addAll(addObstacle, clearButton, saveButton, loadButton);
        box.getChildren().addAll(sectionLabel, buttonBox);
        return box;
    }

    /**
     * Creates a styled button with tooltip and hover effects.
     *
     * @param text    The button text.
     * @param tooltip The tooltip text for the button.
     * @param action  Event handler for the button's {@code onAction}.
     * @return A styled {@link Button} instance.
     */
    private Button createStyledButton(String text, String tooltip,
                                      javafx.event.EventHandler<javafx.event.ActionEvent> action) {
        Button button = new Button(text);
        button.setOnAction(e -> {
            try {
                action.handle(e);
            } catch (Exception ex) {
                showAlert(AlertType.ERROR, "Operation Failed",
                        "Failed to execute " + text + ": " + ex.getMessage());
                DebugUtils.log("Error in " + text + ": " + ex.getMessage());
            }
        });

        button.setTooltip(new Tooltip(tooltip));
        button.setPrefHeight(30);
        button.setMaxWidth(Double.MAX_VALUE);

        Background normalBackground = new Background(
                new BackgroundFill(Color.web("#3498db"), new CornerRadii(4), Insets.EMPTY)
        );
        Background hoverBackground = new Background(
                new BackgroundFill(Color.web("#2980b9"), new CornerRadii(4), Insets.EMPTY)
        );

        // Default appearance
        button.setBackground(normalBackground);
        button.setTextFill(Color.WHITE);
        button.setFont(Font.font("System", FontWeight.NORMAL, 12));

        // Hover effect
        button.setOnMouseEntered(e -> button.setBackground(hoverBackground));
        button.setOnMouseExited(e -> button.setBackground(normalBackground));

        return button;
    }

    /**
     * Creates a slider with tick marks for adjusting the simulation speed.
     *
     * @return A {@link Slider} instance with configured range and properties.
     */
    private Slider createStyledSlider() {
        Slider slider = new Slider(0.5, 2.0, 1.0);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(0.5);
        slider.setMinorTickCount(4);
        slider.setBlockIncrement(0.1);
        slider.setPrefWidth(150);

        Tooltip sliderTooltip = new Tooltip("Adjust the simulation speed");
        Tooltip.install(slider, sliderTooltip);

        // Update RobotArena speed on value change
        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            try {
                robotArena.updateSimulationSpeed(newVal.doubleValue());
                updateStatus(String.format("Simulation speed set to %.1fx", newVal.doubleValue()));
                DebugUtils.log(String.format("Speed changed: %.1fx -> %.1fx",
                        oldVal.doubleValue(), newVal.doubleValue()));
            } catch (Exception ex) {
                showAlert(AlertType.ERROR, "Speed Update Failed",
                        "Failed to update simulation speed: " + ex.getMessage());
                DebugUtils.log("Error updating speed: " + ex.getMessage());
                // Revert to old value
                slider.setValue(oldVal.doubleValue());
            }
        });
        return slider;
    }

    /**
     * Updates the speed of all robots in the arena by a given multiplier.
     * (An external call if needed.)
     *
     * @param speedMultiplier A multiplier to apply to the robot speeds.
     */
    public void updateRobotSpeeds(double speedMultiplier) {
        robotArena.updateSimulationSpeed(speedMultiplier);
    }

    /**
     * Creates the simulation controls section, containing speed slider and start/stop buttons.
     *
     * @return A {@link VBox} representing the simulation controls section.
     */
    private VBox createSimulationSection() {
        VBox simulationControlsSection = new VBox(10);
        simulationControlsSection.setBackground(new Background(
                new BackgroundFill(Color.WHITE, new CornerRadii(5), Insets.EMPTY)
        ));

        Label sectionLabel = new Label("Simulation Controls");
        sectionLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        sectionLabel.setTextFill(Color.web("#34495e"));

        VBox speedBox = new VBox(5);
        speedBox.setAlignment(Pos.CENTER_LEFT);

        Label speedLabel = new Label("Simulation Speed:");
        speedLabel.setFont(Font.font("System", 12));

        // Additional speed slider to show on-screen speed changes
        Slider speedSlider = createStyledSlider();
        Label speedValueLabel = new Label(String.format("%.1fx", speedSlider.getValue()));

        // Reflect changes in the label
        speedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double speed = newVal.doubleValue();
            speedLabel.setText(String.format("Speed: %.1fx", speed));
            robotArena.updateSimulationSpeed(speed);
            DebugUtils.log("Speed slider adjusted: " + speed);
        });

        Button resetSpeedButton = new Button("Reset Speed");
        resetSpeedButton.setOnAction(e -> speedSlider.setValue(1.0));

        speedBox.getChildren().addAll(speedLabel, speedSlider, speedValueLabel, resetSpeedButton);

        // Animation control buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_LEFT);

        Button startButton = createStyledButton(
                "Start",
                "Start simulation",
                e -> {
                    MainApp.toggleAnimation(true);
                    updateStatus("Animation started.");
                    DebugUtils.log("Start button: Animation started.");
                }
        );

        Button stopButton = createStyledButton(
                "Stop",
                "Stop simulation",
                e -> {
                    MainApp.toggleAnimation(false);
                    updateStatus("Animation stopped.");
                    DebugUtils.log("Stop button: Animation stopped.");
                }
        );

        buttonBox.getChildren().addAll(startButton, stopButton);

        simulationControlsSection.getChildren().addAll(sectionLabel, speedBox, buttonBox);
        return simulationControlsSection;
    }

    /**
     * Creates the status section, containing a basic status label and a more detailed text area.
     *
     * @return A {@link VBox} representing the status display section.
     */
    private VBox createStatusSection() {
        VBox box = new VBox(5);
        box.setPadding(new Insets(10));
        box.setBackground(new Background(
                new BackgroundFill(Color.web("#f8f9fa"), new CornerRadii(5), Insets.EMPTY)
        ));

        statusLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        statusLabel.setTextFill(Color.web("#34495e"));
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(Double.MAX_VALUE);

        detailedStatusArea.setFont(Font.font("System", 12));
        detailedStatusArea.setEditable(false);
        detailedStatusArea.setWrapText(true);
        detailedStatusArea.setMaxHeight(200);

        box.getChildren().addAll(statusLabel, detailedStatusArea);
        return box;
    }

    /**
     * Updates the detailed status area with a provided text.
     *
     * @param detailedStatus The text to display in the detailed status area.
     */
    public void updateDetailedStatus(String detailedStatus) {
        if (detailedStatus != null && !detailedStatus.isBlank()) {
            detailedStatusArea.setText(detailedStatus.trim());
        }
    }

    /**
     * Attempts to find a valid {@link Window} for displaying file choosers and dialogs.
     *
     * @return A {@link Window} if found, or {@code null} otherwise.
     */
    private Window findWindow() {
        // First try the panel's scene
        if (panel.getScene() != null && panel.getScene().getWindow() != null) {
            return panel.getScene().getWindow();
        }
        // Otherwise, fallback to any showing window
        return Stage.getWindows().stream()
                .filter(Window::isShowing)
                .findFirst()
                .orElse(null);
    }

    /**
     * Creates a default directory path for saving arena files.
     *
     * @return The default directory path as a {@link String}.
     */
    private String getDefaultDirectory() {
        String userHome = System.getProperty("user.home");
        return userHome + File.separator + "RobotSimulator";
    }

    /**
     * Handles saving the current arena configuration to a file, with a file chooser dialog.
     */
    public void handleSave() {
        try {
            FileChooser fileChooser = SaveLoadHandler.createFileChooser("Save Arena Configuration");
            Window window = findWindow();
            if (window == null) {
                throw new IllegalStateException("Cannot open save dialog - no active window found");
            }

            File selectedFile = fileChooser.showSaveDialog(window);
            if (selectedFile != null) {
                selectedFile = SaveLoadHandler.ensureFileExtension(selectedFile, ".arena");

                File parentDir = selectedFile.getParentFile();
                if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                    throw new IOException("Failed to create directory: " + parentDir);
                }

                String state = robotArena.saveArenaState();
                if (TextFile.writeFile(selectedFile.getAbsolutePath(), state)) {
                    updateStatus("Arena saved successfully to " + selectedFile.getName());
                    DebugUtils.log("Arena saved to: " + selectedFile.getAbsolutePath());
                } else {
                    throw new IOException("Failed to write file");
                }
            }
        } catch (Exception e) {
            String errorMessage = "Error saving arena: " + e.getMessage();
            updateStatus(errorMessage);
            DebugUtils.log(errorMessage);
            showAlert(Alert.AlertType.ERROR, "Save Error", errorMessage);
        }
    }

    /**
     * Handles loading an arena configuration from a file, via a file chooser dialog.
     */
    public void handleLoad() {
        Window window = findWindow();
        if (window == null) {
            showAlert(AlertType.ERROR, "Error", "Cannot open load dialog - window not found");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load Arena Configuration");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home") + File.separator + "Downloads"));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Robot Arena Files", "*.arena"),
                new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        try {
            File file = fileChooser.showOpenDialog(window);
            if (file != null) {
                String content = TextFile.readFile(file.getAbsolutePath());
                DebugUtils.log("Loaded file content: " + content);

                if (content != null && !content.isBlank()) {

                    robotArena.loadArenaState(content);
                    arenaView.render();
                    updateStatus("Arena loaded successfully from " + file.getName());
                    DebugUtils.log("Arena loaded from: " + file.getAbsolutePath());
                } else {
                    throw new IOException("File is empty or invalid");
                }
            }
        } catch (Exception ex) {
            String errorMessage = "Error loading arena: " + ex.getMessage();
            updateStatus(errorMessage);
            DebugUtils.log(errorMessage);
            showAlert(Alert.AlertType.ERROR, "Load Error", errorMessage);
        }
    }

    /**
     * Updates the status label with the provided message.
     *
     * @param message The status message to display (non-empty).
     */
    public void updateStatus(String message) {
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Status message cannot be null or empty");
        }
        statusLabel.setText("Status: " + message.trim());
    }

    /**
     * Displays an alert dialog with the specified type, title, and content.
     *
     * @param alertType The {@link AlertType} (e.g., INFO, ERROR).
     * @param title     The title of the alert dialog.
     * @param content   The message to display in the alert.
     */
    private void showAlert(AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType, content, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    /**
     * Nested helper class for handling file save/load operations,
     * providing a file chooser and ensuring correct file extensions.
     */
    public static class SaveLoadHandler {

        /**
         * Retrieves the default directory for saving arena configurations,
         * creating it if necessary.
         *
         * @return The {@link Path} to the default directory.
         */
        public static Path getDefaultDirectory() {
            String userHome = System.getProperty("user.home");
            Path savePath = Paths.get(userHome, "RobotSimulator", "saves");

            try {
                Files.createDirectories(savePath);
                return savePath;
            } catch (Exception e) {
                // Fallback to user home if creation fails
                return Paths.get(userHome);
            }
        }

        /**
         * Configures a {@link FileChooser} for saving/loading arena files.
         *
         * @param title The title for the file chooser dialog.
         * @return A configured {@link FileChooser} instance.
         */
        public static FileChooser createFileChooser(String title) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle(title);

            // Set initial directory
            Path defaultDir = getDefaultDirectory();
            if (Files.exists(defaultDir)) {
                fileChooser.setInitialDirectory(defaultDir.toFile());
            }

            // Add extension filters
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Robot Arena Files", "*.arena"),
                    new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                    new FileChooser.ExtensionFilter("All Files", "*.*")
            );

            return fileChooser;
        }

        /**
         * Ensures the file has the specified extension if none is present.
         *
         * @param file             The selected {@link File}.
         * @param defaultExtension The default extension to use if none is found.
         * @return A {@link File} with the proper extension.
         */
        public static File ensureFileExtension(File file, String defaultExtension) {
            if (file == null) return null;

            String name = file.getName();
            if (!name.contains(".")) {
                return new File(file.getParentFile(), name + defaultExtension);
            }
            return file;
        }
    }
}
