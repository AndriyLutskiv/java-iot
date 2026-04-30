package ua.crowpi.projects.p07;

/**
 * Rules engine for the automatic greenhouse controller.
 *
 * <p>Evaluates a {@link GreenhouseReading} against the configured thresholds
 * ({@link ThresholdConfig}) and the current {@link ManualOverride} state to
 * determine which actuators should be active.</p>
 *
 * <p><strong>Decision rules (applied in order):</strong></p>
 * <ol>
 *   <li>If {@code soilPercent < config.getSoilMinPercent()} → activate pump</li>
 *   <li>If {@code tempC > config.getTempMaxCelsius()} → activate fan</li>
 *   <li>If {@code override.isPumpForced()} → activate pump regardless of soil reading</li>
 * </ol>
 *
 * <p>Rules 1 and 3 are combined with a logical OR so that manual override
 * takes effect even when the soil is moist enough.</p>
 */
public class GreenhouseController {

    /** Threshold configuration loaded from {@code greenhouse.properties}. */
    private final ThresholdConfig config;

    /** Manual override state toggled by the physical button on GPIO 11. */
    private final ManualOverride override;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Creates a GreenhouseController with the given configuration and override state.
     *
     * @param config   threshold configuration (non-null)
     * @param override manual override holder (non-null)
     */
    public GreenhouseController(ThresholdConfig config, ManualOverride override) {
        this.config   = config;
        this.override = override;
    }

    // -------------------------------------------------------------------------
    // Rules evaluation
    // -------------------------------------------------------------------------

    /**
     * Evaluates the greenhouse rules against a sensor reading and returns
     * the desired {@link ActuatorState}.
     *
     * <p>The pump rule combines automatic (soil too dry) and manual override
     * with OR so that either condition alone is sufficient to activate the pump.
     * The fan rule is purely automatic — there is no manual fan override in this version.</p>
     *
     * @param reading the latest sensor snapshot (non-null)
     * @return the desired actuator state derived from the rules
     */
    public ActuatorState evaluate(GreenhouseReading reading) {
        // Правило насосу: грунт занадто сухий АБО ручне перевизначення активне
        boolean pump = reading.getSoilPercent() < config.getSoilMinPercent()
                || override.isPumpForced();

        // Правило вентилятора: температура перевищує максимально допустиму
        boolean fan = reading.getTempC() > config.getTempMaxCelsius();

        System.out.printf("[CONTROLLER] soil=%d%% (min=%d), temp=%.1fC (max=%.1f), "
                        + "forced=%s → pump=%s, fan=%s%n",
                reading.getSoilPercent(), config.getSoilMinPercent(),
                reading.getTempC(), config.getTempMaxCelsius(),
                override.isPumpForced(),
                pump ? "ON" : "OFF", fan ? "ON" : "OFF");

        return ActuatorState.of(pump, fan);
    }
}
