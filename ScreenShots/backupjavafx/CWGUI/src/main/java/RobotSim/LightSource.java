package RobotSim;

import RobotSim.ArenaItem;
import RobotSim.RobotArena;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.Glow;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

/**
 * The {@code LightSource} class represents a luminous entity in the {@link RobotArena}.
 * It emits light within a specified range and can be set active or inactive.
 * The class includes pulsing effects for visual feedback.
 *
 * <p>This class extends the {@link ArenaItem} abstract class and overrides
 * drawing and updating methods to provide visual and logic updates for the light source.</p>
 *
 * @version 1.0
 */
class LightSource extends ArenaItem {

    /** Intensity of the light, clamped between [0..1]. */
    private double intensity;

    /** Effective range at which this light can be detected. */
    private double range;

    /** Base color used to draw the light's glow. */
    private Color lightColor;

    /** Tracks the pulsing phase for the light animation. */
    private double pulsePhase = 0;

    /** Rate at which the pulsing phase changes. */
    private static final double PULSE_RATE = 0.05;

    /** Indicates whether the light source is currently active. */
    private boolean active = true;

    /**
     * Constructs a new {@code LightSource}.
     *
     * @param x         The x-coordinate of the light source's center.
     * @param y         The y-coordinate of the light source's center.
     * @param radius    The radius of the light source.
     * @param intensity The intensity of the light (0.0 to 1.0).
     * @param range     The base range at which the light can be detected.
     */
    public LightSource(double x, double y, double radius, double intensity, double range) {
        super(x, y, radius);

        // Keep intensity within [0..1].
        this.intensity = Math.min(1.0, Math.max(0.0, intensity));

        // Increase the range for extended detection.
        double rangeMultiplier = 3; // Example multiplier for a more pronounced effect.
        this.range = range * rangeMultiplier;

        // A lightly yellow color with partial transparency.
        this.lightColor = Color.rgb(255, 255, 200, 0.5);
    }

    /**
     * Determines whether the light source is currently active.
     *
     * @return {@code true} if active, {@code false} otherwise
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Sets the light source to active or inactive.
     *
     * @param active {@code true} to activate the light, {@code false} to deactivate
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Calculates the light intensity at a given point (x, y).
     * If the point is outside the range or the light is inactive, returns 0.
     * Otherwise, applies a linear falloff multiplied by a pulsing factor.
     *
     * @param x The x-coordinate of the point to measure.
     * @param y The y-coordinate of the point to measure.
     * @return The computed light intensity at the specified location.
     */
    public double getLightIntensityAt(double x, double y) {
        if (!active) {
            // No light contribution if inactive
            return 0.0;
        }
        double distance = Math.hypot(x - getX(), y - getY());
        if (distance > range) {
            return 0.0;
        }

        // Linear falloff
        double normalizedDistance = distance / range;
        double baseFalloff = intensity * (1.0 - normalizedDistance);

        // Add a pulsing factor
        return baseFalloff * (0.8 + 0.2 * Math.sin(pulsePhase));
    }

    /**
     * Renders the light source and its glow on the specified {@code GraphicsContext}.
     * Drawing is skipped if the light is inactive.
     *
     * @param gc The {@code GraphicsContext} to draw on.
     */
    @Override
    public void draw(GraphicsContext gc) {
        if (!active) {
            return; // Skip drawing when inactive
        }

        // Apply a Glow effect that pulsates over time
        Glow glow = new Glow(0.8 + 0.2 * Math.sin(pulsePhase));
        gc.setEffect(glow);

        // Draw outer glow with a radial gradient
        RadialGradient outerGlow = new RadialGradient(
                0, 0, getX(), getY(), range,
                false, CycleMethod.NO_CYCLE,
                new Stop(0, lightColor.deriveColor(1, 1, 1, 0.3)),
                new Stop(0.5, lightColor.deriveColor(1, 1, 1, 0.1)),
                new Stop(1, lightColor.deriveColor(1, 1, 1, 0))
        );
        gc.setFill(outerGlow);
        gc.fillOval(getX() - range, getY() - range, range * 2, range * 2);

        // Draw the core glow (center circle)
        RadialGradient coreGlow = new RadialGradient(
                0, 0, getX(), getY(), getRadius(),
                false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.WHITE),
                new Stop(0.7, lightColor),
                new Stop(1, lightColor.darker())
        );
        gc.setFill(coreGlow);
        gc.fillOval(getX() - getRadius(), getY() - getRadius(),
                getRadius() * 2, getRadius() * 2);

        // Reset any effects after drawing
        gc.setEffect(null);
    }

    /**
     * Updates the state of the light source in the arena, including the pulsing phase
     * if it is active.
     *
     * @param arena The current {@link RobotArena} (not used in this example).
     */
    @Override
    public void updateState(RobotArena arena) {
        if (!active) {
            return; // Skip updates if inactive
        }

        // Increment the pulsing phase
        pulsePhase += PULSE_RATE;
        if (pulsePhase > Math.PI * 2) {
            pulsePhase -= Math.PI * 2; // Keep the phase in range
        }
    }

    /**
     * Sets the intensity of the light source (clamped between 0 and 1).
     *
     * @param intensity The new intensity, clamped to [0..1].
     */
    public void setIntensity(double intensity) {
        this.intensity = Math.min(1.0, Math.max(0.0, intensity));
    }

    /**
     * Returns the current intensity of the light source.
     *
     * @return The intensity value.
     */
    public double getIntensity() {
        return intensity;
    }

    /**
     * Retrieves the current range of the light source.
     *
     * @return The detection range of the light.
     */
    public double getRange() {
        return range;
    }

    /**
     * Sets a new range for the light source.
     *
     * @param newRange The new range value to apply.
     */
    public void setRange(double newRange) {
        this.range = newRange;
    }
}
