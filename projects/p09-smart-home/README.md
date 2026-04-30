# p09 — Smart Home Assistant

A keypad/IR-driven menu system to control relays and a DHT11 temperature
sensor. All device state transitions are persisted to a local SQLite database,
enabling a complete event history and state recovery after restart.

## Hardware wiring

| Component          | Pin / Bus         | Notes                           |
|--------------------|-------------------|---------------------------------|
| DHT11              | GPIO 4 (BCM)      | Temperature + humidity          |
| IR receiver        | GPIO 18 (BCM)     | NEC protocol remote             |
| 4×4 Keypad rows    | GPIO 5,6,13,19    | Output (BCM)                    |
| 4×4 Keypad cols    | GPIO 26,21,20,16  | Input with pull-down (BCM)      |
| Relay CH1 (light)  | GPIO 17 (BCM)     | Active-low relay board          |
| Relay CH2 (fan)    | GPIO 27 (BCM)     | Active-low relay board          |
| Relay CH3 (heater) | GPIO 22 (BCM)     | Active-low relay board          |
| LCD (I²C)          | SDA/SCL (I²C-1)   | Address 0x27                    |

## Menu system

```
[1] Light ON/OFF
[2] Fan ON/OFF
[3] Heater ON/OFF
[4] Show temperature
[5] Event history (last 10)
[0] Exit
```

## SQLite schema

```sql
CREATE TABLE device_events (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    device     TEXT    NOT NULL,
    event_type TEXT    NOT NULL,
    old_state  TEXT,
    new_state  TEXT    NOT NULL,
    timestamp  TEXT    NOT NULL
);
```

The database file is `smart_home.db` in the working directory.
Unit tests use `jdbc:sqlite::memory:` for full isolation.

## Running

```bash
# Hardware mode
java -jar crowpi-suite-1.0.0.jar --project smart-home

# Mock mode (keyboard simulates keypad, no GPIO required)
java -jar crowpi-suite-1.0.0.jar --project smart-home --mock
```

## Key classes

| Class                    | Responsibility                                       |
|--------------------------|------------------------------------------------------|
| `DeviceEventRepository`  | JDBC CRUD against SQLite; `findLast(n)`, `save()`   |
| `SmartHomeService`       | Business logic: toggle device, read sensor           |
| `DeviceState`            | Enum: `ON`, `OFF`                                   |
| `DeviceEvent`            | Value object for a single state-change record        |
| `SmartHomeProject`       | Menu loop, keypad dispatch, LCD updates              |
