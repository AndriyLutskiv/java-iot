# p01 — Thermometer & Humidity Monitor

## What it does

Reads temperature and humidity from a DHT11 sensor every 2 seconds and
displays the values on a 16×2 LCD. An RGB LED changes colour based on the
thermal zone (blue = cold, green = comfortable, red = hot). Two push buttons
let the user raise or lower the alert threshold at runtime. When the temperature
crosses the threshold the buzzer sounds three short beeps. Every reading is
appended to a CSV file for later analysis in spreadsheet software.

## CrowPi Components

| Component         | GPIO / Bus          | Note                                        |
|-------------------|---------------------|---------------------------------------------|
| DHT11 sensor      | BCM GPIO 4          | Single-wire bit-bang protocol               |
| LCD 16×2 (I²C)    | I²C address 0x21    | HD44780 via PCF8574 backpack                |
| RGB LED — Red     | BCM GPIO 22         | HOT zone indicator                          |
| RGB LED — Green   | BCM GPIO 27         | COMFORT zone indicator                      |
| RGB LED — Blue    | BCM GPIO 17         | COLD zone indicator                         |
| Passive buzzer    | BCM GPIO 18 (PWM0)  | 2 kHz square wave, 3 beeps on threshold     |
| Button 1          | BCM GPIO 11         | Raises alert threshold by 1 °C per press    |
| Button 2          | BCM GPIO 9          | Lowers alert threshold by 1 °C per press    |

## Run

```bash
# Mock mode (laptop — no real hardware needed)
./gradlew :projects:p01-thermometer:run --args="--project p01 --mock"

# Real hardware (Raspberry Pi 3 only)
./gradlew shadowJar
java -jar build/libs/crowpi-suite-1.0.0.jar --project p01
```

Or select project **p01** from the interactive menu:

```bash
java -jar build/libs/crowpi-suite-1.0.0.jar
```

## Key Java Concepts

| Concept                      | Where used                                             |
|------------------------------|--------------------------------------------------------|
| `ScheduledExecutorService`   | `ThermometerProject` — periodic DHT11 polling          |
| Enum state machine           | `ThermalZone` — classifies temperature into 3 zones    |
| Volatile field + AtomicBoolean | `alertThreshold`, `running` — thread-safe flag updates |
| Apache Commons CSV           | `CsvDataLogger` — append-mode structured logging       |
| Interface + mock injection   | `SensorReader<T>`, `GpioFacade`, `I2cFacade` facades   |
| Properties loading           | `AlertConfig` + `PropertiesLoader` utility             |
| Try-with-resources           | `CsvDataLogger.log()` — safe file handle management    |

## Demo Scenario

Running in `--mock` mode cycles through three pre-configured readings:

| Reading # | Temp    | Humidity | Zone    | LED colour |
|-----------|---------|----------|---------|------------|
| 1         | 15.0 °C | 60 %     | COLD    | Blue       |
| 2         | 23.0 °C | 65 %     | COMFORT | Green      |
| 3         | 32.0 °C | 70 %     | HOT     | Red        |
| 4         | 15.0 °C | 60 %     | COLD    | Blue (cycles) |

The buzzer fires on the COMFORT → HOT transition (reading 2 → 3) when the
default threshold of 28 °C is crossed.

## Configuration

Edit `src/main/resources/thermometer.properties` to tune the defaults:

```properties
alert.threshold.celsius=28
poll.interval.seconds=2
log.file=logs/thermometer.csv
```

## UML

See [docs/uml/classes.puml](docs/uml/classes.puml)
