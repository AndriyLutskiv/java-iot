# CrowPi Java Educational Suite

A collection of 11 hands-on Raspberry Pi 3B projects built with Java 11 and the CrowPi learning platform. Each project demonstrates a distinct software-engineering concept using real hardware components wired through Pi4J v1.4.

---

## Table of Contents

- [Requirements](#requirements)
- [Quick Start](#quick-start)
- [All Projects](#all-projects)
- [Hardware Setup](#hardware-setup)
  - [p01 — Thermometer](#p01--thermometer--humidity-monitor)
  - [p02 — Motion Counter](#p02--motion-event-counter)
  - [p03 — Music Box](#p03--ir-remote-music-box)
  - [p04 — Alarm System](#p04--alarm-system)
  - [p05 — Radar](#p05--ultrasonic-radar)
  - [p06 — Reaction Trainer](#p06--reaction-speed-trainer)
  - [p07 — Greenhouse](#p07--automatic-greenhouse)
  - [p08 — RFID Lock](#p08--rfid-door-lock)
  - [p09 — Smart Home](#p09--smart-home-assistant)
  - [p10 — Weather Station](#p10--weather-station)
  - [p11 — LCD Game](#p11--lcd-platform-game)
- [Building the JAR](#building-the-jar)
- [Running](#running)
- [Mock Mode (no hardware)](#mock-mode-no-hardware)
- [Configuration](#configuration)
- [Adding a New Project](#adding-a-new-project)

---

## Requirements

| Component | Version |
|-----------|---------|
| Hardware  | Raspberry Pi 3B / 3B+ with CrowPi case |
| OS        | Raspberry Pi OS Bullseye (32-bit) |
| Java (RPi)| OpenJDK 11 ARM: `sudo apt install openjdk-11-jdk` |
| Java (dev)| OpenJDK 11 (build machine) |
| Gradle    | 7.6.1 (wrapper included — use `./gradlew`) |

> **Java version matters.** Gradle 7.6.1 does not support Java 21. Always build with Java 11:
> ```bash
> export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
> ```

---

## Quick Start

```bash
# Build the self-contained fat JAR
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
./gradlew shadowJar

# List all 12 projects
java -jar build/libs/crowpi-suite-1.0.0.jar --list

# Run any project in mock mode (no hardware needed)
java -jar build/libs/crowpi-suite-1.0.0.jar --project=p01 --mock
java -jar build/libs/crowpi-suite-1.0.0.jar --project=p04 --mock
java -jar build/libs/crowpi-suite-1.0.0.jar --project=smart-home --mock

# Interactive menu
java -jar build/libs/crowpi-suite-1.0.0.jar
```

---

## All Projects

| # | Project ID | Name | Key Java Concept | Hardware |
|---|------------|------|-----------------|----------|
| 1 | `p01` | Thermometer & Humidity Monitor | `ScheduledExecutorService`, CSV logging | DHT11, LCD, RGB LED, Buzzer |
| 2 | `p02` | Motion Event Counter | `volatile`, GPIO listeners, enum modes | PIR, 7-segment, Buzzer |
| 3 | `p03` | IR Remote Music Box | Thread, PWM audio, enum, Iterator | IR receiver, Buzzer, LCD, RGB |
| 4 | `p04` | Alarm System | FSM (`EnumMap`), SHA-256, GPIO polling | PIR, Keypad 4×4, Relay, LCD |
| 5 | `p05` | Ultrasonic Radar | PWM servo, distance zones, JSON export | HC-SR04, Servo, LCD, RGB |
| 6 | `p06` | Reaction Speed Trainer | `AtomicLong`, concurrency, game loop | RGB LED, Buttons, Buzzer |
| 7 | `p07` | Automatic Greenhouse | Rules engine, SPI ADC, CSV logging | DHT11, Soil sensor, Relays, LCD |
| 8 | `p08` | RFID Door Lock | `HashMap`, SPI, Log4j2, relay | MFRC522, LCD, RGB, Buzzer, Relay |
| 9 | `smart-home` | Smart Home Assistant | JDBC, Repository pattern, SQLite | DHT11, Keypad, Relays, LCD |
| 10 | `p10` | Weather Station | `RingBuffer<T>`, OLS regression, HTML | DHT11, Sound sensor, LCD |
| 11 | `p11` | LCD Platform Game | Game loop, double buffer, JSON | LCD, Buttons, Buzzer, RGB |
| 12 | `p12` | Currency & Clock Display | `java.net.http.HttpClient`, Jackson, REST API | LCD (I²C only) |

Each project has its own `README.md` with GPIO wiring, run instructions, and key concepts.

---

## Hardware Setup

### GPIO Pin Map (BCM numbering)

```
BCM  │ Used by
─────┼──────────────────────────────────────────
  4  │ p01, p07, p09, p10 — DHT11 data
  6  │ p04 — RGB red  /  p10 — RGB green  /  p11 — PAUSE button
  9  │ p01 — Button2  /  p06 — RGB green (stimulus)
 10  │ p06 — RGB blue (stimulus)
 11  │ p01 — Button1  /  p06 — RGB red  /  p07 — Button
 12  │ p02 — 7-seg segment G
 13  │ p02 — 7-seg segment B  /  p11 — JUMP button
 16  │ p02 — 7-seg segment F  /  p04 — Keypad col 1
 17  │ p03 — RGB blue  /  p04 — Keypad row 1  /  p08 — RGB blue  /  p10 — Sound sensor
 18  │ p01,p03,p04,p06,p08,p11 — Buzzer (PWM0)
 19  │ p04 — Keypad col 4  /  p06 — Player 2 button  /  p11 — RIGHT button
 20  │ p02 — 7-seg segment E  /  p09 — Fan relay (channel 2)
 21  │ p02 — 7-seg segment D  /  p04 — Relay  /  p07 — Pump relay  /  p09 — Light relay
 22  │ p01 — RGB red  /  p04 — Keypad row 3  /  p05,p07,p08,p10 — RGB red
 23  │ p04 — Keypad col 4  /  p05 — HC-SR04 trigger
 24  │ p04 — Keypad col 3  /  p05 — HC-SR04 echo
 25  │ p04 — Keypad col 2  /  p05 — Servo PWM
 26  │ p04 — PIR  /  p06 — Player 1 button  /  p11 — LEFT button
 27  │ p01 — RGB green  /  p04 — Keypad row 2  /  p05,p07,p08 — RGB green  /  p10 — Tilt sensor
─────┼──────────────────────────────────────────
I2C  │ SDA=GPIO2 (pin 3), SCL=GPIO3 (pin 5)
     │ LCD 16×2 at address 0x27 — used by p01,p03,p04,p05,p07,p08,p09,p10,p11
SPI  │ CE0=GPIO8 — MFRC522 RFID (p08)
```

---

### p01 — Thermometer & Humidity Monitor

**Components:** DHT11, LCD 16×2 (I2C), RGB LED, Buzzer, 2× Button

```
DHT11 DATA  →  GPIO 4  (pin 7)   + 10kΩ pull-up to 3.3V
RGB Red     →  GPIO 22 (pin 15)  + 330Ω
RGB Green   →  GPIO 27 (pin 13)  + 330Ω
RGB Blue    →  GPIO 17 (pin 11)  + 330Ω
Buzzer +    →  GPIO 18 (pin 12)  PWM0
Button1 +   →  GPIO 11 (pin 23)  → raise threshold
Button2 +   →  GPIO 9  (pin 21)  → lower threshold
LCD SDA/SCL →  GPIO 2/3 (pins 3,5)  addr 0x27
```

---

### p02 — Motion Event Counter

**Components:** PIR HC-SR501, 7-segment display, 2× Button, Buzzer

```
PIR OUT     →  GPIO 26 (pin 37)
7-seg A     →  GPIO 6  (pin 31)  + 220Ω each
7-seg B     →  GPIO 13 (pin 33)
7-seg C     →  GPIO 19 (pin 35)
7-seg D     →  GPIO 21 (pin 40)
7-seg E     →  GPIO 20 (pin 38)
7-seg F     →  GPIO 16 (pin 36)
7-seg G     →  GPIO 12 (pin 32)
7-seg COM   →  GND
Button1 +   →  GPIO 11 (pin 23)  → reset
Button2 +   →  GPIO 9  (pin 21)  → mode toggle
Buzzer +    →  GPIO 18 (pin 12)
```

---

### p03 — IR Remote Music Box

**Components:** IR receiver, Buzzer (passive, PWM), LCD 16×2, RGB LED

```
IR receiver OUT  →  GPIO 24 (pin 18)
Buzzer +         →  GPIO 18 (pin 12)  PWM0
RGB Red          →  GPIO 22 (pin 15)  + 330Ω
RGB Green        →  GPIO 27 (pin 13)  + 330Ω
RGB Blue         →  GPIO 17 (pin 11)  + 330Ω
LCD SDA/SCL      →  GPIO 2/3           addr 0x27
```

IR remote: buttons 1–9 play melodies; button 0 stops.

---

### p04 — Alarm System

**Components:** PIR HC-SR501, 4×4 Matrix Keypad, Buzzer, RGB LED, LCD 16×2, Relay

#### PIR Sensor
```
HC-SR501 VCC  →  5V (pin 2)
HC-SR501 GND  →  GND (pin 6)
HC-SR501 OUT  →  GPIO 26 (pin 37)
```
Set HC-SR501 jumper to **H** (repeating trigger) mode.

#### 4×4 Matrix Keypad
```
Row 1  →  GPIO 17 (pin 11)   Col 1  →  GPIO 16 (pin 36)
Row 2  →  GPIO 27 (pin 13)   Col 2  →  GPIO 25 (pin 22)
Row 3  →  GPIO 22 (pin 15)   Col 3  →  GPIO 24 (pin 18)
Row 4  →  GPIO  5 (pin 29)   Col 4  →  GPIO 23 (pin 16)
```

#### RGB LED, Buzzer, Relay, LCD — same as described above

| State | RGB Color |
|-------|-----------|
| DISARMED | Green |
| ARMED | Yellow |
| TRIGGERED | Red |
| LOCKED | Purple |

**Changing the PIN:**
```bash
echo -n "YOUR_NEW_PIN" | sha256sum
# Copy result into alarm.properties: pin.hash=<value>
```

---

### p05 — Ultrasonic Radar

**Components:** HC-SR04 (TRIGGER/ECHO), Servo motor (PWM), LCD, RGB LED

```
HC-SR04 VCC     →  5V (pin 2)
HC-SR04 GND     →  GND (pin 6)
HC-SR04 TRIGGER →  GPIO 23 (pin 16)
HC-SR04 ECHO    →  GPIO 24 (pin 18)   + voltage divider 1kΩ/2kΩ (5V→3.3V)
Servo signal    →  GPIO 25 (pin 22)   PWM 50Hz
RGB Red         →  GPIO 22 (pin 15)
RGB Green       →  GPIO 27 (pin 13)
RGB Blue        →  GPIO 17 (pin 11)
LCD SDA/SCL     →  GPIO 2/3            addr 0x27
```

| Zone | Distance | RGB |
|------|----------|-----|
| CLEAR | > 50 cm | Green |
| WARNING | 20–50 cm | Yellow |
| DANGER | < 20 cm | Red (blinks) |

---

### p06 — Reaction Speed Trainer

**Components:** RGB LED, 2× Pushbutton, Buzzer

```
RGB Red    →  GPIO 11 (pin 23)  + 330Ω
RGB Green  →  GPIO  9 (pin 21)  + 330Ω
RGB Blue   →  GPIO 10 (pin 19)  + 330Ω
P1 Button  →  GPIO 26 (pin 37)  → internal pull-down
P2 Button  →  GPIO 19 (pin 35)  → internal pull-down
Buzzer +   →  GPIO 18 (pin 12)
```

Press your button **after** the RGB flashes white. Pressing before the flash = -1 point.

---

### p07 — Automatic Greenhouse

**Components:** DHT11, Soil moisture sensor (MCP3008 SPI ADC), 2× Relay, LCD, RGB LED, Button

```
DHT11 DATA    →  GPIO 4  (pin 7)
Soil sensor   →  MCP3008 channel 0 (SPI)
Pump relay    →  GPIO 21 (pin 40)   → water pump
Fan relay     →  GPIO 20 (pin 38)   → cooling fan
Button        →  GPIO 11 (pin 23)   → manual pump override
RGB Red       →  GPIO 22 (pin 15)
RGB Green     →  GPIO 27 (pin 13)
RGB Blue      →  GPIO 17 (pin 11)
LCD SDA/SCL   →  GPIO 2/3            addr 0x27
```

Configure thresholds in `greenhouse.properties` (classpath).

---

### p08 — RFID Door Lock

**Components:** MFRC522 RFID reader (SPI), LCD, RGB LED, Buzzer, Relay (door lock)

```
MFRC522 SDA   →  GPIO 8  (pin 24)   SPI CE0
MFRC522 SCK   →  GPIO 11 (pin 23)   SPI CLK
MFRC522 MOSI  →  GPIO 10 (pin 19)   SPI MOSI
MFRC522 MISO  →  GPIO 9  (pin 21)   SPI MISO
MFRC522 RST   →  GPIO 25 (pin 22)
MFRC522 VCC   →  3.3V (pin 1)
Relay IN      →  GPIO 21 (pin 40)   → door lock
RGB Red       →  GPIO 22 (pin 15)
RGB Green     →  GPIO 27 (pin 13)
RGB Blue      →  GPIO 17 (pin 11)
Buzzer +      →  GPIO 18 (pin 12)
LCD SDA/SCL   →  GPIO 2/3            addr 0x27
```

Enable SPI: `sudo raspi-config` → Interface Options → SPI → Enable.

Pre-seeded test cards:
| UID | Owner | Result |
|-----|-------|--------|
| `AA:BB:CC:DD` | Student Ivanenko | GRANTED |
| `11:22:33:44` | Teacher Petrenko | GRANTED |
| `FF:FF:FF:FF` | Deactivated Card | DENIED |

---

### p09 — Smart Home Assistant

**Components:** DHT11, 2× Relay, LCD 16×2 (I2C), Keypad 4×4

```
DHT11 DATA   →  GPIO 4  (pin 7)
Light relay  →  GPIO 21 (pin 40)
Fan relay    →  GPIO 20 (pin 38)
Keypad rows  →  GPIO 17, 27, 22, 5
Keypad cols  →  GPIO 16, 25, 24, 23
LCD SDA/SCL  →  GPIO 2/3    addr 0x27
```

SQLite database `smart_home.db` is created automatically.

```bash
sqlite3 smart_home.db "SELECT * FROM device_events ORDER BY id DESC LIMIT 20;"
```

---

### p10 — Weather Station

**Components:** DHT11, Sound sensor, Tilt sensor, LCD, RGB LED, Button

```
DHT11 DATA    →  GPIO 4  (pin 7)
Sound sensor  →  GPIO 17 (pin 11)
Tilt sensor   →  GPIO 27 (pin 13)
Button        →  GPIO 11 (pin 23)  → generate HTML report
RGB Red       →  GPIO 22 (pin 15)
RGB Green     →  GPIO 6  (pin 31)
RGB Blue      →  GPIO 13 (pin 33)
LCD SDA/SCL   →  GPIO 2/3           addr 0x27
```

Press the button to generate `weather_report.html` with a 3-hour history and OLS trend.

| Heat Index | RGB |
|-----------|-----|
| < 27°C | Blue (comfortable) |
| 27–32°C | Green (warm) |
| 32–40°C | Yellow (hot) |
| > 40°C | Red (danger) |

---

### p11 — LCD Platform Game

**Components:** LCD 16×2, 4× Buttons, Buzzer, RGB LED

```
LEFT button   →  GPIO 26 (pin 37)
RIGHT button  →  GPIO 19 (pin 35)
JUMP button   →  GPIO 13 (pin 33)
PAUSE button  →  GPIO  6 (pin 31)
Buzzer +      →  GPIO 18 (pin 12)
LCD SDA/SCL   →  GPIO 2/3    addr 0x27
```

Top-10 leaderboard stored in `scores.json`.

---

### p12 — Currency & Clock Display

**Components:** LCD 16×2 (I²C only — no GPIO pins required)

```
LCD SDA/SCL  →  GPIO 2/3    addr 0x27
```

Alternates every 5 seconds between:
- **Clock page** — live time (`HH:MM:SS`) and date (`DD.MM.YYYY`), updated every second
- **Currency page** — USD/UAH and EUR/UAH rates fetched from the NBU REST API

NBU API endpoint: `https://bank.gov.ua/NBU_Exchange/exchange?json`

On fetch failure the last cached rates (or `N/A`) are shown. No extra dependencies —
uses `java.net.http.HttpClient` built into Java 11.

```bash
java -jar crowpi-suite-1.0.0.jar --project p12          # hardware (RPi)
java -jar crowpi-suite-1.0.0.jar --project p12 --mock   # mock (any PC)
```

---

## Building the JAR

```bash
cd rpi3-project-demo
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64

# Build + test
./gradlew test shadowJar

# Output: build/libs/crowpi-suite-1.0.0.jar  (~18 MB)

# Copy to Raspberry Pi
scp build/libs/crowpi-suite-1.0.0.jar pi@raspberrypi.local:~/
```

---

## Running

```bash
# Interactive menu (all 12 projects)
java -jar crowpi-suite-1.0.0.jar

# List all projects
java -jar crowpi-suite-1.0.0.jar --list

# Run a specific project
java -jar crowpi-suite-1.0.0.jar --project=p01
java -jar crowpi-suite-1.0.0.jar --project=p04
java -jar crowpi-suite-1.0.0.jar --project=smart-home
java -jar crowpi-suite-1.0.0.jar --project=p11

# Help
java -jar crowpi-suite-1.0.0.jar --help
```

> **GPIO access on Raspberry Pi requires the `gpio` group:**
> ```bash
> sudo usermod -aG gpio pi && sudo reboot
> # Or run with sudo:
> sudo java -jar crowpi-suite-1.0.0.jar --project=p04
> ```

Press **Ctrl+C** to stop. The JVM shutdown hook calls `project.shutdown()` cleanly.

---

## Mock Mode (no hardware)

Run on any laptop/desktop without a Raspberry Pi:

```bash
java -jar crowpi-suite-1.0.0.jar --project=p01 --mock
java -jar crowpi-suite-1.0.0.jar --project=p03 --mock
java -jar crowpi-suite-1.0.0.jar --project=p04 --mock
java -jar crowpi-suite-1.0.0.jar --project=p07 --mock
java -jar crowpi-suite-1.0.0.jar --project=p11 --mock
```

Mock mode features:
- `MockI2cFacade` renders a box-drawing LCD to the console
- `MockGpioFacade` fires GPIO listeners automatically every 5s
- `MockSensorReader<T>` cycles through pre-defined sensor values
- RFID reader cycles through 4 test card UIDs

---

## Configuration

### p01 — thermometer.properties
| Property | Default | Description |
|----------|---------|-------------|
| `alert.threshold.celsius` | `28` | Temperature alert level |
| `poll.interval.seconds` | `2` | DHT11 read interval |
| `log.file` | `logs/thermometer.csv` | CSV output path |

### p04 — alarm.properties
| Property | Default | Description |
|----------|---------|-------------|
| `pin.hash` | SHA-256 of `1234` | Access PIN (hex SHA-256 digest) |
| `lockout.duration.seconds` | `30` | Lockout after 3 wrong PINs |
| `log.file` | `logs/alarm.log` | Alarm event log |

```bash
# Generate SHA-256 for a new PIN
echo -n "YOUR_PIN" | sha256sum
```

### p07 — greenhouse.properties
| Property | Default |
|----------|---------|
| `temp.max.celsius` | `30` |
| `soil.moisture.min.percent` | `40` |
| `poll.interval.seconds` | `30` |

### p09 — SQLite
```bash
sqlite3 smart_home.db ".tables"
sqlite3 smart_home.db "SELECT * FROM profiles;"
sqlite3 smart_home.db "SELECT * FROM device_events ORDER BY id DESC LIMIT 10;"
```

---

## Adding a New Project

1. Create `projects/pXX-name/` with the standard directory structure
2. Add `build.gradle` with `implementation project(':core')`
3. Implement `CrowPiProject` in package `ua.crowpi.projects.pXX`
4. Register in `src/main/resources/META-INF/services/ua.crowpi.core.CrowPiProject`
5. Add `include 'projects:pXX-name'` to `settings.gradle`
6. Add `implementation project(':projects:pXX-name')` to root `build.gradle`
7. Write `README.md` and UML diagram in `docs/uml/classes.puml`

```java
public class MyProject implements CrowPiProject {
    @Override public String getName()        { return "My Project"; }
    @Override public String getProjectId()   { return "pXX"; }
    @Override public String getDescription() { return "..."; }
    @Override public void run(boolean mock)  throws HardwareException { ... }
    @Override public void shutdown()         { ... }
}
```
