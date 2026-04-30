package ua.crowpi.projects.p09;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.crowpi.core.exception.DatabaseException;
import ua.crowpi.core.exception.HardwareException;
import ua.crowpi.core.hardware.I2cFacade;
import ua.crowpi.core.mock.MockSensorReader;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for p09 Smart Home Assistant.
 *
 * <p>Database tests use an in-memory SQLite connection ({@code jdbc:sqlite::memory:}) so
 * they run on any machine without touching the filesystem. Mockito is used for the
 * hardware controller tests.</p>
 */
@ExtendWith(MockitoExtension.class)
class SmartHomeTest {

    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // Shared in-memory connection for all DB tests in one test run
    private Connection conn;
    private DatabaseManager db;
    private ProfileRepository profileRepo;
    private DeviceEventRepository eventRepo;

    @Mock
    private I2cFacade mockLcd;

    @BeforeEach
    void setUp() throws SQLException, DatabaseException {
        // Відкриваємо in-memory SQLite — кожен @BeforeEach дає чисту БД
        conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        db = new DatabaseManager(conn);
        db.createTables();
        profileRepo = new ProfileRepository(conn);
        eventRepo = new DeviceEventRepository(conn);
    }

    // =========================================================================
    // Test 1: ProfileRepository — save and find
    // =========================================================================

    @Test
    void testProfileRepository_saveAndFind() throws DatabaseException {
        // Arrange: новий профіль без id
        Profile profile = new Profile();
        profile.setName("TEST_PROFILE");
        profile.setTempThreshold(24.5);
        profile.setLightOn(true);
        profile.setFanOn(false);
        profile.setCreatedAt("2024-01-01T10:00:00");

        // Act: зберегти
        Profile saved = profileRepo.save(profile);

        // Assert: id присвоєний, знайдений по імені
        assertTrue(saved.getId() > 0,
                "save() must set a generated id > 0");

        Optional<Profile> found = profileRepo.findByName("TEST_PROFILE");
        assertTrue(found.isPresent(), "findByName should return the saved profile");
        assertEquals("TEST_PROFILE", found.get().getName());
        assertEquals(24.5, found.get().getTempThreshold(), 0.001);
        assertTrue(found.get().isLightOn(), "lightOn should be true");
        assertFalse(found.get().isFanOn(), "fanOn should be false");
    }

    // =========================================================================
    // Test 2: ProfileRepository — findByName returns empty for missing name
    // =========================================================================

    @Test
    void testProfileRepository_findByName_notFound() throws DatabaseException {
        // Act: шукаємо неіснуючий профіль
        Optional<Profile> result = profileRepo.findByName("NONEXISTENT");

        // Assert: Optional порожній
        assertFalse(result.isPresent(),
                "findByName must return empty Optional for unknown profile name");
    }

    // =========================================================================
    // Test 3: DeviceEventRepository — insert and findRecent
    // =========================================================================

    @Test
    void testDeviceEventRepository_insertAndFindRecent() throws DatabaseException {
        // Вставляємо 3 події в хронологічному порядку
        String ts = LocalDateTime.now().format(TS_FMT);
        eventRepo.insert(DeviceEvent.of("LIGHT_ON",  "LIGHT", "OFF", "ON",  null, ts));
        eventRepo.insert(DeviceEvent.of("FAN_ON",    "FAN",   "OFF", "ON",  null, ts));
        eventRepo.insert(DeviceEvent.of("LIGHT_OFF", "LIGHT", "ON",  "OFF", null, ts));

        // findRecent(10) повертає до 10 подій, найновіша перша
        List<DeviceEvent> recent = eventRepo.findRecent(10);

        assertEquals(3, recent.size(), "findRecent should return all 3 inserted events");
        // Сортування за id DESC → остання вставлена йде першою
        assertEquals("LIGHT_OFF", recent.get(0).getEventType(),
                "Most recent event should be first");
    }

    // =========================================================================
    // Test 4: DeviceEventRepository — countByType
    // =========================================================================

    @Test
    void testDeviceEventRepository_countByType() throws DatabaseException {
        String ts = LocalDateTime.now().format(TS_FMT);
        // Вставляємо кілька подій різних типів
        eventRepo.insert(DeviceEvent.of("FAN_ON",  "FAN",   "OFF", "ON",  null, ts));
        eventRepo.insert(DeviceEvent.of("FAN_OFF", "FAN",   "ON",  "OFF", null, ts));
        eventRepo.insert(DeviceEvent.of("FAN_ON",  "FAN",   "OFF", "ON",  null, ts));
        eventRepo.insert(DeviceEvent.of("LIGHT_ON","LIGHT", "OFF", "ON",  null, ts));

        // FAN_ON повинно зустрічатись двічі
        int fanOnCount = eventRepo.countByType("FAN_ON");
        assertEquals(2, fanOnCount,
                "countByType('FAN_ON') should return 2");

        // LIGHT_OFF ще не вставлявся
        int lightOffCount = eventRepo.countByType("LIGHT_OFF");
        assertEquals(0, lightOffCount,
                "countByType for non-existent type should return 0");
    }

    // =========================================================================
    // Test 5: DatabaseManager — createTables() is idempotent
    // =========================================================================

    @Test
    void testDatabaseManager_createTablesIdempotent() throws DatabaseException {
        // Перший виклик — вже відбувся у setUp().
        // Другий виклик не повинен кидати виняток.
        db.createTables(); // другий виклик
        db.createTables(); // третій виклик для надійності

        // Якщо дійшли сюди — тест пройдений
        // Додаткова перевірка: таблиці реально існують і приймають запити
        Profile p = new Profile();
        p.setName("IDEMPOTENT_CHECK");
        p.setTempThreshold(25.0);
        p.setLightOn(false);
        p.setFanOn(false);
        p.setCreatedAt("2024-01-01T00:00:00");

        Profile saved = profileRepo.save(p);
        assertTrue(saved.getId() > 0, "Profiles table must be accessible after multiple createTables()");
    }

    // =========================================================================
    // Test 6: MenuNavigator + LightMenuItem — SELECT calls LightController.toggle()
    // =========================================================================

    @Test
    void testMenuNavigator_lightToggle_callsController() {
        // Arrange: мокуємо LightController і DeviceEventRepository
        LightController mockLight = mock(LightController.class);
        DeviceEventRepository mockEventRepo = mock(DeviceEventRepository.class);
        DeviceState state = new DeviceState(false, false, "DAY");

        // Створюємо MenuNavigator з одним LightMenuItem
        MenuNavigator nav = new MenuNavigator(mockLcd);
        nav.addItem(new LightMenuItem(mockLight, state, mockEventRepo, mockLcd));
        nav.setCurrentIndex(0);

        // Act: натискаємо SELECT
        nav.step(NavigationInput.NavAction.SELECT);

        // Assert: toggle() викликано на контролері
        verify(mockLight).toggle();
    }

    // =========================================================================
    // Test 7: AutoTempControl — fan turns on when temperature exceeds threshold
    // =========================================================================

    @Test
    void testAutoTempControl_fanOnWhenHot() throws HardwareException {
        // Arrange: температура 30°C > порогу профілю 26°C
        DhtReading hotReading = new DhtReading(30.0, 60.0);
        MockSensorReader<DhtReading> sensor = new MockSensorReader<>(hotReading);

        FanController mockFan = mock(FanController.class);
        DeviceState state = new DeviceState(false, false, "DAY");
        Profile profile = new Profile(1, "DAY", 26.0, false, false, "2024-01-01T00:00:00");

        // mockEventRepo.insert() кидає checked DatabaseException,
        // але Mockito за замовчуванням не кидає виняток для mock-методів
        DeviceEventRepository mockEventRepo = mock(DeviceEventRepository.class);

        AutoTempControl atc = new AutoTempControl(sensor, mockFan, mockEventRepo, state, profile);

        // Act: викликаємо один цикл перевірки без запуску потоку
        atc.checkTemperature();

        // Assert: вентилятор увімкнено
        verify(mockFan).turnOn();
    }

    // =========================================================================
    // Test 8: seedDefaultProfiles inserts DAY/NIGHT/VACATION
    // =========================================================================

    @Test
    void testDatabaseManager_seedDefaultProfiles_insertsMissingProfiles()
            throws DatabaseException {
        // Seed на чистій БД
        db.seedDefaultProfiles(profileRepo);

        List<Profile> all = profileRepo.findAll();
        assertEquals(3, all.size(), "Seed must create 3 default profiles");

        Optional<Profile> day = profileRepo.findByName("DAY");
        assertTrue(day.isPresent());
        assertEquals(26.0, day.get().getTempThreshold(), 0.001);

        Optional<Profile> night = profileRepo.findByName("NIGHT");
        assertTrue(night.isPresent());
        assertEquals(22.0, night.get().getTempThreshold(), 0.001);

        Optional<Profile> vacation = profileRepo.findByName("VACATION");
        assertTrue(vacation.isPresent());
        assertEquals(30.0, vacation.get().getTempThreshold(), 0.001);
    }

    // =========================================================================
    // Test 9: ProfileRepository.update — changes are persisted
    // =========================================================================

    @Test
    void testProfileRepository_update_persistsChanges() throws DatabaseException {
        // Arrange: зберегти профіль
        Profile profile = new Profile();
        profile.setName("UPDATE_TEST");
        profile.setTempThreshold(20.0);
        profile.setLightOn(false);
        profile.setFanOn(false);
        profile.setCreatedAt("2024-01-01T00:00:00");
        profileRepo.save(profile);

        // Act: оновити поріг температури
        profile.setTempThreshold(29.5);
        profile.setLightOn(true);
        profileRepo.update(profile);

        // Assert: нові значення відображені у БД
        Optional<Profile> loaded = profileRepo.findByName("UPDATE_TEST");
        assertTrue(loaded.isPresent());
        assertEquals(29.5, loaded.get().getTempThreshold(), 0.001);
        assertTrue(loaded.get().isLightOn());
    }

    // =========================================================================
    // Test 10: DeviceEventRepository.findRecent respects the limit
    // =========================================================================

    @Test
    void testDeviceEventRepository_findRecent_respectsLimit() throws DatabaseException {
        String ts = LocalDateTime.now().format(TS_FMT);
        // Вставляємо 7 подій
        for (int i = 0; i < 7; i++) {
            eventRepo.insert(DeviceEvent.of("LIGHT_ON", "LIGHT", "OFF", "ON", null, ts));
        }

        // findRecent(3) повинен повернути рівно 3
        List<DeviceEvent> recent = eventRepo.findRecent(3);
        assertEquals(3, recent.size(),
                "findRecent(3) must return at most 3 events even if more exist");
    }

    // =========================================================================
    // Test 11: DeviceEvent nullable oldState and profileId
    // =========================================================================

    @Test
    void testDeviceEvent_nullableFields_roundtrip() throws DatabaseException {
        // old_state і profile_id можуть бути null
        DeviceEvent event = DeviceEvent.of("SYSTEM_START", "SYSTEM",
                null, "RUNNING", null,
                LocalDateTime.now().format(TS_FMT));
        eventRepo.insert(event);

        List<DeviceEvent> recent = eventRepo.findRecent(1);
        assertEquals(1, recent.size());
        DeviceEvent loaded = recent.get(0);
        assertNotNull(loaded);
        // null-поля повинні залишитись null після round-trip через SQLite
        assertTrue(loaded.getOldState() == null || loaded.getOldState().isEmpty(),
                "oldState should be null");
        assertTrue(loaded.getProfileId() == null,
                "profileId should be null");
    }
}
