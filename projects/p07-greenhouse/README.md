# p07 — Automatic Greenhouse

An automated plant care system that continuously monitors soil moisture and
ambient temperature, then drives a water pump and ventilation fan through a
relay board. All readings are logged to a timestamped CSV file.

## Hardware wiring

| Component              | Pin / Bus         | Notes                           |
|------------------------|-------------------|---------------------------------|
| DHT11 (temp/humidity)  | GPIO 4 (BCM)      | Data pin                        |
| Soil moisture sensor   | GPIO 17 (BCM)     | Analogue → digital via ADC or comparator |
| Relay CH1 (pump)       | GPIO 27 (BCM)     | Active-low relay board          |
| Relay CH2 (fan)        | GPIO 22 (BCM)     | Active-low relay board          |
| Buzzer (alert)         | GPIO 12 (BCM/PWM) | Sounds when both actuators on   |
| LCD (I²C)              | SDA/SCL (I²C-1)   | Address 0x27                    |

## Configuration

`greenhouse.properties` (place beside the JAR):

```properties
temp.max.celsius=30
soil.moisture.min.percent=40
poll.interval.seconds=5
csv.output.dir=.
```

## Actuator logic

| Condition                     | Pump | Fan  |
|-------------------------------|------|------|
| Soil OK, Temp OK              | OFF  | OFF  |
| Soil dry only                 | ON   | OFF  |
| Temp high only                | OFF  | ON   |
| Soil dry **and** Temp high    | ON   | ON   |

Manual override (button on GPIO 11) toggles `MANUAL` mode, freezing all
automatic actuator decisions.

## CSV log format

```
timestamp,temperature_c,humidity_pct,soil_moisture_pct,pump,fan
2024-01-15T10:30:00,24.5,58,35,true,false
```

## Running

```bash
# Hardware mode
java -jar crowpi-suite-1.0.0.jar --project p07

# Mock mode (simulated sensor cycling)
java -jar crowpi-suite-1.0.0.jar --project p07 --mock
```

## Key classes

| Class                  | Responsibility                                         |
|------------------------|--------------------------------------------------------|
| `GreenhouseController` | Reads sensors, decides actuator state, writes LCD      |
| `ThresholdConfig`      | Loads `greenhouse.properties`                          |
| `ActuatorState`        | Enum: `ALL_OFF`, `PUMP_ON`, `FAN_ON`, `BOTH_ON`       |
| `RelayBoard`           | GPIO abstraction for the two relay channels            |
| `SoilMoistureSensor`   | Converts GPIO digital read to moisture percentage      |
| `CsvDataLogger`        | Appends rows with header-on-first-write logic          |
| `ManualOverride`       | Thread-safe toggle flag                                |
| `GreenhouseProject`    | Hardware wiring, poll loop, shutdown hook              |
