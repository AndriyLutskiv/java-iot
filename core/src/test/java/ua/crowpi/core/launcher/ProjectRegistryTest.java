package ua.crowpi.core.launcher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ua.crowpi.core.CrowPiProject;
import ua.crowpi.core.exception.HardwareException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link ProjectRegistry}.
 *
 * <p>Tests use manually registered stub projects rather than ServiceLoader discovery,
 * so they run on any machine without a fat JAR and without real project implementations.</p>
 */
class ProjectRegistryTest {

    // Реєстр, що тестується — кожен тест отримує чистий екземпляр
    private ProjectRegistry registry;

    @BeforeEach
    void setUp() {
        // Конструктор за замовчуванням запускає ServiceLoader; у тестовому classpath
        // жодних реальних CrowPiProject немає, тому реєстр порожній після ініціалізації
        registry = new ProjectRegistry();
    }

    @Test
    void testRegister_projectAppearsInList() {
        CrowPiProject stub = stubProject("alpha", "Alpha Project");

        registry.register(stub);

        assertEquals(1, registry.size());
        assertEquals("Alpha Project", registry.getAllProjects().get(0).getName());
    }

    @Test
    void testRegister_multipleProjectsSortedById() {
        // Реєструємо у зворотному порядку — після register() список має бути відсортований
        registry.register(stubProject("p03", "Three"));
        registry.register(stubProject("p01", "One"));
        registry.register(stubProject("p02", "Two"));

        assertEquals("p01", registry.getAllProjects().get(0).getProjectId());
        assertEquals("p02", registry.getAllProjects().get(1).getProjectId());
        assertEquals("p03", registry.getAllProjects().get(2).getProjectId());
    }

    @Test
    void testFindById_existingId_returnsProject() {
        registry.register(stubProject("rfid", "RFID Door Lock"));

        Optional<CrowPiProject> result = registry.findById("rfid");

        assertTrue(result.isPresent());
        assertEquals("RFID Door Lock", result.get().getName());
    }

    @Test
    void testFindById_caseInsensitive() {
        registry.register(stubProject("thermometer", "Thermometer Monitor"));

        // ID у верхньому регістрі повинен знаходити проект з нижнім
        Optional<CrowPiProject> result = registry.findById("THERMOMETER");

        assertTrue(result.isPresent());
    }

    @Test
    void testFindById_unknownId_returnsEmpty() {
        registry.register(stubProject("counter", "Counter"));

        Optional<CrowPiProject> result = registry.findById("nonexistent");

        assertFalse(result.isPresent());
    }

    @Test
    void testFindById_nullId_returnsEmpty() {
        Optional<CrowPiProject> result = registry.findById(null);

        assertFalse(result.isPresent());
    }

    @Test
    void testRegister_nullProject_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> registry.register(null));
    }

    @Test
    void testSize_emptyRegistry_returnsZero() {
        // Реєстр без жодного зареєстрованого проекту
        assertEquals(0, registry.size());
    }

    @Test
    void testGetAllProjects_returnsUnmodifiableView() {
        registry.register(stubProject("p01", "One"));

        // Спроба змінити повернений список повинна кинути UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class,
                () -> registry.getAllProjects().add(stubProject("p99", "Hack")));
    }

    // -------------------------------------------------------------------------
    // Допоміжний фабричний метод
    // -------------------------------------------------------------------------

    /**
     * Creates a minimal stub CrowPiProject for use in tests.
     *
     * @param id   project id
     * @param name display name
     * @return anonymous stub implementation
     */
    private static CrowPiProject stubProject(String id, String name) {
        return new CrowPiProject() {
            @Override public String getName()        { return name; }
            @Override public String getProjectId()   { return id; }
            @Override public String getDescription() { return "stub description"; }
            @Override public void run(boolean mockMode) throws HardwareException { /* stub */ }
            @Override public void shutdown() { /* stub */ }
        };
    }
}
