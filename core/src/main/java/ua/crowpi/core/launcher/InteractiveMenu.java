package ua.crowpi.core.launcher;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ua.crowpi.core.CrowPiProject;
import ua.crowpi.core.exception.HardwareException;
import ua.crowpi.core.util.GracefulShutdown;

import java.util.List;
import java.util.Scanner;

/**
 * Renders an interactive console menu for selecting and launching CrowPi projects.
 *
 * <p>The menu displays all projects registered in the {@link ProjectRegistry} as
 * a numbered list inside a box-drawing border. The student enters a number to
 * launch the corresponding project, or {@code 0} to exit.</p>
 *
 * <p>Example output when all 11 projects are registered:</p>
 * <pre>
 * ╔══════════════════════════════════════════════╗
 * ║      CrowPi Java Educational Suite          ║
 * ║   Raspberry Pi 3B  ·  Java 11  ·  Pi4J 1.4  ║
 * ╠══════════════════════════════════════════════╣
 * ║  1.  Thermometer &amp; Humidity Monitor         ║
 * ║  2.  Motion Event Counter                   ║
 * ...
 * ║  0.  Exit                                   ║
 * ╚══════════════════════════════════════════════╝
 * Select [0-11]:
 * </pre>
 *
 * <p>The menu loops continuously until the user selects {@code 0} or an
 * {@link InterruptedException} is raised (e.g. Ctrl+C).</p>
 */
public class InteractiveMenu {

    private static final Logger LOG = LogManager.getLogger(InteractiveMenu.class);

    /** Total inner width of the menu border (characters between ║ and ║). */
    private static final int BOX_WIDTH = 46;

    private final ProjectRegistry registry;

    /**
     * Creates an InteractiveMenu backed by the given project registry.
     *
     * @param registry source of available projects; must not be {@code null}
     */
    public InteractiveMenu(ProjectRegistry registry) {
        if (registry == null) {
            throw new IllegalArgumentException("ProjectRegistry must not be null");
        }
        this.registry = registry;
    }

    /**
     * Displays the menu in a loop and runs the project chosen by the user.
     *
     * <p>Each selected project is wrapped in a {@link GracefulShutdown} hook before
     * {@link CrowPiProject#run(boolean)} is called. The hook is registered fresh for
     * every project invocation so a new project started after returning from a previous
     * one also gets cleanup on exit.</p>
     *
     * @param mockMode {@code true} to run with mock hardware; {@code false} for real GPIO
     */
    public void show(boolean mockMode) {
        // Scanner читає зі System.in — не закриваємо його, щоб не закрити stdin
        Scanner scanner = new Scanner(System.in);
        List<CrowPiProject> projects = registry.getAllProjects();

        while (true) {
            printBanner(projects, mockMode);

            int maxChoice = projects.size();
            System.out.printf("Select [0-%d]: ", maxChoice);

            // Зчитуємо рядок цілком, щоб не залишати '\n' у буфері після nextInt()
            String line = scanner.nextLine().trim();
            int choice;
            try {
                choice = Integer.parseInt(line);
            } catch (NumberFormatException e) {
                // Некоректне введення — просто показуємо меню знову без повідомлення про помилку
                System.out.println("  Please enter a number.");
                continue;
            }

            if (choice == 0) {
                System.out.println("  Goodbye!");
                LOG.info("User selected Exit from interactive menu");
                break;
            }

            if (choice < 1 || choice > maxChoice) {
                System.out.printf("  Invalid choice. Please enter 0 to %d.%n", maxChoice);
                continue;
            }

            // Вибираємо проект (список 0-indexed, меню 1-indexed)
            CrowPiProject selected = projects.get(choice - 1);
            LOG.info("User selected project: {} ({})", selected.getName(), selected.getProjectId());

            // Реєструємо shutdown hook щоб GPIO звільнились навіть після Ctrl+C
            GracefulShutdown.register(selected);

            System.out.printf("%n  Starting: %s%n", selected.getName());
            if (mockMode) {
                System.out.println("  [MOCK MODE — no real hardware required]");
            }
            System.out.println("  Press Ctrl+C to stop and return to menu.");
            System.out.println();

            try {
                selected.run(mockMode);
            } catch (HardwareException e) {
                LOG.error("Hardware error in project '{}': {}", selected.getProjectId(), e.getMessage(), e);
                System.out.printf("  ERROR: %s%n", e.getMessage());
                System.out.println("  Check hardware connections or run with --mock flag.");
            } catch (Exception e) {
                LOG.error("Unexpected error in project '{}'", selected.getProjectId(), e);
                System.out.printf("  Unexpected error: %s%n", e.getMessage());
            } finally {
                // Явний виклик shutdown після повернення з run() — страховка якщо hook не спрацював
                try {
                    selected.shutdown();
                } catch (Exception ignore) {
                    // shutdown() має бути idempotent — помилка при повторному виклику ігнорується
                }
            }

            System.out.println();
        }
    }

    // -------------------------------------------------------------------------
    // Відображення банера
    // -------------------------------------------------------------------------

    /**
     * Prints the full menu banner with project list to stdout.
     *
     * @param projects  ordered list of registered projects
     * @param mockMode  if {@code true}, appends a "MOCK" indicator to the header
     */
    private void printBanner(List<CrowPiProject> projects, boolean mockMode) {
        System.out.println();
        // Верхня рамка
        System.out.println("\u2554" + repeat('\u2550', BOX_WIDTH) + "\u2557");

        // Заголовок
        printCentered("CrowPi Java Educational Suite");
        String subtitle = mockMode
                ? "Raspberry Pi 3B  \u00b7  Java 11  \u00b7  MOCK MODE"
                : "Raspberry Pi 3B  \u00b7  Java 11  \u00b7  Pi4J 1.4";
        printCentered(subtitle);

        // Роздільник між заголовком і списком
        System.out.println("\u2560" + repeat('\u2550', BOX_WIDTH) + "\u2563");

        if (projects.isEmpty()) {
            // Жодного проекту не знайдено — підказка розробнику
            printLeft("  (no projects registered — add project modules)");
        } else {
            for (int i = 0; i < projects.size(); i++) {
                CrowPiProject p = projects.get(i);
                // Форматуємо: "  1.  Thermometer & Humidity Monitor"
                String entry = String.format("  %2d.  %s", i + 1, p.getName());
                printLeft(entry);
            }
        }

        // Пункт виходу
        printLeft("   0.  Exit");

        // Нижня рамка
        System.out.println("\u255a" + repeat('\u2550', BOX_WIDTH) + "\u255d");
    }

    /**
     * Prints a line centred inside the box border.
     *
     * @param text the text to centre
     */
    private void printCentered(String text) {
        int padding = (BOX_WIDTH - text.length()) / 2;
        // Якщо текст довший за ширину — друкуємо без відступу
        int leftPad = Math.max(padding, 0);
        int rightPad = Math.max(BOX_WIDTH - text.length() - leftPad, 0);
        System.out.println("\u2551" + repeat(' ', leftPad) + text + repeat(' ', rightPad) + "\u2551");
    }

    /**
     * Prints a left-aligned line inside the box border, padding to full width.
     *
     * @param text the text to print flush-left
     */
    private void printLeft(String text) {
        // Обрізаємо якщо текст задовгий, щоб не зламати рамку
        String clamped = text.length() > BOX_WIDTH ? text.substring(0, BOX_WIDTH) : text;
        int rightPad = BOX_WIDTH - clamped.length();
        System.out.println("\u2551" + clamped + repeat(' ', rightPad) + "\u2551");
    }

    /**
     * Returns a string consisting of {@code count} repetitions of {@code ch}.
     *
     * @param ch    character to repeat
     * @param count number of repetitions (must be ≥ 0)
     * @return repeated character string
     */
    private static String repeat(char ch, int count) {
        if (count <= 0) return "";
        // StringBuilder ефективніший за String.valueOf(ch).repeat() для Java 11
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(ch);
        }
        return sb.toString();
    }
}
