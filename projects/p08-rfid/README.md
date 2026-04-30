# p08 — RFID Door Lock

An MFRC522-based RFID access control system. Known card UIDs are stored in an
in-memory database pre-seeded with three demo cards. Access attempts are
logged via Log4j 2. A relay controls the door lock solenoid.

## Hardware wiring

| Component          | Pin / Bus         | Notes                             |
|--------------------|-------------------|-----------------------------------|
| MFRC522 SDA (CS)   | GPIO 8 (SPI CE0)  | SPI chip-select                   |
| MFRC522 SCK        | GPIO 11 (SPI CLK) | SPI clock                         |
| MFRC522 MOSI       | GPIO 10 (SPI MOSI)|                                   |
| MFRC522 MISO       | GPIO 9  (SPI MISO)|                                   |
| MFRC522 RST        | GPIO 25 (BCM)     | Reset pin                         |
| Relay (lock)       | GPIO 17 (BCM)     | Active-low; unlocks door on grant |
| Green LED (grant)  | GPIO 27 (BCM)     | Lights for 2 s on access granted  |
| Red LED (deny)     | GPIO 22 (BCM)     | Lights for 2 s on access denied   |
| Buzzer             | GPIO 12 (BCM/PWM) | Beep on grant, double-beep on deny|

## Pre-seeded demo cards

| UID           | Name              | Role        |
|---------------|-------------------|-------------|
| `AA:BB:CC:DD` | Demo Card Alpha   | USER        |
| `11:22:33:44` | Demo Card Beta    | ADMIN       |
| `DE:AD:BE:EF` | Demo Card Gamma   | USER        |

Additional cards can be registered programmatically via `CardDatabase.register()`.

## Access results

| Result         | LED    | Relay  | Log level |
|----------------|--------|--------|-----------|
| `GRANTED`      | Green  | Unlock | INFO      |
| `DENIED`       | Red    | Locked | WARN      |
| `UNKNOWN_CARD` | Red    | Locked | WARN      |

## Running

```bash
# Hardware mode
java -jar crowpi-suite-1.0.0.jar --project p08

# Mock mode (cycles through 5 random UIDs including known and unknown cards)
java -jar crowpi-suite-1.0.0.jar --project p08 --mock
```

## Key classes

| Class                 | Responsibility                                          |
|-----------------------|---------------------------------------------------------|
| `CardDatabase`        | In-memory UID→KnownCard map, case-insensitive lookup   |
| `AccessController`    | Orchestrates read → lookup → actuate → log             |
| `RfidReader`          | SPI-based MFRC522 communication / mock cycler          |
| `RelayDoorLock`       | GPIO abstraction for lock solenoid                     |
| `AccessAttemptLogger` | Log4j 2 structured access log                          |
| `KnownCard`           | Value object: UID, name, role                          |
| `AccessResult`        | Enum: `GRANTED`, `DENIED`, `UNKNOWN_CARD`              |
| `RfidProject`         | Hardware wiring and main poll loop                     |
