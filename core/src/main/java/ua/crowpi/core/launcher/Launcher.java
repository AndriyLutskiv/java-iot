package ua.crowpi.core.launcher;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ua.crowpi.core.CrowPiProject;
import ua.crowpi.core.exception.HardwareException;
import ua.crowpi.core.util.GracefulShutdown;

import java.util.List;
import java.util.Optional;

/**
 * Application entry point for the CrowPi Java Educational Suite.
 *
 * <p>Parses CLI arguments using Apache Commons CLI and either:</p>
 * <ul>
 *   <li>Launches a specific project directly ({@code --project=&lt;id&gt;});</li>
 *   <li>Shows the {@link InteractiveMenu} when no project is specified;</li>
 *   <li>Lists all registered projects and exits ({@code --list});</li>
 *   <li>Prints help ({@code --help}).</li>
 * </ul>
 *
 * <h3>CLI Reference</h3>
 * <pre>
 * Usage: java -jar crowpi-suite.jar [options]
 *
 *   --project=&lt;id&gt;   Launch a specific project directly (e.g. --project=rfid)
 *   --mock            Run without real GPIO (mock mode for laptop development)
 *   --list            Print all available projects and exit
 *   --help            Print this help message and exit
 * </pre>
 *
 * <h3>Examples</h3>
 * <pre>
 *   # Interactive menu on Raspberry Pi
 *   java -jar crowpi-suite.jar
 *
 *   # Run RFID project directly on real hardware
 *   java -jar crowpi-suite.jar --project=rfid
 *
 *   # Run Smart Home in mock mode on a laptop
 *   java -jar crowpi-suite.jar --project=smart-home --mock
 *
 *   # List all registered projects
 *   java -jar crowpi-suite.jar --list
 * </pre>
 */
public class Launcher {

    private static final Logger LOG = LogManager.getLogger(Launcher.class);

    // -------------------------------------------------------------------------
    // Опис CLI-опцій
    // -------------------------------------------------------------------------

    /** Apache Commons CLI descriptor for all supported command-line options. */
    private static final Options CLI_OPTIONS = buildCliOptions();

    /**
     * Builds the Apache Commons CLI {@link Options} descriptor.
     *
     * @return configured Options object
     */
    private static Options buildCliOptions() {
        Options opts = new Options();

        // --project=<id>  — запустити конкретний проект напряму
        opts.addOption(Option.builder()
                .longOpt("project")
                .hasArg()
                .argName("id")
                .desc("Launch a specific project by its id (e.g. rfid, smart-home, thermometer)")
                .build());

        // --mock  — режим без реального GPIO (для розробки на ноутбуці)
        opts.addOption(Option.builder()
                .longOpt("mock")
                .desc("Run without real hardware (mock mode for laptop development)")
                .build());

        // --list  — вивести список проектів і вийти
        opts.addOption(Option.builder()
                .longOpt("list")
                .desc("Print all available projects and exit")
                .build());

        // --help  — довідка
        opts.addOption(Option.builder()
                .longOpt("help")
                .desc("Print this help message and exit")
                .build());

        return opts;
    }

    // -------------------------------------------------------------------------
    // main()
    // -------------------------------------------------------------------------

    /**
     * Application entry point.
     *
     * <p>Exits with code {@code 0} on success, {@code 1} on argument errors,
     * and {@code 2} on hardware failures.</p>
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        LOG.info("CrowPi Java Educational Suite starting");

        CommandLine cmd = parseArgs(args);
        if (cmd == null) {
            // parseArgs вже надрукував повідомлення про помилку
            System.exit(1);
        }

        if (cmd.hasOption("help")) {
            printHelp();
            System.exit(0);
        }

        // Ініціалізуємо реєстр — ServiceLoader знаходить усі проекти в fat JAR
        ProjectRegistry registry = new ProjectRegistry();

        if (cmd.hasOption("list")) {
            printProjectList(registry);
            System.exit(0);
        }

        boolean mockMode = cmd.hasOption("mock");
        if (mockMode) {
            LOG.info("Mock mode enabled — no real GPIO will be accessed");
        }

        if (cmd.hasOption("project")) {
            // Прямий запуск конкретного проекту
            String projectId = cmd.getOptionValue("project");
            runProjectDirectly(registry, projectId, mockMode);
        } else {
            // Інтерактивне меню — студент обирає проект вручну
            InteractiveMenu menu = new InteractiveMenu(registry);
            menu.show(mockMode);
        }

        LOG.info("CrowPi Java Educational Suite exiting normally");
        System.exit(0);
    }

    // -------------------------------------------------------------------------
    // Допоміжні методи
    // -------------------------------------------------------------------------

    /**
     * Parses the raw command-line arguments into a {@link CommandLine} object.
     *
     * @param args the raw {@code main()} arguments
     * @return parsed {@link CommandLine}, or {@code null} if parsing failed
     */
    private static CommandLine parseArgs(String[] args) {
        CommandLineParser parser = new DefaultParser();
        try {
            return parser.parse(CLI_OPTIONS, args);
        } catch (ParseException e) {
            // Невалідні аргументи — друкуємо помилку і підказку
            System.err.println("Error: " + e.getMessage());
            System.err.println("Use --help for usage information.");
            LOG.error("CLI argument parsing failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Prints usage help to stdout using the standard Apache Commons CLI formatter.
     */
    private static void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        // Ширина 80 символів — стандарт для терміналів RPi
        formatter.setWidth(80);
        formatter.printHelp("java -jar crowpi-suite.jar", "\nCrowPi Java Educational Suite\n\n",
                CLI_OPTIONS,
                "\nExamples:\n"
                        + "  java -jar crowpi-suite.jar                        # interactive menu\n"
                        + "  java -jar crowpi-suite.jar --project=rfid          # run RFID project\n"
                        + "  java -jar crowpi-suite.jar --project=rfid --mock  # mock mode\n",
                true);
    }

    /**
     * Prints all registered projects as a numbered list.
     *
     * @param registry the populated project registry
     */
    private static void printProjectList(ProjectRegistry registry) {
        List<CrowPiProject> projects = registry.getAllProjects();
        System.out.println("CrowPi Java Educational Suite — Available Projects");
        System.out.println("--------------------------------------------------");
        if (projects.isEmpty()) {
            System.out.println("  (no projects registered)");
        } else {
            for (int i = 0; i < projects.size(); i++) {
                CrowPiProject p = projects.get(i);
                System.out.printf("  %2d.  %-30s  id=%-20s  %s%n",
                        i + 1, p.getName(), p.getProjectId(), p.getDescription());
            }
        }
        System.out.printf("%n  Total: %d project(s)%n", projects.size());
    }

    /**
     * Looks up the project by ID and runs it directly (no menu interaction).
     *
     * @param registry  the populated project registry
     * @param projectId the project identifier from {@code --project} flag
     * @param mockMode  whether to use mock hardware
     */
    private static void runProjectDirectly(ProjectRegistry registry,
                                           String projectId,
                                           boolean mockMode) {
        Optional<CrowPiProject> found = registry.findById(projectId);

        if (!found.isPresent()) {
            // Проект не знайдено — виводимо список доступних id-ів для підказки
            System.err.printf("Error: project '%s' not found.%n", projectId);
            System.err.println("Available project ids:");
            for (CrowPiProject p : registry.getAllProjects()) {
                System.err.printf("  %s  (%s)%n", p.getProjectId(), p.getName());
            }
            System.exit(1);
        }

        CrowPiProject project = found.get();
        LOG.info("Running project directly: {} ({})", project.getName(), project.getProjectId());

        // Реєструємо shutdown hook до run(), щоб Ctrl+C завжди звільняв GPIO
        GracefulShutdown.register(project);

        System.out.printf("Starting: %s%n", project.getName());
        System.out.printf("Description: %s%n", project.getDescription());
        if (mockMode) {
            System.out.println("[MOCK MODE — no real hardware required]");
        }
        System.out.println("Press Ctrl+C to stop.");
        System.out.println();

        try {
            project.run(mockMode);
        } catch (HardwareException e) {
            LOG.error("Hardware error in project '{}': {}", project.getProjectId(), e.getMessage(), e);
            System.err.printf("Hardware error: %s%n", e.getMessage());
            System.err.println("Check connections or run with --mock flag.");
            System.exit(2);
        } catch (Exception e) {
            LOG.error("Unexpected error in project '{}'", project.getProjectId(), e);
            System.err.printf("Unexpected error: %s%n", e.getMessage());
            System.exit(2);
        } finally {
            // Явний shutdown після run() — ідемпотентний виклик, не зашкодить
            try {
                project.shutdown();
            } catch (Exception ignore) {
                // Помилки при shutdown не повинні маскувати оригінальний результат
            }
        }
    }
}
