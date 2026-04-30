# p10 — Weather Station (Meteostation)

A DHT11-based weather monitor that maintains a 3-hour rolling history in a
generic `RingBuffer<T>`, computes an OLS linear-regression trend, calculates
heat index (Rothfusz formula), generates an HTML report, and logs all readings
to CSV.

## Hardware wiring

| Component          | Pin / Bus         | Notes                           |
|--------------------|-------------------|---------------------------------|
| DHT11              | GPIO 4 (BCM)      | Temperature + humidity          |
| BMP180 (pressure)  | SDA/SCL (I²C-1)   | Optional; omit in mock mode     |
| Tilt sensor        | GPIO 17 (BCM)     | Detects station knocked over    |
| LCD (I²C)          | SDA/SCL (I²C-1)   | Address 0x27                    |
| Buzzer             | GPIO 12 (BCM/PWM) | Alert on threshold breach       |

## Forecast algorithm

The last N readings (up to 180, one per minute) are stored in a
`RingBuffer<WeatherReading>`. `TrendAnalyzer` computes the OLS slope of
temperature over time:

| Slope            | Forecast        |
|------------------|-----------------|
| > +0.05 °C/min   | `IMPROVING`     |
| < −0.05 °C/min   | `WORSENING`     |
| Otherwise        | `STABLE`        |

## Output files

| File                           | Format                   |
|--------------------------------|--------------------------|
| `meteo_<timestamp>.csv`        | Timestamp, temp, humidity, pressure, heat index |
| `meteo_report_<timestamp>.html`| Self-contained HTML with embedded table          |

## Running

```bash
# Hardware mode (60 s poll interval)
java -jar crowpi-suite-1.0.0.jar --project p10

# Mock mode (5 s poll interval, cycles 5 preset readings)
java -jar crowpi-suite-1.0.0.jar --project p10 --mock
```

## Key classes

| Class                | Responsibility                                         |
|----------------------|--------------------------------------------------------|
| `RingBuffer<T>`      | Bounded FIFO backed by `ArrayDeque`; oldest dropped    |
| `TrendAnalyzer`      | OLS slope → `WeatherForecast` enum                    |
| `HeatIndex`          | Rothfusz equation; valid for temp ≥ 27 °C, RH ≥ 40 %  |
| `WeatherReading`     | Value object: temp, humidity, pressure, timestamp      |
| `WeatherForecast`    | Enum: `IMPROVING`, `STABLE`, `WORSENING`              |
| `HtmlReportGenerator`| Generates a Bootstrap-styled HTML table               |
| `CsvDataLogger`      | Apache Commons CSV, header on first write              |
| `MeteostationProject`| Poll loop, ring buffer management, report trigger      |
