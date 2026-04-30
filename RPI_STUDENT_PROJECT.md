# Claude Code Task: CrowPi Java Educational Suite

## АБСОЛЮТНІ ПРАВИЛА — ПРОЧИТАТИ ПЕРЕД УСІМ

1. Кожен клас — ПОВНА реалізація. Жодного `// TODO`, жодних заглушок.
   Кожен метод має реальне тіло з логікою.
2. Усі `public` / `protected` методи → Javadoc англійською.
3. Усі нетривіальні блоки логіки → inline-коментарі українською,
   що пояснюють ЧОМУ саме так, а не просто що робить рядок.
   Приклад правильного коментаря:
````java
   // Використовуємо AtomicLong замість long, бо час реакції записується
   // одночасно з GPIO-listener-потоку та з основного ігрового циклу.
   // Без атомарності можливий race condition і некоректний результат.
   private final AtomicLong player1Time = new AtomicLong(-1);
````
4. Кожен файл — повний список імпортів зверху.
5. Усі `.puml` файли — валідний PlantUML синтаксис.
6. Проект має компілюватись командою `./gradlew shadowJar` без помилок.
7. Java version: **Java 11 LTS** (OpenJDK 11 ARM). Не використовувати
   API, що з'явились у Java 14+ (records, sealed classes тощо).
   Замість records — звичайні POJO з конструктором, getters, toString.

---

## ЦІЛЬОВЕ ЗАЛІЗО

- Raspberry Pi 3B (ARMv7 32-bit, Raspbian Buster / Raspberry Pi OS Legacy)
- CrowPi kit (стара версія, 7-дюймовий дисплей)
- Вбудовані компоненти CrowPi (підключені через PCB плату):
  - DHT11 (температура + вологість)
  - LCD 16x2 (I2C, addr 0x27)
  - RGB LED (3 GPIO піни)
  - Buzzer (пасивний, PWM)
  - PIR sensor (GPIO)
  - HC-SR04 (ультразвук: trigger + echo GPIO)
  - Keypad 4x4 (8 GPIO пінів)
  - IR receiver (GPIO + lirc)
  - RFID RC-522 (SPI)
  - Relay x2 (GPIO)
  - 7-segment display (GPIO або I2C)
  - Servo motor (PWM GPIO)
  - Soil moisture sensor (аналоговий через MCP3008 SPI ADC)
  - Sound sensor (GPIO digital out)
  - Tilt sensor (GPIO)
  - Buttons x4 (GPIO)

---

## СТРУКТУРА РЕПОЗИТОРІЮ
````
crowpi-java-suite/
├── build.gradle                   # Gradle (основна система збірки)
├── settings.gradle                # Multi-module settings
├── gradlew
├── gradlew.bat
├── gradle/wrapper/gradle-wrapper.properties
├── pom.xml                        # Maven (для навчального порівняння)
├── README.md                      # Кореневий README
├── docs/
│   ├── BUILD_SYSTEMS_COMPARISON.md  # Таблиця Gradle vs Maven
│   └── diagrams/                  # Всі .puml файли зібрані разом
│       ├── architecture.puml      # Загальна архітектура модулів
│       └── ... (копії з модулів)
├── core/                          # Спільна інфраструктура
│   ├── build.gradle
│   └── src/
│       ├── main/java/ua/crowpi/core/
│       │   ├── CrowPiProject.java          # Головний інтерфейс проекту
│       │   ├── launcher/
│       │   │   ├── Launcher.java           # main() клас
│       │   │   ├── ProjectRegistry.java    # Реєстр усіх проектів
│       │   │   └── InteractiveMenu.java    # Консольне меню
│       │   ├── hardware/
│       │   │   ├── GpioFacade.java
│       │   │   ├── I2cFacade.java
│       │   │   ├── PwmFacade.java
│       │   │   ├── SpiFacade.java
│       │   │   └── SensorReader.java       # Generic interface
│       │   ├── mock/
│       │   │   ├── MockGpioFacade.java
│       │   │   ├── MockI2cFacade.java
│       │   │   ├── MockPwmFacade.java
│       │   │   ├── MockSpiFacade.java
│       │   │   └── MockSensorReader.java
│       │   ├── exception/
│       │   │   ├── HardwareException.java
│       │   │   ├── DatabaseException.java
│       │   │   └── ConfigException.java
│       │   └── util/
│       │       ├── GracefulShutdown.java   # ShutdownHook helper
│       │       └── PropertiesLoader.java   # .properties читалка
│       └── test/java/ua/crowpi/core/
│           ├── launcher/
│           │   └── ProjectRegistryTest.java
│           └── util/
│               └── PropertiesLoaderTest.java
└── projects/
    ├── p01-thermometer/
    ├── p02-counter/
    ├── p03-music-box/
    ├── p04-alarm/
    ├── p05-radar/
    ├── p06-reaction/
    ├── p07-greenhouse/
    ├── p08-rfid/
    ├── p09-smart-home/            # <-- SQLite тут
    ├── p10-meteostation/
    └── p11-lcd-game/
````

Кожен модуль `projects/pXX-name/` має структуру:
````
pXX-name/
├── build.gradle
├── README.md
├── docs/uml/
│   ├── classes.puml               # Обов'язково якщо > 3 класів
│   └── er.puml                    # Тільки p09-smart-home
└── src/
    ├── main/java/ua/crowpi/projects/pXX/
    │   └── ... (всі класи проекту)
    └── test/java/ua/crowpi/projects/pXX/
        └── ... (JUnit тести)
````

---

## СИСТЕМА ЗБІРКИ

### Gradle (основна)

`settings.gradle`:
````groovy
rootProject.name = 'crowpi-java-suite'
include 'core'
include 'projects:p01-thermometer'
include 'projects:p02-counter'
include 'projects:p03-music-box'
include 'projects:p04-alarm'
include 'projects:p05-radar'
include 'projects:p06-reaction'
include 'projects:p07-greenhouse'
include 'projects:p08-rfid'
include 'projects:p09-smart-home'
include 'projects:p10-meteostation'
include 'projects:p11-lcd-game'
````

Кореневий `build.gradle`:
````groovy
plugins {
    id 'java'
    id 'com.github.johnrengelman.shadow' version '7.1.2'  // v7 для Java 11
}

allprojects {
    group = 'ua.crowpi'
    version = '1.0.0'
    
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply plugin: 'java'
    
    java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    
    dependencies {
        // Pi4J v1.4 — єдина версія що підтримує RPi 3 GPIO
        implementation 'com.pi4j:pi4j-core:1.4'
        
        // Apache Commons CLI — парсинг аргументів командного рядка
        implementation 'commons-cli:commons-cli:1.5.0'
        
        // Log4j 2 — логування
        implementation 'org.apache.logging.log4j:log4j-api:2.20.0'
        implementation 'org.apache.logging.log4j:log4j-core:2.20.0'
        
        // Apache Commons Lang — утиліти для рядків, рефлексії
        implementation 'org.apache.commons:commons-lang3:3.12.0'
        
        // Apache Commons CSV — запис CSV файлів (p07, p10)
        implementation 'org.apache.commons:commons-csv:1.10.0'
        
        // Jackson — JSON серіалізація/десеріалізація (p09, p11)
        implementation 'com.fasterxml.jackson.core:jackson-databind:2.14.2'
        
        // SQLite JDBC (p09-smart-home)
        implementation 'org.xerial:sqlite-jdbc:3.42.0.0'
        
        // JUnit 5
        testImplementation 'org.junit.jupiter:junit-jupiter-api:5.9.3'
        testImplementation 'org.junit.jupiter:junit-jupiter-params:5.9.3'
        testRuntimeOnly 'org.junit.jupiter:junit-jupiter-engine:5.9.3'
        
        // Mockito — мокування залежностей у тестах
        testImplementation 'org.mockito:mockito-core:4.11.0'
        testImplementation 'org.mockito:mockito-junit-jupiter:4.11.0'
    }
    
    test {
        useJUnitPlatform()
    }
}

// Fat JAR збирається тільки в root
shadowJar {
    archiveBaseName = 'crowpi-suite'
    archiveClassifier = ''
    archiveVersion = '1.0.0'
    manifest {
        attributes 'Main-Class': 'ua.crowpi.core.launcher.Launcher'
    }
    // Об'єднуємо всі subproject outputs
    configurations = [project.configurations.runtimeClasspath]
}
````

### Maven (для навчального порівняння)

Кореневий `pom.xml` — multi-module Maven проект з `maven-shade-plugin`.
Додати коментарі XML у кожній секції що пояснюють відповідник у Gradle.
Приклад:
````xml
<!-- Gradle еквівалент: id 'com.github.johnrengelman.shadow' version '7.1.2' -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    ...
</plugin>
````

Файл `docs/BUILD_SYSTEMS_COMPARISON.md` — markdown таблиця:
| Концепція | Gradle | Maven |
|-----------|--------|-------|
| Версія Java | `sourceCompatibility = VERSION_11` | `<maven.compiler.source>11</maven.compiler.source>` |
| ... і т.д. для 15+ концепцій |

---

## ENTRY POINT — LAUNCHER

### `ua.crowpi.core.launcher.Launcher` (клас з `main()`)

CLI аргументи через Apache Commons CLI:
````
--project=<name>   Запустити конкретний проект напряму
--mock             Режим без реального GPIO (для розробки на ноутбуці)
--list             Вивести список проектів і вийти
--help             Довідка
````

Якщо `--project` не вказано — показати інтерактивне меню:
````
╔══════════════════════════════════════════════╗
║      CrowPi Java Educational Suite          ║
║   Raspberry Pi 3B  ·  Java 11  ·  Pi4J 1.4  ║
╠══════════════════════════════════════════════╣
║  1.  Thermometer & Humidity Monitor         ║
║  2.  Motion Event Counter                   ║
║  3.  IR Remote Music Box                    ║
║  4.  Alarm System with PIN Code             ║
║  5.  Ultrasonic Radar                       ║
║  6.  Reaction Speed Trainer (2 players)     ║
║  7.  Automatic Greenhouse                   ║
║  8.  RFID Door Lock                         ║
║  9.  Smart Home Assistant  [SQLite]         ║
║  10. Weather Station + Trend Forecast       ║
║  11. LCD Platform Game                      ║
║  0.  Exit                                   ║
╚══════════════════════════════════════════════╝
Select [0-11]:
````

### `CrowPiProject` — головний інтерфейс
````java
public interface CrowPiProject {
    String getName();           // "RFID Door Lock"
    String getProjectId();      // "rfid"
    String getDescription();    // одне речення
    void run(boolean mockMode) throws HardwareException;
    void shutdown();            // викликається ShutdownHook
}
````

### Апаратна абстракція

`core/hardware/` — інтерфейси:
````java
public interface GpioFacade {
    void setOutput(int pin, boolean high);
    boolean readInput(int pin);
    void addListener(int pin, PinChangeListener listener);
    void pwm(int pin, int frequencyHz, float dutyCycle);
    void close();
}

public interface I2cFacade {
    void writeByte(int deviceAddr, int register, byte value);
    byte readByte(int deviceAddr, int register);
    byte[] readBytes(int deviceAddr, int register, int length);
}

public interface SensorReader<T> {
    T read() throws HardwareException;
}
````

`core/mock/` — реалізації для `--mock`:
- `MockGpioFacade`: логує в stdout, для `readInput()` повертає симульовані значення
- `MockI2cFacade`: логує I2C команди, повертає dummy bytes
- `MockDht11Reader implements SensorReader<TemperatureReading>`:
  повертає синусоїдальні значення температури 18–28°C
- `MockPirSensor`: кидає події кожні 5 секунд через ScheduledExecutorService
- `MockRfidReader`: циклічно повертає 3 pre-defined UID рядки
- `MockLcd`: виводить в консоль з рамкою:
````
  ┌────────────────┐
  │TEMP: 23.4C     │
  │STATUS: COMFORT │
  └────────────────┘
````

---

## ПРОЕКТИ — ПОВНА СПЕЦИФІКАЦІЯ

### p01-thermometer — Thermometer & Humidity Monitor

**Компоненти CrowPi:** DHT11, LCD 16x2, RGB LED, Buzzer, Button x2

**Пакет:** `ua.crowpi.projects.p01`

**Класи:**
````
ThermometerProject    implements CrowPiProject
Dht11Reader           implements SensorReader<TemperatureReading>
TemperatureReading    // POJO: double temp, double humidity, LocalDateTime time
ThermalZone           // enum: COLD(<18), COMFORT(18-28), HOT(>28) з методом forTemp()
LcdDisplayHelper      // helper: форматування рядків для LCD 16x2
AlertConfig           // завантажується з thermometer.properties
````

**Логіка:**
- `ScheduledExecutorService` читає DHT11 кожні 2 секунди
- LCD рядок 1: `TEMP: 23.4C H:61%`
- LCD рядок 2: `STATUS: COMFORT  `
- RGB LED: синій=COLD, зелений=COMFORT, червоний=HOT
- Button1 = підвищити поріг тривоги на 1°C, Button2 = знизити
- Buzzer: 3 короткі сигнали коли температура перетинає поріг
- Лог у `logs/thermometer.csv` через Apache Commons CSV:
  `timestamp,temperature,humidity,zone,threshold`

**GPIO піни (CrowPi mapping):**
- DHT11: GPIO 4
- LCD SDA: I2C bus 1, addr 0x27
- RGB R: GPIO 22, G: GPIO 27, B: GPIO 17
- Buzzer: GPIO 18 (PWM)
- Button1: GPIO 11, Button2: GPIO 9

**Файл конфігурації** `src/main/resources/thermometer.properties`:
````properties
alert.threshold.celsius=28
poll.interval.seconds=2
log.file=logs/thermometer.csv
````

**Тести** `ThermometerTest`:
````java
// Тест 1: ThermalZone.forTemp() повертає правильну зону
@Test void testThermalZoneForTemp_cold() { ... }
@Test void testThermalZoneForTemp_comfort() { ... }
@Test void testThermalZoneForTemp_hot() { ... }
// Тест 2: AlertConfig завантажує threshold з properties
@Test void testAlertConfigLoadsThreshold() { ... }
// Тест 3: LcdDisplayHelper форматує рядок рівно 16 символів
@Test void testLcdLineExactly16Chars() { ... }
````

---

### p02-counter — Motion Event Counter

**Компоненти CrowPi:** PIR, 7-segment display, Button x2, Buzzer

**Пакет:** `ua.crowpi.projects.p02`

**Класи:**
````
CounterProject        implements CrowPiProject
PirListener           implements GpioPinListenerDigital (Pi4J)
SevenSegmentDisplay   // таблиця сегментів A-G для цифр 0-9
CounterMode           // enum: COUNT_UP, COUNTDOWN
EventRecord           // POJO: LocalDateTime time, CounterMode mode, int value
EventFileLogger       // append до events.log
````

**Логіка:**
- PIR спрацьовує → `volatile int counter` інкремент/декремент
- SevenSegmentDisplay показує поточне значення (0-9, потім 0 знову)
- Button1 = RESET (counter = 0)
- Button2 = перемикає CounterMode
- У режимі COUNTDOWN: Button1 встановлює стартове значення (5 натискань = 5)
- Buzzer: 1 клік на кожну подію PIR
- Append до `logs/events.log`: `2024-01-15T14:23:11 | COUNT_UP | value=7`

**Тести** `CounterTest`:
````java
@Test void testSevenSegmentMappingAllDigits()    // перевірити всі 10 цифр
@Test void testCounterModeToggle()               // COUNT_UP → COUNTDOWN → COUNT_UP
@Test void testEventRecordToString()             // формат лог-рядка
````

---

### p03-music-box — IR Remote Music Box

**Компоненти CrowPi:** IR receiver, Buzzer (PWM), LCD, RGB LED

**Пакет:** `ua.crowpi.projects.p03`

**Класи:**
````
MusicBoxProject       implements CrowPiProject
IrCodeDecoder         // NEC protocol: raw GPIO pulses → int keyCode
Note                  // POJO: int frequencyHz, int durationMs
Melody                // POJO: String name, List<Note> notes
MelodyPlayer          // відтворює Melody через PWM у окремому Thread
MelodyLibrary         // static final колекція 9 мелодій
PlaybackState         // enum: IDLE, PLAYING, PAUSED
LcdProgressRenderer   // малює ASCII прогрес-бар на LCD
````

**Мелодії в MelodyLibrary:**
1. Morse SOS (`...---...`)
2. C-major scale (до-ре-мі-фа-соль-ля-сі)
3. Jingle Bells (перші 8 нот)
4. Happy Birthday (перші 8 нот)
5. Ode to Joy (перші 8 нот)
6. Twinkle Twinkle (перші 8 нот)
7. Imperial March (перші 8 нот)
8. Mario theme (перші 8 нот)
9. Patrol signal (військовий сигнал увагу)

Кожна нота — конкретні значення Hz і ms (не заглушки!).

**Логіка:**
- IR кнопки 1-9 → відтворити відповідну мелодію
- IR кнопка 0 → зупинити
- LCD рядок 1: назва мелодії (≤16 символів)
- LCD рядок 2: `[████████░░]` прогрес (10 блоків)
- RGB LED: пульсує разом з нотами (ON на час ноти, OFF на паузу між)
- `MelodyPlayer` використовує окремий `Thread`, зупиняється через `volatile boolean stopped`

**Тести** `MusicBoxTest`:
````java
@Test void testMelodyLibraryHasNineMelodies()
@Test void testNoteValidFrequency()              // всі ноти 20-20000 Hz
@Test void testLcdProgressRenderer_full()        // 10/10 → "██████████"
@Test void testLcdProgressRenderer_half()        // 5/10  → "█████░░░░░"
@Test void testLcdProgressRenderer_zero()        // 0/10  → "░░░░░░░░░░"
````

---

### p04-alarm — Alarm System with PIN Code

**Компоненти CrowPi:** PIR, Keypad 4x4, Buzzer, RGB LED, LCD, Relay

**Пакет:** `ua.crowpi.projects.p04`

**Класи:**
````
AlarmProject          implements CrowPiProject
AlarmState            // enum: DISARMED, ARMED, TRIGGERED, LOCKED
AlarmEvent            // enum: PIR_TRIGGERED, CORRECT_PIN, WRONG_PIN, LOCK_EXPIRED
AlarmFsm              // Finite State Machine: Map<AlarmState, Map<AlarmEvent, AlarmState>>
KeypadReader          // 4x4 матрична клавіатура: GPIO polling → char
PinValidator          // SHA-256 hash порівняння, лічильник невдалих спроб
AlarmConfig           // завантажується з alarm.properties
AlarmLogger           // Log4j2: пише до logs/alarm.log
RelayController       // керує GPIO реле (канал 1)
SirenPlayer           // генерує сирену через PWM buzzer в окремому Thread
````

**Логіка FSM (таблиця переходів):**
````
DISARMED + CORRECT_PIN → ARMED
ARMED    + CORRECT_PIN → DISARMED
ARMED    + PIR_TRIGGERED → TRIGGERED
TRIGGERED + CORRECT_PIN → DISARMED
ANY_STATE + 3x WRONG_PIN → LOCKED
LOCKED + LOCK_EXPIRED (30s) → DISARMED
````

- LCD рядок 1: поточний стан (`STATUS: ARMED   `)
- LCD рядок 2: введення PIN як `PIN: ****       ` (маскування)
- Relay: клацає при ARMED↔DISARMED
- RGB: зелений=DISARMED, жовтий=ARMED, червоний=TRIGGERED, фіолетовий=LOCKED
- `SirenPlayer`: чергує 880Hz і 660Hz кожні 300ms поки TRIGGERED
- Всі події пишуться через Log4j2 у `logs/alarm.log`

**Файл конфігурації** `alarm.properties`:
````properties
# PIN зберігається як SHA-256 хеш, не відкритим текстом
pin.hash=ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94d
# хеш від "1234"
lockout.duration.seconds=30
log.file=logs/alarm.log
````

**Тести** `AlarmTest`:
````java
@Test void testFsmTransition_disarmedToArmed()
@Test void testFsmTransition_armedToTriggered()
@Test void testFsmTransition_triggeredToDisarmed()
@Test void testFsmInvalidTransition_doesNotChange()  // нелегальний перехід ігнорується
@Test void testPinValidator_correctPin()
@Test void testPinValidator_wrongPin()
@Test void testPinValidator_lockoutAfterThreeAttempts()
@Test void testPinValidator_sha256Hash()             // хеш від "1234" = очікуваний
// Mockito: мокувати AlarmFsm у тесті AlarmProject
@ExtendWith(MockitoExtension.class)
@Test void testAlarmProject_pirTriggerCallsFsmTransition() { ... }
````

---

### p05-radar — Ultrasonic Radar

**Компоненти CrowPi:** HC-SR04, Servo motor (PWM), LCD, RGB LED

**Пакет:** `ua.crowpi.projects.p05`

**Класи:**
````
RadarProject          implements CrowPiProject
UltrasonicSensor      // trigger pulse → echo timing → відстань в см
ServoController       // кут 0-180° → duty cycle PWM (50Hz, 1-2ms pulse)
RadarScan             // POJO: int angleDeg, double distanceCm, Instant time
ScanResult            // List<RadarScan> для одного повного проходу 0→180°
DistanceZone          // enum: CLEAR(>50cm), WARNING(20-50), DANGER(<20)
RadarDataExporter     // серіалізує ScanResult у JSON через Jackson
ConsoleRadarRenderer  // виводить ASCII-радар у термінал (для mock режиму)
````

**Логіка:**
- Servo крутить HC-SR04: 0°→180°→0°, крок 5°, затримка 50ms між кроками
- Кожен крок: `UltrasonicSensor.measure()` → `RadarScan`
- LCD: `ANG:045 DIST:32cm`
- RGB: CLEAR=зелений, WARNING=жовтий, DANGER=червоний (блимає)
- Після кожного повного проходу: зберегти `scan_YYYYMMDD_HHmmss.json`
- Stdout: `SCAN,045,32.4` (для зовнішнього Processing visualizer)
- `ConsoleRadarRenderer` у mock-режимі: ASCII напівкруг з позначеними цілями

**Тести** `RadarTest`:
````java
@Test void testUltrasonicSensor_distanceCalculation()
  // відстань = (echoMicros * 343.0) / (2 * 1_000_000) * 100 → см
  // перевірити з конкретними значеннями: 5800us → ~99.5cm
@Test void testServoController_angleToFrequency()
  // 0° → ~1ms pulse → 5% duty @ 50Hz
  // 90° → ~1.5ms pulse → 7.5% duty @ 50Hz
  // 180° → ~2ms pulse → 10% duty @ 50Hz
@Test void testDistanceZone_boundaries()
@Test void testRadarDataExporter_validJson()        // JSON серіалізація
````

---

### p06-reaction — Reaction Speed Trainer (2 Players)

**Компоненти CrowPi:** RGB LED, Button x4 (по 2 на гравця), Buzzer, LCD, 7-segment display

**Пакет:** `ua.crowpi.projects.p06`

**Класи:**
````
ReactionProject       implements CrowPiProject
Player                // POJO: String name, int score, List<Long> reactionTimesMs
GameRound             // POJO: long stimulusTime, long p1Time, long p2Time, Player winner
GameSession           // List<GameRound>, метод calcStats(), getWinner()
ReactionEngine        // ігровий цикл: 5 раундів
StimulusGenerator     // Random затримка 1000-4000ms, потім GPIO RGB flash
PlayerStats           // середнє, мін, макс час реакції з List<Long>
VictoryJingle         // PWM мелодія переможця на buzzer
````

**Логіка:**
- 5 раундів на сесію
- Кожен раунд:
  1. LCD: `READY... GET SET!`
  2. `StimulusGenerator`: пауза Random(1000,4000)ms → RGB LED спалахує
  3. `AtomicLong p1Time, p2Time` записуються GPIO listener-ами
  4. Перший хто натиснув ПІСЛЯ stimulus → отримує очко
  5. False start (кнопка ДО stimulus) → штраф -1 очко
  6. LCD рядок 1: `P1: 312ms P2:445`
  7. LCD рядок 2: `SCORE: P1=3 P2=2`
  8. 7-segment: рахунок поточного лідера
- Після 5 раундів: LCD `WINNER: PLAYER 1!` + `VictoryJingle`

**Тести** `ReactionTest`:
````java
@Test void testPlayerStats_average()
@Test void testPlayerStats_min()
@Test void testGameSession_getWinner_player1Wins()
@Test void testGameSession_getWinner_draw()
@Test void testGameRound_falseStartPenalty()
// Mockito: мокувати StimulusGenerator у ReactionEngine
@Test void testReactionEngine_falseStartDetected()
````

---

### p07-greenhouse — Automatic Greenhouse

**Компоненти CrowPi:** DHT11, soil moisture (MCP3008 SPI ADC), Relay x2, LCD, RGB LED, Button

**Пакет:** `ua.crowpi.projects.p07`

**Класи:**
````
GreenhouseProject     implements CrowPiProject
SoilMoistureSensor    // SPI MCP3008 channel 0 → 0-100% вологість
GreenhouseReading     // POJO: double temp, double humidity, int soilPercent, LocalDateTime
ActuatorState         // enum: ALL_OFF, PUMP_ON, FAN_ON, BOTH_ON
GreenhouseController  // rules engine: reading + thresholds → ActuatorState
ThresholdConfig       // temp max, soil min, завантажується з greenhouse.properties
RelayBoard            // два реле: PUMP=relay1, FAN=relay2
CsvDataLogger         // Apache Commons CSV: logs/greenhouse.csv
ManualOverride        // стан ручного перевизначення для кожного актуатора
````

**Правила GreenhouseController:**
````java
if (soilPercent < thresholds.soilMin) actuators |= PUMP_ON
if (temp > thresholds.tempMax) actuators |= FAN_ON
if (manualOverride.isPumpForced()) actuators |= PUMP_ON
````

**Логіка:**
- `ScheduledExecutorService`: читати сенсори кожні 30 секунд
- LCD рядок 1: `SOIL:45% T:24.1C`
- LCD рядок 2: `PUMP:OFF  FAN:ON`
- RGB: зелений=все норм, синій=полив, червоний=перегрів, жовтий=обидва
- Button: натискання перемикає manual override насоса
- CSV: `timestamp,temp_c,humidity_pct,soil_pct,pump_state,fan_state`

**Тести** `GreenhouseTest`:
````java
@Test void testGreenhouseController_pumpOnWhenDrySoil()
@Test void testGreenhouseController_fanOnWhenHot()
@Test void testGreenhouseController_bothOnWhenDryAndHot()
@Test void testGreenhouseController_allOffWhenNormal()
@Test void testManualOverride_overridesAutoLogic()
@Test void testCsvDataLogger_headerRow()            // перший рядок = header
````

---

### p08-rfid — RFID Door Lock

**Компоненти CrowPi:** RFID RC-522 (SPI), LCD, RGB LED, Buzzer, Relay

**Пакет:** `ua.crowpi.projects.p08`

**Класи:**
````
RfidProject           implements CrowPiProject
RfidReader            // SPI комунікація з MFRC522: повертає String uid
AccessController      // головна бізнес-логіка
KnownCard             // POJO: String uid, String ownerName, boolean active
AccessResult          // enum: GRANTED, DENIED, UNKNOWN_CARD
CardDatabase          // in-memory Map<String, KnownCard> (pre-seeded)
AccessAttemptLogger   // Log4j2: logs/rfid_access.log
RelayController       // GPIO реле = дверний замок
````

Примітка: p08 використовує простий in-memory `CardDatabase` (HashMap),
без SQLite. SQLite використовується тільки у p09.

**Pre-seeded картки у CardDatabase:**
````java
// UID-и для тестування в mock-режимі
"AA:BB:CC:DD" → KnownCard("Студент Іваненко", active=true)
"11:22:33:44" → KnownCard("Викладач Петренко", active=true)
"FF:FF:FF:FF" → KnownCard("Деактивована картка", active=false)
````

**Логіка:**
- Зчитати UID → шукати в `CardDatabase`
- GRANTED (active=true): зелений LED + relay ON (500ms) + `ДОСТУП ДОЗВОЛЕНО` на LCD
- DENIED (active=false): жовтий LED + `КАРТКА ДЕАКТИВОВАНА` на LCD
- UNKNOWN_CARD: червоний LED + buzzer + `НЕВІДОМА КАРТКА` на LCD
- Кожна спроба логується через Log4j2

**Тести** `RfidTest`:
````java
@Test void testCardDatabase_knownActiveCard()
@Test void testCardDatabase_knownInactiveCard()
@Test void testCardDatabase_unknownCard()
@Test void testAccessResult_enumValues()
// Mockito: мокувати RfidReader і перевіряти логіку AccessController
@Mock RfidReader rfidReader;
@Test void testAccessController_grantedForActiveCard() { ... }
@Test void testAccessController_deniedForInactiveCard() { ... }
@Test void testAccessController_unknownCardLogged() { ... }
````

---

### p09-smart-home — Smart Home Assistant (SQLite)

**Компоненти CrowPi:** Keypad 4x4, IR remote, LCD, Relay x2, RGB LED, Buzzer, DHT11

**Пакет:** `ua.crowpi.projects.p09`

**SQLite схема БД** (файл `smartHome.db`, створюється автоматично):
````sql
-- Профілі налаштувань (день/ніч/відпустка тощо)
CREATE TABLE IF NOT EXISTS profiles (
    id        INTEGER PRIMARY KEY AUTOINCREMENT,
    name      TEXT    NOT NULL UNIQUE,  -- 'DAY', 'NIGHT', 'VACATION'
    temp_threshold_c REAL NOT NULL DEFAULT 26.0,
    light_on  INTEGER NOT NULL DEFAULT 0,
    fan_on    INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL
);

-- Журнал подій системи розумного дому
CREATE TABLE IF NOT EXISTS device_events (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    event_type TEXT NOT NULL,   -- 'LIGHT_ON', 'LIGHT_OFF', 'FAN_ON', 'FAN_OFF',
                                -- 'PROFILE_SWITCH', 'TEMP_ALERT', 'TIMER_EXPIRED'
    device     TEXT NOT NULL,   -- 'LIGHT', 'FAN', 'SYSTEM'
    old_state  TEXT,
    new_state  TEXT NOT NULL,
    profile_id INTEGER REFERENCES profiles(id),
    timestamp  TEXT NOT NULL
);
````

**Класи:**
````
SmartHomeProject      implements CrowPiProject
Profile               // POJO: id, name, tempThreshold, lightOn, fanOn, createdAt
DeviceEvent           // POJO: id, eventType, device, oldState, newState, profileId, timestamp
ProfileRepository     // JDBC CRUD: findAll, findByName, save, update
DeviceEventRepository // JDBC: insert, findRecent(int limit), countByType
DatabaseManager       // singleton: Connection, createTables(), migrations
MenuItem              // interface: String getLabel(), void execute()
MenuNavigator         // List<MenuItem>, currentIndex, IR/keypad input routing
DeviceState           // POJO: boolean lightOn, boolean fanOn, String activeProfileName
LightController       // wraps Relay channel 1
FanController         // wraps Relay channel 2
AutoTempControl       // Thread: читає DHT11, вмикає вентилятор за профілем
InputRouter           // вирішує: IR чи keypad → один інтерфейс NavigationInput
````

**Меню (навігація IR стрілками або keypad A/B/C/D):**
````
[1] TEMPERATURE        → показати DHT11 на LCD
[2] LIGHT ON/OFF       → перемкнути Relay1, записати DeviceEvent
[3] FAN ON/OFF         → перемкнути Relay2, записати DeviceEvent
[4] SET TIMER          → ввести хвилини через keypad, auto-off
[5] SWITCH PROFILE     → DAY / NIGHT / VACATION (з БД)
[6] VIEW EVENT LOG     → прокрутити останні 10 подій по LCD
[7] SAVE CURRENT STATE → зберегти поточний стан у активний профіль
````

**Логіка:**
- `DatabaseManager` при старті: `createTables()`, seed 3 дефолтні профілі
- Активний профіль при старті: завантажити з `profiles` WHERE name='DAY'
- `AutoTempControl` Thread: кожні 60с читає DHT11, якщо temp > profile.tempThreshold → FAN_ON + DeviceEvent
- Timer: `ScheduledExecutorService.schedule()` → після N хвилин вимкнути пристрій + DeviceEvent(TIMER_EXPIRED)
- VIEW EVENT LOG: `findRecent(10)` → прокрутка по LCD (Button або IR OK → наступна подія)

**Тести** `SmartHomeTest`:
````java
// Тести репозиторіїв з in-memory SQLite (`:memory:` connection)
@BeforeEach void setUp() {
    conn = DriverManager.getConnection("jdbc:sqlite::memory:");
    db = new DatabaseManager(conn);
    db.createTables();
}

@Test void testProfileRepository_saveAndFind()
@Test void testProfileRepository_findByName_notFound()
@Test void testDeviceEventRepository_insertAndFindRecent()
@Test void testDeviceEventRepository_countByType()
@Test void testDatabaseManager_createTablesIdempotent()  // двічі createTables() не падає

// Mockito: мокувати LightController у MenuNavigator тестах
@Test void testMenuNavigator_lightToggle_callsController()
@Test void testAutoTempControl_fanOnWhenHot()
````

**ER діаграма** `docs/uml/er.puml`:
````plantuml
@startuml smart_home_er
!theme plain
title Smart Home Assistant — ER Diagram

entity "profiles" {
    * id : INTEGER <<PK, AUTOINCREMENT>>
    --
    * name : TEXT <<UNIQUE>>
    * temp_threshold_c : REAL
    * light_on : INTEGER
    * fan_on : INTEGER
    * created_at : TEXT
}

entity "device_events" {
    * id : INTEGER <<PK, AUTOINCREMENT>>
    --
    * event_type : TEXT
    * device : TEXT
    old_state : TEXT
    * new_state : TEXT
    profile_id : INTEGER <<FK>>
    * timestamp : TEXT
}

profiles ||--o{ device_events : "referenced by"

note right of device_events
    event_type values:
    LIGHT_ON, LIGHT_OFF,
    FAN_ON, FAN_OFF,
    PROFILE_SWITCH,
    TEMP_ALERT,
    TIMER_EXPIRED
end note

note right of profiles
    Default profiles:
    DAY (threshold 26°C)
    NIGHT (threshold 22°C)
    VACATION (threshold 30°C)
end note
@enduml
````

---

### p10-meteostation — Weather Station with Trend Forecast

**Компоненти CrowPi:** DHT11, sound sensor, tilt sensor, LCD, RGB LED, 7-segment display, Button

**Пакет:** `ua.crowpi.projects.p10`

**Класи:**
````
MeteostationProject   implements CrowPiProject
WeatherReading        // POJO: double temp, double humidity, boolean noisy,
                      //       boolean windDetected, LocalDateTime timestamp
RingBuffer<T>         // generic: ArrayDeque з фіксованою ємністю 180
TrendAnalyzer         // лінійна регресія МНК над List<Double> → slope
WeatherForecast       // enum: IMPROVING(slope>+0.05), STABLE, WORSENING(slope<-0.05)
HeatIndex             // static util: формула Rothfusz → apparent temp
HtmlReportGenerator   // StringBuilder → weather_report.html
CsvDataLogger         // Apache Commons CSV → logs/weather.csv
````

**Лінійна регресія у TrendAnalyzer:**
````java
// Метод найменших квадратів для List<Double> values (рівні інтервали часу)
// slope = (n*Σxy - Σx*Σy) / (n*Σx² - (Σx)²)
// де x = індекс вимірювання (0,1,2,...), y = значення температури
public double calculateSlope(List<Double> values) { ... }
````

**Логіка:**
- `ScheduledExecutorService`: читати кожні 60с, додавати до `RingBuffer(180)`
- `TrendAnalyzer.calculateSlope()` на поточному буфері → `WeatherForecast`
- LCD рядок 1: `T:23.4 H:61 HI:25`  (HI = Heat Index)
- LCD рядок 2: `TREND:↑ IMPROVING  `
- 7-segment: ціла частина температури
- Button: генерувати HTML звіт → `weather_report.html`
- RGB: за Heat Index зонами (HI<27=синій, 27-32=зелений, 32-40=жовтий, >40=червоний)

**Тести** `MeteostationTest`:
````java
@Test void testRingBuffer_overflow()          // 181-й елемент витісняє перший
@Test void testRingBuffer_toList_order()      // порядок збережено
@Test void testTrendAnalyzer_risingSlope()    // [20,21,22,23,24] → slope ≈ +1.0
@Test void testTrendAnalyzer_fallingSlope()   // [24,23,22,21,20] → slope ≈ -1.0
@Test void testTrendAnalyzer_flatSlope()      // [22,22,22,22,22] → slope ≈ 0.0
@Test void testWeatherForecast_fromSlope()    // slope > 0.05 → IMPROVING
@Test void testHeatIndex_knownValue()
  // temp=35°C, humidity=80% → HI ≈ 43°C (за таблицею Rothfusz)
@Test void testHtmlReportGenerator_containsTitle()
````

---

### p11-lcd-game — LCD Platform Game

**Компоненти CrowPi:** LCD 16x2, Button x4 (ліво/право/стрибок/pause), Buzzer, RGB LED

**Пакет:** `ua.crowpi.projects.p11`

**Класи:**
````
LcdGameProject        implements CrowPiProject
GameState             // enum: MENU, PLAYING, PAUSED, GAME_OVER, WIN
GameEngine            // головний ігровий цикл 100ms tick
Player                // x,y,velocityY,health,score,alive (все int/boolean)
Platform              // x,y,width (статична перешкода)
Enemy                 // x,y,direction,speed (рухається у своєму Thread)
Coin                  // x,y,collected
GameWorld             // Player + List<Platform> + List<Enemy> + List<Coin>
Physics               // static: GRAVITY=1, applyGravity(), checkCollision()
LcdRenderer           // char[2][16] буфер → LCD (double buffering)
LcdCharset            // константи custom chars: PLAYER='\1', ENEMY='\2', COIN='\3'
SoundEffects          // enum з PWM мелодіями: JUMP, COIN_COLLECT, HIT, WIN, GAME_OVER
ScoreRecord           // POJO: String playerName, int score, LocalDateTime date
LeaderBoard           // завантаження/збереження топ-10 у scores.json через Jackson
````

**Координатна система:**
````
LCD 16x2: x=0..15 (стовпці), y=0..1 (рядки)
y=0 = верхній рядок (повітря)
y=1 = нижній рядок (підлога і платформи)
````

**Логіка:**
- `GameEngine` tick кожні 100ms через `ScheduledExecutorService`
- `Physics.applyGravity(player)`: якщо player не на платформі → velocityY++ → y+=velocityY
- Стрибок: Button3 → якщо player.y==1 → player.velocityY = -3
- Ліво/право: Button1/Button2 → player.x ±1 (з перевіркою меж 0..15)
- `Enemy` рухається у власному `Thread`, synchronized на `GameWorld`
- `LcdRenderer`: будує `char[2][16]` array, записує custom chars позиції, пише в LCD один раз
- `LeaderBoard`: при GAME_OVER якщо score входить у топ-10 → ввести ім'я через кнопки → зберегти JSON
- Custom chars завантажуються в LCD через I2C при старті гри

**Тести** `LcdGameTest`:
````java
@Test void testPhysics_gravityIncreasesVelocity()
@Test void testPhysics_playerLandsOnPlatform()
@Test void testPhysics_playerCollidesWithEnemy()
@Test void testLeaderBoard_addScore_maintainsTop10()
@Test void testLeaderBoard_addScore_sortedDescending()
@Test void testLeaderBoard_addScore_eleventhScoreDropped()
@Test void testLcdRenderer_playerAtCorrectPosition()
// Mockito: мокувати I2cFacade у LcdRenderer
@Mock I2cFacade i2c;
@Test void testLcdRenderer_writesCorrectBuffer() { ... }
````

---

## PLANTUML ДІАГРАМИ

### Загальна архітектура `docs/diagrams/architecture.puml`
````plantuml
@startuml crowpi_architecture
!theme plain
title CrowPi Java Suite — Module Architecture

package "core" {
    interface CrowPiProject
    class Launcher
    class ProjectRegistry
    class InteractiveMenu
    package "hardware" {
        interface GpioFacade
        interface I2cFacade
        interface SensorReader
    }
    package "mock" {
        class MockGpioFacade
        class MockI2cFacade
    }
    package "exception" {
        class HardwareException
        class DatabaseException
    }
}

package "projects" {
    class p01_ThermometerProject
    class p04_AlarmProject
    class p09_SmartHomeProject
    note "... та інші 8 проектів" as N1
}

Launcher --> ProjectRegistry
ProjectRegistry --> CrowPiProject
p01_ThermometerProject ..|> CrowPiProject
p04_AlarmProject ..|> CrowPiProject
p09_SmartHomeProject ..|> CrowPiProject
MockGpioFacade ..|> GpioFacade
MockI2cFacade ..|> I2cFacade
@enduml
````

### Для кожного проекту з > 3 класів: `projects/pXX/docs/uml/classes.puml`

Включати:
- Всі класи з ключовими полями і методами (не getter/setter)
- enum зі значеннями
- Стрілки: `..>` залежність, `-->` асоціація, `..|>` реалізація, `--|>` наслідування
- Для FSM (p04): примітки зі станами переходів
- Для p09: вказати які класи звертаються до БД

---

## ДОКУМЕНТАЦІЯ КОЖНОГО МОДУЛЯ

### `projects/pXX-name/README.md` — обов'язковий вміст:
````markdown
# p0X — Назва проекту

## Що робить
(2-3 речення: суть, чому цікаво)

## Компоненти CrowPi
| Компонент | GPIO / Шина | Примітка |
|-----------|-------------|----------|
| DHT11     | GPIO 4      | Дані     |
| LCD 16x2  | I2C addr 0x27 | SDA/SCL |

## Запуск
```bash
# Реальне залізо
java -jar crowpi-suite.jar --project=<id>

# Без RPi (mock режим)
java -jar crowpi-suite.jar --project=<id> --mock
```

## Ключові концепції Java
- `ScheduledExecutorService` — ...
- `enum` зі станами — ...

## Сценарій демонстрації вступникам
1. Запустити у --mock режимі на ноутбуці
2. Показати консоль: ...
3. Пояснити що відбулось: ...

## UML діаграма
Дивись [docs/uml/classes.puml](docs/uml/classes.puml)
````

### Кореневий `README.md` включає:
- Огляд і навчальна мета
- Таблиця всіх 11 проектів (id, назва, складність, SQLite, ключова концепція)
- Встановлення Java 11 на RPi 3 (покрокова інструкція)
- Встановлення Pi4J v1.4
- `./gradlew shadowJar` → як запустити
- Як додати новий проект (реалізувати `CrowPiProject`, зареєструвати в `ProjectRegistry`)

---

## ВИМОГИ ДО ТЕСТІВ — ЗВЕДЕНО

Тести запускаються на будь-якій машині (не потрібен RPi):
````bash
./gradlew test
````

**Правила написання тестів:**
1. Клас тесту: `XxxTest` у пакеті `ua.crowpi.projects.pXX`
2. Annotation: `@ExtendWith(MockitoExtension.class)` якщо є моки
3. Hardware інтерфейси (`GpioFacade`, `I2cFacade` тощо) — тільки через `@Mock`
4. Тести чистої логіки (математика, FSM, колекції) — без моків
5. SQLite тести (p09) — використовувати `jdbc:sqlite::memory:`
6. Кожен тест: одна перевірка (`@Test` = один `assert` або логічно пов'язана група)
7. Назви тестів: `testMethodName_scenario_expectedResult()`

**Мінімальна кількість тестів на проект:**
- p01, p02, p03: ≥ 3 тести
- p04, p05, p06, p07, p08: ≥ 5 тестів (включно з Mockito)
- p09: ≥ 7 тестів (БД + Mockito)
- p10, p11: ≥ 6 тестів

---

## ПОРЯДОК ГЕНЕРАЦІЇ (якщо є обмеження контексту)

Пріоритет:
1. `core/` — Launcher, CrowPiProject, GpioFacade, I2cFacade, SensorReader,
   MockGpioFacade, MockI2cFacade, MockLcd, HardwareException,
   ProjectRegistry, InteractiveMenu, GracefulShutdown
2. `p09-smart-home/` — SQLite, найскладніший
3. `p04-alarm/` — FSM патерн
4. `p06-reaction/` — конкурентність
5. `p10-meteostation/` — Generic RingBuffer, лінійна регресія
6. `p11-lcd-game/` — Game Loop, double buffer
7. `p01..p03`, `p05`, `p07`, `p08` — простіші, у будь-якому порядку

---

## ФІНАЛЬНА ПЕРЕВІРКА

Перед завершенням переконатись:
- [ ] `./gradlew shadowJar` компілюється без помилок
- [ ] `java -jar crowpi-suite.jar --list` виводить 11 проектів
- [ ] `java -jar crowpi-suite.jar --project=smart-home --mock` запускає p09
- [ ] `./gradlew test` — всі тести зелені
- [ ] Кожен `.puml` файл починається з `@startuml` і закінчується `@enduml`
- [ ] `p09-smart-home/docs/uml/er.puml` містить обидві таблиці і зв'язок FK
- [ ] Всі `public` методи мають Javadoc
- [ ] Всі нетривіальні блоки логіки мають inline-коментарі українською
- [ ] `pom.xml` існує і має XML-коментарі з Gradle еквівалентами
- [ ] `docs/BUILD_SYSTEMS_COMPARISON.md` містить таблицю ≥ 10 рядків
