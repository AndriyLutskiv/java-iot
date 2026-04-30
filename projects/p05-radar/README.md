# p05 — Ultrasonic Radar

A servo-mounted HC-SR04 ultrasonic sensor sweeps from 0° to 180° in 10°
steps, measuring distance at each angle. Results are colour-coded by zone
and exported to a JSON file after each sweep.

## Hardware wiring

| Component           | Pin / Bus         | Notes                           |
|---------------------|-------------------|---------------------------------|
| HC-SR04 TRIG        | GPIO 23 (BCM)     | Output                          |
| HC-SR04 ECHO        | GPIO 24 (BCM)     | Input (use 3.3 V logic divider) |
| SG90 servo (PWM)    | GPIO 18 (BCM/PWM) | 50 Hz, 1-2 ms pulse width       |
| Buzzer (warning)    | GPIO 12 (BCM/PWM) | Sounds when DANGER zone entered |

## Distance zones

| Zone      | Distance     | Console colour |
|-----------|--------------|----------------|
| `CLEAR`   | > 50 cm      | Green          |
| `WARNING` | 20 – 50 cm   | Yellow         |
| `DANGER`  | < 20 cm      | Red            |

## Output files

After every sweep a JSON file is written to the working directory:

```
radar_<timestamp>.json
```

Example content:
```json
[
  {"angle":0,"distanceCm":82.3,"zone":"CLEAR"},
  {"angle":10,"distanceCm":35.1,"zone":"WARNING"},
  ...
]
```

## Running

```bash
# Hardware mode
java -jar crowpi-suite-1.0.0.jar --project p05

# Mock mode (random distances 5-200 cm)
java -jar crowpi-suite-1.0.0.jar --project p05 --mock
```

## Key classes

| Class                  | Responsibility                                        |
|------------------------|-------------------------------------------------------|
| `UltrasonicSensor`     | GPIO trigger/echo timing → distance in cm             |
| `ServoController`      | PWM pulse width calculation for 0-180° sweep          |
| `DistanceZone`         | Enum with `forDistance(double)` factory               |
| `RadarScan`            | Full sweep: list of `ScanResult` records              |
| `RadarDataExporter`    | Serialises scan to JSON (manual `StringBuilder`)      |
| `ConsoleRadarRenderer` | ANSI-coloured sweep display on stdout                 |
| `RadarProject`         | Main loop: sweep → render → export, repeat            |
