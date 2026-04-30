# p02 — Motion Event Counter

> CrowPi Java Educational Suite · Raspberry Pi 3 · Java 11

---

## What it does

The **Motion Event Counter** detects human presence with a PIR (Passive Infrared)
sensor and displays a running count on a 7-segment LED display.
Every detection fires a short buzzer click for audible feedback and appends a
timestamped record to `logs/events.log`.

Two push-buttons let the student interact at runtime:

| Button  | `COUNT_UP` mode           | `COUNTDOWN` mode                      |
|---------|---------------------------|---------------------------------------|
| Button1 | Reset counter → 0         | Increment the countdown start value   |
| Button2 | Switch to `COUNTDOWN`     | Switch to `COUNT_UP`                  |

The counter wraps modulo 10 (0–9) in both directions.

---

## CrowPi Components

| Component          | BCM GPIO pin(s)              | Role                        |
|--------------------|------------------------------|-----------------------------|
| PIR sensor         | GPIO 26                      | Detects motion               |
| 7-segment display  | GPIO 6, 13, 19, 21, 20, 16, 12 | Shows current count (0–9) |
| Button 1           | GPIO 11                      | Reset / set start value      |
| Button 2           | GPIO  9                      | Toggle COUNT_UP ↔ COUNTDOWN  |
| Active buzzer      | GPIO 18 (PWM)                | Audible click on each event  |

---

## Run

**Mock mode (laptop, no hardware):**

```bash
# From repository root
./gradlew :projects:p02-counter:run --args="--project p02 --mock"
```

**Real hardware (Raspberry Pi 3):**

```bash
./gradlew shadowJar
java -jar build/libs/crowpi-suite-1.0.0.jar --project p02
```

The fat-JAR is assembled at the root project level and contains all modules.

---

## Key Java Concepts

| Concept | Where it appears |
|---|---|
| `enum` with methods | `CounterMode.toggle()` — behaviour inside an enum constant |
| `volatile` fields | `counter`, `mode`, `running` — safe cross-thread visibility |
| Functional interface / lambda | `gpio.addListener(pin, (p, high) -> …)` |
| Java NIO file append | `EventFileLogger` — `Files.write` with `APPEND \| CREATE` |
| `ServiceLoader` registration | `META-INF/services/ua.crowpi.core.CrowPiProject` |
| Dependency injection (constructor) | `CounterProject(GpioFacade)` — testable without real GPIO |
| Immutable POJO | `EventRecord` — all fields `final`, set once in constructor |
| Modulo wrap-around | `counter = (counter + 1) % 10` — clean 0-9 cycling |

---

## Demo Scenario

1. Run in mock mode — the mock facade fires simulated PIR events every **5 seconds**.
2. Watch the `[MOCK GPIO]` output in the terminal — segment pins toggle as the
   display updates.
3. After three simulated events `logs/events.log` contains lines such as:
   ```
   2024-01-15T14:23:06 | COUNT_UP | value=1
   2024-01-15T14:23:11 | COUNT_UP | value=2
   2024-01-15T14:23:16 | COUNT_UP | value=3
   ```
4. On real hardware, press **Button 2** to switch to `COUNTDOWN`, then use
   **Button 1** to set the start value (each press +1, up to 9). The display
   shows the chosen start value immediately.

---

## UML

Class diagram: [`docs/uml/classes.puml`](docs/uml/classes.puml)

```
CounterProject
  ├── GpioFacade          (injected — MockGpioFacade in mock mode)
  ├── SevenSegmentDisplay (drives 7 GPIO output pins)
  ├── EventFileLogger     (appends to logs/events.log)
  └── CounterMode         (volatile enum field; toggled by Button2)
```

Render with [PlantUML](https://plantuml.com) or the VS Code PlantUML extension.

---

## File layout

```
p02-counter/
├── build.gradle
├── README.md
├── docs/
│   └── uml/
│       └── classes.puml
└── src/
    ├── main/
    │   ├── java/ua/crowpi/projects/p02/
    │   │   ├── CounterMode.java
    │   │   ├── CounterProject.java
    │   │   ├── EventFileLogger.java
    │   │   ├── EventRecord.java
    │   │   └── SevenSegmentDisplay.java
    │   └── resources/META-INF/services/
    │       └── ua.crowpi.core.CrowPiProject
    └── test/
        └── java/ua/crowpi/projects/p02/
            └── CounterTest.java
```
