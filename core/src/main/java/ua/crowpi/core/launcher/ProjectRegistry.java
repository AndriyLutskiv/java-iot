package ua.crowpi.core.launcher;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ua.crowpi.core.CrowPiProject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Central registry of all {@link CrowPiProject} implementations available in the fat JAR.
 *
 * <p>Discovery uses the standard Java {@link ServiceLoader} mechanism. Each project module
 * declares its implementation by placing the fully-qualified class name in:</p>
 * <pre>{@code META-INF/services/ua.crowpi.core.CrowPiProject}</pre>
 * <p>When {@code ./gradlew shadowJar} merges all modules, all service declarations are
 * combined, so the registry automatically discovers all 11 projects at runtime.</p>
 *
 * <p>Projects can also be registered programmatically via {@link #register(CrowPiProject)},
 * which is convenient for integration tests that inject a mock project.</p>
 *
 * <p>After discovery the list is sorted by {@link CrowPiProject#getProjectId()} so the
 * menu always shows projects in a consistent order (p01 → p11).</p>
 */
public class ProjectRegistry {

    private static final Logger LOG = LogManager.getLogger(ProjectRegistry.class);

    // Змінюваний список — після конструктора сортується і більше не змінюється
    // (якщо не викликати register() вручну)
    private final List<CrowPiProject> projects = new ArrayList<>();

    /**
     * Creates a ProjectRegistry and immediately discovers all available projects
     * via {@link ServiceLoader}.
     *
     * <p>If ServiceLoader throws any exception for a particular implementation
     * (e.g. the class cannot be instantiated), that implementation is skipped
     * and a warning is logged — the other projects remain available.</p>
     */
    public ProjectRegistry() {
        loadFromServiceLoader();
        // Сортуємо за projectId щоб порядок у меню завжди відповідав p01…p11
        projects.sort((a, b) -> a.getProjectId().compareToIgnoreCase(b.getProjectId()));
        LOG.info("ProjectRegistry initialised: {} project(s) discovered", projects.size());
    }

    /**
     * Returns an unmodifiable view of all registered projects.
     *
     * @return read-only list ordered by {@link CrowPiProject#getProjectId()}
     */
    public List<CrowPiProject> getAllProjects() {
        return Collections.unmodifiableList(projects);
    }

    /**
     * Finds a project by its short identifier (case-insensitive).
     *
     * @param projectId the project id to search for, e.g. {@code "rfid"} or {@code "p08"}
     * @return an {@link Optional} containing the project, or empty if not found
     */
    public Optional<CrowPiProject> findById(String projectId) {
        if (projectId == null || projectId.isEmpty()) {
            return Optional.empty();
        }
        // Пошук без урахування регістру — щоб --project=RFID і --project=rfid
        // працювали однаково
        return projects.stream()
                .filter(p -> p.getProjectId().equalsIgnoreCase(projectId))
                .findFirst();
    }

    /**
     * Manually registers a project into the registry.
     *
     * <p>Intended for integration tests or future dynamic loading scenarios.
     * Duplicate project IDs are allowed (last-write wins in menu display,
     * {@link #findById} returns the first match).</p>
     *
     * @param project the project to add; must not be {@code null}
     */
    public void register(CrowPiProject project) {
        if (project == null) {
            throw new IllegalArgumentException("Cannot register a null project");
        }
        projects.add(project);
        // Пересортовуємо після ручної реєстрації, щоб порядок залишався коректним
        projects.sort((a, b) -> a.getProjectId().compareToIgnoreCase(b.getProjectId()));
        LOG.debug("Manually registered project: {} ({})", project.getName(), project.getProjectId());
    }

    /**
     * Returns the total number of registered projects.
     *
     * @return project count
     */
    public int size() {
        return projects.size();
    }

    // -------------------------------------------------------------------------
    // Внутрішня логіка ServiceLoader
    // -------------------------------------------------------------------------

    /**
     * Iterates over all {@link CrowPiProject} implementations found by the ServiceLoader.
     * Failures are logged as warnings but do not abort the loading of other implementations.
     */
    private void loadFromServiceLoader() {
        // ServiceLoader.load() шукає META-INF/services/ua.crowpi.core.CrowPiProject
        // у всіх JAR-файлах на classpath — включно зі злитими модулями у shadowJar
        ServiceLoader<CrowPiProject> loader = ServiceLoader.load(CrowPiProject.class);

        // Використовуємо Iterator замість enhanced for-loop або stream(),
        // щоб перехоплювати ServiceConfigurationError по одній реалізації за раз.
        // Якби ітерація впала на першому ж рядку enhanced for — решта проектів
        // не завантажились би зовсім.
        Iterator<CrowPiProject> it = loader.iterator();
        while (it.hasNext()) {
            try {
                CrowPiProject project = it.next();
                projects.add(project);
                LOG.debug("Discovered project via ServiceLoader: {} (id={})",
                        project.getName(), project.getProjectId());
            } catch (Exception e) {
                // Не зупиняємось на помилці — інші проекти можуть бути робочими
                LOG.warn("Failed to load a CrowPiProject implementation: {}", e.getMessage());
            }
        }
    }
}
