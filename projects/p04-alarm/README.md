# p04 — Alarm System (FSM)

PIR-triggered alarm with SHA-256 PIN code entry, a keypad, buzzer siren,
and a relay-controlled strobe. The system is modelled as a finite state
machine with lockout after three wrong PIN attempts.

## Hardware wiring

| Component         | Pin / Bus         | Notes                           |
|-------------------|-------------------|---------------------------------|
| PIR sensor        | GPIO 4 (BCM)      | HC-SR501, output to GPIO        |
| 4×4 Keypad rows   | GPIO 5,6,13,19    | Output (BCM)                    |
| 4×4 Keypad cols   | GPIO 26,21,20,16  | Input with pull-down (BCM)      |
| Passive buzzer    | GPIO 12 (BCM/PWM) | Siren during ALARM state        |
| Relay (strobe)    | GPIO 17 (BCM)     | Active-low relay board          |
| LCD (I²C)         | SDA/SCL (I²C-1)   | Address 0x27                    |

## FSM states

```
IDLE ──(PIR)──▶ ALARM ──(correct PIN)──▶ IDLE
              ╰──(3 wrong PINs)──▶ LOCKED ──(timeout 30 s)──▶ IDLE
```

| State    | Display message        | Siren |
|----------|------------------------|-------|
| `IDLE`   | `ARMED  Ready`         | off   |
| `ALARM`  | `!! ALARM !!  PIN:`    | on    |
| `LOCKED` | `LOCKED  30 s...`      | off   |

## Configuration

The PIN is stored as a SHA-256 hex digest in `alarm.properties`:

```properties
alarm.pin.hash=<sha256-of-your-pin>
alarm.lockout.seconds=30
```

Generate a hash:
```bash
echo -n "1234" | sha256sum
```

## Running

```bash
# Hardware mode
java -jar crowpi-suite-1.0.0.jar --project p04

# Mock mode (auto-fires PIR every 5 s, PIN = "0000")
java -jar crowpi-suite-1.0.0.jar --project p04 --mock
```

## Key classes

| Class            | Responsibility                                      |
|------------------|-----------------------------------------------------|
| `AlarmFsm`       | `EnumMap`-based FSM; `transition(AlarmEvent)`       |
| `PinValidator`   | SHA-256 hash comparison, fail counter, lockout flag |
| `AlarmProject`   | GPIO wiring, PIR listener, main loop                |
| `AlarmTest`      | 9 unit tests including FSM transitions and lockout  |
